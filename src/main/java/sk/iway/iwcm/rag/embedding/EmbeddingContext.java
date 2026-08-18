package sk.iway.iwcm.rag.embedding;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Request context used to resolve provider-specific headers and embedding task semantics.
 */
public record EmbeddingContext(HttpServletRequest request, String domainName, TaskType taskType) {

    public enum TaskType {
        RETRIEVAL_DOCUMENT,
        RETRIEVAL_QUERY
    }

    public static EmbeddingContext forSearch(HttpServletRequest request) {
        return new EmbeddingContext(request, null, TaskType.RETRIEVAL_QUERY);
    }

    public static EmbeddingContext forIndexing(String domainName) {
        return new EmbeddingContext(null, domainName, TaskType.RETRIEVAL_DOCUMENT);
    }
}
