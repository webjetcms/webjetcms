package sk.iway.iwcm.stat.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class DocNewDTO {

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_time.order"
    )
	private Integer order;

    //Hidden, used just for filter and for chart
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="editor.date",
        visible = false,
        className = "not-export"
    )
	private Date dayDate;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_time.year"
    )
	private Integer year;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_doc.month"
    )
	private Integer month;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_doc.week",
        visible = false //initiali not visible
    )
	private Integer week;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_doc.days"
    )
	private Integer day;

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
        title="stat_time.unique_users"
    )
	private Integer uniqueUsers;

    //Variant B
    //Columns are hidden until "Varinat B" graph data containt at least 1 value
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat.doc_new.visits.b_variant",
        visible = false
    )
	private Integer visitsVarinatB;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat.doc_new.sessions.b_variant",
        visible = false
    )
	private Integer sessionsVarinatB;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat.doc_new.unique_users.b_variant",
        visible = false
    )
	private Integer uniqueUsersVarinatB;
}
