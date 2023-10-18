package sk.iway.iwcm.stat.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class ActualLogonUserDTO {

    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.stat.actual_users.logon_time"
    )
	private Date logonTime;

    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="components.stat.actual_users.last_activity"
    )
	private Date lastActivity;

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
        title="components.stat.actual_users.last_url"
    )
	private String lastUrl;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.stat.actual_users.ip"
    )
	private String userIp;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.stat.actual_users.host"
    )
	private String userHost;

    //Hidden, used yust for identfication in back-end
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.users.id",
        visible = false
    )
	private Integer userId;
}
