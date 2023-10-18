package sk.iway.iwcm.components.calendar.jpa;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.drew.lang.annotations.NotNull;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "calendar")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_CALENDAR_CREATE)
public class CalendarEventsEntity implements Serializable {

    public CalendarEventsEntity(){
        //konstruktor
    }

    @Id
    @Column(name = "calendar_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_calendar")
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID")
    private Long id;

    @Column(name = "title")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="calendar.name",
        tab = "basic"
    )
    @NotBlank
    private String title;

    @Column(name = "lng")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="calendar_edit.language",
        hidden = true,
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "temp.slovak", value = "sk"),
                    @DataTableColumnEditorAttr(key = "temp.english=", value = "en"),
                    @DataTableColumnEditorAttr(key = "temp.czech", value = "cz"),
                    @DataTableColumnEditorAttr(key = "temp.deutsch", value = "de")
                }
            )
        }
    )
    @Size(max = 3)
    private String lng;

    @Column(name = "date_from")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="calendar.begin",
        tab = "basic"
    )
	private Date dateFrom;

    @Column(name = "date_to")
	@Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="calendar.end",
        tab = "basic"
    )
    @NotNull
	private Date dateTo;

    @Column(name = "time_range")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="calendar_edit.time_range",
        tab = "basic",
        hidden = true
    )
    private String timeRange;

    @Column(name = "area")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="calendar_edit.area",
        hidden = true,
        tab="advanced"
    )
    @Size(max = 255)
    private String area;

    @Column(name = "city")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="calendar_edit.city",
        hidden = true,
        tab="advanced"
    )
    @Size(max = 255)
    private String city;

    @Column(name = "address")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="calendar_edit.address",
        hidden = true,
        tab="advanced"
    )
    @Size(max = 255)
    private String address;

    @Column(name = "info_1")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="calendar_events.field_a",
        hidden = true,
        tab="advanced"
    )
    @Size(max = 255)
    private String fieldA;

    @Column(name = "info_2")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="calendar_events.field_b",
        hidden = true,
        tab="advanced"
    )
    @Size(max = 255)
    private String fieldB;

    @Column(name = "info_3")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "calendar_events.field_c",
        hidden = true,
        tab = "advanced"
    )
    @Size(max = 255)
    private String fieldC;

    @Column(name = "info_4")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "calendar_events.field_d",
        hidden = true,
        tab = "advanced"
    )
    @Size(max = 255)
    private String fieldD;

    @Column(name = "info_5")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "calendar_events.field_e",
        hidden = true,
        tab = "advanced"
    )
    @Size(max = 255)
    private String fieldE;

    @Column(name = "type_id")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "calendar.type",
        tab = "advanced"
    )
    @NotNull
    private Integer typeId;

    @Column(name = "description")
    @DataTableColumn(
        inputType = DataTableColumnType.WYSIWYG,
        title = "calendar_edit.description",
        tab = "description",
        hidden = true
    )
    private String description;

    @Column(name = "notify_hours_before")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "calendar_edit.notify_hours_before",
        tab = "notification",
        hidden = true,
        editor = {
            @DataTableColumnEditor(message = "calendar_edit.notify_hours_before_note")
        }
    )
    private Integer notifyHoursBefore;

    @Column(name = "notify_emails")
    private String notifyEmails;

    @Column(name = "notify_sender")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "notification",
        title = "calendar_edit.notify_sender",
        hidden = true
    )
    @Size(max = 255)
    private String notifySender;

    @Column(name = "notify_introtext")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="calendar_edit.notify_introtext",
        tab = "notification",
        hidden = true
    )
    private String notifyIntrotext;

    @Column(name = "notify_sendsms")
    @DataTableColumn(
        inputType = DataTableColumnType.HIDDEN,
        title="calendar_edit.notify_sendsms",
        tab = "notification",
        hidden = true
    )
    private Boolean notifySendsms;

    @Column(name = "creator_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    @NotNull
    private Integer creatorId;

    @Column(name = "domain_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    @NotNull
    private Integer domainId;

    @Transient
    @DataTableColumnNested
	private CalendarEventsEditorFields editorFields = null;

    @Column(name = "approve")
    private Integer approve;

    // @Column(name = "notify_sent")
    // private Integer notifySent;

    // @Column(name = "suggest")
    // private Integer suggest;

    // @Column(name = "hash_string")
    // private String hashString;
}
