package sk.iway.iwcm.rag.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.embedding.EmbeddingBatchResult;
import sk.iway.iwcm.rag.embedding.EmbeddingProvider;
import sk.iway.iwcm.rag.service.RagEmbeddingStatService;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.rag.service.RagSettingsService;
import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;
import sk.iway.iwcm.rag.vectorstore.VectorStore;

/**
 * Service for semantic search over document embeddings.
 * Embeds the query, searches pgvector, and returns results aggregated by document.
 */
@Service
public class SemanticSearchService {

    private static final double ADAPTIVE_THRESHOLD_TOP_RATIO = 0.7d;

    private static final String HYBRID_MODE_ALWAYS = "always";
    private static final String HYBRID_MODE_SHORT_QUERY_ONLY = "short_query_only";
    private static final String HYBRID_MODE_FALLBACK_ON_LOW_VECTOR = "fallback_on_low_vector";

    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final RagEmbeddingStatService ragEmbeddingStatService;

    private final RagService ragService;

    @Autowired
    public SemanticSearchService(EmbeddingProvider embeddingProvider, VectorStore vectorStore, RagEmbeddingStatService ragEmbeddingStatService, RagService ragService) {
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.ragEmbeddingStatService = ragEmbeddingStatService;

        this.ragService = ragService;
    }

    /**
     * Search for documents similar to the query text.
     * Returns unique document IDs ordered by best chunk similarity.
     *
     * @param query search query text
     * @param domainId domain to search in (null for all)
     * @param language language filter (null for all)
     * @param maxResults maximum number of unique documents to return
     * @param entityType entity type to filter by
     * @param request current request with component PageParams and rootGroup attributes
     * @return list of document IDs with their best similarity scores
     */
    public List<SemanticSearchResult> search(String query, Integer domainId, String language, int maxResults, RagEntityType entityType, HttpServletRequest request) {
        if (vectorStore.isAvailableAndInitialized() == false) {
            // If vector store is not available or not initialized, we cannot perform semantic search, return empty results
            Logger.debug(SemanticSearchService.class, "Vector store not available or initialized, returning empty results");
            return List.of();
        }

        String model = embeddingProvider.getDefaultModel();
        EmbeddingBatchResult embeddingResult;

        try {
            embeddingResult = embeddingProvider.embedWithUsage(List.of(query), model);
        } catch (ProviderCallException e) {
            Logger.error(SemanticSearchService.class, "Error generating query embedding: " + e.getMessage(), e);
            Adminlog.add(Adminlog.TYPE_SEARCH, "Error generating query embedding: " + e.getMessage(), null, null);
            return List.of();
        }

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

        PageParams pageParams = new PageParams(request);

        double minimumSimilarity = RagSettingsService.getSemanticMinimumSimilarity(pageParams);
        int minimumResults = RagSettingsService.getSemanticMinimumResults(pageParams);
        int minimumResultsForCall = Math.min(Math.max(0, minimumResults), Math.max(0, maxResults));

        int chunkFetchMultiplier = Math.max(1, RagSettingsService.getHybridChunkFetchMultiplier(pageParams));
        int chunkLimit = Math.max(1, maxResults * chunkFetchMultiplier);

        Map<String, Object> bonusParams = null;
        String rootGroupString = String.valueOf(request.getAttribute("rootGroup"));
        if(Tools.isNotEmpty(rootGroupString)) {
            GroupsDB groupsDB = GroupsDB.getInstance();

            int[] rootGroupsIds = Tools.getTokensInt(rootGroupString, "+");

            // Prefer indexed root-group columns for the first three levels, but keep full subtree
            // in group_id as well to preserve deep-folder scoping.
            Set<Integer> first3LayersGroups = new HashSet<>();
            for(int rootGroupId : rootGroupsIds) {
                if(rootGroupId < 1) continue;

                for(GroupDetails group : groupsDB.getGroupsTree(rootGroupId, true, false, false, 2)) {
                    first3LayersGroups.add( group.getGroupId() );
                }
            }

            Set<Integer> restGroups = new HashSet<>();
            for(int rootGroupId : rootGroupsIds) {
                if(rootGroupId < 1) continue;

                for(GroupDetails group : groupsDB.getGroupsTree(rootGroupId, false, true)) {
                    restGroups.add(group.getGroupId());
                }
            }

            bonusParams = new HashMap<>();
            bonusParams.put("rootGroupL1", first3LayersGroups);
            bonusParams.put("rootGroupL2", first3LayersGroups);
            bonusParams.put("rootGroupL3", first3LayersGroups);
            bonusParams.put("rootGroups", restGroups);
        }

        List<VectorSearchResult> vectorChunkResults = vectorStore.search(queryEmbedding, model, entityType, domainId, language, chunkLimit, bonusParams);

        boolean useHybridSearch = shouldUseHybridSearch(query, vectorChunkResults, minimumResultsForCall, pageParams);
        List<VectorSearchResult> chunkResults = vectorChunkResults;
        if (useHybridSearch) {
            if(bonusParams == null) bonusParams = new HashMap<>();
            bonusParams.put("hybridFtsUseIlikeFallback", RagSettingsService.getHybridFtsUseIlikeFallback(pageParams));

            List<VectorSearchResult> fulltextChunkResults = vectorStore.searchFulltext(query, model, entityType, domainId, language, chunkLimit, bonusParams);
            if (fulltextChunkResults.isEmpty() == false) {
                List<VectorSearchResult> mergedChunkResults = mergeChunkResultsWithRrf(vectorChunkResults, fulltextChunkResults, pageParams);
                if (mergedChunkResults.isEmpty() == false) {
                    chunkResults = mergedChunkResults;
                }
            }
        }

        // Generate answers from raw vector chunks because the answer post-processor has its own context merge rules.
        String answer = null;
        if(RagSettingsService.isAnswerAllowed(pageParams)) {
            answer = ragService.answerQuestion(query, domainId, chunkResults, request);
            answer = Tools.escapeHtml(answer); // just in case
        }
        request.setAttribute("ragAnswer", Tools.isEmpty(answer) ? null : answer);

        List<SemanticSearchResult> sortedResults = aggregateByDocumentBestScore(chunkResults);

        return filterResultsBySimilarity(sortedResults, minimumSimilarity, minimumResultsForCall).stream()
            .limit(maxResults)
            .toList();
    }

    boolean shouldUseHybridSearch(String query, List<VectorSearchResult> vectorChunkResults, int minimumResultsForCall, PageParams pageParams) {
        if (RagSettingsService.isHybridSearchEnabled(pageParams) == false) return false;

        String mode = RagSettingsService.getHybridSearchMode(pageParams).toLowerCase();
        return switch (mode) {
            case HYBRID_MODE_ALWAYS -> true;
            case HYBRID_MODE_SHORT_QUERY_ONLY -> isShortQuery(query, pageParams);
            case HYBRID_MODE_FALLBACK_ON_LOW_VECTOR -> {
                double topSimilarityThreshold = RagSettingsService.getHybridFallbackTopSimilarity(pageParams);

                boolean lowTopSimilarity = getTopSimilarity(vectorChunkResults) < topSimilarityThreshold;
                boolean fewResults = vectorChunkResults.size() < minimumResultsForCall;
                yield lowTopSimilarity || fewResults;
            }
            default -> false;
        };
    }

    private boolean isShortQuery(String query, PageParams pageParams) {
        String normalizedQuery = Tools.getStringValue(query, "").trim();
        if (normalizedQuery.isEmpty()) return false;

        int maxChars = Math.max(1, RagSettingsService.getHybridShortQueryMaxChars(pageParams));
        int maxTerms = Math.max(1, RagSettingsService.getHybridShortQueryMaxTerms(pageParams));
        int termCount = normalizedQuery.split("\\s+").length;

        return normalizedQuery.length() <= maxChars || termCount <= maxTerms;
    }

    private double getTopSimilarity(List<VectorSearchResult> vectorChunkResults) {
        if (vectorChunkResults == null || vectorChunkResults.isEmpty()) return 0d;

        Double topSimilarity = vectorChunkResults.get(0).getSimilarity();
        return topSimilarity == null ? 0d : topSimilarity.doubleValue();
    }

    List<VectorSearchResult> mergeChunkResultsWithRrf(List<VectorSearchResult> vectorChunkResults, List<VectorSearchResult> fulltextChunkResults, PageParams pageParams) {
        if ((vectorChunkResults == null || vectorChunkResults.isEmpty()) && (fulltextChunkResults == null || fulltextChunkResults.isEmpty())) {
            return List.of();
        }

        double vectorWeight = RagSettingsService.getHybridVectorWeight(pageParams);
        double fulltextWeight = RagSettingsService.getHybridFtsWeight(pageParams);
        int rrfK = Math.max(1, RagSettingsService.getHybridRrfK(pageParams));
        double rrfNormalizationFactor = rrfK + 1d;

        Map<String, Double> scoreByChunkKey = new HashMap<>();
        Map<String, VectorSearchResult> resultByChunkKey = new HashMap<>();

        addRrfScores(vectorChunkResults, vectorWeight, rrfK, scoreByChunkKey, resultByChunkKey);
        addRrfScores(fulltextChunkResults, fulltextWeight, rrfK, scoreByChunkKey, resultByChunkKey);

        return scoreByChunkKey.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .map(entry -> {
                VectorSearchResult source = resultByChunkKey.get(entry.getKey());
                double normalizedSimilarity = Math.min(1d, entry.getValue().doubleValue() * rrfNormalizationFactor);
                return new VectorSearchResult(
                    source.getChunkId(),
                    source.getEntityType(),
                    source.getEntityId(),
                    source.getChunkIndex(),
                    source.getChunkText(),
                    normalizedSimilarity
                );
            })
            .toList();
    }

    private void addRrfScores(List<VectorSearchResult> results, double weight, int rrfK,
                              Map<String, Double> scoreByChunkKey,
                              Map<String, VectorSearchResult> resultByChunkKey) {
        if (results == null || results.isEmpty()) return;

        for (int i = 0; i < results.size(); i++) {
            VectorSearchResult result = results.get(i);
            String chunkKey = getChunkKey(result);
            int rank = i + 1;
            double scoreIncrement = weight / (rrfK + rank);

            Double currentScore = scoreByChunkKey.get(chunkKey);
            if (currentScore == null) {
                scoreByChunkKey.put(chunkKey, scoreIncrement);
            } else {
                scoreByChunkKey.put(chunkKey, currentScore.doubleValue() + scoreIncrement);
            }
            resultByChunkKey.putIfAbsent(chunkKey, result);
        }
    }

    private String getChunkKey(VectorSearchResult result) {
        if (result.getChunkId() != null && result.getChunkId().longValue() > 0) {
            return "id:" + result.getChunkId();
        }
        return result.getEntityType() + ":" + result.getEntityId() + ":" + result.getChunkIndex();
    }

    private List<SemanticSearchResult> aggregateByDocumentBestScore(List<VectorSearchResult> chunkResults) {
        Map<Long, SemanticSearchResult> docMap = new HashMap<>();
        for (VectorSearchResult chunk : chunkResults) {
            if ("document".equalsIgnoreCase(chunk.getEntityType()) == false) continue;

            docMap.compute(chunk.getEntityId(), (id, existing) -> {
                if (existing == null) {
                    return new SemanticSearchResult(chunk.getEntityId(), chunk.getSimilarity());
                }

                Double currentSimilarity = chunk.getSimilarity();
                Double existingSimilarity = existing.getSimilarity();
                if (currentSimilarity != null && (existingSimilarity == null || currentSimilarity.doubleValue() > existingSimilarity.doubleValue())) {
                    existing.setSimilarity(currentSimilarity);
                }
                return existing;
            });
        }

        return docMap.values().stream()
            .sorted(Comparator.comparing(SemanticSearchResult::getSimilarity, Comparator.nullsLast(Double::compareTo)).reversed())
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

    public boolean isAvailable() {
        return vectorStore.isAvailableAndInitialized();
    }
}
