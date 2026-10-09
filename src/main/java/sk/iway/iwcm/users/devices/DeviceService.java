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

/**
 * Persists browser recognition, device confirmation and blocking for individual user accounts.
 *
 * <p>Each account and browser-token hash identify one device record, which also holds its latest
 * new-device notice. Recognition depends on the last successful login, independently of whether
 * the owner has confirmed the device. Confirmation resolves the notice; blocking requires an
 * email-code challenge before that browser can complete another login. Expiration of the notice
 * alone does not remove the device or its block.
 *
 * <p>{@link AdminDeviceService} handles authentication, cookies, proof generation, email delivery
 * and session termination. This service receives account IDs and already hashed tokens or proofs,
 * applies the persistence rules and audits state changes. Repository failures are exposed as
 * {@link IllegalStateException} so callers can distinguish unavailable storage from a rejected proof.
 */
@Service
public class DeviceService {
    static final long NOTICE_AGE = Duration.ofDays(7).toMillis();
    static final long LINK_AGE = Duration.ofHours(24).toMillis();
    static final long CODE_AGE = Duration.ofMinutes(10).toMillis();
    static final long CODE_RESEND_DELAY = Duration.ofMinutes(1).toMillis();
    static final int CODE_ATTEMPTS = 5;

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /**
     * Records a successful login when no location is available.
     *
     * <p>Uses the same recognition rules as the location-aware overload and stores {@code null}
     * as the latest location, including when a previous login had a known location.
     *
     * @param userId account that completed authentication
     * @param tokenHash hash of the persistent browser-recognition token
     * @param now successful login time in epoch milliseconds
     * @param knownSinceCutoff last-login cutoff in epoch milliseconds; older or equal values require re-detection
     * @param browserName browser name to retain when the device is detected
     * @param browserVersion browser version to retain when the device is detected
     * @param operatingSystem operating system to retain when the device is detected
     * @param ipAddress IP address of this login
     * @return the newly detected or re-detected device, or {@code null} for an already recognized device
     * @throws IllegalStateException if the device is blocked or the operation fails
     */
    public DeviceEntity recordLogin(int userId, String tokenHash, long now, long knownSinceCutoff,
        String browserName, String browserVersion, String operatingSystem, String ipAddress) {
        return recordLogin(userId, tokenHash, now, knownSinceCutoff, browserName, browserVersion, operatingSystem, ipAddress, null);
    }

    /**
     * Records the latest successful login and creates a notice when browser recognition has expired.
     *
     * <p>A device is recognized when its stored {@code lastSeen} is strictly newer than the cutoff;
     * confirmation is not required for recognition. Every accepted login refreshes the time, IP and
     * location. A recognized device keeps its original browser/OS snapshot and confirmation state.
     *
     * <p>A missing device is inserted; an expired device reuses its existing record. Both cases start
     * a new notice, capture the browser/OS snapshot and clear confirmation and outstanding email proofs.
     * Returning that record tells the caller to send a notification. Blocked devices must first pass
     * the separate unblock flow and cannot be refreshed here.
     *
     * @param userId account that completed authentication
     * @param tokenHash hash of the persistent browser-recognition token
     * @param now successful login time in epoch milliseconds
     * @param knownSinceCutoff last-login cutoff in epoch milliseconds; older or equal values require re-detection
     * @param browserName browser name to retain when the device is detected
     * @param browserVersion browser version to retain when the device is detected
     * @param operatingSystem operating system to retain when the device is detected
     * @param ipAddress IP address of this login
     * @param location advisory location of this login, or {@code null} when unavailable
     * @return the newly detected or re-detected device, or {@code null} for an already recognized device
     * @throws IllegalStateException if the device is blocked or the operation fails
     */
    public DeviceEntity recordLogin(int userId, String tokenHash, long now, long knownSinceCutoff,
        String browserName, String browserVersion, String operatingSystem, String ipAddress, String location) {
        return execute(() -> {
            DeviceEntity device = deviceRepository.findByUserIdAndTokenHash(userId, tokenHash).orElse(null);
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
            DeviceEntity saved = deviceRepository.save(device);
            if (!recognized) audit(saved, "detected");
            return recognized ? null : saved;
        });
    }

    /**
     * Adds a location obtained after login without replacing a location already stored for that login.
     *
     * <p>The lookup must still show the same login timestamp and a missing location. Otherwise the
     * result is ignored, so a delayed lookup does not intentionally update a newer login. This does
     * not refresh recognition, create another notice or send another notification.
     *
     * @param userId account that owns the device
     * @param deviceId device associated with the completed login
     * @param loginTime original successful login time in epoch milliseconds
     * @param location advisory location resolved for that login
     */
    public void completeLocation(int userId, long deviceId, long loginTime, String location) {
        execute(() -> {
            DeviceEntity device = deviceRepository.findByUserIdAndId(userId, deviceId).orElse(null);
            if (device != null && device.getLastSeen().toEpochMilli() == loginTime && device.getLocation() == null) {
                device.setLocation(location);
                deviceRepository.save(device);
            }
            return null;
        });
    }

    /**
     * Finds recent device notices that still need the account owner's attention on the dashboard.
     *
     * <p>Only unconfirmed, unblocked devices detected less than seven days ago are included.
     * Repeated recognized logins update {@code lastSeen}, but do not extend this notice period.
     *
     * @param userId account whose notices are requested
     * @param now current time in epoch milliseconds, used to calculate the notice cutoff
     * @return unresolved devices ordered by detection time descending and ID ascending
     */
    public List<DeviceEntity> findActive(int userId, long now) {
        return execute(() -> deviceRepository.findByUserIdAndConfirmedAtIsNullAndReportedAtIsNullAndCreateDateAfterOrderByCreateDateDescIdAsc(
            userId, Instant.ofEpochMilli(now - NOTICE_AGE)));
    }

    /**
     * Loads an owned device for a notice detail or security action, regardless of its notice age.
     *
     * <p>Unlike {@link #findActive(int, long)}, this also returns confirmed, blocked and notice-expired
     * devices. Callers must check whether their intended action is allowed for the returned state.
     *
     * @param userId account that must own the device
     * @param id device ID referenced by the notice or action
     * @return the current device record, or {@code null} when missing or owned by another account
     */
    public DeviceEntity findEvent(int userId, long id) {
        return execute(() -> deviceRepository.findByUserIdAndId(userId, id).orElse(null));
    }

    /**
     * Resolves a browser cookie to its account-owned device record without changing device state.
     *
     * <p>Used both to check blocking before login and to associate a recognized device with the
     * completed session. A matching record alone does not establish that recognition is still valid.
     *
     * @param userId account to which the browser token belongs
     * @param tokenHash hash of the browser-recognition token, not the raw cookie value
     * @return the matching device, or {@code null} when the account has no record for this token
     */
    public DeviceEntity findByTokenHash(int userId, String tokenHash) {
        return execute(() -> deviceRepository.findByUserIdAndTokenHash(userId, tokenHash).orElse(null));
    }

    /**
     * Loads device states in one query so session listings can show current confirmation information.
     *
     * @param userId account whose devices may be returned
     * @param ids device IDs attached to the account's sessions; an empty set avoids a database query
     * @return matching owned devices, omitting missing and foreign IDs without guaranteeing order
     */
    public List<DeviceEntity> findByIds(int userId, Set<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return execute(() -> deviceRepository.findByUserIdAndIdIn(userId, ids));
    }

    /**
     * Lists retained device records for the account's device-management view.
     *
     * <p>Includes confirmed, blocked and notice-expired devices, even without an active session.
     * Pagination and ordering come from the caller rather than the active-notice rules.
     *
     * @param userId account whose devices are listed
     * @param pageable requested page, size and sort order
     * @return a page of owned device records with the total matching count
     */
    public Page<DeviceEntity> findDevices(int userId, Pageable pageable) {
        return execute(() -> deviceRepository.findAllByUserId(userId, pageable));
    }

    /**
     * Stores a confirmation proof before the caller sends its secret to the account owner's email.
     *
     * <p>Only unconfirmed, unblocked devices qualify. Links last 24 hours; codes last ten minutes
     * and use the resend and attempt limits described by {@link #issueProof(int, long, String, long, boolean, boolean)}.
     * Issuing one proof type preserves the other, allowing an existing email link to remain usable.
     *
     * @param userId account that must own the device
     * @param id device to confirm
     * @param hash hash of the new email token or code, prepared by the caller
     * @param now issue time in epoch milliseconds
     * @param code {@code true} for a numeric email code, {@code false} for an email-link token
     * @return {@code true} when stored, or {@code false} when the device state or code limits reject issuance
     */
    public boolean issueConfirmation(int userId, long id, String hash, long now, boolean code) {
        return issueProof(userId, id, hash, now, code, false);
    }

    /**
     * Stores an email code for the pending login challenge of a blocked browser.
     *
     * <p>The caller binds the code hash to the pending login and sends the email only after issuance
     * succeeds. This reuses the device's code fields and limits; it does not remove the block.
     *
     * @param userId account that must own the blocked device
     * @param id device awaiting unblock verification
     * @param hash hash of the session-bound unblock code, prepared by the caller
     * @param now issue time in epoch milliseconds
     * @return {@code true} when stored, or {@code false} when the device state or code limits reject issuance
     */
    public boolean issueUnblockCode(int userId, long id, String hash, long now) {
        return issueProof(userId, id, hash, now, true, true);
    }

    /**
     * Applies the shared issuance rules for ordinary confirmation and blocked-device login challenges.
     *
     * <p>The owned device must be unconfirmed and its blocking state must match the requested flow.
     * For an active code, issuance is rejected during the first minute or after five claimed attempts.
     * A permitted resend replaces the code and its ten-minute deadline while preserving attempts;
     * attempts reset only when the previous code has expired or has no deadline. A link instead
     * replaces only its own hash and 24-hour deadline, without using the code limits.
     *
     * @param userId account that must own the device
     * @param id device receiving the proof
     * @param hash hash to store for the selected proof type
     * @param now issue time in epoch milliseconds
     * @param code whether to issue a code instead of a link token
     * @param unblock whether this is a blocked-device challenge instead of ordinary confirmation
     * @return {@code true} when the proof is saved, or {@code false} when issuance is not allowed
     */
    private boolean issueProof(int userId, long id, String hash, long now, boolean code, boolean unblock) {
        return execute(() -> {
            DeviceEntity device = deviceRepository.findByUserIdAndId(userId, id).orElse(null);
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
            deviceRepository.save(device);
            return true;
        });
    }

    /**
     * Confirms an unblocked device using a proof delivered to the account owner's email.
     *
     * <p>Delegates proof validation and code-attempt accounting to the shared verification flow.
     * Successful confirmation clears both proof types and resolves the notice without refreshing
     * the last-login timestamp. This action cannot remove a device block.
     *
     * @param userId account that must own the device
     * @param id device to confirm
     * @param hash hash of the submitted email proof, prepared by the caller
     * @param now verification time in epoch milliseconds
     * @param code {@code true} for a numeric email code, {@code false} for an email-link token
     * @return the confirmed device, or {@code null} when the device or proof cannot be accepted
     */
    public DeviceEntity confirm(int userId, long id, String hash, long now, boolean code) {
        return verifyProof(userId, id, hash, now, code, false);
    }

    /**
     * Removes a browser's block after its pending login email code is accepted.
     *
     * <p>Also confirms the device and refreshes {@code lastSeen}, preventing immediate re-detection
     * after a long block. The caller remains responsible for completing the pending authentication.
     *
     * @param userId account that must own the blocked device
     * @param id device awaiting unblock verification
     * @param hash hash of the submitted session-bound unblock code
     * @param now verification time in epoch milliseconds
     * @return the unblocked, confirmed device, or {@code null} when the device or code cannot be accepted
     */
    public DeviceEntity unblock(int userId, long id, String hash, long now) {
        return verifyProof(userId, id, hash, now, true, true);
    }

    /**
     * Verifies a stored email proof and persists the resulting confirmation or unblock transition.
     *
     * <ol>
     * <li>Load the owned device and require an unconfirmed state, the expected blocking state and
     * an unexpired proof of the requested type.</li>
     * <li>For codes, claim one attempt through the repository's conditional increment before comparing
     * hashes. The update checks the current proof and limit in the database, so a stale attempt count
     * cannot grant another guess. A correct fifth attempt is allowed; email links skip this counter.</li>
     * <li>Compare hashes with {@link MessageDigest#isEqual(byte[], byte[])}. A mismatch returns without
     * saving the loaded entity: the attempt is already counted, and saving stale state could undo it.</li>
     * <li>On success, set confirmation, clear both proofs and save before auditing. The unblock flow
     * additionally clears the block and refreshes the recognition timestamp.</li>
     * </ol>
     *
     * <p>Only the attempt claim is atomic; the subsequent confirmation save is a separate operation.
     *
     * @param userId account that must own the device
     * @param id device being verified
     * @param hash hash of the submitted proof; {@code null} is rejected without claiming an attempt
     * @param now verification time in epoch milliseconds
     * @param code whether to verify a code instead of an email-link token
     * @param unblock whether success should remove an existing block
     * @return the saved device, or {@code null} for a missing device, incompatible state or rejected proof
     */
    private DeviceEntity verifyProof(int userId, long id, String hash, long now, boolean code, boolean unblock) {
        return execute(() -> {
            DeviceEntity device = deviceRepository.findByUserIdAndId(userId, id).orElse(null);
            if (device == null || device.getConfirmedAt() != null || (device.getReportedAt() != null) != unblock || hash == null) return null;
            String expected = code ? device.getCodeHash() : device.getConfirmationHash();
            Instant expires = code ? device.getCodeExpires() : device.getConfirmationExpires();
            if (expected == null || expires == null || expires.toEpochMilli() <= now) return null;
            if (code && deviceRepository.claimCodeAttempt(userId, id, expected, expires,
                Instant.ofEpochMilli(now), unblock, CODE_ATTEMPTS) != 1) return null;
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), hash.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            device.setConfirmedAt(Instant.ofEpochMilli(now));
            if (unblock) {
                device.setReportedAt(null);
                device.setLastSeen(Instant.ofEpochMilli(now));
            }
            clearVerification(device);
            DeviceEntity saved = deviceRepository.save(device);
            audit(saved, unblock ? "confirmed (unblock code)" : code ? "confirmed (email code)" : "confirmed (email link)");
            return saved;
        });
    }

    /**
     * Confirms a device based on a second factor already verified by the login flow.
     *
     * <p>The caller must establish that the factor succeeded; this method does not validate it.
     * A newly confirmed device has both email proofs revoked and the transition audited. An already
     * confirmed device is returned unchanged. Blocked devices still require their email-code challenge.
     *
     * @param userId account whose second factor was verified
     * @param id device used for the completed login
     * @param now confirmation time in epoch milliseconds
     * @return the confirmed device, or {@code null} when missing, foreign or blocked
     */
    public DeviceEntity confirmAfterSecondFactor(int userId, long id, long now) {
        return execute(() -> {
            DeviceEntity device = deviceRepository.findByUserIdAndId(userId, id).orElse(null);
            if (device == null || device.getReportedAt() != null) return null;
            if (device.getConfirmedAt() == null) {
                device.setConfirmedAt(Instant.ofEpochMilli(now));
                clearVerification(device);
                deviceRepository.save(device);
                audit(device, "confirmed (second factor)");
            }
            return device;
        });
    }

    /**
     * Marks an owned browser as blocked after the account owner reports an unrecognized login.
     *
     * <p>Clears confirmation and both email proofs, removing the device from active notices while
     * retaining its record for future login checks. Repeated reports preserve the original block time
     * and produce no additional audit entry. Session termination is handled by {@link AdminDeviceService}.
     *
     * @param userId account reporting its device
     * @param id device to block
     * @param now report time in epoch milliseconds
     * @return the blocked device, including an already blocked record, or {@code null} when missing or foreign
     */
    public DeviceEntity report(int userId, long id, long now) {
        return execute(() -> {
            DeviceEntity device = deviceRepository.findByUserIdAndId(userId, id).orElse(null);
            if (device == null) return null;
            if (device.getReportedAt() == null) {
                device.setReportedAt(Instant.ofEpochMilli(now));
                device.setConfirmedAt(null);
                clearVerification(device);
                deviceRepository.save(device);
                audit(device, "blocked");
            }
            return device;
        });
    }

    /**
     * Revokes both email proof types and resets attempts on the supplied in-memory entity.
     *
     * <p>Used when a notice is replaced or resolved, or the device is blocked. The caller must persist
     * the entity together with its state change; this helper performs no database write.
     *
     * @param device device whose outstanding verification data must be discarded
     */
    private static void clearVerification(DeviceEntity device) {
        device.setConfirmationHash(null);
        device.setConfirmationExpires(null);
        device.setCodeHash(null);
        device.setCodeExpires(null);
        device.setCodeAttempts(0);
    }

    /**
     * Writes a security audit entry after a device transition has been persisted.
     *
     * @param device saved device supplying the account, browser, operating system and IP metadata
     * @param action transition description without recognition tokens, proof hashes or email codes
     */
    private static void audit(DeviceEntity device, String action) {
        Adminlog.add(Adminlog.TYPE_USER_DEVICE, device.getUserId(), "Device " + action
            + ": userId=" + device.getUserId() + ", deviceId=" + device.getId()
            + ", browser=" + device.getBrowserName() + " " + device.getBrowserVersion()
            + ", operatingSystem=" + device.getOperatingSystem() + ", ipAddress=" + device.getIpAddress(), -1, -1);
    }

    /**
     * Removes unblocked devices whose last successful login is at or before the retention cutoff.
     *
     * <p>Called by {@link DeviceCleanup}; deleting a device also removes its notice and outstanding
     * proofs. Blocked records are retained regardless of age so cleanup cannot silently lift a block.
     *
     * @param knownSinceCutoff latest expired last-login time in epoch milliseconds, inclusive
     */
    public void cleanup(long knownSinceCutoff) {
        execute(() -> {
            deviceRepository.deleteExpired(Instant.ofEpochMilli(knownSinceCutoff));
            return null;
        });
    }

    /**
     * Gives persistence operations a common failure contract for dashboard and login callers.
     *
     * <p>Preserves normal results, including {@code null} or {@code false} for rejected actions,
     * and wraps runtime failures with a neutral message while retaining the cause for logging.
     * This wrapper adds no transaction boundary and does not retry the operation.
     *
     * @param <T> result type of the operation
     * @param action repository work and any associated state-change audit
     * @return the operation's result unchanged
     * @throws IllegalStateException if the operation throws a runtime exception
     */
    private <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Device history is temporarily unavailable", exception);
        }
    }
}
