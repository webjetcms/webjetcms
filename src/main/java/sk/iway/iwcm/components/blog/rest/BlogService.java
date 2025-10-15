package sk.iway.iwcm.components.blog.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Page;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.rest.GetAllItemsDocOptions;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;
import sk.iway.iwcm.system.jpa.JpaTools;

public class BlogService {

    private BlogService() { /*private constructor to hide the implicit public one*/ }

    public static DatatablePageImpl<DocDetails> getAllItems(GetAllItemsDocOptions options) {
		Page<DocDetails> page = null;
		List<Integer> allGroupIds = getBloggerDataList(options.getCurrentUser(), Tools.getIntValue(options.getRequest().getParameter("groupId"), -1));
		if(allGroupIds == null || allGroupIds.isEmpty()) page = new DatatablePageImpl<>(new ArrayList<>());
		else  {
			int[] groupIdsArray = allGroupIds.stream().mapToInt(Integer::intValue).toArray();
			page = options.getDocDetailsRepository().findAllByGroupIdIn(groupIdsArray, options.getPageable());
		}

        return WebpagesService.preparePage(page, options);
    }

    private static List<Integer> getBloggerDataList(Identity currentUser, int selectedGroupId) {
		if(BloggerService.isUserBloggerAdmin(currentUser)) {
			//It's admin with perms cmp_blog && cmp_blog_admin -> return all bloggers web pages
			List<Integer> allGroupIds = BloggerService.getAllBloggersGroupIds();

			if(selectedGroupId == -1) //Docs from all bloggers groups
				return allGroupIds;
			else if(allGroupIds.contains(selectedGroupId)) //Docs from specific blogger group
				return Arrays.asList(selectedGroupId);
		} else if(BloggerService.isUserBlogger( currentUser )) {
			//It's blogger -> return only his web pages
			int rootGroupId = Tools.getTokensInt(currentUser.getEditableGroups(), ",")[0];

			//Get groups tree from user editable root group
			List<GroupDetails> groupsTree = GroupsDB.getInstance().getGroupsTree(rootGroupId, true, true);
			List<Integer> groupIds = new ArrayList<>();
			for(GroupDetails group : groupsTree) groupIds.add(group.getGroupId());

			if(selectedGroupId == -1)
				return groupIds;
			else if(groupIds.contains(selectedGroupId))
				return Arrays.asList(selectedGroupId);
		}

		//User has no right or specific groupId is not from his groups tree (or not from any blogger group tree)
		return null;
	}

	public static void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<DocDetails> root, CriteriaBuilder builder, Identity user) {
		//Orig logic
        WebpagesService.addSpecSearch(params, predicates, root, builder, user);

		//remove groupId predicate
		JpaTools.removePredicateWithName("groupId", predicates);

		//Plus our groupId logic
		List<Integer> bloggersGroupIds = getBloggerDataList(user, Tools.getIntValue(params.get("groupId"), -1));
		if(bloggersGroupIds != null)
			predicates.add(root.get("groupId").in(bloggersGroupIds));
	}

	public static List<LabelValueInteger> getActualBloggerGroups(Identity currentUser) {
        List<LabelValueInteger> groupsMap = new ArrayList<>();

        //Check if it's blogger ADMIN
        if(BloggerService.isUserBloggerAdmin( currentUser )) {
            //Return all bloggers groups
            List<Integer> bloggersRootGroupsIds = BloggerService.getAllBloggersRootGroupIds();

            for(Integer rootGroupId : bloggersRootGroupsIds) {
                //Get groups tree from user editable root group
				List<GroupDetails> groopsTree = GroupsDB.getInstance().getGroupsTree(rootGroupId, true, true);
                for(GroupDetails group : groopsTree)
                    groupsMap.add( new LabelValueInteger(group.getFullPath(), group.getGroupId()) );
            }
            return groupsMap;
        }

        //Check if it's blogger with perm
        if(BloggerService.isUserBlogger( currentUser )) {
            //Root group id
            int rootGroupId = Tools.getTokensInt(currentUser.getEditableGroups(), ",")[0];
            List<GroupDetails> groopsTree = GroupsDB.getInstance().getGroupsTree(rootGroupId, true, true);
            for(GroupDetails group : groopsTree) {
				groupsMap.add( new LabelValueInteger(group.getFullPath(), group.getGroupId()) );
			}

            return groupsMap;
        }

		//!! SORT is NOT necessary - we are getting whole tree, so they are already sorted

        //Else return empty map
        return groupsMap;
    }
}
