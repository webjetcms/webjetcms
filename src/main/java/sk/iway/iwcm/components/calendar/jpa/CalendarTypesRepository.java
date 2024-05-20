package sk.iway.iwcm.components.calendar.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface CalendarTypesRepository extends DomainIdRepository<CalendarTypesEntity, Long> {

    @Query("SELECT cte.id FROM CalendarTypesEntity cte WHERE cte.approverId = :approverId AND cte.domainId = :domainId")
    List<Integer> getTypeIdsByApproverAndDomain(@Param("approverId")Integer approverId, @Param("domainId")Integer domainId);
}