package sk.iway.iwcm.components.reservation.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

import java.util.List;

@Repository
public interface ReservationObjectRepository extends DomainIdRepository<ReservationObjectEntity, Long> {
    List<ReservationObjectEntity> findAllByIdIn(Integer[] ids);

    @Query("SELECT roe.reservationForAllDay FROM ReservationObjectEntity roe WHERE roe.id = :id")
    boolean isReservationForAllDay(@Param("id")Long id);

    List<ReservationObjectEntity> findAllByDomainIdAndReservationForAllDayFalse(Integer domainId);
}
