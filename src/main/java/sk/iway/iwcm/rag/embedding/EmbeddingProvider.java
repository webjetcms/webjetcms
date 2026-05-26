package sk.iway.iwcm.rag.embedding;

import java.util.List;

/**
 * Interface for generating text embeddings.
 * Implementations call an external API (e.g., OpenAI /v1/embeddings) to convert text to vector representations.
 */
public interface EmbeddingProvider {

    /**
     * Generate embeddings for a list of text chunks.
     * @param texts list of text strings to embed
     * @param model the embedding model name (e.g., "text-embedding-3-small")
     * @return list of float arrays, one embedding per input text
     */
    List<float[]> embed(List<String> texts, String model);

    /**
     * Generate embeddings with provider usage metadata.
     * Implementations may override this method to return token usage.
     */
    default EmbeddingBatchResult embedWithUsage(List<String> texts, String model) {
        return new EmbeddingBatchResult(embed(texts, model), 0);
    }

    /**
     * Generate embedding for a single text.
     * @param text text string to embed
     * @param model the embedding model name
     * @return float array representing the embedding vector
     */
    default float[] embed(String text, String model) {
        List<float[]> results = embedWithUsage(List.of(text), model).getEmbeddings();
        if (results == null || results.isEmpty()) return new float[0];
        return results.get(0);
    }

    /**
     * Generate embedding for a single text with provider usage metadata.
     */
    default EmbeddingBatchResult embedWithUsage(String text, String model) {
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
