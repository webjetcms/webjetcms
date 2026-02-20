package sk.iway.iwcm.editor.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.rest.GroupSchedulerDto;
import sk.iway.iwcm.editor.rest.GroupSchedulerDtoRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.NotifyBean;

/**
 * service class for groups operations
 */
@Service
@RequestScope
public class GroupsService extends NotifyService {

    private GroupSchedulerDtoRepository groupSchedulerDtoRepository;
    private Prop prop;

    @Autowired
    public GroupsService(GroupSchedulerDtoRepository groupSchedulerDtoRepository, HttpServletRequest request) {
        this.groupSchedulerDtoRepository = groupSchedulerDtoRepository;
        this.prop = Prop.getInstance(request);
    }

	/**
	 * Recover group from trash.
	 * It will try to find last parent group from history and use it as recover location.
	 * If no history is found, group will be recovered to root.
	 * @param group
	 * @param currentUser
	 * @return
	 */
    public boolean recoverGroupFromTrash(GroupDetails group, Identity currentUser) {

		if (isInTrash(group) == false) return false; //Group is not in trash

		//Get group by id + chec perms
		GroupsDB groupsDB = GroupsDB.getInstance();

		//Check user perms for dest group
		if(GroupsDB.isGroupEditable(currentUser, group.getGroupId())==false) {
			NotifyBean info = new NotifyBean(prop.getText("editor.recover.notify_title.failed_folder"), prop.getText("editor.recover.notify.no_right"), NotifyBean.NotifyType.WARNING, 60000);
			addNotify(info);
			return false;
		}

		int parentGroupId = 0;
		String parentGroupPath = prop.getText("stat_settings.group_id");
		//get last parent_group_id value from history (groups_scheduler table)
		GroupSchedulerDto latestGroupHistory = groupSchedulerDtoRepository.findFirstByGroupIdAndWhenToPublishNullOrderBySaveDateDesc(Long.valueOf(group.getGroupId()));

		//check if parent exists and NOT in trash
		if (latestGroupHistory != null) {
			GroupDetails parentGroup = groupsDB.getGroup(latestGroupHistory.getParentGroupId());
			if (parentGroup != null && isInTrash(parentGroup) == false) {
				parentGroupId = latestGroupHistory.getParentGroupId();
				parentGroupPath = parentGroup.getFullPath();
			}
		}

		//Set folder derent to root
		group.setParentGroupId( parentGroupId );
		(new SimpleQuery()).execute("UPDATE groups SET parent_group_id=? WHERE group_id=?", parentGroupId, group.getGroupId());

		NotifyBean info = new NotifyBean(prop.getText("editor.recover.notify_title.success_folder"), prop.getText("editor.recover.notify_body.success_group", group.getGroupName(), parentGroupPath), NotifyBean.NotifyType.SUCCESS, 60000);
		addNotify(info);

		//Update sub groups
		StringBuilder groups = new StringBuilder();
		List<GroupDetails> subGroups = groupsDB.getGroupsTree(group.getGroupId(), true, true);
		for (GroupDetails g : subGroups) {
			if (groups.length() > 0) groups.append(',').append(g.getGroupId());
			else groups.append(g.getGroupId());
		}

		//Repo
		DocHistoryRepository docHistoryRepository = Tools.getSpringBean("docHistoryRepository", DocHistoryRepository.class);

		//List of doc id's that available should be updated (depend on history)
		List<Integer> docIdsToChange = (new SimpleQuery()).forListInteger("SELECT doc_id FROM documents WHERE group_id IN (" + groups.toString() + ")");

		//History records
		List<DocHistory> historyRecords = docHistoryRepository.findByDocIdInActual(docIdsToChange);

		List<Integer> availableTrue = new ArrayList<>(); //Update availale to true
		List<Integer> availableFalse = new ArrayList<>(); //Update availale to false
		List<Integer> notFound = new ArrayList<>(); //No history found

		//Loops docIdsToChange and match them with historyRecords
		boolean wasFound = false;
		for(Integer docId : docIdsToChange) {
			wasFound = false;
			for(DocHistory history : historyRecords) {
				if(history.getDocId() == docId) {
					wasFound = true;
					if(history.isAvailable()) availableTrue.add(docId);
					else availableFalse.add(docId);
					break;
				}
			}
			if(!wasFound) notFound.add(docId);
		}

		//If history was not found, check last history with ACTUAL=false (maybe only actual is missing)
		for(Integer notFoundId : notFound) {
			Optional<DocHistory> history = docHistoryRepository.findTopByDocIdOrderBySaveDateDesc(notFoundId);
			boolean available = history.isPresent() ? history.get().isAvailable() : true; //If history record is still not found, set available to true
			if(available) availableTrue.add(notFoundId);
			else availableFalse.add(notFoundId);
		}

		//Updatw doc's available status
		if (availableTrue.isEmpty()==false) (new SimpleQuery()).execute("UPDATE documents SET available="+DB.getBooleanSql(true)+", sync_status=1 WHERE doc_id IN (" + StringUtils.join(availableTrue, ",") + ")");
		if (availableFalse.isEmpty()==false) (new SimpleQuery()).execute("UPDATE documents SET available="+DB.getBooleanSql(false)+", sync_status=1 WHERE doc_id IN (" + StringUtils.join(availableFalse, ",") + ")");

		//aktualizuj FT stplce
		DocDB.updateFileNameField(group.getGroupId());

		//Refresh
		DocDB.getInstance(true);
		GroupsDB.getInstance(true);

        return true;
	}

	/**
	 * Check if group is in trash
	 * @param group
	 * @return
	 */
    public static boolean isInTrash(GroupDetails group) {
        GroupDetails trashGroupDetails = getTrashGroupDetails();
        if (group.getFullPath().startsWith(trashGroupDetails.getFullPath())) return true;
        return false;
    }

	/**
     * Vrati adresar Kos
     * @return
     */
    public static GroupDetails getTrashGroupDetails() {
        GroupsDB groupsDB = GroupsDB.getInstance();
        return groupsDB.getTrashGroup();
    }

    /**
     * Vrati adresar System (lokalny)
     * @return
     */
    public static GroupDetails getSystemGroupDetails() {
        GroupsDB groupsDB = GroupsDB.getInstance();
        GroupDetails system = groupsDB.getLocalSystemGroup();

        //ak sa nenaslo, pouzi globalny
        if (system == null) {
            system = groupsDB.getGroupByPath("/System");
        }

        return system;
    }

	/**
	 * Sort list of groups into tree structure (deeper one last)
	 * @param groups
	 * @return
	 */
	public static List<GroupDetails> sortItIntoTree(List<GroupDetails> groups) {
        Collections.sort(groups, (s1, s2) -> { return s1.getFullPath().split("/").length - s2.getFullPath().split("/").length; });

        List<GroupDetails> sorted = new ArrayList<>();

        for(GroupDetails group : groups) {
            int parentIndex = -1;
            for(int i = 0; i < sorted.size(); i++) {
                if(group.getParentGroupId() == sorted.get(i).getGroupId()) {
                    parentIndex = i;
                    break;
                }
            }
            if(parentIndex == -1) sorted.add(0, group);
            else sorted.add(parentIndex + 1, group);
        }

        return sorted;
    }

	/**
	 * Check if title is syncable between group and webpage
	 * @param docId
	 * @param groupId
	 * @return
	 */
	public static boolean canSyncTitle(Integer docId, Integer groupId) {

		if(Constants.getBoolean("syncGroupAndWebpageTitle")==false) return false;

		if(docId == null || docId.intValue() < 1) return true;

		//Is DOC, main DOC for SEVERAL groups ?
		int defaultDocCount = (new SimpleQuery()).forInt("SELECT COUNT(group_id) FROM groups WHERE default_doc_id = ?", docId);
		if(defaultDocCount > 1) return false;

		//
		GroupDetails group = GroupsDB.getInstance().getGroup(groupId.intValue());
		if(group.getDefaultDocId() != docId) return false;

		return true;
	}

	/**
	 * Check if title is syncable between group and webpages
	 * Used before save of groupDetails
	 * @param toSaveGroup
	 * @return
	 */
	public static boolean canSyncTitle(GroupDetails toSaveGroup) {

		if(Constants.getBoolean("syncGroupAndWebpageTitle")==false) return false;

		if(toSaveGroup == null) return false;

		//Is DOC, main DOC for SEVERAL groups ?
		int defaultDocCount = (new SimpleQuery()).forInt("SELECT COUNT(group_id) FROM groups WHERE default_doc_id = ?", toSaveGroup.getDefaultDocId());
		if(defaultDocCount > 1) return false;

		//Is DOC in another group that current changed group ?
		DocDetails docDetails = DocDB.getInstance().getDoc(toSaveGroup.getDefaultDocId());
		if(docDetails.getGroupId() != toSaveGroup.getGroupId()) return false;

		return true;
	}
}
