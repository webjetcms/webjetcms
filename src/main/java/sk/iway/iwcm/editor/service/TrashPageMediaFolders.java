package sk.iway.iwcm.editor.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.rest.GroupSchedulerDtoRepository;

/** Resolves upload locations without changing the page or restoring any part of the cached folder tree. */
class TrashPageMediaFolders {
    record Folder(int groupId, String fullPath, String urlPath, String domain) {}

    private final DocHistoryRepository pageHistory;
    private final GroupSchedulerDtoRepository groupHistory;
    private final Map<Integer, Optional<Folder>> folders = new HashMap<>();
    private Set<Integer> trashRoots;
    private Map<Integer, Integer> previousGroups;
    private Map<Integer, Integer> previousParents;

    TrashPageMediaFolders(DocHistoryRepository pageHistory, GroupSchedulerDtoRepository groupHistory) {
        this.pageHistory = pageHistory;
        this.groupHistory = groupHistory;
    }

    private Set<Integer> trashRoots() {
        if (trashRoots == null) {
            trashRoots = new HashSet<>();
            for (GroupDetails root : GroupsDB.getInstance().getTrashGroupsAllDomains()) trashRoots.add(root.getGroupId());
        }
        return trashRoots;
    }

    boolean isInTrash(int groupId) {
        GroupsDB groups = GroupsDB.getInstance();
        return groups.isInTrash(groupId) && groups.getParentGroups(groupId, true).stream()
            .anyMatch(group -> trashRoots().contains(group.getGroupId()));
    }

    Folder forPage(DocDetails page) {
        int groupId = page.getGroupId();
        if (trashRoots().contains(groupId)) {
            if (previousGroups == null) {
                previousGroups = new HashMap<>();
                for (var row : pageHistory.findGroupsBeforeTrash(trashRoots())) previousGroups.put(row.getDocId(), row.getGroupId());
            }
            groupId = previousGroups.getOrDefault(page.getDocId(), -1);
        }
        return folders.computeIfAbsent(groupId, this::resolveFolder).orElse(null);
    }

    private int previousParent(int groupId) {
        if (previousParents == null) {
            previousParents = new HashMap<>();
            List<Integer> moved = GroupsDB.getInstance().getTopLevelGroupsInTrash().stream().map(GroupDetails::getGroupId).toList();
            // Bound IN lists for installations with thousands of deleted folders.
            for (int start = 0; start < moved.size(); start += 500) {
                var ids = moved.subList(start, Math.min(start + 500, moved.size()));
                for (var row : groupHistory.findParentsForRecovery(ids)) {
                    previousParents.putIfAbsent(row.getGroupId(), row.getParentGroupId() == null ? -1 : row.getParentGroupId());
                }
            }
        }
        return previousParents.getOrDefault(groupId, -1);
    }

    private Optional<Folder> resolveFolder(int groupId) {
        if (groupId < 1) return Optional.empty();
        GroupsDB groups = GroupsDB.getInstance();
        GroupDetails source = groups.getGroup(groupId);
        if (source == null) return Optional.empty();
        if (!isInTrash(groupId)) {
            return Optional.of(new Folder(groupId, source.getFullPath(), groups.getURLPath(groupId), Tools.getStringValue(groups.getDomain(groupId), "")));
        }

        // A page deleted with its folder retains its group ID. Reconnect the moved ancestor using folder recovery history.
        Map<Integer, GroupDetails> ancestors = new HashMap<>();
        StringBuilder fullPath = new StringBuilder();
        GroupDetails group = source;
        while (group != null && !ancestors.containsKey(group.getGroupId()) && !trashRoots().contains(group.getGroupId())) {
            fullPath.insert(0, "/" + group.getGroupName());
            int parent = group.getParentGroupId();
            if (trashRoots().contains(parent)) {
                parent = previousParent(group.getGroupId());
                if (parent < 0) return Optional.empty();
                GroupDetails original = new GroupDetails();
                original.setGroupId(group.getGroupId());
                original.setUrlDirName(group.getUrlDirName());
                original.setDomainName(group.getDomainName());
                original.setParentGroupId(parent);
                group = original;
            }
            ancestors.put(group.getGroupId(), group);
            if (parent == 0) {
                String[] location = GroupsDB.getURLPathDomainGroup(ancestors, groupId);
                String domain = InitServlet.isTypeCloud() ? source.getDomainName() : location[1];
                return Optional.of(new Folder(groupId, fullPath.toString(), location[0], Tools.getStringValue(domain, "")));
            }
            group = groups.getGroup(parent);
        }
        return Optional.empty();
    }
}
