package sk.iway.iwcm.rag.pgvector;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface EmbeddingChunkRepository extends DomainIdRepository<EmbeddingChunkEntity, Long> {

    List<EmbeddingChunkEntity> findByEntityTypeAndEntityId(RagEntityType entityType, Long entityId);

    List<EmbeddingChunkEntity> findByEntityTypeAndEntityIdAndEmbeddingModel(RagEntityType entityType, Long entityId, String embeddingModel);

    @Modifying
    @Query("DELETE FROM EmbeddingChunkEntity c WHERE c.entityType = :entityType AND c.entityId = :entityId")
    void deleteByEntityTypeAndEntityId(@Param("entityType") RagEntityType entityType, @Param("entityId") Long entityId);

    Long countByStatus(EmbeddingChunkStatus status);

    @Query("SELECT DISTINCT c.entityType FROM EmbeddingChunkEntity c WHERE c.domainId = :domainId")
    List<RagEntityType> findDistinctEntityTypes(@Param("domainId") Integer domainId);
}
