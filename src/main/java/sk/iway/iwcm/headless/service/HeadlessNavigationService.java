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
     *
     * @param rootPath   virtual path of the root group (e.g., "/" for root)
     * @param rootGroupId group ID to start from (alternative to rootPath)
     * @param depth      maximum depth to traverse (0 = unlimited)
     * @param lng        optional language override
     * @param session    HTTP session for user context
     * @return list of top-level navigation items with children
     */
    public List<NavigationItem> buildNavigation(String rootPath, String rootGroupId, int depth, String lng, HttpSession session) {
        List<NavigationItem> result = new ArrayList<>();

        int startGroupId;

        if (Tools.isNotEmpty(rootPath)) {
            // Resolve root group from path
            startGroupId = resolveRootGroupId(rootPath);
        } else if (Tools.isNotEmpty(rootGroupId)) {
            startGroupId = Integer.parseInt(rootGroupId);
        } else {
            // Default to root (group 0)
            startGroupId = 0;
        }

        if (startGroupId <= 0) {
            return result;
        }

        // Build navigation tree from groups
        List<GroupDetails> groups = GroupsDB.getInstance().getGroupsTree(startGroupId, true, false, true, depth > 0 ? depth : null);
        if (groups == null || groups.isEmpty()) {
            return result;
        }

        // Convert groups to NavigationItems
        for (GroupDetails group : groups) {
            NavigationItem item = convertGroupToNavigationItem(group, lng, 0, session);
            result.add(item);
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
     */
    private NavigationItem convertGroupToNavigationItem(GroupDetails group, String lng, int level, HttpSession session) {
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
                    // Try to get language from document
                    // Language from document default - use constant
                    item.setLanguage(Constants.getString("defaultLanguage"));
                }
            }
        }

        // Check for children
        List<GroupDetails> children = GroupsDB.getInstance().getGroupsTree(group.getGroupId(), false, false, true);
        if (children != null && !children.isEmpty()) {
            item.setHasChildren(true);
            List<NavigationItem> childItems = new ArrayList<>();
            for (GroupDetails child : children) {
                childItems.add(convertGroupToNavigationItem(child, lng, level + 1, session));
            }
            item.setChildren(childItems);
        } else {
            item.setHasChildren(false);
        }

        return item;
    }
}
