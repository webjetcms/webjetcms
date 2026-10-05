package sk.iway.iwcm.users.devices;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.support.SQLExceptionSubclassTranslator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Account-independent browser history; callers supply the authenticated account and domain. */
@Service
public class DeviceService {
    static final long NOTICE_AGE = Duration.ofDays(7).toMillis();
    static final long EVENT_AGE = Duration.ofDays(90).toMillis();
    private static final int LOGIN_ATTEMPTS = 3;

    private final DeviceRepository devices;
    private final LoginEventRepository events;
    private final TransactionTemplate transaction;

    public DeviceService(DeviceRepository devices, LoginEventRepository events,
        @Qualifier("webjet2022TransactionManager") PlatformTransactionManager transactionManager) {
        this.devices = devices;
        this.events = events;
        transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Commits recognition and a new event atomically before the caller sends its notification.
     * Existing devices are locked across application nodes. Concurrent first inserts retry in a fresh
     * transaction after the unique key or a database lock resolves the race.
     *
     * @return the new login snapshot, or null when the device was already recognized
     */
    public LoginEvent recordLogin(int userId, int domainId, String tokenHash, long now, long knownSinceCutoff,
        String browserName, String browserVersion, String operatingSystem, String ipAddress) {
        for (int attempt = 1; ; attempt++) {
            try {
                return transaction.execute(status -> {
                    DeviceEntity device = devices.findForUpdate(userId, domainId, tokenHash).orElse(null);
                    boolean recognized = device != null && device.getRevokedAt() == null
                        && device.getLastSeen().toEpochMilli() > knownSinceCutoff;
                    if (device == null) {
                        device = new DeviceEntity();
                        device.setUserId(userId);
                        device.setDomainId(domainId);
                        device.setTokenHash(tokenHash);
                        device.setLastSeen(Instant.ofEpochMilli(now));
                        devices.saveAndFlush(device);
                    } else {
                        device.setLastSeen(Instant.ofEpochMilli(Math.max(now, device.getLastSeen().toEpochMilli())));
                        device.setRevokedAt(null);
                    }
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
            } catch (RuntimeException exception) {
                if (attempt == LOGIN_ATTEMPTS || !retryable(exception)) throw unavailable(exception);
            }
        }
    }

    /** Returns unconfirmed login snapshots within the original seven-day notice period. */
    public List<LoginEvent> findActive(int userId, int domainId, long now) {
        return read(() -> events.findByUserIdAndDomainIdAndConfirmedAtIsNullAndCreatedAtAfterOrderByCreatedAtDescIdAsc(
            userId, domainId, Instant.ofEpochMilli(now - NOTICE_AGE)).stream().map(DeviceService::snapshot).toList());
    }

    /** Returns a retained owned event without changing either recognition or acknowledgment. */
    public LoginEvent findEvent(int userId, int domainId, String id, long now) {
        return read(() -> events.findByUserIdAndDomainIdAndIdAndCreatedAtAfter(userId, domainId, id,
            Instant.ofEpochMilli(now - EVENT_AGE)).map(DeviceService::snapshot).orElse(null));
    }

    /** Acknowledges an owned event without changing browser recognition or its last-login time. */
    public LoginEvent confirm(int userId, int domainId, String id, long now) {
        return write(() -> {
            LoginEventEntity event = events.findForUpdate(userId, domainId, id, Instant.ofEpochMilli(now - EVENT_AGE)).orElse(null);
            if (event == null) return null;
            if (event.getConfirmedAt() == null) event.setConfirmedAt(Instant.ofEpochMilli(now));
            return snapshot(event);
        });
    }

    /** Reports an event once, retaining its notice and revoking only that account's browser recognition. */
    public LoginEvent report(int userId, int domainId, String id, long now) {
        return write(() -> {
            LoginEventEntity event = events.findForUpdate(userId, domainId, id, Instant.ofEpochMilli(now - EVENT_AGE)).orElse(null);
            if (event == null) return null;
            if (event.getReportedAt() == null) {
                devices.findForUpdate(userId, domainId, event.getTokenHash()).ifPresent(device -> device.setRevokedAt(Instant.ofEpochMilli(now)));
                event.setReportedAt(Instant.ofEpochMilli(now));
                event.setConfirmedAt(null);
            }
            return snapshot(event);
        });
    }

    /** Removes expired records in bulk while preserving the independent event-retention window. */
    public void cleanup(long now, long knownSinceCutoff) {
        write(() -> {
            events.deleteExpired(Instant.ofEpochMilli(now - EVENT_AGE));
            devices.deleteExpired(Instant.ofEpochMilli(knownSinceCutoff));
            return null;
        });
    }

    private <T> T write(Supplier<T> action) {
        try {
            return transaction.execute(status -> action.get());
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    private <T> T read(Supplier<T> action) {
        try {
            return action.get();
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    private static IllegalStateException unavailable(RuntimeException exception) {
        return new IllegalStateException("Device history is temporarily unavailable", exception);
    }

    /** EclipseLink can wrap constraint and deadlock failures without translating them to Spring's categories. */
    private static boolean retryable(RuntimeException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof DataIntegrityViolationException || cause instanceof ConcurrencyFailureException) return true;
            if (cause instanceof SQLException sqlException) {
                DataAccessException translated = new SQLExceptionSubclassTranslator().translate("Save login device", null, sqlException);
                return translated instanceof DataIntegrityViolationException || translated instanceof ConcurrencyFailureException;
            }
        }
        return false;
    }

    private static LoginEvent snapshot(LoginEventEntity event) {
        long createdAt = event.getCreatedAt().toEpochMilli();
        return new LoginEvent(event.getId(), createdAt, createdAt + NOTICE_AGE, event.getBrowserName(), event.getBrowserVersion(),
            event.getOperatingSystem(), event.getIpAddress(), event.getConfirmedAt() == null ? null : event.getConfirmedAt().toEpochMilli(),
            event.getReportedAt() == null ? null : event.getReportedAt().toEpochMilli());
    }
}
