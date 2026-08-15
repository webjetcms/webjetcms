package sk.iway.iwcm.components.translation_keys.rest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

@RestController
@RequestMapping(value = "/admin/rest/settings/translation-keys/tree")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('edit_text')")
public class TranslationKeyTreeRestController extends JsTreeRestController<JsTreeItem> {

    static final String ROOT_ID = "translation-key-root-node";

    private final TranslationKeyService translationKeyService;

    public TranslationKeyTreeRestController(TranslationKeyService translationKeyService) {
        this.translationKeyService = translationKeyService;
    }

    @Override
    protected void tree(Map<String, Object> result, JsTreeMoveItem item) {
        Set<String> keys = translationKeyService.getVisibleTranslationKeys(getUser());
        String treeSearchValue = getRequest().getParameter("treeSearchValue");
        List<JsTreeItem> items;

        if (Tools.isNotEmpty(treeSearchValue)) {
            items = getSearchItems(keys, treeSearchValue, getRequest().getParameter("treeSearchType"));
        } else if (Tools.isEmpty(item.getId()) || "0".equals(item.getId())) {
            items = List.of(createRootItem(hasFolderItems(keys)));
        } else {
            String prefix = ROOT_ID.equals(item.getId()) ? "" : item.getId();
            items = getItems(keys, prefix);
        }

        result.put("result", true);
        result.put("items", items);
    }

    static List<JsTreeItem> getItems(Set<String> keys, String prefix) {
        Map<String, Boolean> childSegments = new TreeMap<>();
        String prefixWithSeparator = Tools.isEmpty(prefix) ? "" : prefix + ".";

        for (String key : keys) {
            if (key.startsWith(prefixWithSeparator) == false || key.equals(prefix)) continue;

            String remainingKey = key.substring(prefixWithSeparator.length());
            int separatorIndex = remainingKey.indexOf('.');
            if (separatorIndex < 0) continue;

            String segment = remainingKey.substring(0, separatorIndex);
            if (Tools.isEmpty(segment)) continue;

            boolean hasChildFolders = remainingKey.indexOf('.', separatorIndex + 1) >= 0;
            childSegments.merge(segment, hasChildFolders, Boolean::logicalOr);
        }

        List<JsTreeItem> items = new ArrayList<>();
        for (Map.Entry<String, Boolean> child : childSegments.entrySet()) {
            String childPrefix = Tools.isEmpty(prefix) ? child.getKey() : prefixWithSeparator + child.getKey();
            items.add(createItem(childPrefix, child.getKey(), child.getValue(), null));
        }
        return items;
    }

    private List<JsTreeItem> getSearchItems(Set<String> keys, String searchValue, String searchType) {
        Map<String, JsTreeItem> allItems = getAllItems(keys);
        Set<String> includedIds = new LinkedHashSet<>();
        includedIds.add(ROOT_ID);

        for (JsTreeItem candidate : allItems.values()) {
            if (ROOT_ID.equals(candidate.getId()) || matchesSearch(candidate.getText(), searchValue, searchType) == false) continue;

            JsTreeItem current = candidate;
            while (current != null && includedIds.add(current.getId())) {
                current = allItems.get(current.getParent());
            }
        }

        return allItems.values().stream()
            .filter(treeItem -> includedIds.contains(treeItem.getId()))
            .sorted(Comparator.comparingInt(TranslationKeyTreeRestController::getDepth).thenComparing(JsTreeItem::getId))
            .toList();
    }

    private Map<String, JsTreeItem> getAllItems(Set<String> keys) {
        Map<String, JsTreeItem> items = new TreeMap<>();
        items.put(ROOT_ID, createRootItem(hasFolderItems(keys)));

        for (String key : keys) {
            String[] segments = key.split("\\.");
            String parent = ROOT_ID;
            String prefix = "";

            for (int i = 0; i < segments.length - 1; i++) {
                String segment = segments[i];
                if (Tools.isEmpty(segment)) continue;

                prefix = Tools.isEmpty(prefix) ? segment : prefix + "." + segment;
                boolean hasChildFolders = i < segments.length - 2;
                JsTreeItem treeItem = items.get(prefix);
                if (treeItem == null) {
                    treeItem = createItem(prefix, segment, hasChildFolders, parent);
                    items.put(prefix, treeItem);
                } else if (hasChildFolders) {
                    treeItem.setChildren(true);
                }
                parent = prefix;
            }
        }

        return items;
    }

    private static boolean hasFolderItems(Set<String> keys) {
        return keys.stream().anyMatch(key -> key.indexOf('.') > 0);
    }

    private static boolean matchesSearch(String text, String searchValue, String searchType) {
        String normalizedText = DB.internationalToEnglish(text).toLowerCase(Locale.ROOT);
        String normalizedSearch = DB.internationalToEnglish(searchValue).toLowerCase(Locale.ROOT);

        if ("startwith".equals(searchType)) return normalizedText.startsWith(normalizedSearch);
        if ("endwith".equals(searchType)) return normalizedText.endsWith(normalizedSearch);
        if ("equals".equals(searchType)) return normalizedText.equals(normalizedSearch);
        return normalizedText.contains(normalizedSearch);
    }

    private static int getDepth(JsTreeItem item) {
        if (ROOT_ID.equals(item.getId())) return 0;
        return item.getId().split("\\.").length;
    }

    private JsTreeItem createRootItem(boolean hasChildren) {
        JsTreeItem root = createItem(ROOT_ID, getProp().getText("components.translation_key.all_keys"), hasChildren, "#");
        root.setVirtualPath("");
        root.getState().setOpened(true);
        root.getState().setSelected(true);
        root.setIcon("ti ti-home");
        return root;
    }

    private static JsTreeItem createItem(String id, String text, boolean hasChildren, String parent) {
        JsTreeItem item = new JsTreeItem();
        item.setId(id);
        item.setText(text);
        item.setVirtualPath(id);
        item.setParent(parent);
        item.setState(new JsTreeItemState());
        setNodeHasChildren(item, hasChildren);
        return item;
    }

    private static void setNodeHasChildren(JsTreeItem item, boolean hasChildren) {
        item.setChildren(hasChildren);
        item.setIcon("ti ti-folder-filled");
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
