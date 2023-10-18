package sk.iway.iwcm.components.reservation.jpa;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Long>, JpaSpecificationExecutor<ReservationEntity> {

    Page<ReservationEntity> findAllByDomainId(Integer domainId, Pageable pageable);

    List<ReservationEntity> findAllByReservationObjectIdAndDomainId(Integer reservationObjectId, Integer domainId);

    //Get reservation where date interval is overlaping
    //Formula ((Start1 <= End2) && (End1 => Start2)) , if true, then two dates are overlaping (so input is reverse)
    List<ReservationEntity> findAllByReservationObjectIdAndDomainIdAndDateFromLessThanEqualAndDateToGreaterThanEqualAndAcceptedTrue(Integer reservationObjectId, Integer domainId, Date dateFrom, Date dateTo);
}
