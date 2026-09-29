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

/** Splits text into overlapping chunks at sentence or paragraph boundaries. */
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
     * Each chunk adds new content, even when overlap is large. A sentence or unpunctuated
     * paragraph longer than the target stays intact instead of being split inside a word.
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
        List<Integer> boundaries = findBoundaries(text);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int end = 0;
        while (end < boundaries.size() - 1) {
            int startOffset = boundaries.get(start);
            int targetEnd = startOffset + Math.min(chunkSize, text.length() - startOffset);
            end = nearestBoundary(boundaries, targetEnd, end + 1, boundaries.size() - 1);
            int endOffset = boundaries.get(end);
            chunks.add(text.substring(startOffset, endOffset).trim());
            if (end == boundaries.size() - 1) break;
            start = overlap == 0 ? end
                : nearestBoundary(boundaries, Math.max(0, endOffset - overlap), start + 1, end);
        }
        return chunks;
    }

    /**
     * Collects sentence starts with the JDK sentence iterator and adds paragraph breaks
     * for headings and other text without terminal punctuation. Soft line wraps are retained.
     *
     * @param text normalized nonempty text
     * @return ordered unique boundaries, including zero and the end of the text
     */
    private List<Integer> findBoundaries(String text) {
        Set<Integer> boundaries = new TreeSet<>();
        boundaries.add(0);
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.ROOT);
        sentences.setText(text);
        for (int end = sentences.next(); end != BreakIterator.DONE; end = sentences.next()) {
            boundaries.add(skipWhitespace(text, end));
        }
        Matcher paragraphs = PARAGRAPH_BREAK.matcher(text);
        while (paragraphs.find()) boundaries.add(skipWhitespace(text, paragraphs.end()));
        return new ArrayList<>(boundaries);
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
