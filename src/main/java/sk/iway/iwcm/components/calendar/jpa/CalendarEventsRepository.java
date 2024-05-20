package sk.iway.iwcm.components.calendar.jpa;

import org.springframework.stereotype.Repository;

import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface CalendarEventsRepository extends DomainIdRepository<CalendarEventsEntity, Long> {
}