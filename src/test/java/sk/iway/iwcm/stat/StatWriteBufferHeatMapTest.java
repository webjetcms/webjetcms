package sk.iway.iwcm.stat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies idempotent event inserts without changing legacy buffer behavior. */
class StatWriteBufferHeatMapTest extends BaseWebjetTest {
    /** Buffer keys share equivalent statements while keeping partitions and duplicate policies separate. */
    @Test
    void groupsOnlyEquivalentBufferStatements() {
        Map<UpdateInsertSqlPair, Integer> batches = new HashMap<>();
        batches.put(new UpdateInsertSqlPair("insert", "_2026_8", true), 1);
        assertEquals(1, batches.get(new UpdateInsertSqlPair(new String("insert"), "_2026_8", true)));
        batches.put(new UpdateInsertSqlPair("insert", "_2026_9", true), 2);
        batches.put(new UpdateInsertSqlPair("insert", "_2026_8", false), 3);
        batches.put(new UpdateInsertSqlPair(null, "insert"), 4);
        batches.put(new UpdateInsertSqlPair("update", "insert"), 5);
        assertEquals(5, batches.size());
        assertEquals(4, batches.get(new UpdateInsertSqlPair(null, "insert")));
    }

    @Test
    void duplicateClickDoesNotPreventTheFollowingClickFromBeingWritten() throws Exception {
        int originalType = Constants.DB_TYPE;
        Constants.DB_TYPE = Constants.DB_PGSQL;
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenThrow(new SQLException("duplicate event", "23505")).thenReturn(1);
        UpdateInsertSqlPair sql = new UpdateInsertSqlPair("INSERT INTO stat_clicks_2026_8 (event_id) VALUES (?)", "_2026_8", true);
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
        UpdateInsertSqlPair sql = new UpdateInsertSqlPair(null, "INSERT INTO stat_clicks_2026_8 (event_id) VALUES (?)");
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class); MockedStatic<StatNewDB> tables = mockStatic(StatNewDB.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            assertFalse(save(sql, List.of(new Object[] { "duplicate" }, new Object[] { "next" })));
            verify(statement).executeUpdate();
            tables.verify(() -> StatNewDB.createStatTablesFromError("duplicate event", null, "stat_clicks"));
        }
    }

    @Test
    void missingPartitionRetryUsesTheEventsMonth() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("table does not exist", "42P01"));
        UpdateInsertSqlPair sql = new UpdateInsertSqlPair("INSERT INTO stat_clicks_2025_12 (event_id) VALUES (?)", "_2025_12", true);
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class); MockedStatic<StatNewDB> tables = mockStatic(StatNewDB.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            tables.when(() -> StatNewDB.createStatTablesFromError("table does not exist", "_2025_12", "stat_clicks")).thenReturn(true);
            assertFalse(save(sql, java.util.Collections.singletonList(new Object[] { "event" })));
            tables.verify(() -> StatNewDB.createStatTablesFromError("table does not exist", "_2025_12", "stat_clicks"));
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

    private static boolean save(UpdateInsertSqlPair sql, List<Object[]> values) throws Exception {
        Method save = StatWriteBuffer.class.getDeclaredMethod("batchSave", UpdateInsertSqlPair.class, List.class, Map.class);
        save.setAccessible(true);
        return (boolean) save.invoke(null, sql, values, Map.of(sql, "stat_clicks"));
    }
}
