package sk.iway.iwcm.components.users.userdetail;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/**
 * Entity to edit user details for current user without useredit perms (show only limited fields)
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_USER_EDIT)
public class UserDetailsSelfEntity extends UserDetailsBasic {

    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.users.email}]]",
        tab = "personalInfo",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "useredit.access")
                }
            )
        },
        sortAfter = "photo"
    )
    private String email;

}
