package sk.iway.iwcm.components.configuration.model;

import java.util.Date;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Setter
@Getter
public class ConfDetailsDto extends ConfDetails {

	@DataTableColumn(
        inputType = DataTableColumnType.ID,
        renderFormat = "dt-format-selector",
        title = "admin.conf_editor.id",
        tab = "basic"
    )
    private Long id;

	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        renderFormat = "dt-format-text-wrap",
        title = "admin.conf_editor.old_value",
        tab = "basic",
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
        tab="basic",
        sortAfter = "value"
    )
    private boolean encrypt;

    @DataTableColumn(
        hidden = true,
        inputType = DataTableColumnType.DATE,
        renderFormat = "dt-format-date-time",
        title = "admin.conf_editor.change_from",
        tab = "advanced",
        editor = {
            @DataTableColumnEditor(
                type = "datetime"
            )
        }
    )
   private Date datePrepared;

    //special anotation, create a ConfPreparedEntity table inside history tab of configuration.pug
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "advanced",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/settings/prepared?mode=planned&name={name}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.configuration.model.ConfPreparedEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "3,desc"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,import,celledit")
            }
        )
    })
    private List<ConfPreparedEntity> confPrepared;

    //special anotation, create a ConfPreparedEntity table inside history tab of configuration.pug
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "history",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/settings/prepared?mode=history&name={name}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.configuration.model.ConfPreparedEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "2,desc"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,remove,import,celledit")
            }
        )
    })
    private List<ConfPreparedEntity> confHistory;

    public ConfDetailsDto() {
    }
}