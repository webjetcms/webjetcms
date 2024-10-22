package sk.iway.iwcm.components.reservation.jpa;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface ReservationRepository extends DomainIdRepository<ReservationEntity, Long> {

    List<ReservationEntity> findAllByReservationObjectIdAndDomainId(Long reservationObjectId, Integer domainId);

    //Get reservation where date interval is overlapping
    //Formula ((Start1 <= End2) && (End1 => Start2)) , if true, then two dates are overlapping (so input is reverse)


    List<ReservationEntity> findAllByReservationObjectIdAndDomainIdAndDateFromLessThanEqualAndDateToGreaterThanEqualAndAcceptedTrue(Long reservationObjectId, Integer domainId, Date dateFrom, Date dateTo);
    
    //List<ReservationEntity> findAllByDomainIdAndDateFromLessThanEqualAndDateToGreaterThanEqualAndAcceptedTrue(Integer domainId, Date dateFrom, Date dateTo);

    @Query("SELECT re FROM ReservationEntity re JOIN ReservationObjectEntity roe ON re.reservationObjectId = roe.id WHERE re.dateFrom <= :dateFrom AND re.dateTo >= :dateTo AND roe.reservationForAllDay = :reservationForAllDay AND re.accepted = true AND re.domainId = :domainId")
    List<ReservationEntity> findByDateAndType(@Param("dateFrom") Date dateFrom, @Param("dateTo") Date dateTo, @Param("reservationForAllDay") Boolean reservationForAllDay, @Param("domainId") Integer domainId);
}