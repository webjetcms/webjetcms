package sk.iway.iwcm.users.devices;

import java.time.Instant;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

import lombok.Getter;
import lombok.Setter;

/** Account-scoped browser recognition, always read from the database rather than a shared JPA cache. */
@Entity
@Table(name = "user_login_devices")
@IdClass(DeviceId.class)
@Cacheable(false)
@Getter
@Setter
public class DeviceEntity implements Persistable<DeviceId> {
    @Id
    @Column(name = "domain_id", nullable = false)
    private int domainId;

    @Id
    @Column(name = "user_id", nullable = false)
    private int userId;

    @Id
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Transient
    private boolean newEntity = true;

    @Override
    public DeviceId getId() {
        return new DeviceId(domainId, userId, tokenHash);
    }

    /** Assigned identifiers must use INSERT on first use so concurrent creation hits the unique key. */
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
