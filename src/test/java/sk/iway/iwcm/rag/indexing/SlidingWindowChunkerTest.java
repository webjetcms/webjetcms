package sk.iway.iwcm.rag.indexing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.test.BaseWebjetTest;

class SlidingWindowChunkerTest extends BaseWebjetTest {

    private SlidingWindowChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new SlidingWindowChunker();
    }

    @Test
    void testEmptyText() {
        List<String> chunks = chunker.chunk("");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testNullText() {
        List<String> chunks = chunker.chunk(null);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testShortText() {
        String text = "Short text that fits in one chunk.";
        List<String> chunks = chunker.chunk(text);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    void testChunkingWithOverlap() {
        // Create text longer than chunk size
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Word").append(i).append(" ");
        }
        String text = sb.toString().trim();

        // Use small chunk size for testing
        List<String> chunks = chunker.chunk(text, 50, 10);

        assertTrue(chunks.size() > 1, "Text should be split into multiple chunks");

        // Verify overlap still preserves context across neighboring chunks.
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertFalse(chunks.get(i).isEmpty(), "Chunk should not be empty");
            assertTrue(hasSharedContext(chunks.get(i), chunks.get(i + 1), 6), "Chunk " + i + " must share context with chunk " + (i + 1));
        }
    }

    @Test
    void testAllChunksContainText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("Word").append(i).append(" ");
        }

        List<String> chunks = chunker.chunk(sb.toString().trim(), 100, 20);

        for (String chunk : chunks) {
            assertFalse(chunk.isEmpty(), "Each chunk should contain text");
            assertTrue(chunk.length() <= 100 || chunks.size() == 1, "Chunk should not exceed max size");
        }
    }

    /**
     * Verifies that chunking prefers paragraph breaks when a suitable boundary is available.
     */
    @Test
    void testPrefersParagraphBoundary() {
        String text = "First paragraph has enough words to make it a useful chunk.\n\nSecond paragraph should start a new chunk cleanly.";

        List<String> chunks = chunker.chunk(text, 70, 10);

        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).endsWith("useful chunk."));
        assertTrue(chunks.get(1).contains("Second paragraph"));
    }

    /**
     * Verifies that chunking prefers sentence punctuation before splitting inside a sentence.
     */
    @Test
    void testPrefersSentenceBoundary() {
        String text = "The first sentence has enough words to pass the minimum boundary. The second sentence should not be split in half.";

        List<String> chunks = chunker.chunk(text, 75, 10);

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.get(0).endsWith("."));
    }

    /**
     * Verifies that chunking prefers whitespace before falling back to a hard split.
     */
    @Test
    void testPrefersWhitespaceBoundary() {
        String text = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda";

        List<String> chunks = chunker.chunk(text, 35, 5);

        assertTrue(chunks.size() > 1);
        assertFalse(chunks.get(0).endsWith("eps"), "Chunk should not split a word when whitespace is available");
    }

    /**
     * Verifies that chunking still progresses when a long token has no natural boundary.
     */
    @Test
    void testHardSplitFallbackForLongToken() {
        String text = "abcdefghijklmnopqrstuvwxyz";

        List<String> chunks = chunker.chunk(text, 10, 2);

        assertEquals("abcdefghij", chunks.get(0));
        assertEquals("ijklmnopqr", chunks.get(1));
    }

    /**
     * Checks whether neighboring chunks share at least the requested suffix/prefix context.
     *
     * @param previous previous chunk text
     * @param next next chunk text
     * @param minLength minimum required shared context length
     * @return true when the end of previous appears at the start of next
     */
    private boolean hasSharedContext(String previous, String next, int minLength) {
        int max = Math.min(previous.length(), next.length());
        for (int len = max; len >= minLength; len--) {
            if (next.startsWith(previous.substring(previous.length() - len))) {
                return true;
            }
        }
        return false;
    }
}
