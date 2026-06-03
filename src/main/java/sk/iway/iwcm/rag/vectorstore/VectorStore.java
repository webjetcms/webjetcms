package sk.iway.iwcm.rag.vectorstore;

import java.util.List;
import java.util.Map;

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
     * @param embeddingModel model used to generate/query embeddings
     * @param domainId domain ID to filter by (null for all)
     * @param language language to filter by (null for all)
     * @param limit max number of results
     * @return list of search results ordered by similarity (descending)
     */
    List<VectorSearchResult> search(float[] queryEmbedding, String embeddingModel, Integer domainId, String language, int limit);

    /**
     * Find relevant chunks by fulltext search in chunk text.
     * @param query textual query
     * @param embeddingModel model used to filter rows
     * @param domainId domain ID to filter by (null for all)
     * @param language language to filter by (null for all)
     * @param limit max number of results
     * @return list of search results ordered by fulltext rank (descending)
     */
    List<VectorSearchResult> searchFulltext(String query, String embeddingModel, Integer domainId, String language, int limit);

    /**
     * Check if the vector store can be used in the current runtime configuration.
     * This does not guarantee that required schema objects already exist.
     */
    boolean isAvailable();

    /**
     * Check if the vector store is available and schema is initialized.
     * Implementations should return true only when operations can be executed safely.
     */
    boolean isAvailableAndInitialized();

    /**
     * Mark all chunks for an entity as ERROR with a message.
     * @param entityType entity type
     * @param entityId entity ID
     * @param embeddingModel model used
     * @param errorMessage error description
     */
    void markError(String entityType, long entityId, String embeddingModel, String errorMessage);

    /**
     * Initialize the pgvector extension and create the table if needed.
     */
    boolean initializeSchema();

    /**
     * Delete all stored embeddings for a specific model.
     * @param embeddingModel model used to generate the embeddings to delete
     */
    boolean deleteModelData(String embeddingModel);

    /**
     * Get existing embeddings for an entity, keyed by content hash.
     * Used to skip re-embedding unchanged chunks.
     */
    Map<String, float[]> getExistingEmbeddingsByHash(String entityType, long entityId, String embeddingModel);
}
