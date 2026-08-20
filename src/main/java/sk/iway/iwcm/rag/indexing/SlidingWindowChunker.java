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

    /**
     * Split text into overlapping chunks.
     * @param text the full text to split
     * @return list of text chunks
     */
    public List<String> chunk(String text) {
        int chunkSize = Constants.getInt("ragEmbeddingChunkSize");
        int overlap = Constants.getInt("ragEmbeddingChunkOverlap");
        return chunk(text, chunkSize, overlap);
    }

    /**
     * Split text into overlapping chunks with specified parameters. Chunk ends prefer
     * paragraph, line, sentence, and whitespace boundaries before falling back to a
     * hard character split.
     *
     * @param text the full text to split
     * @param chunkSize maximum number of characters per chunk
     * @param overlap number of characters to overlap between consecutive chunks
     * @return list of text chunks
     */
    public List<String> chunk(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) return chunks;

        text = text.replace("\r\n", "\n").replace('\r', '\n').trim();

        if (chunkSize <= 0) {
            chunks.add(text);
            return chunks;
        }

        overlap = Math.max(0, Math.min(overlap, chunkSize - 1));

        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        int pos = 0;
        while (pos < text.length()) {
            int maxEnd = Math.min(pos + chunkSize, text.length());
            int end = maxEnd == text.length() ? maxEnd : findBestBoundary(text, pos, maxEnd, chunkSize);
            if (end <= pos) end = maxEnd;

            String chunk = text.substring(pos, end).trim();
            if (chunk.isEmpty() == false) {
                chunks.add(chunk);
            }
            if (end == text.length()) break;

            int nextPos = end - overlap;
            if (nextPos <= pos) {
                nextPos = pos + Math.max(1, chunkSize - overlap);
            }
            pos = Math.min(nextPos, text.length());
        }

        return chunks;
    }

    /**
     * Finds the best natural boundary for a chunk within the current window.
     * Prefers paragraph breaks, then line breaks, sentence punctuation, whitespace,
     * and finally the requested hard limit.
     *
     * @param text normalized source text
     * @param start start index of the current chunk
     * @param maxEnd maximum allowed end index for the current chunk
     * @param chunkSize requested maximum chunk size
     * @return selected end index for the current chunk
     */
    private int findBestBoundary(String text, int start, int maxEnd, int chunkSize) {
        int minBoundary = start + Math.max(1, (int) Math.floor(chunkSize * 0.55d));
        if (minBoundary >= maxEnd) return maxEnd;

        int paragraph = text.lastIndexOf("\n\n", maxEnd);
        if (paragraph >= minBoundary) return paragraph + 2;

        int line = text.lastIndexOf('\n', maxEnd);
        if (line >= minBoundary) return line + 1;

        for (int i = maxEnd - 1; i >= minBoundary; i--) {
            char c = text.charAt(i);
            if (isSentenceBoundary(c) && isBoundaryFollowedByWhitespace(text, i) && isNotDecimalNumber(text, i)) {
                return i + 1;
            }
        }

        for (int i = maxEnd - 1; i >= minBoundary; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }

        return maxEnd;
    }

    /**
     * Checks whether a character can act as a sentence-like boundary.
     *
     * @param c character to inspect
     * @return true when the character is accepted as sentence punctuation
     */
    private boolean isSentenceBoundary(char c) {
        return c == '.' || c == '!' || c == '?' || c == ';' || c == ':';
    }

    /**
     * Verifies that punctuation is followed by whitespace or the end of the text.
     *
     * @param text source text
     * @param boundaryIndex index of the candidate boundary punctuation
     * @return true when the boundary is followed by whitespace or no more text
     */
    private boolean isBoundaryFollowedByWhitespace(String text, int boundaryIndex) {
        int next = boundaryIndex + 1;
        return next >= text.length() || Character.isWhitespace(text.charAt(next));
    }

    /**
     * Prevents decimal numbers from being treated as sentence boundaries.
     *
     * @param text source text
     * @param boundaryIndex index of the candidate period
     * @return true when the period is not between two digits
     */
    private boolean isNotDecimalNumber(String text, int boundaryIndex) {
        if (text.charAt(boundaryIndex) != '.') return true;

        int previous = boundaryIndex - 1;
        int next = boundaryIndex + 1;
        return previous < 0 || next >= text.length() ||
                Character.isDigit(text.charAt(previous)) == false ||
                Character.isDigit(text.charAt(next)) == false;
    }
}
