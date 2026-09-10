package sk.iway.iwcm.stat.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.opentest4j.TestAbortedException;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.system.UpdateDatabase;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Verifies that {@link BrowserIdentifierMigrationService} preserves referenced stat keys,
 * reports progress, and stops scanning once all deletion candidates are known to be in use.
 *
 * <p>Database tests run on MariaDB using table definitions from the CI fixture
 * {@code .github/workflows/blank_web_autotest.sql}. Each database test uses an isolated schema,
 * so the configured database user needs permission to create and drop test databases.
 * Database tests are skipped before any DDL if the original database contains {@code stat_views_2024_2}.</p>
 */
class BrowserIdentifierMigrationReferenceTest extends BaseWebjetTest {

    /**
     * Verifies that finalization deletes only unused keys, preserves browser and OS references,
     * and publishes progress for one scan per relevant current or legacy table.
     */
    @Test
    void finalizationShouldPreserveUsedKeysAndPublishTableProgress() throws Exception {
        checkFinalization(false);
    }

    /**
     * Verifies that a failed reference scan preserves all stat keys and reports an error
     * without marking finalization as completed.
     */
    @Test
    void failedReferenceCheckShouldPreventDeletion() throws Exception {
        checkFinalization(true);
    }

    /**
     * Checks key preservation and progress on success, or unchanged keys on reference-scan failure.
     * Creating the {@link TestDatabase} fixture aborts this helper before any database writes
     * if the configured database contains {@code stat_views_2024_2}.
     *
     * @param failReferenceCheck whether the reference scan should fail instead of completing
     */
    private void checkFinalization(boolean failReferenceCheck) throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService(executor);
        List<BrowserIdentifierMigrationService.State> progress = new ArrayList<>();

        try (TestDatabase fixture = new TestDatabase("seo_bots", "stat_keys", "stat_views", "stat_from", "stat_error", "emails_stat_click");
             Statement sql = fixture.connection.createStatement();
             MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
             MockedStatic<UpdateDatabase> updates = mockStatic(UpdateDatabase.class);
             MockedStatic<StatDB> statDB = mockStatic(StatDB.class);
             MockedStatic<ClusterDB> cluster = mockStatic(ClusterDB.class);
             MockedStatic<Adminlog> audit = mockStatic(Adminlog.class);
             MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            sql.execute("INSERT INTO stat_keys (stat_keys_id, value) VALUES (41, 'Chrome 127'), (42, 'Nokia Series 40'), " +
                "(43, 'Custom System Version 1'), (44, 'Chrome 126'), (45, 'Chrome 125'), (46, 'Chrome 124'), " +
                "(100, 'Chrome'), (200, 'Nokia Series'), (300, 'Custom System Version')");
            sql.execute("INSERT INTO stat_views (browser_ua_id, platform_id, subplatform_id) VALUES (100, 42, NULL)");
            sql.execute("CREATE TABLE stat_views_2024_2 LIKE stat_views");
            sql.execute("INSERT INTO stat_views_2024_2 (browser_ua_id, platform_id, subplatform_id) VALUES (46, 200, 43)");
            sql.execute("RENAME TABLE stat_error TO stat_error_2023_9");
            sql.execute("ALTER TABLE stat_error_2023_9 ADD COLUMN IF NOT EXISTS browser_ua_id INT");
            sql.execute("INSERT INTO stat_error_2023_9 (browser_ua_id) VALUES (44)");
            sql.execute("ALTER TABLE emails_stat_click ADD COLUMN IF NOT EXISTS browser_ua_id INT");
            sql.execute("INSERT INTO emails_stat_click (email_id, link, click_date, browser_ua_id) VALUES (1, '/', CURRENT_TIMESTAMP, 45)");

            updates.when(() -> UpdateDatabase.isAllreadyUpdated(BrowserIdentifierMigrationService.UPDATE_NOTE)).thenReturn(true);
            statDB.when(StatDB::getInstance).thenReturn(mock(StatDB.class));
            Connection connection = spy(fixture.connection);
            doNothing().when(connection).close();
            doAnswer(query -> {
                progress.add(service.getStatus());
                if (failReferenceCheck) throw new SQLException("Reference scan failed");
                return query.callRealMethod();
            }).when(connection).prepareStatement(startsWith("SELECT DISTINCT "));
            dbPool.when(DBPool::getConnection).thenReturn(connection);

            service.finalizeCompletedMigration();
            ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
            verify(executor).execute(task.capture());
            task.getValue().run();

            BrowserIdentifierMigrationService.State state = service.getStatus();
            assertFalse(state.isRunning());
            if (failReferenceCheck) {
                assertFalse(state.isFinalized());
                assertEquals("Reference scan failed", state.getError());
                assertEquals(9, countKeys(sql));
                return;
            }

            assertTrue(state.isFinalized(), state.getError());
            assertEquals(1, state.getDeletedStatKeys());
            assertEquals(5, state.getRetainedStatKeys());
            assertEquals(8, countKeys(sql));
            assertEquals(4, progress.size(), "Each table with key columns must be scanned only once");
            assertEquals(List.of(0, 1, 3, 4), progress.stream().map(BrowserIdentifierMigrationService.State::getTableIndex).toList());
            for (BrowserIdentifierMigrationService.State snapshot : progress) {
                assertTrue(snapshot.isRunning());
                assertTrue(snapshot.isFinalizing());
                assertEquals(0, snapshot.getTableMaxId(), "The UI must show an indeterminate scan");
                assertEquals(6, snapshot.getTotalTables());
            }
            try (ResultSet result = sql.executeQuery("SELECT value FROM stat_keys WHERE stat_keys_id=41")) {
                assertFalse(result.next());
            }
            try (ResultSet result = sql.executeQuery("SELECT k.value FROM stat_views v JOIN stat_keys k ON k.stat_keys_id=v.platform_id")) {
                assertTrue(result.next());
                assertEquals("Nokia Series 40", result.getString(1));
            }
        } finally {
            service.destroy();
        }
    }

    /**
     * Verifies that finding all 1,001 candidate keys in the first table avoids scanning
     * the remaining tables and leaves no deletion candidates.
     */
    @Test
    void findingAllCandidatesInUseShouldSkipRemainingTablesRegardlessOfMappingCount() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        try (TestDatabase fixture = new TestDatabase("stat_views");
             Statement sql = fixture.connection.createStatement()) {
            Connection connection = spy(fixture.connection);
            sql.execute("CREATE TABLE stat_views_2024_2 LIKE stat_views");
            List<BrowserIdentifierMigrationService.Mapping> mappings = new ArrayList<>();
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO stat_views (browser_ua_id) VALUES (?)")) {
                for (long id = 1; id <= 1001; id++) {
                    mappings.add(new BrowserIdentifierMigrationService.Mapping(id, 2000, "Chrome " + id, "Chrome"));
                    insert.setLong(1, id);
                    insert.addBatch();
                }
                insert.executeBatch();
            }

            assertTrue(service.findUnusedStatKeys(connection, mappings).isEmpty());
            verify(connection, times(1)).prepareStatement(startsWith("SELECT DISTINCT "));
        } finally {
            service.destroy();
        }
    }

    /** Verifies that empty mappings yield no deletion candidates without accessing the database. */
    @Test
    void emptyMappingsShouldSkipTableDiscovery() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        try {
            assertTrue(service.findUnusedStatKeys(connection, List.of()).isEmpty());
            verifyNoInteractions(connection);
        } finally {
            service.destroy();
        }
    }

    /**
     * Verifies that fixture construction aborts and closes the connection without DDL
     * when {@code stat_views_2024_2} exists or its presence cannot be checked.
     */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void unsafeDatabaseShouldPreventFixtureCreation(boolean failCheck) throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement check = mock(PreparedStatement.class);
        ResultSet tables = mock(ResultSet.class);
        when(connection.getCatalog()).thenReturn("webjetcms_web");
        when(connection.prepareStatement(TestDatabase.CHECK_HISTORY_TABLE)).thenReturn(check);
        if (failCheck) {
            when(check.executeQuery()).thenThrow(new SQLException("History check failed"));
        } else {
            when(check.executeQuery()).thenReturn(tables);
            when(tables.next()).thenReturn(true);
        }

        try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class)) {
            dbPool.when(DBPool::getConnection).thenReturn(connection);
            if (failCheck) {
                assertThrows(SQLException.class, () -> new TestDatabase("stat_keys"));
            } else {
                assertThrows(TestAbortedException.class, () -> new TestDatabase("stat_keys"));
            }
        }

        verify(check).setString(1, "webjetcms_web");
        verify(check).setString(2, "stat_views_2024_2");
        verify(connection).getCatalog();
        verify(connection).prepareStatement(TestDatabase.CHECK_HISTORY_TABLE);
        verify(connection).close();
        verifyNoMoreInteractions(connection);
    }

    private int countKeys(Statement sql) throws SQLException {
        try (ResultSet result = sql.executeQuery("SELECT COUNT(*) FROM stat_keys")) {
            result.next();
            return result.getInt(1);
        }
    }

    /** Imports only the required table definitions and removes all fixture data on close. */
    private static class TestDatabase implements AutoCloseable {
        private static final String CHECK_HISTORY_TABLE =
            "SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name=?";
        private final Connection connection;
        private final String originalCatalog;
        private final String catalog = "browser_migration_test_" + UUID.randomUUID().toString().replace("-", "");

        /**
         * Creates an isolated fixture only after checking the original database for
         * {@code stat_views_2024_2}. Finding that table aborts the calling JUnit test before
         * any schema creation; a failed check also closes the connection and prevents DDL.
         */
        TestDatabase(String... tables) throws IOException, SQLException {
            String definitions = Files.readString(Path.of(".github/workflows/blank_web_autotest.sql"));
            connection = DBPool.getConnection();
            originalCatalog = connection.getCatalog();
            try (PreparedStatement check = connection.prepareStatement(CHECK_HISTORY_TABLE)) {
                check.setString(1, originalCatalog);
                check.setString(2, "stat_views_2024_2");
                try (ResultSet tablesFound = check.executeQuery()) {
                    assumeFalse(tablesFound.next(),
                        "Skipping database test: stat_views_2024_2 exists in the configured database; a blank test database is required");
                }
            } catch (SQLException | RuntimeException ex) {
                connection.close();
                throw ex;
            }
            try (Statement sql = connection.createStatement()) {
                sql.execute("CREATE DATABASE " + catalog);
                connection.setCatalog(catalog);
                for (String table : tables) {
                    Matcher definition = Pattern.compile("(?ms)^CREATE TABLE `" + table + "` \\(.*?^\\) ENGINE=[^;]+;")
                        .matcher(definitions);
                    if (definition.find() == false) throw new SQLException("Missing SQL fixture table: " + table);
                    sql.execute(definition.group());
                }
            } catch (SQLException ex) {
                close();
                throw ex;
            }
        }

        @Override
        public void close() throws SQLException {
            try (Statement sql = connection.createStatement()) {
                connection.setAutoCommit(true);
                connection.setCatalog(originalCatalog);
                sql.execute("DROP DATABASE IF EXISTS " + catalog);
            } finally {
                connection.close();
            }
        }
    }
}
