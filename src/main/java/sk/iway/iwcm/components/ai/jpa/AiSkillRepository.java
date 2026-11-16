package sk.iway.iwcm.components.ai.jpa;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

public interface AiSkillRepository extends DomainIdRepository<AiSkillEntity, Long> {

    record SkillPreview(String name, String excerpt) {}

    @Query("select new sk.iway.iwcm.components.ai.jpa.AiSkillRepository$SkillPreview(s.name, substring(s.instructions, 1, 160)) " +
            "from AiSkillEntity s where s.domainId = ?1 order by s.name")
    List<SkillPreview> findPreviews(int domainId);

    @Query("select s.name from AiSkillEntity s where s.domainId = ?1 and s.name in ?2")
    List<String> findLinkedNames(int domainId, Collection<String> names);

    List<AiSkillEntity> findByDomainIdAndNameInOrderByNameAsc(int domainId, Collection<String> names);

    boolean existsByDomainIdAndNameIgnoreCase(int domainId, String name);
}
