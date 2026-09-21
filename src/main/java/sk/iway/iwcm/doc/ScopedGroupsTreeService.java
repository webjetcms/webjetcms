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

/** Builds a scoped application folder tree with navigation-only ancestors above its configured folders. */
public class ScopedGroupsTreeService {
    private final Identity user;
    private final boolean checkGroupsPerms;
    private final Map<Integer, GroupDetails> groups = new LinkedHashMap<>();
    private final Map<Integer, String> filters = new LinkedHashMap<>();
    private final Map<Integer, List<Integer>> children = new LinkedHashMap<>();
    private final Set<Integer> selectable = new HashSet<>();
    private final Set<Integer> navigationParents = new HashSet<>();

    /**
     * Resolves configured roots and their visible descendants from the cached folder structure.
     * Viewable ancestors remain expandable but cannot select an article list.
     */
    public ScopedGroupsTreeService(List<LabelValue> folders, Identity user, String domain) {
        this(folders, user, domain, true);
    }

    /** Allows applications with their own administrative folder scope to supply trusted roots. */
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
     * Returns roots, lazy children, or search matches with their scoped ancestors.
     * The initial response opens the selected folder's path and includes its siblings.
     */
    public List<ScopedGroupsTreeItem> getItems(int parentId, int selectedId, String searchValue, String searchType) {
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

        List<ScopedGroupsTreeItem> items = new ArrayList<>();
        for (GroupDetails group : GroupsTreeService.sortGroupsBasedOnUserSettings(user, new ArrayList<>(groups.values()))) {
            int id = group.getGroupId();
            if (!included.contains(id)) continue;
            boolean navigationOnly = navigationParents.contains(id);
            ScopedGroupsTreeItem item = new ScopedGroupsTreeItem(group, user, navigationOnly ? null : filters.getOrDefault(id, String.valueOf(id)), checkGroupsPerms);
            int parent = parentId(group);
            item.setParent(parent == 0 ? "#" : String.valueOf(parent));
            if (navigationOnly) item.setIcon("ti ti-folders");
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

    /** Checks whether a folder can be selected within this user's application scope. */
    public boolean isSelectable(int groupId) {
        return selectable.contains(groupId);
    }

    private boolean isVisible(GroupDetails group, String domain) {
        if (group == null) return false;
        if (Constants.getBoolean("multiDomainEnabled") && !domain.equals(group.getDomainName())) return false;
        if (group.isHiddenInAdmin() && user.isDisabledItem("editor_show_hidden_folders")) return false;
        return !checkGroupsPerms || GroupsDB.isGroupEditable(user, group.getGroupId()) || GroupsDB.isGroupViewable(user, group.getGroupId());
    }

    private int parentId(GroupDetails group) {
        return groups.containsKey(group.getParentGroupId()) ? group.getParentGroupId() : 0;
    }

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
