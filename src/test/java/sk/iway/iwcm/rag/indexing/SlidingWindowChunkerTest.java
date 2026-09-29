package sk.iway.iwcm.rag.indexing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;

/** Verifies bounded chunk sizes, sentence boundaries, overlap, and complete content coverage. */
class SlidingWindowChunkerTest {

    private static final String TEXT = "Alpha is ready. Bravo is ready. Charlie is ready. Delta is ready. Echo is ready.";
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

    /** Sentence boundaries remain intact when overlap is zero, negative, or too small to repeat a sentence. */
    @ParameterizedTest
    @ValueSource(ints = {-10, 0, 1})
    void adjustsChunkSizeToWholeSentences(int overlap) {
        assertEquals(List.of(
            "Alpha is ready. Bravo is ready.",
            "Charlie is ready. Delta is ready.",
            "Echo is ready."
        ), chunker.chunk(TEXT, 31, overlap));
    }

    /** Normal and excessive overlap repeat whole sentences while each chunk advances. */
    @ParameterizedTest
    @ValueSource(ints = {15, 16, 30, 31, 1000})
    void overlapsWholeSentences(int overlap) {
        assertEquals(List.of(
            "Alpha is ready. Bravo is ready.",
            "Bravo is ready. Charlie is ready.",
            "Charlie is ready. Delta is ready.",
            "Delta is ready. Echo is ready."
        ), chunker.chunk(TEXT, 31, overlap));
    }

    /** Sentences, paragraphs, and tokens within the 50% allowance remain intact. */
    @ParameterizedTest
    @ValueSource(strings = {
        "This sentence is much longer than the requested chunk size and must remain complete.",
        "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi omicron",
        "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
    })
    void preservesUnitsWithinAllowance(String text) {
        assertEquals(List.of(text), chunker.chunk(text, 60, 12));
    }

    /** Large unpunctuated passages split at words without losing content or exceeding the default cap. */
    @ParameterizedTest
    @ValueSource(ints = {0, 200, 999})
    void boundsLargePassages(int overlap) {
        String text = IntStream.range(0, 12_000).mapToObj(i -> "word" + i).collect(Collectors.joining(" "));
        assertBoundedCoverage(text, chunker.chunk(text, 1000, overlap), 1500);
    }

    /** Overlap shrinks when keeping a preceding sentence would make the next chunk exceed the cap. */
    @Test
    void boundsChunksIncludingOverlap() {
        String first = "A".repeat(598) + ".";
        String second = "B".repeat(598) + ".";
        String third = "C".repeat(1399) + ".";

        assertEquals(List.of(first + " " + second, third),
            chunker.chunk(first + " " + second + " " + third, 1000, 900));
    }

    /** Tokens longer than the cap are split without dropping characters. */
    @Test
    void splitsOversizedTokens() {
        String text = "a".repeat(3500);
        List<String> chunks = chunker.chunk(text, 1000, 0);

        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 1500));
        assertEquals(text, String.join("", chunks));
    }

    /** A word longer than the target remains intact when its end still fits inside the cap. */
    @ParameterizedTest
    @ValueSource(ints = {1200, 1500})
    void preservesWordsWithinCap(int wordLength) {
        String word = "x".repeat(wordLength);
        String text = word + " tail".repeat(100);
        List<String> chunks = chunker.chunk(text, 1000, 0);

        assertEquals(word, chunks.get(0));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.length() <= 1500));
        assertEquals(text, String.join(" ", chunks));
    }

    /** Hard boundaries never separate the UTF-16 units of a supplementary character, even with a tiny target. */
    @ParameterizedTest
    @CsvSource({"1, 2", "3, 4", "1000, 1500"})
    void preservesSurrogatePairs(int size, int maximumSize) {
        String text = "a😀".repeat(600);
        List<String> chunks = chunker.chunk(text, size, 0);

        assertEquals(text, String.join("", chunks));
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= maximumSize);
            assertTrue(Character.isLowSurrogate(chunk.charAt(0)) == false);
            assertTrue(Character.isHighSurrogate(chunk.charAt(chunk.length() - 1)) == false);
        }
    }

    /** Very long whitespace runs cannot create empty chunks or hide the following sentence. */
    @Test
    void skipsEmptyChunksInLongWhitespace() {
        String text = "First sentence." + " ".repeat(2000) + "Next sentence.";
        assertEquals(List.of("First sentence.", "Next sentence."), chunker.chunk(text, 1000, 0));
    }

    /** A wrapped sentence exceeding the cap is split, and subsequent content is still emitted. */
    @Test
    void continuesAfterAnOversizedSentence() {
        String text = "This sentence is much longer than\nthe requested chunk size and must be split. Next sentence.";
        assertBoundedCoverage(text, chunker.chunk(text, 40, 35), 60);
    }

    /** Headings and paragraphs without sentence punctuation provide safe structural boundaries. */
    @Test
    void splitsAtParagraphBoundaries() {
        assertEquals(List.of("# Heading", "A long line without punctuation", "Final paragraph without punctuation"),
            chunker.chunk("# Heading\n \nA long line without punctuation\n\nFinal paragraph without punctuation", 24, 0));
    }

    /** A soft line wrap never causes a split inside a sentence. */
    @Test
    void keepsWrappedSentencesTogether() {
        String sentence = "This sentence wraps\nonto another line before ending.";
        assertEquals(List.of(sentence, "Next sentence."), chunker.chunk(sentence + " Next sentence.", 42, 5));
    }

    /** Decimal points, colons, and semicolons stay inside sentences; closing quotes and brackets stay with them. */
    @Test
    void handlesSentencePunctuation() {
        List<String> sentences = List.of("It costs 1.25 euros; use this value: exactly.",
            "She asked \"Ready?\"", "The answer was (yes!).", "A final sentence.");
        assertEquals(List.of(sentences.get(0), sentences.get(1) + " " + sentences.get(2), sentences.get(3)),
            chunker.chunk(String.join(" ", sentences), 40, 0));
    }

    /** Checks word boundaries, bounded size, forward progress, and complete coverage of unique source passages. */
    private void assertBoundedCoverage(String text, List<String> chunks, int maximumSize) {
        int previousStart = -1;
        int previousEnd = 0;
        for (String chunk : chunks) {
            assertTrue(chunk.isBlank() == false, "Chunks must contain text");
            assertTrue(chunk.length() <= maximumSize, "Chunks must respect the cap including overlap");
            int start = text.indexOf(chunk, previousStart + 1);
            assertTrue(start > previousStart, "Every chunk must advance in the source");
            int end = start + chunk.length();
            assertTrue(end > previousEnd, "Every chunk must add new content");
            assertTrue(start <= previousEnd || text.substring(previousEnd, start).isBlank(), "No text may be skipped");
            assertTrue(start == 0 || Character.isWhitespace(text.charAt(start - 1)), "Chunk must start at a word boundary");
            assertTrue(end == text.length() || Character.isWhitespace(text.charAt(end)), "Chunk must end at a word boundary");
            previousStart = start;
            previousEnd = end;
        }
        assertEquals(text.length(), previousEnd, "All source content must be emitted");
    }
}
