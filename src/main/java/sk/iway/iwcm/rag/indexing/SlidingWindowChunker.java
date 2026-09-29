package sk.iway.iwcm.rag.indexing;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import sk.iway.iwcm.Constants;

/** Splits text into bounded, overlapping chunks, preferring sentence or paragraph boundaries. */
@Component
public class SlidingWindowChunker {

    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\n[ \\t]*\\n");

    /**
     * Splits text using the configured approximate chunk size and overlap.
     *
     * @param text the full text to split
     * @return list of text chunks
     */
    public List<String> chunk(String text) {
        int chunkSize = Constants.getInt("ragEmbeddingChunkSize");
        int overlap = Constants.getInt("ragEmbeddingChunkOverlap");
        return chunk(text, chunkSize, overlap);
    }

    /**
     * Chooses sentence or paragraph boundaries nearest the requested size and overlap.
     * Each chunk adds new content, even when overlap is large. Chunks may exceed the target
     * by up to 50%, with a minimum cap of two UTF-16 units to preserve surrogate pairs.
     * Longer units split at whitespace, or inside an oversized token when necessary.
     *
     * @param text the full text to split
     * @param chunkSize target number of characters per chunk; nonpositive values disable splitting
     * @param overlap target overlap in characters, rounded to whole sentences or paragraphs
     * @return nonempty chunks with approximate sizes and no gaps in the source content
     */
    public List<String> chunk(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        text = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (chunkSize <= 0 || text.length() <= chunkSize) return new ArrayList<>(List.of(text));

        overlap = Math.max(0, Math.min(overlap, chunkSize - 1));
        int maximumChunkSize = (int) Math.min(Integer.MAX_VALUE, Math.max(2L, chunkSize + (long) chunkSize / 2));
        List<Integer> boundaries = findBoundaries(text, chunkSize, maximumChunkSize);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int end = 0;
        while (end < boundaries.size() - 1) {
            while (boundaries.get(end + 1) - boundaries.get(start) > maximumChunkSize) start++;
            int startOffset = boundaries.get(start);
            int targetEnd = startOffset + Math.min(chunkSize, text.length() - startOffset);
            int maximumEnd = startOffset + Math.min(maximumChunkSize, text.length() - startOffset);
            int maximumEndIndex = Collections.binarySearch(boundaries, maximumEnd);
            if (maximumEndIndex < 0) maximumEndIndex = -maximumEndIndex - 2;
            end = nearestBoundary(boundaries, targetEnd, end + 1, maximumEndIndex);
            int endOffset = boundaries.get(end);
            String chunk = text.substring(startOffset, endOffset).trim();
            if (chunk.isEmpty() == false) chunks.add(chunk);
            if (end == boundaries.size() - 1) break;
            start = overlap == 0 ? end
                : nearestBoundary(boundaries, Math.max(0, endOffset - overlap), start + 1, end);
        }
        return chunks;
    }

    /**
     * Collects sentence starts with the JDK sentence iterator and adds paragraph breaks
     * for headings and other text without terminal punctuation. Oversized gaps receive
     * whitespace or hard boundaries so every unit can fit within the chunk cap.
     *
     * @param text normalized nonempty text
     * @param chunkSize target size used when splitting oversized units
     * @param maximumChunkSize maximum size of a chunk, including overlap
     * @return ordered unique boundaries, including zero and the end of the text
     */
    private List<Integer> findBoundaries(String text, int chunkSize, int maximumChunkSize) {
        Set<Integer> boundaries = new TreeSet<>();
        boundaries.add(0);
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.ROOT);
        sentences.setText(text);
        for (int end = sentences.next(); end != BreakIterator.DONE; end = sentences.next()) {
            boundaries.add(skipWhitespace(text, end));
        }
        Matcher paragraphs = PARAGRAPH_BREAK.matcher(text);
        while (paragraphs.find()) boundaries.add(skipWhitespace(text, paragraphs.end()));

        List<Integer> bounded = new ArrayList<>();
        int previous = 0;
        for (int boundary : boundaries) {
            while (boundary - previous > maximumChunkSize) {
                previous = splitOversizedUnit(text, previous, previous + Math.max(2, chunkSize), previous + maximumChunkSize);
                bounded.add(previous);
            }
            bounded.add(boundary);
            previous = boundary;
        }
        return bounded;
    }

    /** Prefers whitespace near the target within the cap, otherwise splitting without breaking a surrogate pair. */
    private int splitOversizedUnit(String text, int start, int target, int maximum) {
        for (int offset = target - 1; offset >= start; offset--) {
            if (Character.isWhitespace(text.charAt(offset))) return offset + 1;
        }
        for (int offset = target; offset <= maximum; offset++) {
            if (Character.isWhitespace(text.charAt(offset))) return Math.min(offset + 1, maximum);
        }
        if (Character.isHighSurrogate(text.charAt(target - 1)) && Character.isLowSurrogate(text.charAt(target))) return target - 1;
        return target;
    }

    private int skipWhitespace(String text, int offset) {
        while (offset < text.length() && Character.isWhitespace(text.charAt(offset))) offset++;
        return offset;
    }

    /**
     * Finds the closest allowed boundary, preferring the earlier boundary on a tie.
     * Index bounds ensure that successive chunks advance without dropping source content.
     *
     * @param boundaries ordered character offsets
     * @param target desired character offset
     * @param minimum first allowed boundary index
     * @param maximum last allowed boundary index
     * @return index of the nearest allowed boundary
     */
    private int nearestBoundary(List<Integer> boundaries, int target, int minimum, int maximum) {
        int match = Collections.binarySearch(boundaries, target);
        if (match >= 0) return Math.max(minimum, Math.min(match, maximum));
        int next = Math.max(minimum, Math.min(-match - 1, maximum));
        int previous = Math.max(minimum, next - 1);
        return Math.abs(boundaries.get(previous) - target) <= Math.abs(boundaries.get(next) - target) ? previous : next;
    }
}
