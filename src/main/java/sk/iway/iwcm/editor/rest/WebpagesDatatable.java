package sk.iway.iwcm.editor.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.components.ai.tools.AiToolService;
import sk.iway.iwcm.editor.service.DeleteRequestContext;
import sk.iway.iwcm.editor.service.EditorService;
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

    /** Previews headers for unsaved editor values without saving or publishing the page. */
    @PostMapping("/http-headers")
    public Map<String, String> httpHeaders(@RequestBody DocDetails doc, HttpServletRequest request, HttpServletResponse response) {
        EditorForm form = new EditorForm(doc);
        if (Boolean.TRUE.equals(doc.getGenerateUrlFromTitle())) form.setVirtualPath("");
        else if (Boolean.TRUE.equals(doc.getUrlInheritGroup())) form.setVirtualPath(doc.getEditorVirtualPath() == null ? "" : doc.getEditorVirtualPath());
        EditorDB.setVirtualPath(form);
        String path = form.getVirtualPath();
        if (path == null || path.isEmpty()) path = "/showdoc.do?docid=" + doc.getDocId();
        return PathFilter.previewPageHeaders(path, doc, request, response);
    }

    @Override
    public Page<DocDetails> getAllItems(Pageable pageable) {
        GetAllItemsDocOptions options = getDefaultOptions(pageable, true);

        return checkListAccess(WebpagesService.getAllItems(options));
    }

    @Override
    public DocDetails editItem(DocDetails entity, long id) {

        DocDetails original = DocDB.getInstance().getDoc(entity.getDocId(), -1, false);
        // EditorFacade saves the submitted entity without the default DataTable merge. Hidden editor fields
        // are omitted from the request, so their defaults (e.g. fieldA = "") would overwrite stored content.
        // Restore hidden/disabled fields using the shared DataTable rules and the version being edited,
        // including history selection and multigroup mapping, rather than always using the published page.
        DocDetails one = editorFacade.getExistingDocForEditor((int) id, entity.getHistoryId());
        if (one == null) throwError("datatables.error.recordIsNotEditable");
        restoreNonEditableFields(entity, one, getNonEditableProperties(entity));
        DocDetails saved = editorFacade.save(entity);

        List<UserDetails> approveByUsers = editorFacade.getApprovers();
        addInsertEditNotify(false, approveByUsers);

        if (editorFacade.isForceReload()) setForceReload(true);

        if (isRefreshMenuRequired(original, saved)) setForceReload(true);

        addNotify(editorFacade.getNotify());

        if (RequestBean.getAttribute("forceReloadTree")!=null) setForceReload(true);

        return saved;
    }

    private static void restoreNonEditableFields(Object entity, Object defaults, List<String> fields) {
        // Direct access preserves empty defaults even for setters that ignore empty strings.
        var target = new DirectFieldAccessor(entity);
        var source = new DirectFieldAccessor(defaults);
        for (String name : fields) target.setPropertyValue(name, source.getPropertyValue(name));
    }

    @Override
    public DocDetails insertItem(DocDetails entity) {
        boolean toolRequest = AiToolService.isToolRequest(getRequest(), WebpagesRestController.class);
        if (toolRequest) {
            validateToolCreate(entity); // Checks the persisted folder, including Trash.
        } else if (entity.getEditorFields() != null && entity.getEditorFields().getGroupDetails() != null) {
            String trash = Prop.getInstance(Constants.getString("defaultLanguage")).getText("config.trash_dir");
            if (entity.getEditorFields().getGroupDetails().getFullPath().startsWith(trash)) {
                addNotify(new NotifyBean(getProp().getText("webpage.insert_into_trash.title"), getProp().getText("webpage.insert_into_trash.text"), NotifyBean.NotifyType.ERROR, 60000));
                return null;
            }
        }

        DocDetails saved = editorFacade.save(entity);
        addNotify(editorFacade.getNotify());

        //ak je zapnute zobrazenie zoznamu stranok pre novu stranku musim spravit reload
        //ostatne ako zmena adresara vyvolava reload uz standardne
        if (WebpagesService.isTreeShowPages(getUser())) setForceReload(true);

        List<UserDetails> approveByUsers = editorFacade.getApprovers();
        addInsertEditNotify(true, approveByUsers);
        if (toolRequest) addNotifyToEditorFields(saved);

        if (RequestBean.getAttribute("forceReloadTree")!=null) setForceReload(true);

        return saved;
    }

    private void validateToolCreate(DocDetails entity) {
        var fields = entity.getEditorFields();
        if (fields == null || fields.getGroupDetails() == null || fields.getGroupDetails().getGroupId() < 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A positive editorFields.groupDetails.groupId is required.");
        var folder = WebpagesService.requireCreateFolder(getUser(), fields.getGroupDetails().getGroupId());
        var defaults = editorFacade.getDocForEditor(-1, -1, folder.getGroupId());
        restoreNonEditableFields(entity, defaults, getNonEditableProperties(entity));
        restoreNonEditableFields(fields, defaults.getEditorFields(), getNonEditableFields(DocEditorFields.class));
        fields.setGroupDetails(folder);
        if (fields.getGroupCopyDetails() != null)
            for (var copy : fields.getGroupCopyDetails()) WebpagesService.requireCreateFolder(getUser(), copy.getGroupId());
        if (fields.getAttrs() != null) {
            for (var attr : fields.getAttrs()) {
                var value = attr.getDocAtrEntityFirst();
                if (value == null) continue;
                var definition = docAtrDefRepository.findFirstByIdAndDomainId(attr.getId(), CloudToolsForCore.getDomainId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown page attribute."));
                value.setAtrDef(definition);
                value.setDocId(null);
                value.setId(null);
            }
        }
    }

    @Override
    public DocDetails getOneItem(long id) {
        int groupId = getGroupIdParam();
        int historyId = Tools.getIntValue(getRequest().getParameter("historyId"), -1);

        boolean tool = AiToolService.isToolRequest(getRequest(), WebpagesRestController.class);
        if (tool && id == -1) {
            if (Tools.getIntValue(getRequest().getParameter(GROUP_ID_PARAM_NAME), 0) < 1 || getRequest().getParameter("historyId") != null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use id=-1 with a positive groupId and no historyId.");
            WebpagesService.requireCreateFolder(getUser(), groupId);
        } else if (tool && (id < 1 || id > Integer.MAX_VALUE || groupId < 1 || groupId == Constants.getInt("systemPagesDocsToApprove"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a page ID and optional historyId.");
        }

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

        int groupId = getGroupIdParam();

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
            return checkListAccess(WebpagesService.getAllItems(options));
        }

        return checkListAccess(super.findByColumns(params, pageable, search));
    }

    private Page<DocDetails> checkListAccess(Page<DocDetails> pages) {
        if (AiToolService.isToolRequest(getRequest(), WebpagesRestController.class)) {
            for (DocDetails page : pages) {
                if (!WebpagesService.isInCurrentDomain(GroupsDB.getInstance().getGroup(page.getGroupId())) || !EditorService.hasPagePermission(getUser(), page))
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "List pages within an accessible folder.");
            }
        }
        return pages;
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
        if (!AiToolService.isToolRequest(getRequest(), WebpagesRestController.class)) setPreviewObject(saved, getRequest());
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
        if (getUser() == null) return false;

        // A selected page may already have been deleted through its mirror in this request.
        // Its permissions were checked before deletion; do not reject it now because it is in Trash or missing.
        // The deletion service also checks this marker and skips deleting the page again.
        if (id != null && DeleteRequestContext.isDeleted(DocDetails.class, id.intValue())) return true;

        // The action endpoint uses -1 for actions without a selected record.
        if (entity == null) return Long.valueOf(-1L).equals(id);
        if (entity.getDocId()>0) {
            DocDetails original = DocDB.getInstance().getDoc(entity.getDocId(), -1, false);
            if (original == null) return false;
            if (InitServlet.isTypeCloud() && !GroupsDB.isGroupEditable(getUser(), original.getGroupId())) return false;
            // Check object access without requiring pageSave merely to open an editor or delete a page.
            return EditorService.hasPagePermission(getUser(), original);
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

        int groupId = getGroupIdParam();
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

    private int getGroupIdParam() {
        String groupIdParam = getRequest().getParameter(GROUP_ID_PARAM_NAME);
        int defaultId = Constants.getInt("rootGroupId");

        if (Tools.isEmpty(groupIdParam)) {
            //for multidomain use groupid by root group for domain
            int domainId = CloudToolsForCore.getDomainId();
            if (domainId > 1) {
                defaultId = domainId;
            }
        }

        return Tools.getIntValue(groupIdParam, defaultId);
    }
}
