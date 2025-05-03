package sk.iway.iwcm.editor.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UserDetails;

/**
 * Base for all REST controlers for webpages/based on web-pages-datatable.js
 */
public class WebpagesDatatable extends DatatableRestControllerV2<DocDetails, Long> {

    protected final DocDetailsRepository docDetailsRepository;
    protected final EditorFacade editorFacade;
    protected final DocAtrDefRepository docAtrDefRepository;

    private static final String GROUP_ID_PARAM_NAME = "groupId";

    @Autowired
    public WebpagesDatatable(DocDetailsRepository docDetailsRepository, EditorFacade editorFacade, DocAtrDefRepository docAtrDefRepository) {
        super(docDetailsRepository);
        this.docDetailsRepository = docDetailsRepository;
        this.editorFacade = editorFacade;
        this.docAtrDefRepository = docAtrDefRepository;
    }

    @Override
    public Page<DocDetails> getAllItems(Pageable pageable) {
        GetAllItemsDocOptions options = getDefaultOptions(pageable, true);

        return WebpagesService.getAllItems(options);
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
        //Cant insert in TRASH group
        Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
        String trashDirName = propSystem.getText("config.trash_dir");
        if(entity.getEditorFields() != null && entity.getEditorFields().getGroupDetails() != null && entity.getEditorFields().getGroupDetails().getFullPath().startsWith(trashDirName) ) { //starts with so it block insert in trash and child folders
            addNotify( new NotifyBean(getProp().getText("webpage.insert_into_trash.title"), getProp().getText("webpage.insert_into_trash.text"), NotifyBean.NotifyType.ERROR, 60000) );
            return null;
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
        int groupId = getGroupIdParamName();
        int historyId = Tools.getIntValue(getRequest().getParameter("historyId"), -1);

        List<NotifyBean> notifyList = new ArrayList<>();
        DocDetails docToReturn = WebpagesService.getOneItem(id, groupId, historyId, editorFacade, docAtrDefRepository, notifyList, getRequest());
        addNotify(notifyList);

        return docToReturn;
    }

    @Override
    public boolean deleteItem(DocDetails entity, long id) {
        boolean deleted = editorFacade.delete(entity);
        addNotify(editorFacade.getNotify());

        //ak je zapnute zobrazenie zoznamu stranok pre novu stranku musim spravit reload
        //ostatne ako zmena adresara vyvolava reload uz standardne
        if (WebpagesService.isTreeShowPages(getUser())) setForceReload(true);

        return deleted;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<DocDetails> root, CriteriaBuilder builder) {

        WebpagesService.addSpecSearch(params, predicates, root, builder, getUser());

        super.addSpecSearch(params, predicates, root, builder);
    }

    @Override
    public DocDetails processFromEntity(DocDetails entity, ProcessItemAction action, int rowCount) {
        return (DocDetails)WebpagesService.processFromEntity(entity, action, getRequest(), rowCount == 1);
    }

    @Override
    public Page<DocDetails> findByColumns(@RequestParam Map<String, String> params, Pageable pageable, DocDetails search) {

        int groupId = getGroupIdParamName();

        //ak chcem zobrazit recentPages
        if(groupId == Constants.getInt("systemPagesRecentPages")) {

            //Key groupId (and other) must be removed because we set this params in special way inside getAllItems method
            params.remove(GROUP_ID_PARAM_NAME);
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
        }

        return false;
    }


    /**
     * Overi, ci je potrebne obnovit aj stromovu strukturu (ak su zobrazene aj stranky) v jstree
     * @param entity
     * @param saved
     * @return
     */
    protected boolean isRefreshMenuRequired(DocDetails entity, DocDetails saved) {
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
    protected void addInsertEditNotify(boolean isInsert, List<UserDetails> approveByUsers) {
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

    /**
     * Prepare options for getAllItems method, read parameters from request
     * - groupId, if not present, set to Constants.rootGroupId
     * - userGroupId, if not present, set to -1
     * - recursive, if not present, set to false
     * @param pageable
     * @param checkPerms - true to verify user permissions for groupId
     * @return
     */
    public GetAllItemsDocOptions getDefaultOptions(Pageable pageable, boolean checkPerms) {
        GetAllItemsDocOptions options = new GetAllItemsDocOptions(getRequest());

        int groupId = getGroupIdParamName();
        getRequest().getSession().setAttribute(Constants.SESSION_GROUP_ID, String.valueOf(groupId));

        options.setGroupId(groupId);
        options.setUserGroupId(Tools.getIntValue(getRequest().getParameter("userGroupId"), -1));
        options.setPageable(pageable);

        options.setDocDetailsRepository(docDetailsRepository);
        options.setDocAtrDefRepository(docAtrDefRepository);

        options.setTempId(Tools.getIntValue(getRequest().getParameter("tempId"), -1));

        if("true".equals(getRequest().getParameter("recursive"))) options.setRecursiveSubfolders(true);

        if (checkPerms) {
            if (options.getGroupId()!=Constants.getInt("systemPagesRecentPages") && GroupsDB.isGroupEditable(getUser(), options.getGroupId())==false && GroupsDB.isGroupViewable(getUser(), options.getGroupId())==false) {
                throwError("components.jstree.access_denied__group");
            }
        }
        return options;
    }

    private int getGroupIdParamName() {
        return Tools.getIntValue(getRequest().getParameter(GROUP_ID_PARAM_NAME), Constants.getInt("rootGroupId"));
    }
}