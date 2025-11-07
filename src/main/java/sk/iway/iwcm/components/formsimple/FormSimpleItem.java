package sk.iway.iwcm.components.formsimple;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableOptionMethod;

@Getter
@Setter
public class FormSimpleItem {

    public static enum FieldsNames {
        REQUIRED("required"),
        LABEL("label"),
        VALUE("value"),
        PLACEHOLDER("placeholder"),
        TOOLTIP("tooltip");

        private final String fieldName;

        FieldsNames(String fieldName) {
            this.fieldName = fieldName;
        }

        public String value() {
            return fieldName;
        }
    }

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.formsimple.fieldType", editor = {
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

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.formsimple.required", hidden = true)
    private Boolean required;

    @DataTableColumn(inputType = DataTableColumnType.QUILL, title = "components.formsimple.label", hidden = true)
    private String label;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.formsimple.value", hidden = true)
    private String value;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.formsimple.placeholder", hidden = true)
    private String placeholder;

    @DataTableColumn(inputType = DataTableColumnType.QUILL, title = "components.formsimple.tooltip", hidden = true)
    private String tooltip;
}
