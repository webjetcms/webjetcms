package sk.iway.iwcm.doc;

import com.fasterxml.jackson.annotation.JsonProperty;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeItemState;
import sk.iway.iwcm.admin.jstree.JsTreeItemType;
import sk.iway.iwcm.users.UserDetails;

public class GroupsJsTreeItem extends JsTreeItem {

    @JsonProperty("groupDetails")
    private GroupDetails group;
    //private UserDetails user;

    public GroupsJsTreeItem(GroupDetails group, UserDetails user, boolean showPages) {
        this.group = group;
        //this.user = user;

        setId("" + group.getGroupId());
        setText(Tools.replace(group.getGroupName(), "&#47;", "/"));
        setVirtualPath(group.getVirtualPath());

        setIcon(getIconPrivate(user));
        setState(getStatePrivate());

        setChildren(hasChildren(showPages));
        setType(JsTreeItemType.GROUP);
    }

    private String getIconPrivate(UserDetails user) {
        String faPrefix = "fas";

        if (group.getMenuType()==GroupDetails.MENU_TYPE_HIDDEN) {
            faPrefix = "far";
        }

        if (group.isInternal()) {
            addLiClass("is-internal");
        }

        if (Tools.isNotEmpty(group.getPasswordProtected())) {
            addTextIcon("fas fa-lock");
        }

        if (GroupsDB.isGroupEditable(user, group.getGroupId())==false) {
            return faPrefix+" fa-folder-times";
        }

        return faPrefix+" fa-folder";
    }

    private JsTreeItemState getStatePrivate() {
        return new JsTreeItemState();
    }

    private boolean hasChildren(boolean showPages) {
        return GroupsDB.getInstance().hasAnyChild(group.getGroupId(), showPages);
    }

    public GroupDetails getGroup() {
        return group;
    }

    public void setGroup(GroupDetails group) {
        this.group = group;
    }
}
