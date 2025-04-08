package sk.iway.iwcm.dmail.jpa;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.annotations.validations.MultipleEmails;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "emails_campain")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_DMAIL)
public class CampaingsEntity implements Serializable {

    //Set entity domain id
	@PrePersist
	public void prePersist() {
		if(domainId == null) domainId = CloudToolsForCore.getDomainId();
	}

    @Id
    @Column(name = "emails_campain_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_emails_campain")
    @DataTableColumn(inputType = DataTableColumnType.ID, tab = "main")
    private Long id;

    @Column(name = "subject")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.dmail.camp.subject",
        tab = "main",
        sortAfter = "editorFields.pageToSend"
    )
    @Size(max = 255)
    private String subject;

    @Column(name = "sender_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="user.Name",
        tab = "main",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "email.sender"),
                }
            )
        }
    )
    @Size(max = 255)
    private String senderName;

    @Column(name = "sender_email")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="user.email",
        tab = "main"
    )
    @Size(max = 255)
    @Email
    private String senderEmail;

    @Column(name = "url")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private String url;

    @Column(name = "create_date")
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.dmail.camp.date",
        hiddenEditor = true
    )
    private Date createDate;

    @Column(name = "last_sent_date")
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.dmail.camp.sent_date",
        hiddenEditor = true
    )
    private Date lastSentDate;

    //Handle thru editorFields
    @Column(name = "user_groups")
    @Size(max = 64)
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private String userGroupsIds;

    //Bind editor fields
    @Transient
    @DataTableColumnNested
	private CampaingsEditorFields editorFields = null;

    //From these two colulmns is concated editorFields.sentMails
    @Column(name = "count_of_recipients")
    private Integer countOfRecipients;

    @Column(name = "count_of_sent_mails")
    private Integer countOfSentMails;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "receivers",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url",
                                            value = "/admin/rest/dmail/emails?campainId={id}&selectType=recipients"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.dmail.jpa.EmailsEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "id,recipientName,recipientEmail,sentDate,status,groupIds"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "3,desc")
            }
        )
    })
    private List<EmailsEntity> recipientsTab;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "opens",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/dmail/emails?campainId={id}&selectType=opens"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.dmail.jpa.EmailsEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,remove,import,celledit,duplicate"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "id,recipientName,recipientEmail,seenDate"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "0,desc")
            }
        )
    })
    private List<EmailsEntity> opensTab;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "clicks",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/dmail/stat-clicks?campainId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.dmail.jpa.StatClicksEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,remove,import,celledit,duplicate"),
            }
        )
    })
    private List<StatClicksEntity> statClicksTab;

    @Column(name = "created_by_user_id")
    private Integer createdByUserId;

    @Column
    @DataTableColumn(
        inputType = DataTableColumnType.HIDDEN
    )
    private String attachments;

    @MultipleEmails(checkUnsubscribed = false, canByEmpty = true)
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.dmail.replyTo",
        tab = "advanced",
        visible = false
    )
    @Size(max = 255)
    @Column(name = "reply_to")
    private String replyTo;

    @MultipleEmails(checkUnsubscribed = false, canByEmpty = true)
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.dmail.ccEmail",
        tab = "advanced",
        visible = false
    )
    @Size(max = 255)
    @Column(name = "cc_email")
    private String ccEmail;

    @MultipleEmails(checkUnsubscribed = false, canByEmpty = true)
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.dmail.bccEmail",
        tab = "advanced",
        visible = false
    )
    @Size(max = 255)
    @Column(name = "bcc_email")
    private String bccEmail;

    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.dmail.campaigns.sendAt",
        tab = "advanced",
        visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
                }
            )
        }
    )

    @Column(name = "send_at")
    private Date sendAt;

    @Column(name = "domain_id")
    private Integer domainId;
}
