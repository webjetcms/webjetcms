package sk.iway.iwcm.stat.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class SearchEnginesDTO {

    public SearchEnginesDTO() {}

    public SearchEnginesDTO(String serverName, Integer accesCount) {
        this.serverName = serverName;
        this.accesCount = accesCount;
    }

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_searchengines.order"
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
        inputType = DataTableColumnType.TEXT,
        title="stat_searchengines.query_name",
		renderFormatLinkTemplate = "javascript:getSearchEnginesDetails({{order}});"
    )
	private String queryName;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_searchengines.query_count"
    )
	private Integer queryCount;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="%",
        renderFormat = "dt-format-number--decimal"
    )
	private Double percentage;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_searchengines.server_name"
    )
	private String serverName;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_searchengines.access_count"
    )
	private Integer accesCount;
}
