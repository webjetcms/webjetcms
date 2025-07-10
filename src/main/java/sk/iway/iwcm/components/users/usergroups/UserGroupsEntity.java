package sk.iway.iwcm.components.users.usergroups;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsNotNullConverter;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/**
 * Entita skupin pouzivatelov
 */
@Entity
@Table(name = "user_groups")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_USER_GROUP_UPDATE)
public class UserGroupsEntity {

    public UserGroupsEntity() {
        //
    }

    @Id
	@Column(name = "user_group_id")
    @GeneratedValue(strategy=GenerationType.IDENTITY, generator = "S_user_groups")
    @DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

    @Size(max = 255)
    @NotBlank
    @Column(name = "user_group_name")
	@DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title = "[[#{groupedit.name}]]",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(type = "text")
        }
    )
    private String userGroupName;

    @Column(name = "user_group_type")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "groupedit.type",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "groupedit.type_perms", value = "0"),
                    @DataTableColumnEditorAttr(key = "groupedit.type_email", value = "1")
                }
            )
        }
    )
    private Integer userGroupType;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "folders",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/groups?userGroupId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.doc.GroupDetails"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "groupId,groupName,fullPath"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-updateColumnsFunction", value = "updateColumnsGroupDetails"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,remove,import,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "user.group.groups_title"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false")
            }
        )
    })
    private List<GroupDetails> groupDetailsList;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "sites",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/web-pages?userGroupId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.doc.DocDetails"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "id,title,fullPath"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-updateColumnsFunction", value = "updateColumnsDocDetails"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,remove,import,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "user.group.doc_title"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true")
            }
        )
    })
    private List<DocDetails> docDetailsList;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "users",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/users?userGroupId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.users.userdetail.UserDetailsEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "id,login,firstName,lastName,email"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-updateColumnsFunction", value = "updateColumnsUserDetails"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,remove,import,celledit"),
                @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "user.group.users_title"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true")
            }
        )
    })
    private List<UserDetailsEntity> usersList;

    @Lob
    @Column(name = "user_group_comment")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "[[#{groupedit.comment}]]",
        tab = "basic"
    )
    private String userGroupComment;

    @Column(name = "require_approve")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "[[#{groupedit.require_approve}]]",
        tab = "basic"    )
    private Boolean requireApprove;

    @Column(name = "allow_user_edit")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "[[#{groupedit.allow_user_edit}]]",
        tab = "basic"
    )
    private Boolean allowUserEdit;


    @Column(name = "require_email_verification")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "[[#{components.user.require_email_verification_title}]]",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                message = "[[#{components.user.require_email_verification_info}]]"
            )
        }
    )
    private Boolean requireEmailVerification;

    @Column(name = "email_doc_id")
    @Convert(converter = DocDetailsNotNullConverter.class)
    @DataTableColumn(
        inputType = DataTableColumnType.JSON,
        className = "dt-tree-page-null",
        title = "[[#{groupedit.email_doc_id}]]",
        tab = "basic",
        visible = false
    )
    private DocDetailsDto emailDoc;

    @Column(name = "price_discount")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "groupedit.price_discount",
        tab = "basic"
    )
    @Max(100)
    @Min(0)
    private Integer priceDiscount;
}
