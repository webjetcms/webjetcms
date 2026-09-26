package sk.iway.iwcm.components.welcome;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.DBPool;

/** Stores bounded dashboard records in the existing administration settings table. */
@Repository
public class DashboardSettingsRepository {
    static final String PREFIX = "overview.";
    static final String LAYOUT_KEY = PREFIX + "layout.v1";
    static final String NEWS_KEY = PREFIX + "news";
    static final String WIDGET_PREFIX = PREFIX + "widget.";
    static final String DOMAIN_PREFIX = PREFIX + "domain.";

    @FunctionalInterface
    interface ConnectionFactory {
        Connection open() throws SQLException;
    }

    @FunctionalInterface
    private interface SettingsWrite {
        void execute(Connection connection) throws SQLException;
    }

    private final ConnectionFactory connections;

    public DashboardSettingsRepository() {
        this(DBPool::getConnection);
    }

    DashboardSettingsRepository(ConnectionFactory connections) {
        this.connections = connections;
    }

    /** Reads from the database rather than the cached user or session settings. */
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
     * Other domains retain existing instances and one write of removed instances for immediate undo.
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

    /** Removes only this account's dashboard records, including options for every domain. */
    public void reset(int userId) {
        write(userId, connection -> {
            for (String key : read(connection, userId).keySet()) {
                if (key.equals(LAYOUT_KEY) || key.equals(NEWS_KEY) || key.startsWith(WIDGET_PREFIX) || key.startsWith(DOMAIN_PREFIX)) {
                    execute(connection, "DELETE FROM user_settings_admin WHERE user_id=? AND skey=?", userId, key, null);
                }
            }
        });
    }

    /** Serializes saves and resets for one account and rolls back every failed mutation. */
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

    private void execute(Connection connection, String sql, int userId, String key, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, key);
            if (value != null) statement.setString(3, value);
            statement.executeUpdate();
        }
    }

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

    private void requireTransactionalSettings(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT ENGINE FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='user_settings_admin'")) {
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next() || !"InnoDB".equalsIgnoreCase(rows.getString(1))) {
                    throw new SQLException("Dashboard settings require the user_settings_admin table to use InnoDB");
                }
            }
        }
    }

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
