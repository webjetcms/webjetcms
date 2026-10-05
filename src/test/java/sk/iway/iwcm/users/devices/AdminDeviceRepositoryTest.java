package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Verifies retention boundaries, owner scoping and atomic writes using mocked JDBC calls. */
class AdminDeviceRepositoryTest {
    private static final long NOW = 1_791_187_200_123L;
    private static final long CUTOFF = NOW - Duration.ofDays(90).toMillis();
    private static final String HASH = "0123456789abcdef".repeat(4);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement ownerLock = mock(PreparedStatement.class);
    private final PreparedStatement deviceRead = mock(PreparedStatement.class);
    private final PreparedStatement deviceInsert = mock(PreparedStatement.class);
    private final PreparedStatement deviceUpdate = mock(PreparedStatement.class);
    private final PreparedStatement eventRead = mock(PreparedStatement.class);
    private final PreparedStatement eventInsert = mock(PreparedStatement.class);
    private final PreparedStatement eventUpdate = mock(PreparedStatement.class);
    private final ResultSet devices = mock(ResultSet.class);
    private final ResultSet events = mock(ResultSet.class);
    private final AdminDeviceRepository repository = new AdminDeviceRepository(() -> connection);

    @BeforeEach
    void prepareConnection() throws SQLException {
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        AtomicBoolean autoCommit = new AtomicBoolean(true);
        when(connection.getAutoCommit()).thenAnswer(invocation -> autoCommit.get());
        doAnswer(invocation -> { autoCommit.set(invocation.getArgument(0)); return null; }).when(connection).setAutoCommit(anyBoolean());
        when(connection.prepareStatement(startsWith("SELECT user_id"))).thenReturn(ownerLock);
        when(connection.prepareStatement(startsWith("SELECT last_seen"))).thenReturn(deviceRead);
        when(connection.prepareStatement(startsWith("INSERT INTO user_login_devices"))).thenReturn(deviceInsert);
        when(connection.prepareStatement(startsWith("UPDATE user_login_devices"))).thenReturn(deviceUpdate);
        when(connection.prepareStatement(startsWith("SELECT event_id"))).thenReturn(eventRead);
        when(connection.prepareStatement(startsWith("INSERT INTO user_login_events"))).thenReturn(eventInsert);
        when(connection.prepareStatement(startsWith("UPDATE user_login_events"))).thenReturn(eventUpdate);
        when(deviceRead.executeQuery()).thenReturn(devices);
        when(eventRead.executeQuery()).thenReturn(events);
        ResultSet owner = mock(ResultSet.class);
        when(owner.next()).thenReturn(true);
        when(ownerLock.executeQuery()).thenReturn(owner);
    }

    /** Serializes creation before persisting recognition and its snapshot in the same transaction. */
    @ParameterizedTest
    @ValueSource(strings = { "PostgreSQL", "Oracle", "Microsoft SQL Server" })
    void locksAccountAndDeviceBeforeFirstUseAndCommitsBothWrites(String product) throws SQLException {
        when(connection.getMetaData().getDatabaseProductName()).thenReturn(product);
        AdminLoginEvent event = recordLogin();

        assertNotNull(event);
        assertEquals(NOW, event.createdAt());
        assertEquals(NOW + Duration.ofDays(7).toMillis(), event.expiresAt());
        assertEquals("Firefox", event.browserName());
        verify(deviceRead).setInt(1, 7);
        verify(deviceRead).setInt(2, 42);
        verify(deviceRead).setString(3, HASH);
        verify(eventInsert).setInt(2, 7);
        verify(eventInsert).setInt(3, 42);
        verify(eventInsert).setString(4, HASH);
        String expectedLock = product.contains("SQL Server")
            ? "SELECT user_id FROM users WITH (UPDLOCK, ROWLOCK, HOLDLOCK) WHERE user_id=?"
            : "SELECT user_id FROM users WHERE user_id=? FOR UPDATE";
        verify(connection).prepareStatement(expectedLock);
        verify(connection).prepareStatement(product.contains("SQL Server")
            ? "SELECT last_seen, revoked_at FROM user_login_devices WITH (UPDLOCK, ROWLOCK, HOLDLOCK) WHERE user_id=? AND domain_id=? AND token_hash=?"
            : "SELECT last_seen, revoked_at FROM user_login_devices WHERE user_id=? AND domain_id=? AND token_hash=? FOR UPDATE");
        var order = inOrder(connection, ownerLock, deviceRead, deviceInsert, eventInsert);
        order.verify(connection).setAutoCommit(false);
        order.verify(ownerLock).executeQuery();
        order.verify(deviceRead).executeQuery();
        order.verify(deviceInsert).executeUpdate();
        order.verify(eventInsert).executeUpdate();
        order.verify(connection).commit();
        order.verify(connection).setAutoCommit(true);
        verify(connection, never()).rollback();
    }

    /** Refreshes a recognized browser without a duplicate event or email trigger. */
    @Test
    void refreshesKnownDeviceWithoutCreatingAnEvent() throws SQLException {
        stubDevice(CUTOFF + 1, null);
        assertNull(recordLogin());
        verify(deviceUpdate).setTimestamp(1, new Timestamp(NOW));
        verify(deviceUpdate).setInt(2, 7);
        verify(deviceUpdate).setInt(3, 42);
        verify(deviceUpdate).setString(4, HASH);
        verifyNoInteractions(deviceInsert, eventInsert);
    }

    /** Treats the exact retention boundary as expired and reuses the existing device row. */
    @Test
    void recordsNewEventAtExactExpiryBoundary() throws SQLException {
        stubDevice(CUTOFF, null);
        assertNotNull(recordLogin());
        verify(deviceUpdate).executeUpdate();
        verify(eventInsert).executeUpdate();
        verifyNoInteractions(deviceInsert);
    }

    /** A report invalidates even a recent browser; an older request cannot move its last use backwards. */
    @Test
    void recordsRevokedDeviceAndKeepsLastSeenMonotonic() throws SQLException {
        stubDevice(NOW + 10, NOW - 1);
        assertNotNull(recordLogin());
        verify(deviceUpdate).setTimestamp(1, new Timestamp(NOW + 10));
        verify(eventInsert).executeUpdate();
    }

    /** Requests rollback of recognition if the associated event snapshot cannot be inserted. */
    @Test
    void rollsBackRecognitionWhenEventInsertFails() throws SQLException {
        when(eventInsert.executeUpdate()).thenThrow(new SQLException("Insert failed"));
        assertThrows(IllegalStateException.class, this::recordLogin);
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).setAutoCommit(true);
    }

    /** Applies the seven-day warning deadline independently of confirmation and the 90-day history. */
    @Test
    void warningQueryIsOwnerScopedAndUsesOriginalSevenDayDeadline() throws SQLException {
        stubEvent(null, null);
        when(events.next()).thenReturn(true, false);
        var active = repository.findActive(7, 42, NOW);
        assertEquals(1, active.size());
        assertEquals(NOW - 1000 + Duration.ofDays(7).toMillis(), active.get(0).expiresAt());
        verify(eventRead).setInt(1, 7);
        verify(eventRead).setInt(2, 42);
        verify(eventRead).setTimestamp(3, new Timestamp(NOW - Duration.ofDays(7).toMillis()));
        verify(connection).prepareStatement(contains("confirmed_at IS NULL AND created_at>?"));
        verifyNoInteractions(deviceRead, ownerLock);
    }

    /** Missing, foreign and expired events cause no account mutation. */
    @Test
    void unavailableEventCannotBeConfirmedOrReported() throws SQLException {
        assertNull(repository.findEvent(7, 42, "other-event", NOW));
        assertNull(repository.confirm(7, 42, "other-event", NOW));
        assertNull(repository.report(7, 42, "other-event", NOW));
        verify(eventRead, times(3)).setInt(1, 7);
        verify(eventRead, times(3)).setInt(2, 42);
        verify(eventRead, times(3)).setString(3, "other-event");
        verify(eventRead, times(3)).setTimestamp(4, new Timestamp(CUTOFF));
        verifyNoInteractions(deviceUpdate, eventUpdate);
    }

    /** Confirmation hides a notification without reauthorizing a previously reported browser. */
    @Test
    void confirmationOnlyChangesNotificationStateAndIsIdempotent() throws SQLException {
        stubEvent(null, NOW - 20);
        AdminLoginEvent confirmed = repository.confirm(7, 42, "event-1", NOW);
        assertEquals(NOW, confirmed.confirmedAt());
        assertEquals(NOW - 20, confirmed.reportedAt());
        verify(eventUpdate).executeUpdate();
        verifyNoInteractions(deviceUpdate);

        clearInvocations(eventUpdate);
        stubEvent(NOW - 5, NOW - 20);
        assertEquals(NOW - 5, repository.confirm(7, 42, "event-1", NOW).confirmedAt());
        verifyNoInteractions(eventUpdate);
    }

    /** Reporting revokes the account's browser and restores the original warning atomically. */
    @Test
    void reportRevokesRecognitionAndPreservesWarningWithoutExtendingItsDeadline() throws SQLException {
        stubEvent(NOW - 100, null);
        AdminLoginEvent reported = repository.report(7, 42, "event-1", NOW);
        assertEquals(NOW, reported.reportedAt());
        assertNull(reported.confirmedAt());
        assertEquals(NOW - 1000 + Duration.ofDays(7).toMillis(), reported.expiresAt());
        verify(deviceUpdate).setInt(2, 7);
        verify(deviceUpdate).setInt(3, 42);
        verify(deviceUpdate).setString(4, "event-1");
        verify(deviceUpdate).setInt(5, 7);
        verify(deviceUpdate).setInt(6, 42);
        var order = inOrder(deviceUpdate, eventUpdate, connection);
        order.verify(deviceUpdate).executeUpdate();
        order.verify(eventUpdate).executeUpdate();
        order.verify(connection).commit();

        clearInvocations(deviceUpdate, eventUpdate);
        stubEvent(null, NOW - 10);
        assertEquals(NOW - 10, repository.report(7, 42, "event-1", NOW).reportedAt());
        verifyNoInteractions(deviceUpdate, eventUpdate);
    }

    /** Uses a database-wide account advisory lock on MySQL despite legacy MyISAM users tables. */
    @ParameterizedTest
    @ValueSource(strings = { "MySQL", "MariaDB" })
    void mysqlAccountLockRemainsHeldUntilCommitOrRollback(String product) throws SQLException {
        when(connection.getMetaData().getDatabaseProductName()).thenReturn(product);
        PreparedStatement acquire = mock(PreparedStatement.class);
        PreparedStatement release = mock(PreparedStatement.class);
        ResultSet acquired = mock(ResultSet.class);
        when(connection.prepareStatement("SELECT GET_LOCK(?, 10)")).thenReturn(acquire);
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(release);
        when(acquire.executeQuery()).thenReturn(acquired);
        when(acquired.next()).thenReturn(true);
        when(acquired.getInt(1)).thenReturn(1);
        when(release.executeQuery()).thenReturn(mock(ResultSet.class));
        recordLogin();
        var order = inOrder(acquire, connection, release);
        order.verify(acquire).setString(1, "webjet-login-device-7");
        order.verify(acquire).executeQuery();
        order.verify(connection).commit();
        order.verify(release).executeQuery();
        order.verify(connection).setAutoCommit(true);

        clearInvocations(connection, release);
        when(eventInsert.executeUpdate()).thenThrow(new SQLException("Insert failed"));
        assertThrows(IllegalStateException.class, this::recordLogin);
        order = inOrder(connection, release);
        order.verify(connection).rollback();
        order.verify(release).executeQuery();
        order.verify(connection).setAutoCommit(true);
    }

    /** Prunes the 90-day event history independently from the configurable recognition duration. */
    @Test
    void cleanupUsesIndependentRetentionBoundaries() throws SQLException {
        PreparedStatement deleteEvents = mock(PreparedStatement.class);
        PreparedStatement deleteDevices = mock(PreparedStatement.class);
        when(connection.prepareStatement("DELETE FROM user_login_events WHERE created_at<=?")).thenReturn(deleteEvents);
        when(connection.prepareStatement("DELETE FROM user_login_devices WHERE last_seen<=?")).thenReturn(deleteDevices);
        long deviceCutoff = NOW - Duration.ofDays(30).toMillis();
        repository.cleanup(NOW, deviceCutoff);
        verify(deleteEvents).setTimestamp(1, new Timestamp(CUTOFF));
        verify(deleteDevices).setTimestamp(1, new Timestamp(deviceCutoff));
        verify(deleteEvents).executeUpdate();
        verify(deleteDevices).executeUpdate();
    }

    private AdminLoginEvent recordLogin() {
        return repository.recordLogin(7, 42, HASH, NOW, CUTOFF, "Firefox", "131", "Windows 11", "127.0.0.1");
    }

    private void stubDevice(long lastSeen, Long revokedAt) throws SQLException {
        when(devices.next()).thenReturn(true);
        when(devices.getTimestamp("last_seen")).thenReturn(new Timestamp(lastSeen));
        when(devices.getTimestamp("revoked_at")).thenReturn(revokedAt == null ? null : new Timestamp(revokedAt));
    }

    private void stubEvent(Long confirmedAt, Long reportedAt) throws SQLException {
        when(events.next()).thenReturn(true);
        when(events.getString("event_id")).thenReturn("event-1");
        when(events.getTimestamp("created_at")).thenReturn(new Timestamp(NOW - 1000));
        when(events.getTimestamp("confirmed_at")).thenReturn(confirmedAt == null ? null : new Timestamp(confirmedAt));
        when(events.getTimestamp("reported_at")).thenReturn(reportedAt == null ? null : new Timestamp(reportedAt));
    }
}
