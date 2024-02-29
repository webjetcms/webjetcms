package sk.iway.iwcm.stat.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class CountryDTO {

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_country.order"
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
        inputType = DataTableColumnType.TEXT,
        title="stat_country.country"
    )
	private String country;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_country.visits"
    )
	private Integer visits;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="%",
        renderFormat = "dt-format-number--decimal"
    )
	private Double percentage;
}
