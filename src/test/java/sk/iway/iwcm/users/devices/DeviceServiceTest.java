package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;

import sk.iway.iwcm.Adminlog;

/** Verifies browser recognition and the latest notice stored on the same device. */
class DeviceServiceTest {
    private static final int USER_ID = 7;
    private static final long DEVICE_ID = 42;
    private static final long NOW = 1_791_187_200_123L;
    private static final long CUTOFF = NOW - Duration.ofDays(90).toMillis();
    private static final String TOKEN_HASH = "a".repeat(64);

    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final DeviceService service = new DeviceService(devices);
    private MockedStatic<Adminlog> audit;

    @BeforeEach
    void returnSavedDeviceWithGeneratedId() {
        audit = mockStatic(Adminlog.class);
        when(devices.save(any(DeviceEntity.class))).thenAnswer(invocation -> {
            DeviceEntity device = invocation.getArgument(0);
            if (device.getId() == null) device.setId(DEVICE_ID);
            return device;
        });
    }

    @AfterEach
    void releaseAudit() {
        audit.close();
    }

    /** A new browser and its notice are saved together and exposed using the generated device ID. */
    @Test
    void newDeviceReturnsAccountOwnedNotice() {
        DeviceEntity result = recordLogin();

        ArgumentCaptor<DeviceEntity> saved = ArgumentCaptor.forClass(DeviceEntity.class);
        verify(devices).save(saved.capture());
        DeviceEntity device = saved.getValue();
        assertEquals(USER_ID, device.getUserId());
        assertEquals(TOKEN_HASH, device.getTokenHash());
        assertEquals(Instant.ofEpochMilli(NOW), device.getCreateDate());
        assertEquals(Instant.ofEpochMilli(NOW), device.getLastSeen());
        assertEquals(DEVICE_ID, result.getId());
        assertEquals(NOW, result.getCreateDate().toEpochMilli());
        assertEquals(NOW + Duration.ofDays(7).toMillis(), result.getExpiresAt());
        assertEquals("Firefox", result.getBrowserName());
        assertEquals("131.0", result.getBrowserVersion());
        assertEquals("Windows 11", result.getOperatingSystem());
        assertEquals("192.0.2.1", result.getIpAddress());
        assertNull(result.getConfirmedAt());
        assertNull(result.getReportedAt());
        verifyAudit("detected");
        audit.verifyNoMoreInteractions();
    }

    /** A regular login changes lastSeen without altering the notice or its acknowledgment. */
    @Test
    void recognizedDeviceRefreshPreservesNotice() {
        DeviceEntity device = device(NOW - Duration.ofDays(6).toMillis());
        device.setConfirmedAt(Instant.ofEpochMilli(NOW - 100));
        device.setIpAddress("192.0.2.2");
        when(devices.findByUserIdAndTokenHash(USER_ID, TOKEN_HASH)).thenReturn(Optional.of(device));

        assertNull(recordLogin());

        assertEquals(Instant.ofEpochMilli(NOW), device.getLastSeen());
        assertEquals(Instant.ofEpochMilli(NOW - Duration.ofDays(6).toMillis()), device.getCreateDate());
        assertEquals(Instant.ofEpochMilli(NOW - 100), device.getConfirmedAt());
        assertEquals("192.0.2.1", device.getIpAddress());
        verify(devices).save(device);
        audit.verifyNoInteractions();
    }

    /** Expiration renews the notice on the existing device instead of adding a history record. */
    @Test
    void deviceAtExpiryBoundaryRefreshesNotice() {
        DeviceEntity device = device(CUTOFF);
        device.setConfirmedAt(Instant.ofEpochMilli(CUTOFF + 1));
        when(devices.findByUserIdAndTokenHash(USER_ID, TOKEN_HASH)).thenReturn(Optional.of(device));

        DeviceEntity result = recordLogin();

        assertEquals(DEVICE_ID, result.getId());
        assertEquals(NOW, result.getCreateDate().toEpochMilli());
        assertNull(result.getConfirmedAt());
        assertNull(result.getReportedAt());
        verify(devices).save(device);
        verifyAudit("detected");
        audit.verifyNoMoreInteractions();
    }

    /** Recording a blocked browser cannot silently discard its block or refresh its lifetime. */
    @Test
    void reportedDeviceRemainsBlockedOnNextLogin() {
        DeviceEntity device = device(NOW - 100);
        device.setReportedAt(Instant.ofEpochMilli(NOW - 50));
        when(devices.findByUserIdAndTokenHash(USER_ID, TOKEN_HASH)).thenReturn(Optional.of(device));
        assertThrows(IllegalStateException.class, this::recordLogin);
        assertEquals(Instant.ofEpochMilli(NOW - 50), device.getReportedAt());
        assertEquals(Instant.ofEpochMilli(NOW - 100), device.getLastSeen());
        verify(devices, never()).save(any());
        audit.verifyNoInteractions();
    }

    /** Only a dedicated unblock code can clear blocking, including after browser recognition expires. */
    @Test
    void unblockCodeConfirmsTheDeviceAndPreventsImmediateRedetection() {
        DeviceEntity device = device(CUTOFF - 1);
        ownedDevice(device);
        when(devices.findByUserIdAndTokenHash(USER_ID, TOKEN_HASH)).thenReturn(Optional.of(device));
        service.report(USER_ID, DEVICE_ID, NOW - 1);
        assertNull(service.confirmAfterSecondFactor(USER_ID, DEVICE_ID, NOW));
        assertFalse(service.issueConfirmation(USER_ID, DEVICE_ID, "link", NOW, false));
        assertTrue(service.issueUnblockCode(USER_ID, DEVICE_ID, "unblock", NOW));
        assertNull(service.confirm(USER_ID, DEVICE_ID, "unblock", NOW, true));
        assertNull(service.unblock(USER_ID, DEVICE_ID, "wrong", NOW));
        assertNotNull(device.getReportedAt());
        assertNotNull(service.unblock(USER_ID, DEVICE_ID, "unblock", NOW));
        assertNull(device.getReportedAt());
        assertEquals(Instant.ofEpochMilli(NOW), device.getConfirmedAt());
        assertNull(recordLogin());
        assertNull(service.unblock(USER_ID, DEVICE_ID, "unblock", NOW + 1));
        verifyAudit("blocked");
        verifyAudit("confirmed (unblock code)");
        audit.verifyNoMoreInteractions();
    }

    /** An actual second factor confirms ordinary devices and invalidates obsolete email proofs. */
    @Test
    void secondFactorConfirmsOnlyUnblockedDevices() {
        DeviceEntity device = device(NOW - 100);
        ownedDevice(device);
        service.issueConfirmation(USER_ID, DEVICE_ID, "link", NOW, false);
        assertNotNull(service.confirmAfterSecondFactor(USER_ID, DEVICE_ID, NOW));
        assertEquals(Instant.ofEpochMilli(NOW), device.getConfirmedAt());
        assertNull(device.getConfirmationHash());
        assertNotNull(service.confirmAfterSecondFactor(USER_ID, DEVICE_ID, NOW + 1));
        service.report(USER_ID, DEVICE_ID, NOW + 1);
        assertNull(service.confirmAfterSecondFactor(USER_ID, DEVICE_ID, NOW + 2));
        assertNotNull(device.getReportedAt());
        verifyAudit("confirmed (second factor)");
        verifyAudit("blocked");
        audit.verifyNoMoreInteractions();
    }

    /** Reporting clears confirmation and leaves the original notice deadline and lastSeen unchanged. */
    @Test
    void reportingTwicePreservesOriginalNoticeAndReportTime() {
        DeviceEntity device = device(NOW - 100);
        device.setConfirmedAt(Instant.ofEpochMilli(NOW - 50));
        ownedDevice(device);

        DeviceEntity first = service.report(USER_ID, DEVICE_ID, NOW);
        DeviceEntity repeated = service.report(USER_ID, DEVICE_ID, NOW + 1);

        assertEquals(first, repeated);
        assertEquals(Instant.ofEpochMilli(NOW), first.getReportedAt());
        assertNull(first.getConfirmedAt());
        assertEquals(NOW - 100, first.getCreateDate().toEpochMilli());
        assertEquals(NOW - 100 + Duration.ofDays(7).toMillis(), first.getExpiresAt());
        assertEquals(Instant.ofEpochMilli(NOW - 100), device.getLastSeen());
        verify(devices).save(device);
        verifyAudit("blocked");
        audit.verifyNoMoreInteractions();
    }

    /** Email confirmation is single-use, account-owned and preserves the login timestamp. */
    @Test
    void linkRequiresMatchingUnexpiredProofAndCannotBeReplayed() {
        DeviceEntity device = device(NOW - 100);
        ownedDevice(device);
        assertTrue(service.issueConfirmation(USER_ID, DEVICE_ID, TOKEN_HASH, NOW, false));
        assertNull(service.confirm(USER_ID, DEVICE_ID, "wrong", NOW, false));
        assertNull(device.getConfirmedAt());
        DeviceEntity confirmed = service.confirm(USER_ID, DEVICE_ID, TOKEN_HASH, NOW + 1, false);
        assertEquals(Instant.ofEpochMilli(NOW + 1), confirmed.getConfirmedAt());
        assertEquals(Instant.ofEpochMilli(NOW - 100), confirmed.getLastSeen());
        assertNull(confirmed.getConfirmationHash());
        assertNull(service.confirm(USER_ID, DEVICE_ID, TOKEN_HASH, NOW + 2, false));
        verifyAudit("confirmed (email link)");
        audit.verifyNoMoreInteractions();
    }

    /** Expired links and reported devices cannot establish trust. */
    @Test
    void expiresAndRevokesEmailProofs() {
        DeviceEntity device = device(NOW - 100);
        ownedDevice(device);
        service.issueConfirmation(USER_ID, DEVICE_ID, TOKEN_HASH, NOW, false);
        assertNull(service.confirm(USER_ID, DEVICE_ID, TOKEN_HASH, NOW + DeviceService.LINK_AGE, false));
        service.report(USER_ID, DEVICE_ID, NOW + 1);
        assertNull(device.getConfirmationHash());
        assertFalse(service.issueConfirmation(USER_ID, DEVICE_ID, TOKEN_HASH, NOW + 2, true));
        assertNull(service.confirm(USER_ID, DEVICE_ID, TOKEN_HASH, NOW + 2, false));
    }

    /** Codes expire, have five attempts, and resends cannot bypass the per-device cooldown. */
    @Test
    void limitsCodeAttemptsAndResendsWithoutInvalidatingTheOriginalEmailLink() {
        DeviceEntity device = device(NOW - 100);
        ownedDevice(device);
        service.issueConfirmation(USER_ID, DEVICE_ID, TOKEN_HASH, NOW, false);
        assertTrue(service.issueConfirmation(USER_ID, DEVICE_ID, "code", NOW, true));
        assertFalse(service.issueConfirmation(USER_ID, DEVICE_ID, "replacement", NOW + 59_999, true));
        for (int attempt = 0; attempt < 5; attempt++) assertNull(service.confirm(USER_ID, DEVICE_ID, "wrong", NOW + 1, true));
        assertEquals(5, device.getCodeAttempts());
        assertNull(service.confirm(USER_ID, DEVICE_ID, "code", NOW + 2, true));
        assertFalse(service.issueConfirmation(USER_ID, DEVICE_ID, "replacement", NOW + 60_000, true));
        assertTrue(service.issueConfirmation(USER_ID, DEVICE_ID, "replacement", NOW + DeviceService.CODE_AGE, true));
        assertNull(service.confirm(USER_ID, DEVICE_ID, "code", NOW + 60_001, true));
        assertNull(service.confirm(USER_ID, DEVICE_ID, "replacement", NOW + 2 * DeviceService.CODE_AGE, true));
        assertEquals(TOKEN_HASH, device.getConfirmationHash());
        assertNotNull(service.confirm(USER_ID, DEVICE_ID, TOKEN_HASH, NOW + 60_002, false));
        assertNull(device.getCodeHash());
    }

    /** A valid final code attempt consumes both proofs and hides secrets from JSON. */
    @Test
    void acceptsFifthCodeAttemptAndInvalidatesEmailLink() throws Exception {
        DeviceEntity device = device(NOW - 100);
        ownedDevice(device);
        service.issueConfirmation(USER_ID, DEVICE_ID, TOKEN_HASH, NOW, false);
        service.issueConfirmation(USER_ID, DEVICE_ID, "code", NOW, true);
        String json = sk.iway.iwcm.JsonTools.objectToJSON(device);
        assertFalse(json.contains("confirmationHash"));
        assertFalse(json.contains("codeHash"));
        assertFalse(json.contains("codeAttempts"));
        for (int attempt = 0; attempt < 4; attempt++) service.confirm(USER_ID, DEVICE_ID, "wrong", NOW + 1, true);
        assertNotNull(service.confirm(USER_ID, DEVICE_ID, "code", NOW + 2, true));
        assertNull(service.confirm(USER_ID, DEVICE_ID, "code", NOW + 3, true));
        assertNull(service.confirm(USER_ID, DEVICE_ID, TOKEN_HASH, NOW + 3, false));
        verifyAudit("confirmed (email code)");
        audit.verifyNoMoreInteractions();
    }

    /** A stale snapshot cannot accept a correct code when the database refuses another attempt. */
    @Test
    void rejectedAttemptCannotConfirmOrSaveStaleDevice() {
        DeviceEntity device = device(NOW - 100);
        ownedDevice(device);
        service.issueConfirmation(USER_ID, DEVICE_ID, "code", NOW, true);
        doReturn(0).when(devices).claimCodeAttempt(USER_ID, DEVICE_ID, "code", device.getCodeExpires(),
            Instant.ofEpochMilli(NOW + 1), false, DeviceService.CODE_ATTEMPTS);
        clearInvocations(devices);

        assertNull(service.confirm(USER_ID, DEVICE_ID, "code", NOW + 1, true));

        assertEquals(0, device.getCodeAttempts(), "The loaded snapshot must remain unchanged");
        assertNull(device.getConfirmedAt());
        verify(devices, never()).save(any());
        audit.verifyNoInteractions();
    }

    /** Missing and foreign device IDs cannot be read, confirmed or reported. */
    @Test
    void inaccessibleDeviceCannotBeReadOrChanged() {
        assertNull(service.findEvent(USER_ID, DEVICE_ID));
        assertNull(service.confirm(USER_ID, DEVICE_ID, TOKEN_HASH, NOW, false));
        assertNull(service.report(USER_ID, DEVICE_ID, NOW));

        verify(devices, times(3)).findByUserIdAndId(USER_ID, DEVICE_ID);
        verify(devices, never()).save(any());
        audit.verifyNoInteractions();
    }

    /** A duplicate browser insert is reported without retrying the save. */
    @Test
    void duplicateDeviceInsertDoesNotRetry() {
        var failure = new DataIntegrityViolationException("Duplicate device");
        when(devices.save(any(DeviceEntity.class))).thenThrow(failure);

        IllegalStateException result = assertThrows(IllegalStateException.class, this::recordLogin);

        assertSame(failure, result.getCause());
        verify(devices).findByUserIdAndTokenHash(USER_ID, TOKEN_HASH);
        verify(devices).save(any(DeviceEntity.class));
        audit.verifyNoInteractions();
    }

    /** Failed acknowledgment keeps the neutral persistence error contract. */
    @Test
    void confirmationSaveFailureIsReported() {
        DeviceEntity device = device(NOW - 100);
        ownedDevice(device);
        service.issueConfirmation(USER_ID, DEVICE_ID, TOKEN_HASH, NOW, false);
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(devices.save(device)).thenThrow(failure);

        IllegalStateException result = assertThrows(IllegalStateException.class,
            () -> service.confirm(USER_ID, DEVICE_ID, TOKEN_HASH, NOW, false));

        assertSame(failure, result.getCause());
        audit.verifyNoInteractions();
    }

    /** A failed block cannot be recorded as a successful device transition. */
    @Test
    void blockingSaveFailureIsNotAudited() {
        DeviceEntity device = device(NOW - 100);
        ownedDevice(device);
        when(devices.save(device)).thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        assertThrows(IllegalStateException.class, () -> service.report(USER_ID, DEVICE_ID, NOW));

        audit.verifyNoInteractions();
    }

    /** Read failures follow the same neutral service contract used by dashboard error handling. */
    @Test
    void readFailureIsTranslatedToUnavailableHistory() {
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(devices.findByUserIdAndId(USER_ID, DEVICE_ID)).thenThrow(failure);

        IllegalStateException result = assertThrows(IllegalStateException.class,
            () -> service.findEvent(USER_ID, DEVICE_ID));

        assertSame(failure, result.getCause());
        verify(devices, never()).save(any());
    }

    private DeviceEntity recordLogin() {
        return service.recordLogin(USER_ID, TOKEN_HASH, NOW, CUTOFF,
            "Firefox", "131.0", "Windows 11", "192.0.2.1");
    }

    private void verifyAudit(String action) {
        audit.verify(() -> Adminlog.add(Adminlog.TYPE_USER_DEVICE, USER_ID,
            "Device " + action + ": userId=7, deviceId=42, browser=Firefox 131.0, operatingSystem=Windows 11, ipAddress=192.0.2.1", -1, -1));
    }

    private void ownedDevice(DeviceEntity device) {
        when(devices.findByUserIdAndId(USER_ID, DEVICE_ID)).thenReturn(Optional.of(device));
        when(devices.claimCodeAttempt(eq(USER_ID), eq(DEVICE_ID), anyString(), any(Instant.class),
            any(Instant.class), anyBoolean(), eq(DeviceService.CODE_ATTEMPTS))).thenAnswer(invocation -> {
                if (device.getCodeAttempts() >= DeviceService.CODE_ATTEMPTS) return 0;
                device.setCodeAttempts(device.getCodeAttempts() + 1);
                return 1;
            });
    }

    private static DeviceEntity device(long createdAt) {
        DeviceEntity device = new DeviceEntity();
        device.setId(DEVICE_ID);
        device.setUserId(USER_ID);
        device.setTokenHash(TOKEN_HASH);
        device.setCreateDate(Instant.ofEpochMilli(createdAt));
        device.setLastSeen(Instant.ofEpochMilli(createdAt));
        device.setBrowserName("Firefox");
        device.setBrowserVersion("131.0");
        device.setOperatingSystem("Windows 11");
        device.setIpAddress("192.0.2.1");
        return device;
    }
}
