package sk.iway.iwcm.editor.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;

/**
 * REST pre vratenie zoznamu stranok na schvalenie aktualne prihlasenym pouzivatelom
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/webpages/toapprove")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
public class WebApproveRestController extends DatatableRestControllerV2<DocHistory, Long>{

    private final DocHistoryRepository docHistoryRepository;

    @Autowired
    public WebApproveRestController(DocHistoryRepository docHistoryRepository) {
        super(docHistoryRepository);
        this.docHistoryRepository = docHistoryRepository;
    }

    @Override
    public Page<DocHistory> getAllItems(Pageable pageable) {

        Page<DocHistory> historyList = docHistoryRepository.findAll(getToApproveConditions(getUser().getUserId()), pageable);

        processFromEntity(historyList, ProcessItemAction.GETALL);

        DatatablePageImpl<DocHistory> pageImpl = new DatatablePageImpl<>(historyList);

        WebpagesService ws = new WebpagesService(-1, getUser(), getProp(), getRequest());

        pageImpl.addOptions("tempId", ws.getTemplates(false), "tempName", "tempId", true);
        pageImpl.addOptions("menuDocId,rightMenuDocId", ws.getMenuList(true), "title", "docId", false);
        pageImpl.addOptions("headerDocId,footerDocId", ws.getHeaderList(true), "title", "docId", false);
        pageImpl.addOptions("editorFields.emails", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_EMAIL), "userGroupName", "userGroupId", false);
        pageImpl.addOptions("editorFields.permisions", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS), "userGroupName", "userGroupId", false);
        pageImpl.addOptions("perexGroups", ws.getPerexGroups(false), "perexGroupName", "perexGroupId", false);

        return pageImpl;
    }

    private static List<Predicate> getPredicates(int userId, Root<DocHistory> root, CriteriaBuilder builder) {
        final List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.isNotNull(root.get("awaitingApprove")));
        predicates.add(builder.like(root.get("awaitingApprove"), "%," + userId + ",%"));

        //TODO: pridat do history tabulky root_group_l1 atd ako je aj v documents tabulke a testovat to podla toho
        int domainId = CloudToolsForCore.getDomainId();
        if (domainId>0) {
            //ziskaj zoznam vsetkych groupId v tejto domene
            GroupsDB groupsDB = GroupsDB.getInstance();
            GroupDetails group = groupsDB.getGroup(domainId);
            if (group != null) {
                String domainName = group.getDomainName();
                if (Tools.isNotEmpty(domainName)) {
                    List<Integer> awaitingGroupIds = (new SimpleQuery()).forListInteger("SELECT DISTINCT group_id FROM documents_history WHERE awaiting_approve LIKE ?", "%," + userId + ",%");

                    List<Integer> groupIdsInDomain = new ArrayList<>();

                    //check if group is in domain
                    for (Integer groupId : awaitingGroupIds) {
                        GroupDetails groupDetails = groupsDB.getGroup(groupId);
                        if (groupDetails != null) {
                            if (groupDetails.getDomainName().equals(domainName)) {
                                groupIdsInDomain.add(groupId);
                            }
                        }
                    }

                    if (groupIdsInDomain.size()>0) {
                        predicates.add(root.get("groupId").in(groupIdsInDomain));
                    }
                }
            }
        }

        return predicates;
    }

    public static Specification<DocHistory> getToApproveConditions(int userId) {
		return (Specification<DocHistory>) (root, query, builder) -> {
			final List<Predicate> predicates = getPredicates(userId, root, builder);

			return builder.and(predicates.toArray(new Predicate[predicates.size()]));
		};
	}

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<DocHistory> root, CriteriaBuilder builder) {
        predicates.addAll(getPredicates(getUser().getUserId(), root, builder));
    }

    @Override
    public DocHistory processFromEntity(DocHistory entity, ProcessItemAction action) {
        //otoc nastavenie docId a historyId, lebo tak to pozaduje DT
        int docId = entity.getDocId();
        entity.setDocId(entity.getHistoryId());
        entity.setHistoryId(docId);

        WebpagesService.processFromEntity(entity, ProcessItemAction.GETALL, getRequest());

        return entity;
    }


}
