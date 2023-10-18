package sk.iway.iwcm.doc;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
public class TemplateDetailEditorFields {

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "templates.temps-list.replace_template",
        tab = "templatesTab",
        className = "hide-on-create",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before")
                },
                message = "templates.temps-list.replace_template.tooltip"
            )
        }
    )
    private Integer mergeToTempId = -1;

    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "&nbsp;",
        tab = "templatesTab",
        className = "hide-on-create",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "templates.temps-list.replace_template_checkbox", value = "true")
                }
            )
        }
    )
    private boolean mergeTemplates = false;
}
