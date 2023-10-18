package sk.iway.iwcm.stat.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;


@Getter
@Setter
public class TopDTO {

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_browser.order"
    )
	private Integer order;

	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="editor.date",
        visible = false
    )
	private Date dayDate;

	@DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_doc.doc_id",
		renderFormatLinkTemplate = "javascript:getDocNew({{docId}});"
    )
	private String name;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_time.visits"
    )
	private Integer visits;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_time.sessions"
    )
	private Integer sessions;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat.graph.unique_users"
    )
	private Integer uniqueUsers;

    //Hidden, used just for identification
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="webpages.doc_id",
        visible = false
    )
	private Integer docId;
}
