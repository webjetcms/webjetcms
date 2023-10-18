package sk.iway.iwcm.components.monitoring.jpa;

import java.util.Date;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitoringRepository extends JpaRepository<MonitoringEntity, Long>, JpaSpecificationExecutor<MonitoringEntity>{

    Page<MonitoringEntity> findAllByDayDateGreaterThanEqualAndDayDateLessThanEqual(Pageable pageable, Date from, Date to );
}
