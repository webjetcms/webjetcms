package sk.iway.iwcm.rag.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;
import sk.iway.iwcm.test.BaseWebjetTest;

class SemanticSearchServiceFilterTest extends BaseWebjetTest {

    @Test
    void filterResultsBySimilarityAddsFallbackWhenThresholdKeepsTooFew() {
        SemanticSearchService service = new SemanticSearchService(null, null, null);

        List<SemanticSearchResult> results = List.of(
            new SemanticSearchResult(1L, 0.90),
            new SemanticSearchResult(2L, 0.80),
            new SemanticSearchResult(3L, 0.60),
            new SemanticSearchResult(4L, 0.50)
        );

        List<SemanticSearchResult> filtered = service.filterResultsBySimilarity(results, 0.2d, 3);

        assertEquals(3, filtered.size());
        assertEquals(1L, filtered.get(0).getDocId());
        assertEquals(2L, filtered.get(1).getDocId());
        assertEquals(3L, filtered.get(2).getDocId());
    }

    @Test
    void filterResultsBySimilarityKeepsAllStrongResults() {
        SemanticSearchService service = new SemanticSearchService(null, null, null);

        List<SemanticSearchResult> results = List.of(
            new SemanticSearchResult(1L, 0.95),
            new SemanticSearchResult(2L, 0.90),
            new SemanticSearchResult(3L, 0.80),
            new SemanticSearchResult(4L, 0.75)
        );

        List<SemanticSearchResult> filtered = service.filterResultsBySimilarity(results, 0.2d, 2);

        assertEquals(4, filtered.size());
        assertEquals(1L, filtered.get(0).getDocId());
        assertEquals(4L, filtered.get(3).getDocId());
    }

    @Test
    void filterResultsBySimilarityUsesConfiguredFloorWhenTopSimilarityIsLow() {
        SemanticSearchService service = new SemanticSearchService(null, null, null);

        List<SemanticSearchResult> results = List.of(
            new SemanticSearchResult(1L, 0.25),
            new SemanticSearchResult(2L, 0.20),
            new SemanticSearchResult(3L, 0.15),
            new SemanticSearchResult(4L, 0.10)
        );

        List<SemanticSearchResult> filtered = service.filterResultsBySimilarity(results, 0.4d, 3);

        assertEquals(3, filtered.size());
        assertEquals(1L, filtered.get(0).getDocId());
        assertEquals(2L, filtered.get(1).getDocId());
        assertEquals(3L, filtered.get(2).getDocId());
    }

    @Test
    void filterResultsBySimilarityReturnsEmptyForEmptyInput() {
        SemanticSearchService service = new SemanticSearchService(null, null, null);

        List<SemanticSearchResult> filtered = service.filterResultsBySimilarity(List.of(), 0.2d, 3);

        assertEquals(0, filtered.size());
    }

    @Test
    void mergeChunkResultsWithRrfBoostsChunksPresentInBothSources() {
        SemanticSearchService service = new SemanticSearchService(null, null, null);

        List<VectorSearchResult> vectorResults = List.of(
            new VectorSearchResult(1L, "document", 10L, 0, "vector-1", 0.90),
            new VectorSearchResult(2L, "document", 20L, 0, "vector-2", 0.80)
        );

        List<VectorSearchResult> fulltextResults = List.of(
            new VectorSearchResult(2L, "document", 20L, 0, "fts-2", 0.95),
            new VectorSearchResult(3L, "document", 30L, 0, "fts-3", 0.70)
        );

        List<VectorSearchResult> merged = service.mergeChunkResultsWithRrf(vectorResults, fulltextResults);

        assertEquals(3, merged.size());
        assertEquals(2L, merged.get(0).getChunkId());
    }
}
