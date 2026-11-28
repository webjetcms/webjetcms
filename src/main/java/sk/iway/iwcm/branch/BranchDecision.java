package sk.iway.iwcm.branch;

import java.util.Objects;

/** Pure whole-object B/L/W decision shared by preview and the future publisher. */
public final class BranchDecision {
    private BranchDecision() { }

    public enum Source { LIVE, WORK }
    public record StoredChoice(Source source, String liveHash, String workHash) {
        public StoredChoice { Objects.requireNonNull(source); Objects.requireNonNull(liveHash); Objects.requireNonNull(workHash); }
    }
    public record Result(Source source, boolean conflict, boolean writeRequired) { }

    /** A stored choice is checked before no-op normalization. */
    public static Result resolve(String baseHash, String liveHash, String workHash, StoredChoice choice) {
        Objects.requireNonNull(baseHash);
        Objects.requireNonNull(liveHash);
        Objects.requireNonNull(workHash);
        if (choice != null) {
            if (!liveHash.equals(choice.liveHash()) || !workHash.equals(choice.workHash())) {
                throw new StaleChoiceException();
            }
            if (choice.source() == Source.LIVE || liveHash.equals(workHash)) return new Result(Source.LIVE, false, false);
            return new Result(Source.WORK, false, true);
        }
        if (workHash.equals(baseHash) || liveHash.equals(workHash)) return new Result(Source.LIVE, false, false);
        if (liveHash.equals(baseHash)) return new Result(Source.WORK, false, true);
        return new Result(Source.WORK, true, false);
    }

    /** The caller must map this condition to a conflict response, never a LIVE fallback. */
    public static final class StaleChoiceException extends IllegalStateException {
        public StaleChoiceException() { super("The saved branch choice is stale; compare LIVE and WORK again."); }
    }
}
