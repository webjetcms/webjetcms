package sk.iway.iwcm.components.calendar.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "calendar")
public class NonApprovedEventEntity extends CalendarEventsBasic {
    
}