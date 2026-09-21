package sk.iway.iwcm.doc;

import lombok.Getter;
import sk.iway.iwcm.users.UserDetails;

/** An application folder with its article filter kept separate from the numeric tree ID. */
@Getter
public class ScopedGroupsTreeItem extends GroupsJsTreeItem {
    private final String groupIdList;

    public ScopedGroupsTreeItem(GroupDetails group, UserDetails user, String groupIdList, boolean checkGroupsPerms) {
        super(group, user, false, checkGroupsPerms);
        this.groupIdList = groupIdList;
    }
}
