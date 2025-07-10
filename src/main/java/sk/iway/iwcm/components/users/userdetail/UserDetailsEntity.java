package sk.iway.iwcm.components.users.userdetail;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

/**
 * Udaje pouzivatela
 * POZOR: z databazy necita/nezapisuje password a salt, to sa zapisuje separatnym SQL prikazom
 * je to tak kvoli bezpecnosti, aby sa nahodou nedostali hash hesiel/salt na FE
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_USER_EDIT)
public class UserDetailsEntity extends UserDetailsBasic {

    public UserDetailsEntity(){
        //
    }

    @PrePersist
    public void prePersist() {
        if (this.getDomainId()==null) {
            if (UserDetailsService.isUsersSplitByDomain()) {
                //in multiweb we must set current domainId
                this.setDomainId(CloudToolsForCore.getDomainId());
            } else {
                this.setDomainId(1);
            }
        }
    }

    /*PERSONAL INFO - Access*/

    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.users.email}]]",
        tab = "personalInfo",
        sortAfter = "editorFields.login"
    )
    private String email;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "[[#{components.users.authorized}]]",
        tab = "personalInfo",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "useredit.access")
                }
            )
        },
        visible = false,
        sortAfter = "photo"
    )
    private Boolean authorized;

    @Column(name = "allow_login_start")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title = "[[#{user.admin.allowLoginStart}]]",
        tab = "personalInfo",
        visible = false,
        sortAfter = "authorized"
    )
    private Date allowLoginStart;

    @Column(name = "allow_login_end")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title = "[[#{user.admin.allowLoginEnd}]]",
        tab = "personalInfo",
        visible = false,
        sortAfter = "allowLoginStart"
    )
    private Date allowLoginEnd;


    /*FREE ITEMS - Fre usable items*/

    @Column(name = "field_a")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{user.field_a}]]",
        tab = "freeItems",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.users.free_usabel_items")
                }
            )
        },
        visible = false
    )
    private String fieldA;

    @Column(name = "field_b")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{user.field_b}]]",
        tab = "freeItems",
        visible = false
    )
    private String fieldB;

    @Column(name = "field_c")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{user.field_c}]]",
        tab = "freeItems",
        visible = false
    )
    private String fieldC;

    @Column(name = "field_d")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{user.field_d}]]",
        tab = "freeItems",
        visible = false
    )
    private String fieldD;

    @Column(name = "field_e")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{user.field_e}]]",
        tab = "freeItems",
        visible = false
    )
    private String fieldE;

    /*RIGHTS*/

    @Column(name = "is_admin")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "user.admin.admin",
        tab = "rightsTab",
        editor = {
            @DataTableColumnEditor(
                label = "components.users.admin",
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.users.admin_section_entry"),
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
                }
            )
        },
        visible = true,
        sortAfter = "editorFields.emails"
    )
    private Boolean admin;

    @Transient
    private String userGroupsNames;

    //ciarkou oddeleny zoznam
    @Column(name = "user_groups")
    private String userGroupsIds;

    @Column(name = "editable_groups")
    private String editableGroups;

    @Column(name = "editable_pages")
    private String editablePages;

    @Lob
    @Column(name = "writable_folders")
    private String writableFolders;

    @Column(name = "reg_date")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        title = "components.forum.regdate",
        inputType = DataTableColumnType.DATETIME,
        visible = false,
        tab = "personalInfo",
        sortAfter = "editorFields.allowWeakPassword",
        className = "hide-on-create",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
                }
            )
        }
    )
    private Date regDate;

    //vo WJ8 sa settuje v triede UsersDB na riadku 88
    @Transient
    private String lastLogon;

    @Column(name = "last_logon")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        title = "admin.users.lastlogon",
        inputType = DataTableColumnType.DATETIME,
        tab = "personalInfo",
        sortAfter = "regDate",
        className = "hide-on-create",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
                }
            )
        }
    )
    private Date lastLogonAsDate;

    // private String modulePerms;

    // private String disabledItems;

    @Column(name = "forum_rank")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, hidden = true)
    private Integer forumRank;

    @Column(name = "rating_rank")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, hidden = true)
    private Integer ratingRank;

//    private String authorizeHash;

    @Column(name = "parent_id")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        hidden = true,
        hiddenEditor = true,
        visible = false
    )
    private Integer parentId;

    @Transient //toto nechceme citat z DB a ani prenasat na FE
    @JsonIgnore
    private String passwordSalt;

    @Column(name = "domain_id")
    private Integer domainId;

    //@Column(name = "mobile_device")
    //private String mobileDevice;

    //@Column(name = "api_key")
    @DataTableColumn(
        inputType = DataTableColumnType.PASSWORD,
        title = "[[#{components.user.apiKey}]]",
        tab = "personalInfo",
        hidden = true
    )
    @Transient //toto nechceme citat z DB
    private String apiKey;

    @Transient
    @DataTableColumnNested
    @Valid
	private UserDetailsEditorFields editorFields = null;

}
