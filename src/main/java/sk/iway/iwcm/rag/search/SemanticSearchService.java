package sk.iway.iwcm.rag.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
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

    private static final double ADAPTIVE_THRESHOLD_TOP_RATIO = 0.7d;

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

        List<SemanticSearchResult> sortedResults = docMap.values().stream()
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .toList();

        double minimumSimilarity = Tools.getDoubleValue(Constants.getString("ragSemanticSearchMinSimilarity"), 0.2d);
        int minimumResults = Constants.getInt("ragSemanticSearchMinResults");
        int minimumResultsForCall = Math.min(Math.max(0, minimumResults), Math.max(0, maxResults));

        return filterResultsBySimilarity(sortedResults, minimumSimilarity, minimumResultsForCall).stream()
            .limit(maxResults)
            .toList();
    }

    /**
     * Filter and sort semantic search results based on similarity thresholds and minimum result count.
     * Uses an adaptive similarity threshold based on the top result to allow for more results when the top similarity is low.
     * @param sortedResults - list of results sorted by similarity descending
     * @param minimumSimilarity - absolute minimum similarity threshold (0.0 to 1.0)
     * @param minimumResultCount - minimum number of results to return regardless of similarity (0 for no minimum)
     * @return list of filtered semantic search results
     */
    public List<SemanticSearchResult> filterResultsBySimilarity(List<SemanticSearchResult> sortedResults, double minimumSimilarity, int minimumResultCount) {
        if (sortedResults == null || sortedResults.isEmpty()) {
            return List.of();
        }

        int minCount = Math.max(0, minimumResultCount);
        double similarityFloor = Math.max(0d, Math.min(1d, minimumSimilarity));

        Double topSimilarityValue = sortedResults.get(0).getSimilarity();
        double topSimilarity = topSimilarityValue == null ? 0d : topSimilarityValue.doubleValue();
        double adaptiveSimilarityThreshold = Math.max(similarityFloor, topSimilarity * ADAPTIVE_THRESHOLD_TOP_RATIO);

        List<SemanticSearchResult> filteredResults = new ArrayList<>();
        for (SemanticSearchResult result : sortedResults) {
            if (result.getSimilarity() == null) continue;
            if (result.getSimilarity().doubleValue() >= adaptiveSimilarityThreshold || filteredResults.size() < minCount) {
                filteredResults.add(result);
            }
        }

        return filteredResults;
    }

    /**
     * Check if semantic search is enabled and available.
     */
    public boolean isAvailable() {
        return Constants.getBoolean("ragSemanticSearchEnabled") && vectorStore.isAvailable();
    }
}
