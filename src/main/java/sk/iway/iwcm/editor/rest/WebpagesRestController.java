package sk.iway.iwcm.editor.rest;

import java.util.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.blog.rest.BlogService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.HistoryDB;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefEntity;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyButton;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UserDetails;

/**
 * Rest controller pre datatabulku v administracii web stranky #44836
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/web-pages")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('Constants:webpagesFunctionsPerms')")
public class WebpagesRestController extends DatatableRestControllerV2<DocDetails, Long> {

    private final DocDetailsRepository docDetailsRepository;
    private final EditorFacade editorFacade;
    private final DocAtrDefRepository docAtrDefRepository;

    @Autowired
    public WebpagesRestController(DocDetailsRepository docDetailsRepository, EditorFacade editorFacade, DocAtrDefRepository docAtrDefRepository) {
        super(docDetailsRepository);
        this.docDetailsRepository = docDetailsRepository;
        this.editorFacade = editorFacade;
        this.docAtrDefRepository = docAtrDefRepository;
    }

    @Override
    public Page<DocDetails> getAllItems(Pageable pageable) {

        GetAllItemsDocOptions options = new GetAllItemsDocOptions(getRequest());

        int groupId = Tools.getIntValue(getRequest().getParameter("groupId"), Constants.getInt("rootGroupId"));
        groupId = getNewsGroupId(groupId);
        getRequest().getSession().setAttribute(Constants.SESSION_GROUP_ID, String.valueOf(groupId));

        options.setGroupId(groupId);
        options.setUserGroupId(Tools.getIntValue(getRequest().getParameter("userGroupId"), -1));
        options.setPageable(pageable);

        options.setDocDetailsRepository(docDetailsRepository);
        options.setDocAtrDefRepository(docAtrDefRepository);

        options.setTempId(Tools.getIntValue(getRequest().getParameter("tempId"), -1));

        if("true".equals(getRequest().getParameter("recursive"))) options.setRecursiveSubfolders(true);

        if(isBlogVersion()) {
            //its blogger user
            if (BlogService.isUserBloggerOrBloggerAdmin(getUser())==false) {
                throwError(getProp().getText("components.permsDenied"));
            }
        } else {
           //Original control
           if (options.getGroupId()!=Constants.getInt("systemPagesRecentPages") && GroupsDB.isGroupEditable(getUser(), options.getGroupId())==false && GroupsDB.isGroupViewable(getUser(), options.getGroupId())==false) {
                throwError("components.jstree.access_denied__group");
            }
        }

        return WebpagesService.getAllItems(options);
    }

    @Override
    public void beforeSave(DocDetails entity) {
        //In abtesting version user cant edit/insert/duplicate page's
        if(isAbTestingVersion()) {
            throwError(getProp().getText("admin.editPage.error"));
        }
    }

    @Override
    public DocDetails editItem(DocDetails entity, long id) {

        DocDetails original = DocDB.getInstance().getDoc(entity.getDocId(), -1, false);
        DocDetails saved = editorFacade.save(entity);

        List<UserDetails> approveByUsers = editorFacade.getApprovers();
        addInsertEditNotify(false, approveByUsers);

        if (editorFacade.isForceReload()) setForceReload(true);

        if (isRefreshMenuRequired(original, saved)) setForceReload(true);

        addNotify(editorFacade.getNotify());

        if (RequestBean.getAttribute("forceReloadTree")!=null) setForceReload(true);

        return saved;
    }

    @Override
    public DocDetails insertItem(DocDetails entity) {
        //Is this blog version
        if(isBlogVersion()) {
            //Check user perms
            boolean isBloggerAdmin = BlogService.isUserBloggerAdmin( getUser() );
            boolean isBlogger = BlogService.isUserBlogger( getUser() );

            //If user is not blogger admin or blogger, throw error
            if(isBloggerAdmin==false && isBlogger==false) throwError(getProp().getText("components.blog.basic_perm_error"));

            //There must be selected groupId
            int selectedGroup = entity.getEditorFields().getGroupDetails().getGroupId();

            //Check that default group is not selected
            if(selectedGroup == Constants.getInt("rootGroupId")) throwError(getProp().getText("components.blog.add_page.error"));

            //If user is blogger admin, check if group is blogger group
            if(isBloggerAdmin) {
                List<Integer> allBloggersGroupIds = BlogService.getAllBloggersGroupIds();
                if(false==allBloggersGroupIds.contains(selectedGroup)) throwError(getProp().getText("components.blog.basic_perm_error"));
            } //If user is blogger, check if he has perm to selected group
            else if(isBlogger && false == GroupsDB.isGroupEditable(getUser(), selectedGroup)) throwError(getProp().getText("components.blog.basic_perm_error"));

            //For safety, cant set this
            entity.getEditorFields().setGroupCopyDetails(null);
        }

        DocDetails saved = editorFacade.save(entity);
        addNotify(editorFacade.getNotify());

        //ak je zapnute zobrazenie zoznamu stranok pre novu stranku musim spravit reload
        //ostatne ako zmena adresara vyvolava reload uz standardne
        if (WebpagesService.isTreeShowPages(getUser())) setForceReload(true);

        List<UserDetails> approveByUsers = editorFacade.getApprovers();
        addInsertEditNotify(true, approveByUsers);

        if (RequestBean.getAttribute("forceReloadTree")!=null) setForceReload(true);

        return saved;
    }

    @Override
    public DocDetails getOneItem(long id) {
        int groupId = Tools.getIntValue(getRequest().getParameter("groupId"), Constants.getInt("rootGroupId"));
        int historyId = Tools.getIntValue(getRequest().getParameter("historyId"), -1);

        if (isBlogVersion()) {
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
        }
        groupId = getNewsGroupId(groupId);

        if (groupId == Constants.getInt("systemPagesDocsToApprove")) {
            //pre tento pripad mame otocene docid a historyid, ale principialne dostavame v id hodnotu historyid, takze to potrebujeme takto nacitat
            historyId = (int)id;
            //ziskaj docid podla historyid
            id = (new SimpleQuery()).forInt("SELECT doc_id FROM documents_history WHERE history_id=?", historyId);
        }

        Prop prop = Prop.getInstance(getRequest());

        DocDetails doc = editorFacade.getDocForEditor((int) id, historyId, groupId);

        if(id == -1) {
            doc.setGenerateUrlFromTitle(true);
        }

        if (ContextFilter.isRunning(getRequest())) {
            // do editoru nahrame texty s pridanymi linkami
            doc.setData(ContextFilter.addContextPath(getRequest().getContextPath(), doc.getData()));
        }

        if (groupId == Constants.getInt("systemPagesDocsToApprove")) {
			int docId = doc.getDocId();
			doc.setDocId(historyId);
			doc.setHistoryId(docId);
        }

        //over, ci existuju neschvalene/rozpracovane verzie, ak ano, zobraz notifikaciu
        HistoryDB historyDB = new HistoryDB("iwcm");
        List<DocDetails> history = historyDB.getHistory(doc.getDocId(), false, true);
        if (history != null && history.isEmpty()==false) {
            if (historyId < 1) {
                //ak nemame zadane historyId pridaj notifikaciu o tom, ze existuje novsia verzia
                NotifyBean notify = new NotifyBean(prop.getText("text.warning"), prop.getText("editor.notify.checkHistory"), NotifyBean.NotifyType.WARNING, 15000);
                notify.addButton(new NotifyButton(prop.getText("editor.notify.editFromHistory"), "btn btn-primary", "ti ti-history", "editFromHistory("+history.get(0).getDocId()+", "+history.get(0).getHistoryId()+")"));
                addNotify(notify);
            }
            getRequest().getSession().removeAttribute("docHistory");
        }

        List<DocAtrDefEntity> atrDefs = docAtrDefRepository.findAllByDocId(MultigroupMappingDB.getMasterDocId(doc.getDocId(), true), CloudToolsForCore.getDomainId());
        atrDefs.forEach(f -> {
            if (f.getDocAtrEntities()!=null && f.getDocAtrEntities().isEmpty()==false) {
                //normally in JSON we don't want to send all DocAtrEntity relationship (it's lazy loaded ant it will be populated), it's JsonIgnored,
                //we just need first entity to be sent, so set it here for this specific case
                f.setDocAtrEntityFirst(f.getDocAtrEntities().get(0));
            }
        });
        doc.getEditorFields().setAttrs(atrDefs);

        String newPageTitleKey = getRequest().getParameter("newPageTitleKey");
        if (Tools.isNotEmpty(newPageTitleKey) && doc.getDocId()<1) {
            doc.setTitle(prop.getText(newPageTitleKey));
        }

        return doc;
    }

    @Override
    public boolean deleteItem(DocDetails entity, long id) {
        //In abtesting version user cant edit page's
        if(isAbTestingVersion()) {
            throwError(getProp().getText("admin.editPage.error"));
        }

        boolean deleted = editorFacade.delete(entity);
        addNotify(editorFacade.getNotify());

        //ak je zapnute zobrazenie zoznamu stranok pre novu stranku musim spravit reload
        //ostatne ako zmena adresara vyvolava reload uz standardne
        if (WebpagesService.isTreeShowPages(getUser())) setForceReload(true);

        return deleted;
    }

    /**
     * Vrati zoznam parent adresarov pre zadane docid, nastavi aj domenu ak je odlisna
     * a kartu (System/Kos) v ktorej sa nachadza koren adresarov
     * @param docId
     * @return
     */
    @RequestMapping(path="/parents/{id}")
    public ParentGroupsResult parentGroups(@PathVariable("id") int docId) {
        ParentGroupsResult result = new ParentGroupsResult();

        DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
        if (doc != null) {
            List<GroupDetails> parentGroups = GroupsDB.getInstance().getParentGroups(doc.getGroupId());
            result.setParentGroups(doc.getGroup(), parentGroups);
        }

        return result;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<DocDetails> root, CriteriaBuilder builder) {

        WebpagesService.addSpecSearch(params, predicates, root, builder, getUser());

        super.addSpecSearch(params, predicates, root, builder);
    }

    @Override
    public DocDetails processFromEntity(DocDetails entity, ProcessItemAction action) {
        return (DocDetails)WebpagesService.processFromEntity(entity, action, getRequest());
    }

    @Override
    public Page<DocDetails> findByColumns(@RequestParam Map<String, String> params, Pageable pageable, DocDetails search) {

        Integer groupId = Tools.getIntValue(getRequest().getParameter("groupId"), Constants.getInt("rootGroupId"));

        //ak chcem zobrazit recentPages
        if(groupId == Constants.getInt("systemPagesRecentPages")) {

            //Key groupId (and other) must be removed because we set this params in special way inside getAllItems method
            params.remove("groupId");
            params.remove("size");
            params.remove("page");
            params.remove("sort");

            BeanWrapperImpl searchWrapped = new BeanWrapperImpl(search);
            final Map<String, String> searchProperties = new HashMap<>();
            getSearchProperties(params, searchProperties, searchWrapped, null, false);

            //Get specification from columns params
            Specification<DocDetails> columnsSpecification = getSearchConditions(searchProperties, params, search);

            GetAllItemsDocOptions options = new GetAllItemsDocOptions(getRequest());

            options.setGroupId(groupId);
            options.setUserGroupId(Tools.getIntValue(getRequest().getParameter("userGroupId"), -1));
            options.setPageable(pageable);
            options.setDocDetailsRepository(docDetailsRepository);
            options.setDocAtrDefRepository(docAtrDefRepository);
            options.setColumnsSpecification(columnsSpecification);

            //If second param columnsSpecification is set, method will join columnsSpecification to created specification by method
            Page<DocDetails> page = WebpagesService.getAllItems(options);

            return page;
        }

        return super.findByColumns(params, pageable, search);
    }

    /**
     * Ulozi do session DocDetails vytvoreny z JSON objektu, ktory sa nasledne pouzije
     * v PreviewController pre vytvorenie nahladu web stranky
     * Je to takto, lebo inak som nevedel preniest JSON data z editora priamo do
     * PreviewControllera
     */
    @PostMapping(value="/preview/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String preview(@RequestBody DocDetails entity, HttpServletRequest request) {

        entity.getEditorFields().toDocDetails(entity);

        setPreviewObject(entity, request);
        return "{\"ok\": true}";
    }

    @Override
    public void afterSave(DocDetails entity, DocDetails saved) {
        setPreviewObject(saved, getRequest());
    }

    /**
     * Ulozi do session DocDetails objekt pre nahlad stranky
     * @param entity
     * @param request
     */
    private void setPreviewObject(DocDetails entity, HttpServletRequest request) {
        request.getSession().setAttribute("ShowdocAction.showDocData", entity);
    }

    @Override
    public boolean processAction(DocDetails entity, String action) {
        if ("saveAsAbTest".equals(action)) {
            //sprav kopiu entity a uloz ako B variant
            DocDetails saved = editorFacade.saveAsBVariant(entity);
            if (saved != null) setForceReload(true);

            addNotify(editorFacade.getNotify());

            return true;
        } else if("recoverDoc".equals(action)) {
            editorFacade.recoverWebpageFromTrash(entity);

            addNotify(editorFacade.getNotify());

            return true;
        } else if("addBloggerGroup".equals(action)) {
            String customData = getRequest().getParameter("customData");
            boolean success = BlogService.addNewBloggerGroup(editorFacade, getUser(), customData);

            if(success)
                addNotify( new NotifyBean(getProp().getText("components.blog.add_folder.title"), getProp().getText("components.blog.add_new_group.success"), NotifyBean.NotifyType.SUCCESS, 60000) );
            else
                addNotify( new NotifyBean(getProp().getText("components.blog.add_folder.title"), getProp().getText("components.blog.add_new_group.failed"), NotifyBean.NotifyType.ERROR, 60000) );

            return true;
        }

        return false;
    }


    /**
     * Overi, ci je potrebne obnovit aj stromovu strukturu (ak su zobrazene aj stranky) v jstree
     * @param entity
     * @param saved
     * @return
     */
    private boolean isRefreshMenuRequired(DocDetails entity, DocDetails saved) {
        if (WebpagesService.isTreeShowPages(getUser())==false) return false;

		if (entity.getDocId() < 1) return true;

		if (saved != null) {
			if (saved.getTitle().equals(entity.getTitle()) == false) return true;
			if (saved.getExternalLink().equals(entity.getExternalLink()) == false) return true;
			if (saved.getVirtualPath().equals(entity.getVirtualPath()) == false) return true;
			if (saved.isAvailable() != entity.isAvailable()) return true;
			if (saved.getGroupId() != entity.getGroupId()) return true;
			if (saved.getSortPriority() != entity.getSortPriority()) return true;
		}

		return false;
	}

    @Override
    public void afterDuplicate(DocDetails entity, Long originalId) {
        if (originalId!=null && entity.getDocId()>0) editorFacade.duplicateMedia(originalId.intValue(), entity.getDocId());
    }

    /**
     * Show notification to user if action need to be appoved by approver by sending email ( + approver full name).
     * @param isInsert - true if its insert, false if its edit (change notify text)
     * @param approveByUsers - list of users that one of them need approve this action
     */
    private void addInsertEditNotify(boolean isInsert, List<UserDetails> approveByUsers) {
        String needApproveText = isInsert ? "editor.approveRequestInsert" : "editor.approveRequestGet";

        if(approveByUsers != null && approveByUsers.isEmpty()==false) {
            StringBuilder userNames = new StringBuilder();
            for (UserDetails approveUser : approveByUsers) {
                if (userNames.length()>0) userNames.append(", ");
                userNames.append(approveUser.getFullName());
            }

            NotifyBean notify = new NotifyBean(getProp().getText("editor.approve.notifyTitle"), getProp().getText(needApproveText)+": "+userNames.toString(), NotifyBean.NotifyType.INFO, 60000);
            addNotify(notify);
        } else {
            if (editorFacade.isPageSavedAsWorkVersion()) {
                NotifyBean notify = new NotifyBean(getProp().getText("text.info"), getProp().getText("editor.pageSaved"), NotifyBean.NotifyType.INFO, 15000);
                addNotify(notify);
            }

            if (editorFacade.getPublihStart() != null) {
                String publishDateTime = Tools.formatDateTimeSeconds(editorFacade.getPublihStart());
                NotifyBean notify = new NotifyBean(getProp().getText("text.warning"), getProp().getText("editor.publish.pagesaved")+" "+publishDateTime, NotifyBean.NotifyType.WARNING, 30000);
                addNotify(notify);
            }
        }
    }

    @RequestMapping(value="/blogger-groups")
    public Map<Integer, String> getActualBloggerGroups() {
        Map<Integer, String> groupsMap = new HashMap<>();

        //Check if it's blogger ADMIN
        if(BlogService.isUserBloggerAdmin( getUser() )) {
            //Return all bloggers groups
            List<Integer> bloggersRootGroupsIds = BlogService.getAllBloggersRootGroupIds();

            for(Integer rootGroupId : bloggersRootGroupsIds) {
                //Get groups tree from user editable root group
				List<GroupDetails> groopsTree = GroupsDB.getInstance().getGroupsTree(rootGroupId, true, true);
                for(GroupDetails group : groopsTree)
                    groupsMap.put(group.getGroupId(), group.getFullPath());
            }
            return groupsMap;
        }

        //Check if it's blogger with perm
        if(BlogService.isUserBlogger( getUser() )) {
            //Root group id
            int rootGroupId = Tools.getTokensInt(getUser().getEditableGroups(), ",")[0];
            List<GroupDetails> groopsTree = GroupsDB.getInstance().getGroupsTree(rootGroupId, true, true);
            for(GroupDetails group : groopsTree)
                groupsMap.put(group.getGroupId(), group.getFullPath());

            return groupsMap;
        }

        //Else return empty map
        return groupsMap;
    }

    private int getNewsGroupId(int currentGroupId) {
        int groupId = currentGroupId;
        //news version get group ID in groupIdList parameter
        String groupIdList = getRequest().getParameter("groupIdList");
        if (Tools.isNotEmpty(groupIdList)) {
            if (groupId < 1 || groupId == Constants.getInt("rootGroupId")) {
                try {
                    groupIdList = Tools.replace(groupIdList, "*", "");
                    if (groupIdList.indexOf(",")!=-1) groupIdList=groupIdList.substring(0, groupIdList.indexOf(","));

                    int groupIdParser = Tools.getIntValue(groupIdList, -1);
                    if (groupIdParser > 0) groupId = groupIdParser;
                } catch (Exception e) {
                    //do nothing - failsafe
                }
            }
        }
        return groupId;
    }

    private boolean isBlogVersion() {
        return "true".equals(getRequest().getParameter("isBlogVersion"));
    }

    private boolean isAbTestingVersion() {
        return "true".equals(getRequest().getParameter("isABtestingVersion"));
    }

    @Override
    public boolean checkItemPerms(DocDetails entity, Long id) {
        if (InitServlet.isTypeCloud() && entity.getDocId()>0) {
            DocDetails old = DocDB.getInstance().getBasicDocDetails(entity.getDocId(), false);
            if (old != null) {
                GroupDetails group = GroupsDB.getInstance().getGroup(old.getGroupId());
                if (group != null && GroupsDB.isGroupEditable(getUser(), group.getGroupId())==false) return false;
            }
        }
        return true;
    }
}