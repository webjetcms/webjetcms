package sk.iway.iwcm.users.devices;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

/** Recognizes a user's browsers and maintains one current notice per device. */
@Service
public class DeviceService {
    static final long NOTICE_AGE = Duration.ofDays(7).toMillis();

    private final DeviceRepository devices;

    public DeviceService(DeviceRepository devices) {
        this.devices = devices;
    }

    /**
     * Refreshes recognition and replaces the device's notice when it is no longer recognized.
     *
     * @return the device with a new notice, or null when the device was already recognized
     */
    public DeviceEntity recordLogin(int userId, String tokenHash, long now, long knownSinceCutoff,
        String browserName, String browserVersion, String operatingSystem, String ipAddress) {
        return execute(() -> {
            DeviceEntity device = devices.findByUserIdAndTokenHash(userId, tokenHash).orElse(null);
            boolean recognized = device != null && device.getReportedAt() == null
                && device.getLastSeen().toEpochMilli() > knownSinceCutoff;
            if (device == null) {
                device = new DeviceEntity();
                device.setUserId(userId);
                device.setTokenHash(tokenHash);
            }
            device.setLastSeen(Instant.ofEpochMilli(now));
            if (!recognized) {
                device.setCreateDate(Instant.ofEpochMilli(now));
                device.setBrowserName(browserName);
                device.setBrowserVersion(browserVersion);
                device.setOperatingSystem(operatingSystem);
                device.setIpAddress(ipAddress);
                device.setConfirmedAt(null);
                device.setReportedAt(null);
            }
            DeviceEntity saved = devices.save(device);
            return recognized ? null : saved;
        });
    }

    /** Returns devices with unconfirmed notices within the original seven-day notice period. */
    public List<DeviceEntity> findActive(int userId, long now) {
        return execute(() -> devices.findByUserIdAndConfirmedAtIsNullAndCreateDateAfterOrderByCreateDateDescIdAsc(
            userId, Instant.ofEpochMilli(now - NOTICE_AGE)));
    }

    /** Returns the device's current notice while the owned device record exists. */
    public DeviceEntity findEvent(int userId, long id) {
        return execute(() -> devices.findByUserIdAndId(userId, id).orElse(null));
    }

    /** Resolves an existing browser after a recognized login without changing its notice. */
    public DeviceEntity findByTokenHash(int userId, String tokenHash) {
        return execute(() -> devices.findByUserIdAndTokenHash(userId, tokenHash).orElse(null));
    }

    /** Loads current confirmation state for the account's session devices in one query. */
    public List<DeviceEntity> findByIds(int userId, Set<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return execute(() -> devices.findByUserIdAndIdIn(userId, ids));
    }

    /** Acknowledges an owned event without changing browser recognition or its last-login time. */
    public DeviceEntity confirm(int userId, long id, long now) {
        return execute(() -> {
            DeviceEntity device = devices.findByUserIdAndId(userId, id).orElse(null);
            if (device == null) return null;
            if (device.getConfirmedAt() == null) {
                device.setConfirmedAt(Instant.ofEpochMilli(now));
                devices.save(device);
            }
            return device;
        });
    }

    /** Reports an event, retaining its notice and revoking only that account's browser recognition. */
    public DeviceEntity report(int userId, long id, long now) {
        return execute(() -> {
            DeviceEntity device = devices.findByUserIdAndId(userId, id).orElse(null);
            if (device == null) return null;
            if (device.getReportedAt() == null) {
                device.setReportedAt(Instant.ofEpochMilli(now));
                device.setConfirmedAt(null);
                devices.save(device);
            }
            return device;
        });
    }

    /** Removes inactive devices and their notices in one bulk delete. */
    public void cleanup(long knownSinceCutoff) {
        execute(() -> {
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
}
