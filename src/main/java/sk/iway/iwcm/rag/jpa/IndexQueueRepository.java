package sk.iway.iwcm.rag.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

/**
 * Spring Data repository for {@link IndexQueueEntity}.
 * Provides methods to query, insert, and delete RAG indexing queue entries.
 */
@Repository
public interface IndexQueueRepository extends DomainIdRepository<IndexQueueEntity, Long> {

    List<IndexQueueEntity> findTop500ByOrderByCreateDateAsc();

    /** @return highest queue ID present at the start of a cron run, or {@code null} for an empty queue */
    @Query("SELECT MAX(q.id) FROM IndexQueueEntity q")
    Long findMaxId();

    /**
     * Returns the next stable queue batch within a cron run's initial ID snapshot.
     * The cursor ensures failed or undeletable rows are attempted only once per run,
     * while later rows from the same snapshot can still be processed.
     *
     * @param afterId last queue ID attempted in the current run
     * @param maxId highest queue ID included in the current run
     * @return up to 500 subsequent queue rows
     */
    List<IndexQueueEntity> findTop500ByIdGreaterThanAndIdLessThanEqualOrderByIdAsc(Long afterId, Long maxId);

    @Modifying
    @Transactional
    @Query("DELETE FROM IndexQueueEntity q WHERE q.entityType = :entityType AND q.entityId = :entityId AND q.domainId = :domainId")
    void deleteByEntityTypeAndEntityId(@Param("entityType") RagEntityType entityType, @Param("entityId") Integer entityId, @Param("domainId") Integer domainId);

    @Modifying
    @Transactional
    @Query("DELETE FROM IndexQueueEntity q WHERE q.entityType = :entityType AND q.entityId IN :entityIds AND q.domainId = :domainId")
    void deleteByEntityTypeAndEntityId(@Param("entityType") RagEntityType entityType, @Param("entityIds") List<Integer> entityIds, @Param("domainId") Integer domainId);

    @Query("SELECT q.entityId FROM IndexQueueEntity q WHERE q.entityType = :entityType AND q.action = :action AND q.domainId = :domainId")
    List<Integer> findExistingEntityIds(@Param("entityType") RagEntityType entityType, @Param("action") RagIndexAction action, @Param("domainId") Integer domainId);
}
