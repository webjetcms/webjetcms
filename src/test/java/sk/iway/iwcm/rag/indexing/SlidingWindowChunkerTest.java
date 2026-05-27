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

        // Verify overlap: end of chunk N should appear at start of chunk N+1
        for (int i = 0; i < chunks.size() - 1; i++) {
            String currentEnd = chunks.get(i).substring(Math.max(0, chunks.get(i).length() - 10));
            String nextStart = chunks.get(i + 1).substring(0, Math.min(10, chunks.get(i + 1).length()));

            assertFalse(chunks.get(i).isEmpty(), "Chunk should not be empty");
            assertEquals(currentEnd, nextStart, "Chunk " + i + " must overlap with chunk " + (i + 1) + " by 10 characters");
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
}
