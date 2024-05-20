package sk.iway.iwcm.components.reservation.jpa;
import java.util.List;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface ReservationObjectPriceRepository  extends DomainIdRepository<ReservationObjectPriceEntity, Long> {
    List<ReservationObjectPriceEntity> findAllByObjectIdAndDomainId(Integer objectId, Integer domainId);
}
