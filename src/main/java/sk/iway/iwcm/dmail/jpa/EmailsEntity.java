package sk.iway.iwcm.dmail.jpa;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;

@Entity
@Table(name = "emails")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_SENDMAIL)
public class EmailsEntity extends ActiveRecordRepository implements Serializable {

    public EmailsEntity() {}

    public EmailsEntity(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public EmailsEntity(String recipientEmail, String recipientName) {
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
    }

    @Id
    @Column(name = "email_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_emails")
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID", renderFormat = "dt-format-selector")
    private Long id;

    @Column(name = "recipient_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="user.Name"
    )
    @Size(max = 128)
    private String recipientName;

    @Column(name = "recipient_email")
    @NotEmpty
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.dmail.campaings.email.title",
        editor = {
            @DataTableColumnEditor( type = "textarea" )
        }
    )
    @Size(max = 128)
    private String recipientEmail;

    @Column(name = "sent_date")
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.dmail.camp.sent_date",
        hiddenEditor = true
    )
    private Date sentDate;

    @Column(name = "seen_date")
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.dmail.camp.open_date",
        hiddenEditor = true
    )
    private Date seenDate;

    //Just info column about status
    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.dmail.campaings.status",
        hiddenEditor = true
    )
    private String status;

    //Not shown but used columns
    @Column(name = "campain_id")
    private Long campainId;

    @Column(name = "recipient_user_id")
    private Integer recipientUserId;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_email")
    private String senderEmail;

    @Column(name = "created_by_user_id")
    private Integer createdByUserId;

    @Column(name = "create_date")
    private Date createDate;

    @Column(name = "subject")
    private String subject;

    @Column(name = "url")
    private String url;

    @Column
    private Boolean disabled;

    @Column
    private Integer retry;

    @Column
    private String attachments;

    @Column(name = "reply_to")
    private String replyTo;

    @Column(name = "cc_email")
    private String ccEmail;

    @Column(name = "bcc_email")
    private String bccEmail;

    @Column(name = "send_at")
    private Date sendAt;

    public void setId(Long id) {
		this. id = id;
	}
}
