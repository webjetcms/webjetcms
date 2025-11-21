package sk.iway.iwcm.components.formsimple;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableOptionMethod;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@Getter
@Setter
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "datatable.tab.basic", selected = true),
        @DataTableTab(id = "advanced", title = "datatable.tab.advanced")
})
public class FormSimpleItem {

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.formsimple.fieldType", className = "dt-row-edit", tab="basic", editor = {
        @DataTableColumnEditor(
            optionMethods = {
                @DataTableOptionMethod(
                    className = "sk.iway.iwcm.components.formsimple.FormSimpleApp",
                    methodName = "getFieldTypes",
                    labelProperty = "label",
                    valueProperty = "value"
                )
            }
        )
    })
    private String fieldType;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.formsimple.required", tab="basic", hidden = false)
    private Boolean required;

    @DataTableColumn(inputType = DataTableColumnType.QUILL, title = "components.formsimple.label", tab="basic", hidden = false)
    private String label;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.formsimple.value", tab="advanced", hidden = false)
    private String value;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.formsimple.placeholder", tab="advanced", hidden = false)
    private String placeholder;

    @DataTableColumn(inputType = DataTableColumnType.QUILL, title = "components.formsimple.tooltip", tab="advanced", hidden = false)
    private String tooltip;
}
