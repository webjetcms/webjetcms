package sk.iway.iwcm.rag.embedding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.providers.openai.OpenAiSupportService;

/**
 * OpenAI embedding provider using the /v1/embeddings API.
 * Uses the same API key as the AI assistants (ai_openAiAuthKey).
 */
@Service
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private static final String EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public List<float[]> embed(List<String> texts, String model) {
        if (texts == null || texts.isEmpty()) return List.of();

        String apiKey = OpenAiSupportService.getApiKey();
        if (Tools.isEmpty(apiKey)) {
            Logger.error(OpenAiEmbeddingProvider.class, "OpenAI API key is not configured (ai_openAiAuthKey)");
            return List.of();
        }

        if (Tools.isEmpty(model)) model = getDefaultModel();

        try {
            ObjectNode requestBody = MAPPER.createObjectNode();
            requestBody.put("model", model);

            if (supportsDimensionsParameter(model)) {
                int dimensions = getDimensions(model);
                if (dimensions > 0) {
                    requestBody.put("dimensions", dimensions);
                }
            }

            ArrayNode inputArray = MAPPER.createArrayNode();
            for (String text : texts) {
                inputArray.add(text);
            }
            requestBody.set("input", inputArray);

            HttpPost post = new HttpPost(EMBEDDINGS_URL);
            post.setHeader("Authorization", "Bearer " + apiKey);
            post.setHeader("Content-Type", "application/json; charset=utf-8");
            post.setEntity(new StringEntity(MAPPER.writeValueAsString(requestBody), StandardCharsets.UTF_8));

            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (statusCode < 200 || statusCode >= 300) {
                    Logger.error(OpenAiEmbeddingProvider.class, "OpenAI embeddings API error " + statusCode + ": " + responseBody);
                    return List.of();
                }

                return parseEmbeddings(responseBody);
            }
        } catch (IOException e) {
            Logger.error(OpenAiEmbeddingProvider.class, "Error calling OpenAI embeddings API: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public int getDimensions(String model) {
        return Constants.getInt("ragEmbeddingDimensions");
    }

    @Override
    public String getDefaultModel() {
        return Constants.getString("ragEmbeddingModel");
    }

    private boolean supportsDimensionsParameter(String model) {
        return Tools.isNotEmpty(model) && model.startsWith("text-embedding-3-");
    }

    /**
     * Parse the OpenAI embeddings API JSON response into a list of float arrays.
     * Expects the standard response format with a "data" array containing "embedding" arrays.
     * @param responseBody the raw JSON response body
     * @return list of embedding vectors
     * @throws IOException if JSON parsing fails
     */
    private List<float[]> parseEmbeddings(String responseBody) throws IOException {
        List<float[]> embeddings = new ArrayList<>();
        JsonNode root = MAPPER.readTree(responseBody);
        JsonNode dataArray = root.path("data");

        if (dataArray.isArray()) {
            for (JsonNode item : dataArray) {
                JsonNode embeddingNode = item.path("embedding");
                if (embeddingNode.isArray()) {
                    float[] vector = new float[embeddingNode.size()];
                    for (int i = 0; i < embeddingNode.size(); i++) {
                        vector[i] = (float) embeddingNode.get(i).asDouble();
                    }
                    embeddings.add(vector);
                }
            }
        }

        return embeddings;
    }
}
