package sk.iway.iwcm.users.devices;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

/** Account-independent browser history; callers supply the authenticated account and domain. */
@Service
public class DeviceService {
    static final long NOTICE_AGE = Duration.ofDays(7).toMillis();
    static final long EVENT_AGE = Duration.ofDays(90).toMillis();

    private final DeviceRepository devices;
    private final LoginEventRepository events;

    public DeviceService(DeviceRepository devices, LoginEventRepository events) {
        this.devices = devices;
        this.events = events;
    }

    /**
     * Saves recognition and a new event independently before the caller sends its notification.
     *
     * @return the new login snapshot, or null when the device was already recognized
     */
    public LoginEvent recordLogin(int userId, int domainId, String tokenHash, long now, long knownSinceCutoff,
        String browserName, String browserVersion, String operatingSystem, String ipAddress) {
        return execute(() -> {
            DeviceEntity device = devices.findById(new DeviceId(domainId, userId, tokenHash)).orElse(null);
            boolean recognized = device != null && device.getRevokedAt() == null
                && device.getLastSeen().toEpochMilli() > knownSinceCutoff;
            if (device == null) {
                device = new DeviceEntity();
                device.setUserId(userId);
                device.setDomainId(domainId);
                device.setTokenHash(tokenHash);
            }
            device.setLastSeen(Instant.ofEpochMilli(now));
            device.setRevokedAt(null);
            devices.save(device);
            if (recognized) return null;

            LoginEventEntity event = new LoginEventEntity();
            event.setId(UUID.randomUUID().toString());
            event.setUserId(userId);
            event.setDomainId(domainId);
            event.setTokenHash(tokenHash);
            event.setCreatedAt(Instant.ofEpochMilli(now));
            event.setBrowserName(browserName);
            event.setBrowserVersion(browserVersion);
            event.setOperatingSystem(operatingSystem);
            event.setIpAddress(ipAddress);
            events.save(event);
            return snapshot(event);
        });
    }

    /** Returns unconfirmed login snapshots within the original seven-day notice period. */
    public List<LoginEvent> findActive(int userId, int domainId, long now) {
        return execute(() -> events.findByUserIdAndDomainIdAndConfirmedAtIsNullAndCreatedAtAfterOrderByCreatedAtDescIdAsc(
            userId, domainId, Instant.ofEpochMilli(now - NOTICE_AGE)).stream().map(DeviceService::snapshot).toList());
    }

    /** Returns a retained owned event without changing either recognition or acknowledgment. */
    public LoginEvent findEvent(int userId, int domainId, String id, long now) {
        return execute(() -> events.findByUserIdAndDomainIdAndIdAndCreatedAtAfter(userId, domainId, id,
            Instant.ofEpochMilli(now - EVENT_AGE)).map(DeviceService::snapshot).orElse(null));
    }

    /** Acknowledges an owned event without changing browser recognition or its last-login time. */
    public LoginEvent confirm(int userId, int domainId, String id, long now) {
        return execute(() -> {
            LoginEventEntity event = events.findByUserIdAndDomainIdAndIdAndCreatedAtAfter(userId, domainId, id, Instant.ofEpochMilli(now - EVENT_AGE)).orElse(null);
            if (event == null) return null;
            if (event.getConfirmedAt() == null) {
                event.setConfirmedAt(Instant.ofEpochMilli(now));
                events.save(event);
            }
            return snapshot(event);
        });
    }

    /** Reports an event, retaining its notice and revoking only that account's browser recognition. */
    public LoginEvent report(int userId, int domainId, String id, long now) {
        return execute(() -> {
            LoginEventEntity event = events.findByUserIdAndDomainIdAndIdAndCreatedAtAfter(userId, domainId, id, Instant.ofEpochMilli(now - EVENT_AGE)).orElse(null);
            if (event == null) return null;
            if (event.getReportedAt() == null) {
                devices.findById(new DeviceId(domainId, userId, event.getTokenHash())).ifPresent(device -> {
                    device.setRevokedAt(Instant.ofEpochMilli(now));
                    devices.save(device);
                });
                event.setReportedAt(Instant.ofEpochMilli(now));
                event.setConfirmedAt(null);
                events.save(event);
            }
            return snapshot(event);
        });
    }

    /** Removes expired records in bulk while preserving the independent event-retention window. */
    public void cleanup(long now, long knownSinceCutoff) {
        execute(() -> {
            events.deleteExpired(Instant.ofEpochMilli(now - EVENT_AGE));
            devices.deleteExpired(Instant.ofEpochMilli(knownSinceCutoff));
            return null;
        });
    }

    private <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Device history is temporarily unavailable", exception);
        }
    }

    private static LoginEvent snapshot(LoginEventEntity event) {
        long createdAt = event.getCreatedAt().toEpochMilli();
        return new LoginEvent(event.getId(), createdAt, createdAt + NOTICE_AGE, event.getBrowserName(), event.getBrowserVersion(),
            event.getOperatingSystem(), event.getIpAddress(), event.getConfirmedAt() == null ? null : event.getConfirmedAt().toEpochMilli(),
            event.getReportedAt() == null ? null : event.getReportedAt().toEpochMilli());
    }
}
