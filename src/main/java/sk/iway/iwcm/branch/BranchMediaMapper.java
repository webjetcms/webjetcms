package sk.iway.iwcm.branch;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/** Media content includes existing category membership, but excludes its visibility cutoff. */
public final class BranchMediaMapper {
    private BranchMediaMapper() { }

    public static BranchSnapshot snapshot(Map<String, ?> row, Object categoryIds) {
        Map<String, Object> values = BranchFields.supportedValues(BranchFields.MEDIA, row);
        values.put("group_ids_snapshot", BranchFields.normalizedIds(categoryIds));
        return new BranchSnapshot(BranchSnapshot.State.ACTIVE, values);
    }

    /** Use only for a changed WORK UPSERT; LIVE/no-op must preserve its original last_update. */
    public static Map<String, Object> project(Map<String, ?> work, Map<String, ?> live,
            Map<String, ?> initial, Instant effectiveOwnerSaveTime) {
        if (effectiveOwnerSaveTime == null) throw new IllegalArgumentException("Missing effective owner save date");
        long millis = Math.floorDiv(effectiveOwnerSaveTime.toEpochMilli(), 1000) * 1000;
        return BranchFields.project(BranchFields.MEDIA, work, live, initial,
            Map.of("last_update", new java.sql.Timestamp(millis)));
    }

    public static Map<String, Object> initialValues(int id, int ownerDocId, int domainId) {
        if (id <= 0 || ownerDocId <= 0 || domainId <= 0) throw new IllegalArgumentException("Invalid new media identity");
        Map<String, Object> values = BranchFields.nullValues(BranchFields.MEDIA);
        values.put("media_id", id);
        values.put("media_fk_id", ownerDocId);
        values.put("media_fk_table_name", "documents");
        values.put("domain_id", domainId);
        values.put("media_sort_order", 10);
        return Collections.unmodifiableMap(values);
    }
}
