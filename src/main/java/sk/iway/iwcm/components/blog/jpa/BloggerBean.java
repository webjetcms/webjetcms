package sk.iway.iwcm.components.blog.jpa;

import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
public class BloggerBean {

    @Id
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title = "[[#{components.user.login}]]"
    )
    @NotBlank
    private String login;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{components.users.email}]]"
    )
    @NotBlank
    private String email;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{user.firstName}]]"
    )
    @NotBlank
    private String firstName;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "[[#{user.lastName}]]"
    )
    @NotBlank
    private String lastName;

    @DataTableColumn(
        inputType = DataTableColumnType.PASSWORD,
        title = "[[#{components.user.password}]]",
        visible = false,
        editor = {
            @DataTableColumnEditor(
				message="components.user.password.leaveBlankForGenerateTooltip"
			)
        }
    )
    private String password;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="stat_settings.group_id", filter=false, orderable=false, className = "dt-tree-group required",  editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addGroup") })
    })
    private GroupDetails editableGroup;
}
