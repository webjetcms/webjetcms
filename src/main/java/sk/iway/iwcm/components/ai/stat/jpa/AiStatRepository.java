package sk.iway.iwcm.components.ai.stat.jpa;

import java.util.Date;
import java.util.List;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

public interface AiStatRepository extends DomainIdRepository<AiStatEntity, Long>{
    List<AiStatEntity> findAllByCreatedBetweenAndDomainId(Date from, Date to, Integer domainId);
}