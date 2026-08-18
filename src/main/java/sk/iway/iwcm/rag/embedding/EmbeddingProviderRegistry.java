package sk.iway.iwcm.rag.embedding;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;

/**
 * Resolves an embedding provider by the identifier stored in an AI assistant.
 */
@Service
public class EmbeddingProviderRegistry {

    private final OpenAiEmbeddingProvider openAiProvider;
    private final GeminiEmbeddingProvider geminiProvider;
    private final OpenRouterEmbeddingProvider openRouterProvider;

    public EmbeddingProviderRegistry(@Qualifier("openAiEmbeddingProvider") OpenAiEmbeddingProvider openAiProvider,
                                     GeminiEmbeddingProvider geminiProvider,
                                     OpenRouterEmbeddingProvider openRouterProvider) {
        this.openAiProvider = openAiProvider;
        this.geminiProvider = geminiProvider;
        this.openRouterProvider = openRouterProvider;
    }

    public EmbeddingProvider getProvider(String providerId) throws ProviderCallException {
        if (Tools.isEmpty(providerId)) {
            throw new ProviderCallException("Embedding assistant has no provider configured");
        }

        String normalizedProviderId = providerId.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedProviderId) {
            case "openai" -> openAiProvider;
            case "gemini" -> geminiProvider;
            case "openrouter" -> openRouterProvider;
            default -> throw new ProviderCallException(
                "Unsupported embedding provider: " + normalizedProviderId
                    + ". Supported providers are openai, gemini and openrouter."
            );
        };
    }
}
