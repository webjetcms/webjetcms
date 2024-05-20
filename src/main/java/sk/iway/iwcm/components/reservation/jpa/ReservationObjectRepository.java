package sk.iway.iwcm.components.reservation.jpa;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface ReservationObjectRepository extends DomainIdRepository<ReservationObjectEntity, Long> {
}
