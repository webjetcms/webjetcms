package sk.iway.iwcm.components.gdpr.model;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface GdprRegExpRepository extends DomainIdRepository<GdprRegExpBean, Long> {
}
