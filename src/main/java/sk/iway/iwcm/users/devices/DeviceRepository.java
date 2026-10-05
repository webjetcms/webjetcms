package sk.iway.iwcm.users.devices;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Spring Data persistence for browser recognition with explicit account and domain boundaries. */
@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, DeviceId>, JpaSpecificationExecutor<DeviceEntity> {
    /** Deletes expired recognition records using the repository's bulk delete operation. */
    default long deleteExpired(Instant cutoff) {
        return delete((root, builder) -> builder.lessThanOrEqualTo(root.get("lastSeen"), cutoff));
    }
}
