package sk.iway.iwcm.users.devices;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Stores browser recognition and notices, with every account-facing lookup scoped by user ID. */
@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, Long>, JpaSpecificationExecutor<DeviceEntity> {
    Optional<DeviceEntity> findByUserIdAndTokenHash(int userId, String tokenHash);

    Optional<DeviceEntity> findByUserIdAndId(int userId, long id);

    List<DeviceEntity> findByUserIdAndIdIn(int userId, Set<Long> ids);

    Page<DeviceEntity> findAllByUserId(int userId, Pageable pageable);

    List<DeviceEntity> findByUserIdAndConfirmedAtIsNullAndReportedAtIsNullAndCreateDateAfterOrderByCreateDateDescIdAsc(int userId, Instant cutoff);

    /**
     * Claims one attempt only while the same code remains active and below its attempt limit.
     *
     * @return {@code 1} when the attempt was counted, otherwise {@code 0}
     */
    @Transactional(transactionManager = "webjet2022TransactionManager")
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE DeviceEntity d SET d.codeAttempts = d.codeAttempts + 1
        WHERE d.userId = :userId AND d.id = :id
          AND d.codeHash = :expectedHash AND d.codeExpires = :expectedExpires
          AND d.codeExpires > :now AND d.codeAttempts < :maxAttempts
          AND d.confirmedAt IS NULL
          AND ((:unblock = false AND d.reportedAt IS NULL)
            OR (:unblock = true AND d.reportedAt IS NOT NULL))
        """)
    int claimCodeAttempt(@Param("userId") int userId, @Param("id") long id,
        @Param("expectedHash") String expectedHash, @Param("expectedExpires") Instant expectedExpires,
        @Param("now") Instant now, @Param("unblock") boolean unblock, @Param("maxAttempts") int maxAttempts);

    /** Deletes expired recognition records using the repository's bulk delete operation. */
    default long deleteExpired(Instant cutoff) {
        return delete((root, builder) -> builder.and(builder.isNull(root.get("reportedAt")),
            builder.lessThanOrEqualTo(root.get("lastSeen"), cutoff)));
    }
}
