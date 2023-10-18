package sk.iway.iwcm.stat.jpa;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/*This class is used to show incoming outgoing stats in doc-new page */

@Getter
@Setter
public class InOutDTO {
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_errors.order"
    )
	private Integer order;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_top.name"
    )
	private String name;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_errors.count"
    )
	private Integer count;
}
