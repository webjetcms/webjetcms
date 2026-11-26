package sk.iway.iwcm.branch;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.zaxxer.hikari.HikariDataSource;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;

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
        insertReserved(connection,type,columns);
    }

    static void insertReserved(Connection connection, BranchType type, Map<String, ?> columns) throws SQLException {
        String table = type.liveTable();
        if (type == BranchType.MEDIA || Constants.DB_TYPE != Constants.DB_MSSQL) {
            BranchStore.insert(connection,table,columns);
            return;
        }
        // The cleanup also runs when the response to SET ... ON is lost.
        try (Statement session = connection.createStatement();
             IdentityScope scope = () -> resetIdentity(connection, session, table)) {
            session.execute("SET IDENTITY_INSERT " + table + " ON");
            BranchStore.insert(connection,table,columns);
        }
    }

    @FunctionalInterface
    private interface IdentityScope extends AutoCloseable {
        @Override void close() throws SQLException;
    }

    static void resetIdentity(Connection connection, Statement session, String table) throws SQLException {
        try { session.execute("SET IDENTITY_INSERT " + table + " OFF"); }
        catch (SQLException reset) {
            // A rollback cannot clear this session setting. Evict the checked-out Hikari connection.
            try {
                var pool = DBPool.getInstance().getDataSource("iwcm");
                if (pool.isWrapperFor(HikariDataSource.class)) {
                    pool.unwrap(HikariDataSource.class).evictConnection(connection);
                    // Finish closing the proxy here. Hikari 6.3 may try to reset an already evicted session.
                    try { connection.close(); } catch (Exception close) { reset.addSuppressed(close); }
                } else connection.abort(Runnable::run);
            } catch (Exception eviction) {
                reset.addSuppressed(eviction);
                try { connection.abort(Runnable::run); } catch (Exception abort) { reset.addSuppressed(abort); }
            }
            throw reset;
        }
    }
}
