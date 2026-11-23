package sk.iway.iwcm.rag.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.rag.indexing.MarkdownChunker;
import sk.iway.iwcm.rag.indexing.MarkdownContentExtractor;
import sk.iway.iwcm.rag.indexing.SlidingWindowChunker;
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

    /** Markdown heading prefixes must not consume the context budget by preserving repeated paragraphs. */
    @Test
    void mergesContextualMarkdownWithinBlockAndTotalBudgets() {
        var extractor = new MarkdownContentExtractor();
        var chunker = new MarkdownChunker(extractor, new SlidingWindowChunker());
        String text = "## Settings\n\n" + String.join("\n\n", IntStream.range(0, 5)
            .mapToObj(i -> "Paragraph " + i + ": alpha beta gamma delta.").toList());
        var passages = chunker.chunkWithOffsets(text, 100, 20);
        var inputs = extractor.addHeadingContext(text, "Guide", passages);
        var chunks = new java.util.ArrayList<>(IntStream.range(0, inputs.size())
            .mapToObj(i -> markdownChunk(1L, i, inputs.get(i), .9 - i * .01)).toList());
        String expected = "Guide > Settings\n\n" + text;
        var other = markdownChunk(2L, 0, "Other guide\n\nAdditional useful instructions.", .7);
        chunks.add(other);
        var processor = new RagChunkPostProcessor(10, .1, 1, 2,
            expected.length() + other.getChunkText().length(), expected.length());

        assertTrue(passages.size() >= 3, "The fixture must exercise repeated overlap removal");
        List<MergedContextBlock> blocks = processor.process(chunks);

        assertEquals(2, blocks.size());
        assertEquals(expected, blocks.get(0).getText());
        assertEquals(passages.size(), blocks.get(0).getSourceChunkCount());
        assertEquals(other.getChunkText(), blocks.get(1).getText());
    }

    /** A changed heading context is retained once, without duplicating overlap or stripping code indentation. */
    @Test
    void preservesChangedMarkdownHeadingContext() {
        String overlap = "Shared access instructions for every account.";
        String nextOverlap = "Review permissions before saving the settings.";
        String first = "Guide > Settings\n\nIntroduction.\n\n### Access\n\n" + overlap;
        String next = "Guide > Settings > Access\n\n" + overlap + "\n\n" + nextOverlap;
        String last = "Guide > Settings > Access\n\n" + nextOverlap + "\n\n    saveSettings();";
        var processor = new RagChunkPostProcessor(5, .1, 1, 3, 5000, 2000);

        List<MergedContextBlock> blocks = processor.process(List.of(
            markdownChunk(1L, 0, first, .9), markdownChunk(1L, 1, next, .85), markdownChunk(1L, 2, last, .8)));

        assertEquals(1, blocks.size());
        assertEquals(first + "\n\nGuide > Settings > Access\n\n" + nextOverlap + "\n\n    saveSettings();",
            blocks.get(0).getText());
        assertEquals(3, blocks.get(0).getSourceChunkCount());
    }

    /** Starting a block due to size or an index gap retains its full context and resets subsequent merging. */
    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void retainsMarkdownContextWhenStartingNewBlock(int nextIndex) {
        String overlap = "Shared access instructions for every account.";
        String nextOverlap = "Review permissions before saving the settings.";
        String first = "Guide > Settings\n\n" + "Read the introduction before making changes. ".repeat(3) + overlap;
        String next = "Guide > Settings > Access\n\n" + overlap + "\n\n" + nextOverlap;
        String last = "Guide > Settings > Access\n\n" + nextOverlap + "\n\nSave settings.";
        var processor = new RagChunkPostProcessor(5, .1, 1, 3, 5000, nextIndex == 1 ? first.length() : 2000);

        List<MergedContextBlock> blocks = processor.process(List.of(
            markdownChunk(1L, 0, first, .9), markdownChunk(1L, nextIndex, next, .85),
            markdownChunk(1L, nextIndex + 1, last, .8)));

        assertEquals(2, blocks.size());
        assertEquals(first, blocks.get(0).getText());
        assertEquals(next + "\n\nSave settings.", blocks.get(1).getText());
        assertEquals(nextIndex, blocks.get(1).getStartChunkIndex());
        assertEquals(nextIndex + 1, blocks.get(1).getEndChunkIndex());
    }

    /** Document paragraphs that resemble heading prefixes remain part of their original content. */
    @Test
    void preservesDocumentParagraphsResemblingMarkdownContext() {
        String prefix = "Guide > Settings\n\n";
        String first = prefix + "Read the introduction.";
        String next = prefix + "Save the settings.";
        var processor = new RagChunkPostProcessor(5, .1, 1, 3, 5000, 2000);

        var blocks = processor.process(List.of(chunk(1L, 0, first, .9), chunk(1L, 1, next, .8)));

        assertEquals(first + "\n" + next, blocks.get(0).getText());
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

    private VectorSearchResult markdownChunk(Long entityId, int chunkIndex, String text, double similarity) {
        VectorSearchResult result = chunk(entityId, chunkIndex, text, similarity);
        result.setEntityType("MARKDOWN");
        return result;
    }
}
