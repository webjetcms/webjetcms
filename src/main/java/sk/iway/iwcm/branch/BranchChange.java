package sk.iway.iwcm.branch;

import java.util.Objects;

/** A common comparison envelope; each type keeps exactly one authoritative work table. */
public record BranchChange(long branchId, long rowId, BranchType type, String objectKey,
        Operation operation, BranchSnapshot base, BranchSnapshot work, BranchDecision.StoredChoice choice) {
    public enum Operation { UPSERT, DELETE }

    public BranchChange {
        if (branchId <= 0 || rowId <= 0) throw new IllegalArgumentException("Invalid branch row identity");
        Objects.requireNonNull(type);
        Objects.requireNonNull(objectKey);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(base);
        Objects.requireNonNull(work);
        if (type == BranchType.FILE) {
            if (!BranchFilePaths.normalize(objectKey).equals(objectKey))
                throw new IllegalArgumentException("File key must be a normalized logical path");
        } else {
            try {
                int id = Integer.parseInt(objectKey);
                if (id <= 0 || !Integer.toString(id).equals(objectKey)) throw new IllegalArgumentException("Invalid content identity");
            } catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid content identity", e); }
        }
        if (type == BranchType.FILE && operation == Operation.DELETE && "DIRECTORY".equals(base.fields().get("entry_kind"))) throw new IllegalArgumentException("Directory deletion is not supported");
        if (operation == Operation.UPSERT && work.state() != BranchSnapshot.State.ACTIVE) throw new IllegalArgumentException("UPSERT must be active");
        if (operation == Operation.DELETE && work.state() == BranchSnapshot.State.ACTIVE) throw new IllegalArgumentException("DELETE must be absent or trash");
        if (operation == Operation.DELETE && base.state() == BranchSnapshot.State.ABSENT) throw new IllegalArgumentException("Remove a new work row instead of recording DELETE");
    }

    public String identity() { return type.name().toLowerCase(java.util.Locale.ROOT) + ":" + objectKey; }

    public BranchDecision.Result decide(BranchSnapshot live) {
        return BranchDecision.resolve(base.hash(), live.hash(), work.hash(), choice);
    }
}
