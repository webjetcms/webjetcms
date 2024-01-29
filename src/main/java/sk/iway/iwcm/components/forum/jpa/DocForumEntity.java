package sk.iway.iwcm.components.forum.jpa;

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

import org.apache.struts.upload.FormFile;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.doc.DocDetailsConverter;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;
import sk.iway.iwcm.users.UserBasicDto;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.users.jpa.UserBasicDtoConverter;

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
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID", tab="main")
    private Long id;

    @Column(name = "doc_id")
    private Integer docId;

    @Transient
    @DataTableColumn(
        inputType = { DataTableColumnType.JSON },
        className = "dt-tree-page",
        title = "components.blog.forum.article",
        tab="main",
        editor = @DataTableColumnEditor(
            attr = @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
        ),
        filter = true
    )
    private DocDetailsDto docDetails;

    @Column(name = "subject")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="forum.subject",
        tab="main"
    )
    private String subject;

    @Column(name = "send_answer_notif")
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "components.forum.send_answer_notif",
        tab="main",
        hidden = true
    )
    private Boolean sendAnswerNotif;

    @Column(name = "active")
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "components.forum.active",
        tab="main",
        hidden = true
    )
    private Boolean active;

    @Column(name = "question")
    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        className = "dt-row-edit",
        title="components.forum.admin.text",
        tab="main"
    )
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String question;

    @Column(name = "question_date")
    @Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.forum.admin.datum",
        hiddenEditor = true
    )
    private Date questionDate;

    @Column(name = "author_name")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.forum.admin.autor",
        tab="main"
    )
    private String authorName;

    @Column(name = "author_email")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.forum.admin.email",
        tab="main"
    )
    private String authorEmail;

    @Transient
	@DataTableColumnNested
	private DocForumEditorFields editorFields = null;

    /* Adding ForumGroupEntity (DB tbale forum), it's used as forum setting's */
    @Transient
    @DataTableColumnNested
    private ForumGroupEntity forumGroupEntity;

    @Column(name = "confirmed")
    @DataTableColumn( inputType = DataTableColumnType.HIDDEN )
    private Boolean confirmed;

    @Column(name = "deleted")
    @DataTableColumn( inputType = DataTableColumnType.HIDDEN )
    private Boolean deleted;

    @Column(name = "parent_id")
    private Integer parentId;

    @Column(name = "ip")
    private String ip;

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

    @Column(name = "domain_id")
    private Integer domainId;

    @Transient
    private FormFile uploadedFile = null;

    @Transient
    private String prefix;

    @Transient
    private Integer level;

    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) //toto nepotrebujeme deserializovat pri post requeste
    private UserBasicDto userDetails;

    //***************SPECIAL LOGIC METHODS***************************
    public void setDocDetails(DocDetailsDto docDetails) {
        this.docDetails = docDetails;
        if (docDetails != null) this.docId = docDetails.getDocId();
    }

    private boolean isAdmin(UserDetails user, ForumGroupEntity fge) {
        //If not set, try get from this entity
        if(fge == null) fge = getForumGroupEntity();

        if(user != null && fge != null && fge.isAdmin(user)) return true;

        if(user != null && user.isAdmin()) return true;

        return false;
    }

    private boolean baseCheck(UserDetails user, ForumGroupEntity fge, boolean mustBeLogged) {
        //If not set, try get from this entity
        if(fge == null) fge = getForumGroupEntity();

        //Must be logged -> when perform delete and upload
        if(mustBeLogged) {
            //Check if he is logged
            if(user == null) return false;

            //When logged, he must be allso AUTHOR of the Forum
            if(getUserId() != null && getUserId() != user.getUserId()) return false;
        }

        //Check that whole DOC is active
        if(fge != null && !fge.getActive()) return false;

        //Check if is allready deleted
        if(getDeleted()) return false;

        //Must be active
        if(!getActive()) return false;

        //Must be approved
        if(!getConfirmed()) return false;

        return true;
    }

    public boolean canDelete(UserDetails user, int delMinutes) {
        return canDelete(user, delMinutes, null);
    }

    public boolean canDelete(UserDetails user, int delMinutes, ForumGroupEntity fge) {
        //ADMIN can do anything
        if(isAdmin(user, fge)) return true;

        //If he is not admin, do all the test's
        if(!baseCheck(user, fge, true)) return false;

        //Question date must be set to compute limit range
		if (getQuestionDate() == null) return false;

        //Delete can be performed only in specific range from create
		long delTimeLimitMsec = getQuestionDate().getTime() + (delMinutes * 60L * 1000);
		if(delTimeLimitMsec > Tools.getNow()) return true;

        //It's all good
		return false;
	}

    public boolean canUpload(UserDetails user, LabelValueDetails uploadLimits) {
        return canUpload(user, uploadLimits, null);
    }

    public boolean canUpload(UserDetails user, LabelValueDetails uploadLimits, ForumGroupEntity fge) {
        //ADMIN can do anything
        if(isAdmin(user, fge)) return true;

        //If he is not admin, do all the test's
        if(!baseCheck(user, fge, true)) return false;

        //Upload limits must be set
        if(uploadLimits == null) return false;

        //It's all good
		return true;
    }


    public boolean canPost() {
        return canPost(null, null);
    }

    public boolean canPost(ForumGroupEntity fge) {
        return canPost(fge, null);
    }

    public boolean canPost(ForumGroupEntity fge, UserDetails user) {
        //ADMIN can do anything
        if(isAdmin(user, fge)) return true;

        //If he is not admin, do all the test's
        if(!baseCheck(user, fge, false)) return false;

        return true;
    }

    public String getSubject() {
        if (subject == null || subject.trim().length() < 1)
            return ("Bez nadpisu");

        return subject;
    }

    public void setQuestion(String question) {
   	    //odfiltruj nepovolene veci
   	    question = Tools.replaceIgnoreCase(question, "script", "<span style='display: none;'>s</span>script");

        this.question = question;
    }

    public String getQuestionHtml() {
        if (question == null)
            return ("");

        String ret = Tools.replace(question, "<", "&lt;");
        ret = Tools.replace(ret, "\n", "<br>");
        return (ret);
    }

    public String getAuthorPhoto(String defaultPhoto) {
        UserDetails tmp = UsersDB.getUser(getUserId());

        if(tmp == null) tmp = new UserDetails();

        String authorPhoto = tmp.getPhoto();

        if(Tools.isEmpty(authorPhoto)) return defaultPhoto;
        return GalleryDB.getImagePathOriginal(authorPhoto);
    }

    //******************METHODS for BACKWARD COMPATIBILITY*********************
    public boolean isConfirmed() {
        if(getConfirmed() == null) return false;
		return getConfirmed();
	}

    public boolean isSendNotif() {
		if(getSendAnswerNotif() == null) return false;
        return getSendAnswerNotif();
	}

    public boolean isActive() {
        if(getActive() == null) return false;
		return getActive();
	}

    public boolean isDeleted() {
        if(getDeleted() == null) return false;
		return getDeleted();
	}

    public int getForumId() {
        if (getId() == null) return 0;
        return getId().intValue();
    }

    public String getQuestionDateDisplayDate() {
        if(getQuestionDate() == null) return "";

        return Tools.formatDate(getQuestionDate());
    }

    public String getQuestionDateDisplayTime() {
        if(getQuestionDate() == null) return "";

        return Tools.formatTime(getQuestionDate());
    }

    public String getLastPost() {
        return Tools.formatDateTime(getStatLastPost(), "");
	}

    public UserBasicDto getUserDetails() {
        if (userId == null || userId < 1) return null;
        if (userDetails!=null) return userDetails;
        userDetails = new UserBasicDtoConverter().convertToEntityAttribute(userId);
        return userDetails;
    }

    public DocDetailsDto getDocDetails() {
        if (docId == null || docId < 1) return null;
        if (docDetails!=null) return docDetails;
        docDetails = new DocDetailsConverter().convertToEntityAttribute(docId);
        return docDetails;
    }
}