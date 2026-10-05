package sk.iway.iwcm.users.devices;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data persistence for browser recognition with explicit account and domain boundaries. */
@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, DeviceId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DeviceEntity d WHERE d.userId=:userId AND d.domainId=:domainId AND d.tokenHash=:tokenHash")
    Optional<DeviceEntity> findForUpdate(@Param("userId") int userId, @Param("domainId") int domainId,
        @Param("tokenHash") String tokenHash);

    @Modifying
    @Query("DELETE FROM DeviceEntity d WHERE d.lastSeen<=:cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
