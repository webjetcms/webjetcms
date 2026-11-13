package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Constants;

/**
 * Opt-in P01 probe against the disposable MariaDB CI schema, not a content writer.
 * Run the two methods in separate test JVMs, restarting the disposable DB between them.
 * This proves JDBC allocation behavior; it does not prove DBPool, JPA or cache isolation.
 */
@EnabledIfEnvironmentVariable(named = "WEBJET_P01_JDBC_URL", matches = ".+")
class BranchIdPocTest
{
    private static final String EXPECTED_URL = "jdbc:mariadb://127.0.0.1:33316/webjet_p01";

    /**
     * Verifies the central guard while preserving the legacy flag-off dirty read.
     */
    @Test
    void coreIsolationGuardSeparatesFlagOffAndOn() throws Exception
    {
        boolean original = Constants.getBoolean("branchesEnabled");
        boolean originalExternalDirectories = Constants.getBoolean("enableStaticFilesExternalDir");
        try (Connection writer = connect(); Connection reader = connect())
        {
            Constants.setBoolean("enableStaticFilesExternalDir", true);
            writer.setAutoCommit(false);
            try
            {
                int id = insertNative(writer, "documents");
                Constants.setBoolean("branchesEnabled", false);
                DBPool.setTransactionIsolationReadCommited(reader);
                assertEquals(0, countId(reader, "documents", id));
                DBPool.setTransactionIsolationReadUNCommited(reader);
                assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, reader.getTransactionIsolation());
                assertEquals(1, countId(reader, "documents", id));
                Constants.setBoolean("branchesEnabled", true);
                DBPool.setTransactionIsolationReadUNCommited(reader);
                assertEquals(Connection.TRANSACTION_READ_COMMITTED, reader.getTransactionIsolation());
                assertEquals(0, countId(reader, "documents", id));
                System.out.println("P01 core guard PASS: flag-off preserves dirty reads; flag-on hides the technical row");
            }
            finally
            {
                writer.rollback();
            }
        }
        finally
        {
            Constants.setBoolean("branchesEnabled", original);
            Constants.setBoolean("enableStaticFilesExternalDir", originalExternalDirectories);
        }
    }

    /**
     * Commits reservations without live placeholder rows and records the restart baseline.
     * A fresh CI database is required when rerunning this phase.
     */
    @Test
    void beforeDatabaseRestart() throws Exception
    {
        try (Connection connection = connect())
        {
            execute(connection, "CREATE TABLE IF NOT EXISTS p01_id_reservations ("
                + "table_name VARCHAR(32) NOT NULL, case_name VARCHAR(32) NOT NULL, object_id INT NOT NULL, "
                + "PRIMARY KEY(table_name, case_name), UNIQUE(table_name, object_id)) ENGINE=InnoDB");
            execute(connection, "CREATE TABLE IF NOT EXISTS p01_id_restart (uptime BIGINT NOT NULL) ENGINE=InnoDB");
            assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM p01_id_reservations"),
                "Recreate the disposable CI database, including scratch tables, before repeating phase one");

            for (String table : new String[] {"documents", "groups"})
            {
                assertInnoDb(connection, table);
                long baseline = scalar(connection, "SELECT COUNT(*) FROM `" + table + "`");
                int[] capturedId = new int[1];
                int lower = reserve(table, "lower", true, capturedId);
                int higher = reserve(table, "higher", false, capturedId);
                assertTrue(higher > lower);
                assertEquals(0, countId(connection, table, lower));
                assertEquals(0, countId(connection, table, higher));
                assertTrue(reservation(connection, table, "concurrent") > lower);

                SQLException failure = assertThrows(SQLException.class,
                    () -> reserve(table, "lower", false, capturedId));
                assertTrue(failure.getSQLState().startsWith("23"), "Expected duplicate reservation failure");
                int failedId = capturedId[0];
                assertEquals(0, countId(connection, table, failedId));
                assertEquals(lower, reservation(connection, table, "lower"));
                int afterFailure = insertNative(connection, table);
                assertTrue(afterFailure > failedId);
                remember(connection, table, "after_failure", afterFailure);
                assertEquals(baseline + 2, scalar(connection, "SELECT COUNT(*) FROM `" + table + "`"));
                System.out.printf("P01 %s reserved=%d,%d concurrent=%d failed=%d next=%d; no live placeholders%n",
                    table, lower, higher, reservation(connection, table, "concurrent"), failedId, afterFailure);
            }

            execute(connection, "DELETE FROM p01_id_restart");
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO p01_id_restart VALUES (?)"))
            {
                statement.setLong(1, uptime(connection));
                statement.executeUpdate();
            }
            System.out.printf("P01 profile: %s / %s; restart baseline uptime=%d%n",
                connection.getMetaData().getDatabaseProductVersion(), connection.getMetaData().getDriverVersion(),
                uptime(connection));
        }
    }

    /**
     * Reloads reservations after DB/JVM restart, inserts higher IDs before lower IDs,
     * and verifies that explicit inserts cannot overwrite an existing identity.
     */
    @Test
    void afterDatabaseRestart() throws Exception
    {
        try (Connection connection = connect())
        {
            long beforeUptime = scalar(connection, "SELECT uptime FROM p01_id_restart");
            assertTrue(uptime(connection) < beforeUptime,
                "Restart the disposable database and run this method promptly in a new test JVM");
            for (String table : new String[] {"documents", "groups"})
            {
                int lower = reservation(connection, table, "lower");
                int higher = reservation(connection, table, "higher");
                int beforeRestart = reservation(connection, table, "after_failure");
                assertEquals(0, countId(connection, table, lower));
                assertEquals(0, countId(connection, table, higher));
                int firstNative = insertNative(connection, table);
                assertTrue(firstNative > beforeRestart);
                assertNotEquals(lower, firstNative);
                assertNotEquals(higher, firstNative);
                insertExplicit(connection, table, higher);
                insertExplicit(connection, table, lower);
                assertEquals(1, countId(connection, table, higher));
                assertEquals(1, countId(connection, table, lower));
                SQLException duplicate = assertThrows(SQLException.class, () -> insertExplicit(connection, table, lower));
                assertTrue(duplicate.getSQLState().startsWith("23"));
                int nextNative = insertNative(connection, table);
                assertTrue(nextNative > firstNative);
                System.out.printf("P01 %s after DB/JVM restart: native=%d, explicit=%d then %d, next native=%d%n",
                    table, firstNative, higher, lower, nextNative);
            }
        }
    }

    private static Connection connect() throws SQLException
    {
        String url = System.getenv("WEBJET_P01_JDBC_URL");
        if (!EXPECTED_URL.equals(url))
        {
            throw new IllegalArgumentException("P01 writes are restricted to " + EXPECTED_URL);
        }
        Connection connection = DriverManager.getConnection(url,
            System.getenv().getOrDefault("WEBJET_P01_JDBC_USER", "root"),
            System.getenv().getOrDefault("WEBJET_P01_JDBC_PASSWORD", ""));
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        return connection;
    }

    private static int reserve(String table, String label, boolean probeVisibility, int[] capturedId) throws Exception
    {
        try (Connection writer = connect())
        {
            writer.setAutoCommit(false);
            try
            {
                int id = insertNative(writer, table);
                capturedId[0] = id;
                if (probeVisibility)
                {
                    probeUncommittedRow(table, id);
                }
                try (PreparedStatement statement = writer.prepareStatement("DELETE FROM `" + table + "` WHERE " + key(table) + "=?"))
                {
                    statement.setInt(1, id);
                    assertEquals(1, statement.executeUpdate());
                }
                remember(writer, table, label, id);
                writer.commit();
                return id;
            }
            catch (Exception failure)
            {
                writer.rollback();
                throw failure;
            }
        }
    }

    private static void probeUncommittedRow(String table, int id) throws Exception
    {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try
        {
            Future<Integer> concurrent = executor.submit(() -> {
                try (Connection connection = connect())
                {
                    int normalId = insertNative(connection, table);
                    remember(connection, table, "concurrent", normalId);
                    return normalId;
                }
            });
            try (Connection reader = connect())
            {
                assertEquals(0, countId(reader, table, id), "READ_COMMITTED must hide the technical row");
                reader.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                assertEquals(1, countId(reader, table, id), "Positive control must observe the dirty row");
                reader.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                assertEquals(0, countId(reader, table, id));
            }
            assertNotEquals(id, concurrent.get(10, TimeUnit.SECONDS));
        }
        finally
        {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static int insertNative(Connection connection, String table) throws SQLException
    {
        String sql = "documents".equals(table)
            ? "INSERT INTO documents (title,data,data_asc,navbar) VALUES ('P01 ID probe','','','P01 ID probe')"
            : "INSERT INTO `groups` (group_name) VALUES ('P01 ID probe')";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            assertEquals(1, statement.executeUpdate());
            try (ResultSet keys = statement.getGeneratedKeys())
            {
                assertTrue(keys.next());
                long id = keys.getLong(1);
                assertTrue(id > 0 && id <= Integer.MAX_VALUE);
                assertFalse(keys.next());
                return (int) id;
            }
        }
    }

    private static void insertExplicit(Connection connection, String table, int id) throws SQLException
    {
        String sql = "documents".equals(table)
            ? "INSERT INTO documents (doc_id,title,data,data_asc,navbar) VALUES (?,'P01 explicit','','','P01 explicit')"
            : "INSERT INTO `groups` (group_id,group_name) VALUES (?,'P01 explicit')";
        try (PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, id);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static void remember(Connection connection, String table, String label, int id) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO p01_id_reservations VALUES (?,?,?)"))
        {
            statement.setString(1, table);
            statement.setString(2, label);
            statement.setInt(3, id);
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static int reservation(Connection connection, String table, String label) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT object_id FROM p01_id_reservations WHERE table_name=? AND case_name=?"))
        {
            statement.setString(1, table);
            statement.setString(2, label);
            try (ResultSet result = statement.executeQuery())
            {
                assertTrue(result.next());
                int id = result.getInt(1);
                assertFalse(result.next());
                return id;
            }
        }
    }

    private static long countId(Connection connection, String table, int id) throws SQLException
    {
        return scalar(connection, "SELECT COUNT(*) FROM `" + table + "` WHERE " + key(table) + "=" + id);
    }

    private static String key(String table)
    {
        return "documents".equals(table) ? "doc_id" : "group_id";
    }

    private static long uptime(Connection connection) throws SQLException
    {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SHOW GLOBAL STATUS LIKE 'Uptime'"))
        {
            assertTrue(result.next());
            return result.getLong(2);
        }
    }

    private static void assertInnoDb(Connection connection, String table) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT engine FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?"))
        {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery())
            {
                assertTrue(result.next());
                assertEquals("InnoDB", result.getString(1));
            }
        }
    }

    private static long scalar(Connection connection, String sql) throws SQLException
    {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
        {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException
    {
        try (Statement statement = connection.createStatement())
        {
            statement.execute(sql);
        }
    }
}
