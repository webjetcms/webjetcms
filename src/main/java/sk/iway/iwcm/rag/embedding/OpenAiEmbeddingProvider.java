package sk.iway.iwcm.rag.embedding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;
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
    public List<float[]> embed(List<String> texts, String model) throws ProviderCallException {
        return embedWithUsage(texts, model).getEmbeddings();
    }

    @Override
    public EmbeddingBatchResult embedWithUsage(List<String> texts, String model) throws ProviderCallException {
        if (texts == null || texts.isEmpty()) return EmbeddingBatchResult.empty();

        String apiKey = OpenAiSupportService.getApiKey();
        if (Tools.isEmpty(apiKey)) {
            throw new ProviderCallException("OpenAI API key is not configured (ai_openAiAuthKey)");
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

            // Configure timeouts to prevent hanging on slow/unresponsive OpenAI API
            RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000)  // 10 seconds to connect
                .setSocketTimeout(30000)   // 30 seconds to read response
                .setConnectionRequestTimeout(5000)  // 5 seconds to get connection from pool
                .build();
            post.setConfig(requestConfig);

            // Create HTTP client with retry handler (3 retries for transient failures)
            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                    .build();
                CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (statusCode < 200 || statusCode >= 300) {
                    throw new ProviderCallException("OpenAI embeddings API error " + statusCode + ": " + responseBody);
                }

                JsonNode root = MAPPER.readTree(responseBody);
                List<float[]> embeddings = parseEmbeddings(root);
                int usedTokens = parseUsedTokens(root);
                return new EmbeddingBatchResult(embeddings, usedTokens);
            }
        } catch (IOException e) {
            throw new ProviderCallException("Error calling OpenAI embeddings API: " + e.getMessage());
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
        * @param root parsed JSON response body
     * @return list of embedding vectors
     */
    private List<float[]> parseEmbeddings(JsonNode root) {
        List<float[]> embeddings = new ArrayList<>();
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

    private int parseUsedTokens(JsonNode root) {
        JsonNode usage = root.path("usage");
        if (usage.isMissingNode() || usage.isNull()) return 0;

        int totalTokens = usage.path("total_tokens").asInt(0);
        if (totalTokens > 0) return totalTokens;

        return usage.path("prompt_tokens").asInt(0);
    }
}
