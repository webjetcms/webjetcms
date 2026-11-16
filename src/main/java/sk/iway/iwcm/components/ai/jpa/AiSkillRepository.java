package sk.iway.iwcm.components.ai.jpa;

import java.util.Collection;
import java.util.List;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

public interface AiSkillRepository extends DomainIdRepository<AiSkillEntity, Long> {

    /** Skill picker metadata for the assistant editor. */
    record Option(Long id, String name) {}

    List<Option> findOptionsByDomainIdOrderByNameAscIdAsc(int domainId);

    List<AiSkillEntity> findByDomainIdAndIdInOrderByIdAsc(int domainId, Collection<Long> ids);

    long countByDomainIdAndIdIn(int domainId, Collection<Long> ids);
}
