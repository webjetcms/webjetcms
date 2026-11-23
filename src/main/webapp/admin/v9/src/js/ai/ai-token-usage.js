export const emptyTokenUsage = { totalTokens: 0, tokenBreakdown: { inputTokens: 0, outputTokens: 0 } };

/** Sums recorded usage, preserving total-only history when the provider split is unavailable. */
export function sumTokenUsage(previous, next) {
    if (!Number.isSafeInteger(next.totalTokens) || next.totalTokens < 0) return previous;
    const before = previous.tokenBreakdown;
    const after = next.tokenBreakdown;
    if (next.totalTokens === 0 && !after?.inputTokens && !after?.outputTokens) return previous;
    const hasSplit = before && Number.isSafeInteger(after?.inputTokens) && after.inputTokens >= 0
        && Number.isSafeInteger(after?.outputTokens) && after.outputTokens >= 0;
    return {
        totalTokens: previous.totalTokens + next.totalTokens,
        tokenBreakdown: hasSplit ? {
            inputTokens: before.inputTokens + after.inputTokens,
            outputTokens: before.outputTokens + after.outputTokens
        } : null
    };
}
