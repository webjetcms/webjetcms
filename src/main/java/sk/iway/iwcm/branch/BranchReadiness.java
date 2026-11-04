package sk.iway.iwcm.branch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.ConfDB;

/** Fail-closed prerequisites for the single-writer MariaDB POC. No engine conversion. */
public final class BranchReadiness {
    private BranchReadiness() { }

    public static void requireEnabled() {
        if (!Constants.getBoolean("branchesEnabled")) throw new IllegalStateException("Publishing branches are disabled");
        if (!Constants.getBoolean("branchSingleWriterConfirmed"))
            throw new IllegalStateException("Confirm the same branch configuration on all content nodes and a single writable database");
    }

    /** Verifies physical storage and schema before a branch transaction can write. */
    public static void verify(Connection connection) throws SQLException {
        requireEnabled();
        if (!"MariaDB".equals(connection.getMetaData().getDatabaseProductName())
                || connection.getMetaData().getDatabaseMajorVersion() != 12
                || connection.getMetaData().getDatabaseMinorVersion() != 2)
            throw new SQLException("Only the verified MariaDB 12.2 single-writer POC profile is supported");
        if (connection.isReadOnly() || connection.getTransactionIsolation() < Connection.TRANSACTION_READ_COMMITTED)
            throw new SQLException("Branches require a writable connection at READ_COMMITTED or stronger");
        String audit = ConfDB.ADMINLOG_TABLE_NAME;
        if (audit == null || !audit.matches("[a-zA-Z0-9_]+")) throw new SQLException("Invalid configured audit table");
        Set<String> tables = new LinkedHashSet<>(List.of("documents", "groups", "media", "pkey_generator",
            "media_group_to_media", "documents_history", "groups_scheduler", "perex_group_doc", audit,
            "branches", "branch_documents", "branch_groups", "branch_media", "branch_files"));
        for (String table : tables) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
                statement.setString(1, table);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next() || !"InnoDB".equalsIgnoreCase(result.getString(1)))
                        throw new SQLException("Publishing branches require InnoDB: " + table);
                }
            }
        }
        for (BranchType type : List.of(BranchType.DOCUMENT, BranchType.GROUP, BranchType.MEDIA)) {
            Set<String> actual = new LinkedHashSet<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
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
            try { connection.rollback(); }
            catch (SQLException ex) { try { connection.abort(Runnable::run); } catch (SQLException abort) { ex.addSuppressed(abort); } throw ex; }
            connection.setAutoCommit(true);
        }
        try (Connection jpa = sk.iway.iwcm.DBPool.getInstance().getDataSource("iwcm").getConnection()) {
            if (jpa.isReadOnly() || jpa.getTransactionIsolation() < Connection.TRANSACTION_READ_COMMITTED)
                throw new SQLException("Direct JPA datasource must use at least READ_COMMITTED");
        }
    }

    /** Requires a preconfigured private directory, rejecting symlinks into the served application. */
    public static Path privateRoot() throws IOException {
        String configured = Constants.getString("branchStorageRoot");
        if (configured == null || configured.isBlank() || !Path.of(configured).isAbsolute())
            throw new IOException("branchStorageRoot must be an absolute existing private directory");
        Path root = Path.of(configured).toRealPath();
        String served = Tools.getRealPath("/");
        if (served == null || served.isBlank()) throw new IOException("Cannot verify the served web root");
        Path webRoot = Path.of(served).toRealPath();
        if (root.startsWith(webRoot) || webRoot.startsWith(root) || !Files.isDirectory(root))
            throw new IOException("branchStorageRoot must be separate from the served application");
        Path probe = Files.createTempFile(root, ".branch-probe-", ".tmp");
        try { Files.writeString(probe, "branch storage probe"); }
        finally { Files.deleteIfExists(probe); }
        return root;
    }

    /** Checks actual DML grants without publishing content; the caller must roll back. */
    public static void verifyWritePrivileges(Connection connection) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Privilege probe requires an explicit rollback transaction");
        for (BranchType type : List.of(BranchType.DOCUMENT, BranchType.GROUP)) {
            int id = BranchStore.reserveNative(connection, type);
            // INSERT with the already generated identity is a distinct required grant/contract.
            String sql = type == BranchType.DOCUMENT
                ? "INSERT INTO documents(doc_id,title,data,data_asc,navbar) VALUES (?,'','','','')"
                : "INSERT INTO `groups`(group_id,group_name) VALUES (?,'')";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id); statement.executeUpdate();
            }
        }
        for (String table : List.of("documents", "groups", "pkey_generator", "media", "media_group_to_media", "documents_history", "groups_scheduler",
                "perex_group_doc", ConfDB.ADMINLOG_TABLE_NAME, "branches", "branch_documents", "branch_groups", "branch_media", "branch_files")) {
            String column;
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM `" + table + "` WHERE 1=0");
                 ResultSet result = statement.executeQuery()) { column = result.getMetaData().getColumnName(1); }
            try (PreparedStatement statement = connection.prepareStatement("UPDATE `" + table + "` SET `" + column + "`=`" + column + "` WHERE 1=0")) { statement.executeUpdate(); }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `" + table + "` WHERE 1=0")) { statement.executeUpdate(); }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO `" + table + "` SELECT * FROM `" + table + "` WHERE 1=0")) { statement.executeUpdate(); }
        }
    }
}
