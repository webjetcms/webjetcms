package sk.iway.iwcm.components.gdpr;

import jakarta.persistence.EntityListeners;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_GDPR_DELETE)
public class GdprDataDeletingEntity {

    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title="components.gdpr.type", className = "ai-off",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String type;

    //statTime include value
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat.time",
        hidden = true
    )
    private Integer statTime;

    //statTime is filled in RestController and is combination of statTime (integer value) and string value days/years
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat.time",
        hiddenEditor = true
    )
    private String statTimeString;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="formslist.pocet_zaznamov",
        hiddenEditor = true
    )
    private Integer recordCnt;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="editor.form.action",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String action;
}
