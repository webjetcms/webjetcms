package sk.iway.iwcm.components.calendar.jpa;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "calendar")
public class NonApprovedEventEntity extends CalendarEventsBasic {
    
}