package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.UpdateDatabase;

/** Verifies resumable monthly-table upgrades using JDBC mocks without touching a database. */
class HeatMapSchemaTest {
    private static final String TABLE = "stat_clicks_2024_2";

    static List<Integer> databaseTypes() {
        return List.of(Constants.DB_MYSQL, Constants.DB_PGSQL, Constants.DB_MSSQL, Constants.DB_ORACLE);
    }

    /** Each supported database preserves legacy columns and installs compatible defaults and unique indexes. */
    @ParameterizedTest
    @MethodSource("databaseTypes")
    void upgradesOnlyOriginalMonthlyTablesAndCanRunAgain(int databaseType) throws Exception {
        DatabaseFixture fixture = new DatabaseFixture();
        when(fixture.metadata.storesUpperCaseIdentifiers()).thenReturn(databaseType == Constants.DB_ORACLE);

        HeatMapSchema.upgradeExistingTables(fixture.connection, databaseType);

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
        HeatMapSchema.upgradeExistingTables(fixture.connection, databaseType);
        assertEquals(List.of(backfill), fixture.executed, "A repeated upgrade only retries unattributed legacy domains");
    }

    /** A failure after some DDL leaves those successful steps reusable on the next startup. */
    @Test
    void resumesAfterFailedIndexCreationWithoutRepeatingColumns() throws Exception {
        DatabaseFixture fixture = new DatabaseFixture();
        fixture.failPageIndex = true;
        assertThrows(SQLException.class, () -> HeatMapSchema.upgradeExistingTables(fixture.connection, Constants.DB_PGSQL));
        assertTrue(fixture.columns.contains("viewport_width"));
        assertTrue(fixture.indexes.contains("hm_event_2024_2"));
        assertFalse(fixture.indexes.contains("hm_page_2024_2"));

        fixture.failPageIndex = false;
        fixture.executed.clear();
        HeatMapSchema.upgradeExistingTables(fixture.connection, Constants.DB_PGSQL);
        assertTrue(fixture.indexes.contains("hm_page_2024_2"));
        assertFalse(fixture.executed.stream().anyMatch(sql -> sql.startsWith("ALTER TABLE") || sql.startsWith("CREATE UNIQUE INDEX")));
    }

    /** jTDS schema discovery falls back to SQL without broadening migration to other schemas. */
    @Test
    void supportsJdbcDriversWithoutGetSchema() throws Exception {
        DatabaseFixture fixture = new DatabaseFixture();
        when(fixture.connection.getSchema()).thenThrow(new AbstractMethodError("JDBC 4.1 not implemented"));
        ResultSet schema = rows(List.of("dbo"), "unused");
        when(schema.getString(1)).thenReturn("dbo");
        when(fixture.statement.executeQuery("SELECT SCHEMA_NAME()")).thenReturn(schema);

        HeatMapSchema.upgradeExistingTables(fixture.connection, Constants.DB_MSSQL);

        verify(fixture.metadata).getTables(eq("cms"), eq("dbo"), eq("%"), any(String[].class));
        assertTrue(fixture.executed.stream().filter(sql -> sql.startsWith("ALTER TABLE"))
                .allMatch(sql -> sql.contains("\"dbo\"." + TABLE)));
    }

    /** Underscores in JDBC schema patterns cannot select a similarly named schema or its columns. */
    @Test
    void ignoresMetadataFromOtherSchemasMatchingTheSamePattern() throws Exception {
        DatabaseFixture fixture = new DatabaseFixture();
        String activeSchema = "WEBJET_CMS";
        String foreignSchema = "WEBJET1CMS";
        when(fixture.connection.getSchema()).thenReturn(activeSchema);
        when(fixture.metadata.getTables(eq("cms"), eq(activeSchema), eq("%"), any(String[].class))).thenAnswer(invocation -> {
            ResultSet result = mock(ResultSet.class);
            AtomicInteger row = new AtomicInteger(-1);
            when(result.next()).thenAnswer(call -> row.incrementAndGet() < 2);
            when(result.getString("TABLE_NAME")).thenAnswer(call -> row.get() == 0 ? "stat_clicks_2024_3" : TABLE);
            when(result.getString("TABLE_SCHEM")).thenAnswer(call -> row.get() == 0 ? foreignSchema : activeSchema);
            return result;
        });
        when(fixture.metadata.getColumns("cms", activeSchema, TABLE, "%")).thenAnswer(invocation -> {
            List<String> localColumns = new ArrayList<>(fixture.columns);
            List<String> mixedColumns = new ArrayList<>(localColumns);
            mixedColumns.addAll(List.of("event_id", "domain_name", "viewport_width"));
            ResultSet result = mock(ResultSet.class);
            AtomicInteger row = new AtomicInteger(-1);
            when(result.next()).thenAnswer(call -> row.incrementAndGet() < mixedColumns.size());
            when(result.getString("TABLE_NAME")).thenReturn(TABLE);
            when(result.getString("COLUMN_NAME")).thenAnswer(call -> mixedColumns.get(row.get()));
            when(result.getString("TABLE_SCHEM")).thenAnswer(call -> row.get() < localColumns.size() ? activeSchema : foreignSchema);
            return result;
        });

        HeatMapSchema.upgradeExistingTables(fixture.connection, Constants.DB_ORACLE);

        assertEquals(3, fixture.executed.stream().filter(sql -> sql.startsWith("ALTER TABLE")).count());
        assertFalse(fixture.executed.stream().anyMatch(sql -> sql.contains("stat_clicks_2024_3") || sql.contains(foreignSchema)));
        verify(fixture.metadata).getIndexInfo("cms", activeSchema, TABLE, false, false);
    }

    /** Startup records success only after every discovered table has been upgraded. */
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

    private static ResultSet rows(List<String> values, String field) throws SQLException {
        ResultSet result = mock(ResultSet.class);
        AtomicInteger position = new AtomicInteger(-1);
        when(result.next()).thenAnswer(invocation -> position.incrementAndGet() < values.size());
        when(result.getString(field)).thenAnswer(invocation -> values.get(position.get()));
        return result;
    }

    private static final class DatabaseFixture {
        private final Connection connection = mock(Connection.class);
        private final DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        private final Statement statement = mock(Statement.class);
        private final Set<String> columns = new HashSet<>(Set.of("stat_click_id", "document_id", "x", "y", "day_of_month"));
        private final Set<String> indexes = new HashSet<>(Set.of("to_document__2024_2"));
        private final List<String> executed = new ArrayList<>();
        private boolean failPageIndex;

        private DatabaseFixture() throws SQLException {
            when(connection.getMetaData()).thenReturn(metadata);
            when(connection.getCatalog()).thenReturn("cms");
            when(connection.getSchema()).thenReturn("public");
            when(connection.createStatement()).thenReturn(statement);
            when(metadata.getIdentifierQuoteString()).thenReturn("\"");
            when(metadata.getTables(eq("cms"), any(), eq("%"), any(String[].class))).thenAnswer(invocation -> {
                ResultSet result = rows(List.of(TABLE, "stat_clicks_v2_2024_2", "stat_clicks_2024_13", "documents"), "TABLE_NAME");
                when(result.getString("TABLE_SCHEM")).thenReturn(invocation.getArgument(1));
                return result;
            });
            when(metadata.getColumns(eq("cms"), any(), eq(TABLE), eq("%"))).thenAnswer(invocation -> {
                ResultSet result = rows(new ArrayList<>(columns), "COLUMN_NAME");
                when(result.getString("TABLE_NAME")).thenReturn(TABLE);
                when(result.getString("TABLE_SCHEM")).thenReturn(invocation.getArgument(1));
                return result;
            });
            when(metadata.getIndexInfo(eq("cms"), any(), eq(TABLE), anyBoolean(), anyBoolean())).thenAnswer(invocation ->
                    rows(new ArrayList<>(indexes), "INDEX_NAME"));
            when(statement.execute(anyString())).thenAnswer(invocation -> {
                String sql = invocation.getArgument(0);
                executed.add(sql);
                if (sql.startsWith("ALTER TABLE")) columns.add(sql.split(" ADD ")[1].split(" ")[0]);
                else if (sql.startsWith("CREATE UNIQUE INDEX")) indexes.add(sql.split(" ")[3]);
                else if (sql.startsWith("CREATE INDEX")) {
                    if (failPageIndex) throw new SQLException("index permission denied");
                    indexes.add(sql.split(" ")[2]);
                }
                return true;
            });
            when(statement.executeUpdate(anyString())).thenAnswer(invocation -> {
                executed.add(invocation.getArgument(0));
                return 3;
            });
        }
    }
}
