package sk.iway.iwcm.rag.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndexQueueRepository extends JpaRepository<IndexQueueEntity, Long>, JpaSpecificationExecutor<IndexQueueEntity> {

    List<IndexQueueEntity> findTop100ByOrderByCreateDateAsc();

    @Modifying
    @Query("DELETE FROM IndexQueueEntity q WHERE q.entityType = :entityType AND q.entityId = :entityId")
    void deleteByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Integer entityId);
}
