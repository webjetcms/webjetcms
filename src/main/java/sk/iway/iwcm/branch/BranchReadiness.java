package sk.iway.iwcm.branch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/** Prerequisites for publishing packages on a single-writer database. */
public final class BranchReadiness {
    public static final String DEFAULT_STORAGE_ROOT = "/WEB-INF/branches";
    private static final Set<String> SUPPORTED_MARIADB_SERIES = Set.of("10.11", "11.4", "12.2");

    private BranchReadiness() { }

    public static void requireEnabled() {
        if (!BranchAccess.isEnabled())
            throw new IllegalStateException("Working versions require branchesEnabled=true and enableStaticFilesExternalDir=true");
    }

    /** Verifies physical storage and schema before a branch transaction can write. */
    public static void verify() throws SQLException {
        try (Connection connection = sk.iway.iwcm.DBPool.getConnection()) { verify(connection); }
    }

    /** Verifies prerequisites using the caller's transaction connection. */
    public static void verify(Connection connection) throws SQLException {
        requireEnabled();
        DatabaseMetaData database = connection.getMetaData();
        String versionSeries = database.getDatabaseMajorVersion() + "." + database.getDatabaseMinorVersion();
        boolean postgres = "PostgreSQL".equals(database.getDatabaseProductName());
        boolean maria = "MariaDB".equals(database.getDatabaseProductName()) && SUPPORTED_MARIADB_SERIES.contains(versionSeries);
        boolean mssql = "Microsoft SQL Server".equals(database.getDatabaseProductName()) && database.getDatabaseMajorVersion() == 16;
        if (!mssql && !maria && !(postgres && database.getDatabaseMajorVersion() >= 16))
            throw new SQLException("Publishing packages require MariaDB 10.11, 11.4 or 12.2, PostgreSQL 16 or later, or Microsoft SQL Server 2022 (16.x)");
        if (connection.isReadOnly() || connection.getTransactionIsolation() < Connection.TRANSACTION_READ_COMMITTED)
            throw new SQLException("Branches require a writable connection at READ_COMMITTED or stronger");
        List<String> tables = new ArrayList<>(List.of("documents", "groups", "media", "media_group_to_media",
            "documents_history", "groups_scheduler", "branches", "branch_documents", "branch_groups", "branch_media", "branch_files"));
        if (postgres || mssql) tables.addAll(List.of("pkey_generator", "perex_group_doc", "_adminlog_", "file_history"));
        String tableCheck = mssql ? "SELECT 1 FROM sys.tables WHERE schema_id=SCHEMA_ID() AND name=?" : postgres
            ? "SELECT 1 FROM pg_class WHERE oid=to_regclass(?) AND relnamespace=current_schema()::regnamespace AND relkind='r' AND relpersistence='p'"
            : "SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND ENGINE='InnoDB'";
        try (PreparedStatement statement = connection.prepareStatement(tableCheck)) {
            for (String table : tables) {
                statement.setString(1, table);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new SQLException(postgres || mssql ? "Missing permanent table in current schema: " + table
                        : "Publishing branches require InnoDB: " + table);
                }
            }
        }
        if (postgres) verifyPostgresWriter(connection);
        if (mssql) sqlServerWriter(connection);
        String currentSchema = mssql ? "SCHEMA_NAME()" : postgres ? "current_schema()" : "DATABASE()";
        for (BranchType type : List.of(BranchType.DOCUMENT, BranchType.GROUP, BranchType.MEDIA)) {
            Set<String> actual = new LinkedHashSet<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=" + currentSchema + " AND TABLE_NAME=?")) {
                statement.setString(1, type.liveTable());
                try (ResultSet result = statement.executeQuery()) { while (result.next()) actual.add(result.getString(1)); }
            }
            BranchFields.forType(type).verifyColumns(actual);
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT value,table_name,table_pkey_name FROM pkey_generator WHERE name='media'");
             ResultSet result = statement.executeQuery()) {
            if (!result.next() || !"media".equals(result.getString(2)) || !"media_id".equals(result.getString(3)) || result.getLong(1) < 0)
                throw new SQLException("Missing or invalid media generator; initialize the existing LIVE generator before enabling branches");
        }
        try { privateRoot(); }
        catch (IOException | IllegalArgumentException ex) { throw new SQLException("Private branch storage is unavailable", ex); }
    }

    /** Verifies branch prerequisites explicitly, without coupling them to server startup. */
    public static void verifyActivation() throws SQLException {
        try (Connection connection = sk.iway.iwcm.DBPool.getConnection()) {
            verifyActivation(connection);
        }
    }

    /** Runs the full preflight before an administrative operation creates a package. */
    static void verifyActivation(Connection connection) throws SQLException {
        verify(connection);
        connection.setAutoCommit(false);
        try { verifyWritePrivileges(connection); }
        finally {
            if (!connection.isClosed()) {
                try { connection.rollback(); }
                catch (SQLException ex) { try { connection.abort(Runnable::run); } catch (SQLException abort) { ex.addSuppressed(abort); } throw ex; }
                connection.setAutoCommit(true);
            }
        }
        try (Connection jpa = sk.iway.iwcm.DBPool.getInstance().getDataSource("iwcm").getConnection()) {
            verifyJpaConnection(connection, jpa);
        }
    }

    static void verifyJpaConnection(Connection jdbc, Connection jpa) throws SQLException {
        if (jpa.isReadOnly() || jpa.getTransactionIsolation() < Connection.TRANSACTION_READ_COMMITTED)
            throw new SQLException("Direct JPA datasource must be writable at READ_COMMITTED or stronger");
        if ("Microsoft SQL Server".equals(jdbc.getMetaData().getDatabaseProductName())) {
            if (!sqlServerWriter(jdbc).equals(sqlServerWriter(jpa)))
                throw new SQLException("JDBC and JPA must use the same SQL Server, database, schema and user");
        }
        if ("PostgreSQL".equals(jdbc.getMetaData().getDatabaseProductName())) {
            if (!jdbc.getSchema().equals(jpa.getSchema()) || !jdbc.getCatalog().equals(jpa.getCatalog()))
                throw new SQLException("JDBC and JPA must use the same PostgreSQL database and schema");
            verifyPostgresWriter(jpa);
        }
    }

    private static List<String> sqlServerWriter(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("""
                SELECT SERVERPROPERTY('ServerName'),DB_NAME(),SCHEMA_NAME(),CURRENT_USER
                FROM sys.databases WHERE database_id=DB_ID() AND is_read_only=0 AND compatibility_level>=130
                AND DATABASEPROPERTYEX(DB_NAME(),'Updateability')='READ_WRITE'
                """)) {
            if (!rows.next()) throw new SQLException("Publishing packages require a writable SQL Server database at compatibility level 130 or higher");
            return List.of(rows.getString(1), rows.getString(2), rows.getString(3), rows.getString(4));
        }
    }

    private static void verifySqlServerPrivileges(Connection connection) throws SQLException {
        String check = "SELECT HAS_PERMS_BY_NAME(QUOTENAME(SCHEMA_NAME())+'.'+QUOTENAME(?),'OBJECT',?)";
        try (var statement = connection.prepareStatement(check)) {
            for (String table : List.of("documents","groups","media","pkey_generator","media_group_to_media","perex_group_doc",
                    "branches","branch_documents","branch_groups","branch_media","branch_files","documents_history","groups_scheduler","_adminlog_","file_history")) {
                List<String> permissions = switch (table) {
                    case "_adminlog_" -> List.of("INSERT");
                    case "file_history", "groups_scheduler" -> List.of("SELECT","INSERT");
                    case "documents_history", "pkey_generator" -> List.of("SELECT","INSERT","UPDATE");
                    case "documents", "groups" -> List.of("SELECT","INSERT","UPDATE","DELETE","ALTER");
                    default -> List.of("SELECT","INSERT","UPDATE","DELETE");
                };
                for (String permission : permissions) {
                    statement.setString(1,table);
                    statement.setString(2,permission);
                    try (var rows = statement.executeQuery()) {
                        if (!rows.next() || rows.getInt(1) != 1) throw new SQLException("Missing " + permission + " on " + table);
                    }
                }
            }
        }
        // Resolve the actual WORK projection and exercise long Unicode parameters with this driver.
        for (BranchType type : List.of(BranchType.DOCUMENT, BranchType.GROUP, BranchType.MEDIA)) {
            String columns = String.join(",", BranchFields.forType(type).columns());
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT TOP (0) " + columns + " FROM " + type.workTable())) {
                if (rows.getMetaData().isAutoIncrement(rows.findColumn(type.idColumn())))
                    throw new SQLException("WORK content IDs must use their reserved LIVE values: " + type.workTable());
            }
        }
        String sample = "Ž漢🐴".repeat(1200);
        try (var statement = connection.prepareStatement("SELECT sample FROM OPENJSON(CAST(? AS nvarchar(max))) WITH (sample nvarchar(max))")) {
            statement.setString(1,"{\"sample\":\"" + sample + "\"}");
            try (var rows = statement.executeQuery()) {
                if (!rows.next() || !sample.equals(rows.getString(1))) throw new SQLException("The JDBC driver must preserve long Unicode parameters");
            }
        }
        for (BranchType type : List.of(BranchType.DOCUMENT, BranchType.GROUP)) {
            int id = BranchStore.reserveNative(connection,type);
            Map<String,Object> probe = type == BranchType.DOCUMENT ? BranchDocumentMapper.initialValues(id,1,java.time.Instant.now(),false)
                : BranchGroupMapper.initialValues(id,"default");
            BranchRowWriter.insertReserved(connection,type,probe);
        }
    }

    private static void verifyPostgresWriter(Connection connection) throws SQLException {
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT NOT pg_is_in_recovery() AND NOT current_setting('transaction_read_only')::boolean");
             ResultSet result = check.executeQuery()) {
            if (!result.next() || !result.getBoolean(1)) throw new SQLException("Publishing requires a writable PostgreSQL primary");
        }
    }

    /** Resolves the protected default directory or an explicit absolute filesystem override. */
    static Path storageRoot() {
        String configured = Constants.getString("branchStorageRoot");
        if (configured == null || configured.isBlank() || DEFAULT_STORAGE_ROOT.equals(configured))
            return Path.of(Tools.getRealPath(DEFAULT_STORAGE_ROOT)).toAbsolutePath().normalize();
        Path root = Path.of(configured);
        if (!root.isAbsolute()) throw new IllegalArgumentException("branchStorageRoot must be an absolute filesystem path");
        return root.normalize();
    }

    /** Creates protected default storage and verifies that private bytes cannot enter a public directory. */
    public static Path privateRoot() throws IOException {
        String configured = Constants.getString("branchStorageRoot");
        boolean useDefault = configured == null || configured.isBlank() || DEFAULT_STORAGE_ROOT.equals(configured);
        String served = Tools.getRealPath("/");
        if (served == null || served.isBlank()) throw new IOException("Cannot verify the served web root");
        Path webRoot = Path.of(served).toRealPath();
        Path protectedRoot = webRoot.resolve("WEB-INF");
        Path directory = storageRoot();
        if (useDefault) {
            if (!directory.getParent().toRealPath().equals(protectedRoot))
                throw new IOException("Default branch storage must remain inside WEB-INF");
            Files.createDirectories(directory);
        }
        Path root = directory.toRealPath();
        boolean protectedStorage = root.startsWith(protectedRoot) && !root.equals(protectedRoot);
        if ((useDefault && !protectedStorage) || webRoot.startsWith(root)
                || (root.startsWith(webRoot) && !protectedStorage) || !Files.isDirectory(root))
            throw new IOException("Branch storage must be inside WEB-INF or outside the served application");
        Path probe = Files.createTempFile(root, ".branch-probe-", ".tmp");
        try { Files.writeString(probe, "branch storage probe"); }
        finally { Files.deleteIfExists(probe); }
        return root;
    }

    /** Checks actual DML grants without publishing content; the caller must roll back. */
    public static void verifyWritePrivileges(Connection connection) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Privilege probe requires an explicit rollback transaction");
        if ("Microsoft SQL Server".equals(connection.getMetaData().getDatabaseProductName())) {
            verifySqlServerPrivileges(connection);
            return;
        }
        if ("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
            verifyPostgresPrivileges(connection);
        }
        for (BranchType type : List.of(BranchType.DOCUMENT, BranchType.GROUP)) {
            int id = BranchStore.reserveNative(connection, type);
            // INSERT with the already generated identity is a distinct required grant/contract.
            String sql = type == BranchType.DOCUMENT
                ? "INSERT INTO documents(doc_id,title,data,data_asc,navbar) VALUES (?,'','','','')"
                : "INSERT INTO groups(group_id,group_name) VALUES (?,'')";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id); statement.executeUpdate();
            }
        }
        for (String table : List.of("documents", "groups", "pkey_generator", "media", "media_group_to_media", "documents_history", "groups_scheduler",
                "perex_group_doc", "branches", "branch_documents", "branch_groups", "branch_media", "branch_files")) {
            String column;
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE 1=0");
                 ResultSet result = statement.executeQuery()) { column = result.getMetaData().getColumnName(1); }
            try (PreparedStatement statement = connection.prepareStatement("UPDATE " + table + " SET " + column + "=" + column + " WHERE 1=0")) { statement.executeUpdate(); }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + " WHERE 1=0")) { statement.executeUpdate(); }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + " SELECT * FROM " + table + " WHERE 1=0")) { statement.executeUpdate(); }
        }
    }

    private static void verifyPostgresPrivileges(Connection connection) throws SQLException {
        String sql = """
            SELECT table_name, column_name FROM information_schema.columns
            WHERE table_schema=current_schema() AND table_name IN
                ('documents','groups','branches','branch_documents','branch_groups','branch_media','branch_files',
                 'documents_history','groups_scheduler','media_group_to_media','perex_group_doc','_adminlog_','file_history')
              AND pg_get_serial_sequence(format('%I.%I', table_schema, table_name),column_name) IS NOT NULL
              AND NOT has_sequence_privilege(pg_get_serial_sequence(format('%I.%I', table_schema, table_name),column_name),'USAGE,UPDATE')
            """;
        try (PreparedStatement check = connection.prepareStatement(sql); ResultSet result = check.executeQuery()) {
            if (result.next()) throw new SQLException("Missing sequence privileges for " + result.getString(1) + "." + result.getString(2));
        }
        for (BranchType type : List.of(BranchType.DOCUMENT, BranchType.GROUP, BranchType.MEDIA)) {
            String columns = String.join(",", BranchFields.forType(type).columns());
            String union = "SELECT " + columns + " FROM " + type.liveTable() + " WHERE false UNION ALL SELECT "
                + columns + " FROM " + type.workTable() + " WHERE false";
            // PostgreSQL resolves the types of all columns even in an empty projection.
            try (PreparedStatement check = connection.prepareStatement(union)) { check.executeQuery().close(); }
        }
        for (String table : List.of("_adminlog_", "file_history")) {
            try (PreparedStatement check = connection.prepareStatement("INSERT INTO " + table + " SELECT * FROM " + table + " WHERE false")) {
                check.executeUpdate();
            }
        }
        try (PreparedStatement check = connection.prepareStatement("UPDATE pkey_generator SET value=value WHERE false")) {
            check.executeUpdate();
        }
    }
}
