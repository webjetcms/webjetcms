package sk.iway.iwcm.stat.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class ErrorDTO {

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_errors.order"
    )
	private Integer order;

    //Hidden, used just for filter
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="editor.date",
        visible = false,
        className = "not-export"
    )
    private Date dayDate;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_errors.year"
    )
	private Integer year;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_errors.week"
    )
	private Integer week;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_errors.url"
    )
	private String url;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_errors.query_string"
    )
	private String errorMessage;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_errors.count"
    )
	private Integer count;
}
