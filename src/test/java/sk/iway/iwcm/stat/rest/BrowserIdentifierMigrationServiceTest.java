package sk.iway.iwcm.stat.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.system.UpdateDatabase;
import sk.iway.iwcm.system.cluster.ClusterDB;

class BrowserIdentifierMigrationServiceTest {

    private static final String LOAD_STAT_KEYS = "SELECT stat_keys_id, value FROM stat_keys";
    private static final String FIND_STAT_KEY = "SELECT stat_keys_id, value FROM stat_keys WHERE value=?";
    private static final String CHECK_STAT_KEY_ID = "SELECT value FROM stat_keys WHERE stat_keys_id=?";
    private static final String INSERT_STAT_KEY = "INSERT INTO stat_keys (stat_keys_id, value) VALUES (?, ?)";
    private static final String LOAD_SEO_BOTS = "SELECT seo_bots_id, name FROM seo_bots ORDER BY seo_bots_id";
    private static final String LOAD_SEO_BOT_STATS = "SELECT visit_count, last_visit FROM seo_bots WHERE seo_bots_id=?";
    private static final String UPDATE_SEO_BOT_STATS = "UPDATE seo_bots SET visit_count=?, last_visit=? WHERE seo_bots_id=?";
    private static final String DELETE_SEO_BOT = "DELETE FROM seo_bots WHERE seo_bots_id=?";
    private static final String DELETE_STAT_KEY = "DELETE FROM stat_keys WHERE stat_keys_id=?";
    private static final String CREATE_SEO_BOTS_NAME_INDEX = "CREATE UNIQUE INDEX ix_seo_bots_name ON seo_bots (name)";
    private static final String LOAD_STAT_VIEW = "SELECT view_id, browser_id, browser_ua_id FROM stat_views WHERE view_id>? AND view_id<=? ORDER BY view_id";
    private static final String UPDATE_STAT_VIEW = "UPDATE stat_views SET browser_id=?, browser_ua_id=? WHERE view_id=?";
    private static final String LOAD_STAT_FROM = "SELECT from_id, browser_id FROM stat_from WHERE from_id>? AND from_id<=? ORDER BY from_id";
    private static final String UPDATE_STAT_FROM = "UPDATE stat_from SET browser_id=? WHERE from_id=?";
    private static final String UPDATE_STAT_ERROR = "UPDATE stat_error_2023_9 SET browser_ua_id=CASE browser_ua_id " +
        "WHEN ? THEN ? WHEN ? THEN ? ELSE browser_ua_id END WHERE browser_ua_id IN (?,?)";

    @Test
    void auditCompletedTableShouldWriteMigrationDetails() {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();

        try (MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class)) {
            service.auditCompletedTable("stat_views_2026", 123, 62_345);

            adminlog.verify(() -> Adminlog.add(
                Adminlog.TYPE_UPDATEDB,
                "Browser identifier migration completed: table=stat_views_2026, convertedRecords=123, duration=00:01:02.345",
                -1,
                -1
            ));
        }

        service.destroy();
    }

    @Test
    void auditFinalizationShouldWriteCleanupDetails() {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        BrowserIdentifierMigrationService.FinalizationResult result =
            new BrowserIdentifierMigrationService.FinalizationResult(997, 3);

        try (MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class)) {
            service.auditFinalization(189, result);

            adminlog.verify(() -> Adminlog.add(
                Adminlog.TYPE_UPDATEDB,
                "Browser identifier migration finalized: deletedSeoBots=189, deletedStatKeys=997, retainedStatKeys=3",
                -1,
                -1
            ));
        }

        service.destroy();
    }

    @Test
    void startAndStopShouldUseSingleBackgroundTask() {
        ExecutorService executor = mock(ExecutorService.class);
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService(executor);

        try (MockedStatic<UpdateDatabase> updateDatabase = mockStatic(UpdateDatabase.class)) {
            BrowserIdentifierMigrationService.State started = service.start();
            BrowserIdentifierMigrationService.State alreadyRunning = service.start();
            BrowserIdentifierMigrationService.State stopping = service.stop();

            assertTrue(started.isRunning());
            assertTrue(alreadyRunning.isRunning());
            assertTrue(stopping.isRunning());
            assertTrue(stopping.isStopRequested());
            verify(executor, times(1)).execute(any(Runnable.class));
        }

        service.destroy();
        verify(executor).shutdownNow();
    }

    @Test
    void finalizeShouldStartAsSingleBackgroundTask() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService(executor);

        try (MockedStatic<UpdateDatabase> updateDatabase = mockStatic(UpdateDatabase.class)) {
            updateDatabase.when(() -> UpdateDatabase.isAllreadyUpdated(BrowserIdentifierMigrationService.UPDATE_NOTE)).thenReturn(true);

            BrowserIdentifierMigrationService.State started = service.finalizeCompletedMigration();
            BrowserIdentifierMigrationService.State alreadyRunning = service.finalizeCompletedMigration();

            assertTrue(started.isRunning());
            assertTrue(started.isFinalizing());
            assertFalse(started.isDone());
            assertTrue(alreadyRunning.isRunning());
            verify(executor, times(1)).execute(any(Runnable.class));
        }

        service.destroy();
    }

    @Test
    void deleteStatKeysShouldDeleteAllMigratedSources() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        PreparedStatement delete = mock(PreparedStatement.class);
        when(connection.prepareStatement(DELETE_STAT_KEY)).thenReturn(delete);
        when(delete.executeBatch()).thenReturn(new int[] { 1, 1 });

        int deleted = service.deleteStatKeys(
            connection,
            List.of(
                new BrowserIdentifierMigrationService.Mapping(41L, 100L, "Chrome 127", "Chrome"),
                new BrowserIdentifierMigrationService.Mapping(42L, 100L, "Chrome 128", "Chrome")
            )
        );

        assertEquals(2, deleted);
        verify(delete).setLong(1, 41L);
        verify(delete).setLong(1, 42L);
        verify(delete).executeBatch();
        service.destroy();
    }

    @Test
    void tableDefinitionsShouldInspectColumnsWithoutJdbcMetadataQueries() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetadata = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        Statement statement = mock(Statement.class);
        ResultSet emptyTable = mock(ResultSet.class);
        ResultSetMetaData tableMetadata = mock(ResultSetMetaData.class);
        when(connection.getMetaData()).thenReturn(databaseMetadata);
        when(databaseMetadata.getTables(isNull(), isNull(), eq("%"), any(String[].class))).thenReturn(tables);
        when(tables.next()).thenReturn(true, false);
        when(tables.getString("TABLE_NAME")).thenReturn("stat_from_2018_2");
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT * FROM stat_from_2018_2 WHERE 1=0")).thenReturn(emptyTable);
        when(emptyTable.getMetaData()).thenReturn(tableMetadata);
        when(tableMetadata.getColumnCount()).thenReturn(2);
        when(tableMetadata.getColumnName(1)).thenReturn("FROM_ID");
        when(tableMetadata.getColumnName(2)).thenReturn("BROWSER_ID");

        List<BrowserIdentifierMigrationService.TableDefinition> definitions = service.tableDefinitions(connection);

        assertEquals(1, definitions.size());
        assertEquals("stat_from_2018_2", definitions.get(0).name());
        assertEquals("from_id", definitions.get(0).idColumn());
        assertFalse(definitions.get(0).browserKey());
        verify(databaseMetadata, never()).getColumns(any(), any(), any(), any());
        service.destroy();
    }

    @Test
    void tableDefinitionsShouldIncludeStatErrorWithoutIdColumn() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetadata = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        Statement statement = mock(Statement.class);
        ResultSet emptyTable = mock(ResultSet.class);
        ResultSetMetaData tableMetadata = mock(ResultSetMetaData.class);
        when(connection.getMetaData()).thenReturn(databaseMetadata);
        when(databaseMetadata.getTables(isNull(), isNull(), eq("%"), any(String[].class))).thenReturn(tables);
        when(tables.next()).thenReturn(true, false);
        when(tables.getString("TABLE_NAME")).thenReturn("stat_error_2023_9");
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT * FROM stat_error_2023_9 WHERE 1=0")).thenReturn(emptyTable);
        when(emptyTable.getMetaData()).thenReturn(tableMetadata);
        when(tableMetadata.getColumnCount()).thenReturn(1);
        when(tableMetadata.getColumnName(1)).thenReturn("BROWSER_UA_ID");

        List<BrowserIdentifierMigrationService.TableDefinition> definitions = service.tableDefinitions(connection);

        assertEquals(1, definitions.size());
        assertEquals("stat_error_2023_9", definitions.get(0).name());
        assertNull(definitions.get(0).idColumn());
        assertTrue(definitions.get(0).browserKey());
        assertTrue(definitions.get(0).statError());
        service.destroy();
    }

    @Test
    void updateStatErrorMappingBatchShouldUseSingleSetBasedUpdate() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        PreparedStatement update = mock(PreparedStatement.class);
        when(connection.prepareStatement(UPDATE_STAT_ERROR)).thenReturn(update);
        when(update.executeUpdate()).thenReturn(17);

        long updated = service.updateStatErrorMappingBatch(
            connection,
            "stat_error_2023_9",
            List.of(
                new BrowserIdentifierMigrationService.Mapping(2694L, 3251L, "Chrome 127", "Chrome"),
                new BrowserIdentifierMigrationService.Mapping(2695L, 3251L, "Chrome 128", "Chrome")
            )
        );

        assertEquals(17, updated);
        InOrder parameters = inOrder(update);
        parameters.verify(update).setLong(1, 2694L);
        parameters.verify(update).setLong(2, 3251L);
        parameters.verify(update).setLong(3, 2695L);
        parameters.verify(update).setLong(4, 3251L);
        parameters.verify(update).setLong(5, 2694L);
        parameters.verify(update).setLong(6, 2695L);
        parameters.verify(update).executeUpdate();
        service.destroy();
    }

    @Test
    void markAsCompletedShouldSaveUpdateNote() {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();

        try (MockedStatic<UpdateDatabase> updateDatabase = mockStatic(UpdateDatabase.class)) {
            service.markAsCompleted();

            updateDatabase.verify(() -> UpdateDatabase.saveSuccessUpdate(BrowserIdentifierMigrationService.UPDATE_NOTE));
        }
    }

    @Test
    void markAsCompletedShouldNotSaveExistingUpdateNoteAgain() {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();

        try (MockedStatic<UpdateDatabase> updateDatabase = mockStatic(UpdateDatabase.class)) {
            updateDatabase.when(() -> UpdateDatabase.isAllreadyUpdated(BrowserIdentifierMigrationService.UPDATE_NOTE)).thenReturn(true);

            service.markAsCompleted();

            updateDatabase.verify(() -> UpdateDatabase.saveSuccessUpdate(BrowserIdentifierMigrationService.UPDATE_NOTE), never());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "Chrome 127.0, Chrome",
        "Googlebot 2.1, Googlebot",
        "Mobile Safari 17.4, Mobile Safari",
        "python-requests 2.31.0, python-requests",
        "1.0, Unknown",
        "Chrome, Chrome"
    })
    void shouldNormalizeBrowserIdentifier(String source, String expected) {
        assertEquals(expected, BrowserIdentifierMigrationService.normalizeBrowserIdentifier(source));
    }

    @Test
    void buildStatKeyMappingsShouldCreateMissingTargetOnceAndValidateIt() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        PreparedStatement loadStatement = mock(PreparedStatement.class);
        PreparedStatement findBeforeInsert = mock(PreparedStatement.class);
        PreparedStatement checkAvailableId = mock(PreparedStatement.class);
        PreparedStatement insertStatement = mock(PreparedStatement.class);
        PreparedStatement findAfterInsert = mock(PreparedStatement.class);
        PreparedStatement validateTarget = mock(PreparedStatement.class);
        ResultSet sourceRows = mock(ResultSet.class);
        ResultSet missingTarget = mock(ResultSet.class);
        ResultSet availableId = mock(ResultSet.class);
        ResultSet insertedTarget = mock(ResultSet.class);
        ResultSet validatedTarget = mock(ResultSet.class);

        when(connection.prepareStatement(LOAD_STAT_KEYS)).thenReturn(loadStatement);
        when(connection.prepareStatement(FIND_STAT_KEY)).thenReturn(findBeforeInsert, findAfterInsert);
        when(connection.prepareStatement(CHECK_STAT_KEY_ID)).thenReturn(checkAvailableId, validateTarget);
        when(connection.prepareStatement(INSERT_STAT_KEY)).thenReturn(insertStatement);
        when(loadStatement.executeQuery()).thenReturn(sourceRows);
        when(findBeforeInsert.executeQuery()).thenReturn(missingTarget);
        when(checkAvailableId.executeQuery()).thenReturn(availableId);
        when(findAfterInsert.executeQuery()).thenReturn(insertedTarget);
        when(validateTarget.executeQuery()).thenReturn(validatedTarget);
        when(sourceRows.next()).thenReturn(true, true, false);
        when(sourceRows.getLong(1)).thenReturn(41L, 42L);
        when(sourceRows.getString(2)).thenReturn("Chrome 127", "Chrome 128");
        when(missingTarget.next()).thenReturn(false);
        when(availableId.next()).thenReturn(false);
        when(insertStatement.executeUpdate()).thenReturn(1);
        when(insertedTarget.next()).thenReturn(true, false);
        when(insertedTarget.getLong(1)).thenReturn(100L);
        when(insertedTarget.getString(2)).thenReturn("Chrome");
        when(validatedTarget.next()).thenReturn(true, false);
        when(validatedTarget.getString(1)).thenReturn("Chrome");

        BrowserIdentifierMigrationService.StatKeyMappingResult result;
        try (MockedStatic<PkeyGenerator> pkeyGenerator = mockStatic(PkeyGenerator.class)) {
            pkeyGenerator.when(() -> PkeyGenerator.getNextValue("stat_keys")).thenReturn(100);
            result = service.buildStatKeyMappings(connection, true);
        }

        assertTrue(result.cacheRefreshRequired());
        assertEquals(2, result.mappings().size());
        assertEquals(41L, result.mappings().get(0).getSourceId());
        assertEquals(100L, result.mappings().get(0).getTargetId());
        assertEquals(42L, result.mappings().get(1).getSourceId());
        assertEquals(100L, result.mappings().get(1).getTargetId());
        verify(insertStatement).setLong(1, 100L);
        verify(insertStatement).setString(2, "Chrome");
        InOrder order = inOrder(sourceRows, insertStatement);
        order.verify(sourceRows).close();
        order.verify(insertStatement).executeUpdate();
    }

    @Test
    void runMigrationShouldRollbackAndExposeStatKeyInsertFailure() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        PreparedStatement loadSeoBots = mock(PreparedStatement.class);
        PreparedStatement loadStatement = mock(PreparedStatement.class);
        PreparedStatement findTarget = mock(PreparedStatement.class);
        PreparedStatement checkAvailableId = mock(PreparedStatement.class);
        PreparedStatement insertStatement = mock(PreparedStatement.class);
        ResultSet seoBotRows = mock(ResultSet.class);
        ResultSet sourceRows = mock(ResultSet.class);
        ResultSet missingTarget = mock(ResultSet.class);
        ResultSet availableId = mock(ResultSet.class);
        SQLException insertFailure = new SQLException("stat_keys insert failed");

        when(connection.prepareStatement(LOAD_SEO_BOTS)).thenReturn(loadSeoBots);
        when(connection.prepareStatement(LOAD_STAT_KEYS)).thenReturn(loadStatement);
        when(connection.prepareStatement(FIND_STAT_KEY)).thenReturn(findTarget);
        when(connection.prepareStatement(CHECK_STAT_KEY_ID)).thenReturn(checkAvailableId);
        when(connection.prepareStatement(INSERT_STAT_KEY)).thenReturn(insertStatement);
        when(loadSeoBots.executeQuery()).thenReturn(seoBotRows);
        when(loadStatement.executeQuery()).thenReturn(sourceRows);
        when(findTarget.executeQuery()).thenReturn(missingTarget);
        when(checkAvailableId.executeQuery()).thenReturn(availableId);
        when(seoBotRows.next()).thenReturn(false);
        when(sourceRows.next()).thenReturn(true, false);
        when(sourceRows.getLong(1)).thenReturn(41L);
        when(sourceRows.getString(2)).thenReturn("Chrome 127");
        when(missingTarget.next()).thenReturn(false);
        when(availableId.next()).thenReturn(false);
        when(insertStatement.executeUpdate()).thenThrow(insertFailure);

        try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
             MockedStatic<PkeyGenerator> pkeyGenerator = mockStatic(PkeyGenerator.class);
             MockedStatic<StatDB> statDB = mockStatic(StatDB.class);
             MockedStatic<ClusterDB> clusterDB = mockStatic(ClusterDB.class);
             MockedStatic<UpdateDatabase> updateDatabase = mockStatic(UpdateDatabase.class);
             MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            dbPool.when(DBPool::getConnection).thenReturn(connection);
            pkeyGenerator.when(() -> PkeyGenerator.getNextValue("stat_keys")).thenReturn(100);
            service.runMigration();

            assertEquals(insertFailure.getMessage(), service.getStatus().getError());
            statDB.verify(() -> StatDB.getInstance(true), never());
            clusterDB.verify(() -> ClusterDB.addRefresh(StatDB.class), never());
        }

        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void verifyStatKeyTargetsShouldRejectMissingTarget() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        PreparedStatement validateTarget = mock(PreparedStatement.class);
        ResultSet missingTarget = mock(ResultSet.class);
        when(connection.prepareStatement(CHECK_STAT_KEY_ID)).thenReturn(validateTarget);
        when(validateTarget.executeQuery()).thenReturn(missingTarget);
        when(missingTarget.next()).thenReturn(false);

        SQLException thrown = assertThrows(
            SQLException.class,
            () -> service.verifyStatKeyTargets(
                connection,
                List.of(new BrowserIdentifierMigrationService.Mapping(41L, 100L, "Chrome 127", "Chrome"))
            )
        );

        assertTrue(thrown.getMessage().contains("exactly one row"));
    }

    @Test
    void migrateRowsShouldPreserveNullBrowserIdWhenUaIdChanges() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        BrowserIdentifierMigrationService.State state = new BrowserIdentifierMigrationService.State();
        BrowserIdentifierMigrationService.TableDefinition table =
            new BrowserIdentifierMigrationService.TableDefinition("stat_views", "view_id", true);
        Connection connection = mock(Connection.class);
        PreparedStatement read = mock(PreparedStatement.class);
        PreparedStatement write = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        state.setTableMaxId(1);

        when(connection.prepareStatement(LOAD_STAT_VIEW)).thenReturn(read);
        when(connection.prepareStatement(UPDATE_STAT_VIEW)).thenReturn(write);
        when(read.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        when(rows.getLong(1)).thenReturn(1L);
        when(rows.getLong(2)).thenReturn(0L);
        when(rows.getLong(3)).thenReturn(41L);
        when(rows.wasNull()).thenReturn(true, false);

        long lastId = service.migrateRows(connection, table, state, Map.of(), Map.of(41L, 100L));

        InOrder readOrder = inOrder(rows);
        readOrder.verify(rows).getLong(2);
        readOrder.verify(rows).wasNull();
        readOrder.verify(rows).getLong(3);
        readOrder.verify(rows).wasNull();
        verify(write).setNull(1, Types.BIGINT);
        verify(write).setLong(2, 100L);
        verify(write).setLong(3, 1L);
        verify(write).addBatch();
        verify(write).executeBatch();
        assertEquals(1L, lastId);
        assertEquals(1L, state.getScanned());
        assertEquals(1L, state.getUpdated());
    }

    @Test
    void migrateRowsShouldPreserveNullUaIdWhenBrowserIdChanges() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        BrowserIdentifierMigrationService.State state = new BrowserIdentifierMigrationService.State();
        BrowserIdentifierMigrationService.TableDefinition table =
            new BrowserIdentifierMigrationService.TableDefinition("stat_views", "view_id", true);
        Connection connection = mock(Connection.class);
        PreparedStatement read = mock(PreparedStatement.class);
        PreparedStatement write = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        state.setTableMaxId(1);

        when(connection.prepareStatement(LOAD_STAT_VIEW)).thenReturn(read);
        when(connection.prepareStatement(UPDATE_STAT_VIEW)).thenReturn(write);
        when(read.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        when(rows.getLong(1)).thenReturn(1L);
        when(rows.getLong(2)).thenReturn(41L);
        when(rows.getLong(3)).thenReturn(0L);
        when(rows.wasNull()).thenReturn(false, true);

        long lastId = service.migrateRows(connection, table, state, Map.of(41L, 100L), Map.of());

        verify(write).setLong(1, 100L);
        verify(write).setNull(2, Types.INTEGER);
        verify(write).setLong(3, 1L);
        verify(write).addBatch();
        verify(write).executeBatch();
        assertEquals(1L, lastId);
        assertEquals(1L, state.getScanned());
        assertEquals(1L, state.getUpdated());
    }

    @Test
    void migrateRowsShouldNotApplyZeroMappingToNullBrowserId() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        BrowserIdentifierMigrationService.State state = new BrowserIdentifierMigrationService.State();
        BrowserIdentifierMigrationService.TableDefinition table =
            new BrowserIdentifierMigrationService.TableDefinition("stat_from", "from_id", false);
        Connection connection = mock(Connection.class);
        PreparedStatement read = mock(PreparedStatement.class);
        PreparedStatement write = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        state.setTableMaxId(1);

        when(connection.prepareStatement(LOAD_STAT_FROM)).thenReturn(read);
        when(connection.prepareStatement(UPDATE_STAT_FROM)).thenReturn(write);
        when(read.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        when(rows.getLong(1)).thenReturn(1L);
        when(rows.getLong(2)).thenReturn(0L);
        when(rows.wasNull()).thenReturn(true);

        long lastId = service.migrateRows(connection, table, state, Map.of(0L, 100L), Map.of());

        verify(write, never()).addBatch();
        verify(write, never()).executeBatch();
        assertEquals(1L, lastId);
        assertEquals(1L, state.getScanned());
        assertEquals(0L, state.getUpdated());
    }

    @Test
    void ensureUniqueNameIndexShouldAcceptExactSingleColumnIndex() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mockSeoBotsMetadata(connection);
        ResultSet indexes = mock(ResultSet.class);
        when(metadata.getIndexInfo(isNull(), isNull(), eq("SEO_BOTS"), eq(true), eq(false))).thenReturn(indexes);
        when(indexes.next()).thenReturn(true, false);
        when(indexes.getString("INDEX_NAME")).thenReturn("IX_SEO_BOTS_NAME");
        when(indexes.getString("COLUMN_NAME")).thenReturn("NAME");
        when(indexes.getBoolean("NON_UNIQUE")).thenReturn(false);
        when(indexes.getShort("TYPE")).thenReturn(DatabaseMetaData.tableIndexOther);
        when(indexes.getShort("ORDINAL_POSITION")).thenReturn((short) 1);

        service.ensureUniqueNameIndex(connection);

        verify(connection, never()).createStatement();
    }

    @Test
    void ensureUniqueNameIndexShouldCreateExactIndexWhenOnlyCompositeIndexExists() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mockSeoBotsMetadata(connection);
        ResultSet compositeIndex = mock(ResultSet.class);
        ResultSet exactIndex = mock(ResultSet.class);
        Statement createIndex = mock(Statement.class);
        when(metadata.getIndexInfo(isNull(), isNull(), eq("SEO_BOTS"), eq(true), eq(false)))
            .thenReturn(compositeIndex, exactIndex);
        when(compositeIndex.next()).thenReturn(true, true, false);
        when(compositeIndex.getString("INDEX_NAME")).thenReturn("IX_SEO_BOTS_NAME_DOMAIN");
        when(compositeIndex.getString("COLUMN_NAME")).thenReturn("NAME", "DOMAIN_ID");
        when(compositeIndex.getBoolean("NON_UNIQUE")).thenReturn(false);
        when(compositeIndex.getShort("TYPE")).thenReturn(DatabaseMetaData.tableIndexOther);
        when(compositeIndex.getShort("ORDINAL_POSITION")).thenReturn((short) 1, (short) 2);
        when(exactIndex.next()).thenReturn(true, false);
        when(exactIndex.getString("INDEX_NAME")).thenReturn("IX_SEO_BOTS_NAME");
        when(exactIndex.getString("COLUMN_NAME")).thenReturn("NAME");
        when(exactIndex.getBoolean("NON_UNIQUE")).thenReturn(false);
        when(exactIndex.getShort("TYPE")).thenReturn(DatabaseMetaData.tableIndexOther);
        when(exactIndex.getShort("ORDINAL_POSITION")).thenReturn((short) 1);
        when(connection.createStatement()).thenReturn(createIndex);

        service.ensureUniqueNameIndex(connection);

        verify(createIndex).executeUpdate(CREATE_SEO_BOTS_NAME_INDEX);
    }

    @Test
    void ensureUniqueNameIndexShouldFailWhenIndexIsMissingAfterDdl() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mockSeoBotsMetadata(connection);
        ResultSet indexesBefore = mock(ResultSet.class);
        ResultSet indexesAfter = mock(ResultSet.class);
        Statement createIndex = mock(Statement.class);
        when(metadata.getIndexInfo(isNull(), isNull(), eq("SEO_BOTS"), eq(true), eq(false)))
            .thenReturn(indexesBefore, indexesAfter);
        when(connection.createStatement()).thenReturn(createIndex);

        SQLException thrown = assertThrows(SQLException.class, () -> service.ensureUniqueNameIndex(connection));

        assertTrue(thrown.getMessage().contains("was not created"));
        verify(createIndex).executeUpdate(CREATE_SEO_BOTS_NAME_INDEX);
    }

    @Test
    void ensureUniqueNameIndexShouldAcceptIndexCreatedConcurrently() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mockSeoBotsMetadata(connection);
        ResultSet indexesBefore = mock(ResultSet.class);
        ResultSet indexesAfter = mock(ResultSet.class);
        Statement createIndex = mock(Statement.class);
        SQLException concurrentCreateFailure = new SQLException("index already exists");
        when(metadata.getIndexInfo(isNull(), isNull(), eq("SEO_BOTS"), eq(true), eq(false)))
            .thenReturn(indexesBefore, indexesAfter);
        when(indexesAfter.next()).thenReturn(true, false);
        when(indexesAfter.getString("INDEX_NAME")).thenReturn("IX_SEO_BOTS_NAME");
        when(indexesAfter.getString("COLUMN_NAME")).thenReturn("NAME");
        when(indexesAfter.getBoolean("NON_UNIQUE")).thenReturn(false);
        when(indexesAfter.getShort("TYPE")).thenReturn(DatabaseMetaData.tableIndexOther);
        when(indexesAfter.getShort("ORDINAL_POSITION")).thenReturn((short) 1);
        when(connection.createStatement()).thenReturn(createIndex);
        when(createIndex.executeUpdate(CREATE_SEO_BOTS_NAME_INDEX)).thenThrow(concurrentCreateFailure);

        service.ensureUniqueNameIndex(connection);

        verify(createIndex).executeUpdate(CREATE_SEO_BOTS_NAME_INDEX);
    }

    @Test
    void ensureUniqueNameIndexShouldPropagateCreateFailureWhenIndexIsStillMissing() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mockSeoBotsMetadata(connection);
        ResultSet indexesBefore = mock(ResultSet.class);
        ResultSet indexesAfter = mock(ResultSet.class);
        Statement createIndex = mock(Statement.class);
        SQLException createFailure = new SQLException("duplicate values prevent unique index creation");
        when(metadata.getIndexInfo(isNull(), isNull(), eq("SEO_BOTS"), eq(true), eq(false)))
            .thenReturn(indexesBefore, indexesAfter);
        when(connection.createStatement()).thenReturn(createIndex);
        when(createIndex.executeUpdate(CREATE_SEO_BOTS_NAME_INDEX)).thenThrow(createFailure);

        SQLException thrown = assertThrows(SQLException.class, () -> service.ensureUniqueNameIndex(connection));

        assertSame(createFailure, thrown);
    }

    @Test
    void finalizeSeoBotsShouldDeleteSourcesBeforeCreatingUniqueIndex() throws Exception {
        BrowserIdentifierMigrationService service = new BrowserIdentifierMigrationService();
        Connection connection = mock(Connection.class);
        PreparedStatement sourceStats = mock(PreparedStatement.class);
        PreparedStatement targetStats = mock(PreparedStatement.class);
        PreparedStatement updateTarget = mock(PreparedStatement.class);
        PreparedStatement deleteSource = mock(PreparedStatement.class);
        ResultSet sourceRow = mock(ResultSet.class);
        ResultSet targetRow = mock(ResultSet.class);
        DatabaseMetaData metadata = mockSeoBotsMetadata(connection);
        ResultSet indexesBefore = mock(ResultSet.class);
        ResultSet indexesAfter = mock(ResultSet.class);
        Statement createIndex = mock(Statement.class);
        Timestamp sourceVisit = Timestamp.valueOf("2026-08-01 10:00:00");
        Timestamp targetVisit = Timestamp.valueOf("2026-08-02 10:00:00");

        when(connection.prepareStatement(LOAD_SEO_BOT_STATS)).thenReturn(sourceStats, targetStats);
        when(connection.prepareStatement(UPDATE_SEO_BOT_STATS)).thenReturn(updateTarget);
        when(connection.prepareStatement(DELETE_SEO_BOT)).thenReturn(deleteSource);
        when(sourceStats.executeQuery()).thenReturn(sourceRow);
        when(targetStats.executeQuery()).thenReturn(targetRow);
        when(sourceRow.next()).thenReturn(true, false);
        when(sourceRow.getLong(1)).thenReturn(2L);
        when(sourceRow.getTimestamp(2)).thenReturn(sourceVisit);
        when(targetRow.next()).thenReturn(true, false);
        when(targetRow.getLong(1)).thenReturn(3L);
        when(targetRow.getTimestamp(2)).thenReturn(targetVisit);
        when(metadata.getIndexInfo(isNull(), isNull(), eq("SEO_BOTS"), eq(true), eq(false)))
            .thenReturn(indexesBefore, indexesAfter);
        when(indexesAfter.next()).thenReturn(true, false);
        when(indexesAfter.getString("INDEX_NAME")).thenReturn("IX_SEO_BOTS_NAME");
        when(indexesAfter.getString("COLUMN_NAME")).thenReturn("NAME");
        when(indexesAfter.getBoolean("NON_UNIQUE")).thenReturn(false);
        when(indexesAfter.getShort("TYPE")).thenReturn(DatabaseMetaData.tableIndexOther);
        when(indexesAfter.getShort("ORDINAL_POSITION")).thenReturn((short) 1);
        when(connection.createStatement()).thenReturn(createIndex);

        service.finalizeSeoBots(
            connection,
            List.of(new BrowserIdentifierMigrationService.Mapping(41L, 100L, "Chrome 127", "Chrome"))
        );

        verify(updateTarget).setLong(1, 5L);
        verify(updateTarget).setTimestamp(2, targetVisit);
        InOrder order = inOrder(deleteSource, createIndex);
        order.verify(deleteSource).executeBatch();
        order.verify(createIndex).executeUpdate(CREATE_SEO_BOTS_NAME_INDEX);
    }

    private DatabaseMetaData mockSeoBotsMetadata(Connection connection) throws SQLException {
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getTables(isNull(), isNull(), eq("%"), any(String[].class))).thenReturn(tables);
        when(tables.next()).thenReturn(true, false);
        when(tables.getString("TABLE_NAME")).thenReturn("SEO_BOTS");
        return metadata;
    }
}
