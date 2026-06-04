package sk.iway.iwcm.rag.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;

/**
 * Post-processing pipeline for RAG retrieved chunks.
 *
 * Steps:
 * 1. Select top-K chunks by similarity, then apply soft minimum similarity filter
 * 2. Sort by entityId, then chunkIndex
 * 3. Group by entityId
 * 4. Merge adjacent chunks within each group (removing sliding-window overlap)
 * 5. Limit output to maxBlocks / maxCharacters
 *
 * Similarity is used for ranking and soft filtering, never as hard truth.
 * The pipeline never returns zero chunks when decent top results exist.
 */
public class RagChunkPostProcessor {

    private final int topK;
    private final double minSimilarity;
    private final int maxChunkGap;
    private final int maxBlocks;
    private final int maxCharacters;
    private final int maxMergedBlockCharacters;

    /**
     * @param topK                      number of top chunks to always keep regardless of similarity
     * @param minSimilarity             soft similarity threshold; chunks below this are dropped only if enough top-K chunks remain
     * @param maxChunkGap               maximum gap between chunkIndex values to still merge (1 = adjacent only)
     * @param maxBlocks                 maximum number of merged context blocks to return
     * @param maxCharacters             maximum total characters across all returned blocks
     * @param maxMergedBlockCharacters  maximum characters in a single merged block; prevents unbounded merging
     */
    public RagChunkPostProcessor(int topK, double minSimilarity, int maxChunkGap, int maxBlocks, int maxCharacters, int maxMergedBlockCharacters) {
        this.topK = topK;
        this.minSimilarity = minSimilarity;
        this.maxChunkGap = maxChunkGap;
        this.maxBlocks = maxBlocks;
        this.maxCharacters = maxCharacters;
        this.maxMergedBlockCharacters = maxMergedBlockCharacters;
    }

    /**
     * Run the full post-processing pipeline on retrieved chunks.
     *
     * @param chunks raw vector search results
     * @return merged, filtered, and limited context blocks ready for the LLM prompt
     */
    public List<MergedContextBlock> process(List<VectorSearchResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        // 1. Select top-K by similarity, then apply soft similarity filter
        List<VectorSearchResult> filtered = selectAndFilter(chunks);
        if (filtered.isEmpty()) {
            return List.of();
        }

        // 2. Sort by entityId, then chunkIndex
        List<VectorSearchResult> sorted = sortChunks(filtered);

        // 3. Group by entityId
        Map<Long, List<VectorSearchResult>> grouped = groupByEntityId(sorted);

        // 4. Merge adjacent chunks within each entity group
        List<MergedContextBlock> merged = new ArrayList<>();
        for (List<VectorSearchResult> entityChunks : grouped.values()) {
            merged.addAll(mergeAdjacentChunks(entityChunks));
        }

        // 5. Sort by maxSimilarity descending, then limit
        merged.sort(Comparator.comparingDouble(MergedContextBlock::getMaxSimilarity).reversed());

        return limitBlocks(merged);
    }

    /**
     * Select top-K chunks by similarity first, then apply an adaptive similarity threshold.
     *
     * Strategy:
     * 1. Sort all chunks by similarity descending and take top K.
     * 2. Compute an adaptive threshold based on the number of available chunks:
     *    - Many chunks available → tighter threshold (be selective, less noise)
     *    - Few chunks available → looser threshold (keep more info)
     *    The threshold scales linearly between minSimilarity * 0.6 (for 1 chunk)
     *    and minSimilarity * 1.2 (for topK or more chunks).
     * 3. Never return zero — always keep at least the single best result.
     */
    List<VectorSearchResult> selectAndFilter(List<VectorSearchResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        // Sort by similarity descending and take top K
        List<VectorSearchResult> ranked = new ArrayList<>(chunks);
        ranked.sort(Comparator.comparingDouble((VectorSearchResult c) -> getSimilarity(c)).reversed());

        int limit = Math.min(topK, ranked.size());
        List<VectorSearchResult> topChunks = new ArrayList<>(ranked.subList(0, limit));

        // Adaptive threshold: scale based on how many chunks we have
        double adaptiveThreshold = computeAdaptiveThreshold(topChunks.size());

        // Apply soft similarity: remove chunks below adaptive threshold, but always keep at least 1
        List<VectorSearchResult> aboveThreshold = topChunks.stream()
                .filter(c -> getSimilarity(c) >= adaptiveThreshold)
                .collect(Collectors.toList());

        if (aboveThreshold.isEmpty()) {
            // All top-K chunks are below threshold — keep the single best result
            return List.of(topChunks.get(0));
        }

        return aboveThreshold;
    }

    /**
     * Compute an adaptive similarity threshold based on the number of available chunks.
     *
     * Few chunks (1-2): threshold = minSimilarity * 0.6 (loose, keep more context)
     * Many chunks (topK): threshold = minSimilarity * 1.2 (tight, be selective)
     * In between: linear interpolation
     */
    double computeAdaptiveThreshold(int chunkCount) {
        if (chunkCount <= 1) {
            return minSimilarity * 0.6;
        }
        if (chunkCount >= topK) {
            return minSimilarity * 1.2;
        }
        // Linear interpolation between loose (0.6x) and tight (1.2x)
        double ratio = (double) (chunkCount - 1) / (topK - 1);
        double multiplier = 0.6 + ratio * (1.2 - 0.6);
        return minSimilarity * multiplier;
    }

    /**
     * Sort chunks by entityId ascending, then chunkIndex ascending.
     */
    List<VectorSearchResult> sortChunks(List<VectorSearchResult> chunks) {
        List<VectorSearchResult> sorted = new ArrayList<>(chunks);
        sorted.sort(Comparator
                .comparingLong((VectorSearchResult c) -> c.getEntityId() != null ? c.getEntityId() : 0L)
                .thenComparingInt(c -> c.getChunkIndex() != null ? c.getChunkIndex() : 0));
        return sorted;
    }

    /**
     * Group chunks by entityId preserving insertion order (already sorted).
     */
    Map<Long, List<VectorSearchResult>> groupByEntityId(List<VectorSearchResult> sortedChunks) {
        Map<Long, List<VectorSearchResult>> grouped = new LinkedHashMap<>();
        for (VectorSearchResult chunk : sortedChunks) {
            Long entityId = chunk.getEntityId() != null ? chunk.getEntityId() : 0L;
            grouped.computeIfAbsent(entityId, k -> new ArrayList<>()).add(chunk);
        }
        return grouped;
    }

    /**
     * Merge adjacent chunks (within maxChunkGap) from the same entity.
     * Removes sliding-window overlap when appending chunk text.
     *
     * @param entityChunks chunks from a single entity, sorted by chunkIndex
     * @return list of merged context blocks
     */
    List<MergedContextBlock> mergeAdjacentChunks(List<VectorSearchResult> entityChunks) {
        if (entityChunks == null || entityChunks.isEmpty()) {
            return List.of();
        }

        List<MergedContextBlock> blocks = new ArrayList<>();

        VectorSearchResult first = entityChunks.get(0);
        StringBuilder textBuilder = new StringBuilder(getChunkText(first));
        int startIndex = getChunkIndex(first);
        int endIndex = startIndex;
        double maxSim = getSimilarity(first);
        double sumSim = maxSim;
        int count = 1;

        for (int i = 1; i < entityChunks.size(); i++) {
            VectorSearchResult current = entityChunks.get(i);
            int currentIndex = getChunkIndex(current);
            int gap = currentIndex - endIndex;

            if (gap <= maxChunkGap) {
                // Merge: append with overlap removal, but respect per-block size limit
                String currentText = getChunkText(current);
                String merged = textBuilder.toString();
                String appended = removeOverlap(merged, currentText);

                if (textBuilder.length() + appended.length() > maxMergedBlockCharacters) {
                    // Block would exceed size limit: finalize current block and start a new one
                    blocks.add(buildBlock(first.getEntityId(), startIndex, endIndex,
                            textBuilder.toString(), maxSim, sumSim, count));

                    textBuilder = new StringBuilder(currentText);
                    startIndex = currentIndex;
                    endIndex = currentIndex;
                    maxSim = getSimilarity(current);
                    sumSim = maxSim;
                    count = 1;
                } else {
                    textBuilder.append(appended);
                    endIndex = currentIndex;
                    double sim = getSimilarity(current);
                    maxSim = Math.max(maxSim, sim);
                    sumSim += sim;
                    count++;
                }
            } else {
                // Gap too large: finalize current block and start a new one
                blocks.add(buildBlock(first.getEntityId(), startIndex, endIndex,
                        textBuilder.toString(), maxSim, sumSim, count));

                textBuilder = new StringBuilder(getChunkText(current));
                startIndex = currentIndex;
                endIndex = currentIndex;
                maxSim = getSimilarity(current);
                sumSim = maxSim;
                count = 1;
            }
        }

        // Finalize last block
        blocks.add(buildBlock(first.getEntityId(), startIndex, endIndex,
                textBuilder.toString(), maxSim, sumSim, count));

        return blocks;
    }

    /**
     * Remove overlapping text between the end of 'existing' and the beginning of 'next'.
     * Sliding-window chunking produces overlap, so the tail of 'existing' may match
     * the head of 'next'. This method finds and removes that duplication.
     *
     * Falls back to newline-separated concatenation if no overlap is found.
     */
    String removeOverlap(String existing, String next) {
        if (existing == null || existing.isEmpty() || next == null || next.isEmpty()) {
            return next != null ? next : "";
        }

        // Normalize whitespace for comparison while preserving the original next text
        String existingNorm = normalizeWhitespace(existing);
        String nextNorm = normalizeWhitespace(next);

        // Try to find the longest suffix of existing that matches a prefix of next.
        // We limit the search window to avoid O(n^2) on very long texts.
        int maxSearchLen = Math.min(existingNorm.length(), nextNorm.length());

        int bestOverlap = 0;
        for (int len = maxSearchLen; len >= 20; len--) {
            String suffix = existingNorm.substring(existingNorm.length() - len);
            if (nextNorm.startsWith(suffix)) {
                bestOverlap = len;
                break;
            }
        }

        if (bestOverlap > 0) {
            // Find the corresponding position in the original (non-normalized) next text.
            // We map the normalized overlap length back to the original text.
            int origPos = findOriginalPosition(next, bestOverlap);
            return next.substring(origPos);
        }

        // No overlap found, concatenate with newline
        return "\n" + next;
    }

    /**
     * Find the position in originalText that corresponds to 'normalizedLength' characters
     * of normalized text from the beginning.
     */
    private int findOriginalPosition(String originalText, int normalizedLength) {
        int normCount = 0;
        int i = 0;
        boolean lastWasSpace = false;

        while (i < originalText.length() && normCount < normalizedLength) {
            char c = originalText.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    normCount++;
                    lastWasSpace = true;
                }
            } else {
                normCount++;
                lastWasSpace = false;
            }
            i++;
        }
        // Skip any remaining whitespace after the overlap
        while (i < originalText.length() && Character.isWhitespace(originalText.charAt(i))) {
            i++;
        }
        return i;
    }

    /**
     * Collapse all whitespace sequences into a single space and trim.
     */
    String normalizeWhitespace(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * Limit the result to maxBlocks and maxCharacters.
     */
    List<MergedContextBlock> limitBlocks(List<MergedContextBlock> blocks) {
        List<MergedContextBlock> result = new ArrayList<>();
        int totalChars = 0;

        for (MergedContextBlock block : blocks) {
            if (result.size() >= maxBlocks) break;

            String text = block.getText();
            int textLen = text != null ? text.length() : 0;

            if (totalChars + textLen > maxCharacters && !result.isEmpty()) {
                // Try to include a truncated version if this is the first block that exceeds the limit
                break;
            }

            result.add(block);
            totalChars += textLen;
        }

        return result;
    }

    private MergedContextBlock buildBlock(Long entityId, int startIndex, int endIndex,
                                          String text, double maxSim, double sumSim, int count) {
        return new MergedContextBlock(
                entityId,
                startIndex,
                endIndex,
                text,
                maxSim,
                count > 0 ? sumSim / count : 0.0,
                count
        );
    }

    private String getChunkText(VectorSearchResult chunk) {
        return chunk.getChunkText() != null ? chunk.getChunkText() : "";
    }

    private int getChunkIndex(VectorSearchResult chunk) {
        return chunk.getChunkIndex() != null ? chunk.getChunkIndex() : 0;
    }

    private double getSimilarity(VectorSearchResult chunk) {
        return chunk.getSimilarity() != null ? chunk.getSimilarity() : 0.0;
    }
}
