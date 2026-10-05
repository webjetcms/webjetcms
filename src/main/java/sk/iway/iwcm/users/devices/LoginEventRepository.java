package sk.iway.iwcm.users.devices;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data persistence for login snapshots; every account-facing lookup includes its owner and domain. */
@Repository
public interface LoginEventRepository extends JpaRepository<LoginEventEntity, String> {
    List<LoginEventEntity> findByUserIdAndDomainIdAndConfirmedAtIsNullAndCreatedAtAfterOrderByCreatedAtDescIdAsc(
        int userId, int domainId, Instant cutoff);

    Optional<LoginEventEntity> findByUserIdAndDomainIdAndIdAndCreatedAtAfter(int userId, int domainId,
        String id, Instant cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM LoginEventEntity e WHERE e.userId=:userId AND e.domainId=:domainId AND e.id=:id AND e.createdAt>:cutoff")
    Optional<LoginEventEntity> findForUpdate(@Param("userId") int userId, @Param("domainId") int domainId,
        @Param("id") String id, @Param("cutoff") Instant cutoff);

    @Modifying
    @Query("DELETE FROM LoginEventEntity e WHERE e.createdAt<=:cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
