package sk.iway.iwcm.components.ai.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

public interface AssistantDefinitionRepository extends DomainIdRepository<AssistantDefinitionEntity, Long> {
    List<AssistantDefinitionEntity> findAllByNameLikeAndDomainId(String name, Integer domainId);

    Optional<AssistantDefinitionEntity> findByIdAndDomainId(Long id, Integer domainId);

    @Query("SELECT ade FROM AssistantDefinitionEntity ade WHERE ade.name = :name AND ade.provider = :provider AND ade.domainId = :domainId")
    List<AssistantDefinitionEntity> getEntitiesCount(@Param("name") String name, @Param("provider") String provider, @Param("domainId") Integer domainId);
}