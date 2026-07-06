package sk.iway.iwcm.rag.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;

class RagChunkPostProcessorTest {

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
     * Verifies that adjacent sliding-window chunks are merged without duplicated overlap text.
     */
    @Test
    void mergeAdjacentChunksRemovesSlidingWindowOverlap() {
        RagChunkPostProcessor processor = new RagChunkPostProcessor(5, 0.1d, 1, 3, 5000, 2000);

        List<MergedContextBlock> blocks = processor.process(List.of(
            chunk(1L, 0, "Intro text before. Shared overlap phrase for removal", 0.90d),
            chunk(1L, 1, "Shared overlap phrase for removal and unique second.", 0.80d)
        ));

        assertEquals(1, blocks.size());
        String text = blocks.get(0).getText();
        assertTrue(text.contains("Intro text before."));
        assertTrue(text.contains("and unique second."));
        assertEquals(text.indexOf("Shared overlap phrase"), text.lastIndexOf("Shared overlap phrase"));
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
