package sk.iway.iwcm.branch;

import java.util.Objects;

/** Immutable, server-verified context of a single content operation. */
public record BranchContext(long branchId, String domainName, int userId, Mode mode) {
    public enum Mode { LIVE, ADMIN, PREVIEW }

    public BranchContext {
        Objects.requireNonNull(domainName, "domainName");
        Objects.requireNonNull(mode, "mode");
        if (branchId < 0 || (mode == Mode.LIVE) != (branchId == 0))
            throw new IllegalArgumentException("LIVE requires branchId 0; branch modes require a positive ID");
        if (branchId > 0 && (domainName.isBlank() || userId <= 0))
            throw new IllegalArgumentException("A branch context requires a domain and authenticated user");
    }

    public static BranchContext live(String domainName, int userId) {
        return new BranchContext(0, domainName, userId, Mode.LIVE);
    }

    /** Returns the standard scope used by every branch storage operation. */
    public int domainId() {
        return BranchDomains.id(domainName);
    }

    public boolean isBranch() {
        return branchId > 0;
    }
}
