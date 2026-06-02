package sk.iway.iwcm.rag.embedding;

import java.util.List;

/**
 * Result of embedding generation containing vectors and provider usage metadata.
 */
public class EmbeddingBatchResult {

    private final List<float[]> embeddings;
    private final int usedTokens;

    public EmbeddingBatchResult(List<float[]> embeddings, int usedTokens) {
        this.embeddings = embeddings == null ? List.of() : embeddings;
        this.usedTokens = usedTokens;
    }

    public List<float[]> getEmbeddings() {
        return embeddings;
    }

    public int getUsedTokens() {
        return usedTokens;
    }

    public static EmbeddingBatchResult empty() {
        return new EmbeddingBatchResult(List.of(), 0);
    }
}