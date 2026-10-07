package sk.iway.iwcm.rag.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webjetcms.ai.EmbeddingInputType;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.embedding.EmbeddingBatchResult;
import sk.iway.iwcm.rag.embedding.EmbeddingService;
import sk.iway.iwcm.rag.service.MarkdownIndexService;
import sk.iway.iwcm.rag.service.RagEmbeddingStatService;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.rag.service.RagSettingsService;
import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;
import sk.iway.iwcm.rag.vectorstore.VectorStore;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;
import sk.iway.iwcm.users.UsersDB;

/**
 * Provides shared semantic and hybrid retrieval for document and Markdown embeddings.
 * Document searches can aggregate chunks and generate answers; source-specific callers can
 * retrieve raw chunks for authorization and citation enrichment before generating answers.
 */
@Service
public class SemanticSearchService {

    private static final double ADAPTIVE_THRESHOLD_TOP_RATIO = 0.7d;

    private static final String HYBRID_MODE_ALWAYS = "always";
    private static final String HYBRID_MODE_SHORT_QUERY_ONLY = "short_query_only";
    private static final String HYBRID_MODE_FALLBACK_ON_LOW_VECTOR = "fallback_on_low_vector";

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final RagEmbeddingStatService ragEmbeddingStatService;

    private final RagService ragService;
    private final RerankerService rerankerService;

    @Autowired
    public SemanticSearchService(EmbeddingService embeddingService, VectorStore vectorStore, RagEmbeddingStatService ragEmbeddingStatService,
            RagService ragService, RerankerService rerankerService) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.ragEmbeddingStatService = ragEmbeddingStatService;

        this.ragService = ragService;
        this.rerankerService = rerankerService;
    }

    /**
     * Searches for documents similar to the query text.
     *
     * Returns unique document IDs ordered by their best local ranking score.
     * Depending on component settings, vector results may be merged with full-text results. When answer generation is
     * enabled, the sanitized answer is stored in the request attribute {@code ragAnswer}.
     *
     * @param query search query text
     * @param domainId domain to search in (null for all)
     * @param language language filter (null for all)
     * @param maxResults maximum number of unique documents to return
     * @param entityType entity type to filter by
     * @param request current request with component PageParams and rootGroup attributes
     * @return document IDs with their best retrieval and rerank scores, ordered by ranking score
     */
    public List<SemanticSearchResult> search(String query, Integer domainId, String language, int maxResults, RagEntityType entityType, HttpServletRequest request) {
        if (isAvailable() == false) return List.of();

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

        List<VectorSearchResult> chunkResults = searchChunks(query, domainId, language, maxResults, entityType, bonusParams, request);
        chunkResults = rerankChunks(query, authorizeDocuments(chunkResults, request));
        PageParams pageParams = new PageParams(request);
        double minimumSimilarity = RagSettingsService.getSemanticMinimumSimilarity(pageParams);
        int minimumResultsForCall = Math.min(Math.max(0, RagSettingsService.getSemanticMinimumResults(pageParams)), Math.max(0, maxResults));

        // The answer post-processor applies context merging to authorized, optionally reranked chunks.
        String answer = null;
        if(RagSettingsService.isAnswerAllowed(pageParams)) {
            answer = ragService.answerQuestion(query, domainId, chunkResults, request);
            answer = AllowSafeHtmlAttributeConverter.sanitize(answer);
        }
        request.setAttribute("ragAnswer", Tools.isEmpty(answer) ? null : answer);

        List<SemanticSearchResult> sortedResults = aggregateBySourceBestScore(chunkResults);

        return filterResultsBySimilarity(sortedResults, minimumSimilarity, minimumResultsForCall).stream()
            .limit(maxResults)
            .toList();
    }

    /**
     * Reranks authorized chunks before source aggregation and answer context selection.
     *
     * @param query user question
     * @param chunks authorized retrieval candidates
     * @return all candidates ordered by retrieval score and local text matches
     */
    public List<VectorSearchResult> rerankChunks(String query, List<VectorSearchResult> chunks) {
        return rerankerService.rerank(query, chunks);
    }

    /**
     * Excludes unavailable, internal, and inaccessible pages before their text reaches an AI provider.
     * Adds the current document title to each retained chunk for local reranking and citations.
     *
     * @param chunks retrieved document candidates
     * @param request request identifying the user whose document access is checked
     * @return authorized document chunks in retrieval order
     */
    List<VectorSearchResult> authorizeDocuments(List<VectorSearchResult> chunks, HttpServletRequest request) {
        List<VectorSearchResult> authorized = new ArrayList<>();
        Identity user = UsersDB.getCurrentUser(request);
        for (VectorSearchResult chunk : chunks) {
            if ("document".equalsIgnoreCase(chunk.getEntityType()) == false || chunk.getEntityId() == null) continue;
            DocDetails doc = DocDB.getInstance().getBasicDocDetails(chunk.getEntityId().intValue(), false);
            if (doc == null || doc.isAvailable() == false || doc.isSearchable() == false) continue;
            GroupDetails group = GroupsDB.getInstance().getGroup(doc.getGroupId());
            if ((group != null && group.isInternal()) || DocDB.canAccess(doc, user, true) == false) continue;
            chunk.setSourceTitle(doc.getTitle());
            authorized.add(chunk);
        }
        return authorized;
    }

    /**
     * Retrieves ranked chunks using the shared embedding and optional hybrid-search pipeline.
     * Source-specific callers supply filters before retrieval and enrich or authorize chunks before generating answers.
     *
     * @param query search query text
     * @param domainId requesting domain used for assistants and usage; Markdown storage is shared in domain zero
     * @param language language filter
     * @param maxResults desired document count used to calculate the chunk retrieval limit
     * @param entityType source entity type
     * @param filters additional vector-store filters applied before the result limit
     * @param request current request with optional component settings
     * @return ranked chunks without answer generation or document aggregation
     */
    public List<VectorSearchResult> searchChunks(String query, Integer domainId, String language, int maxResults,
            RagEntityType entityType, Map<String, Object> filters, HttpServletRequest request) {
        boolean markdown = entityType == RagEntityType.MARKDOWN;
        boolean available;
        try (DomainRequestBeanScope ignored = markdown ? DomainRequestBeanScope.open(null) : null) {
            available = isAvailable();
        }
        if (available == false) {
            // If vector store is not available or not initialized, we cannot perform semantic search, return empty results
            Logger.debug(SemanticSearchService.class, "Vector store not available or initialized, returning empty results");
            return List.of();
        }

        AssistantDefinitionEntity embeddingAssistant;
        String provider;
        String model;
        EmbeddingBatchResult embeddingResult;

        try {
            // Shared Markdown vectors still use the requesting domain's assistant and provider credentials.
            embeddingAssistant = markdown
                ? ragEmbeddingStatService.getSearchAssistant(domainId)
                : ragEmbeddingStatService.getSearchAssistant();
            if (embeddingAssistant == null) {
                throw new ProviderCallException("RAG search embedding assistant is not available");
            }
            if (Tools.isEmpty(embeddingAssistant.getProvider())) {
                throw new ProviderCallException("RAG search embedding assistant has no provider configured");
            }
            if (Tools.isEmpty(embeddingAssistant.getModel())) {
                throw new ProviderCallException("RAG search embedding assistant has no model configured");
            }

            provider = embeddingAssistant.getProvider().trim().toLowerCase(Locale.ROOT);
            model = embeddingAssistant.getModel();
            embeddingResult = embeddingService.embedWithUsage(List.of(query), embeddingAssistant, request, EmbeddingInputType.QUERY);
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

        if (markdown) {
            ragEmbeddingStatService.recordSearchTokens(embeddingAssistant, embeddingResult.getUsedTokens(), domainId);
        } else {
            ragEmbeddingStatService.recordSearchTokens(embeddingAssistant, embeddingResult.getUsedTokens());
        }

        float[] queryEmbedding = queryEmbeddings.get(0);
        if (queryEmbedding.length == 0) {
            Logger.error(SemanticSearchService.class, "Failed to generate query embedding");
            return List.of();
        }

        // Markdown retrieval uses shared storage and global settings after embedding in the caller's domain.
        try (DomainRequestBeanScope ignored = markdown ? DomainRequestBeanScope.open(null) : null) {
            return retrieveChunks(query, queryEmbedding, provider, model,
                markdown ? Integer.valueOf(MarkdownIndexService.SHARED_DOMAIN_ID) : domainId,
                language, maxResults, entityType, filters, request);
        }
    }

    /**
     * Retrieves vector matches and optionally combines full-text matches using reciprocal rank fusion.
     *
     * @param query original query text for full-text search and hybrid-mode selection
     * @param queryEmbedding query vector for similarity search
     * @param provider embedding provider identifier
     * @param model embedding model identifier
     * @param domainId storage domain to filter by, or {@code null} for all domains
     * @param language optional language filter
     * @param maxResults requested source count used to derive the chunk limit
     * @param entityType source entity type
     * @param filters optional source-specific filters
     * @param request request providing component settings
     * @return ranked vector or fused chunk results
     */
    private List<VectorSearchResult> retrieveChunks(String query, float[] queryEmbedding, String provider, String model,
            Integer domainId, String language, int maxResults, RagEntityType entityType, Map<String, Object> filters,
            HttpServletRequest request) {
        Map<String, Object> bonusParams = filters == null ? null : new HashMap<>(filters);
        PageParams pageParams = new PageParams(request);

        int minimumResults = RagSettingsService.getSemanticMinimumResults(pageParams);
        int minimumResultsForCall = Math.min(Math.max(0, minimumResults), Math.max(0, maxResults));

        int chunkFetchMultiplier = Math.max(1, RagSettingsService.getHybridChunkFetchMultiplier(pageParams));
        int chunkLimit = Math.max(1, maxResults * chunkFetchMultiplier);

        List<VectorSearchResult> vectorChunkResults = vectorStore.search(queryEmbedding, provider, model, entityType, domainId, language, chunkLimit, bonusParams);

        boolean useHybridSearch = shouldUseHybridSearch(query, vectorChunkResults, minimumResultsForCall, pageParams);
        List<VectorSearchResult> chunkResults = vectorChunkResults;
        if (useHybridSearch) {
            if(bonusParams == null) bonusParams = new HashMap<>();
            bonusParams.put("hybridFtsUseIlikeFallback", RagSettingsService.getHybridFtsUseIlikeFallback(pageParams));

            List<VectorSearchResult> fulltextChunkResults = vectorStore.searchFulltext(query, provider, model, entityType, domainId, language, chunkLimit, bonusParams);
            if (fulltextChunkResults.isEmpty() == false) {
                List<VectorSearchResult> mergedChunkResults = mergeChunkResultsWithRrf(vectorChunkResults, fulltextChunkResults, pageParams);
                if (mergedChunkResults.isEmpty() == false) {
                    chunkResults = mergedChunkResults;
                }
            }
        }

        return chunkResults;
    }

    /**
     * Determines whether the configured hybrid mode requires full-text retrieval for this search.
     *
     * @param query original search query
     * @param vectorChunkResults results returned by vector retrieval
     * @param minimumResultsForCall minimum result count used by fallback mode
     * @param pageParams component settings controlling hybrid search
     * @return {@code true} when full-text retrieval should be combined with vector retrieval
     */
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

    /**
     * Classifies a nonempty query as short when either its character count or term count meets the configured limit.
     *
     * @param query query text, possibly null
     * @param pageParams component settings containing short-query limits
     * @return {@code true} when either configured limit includes the query
     */
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

    /**
     * Merges vector and full-text rankings using weighted reciprocal rank fusion.
     *
     * @param vectorChunkResults ranked vector-search results
     * @param fulltextChunkResults ranked full-text results
     * @param pageParams component settings containing fusion weights and the RRF constant
     * @return fused results sorted by descending normalized RRF score, or an empty list when both inputs are empty
     */
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
                VectorSearchResult merged = new VectorSearchResult(
                    source.getChunkId(),
                    source.getEntityType(),
                    source.getEntityId(),
                    source.getChunkIndex(),
                    source.getChunkText(),
                    normalizedSimilarity
                );
                merged.setSourceTitle(source.getSourceTitle());
                merged.setSourceUrl(source.getSourceUrl());
                return merged;
            })
            .toList();
    }

    /**
     * Accumulates weighted reciprocal-rank contributions for a result list.
     *
     * @param results ranked results whose scores should be added
     * @param weight weight applied to this ranking source
     * @param rrfK reciprocal-rank-fusion constant
     * @param scoreByChunkKey accumulated score by stable chunk key
     * @param resultByChunkKey representative result by stable chunk key
     */
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

    /**
     * Aggregates one source type by entity ID, preserving independent best retrieval and ranking scores.
     *
     * @param chunkResults chunk-level search results
     * @return source-level results sorted by descending best ranking score
     */
    List<SemanticSearchResult> aggregateBySourceBestScore(List<VectorSearchResult> chunkResults) {
        Map<Long, SemanticSearchResult> docMap = new LinkedHashMap<>();
        for (VectorSearchResult chunk : chunkResults) {
            SemanticSearchResult result = docMap.computeIfAbsent(chunk.getEntityId(), id -> new SemanticSearchResult(chunk));
            if (chunk.getSimilarity() != null && (result.getSimilarity() == null || chunk.getSimilarity() > result.getSimilarity())) {
                result.setSimilarity(chunk.getSimilarity());
            }
            if (chunk.getRerankScore() != null && (result.getRerankScore() == null || chunk.getRerankScore() > result.getRerankScore())) {
                result.setRerankScore(chunk.getRerankScore());
            }
        }

        return docMap.values().stream()
            .sorted(Comparator.comparingDouble(SemanticSearchResult::getRankingScore).reversed())
            .toList();
    }

    /**
     * Filters pre-sorted semantic results using absolute and adaptive similarity thresholds.
     *
     * Input order is preserved. Results with a similarity value are retained until the requested minimum count is
     * reached; subsequent results must meet the effective threshold. Results without a similarity value are omitted.
     * Local reranking changes order, while thresholds still use original retrieval similarities.
     *
     * @param sortedResults results ordered by descending ranking score
     * @param minimumSimilarity absolute similarity floor, clamped to the range {@code 0.0}–{@code 1.0}
     * @param minimumResultCount requested minimum number of non-null-similarity results
     * @return filtered results in their original order
     */
    public List<SemanticSearchResult> filterResultsBySimilarity(List<SemanticSearchResult> sortedResults, double minimumSimilarity, int minimumResultCount) {
        if (sortedResults == null || sortedResults.isEmpty()) {
            return List.of();
        }

        int minCount = Math.max(0, minimumResultCount);
        double similarityFloor = Math.max(0d, Math.min(1d, minimumSimilarity));

        double topSimilarity = sortedResults.stream().map(SemanticSearchResult::getSimilarity)
            .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).max().orElse(0d);
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
     * Checks search availability and upgrades the shared schema on first use when necessary.
     * Migration uses global configuration and always restores the requesting domain's context.
     *
     * @return whether search can safely access the current vector schema
     */
    public boolean isAvailable() {
        try {
            if (vectorStore.isAvailableAndInitialized()) return true;
            if (vectorStore.isAvailable() == false) return false;
            try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(null)) {
                return vectorStore.initializeSchema();
            }
        } catch (RuntimeException exception) {
            Logger.error(SemanticSearchService.class, "Cannot initialize vector store for search", exception);
            return false;
        }
    }
}
