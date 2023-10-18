package sk.iway.iwcm.components.seo.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class StatKeywordsDTO {

    public StatKeywordsDTO() {}

    public StatKeywordsDTO(String queryName, Integer queryCount) {
        this.queryName = queryName;
        this.queryCount = queryCount;
    }
    
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_searchengines.order",
        visible = true
    )
	private Integer order;

    //Hidden, used just for filter and for chart
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="editor.date",
        visible = false
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
}
