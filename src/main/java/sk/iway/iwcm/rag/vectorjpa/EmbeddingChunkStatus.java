package sk.iway.iwcm.rag.vectorjpa;

/**
 * Status of an embedding chunk in the RAG pipeline.
 */
public enum EmbeddingChunkStatus {
    PENDING,
    COMPLETED,
    ERROR
}
