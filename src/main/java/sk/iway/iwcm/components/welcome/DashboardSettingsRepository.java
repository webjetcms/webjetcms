package sk.iway.iwcm.components.welcome;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.DBPool;

/** Persists dashboard records in the administration settings table with account-level locking for writes. */
@Repository
public class DashboardSettingsRepository {
    static final String PREFIX = "overview.";
    static final String LAYOUT_KEY = PREFIX + "layout.v1";
    static final String NEWS_KEY = PREFIX + "news";
    static final String WIDGET_PREFIX = PREFIX + "widget.";
    static final String DOMAIN_PREFIX = PREFIX + "domain.";

    /** Supplies database connections that callers close after each dashboard operation. */
    @FunctionalInterface
    interface ConnectionFactory {
        /**
         * Opens a connection for a dashboard database operation.
         *
         * @return connection owned by the caller
         * @throws SQLException if a connection cannot be obtained
         */
        Connection open() throws SQLException;
    }

    /** Applies a settings mutation within an account lock and a repository-managed transaction. */
    @FunctionalInterface
    private interface SettingsWrite {
        /**
         * Performs the mutation using the current transaction.
         *
         * @param connection connection whose transaction is committed or rolled back by the repository
         * @throws SQLException if the database mutation fails
         */
        void execute(Connection connection) throws SQLException;
    }

    private final ConnectionFactory connections;

    public DashboardSettingsRepository() {
        this(DBPool::getConnection);
    }

    DashboardSettingsRepository(ConnectionFactory connections) {
        this.connections = connections;
    }

    /**
     * Reads all dashboard records for an account directly from the database.
     * On SQL Server, the read takes the account lock in a transaction to coordinate with writes.
     *
     * @param userId ID of the account that owns the settings
     * @return stored dashboard values keyed by administration settings keys, including options for all domains
     * @throws IllegalStateException if the database read fails
     */
    public Map<String, String> read(int userId) {
        try (Connection connection = connections.open()) {
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase(java.util.Locale.ROOT);
            if (!product.contains("sql server")) return read(connection, userId);
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                lockUser(connection, userId, product);
                Map<String, String> records = read(connection, userId);
                connection.commit();
                return records;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load dashboard settings", exception);
        }
    }

    /**
     * Replaces the global layout and current-domain options in one transaction.
     * Other domains retain options for current instances and instances from the previous layout,
     * allowing an immediately removed widget to be restored by undo.
     *
     * @param userId ID of the account that owns the settings
     * @param domainKey decimal root group ID selecting the domain options to replace
     * @param records validated records to insert after removing the superseded settings
     * @param instanceIds widget instance IDs present in the replacement layout
     * @throws IllegalStateException if the database mutation fails
     */
    public void replace(int userId, String domainKey, Map<String, String> records, Set<String> instanceIds) {
        write(userId, connection -> {
            Map<String, String> previous = read(connection, userId);
            Set<String> previousInstanceIds = previous.keySet().stream().filter(key -> key.startsWith(WIDGET_PREFIX))
                .map(key -> key.substring(WIDGET_PREFIX.length())).collect(Collectors.toSet());
            String domainPrefix = DOMAIN_PREFIX + domainKey + ".";
            for (String key : previous.keySet()) {
                String instanceId = key.substring(key.lastIndexOf('.') + 1);
                boolean obsoleteDomain = key.startsWith(DOMAIN_PREFIX)
                    && !instanceIds.contains(instanceId) && !previousInstanceIds.contains(instanceId);
                if (key.equals(LAYOUT_KEY) || key.equals(NEWS_KEY) || key.startsWith(WIDGET_PREFIX)
                    || key.startsWith(domainPrefix) || obsoleteDomain) {
                    execute(connection, "DELETE FROM user_settings_admin WHERE user_id=? AND skey=?", userId, key, null);
                }
            }
            for (Map.Entry<String, String> record : records.entrySet()) {
                execute(connection, "INSERT INTO user_settings_admin (user_id, skey, value) VALUES (?, ?, ?)", userId, record.getKey(), record.getValue());
            }
        });
    }

    /**
     * Atomically replaces layout, widget, news and domain records using a locked account snapshot.
     * The callback computes the replacement records before any existing records are removed.
     *
     * @param userId ID of the account whose settings are reset
     * @param retain callback that builds replacement records from the account's current dashboard records
     * @return replacement records persisted by the transaction
     * @throws IllegalStateException if the database mutation fails
     */
    public Map<String, String> reset(int userId, Function<Map<String, String>, Map<String, String>> retain) {
        Map<String, String> retained = new LinkedHashMap<>();
        write(userId, connection -> {
            Map<String, String> previous = read(connection, userId);
            retained.putAll(retain.apply(previous));
            for (String key : previous.keySet()) {
                if (key.equals(LAYOUT_KEY) || key.equals(NEWS_KEY) || key.startsWith(WIDGET_PREFIX) || key.startsWith(DOMAIN_PREFIX)) {
                    execute(connection, "DELETE FROM user_settings_admin WHERE user_id=? AND skey=?", userId, key, null);
                }
            }
            for (Map.Entry<String, String> record : retained.entrySet()) {
                execute(connection, "INSERT INTO user_settings_admin (user_id, skey, value) VALUES (?, ?, ?)", userId, record.getKey(), record.getValue());
            }
        });
        return retained;
    }

    /**
     * Serializes account mutations and commits them in a transaction, rolling back on failure.
     * MySQL and MariaDB require InnoDB settings storage and use an advisory lock; other databases
     * lock the owner's user row. The original auto-commit mode is restored before closing the connection.
     *
     * @param userId ID of the account to lock
     * @param mutation settings changes to apply while holding the account lock
     * @throws IllegalStateException if a database operation fails
     */
    private void write(int userId, SettingsWrite mutation) {
        try (Connection connection = connections.open()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase(java.util.Locale.ROOT);
            boolean mysql = product.contains("mysql") || product.contains("mariadb");
            boolean advisoryLock = false;
            try {
                if (mysql) {
                    requireTransactionalSettings(connection);
                    acquireMysqlLock(connection, userId);
                    advisoryLock = true;
                }
                connection.setAutoCommit(false);
                if (!mysql) lockUser(connection, userId, product);

                mutation.execute(connection);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                if (!connection.getAutoCommit()) connection.rollback();
                throw exception;
            } finally {
                if (advisoryLock) releaseMysqlLock(connection, userId);
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save dashboard settings", exception);
        }
    }

    /**
     * Reads the account's dashboard-prefixed settings through the supplied connection.
     *
     * @param connection connection participating in the caller's current transaction, if any
     * @param userId ID of the account that owns the settings
     * @return dashboard setting keys mapped to their stored values
     * @throws SQLException if the settings query fails
     */
    private Map<String, String> read(Connection connection, int userId) throws SQLException {
        Map<String, String> records = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT skey, value FROM user_settings_admin WHERE user_id=? AND skey LIKE ?")) {
            statement.setInt(1, userId);
            statement.setString(2, PREFIX + "%");
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) records.put(rows.getString(1), rows.getString(2));
            }
        }
        return records;
    }

    /**
     * Executes a settings insert or delete with positional bindings for the owner, key and optional value.
     *
     * @param connection connection in the active write transaction
     * @param sql statement with two placeholders for deletion or three for insertion
     * @param userId account ID bound to the first placeholder
     * @param key settings key bound to the second placeholder
     * @param value value bound to the third placeholder, or {@code null} for a delete statement
     * @throws SQLException if statement preparation or execution fails
     */
    private void execute(Connection connection, String sql, int userId, String key, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, key);
            if (value != null) statement.setString(3, value);
            statement.executeUpdate();
        }
    }

    /**
     * Locks the owner's user row for the current transaction using database-specific SQL.
     *
     * @param connection connection with an active transaction
     * @param userId ID of the account to lock
     * @param product lowercase database product name used to select the SQL Server lock syntax
     * @throws SQLException if the owner does not exist or the lock query fails
     */
    private void lockUser(Connection connection, int userId, String product) throws SQLException {
        String sql = product.contains("sql server")
            ? "SELECT user_id FROM users WITH (UPDLOCK, ROWLOCK, HOLDLOCK) WHERE user_id=?"
            : "SELECT user_id FROM users WHERE user_id=? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("Dashboard settings owner does not exist");
            }
        }
    }

    /**
     * Verifies that MySQL or MariaDB can roll back writes to the administration settings table.
     *
     * @param connection connection to the database containing the settings table
     * @throws SQLException if the table is missing, does not use InnoDB or its engine cannot be queried
     */
    private void requireTransactionalSettings(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT ENGINE FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='user_settings_admin'")) {
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next() || !"InnoDB".equalsIgnoreCase(rows.getString(1))) {
                    throw new SQLException("Dashboard settings require the user_settings_admin table to use InnoDB");
                }
            }
        }
    }

    /**
     * Acquires the account's MySQL or MariaDB advisory lock, waiting for at most ten seconds.
     *
     * @param connection connection that will hold the lock until it is explicitly released
     * @param userId ID of the account whose settings are locked
     * @throws SQLException if the lock cannot be acquired or the lock query fails
     */
    private void acquireMysqlLock(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, 10)")) {
            statement.setString(1, "webjet-dashboard-" + userId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next() || rows.getInt(1) != 1) throw new SQLException("Could not lock dashboard settings");
            }
        }
    }

    private void releaseMysqlLock(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, "webjet-dashboard-" + userId);
            statement.executeQuery().close();
        }
    }
}
