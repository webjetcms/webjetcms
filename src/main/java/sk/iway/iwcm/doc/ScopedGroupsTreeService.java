package sk.iway.iwcm.doc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Builds jsTree folder nodes within the configured scope of applications such as News and Blog.
 *
 * <p>The scope is resolved from {@link GroupsDB} during construction and contains configured roots
 * and their visible descendants. Visible ancestors outside that scope provide navigation paths
 * but cannot be selected. Folder permissions determine visibility and selection unless the caller
 * explicitly disables those checks for an application-managed scope.</p>
 *
 * <p>Supports initial tree loading, lazy child loading, and folder-name searches through
 * {@link #getItems(int, int, String, String)}.</p>
 */
public class ScopedGroupsTreeService {
    private final Identity user;
    private final boolean checkGroupsPerms;
    private final Map<Integer, GroupDetails> groups = new LinkedHashMap<>();
    private final Map<Integer, String> filters = new LinkedHashMap<>();
    private final Map<Integer, List<Integer>> children = new LinkedHashMap<>();
    private final Set<Integer> selectable = new HashSet<>();
    private final Set<Integer> navigationParents = new HashSet<>();

    /**
     * Resolves the application folder scope with folder permission checks enabled.
     *
     * @param folders configured roots whose values contain a positive folder ID, optionally followed
     *                by {@code *} to preserve recursive article filtering
     * @param user user whose permissions and tree sorting preferences apply; {@code null} produces
     *             an empty scope
     * @param domain domain to include when {@code multiDomainEnabled} is enabled; must not be
     *               {@code null} in that case
     * @see #ScopedGroupsTreeService(List, Identity, String, boolean)
     */
    public ScopedGroupsTreeService(List<LabelValue> folders, Identity user, String domain) {
        this(folders, user, domain, true);
    }

    /**
     * Resolves configured roots, visible descendants, and navigation ancestors from the folder cache.
     *
     * <p>Descendants are traversed regardless of a root's {@code *} suffix. The suffix is retained
     * in the node's article filter, and a suffixed value takes precedence for duplicate root IDs.
     * Folders that fail visibility checks are skipped together with their descendant traversal.</p>
     *
     * <p>With permission checks enabled, only editable folders are selectable; viewable folders
     * remain available for navigation. Users restricted to individual pages without editable folders
     * receive an empty scope. Ancestors added above the configured roots are never selectable.</p>
     *
     * @param folders configured roots whose values contain a positive folder ID, optionally followed
     *                by {@code *}; labels are unused, and invalid or nonpositive IDs are ignored
     * @param user user whose permissions and tree sorting preferences apply; {@code null} produces
     *             an empty scope
     * @param domain domain to include when {@code multiDomainEnabled} is enabled; must not be
     *               {@code null} in that case
     * @param checkGroupsPerms whether to enforce folder edit and view permissions; {@code false}
     *                         trusts the caller's configured scope while retaining domain and hidden-folder checks
     */
    public ScopedGroupsTreeService(List<LabelValue> folders, Identity user, String domain, boolean checkGroupsPerms) {
        this.user = user;
        this.checkGroupsPerms = checkGroupsPerms;
        if (user == null || (checkGroupsPerms && Tools.isEmpty(user.getEditableGroups(true)) && Tools.isNotEmpty(user.getEditablePages()))) return;

        GroupsDB groupsDB = GroupsDB.getInstance();
        for (LabelValue folder : folders) {
            String value = folder.getValue();
            int id = Tools.getIntValue(Tools.replace(value, "*", ""), -1);
            if (id > 0) filters.merge(id, value, (oldValue, newValue) -> oldValue.endsWith("*") ? oldValue : newValue);
        }

        ArrayDeque<Integer> pending = new ArrayDeque<>(filters.keySet());
        Set<Integer> visited = new HashSet<>();
        while (!pending.isEmpty()) {
            int id = pending.removeFirst();
            if (!visited.add(id)) continue;
            GroupDetails group = groupsDB.getGroup(id);
            if (!isVisible(group, domain)) continue;

            groups.put(id, group);
            if (!checkGroupsPerms || GroupsDB.isGroupEditable(user, id)) selectable.add(id);
            for (GroupDetails child : GroupsTreeService.sortGroupsBasedOnUserSettings(user, groupsDB.getGroups(id))) {
                pending.addLast(child.getGroupId());
            }
        }

        for (int id : filters.keySet()) {
            GroupDetails group = groups.get(id);
            while (group != null && group.getParentGroupId() > 0 && !groups.containsKey(group.getParentGroupId())) {
                group = groupsDB.getGroup(group.getParentGroupId());
                if (!isVisible(group, domain)) break;
                groups.put(group.getGroupId(), group);
                navigationParents.add(group.getGroupId());
            }
        }

        for (GroupDetails group : groups.values()) {
            children.computeIfAbsent(parentId(group), key -> new ArrayList<>()).add(group.getGroupId());
        }
    }

    /**
     * Creates a flat list of jsTree nodes for initial loading, lazy child loading, or searching.
     *
     * <p>Without a search, {@code parentId == 0} loads the roots and opens navigation ancestors
     * and the selected folder's path, including direct children of every opened folder. If the
     * requested selection is unavailable, the first selectable folder collected during construction
     * is selected if one exists. A nonzero parent ID loads only that folder's direct children.</p>
     *
     * <p>A nonblank search takes precedence over both IDs and returns matching folder names with
     * their scoped ancestors opened. Comparisons ignore case and diacritics. Search and lazy-load
     * responses do not mark any node as selected. Nodes outside the selectable set are disabled.</p>
     *
     * @param parentId parent folder whose children to load, or {@code 0} for the initial tree
     * @param selectedId preferred selectable folder for initial loading; ignored for searches
     *                   and lazy child loading
     * @param searchValue folder-name search text, or {@code null} or a blank string to load the tree
     * @param searchType comparison mode: {@code startwith}, {@code endwith}, or {@code equals};
     *                   any other value, including {@code null}, uses substring matching
     * @return nodes ordered by the user's tree sorting preferences, with parent IDs and loading
     *         states set for jsTree; empty if no nodes qualify or a requested lazy-load parent is unknown
     */
    public List<GroupsJsTreeItem> getItems(int parentId, int selectedId, String searchValue, String searchType) {
        Set<Integer> included = new LinkedHashSet<>();
        Set<Integer> opened = new HashSet<>();
        boolean searching = Tools.isNotEmpty(searchValue);
        int selection = -1;

        if (searching) {
            String search = normalize(searchValue);
            for (GroupDetails group : groups.values()) {
                if (matches(normalize(group.getGroupName()), search, searchType)) {
                    included.add(group.getGroupId());
                    addParents(group.getGroupId(), included, opened);
                }
            }
        } else if (parentId == 0) {
            selection = selectable.contains(selectedId) ? selectedId : groups.keySet().stream()
                    .filter(selectable::contains).findFirst().orElse(-1);
            included.addAll(children.getOrDefault(0, List.of()));
            included.addAll(navigationParents);
            opened.addAll(navigationParents);
            if (selection > 0) {
                addParents(selection, included, opened);
                included.add(selection);
            }
            for (int id : opened) included.addAll(children.getOrDefault(id, List.of()));
        } else if (groups.containsKey(parentId)) {
            included.addAll(children.getOrDefault(parentId, List.of()));
        }

        List<GroupsJsTreeItem> items = new ArrayList<>();
        for (GroupDetails group : GroupsTreeService.sortGroupsBasedOnUserSettings(user, new ArrayList<>(groups.values()))) {
            int id = group.getGroupId();
            if (!included.contains(id)) continue;
            boolean navigationOnly = navigationParents.contains(id);
            GroupsJsTreeItem item = new GroupsJsTreeItem(group, user, false, checkGroupsPerms);
            item.setGroupIdList(navigationOnly ? null : filters.getOrDefault(id, String.valueOf(id)));
            int parent = parentId(group);
            item.setParent(parent == 0 ? "#" : String.valueOf(parent));
            if (navigationOnly) item.setIcon("ti ti-folder-x");
            item.setAAttr(Map.of("title", group.getFullPath()));
            item.getState().setDisabled(!selectable.contains(id));
            item.getState().setSelected(id == selection);
            item.getState().setOpened(opened.contains(id));
            // Flat, preloaded branches must not advertise an additional lazy load.
            item.setChildren(searching || opened.contains(id) ? null : children.containsKey(id));
            items.add(item);
        }
        return items;
    }

    /**
     * Checks whether a folder belongs to the selectable scope resolved during construction.
     *
     * @param groupId folder ID to check
     * @return {@code true} if the folder is selectable; {@code false} for navigation-only,
     *         excluded, or unknown folders
     */
    public boolean isSelectable(int groupId) {
        return selectable.contains(groupId);
    }

    /**
     * Applies domain, hidden-folder, and optional folder permission checks to a candidate folder.
     *
     * @param group candidate folder, or {@code null} if it does not exist
     * @param domain required folder domain when {@code multiDomainEnabled} is enabled
     * @return {@code true} if the folder exists and passes all applicable visibility checks
     */
    private boolean isVisible(GroupDetails group, String domain) {
        if (group == null) return false;
        if (Constants.getBoolean("multiDomainEnabled") && !domain.equals(group.getDomainName())) return false;
        if (group.isHiddenInAdmin() && user.isDisabledItem("editor_show_hidden_folders")) return false;
        return !checkGroupsPerms || GroupsDB.isGroupEditable(user, group.getGroupId()) || GroupsDB.isGroupViewable(user, group.getGroupId());
    }

    private int parentId(GroupDetails group) {
        return groups.containsKey(group.getParentGroupId()) ? group.getParentGroupId() : 0;
    }

    /**
     * Adds a folder's scoped ancestors to the included and opened node sets.
     *
     * <p>Traversal stops at the scoped root or a repeated folder to avoid following cycles
     * indefinitely. An unknown starting folder leaves both sets unchanged.</p>
     *
     * @param id folder ID whose ancestor path to traverse
     * @param included mutable set to which ancestor IDs are added
     * @param opened mutable set to which ancestor IDs to expand are added
     */
    private void addParents(int id, Set<Integer> included, Set<Integer> opened) {
        Set<Integer> visited = new HashSet<>();
        GroupDetails group = groups.get(id);
        while (group != null && visited.add(group.getGroupId())) {
            int parent = parentId(group);
            if (parent == 0) break;
            included.add(parent);
            opened.add(parent);
            group = groups.get(parent);
        }
    }

    private static String normalize(String value) {
        return DB.internationalToEnglish(value).toLowerCase(Locale.ROOT);
    }

    private static boolean matches(String name, String search, String type) {
        if ("startwith".equals(type)) return name.startsWith(search);
        if ("endwith".equals(type)) return name.endsWith(search);
        if ("equals".equals(type)) return name.equals(search);
        return name.contains(search);
    }
}
