package sk.iway.iwcm.rag.indexing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import sk.iway.iwcm.Constants;

/**
 * Splits text into overlapping chunks using a sliding window approach.
 * This ensures context is preserved at chunk boundaries for better embedding quality.
 */
@Component
public class SlidingWindowChunker {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP = 100;

    /**
     * Split text into overlapping chunks.
     * @param text the full text to split
     * @return list of text chunks
     */
    public List<String> chunk(String text) {
        int chunkSize = Constants.getInt("ragChunkSize", DEFAULT_CHUNK_SIZE);
        int overlap = Constants.getInt("ragChunkOverlap", DEFAULT_OVERLAP);
        return chunk(text, chunkSize, overlap);
    }

    /**
     * Split text into overlapping chunks with specified parameters.
     * @param text the full text to split
     * @param chunkSize maximum number of characters per chunk
     * @param overlap number of characters to overlap between consecutive chunks
     * @return list of text chunks
     */
    public List<String> chunk(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) return chunks;

        text = text.trim();

        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        int step = chunkSize - overlap;
        if (step <= 0) step = 1;

        int pos = 0;
        while (pos < text.length()) {
            int end = Math.min(pos + chunkSize, text.length());
            String chunk = text.substring(pos, end).trim();
            if (chunk.isEmpty() == false) {
                chunks.add(chunk);
            }
            if (end == text.length()) break;
            pos += step;
        }

        return chunks;
    }
}
