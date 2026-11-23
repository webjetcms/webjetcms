package sk.iway.iwcm.components.ai.dto;

import com.webjetcms.ai.TokenUsage;

/** Provider-reported input and output counts. Older transcripts may contain only a total. */
public record TokenBreakdown(long inputTokens, long outputTokens) {
    public static final TokenBreakdown ZERO = new TokenBreakdown(0, 0);

    /** A total-only round makes the split unavailable for the whole conversation. */
    public static TokenBreakdown add(TokenBreakdown previous, TokenUsage usage) {
        if (previous == null || (usage.totalTokens() > 0 && usage.inputTokens() == 0 && usage.outputTokens() == 0)) return null;
        return new TokenBreakdown(previous.inputTokens + usage.inputTokens(), previous.outputTokens + usage.outputTokens());
    }
}
