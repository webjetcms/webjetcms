package sk.iway.iwcm.components.ai.jpa;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

public interface AiSkillRepository extends DomainIdRepository<AiSkillEntity, Long> {

    @Query("select s.name from AiSkillEntity s where s.domainId = ?1 order by s.name")
    List<String> listNames(int domainId);

    @Query("select s.name from AiSkillEntity s where s.domainId = ?1 and s.name in ?2")
    List<String> findLinkedNames(int domainId, Collection<String> names);

    List<AiSkillEntity> findByDomainIdAndNameInOrderByNameAsc(int domainId, Collection<String> names);

    boolean existsByDomainIdAndNameIgnoreCase(int domainId, String name);
}
