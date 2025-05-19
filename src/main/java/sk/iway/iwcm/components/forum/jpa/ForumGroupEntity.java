package sk.iway.iwcm.components.forum.jpa;

import java.io.Serializable;
import java.util.Date;
import java.util.StringTokenizer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.users.UserDetails;

/**
 * ForumGroupEntity aka ForumEntity. We let the old historic name ForumGroupEntity, so after remade to Spring
 * it is clear what this entity is for (or what is replacing).
 */

@Entity
@Table(name = "forum")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_FORUM_SAVE)
public class ForumGroupEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_forum")
    @DataTableColumn(inputType = DataTableColumnType.ID, tab="advanced", hidden = true)
    private Long id;

    //Is active or not
    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.forum.status",
        tab="advanced",
        hidden = true,
        editor = @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{components.forum.admin.basicInfo}]]"),
                @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
            }
        )
    )
    private String forumStatus;

    //Is message board or no
    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.forum.admin.forumType",
        tab="advanced",
        hidden = true,
        editor = @DataTableColumnEditor(
            attr = @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
        )
    )
    private String forumType;

    @Column(name = "advertisement_type")
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title="components.forum.admin.advertisementType",
        tab="advanced",
        hidden = true
    )
    private Boolean advertisementType;

    @Column(name = "message_confirmation")
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title="components.forum.confirmation",
        tab="advanced",
        hidden = true
    )
    private Boolean messageConfirmation;

    @Column(name = "approve_email")
    @Size(max = 255)
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.forum.approve_email",
        tab="advanced",
        hidden = true
    )
    private String approveEmail;

    @Column(name = "notif_email")
    @Size(max = 255)
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.forum.send_notif",
        tab="advanced",
        hidden = true
    )
    private String notifEmail;

    //Permissions
    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "components.forum.user_groups_for_addmessage",
        tab = "advanced",
        hidden = true,
        editor = {
            @DataTableColumnEditor(attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "user.permissions"),
                @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
            })
        }
    )
    private Integer[] addMessagePerms;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "components.forum.adminGroups",
        tab = "advanced",
        hidden = true,
        editor = {
            @DataTableColumnEditor(attr = @DataTableColumnEditorAttr(key = "unselectedValue", value = "") )
        }
    )
    private Integer[] adminPerms;

    @Column(name = "active")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="components.forum.active",
        tab="advanced",
        hidden = true,
        editor = @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{components.forum.admin.availability}]]")
            }
        )
    )
    private Boolean active;

    @Column(name = "date_from")
    @Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.forum.date_from",
        tab="advanced",
        hidden = true
    )
    private Date dateFrom;

    @Column(name = "date_to")
    @Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.forum.date_to",
        tab="advanced",
        hidden = true
    )
    private Date dateTo;

    @Column(name = "hours_after_last_message")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.forum.hours_after",
        tab="advanced",
        hidden = true
    )
    @Min(value = 0)
    private Integer hoursAfterLastMessage;

    //Dont show them, just need their values during saving
    @Column(name = "doc_id")
    @DataTableColumn(
        inputType = DataTableColumnType.HIDDEN
    )
    private Integer docId;

    @Column(name = "message_board")
    @DataTableColumn(
        inputType = DataTableColumnType.HIDDEN
    )
    private Boolean messageBoard;

    @Column(name = "admin_groups")
    @Size(max = 32)
    private String adminGroups;

    @Column(name = "addmessage_groups")
    @Size(max = 128)
    private String addmessageGroups;

    @Column(name = "notify_page_author")
    private Boolean notifyPageAuthor;

    @Column(name = "domain_id")
    private Integer domainId;

    /**
	 * Otestuje ci moze pouzivatel pridat prispevok do fora
	 * @param user
	 * @return
	 */
	public boolean canPostMessage(UserDetails user) {
		if (Tools.isEmpty(getAddmessageGroups())) return true;
		if (user == null) return(false);

		String ids = getAddmessageGroups();
		if(Tools.isNotEmpty(ids))
		{
			StringTokenizer st = new StringTokenizer(ids,",+");
			while(st.hasMoreTokens())
			{
				int userGroupId = Tools.getIntValue(st.nextToken(), -1);
				if (userGroupId < 1) continue;
				if(user.isInUserGroup(userGroupId)) return true;
			}
		}
		return false;
	}

    /**
	 * Vrati true, ak zadany pouzivatel patri medzi administratorov fora
	 * @param user
	 * @return
	 */
	public boolean isAdmin(UserDetails user) {

		if (user == null) return(false);
        if (user.isAdmin()) return true;

        String ids = getAdminGroups();
		if(Tools.isNotEmpty(ids))  {
			StringTokenizer st = new StringTokenizer(ids,",+");
			while(st.hasMoreTokens()) {
				int userGroupId = Tools.getIntValue(st.nextToken(), -1);
				if (userGroupId < 1) continue;
				if(user.isInUserGroup(userGroupId)) return true;
			}
		}
		return false;
	}

    public boolean isActive() {
		if(getActive() == null) return false;
        return getActive();
	}

    public boolean isMessageConfirmation() {
        if(getMessageConfirmation() == null) return false;
		return getMessageConfirmation();
	}

    public boolean isMessageBoard() {
        if(getMessageBoard() == null) return false;
		return getMessageBoard();
	}

    public boolean isAdvertisementType() {
        if(getAdvertisementType() == null) return false;
		return getAdvertisementType();
	}
}
