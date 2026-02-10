package sk.iway.iwcm.dmail.jpa;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.database.ActiveRecordRepository;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;

@Entity
@Table(name = "emails")
@Getter
@Setter
//not auditing, because for large number of emails it would slow down the process and bloat the adminlog
//@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
//@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_DMAIL)
public class EmailsEntity extends ActiveRecordRepository {

    public EmailsEntity() {}

    public EmailsEntity(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public EmailsEntity(String recipientEmail, String recipientName) {
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
    }

    //Set entity domain id
	@PrePersist
	public void prePersist() {
		if(domainId == null) domainId = CloudToolsForCore.getDomainId();
	}

    @Id
    @Column(name = "email_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_emails")
    @DataTableColumn(inputType = DataTableColumnType.ID)
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
    private String recipientEmail;

    /**
     * During recipients adding, this flag is used to skip wrong emails.
     * Used in campaigns.
     */
    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title="components.datatables.data.skip_wrong",
        hidden = true
    )
    private boolean skipWrongEmails;

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

    @OneToOne
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@JsonBackReference(value="userDetailsEntity")
	@JoinColumn(name="recipient_user_id", insertable = false, updatable = false)
	private UserDetailsEntity userDetailsEntity;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.MULTISELECT,
        title = "user.admin.userGroups",
        hiddenEditor = true
    )
    private Integer[] groupIds;

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

    @Column(name = "domain_id")
    private Integer domainId;

    public void setId(Long id) {
		this. id = id;
	}

    public Integer[] getGroupIds() {
        if(userDetailsEntity == null) return new Integer[0];

        String ugi = userDetailsEntity.getUserGroupsIds();
        if(Tools.isNotEmpty(ugi)) {
            return Tools.getTokensInteger(ugi, ",");
        }

        return new Integer[0];
    }
}
