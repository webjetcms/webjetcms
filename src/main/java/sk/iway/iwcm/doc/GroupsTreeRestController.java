package sk.iway.iwcm.doc;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeItemType;
import sk.iway.iwcm.admin.jstree.JsTreeMoveItem;
import sk.iway.iwcm.admin.jstree.JsTreeRestController;
import sk.iway.iwcm.admin.layout.LayoutService;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.service.GroupsService;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.users.UsersDB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/admin/rest/groups/tree")
//allow admin to access jstree from other apps like perex groups
@PreAuthorize(value = "@WebjetSecurityService.isAdmin()")
public class GroupsTreeRestController extends JsTreeRestController<DocGroupInterface> {

    private final GroupsTreeService groupsTreeService;
    private final EditorFacade editorFacade;

    public GroupsTreeRestController(GroupsTreeService groupsTreeService, EditorFacade editorFacade) {
        this.groupsTreeService = groupsTreeService;
        this.editorFacade = editorFacade;
    }

    @Override
    protected void tree(Map<String, Object> result, JsTreeMoveItem item) {
        int id = item.getIdInt();
        if (id < 1 && item.getId() != null && item.getId().startsWith("/")) {
            //try to get ID from path
            GroupDetails group = GroupsDB.getInstance().getGroupByPath(item.getId());
            if (group != null) id = group.getGroupId();
        }
        final Identity user = UsersDB.getCurrentUser(getRequest());

        if (GroupsDB.isGroupViewable(user, id)==false && GroupsDB.isGroupEditable(user, id)==false) {
            result.put("result", false);
            result.put("error", getProp().getText("components.jstree.access_denied__group"));
            return;
        }

        getRequest().getSession().setAttribute(Constants.SESSION_GROUP_ID, String.valueOf(id));

        boolean parentEditable = GroupsDB.isGroupEditable(user, id);
        boolean parentViewable = GroupsDB.isGroupViewable(user, id);

        if (!parentEditable && !parentViewable)
        {
            result.put("result", false);
            result.put("error", getProp().getText("components.jstree.access_denied_parenteditable_and_parentviewable_is_false"));
            return;
        }

        boolean showPages = true;

        String click = getRequest().getParameter("click");
        if (click!=null && click.contains("dt-tree-group")) showPages = false;

        List<JsTreeItem> items = new ArrayList<>();

        if (click != null && (click.contains("dt-tree-group-root") || click.contains("dt-tree-groupid-root")) && id<0) {
            //id<0=zobraz root group
            GroupsJsTreeItem rootItem = new GroupsJsTreeItem(WebpagesService.getRootGroup(), user, false);
            rootItem.setIcon("ti ti-home");
            rootItem.getState().setLoaded(true);
            rootItem.getState().setOpened(true);
            items.add(rootItem);
        }
        if (click != null && click.contains("alldomains") && id<0 && Constants.getBoolean("multiDomainEnabled")) {
            //vygeneruj zoznam vsetkych domen
            LayoutService ls = new LayoutService(getRequest());
            List<String> domains = ls.getLayoutBean().getHeader().getDomains();
            for (String domain : domains) {
                GroupDetails domainGroup = WebpagesService.getRootGroup();
                domainGroup.setGroupName(domain);

                GroupsJsTreeItem rootItem = new GroupsJsTreeItem(domainGroup, user, false);
                rootItem.setIcon("ti ti-home");
                rootItem.setId("domain:"+domain);
                items.add(rootItem);
            }
        } else {
            if (id<0) id = 0;
            String requestedDomain = null;
            if (click!=null && click.contains("alldomains")) {
                String domain = item.getId();
                //pre virtualny koren domeny posielame id ako domain:www.domena.sk, ziskaj ju a pouzi ju na filtrovanie
                if (domain != null && domain.startsWith("domain:")) requestedDomain = domain.substring(7);
            }
            items.addAll(groupsTreeService.getItems(user, id, showPages, click, requestedDomain, getRequest()));
        }

        if (RequestBean.getAttribute("forceReloadTree")!=null) result.put("forceReloadTree", true);

        //If user can edited only selected groups, he can't use root group (because root is everythig)
        if( Tools.isNotEmpty(user.getEditableGroups()) ) { //Cant show all groups

            //Special case -> if we want tree items for STAT section AND user have cmp_stat_seeallgroups right, we do not filter by perms but RESTURN ALL ITEMS
            String referer = getRequest().getHeader("referer");
            if(false == (referer != null && referer.contains("/apps/stat/admin/") && user.isEnabledItem("cmp_stat_seeallgroups")) ) {
                //If root group is in list, remove it
                if(items.size()>0 && Tools.getIntValue( items.get(0).getId(), -1 ) == 0)
                    items.remove(0);
            }
        }

        result.put("result", true);
        result.put("items", items);
    }

    /**
     * Implementaacia abstraknej metody z parent controllera presunu polozky v stromovej strukture
     * @param result - mapa s vysledkom, ocakava sa kluc result (boolean) a moze obsahovat error (String), ktory sa na FE zobrazi v toastr.error
     * @param item   - {@link JsTreeMoveItem} presunuta polozka
     */
    @Override
    @PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
    protected void move(Map<String, Object> result, JsTreeMoveItem item) {
        JsTreeItem original = item.getNode().getOriginal();
        if (original == null) {
            result.put("result", false);
            result.put("error", getProp().getText("java.GroupsTreeRestController.move.json_original_missing"));
            return;
        }

        final Identity user = UsersDB.getCurrentUser(getRequest());
        String virtualPath = original.getVirtualPath();
        if (original.getType() == JsTreeItemType.GROUP) {
            GroupsDB groupsDB = GroupsDB.getInstance();

            GroupDetails groupById = groupsDB.getGroup(Tools.getIntValue(original.getId(), 0));
            if (groupById == null) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.group_not_found", original.getId()));
                return;
            }

            GroupDetails parent = groupsDB.getGroup(Tools.getIntValue(item.getParent(), 0));
            if (parent == null) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.parent_not_found", item.getParent()));
                return;
            }

            if (user.isDisabledItem("editDir")) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.editDir_disabled", item.getParent()));
                return;
            }

            if (!GroupsDB.isGroupEditable(user, groupById.getGroupId())) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.group_not_editable", item.getParent()));
                return;
            }
            if (!GroupsDB.isGroupEditable(user, parent.getGroupId())) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.parent_group_not_editable", item.getParent()));
                return;
            }

            groupById.setParentGroupId(parent.getGroupId());
            boolean saved = groupsDB.save(groupById);
            if (!saved) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.save_failed"));
                return;
            }

            List<GroupDetails> groups = groupsDB.getGroups(parent.getGroupId());
            List<GroupDetails> collect = groups.stream().filter(g -> g.getGroupId() != groupById.getGroupId()).sorted(Comparator.comparing(GroupDetails::getSortPriority)).collect(Collectors.toList());

            int sortPriority = collect.isEmpty()==false ? collect.get(0).getSortPriority() : parent.getSortPriority() * 10;
            collect.add(item.getPosition(), groupById);

            for (GroupDetails groupDetails : collect) {
                groupDetails.setSortPriority(sortPriority);
                groupsDB.save(groupDetails);
                sortPriority += Constants.getInt("sortPriorityIncrementGroup");
            }

            // refresh
            GroupsDB.getInstance(true);
        }

        else if (original.getType() == JsTreeItemType.PAGE) {
            GroupsDB groupsDB = GroupsDB.getInstance();
            DocDB docDB = DocDB.getInstance();

            int docId = docDB.getVirtualPathDocId(virtualPath, DocDB.getDomain(getRequest()));
            editorFacade.setIgnoreMultigroupMapping(true);
            DocDetails editorForm = editorFacade.getDocForEditor(docId, -1, -1);

            if (editorForm == null) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.doc_not_found", "" + docId));
                return;
            }

            GroupDetails parent = groupsDB.getGroup(Tools.getIntValue(item.getParent(), 0));
            if (parent == null) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.parent_not_found"));
                return;
            }

            if (user.isDisabledItem("addPage")) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.addPage_disabled"));
                return;
            }

            if (!editorFacade.isPageEditable(user, editorForm, false)) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.doc_not_editable"));
                return;
            }

            if (!GroupsDB.isGroupEditable(user, parent.getGroupId())) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.parent_group_not_editable"));
                return;
            }

            editorForm.setAuthorId(user.getUserId());
            editorForm.setGroupId(parent.getGroupId());
            editorForm.getEditorFields().setGroupDetails(parent);
            editorForm.getEditorFields().setRequestPublish(true);

            editorFacade.save(editorForm);
            int historyId = editorForm.getHistoryId();
            if (historyId == 0) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.save_failed"));
                return;
            }

            groupsTreeService.fixSortPriority(getRequest(), editorForm.getDocId(), parent, item.getPosition());
        }

        if (RequestBean.getAttribute("forceReloadTree")!=null) result.put("forceReloadTree", true);

        result.put("result", true);
    }

    @Override
    @PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
    protected void save(Map<String, Object> result, DocGroupInterface item) {
        // DocDetails
        /*if (item.getClass().isAssignableFrom(DocDetails.class)) {
            DocDetails doc = (DocDetails) item;
        }

        // GroupDetails
        else {
            //GroupDetails group = (GroupDetails) item;
        }*/
    }

    @Override
    @PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
    protected void delete(Map<String, Object> result, DocGroupInterface item) {
        // DocDetails
        /*if (item.getClass().isAssignableFrom(DocDetails.class)) {
            //DocDetails doc = (DocDetails) item;
        }

        // GroupDetails
        else {
            //GroupDetails group = (GroupDetails) item;
        }*/
    }

    @Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        //kontrola prav
        return true;
    }

    @GetMapping("/trash")
    public GroupDetails getTrashGroupDetails() {
        return GroupsService.getTrashGroupDetails();
    }

    @GetMapping("/system")
    public GroupDetails getSystemGroupDetails() {
        return GroupsService.getSystemGroupDetails();
    }

    /**
     * Retun default group option for groupTree (for current user).
     * If given groupId is out of user perms, return first permitted group.
     * IF user have right cmp_stat_seeallgroups, he can see all groups (in stat section ONLY).
     * @param groupId - group that is default selected
     * @return
     */
    @GetMapping("/defaultValue")
    public GroupDetails gerDefaultGroupTreeOptionForUser(@RequestParam("groupId") int groupId) {
        return GroupsTreeService.gerDefaultGroupTreeOptionForUser(groupId, getUser());
    }
}