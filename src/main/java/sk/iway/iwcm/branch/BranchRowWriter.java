package sk.iway.iwcm.branch;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Low-level INSERT only; history, authorization, normalization and publication belong to P06. */
public final class BranchRowWriter {
    private BranchRowWriter() { }

    public static void insertDocumentWithId(Connection connection, BranchContext live, Map<String, ?> values) throws SQLException {
        insert(connection,live,BranchType.DOCUMENT,values);
    }

    public static void insertGroupWithId(Connection connection, BranchContext live, Map<String, ?> values) throws SQLException {
        insert(connection,live,BranchType.GROUP,values);
    }

    /** Inserts exactly one media identity and its existing category memberships on the same connection. */
    public static void insertMediaWithId(Connection connection, BranchContext live, Map<String, ?> values, List<Integer> categoryIds) throws SQLException {
        insert(connection,live,BranchType.MEDIA,values);
        for (Integer groupId : BranchFields.normalizedIds(categoryIds)) {
            try (java.sql.PreparedStatement statement=connection.prepareStatement("INSERT INTO media_group_to_media(media_id,media_group_id) VALUES (?,?)")) {
                statement.setInt(1,((Number)values.get("media_id")).intValue()); statement.setInt(2,groupId); statement.executeUpdate();
            }
        }
    }

    private static void insert(Connection connection, BranchContext live, BranchType type, Map<String, ?> values) throws SQLException {
        if (live.mode()!=BranchContext.Mode.LIVE || connection.getAutoCommit())
            throw new IllegalArgumentException("Explicit LIVE context and caller-owned transaction required");
        Object raw=values.get(type.idColumn());
        if (!(raw instanceof Number number) || new java.math.BigDecimal(number.toString()).scale()>0 || number.longValue()<=0 || number.longValue()>Integer.MAX_VALUE)
            throw new IllegalArgumentException("A reserved positive content ID is required");
        Map<String, Object> columns=new LinkedHashMap<>();
        for (String name:BranchFields.forType(type).columns()) {
            if (!values.containsKey(name))throw new IllegalArgumentException("Missing publication field "+name);
            columns.put(name,values.get(name));
        }
        // Duplicate keys deliberately fail. This writer must never silently UPDATE an identity.
        BranchStore.insert(connection,type.liveTable(),columns);
    }
}
