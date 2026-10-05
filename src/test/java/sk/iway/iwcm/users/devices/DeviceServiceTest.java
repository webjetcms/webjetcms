package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;

/** Verifies recognition, acknowledgment and independent repository writes. */
class DeviceServiceTest {
    private static final int USER_ID = 7;
    private static final int DOMAIN_ID = 42;
    private static final long NOW = 1_791_187_200_123L;
    private static final long CUTOFF = NOW - Duration.ofDays(90).toMillis();
    private static final String TOKEN_HASH = "a".repeat(64);
    private static final String EVENT_ID = "b60350ca-a38d-4b2e-b5b7-159f2f0f67f0";

    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final LoginEventRepository events = mock(LoginEventRepository.class);
    private final DeviceService service = new DeviceService(devices, events);

    /** Recognition and the notification snapshot are saved before a new event is returned. */
    @Test
    void newDeviceSavesAccountScopedLoginSnapshot() {
        LoginEvent result = recordLogin();

        ArgumentCaptor<DeviceEntity> device = ArgumentCaptor.forClass(DeviceEntity.class);
        ArgumentCaptor<LoginEventEntity> event = ArgumentCaptor.forClass(LoginEventEntity.class);
        var order = inOrder(devices, events);
        order.verify(devices).findById(new DeviceId(DOMAIN_ID, USER_ID, TOKEN_HASH));
        order.verify(devices).save(device.capture());
        order.verify(events).save(event.capture());
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
    }

    /** A successful login refreshes recognition while retaining the event's original notice deadline. */
    @Test
    void recognizedDeviceRefreshDoesNotCreateOrExtendNotification() {
        DeviceEntity device = device(CUTOFF + 1, null);
        LoginEventEntity event = event(NOW - Duration.ofDays(6).toMillis());
        when(devices.findById(new DeviceId(DOMAIN_ID, USER_ID, TOKEN_HASH))).thenReturn(Optional.of(device));
        when(events.findByUserIdAndDomainIdAndIdAndCreatedAtAfter(USER_ID, DOMAIN_ID, EVENT_ID, Instant.ofEpochMilli(CUTOFF)))
            .thenReturn(Optional.of(event));

        assertNull(recordLogin());
        LoginEvent original = service.findEvent(USER_ID, DOMAIN_ID, EVENT_ID, NOW);

        assertEquals(Instant.ofEpochMilli(NOW), device.getLastSeen());
        assertEquals(event.getCreatedAt().toEpochMilli() + Duration.ofDays(7).toMillis(), original.expiresAt());
        verify(events, never()).save(any());
        verify(devices).save(device);
    }

    /** Returning exactly at the configured inactivity boundary produces a new notification. */
    @Test
    void deviceAtExpiryBoundaryCreatesNewEvent() {
        DeviceEntity device = device(CUTOFF, null);
        when(devices.findById(new DeviceId(DOMAIN_ID, USER_ID, TOKEN_HASH))).thenReturn(Optional.of(device));

        assertNotNull(recordLogin());

        assertEquals(Instant.ofEpochMilli(NOW), device.getLastSeen());
        verify(events).save(any(LoginEventEntity.class));
    }

    /** A reported device is recognized anew on its next completed authentication. */
    @Test
    void revokedDeviceCreatesNewEventAndClearsRevocation() {
        DeviceEntity device = device(NOW - 1, NOW - 1);
        when(devices.findById(new DeviceId(DOMAIN_ID, USER_ID, TOKEN_HASH))).thenReturn(Optional.of(device));

        assertNotNull(recordLogin());

        assertNull(device.getRevokedAt());
        assertEquals(Instant.ofEpochMilli(NOW), device.getLastSeen());
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

        verifyNoInteractions(devices);
    }

    /** Reporting a previously confirmed event restores its notice and revokes only the matching device. */
    @Test
    void firstReportRevokesRecognitionAndPreservesOriginalNoticeWindow() {
        LoginEventEntity event = event(NOW - 100);
        event.setConfirmedAt(Instant.ofEpochMilli(NOW - 50));
        DeviceEntity device = device(NOW - 100, null);
        ownedEvent(event);
        when(devices.findById(new DeviceId(DOMAIN_ID, USER_ID, TOKEN_HASH))).thenReturn(Optional.of(device));

        LoginEvent result = service.report(USER_ID, DOMAIN_ID, EVENT_ID, NOW);

        assertEquals(NOW, result.reportedAt());
        assertNull(result.confirmedAt());
        assertEquals(NOW - 100, result.createdAt());
        assertEquals(NOW - 100 + Duration.ofDays(7).toMillis(), result.expiresAt());
        assertEquals(Instant.ofEpochMilli(NOW), device.getRevokedAt());
        assertEquals(Instant.ofEpochMilli(NOW - 100), device.getLastSeen());
        verify(events).save(event);
        verify(devices).save(device);
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
        when(events.findByUserIdAndDomainIdAndIdAndCreatedAtAfter(eq(USER_ID), eq(DOMAIN_ID), eq(EVENT_ID), any(Instant.class)))
            .thenReturn(Optional.of(event));

        LoginEvent first = service.confirm(USER_ID, DOMAIN_ID, EVENT_ID, NOW);
        LoginEvent repeated = service.confirm(USER_ID, DOMAIN_ID, EVENT_ID, NOW + 1);

        assertEquals(NOW, first.confirmedAt());
        assertEquals(first, repeated);
        verify(events).save(event);
        verifyNoInteractions(devices);
    }

    /** Missing, expired, and foreign events have the same neutral result and do not touch devices. */
    @Test
    void inaccessibleEventCannotBeConfirmedOrReported() {
        assertNull(service.confirm(USER_ID, DOMAIN_ID, EVENT_ID, NOW));
        assertNull(service.report(USER_ID, DOMAIN_ID, EVENT_ID, NOW));

        verify(events, times(2)).findByUserIdAndDomainIdAndIdAndCreatedAtAfter(USER_ID, DOMAIN_ID, EVENT_ID, Instant.ofEpochMilli(CUTOFF));
        verifyNoInteractions(devices);
        verify(events, never()).save(any());
    }

    /** A rejected event is reported after the device save, without retrying either repository operation. */
    @Test
    void eventFailureDoesNotRetryIndependentWrites() {
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(events.save(any(LoginEventEntity.class))).thenThrow(failure);

        IllegalStateException result = assertThrows(IllegalStateException.class, this::recordLogin);

        assertSame(failure, result.getCause());
        verify(devices).save(any(DeviceEntity.class));
        verify(events).save(any(LoginEventEntity.class));
    }

    /** A duplicate device insert is reported without retrying or creating a notification event. */
    @Test
    void duplicateDeviceInsertDoesNotRetry() {
        var failure = new DataIntegrityViolationException("Duplicate device");
        when(devices.save(any(DeviceEntity.class))).thenThrow(failure);

        IllegalStateException result = assertThrows(IllegalStateException.class, this::recordLogin);

        assertSame(failure, result.getCause());
        verify(devices).findById(new DeviceId(DOMAIN_ID, USER_ID, TOKEN_HASH));
        verify(devices).save(any(DeviceEntity.class));
        verifyNoInteractions(events);
    }

    /** Failed acknowledgment keeps the neutral persistence error contract. */
    @Test
    void confirmationSaveFailureIsReported() {
        LoginEventEntity event = event(NOW - 100);
        ownedEvent(event);
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(events.save(event)).thenThrow(failure);

        IllegalStateException result = assertThrows(IllegalStateException.class,
            () -> service.confirm(USER_ID, DOMAIN_ID, EVENT_ID, NOW));

        assertSame(failure, result.getCause());
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
        verifyNoInteractions(devices);
    }

    /** Device retention may vary by configuration while email events always retain their fixed 90 days. */
    @Test
    void cleanupUsesIndependentEventAndDeviceCutoffs() {
        long deviceCutoff = NOW - Duration.ofDays(180).toMillis();

        service.cleanup(NOW, deviceCutoff);

        verify(events).deleteExpired(Instant.ofEpochMilli(CUTOFF));
        verify(devices).deleteExpired(Instant.ofEpochMilli(deviceCutoff));
    }

    private LoginEvent recordLogin() {
        return service.recordLogin(USER_ID, DOMAIN_ID, TOKEN_HASH, NOW, CUTOFF,
            "Firefox", "131.0", "Windows 11", "192.0.2.1");
    }

    private void ownedEvent(LoginEventEntity event) {
        when(events.findByUserIdAndDomainIdAndIdAndCreatedAtAfter(USER_ID, DOMAIN_ID, EVENT_ID, Instant.ofEpochMilli(CUTOFF))).thenReturn(Optional.of(event));
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
