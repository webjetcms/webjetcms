package sk.iway.iwcm.calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import sk.iway.iwcm.database.ActiveRecord;

@Entity
@Table(name="calendar_types")
public class CalendarTypeBean extends ActiveRecord {
    @Column(name="type_id")
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY, generator="S_calendar_types")
    private int id;
    @Column
    private String name;
    @Column(name="schvalovatel_id")
    private int approverId;
    @Column(name="domain_id")
    private int domainId;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getApproverId() {
        return approverId;
    }

    public void setApproverId(int approverId) {
        this.approverId = approverId;
    }

    public int getDomainId() {
        return domainId;
    }

    public void setDomainId(int domainId) {
        this.domainId = domainId;
    }
}
