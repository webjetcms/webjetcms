package sk.iway.iwcm.components.reservation.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationObjectRepository extends JpaRepository<ReservationObjectEntity, Long> {

    Page<ReservationObjectEntity> findAllByDomainId(Integer domainId, Pageable pageable);

    List<ReservationObjectEntity> findAllByDomainId(Integer domainId);

    ReservationObjectEntity findByIdAndDomainId(long id, Integer domainId);
}
