package sk.iway.iwcm.rag.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface IndexQueueRepository extends DomainIdRepository<IndexQueueEntity, Long> {

    List<IndexQueueEntity> findTop100ByOrderByCreateDateAsc();

    List<IndexQueueEntity> findTop100ByDomainIdOrderByCreateDateAsc(int domainId);

    @Modifying
    @Transactional
    @Query("DELETE FROM IndexQueueEntity q WHERE q.entityType = :entityType AND q.entityId = :entityId AND q.domainId = :domainId")
    void deleteByEntityTypeAndEntityId(@Param("entityType") RagEntityType entityType, @Param("entityId") Integer entityId, @Param("domainId") Integer domainId);
}