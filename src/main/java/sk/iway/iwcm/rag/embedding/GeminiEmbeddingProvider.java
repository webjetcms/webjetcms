package sk.iway.iwcm.rag.embedding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import com.webjetcms.ai.AiProviderConfig;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;

/**
 * Gemini embedding provider using the native batchEmbedContents API.
 */
@Service
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebjetAiConfigurationService configurationService;

    public GeminiEmbeddingProvider(WebjetAiConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public String getProviderId() {
        return "gemini";
    }

    @Override
    public List<float[]> embed(List<String> texts, String model) throws ProviderCallException {
        return embedWithUsage(texts, model).getEmbeddings();
    }

    @Override
    public EmbeddingBatchResult embedWithUsage(List<String> texts, String model) throws ProviderCallException {
        return embedWithUsage(texts, model, null);
    }

    @Override
    public EmbeddingBatchResult embedWithUsage(List<String> texts, String model, EmbeddingContext context) throws ProviderCallException {
        if (texts == null || texts.isEmpty()) return EmbeddingBatchResult.empty();

        AiProviderConfig configuration = resolveConfiguration(context);
        if (Tools.isEmpty(configuration.apiKey())) {
            throw new ProviderCallException("Gemini API key is not configured (ai_geminiAuthKey)");
        }

        if (Tools.isEmpty(model)) model = getDefaultModel();
        String modelResource = normalizeModelResource(model);
        String taskType = getTaskType(model, context);

        try {
            ObjectNode requestBody = MAPPER.createObjectNode();
            ArrayNode requests = MAPPER.createArrayNode();
            int dimensions = getDimensions(model);

            for (String text : texts) {
                ObjectNode request = MAPPER.createObjectNode();
                request.put("model", modelResource);

                ObjectNode part = MAPPER.createObjectNode();
                part.put("text", text);
                ArrayNode parts = MAPPER.createArrayNode();
                parts.add(part);
                ObjectNode content = MAPPER.createObjectNode();
                content.set("parts", parts);
                request.set("content", content);

                if (dimensions > 0) {
                    request.put("outputDimensionality", dimensions);
                }
                if (taskType != null) {
                    request.put("taskType", taskType);
                }
                requests.add(request);
            }
            requestBody.set("requests", requests);

            HttpPost post = new HttpPost(getEmbeddingsUrl(modelResource));
            post.setHeader("x-goog-api-key", configuration.apiKey());
            post.setHeader("Content-Type", "application/json; charset=utf-8");
            for (Map.Entry<String, String> header : configuration.trustedHeaders().entrySet()) {
                post.setHeader(header.getKey(), header.getValue());
            }
            post.setEntity(new StringEntity(MAPPER.writeValueAsString(requestBody), StandardCharsets.UTF_8));

            RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(configuration.connectTimeoutMillis())
                .setSocketTimeout(configuration.responseTimeoutMillis())
                .setConnectionRequestTimeout(configuration.connectTimeoutMillis())
                .build();
            post.setConfig(requestConfig);

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                    .build();
                CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (statusCode < 200 || statusCode >= 300) {
                    throw new ProviderCallException("Gemini embeddings API error " + statusCode + ": " + responseBody);
                }

                JsonNode root = MAPPER.readTree(responseBody);
                List<float[]> embeddings = parseEmbeddings(root);
                enforceDimensions(embeddings, dimensions);
                if (isReducedGeminiEmbedding001(model, dimensions)) {
                    normalizeEmbeddings(embeddings);
                }
                return new EmbeddingBatchResult(embeddings, parseUsedTokens(root));
            }
        } catch (IOException e) {
            throw new ProviderCallException("Error calling Gemini embeddings API: " + e.getMessage(), e);
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

    String getEmbeddingsUrl(String modelResource) {
        return API_BASE_URL + "/" + modelResource + ":batchEmbedContents";
    }

    private AiProviderConfig resolveConfiguration(EmbeddingContext context) {
        if (context != null) {
            if (context.request() != null) {
                return configurationService.resolve(getProviderId(), context.request());
            }
            if (Tools.isNotEmpty(context.domainName())) {
                return configurationService.resolveForDomain(getProviderId(), context.domainName());
            }
        }
        return configurationService.resolve(getProviderId());
    }

    private String getTaskType(String model, EmbeddingContext context) {
        if (context == null || context.taskType() == null || isGeminiEmbedding001(model) == false) {
            return null;
        }
        return context.taskType().name();
    }

    private boolean isReducedGeminiEmbedding001(String model, int dimensions) {
        return dimensions > 0 && dimensions < 3072 && isGeminiEmbedding001(model);
    }

    private boolean isGeminiEmbedding001(String model) {
        if (Tools.isEmpty(model)) return false;
        String modelName = model.startsWith("models/") ? model.substring("models/".length()) : model;
        return "gemini-embedding-001".equals(modelName);
    }

    private void normalizeEmbeddings(List<float[]> embeddings) {
        for (float[] embedding : embeddings) {
            double sumOfSquares = 0;
            for (float value : embedding) {
                sumOfSquares += value * value;
            }
            if (sumOfSquares == 0) continue;

            double norm = Math.sqrt(sumOfSquares);
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] = (float) (embedding[i] / norm);
            }
        }
    }

    private void enforceDimensions(List<float[]> embeddings, int dimensions) throws ProviderCallException {
        if (dimensions < 1) return;

        for (int i = 0; i < embeddings.size(); i++) {
            float[] embedding = embeddings.get(i);
            if (embedding.length < dimensions) {
                throw new ProviderCallException(
                    "Gemini returned " + embedding.length + " embedding dimensions, expected " + dimensions
                );
            }
            if (embedding.length > dimensions) {
                embeddings.set(i, Arrays.copyOf(embedding, dimensions));
            }
        }
    }

    private String normalizeModelResource(String model) throws ProviderCallException {
        String modelName = model.trim();
        if (modelName.startsWith("models/")) {
            modelName = modelName.substring("models/".length());
        }
        if (modelName.matches("[A-Za-z0-9._-]+") == false) {
            throw new ProviderCallException("Invalid Gemini embedding model name: " + model);
        }
        return "models/" + modelName;
    }

    private List<float[]> parseEmbeddings(JsonNode root) {
        List<float[]> embeddings = new ArrayList<>();
        JsonNode embeddingsArray = root.path("embeddings");

        if (embeddingsArray.isArray()) {
            for (JsonNode item : embeddingsArray) {
                JsonNode valuesNode = item.path("values");
                if (valuesNode.isArray()) {
                    float[] vector = new float[valuesNode.size()];
                    for (int i = 0; i < valuesNode.size(); i++) {
                        vector[i] = (float) valuesNode.get(i).asDouble();
                    }
                    embeddings.add(vector);
                }
            }
        }

        return embeddings;
    }

    private int parseUsedTokens(JsonNode root) {
        return root.path("usageMetadata").path("promptTokenCount").asInt(0);
    }
}
