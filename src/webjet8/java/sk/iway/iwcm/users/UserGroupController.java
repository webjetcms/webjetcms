package sk.iway.iwcm.users;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.datatables.DataTablesFieldError;
import sk.iway.iwcm.system.datatables.DataTablesInterface;

/**
 * UserGroupController.java
 *
 * Class UserGroupController is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2019
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      19.2.2019 17:31
 * modified     19.2.2019 17:31
 */

public class UserGroupController implements DataTablesInterface {

    @Override
    public List<Object> list(HttpServletRequest request) {
        List<UserGroupDetailsWrapper> temp = new ArrayList<>();
        for (UserGroupDetails userGroupDetails : UserGroupsDB.getInstance().getUserGroups()) {
            temp.add(new UserGroupDetailsWrapper(userGroupDetails));
        }
        this.fetchCounts(temp);
        return new ArrayList<>(temp);
    }

    @Override
    public List<Object> save(HttpServletRequest request, Map<Integer, Map<String, String>> parsedRequest) {
        Map<String,String> parsedMap = parsedRequest.get(parsedRequest.keySet().iterator().next());

        UserGroupDetails userGroupDetails = null;
        switch (parsedMap.get("action")) {
            case "create" :
                userGroupDetails = new UserGroupDetails();
                this.mapUserGroup(userGroupDetails, parsedMap);
                UserGroupsDB.saveUserGroup(userGroupDetails);
                break;
            case "edit" :
                userGroupDetails = UserGroupsDB.getInstance().getUserGroup(Tools.getIntValue(parsedMap.get("userGroupId"),-1));
                this.mapUserGroup(userGroupDetails, parsedMap);
                UserGroupsDB.saveUserGroup(userGroupDetails);
                break;
            case "remove" :
                userGroupDetails = UserGroupsDB.getInstance().getUserGroup(Tools.getIntValue(parsedMap.get("userGroupId"),-1));
                UserGroupsDB.getInstance().remove(userGroupDetails.getUserGroupId());
                break;
            default: break;
        }
        List<UserGroupDetailsWrapper> ret = new ArrayList<>();
        if (userGroupDetails != null)
            ret.add(new UserGroupDetailsWrapper(userGroupDetails));
        return new ArrayList<>(ret);
    }

    @Override
    public List<DataTablesFieldError> getFieldErrors() {
        return null;
    }

    @Override
    public String getError() {
        return null;
    }

    @Override
    public boolean canSave(Identity user) {
        return !(user==null || user.isEnabledItem("user.admin.userGroups")==false);
    }

    private void mapUserGroup(UserGroupDetails userGroupDetails, Map<String,String> parsedMap) {
        userGroupDetails.setUserGroupId(Tools.getIntValue(parsedMap.get("userGroupId"),-1));
        userGroupDetails.setEmailDocId(Tools.getIntValue(parsedMap.get("emailDocId"), -1));
        userGroupDetails.setUserGroupName(parsedMap.get("userGroupName"));
        userGroupDetails.setAllowUserEdit(Tools.getBooleanValue(parsedMap.get("allowUserEdit"),false));
        userGroupDetails.setRequireApprove(Tools.getBooleanValue(parsedMap.get("requireApprove"),false));
//        userGroupDetails.setRequireEmailVerification(Tools.getBooleanValue(parsedMap.get("userGroupType"), false));
        userGroupDetails.setUserGroupType(Tools.getIntValue(parsedMap.get("userGroupType"), -1));
        userGroupDetails.setUserGroupComment(parsedMap.get("userGroupComment"));
    }

    private void fetchCounts(List<UserGroupDetailsWrapper> list) {
        Map<Integer, Integer> userGroupCountMap = UserTools.numberOfUsersInGroups();
        Map<Integer, String> protectedGroupsMap = GroupsDB.getProtectedGroups();
        for (UserGroupDetailsWrapper userGroupDetailsWrapper : list ) {
            // find user count
            userGroupDetailsWrapper.setUserGroupCount(userGroupCountMap.getOrDefault(userGroupDetailsWrapper.getUserGroupId(), 0));
            // find doc groups count
            String testStr = protectedGroupsMap.get(userGroupDetailsWrapper.getUserGroupId());
            if (Tools.isNotEmpty(testStr)) {
                // kedze stringy mozu byt napr ",123," a to SQL nezobere...
                String[] test = protectedGroupsMap.get(userGroupDetailsWrapper.getUserGroupId()).split(",");
                userGroupDetailsWrapper.setWebGroupCount(test.length);
                userGroupDetailsWrapper.setWebCount(new SimpleQuery().forInt(
                        "SELECT count(doc_id) FROM documents WHERE available=1 AND (password_protected IS null OR password_protected='') AND group_id IN ("+String.join(",", test) +")"));
            }
        }
    }
}
