package sk.iway.iwcm.users.devices;

import java.time.Instant;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

import lombok.Getter;
import lombok.Setter;

/** Stores the original login snapshot and its independently mutable acknowledgment state. */
@Entity
@Table(name = "user_login_events")
@Cacheable(false)
@Getter
@Setter
public class LoginEventEntity implements Persistable<String> {
    @Id
    @Column(name = "event_id", nullable = false, length = 36, updatable = false)
    private String id;

    @Column(name = "domain_id", nullable = false, updatable = false)
    private int domainId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private int userId;

    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "browser_name", length = 128, updatable = false)
    private String browserName;

    @Column(name = "browser_version", length = 64, updatable = false)
    private String browserVersion;

    @Column(name = "operating_system", length = 128, updatable = false)
    private String operatingSystem;

    @Column(name = "ip_address", length = 64, updatable = false)
    private String ipAddress;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "reported_at")
    private Instant reportedAt;

    @Transient
    private boolean newEntity = true;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    private void markPersisted() {
        newEntity = false;
    }
}
