package sk.iway.iwcm.rag.embedding;

import java.util.List;

import sk.iway.iwcm.components.ai.providers.ProviderCallException;

/**
 * Interface for generating text embeddings.
 * Implementations call an external API (e.g., OpenAI /v1/embeddings) to convert text to vector representations.
 */
public interface EmbeddingProvider {

    /**
     * Returns the provider identifier used by AI configuration and statistics.
     */
    default String getProviderId() {
        return "openai";
    }

    /**
     * Generate embeddings for a list of text chunks.
     * @param texts list of text strings to embed
     * @param model the embedding model name (e.g., "text-embedding-3-small")
     * @return list of float arrays, one embedding per input text
     */
    List<float[]> embed(List<String> texts, String model) throws ProviderCallException;

    /**
     * Generate embeddings with provider usage metadata.
     * Implementations may override this method to return token usage.
     */
    default EmbeddingBatchResult embedWithUsage(List<String> texts, String model) throws ProviderCallException {
        return new EmbeddingBatchResult(embed(texts, model), 0);
    }

    /**
     * Generate embeddings using request or domain context for provider-specific trusted headers.
     */
    default EmbeddingBatchResult embedWithUsage(List<String> texts, String model, EmbeddingContext context) throws ProviderCallException {
        return embedWithUsage(texts, model);
    }

    /**
     * Generate embedding for a single text.
     * @param text text string to embed
     * @param model the embedding model name
     * @return float array representing the embedding vector
     */
    default float[] embed(String text, String model) throws ProviderCallException {
        List<float[]> results = embedWithUsage(List.of(text), model).getEmbeddings();
        if (results == null || results.isEmpty()) return new float[0];
        return results.get(0);
    }

    /**
     * Generate embedding for a single text with provider usage metadata.
     */
    default EmbeddingBatchResult embedWithUsage(String text, String model) throws ProviderCallException {
        return embedWithUsage(List.of(text), model);
    }

    /**
     * Returns the number of dimensions for the given model.
     */
    int getDimensions(String model);

    /**
     * Returns the model identifier string.
     */
    String getDefaultModel();
}
