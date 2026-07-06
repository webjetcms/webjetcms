package sk.iway.iwcm.rag.vectorstore;

import java.util.List;
import java.util.Map;

import sk.iway.iwcm.rag.service.RagEntityType;

/**
 * Abstraction for vector storage and similarity search.
 * Handles ONLY the embedding (vector) column via native SQL.
 * Entity CRUD operations are handled by EmbeddingChunkRepository (JPA).
 * Primary implementation uses PgVector (PostgreSQL + pgvector extension).
 */
public interface VectorStore {

    /**
     * Update the embedding vector for an existing chunk entity.
     * The entity must already be saved via EmbeddingChunkRepository.
     * @param id the chunk entity ID
     * @param embedding the embedding vector
     */
    void updateEmbedding(Long id, float[] embedding);

    /**
     * Update embedding vectors for multiple existing chunk entities in a batch.
     * All entities must already be saved via EmbeddingChunkRepository.
     * @param ids the chunk entity IDs
     * @param embeddings the embedding vectors (must match ids in size and order)
     */
    void updateEmbeddingBatch(List<Long> ids, List<float[]> embeddings);

    /**
     * Find the most similar chunks to the query embedding.
     * @param queryEmbedding the query vector
     * @param embeddingModel model used to generate/query embeddings
     * @param entityType entity type to filter by (null for all)
     * @param domainId domain ID to filter by (null for all)
     * @param language language to filter by (null for all)
     * @param limit max number of results
     * @param bonusParams optional store-specific filters, such as document root groups
     * @return list of search results ordered by similarity (descending)
     */
    List<VectorSearchResult> search(float[] queryEmbedding, String embeddingModel, RagEntityType entityType, Integer domainId, String language, int limit, Map<String, Object> bonusParams);

    /**
     * Find relevant chunks by fulltext search in chunk text.
     * @param query textual query
     * @param embeddingModel model used to filter rows
     * @param entityType entity type to filter by (null for all)
     * @param domainId domain ID to filter by (null for all)
     * @param language language to filter by (null for all)
     * @param limit max number of results
     * @param bonusParams optional store-specific filters, such as document root groups and fallback flags
     * @return list of search results ordered by fulltext rank (descending)
     */
    List<VectorSearchResult> searchFulltext(String query, String embeddingModel, RagEntityType entityType, Integer domainId, String language, int limit, Map<String, Object> bonusParams);

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
