package sk.iway.iwcm.rag.pgvector;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

/**
 * Spring Data repository for {@link EmbeddingChunkEntity}.
 * Provides JPA query methods for managing embedding chunks in the rag_embedding_chunks table.
 * Note: the actual embedding vector column is managed via native SQL in {@link sk.iway.iwcm.rag.vectorstore.PgVectorStore}.
 */
@Repository
public interface EmbeddingChunkRepository extends DomainIdRepository<EmbeddingChunkEntity, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM EmbeddingChunkEntity c WHERE c.entityType = :entityType AND c.entityId = :entityId AND c.domainId = :domainId")
    void deleteByEntityTypeAndEntityIdAndDomainId(@Param("entityType") RagEntityType entityType, @Param("entityId") Long entityId, @Param("domainId") Integer domainId);

    @Transactional
    @Modifying
    @Query("DELETE FROM EmbeddingChunkEntity c WHERE c.entityType = :entityType AND c.entityId = :entityId AND c.embeddingModel = :embeddingModel AND c.domainId = :domainId")
    void deleteByEntityTypeAndEntityIdAndEmbeddingModelAndDomainId(@Param("entityType") RagEntityType entityType, @Param("entityId") Long entityId, @Param("embeddingModel") String embeddingModel, @Param("domainId") Integer domainId);

    @Query("SELECT DISTINCT c.entityType FROM EmbeddingChunkEntity c WHERE c.domainId = :domainId")
    List<RagEntityType> findDistinctEntityTypes(@Param("domainId") Integer domainId);

    @Query("SELECT DISTINCT c.entityId FROM EmbeddingChunkEntity c WHERE c.entityType = :entityType AND c.domainId = :domainId")
    List<Integer> findDistinctEntityIdsByEntityTypeAndDomainId(RagEntityType entityType, Integer domainId);

    @Query("SELECT DISTINCT c.entityId FROM EmbeddingChunkEntity c WHERE c.entityType = sk.iway.iwcm.rag.service.RagEntityType.DOCUMENT AND (c.groupId IS NULL OR c.rootGroupL1 IS NULL OR c.rootGroupL2 IS NULL OR c.rootGroupL3 IS NULL)")
    List<Long> findDistinctDocumentEntityIdsWithIncompleteGroupData();

    @Transactional
    @Modifying
    @Query("UPDATE EmbeddingChunkEntity c SET c.groupId = :groupId, c.rootGroupL1 = :rootGroupL1, c.rootGroupL2 = :rootGroupL2, c.rootGroupL3 = :rootGroupL3 WHERE c.entityType = sk.iway.iwcm.rag.service.RagEntityType.DOCUMENT AND c.entityId = :entityId")
    int updateDocumentGroupData(
        @Param("entityId") Long entityId,
        @Param("groupId") Integer groupId,
        @Param("rootGroupL1") Integer rootGroupL1,
        @Param("rootGroupL2") Integer rootGroupL2,
        @Param("rootGroupL3") Integer rootGroupL3
    );
}
