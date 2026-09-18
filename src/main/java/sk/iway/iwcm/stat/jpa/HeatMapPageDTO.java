package sk.iway.iwcm.stat.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/** Read-only page totals for the selected domain and reporting period. */
@Getter
@Setter
public class HeatMapPageDTO {
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "webpages.doc_id")
    private Long id;

    @DataTableColumn(inputType = DataTableColumnType.DATE, title = "editor.date", visible = false, className = "not-export")
    private Date dayDate;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.stat.heatmap.title")
    private String name;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.stat.heatmap.url")
    private String url;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.stat.heatmap.clicks")
    private Long clicks;
}
