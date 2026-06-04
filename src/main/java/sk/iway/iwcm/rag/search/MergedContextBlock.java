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
    private int startChunkIndex;
    private int endChunkIndex;
    private String text;
    private double maxSimilarity;
    private double averageSimilarity;
    private int sourceChunkCount;

    public MergedContextBlock() {}

    public MergedContextBlock(Long entityId, int startChunkIndex, int endChunkIndex,
                              String text, double maxSimilarity, double averageSimilarity,
                              int sourceChunkCount) {
        this.entityId = entityId;
        this.startChunkIndex = startChunkIndex;
        this.endChunkIndex = endChunkIndex;
        this.text = text;
        this.maxSimilarity = maxSimilarity;
        this.averageSimilarity = averageSimilarity;
        this.sourceChunkCount = sourceChunkCount;
    }
}
