package sk.iway.iwcm.dmail.jpa;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface CampaingsRepository extends DomainIdRepository<CampaingsEntity, Long> {

}