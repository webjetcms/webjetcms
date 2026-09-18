package sk.iway.iwcm.components.news;

import lombok.Getter;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsJsTreeItem;
import sk.iway.iwcm.users.UserDetails;

/** A News folder with its article filter kept separate from the numeric tree ID. */
@Getter
public class NewsTreeItem extends GroupsJsTreeItem {
    private final String groupIdList;

    public NewsTreeItem(GroupDetails group, UserDetails user, String groupIdList) {
        super(group, user, false);
        this.groupIdList = groupIdList;
    }
}
