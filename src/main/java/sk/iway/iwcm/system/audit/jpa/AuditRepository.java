package sk.iway.iwcm.system.audit.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface AuditRepository extends JpaRepository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {

    @Query("SELECT COUNT(ale.id) FROM AuditLogEntity ale WHERE ale.logType = :logType AND ale.subId1 = :subId1 AND ale.subId2 = :subId2")
    Integer getCountOfLogs(@Param("logType") Integer logType, @Param("subId1") Integer subId1, @Param("subId2") Integer subId2);

    List<AuditLogEntity> findAllByLogTypeAndSubId1AndCreateDateBetween(Integer logType, Integer subId1, java.sql.Timestamp startDate, java.sql.Timestamp endDate);
}