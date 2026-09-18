package sk.iway.iwcm.rag.vectorstore;

/**
 * Database backends supported by the RAG vector store.
 */
public enum VectorStoreBackend {
    POSTGRESQL,
    MARIADB,
    UNSUPPORTED
}
