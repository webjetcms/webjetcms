package sk.iway.iwcm.rag.vectorstore;

import java.util.List;

/**
 * Abstraction for vector storage and similarity search.
 * Primary implementation uses PgVector (PostgreSQL + pgvector extension).
 */
public interface VectorStore {

    /**
     * Store a chunk with its embedding vector.
     * @param entityType entity type (e.g., "document")
     * @param entityId entity ID
     * @param chunkIndex chunk position within the entity
     * @param chunkText the text content of the chunk
     * @param contentHash SHA-256 hash of the chunk text for deduplication
     * @param embedding the embedding vector
     * @param embeddingModel model used to generate the embedding
     * @param dimensions number of dimensions
     * @param language language code
     * @param domainId domain ID
     */
    void store(String entityType, long entityId, int chunkIndex, String chunkText,
               String contentHash, float[] embedding, String embeddingModel,
               int dimensions, String language, Integer domainId);

    /**
     * Delete all chunks for an entity.
     * @param entityType entity type
     * @param entityId entity ID
     */
    void deleteByEntity(String entityType, long entityId);

    /**
     * Find the most similar chunks to the query embedding.
     * @param queryEmbedding the query vector
     * @param domainId domain ID to filter by (null for all)
     * @param language language to filter by (null for all)
     * @param limit max number of results
     * @return list of search results ordered by similarity (descending)
     */
    List<VectorSearchResult> search(float[] queryEmbedding, Integer domainId, String language, int limit);

    /**
     * Check if the vector store is available and properly initialized.
     */
    boolean isAvailable();

    /**
     * Initialize the pgvector extension and create the table if needed.
     */
    void initializeSchema();
}
