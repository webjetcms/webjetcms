package sk.iway.iwcm.rag.vectorstore;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.rag.vectorstore.VectorStoreDataSourceResolver.Resolution;

/**
 * Routes vector operations to the implementation matching the selected RAG datasource.
 */
@Primary
@Service
public class VectorStoreRouter implements VectorStore {

    private final PgVectorStore pgVectorStore;
    private final MariaDbVectorStore mariaDbVectorStore;

    private volatile String lastLoggedUnavailableReason;

    public VectorStoreRouter(PgVectorStore pgVectorStore, MariaDbVectorStore mariaDbVectorStore) {
        this.pgVectorStore = pgVectorStore;
        this.mariaDbVectorStore = mariaDbVectorStore;
    }

    @Override
    public void updateEmbedding(Long id, float[] embedding) {
        VectorStore delegate = getDelegate();
        if (delegate != null) delegate.updateEmbedding(id, embedding);
    }

    @Override
    public void updateEmbeddingBatch(List<Long> ids, List<float[]> embeddings) {
        VectorStore delegate = getDelegate();
        if (delegate != null) delegate.updateEmbeddingBatch(ids, embeddings);
    }

    @Override
    public List<VectorSearchResult> search(float[] queryEmbedding, String embeddingProvider, String embeddingModel,
                                           RagEntityType entityType, Integer domainId, String language, int limit,
                                           Map<String, Object> bonusParams) {
        VectorStore delegate = getDelegate();
        if (delegate == null) return List.of();
        return delegate.search(queryEmbedding, embeddingProvider, embeddingModel, entityType, domainId, language, limit, bonusParams);
    }

    @Override
    public List<VectorSearchResult> searchFulltext(String query, String embeddingProvider, String embeddingModel,
                                                   RagEntityType entityType, Integer domainId, String language, int limit,
                                                   Map<String, Object> bonusParams) {
        VectorStore delegate = getDelegate();
        if (delegate == null) return List.of();
        return delegate.searchFulltext(query, embeddingProvider, embeddingModel, entityType, domainId, language, limit, bonusParams);
    }

    @Override
    public boolean isAvailable() {
        if (Constants.getBoolean("ragSemanticSearchEnabled") == false) return false;

        Resolution resolution = VectorStoreDataSourceResolver.resolve();
        VectorStore delegate = getDelegate(resolution);
        return delegate != null && delegate.isAvailable();
    }

    @Override
    public boolean isAvailableAndInitialized() {
        VectorStore delegate = getDelegate();
        return delegate != null && delegate.isAvailableAndInitialized();
    }

    @Override
    public boolean initializeSchema() {
        VectorStore delegate = getDelegate();
        return delegate != null && delegate.initializeSchema();
    }

    @Override
    public boolean recreateIndex() {
        VectorStore delegate = getDelegate();
        return delegate != null && delegate.recreateIndex();
    }

    @Override
    public boolean resetDimensions(int dimensions) {
        VectorStore delegate = getDelegate();
        return delegate != null && delegate.resetDimensions(dimensions);
    }

    @Override
    @Deprecated(forRemoval = false)
    public Map<String, float[]> getExistingEmbeddingsByHash(String entityType, long entityId,
                                                             String embeddingProvider, String embeddingModel) {
        VectorStore delegate = getDelegate();
        if (delegate == null) return Map.of();
        return delegate.getExistingEmbeddingsByHash(entityType, entityId, embeddingProvider, embeddingModel);
    }

    @Override
    public Map<String, float[]> getExistingEmbeddingsByHash(String entityType, long entityId,
                                                             String embeddingProvider, String embeddingModel,
                                                             int domainId) {
        VectorStore delegate = getDelegate();
        if (delegate == null) return Map.of();
        return delegate.getExistingEmbeddingsByHash(entityType, entityId, embeddingProvider, embeddingModel, domainId);
    }

    private VectorStore getDelegate() {
        return getDelegate(VectorStoreDataSourceResolver.resolve());
    }

    private VectorStore getDelegate(Resolution resolution) {
        return switch (resolution.backend()) {
            case POSTGRESQL -> pgVectorStore;
            case MARIADB -> mariaDbVectorStore;
            case UNSUPPORTED -> {
                logUnavailable(resolution.reason());
                yield null;
            }
        };
    }

    private void logUnavailable(String reason) {
        String message = reason == null ? "Unsupported RAG vector datasource" : reason;
        if (message.equals(lastLoggedUnavailableReason)) return;

        lastLoggedUnavailableReason = message;
        Logger.warn(VectorStoreRouter.class, "RAG vector store is unavailable: " + message);
    }
}
