package sk.iway.iwcm.users.devices;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Stores browser recognition and notices, with every account-facing lookup scoped by user ID. */
@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, Long>, JpaSpecificationExecutor<DeviceEntity> {
    Optional<DeviceEntity> findByUserIdAndTokenHash(int userId, String tokenHash);

    Optional<DeviceEntity> findByUserIdAndId(int userId, long id);

    List<DeviceEntity> findByUserIdAndIdIn(int userId, Set<Long> ids);

    List<DeviceEntity> findByUserIdAndConfirmedAtIsNullAndCreateDateAfterOrderByCreateDateDescIdAsc(int userId, Instant cutoff);

    /** Deletes expired recognition records using the repository's bulk delete operation. */
    default long deleteExpired(Instant cutoff) {
        return delete((root, builder) -> builder.lessThanOrEqualTo(root.get("lastSeen"), cutoff));
    }
}
