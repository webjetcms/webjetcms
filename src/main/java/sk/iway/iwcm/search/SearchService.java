package sk.iway.iwcm.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.GroupsTreeService;

/**
 * Search service to search term in ADMIN
 */
public class SearchService {

    private SearchService() {}

    /**
     * Search for term in all webpages
     * @param params search parameters; searchAllGroups=true searches all editable folders and pages in the current domain for docs
     * @param user user
     * @param predicates
     */
    public static void getWebPagesData(Map<String, String> params, Identity user, List<Predicate> predicates, CriteriaBuilder builder, Root<DocDetails> root) {
        String searchText = Tools.getStringValue(params.get("searchText"), null);
        String searchType = Tools.getStringValue(params.get("searchType"), "docs");
        boolean searchAllGroups = "docs".equals(searchType) && "true".equals(params.get("searchAllGroups"));
        int rootGroupId = Tools.getIntValue(params.get("searchRootGroupId"), -1);
        int rootGroupIdFinal = searchAllGroups ? -1 : GroupsTreeService.getDefaultGroupTreeOptionForUser(rootGroupId, user).getGroupId();
        fillPredicates(searchText, searchType, rootGroupIdFinal, searchAllGroups, user, predicates, root, builder);
    }

    public static List<DocDetails> searchTextAll(String searchText, String searchType, int rootGroupId, Identity user) {
        //Safety check - if user has no rights for root folder, set it to default
        int rootGroupIdFinal = GroupsTreeService.getDefaultGroupTreeOptionForUser(rootGroupId, user).getGroupId();

        @SuppressWarnings("java:S1602")
        Specification<DocDetails> spec = (Specification<DocDetails>) (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();
            fillPredicates(searchText, searchType, rootGroupIdFinal, false, user, predicates, root, builder);
            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };

        DocDetailsRepository repo = Tools.getSpringBean("docDetailsRepository", DocDetailsRepository.class);
        return repo.findAll(spec);
    }

    private static void fillPredicates(String searchText, String searchType, int rootGroupId, boolean searchAllGroups, Identity user, List<Predicate> predicates, Root<DocDetails> root, CriteriaBuilder builder) {

        if ("tatrabanka".equals(Constants.getInstallName())) {
            //fix na TB, ale inak rozumne som to nevedel spravit
            //vo fulltexte to mame ako TatraPay TB cize s medzerou kvoli sup
            searchText = Tools.replace(searchText, "TB", " TB");
        }

        //odstran dvojite medzery
        searchText = Tools.replace(searchText, "  ", " ");

        // Prepare search text
        String searchTextFinal = searchText.toLowerCase();

        //filter by current domain
        if(searchAllGroups || InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true)
        {
            Integer[] rootGroupIds = Tools.getTokensInteger(CloudToolsForCore.getRootGroupIds(), ",");
            predicates.add(root.get("rootGroupL1").in((Object[])rootGroupIds));
        }

        // Groups and Pages permissions check ONLY for docs filtering (files cant be changes anyway)
        if("docs".equals(searchType)) {
            // Prepare filtering by user rights to folders or pages
            Integer[] editablePagesIds = Tools.getTokensInteger(user.getEditablePages(), ",");
            if(rootGroupId > 0 || (searchAllGroups && Tools.isNotEmpty(user.getEditableGroups(true)))) {
                GroupsDB groupsDB = GroupsDB.getInstance();
                Integer[] groupsTreeIds = searchAllGroups
                    ? Arrays.stream(groupsDB.expandGroupIdsToChilds(Tools.getTokensInt(user.getEditableGroups(true), ","), true)).boxed().toArray(Integer[]::new)
                    : groupsDB.getGroupsTree(rootGroupId, true, true).stream().map(GroupDetails::getGroupId).toArray(Integer[]::new);
                Predicate groupsPredicate = groupsTreeIds.length == 0 ? builder.disjunction() : root.get("groupId").in((Object[]) groupsTreeIds);

                if(editablePagesIds.length > 0) {
                    // Right for pages in whole tree from given id + right for pages in editablePages
                    predicates.add(builder.or(
                        groupsPredicate,
                        root.get("id").in((Object[]) editablePagesIds)
                    ));
                } else {
                    // Right for pages in whole tree from given id
                    predicates.add(groupsPredicate);
                }
            } else if(editablePagesIds.length > 0) {
                // Right for pages in editablePages
                predicates.add(root.get("id").in((Object[]) editablePagesIds));
            }
        }

        if (Tools.isNotEmpty(searchTextFinal)) {
            predicates.add(builder.or(
                builder.like(root.get("data"), "%" + searchTextFinal + "%"),
                builder.like(root.get("dataAsc"), "%" + DB.internationalToEnglish(searchTextFinal) + "%"),
                builder.like(root.get("title"), "%" + searchTextFinal + "%")
            ));
        }

        if("files".equals(searchType)) {
            predicates.add(builder.like(root.get("externalLink"), "/files/%"));
        } else {
            predicates.add(builder.or(
                builder.isNull(root.get("externalLink")),
                builder.notLike(root.get("externalLink"), "/files/%")
            ));
        }
    }
}
