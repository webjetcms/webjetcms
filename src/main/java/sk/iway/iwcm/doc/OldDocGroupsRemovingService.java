package sk.iway.iwcm.doc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sk.iway.iwcm.components.gdpr.GdprDataDeleting.GdprDataDeletingType;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;

public class OldDocGroupsRemovingService {

    public enum ActionType {
        DOCS,
        GROUPS,
        ALL
    }

    /* =============== DELETE ACTION - PUBLIC =============== */

    public static void deleteOldDocAndGroups() {
        Date[] createdRange = getDefaultCreatedRange();

        deleteOldDocAndGroups(createdRange[0], createdRange[1], ActionType.ALL);
    }

    public static void deleteOldDocAndGroups(Date createdFrom, Date createdTo, ActionType actionType) {
        if (isInvalidDateRange(createdFrom, createdTo)) {
            return;
        }

        ActionType resolvedActionType = resolveActionType(actionType);

        if (shouldProcessOldGroups(resolvedActionType)) {
            // Remove all old groups from trash
            removeOldTrashGroups(createdFrom, createdTo);
        }

        if (shouldProcessOldDocs(resolvedActionType)) {
            // Old groups are gone - now remove docs old enough in trash
            removeOldTrashDocs(createdFrom, createdTo);
        }

        if (shouldProcessEmptyGroups(resolvedActionType)) {
            // Remove empty folders
            removeEmptyTrashGroups();
        }
    }

    /* =============== COUNT ACTION - PUBLIC =============== */

    public static int getCountOfDocAndGroups() {
        Date[] createdRange = getDefaultCreatedRange();

        return getCountOfDocAndGroups(createdRange[0], createdRange[1], ActionType.ALL);
    }

    public static int getCountOfDocAndGroups(Date createdFrom, Date createdTo, ActionType actionType) {
        if (isInvalidDateRange(createdFrom, createdTo)) {
            return 0;
        }

        ActionType resolvedActionType = resolveActionType(actionType);

        // Simulate the deletion so groups/docs removed across phases are counted only once
        Set<Integer> groupIdsToDelete = new HashSet<>();
        Set<Integer> docIdsToDelete = new HashSet<>();

        if (shouldProcessOldGroups(resolvedActionType)) {
            // Phase 1: old top-level trash groups incl. all their sub-groups and docs
            countOldTrashGroups(createdFrom, createdTo, groupIdsToDelete, docIdsToDelete);
        }

        if (shouldProcessOldDocs(resolvedActionType)) {
            // Phase 2: docs old enough in the remaining trash groups
            countOldTrashDocs(createdFrom, createdTo, groupIdsToDelete, docIdsToDelete);
        }

        if (shouldProcessEmptyGroups(resolvedActionType)) {
            // Phase 3: trash groups that become empty after phases 1 and 2
            countEmptyTrashGroups(groupIdsToDelete, docIdsToDelete);
        }

        return groupIdsToDelete.size() + docIdsToDelete.size();
    }

    /* =============== COUNT ACTION - PRIVATE =============== */

    private static void countOldTrashGroups(Date createdFrom, Date createdTo, Set<Integer> groupIdsToDelete, Set<Integer> docIdsToDelete) {
        List<GroupDetails> topLevelGroupsInTrash = GroupsDB.getInstance().getTopLevelGroupsInTrash();
        Set<Integer> oldEnough = new HashSet<>(getOldGroupIds(createdFrom, createdTo));

        for (GroupDetails groupInTrash : topLevelGroupsInTrash) {
            if (oldEnough.contains(groupInTrash.getGroupId())) {
                // Count the group + all sub-groups and all docs inside (whole subtree is removed)
                List<GroupDetails> subtree = GroupsDB.getInstance().getGroupsTree(groupInTrash.getGroupId(), true, true);
                for (GroupDetails group : subtree) {
                    groupIdsToDelete.add(group.getGroupId());
                    List<DocDetails> docs = DocDB.getInstance().getDocByGroup(group.getGroupId(), DocDB.ORDER_ID, true, -1, -1, true, false);
                    for (DocDetails doc : docs) {
                        docIdsToDelete.add(doc.getDocId());
                    }
                }
            }
        }
    }

    private static void countOldTrashDocs(Date createdFrom, Date createdTo, Set<Integer> groupIdsToDelete, Set<Integer> docIdsToDelete) {
        long createdFromTime = createdFrom.getTime();
        long createdToTime = createdTo.getTime();

        List<GroupDetails> trashGroups = GroupsDB.getInstance().getTrashGroupsAllDomains();
        for (GroupDetails trashGroup : trashGroups) {
            List<GroupDetails> allGroupsInTrash = GroupsDB.getInstance().getGroupsTree(trashGroup.getGroupId(), true, true);
            for (GroupDetails group : allGroupsInTrash) {
                // Skip groups already removed in phase 1
                if (groupIdsToDelete.contains(group.getGroupId())) continue;
                List<DocDetails> docs = DocDB.getInstance().getDocByGroup(group.getGroupId(), DocDB.ORDER_ID, true, -1, -1, true, false);
                for (DocDetails doc : docs) {
                    if (isDocInCreatedRange(doc, createdFromTime, createdToTime)) {
                        docIdsToDelete.add(doc.getDocId());
                    }
                }
            }
        }
    }

    private static void countEmptyTrashGroups(Set<Integer> groupIdsToDelete, Set<Integer> docIdsToDelete) {
        List<GroupDetails> trashGroups = GroupsDB.getInstance().getTrashGroupsAllDomains();
        for (GroupDetails trashGroup : trashGroups) {
            // Get all subfolders recursively (excluding the trash root itself)
            List<GroupDetails> allGroupsInTrash = GroupsDB.getInstance().getGroupsTree(trashGroup.getGroupId(), false, true);
            // Process bottom-up (reverse) so leaves are counted before their parents
            for (int i = allGroupsInTrash.size() - 1; i >= 0; i--) {
                GroupDetails group = allGroupsInTrash.get(i);
                if (groupIdsToDelete.contains(group.getGroupId())) continue;
                List<DocDetails> docs = DocDB.getInstance().getDocByGroup(group.getGroupId(), DocDB.ORDER_ID, true, -1, -1, true, false);
                // Group is empty if all its docs are already scheduled for deletion
                boolean hasRemainingDocs = false;
                for (DocDetails doc : docs) {
                    if (!docIdsToDelete.contains(doc.getDocId())) {
                        hasRemainingDocs = true;
                        break;
                    }
                }
                if (!hasRemainingDocs) {
                    // Re-check children considering earlier deletions in this loop
                    List<GroupDetails> children = GroupsDB.getInstance().getGroupsTree(group.getGroupId(), false, true);
                    boolean hasRemainingChildren = false;
                    for (GroupDetails child : children) {
                        if (!groupIdsToDelete.contains(child.getGroupId())) {
                            hasRemainingChildren = true;
                            break;
                        }
                    }
                    if (!hasRemainingChildren) {
                        groupIdsToDelete.add(group.getGroupId());
                    }
                }
            }
        }
    }

    /* =============== DELETE ACTION - PRIVATE =============== */

    private static void removeEmptyTrashGroups() {
        List<GroupDetails> trashGroups = GroupsDB.getInstance().getTrashGroupsAllDomains();
        for (GroupDetails trashGroup : trashGroups) {
            // Get all subfolders recursively (excluding the trash root itself)
            List<GroupDetails> allGroupsInTrash = GroupsDB.getInstance().getGroupsTree(trashGroup.getGroupId(), false, true);
            // Process bottom-up (reverse) so leaves are deleted before their parents
            for (int i = allGroupsInTrash.size() - 1; i >= 0; i--) {
                GroupDetails group = allGroupsInTrash.get(i);
                List<DocDetails> docs = DocDB.getInstance().getDocByGroup(group.getGroupId(), DocDB.ORDER_ID, true, -1, -1, true, false);
                if (docs.isEmpty()) {
                    // Re-check children after earlier deletions in this loop
                    List<GroupDetails> remainingChildren = GroupsDB.getInstance().getGroupsTree(group.getGroupId(), false, true);
                    if (remainingChildren.isEmpty()) {
                        // Remove group + sub-groups
                        // the param withHistory TRUE also secures removing of bound groups_scheduler
                        GroupsDB.deleteGroup(group.getGroupId(), false, true, true, true);
                    }
                }
            }
        }
    }

    private static void removeOldTrashDocs(Date createdFrom, Date createdTo) {
        long createdFromTime = createdFrom.getTime();
        long createdToTime = createdTo.getTime();

        List<GroupDetails> trashGroups = GroupsDB.getInstance().getTrashGroupsAllDomains();
        for (GroupDetails trashGroup : trashGroups) {
            // Get the trash group itself + all subfolders recursively
            List<GroupDetails> allGroupsInTrash = GroupsDB.getInstance().getGroupsTree(trashGroup.getGroupId(), true, true);
            for (GroupDetails group : allGroupsInTrash) {
                // Get all docs in this group (including unavailable), without data column
                List<DocDetails> docs = DocDB.getInstance().getDocByGroup(group.getGroupId(), DocDB.ORDER_ID, true, -1, -1, true, false);
                for (DocDetails doc : docs) {
                    if (isDocInCreatedRange(doc, createdFromTime, createdToTime)) {
                        // Remove doc permanently
                        // the param withHistory TRUE also secures removing of bound documents_history
                        DocDB.deleteDoc(doc.getDocId(), null, true, true);
                    }
                }
            }
        }
    }

    private static void removeOldTrashGroups(Date createdFrom, Date createdTo) {
        List<GroupDetails> topLevelGroupsInTrash = GroupsDB.getInstance().getTopLevelGroupsInTrash();
        Set<Integer> oldEnough = new HashSet<>(getOldGroupIds(createdFrom, createdTo));

        List<Integer> oldGroupsToRemove = new ArrayList<>();
        for (GroupDetails groupInTrash : topLevelGroupsInTrash) {
            if (oldEnough.contains(groupInTrash.getGroupId())) {
                oldGroupsToRemove.add(groupInTrash.getGroupId());
            }
        }

        // Remove groups that are old enough (it will also remove all the subgroups and docs inside)
        for(int groupId : oldGroupsToRemove) {
            // Remove group + sub-groups and all docs inside
            // the param withHistory TRUE also secures removing of bound documents_history and groups_scheduler
            GroupsDB.deleteGroup(groupId, false, true, true, true);
        }
    }

    private static List<Integer> getOldGroupIds(Date createdFrom, Date createdTo) {
        List<Integer> groupsOlderThanDate = new ArrayList<>();

        new ComplexQuery()
            .setSql("SELECT group_id, MIN(schedule_id) as top_id FROM groups_scheduler WHERE awaiting_approve IS NULL AND disapproved_by IS NULL AND save_date >= ? AND save_date <= ? GROUP BY group_id")
            .setParams(createdFrom, createdTo)
            .list(new Mapper<Object>() {
                @Override
                public Object map(java.sql.ResultSet rs) throws java.sql.SQLException {
                    groupsOlderThanDate.add(rs.getInt("group_id"));
                    return null;
                }
            });

        return groupsOlderThanDate;
    }

    private static Date[] getDefaultCreatedRange() {
        Calendar cal = Calendar.getInstance();
        // Threshold: items older than this date should be removed
        cal.add(Calendar.DAY_OF_YEAR, -GdprDataDeletingType.OLD_DOC_AND_GROUPS.getAfterConstantInt());
        Date createdTo = cal.getTime();
        // Lower bound: epoch as a safe minimum
        Date createdFrom = new Date(0);

        return new Date[] { createdFrom, createdTo };
    }

    private static ActionType resolveActionType(ActionType actionType) {
        return actionType != null ? actionType : ActionType.ALL;
    }

    private static boolean shouldProcessOldGroups(ActionType actionType) {
        return actionType == ActionType.ALL || actionType == ActionType.GROUPS;
    }

    private static boolean shouldProcessOldDocs(ActionType actionType) {
        return actionType == ActionType.ALL || actionType == ActionType.DOCS;
    }

    private static boolean shouldProcessEmptyGroups(ActionType actionType) {
        return actionType == ActionType.ALL || actionType == ActionType.GROUPS;
    }

    private static boolean isInvalidDateRange(Date createdFrom, Date createdTo) {
        return createdFrom == null || createdTo == null || createdFrom.after(createdTo);
    }

    private static boolean isDocInCreatedRange(DocDetails doc, long createdFromTime, long createdToTime) {
        long dateCreated = doc.getDateCreated();
        return dateCreated > 0 && dateCreated >= createdFromTime && dateCreated <= createdToTime;
    }

}
