package sk.iway.iwcm.components.users.usergroups;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

/**
 * Skupiny pouzivatelov, udaje zapisuje do DB ale pri zmene vola
 * UserGroupsDB.getInstance(true), kedze skupiny pouzivatelov su cachovane
 */
@RestController
@Datatable
@RequestMapping("/admin/rest/user-groups")
@PreAuthorize("@WebjetSecurityService.hasPermission('user.admin.userGroups')")
public class UserGroupsRestController extends DatatableRestControllerV2<UserGroupsEntity, Long> {

    public UserGroupsRestController(UserGroupsRepository userGroupsRepository) {
        super(userGroupsRepository);
    }

    @Override
    public void afterSave(UserGroupsEntity entity, UserGroupsEntity saved) {
        UserGroupsDB.getInstance(true);
    }

    @Override
    public void afterDelete(UserGroupsEntity entity, long id) {
        UserGroupsDB.getInstance(true);
    }

    @Override
    public boolean processAction(UserGroupsEntity entity, String action) {

        if (entity.getId()!=null && entity.getId().intValue()>0) {
            int groupId = entity.getId().intValue();

            if ("addGroupToAllUsers".equals(action)) {
                List<UserDetails> users = UsersDB.getUsers();
                for (UserDetails user : users)
                {
                    user.addToGroup(groupId);
                    UsersDB.saveUser(user);
                }

                return true;
            } else if("removeGroupFromAllUsers".equals(action)) {
                List<UserDetails> users = UsersDB.getUsers();
                for (UserDetails user : users)
                {
                    user.removeFromGroup(groupId);
                    UsersDB.saveUser(user);
                }
                return true;
            } else if("deleteAllUsersOfThisGroup".equals(action)) {
                List<UserDetails> users = UsersDB.getUsersByGroup(groupId);
                for (UserDetails user : users) {
                    if (user.isInUserGroup(groupId))
					{
                        user.removeFromGroup(groupId);

						if (Tools.isEmpty(user.getUserGroupsIds()) && user.isAdmin() == false && "delete".equals(Constants.getString("dmailUnsubscribeMode"))) {
							UsersDB.deleteUser(user.getUserId());
                        } else if (Tools.isEmpty(user.getUserGroupsIds()) && user.isAdmin() == false && "disable".equals(Constants.getString("dmailUnsubscribeMode"))) {
							user.setAuthorized(false);
                            //v rezime disable ma zostat posledne nastavena user skupina pre neskorsie ucely, takze mu ju znova musime vratit
                            user.addToGroup(groupId);
                            UsersDB.saveUser(user);
                        } else if (Tools.isEmpty(user.getUserGroupsIds()) && user.isAdmin() == false && "removeGroups".equals(Constants.getString("dmailUnsubscribeMode"))) {
							user.setAuthorized(false);
                            UsersDB.saveUser(user);
                        } else {
							UsersDB.saveUser(user);
                        }
					}
                }
                return true;
            }
        }

        return false;
    }

}
