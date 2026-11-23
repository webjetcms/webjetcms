package sk.iway.iwcm.rag.vectorstore;

import lombok.Getter;
import lombok.Setter;

/**
 * Result from a vector similarity search.
 */
@Getter
@Setter
public class VectorSearchResult {

    private Long chunkId;
    private String entityType;
    private Long entityId;
    private Integer chunkIndex;
    private String chunkText;
    private Double similarity;
    private Double rerankScore;
    private String sourceTitle;
    private String sourceUrl;
    private String sourcePath;

    public VectorSearchResult() {}

    /** Returns the rerank score when available, otherwise the original retrieval score. */
    public double getRankingScore() {
        if (rerankScore != null) return rerankScore;
        return similarity != null ? similarity : 0d;
    }

    public VectorSearchResult(Long chunkId, String entityType, Long entityId, Integer chunkIndex, String chunkText, Double similarity) {
        this.chunkId = chunkId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.chunkIndex = chunkIndex;
        this.chunkText = chunkText;
        this.similarity = similarity;
    }
}
