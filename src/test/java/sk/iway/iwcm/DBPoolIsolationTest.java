package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Verifies the branch isolation prerequisite without opening a database connection:
 * legacy dirty reads remain available with the flag off, while strict failures never
 * return an unsafe acquired connection to the caller.
 */
class DBPoolIsolationTest
{
    private MockedStatic<Constants> constants;
    private MockedStatic<Logger> logger;
    private int originalDbType;
    private DBPool originalPool;
    private DBPool pool;
    private DataSource dataSource;
    private Connection connection;

    @BeforeEach
    void prepare() throws Exception
    {
        originalDbType = Constants.DB_TYPE;
        originalPool = (DBPool) ReflectionTestUtils.getField(DBPool.class, "instance");
        constants = Mockito.mockStatic(Constants.class);
        logger = Mockito.mockStatic(Logger.class);
        Constants.DB_TYPE = Constants.DB_MYSQL;
        pool = mock(DBPool.class);
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        when(pool.getDataSource("iwcm")).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        ReflectionTestUtils.setField(DBPool.class, "instance", pool);
    }

    @AfterEach
    void restore()
    {
        ReflectionTestUtils.setField(DBPool.class, "instance", originalPool);
        Constants.DB_TYPE = originalDbType;
        if (logger != null) logger.close();
        if (constants != null) constants.close();
    }

    /** Flag-off callers retain their existing requested isolation level. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void flagOffPreservesRequestedIsolation(boolean dirtyRead) throws Exception
    {
        setIsolation(dirtyRead);
        verify(connection).setTransactionIsolation(dirtyRead
            ? Connection.TRANSACTION_READ_UNCOMMITTED : Connection.TRANSACTION_READ_COMMITTED);
    }

    /** A partially configured feature keeps legacy dirty reads and best-effort error handling. */
    @ParameterizedTest
    @CsvSource({"true, false", "false, true"})
    void incompleteConfigurationPreservesLegacyIsolation(boolean featureFlag, boolean externalDirectories) throws Exception
    {
        constants.when(() -> Constants.getBoolean("branchesEnabled")).thenReturn(featureFlag);
        constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(externalDirectories);
        DBPool.setTransactionIsolationReadUNCommited(connection);
        verify(connection).setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        when(connection.getTransactionIsolation()).thenThrow(new SQLException("legacy isolation failure"));
        assertDoesNotThrow(() -> DBPool.setTransactionIsolationReadUNCommited(connection));
        verify(connection, never()).abort(any());
        verify(connection, never()).close();
    }

    /** Oracle's legacy flag-off path does not call unsupported dirty-read settings. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void flagOffPreservesOracleSkip(boolean dirtyRead)
    {
        Constants.DB_TYPE = Constants.DB_ORACLE;
        setIsolation(dirtyRead);
        verifyNoInteractions(connection);
    }

    /** Legacy callers still log isolation errors and continue when branches are disabled. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void flagOffPreservesBestEffortFailures(boolean dirtyRead) throws Exception
    {
        when(connection.getTransactionIsolation()).thenThrow(new SQLException("legacy isolation failure"));
        assertDoesNotThrow(() -> setIsolation(dirtyRead));
    }

    /** Both entry points upgrade a dirty connection to verified READ_COMMITTED. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void flagOnRequiresReadCommitted(boolean dirtyRead) throws Exception
    {
        enableBranches();
        when(connection.getTransactionIsolation()).thenReturn(
            Connection.TRANSACTION_READ_UNCOMMITTED, Connection.TRANSACTION_READ_COMMITTED);
        setIsolation(dirtyRead);
        verify(connection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        verify(connection, never()).setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
    }

    /** Oracle cannot bypass verification when branches are enabled. */
    @Test
    void flagOnVerifiesOracleConnection() throws Exception
    {
        enableBranches();
        Constants.DB_TYPE = Constants.DB_ORACLE;
        when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        DBPool.setTransactionIsolationReadUNCommited(connection);
        verify(connection).getTransactionIsolation();
        verify(connection, never()).setTransactionIsolation(anyInt());
    }

    /** Isolation inspection failures propagate to callers instead of allowing dirty reads. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void flagOnRejectsInspectionFailure(boolean dirtyRead) throws Exception
    {
        enableBranches();
        SQLException cause = new SQLException("cannot inspect isolation");
        when(connection.getTransactionIsolation()).thenThrow(cause);
        assertSame(cause, assertThrows(IllegalStateException.class, () -> setIsolation(dirtyRead)).getCause());
        verify(connection).abort(any());
        verify(connection).close();
    }

    /** Driver errors while changing isolation propagate through either helper. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void flagOnRejectsSettingFailure(boolean dirtyRead) throws Exception
    {
        enableBranches();
        SQLException cause = new SQLException("cannot change isolation");
        doThrow(cause).when(connection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        assertSame(cause, assertThrows(IllegalStateException.class, () -> setIsolation(dirtyRead)).getCause());
    }

    /** A driver silently ignoring the requested isolation is also a strict failure. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void flagOnRejectsUnchangedIsolation(boolean dirtyRead) throws Exception
    {
        enableBranches();
        when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_UNCOMMITTED);
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> setIsolation(dirtyRead));
        assertEquals("Database connection did not accept READ_COMMITTED isolation", failure.getCause().getMessage());
    }

    /** A valid connection remains open and is returned after the existing initialization. */
    @Test
    void returnsVerifiedConnection() throws Exception
    {
        enableBranches();
        when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        assertSame(connection, DBPool.getConnectionReadUncommited());
        verify(connection).setAutoCommit(true);
        verify(connection, never()).close();
    }

    /** Initialization failure closes the acquired handle with either flag setting. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void initializationFailureClosesConnection(boolean branches) throws Exception
    {
        if (branches) enableBranches();
        doThrow(new SQLException("cannot initialize autoCommit")).when(connection).setAutoCommit(true);
        assertNull(DBPool.getConnection());
        verify(connection).close();
    }

    /** Strict checkout failure also evicts Hikari's pool entry after the helper discards its handle. */
    @Test
    void strictFailureEvictsAndClosesConnection() throws Exception
    {
        enableBranches();
        HikariDataSource hikari = mock(HikariDataSource.class);
        when(dataSource.isWrapperFor(HikariDataSource.class)).thenReturn(true);
        when(dataSource.unwrap(HikariDataSource.class)).thenReturn(hikari);
        when(connection.getTransactionIsolation()).thenThrow(new SQLException("isolation unavailable"));
        assertNull(DBPool.getConnectionReadUncommited());
        verify(hikari).evictConnection(connection);
        verify(connection).abort(any());
        verify(connection, Mockito.atLeastOnce()).close();
    }

    /** Cleanup still closes the handle when pool eviction itself fails. */
    @Test
    void evictionFailureStillClosesConnection() throws Exception
    {
        enableBranches();
        when(dataSource.isWrapperFor(HikariDataSource.class)).thenThrow(new SQLException("pool unavailable"));
        when(connection.getTransactionIsolation()).thenThrow(new SQLException("isolation unavailable"));
        assertNull(DBPool.getConnection());
        verify(connection, Mockito.atLeastOnce()).abort(any());
        verify(connection, Mockito.atLeastOnce()).close();
    }

    /** A failed reset after checkout aborts the physical connection despite Hikari's cached isolation value. */
    @Test
    void directHelperFailureClosesPhysicalHikariConnection() throws Exception
    {
        enableBranches();
        DataSource physicalSource = mock(DataSource.class);
        Connection physical = mock(Connection.class);
        Connection replacement = mock(Connection.class);
        AtomicInteger physicalIsolation = new AtomicInteger(Connection.TRANSACTION_READ_COMMITTED);
        AtomicBoolean closed = new AtomicBoolean();
        when(physicalSource.getConnection()).thenReturn(physical, replacement);
        when(physical.getTransactionIsolation()).thenAnswer(invocation -> physicalIsolation.get());
        when(physical.isClosed()).thenAnswer(invocation -> closed.get());
        when(physical.isValid(anyInt())).thenAnswer(invocation -> !closed.get());
        doAnswer(invocation -> { closed.set(true); return null; }).when(physical).abort(any());
        doAnswer(invocation -> { closed.set(true); return null; }).when(physical).close();
        doAnswer(invocation -> {
            if (closed.get()) throw new SQLException("Connection is closed", "08003");
            return null;
        }).when(physical).clearWarnings();
        when(replacement.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        when(replacement.isValid(anyInt())).thenReturn(true);

        HikariConfig config = new HikariConfig();
        config.setDataSource(physicalSource);
        config.setMinimumIdle(0);
        config.setMaximumPoolSize(1);
        config.setInitializationFailTimeout(-1);
        config.setConnectionTimeout(1000);
        try (HikariDataSource hikari = new HikariDataSource(config))
        {
            Connection pooled = hikari.getConnection();
            assertSame(physical, pooled.unwrap(Connection.class));
            physicalIsolation.set(Connection.TRANSACTION_READ_UNCOMMITTED);
            assertThrows(IllegalStateException.class, () -> DBPool.setTransactionIsolationReadUNCommited(pooled));
            assertTrue(closed.get());
            assertTrue(pooled.isClosed());
            verify(physical).abort(any());
            try (Connection next = hikari.getConnection())
            {
                assertNotSame(physical, next.unwrap(Connection.class));
                assertSame(replacement, next.unwrap(Connection.class));
            }
        }
    }

    private void enableBranches()
    {
        constants.when(() -> Constants.getBoolean("branchesEnabled")).thenReturn(true);
        constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
    }

    private void setIsolation(boolean dirtyRead)
    {
        if (dirtyRead) DBPool.setTransactionIsolationReadUNCommited(connection);
        else DBPool.setTransactionIsolationReadCommited(connection);
    }
}
