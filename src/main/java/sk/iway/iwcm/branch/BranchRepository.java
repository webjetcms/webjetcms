package sk.iway.iwcm.branch;

import java.sql.Timestamp;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

/** Package metadata access with explicit website domain scope. */
public interface BranchRepository extends DomainIdRepository<BranchEntity, Long>
{
    List<BranchEntity> findAllByDomainIdOrderByIdDesc(Integer domainId);
    List<BranchEntity> findAllByDomainIdAndStateNotInOrderByIdDesc(Integer domainId, List<String> states);
    List<BranchEntity> findAllByStateAndPublishAtLessThanEqualOrderByPublishAtAscIdAsc(String state, Timestamp at);
    List<BranchEntity> findAllByStateAndPublishRequestedByIsNotNullOrderByIdAsc(String state);

    @Transactional(transactionManager="webjet2022TransactionManager")
    @Modifying
    @Query("UPDATE BranchEntity b SET b.publishRequestedBy=NULL WHERE b.id=:id AND b.revision=:revision AND b.state='MERGING' AND b.publishRequestedBy=:actor")
    int takePublicationRequest(@Param("id") Long id, @Param("revision") Long revision, @Param("actor") Integer actor);

    @Transactional(transactionManager="webjet2022TransactionManager")
    @Modifying
    @Query("UPDATE BranchEntity b SET b.state='NEEDS_ATTENTION', b.revision=b.revision+1, b.lastError=:reason, b.updatedAt=:now WHERE b.id=:id AND b.revision=:revision AND b.state='SCHEDULED' AND b.publishAt<=:now")
    int markNeedsAttention(@Param("id") Long id, @Param("revision") Long revision, @Param("reason") String reason, @Param("now") Timestamp now);

    @Transactional(transactionManager="webjet2022TransactionManager")
    @Modifying
    @Query("UPDATE BranchEntity b SET b.state='MERGED', b.revision=b.revision+1, b.mergeFinishedAt=:now, b.updatedAt=:now, b.lastError=NULL, b.publishRequestedBy=NULL WHERE b.id=:id AND b.state='MERGING'")
    int markMerged(@Param("id") Long id, @Param("now") Timestamp now);

    @Transactional(transactionManager="webjet2022TransactionManager")
    @Modifying
    @Query("UPDATE BranchEntity b SET b.state=CASE WHEN b.scheduledBy IS NOT NULL AND b.publishAt IS NOT NULL AND :applied=false THEN 'NEEDS_ATTENTION' ELSE 'MERGE_FAILED' END, b.revision=b.revision+1, b.lastError=:reason, b.updatedAt=:now, b.publishRequestedBy=NULL WHERE b.id=:id AND b.state='MERGING'")
    int markPublicationFailed(@Param("id") Long id, @Param("applied") boolean applied, @Param("reason") String reason, @Param("now") Timestamp now);
}
