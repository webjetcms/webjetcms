package sk.iway.iwcm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.i18n.IwayProperties;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Verifies translation migration, collision cleanup and retry after partial progress using a connection-local
 * MariaDB/MySQL temporary table with the CI schema. Application tables and migration history
 * are never modified; startup markers and cache refresh notifications are mocked.
 */
class UpdateDatabaseTranslationKeysTest extends BaseWebjetTest {
    private static final String LEGACY = "components.mustistep.form.back_button";
    private static final String CURRENT = "components.multistep.form.back_button";
    private static final String NOTE = "31.10.2026 migrate multistep translation keys and remove legacy duplicates";
    private static final String OLD_NOTE = "28.10.2026 [sivan] Fix multistep form translation keys";
    private static final Timestamp EARLIER = Timestamp.valueOf("2026-10-01 10:00:00");
    private static final Timestamp LATER = Timestamp.valueOf("2026-10-28 10:00:00");

    /** Preserves IDs, HTML, dates and language boundaries, and permits retry after a missing completion marker. */
    @ParameterizedTest
    @ValueSource(strings = { "", "example.com-", "42." })
    void renamesLegacyKeysWithoutChangingTheirData(String prefix) throws Exception {
        try (TestDatabase database = new TestDatabase()) {
            database.insert(1, prefix + LEGACY, "en", "<button>Customer back</button>", LATER);
            database.insert(2, prefix + CURRENT, "sk", "Slovak override", EARLIER);
            database.insert(3, "components.mustistep_extra.label", "en", "Unrelated", EARLIER);
            database.insert(4, "components.MUSTISTEP.uppercase_only", "en", "Case-sensitive key", EARLIER);

            Set<String> completed = new HashSet<>();
            migrate(database.borrowConnection(), completed);

            List<Translation> expected = List.of(
                new Translation(1, prefix + CURRENT, "en", "<button>Customer back</button>", LATER),
                new Translation(2, prefix + CURRENT, "sk", "Slovak override", EARLIER),
                new Translation(3, "components.mustistep_extra.label", "en", "Unrelated", EARLIER),
                new Translation(4, "components.MUSTISTEP.uppercase_only", "en", "Case-sensitive key", EARLIER)
            );
            assertEquals(expected, database.readAll());
            assertEquals(Set.of(NOTE), completed);

            // Simulate a crash after saving data but before recording completion.
            completed.clear();
            migrate(database.borrowConnection(), completed);
            assertEquals(expected, database.readAll());
            assertEquals(Set.of(NOTE), completed);
        }
    }

    /** Cleans collisions left by the old SQL migration so deleting an override restores the file default. */
    @ParameterizedTest
    @CsvSource({ "'', true", "'', false", "example.com-, true", "example.com-, false", "42., true", "42., false" })
    void removesHiddenLegacyOverrides(String prefix, boolean correctedFirst) throws Exception {
        try (TestDatabase database = new TestDatabase()) {
            long correctedId = correctedFirst ? 1 : 2;
            long legacyId = correctedFirst ? 2 : 1;
            database.insert(legacyId, prefix + LEGACY, "en", "Legacy HTML", LATER);
            database.insert(correctedId, prefix + CURRENT, "en", "Corrected HTML", EARLIER);
            Set<String> completed = new HashSet<>(Set.of(OLD_NOTE));

            migrate(database.borrowConnection(), completed);

            assertEquals(List.of(new Translation(correctedId, prefix + CURRENT, "en", "Corrected HTML", EARLIER)),
                database.readAll());
            assertEquals(Set.of(OLD_NOTE, NOTE), completed);
            assertEquals("Corrected HTML", database.loadTranslation(prefix + CURRENT));

            try (PreparedStatement delete = database.connection.prepareStatement(
                    "DELETE FROM _properties_ WHERE prop_key = ? AND lng = ?")) {
                delete.setString(1, prefix + CURRENT);
                delete.setString(2, "en");
                assertEquals(1, delete.executeUpdate());
            }
            assertTrue(database.readAll().isEmpty());
            assertEquals("File default", database.loadTranslation(prefix + CURRENT));
        }
    }

    /** Retains an earlier rename when duplicate deletion fails, leaves completion unset and finishes on retry. */
    @Test
    void preservesPartialProgressAndFinishesOnRetry() throws Exception {
        try (TestDatabase database = new TestDatabase()) {
            database.insert(1, "42." + LEGACY, "en", "Rename me", EARLIER);
            database.insert(2, LEGACY, "en", "Legacy HTML", LATER);
            database.insert(3, CURRENT, "en", "Corrected HTML", EARLIER);
            Connection failingConnection = database.borrowConnection();
            doAnswer(call -> {
                PreparedStatement delete = spy((PreparedStatement) call.callRealMethod());
                doThrow(new SQLException("Simulated duplicate deletion failure")).when(delete).executeUpdate();
                return delete;
            }).when(failingConnection).prepareStatement("DELETE FROM _properties_ WHERE id = ?");
            Set<String> completed = new HashSet<>();

            migrate(failingConnection, completed);

            assertEquals(List.of(
                new Translation(1, "42." + CURRENT, "en", "Rename me", EARLIER),
                new Translation(2, LEGACY, "en", "Legacy HTML", LATER),
                new Translation(3, CURRENT, "en", "Corrected HTML", EARLIER)
            ), database.readAll());
            assertTrue(completed.isEmpty());
            assertTrue(database.connection.getAutoCommit(), "Completed changes must be saved independently");

            migrate(database.borrowConnection(), completed);
            assertEquals(List.of(
                new Translation(1, "42." + CURRENT, "en", "Rename me", EARLIER),
                new Translation(3, CURRENT, "en", "Corrected HTML", EARLIER)
            ), database.readAll());
            assertEquals(Set.of(NOTE), completed);
        }
    }

    /** A failed initial scan must not be mistaken for an empty, successfully migrated table. */
    @Test
    void doesNotMarkFailedReadAsCompleted() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Simulated read failure"));
        Set<String> completed = new HashSet<>();

        migrate(connection, completed);

        assertTrue(completed.isEmpty());
        verify(connection).close();
    }

    /** A recorded Java migration does not open a database connection or refresh translations again. */
    @Test
    void skipsCompletedMigration() {
        Connection connection = mock(Connection.class);

        migrate(connection, new HashSet<>(Set.of(NOTE)));

        verifyNoInteractions(connection);
    }

    /** Runs the real startup migration while isolating history writes and cache invalidation. */
    private void migrate(Connection connection, Set<String> completed) {
        boolean alreadyCompleted = completed.contains(NOTE);
        try (var pool = mockStatic(DBPool.class);
                var updates = mockStatic(UpdateDatabase.class, call -> {
                    if ("isAllreadyUpdated".equals(call.getMethod().getName())) return completed.contains(call.getArgument(0));
                    if ("saveSuccessUpdate".equals(call.getMethod().getName())) {
                        completed.add(call.getArgument(0));
                        return null;
                    }
                    return call.callRealMethod();
                });
                var props = mockStatic(Prop.class)) {
            pool.when(DBPool::getConnection).thenReturn(connection);
            props.when(() -> Prop.fixKeyTypo(anyString())).thenCallRealMethod();

            ReflectionTestUtils.invokeMethod(UpdateDatabase.class, "migrateMultistepTranslationKeys");

            int successfulRuns = alreadyCompleted == false && completed.contains(NOTE) ? 1 : 0;
            props.verify(() -> Prop.getInstance(true), times(successfulRuns));
            updates.verify(() -> UpdateDatabase.saveSuccessUpdate(NOTE), times(successfulRuns));
            if (alreadyCompleted) pool.verifyNoInteractions();
        }
    }

    private record Translation(long id, String key, String language, String value, Timestamp updated) {}

    /** Shadows only this connection's properties table and removes it before returning the connection to the pool. */
    private static class TestDatabase implements AutoCloseable {
        private final Connection connection;

        TestDatabase() throws Exception {
            String schema = Files.readString(Path.of(".github/workflows/blank_web_autotest.sql"));
            Matcher definition = Pattern.compile("(?ms)^CREATE TABLE `_properties_` \\(.*?^\\) ENGINE=[^;]+;").matcher(schema);
            assertTrue(definition.find(), "The CI properties table definition must exist");
            connection = DBPool.getConnection();
            assertNotNull(connection, "A configured MariaDB/MySQL connection is required");
            try {
                String product = connection.getMetaData().getDatabaseProductName();
                assumeTrue(product.equals("MariaDB") || product.equals("MySQL"), "The temporary-table fixture requires MariaDB/MySQL");
                assertEquals("_properties_", ConfDB.PROPERTIES_TABLE_NAME);
                try (Statement sql = connection.createStatement()) {
                    sql.execute(definition.group().replace("CREATE TABLE", "CREATE TEMPORARY TABLE"));
                }
            } catch (Exception | AssertionError e) {
                connection.close();
                throw e;
            }
        }

        Connection borrowConnection() throws SQLException {
            Connection borrowed = spy(connection);
            doNothing().when(borrowed).close();
            return borrowed;
        }

        void insert(long id, String key, String language, String value, Timestamp updated) throws SQLException {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO _properties_ (id, prop_key, lng, prop_value, update_date) VALUES (?, ?, ?, ?, ?)")) {
                insert.setLong(1, id);
                insert.setString(2, key);
                insert.setString(3, language);
                insert.setString(4, value);
                insert.setTimestamp(5, updated);
                assertEquals(1, insert.executeUpdate());
            }
        }

        List<Translation> readAll() throws SQLException {
            List<Translation> translations = new ArrayList<>();
            try (Statement sql = connection.createStatement();
                    ResultSet rows = sql.executeQuery("SELECT * FROM _properties_ ORDER BY id")) {
                while (rows.next()) {
                    translations.add(new Translation(rows.getLong("id"), rows.getString("prop_key"), rows.getString("lng"),
                        rows.getString("prop_value"), rows.getTimestamp("update_date")));
                }
            }
            return translations;
        }

        /** Reads the migrated rows through Prop's real JDBC loader with a fresh file-default cache. */
        String loadTranslation(String key) throws SQLException {
            IwayProperties defaults = new IwayProperties();
            defaults.setProperty(key, "File default");
            MockServletContext context = new MockServletContext();
            Connection borrowed = borrowConnection();
            try (var pool = mockStatic(DBPool.class);
                    var props = mockStatic(Prop.class, CALLS_REAL_METHODS);
                    var startup = mockStatic(InitServlet.class)) {
                pool.when(() -> DBPool.getConnection("iwcm")).thenReturn(borrowed);
                props.when(() -> Prop.getDefaulProperties("en", "_en", context)).thenReturn(defaults);
                startup.when(InitServlet::isWebjetConfigured).thenReturn(true);
                Prop prop = Prop.getInstance(context, "en", false);
                assertFalse(prop.getProperties().containsKey(key.replace("components.multistep.", "components.mustistep.")));
                return prop.getTextProp(key);
            }
        }

        @Override
        public void close() throws SQLException {
            try {
                try (Statement sql = connection.createStatement()) {
                    sql.execute("DROP TEMPORARY TABLE _properties_");
                }
            } finally {
                connection.close();
            }
        }
    }
}
