package sk.iway.iwcm.components.insertScript;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface InsertScriptRepository extends DomainIdRepository<InsertScriptBean, Long> {

    List<InsertScriptBean> findDistinctAllByPositionLikeAndDomainId(String position, Integer domainId);

    @Query("SELECT MAX(e.sortPriority) FROM InsertScriptBean e WHERE e.position = :position AND e.domainId = :domainId")
    Integer findMaxSortPriorityByPositionAndDomainId(@Param("position") String position, @Param("domainId") int domainId);

}
