package sk.iway.iwcm.users.devices;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import sk.iway.iwcm.Adminlog;

/** Recognizes a user's browsers and maintains one current notice per device. */
@Service
public class DeviceService {
    static final long NOTICE_AGE = Duration.ofDays(7).toMillis();
    static final long LINK_AGE = Duration.ofHours(24).toMillis();
    static final long CODE_AGE = Duration.ofMinutes(10).toMillis();
    static final long CODE_RESEND_DELAY = Duration.ofMinutes(1).toMillis();
    static final int CODE_ATTEMPTS = 5;

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
        return recordLogin(userId, tokenHash, now, knownSinceCutoff, browserName, browserVersion, operatingSystem, ipAddress, null);
    }

    /** Refreshes the last-login IP and optional location together, even for already recognized browsers. */
    public DeviceEntity recordLogin(int userId, String tokenHash, long now, long knownSinceCutoff,
        String browserName, String browserVersion, String operatingSystem, String ipAddress, String location) {
        return execute(() -> {
            DeviceEntity device = devices.findByUserIdAndTokenHash(userId, tokenHash).orElse(null);
            if (device != null && device.getReportedAt() != null)
                throw new IllegalStateException("Blocked device must be verified before recording login");
            boolean recognized = device != null && device.getLastSeen().toEpochMilli() > knownSinceCutoff;
            if (device == null) {
                device = new DeviceEntity();
                device.setUserId(userId);
                device.setTokenHash(tokenHash);
            }
            device.setLastSeen(Instant.ofEpochMilli(now));
            device.setIpAddress(ipAddress);
            device.setLocation(location);
            if (!recognized) {
                device.setCreateDate(Instant.ofEpochMilli(now));
                device.setBrowserName(browserName);
                device.setBrowserVersion(browserVersion);
                device.setOperatingSystem(operatingSystem);
                device.setConfirmedAt(null);
                clearVerification(device);
            }
            DeviceEntity saved = devices.save(device);
            if (!recognized) audit(saved, "detected");
            return recognized ? null : saved;
        });
    }

    /** Fills a missing location only while this login remains the device's latest login. */
    public void completeLocation(int userId, long deviceId, long loginTime, String location) {
        execute(() -> {
            DeviceEntity device = devices.findByUserIdAndId(userId, deviceId).orElse(null);
            if (device != null && device.getLastSeen().toEpochMilli() == loginTime && device.getLocation() == null) {
                device.setLocation(location);
                devices.save(device);
            }
            return null;
        });
    }

    /** Returns unresolved device notices within the original seven-day notice period. */
    public List<DeviceEntity> findActive(int userId, long now) {
        return execute(() -> devices.findByUserIdAndConfirmedAtIsNullAndReportedAtIsNullAndCreateDateAfterOrderByCreateDateDescIdAsc(
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

    /** Lists owned devices, including confirmed, blocked and notice-expired records. */
    public Page<DeviceEntity> findDevices(int userId, Pageable pageable) {
        return execute(() -> devices.findAllByUserId(userId, pageable));
    }

    /** Stores an expiring email proof for an unconfirmed device. */
    public boolean issueConfirmation(int userId, long id, String hash, long now, boolean code) {
        return issueProof(userId, id, hash, now, code, false);
    }

    /** Issues a code for a blocked device, keeping the same resend and attempt limits. */
    public boolean issueUnblockCode(int userId, long id, String hash, long now) {
        return issueProof(userId, id, hash, now, true, true);
    }

    private boolean issueProof(int userId, long id, String hash, long now, boolean code, boolean unblock) {
        return execute(() -> {
            DeviceEntity device = devices.findByUserIdAndId(userId, id).orElse(null);
            if (device == null || device.getConfirmedAt() != null || (device.getReportedAt() != null) != unblock) return false;
            if (code) {
                if (device.getCodeExpires() != null && device.getCodeExpires().toEpochMilli() > now) {
                    if (device.getCodeAttempts() >= CODE_ATTEMPTS
                        || device.getCodeExpires().toEpochMilli() - CODE_AGE + CODE_RESEND_DELAY > now) return false;
                } else device.setCodeAttempts(0);
                device.setCodeHash(hash);
                device.setCodeExpires(Instant.ofEpochMilli(now + CODE_AGE));
            } else {
                device.setConfirmationHash(hash);
                device.setConfirmationExpires(Instant.ofEpochMilli(now + LINK_AGE));
            }
            devices.save(device);
            return true;
        });
    }

    /** Confirms a device using its current email proof and records failed code attempts. */
    public DeviceEntity confirm(int userId, long id, String hash, long now, boolean code) {
        return verifyProof(userId, id, hash, now, code, false);
    }

    /** Unblocks and confirms the browser only after its login code succeeds. */
    public DeviceEntity unblock(int userId, long id, String hash, long now) {
        return verifyProof(userId, id, hash, now, true, true);
    }

    private DeviceEntity verifyProof(int userId, long id, String hash, long now, boolean code, boolean unblock) {
        return execute(() -> {
            DeviceEntity device = devices.findByUserIdAndId(userId, id).orElse(null);
            if (device == null || device.getConfirmedAt() != null || (device.getReportedAt() != null) != unblock || hash == null) return null;
            String expected = code ? device.getCodeHash() : device.getConfirmationHash();
            Instant expires = code ? device.getCodeExpires() : device.getConfirmationExpires();
            if (expected == null || expires == null || expires.toEpochMilli() <= now) return null;
            if (code) {
                if (device.getCodeAttempts() >= CODE_ATTEMPTS) return null;
                device.setCodeAttempts(device.getCodeAttempts() + 1);
            }
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), hash.getBytes(StandardCharsets.UTF_8))) {
                if (code) devices.save(device);
                return null;
            }
            device.setConfirmedAt(Instant.ofEpochMilli(now));
            if (unblock) {
                device.setReportedAt(null);
                device.setLastSeen(Instant.ofEpochMilli(now));
            }
            clearVerification(device);
            DeviceEntity saved = devices.save(device);
            audit(saved, unblock ? "confirmed (unblock code)" : code ? "confirmed (email code)" : "confirmed (email link)");
            return saved;
        });
    }

    /** Confirms an unblocked device after a second factor was verified during this login. */
    public DeviceEntity confirmAfterSecondFactor(int userId, long id, long now) {
        return execute(() -> {
            DeviceEntity device = devices.findByUserIdAndId(userId, id).orElse(null);
            if (device == null || device.getReportedAt() != null) return null;
            if (device.getConfirmedAt() == null) {
                device.setConfirmedAt(Instant.ofEpochMilli(now));
                clearVerification(device);
                devices.save(device);
                audit(device, "confirmed (second factor)");
            }
            return device;
        });
    }

    /** Blocks the account's browser and revokes its outstanding email proofs. */
    public DeviceEntity report(int userId, long id, long now) {
        return execute(() -> {
            DeviceEntity device = devices.findByUserIdAndId(userId, id).orElse(null);
            if (device == null) return null;
            if (device.getReportedAt() == null) {
                device.setReportedAt(Instant.ofEpochMilli(now));
                device.setConfirmedAt(null);
                clearVerification(device);
                devices.save(device);
                audit(device, "blocked");
            }
            return device;
        });
    }

    private static void clearVerification(DeviceEntity device) {
        device.setConfirmationHash(null);
        device.setConfirmationExpires(null);
        device.setCodeHash(null);
        device.setCodeExpires(null);
        device.setCodeAttempts(0);
    }

    /** Audits persisted device transitions without recording recognition tokens or verification proofs. */
    private static void audit(DeviceEntity device, String action) {
        Adminlog.add(Adminlog.TYPE_USER_DEVICE, device.getUserId(), "Device " + action
            + ": userId=" + device.getUserId() + ", deviceId=" + device.getId()
            + ", browser=" + device.getBrowserName() + " " + device.getBrowserVersion()
            + ", operatingSystem=" + device.getOperatingSystem() + ", ipAddress=" + device.getIpAddress(), -1, -1);
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
