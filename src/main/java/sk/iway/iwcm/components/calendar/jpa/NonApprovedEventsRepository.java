package sk.iway.iwcm.components.calendar.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface NonApprovedEventsRepository extends DomainIdRepository<NonApprovedEventEntity, Long> {

    @Transactional
    @Modifying
    @Query("UPDATE CalendarEventsEntity cee SET cee.approve = :approve WHERE cee.id = :id AND cee.domainId = :domainId")
    int updateApprove(@Param("id")Long id, @Param("approve")Integer approve, @Param("domainId")Integer domainId);
}
