package sk.iway.iwcm.rag.vectorjpa;

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
 * Note: the actual embedding vector storage is managed via native SQL through
 * {@link sk.iway.iwcm.rag.vectorstore.VectorStore}.
 */
@Repository
public interface EmbeddingChunkRepository extends DomainIdRepository<EmbeddingChunkEntity, Long> {

    List<EmbeddingChunkEntity> findByEntityTypeAndDomainIdAndSourcePathStartingWith(
        RagEntityType entityType, Integer domainId, String sourcePath
    );

    List<EmbeddingChunkEntity> findByEntityTypeAndEntityIdAndDomainId(
        RagEntityType entityType, Long entityId, Integer domainId
    );

    /**
     * Deletes all chunks for a source in one storage domain, regardless of provider or model.
     *
     * @param entityType source entity type
     * @param entityId source identifier
     * @param domainId storage domain, including zero for shared Markdown
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM EmbeddingChunkEntity c WHERE c.entityType = :entityType AND c.entityId = :entityId AND c.domainId = :domainId")
    void deleteByEntityTypeAndEntityIdAndDomainId(@Param("entityType") RagEntityType entityType, @Param("entityId") Long entityId, @Param("domainId") Integer domainId);

    /**
     * Deletes only the selected provider and model's chunks for a source in one storage domain.
     *
     * @param entityType source entity type
     * @param entityId source identifier
     * @param embeddingProvider provider that generated the chunks
     * @param embeddingModel model that generated the chunks
     * @param domainId storage domain, including zero for shared Markdown
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM EmbeddingChunkEntity c WHERE c.entityType = :entityType AND c.entityId = :entityId AND c.embeddingProvider = :embeddingProvider AND c.embeddingModel = :embeddingModel AND c.domainId = :domainId")
    void deleteByEntityTypeAndEntityIdAndEmbeddingProviderAndEmbeddingModelAndDomainId(
        @Param("entityType") RagEntityType entityType,
        @Param("entityId") Long entityId,
        @Param("embeddingProvider") String embeddingProvider,
        @Param("embeddingModel") String embeddingModel,
        @Param("domainId") Integer domainId
    );

    /**
     * Lists source types with stored chunks in the specified storage domain.
     *
     * @param domainId storage domain, including zero for shared Markdown
     * @return distinct stored entity types
     */
    @Query("SELECT DISTINCT c.entityType FROM EmbeddingChunkEntity c WHERE c.domainId = :domainId")
    List<RagEntityType> findDistinctEntityTypes(@Param("domainId") Integer domainId);

    /**
     * Lists distinct source IDs for document queue operations in one storage domain.
     * The integer return type is intended for document IDs, not Markdown long identifiers.
     *
     * @param entityType source entity type, normally {@code DOCUMENT}
     * @param domainId storage domain
     * @return distinct source IDs represented as integers
     */
    @Query("SELECT DISTINCT c.entityId FROM EmbeddingChunkEntity c WHERE c.entityType = :entityType AND c.domainId = :domainId")
    List<Integer> findDistinctEntityIdsByEntityTypeAndDomainId(RagEntityType entityType, Integer domainId);

    /**
     * Lists source IDs with stored chunks from the selected provider and model.
     * The integer return type is intended for document IDs, not Markdown long identifiers.
     *
     * @param entityType source entity type, normally {@code DOCUMENT}
     * @param embeddingProvider provider that generated the chunks
     * @param embeddingModel model that generated the chunks
     * @param domainId storage domain
     * @return distinct source IDs represented as integers
     */
    @Query("SELECT DISTINCT c.entityId FROM EmbeddingChunkEntity c WHERE c.entityType = :entityType AND c.embeddingProvider = :embeddingProvider AND c.embeddingModel = :embeddingModel AND c.domainId = :domainId")
    List<Integer> findDistinctEntityIdsByEntityTypeAndEmbeddingProviderAndEmbeddingModelAndDomainId(
        @Param("entityType") RagEntityType entityType,
        @Param("embeddingProvider") String embeddingProvider,
        @Param("embeddingModel") String embeddingModel,
        @Param("domainId") Integer domainId
    );

    /**
     * Finds documents whose stored chunks lack one or more group-scope values across all domains.
     *
     * @return distinct document IDs requiring group metadata backfill
     */
    @Query("SELECT DISTINCT c.entityId FROM EmbeddingChunkEntity c WHERE c.entityType = sk.iway.iwcm.rag.service.RagEntityType.DOCUMENT AND (c.groupId IS NULL OR c.rootGroupL1 IS NULL OR c.rootGroupL2 IS NULL OR c.rootGroupL3 IS NULL)")
    List<Long> findDistinctDocumentEntityIdsWithIncompleteGroupData();

    /**
     * Updates group-scope metadata on all chunks of a document across providers, models, and domains.
     *
     * @param entityId document ID
     * @param groupId immediate document group ID
     * @param rootGroupL1 first-level root group ID
     * @param rootGroupL2 second-level root group ID
     * @param rootGroupL3 third-level root group ID
     * @return number of chunk rows updated
     */
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
