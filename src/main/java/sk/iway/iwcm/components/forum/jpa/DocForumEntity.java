package sk.iway.iwcm.components.forum.jpa;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
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

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.doc.DocDetailsConverter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "document_forum")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_FORUM_SAVE)
public class DocForumEntity implements Serializable {

    @Id
    @Column(name = "forum_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_document_forum")
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID")
    private Long id;

    @Column(name = "doc_id")
    private Integer docId;

    @Column(name = "doc_id", updatable = false, insertable = false)
    @Convert(converter = DocDetailsConverter.class)
    @DataTableColumn(
        inputType = { DataTableColumnType.JSON },
        className = "dt-tree-page",
        title = "components.blog.forum.article",
        editor = @DataTableColumnEditor(
            attr = @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
        )
    )
    private DocDetailsDto docDetails;

    @Column(name = "subject")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="forum.subject"
    )
    private String subject;

    @Column(name = "send_answer_notif")
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "components.forum.send_answer_notif",
        hidden = true
    )
    private Boolean sendAnswerNotif;

    @Column(name = "question")
    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        className = "dt-row-edit",
        title="components.forum.admin.text"
    )
    private String question;

    @Column(name = "question_date")
    @Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="components.forum.admin.datum",
        hiddenEditor = true
    )
    private Date questionDate;

    @Column(name = "author_name")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.forum.admin.autor"
    )
    private String authorName;

    @Column(name = "author_email")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.forum.admin.email"
    )
    private String authorEmail;

    @Transient
	@DataTableColumnNested
	private DocForumEditorFields editorFields = null;

    @Column(name = "parent_id")
    private Integer parentId;

    @Column(name = "ip")
    private String ip;

    @Column(name = "confirmed")
    private Boolean confirmed;

    @Column(name = "hash_code")
    private String hashCode;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "flag")
    private String flag;

    @Column(name = "stat_views")
    private Integer statViews;

    @Column(name = "stat_replies")
    private Integer statReplies;

    @Column(name = "stat_last_post")
    private Date statLastPost;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "domain_id")
    private Integer domainId;

    public void setDocDetails(DocDetailsDto docDetails) {
        this.docDetails = docDetails;
        if (docDetails != null) this.docId = docDetails.getDocId();
    }
}
