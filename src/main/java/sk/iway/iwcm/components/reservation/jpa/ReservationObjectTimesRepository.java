package sk.iway.iwcm.components.reservation.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationObjectTimesRepository extends JpaRepository<ReservationObjectTimesEntity, Long> {
    
    List<ReservationObjectTimesEntity> findAllByObjectIdAndDomainId(Integer objectId, Integer domainId);
}
