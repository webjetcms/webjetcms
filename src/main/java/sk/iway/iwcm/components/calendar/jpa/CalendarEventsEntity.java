package sk.iway.iwcm.components.calendar.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name = "calendar")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_CALENDAR_CREATE)
public class CalendarEventsEntity extends CalendarEventsBasic {

    public CalendarEventsEntity(){
        //konstruktor
    }

    @Column(name = "lng")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="calendar_edit.language",
        hidden = true,
        tab = "basic",
        sortAfter = "title",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "temp.slovak", value = "sk"),
                    @DataTableColumnEditorAttr(key = "temp.english", value = "en"),
                    @DataTableColumnEditorAttr(key = "temp.czech", value = "cz"),
                    @DataTableColumnEditorAttr(key = "temp.deutsch", value = "de")
                }
            )
        }
    )
    @Size(max = 3)
    private String lng;

    @Column(name = "time_range")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="calendar_edit.time_range",
        tab = "basic",
        hidden = true,
        sortAfter = "dateTo"
    )
    private String timeRange;

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
}
