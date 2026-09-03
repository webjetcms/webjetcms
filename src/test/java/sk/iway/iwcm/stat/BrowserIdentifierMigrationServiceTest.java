package sk.iway.iwcm.stat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
}
