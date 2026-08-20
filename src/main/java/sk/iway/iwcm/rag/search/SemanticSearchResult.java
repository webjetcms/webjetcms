package sk.iway.iwcm.rag.search;

import lombok.Getter;
import lombok.Setter;

/**
 * Result from semantic search: a document ID with its similarity score.
 */
@Getter
@Setter
public class SemanticSearchResult {

    private Long docId;
    private Double similarity;

    public SemanticSearchResult() {}

    public SemanticSearchResult(Long docId, Double similarity) {
        this.docId = docId;
        this.similarity = similarity;
    }
}
