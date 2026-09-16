package sk.iway.iwcm.stat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies idempotent event inserts without changing legacy buffer behavior. */
class StatWriteBufferHeatMapTest extends BaseWebjetTest {
    @Test
    void duplicateClickDoesNotPreventTheFollowingClickFromBeingWritten() throws Exception {
        int originalType = Constants.DB_TYPE;
        Constants.DB_TYPE = Constants.DB_PGSQL;
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenThrow(new SQLException("duplicate event", "23505")).thenReturn(1);
        UpdateInsertSqlPair sql = new UpdateInsertSqlPair("INSERT INTO stat_clicks_v2_2026_8 (event_id) VALUES (?)", "_2026_8", true);
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            assertTrue(save(sql, List.of(new Object[] { "duplicate" }, new Object[] { "next" })));
            verify(statement, times(2)).executeUpdate();
            verify(statement).setObject(1, "next");
        } finally {
            Constants.DB_TYPE = originalType;
        }
    }

    @Test
    void legacyInsertDoesNotSilentlyIgnoreConstraintErrors() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenThrow(new SQLException("duplicate event", "23505"));
        UpdateInsertSqlPair sql = new UpdateInsertSqlPair(null, "INSERT INTO stat_clicks_v2_2026_8 (event_id) VALUES (?)");
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class); MockedStatic<StatNewDB> tables = mockStatic(StatNewDB.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            assertFalse(save(sql, List.of(new Object[] { "duplicate" }, new Object[] { "next" })));
            verify(statement).executeUpdate();
            tables.verify(() -> StatNewDB.createStatTablesFromError("duplicate event", null, "stat_clicks_v2"));
        }
    }

    @Test
    void missingPartitionRetryUsesTheEventsMonth() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("table does not exist", "42P01"));
        UpdateInsertSqlPair sql = new UpdateInsertSqlPair("INSERT INTO stat_clicks_v2_2025_12 (event_id) VALUES (?)", "_2025_12", true);
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class); MockedStatic<StatNewDB> tables = mockStatic(StatNewDB.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            tables.when(() -> StatNewDB.createStatTablesFromError("table does not exist", "_2025_12", "stat_clicks_v2")).thenReturn(true);
            assertFalse(save(sql, java.util.Collections.singletonList(new Object[] { "event" })));
            tables.verify(() -> StatNewDB.createStatTablesFromError("table does not exist", "_2025_12", "stat_clicks_v2"));
        }
    }

    @Test
    void onlyUniqueKeyViolationsAreIgnoredForEachSupportedDatabase() {
        assertTrue(StatWriteBuffer.isDuplicateKey(new SQLException("duplicate", "23505"), Constants.DB_PGSQL));
        assertTrue(StatWriteBuffer.isDuplicateKey(new SQLException("duplicate", "23000", 1062), Constants.DB_MYSQL));
        assertTrue(StatWriteBuffer.isDuplicateKey(new SQLException("duplicate", "23000", 2627), Constants.DB_MSSQL));
        assertTrue(StatWriteBuffer.isDuplicateKey(new SQLException("duplicate", "23000", 2601), Constants.DB_MSSQL));
        assertTrue(StatWriteBuffer.isDuplicateKey(new SQLException("duplicate", "23000", 1), Constants.DB_ORACLE));
        assertFalse(StatWriteBuffer.isDuplicateKey(new SQLException("null value", "23502"), Constants.DB_PGSQL));
        assertFalse(StatWriteBuffer.isDuplicateKey(new SQLException("permission denied", "42501"), Constants.DB_PGSQL));
        assertFalse(StatWriteBuffer.isDuplicateKey(new SQLException("foreign key", "23000", 1452), Constants.DB_MYSQL));
    }

    @Test
    void monthlySchemaUsesAnEventPrimaryKeyOnEveryDatabase() throws Exception {
        Method ddl = StatNewDB.class.getDeclaredMethod("getCreateStatTableSqlCommand", String.class, String.class, int.class);
        ddl.setAccessible(true);
        String procedure = Constants.getString("statTableCreateProcedureName");
        Constants.setString("statTableCreateProcedureName", "");
        try {
            for (int type : new int[] { Constants.DB_MYSQL, Constants.DB_MSSQL, Constants.DB_PGSQL, Constants.DB_ORACLE }) {
                String sql = (String) ddl.invoke(null, "stat_clicks_v2", "_2026_9", type);
                assertTrue(sql.contains("event_id CHAR(32) NOT NULL PRIMARY KEY"));
                assertTrue(sql.contains("domain_name VARCHAR(255) NOT NULL"));
                assertTrue(sql.contains("viewport_width INT NOT NULL"));
                assertTrue(sql.contains("CREATE INDEX hm_page_2026_9"));
                assertFalse(sql.contains("session_id"));
                assertFalse(sql.contains("IDENTITY"));
            }
        } finally {
            Constants.setString("statTableCreateProcedureName", procedure);
        }
    }

    private static boolean save(UpdateInsertSqlPair sql, List<Object[]> values) throws Exception {
        Method save = StatWriteBuffer.class.getDeclaredMethod("batchSave", UpdateInsertSqlPair.class, List.class, Map.class);
        save.setAccessible(true);
        return (boolean) save.invoke(null, sql, values, Map.of(sql, "stat_clicks_v2"));
    }
}
