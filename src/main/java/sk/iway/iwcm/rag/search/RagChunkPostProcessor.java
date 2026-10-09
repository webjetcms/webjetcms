package sk.iway.iwcm.rag.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;

/**
 * Post-processing pipeline for RAG retrieved chunks.
 *
 * Steps:
 * 1. Select distinct top-K chunks by ranking score, then apply soft minimum similarity filter
 * 2. Sort by entityId, then chunkIndex
 * 3. Group by entity type and entity ID
 * 4. Merge adjacent chunks within each group (removing sliding-window overlap)
 * 5. Limit output to maxBlocks / maxCharacters
 *
 * Local ranking controls order; original similarity controls soft filtering, never as hard truth.
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
     * Configures context ranking, merging, and output limits.
     *
     * @param topK                      number of top chunks considered for context before adaptive thresholding is applied
     * @param minSimilarity             soft similarity threshold; chunks below this are dropped only if enough top chunks remain
     * @param maxChunkGap               maximum gap between chunkIndex values to still merge (1 = adjacent only)
     * @param maxBlocks                 maximum number of merged context blocks to return
     * @param maxCharacters             maximum total characters across all returned blocks
     * @param maxMergedBlockCharacters  maximum characters in a single merged block; prevents unbounded merging
     */
    public RagChunkPostProcessor(int topK, double minSimilarity, int maxChunkGap, int maxBlocks, int maxCharacters, int maxMergedBlockCharacters) {
        this.topK = Math.max(1, topK);
        this.minSimilarity = Math.max(0d, Math.min(1d, minSimilarity));
        this.maxChunkGap = Math.max(0, maxChunkGap);
        this.maxBlocks = Math.max(1, maxBlocks);
        this.maxCharacters = Math.max(1, maxCharacters);
        this.maxMergedBlockCharacters = Math.max(1, maxMergedBlockCharacters);
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

        // 1. Select distinct top-K by ranking score, then apply soft similarity filter
        List<VectorSearchResult> filtered = selectAndFilter(chunks);
        if (filtered.isEmpty()) {
            return List.of();
        }

        // 2. Sort by entityId, then chunkIndex
        List<VectorSearchResult> sorted = sortChunks(filtered);

        // 3. Group by entity type and entity ID
        Map<String, List<VectorSearchResult>> grouped = groupByEntity(sorted);

        // 4. Merge adjacent chunks within each entity group
        List<MergedContextBlock> merged = new ArrayList<>();
        for (List<VectorSearchResult> entityChunks : grouped.values()) {
            merged.addAll(mergeAdjacentChunks(entityChunks));
        }

        // 5. Preserve the selected ranking when enforcing the context budget.
        merged.sort(Comparator.comparingDouble(MergedContextBlock::getRankingScore).reversed());

        return limitBlocks(merged);
    }

    /**
     * Select distinct top-K chunks by ranking score, then apply an adaptive retrieval similarity threshold.
     *
     * Strategy:
     * 1. Sort all chunks by ranking score, skip fully repeated passages from the same source, and take top K.
     * 2. Compute an adaptive threshold based on the number of available chunks:
     *    - Many chunks available → tighter threshold (be selective, less noise)
     *    - Few chunks available → looser threshold (keep more info)
     *    The threshold scales linearly between minSimilarity * 0.6 (for 1 chunk)
     *    and minSimilarity * 1.2 (for topK or more chunks).
     * 3. Never return zero — always keep at least the single best result.
     * Local reranking does not bypass the original similarity threshold.
     *
     * @param chunks raw chunks to rank, possibly null or empty
     * @return top-ranked chunks above the adaptive threshold, retaining the best nonempty candidate
     */
    List<VectorSearchResult> selectAndFilter(List<VectorSearchResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        // Sort by the effective ranking score and take top K.
        List<VectorSearchResult> ranked = new ArrayList<>(chunks);
        ranked.sort(Comparator.comparingDouble(VectorSearchResult::getRankingScore).reversed());

        List<VectorSearchResult> topChunks = new ArrayList<>();
        List<String> selectedTexts = new ArrayList<>();
        for (VectorSearchResult candidate : ranked) {
            String text = normalizeWhitespace(candidate.getChunkText());
            boolean repeated = false;
            for (int i = 0; i < topChunks.size() && text.isEmpty() == false; i++) {
                VectorSearchResult selected = topChunks.get(i);
                if (candidate.getEntityId() != null && java.util.Objects.equals(selected.getEntityId(), candidate.getEntityId())
                        && java.util.Objects.equals(selected.getEntityType(), candidate.getEntityType())
                        && getSimilarity(selected) >= getSimilarity(candidate)
                        && (" " + selectedTexts.get(i) + " ").contains(" " + text + " ")) {
                    repeated = true;
                    break;
                }
            }
            if (repeated) continue;
            topChunks.add(candidate);
            selectedTexts.add(text);
            if (topChunks.size() == topK) break;
        }

        // Adaptive threshold: scale based on how many chunks we have
        double adaptiveThreshold = computeAdaptiveThreshold(topChunks.size());

        // Apply soft similarity: remove chunks below adaptive threshold, but always keep at least 1
        List<VectorSearchResult> aboveThreshold = topChunks.stream()
                .filter(c -> getSimilarity(c) >= adaptiveThreshold)
                .toList();

        if (aboveThreshold.isEmpty()) {
            // All top-K chunks are below threshold — keep the single best result
            return List.of(topChunks.get(0));
        }

        return aboveThreshold;
    }

    /**
     * Computes a soft similarity cutoff that adapts to candidate volume.
     *
     * Rules:
     * - {@code chunkCount <= 1}: use a loose threshold ({@code minSimilarity * 0.6})
     * - {@code chunkCount >= topK}: use a stricter threshold ({@code minSimilarity * 1.2})
     * - otherwise: linearly interpolate between {@code 0.6x} and {@code 1.2x}
     *
     * The final value is clamped to the range {@code [0.0, 1.0]}.
     *
     * @param chunkCount number of top-ranked chunks considered after top-K limiting
     * @return adaptive similarity threshold in range {@code [0.0, 1.0]}
     */
    double computeAdaptiveThreshold(int chunkCount) {
       double threshold;

        if (chunkCount <= 1) {
             threshold = minSimilarity * 0.6;
        } else if (chunkCount >= topK) {
            threshold = minSimilarity * 1.2;
        } else {
            // Linear interpolation between loose (0.6x) and tight (1.2x)
            double ratio = (double) (chunkCount - 1) / (topK - 1);
            double multiplier = 0.6 + ratio * (1.2 - 0.6);
            threshold = minSimilarity * multiplier;
        }

        return Math.max(0d, Math.min(1d, threshold));
    }

    /**
     * Sorts a copy of the chunks by entity ID and then chunk index, treating missing values as zero.
     *
     * @param chunks chunks to sort
     * @return sorted copy of the input list
     */
    List<VectorSearchResult> sortChunks(List<VectorSearchResult> chunks) {
        List<VectorSearchResult> sorted = new ArrayList<>(chunks);
        sorted.sort(Comparator
                .comparingLong((VectorSearchResult c) -> c.getEntityId() != null ? c.getEntityId() : 0L)
                .thenComparingInt(c -> c.getChunkIndex() != null ? c.getChunkIndex() : 0));
        return sorted;
    }

    /**
     * Group chunks by entity type and entity ID preserving insertion order.
     *
     * @param sortedChunks chunks already sorted by entity and chunk index
     * @return ordered map keyed by entity type and entity ID
     */
    Map<String, List<VectorSearchResult>> groupByEntity(List<VectorSearchResult> sortedChunks) {
        Map<String, List<VectorSearchResult>> grouped = new LinkedHashMap<>();
        for (VectorSearchResult chunk : sortedChunks) {
            Long entityId = chunk.getEntityId() != null ? chunk.getEntityId() : 0L;
            String entityType = chunk.getEntityType() != null ? chunk.getEntityType() : "";
            grouped.computeIfAbsent(entityType + ":" + entityId, k -> new ArrayList<>()).add(chunk);
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
        Double rerankScore = first.getRerankScore();
        int count = 1;

        for (int i = 1; i < entityChunks.size(); i++) {
            VectorSearchResult current = entityChunks.get(i);
            int currentIndex = getChunkIndex(current);
            int gap = currentIndex - endIndex;
            String currentText = getChunkText(current);
            String appended = gap <= maxChunkGap ? removeOverlap(textBuilder.toString(), currentText) : currentText;

            if (gap > maxChunkGap || textBuilder.length() + appended.length() > maxMergedBlockCharacters) {
                blocks.add(buildBlock(first, startIndex, endIndex,
                        textBuilder.toString(), maxSim, sumSim, count, rerankScore));

                textBuilder = new StringBuilder(currentText);
                startIndex = currentIndex;
                endIndex = currentIndex;
                maxSim = getSimilarity(current);
                sumSim = maxSim;
                rerankScore = current.getRerankScore();
                count = 1;
            } else {
                textBuilder.append(appended);
                endIndex = currentIndex;
                maxSim = Math.max(maxSim, getSimilarity(current));
                sumSim += getSimilarity(current);
                if (current.getRerankScore() != null && (rerankScore == null || current.getRerankScore() > rerankScore)) {
                    rerankScore = current.getRerankScore();
                }
                count++;
            }
        }

        // Finalize last block
        blocks.add(buildBlock(first, startIndex, endIndex,
                textBuilder.toString(), maxSim, sumSim, count, rerankScore));

        return blocks;
    }

    /**
     * Returns the text to append after removing a matching sliding-window overlap.
     * Whitespace is normalized for comparison; overlaps shorter than 20 normalized characters
     * are ignored. When no overlap is found, the next text is prefixed with a newline.
     *
     * @param existing text already accumulated
     * @param next next chunk text
     * @return unmatched suffix of the next chunk, its newline-prefixed text, or an empty string for null next text
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
     * Maps a trimmed normalized overlap length back to the original text, retaining the following separator.
     *
     * @param originalText unnormalized next-chunk text
     * @param normalizedLength number of normalized characters in the overlap
     * @return original-text offset immediately after the overlap
     */
    private int findOriginalPosition(String originalText, int normalizedLength) {
        int normCount = 0;
        int i = 0;
        boolean lastWasSpace = false;
        while (i < originalText.length() && Character.isWhitespace(originalText.charAt(i))) i++;

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
        return i;
    }

    /**
     * Collapses whitespace sequences to single spaces and trims the result.
     *
     * @param text text to normalize, possibly null
     * @return normalized text, or an empty string for null input
     */
    String normalizeWhitespace(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * Limits context to the configured block and total-character budgets.
     * Only the first block may be truncated when it alone exceeds the character budget.
     *
     * @param blocks ranked context blocks
     * @return blocks that fit the budgets, with a truncated first block when necessary
     */
    List<MergedContextBlock> limitBlocks(List<MergedContextBlock> blocks) {
        List<MergedContextBlock> result = new ArrayList<>();
        int totalChars = 0;

        for (MergedContextBlock block : blocks) {
            if (result.size() >= maxBlocks) break;

            String text = block.getText();
            int textLen = text != null ? text.length() : 0;

            if (totalChars + textLen > maxCharacters) {
                int remaining = maxCharacters - totalChars;
                if (remaining > 0 && result.isEmpty()) {
                    result.add(copyWithText(block, truncateText(text, remaining)));
                }
                break;
            }

            result.add(block);
            totalChars += textLen;
        }

        return result;
    }

    /**
     * Truncates text to the requested maximum length, preferring a nearby
     * whitespace boundary so the returned prompt context does not end mid-word.
     *
     * @param text text to truncate
     * @param maxLength maximum allowed length
     * @return truncated text, or the original text when it already fits
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;

        int end = Math.max(1, maxLength);
        for (int i = end - 1; i > Math.max(0, end - 80); i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                end = i;
                break;
            }
        }

        return text.substring(0, end).trim();
    }

    /**
     * Creates a copy of a merged context block with replacement text while keeping
     * all source metadata and similarity statistics unchanged.
     *
     * @param source original merged context block
     * @param text replacement text
     * @return copied block with the supplied text
     */
    private MergedContextBlock copyWithText(MergedContextBlock source, String text) {
        MergedContextBlock copy = new MergedContextBlock(
            source.getEntityType(),
            source.getEntityId(),
            source.getStartChunkIndex(),
            source.getEndChunkIndex(),
            text,
            source.getMaxSimilarity(),
            source.getAverageSimilarity(),
            source.getSourceChunkCount(),
            source.getSourceTitle(),
            source.getSourceUrl()
        );
        copy.setRerankScore(source.getRerankScore());
        return copy;
    }

    /**
     * Builds a merged context block and computes its average similarity.
     *
     * @param source source chunk supplying entity identity and citation metadata
     * @param startIndex first chunk index included in the block
     * @param endIndex last chunk index included in the block
     * @param text merged chunk text
     * @param maxSim highest similarity among merged chunks
     * @param sumSim sum of similarities among merged chunks
     * @param count number of merged chunks
     * @param rerankScore highest non-null rerank score among merged chunks
     * @return populated merged context block
     */
    private MergedContextBlock buildBlock(VectorSearchResult source, int startIndex, int endIndex,
                                          String text, double maxSim, double sumSim, int count, Double rerankScore) {
        MergedContextBlock block = new MergedContextBlock(
                source.getEntityType(),
                source.getEntityId(),
                startIndex,
                endIndex,
                text,
                maxSim,
                count > 0 ? sumSim / count : 0.0,
                count,
                source.getSourceTitle(),
                source.getSourceUrl()
        );
        block.setRerankScore(rerankScore);
        return block;
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
