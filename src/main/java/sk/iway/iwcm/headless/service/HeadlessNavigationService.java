package sk.iway.iwcm.headless.service;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.headless.dto.NavigationItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for building navigation trees from the WebJET CMS group structure.
 */
@Service("HeadlessNavigationService")
public class HeadlessNavigationService {

    /**
     * Builds a navigation tree from a given root group or path.
     * Uses GroupsDB.getGroups() recursively (similar to MenuULLI.doTree).
     *
     * @param rootPath   virtual path of the root group (e.g., "/" for root)
     * @param startGroupId group ID to start from (alternative to rootPath)
     * @param depth      maximum depth to traverse (0 = unlimited)
     * @param lng        optional language override
     * @param session    HTTP session for user context
     * @return list of top-level navigation items with children
     */
    public List<NavigationItem> buildNavigation(String rootPath, int startGroupId, int depth, String lng, HttpSession session) {
        List<NavigationItem> result = new ArrayList<>();

        int rootGroup = startGroupId;
        if (rootGroup <= 0 && Tools.isNotEmpty(rootPath)) {
            rootGroup = resolveRootGroupId(rootPath);
        }

        if (rootGroup <= 0) {
            return result;
        }

        // Get root group details
        GroupDetails rootGroupDetails = GroupsDB.getInstance().getGroup(rootGroup);
        if (rootGroupDetails == null) {
            return result;
        }

        // Skip groups with MENU_TYPE_NOSUB (no submenu)
        if (rootGroupDetails.getMenuType() == GroupDetails.MENU_TYPE_NOSUB) {
            return result;
        }

        // Build navigation tree using getGroups (recursive, like MenuULLI.doTree)
        List<GroupDetails> children = GroupsDB.getInstance().getGroups(rootGroup);
        if (children != null && !children.isEmpty()) {
            for (GroupDetails child : children) {
                NavigationItem item = convertGroupToNavigationItem(child, lng, 0, session, depth);
                if (item != null) {
                    result.add(item);
                }
            }
        }

        return result;
    }

    /**
     * Resolves a root group ID from a virtual path.
     */
    private int resolveRootGroupId(String rootPath) {
        if (Tools.isEmpty(rootPath)) {
            return 0;
        }

        // Try to resolve via DocDB
        int docId = DocDB.getInstance().getVirtualPathDocId(rootPath, "");
        if (docId > 0) {
            DocDetails doc = DocDB.getInstance().getDoc(docId);
            if (doc != null) {
                return doc.getGroupId();
            }
        }

        // Default to root group
        return 0;
    }

    /**
     * Converts a GroupDetails to a NavigationItem with children.
     * Skips internal groups and groups with MENU_TYPE_NOSUB.
     *
     * @param group the group details
     * @param lng optional language override
     * @param level current depth level
     * @param session HTTP session for user context
     * @param depth maximum depth (0 = unlimited)
     * @return NavigationItem or null if group should be skipped
     */
    private NavigationItem convertGroupToNavigationItem(GroupDetails group, String lng, int level, HttpSession session, int depth) {
        // Skip internal groups (not shown in menu)
        if (group.isInternal()) {
            return null;
        }

        // Skip groups with MENU_TYPE_NOSUB (no submenu)
        if (group.getMenuType() == GroupDetails.MENU_TYPE_NOSUB) {
            return null;
        }

        NavigationItem item = new NavigationItem();
        item.setTitle(group.getNavbarName());
        item.setLevel(level);

        int defaultDocId = group.getDefaultDocId();
        if (defaultDocId > 0) {
            DocDetails doc = DocDB.getInstance().getDoc(defaultDocId);
            if (doc != null) {
                item.setDocId(doc.getDocId());
                item.setVirtualPath(doc.getVirtualPath());
                if (Tools.isNotEmpty(lng)) {
                    item.setLanguage(lng);
                } else if (doc.getGroupId() > 0) {
                    item.setLanguage(Constants.getString("defaultLanguage"));
                }
            }
        }

        // Recursively get children using getGroups (like MenuULLI.doTree)
        int nextLevel = depth > 0 ? level + 1 : level + 1;
        if (depth <= 0 || nextLevel <= depth) {
            List<GroupDetails> children = GroupsDB.getInstance().getGroups(group.getGroupId());
            if (children != null && !children.isEmpty()) {
                List<NavigationItem> childItems = new ArrayList<>();
                for (GroupDetails child : children) {
                    NavigationItem childItem = convertGroupToNavigationItem(child, lng, level + 1, session, depth);
                    if (childItem != null) {
                        childItems.add(childItem);
                    }
                }
                if (!childItems.isEmpty()) {
                    item.setHasChildren(true);
                    item.setChildren(childItems);
                } else {
                    item.setHasChildren(false);
                }
            } else {
                item.setHasChildren(false);
            }
        } else {
            item.setHasChildren(false);
        }

        return item;
    }
}
