package sk.iway.iwcm.rag.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;

class RagChunkPostProcessorTest {

    /** Ranking survives merging and truncation; duplicates are skipped only when retrieval filtering remains safe. */
    @Test
    void preservesRankingAndFiltersRepeatedContext() {
        var overview = chunk(1L, 0, "Overview", .81);
        var first = chunk(2L, 0, "Specific answer. Shared details.", .80);
        var next = chunk(2L, 1, "Shared details. New setting.", .78);
        var duplicate = chunk(2L, 2, "Specific answer.", .77);
        overview.setRerankScore(.69);
        first.setRerankScore(.82);
        next.setRerankScore(.80);
        duplicate.setRerankScore(.81);
        var chunks = List.of(overview, first, next, duplicate);
        var processor = new RagChunkPostProcessor(3, .5, 1, 1, 12, 1000);

        assertEquals(List.of(first, next, overview), processor.selectAndFilter(chunks));
        var block = processor.process(chunks).get(0);
        assertEquals(2L, block.getEntityId());
        assertEquals(.82, block.getRerankScore());
        first.setSimilarity(.1);
        assertEquals(List.of(duplicate, next), processor.selectAndFilter(chunks));
    }

    /**
     * Verifies that adaptive filtering keeps the best top-K chunks above threshold.
     */
    @Test
    void selectAndFilterKeepsTopKAboveAdaptiveThreshold() {
        RagChunkPostProcessor processor = new RagChunkPostProcessor(3, 0.5d, 1, 3, 5000, 2000);

        List<VectorSearchResult> filtered = processor.selectAndFilter(List.of(
            chunk(1L, 0, "best", 0.90d),
            chunk(2L, 0, "second", 0.70d),
            chunk(3L, 0, "below adaptive", 0.55d),
            chunk(4L, 0, "top from later input", 0.95d)
        ));

        assertEquals(3, filtered.size());
        assertEquals(4L, filtered.get(0).getEntityId());
        assertEquals(1L, filtered.get(1).getEntityId());
        assertEquals(2L, filtered.get(2).getEntityId());
    }

    /**
     * Verifies that the best available chunk is kept even when all scores are below threshold.
     */
    @Test
    void selectAndFilterKeepsBestResultWhenAllAreBelowThreshold() {
        RagChunkPostProcessor processor = new RagChunkPostProcessor(2, 0.9d, 1, 3, 5000, 2000);

        List<VectorSearchResult> filtered = processor.selectAndFilter(List.of(
            chunk(1L, 0, "weak", 0.20d),
            chunk(2L, 0, "weaker", 0.10d)
        ));

        assertEquals(1, filtered.size());
        assertEquals(1L, filtered.get(0).getEntityId());
    }

    /**
     * Verifies that overlap removal preserves word separators and handles whitespace before and inside the overlap.
     */
    @ParameterizedTest
    @ValueSource(strings = {"Shared overlap phrase for removal", "  Shared overlap phrase for removal", "\nShared  overlap\tphrase for removal"})
    void mergeAdjacentChunksRemovesSlidingWindowOverlap(String overlap) {
        RagChunkPostProcessor processor = new RagChunkPostProcessor(5, 0.1d, 1, 3, 5000, 2000);

        List<MergedContextBlock> blocks = processor.process(List.of(
            chunk(1L, 0, "Intro text before. Shared overlap phrase for removal", 0.90d),
            chunk(1L, 1, overlap + " and unique second.", 0.80d)
        ));

        assertEquals(1, blocks.size());
        assertEquals("Intro text before. Shared overlap phrase for removal and unique second.", blocks.get(0).getText());
    }

    /**
     * Verifies that non-adjacent chunks from the same entity remain separate context blocks.
     */
    @Test
    void processKeepsNonAdjacentChunksInSeparateBlocks() {
        RagChunkPostProcessor processor = new RagChunkPostProcessor(5, 0.1d, 1, 5, 5000, 2000);

        List<MergedContextBlock> blocks = processor.process(List.of(
            chunk(1L, 0, "first", 0.90d),
            chunk(1L, 3, "third", 0.80d)
        ));

        assertEquals(2, blocks.size());
    }

    /**
     * Verifies that block count and total character limits are enforced.
     */
    @Test
    void limitBlocksHonorsBlockAndCharacterLimits() {
        RagChunkPostProcessor processor = new RagChunkPostProcessor(5, 0.1d, 1, 1, 20, 2000);

        List<MergedContextBlock> blocks = processor.process(List.of(
            chunk(1L, 0, "This text is longer than twenty characters.", 0.90d),
            chunk(2L, 0, "This block should be excluded.", 0.80d)
        ));

        assertEquals(1, blocks.size());
        assertTrue(blocks.get(0).getText().length() <= 20);
        assertFalse(blocks.get(0).getText().contains("excluded"));
    }

    /**
     * Verifies that merging starts a new block when the merged block size would exceed the limit.
     */
    @Test
    void mergeSplitsWhenMergedBlockWouldBeTooLarge() {
        RagChunkPostProcessor processor = new RagChunkPostProcessor(5, 0.1d, 1, 5, 5000, 30);

        List<MergedContextBlock> blocks = processor.process(List.of(
            chunk(1L, 0, "First chunk has enough text.", 0.90d),
            chunk(1L, 1, "Second chunk has enough text.", 0.80d)
        ));

        assertEquals(2, blocks.size());
    }

    /**
     * Creates a vector search result fixture with deterministic IDs.
     *
     * @param entityId source entity ID
     * @param chunkIndex chunk index inside the source entity
     * @param text chunk text
     * @param similarity vector similarity score
     * @return vector search result fixture
     */
    private VectorSearchResult chunk(Long entityId, int chunkIndex, String text, double similarity) {
        return new VectorSearchResult(Long.valueOf(entityId * 10 + chunkIndex), "document", entityId, chunkIndex, text, similarity);
    }
}
