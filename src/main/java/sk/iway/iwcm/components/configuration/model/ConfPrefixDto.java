package sk.iway.iwcm.components.configuration.model;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/**
 * DTO object for configuration with prefix, it shows all conf. with given prefix
 */
@Setter
@Getter
public class ConfPrefixDto extends ConfDetails {

    @DataTableColumn(
        inputType = DataTableColumnType.ID,
        renderFormat = "dt-format-selector",
        title = "admin.conf_editor.id"
    )
    private Long id;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        renderFormat = "dt-format-text-wrap",
        title = "admin.conf_editor.old_value",
        sortAfter = "value",
        className = "wrap",
        editor = {
            @DataTableColumnEditor(
                type = "textarea",
                attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled"),
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
                }
                /*className: 'hide-on-create'*/
            )
        }
    )
    private String oldValue;

    @DataTableColumn(
        hidden = true,
        inputType = DataTableColumnType.BOOLEAN,
        title="admin.conf_editor.encrypt",
        sortAfter = "value"
    )
    private boolean encrypt;
}
