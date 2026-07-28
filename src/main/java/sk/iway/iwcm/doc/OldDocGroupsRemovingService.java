package sk.iway.iwcm.doc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sk.iway.iwcm.components.gdpr.GdprDataDeleting.GdprDataDeletingType;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;

public class OldDocGroupsRemovingService {

    private OldDocGroupsRemovingService() {
        // utility class, only static methods
    }

    public enum ActionType {
        DOCS,
        GROUPS,
        ALL
    }

    /**
     * Deletion plan computed once from the current state of the page/group caches.
     * The plan is used both for counting an individual action and for the real deletion.
     */
    private static class DeletionPlan {
        // rule 1 - roots of old-enough trash sub-trees (the whole sub-tree, including the root, is removed)
        final List<Integer> oldGroupRoots = new ArrayList<>();
        // rule 1 - every group inside the old-enough sub-trees (root + all descendants)
        final Set<Integer> subtreeGroupIds = new HashSet<>();
        // rule 1 - every doc inside the old-enough sub-trees
        final Set<Integer> subtreeDocIds = new HashSet<>();
        // rule 2 - docs old enough in the remaining trash groups (never overlaps subtreeDocIds)
        final Set<Integer> oldDocIds = new HashSet<>();
        // rule 3 - trash groups that become empty after rules 1 and 2, ordered bottom-up (children before parents)
        final List<Integer> emptyGroupIds = new ArrayList<>();

        int getTotalCount() {
            return subtreeGroupIds.size() + emptyGroupIds.size() + subtreeDocIds.size() + oldDocIds.size();
        }
    }

    /* =============== PUBLIC API =============== */

    public static void deleteOldDocAndGroups() {
        Date[] createdRange = getDefaultCreatedRange();

        deleteOldDocAndGroups(createdRange[0], createdRange[1], ActionType.ALL);
    }

    public static void deleteOldDocAndGroups(Date createdFrom, Date createdTo, ActionType actionType) {
        if (isInvalidDateRange(createdFrom, createdTo)) {
            return;
        }

        DeletionPlan plan = computePlan(createdFrom, createdTo, resolveActionType(actionType));
        executePlan(plan);
    }

    public static int getCountOfDocAndGroups() {
        Date[] createdRange = getDefaultCreatedRange();

        return getCountOfDocAndGroups(createdRange[0], createdRange[1], ActionType.ALL);
    }

    public static int getCountOfDocAndGroups(Date createdFrom, Date createdTo, ActionType actionType) {
        if (isInvalidDateRange(createdFrom, createdTo)) {
            return 0;
        }

        ActionType resolvedActionType = resolveActionType(actionType);
        if (resolvedActionType == ActionType.ALL) {
            return getCountOfDocAndGroups(createdFrom, createdTo, ActionType.DOCS)
                + getCountOfDocAndGroups(createdFrom, createdTo, ActionType.GROUPS);
        }

        return computePlan(createdFrom, createdTo, resolvedActionType).getTotalCount();
    }

    /* =============== PLAN COMPUTATION =============== */

    /**
     * Builds the deletion plan by simulating the deletion across all phases, so groups and docs
     * removed by more than one phase are accounted for only once.
     */
    private static DeletionPlan computePlan(Date createdFrom, Date createdTo, ActionType actionType) {
        DeletionPlan plan = new DeletionPlan();
        // per-computation cache of loaded docs, avoids querying the same group multiple times across phases
        Map<Integer, List<DocDetails>> docsCache = new HashMap<>();

        if (shouldProcessOldGroups(actionType)) {
            // Phase 1: old top-level trash groups incl. all their sub-groups and docs
            planOldTrashGroups(createdFrom, createdTo, plan, docsCache);
        }

        if (shouldProcessOldDocs(actionType)) {
            // Phase 2: docs old enough in the remaining trash groups
            planOldTrashDocs(createdFrom, createdTo, plan, docsCache);
        }

        if (shouldProcessEmptyGroups(actionType)) {
            // Phase 3: trash groups that become empty after phases 1 and 2
            planEmptyTrashGroups(plan, docsCache);
        }

        return plan;
    }

    private static void planOldTrashGroups(Date createdFrom, Date createdTo, DeletionPlan plan, Map<Integer, List<DocDetails>> docsCache) {
        List<GroupDetails> topLevelGroupsInTrash = GroupsDB.getInstance().getTopLevelGroupsInTrash();
        Set<Integer> oldEnough = new HashSet<>(getOldGroupIds(createdFrom, createdTo));

        for (GroupDetails groupInTrash : topLevelGroupsInTrash) {
            if (oldEnough.contains(groupInTrash.getGroupId())) {
                plan.oldGroupRoots.add(groupInTrash.getGroupId());
                // The whole sub-tree (root + all descendants) and all docs inside are removed
                List<GroupDetails> subtree = GroupsDB.getInstance().getGroupsTree(groupInTrash.getGroupId(), true, true);
                for (GroupDetails group : subtree) {
                    plan.subtreeGroupIds.add(group.getGroupId());
                    for (DocDetails doc : loadDocs(group.getGroupId(), docsCache)) {
                        plan.subtreeDocIds.add(doc.getDocId());
                    }
                }
            }
        }
    }

    private static void planOldTrashDocs(Date createdFrom, Date createdTo, DeletionPlan plan, Map<Integer, List<DocDetails>> docsCache) {
        long createdFromTime = createdFrom.getTime();
        long createdToTime = createdTo.getTime();

        for (GroupDetails group : getAllTrashGroups()) {
            // Skip groups already removed as part of an old sub-tree in phase 1
            if (plan.subtreeGroupIds.contains(group.getGroupId())) continue;
            for (DocDetails doc : loadDocs(group.getGroupId(), docsCache)) {
                if (plan.subtreeDocIds.contains(doc.getDocId())) continue;
                if (isDocInCreatedRange(doc, createdFromTime, createdToTime)) {
                    plan.oldDocIds.add(doc.getDocId());
                }
            }
        }
    }

    private static void planEmptyTrashGroups(DeletionPlan plan, Map<Integer, List<DocDetails>> docsCache) {
        // Simulate the removals of phases 1 and 2 so a parent can also become empty once its children are gone
        Set<Integer> removedGroups = new HashSet<>(plan.subtreeGroupIds);
        Set<Integer> removedDocs = new HashSet<>(plan.subtreeDocIds);
        removedDocs.addAll(plan.oldDocIds);

        for (GroupDetails trashGroup : GroupsDB.getInstance().getTrashGroupsAllDomains()) {
            // Get all subfolders recursively (excluding the trash root itself)
            List<GroupDetails> allGroupsInTrash = GroupsDB.getInstance().getGroupsTree(trashGroup.getGroupId(), false, true);
            // Process bottom-up (reverse) so leaves are evaluated before their parents
            for (int i = allGroupsInTrash.size() - 1; i >= 0; i--) {
                GroupDetails group = allGroupsInTrash.get(i);
                if (removedGroups.contains(group.getGroupId())) continue;

                if (isGroupEmptyAfterRemoval(group.getGroupId(), removedGroups, removedDocs, docsCache)) {
                    plan.emptyGroupIds.add(group.getGroupId());
                    // Mark as removed so parent groups can also become empty
                    removedGroups.add(group.getGroupId());
                }
            }
        }
    }

    /**
     * A trash group is considered empty when all of its docs and all of its sub-groups are already
     * scheduled for removal (by phase 1 or 2, or by an earlier iteration of phase 3).
     */
    private static boolean isGroupEmptyAfterRemoval(int groupId, Set<Integer> removedGroups, Set<Integer> removedDocs, Map<Integer, List<DocDetails>> docsCache) {
        for (DocDetails doc : loadDocs(groupId, docsCache)) {
            if (!removedDocs.contains(doc.getDocId())) return false;
        }
        for (GroupDetails child : GroupsDB.getInstance().getGroupsTree(groupId, false, true)) {
            if (!removedGroups.contains(child.getGroupId())) return false;
        }
        return true;
    }

    /* =============== PLAN EXECUTION =============== */

    private static void executePlan(DeletionPlan plan) {
        // Phase 1: remove whole old-enough sub-trees (group + sub-groups and all docs inside)
        for (int rootGroupId : plan.oldGroupRoots) {
            // includeParent=true  -> the root group itself is removed as well (not only its sub-groups)
            // the param withHistory TRUE also secures removing of bound documents_history and groups_scheduler
            GroupsDB.deleteGroup(rootGroupId, true, true, true, true);
        }

        // Phase 2: remove docs old enough in the remaining trash groups
        for (int docId : plan.oldDocIds) {
            // the param withHistory TRUE also secures removing of bound documents_history
            DocDB.deleteDoc(docId, null, true, true);
        }

        // Phase 3: remove trash groups that became empty (bottom-up order kept from the plan)
        for (int groupId : plan.emptyGroupIds) {
            // includeParent=true -> the empty group itself is removed
            GroupsDB.deleteGroup(groupId, true, true, true, true);
        }
    }

    /* =============== HELPERS =============== */

    /**
     * Returns the trash root groups plus all groups nested inside them, across all domains.
     */
    private static List<GroupDetails> getAllTrashGroups() {
        List<GroupDetails> result = new ArrayList<>();
        for (GroupDetails trashGroup : GroupsDB.getInstance().getTrashGroupsAllDomains()) {
            // include the trash root itself and its whole sub-tree
            result.addAll(GroupsDB.getInstance().getGroupsTree(trashGroup.getGroupId(), true, true));
        }
        return result;
    }

    /**
     * Loads all docs of a group (including unavailable ones, without the data column) using a cache
     * so the same group is queried from the database only once per computation.
     */
    private static List<DocDetails> loadDocs(int groupId, Map<Integer, List<DocDetails>> docsCache) {
        return docsCache.computeIfAbsent(groupId,
            id -> DocDB.getInstance().getDocByGroup(id, DocDB.ORDER_ID, true, -1, -1, true, false));
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
        return new Date[] { cal.getTime(), new Date() };
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
