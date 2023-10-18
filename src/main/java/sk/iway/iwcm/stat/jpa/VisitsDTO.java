package sk.iway.iwcm.stat.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class VisitsDTO {

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_browser.order"
    )
	private Integer order;

	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="editor.date"
    )
	private Date dayDate;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_time.year",
        visible = false
    )
	private Integer year;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_time.week",
        visible = false
    )
	private Integer week;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_doc.month",
        visible = false
    )
	private Integer month;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_time.hour",
        visible = false
    )
	private Integer hour;

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
}
