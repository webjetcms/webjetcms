package sk.iway.iwcm.editor.approve;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.GroupsTreeService;
import sk.iway.iwcm.editor.rest.GroupSchedulerDto;
import sk.iway.iwcm.editor.rest.GroupSchedulerDtoMapper;
import sk.iway.iwcm.editor.rest.GroupSchedulerDtoRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/**
 * REST pre vratenie zoznamu stranok na schvalenie aktualne prihlasenym pouzivatelom
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/groups/toapprove")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
public class GroupsApproveRestController extends DatatableRestControllerV2<GroupDetails, Long> {

    private final GroupSchedulerDtoRepository repository;

    @Autowired
    public GroupsApproveRestController(GroupSchedulerDtoRepository repository) {
        super(null);
        this.repository = repository;
    }

    @Override
    public Page<GroupDetails> getAllItems(Pageable pageable) {
        Page<GroupSchedulerDto> historyList = new DatatablePageImpl<>(repository.findAll(getToApproveConditions(getUser().getUserId())));

        List<GroupDetails> groupDetailsList = new ArrayList<>();
        groupDetailsList = GroupSchedulerDtoMapper.INSTANCE.groupSchedulerDtosToGroupDetailsList(historyList.getContent());

        return new DatatablePageImpl<>(groupDetailsList);
    }

    public static int countGroupsToApprove(int userId, GroupSchedulerDtoRepository repository) {
        return (int) repository.count(getToApproveConditions(userId));
    }

    public static Specification<GroupSchedulerDto> getToApproveConditions(int userId) {
		return (Specification<GroupSchedulerDto>) (root, query, builder) -> {
			final List<Predicate> predicates = getPredicates(userId, root, builder);
			return builder.and(predicates.toArray(new Predicate[predicates.size()]));
		};
	}

    private static List<Predicate> getPredicates(int userId, Root<GroupSchedulerDto> root, CriteriaBuilder builder) {
        final List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.isNotNull(root.get("awaitingApprove")));
        predicates.add(builder.like(root.get("awaitingApprove"), "%," + userId + ",%"));

        GroupsDB groupsDB = GroupsDB.getInstance();
        String trashPath = GroupsTreeService.getTrashDirPath();
        Set<Integer> awaitingGroupIds = new HashSet<>();

        ComplexQuery cq = new ComplexQuery();
        cq.setSql("SELECT group_id FROM groups_scheduler WHERE awaiting_approve LIKE ?");
        cq.setParams("%," + userId + ",%");
        cq.list(new sk.iway.iwcm.database.Mapper<GroupSchedulerDto>()
		{
			public GroupSchedulerDto map(ResultSet rs) throws SQLException
			{
                int groupId = rs.getInt("group_id");
                awaitingGroupIds.add(groupId);
                return null;
            }
        });

        int domainId = CloudToolsForCore.getDomainId();
        if (domainId>0) {
            GroupDetails group = groupsDB.getGroup(domainId);
            if (group != null) {
                String domainName = group.getDomainName();
                if (Tools.isNotEmpty(domainName)) {
                    List<Integer> groupIdsInDomain = new ArrayList<>();

                    for (Integer groupId : awaitingGroupIds) {
                        GroupDetails groupDetails = groupsDB.getGroup(groupId);
                        if (groupDetails != null) {
                            if (groupDetails.getDomainName().equals(domainName)) {
                                groupIdsInDomain.add(groupId);
                            }
                        }
                    }

                    //we must add something non existent otherwise it will select all data
                    if (groupIdsInDomain.isEmpty()) groupIdsInDomain.add(-1);

                    if (groupIdsInDomain.size()>0) {
                        predicates.add(root.get("groupId").in(groupIdsInDomain));
                    }
                }
            }
        }

        //remove groups in trash
        List<Integer> groupIdsInTrash = new ArrayList<>();

        for (Integer groupId : awaitingGroupIds) {
            GroupDetails groupDetails = groupsDB.getGroup(groupId);
            if (groupDetails != null) {
                if (groupDetails.getFullPath().contains(trashPath)) {
                    groupIdsInTrash.add(groupId);
                }
            }
        }
        if (groupIdsInTrash.isEmpty()==false) {
            predicates.add(builder.not(root.get("groupId").in(groupIdsInTrash)));
        }

        return predicates;
    }
}
