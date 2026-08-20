package sk.iway.iwcm.system.audit.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


@Repository
public interface AuditRepository extends JpaRepository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {
    List<AuditLogEntity> findAllByLogTypeAndSubId1AndCreateDateBetween(Integer logType, Integer subId1, java.sql.Timestamp startDate, java.sql.Timestamp endDate);
}
