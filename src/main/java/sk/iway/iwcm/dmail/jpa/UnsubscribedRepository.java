package sk.iway.iwcm.dmail.jpa;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

/**
 * Odhlasene emaily z hromadneho emailu
 */

@Repository
public interface UnsubscribedRepository extends DomainIdRepository<UnsubscribedEntity, Long> {

}