package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Real engine upgrades and allocator calls in disposable schemas on the configured MariaDB server. */
@EnabledIfEnvironmentVariable(named = "WEBJET_BRANCH_ENGINE_DB", matches = "true")
class BranchEngineMigrationTest extends BaseWebjetTest {
    private static final List<String> CONTENT_TABLES = List.of("documents", "groups", "media",
            "media_group_to_media", "documents_history", "groups_scheduler");
    private static final List<String> BRANCH_TABLES = List.of("branch_files", "branch_media", "branch_groups",
            "branch_documents", "branches");
    private static final String GENERATOR_CONTEXT = "sk.iway.iwcm.PkeyGenerator";
    @TempDir Path storage;
    private HikariDataSource datasource;
    private String schema;
    private Object originalGenerator;
    private Map<String, String> originalConstants;
    private MockedStatic<DBPool> database;
    private MockedStatic<BranchDomains> domains;

    @BeforeEach void setup() throws Exception {
        originalGenerator = Constants.getServletContext().getAttribute(GENERATOR_CONTEXT);
        originalConstants = Map.of("branchesEnabled", Constants.getString("branchesEnabled"),
                "enableStaticFilesExternalDir", Constants.getString("enableStaticFilesExternalDir"),
                "branchStorageRoot", Constants.getString("branchStorageRoot"),
                "pkeyGenBlockSize", Constants.getString("pkeyGenBlockSize"));
        datasource = DBPool.getInstance().getDataSource("iwcm").unwrap(HikariDataSource.class);
        try (Connection connection = datasource.getConnection()) {
            assertEquals("MariaDB", connection.getMetaData().getDatabaseProductName());
            String source = quoted(connection.getCatalog());
            schema = "autotest_branch_engine_" + Long.toUnsignedString(System.nanoTime());
            execute(connection, "CREATE DATABASE " + quoted(schema));
            for (String table : CONTENT_TABLES)
                execute(connection, "CREATE TABLE " + quoted(schema) + ".`" + table + "` LIKE " + source + ".`" + table + "`");
            execute(connection, "CREATE TABLE " + quoted(schema) + ".pkey_generator LIKE " + source + ".pkey_generator");
        }
        try (Connection connection = open()) {
            for (String sql : BranchStorageTest.migration().getMysql().split(";"))
                if (sql.stripLeading().startsWith("CREATE TABLE")) execute(connection, sql);
            for (String table : CONTENT_TABLES) execute(connection, "ALTER TABLE `" + table + "` ENGINE=MyISAM");
            execute(connection, "ALTER TABLE pkey_generator ENGINE=MyISAM");
            execute(connection, "INSERT INTO pkey_generator(name,value,table_name,table_pkey_name) VALUES ('media',1,'media','media_id')");
            execute(connection, "INSERT INTO branches(id,domain_id,name,state,created_by,updated_by,created_at,updated_at) VALUES (1,1,'autotest engine','DRAFT',1,1,NOW(),NOW())");
        }
        Constants.setBoolean("branchesEnabled", true);
        Constants.setBoolean("enableStaticFilesExternalDir", true);
        Constants.setString("branchStorageRoot", storage.toString());
        Constants.setInt("pkeyGenBlockSize", 3);
        Constants.getServletContext().removeAttribute(GENERATOR_CONTEXT);
        database = mockStatic(DBPool.class);
        database.when(DBPool::getConnection).thenAnswer(invocation -> open());
        database.when(() -> DBPool.getConnection("iwcm")).thenAnswer(invocation -> open());
        domains = mockStatic(BranchDomains.class);
        domains.when(() -> BranchDomains.id("autotest.engine")).thenReturn(1);
    }

    @AfterEach void cleanup() throws Exception {
        if (domains != null) domains.close();
        if (database != null) database.close();
        Constants.getServletContext().removeAttribute(GENERATOR_CONTEXT);
        if (originalGenerator != null) Constants.getServletContext().setAttribute(GENERATOR_CONTEXT, originalGenerator);
        if (originalConstants != null) originalConstants.forEach(Constants::setString);
        if (schema != null) try (Connection connection = datasource.getConnection()) {
            execute(connection, "DROP DATABASE " + quoted(schema));
        }
    }

    /** Creates fresh branch tables, upgrades LIVE content, and preserves existing data on a retry. */
    @Test void migratesContentAndPreservesFulltext() throws Exception {
        try (Connection connection = open()) {
            for (String table : BRANCH_TABLES)
                execute(connection, "DROP TABLE `" + table + "`");
            execute(connection, "INSERT INTO documents(title,data,data_asc,navbar) VALUES ('autotest publishing','pracovna verzia publikovanie','pracovna verzia publikovanie','autotest')");
            execute(connection, "INSERT INTO `groups`(group_id,group_name) VALUES (100,'autotest original')");
            applyMigration(connection);
            BranchStorageTest.assertColumns(connection);
            execute(connection, "INSERT INTO branches(id,domain_id,name,state,created_by,updated_by,created_at,updated_at) VALUES (1,1,'autotest engine','DRAFT',1,1,NOW(),NOW())");
            long alters = alterCount(connection);
            applyMigration(connection);
            assertEquals(alters, alterCount(connection), "A retry must not rebuild existing InnoDB tables");
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM branches WHERE id=1 AND name='autotest engine'"));
            for (String table : CONTENT_TABLES) assertEquals("InnoDB", engine(connection, table));
            for (String table : BRANCH_TABLES) assertEquals("InnoDB", engine(connection, table));
            assertEquals("MyISAM", engine(connection, "pkey_generator"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM documents WHERE MATCH(title,data_asc) AGAINST ('+pracovna +verzia' IN BOOLEAN MODE)"));
            connection.setAutoCommit(false);
            execute(connection, "UPDATE `groups` SET group_name='autotest changed' WHERE group_id=100");
            connection.rollback();
            connection.setAutoCommit(true);
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM `groups` WHERE group_id=100 AND group_name='autotest original'"));
            BranchReadiness.verify(connection);
        }
    }

    /** The public allocator and WORK writes accept MyISAM counters without reusing a failed allocation. */
    @Test void allowsMyisamGeneratorAndKeepsFailedAllocationsConsumed() throws Exception {
        try (Connection connection = open()) {
            for (String table : CONTENT_TABLES) execute(connection, "ALTER TABLE `" + table + "` ENGINE=InnoDB");
            BranchReadiness.verify(connection);
            long first = PkeyGenerator.getNextValueAsLongStrict("media");
            long legacy = PkeyGenerator.getNextValueAsLong("media");
            assertTrue(legacy > first);
            BranchStore store = new BranchStore(this::open);
            BranchContext context = new BranchContext(1, "autotest.engine", 1, BranchContext.Mode.ADMIN);
            Map<String, Object> values = BranchMediaMapper.initialValues(1, 141, 1);
            execute(connection, "CREATE TRIGGER autotest_fail_work BEFORE INSERT ON branch_media FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='autotest WORK failure'");
            assertThrows(SQLException.class, () -> store.create(context, BranchType.MEDIA, values));
            execute(connection, "DROP TRIGGER autotest_fail_work");
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM branch_media"));
            assertEquals(0, count(connection, "SELECT revision FROM branches WHERE id=1"));
            long next = ((Number) store.create(context, BranchType.MEDIA, values).get("media_id")).longValue();
            assertTrue(next > legacy + 1, "The failed WORK insert must consume its ID");
            for (int i = 0; i < 10; i++) {
                long current = PkeyGenerator.getNextValueAsLongStrict("media");
                assertTrue(current > next);next = current;
            }
            PkeyGenerator.getInstance(true);
            assertTrue(PkeyGenerator.getNextValueAsLongStrict("media") > next);
            assertEquals("MyISAM", engine(connection, "pkey_generator"));
            execute(connection, "UPDATE pkey_generator SET table_name='autotest_invalid' WHERE name='media'");
            assertThrows(SQLException.class, () -> BranchReadiness.verify(connection));
            execute(connection, "DELETE FROM pkey_generator WHERE name='media'");
            assertThrows(SQLException.class, () -> PkeyGenerator.getNextValueAsLongStrict("media"));
            assertThrows(SQLException.class, () -> BranchReadiness.verify(connection));
        }
    }

    /** Relaxing the counter requirement does not permit nontransactional production content. */
    @Test void rejectsMyisamContent() throws Exception {
        try (Connection connection = open()) {
            for (String table : CONTENT_TABLES) execute(connection, "ALTER TABLE `" + table + "` ENGINE=InnoDB");
            execute(connection, "ALTER TABLE `groups` ENGINE=MyISAM");
            SQLException failure = assertThrows(SQLException.class, () -> BranchReadiness.verify(connection));
            assertTrue(failure.getMessage().contains("InnoDB: groups"));
        }
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(datasource.getJdbcUrl(), datasource.getUsername(), datasource.getPassword());
        try { connection.setCatalog(schema);connection.setAutoCommit(true);connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);return connection; }
        catch (SQLException ex) { connection.close();throw ex; }
    }

    private static void applyMigration(Connection connection) throws Exception {
        for (String sql : BranchStorageTest.migration().getMysql().split(";")) if (!sql.isBlank()) execute(connection, sql);
    }

    private static String engine(Connection connection, String table) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
            statement.setString(1, table);
            try (var rows = statement.executeQuery()) { assertTrue(rows.next());return rows.getString(1); }
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) { statement.execute(sql); }
    }

    private static long count(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement();var rows = statement.executeQuery(sql)) { assertTrue(rows.next());return rows.getLong(1); }
    }

    private static long alterCount(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();var rows = statement.executeQuery("SHOW SESSION STATUS LIKE 'Com_alter_table'")) {
            assertTrue(rows.next());return rows.getLong(2);
        }
    }

    private static String quoted(String identifier) { return "`" + identifier.replace("`", "``") + "`"; }
}
