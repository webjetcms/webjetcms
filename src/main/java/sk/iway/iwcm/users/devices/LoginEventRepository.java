package sk.iway.iwcm.users.devices;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Spring Data persistence for login snapshots; every account-facing lookup includes its owner and domain. */
@Repository
public interface LoginEventRepository extends JpaRepository<LoginEventEntity, String>, JpaSpecificationExecutor<LoginEventEntity> {
    List<LoginEventEntity> findByUserIdAndDomainIdAndConfirmedAtIsNullAndCreatedAtAfterOrderByCreatedAtDescIdAsc(
        int userId, int domainId, Instant cutoff);

    Optional<LoginEventEntity> findByUserIdAndDomainIdAndIdAndCreatedAtAfter(int userId, int domainId,
        String id, Instant cutoff);

    /** Deletes expired event history using the repository's bulk delete operation. */
    default long deleteExpired(Instant cutoff) {
        return delete((root, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), cutoff));
    }
}
