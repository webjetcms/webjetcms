package sk.iway.iwcm.admin.layout;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.AdminlogBean;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@Getter
@Setter
public class AuditDto {
    private String type;
    private String description;
    private String date;
    private int createdByUserId;
    private String createdByUserName;
    private String createdByUserLogin;

    public AuditDto(AdminlogBean adminlog) {
        type = Prop.getInstance().getText("components.adminlog."+adminlog.getLogType());
        description = DB.prepareString(adminlog.getDescription(), 100);
        date = Tools.formatDateTime(adminlog.getCreateDate());
        createdByUserId = adminlog.getUserId();
        UserDetails user = UsersDB.getUser(createdByUserId);
        if (user != null) {
            createdByUserName = user.getFullName();
            createdByUserLogin = user.getLogin();
        }
    }
}
