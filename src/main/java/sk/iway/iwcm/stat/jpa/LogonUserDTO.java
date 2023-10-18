package sk.iway.iwcm.stat.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class LogonUserDTO {

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_usrlogon.order"
    )
	private Integer order;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="user.admin.admin"
    )
	private Boolean admin;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_usrlogon.name",
		renderFormatLinkTemplate = "javascript:getUserDetails({{userId}});"
    )
	private String userName;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_usrlogon.company"
    )
	private String company;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_usrlogon.city"
    )
	private String city;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_usrlogon.logons"
    )
	private Integer logsCount;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_usrlogon.minutes"
    )
	private Integer logonMinutes;

    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="stat_usrlogon.last_logon",
        visible = false
    )
	private Date dayDate;

    //Hidden, used yust for identfication in back-end
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.users.id",
        visible = false
    )
	private Integer userId;
}
