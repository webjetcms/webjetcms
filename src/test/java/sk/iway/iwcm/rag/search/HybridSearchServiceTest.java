package sk.iway.iwcm.rag.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.doc.DocDetails;

class HybridSearchServiceTest {

    @Test
    void testMergeEmptyResults() {
        List<DocDetails> keyword = new ArrayList<>();
        List<SemanticSearchResult> semantic = new ArrayList<>();

        List<DocDetails> merged = HybridSearchService.mergeResults(keyword, semantic, 10);
        assertTrue(merged.isEmpty());
    }

    @Test
    void testMergeKeywordOnlyResults() {
        List<DocDetails> keyword = createKeywordResults(1, 2, 3);
        List<SemanticSearchResult> semantic = new ArrayList<>();

        List<DocDetails> merged = HybridSearchService.mergeResults(keyword, semantic, 10);
        assertEquals(3, merged.size());
        assertEquals(1, merged.get(0).getDocId());
        assertEquals(2, merged.get(1).getDocId());
        assertEquals(3, merged.get(2).getDocId());
    }

    @Test
    void testMergeSemanticOnlyResults() {
        List<DocDetails> keyword = new ArrayList<>();
        List<SemanticSearchResult> semantic = createSemanticResults(4, 5, 6);

        List<DocDetails> merged = HybridSearchService.mergeResults(keyword, semantic, 10);
        // Semantic-only docs won't have DocDetails in the map, so merged list will be empty
        // (caller is responsible for loading them)
        assertTrue(merged.isEmpty());
    }

    @Test
    void testMergeOverlappingResults() {
        // Doc 2 appears in both - should get higher RRF score
        List<DocDetails> keyword = createKeywordResults(1, 2, 3);
        List<SemanticSearchResult> semantic = createSemanticResults(2, 4, 5);

        List<DocDetails> merged = HybridSearchService.mergeResults(keyword, semantic, 10);

        // Doc 2 should be ranked first because it appears in both lists
        assertFalse(merged.isEmpty());
        assertEquals(2, merged.get(0).getDocId());
    }

    @Test
    void testMergeRespectsMaxResults() {
        List<DocDetails> keyword = createKeywordResults(1, 2, 3, 4, 5);
        List<SemanticSearchResult> semantic = createSemanticResults(6, 7, 8, 9, 10);

        List<DocDetails> merged = HybridSearchService.mergeResults(keyword, semantic, 3);
        assertTrue(merged.size() <= 3);
    }

    @Test
    void testGetSemanticOnlyDocIds() {
        List<DocDetails> keyword = createKeywordResults(1, 2, 3);
        List<SemanticSearchResult> semantic = createSemanticResults(2, 4, 5);

        Set<Integer> semanticOnly = HybridSearchService.getSemanticOnlyDocIds(keyword, semantic);
        assertEquals(2, semanticOnly.size());
        assertTrue(semanticOnly.contains(4));
        assertTrue(semanticOnly.contains(5));
        assertFalse(semanticOnly.contains(2)); // 2 is in both
    }

    @Test
    void testGetSemanticOnlyDocIdsNoOverlap() {
        List<DocDetails> keyword = createKeywordResults(1, 2);
        List<SemanticSearchResult> semantic = createSemanticResults(3, 4);

        Set<Integer> semanticOnly = HybridSearchService.getSemanticOnlyDocIds(keyword, semantic);
        assertEquals(2, semanticOnly.size());
        assertTrue(semanticOnly.contains(3));
        assertTrue(semanticOnly.contains(4));
    }

    @Test
    void testGetSemanticOnlyDocIdsFullOverlap() {
        List<DocDetails> keyword = createKeywordResults(1, 2, 3);
        List<SemanticSearchResult> semantic = createSemanticResults(1, 2, 3);

        Set<Integer> semanticOnly = HybridSearchService.getSemanticOnlyDocIds(keyword, semantic);
        assertTrue(semanticOnly.isEmpty());
    }

    private List<DocDetails> createKeywordResults(int... docIds) {
        List<DocDetails> results = new ArrayList<>();
        for (int id : docIds) {
            DocDetails doc = new DocDetails();
            doc.setDocId(id);
            doc.setTitle("Doc " + id);
            results.add(doc);
        }
        return results;
    }

    private List<SemanticSearchResult> createSemanticResults(int... docIds) {
        List<SemanticSearchResult> results = new ArrayList<>();
        double similarity = 0.95;
        for (int id : docIds) {
            results.add(new SemanticSearchResult((long) id, similarity));
            similarity -= 0.05;
        }
        return results;
    }
}
