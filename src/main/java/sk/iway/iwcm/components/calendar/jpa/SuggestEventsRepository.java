package sk.iway.iwcm.components.calendar.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface SuggestEventsRepository extends DomainIdRepository<SuggestEventEntity, Long> {
    
    @Transactional
    @Modifying
    @Query("UPDATE CalendarEventsEntity cee SET cee.suggest = :suggest WHERE cee.id = :id AND cee.domainId = :domainId")
    int updateSuggest(@Param("id")Long id, @Param("suggest")boolean suggest, @Param("domainId")Integer domainId);
}
