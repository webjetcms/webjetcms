package sk.iway.iwcm.dmail.jpa;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "emails_stat_click")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_SENDMAIL)
@SecondaryTable(name = "emails", pkJoinColumns=@PrimaryKeyJoinColumn(name="email_id", referencedColumnName="email_id"))
public class StatClicksEntity extends ActiveRecordRepository implements Serializable {

    @Id
    @Column(name = "click_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_emails_stat_click")
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID", renderFormat = "dt-format-selector")
    private Long id;

    @Column(table="emails", name="recipient_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="user.Name"
    )
    private String recipientName;

    @Column(table="emails", name="recipient_email")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="user.email"
    )
    private String recipientEmail;

    @Column(name = "link")
    @NotEmpty
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.dmail.camp.link"
    )
    @Size(max = 255)
    private String link;

    @Column(table="emails", name="campain_id")
    @DataTableColumn(
        inputType = DataTableColumnType.HIDDEN
    )
    private Long campainId;

    @Column(name = "click_date")
    @NotEmpty
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.dmail.camp.click_date"
    )
    private Date clickDate;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "browser_id")
    private Long browserId;

    @Column(name = "email_id")
    private Long emailId;

    public void setId(Long id) {
		this. id = id;
	}
}
