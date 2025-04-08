package sk.iway.iwcm.components.users.groups_approve;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupDetailsConverter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.users.UserBasicDto;
import sk.iway.iwcm.users.jpa.UserBasicDtoConverter;

/**
 * Pouzivatelia - mapovanie schvalovania
 */
@Entity
@Table(name = "groups_approve")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_USER_UPDATE)
public class GroupsApproveEntity implements Serializable {

    @Id
    @Column(name = "approve_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_groups_approve")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @NotNull
    @Valid
    @Column(name = "group_id")
    @Convert(converter = GroupDetailsConverter.class)
    @DataTableColumn(
        inputType = { DataTableColumnType.OPEN_EDITOR, DataTableColumnType.JSON },
        className = "dt-tree-group",
        title = "useredit.approve.dir"
    )
    private GroupDetails group;

    @NotNull
    @Column(name = "user_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Long userId;

    @NotNull
    @Column(name = "approve_mode")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "useredit.approve.mode",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "useredit.approveMode.approve", value = "0"),
                    @DataTableColumnEditorAttr(key = "useredit.approveMode.notify", value = "1"),
                    @DataTableColumnEditorAttr(key = "useredit.approveMode.none", value = "2"),
                    @DataTableColumnEditorAttr(key = "useredit.approveMode.level2", value = "3")
                }
            )
        }
    )
    private Integer approveMode;

    //User detials, who is approver
	@Column(name = "user_id", insertable = false, updatable = false) // author_id is allready used, so this one must be read onl
    @Convert(converter = UserBasicDtoConverter.class)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) //toto nepotrebujeme deserializovat pri post requeste
    private UserBasicDto userDetails;
}