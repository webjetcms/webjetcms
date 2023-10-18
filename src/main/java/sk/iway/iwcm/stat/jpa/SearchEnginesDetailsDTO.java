package sk.iway.iwcm.stat.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class SearchEnginesDetailsDTO {

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_searchengines.order"
    )
	private Integer order;

    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="editor.date"
    )
	private Date dayDate;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_searchengines.server_name"
    )
	private String serverName;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_top.name"
    )
	private String name;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_searchengines.remote_host"
    )
	private String remoteHost;
}
