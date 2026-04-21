package sk.iway.iwcm.rag.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import sk.iway.iwcm.doc.DocDetails;

/**
 * Merges keyword search results with semantic search results using Reciprocal Rank Fusion (RRF).
 *
 * RRF formula: score(doc) = sum( 1 / (k + rank_i) ) for each ranking source i
 * where k is a constant (typically 60) that prevents high-ranked items from dominating.
 */
public class HybridSearchService {

    private static final int RRF_K = 60;

    /**
     * Merge keyword search results with semantic search results using RRF.
     * Documents that appear in both result sets get higher combined scores.
     *
     * @param keywordResults keyword/fulltext search results (ordered by relevance)
     * @param semanticResults semantic search results (ordered by similarity)
     * @param maxResults maximum number of results to return
     * @return merged and re-ranked list of DocDetails
     */
    public static List<DocDetails> mergeResults(List<DocDetails> keywordResults,
                                                 List<SemanticSearchResult> semanticResults,
                                                 int maxResults) {
        Map<Integer, Double> rrfScores = new HashMap<>();
        Map<Integer, DocDetails> docDetailsMap = new HashMap<>();

        // Score keyword results by their position
        for (int rank = 0; rank < keywordResults.size(); rank++) {
            DocDetails doc = keywordResults.get(rank);
            int docId = doc.getDocId();
            double score = 1.0 / (RRF_K + rank + 1);
            rrfScores.merge(docId, score, Double::sum);
            docDetailsMap.put(docId, doc);
        }

        // Score semantic results by their position
        for (int rank = 0; rank < semanticResults.size(); rank++) {
            SemanticSearchResult result = semanticResults.get(rank);
            int docId = result.getDocId().intValue();
            double score = 1.0 / (RRF_K + rank + 1);
            rrfScores.merge(docId, score, Double::sum);
        }

        // Sort by combined RRF score (descending)
        List<Integer> rankedDocIds = rrfScores.entrySet().stream()
            .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
            .limit(maxResults)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Build result list, preserving DocDetails from keyword results where available
        List<DocDetails> mergedResults = new ArrayList<>();
        for (Integer docId : rankedDocIds) {
            DocDetails doc = docDetailsMap.get(docId);
            if (doc != null) {
                mergedResults.add(doc);
            }
            // Documents only in semantic results will need to be loaded separately
            // by the caller (SearchAction) if needed
        }

        return mergedResults;
    }

    /**
     * Get document IDs from semantic results that are NOT in keyword results.
     * These need to be loaded from the database.
     */
    public static Set<Integer> getSemanticOnlyDocIds(List<DocDetails> keywordResults,
                                                      List<SemanticSearchResult> semanticResults) {
        Set<Integer> keywordDocIds = new HashSet<>();
        for (DocDetails doc : keywordResults) {
            keywordDocIds.add(doc.getDocId());
        }

        Set<Integer> semanticOnlyIds = new HashSet<>();
        for (SemanticSearchResult result : semanticResults) {
            int docId = result.getDocId().intValue();
            if (keywordDocIds.contains(docId) == false) {
                semanticOnlyIds.add(docId);
            }
        }

        return semanticOnlyIds;
    }
}
