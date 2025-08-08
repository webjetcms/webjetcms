package sk.iway.iwcm.components.reservation.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

import java.util.List;

@Repository
public interface ReservationObjectRepository extends DomainIdRepository<ReservationObjectEntity, Long> {
    @Query("SELECT roe FROM ReservationObjectEntity roe WHERE roe.id IN :ids")
    List<ReservationObjectEntity> findAllByIdIn(@Param("ids") Integer[] ids);

    @Query("SELECT roe.reservationForAllDay FROM ReservationObjectEntity roe WHERE roe.id = :id")
    Boolean isReservationForAllDay(@Param("id")Long id);

    List<ReservationObjectEntity> findAllByDomainIdAndReservationForAllDayFalse(Integer domainId);

    List<ReservationObjectEntity> findAllByDomainIdAndReservationForAllDayTrue(Integer domainId);

    @Query("SELECT roe FROM ReservationObjectEntity roe WHERE roe.reservationForAllDay = true AND roe.id IN :ids")
    List<ReservationObjectEntity> getAllDayReservationsWhereIdsIn(@Param("ids")List<Integer> ids);

    @Query("SELECT roe.maxReservations FROM ReservationObjectEntity roe WHERE roe.id = :id")
    Integer getMaxReservationsById(@Param("id")Long id);
}
