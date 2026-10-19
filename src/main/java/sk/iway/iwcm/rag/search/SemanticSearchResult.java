package sk.iway.iwcm.rag.search;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;

/**
 * Result from semantic search: a source ID with its best retrieval and local ranking scores.
 */
@Getter
@Setter
public class SemanticSearchResult {

    private Long docId;
    private Double similarity;
    private Double rerankScore;

    public SemanticSearchResult() {}

    /**
     * Initializes source scores from one candidate chunk.
     *
     * @param chunk candidate supplying the source ID, retrieval score, and optional rerank score
     */
    public SemanticSearchResult(VectorSearchResult chunk) {
        this(chunk.getEntityId(), chunk.getSimilarity());
        this.rerankScore = chunk.getRerankScore();
    }

    /** Returns the score used to order results without changing the original similarity. */
    public double getRankingScore() {
        if (rerankScore != null) return rerankScore;
        return similarity != null ? similarity : 0d;
    }

    public SemanticSearchResult(Long docId, Double similarity) {
        this.docId = docId;
        this.similarity = similarity;
    }
}
