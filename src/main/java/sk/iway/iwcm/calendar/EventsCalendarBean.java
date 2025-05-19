package sk.iway.iwcm.calendar;
import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModelProperty;
import sk.iway.iwcm.database.ActiveRecord;

@Entity
@Table(name="calendar")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventsCalendarBean extends ActiveRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name="calendar_id")
    @GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_calendar")
    private int id;

    @NotEmpty(message = "validation.not_null.error")
    @Column
    private String title;

    @Column
    private String description;

    @NotNull(message = "validation.not_null.error")
    @ApiModelProperty(dataType = "long", value="1521154800000")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="date_from")
    private Date from;

    @NotNull(message = "validation.not_null.error")
    @ApiModelProperty(dataType = "long", value="1521154800000")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="date_to")
    private Date to;

    @NotNull(message = "validation.not_null.error")
    @ManyToOne
    @JoinColumn(name="type_id")
    private CalendarTypeBean type;

    @Column(name="time_range")
    private String timeRange;

    @Column
    private String area;

    @Column
    private String city;

    @Column
    private String address;

    @Column(name="info_1")
    private String info1;

    @Column(name="info_2")
    private String info2;

    @Column(name="info_3")
    private String info3;

    @Column(name="info_4")
    private String info4;

    @Column(name="info_5")
    private String info5;

    @Column(name="notify_hours_before")
    private int notifyHoursBefore;

    @Column(name="notify_emails")
    private String notifyEmails;

    @Column(name="notify_sender")
    private String notifySender;

    @Column(name="notify_introtext")
    private String notifyIntrotext;

    @Column(name="notify_sendsms")
    private boolean notifySendSMS;

    @Column
    private String lng;

    @Column(name="creator_id")
    private int creatorId;

    @Column
    private boolean approve;

    @Column
    private boolean suggest;

    @Column(name="hash_string")
    private String hashString;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public CalendarTypeBean getType() {
        return type;
    }

    public void setType(CalendarTypeBean type) {
        this.type = type;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInfo1() {
        return info1;
    }

    public void setInfo1(String info1) {
        this.info1 = info1;
    }

    public String getInfo2() {
        return info2;
    }

    public void setInfo2(String info2) {
        this.info2 = info2;
    }

    public String getInfo3() {
        return info3;
    }

    public void setInfo3(String info3) {
        this.info3 = info3;
    }

    public String getInfo4() {
        return info4;
    }

    public void setInfo4(String info4) {
        this.info4 = info4;
    }

    public String getInfo5() {
        return info5;
    }

    public void setInfo5(String info5) {
        this.info5 = info5;
    }

    public int getNotifyHoursBefore() {
        return notifyHoursBefore;
    }

    public void setNotifyHoursBefore(int notifyHoursBefore) {
        this.notifyHoursBefore = notifyHoursBefore;
    }

    public String getNotifyEmails() {
        return notifyEmails;
    }

    public void setNotifyEmails(String notifyEmails) {
        this.notifyEmails = notifyEmails;
    }

    public String getNotifySender() {
        return notifySender;
    }

    public void setNotifySender(String notifySender) {
        this.notifySender = notifySender;
    }

    public String getNotifyIntrotext() {
        return notifyIntrotext;
    }

    public void setNotifyIntrotext(String notifyIntrotext) {
        this.notifyIntrotext = notifyIntrotext;
    }

    public boolean isNotifySendSMS() {
        return notifySendSMS;
    }

    public void setNotifySendSMS(boolean notifySendSMS) {
        this.notifySendSMS = notifySendSMS;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public boolean getApprove() {
        return approve;
    }

    public void setApprove(boolean approve) {
        this.approve = approve;
    }

    public boolean isSuggest() {
        return suggest;
    }

    public void setSuggest(boolean suggest) {
        this.suggest = suggest;
    }

    public String getHashString() {
        return hashString;
    }

    public void setHashString(String hashString) {
        this.hashString = hashString;
    }

    public int getDomainId() {
        return domainId;
    }

    public void setDomainId(int domainId) {
        this.domainId = domainId;
    }

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public Date getStart() {
        return from;
    }

    public void setStart(Date from) {
        this.from = from;
    }

    public Date getEnd() {
        return to;
    }

    public void setEnd(Date to) {
        this.to = to;
    }

}