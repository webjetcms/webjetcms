package sk.iway.iwcm.components.insertScript;

import java.util.List;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface InsertScriptRepository extends DomainIdRepository<InsertScriptBean, Long> {

    List<InsertScriptBean> findDistinctAllByPositionLikeAndDomainId(String position, Integer domainId);

}
