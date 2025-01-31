package sk.iway.iwcm.components.blog.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.rest.WebpagesDatatable;
import sk.iway.iwcm.editor.rest.GetAllItemsDocOptions;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;

@Datatable
@RestController
@RequestMapping("/admin/rest/blog")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_blog|cmp_blog_admin')")
public class BlogRestController extends WebpagesDatatable {

    @Autowired
    public BlogRestController(DocDetailsRepository docDetailsRepository, EditorFacade editorFacade, DocAtrDefRepository docAtrDefRepository) {
        super(docDetailsRepository, editorFacade, docAtrDefRepository);
    }

    @Override
    public Page<DocDetails> getAllItems(Pageable pageable) {

        if (BloggerService.isUserBloggerOrBloggerAdmin(getUser())==false) {
            throwError(getProp().getText("components.permsDenied"));
        }

        GetAllItemsDocOptions options = getDefaultOptions(pageable, false);
        return BlogService.getAllItems(options);
    }

    @Override
    public DocDetails insertItem(DocDetails entity) {
        //Check user perms
        boolean isBloggerAdmin = BloggerService.isUserBloggerAdmin( getUser() );
        boolean isBlogger = BloggerService.isUserBlogger( getUser() );

        //If user is not blogger admin or blogger, throw error
        if(isBloggerAdmin==false && isBlogger==false) throwError(getProp().getText("components.blog.basic_perm_error"));

        //There must be selected groupId
        int selectedGroup = entity.getEditorFields().getGroupDetails().getGroupId();

        //Check that default group is not selected
        if(selectedGroup == Constants.getInt("rootGroupId")) throwError(getProp().getText("components.blog.add_page.error"));

        //If user is blogger admin, check if group is blogger group
        if(isBloggerAdmin) {
            List<Integer> allBloggersGroupIds = BloggerService.getAllBloggersGroupIds();
            if(false==allBloggersGroupIds.contains(selectedGroup)) throwError(getProp().getText("components.blog.basic_perm_error"));
        } //If user is blogger, check if he has perm to selected group
        else if(isBlogger && false == GroupsDB.isGroupEditable(getUser(), selectedGroup)) throwError(getProp().getText("components.blog.basic_perm_error"));

        //For safety, cant set this
        entity.getEditorFields().setGroupCopyDetails(null);

        //super from BaseWebpagesRestController
        return super.insertItem(entity);
    }

    @Override
    public boolean processAction(DocDetails entity, String action) {
        if("addBloggerGroup".equals(action)) {
            String customData = getRequest().getParameter("customData");
            boolean success = BloggerService.addNewBloggerGroup(super.editorFacade, getUser(), customData);

            if(success)
                addNotify( new NotifyBean(getProp().getText("components.blog.add_folder.title"), getProp().getText("components.blog.add_new_group.success"), NotifyBean.NotifyType.SUCCESS, 60000) );
            else
                addNotify( new NotifyBean(getProp().getText("components.blog.add_folder.title"), getProp().getText("components.blog.add_new_group.failed"), NotifyBean.NotifyType.ERROR, 60000) );

            return true;
        }

        return false;
    }

    @Override
    public DocDetails getOneItem(long id) {
        int groupId = Tools.getIntValue(getRequest().getParameter("groupId"), Constants.getInt("rootGroupId"));
        int historyId = Tools.getIntValue(getRequest().getParameter("historyId"), -1);

        if (groupId < 1 || groupId == Constants.getInt("rootGroupId")) {
            String unclassifiedGroupName = getProp().getText("components.blog.default_group_name");
            int unclassifiedGroupId = -1;
            int userRootGroupId = WebpagesService.getUserFirstEditableGroup(getUser());
            if (userRootGroupId>0) {
                GroupsDB groupsDB = GroupsDB.getInstance();
                List<GroupDetails> subgroups = groupsDB.getGroups(userRootGroupId);

                for (GroupDetails group : subgroups) {
                    if (group != null && group.getGroupName().equals(unclassifiedGroupName)) unclassifiedGroupId = group.getGroupId();
                }
                if (unclassifiedGroupId > 0) {
                    groupId = unclassifiedGroupId;
                } else {
                    groupId = userRootGroupId;
                }
            }
        }

        List<NotifyBean> notifyList = new ArrayList<>();

        DocDetails docToReturn = WebpagesService.getOneItem(id, groupId, historyId, editorFacade, super.docAtrDefRepository, notifyList, getRequest());

        //
        addNotify(notifyList);

        return docToReturn;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<DocDetails> root, CriteriaBuilder builder) {
        BlogService.addSpecSearch(params, predicates, root, builder, getUser());

        super.addSpecSearch(params, predicates, root, builder);
    }

    @RequestMapping(value="/blogger-groups")
    public List<LabelValueInteger> getActualBloggerGroups() {
        return BlogService.getActualBloggerGroups(getUser());
    }
}
