package sk.iway.iwcm.stat.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class LogonUserDetailsDTO {

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_usrlogon.order"
    )
	private Integer order;

    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="stat_usrlogon.logon_time"
    )
	private Date dayDate;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_usrlogon.minutes"
    )
	private Integer logonMinutes;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_usrlogon.hostname"
    )
	private String hostName;
}
