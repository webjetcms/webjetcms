package sk.iway.iwcm.components.reservation.jpa;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationObjectPriceRepository  extends JpaRepository<ReservationObjectPriceEntity, Long> {
    
    List<ReservationObjectPriceEntity> findAllByDomainId(Integer domainId);

    List<ReservationObjectPriceEntity> findAllByObjectIdAndDomainId(Integer objectId, Integer domainId);
}
