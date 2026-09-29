package sk.iway.iwcm.rag.indexing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;

/** Verifies approximate chunk sizes, complete sentence boundaries, overlap, and forward progress. */
class SlidingWindowChunkerTest {

    private static final List<String> SENTENCES = List.of(
        "Alpha is ready.", "Bravo is ready.", "Charlie is ready.", "Delta is ready.", "Echo is ready.");
    private static final String TEXT = String.join(" ", SENTENCES);
    private final SlidingWindowChunker chunker = new SlidingWindowChunker();

    /** Empty inputs never produce empty chunks. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \n\t "})
    void ignoresEmptyInput(String text) {
        assertEquals(List.of(), chunker.chunk(text, 31, 16));
    }

    /** Disabled splitting and a target larger than the text retain the normalized document. */
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1000, Integer.MAX_VALUE})
    void returnsShortOrUnsplitText(int size) {
        assertEquals(List.of("First sentence.\nSecond sentence."),
            chunker.chunk(" \r\nFirst sentence.\r\nSecond sentence.\r\n ", size, 10));
    }

    /** The configured overload uses the same sentence-aware chunking and overlap as explicit parameters. */
    @Test
    void usesConfiguredSizeAndOverlap() {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getInt("ragEmbeddingChunkSize")).thenReturn(31);
            constants.when(() -> Constants.getInt("ragEmbeddingChunkOverlap")).thenReturn(16);

            assertEquals(chunker.chunk(TEXT, 31, 16), chunker.chunk(TEXT));
        }
    }

    /** Chunk ends may fall before or after the target to preserve the nearest complete sentence. */
    @Test
    void adjustsChunkSizeToWholeSentences() {
        assertEquals(List.of(
            "Alpha is ready. Bravo is ready.",
            "Charlie is ready. Delta is ready.",
            "Echo is ready."
        ), chunker.chunk(TEXT, 31, 0));
    }

    /** Overlap repeats complete sentences and starts the next chunk at their beginning. */
    @Test
    void overlapsWholeSentences() {
        assertEquals(List.of(
            "Alpha is ready. Bravo is ready.",
            "Bravo is ready. Charlie is ready.",
            "Charlie is ready. Delta is ready.",
            "Delta is ready. Echo is ready."
        ), chunker.chunk(TEXT, 31, 16));
    }

    /** Long sentences, unpunctuated text, and long tokens remain intact instead of being cut to size. */
    @ParameterizedTest
    @ValueSource(strings = {
        "This sentence is much longer than the requested chunk size and must remain complete.",
        "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda",
        "abcdefghijklmnopqrstuvwxyz"
    })
    void preservesOversizedUnits(String text) {
        assertEquals(List.of(text), chunker.chunk(text, 10, 2));
    }

    /** A single oversized sentence cannot be overlapped forever or prevent following content from being emitted. */
    @Test
    void continuesAfterAnOversizedSentence() {
        String sentence = "This sentence is much longer than the requested chunk size and must remain complete.";
        assertEquals(List.of(sentence, "Next sentence."), chunker.chunk(sentence + " Next sentence.", 10, 9));
    }

    /** Headings and paragraphs without sentence punctuation provide safe structural boundaries. */
    @Test
    void splitsAtParagraphBoundaries() {
        assertEquals(List.of("# Heading", "A long line without punctuation", "Final paragraph without punctuation"),
            chunker.chunk("# Heading\n \nA long line without punctuation\n\nFinal paragraph without punctuation", 12, 0));
    }

    /** A soft line wrap never causes a split inside a sentence. */
    @Test
    void keepsWrappedSentencesTogether() {
        String sentence = "This sentence wraps\nonto another line before ending.";
        assertEquals(List.of(sentence, "Next sentence."), chunker.chunk(sentence + " Next sentence.", 15, 5));
    }

    /** Decimal points, colons, and semicolons stay inside sentences; closing quotes and brackets stay with them. */
    @Test
    void handlesSentencePunctuation() {
        List<String> sentences = List.of("It costs 1.25 euros; use this value: exactly.",
            "She asked \"Ready?\"", "The answer was (yes!).", "A final sentence.");
        assertEquals(sentences, chunker.chunk(String.join(" ", sentences), 1, 0));
    }

    /** Negative or excessive overlap cannot create gaps, partial sentences, or chunks containing only old content. */
    @ParameterizedTest
    @ValueSource(ints = {-10, 0, 1, 15, 30, 31, 1000})
    void preservesAllContentAndProgresses(int overlap) {
        List<String> chunks = chunker.chunk(TEXT, 31, overlap);
        int previousStart = -1;
        int previousEnd = 0;
        for (String chunk : chunks) {
            int start = TEXT.indexOf(chunk);
            int end = start + chunk.length();
            assertTrue(start > previousStart, "Every chunk must start later than the previous chunk");
            assertTrue(end > previousEnd, "Every chunk must add new content");
            assertTrue(start <= previousEnd || TEXT.substring(previousEnd, start).isBlank(), "No text may be skipped");
            assertTrue(SENTENCES.stream().anyMatch(chunk::startsWith), "Chunk must start with a complete sentence");
            assertTrue(SENTENCES.stream().anyMatch(chunk::endsWith), "Chunk must end with a complete sentence");
            previousStart = start;
            previousEnd = end;
        }
        assertEquals(TEXT.length(), previousEnd);
    }
}
