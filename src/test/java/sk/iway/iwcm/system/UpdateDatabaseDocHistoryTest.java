package sk.iway.iwcm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Checks the startup backfill and its completion records against PostgreSQL session-local tables. */
class UpdateDatabaseDocHistoryTest extends BaseWebjetTest {
    private static final String NOTE =
            "26.03.2026 [sivan] nastavenie is_delete v documents_history pre zaznamy oznacene [DELETE]";
    private static final String UPDATE_CACHE = "allreadyExecutedUpdates";

    /** A failed backfill remains retryable and a successful one records its original migration note. */
    @Test
    void retriesPostgresBackfillUntilItSucceeds() throws SQLException {
        DBPool.getInstance();
        assumeTrue(Constants.DB_TYPE == Constants.DB_PGSQL, "This backfill test requires PostgreSQL");
        Object previousCache = ReflectionTestUtils.getField(UpdateDatabase.class, UPDATE_CACHE);
        try (Connection connection = DBPool.getConnection(); Statement sql = connection.createStatement()) {
            try {
                sql.execute("CREATE TEMP TABLE _db_ (create_date DATE NOT NULL, note VARCHAR(255) NOT NULL)");
                sql.execute("CREATE TEMP TABLE documents_history (history_id INTEGER, title VARCHAR(255), data_asc TEXT)");
                sql.execute("""
                        INSERT INTO pg_temp.documents_history VALUES
                            (1, '[DELETE] Removed page', '[DELETE]'),
                            (2, '[DELETE] Title only', 'Content'),
                            (3, 'Content only', '[DELETE]'),
                            (4, 'Ordinary version', 'Content')
                        """);
                Connection borrowed = spy(connection);
                doNothing().when(borrowed).close();
                try (var pool = mockStatic(DBPool.class)) {
                    pool.when(() -> DBPool.getConnection("iwcm")).thenReturn(borrowed);
                    ReflectionTestUtils.setField(UpdateDatabase.class, UPDATE_CACHE, null);
                    String selectNotes = "SELECT note FROM pg_temp._db_";

                    UpdateDatabase.setIsDeleteDocHistory();
                    assertTrue(query.forListString(selectNotes).isEmpty(),
                            "A failed update must not record completion");

                    sql.execute("ALTER TABLE pg_temp.documents_history ADD is_delete BOOLEAN NOT NULL DEFAULT FALSE");
                    UpdateDatabase.setIsDeleteDocHistory();
                    assertEquals(List.of(1), query.forListInteger(
                            "SELECT history_id FROM pg_temp.documents_history WHERE is_delete"));
                    assertEquals(List.of(NOTE), query.forListString(selectNotes));

                    ReflectionTestUtils.setField(UpdateDatabase.class, UPDATE_CACHE, null);
                    UpdateDatabase.setIsDeleteDocHistory();
                    assertEquals(List.of(NOTE), query.forListString(selectNotes),
                            "A later startup must recognize the successful backfill");
                }
            } finally {
                ReflectionTestUtils.setField(UpdateDatabase.class, UPDATE_CACHE, previousCache);
                sql.execute("DROP TABLE IF EXISTS pg_temp.documents_history, pg_temp._db_");
            }
        }
    }
}
