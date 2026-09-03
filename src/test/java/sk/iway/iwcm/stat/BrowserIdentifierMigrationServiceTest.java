package sk.iway.iwcm.stat;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.PkeyGenerator;
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
    private static final String CREATE_SEO_BOTS_NAME_INDEX = "CREATE UNIQUE INDEX ix_seo_bots_name ON seo_bots (name)";

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
    void processShouldRollbackAndPropagateStatKeyInsertFailure() throws Exception {
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
             MockedStatic<ClusterDB> clusterDB = mockStatic(ClusterDB.class)) {
            dbPool.when(DBPool::getConnection).thenReturn(connection);
            pkeyGenerator.when(() -> PkeyGenerator.getNextValue("stat_keys")).thenReturn(100);
            SQLException thrown = assertThrows(
                SQLException.class,
                () -> service.process(new BrowserIdentifierMigrationService.State())
            );
            assertSame(insertFailure, thrown);
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
