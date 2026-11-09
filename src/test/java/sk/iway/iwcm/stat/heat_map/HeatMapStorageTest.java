package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                    "INSERT INTO stat_clicks_2025_12 (event_id, domain_name, document_id, day_of_month, viewport_width, x, y, coordinate_mode, source_viewport_width) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    "stat_clicks", "_2025_12", EVENT_ID, "example.com", 123, 31, 390, 800, 4000, 0, 390));
        }
    }

    @Test
    void sameMonthQueryAppliesBothDaysAndCanonicalDomain() throws Exception {
        Connection connection = mock(Connection.class);
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

    @Test
    void databaseFailureIsNotReportedAsAnEmptyHeatmap() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("permission denied", "42501"));
        try (MockedStatic<DBPool> pool = mockStatic(DBPool.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            assertThrows(IllegalStateException.class, () -> HeatMapStorage.getPageCounts("example.com", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14)));
        }
    }
}
