package sk.iway.iwcm.components.reservation.jpa;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface ReservationRepository extends DomainIdRepository<ReservationEntity, Long> {

    List<ReservationEntity> findAllByReservationObjectIdAndDomainId(Integer reservationObjectId, Integer domainId);

    //Get reservation where date interval is overlaping
    //Formula ((Start1 <= End2) && (End1 => Start2)) , if true, then two dates are overlaping (so input is reverse)
    List<ReservationEntity> findAllByReservationObjectIdAndDomainIdAndDateFromLessThanEqualAndDateToGreaterThanEqualAndAcceptedTrue(Integer reservationObjectId, Integer domainId, Date dateFrom, Date dateTo);
}
