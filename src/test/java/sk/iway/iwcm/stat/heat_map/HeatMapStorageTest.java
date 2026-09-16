package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.stat.StatWriteBuffer;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies event-date partitioning and isolation of heatmap queries. */
class HeatMapStorageTest extends BaseWebjetTest {
    private static final String EVENT_ID = "0123456789abcdef0123456789abcdef";

    @Test
    void delayedEventKeepsItsOriginalMonthAndDay() {
        long epoch = LocalDate.of(2025, 12, 31).atTime(23, 59).atZone(ZoneId.systemDefault()).toEpochSecond();
        HeatMapEvent event = new HeatMapEvent(EVENT_ID, 123, 390, epoch, 800, 4000);
        try (MockedStatic<StatWriteBuffer> buffer = mockStatic(StatWriteBuffer.class)) {
            HeatMapStorage.record(event, "Example.COM");
            buffer.verify(() -> StatWriteBuffer.addIdempotent(
                    "INSERT INTO stat_clicks_2025_12 (event_id, domain_name, document_id, day_of_month, viewport_width, x, y) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    "stat_clicks", "_2025_12", EVENT_ID, "example.com", 123, 31, 390, 800, 4000));
        }
    }

    @Test
    void invalidEventsCannotReachTheBuffer() {
        long now = Instant.now().getEpochSecond();
        assertThrows(IllegalArgumentException.class, () -> new HeatMapEvent("not-an-id", 1, 390, now, 10, 20));
        assertThrows(IllegalArgumentException.class, () -> new HeatMapEvent(EVENT_ID, 0, 390, now, 10, 20));
        assertThrows(IllegalArgumentException.class, () -> new HeatMapEvent(EVENT_ID, 1, 0, now, 10, 20));
        assertThrows(IllegalArgumentException.class, () -> new HeatMapEvent(EVENT_ID, 1, 390, now, -1, 20));
    }

    @Test
    void sameMonthQueryAppliesBothDaysAndCanonicalDomain() throws Exception {
        Connection connection = connectionWithTables("stat_clicks_2026_9");
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        ResultSet result = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true, false);
        when(result.getInt(1)).thenReturn(456);
        when(result.getLong(2)).thenReturn(7L);
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            assertEquals(Map.of(456, 7L), HeatMapStorage.getPageCounts("Example.COM", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12)));
            verify(statement).setString(1, "example.com");
            verify(statement).setInt(2, 10);
            verify(statement).setInt(3, 12);
        }
    }

    /** Migrated clicks remain visible at their assigned width even without an event identifier. */
    @Test
    void migratedDesktopClicksUseTheSameDomainScopedWidthQuery() throws Exception {
        Connection connection = connectionWithTables("stat_clicks_2010_6");
        PreparedStatement statement = statementWithCount(1920, 7);
        String sql = "SELECT viewport_width, COUNT(*) FROM stat_clicks_2010_6"
                + " WHERE domain_name = ? AND day_of_month BETWEEN ? AND ? AND document_id = ? GROUP BY viewport_width";
        when(connection.prepareStatement(sql)).thenReturn(statement);
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            assertEquals(Map.of(1920, 7L), HeatMapStorage.getWidths("example.com", 123,
                    LocalDate.of(2010, 6, 1), LocalDate.of(2010, 6, 30)));
            verify(statement).setString(1, "example.com");
            verify(statement).setInt(2, 1);
            verify(statement).setInt(3, 30);
            verify(statement).setInt(4, 123);
        }
    }

    @Test
    void yearBoundaryAddsWidthsFromExistingPartitionsOnly() throws Exception {
        Connection connection = connectionWithTables("stat_clicks_2025_12", "stat_clicks_2026_1", "stat_views_2026_1", "stat_clicks_2026_2");
        PreparedStatement december = statementWithCount(390, 3);
        PreparedStatement january = statementWithCount(390, 5);
        when(connection.prepareStatement(anyString())).thenAnswer(invocation ->
                invocation.getArgument(0, String.class).contains("_2025_12") ? december : january);
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            assertEquals(Map.of(390, 8L), HeatMapStorage.getWidths("example.com", 123, LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1)));
            verify(december).setInt(2, 31);
            verify(december).setInt(3, 31);
            verify(january).setInt(2, 1);
            verify(january).setInt(3, 1);
            verify(december).setInt(4, 123);
            verify(january).setInt(4, 123);
        }
    }

    @Test
    void legacyMssqlDriverWithoutGetSchemaStillDiscoversPartitions() throws Exception {
        Connection connection = connectionWithTables("STAT_CLICKS_2026_9");
        when(connection.getSchema()).thenThrow(new AbstractMethodError("JDBC getSchema is not implemented"));
        PreparedStatement statement = statementWithCount(390, 2);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            assertEquals(Map.of(390, 2L), HeatMapStorage.getWidths("example.com", 123, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14)));
        }
    }

    @Test
    void databaseFailureIsNotReportedAsAnEmptyHeatmap() throws Exception {
        Connection connection = connectionWithTables("stat_clicks_2026_9");
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("permission denied", "42501"));
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            assertThrows(IllegalStateException.class, () -> HeatMapStorage.getPageCounts("example.com", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14)));
        }
    }

    private static PreparedStatement statementWithCount(int width, long count) throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true, false);
        when(result.getInt(1)).thenReturn(width);
        when(result.getLong(2)).thenReturn(count);
        return statement;
    }

    private static Connection connectionWithTables(String... names) throws SQLException {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getTables(any(), any(), any(), any())).thenReturn(tables);
        int[] index = { -1 };
        when(tables.next()).thenAnswer(invocation -> ++index[0] < names.length);
        when(tables.getString("TABLE_NAME")).thenAnswer(invocation -> names[index[0]]);
        return connection;
    }
}
