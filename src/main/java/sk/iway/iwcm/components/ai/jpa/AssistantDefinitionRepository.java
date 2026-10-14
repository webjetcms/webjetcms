package sk.iway.iwcm.components.ai.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

public interface AssistantDefinitionRepository extends DomainIdRepository<AssistantDefinitionEntity, Long> {
    List<AssistantDefinitionEntity> findAllByNameLikeAndDomainId(String name, Integer domainId);

    Optional<AssistantDefinitionEntity> findByIdAndDomainId(Long id, Integer domainId);

    Optional<AssistantDefinitionEntity> findFirstByGroupNameAndProviderAndDomainId(String groupName, String provider, Integer domainId);

    Optional<AssistantDefinitionEntity> findFirstByNameAndDomainIdOrderByIdAsc(String name, Integer domainId);

    @Query("SELECT ade FROM AssistantDefinitionEntity ade WHERE ade.name = :name AND ade.provider = :provider AND ade.domainId = :domainId")
    List<AssistantDefinitionEntity> getEntitiesCount(@Param("name") String name, @Param("provider") String provider, @Param("domainId") Integer domainId);

    Optional<AssistantDefinitionEntity> findFirstByClassNameAndDomainIdOrderByIdAsc(String className, Integer domainId);

    List<AssistantDefinitionEntity> findAllByClassNameAndDomainId(String className, Integer domainId);

    /**
     * Atomically adds token usage for an assistant with token limits enabled.
     *
     * @param assistantId assistant whose usage is being recorded
     * @param domainId domain owning the assistant
     * @param usedTokens number of tokens to add
     * @return number of updated assistants
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query("UPDATE AssistantDefinitionEntity ade SET ade.usedTokens = COALESCE(ade.usedTokens, 0) + :usedTokens WHERE ade.id = :assistantId AND ade.domainId = :domainId AND ade.tokenLimit > 0")
    int incrementUsedTokens(@Param("assistantId") Long assistantId, @Param("domainId") Integer domainId, @Param("usedTokens") Long usedTokens);

    /**
     * Reads current notification state while serializing notifications and resets for this assistant.
     * Must be called inside the transaction that sends the notification.
     */
    @Query("SELECT ade FROM AssistantDefinitionEntity ade WHERE ade.id = :assistantId AND ade.domainId = :domainId")
    Optional<AssistantDefinitionEntity> findForTokenNotification(@Param("assistantId") Long assistantId, @Param("domainId") Integer domainId);

    /**
     * Advances the internal marker after the notification is sent or queued.
     */
    @Transactional
    @Modifying
    @Query("UPDATE AssistantDefinitionEntity ade SET ade.notifLevel = :level WHERE ade.id = :assistantId AND ade.domainId = :domainId")
    void updateNotificationLevel(@Param("assistantId") Long assistantId, @Param("domainId") Integer domainId, @Param("level") Integer level);

    /**
     * Checks the stored token usage against an enabled limit in the given domain.
     * @return one if the limit is reached, otherwise zero
     */
    @Query("SELECT COUNT(ade) FROM AssistantDefinitionEntity ade WHERE ade.id = :assistantId AND ade.domainId = :domainId AND ade.tokenLimit > 0 AND COALESCE(ade.usedTokens, 0) >= ade.tokenLimit")
    long countWithReachedTokenLimit(@Param("assistantId") Long assistantId, @Param("domainId") Integer domainId);

    /**
     * Resets token usage for one assistant within its domain.
     * @return number of updated assistants
     */
    @Transactional
    @Modifying
    @Query("UPDATE AssistantDefinitionEntity ade SET ade.usedTokens = 0, ade.notifLevel = 0 WHERE ade.id = :assistantId AND ade.domainId = :domainId")
    int resetUsedTokensByIdAndDomainId(@Param("assistantId") Long assistantId, @Param("domainId") Integer domainId);

    /**
     * Resets token usage for every assistant in one domain.
     * @return number of updated assistants
     */
    @Transactional
    @Modifying
    @Query("UPDATE AssistantDefinitionEntity ade SET ade.usedTokens = 0, ade.notifLevel = 0 WHERE ade.domainId = :domainId")
    int resetUsedTokensByDomainId(@Param("domainId") Integer domainId);

    /**
     * Resets token usage for all assistants across domains.
     * @return number of updated assistants
     */
    @Transactional
    @Modifying
    @Query("UPDATE AssistantDefinitionEntity ade SET ade.usedTokens = 0, ade.notifLevel = 0")
    int resetUsedTokens();

    /** Resets notification eligibility after an editor save disables token limits. */
    @Transactional
    @Modifying
    @Query("UPDATE AssistantDefinitionEntity ade SET ade.usedTokens = 0, ade.notifLevel = 0 WHERE ade.id = :assistantId AND ade.domainId = :domainId AND ade.tokenLimit = 0")
    void resetDisabledTokenUsage(@Param("assistantId") Long assistantId, @Param("domainId") Integer domainId);
}
