package sk.iway.iwcm.rag.pgvector;

/**
 * Status of an embedding chunk in the RAG pipeline.
 */
public enum EmbeddingChunkStatus {
    PENDING,
    COMPLETED,
    ERROR
}
