package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.system.UpdateDatabase;

/** Verifies resumable monthly-table upgrades using JDBC mocks without touching a database. */
class HeatMapSchemaTest {
    private static final String TABLE = "stat_clicks_2024_2";

    static List<Integer> databaseTypes() {
        return List.of(Constants.DB_MYSQL, Constants.DB_PGSQL, Constants.DB_MSSQL, Constants.DB_ORACLE);
    }

    /** Each database preserves legacy columns and skips duplicate columns and indexes on repeated upgrades. */
    @ParameterizedTest
    @MethodSource("databaseTypes")
    void upgradesOnlyOriginalMonthlyTablesAndCanRunAgain(int databaseType) throws Exception {
        DatabaseFixture fixture = new DatabaseFixture(databaseType);

        fixture.upgrade();
        verify(fixture.connection).close();

        assertTrue(fixture.columns.containsAll(Set.of("stat_click_id", "document_id", "x", "y", "day_of_month",
                "event_id", "domain_name", "viewport_width")));
        assertTrue(fixture.indexes.containsAll(Set.of("to_document__2024_2", "hm_event_2024_2", "hm_page_2024_2")));
        assertEquals(3, fixture.executed.stream().filter(sql -> sql.startsWith("ALTER TABLE")).count());
        assertEquals(2, fixture.executed.stream().filter(sql -> sql.startsWith("CREATE ")).count());
        assertTrue(fixture.executed.stream().anyMatch(sql -> sql.contains(databaseType == Constants.DB_ORACLE
                ? "viewport_width INT DEFAULT 1920 NOT NULL" : "viewport_width INT NOT NULL DEFAULT 1920")));
        assertTrue(fixture.executed.stream().anyMatch(sql -> sql.endsWith("ADD event_id CHAR(32) NULL")));
        assertTrue(fixture.executed.stream().anyMatch(sql -> sql.endsWith("ADD domain_name VARCHAR(255) NULL")));
        String eventIndex = fixture.executed.stream().filter(sql -> sql.startsWith("CREATE UNIQUE INDEX")).findFirst().orElseThrow();
        assertEquals(databaseType == Constants.DB_MSSQL, eventIndex.endsWith("WHERE event_id IS NOT NULL"));
        String pageIndex = fixture.executed.stream().filter(sql -> sql.startsWith("CREATE INDEX hm_page")).findFirst().orElseThrow();
        assertEquals(databaseType == Constants.DB_MYSQL, pageIndex.contains("domain_name(128)"));
        String backfill = fixture.executed.stream().filter(sql -> sql.startsWith("UPDATE ")).findFirst().orElseThrow();
        assertTrue(backfill.contains("LOWER(LTRIM(RTRIM(g.domain_name)))"));
        assertTrue(backfill.contains(databaseType == Constants.DB_ORACLE
                ? "FROM \"DOCUMENTS\" d JOIN \"GROUPS\" g" : "FROM \"documents\" d JOIN \"groups\" g"));
        assertTrue(backfill.contains("d.doc_id = " + TABLE + ".document_id"));
        assertTrue(backfill.endsWith("WHERE domain_name IS NULL"));
        assertFalse(fixture.executed.stream().anyMatch(sql -> sql.contains("stat_clicks_v2") || sql.contains("DROP ")
                || sql.contains("SET event_id") || sql.contains("SET x") || sql.contains("SET y")));

        fixture.executed.clear();
        fixture.upgrade();
        assertEquals(List.of(backfill), fixture.executed, "A repeated upgrade only retries unattributed legacy domains");
    }

    /** A failure after some DDL leaves those successful steps reusable on the next startup. */
    @ParameterizedTest
    @MethodSource("databaseTypes")
    void resumesAfterFailedIndexCreationWithoutRepeatingColumns(int databaseType) throws Exception {
        DatabaseFixture fixture = new DatabaseFixture(databaseType);
        fixture.pageIndexFailure = new SQLException("index permission denied");
        assertSame(fixture.pageIndexFailure, assertThrows(SQLException.class, fixture::upgrade));
        verify(fixture.connection).close();
        assertTrue(fixture.columns.contains("viewport_width"));
        assertTrue(fixture.indexes.contains("hm_event_2024_2"));
        assertFalse(fixture.indexes.contains("hm_page_2024_2"));

        fixture.pageIndexFailure = null;
        fixture.executed.clear();
        fixture.upgrade();
        assertTrue(fixture.indexes.contains("hm_page_2024_2"));
        assertFalse(fixture.executed.stream().anyMatch(sql -> sql.startsWith("ALTER TABLE") || sql.startsWith("CREATE UNIQUE INDEX")));
    }

    /** Missing months are skipped while progress and cleanup cover each attempted monthly table. */
    @ParameterizedTest
    @MethodSource("databaseTypes")
    void skipsMissingMonthsAndLogsProgress(int databaseType) throws Exception {
        DatabaseFixture fixture = new DatabaseFixture(databaseType);
        fixture.suffixes = new String[] { "_2024_1", "_2024_2", "_2024_3" };
        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            fixture.upgrade();

            logger.verify(() -> Logger.println(HeatMapSchema.class, "Updating stat_clicks columns 1/3 _2024_1"));
            logger.verify(() -> Logger.println(HeatMapSchema.class, "Updating stat_clicks columns 2/3 _2024_2"));
            logger.verify(() -> Logger.println(HeatMapSchema.class, "Updating stat_clicks columns 3/3 _2024_3"));
            logger.verify(() -> Logger.println(HeatMapSchema.class, "Updated stat_clicks columns in 1 tables"));
        }
        verify(fixture.connection, times(3)).close();
        verify(fixture.statement, times(3)).close();
        assertTrue(fixture.columns.contains("viewport_width"));
        assertFalse(fixture.executed.stream().anyMatch(sql -> sql.contains("stat_clicks_2024_1") || sql.contains("stat_clicks_2024_3")));
    }

    /** A missing backfill dependency must fail the migration rather than be mistaken for a missing month. */
    @ParameterizedTest
    @MethodSource("databaseTypes")
    void propagatesMissingBackfillDependencies(int databaseType) throws Exception {
        DatabaseFixture fixture = new DatabaseFixture(databaseType);
        SQLException failure = fixture.missingTable();
        doThrow(failure).when(fixture.statement).executeUpdate(anyString());

        assertSame(failure, assertThrows(SQLException.class, fixture::upgrade));
        verify(fixture.connection).close();
    }

    /** Duplicate data in a unique index must not be mistaken for an already existing index. */
    @ParameterizedTest
    @MethodSource("databaseTypes")
    void propagatesDuplicateDataErrors(int databaseType) throws Exception {
        DatabaseFixture fixture = new DatabaseFixture(databaseType);
        SQLException failure = switch (databaseType) {
            case Constants.DB_MYSQL -> new SQLException("duplicate data", "23000", 1062);
            case Constants.DB_MSSQL -> new SQLException("duplicate data", "23000", 1505);
            case Constants.DB_ORACLE -> new SQLException("duplicate data", "23000", 1452);
            default -> new SQLException("duplicate data", "23505");
        };
        doThrow(failure).when(fixture.statement).execute(HeatMapSchema.uniqueIndexSql(TABLE, "_2024_2", databaseType));

        assertSame(failure, assertThrows(SQLException.class, fixture::upgrade));
        verify(fixture.connection).close();
    }

    /** Startup records success only after all monthly table upgrades have finished. */
    @Test
    void marksStartupMigrationSuccessfulOnlyAfterAllTablesSucceed() throws Exception {
        try (MockedStatic<UpdateDatabase> updates = mockStatic(UpdateDatabase.class);
                MockedStatic<HeatMapSchema> schema = mockStatic(HeatMapSchema.class);
                MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            updates.when(UpdateDatabase::updateStatClicksColumns).thenCallRealMethod();
            schema.when(HeatMapSchema::upgradeExistingTables).thenThrow(new SQLException("index permission denied"));

            UpdateDatabase.updateStatClicksColumns();
            updates.verify(() -> UpdateDatabase.saveSuccessUpdate(anyString()), never());

            schema.reset();
            UpdateDatabase.updateStatClicksColumns();
            updates.verify(() -> UpdateDatabase.saveSuccessUpdate(anyString()));
        }
    }

    private static final class DatabaseFixture {
        private final int databaseType;
        private final Connection connection = mock(Connection.class);
        private final DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        private final Statement statement = mock(Statement.class);
        private final Set<String> columns = new HashSet<>(Set.of("stat_click_id", "document_id", "x", "y", "day_of_month"));
        private final Set<String> indexes = new HashSet<>(Set.of("to_document__2024_2"));
        private final List<String> executed = new ArrayList<>();
        private SQLException pageIndexFailure;
        private String[] suffixes = { "_2024_2" };

        private void upgrade() throws SQLException {
            int originalType = Constants.DB_TYPE;
            try (MockedStatic<DBPool> pool = mockStatic(DBPool.class);
                    MockedStatic<StatNewDB> statistics = mockStatic(StatNewDB.class)) {
                Constants.DB_TYPE = databaseType;
                pool.when(DBPool::getConnection).thenReturn(connection);
                statistics.when(() -> StatNewDB.getTableSuffix(eq("stat_clicks"), anyLong(), anyLong()))
                        .thenAnswer(invocation -> {
                            Calendar from = Calendar.getInstance();
                            from.setTimeInMillis(invocation.getArgument(1));
                            assertEquals(2000, from.get(Calendar.YEAR));
                            assertEquals(Calendar.JANUARY, from.get(Calendar.MONTH));
                            assertEquals(1, from.get(Calendar.DATE));
                            Calendar to = Calendar.getInstance();
                            to.setTimeInMillis(invocation.getArgument(2));
                            assertEquals(Calendar.getInstance().get(Calendar.YEAR) + 1, to.get(Calendar.YEAR));
                            return suffixes;
                        });
                HeatMapSchema.upgradeExistingTables();
            } finally {
                Constants.DB_TYPE = originalType;
            }
        }

        private DatabaseFixture(int databaseType) throws SQLException {
            this.databaseType = databaseType;
            when(connection.getMetaData()).thenReturn(metadata);
            when(connection.createStatement()).thenReturn(statement);
            when(metadata.getIdentifierQuoteString()).thenReturn("\"");
            when(metadata.storesUpperCaseIdentifiers()).thenReturn(databaseType == Constants.DB_ORACLE);
            when(statement.executeQuery(anyString())).thenAnswer(invocation -> {
                if (!invocation.getArgument(0).equals("SELECT 1 FROM " + TABLE + " WHERE 1=0")) throw missingTable();
                return mock(ResultSet.class);
            });
            when(statement.execute(anyString())).thenAnswer(invocation -> {
                String sql = invocation.getArgument(0);
                if (sql.startsWith("ALTER TABLE")) {
                    if (!columns.add(sql.split(" ADD ")[1].split(" ")[0])) throw duplicate(true);
                } else if (sql.startsWith("CREATE UNIQUE INDEX")) {
                    if (!indexes.add(sql.split(" ")[3])) throw duplicate(false);
                } else if (sql.startsWith("CREATE INDEX")) {
                    if (pageIndexFailure != null) throw pageIndexFailure;
                    if (!indexes.add(sql.split(" ")[2])) throw duplicate(false);
                }
                executed.add(sql);
                return true;
            });
            when(statement.executeUpdate(anyString())).thenAnswer(invocation -> {
                executed.add(invocation.getArgument(0));
                return 3;
            });
        }

        private SQLException duplicate(boolean column) {
            return switch (databaseType) {
                case Constants.DB_MYSQL -> new SQLException("already exists", "42000", column ? 1060 : 1061);
                case Constants.DB_MSSQL -> new SQLException("already exists", "42000", column ? 2705 : 1913);
                case Constants.DB_ORACLE -> new SQLException("already exists", "42000", column ? 1430 : 955);
                default -> new SQLException("already exists", column ? "42701" : "42P07");
            };
        }

        private SQLException missingTable() {
            return switch (databaseType) {
                case Constants.DB_MYSQL -> new SQLException("missing table", "42S02", 1146);
                case Constants.DB_MSSQL -> new SQLException("missing table", "S0002", 208);
                case Constants.DB_ORACLE -> new SQLException("missing table", "42000", 942);
                default -> new SQLException("missing table", "42P01");
            };
        }
    }
}
