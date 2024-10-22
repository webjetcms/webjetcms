package sk.iway.iwcm.components.reservation.jpa;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface ReservationObjectPriceRepository  extends DomainIdRepository<ReservationObjectPriceEntity, Long> {
    List<ReservationObjectPriceEntity> findAllByObjectIdAndDomainId(Long objectId, Integer domainId);
    List<ReservationObjectPriceEntity> findAllByObjectIdAndDomainIdAndDateFromLessThanEqualAndDateToGreaterThanEqual(Long objectId, Integer domainId, Date dateFrom, Date dateTo);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM ReservationObjectTimesEntity rote WHERE rote.objectId = :objectId AND rote.domainId = :domainId")
    void deleteAllByObjectIdAndDomainId(@Param("objectId")Long objectId, @Param("domainId")Integer domainId);
}