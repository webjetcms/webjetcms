package sk.iway.iwcm.users.devices;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.DBPool;

/** Persists browser recognition and immutable login snapshots with account-scoped write locking. */
@Repository
public class AdminDeviceRepository {
    static final long NOTICE_AGE = Duration.ofDays(7).toMillis();
    static final long EVENT_AGE = Duration.ofDays(90).toMillis();
    private static final String EVENT_COLUMNS = "event_id, created_at, browser_name, browser_version, operating_system, ip_address, confirmed_at, reported_at";

    /** Supplies a connection owned and closed by the repository operation. */
    @FunctionalInterface
    interface ConnectionFactory {
        Connection open() throws SQLException;
    }

    /** Runs one operation inside the repository's account lock and transaction. */
    @FunctionalInterface
    private interface AccountWrite<T> {
        T execute(Connection connection) throws SQLException;
    }

    private final ConnectionFactory connections;

    public AdminDeviceRepository() {
        this(DBPool::getConnection);
    }

    AdminDeviceRepository(ConnectionFactory connections) {
        this.connections = connections;
    }

    /**
     * Refreshes recognition and records a snapshot only when the account has not recently used the device.
     * The lock also serializes first use and simultaneous logins after expiry across application nodes.
     *
     * @param userId account whose authentication has completed
     * @param domainId tenant owning the account
     * @param tokenHash SHA-256 hash of the browser token
     * @param now current epoch time in milliseconds
     * @param knownSinceCutoff exclusive lower bound of a recognized last login
     * @param browserName browser name captured at login
     * @param browserVersion browser version captured at login
     * @param operatingSystem operating system captured at login
     * @param ipAddress source IP captured at login
     * @return newly persisted event, or {@code null} when the device was already recognized
     */
    public AdminLoginEvent recordLogin(int userId, int domainId, String tokenHash, long now, long knownSinceCutoff,
        String browserName, String browserVersion, String operatingSystem, String ipAddress) {
        return write(userId, connection -> {
            boolean exists = false;
            boolean recognized = false;
            long lastSeen = now;
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
            boolean sqlServer = product.contains("sql server");
            String readSql = "SELECT last_seen, revoked_at FROM user_login_devices"
                + (sqlServer ? " WITH (UPDLOCK, ROWLOCK, HOLDLOCK)" : "")
                + " WHERE user_id=? AND domain_id=? AND token_hash=?" + (sqlServer ? "" : " FOR UPDATE");
            try (PreparedStatement statement = connection.prepareStatement(readSql)) {
                bindOwner(statement, userId, domainId);
                statement.setString(3, tokenHash);
                try (ResultSet rows = statement.executeQuery()) {
                    if (rows.next()) {
                        exists = true;
                        lastSeen = Math.max(now, rows.getTimestamp("last_seen").getTime());
                        recognized = rows.getTimestamp("revoked_at") == null
                            && rows.getTimestamp("last_seen").getTime() > knownSinceCutoff;
                    }
                }
            }
            String deviceSql = exists
                ? "UPDATE user_login_devices SET last_seen=?, revoked_at=NULL WHERE user_id=? AND domain_id=? AND token_hash=?"
                : "INSERT INTO user_login_devices (last_seen, user_id, domain_id, token_hash) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(deviceSql)) {
                statement.setTimestamp(1, new Timestamp(lastSeen));
                statement.setInt(2, userId);
                statement.setInt(3, domainId);
                statement.setString(4, tokenHash);
                statement.executeUpdate();
            }
            if (recognized) return null;

            AdminLoginEvent event = new AdminLoginEvent(UUID.randomUUID().toString(), now, now + NOTICE_AGE,
                browserName, browserVersion, operatingSystem, ipAddress, null, null);
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO user_login_events (event_id, user_id, domain_id, token_hash, created_at, browser_name, browser_version, operating_system, ip_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                statement.setString(1, event.id());
                statement.setInt(2, userId);
                statement.setInt(3, domainId);
                statement.setString(4, tokenHash);
                statement.setTimestamp(5, new Timestamp(now));
                statement.setString(6, browserName);
                statement.setString(7, browserVersion);
                statement.setString(8, operatingSystem);
                statement.setString(9, ipAddress);
                statement.executeUpdate();
            }
            return event;
        });
    }

    /** Returns unconfirmed events less than seven days old, with the newest first. */
    public List<AdminLoginEvent> findActive(int userId, int domainId, long now) {
        try (Connection connection = connections.open(); PreparedStatement statement = connection.prepareStatement(
            "SELECT " + EVENT_COLUMNS + " FROM user_login_events WHERE user_id=? AND domain_id=? AND confirmed_at IS NULL AND created_at>? ORDER BY created_at DESC, event_id")) {
            bindOwner(statement, userId, domainId);
            statement.setTimestamp(3, new Timestamp(now - NOTICE_AGE));
            List<AdminLoginEvent> events = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) events.add(mapEvent(rows));
            }
            return events;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load login notifications", exception);
        }
    }

    /** Returns an owned event while its 90-day retention period is valid, otherwise {@code null}. */
    public AdminLoginEvent findEvent(int userId, int domainId, String eventId, long now) {
        try (Connection connection = connections.open()) {
            return findEvent(connection, userId, domainId, eventId, now);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load login event", exception);
        }
    }

    /** Hides an owned event idempotently without altering browser recognition. */
    public AdminLoginEvent confirm(int userId, int domainId, String eventId, long now) {
        return write(userId, connection -> {
            AdminLoginEvent event = findEvent(connection, userId, domainId, eventId, now);
            if (event == null || event.confirmedAt() != null) return event;
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE user_login_events SET confirmed_at=? WHERE user_id=? AND domain_id=? AND event_id=?")) {
                bindAction(statement, userId, domainId, eventId, now);
                statement.executeUpdate();
            }
            return new AdminLoginEvent(event.id(), event.createdAt(), event.expiresAt(), event.browserName(),
                event.browserVersion(), event.operatingSystem(), event.ipAddress(), now, event.reportedAt());
        });
    }

    /**
     * Reports an owned login and revokes its browser recognition atomically, retaining its warning.
     * Repeating the same report does not revoke recognition established after the original report.
     *
     * @return the reported event or {@code null} when the event is unavailable to this account
     */
    public AdminLoginEvent report(int userId, int domainId, String eventId, long now) {
        return write(userId, connection -> {
            AdminLoginEvent event = findEvent(connection, userId, domainId, eventId, now);
            if (event == null || event.reportedAt() != null) return event;
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE user_login_devices SET revoked_at=? WHERE user_id=? AND domain_id=? AND token_hash IN (SELECT token_hash FROM user_login_events WHERE event_id=? AND user_id=? AND domain_id=?)")) {
                bindAction(statement, userId, domainId, eventId, now);
                statement.setInt(5, userId);
                statement.setInt(6, domainId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE user_login_events SET reported_at=?, confirmed_at=NULL WHERE user_id=? AND domain_id=? AND event_id=?")) {
                bindAction(statement, userId, domainId, eventId, now);
                statement.executeUpdate();
            }
            return new AdminLoginEvent(event.id(), event.createdAt(), event.expiresAt(), event.browserName(),
                event.browserVersion(), event.operatingSystem(), event.ipAddress(), null, now);
        });
    }

    /** Deletes expired recognition and old event snapshots independently of account activity. */
    public void cleanup(long now, long knownSinceCutoff) {
        try (Connection connection = connections.open()) {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM user_login_events WHERE created_at<=?")) {
                statement.setTimestamp(1, new Timestamp(now - EVENT_AGE));
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM user_login_devices WHERE last_seen<=?")) {
                statement.setTimestamp(1, new Timestamp(knownSinceCutoff));
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not clean up login history", exception);
        }
    }

    private AdminLoginEvent findEvent(Connection connection, int userId, int domainId, String eventId, long now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT " + EVENT_COLUMNS + " FROM user_login_events WHERE user_id=? AND domain_id=? AND event_id=? AND created_at>?")) {
            bindOwner(statement, userId, domainId);
            statement.setString(3, eventId);
            statement.setTimestamp(4, new Timestamp(now - EVENT_AGE));
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? mapEvent(rows) : null;
            }
        }
    }

    private AdminLoginEvent mapEvent(ResultSet rows) throws SQLException {
        long createdAt = rows.getTimestamp("created_at").getTime();
        Timestamp confirmedAt = rows.getTimestamp("confirmed_at");
        Timestamp reportedAt = rows.getTimestamp("reported_at");
        return new AdminLoginEvent(rows.getString("event_id"), createdAt, createdAt + NOTICE_AGE,
            rows.getString("browser_name"), rows.getString("browser_version"), rows.getString("operating_system"),
            rows.getString("ip_address"), confirmedAt == null ? null : confirmedAt.getTime(), reportedAt == null ? null : reportedAt.getTime());
    }

    private void bindOwner(PreparedStatement statement, int userId, int domainId) throws SQLException {
        statement.setInt(1, userId);
        statement.setInt(2, domainId);
    }

    private void bindAction(PreparedStatement statement, int userId, int domainId, String eventId, long now) throws SQLException {
        statement.setTimestamp(1, new Timestamp(now));
        statement.setInt(2, userId);
        statement.setInt(3, domainId);
        statement.setString(4, eventId);
    }

    /** Serializes account writes and restores the pooled connection's transaction mode on every path. */
    private <T> T write(int userId, AccountWrite<T> mutation) {
        try (Connection connection = connections.open()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
            boolean mysql = product.contains("mysql") || product.contains("mariadb");
            boolean advisoryLock = false;
            try {
                if (mysql) {
                    acquireMysqlLock(connection, userId);
                    advisoryLock = true;
                }
                connection.setAutoCommit(false);
                lockUser(connection, userId, product, mysql);
                T result = mutation.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                if (!connection.getAutoCommit()) connection.rollback();
                throw exception;
            } finally {
                try {
                    if (advisoryLock) releaseMysqlLock(connection, userId);
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save login device history", exception);
        }
    }

    private void lockUser(Connection connection, int userId, String product, boolean mysql) throws SQLException {
        String sql = product.contains("sql server")
            ? "SELECT user_id FROM users WITH (UPDLOCK, ROWLOCK, HOLDLOCK) WHERE user_id=?"
            : "SELECT user_id FROM users WHERE user_id=?" + (mysql ? "" : " FOR UPDATE");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("Login device owner does not exist");
            }
        }
    }

    /** MySQL installations can retain MyISAM users tables, which cannot provide transaction row locks. */
    private void acquireMysqlLock(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, 10)")) {
            statement.setString(1, "webjet-login-device-" + userId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next() || rows.getInt(1) != 1) throw new SQLException("Could not lock login device history");
            }
        }
    }

    private void releaseMysqlLock(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, "webjet-login-device-" + userId);
            statement.executeQuery().close();
        }
    }
}
