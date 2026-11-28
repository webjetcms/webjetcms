package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import com.zaxxer.hikari.HikariDataSource;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.test.BaseWebjetTest;

/** SQL Server activation, session state and publication locking in the isolated fixture. */
@EnabledIfEnvironmentVariable(named="WEBJET_MSSQL2022_DB", matches="true")
class BranchSqlServerTest extends BaseWebjetTest {
    @TempDir Path storage;
    private Map<String,String> original;

    @BeforeEach void setup() throws SQLException {
        original = Map.of("branchesEnabled", Constants.getString("branchesEnabled"),
            "enableStaticFilesExternalDir", Constants.getString("enableStaticFilesExternalDir"),
            "branchStorageRoot", Constants.getString("branchStorageRoot"));
        try (var connection = DBPool.getConnection()) {
            BranchTestDomains.requireIsolatedDatabase(connection);
            assertEquals("Microsoft SQL Server", connection.getMetaData().getDatabaseProductName());
        }
        Constants.setBoolean("branchesEnabled", true);
        Constants.setBoolean("enableStaticFilesExternalDir", true);
        Constants.setString("branchStorageRoot", storage.toString());
    }

    @AfterEach void restore() { original.forEach(Constants::setString); }

    @Test void rejectsWeakIsolationAndDifferentJpaDatabase() throws SQLException {
        try (var jdbc = DBPool.getConnection(); var jpa = DBPool.getInstance().getDataSource("iwcm").getConnection()) {
            BranchReadiness.verifyActivation(jdbc);
            jdbc.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            assertThrows(SQLException.class, () -> BranchReadiness.verify(jdbc));
            jdbc.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            jpa.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            assertThrows(SQLException.class, () -> BranchReadiness.verifyJpaConnection(jdbc,jpa));
            jpa.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            jpa.setCatalog("tempdb");
            try { assertThrows(SQLException.class, () -> BranchReadiness.verifyJpaConnection(jdbc,jpa)); }
            finally { jpa.setCatalog(jdbc.getCatalog()); }
        }
    }

    @Test void checksAlterPermissionUnderTheApplicationUser() throws SQLException {
        try (var connection = DBPool.getConnection(); var sql = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                sql.execute("CREATE USER autotest_packages WITHOUT LOGIN WITH DEFAULT_SCHEMA=dbo");
                sql.execute("ALTER ROLE db_datareader ADD MEMBER autotest_packages");
                sql.execute("ALTER ROLE db_datawriter ADD MEMBER autotest_packages");
                sql.execute("EXECUTE AS USER='autotest_packages'");
                try {
                    var failure = assertThrows(SQLException.class, () -> BranchReadiness.verifyWritePrivileges(connection));
                    assertTrue(failure.getMessage().contains("ALTER on documents"), failure.getMessage());
                } finally { sql.execute("REVERT"); }
                sql.execute("GRANT ALTER ON documents TO autotest_packages");
                sql.execute("GRANT ALTER ON groups TO autotest_packages");
                sql.execute("EXECUTE AS USER='autotest_packages'");
                try { BranchReadiness.verifyWritePrivileges(connection); }
                finally { sql.execute("REVERT"); }
            } finally { connection.rollback(); }
        }
    }

    @Test void duplicateInsertRestoresIdentityModeOnTheSameConnection() throws SQLException {
        try (var connection = DBPool.getConnection(); var sql = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                int id = BranchStore.reserveNative(connection,BranchType.DOCUMENT);
                var row = BranchDocumentMapper.initialValues(id,1,Instant.now(),false);
                BranchRowWriter.insertReserved(connection,BranchType.DOCUMENT,row);
                assertThrows(SQLException.class, () -> BranchRowWriter.insertReserved(connection,BranchType.DOCUMENT,row));
                // SQL Server allows IDENTITY_INSERT for only one table at a time.
                sql.execute("SET IDENTITY_INSERT groups ON");
                sql.execute("SET IDENTITY_INSERT groups OFF");
            } finally { connection.rollback(); }
        }
    }

    @Test void failingToResetIdentityEvictsTheCheckedOutConnection() throws SQLException {
        var connection = mock(Connection.class);
        var session = mock(Statement.class);
        var pool = mock(HikariDataSource.class);
        var db = mock(DBPool.class);
        when(connection.createStatement()).thenReturn(session);
        when(db.getDataSource("iwcm")).thenReturn(pool);
        when(pool.isWrapperFor(HikariDataSource.class)).thenReturn(true);
        when(pool.unwrap(HikariDataSource.class)).thenReturn(pool);
        var reset = new SQLException("Lost response to OFF");
        when(session.execute("SET IDENTITY_INSERT documents OFF")).thenThrow(reset);
        var insert = new SQLException("Insert failed");
        when(connection.prepareStatement(anyString())).thenThrow(insert);
        try (var lookup = mockStatic(DBPool.class)) {
            lookup.when(DBPool::getInstance).thenReturn(db);
            assertSame(insert, assertThrows(SQLException.class,
                () -> BranchRowWriter.insertReserved(connection,BranchType.DOCUMENT,Map.of("doc_id",1))));
            assertArrayEquals(new Throwable[]{reset}, insert.getSuppressed());
            verify(pool).evictConnection(connection);
        }
    }

    @Test void anUnrecoverableIdentitySessionIsPhysicallyClosed() throws Exception {
        try (var connection = DBPool.getConnection()) {
            Connection physical = connection.unwrap(Connection.class);
            Statement session = connection.createStatement();
            try (session) { session.execute("SET IDENTITY_INSERT documents ON"); }

            // A closed statement cannot deliver OFF. The next borrower must never receive this session.
            assertThrows(SQLException.class, () -> BranchRowWriter.resetIdentity(connection,session,"documents"));
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while (!physical.isClosed() && System.nanoTime() < deadline) Thread.sleep(10);
            assertTrue(physical.isClosed());
        }
    }

    @Test void publicationLocksBlockLiveEditsUntilRollback() throws SQLException {
        try (var publisher = DBPool.getConnection(); var editor = DBPool.getConnection();
             var settings = editor.createStatement(); var change = editor.prepareStatement("UPDATE documents SET title=? WHERE doc_id=?")) {
            int doc;
            try (var query = publisher.createStatement(); var row = query.executeQuery("SELECT TOP (1) doc_id FROM documents ORDER BY doc_id")) {
                assertTrue(row.next());
                doc = row.getInt(1);
            }
            publisher.setAutoCommit(false);
            editor.setAutoCommit(false);
            try {
                BranchPublishRows.lockLive(publisher,BranchType.DOCUMENT,Map.of("doc_id",doc));
                change.setString(1,"autotest competing LIVE save");
                change.setInt(2,doc);
                settings.execute("SET LOCK_TIMEOUT 300");
                assertEquals(1222, assertThrows(SQLException.class, change::executeUpdate).getErrorCode());
                publisher.rollback();
                assertEquals(1,change.executeUpdate());
            } finally {
                editor.rollback();
                publisher.rollback();
                settings.execute("SET LOCK_TIMEOUT -1");
            }
        }
    }
}
