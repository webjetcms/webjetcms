package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Activation checks on the isolated PostgreSQL server with its fixture owner. */
@EnabledIfEnvironmentVariable(named = "WEBJET_PG16_DB", matches = "true")
class BranchPostgresReadinessTest extends BaseWebjetTest {
    @TempDir Path storage;
    private Map<String,String> original;

    @BeforeEach void setup() throws SQLException {
        original = Map.of("branchesEnabled", Constants.getString("branchesEnabled"),
                "enableStaticFilesExternalDir", Constants.getString("enableStaticFilesExternalDir"),
                "branchStorageRoot", Constants.getString("branchStorageRoot"));
        try (var connection = DBPool.getConnection()) {
            BranchTestDomains.requireIsolatedDatabase(connection);
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
        }
        Constants.setBoolean("branchesEnabled", true);
        Constants.setBoolean("enableStaticFilesExternalDir", true);
        Constants.setString("branchStorageRoot", storage.toString());
    }

    @AfterEach void restore() { original.forEach(Constants::setString); }

    @Test void rejectsFallbackSchemaAndWeakIsolationOnEitherConnection() throws SQLException {
        try (var jdbc = DBPool.getConnection(); var jpa = DBPool.getInstance().getDataSource("iwcm").getConnection()) {
            BranchReadiness.verifyActivation(jdbc);
            jpa.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            assertThrows(SQLException.class, () -> BranchReadiness.verifyJpaConnection(jdbc, jpa));
            jpa.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            jpa.setSchema("public");
            assertThrows(SQLException.class, () -> BranchReadiness.verifyJpaConnection(jdbc, jpa));
            jdbc.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            assertThrows(SQLException.class, () -> BranchReadiness.verify(jdbc));
            jdbc.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            jdbc.setAutoCommit(false);
            try (var statement = jdbc.createStatement()) {
                statement.execute("SET LOCAL search_path TO public, webjet_cms");
                assertThrows(SQLException.class, () -> BranchReadiness.verify(jdbc));
            } finally { jdbc.rollback(); }
        }
    }

    @Test void rejectsMissingSequenceOrTableWriteGrants() throws SQLException {
        try (var connection = DBPool.getConnection(); var statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                String role = "autotest_branch_" + Long.toUnsignedString(System.nanoTime());
                statement.execute("CREATE ROLE " + role);
                statement.execute("GRANT pg_read_all_data TO " + role);
                statement.execute("SET LOCAL ROLE " + role);
                var missing = assertThrows(SQLException.class, () -> BranchReadiness.verifyWritePrivileges(connection));
                assertTrue(missing.getMessage().contains("sequence privileges"), missing.getMessage());
                statement.execute("RESET ROLE");
                statement.execute("GRANT USAGE ON ALL SEQUENCES IN SCHEMA webjet_cms TO " + role);
                statement.execute("SET LOCAL ROLE " + role);
                assertEquals("42501", assertThrows(SQLException.class,
                    () -> BranchReadiness.verifyWritePrivileges(connection)).getSQLState());
            } finally { connection.rollback(); }
        }
    }
}
