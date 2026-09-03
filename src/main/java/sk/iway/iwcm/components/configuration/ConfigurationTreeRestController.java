package sk.iway.iwcm.components.configuration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeItemState;
import sk.iway.iwcm.admin.jstree.JsTreeMoveItem;
import sk.iway.iwcm.admin.jstree.JsTreeRestController;
import sk.iway.iwcm.system.ConfigurationModulePath;

/**
 * Provides the read-only module tree used to filter configuration variables.
 */
@RestController
@RequestMapping(value = "/admin/rest/settings/configuration/tree")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuConfig')")
public class ConfigurationTreeRestController extends JsTreeRestController<JsTreeItem> {

    static final String CHANGED_ID = "configuration-view-changed";
    static final String CUSTOM_ID = "configuration-view-custom";
    static final String ALL_ID = "configuration-view-all";
    static final String MODULE_NODE_ID_PREFIX = "configuration-module-node-";

    static final String TYPE_CHANGED = "configuration-changed";
    static final String TYPE_CUSTOM = "configuration-custom";
    static final String TYPE_ALL = "configuration-all";
    static final String TYPE_MODULE = "configuration-module";

    private final ConfigurationService configurationService;

    public ConfigurationTreeRestController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    protected void tree(Map<String, Object> result, JsTreeMoveItem item) {
        List<String> modulePaths = configurationService.getVisibleModulePaths(getUser());
        String changedText = getProp().getText("admin.conf_editor.tree.changed");
        String customText = getProp().getText("admin.conf_editor.tree.custom");
        String allText = getProp().getText("admin.conf_editor.tree.all");
        String treeSearchValue = getRequest().getParameter("treeSearchValue");

        List<JsTreeItem> items;
        if (Tools.isNotEmpty(treeSearchValue)) {
            items = getSearchItems(modulePaths, changedText, customText, allText, treeSearchValue, getRequest().getParameter("treeSearchType"));
        } else if (Tools.isEmpty(item.getId()) || "0".equals(item.getId())) {
            items = getRootItems(modulePaths, changedText, customText, allText);
        } else {
            String modulePath = getModulePathFromNodeId(item.getId());
            items = modulePath == null ? List.of() : getModuleItems(modulePaths, modulePath);
        }

        result.put("result", true);
        result.put("items", items);
    }

    static List<JsTreeItem> getRootItems(List<String> modulePaths, String changedText, String customText, String allText) {
        List<JsTreeItem> items = new ArrayList<>();
        items.add(createViewItem(CHANGED_ID, changedText, TYPE_CHANGED, "ti ti-database-edit", true));
        items.add(createViewItem(CUSTOM_ID, customText, TYPE_CUSTOM, "ti ti-user-code", false));
        items.add(createViewItem(ALL_ID, allText, TYPE_ALL, "ti ti-list", false));

        for (JsTreeItem moduleItem : getModuleItems(modulePaths, "")) {
            moduleItem.setParent("#");
            items.add(moduleItem);
        }
        return items;
    }

    /**
     * Returns only direct selectable module children of the supplied module path.
     */
    static List<JsTreeItem> getModuleItems(List<String> modulePaths, String parentPath) {
        Map<String, Boolean> childPaths = new TreeMap<>();
        String parentPrefix = Tools.isEmpty(parentPath) ? "" : parentPath + ".";

        if (modulePaths != null) {
            for (String modulePath : modulePaths) {
                if (Tools.isEmpty(modulePath) || modulePath.equals(parentPath)) continue;
                if (Tools.isNotEmpty(parentPrefix) && modulePath.startsWith(parentPrefix) == false) continue;

                String remainingPath = modulePath.substring(parentPrefix.length());
                int separatorIndex = remainingPath.indexOf('.');
                String childSegment = separatorIndex < 0 ? remainingPath : remainingPath.substring(0, separatorIndex);
                if (Tools.isEmpty(childSegment)) continue;

                String childPath = Tools.isEmpty(parentPath) ? childSegment : parentPrefix + childSegment;
                boolean hasChildren = separatorIndex >= 0;
                childPaths.merge(childPath, hasChildren, Boolean::logicalOr);
            }
        }

        List<JsTreeItem> items = new ArrayList<>();
        for (Map.Entry<String, Boolean> child : childPaths.entrySet()) {
            String childPath = child.getKey();
            String childText = childPath.substring(childPath.lastIndexOf('.') + 1);
            items.add(createModuleItem(childPath, childText, child.getValue(), null));
        }
        return items;
    }

    static List<JsTreeItem> getSearchItems(List<String> modulePaths, String changedText, String customText, String allText, String searchValue, String searchType) {
        Map<String, JsTreeItem> allItems = getAllItems(modulePaths, changedText, customText, allText);
        Set<String> includedIds = new LinkedHashSet<>();

        for (JsTreeItem candidate : allItems.values()) {
            if (matchesSearch(candidate.getText(), searchValue, searchType) == false) continue;

            JsTreeItem current = candidate;
            while (current != null && includedIds.add(current.getId())) {
                current = allItems.get(current.getParent());
            }
        }

        return allItems.values().stream()
            .filter(treeItem -> includedIds.contains(treeItem.getId()))
            .toList();
    }

    private static Map<String, JsTreeItem> getAllItems(List<String> modulePaths, String changedText, String customText, String allText) {
        Map<String, JsTreeItem> items = new LinkedHashMap<>();
        items.put(CHANGED_ID, createViewItem(CHANGED_ID, changedText, TYPE_CHANGED, "ti ti-database-edit", false));
        items.put(CUSTOM_ID, createViewItem(CUSTOM_ID, customText, TYPE_CUSTOM, "ti ti-user-code", false));
        items.put(ALL_ID, createViewItem(ALL_ID, allText, TYPE_ALL, "ti ti-list", false));

        Set<String> allModulePaths = new TreeSet<>();
        if (modulePaths != null) {
            for (String modulePath : modulePaths) {
                if (Tools.isEmpty(modulePath)) continue;

                String[] segments = modulePath.split("\\.");
                String path = "";
                for (String segment : segments) {
                    if (Tools.isEmpty(segment)) continue;
                    path = Tools.isEmpty(path) ? segment : path + "." + segment;
                    allModulePaths.add(path);
                }
            }
        }

        for (String modulePath : allModulePaths) {
            int separatorIndex = modulePath.lastIndexOf('.');
            String parentPath = separatorIndex < 0 ? null : modulePath.substring(0, separatorIndex);
            String text = separatorIndex < 0 ? modulePath : modulePath.substring(separatorIndex + 1);
            boolean hasChildren = allModulePaths.stream().anyMatch(path -> path.startsWith(modulePath + "."));
            String parentId = parentPath == null ? "#" : getModuleNodeId(parentPath);
            JsTreeItem moduleItem = createModuleItem(modulePath, text, hasChildren, parentId);
            items.put(moduleItem.getId(), moduleItem);
        }
        return items;
    }

    static boolean matchesSearch(String text, String searchValue, String searchType) {
        String normalizedText = DB.internationalToEnglish(Tools.getStringValue(text, "")).toLowerCase(Locale.ROOT);
        String normalizedSearch = DB.internationalToEnglish(Tools.getStringValue(searchValue, "")).toLowerCase(Locale.ROOT);

        if ("startwith".equals(searchType)) return normalizedText.startsWith(normalizedSearch);
        if ("endwith".equals(searchType)) return normalizedText.endsWith(normalizedSearch);
        if ("equals".equals(searchType)) return normalizedText.equals(normalizedSearch);
        return normalizedText.contains(normalizedSearch);
    }

    private static JsTreeItem createViewItem(String id, String text, String type, String icon, boolean selected) {
        JsTreeItem item = createItem(id, "", text, false, "#");
        item.setTypeCustom(type);
        item.setIcon(icon);
        item.setLiAttr(Map.of("data-configuration-view", type.substring("configuration-".length())));
        item.getState().setSelected(selected);
        return item;
    }

    private static JsTreeItem createModuleItem(String modulePath, String text, boolean hasChildren, String parentId) {
        JsTreeItem item = createItem(getModuleNodeId(modulePath), modulePath, text, hasChildren, parentId);
        item.setTypeCustom(TYPE_MODULE);
        item.setIcon("ti ti-folder-filled");
        item.setLiAttr(Map.of("data-configuration-module", modulePath));
        return item;
    }

    private static JsTreeItem createItem(String id, String virtualPath, String text, boolean hasChildren, String parent) {
        JsTreeItem item = new JsTreeItem();
        item.setId(id);
        item.setText(text);
        item.setAAttr(Map.of("aria-label", text));
        item.setVirtualPath(virtualPath);
        item.setParent(parent);
        item.setChildren(hasChildren);
        item.setState(new JsTreeItemState());
        return item;
    }

    static String getModuleNodeId(String modulePath) {
        String encodedPath = Base64.getUrlEncoder().withoutPadding().encodeToString(modulePath.getBytes(StandardCharsets.UTF_8));
        return MODULE_NODE_ID_PREFIX + encodedPath;
    }

    static String getModulePathFromNodeId(String nodeId) {
        if (nodeId == null || nodeId.startsWith(MODULE_NODE_ID_PREFIX) == false) return null;

        try {
            byte[] decodedPath = Base64.getUrlDecoder().decode(nodeId.substring(MODULE_NODE_ID_PREFIX.length()));
            String modulePath = new String(decodedPath, StandardCharsets.UTF_8);
            return ConfigurationModulePath.isValidPath(modulePath) ? modulePath : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    protected void move(Map<String, Object> result, JsTreeMoveItem item) {
        setReadOnlyError(result);
    }

    @Override
    protected void save(Map<String, Object> result, JsTreeItem item) {
        setReadOnlyError(result);
    }

    @Override
    protected void delete(Map<String, Object> result, JsTreeItem item) {
        setReadOnlyError(result);
    }

    private void setReadOnlyError(Map<String, Object> result) {
        result.put("result", false);
        result.put("error", getProp().getText("admin.operationPermissionDenied"));
    }

    @Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        return true;
    }
}
