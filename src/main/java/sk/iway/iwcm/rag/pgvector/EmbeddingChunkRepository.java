package sk.iway.iwcm.rag.pgvector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmbeddingChunkRepository extends JpaRepository<EmbeddingChunkEntity, Long>, JpaSpecificationExecutor<EmbeddingChunkEntity> {

    List<EmbeddingChunkEntity> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<EmbeddingChunkEntity> findByEntityTypeAndEntityIdAndEmbeddingModel(String entityType, Long entityId, String embeddingModel);

    @Modifying
    @Query("DELETE FROM EmbeddingChunkEntity c WHERE c.entityType = :entityType AND c.entityId = :entityId")
    void deleteByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    Long countByStatus(String status);
}
