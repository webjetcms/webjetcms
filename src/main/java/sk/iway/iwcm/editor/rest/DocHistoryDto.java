package sk.iway.iwcm.editor.rest;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class DocHistoryDto {

    @DataTableColumn(inputType = DataTableColumnType.ID, renderFormat = "dt-format-selector", title = "editor.cell.id")
    private Long id; //historyId

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.forum.docid", visible = false)
    private int docId;

    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title = "history.date")
    private Long historySaveDate;

    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title="editor.willBePublishedAt")
    private Date publishStartStringExtra;

    //obsahuje datum depublikovania (ak je nastavene disableAfterEnd)
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title="editor.willBeDepublishedAt")
    private Date publishEndExtra;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "history.title")
    private String title;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "history.changedBy")
    private String authorName;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "history.approvedBy")
    private String historyApprovedByName;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "history.disapprovedBy")
    private String historyDisapprovedByName;

    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title="editor.dateStart")
    private Date publishStartDate;

    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title="editor.dateEnd")
	private Date publishEndDate;

    private boolean historyActual;
    private boolean disableAfterEnd;
    private BaseEditorFields editorFields;

}
