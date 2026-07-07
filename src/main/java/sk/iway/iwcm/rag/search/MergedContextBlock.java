package sk.iway.iwcm.rag.search;

import lombok.Getter;
import lombok.Setter;

/**
 * A merged context block produced by the RAG post-processing pipeline.
 * Represents one or more adjacent chunks from the same entity, merged into a single text block.
 */
@Getter
@Setter
public class MergedContextBlock {

    private Long entityId;
    private String entityType;
    private int startChunkIndex;
    private int endChunkIndex;
    private String text;
    private double maxSimilarity;
    private double averageSimilarity;
    private int sourceChunkCount;
    private String sourceTitle;
    private String sourceUrl;

    /**
     * Creates an empty merged context block for frameworks and manual population.
     */
    public MergedContextBlock() {}

    /**
     * Creates a merged context block without optional source metadata.
     *
     * @param entityId ID of the source entity
     * @param startChunkIndex first chunk index included in the block
     * @param endChunkIndex last chunk index included in the block
     * @param text merged chunk text
     * @param maxSimilarity highest similarity among merged chunks
     * @param averageSimilarity average similarity across merged chunks
     * @param sourceChunkCount number of chunks merged into this block
     */
    public MergedContextBlock(Long entityId, int startChunkIndex, int endChunkIndex,
                              String text, double maxSimilarity, double averageSimilarity,
                              int sourceChunkCount) {
        this(null, entityId, startChunkIndex, endChunkIndex, text, maxSimilarity, averageSimilarity, sourceChunkCount, null, null);
    }

    /**
     * Creates a merged context block with structured retrieval and optional source
     * metadata for the RAG answer prompt.
     *
     * @param entityType type of the source entity
     * @param entityId ID of the source entity
     * @param startChunkIndex first chunk index included in the block
     * @param endChunkIndex last chunk index included in the block
     * @param text merged chunk text
     * @param maxSimilarity highest similarity among merged chunks
     * @param averageSimilarity average similarity across merged chunks
     * @param sourceChunkCount number of chunks merged into this block
     * @param sourceTitle optional source title for future citations
     * @param sourceUrl optional source URL for future citations
     */
    public MergedContextBlock(String entityType, Long entityId, int startChunkIndex, int endChunkIndex,
                              String text, double maxSimilarity, double averageSimilarity,
                              int sourceChunkCount, String sourceTitle, String sourceUrl) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.startChunkIndex = startChunkIndex;
        this.endChunkIndex = endChunkIndex;
        this.text = text;
        this.maxSimilarity = maxSimilarity;
        this.averageSimilarity = averageSimilarity;
        this.sourceChunkCount = sourceChunkCount;
        this.sourceTitle = sourceTitle;
        this.sourceUrl = sourceUrl;
    }
}
