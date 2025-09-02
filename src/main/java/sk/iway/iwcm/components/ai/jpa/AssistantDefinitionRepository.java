package sk.iway.iwcm.components.ai.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

public interface AssistantDefinitionRepository extends DomainIdRepository<AssistantDefinitionEntity, Long> {
    List<AssistantDefinitionEntity> findAllByNameLikeAndDomainId(String name, Integer domainId);

    @Query("SELECT oaa.name FROM AssistantDefinitionEntity oaa WHERE oaa.name LIKE :name AND oaa.domainId = :domainId")
    List<String> getAssistantNames(@Param("name") String name, @Param("domainId") Integer domainId);

    @Query("SELECT DISTINCT oaa.groupName FROM AssistantDefinitionEntity oaa WHERE oaa.groupName LIKE :groupName AND oaa.domainId = :domainId ORDER BY oaa.groupName ASC")
    List<String> getGroupNames(@Param("groupName") String groupName, @Param("domainId") Integer domainId);

    Optional<AssistantDefinitionEntity> findByIdAndDomainId(Long id, Integer domainId);
}