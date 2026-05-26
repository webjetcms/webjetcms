package sk.iway.iwcm.rag.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.rag.embedding.EmbeddingBatchResult;
import sk.iway.iwcm.rag.embedding.EmbeddingProvider;
import sk.iway.iwcm.rag.service.RagEmbeddingStatService;
import sk.iway.iwcm.rag.vectorstore.PgVectorStore;
import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;

/**
 * Service for semantic search over document embeddings.
 * Embeds the query, searches pgvector, and returns results aggregated by document.
 */
@Service
public class SemanticSearchService {

    private final EmbeddingProvider embeddingProvider;
    private final PgVectorStore vectorStore;
    private final RagEmbeddingStatService ragEmbeddingStatService;

    @Autowired
    public SemanticSearchService(EmbeddingProvider embeddingProvider, PgVectorStore vectorStore, RagEmbeddingStatService ragEmbeddingStatService) {
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.ragEmbeddingStatService = ragEmbeddingStatService;
    }

    /**
     * Search for documents similar to the query text.
     * Returns unique document IDs ordered by best chunk similarity.
     *
     * @param query search query text
     * @param domainId domain to search in (null for all)
     * @param language language filter (null for all)
     * @param maxResults maximum number of unique documents to return
     * @return list of document IDs with their best similarity scores
     */
    public List<SemanticSearchResult> search(String query, Integer domainId, String language, int maxResults) {
        if (vectorStore.isAvailable() == false) {
            Logger.debug(SemanticSearchService.class, "Vector store not available, returning empty results");
            return List.of();
        }

        String model = embeddingProvider.getDefaultModel();
        EmbeddingBatchResult embeddingResult = embeddingProvider.embedWithUsage(List.of(query), model);
        List<float[]> queryEmbeddings = embeddingResult.getEmbeddings();
        if (queryEmbeddings.isEmpty()) {
            Logger.error(SemanticSearchService.class, "Failed to generate query embedding");
            return List.of();
        }

        ragEmbeddingStatService.recordSearchTokens(embeddingResult.getUsedTokens());

        float[] queryEmbedding = queryEmbeddings.get(0);
        if (queryEmbedding.length == 0) {
            Logger.error(SemanticSearchService.class, "Failed to generate query embedding");
            return List.of();
        }

        // Fetch more chunks than needed since multiple chunks can belong to the same document
        int chunkLimit = maxResults * 3;
        List<VectorSearchResult> chunkResults = vectorStore.search(queryEmbedding, domainId, language, chunkLimit);

        // Aggregate by entity_id, keeping best similarity per document
        Map<Long, SemanticSearchResult> docMap = new HashMap<>();
        for (VectorSearchResult chunk : chunkResults) {
            if ("document".equalsIgnoreCase(chunk.getEntityType()) == false) continue;

            docMap.compute(chunk.getEntityId(), (id, existing) -> {
                if (existing == null) {
                    return new SemanticSearchResult(chunk.getEntityId(), chunk.getSimilarity());
                }
                if (chunk.getSimilarity() > existing.getSimilarity()) {
                    existing.setSimilarity(chunk.getSimilarity());
                }
                return existing;
            });
        }

        // Sort by similarity descending and limit
        return docMap.values().stream()
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .limit(maxResults)
            .toList();
    }

    /**
     * Check if semantic search is enabled and available.
     */
    public boolean isAvailable() {
        return Constants.getBoolean("ragSemanticSearchEnabled") && vectorStore.isAvailable();
    }
}
