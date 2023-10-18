package sk.iway.iwcm.components.calendar.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarEventsRepository extends JpaRepository<CalendarEventsEntity, Long> {

    Page<CalendarEventsEntity> findAllByDomainId(Integer domainId, Pageable pageable);

    CalendarEventsEntity findByIdAndDomainId(Long id, Integer domainId);
}
