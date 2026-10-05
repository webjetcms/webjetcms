package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientConnectionException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.persistence.exceptions.DatabaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Verifies recognition and acknowledgment rules at the Spring Data transaction boundary. */
class DeviceServiceTest {
    private static final int USER_ID = 7;
    private static final int DOMAIN_ID = 42;
    private static final long NOW = 1_791_187_200_123L;
    private static final long CUTOFF = NOW - Duration.ofDays(90).toMillis();
    private static final String TOKEN_HASH = "a".repeat(64);
    private static final String EVENT_ID = "b60350ca-a38d-4b2e-b5b7-159f2f0f67f0";

    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final LoginEventRepository events = mock(LoginEventRepository.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final List<TransactionStatus> statuses = new ArrayList<>();
    private final DeviceService service = new DeviceService(devices, events, transactions);

    @BeforeEach
    void createIndependentTransactionForEveryAttempt() {
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenAnswer(invocation -> {
            TransactionDefinition definition = invocation.getArgument(0);
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getPropagationBehavior(),
                "Login retries must not reuse a failed surrounding transaction");
            TransactionStatus status = new SimpleTransactionStatus();
            statuses.add(status);
            return status;
        });
    }

    /** Recognition and the immutable notification snapshot must be committed before leaving the service. */
    @Test
    void newDeviceReturnsCommittedAccountScopedLoginSnapshot() {
        LoginEvent result = recordLogin();

        ArgumentCaptor<DeviceEntity> device = ArgumentCaptor.forClass(DeviceEntity.class);
        ArgumentCaptor<LoginEventEntity> event = ArgumentCaptor.forClass(LoginEventEntity.class);
        var order = inOrder(devices, events, transactions);
        order.verify(transactions).getTransaction(any(TransactionDefinition.class));
        order.verify(devices).findForUpdate(USER_ID, DOMAIN_ID, TOKEN_HASH);
        order.verify(devices).saveAndFlush(device.capture());
        order.verify(events).save(event.capture());
        order.verify(transactions).commit(statuses.get(0));
        assertEquals(new DeviceId(DOMAIN_ID, USER_ID, TOKEN_HASH), device.getValue().getId());
        assertEquals(Instant.ofEpochMilli(NOW), device.getValue().getLastSeen());
        assertEquals(USER_ID, event.getValue().getUserId());
        assertEquals(DOMAIN_ID, event.getValue().getDomainId());
        assertEquals(TOKEN_HASH, event.getValue().getTokenHash());
        assertNotNull(result.id());
        assertEquals(event.getValue().getId(), result.id());
        assertEquals(NOW, result.createdAt());
        assertEquals(NOW + Duration.ofDays(7).toMillis(), result.expiresAt());
        assertEquals("Firefox", result.browserName());
        assertEquals("131.0", result.browserVersion());
        assertEquals("Windows 11", result.operatingSystem());
        assertEquals("192.0.2.1", result.ipAddress());
        verify(transactions, never()).rollback(any());
    }

    /** A successful login refreshes recognition while retaining the event's original notice deadline. */
    @Test
    void recognizedDeviceRefreshDoesNotCreateOrExtendNotification() {
        DeviceEntity device = device(CUTOFF + 1, null);
        LoginEventEntity event = event(NOW - Duration.ofDays(6).toMillis());
        when(devices.findForUpdate(USER_ID, DOMAIN_ID, TOKEN_HASH)).thenReturn(Optional.of(device));
        when(events.findByUserIdAndDomainIdAndIdAndCreatedAtAfter(USER_ID, DOMAIN_ID, EVENT_ID, Instant.ofEpochMilli(CUTOFF)))
            .thenReturn(Optional.of(event));

        assertNull(recordLogin());
        LoginEvent original = service.findEvent(USER_ID, DOMAIN_ID, EVENT_ID, NOW);

        assertEquals(Instant.ofEpochMilli(NOW), device.getLastSeen());
        assertEquals(event.getCreatedAt().toEpochMilli() + Duration.ofDays(7).toMillis(), original.expiresAt());
        verify(events, never()).save(any());
        verify(devices, never()).saveAndFlush(any());
    }

    /** Returning exactly at the configured inactivity boundary produces a new notification. */
    @Test
    void deviceAtExpiryBoundaryCreatesNewEvent() {
        DeviceEntity device = device(CUTOFF, null);
        when(devices.findForUpdate(USER_ID, DOMAIN_ID, TOKEN_HASH)).thenReturn(Optional.of(device));

        assertNotNull(recordLogin());

        assertEquals(Instant.ofEpochMilli(NOW), device.getLastSeen());
        verify(events).save(any(LoginEventEntity.class));
    }

    /** A reported device is recognized anew on its next completed authentication. */
    @Test
    void revokedDeviceCreatesNewEventAndClearsRevocation() {
        DeviceEntity device = device(NOW - 1, NOW - 1);
        when(devices.findForUpdate(USER_ID, DOMAIN_ID, TOKEN_HASH)).thenReturn(Optional.of(device));

        assertNotNull(recordLogin());

        assertNull(device.getRevokedAt());
        assertEquals(Instant.ofEpochMilli(NOW), device.getLastSeen());
    }

    /** Delayed requests must not shorten a more recent successful login's recognition window. */
    @Test
    void earlierRequestDoesNotMoveLastSeenBackwards() {
        DeviceEntity device = device(NOW + 100, null);
        when(devices.findForUpdate(USER_ID, DOMAIN_ID, TOKEN_HASH)).thenReturn(Optional.of(device));

        assertNull(recordLogin());

        assertEquals(Instant.ofEpochMilli(NOW + 100), device.getLastSeen());
        verifyNoInteractions(events);
    }

    /** Active notices and email details use independent retention windows with the same owner scope. */
    @Test
    void readsUseSeparateNoticeAndEmailCutoffs() {
        LoginEventEntity event = event(NOW - 10);
        when(events.findByUserIdAndDomainIdAndConfirmedAtIsNullAndCreatedAtAfterOrderByCreatedAtDescIdAsc(
            USER_ID, DOMAIN_ID, Instant.ofEpochMilli(NOW - Duration.ofDays(7).toMillis()))).thenReturn(List.of(event));
        when(events.findByUserIdAndDomainIdAndIdAndCreatedAtAfter(USER_ID, DOMAIN_ID, EVENT_ID, Instant.ofEpochMilli(CUTOFF)))
            .thenReturn(Optional.of(event));

        assertEquals(List.of(service.findEvent(USER_ID, DOMAIN_ID, EVENT_ID, NOW)),
            service.findActive(USER_ID, DOMAIN_ID, NOW));

        verifyNoInteractions(devices, transactions);
    }

    /** Reporting a previously confirmed event restores its notice and revokes only the matching device. */
    @Test
    void firstReportRevokesRecognitionAndPreservesOriginalNoticeWindow() {
        LoginEventEntity event = event(NOW - 100);
        event.setConfirmedAt(Instant.ofEpochMilli(NOW - 50));
        DeviceEntity device = device(NOW - 100, null);
        ownedEvent(event);
        when(devices.findForUpdate(USER_ID, DOMAIN_ID, TOKEN_HASH)).thenReturn(Optional.of(device));

        LoginEvent result = service.report(USER_ID, DOMAIN_ID, EVENT_ID, NOW);

        assertEquals(NOW, result.reportedAt());
        assertNull(result.confirmedAt());
        assertEquals(NOW - 100, result.createdAt());
        assertEquals(NOW - 100 + Duration.ofDays(7).toMillis(), result.expiresAt());
        assertEquals(Instant.ofEpochMilli(NOW), device.getRevokedAt());
        assertEquals(Instant.ofEpochMilli(NOW - 100), device.getLastSeen());
        verify(transactions).commit(statuses.get(0));
        verify(devices, never()).saveAndFlush(any());
    }

    /** Reopening an old report cannot revoke recognition established by a subsequent login. */
    @Test
    void repeatedReportDoesNotRevokeLaterRecognition() {
        LoginEventEntity event = event(NOW - 100);
        event.setReportedAt(Instant.ofEpochMilli(NOW - 50));
        ownedEvent(event);

        LoginEvent result = service.report(USER_ID, DOMAIN_ID, EVENT_ID, NOW);

        assertEquals(NOW - 50, result.reportedAt());
        verifyNoInteractions(devices);
    }

    /** Confirmation preserves the original acknowledgment timestamp and never changes recognition. */
    @Test
    void confirmingTwiceIsIdempotent() {
        LoginEventEntity event = event(NOW - 100);
        when(events.findForUpdate(eq(USER_ID), eq(DOMAIN_ID), eq(EVENT_ID), any(Instant.class)))
            .thenReturn(Optional.of(event));

        LoginEvent first = service.confirm(USER_ID, DOMAIN_ID, EVENT_ID, NOW);
        LoginEvent repeated = service.confirm(USER_ID, DOMAIN_ID, EVENT_ID, NOW + 1);

        assertEquals(NOW, first.confirmedAt());
        assertEquals(first, repeated);
        verifyNoInteractions(devices);
    }

    /** Missing, expired, and foreign events have the same neutral result and do not touch devices. */
    @Test
    void inaccessibleEventCannotBeConfirmedOrReported() {
        assertNull(service.confirm(USER_ID, DOMAIN_ID, EVENT_ID, NOW));
        assertNull(service.report(USER_ID, DOMAIN_ID, EVENT_ID, NOW));

        verify(events, times(2)).findForUpdate(USER_ID, DOMAIN_ID, EVENT_ID, Instant.ofEpochMilli(CUTOFF));
        verifyNoInteractions(devices);
        verify(events, never()).save(any());
    }

    /** A failed event insert rolls back the device insert and never produces a notification snapshot. */
    @ParameterizedTest
    @MethodSource("nonRetryableFailures")
    void eventFailureRollsBackRecognitionAndDoesNotRetryUnrelatedErrors(RuntimeException failure) {
        when(events.save(any(LoginEventEntity.class))).thenThrow(failure);

        IllegalStateException result = assertThrows(IllegalStateException.class, this::recordLogin);

        assertSame(failure, result.getCause());
        assertEquals(1, statuses.size());
        verify(transactions).rollback(statuses.get(0));
        verify(transactions, never()).commit(any());
    }

    /** A failed commit must not be mistaken for a successful device detection by the mail caller. */
    @Test
    void commitFailurePreventsReturningNotificationSnapshot() {
        var failure = new TransactionSystemException("Commit failed");
        doThrow(failure).when(transactions).commit(any());

        IllegalStateException result = assertThrows(IllegalStateException.class, this::recordLogin);

        assertSame(failure, result.getCause());
        assertEquals(1, statuses.size());
        verify(transactions).commit(statuses.get(0));
    }

    /** A competing first insert must be retried in a fresh transaction and reuse its committed recognition. */
    @ParameterizedTest
    @MethodSource("uniqueInsertFailures")
    void uniqueInsertRaceRetriesAndRecognizesOtherTransactionResult(RuntimeException failure) {
        when(devices.findForUpdate(USER_ID, DOMAIN_ID, TOKEN_HASH))
            .thenReturn(Optional.empty()).thenReturn(Optional.of(device(NOW, null)));
        when(devices.saveAndFlush(any(DeviceEntity.class))).thenThrow(failure);

        assertNull(recordLogin());

        assertEquals(2, new HashSet<>(statuses).size());
        verify(transactions).rollback(statuses.get(0));
        verify(transactions).commit(statuses.get(1));
        verifyNoInteractions(events);
    }

    /** A lock conflict can be retried without losing the new event when the next transaction succeeds. */
    @Test
    void lockRaceRetriesAndCommitsNewEvent() {
        when(devices.findForUpdate(USER_ID, DOMAIN_ID, TOKEN_HASH))
            .thenThrow(new CannotAcquireLockException("Concurrent login"))
            .thenReturn(Optional.of(device(CUTOFF, null)));

        assertNotNull(recordLogin());

        assertEquals(2, new HashSet<>(statuses).size());
        verify(transactions).rollback(statuses.get(0));
        verify(transactions).commit(statuses.get(1));
        verify(events).save(any(LoginEventEntity.class));
    }

    /** Persistent uniqueness and lock failures terminate after three independent rolled-back attempts. */
    @ParameterizedTest
    @MethodSource("retryableFailures")
    void retryableFailuresAreBoundedAndUseFreshTransactions(RuntimeException failure) {
        when(devices.findForUpdate(USER_ID, DOMAIN_ID, TOKEN_HASH)).thenThrow(failure);

        IllegalStateException result = assertThrows(IllegalStateException.class, this::recordLogin);

        assertSame(failure, result.getCause());
        assertEquals(3, new HashSet<>(statuses).size());
        for (TransactionStatus status : statuses) verify(transactions).rollback(status);
        verify(transactions, never()).commit(any());
        verifyNoInteractions(events);
    }

    /** Failed acknowledgment is rolled back instead of reporting an updated event to the caller. */
    @Test
    void mutationFailureRollsBackAndPreservesNeutralException() {
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(events.findForUpdate(USER_ID, DOMAIN_ID, EVENT_ID, Instant.ofEpochMilli(CUTOFF))).thenThrow(failure);

        IllegalStateException result = assertThrows(IllegalStateException.class,
            () -> service.confirm(USER_ID, DOMAIN_ID, EVENT_ID, NOW));

        assertSame(failure, result.getCause());
        verify(transactions).rollback(statuses.get(0));
        verify(transactions, never()).commit(any());
    }

    /** Read failures follow the same neutral service contract used by dashboard error handling. */
    @Test
    void readFailureIsTranslatedToUnavailableHistory() {
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(events.findByUserIdAndDomainIdAndIdAndCreatedAtAfter(USER_ID, DOMAIN_ID, EVENT_ID, Instant.ofEpochMilli(CUTOFF)))
            .thenThrow(failure);

        IllegalStateException result = assertThrows(IllegalStateException.class,
            () -> service.findEvent(USER_ID, DOMAIN_ID, EVENT_ID, NOW));

        assertSame(failure, result.getCause());
        verifyNoInteractions(devices, transactions);
    }

    /** Device retention may vary by configuration while email events always retain their fixed 90 days. */
    @Test
    void cleanupUsesIndependentEventAndDeviceCutoffsInOneTransaction() {
        long deviceCutoff = NOW - Duration.ofDays(180).toMillis();

        service.cleanup(NOW, deviceCutoff);

        verify(events).deleteExpired(Instant.ofEpochMilli(CUTOFF));
        verify(devices).deleteExpired(Instant.ofEpochMilli(deviceCutoff));
        verify(transactions).commit(statuses.get(0));
        assertEquals(1, statuses.size());
    }

    private static Stream<RuntimeException> retryableFailures() {
        return Stream.concat(uniqueInsertFailures(), Stream.of(new CannotAcquireLockException("Concurrent login"),
            wrappedSqlFailure(new SQLException("Transaction rolled back by deadlock", "40001"))));
    }

    private static Stream<RuntimeException> uniqueInsertFailures() {
        return Stream.of(new DataIntegrityViolationException("Duplicate device"),
            wrappedSqlFailure(new SQLIntegrityConstraintViolationException("Duplicate device", "23000")));
    }

    private static Stream<RuntimeException> nonRetryableFailures() {
        return Stream.of(new DataAccessResourceFailureException("Database unavailable"),
            wrappedSqlFailure(new SQLNonTransientConnectionException("Connection unavailable", "08006")));
    }

    private static JpaSystemException wrappedSqlFailure(SQLException exception) {
        return new JpaSystemException(DatabaseException.sqlException(exception));
    }

    private LoginEvent recordLogin() {
        return service.recordLogin(USER_ID, DOMAIN_ID, TOKEN_HASH, NOW, CUTOFF,
            "Firefox", "131.0", "Windows 11", "192.0.2.1");
    }

    private void ownedEvent(LoginEventEntity event) {
        when(events.findForUpdate(USER_ID, DOMAIN_ID, EVENT_ID, Instant.ofEpochMilli(CUTOFF))).thenReturn(Optional.of(event));
    }

    private static DeviceEntity device(long lastSeen, Long revokedAt) {
        DeviceEntity device = new DeviceEntity();
        device.setUserId(USER_ID);
        device.setDomainId(DOMAIN_ID);
        device.setTokenHash(TOKEN_HASH);
        device.setLastSeen(Instant.ofEpochMilli(lastSeen));
        device.setRevokedAt(revokedAt == null ? null : Instant.ofEpochMilli(revokedAt));
        return device;
    }

    private static LoginEventEntity event(long createdAt) {
        LoginEventEntity event = new LoginEventEntity();
        event.setId(EVENT_ID);
        event.setUserId(USER_ID);
        event.setDomainId(DOMAIN_ID);
        event.setTokenHash(TOKEN_HASH);
        event.setCreatedAt(Instant.ofEpochMilli(createdAt));
        event.setBrowserName("Firefox");
        event.setBrowserVersion("131.0");
        event.setOperatingSystem("Windows 11");
        event.setIpAddress("192.0.2.1");
        return event;
    }
}
