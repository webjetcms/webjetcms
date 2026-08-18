package sk.iway.iwcm.rag.embedding;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;

/**
 * OpenRouter embedding provider using its OpenAI-compatible /embeddings API.
 */
@Service
public class OpenRouterEmbeddingProvider extends OpenAiEmbeddingProvider {

    private static final String EMBEDDINGS_URL = "https://openrouter.ai/api/v1/embeddings";

    public OpenRouterEmbeddingProvider(WebjetAiConfigurationService configurationService) {
        super(configurationService);
    }

    @Override
    public String getProviderId() {
        return "openrouter";
    }

    @Override
    protected String getEmbeddingsUrl() {
        return EMBEDDINGS_URL;
    }

    @Override
    protected String getProviderName() {
        return "OpenRouter";
    }

    @Override
    protected String getApiKeyConfigurationName() {
        return "ai_openRouterAuthKey";
    }
}
