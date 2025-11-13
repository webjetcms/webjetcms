package sk.iway.iwcm.editor.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.layout.LayoutService;
import sk.iway.iwcm.admin.settings.AdminSettingsService;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocBasic;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupEditorField;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.GroupsTreeService;
import sk.iway.iwcm.doc.HistoryDB;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefEntity;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.rest.GetAllItemsDocOptions;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyButton;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.SpecSearch;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

/**
 * Priprava zoznamu web stranok a pridruzenych ciselnikov pre DT.
 * Servis je potrebne konstruovat so zadanym group_id, podla neho sa nasledne vracaju data.
 */
public class WebpagesService {

    protected int groupId;
    private Prop prop;
	private GroupDetails localSystemGroup;

	public static final String DATA_NOT_LOADED = "data not loaded";
	private static final String ADMIN_SETTINGS_KEY = "jstreeSettings_web-pages-list";

	private static final String GROUP_ID = "groupId";
	private static final String ROOT_GROUP_ID = "rootGroupId";
	private static final String PASSWORD_PROTECTED = "passwordProtected";
	private static final String DOC_ID = "docId";
	private static final String TITLE = "title";
	private static final String USER_GROUP_ID = "userGroupId";

	private static final String MENU_TYPE_SAME_AS_NORMAL = "groupedit.menu_type_same_as_normal";
	private static final String MENU_TYPE_ONLYDEFAULT = "groupedit.menu_type_onlydefault";
	private static final String MENU_TYPE_HIDDEN = "groupedit.menu_type_hidden";

	public WebpagesService() {}

    public WebpagesService(int groupId, Identity user, Prop prop, HttpServletRequest request) {
        this.groupId = groupId;
		if (groupId<1) {
			//musime vydedukovat korenovy adresar
			//zoznam web stranok v korenovom/prvom adresari v zozname
			int rootGroupId = Constants.getInt(ROOT_GROUP_ID);
			//ziskaj zoznam root adresarov
			GroupsTreeService groupsTreeService = new GroupsTreeService();
			List<JsTreeItem> rootGroups = groupsTreeService.getItems(user, 0, false, "dt-tree-group-filter-system-trash", null, request);
			if (rootGroups.isEmpty()==false) {
				rootGroupId = Tools.getIntValue(rootGroups.get(0).getId(), rootGroupId);
			}
			this.groupId = rootGroupId;
		}
		this.prop = prop;
    }

	/**
	 * Ponecha v zozname len adresare z aktualne nastavenej domeny
	 * @param groups
	 * @return
	 */
	public static List<GroupDetails> filterGroupsByCurrentDomain(List<GroupDetails> groups) {
		List<GroupDetails> filtered = new ArrayList<>();
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		String currentDomain = null;
		if (rb != null) currentDomain = rb.getDomain();
		if (currentDomain == null) currentDomain = "";

		boolean hasLocalSystem = false;
		GroupDetails globalSystem = null;

		for (GroupDetails group : groups) {
			if (Tools.isEmpty(group.getDomainName()) || currentDomain.equals(group.getDomainName())) {

				if ("System".equals(group.getGroupName()) && group.getParentGroupId()<1) {
					//globalny system adresar ma typicky prazdne domenove meno, ak existuje lokalny system nechceme ten globalny mat v zozname
					if (Tools.isEmpty(group.getDomainName())) {
						globalSystem = group;
						continue;
					}
					else hasLocalSystem = true;
				}

				filtered.add(group);
			}
		}

		if (hasLocalSystem==false && globalSystem!=null) filtered.add(globalSystem);

		return filtered;
	}

	/**
	 * Vrati zoznam dostupnych sablon
	 * @param recursive - ak je true vratia sa aj sablony z podadresarov
	 * @return
	 */
    public List<TemplateDetails> getTemplates(boolean recursive) {

		List<TemplateDetails> allTemplates = new ArrayList<>();
		allTemplates.addAll(getTemplates(-1, recursive));
		List<TemplateDetails> templates = TemplatesDB.filterDeviceTemplates(allTemplates);

        return templates;
	}

	/**
	 * Vrati fiktivny korenovy adresar, je potrebny pre zobrazenie v stromovej strukture
	 * v editore ked je mozne vybrat aj korenovy adresar
	 * @return
	 */
	public static GroupDetails getRootGroup() {
		GroupEditorField groupEditorField = new GroupEditorField();

		//Set root parent group details into group editor field
		groupEditorField.setParentGroupDetails(null);

		// Create default root group and set editorFileds
		GroupDetails groupDetails = new GroupDetails();
		groupDetails.setGroupId(0);
		Prop prop = Prop.getInstance();
		groupDetails.setGroupName(prop.getText("stat_settings.group_id"));
		groupDetails.setFullPath("/");
		groupDetails.setEditorFields(groupEditorField);

		return groupDetails;
	}

	/**
	 * Vrati GroupDetails objekt podla zadaneho groupId
	 * @param groupId
	 * @return
	 */
	public static GroupDetails getGroup(int groupId) {
		if (groupId == 0) {
			return getRootGroup();
		} else {
			return GroupsDB.getInstance().getGroup(groupId);
		}
	}

	/**
	 * Vrati DocDetails z cache (BasicDoc), je mozne zadat aj -1 pre vratenie cisteho dokumentu
	 * @param docId
	 * @return
	 */
	public static DocDetails getBasicDoc(int docId) {
		DocDetails doc;
		if (docId < 1) {
			doc = new DocDetails();
            doc.setDocId(0);
            doc.setEditorFields(null);
            doc.setTitle(Prop.getInstance().getText("editor.json.pathNotSet"));
		} else {
			doc = DocDB.getInstance().getBasicDocDetails(docId, true);
		}
		return doc;
	}

	/**
	 * Vrati zoznam hlaviciek pre zobrazenie v DT/e
	 * @param addFromTemplateAndEmptyToSelect
	 * @return
	 */
    public List<DocDetails> getHeaderList(boolean addFromTemplateAndEmptyToSelect) {
		List<DocDetails> docs;
		GroupDetails localSystem = getLocalSystemGroup();
		if (localSystem != null){
			docs = getBasicDocDetailsByGroupRecursive(localSystem.getGroupId(), true);
		} else {
			docs = DocDB.getInstance().getDocByGroup(Constants.getInt("headerFooterGroupId"));
		}
		if (addFromTemplateAndEmptyToSelect) return addFromTemlateAndEmptyDoc(docs);
		return docs;
    }

	/**
	 * Vrati zoznam menu pre zobrazenie v DT/e
	 * @param addFromTemplateAndEmptyToSelect
	 * @return
	 */
    public List<DocDetails> getMenuList(boolean addFromTemplateAndEmptyToSelect) {
		List<DocDetails> docs;
		GroupDetails localSystem = getLocalSystemGroup();
		if (localSystem != null){
			docs = getBasicDocDetailsByGroupRecursive(localSystem.getGroupId(), true);
		} else {
			docs = DocDB.getInstance().getDocByGroup(Constants.getInt("menuGroupId"));
		}
		if (addFromTemplateAndEmptyToSelect) return addFromTemlateAndEmptyDoc(docs);
		return docs;
	}

	/**
	 * Vrati zoznam hlaviciek/paticiek pre zobrazenie v DT/e
	 * @param addFromTemplateAndEmptyToSelect
	 * @return
	 */
	public List<DocDetails> getHeaderFooterMenuList(boolean addFromTemplateAndEmptyToSelect) {
		List<DocDetails> docs;
		GroupDetails localSystem = getLocalSystemGroup();
		if (localSystem != null){
			docs = getBasicDocDetailsByGroupRecursive(localSystem.getGroupId(), true);
		} else {
			DocDB docDB = DocDB.getInstance();
			docs = docDB.getDocByGroup(Constants.getInt("headerFooterGroupId"));
			docs.addAll(docDB.getDocByGroup(Constants.getInt("menuGroupId")));
		}
		if (addFromTemplateAndEmptyToSelect) return addFromTemlateAndEmptyDoc(docs);
		return docs;
	}

	private List<DocDetails> addFromTemlateAndEmptyDoc(List<DocDetails> list)
	{
		addEmptyDoc(list, -2);
		addFromTemlateDoc(list);
		return list;
	}

	/**
	 * Do zoznamu DocDetails objektov prida na prvu poziciu fiktivny DocDetails
	 * s hodnotou "Zo sablony" a id -2
	 * @param list
	 * @return
	 */
    public List<DocDetails> addFromTemlateDoc(List<DocDetails> list)
	{
		DocDetails fromTemplate = new DocDetails();
		fromTemplate.setDocId(-1);
        fromTemplate.setTitle(prop.getText("editor.fromTemplate"));

		list.add(0, fromTemplate);

		return list;
    }

	/**
	 * Do zoznamu DocDetails objektov prida na prvu poziciu fiktivny DocDetails
	 * s hodnotou "Ziadna" a zadanym emptyDocId
	 * @param list
	 * @param emptyDocId - ID prazdneho dokumentu
	 * @return
	 */
	public List<DocDetails> addEmptyDoc(List<DocDetails> list, int emptyDocId)
	{
		DocDetails empty = new DocDetails();
		empty.setDocId(emptyDocId);
		empty.setTitle(prop.getText("editor.empyDoc"));

		list.add(0, empty);

		return list;
	}

	/**
	 * Ziska zoznam stranok z lokalneho system adresara vratane jeho podadresarov (PRVEJ UROVNE)
	 * @param groupId
	 * @param titleIncludePath - ak je true, bude vrateny objekt kopia povodneho a title bude upravene tak, ze obsahuje cestu (pre ciselniky)
	 * @return
	 */
	public static List<DocDetails> getBasicDocDetailsByGroupRecursive(int groupId, boolean titleIncludePath) {
		DocDB docDB = DocDB.getInstance();
		List<DocDetails> localDocsInGroup = filterUnavailableDocs(docDB.getBasicDocDetailsByGroup(groupId, -1));
		List<GroupDetails> subGroups = GroupsDB.getInstance().getGroups(groupId);
		Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
        String trashDirName = propSystem.getText("config.trash_dir");
		for (GroupDetails subGroup : subGroups) {
			//trash preskakujeme
			if (trashDirName.equals(subGroup.getGroupName())) continue;

			List<DocDetails> subDocs = docDB.getBasicDocDetailsByGroup(subGroup.getGroupId(), -1);
			for (DocDetails subDoc : subDocs) {
				if (subDoc.isAvailable()==false) continue;

				if (titleIncludePath) {
					//upravime meno, aby obsahovalo aj meno adresara, aby bolo zrejme od kial pochadza
					DocDetails menuDoc = new DocDetails();
					menuDoc.setDocId(subDoc.getDocId());
					menuDoc.setTitle(subGroup.getGroupName()+"/"+subDoc.getTitle());
					localDocsInGroup.add(menuDoc);
				} else {
					localDocsInGroup.add(subDoc);
				}
			}
		}
		return localDocsInGroup;
	}

	/**
	 * Vrati lokalny /System adresar
	 * @return
	 */
    private GroupDetails getLocalSystemGroup() {
		if (localSystemGroup == null && Constants.getBoolean("templatesUseDomainLocalSystemFolder")) {
			localSystemGroup = GroupsDB.getInstance().getLocalSystemGroup();
		}

		return localSystemGroup;
	}

	/**
	 * Vrati nastaveny GroupDetails objekt
	 * @return
	 */
	public GroupDetails getGroup() {
		if (groupId > 0) {
			return GroupsDB.getInstance().getGroup(groupId);
		}

		return null;
	}

	/**
	 * Vrati ciselnik pre moznost interny adresar
	 * @return
	 */
	public List<LabelValueDetails> getOptionsInternal() {
		List<LabelValueDetails> list = new ArrayList<>();
		list.add(new LabelValueDetails(prop.getText("editor.available-public"), "false"));
		list.add(new LabelValueDetails(prop.getText("editor.notavailable-notpublic"), "true"));
		return list;
	}

	/**
	 * Vrati ciselnik pre zoznam sablon
	 */
	public List<LabelValueDetails> getOptionsTemplates(UserDetails currentUser, GroupDetails group) {
		TemplatesDB templatesDB = TemplatesDB.getInstance();
		List<TemplateDetails> allTemps;
		if (group != null) allTemps = templatesDB.getTemplates(group.getGroupId(), group.getTempId());
		else allTemps = templatesDB.getTemplatesSaved();

		List<TemplateDetails> templateDetailsList = TemplatesDB.filterTemplatesByUser(currentUser, allTemps);
		List<LabelValueDetails> templateNames = new ArrayList<>();

		templateDetailsList.forEach(templateDetails ->
			templateNames.add(new LabelValueDetails(templateDetails.getTempName(), String.valueOf(templateDetails.getTempId())))
		);

		return templateNames;
	}

	/**
	 * Vrati ciselnik pre vyber jazyka
	 */
	public List<LabelValueDetails> getOptionsLanguages(HttpServletRequest request) {
		LayoutService ls = new LayoutService(request);
		List<LabelValueDetails> incompleteLanguages = ls.getLanguages(false, true);
		List<LabelValueDetails> finalLanguages = new ArrayList<>();

		finalLanguages.add(new LabelValueDetails(prop.getText("groupedit.lng.default"), ""));
		finalLanguages.addAll(incompleteLanguages);

		return finalLanguages;
	}

	/**
	 * Vrati ciselnik pre vyber novej stranky
	 * @return
	 */
	public List<LabelValueDetails> getOptionsNewPageHTMLCode() {
		List<LabelValueDetails> list = new ArrayList<>();
		list.add(new LabelValueDetails(prop.getText("groupedit.new_page_template.empty"), "-1"));
		DocDB docDB = DocDB.getInstance();
		List<DocDetails> pageTemps = docDB.getDocByGroup(Constants.getInt("tempGroupId"));
		for (DocDetails pageTemp : pageTemps) {
			list.add(new LabelValueDetails(pageTemp.getTitle(), String.valueOf(pageTemp.getDocId())));
		}
		return list;
	}

	/**
	 * Vrati ciselnik pre moznosti navigacnej listy
	 * @param isLogged
	 * @return
	 */
	public List<LabelValueDetails> getOptionsNavbar(boolean isLogged) {
		List<LabelValueDetails> list = new ArrayList<>();
		if (isLogged) list.add(new LabelValueDetails(prop.getText(MENU_TYPE_SAME_AS_NORMAL), "null"));
		else list.add(new LabelValueDetails(prop.getText("editor.navbar.same_as_menu"), "null"));

		list.add(new LabelValueDetails(prop.getText(MENU_TYPE_ONLYDEFAULT), String.valueOf(GroupDetails.MENU_TYPE_ONLYDEFAULT)));
		list.add(new LabelValueDetails(prop.getText(MENU_TYPE_HIDDEN), String.valueOf(GroupDetails.MENU_TYPE_HIDDEN)));
		//list.add(new LabelValueDetails(prop.getText("groupedit.menu_type_nosub"), String.valueOf(GroupDetails.MENU_TYPE_NOSUB)));
		//list.add(new LabelValueDetails(prop.getText("groupedit.menu_type_normal"), String.valueOf(GroupDetails.MENU_TYPE_NORMAL)));
		return list;
	}

	/**
	 * Vrati ciselnik pre moznost mapy stranok
	 * @param isLogged
	 * @return
	 */
	public List<LabelValueDetails> getOptionsSitemap(boolean isLogged) {
		List<LabelValueDetails> list = new ArrayList<>();
		if (isLogged) list.add(new LabelValueDetails(prop.getText(MENU_TYPE_SAME_AS_NORMAL), "null"));
		else list.add(new LabelValueDetails(prop.getText("editor.navbar.same_as_menu"), "null"));

		list.add(new LabelValueDetails(prop.getText(MENU_TYPE_ONLYDEFAULT), String.valueOf(GroupDetails.MENU_TYPE_ONLYDEFAULT)));
		list.add(new LabelValueDetails(prop.getText(MENU_TYPE_HIDDEN), String.valueOf(GroupDetails.MENU_TYPE_HIDDEN)));
		list.add(new LabelValueDetails(prop.getText("groupedit.menu_type_nosub"), String.valueOf(GroupDetails.MENU_TYPE_NOSUB)));
		list.add(new LabelValueDetails(prop.getText("groupedit.menu_type_normal"), String.valueOf(GroupDetails.MENU_TYPE_NORMAL)));
		return list;
	}

	/**
	 * Vrati ciselnik pre sposob zobrazenia v menu
	 * @param isLogged
	 * @return
	 */
	public List<LabelValueDetails> getMenuType(boolean isLogged) {
		List<LabelValueDetails> list = new ArrayList<>();
		if (isLogged) list.add(new LabelValueDetails(prop.getText(MENU_TYPE_SAME_AS_NORMAL), "-1"));
		list.add(new LabelValueDetails(prop.getText(MENU_TYPE_ONLYDEFAULT), String.valueOf(GroupDetails.MENU_TYPE_ONLYDEFAULT)));
		list.add(new LabelValueDetails(prop.getText(MENU_TYPE_HIDDEN), String.valueOf(GroupDetails.MENU_TYPE_HIDDEN)));
		list.add(new LabelValueDetails(prop.getText("groupedit.menu_type_nosub"), String.valueOf(GroupDetails.MENU_TYPE_NOSUB)));
		list.add(new LabelValueDetails(prop.getText("groupedit.menu_type_normal"), String.valueOf(GroupDetails.MENU_TYPE_NORMAL)));
		return list;
	}

	/**
	 * Vrati zoznam PerexGroupBean objektov
	 * @param recursive - ak je nastavene na true vrati aj PerexGroupBean z podadresarov
	 * @return
	 */
	public List<OptionDto> getPerexGroups(boolean recursive) {
		List<PerexGroupBean> perexGroups = DocDB.getInstance().getPerexGroups(groupId, recursive);
		return DocDB.fixPerexNameDuplicityForOptions(perexGroups);
	}

	/**
	 * Pri zmene atributu nastavi jeho hodnotu do databazy dynamickym SQL prikazom
	 * @param groupId - ID adresara
	 * @param attributeName - meno DB stlpca
	 * @param attributeValue - hodnota
	 */
	public void  setAttributeToSubgroups(int groupId, String attributeName, Object attributeValue){
		GroupsDB groupsDB = GroupsDB.getInstance();
        Connection db_conn = DBPool.getConnection();
        PreparedStatement ps = null;
        String groupIds = groupsDB.getSubgroupsIds(groupId);

        Adminlog.add(Adminlog.TYPE_GROUP, "Force "+DB.removeSlashes(attributeName)+" to subgroups: " + groupIds + " value=" + attributeValue, groupId, -1);

        try {
			ps = db_conn.prepareStatement("UPDATE groups SET " + DB.removeSlashes(attributeName) + "=? WHERE group_id IN (" + groupIds + ")");

			if (attributeValue instanceof String string) ps.setString(1, string);
			else if (attributeValue instanceof Integer integer) ps.setInt(1, integer);
			else if (attributeValue instanceof Boolean bool) ps.setBoolean(1, bool);
			else ps.setObject(1, attributeValue);

            ps.execute();
        } catch (Exception ex) {
            Logger.error(WebpagesService.class, ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (db_conn != null)
                    db_conn.close();
            } catch (Exception ex) {
                Logger.error(WebpagesService.class, ex);
            }
		  }

		  //mame zmeny je najlepsie refreshnut celu GroupsDB
		  GroupsDB.getInstance(true);
    }

	/**
	 * Pregeneruje URL adresy stranok v zadanom adresari
	 * @param rootGroupId
	 * @param user
	 * @param request
	 * @param onlyChangeUrlInheritGroup //Default false, ak je true bude vykonane pregenerovanie iba pre tie kt. maju urlInheritGroup = true
	 */
	public static void regenerateUrl(int rootGroupId, Identity user, HttpServletRequest request, boolean onlyChangeUrlInheritGroup)
	{
		//ziskaj zoznam stranok v adresari
		List<DocDetails> docs = DocDB.getInstance().getDocByGroup(rootGroupId);
		EditorForm ef;
		for (DocDetails doc : docs)
		{

			if(onlyChangeUrlInheritGroup) {
				//Ak ma doc urlInheritGroup != true preskoc tento doc a nepregeneruj url
				if(Boolean.TRUE.equals(doc.getUrlInheritGroup())==false && Boolean.TRUE.equals(doc.getGenerateUrlFromTitle())==false) {
					continue;
				}
			}

			ef = EditorDB.getEditorForm(request, doc.getDocId(), -1, rootGroupId);
			//out.println("<strong>"+ef.getTitle()+"</strong> [docid:"+ef.getDocId()+"] - "+ef.getVirtualPath());

			if (ef.getVirtualPath().contains("*"))
			{
				//out.println(" skipping (contains *)<br/>");
				continue;
			}

			ef.setVirtualPath("");

			if (Boolean.TRUE.equals(doc.getUrlInheritGroup())) ef.setVirtualPath(doc.getEditorVirtualPath());

			//nastav aktualneho usera
			ef.setAuthorId(user.getUserId());
			ef.setPublish("1");

			EditorDB.saveEditorForm(ef, request);

			//out.println(" -> " + ef.getVirtualPath()+"<br>");

			EditorDB.cleanSessionData(request);

			//out.flush();
		}

		//out.flush();

		//rekurzivne sa zavolaj na podadresare
		List<GroupDetails> subGroups = GroupsDB.getInstance().getGroups(rootGroupId);
		for (GroupDetails group : subGroups)
		{
			regenerateUrl(group.getGroupId(), user, request, onlyChangeUrlInheritGroup);
		}
		//out.flush();
	}

	/**
	 * Zo zoznamu DocDetails odstrani objekty, ktore nie su dostupne na zobrazenie (available=false)
	 * @param original
	 * @return
	 */
	private static List<DocDetails> filterUnavailableDocs(List<DocDetails> original) {
		List<DocDetails> filtered = new ArrayList<>();
		for (DocDetails doc : original) {
			if (doc.isAvailable()==false) continue;
			filtered.add(doc);
		}
		return filtered;
	}

	/**
	 * Return list of groups (from this domain) that use specific template.
	 * @param tempId id of template that group must use
	 * @return list of groups
	 */
	public List<GroupDetails> getGroupsByTemplateId(int tempId) {

		GroupsDB groupsDB = GroupsDB.getInstance();
		List<GroupDetails> result = new ArrayList<>();

		for(GroupDetails group : groupsDB.getGroupsAll()) {
			if(group.getTempId() == tempId)
				result.add(group);
		}

		return filterGroupsByCurrentDomain(result);
	}

	/**
	 * Vrati zoznam adresarov podla zadanej skupiny pouzivatelov
	 * @param userGroupId
	 * @return
	 */
	public List<GroupDetails> getGroupsByPasswordProtected(int userGroupId) {

		GroupsDB groupsDB = GroupsDB.getInstance();
		List<GroupDetails> result = new ArrayList<>();

		for(GroupDetails group : groupsDB.getGroupsAll()) {

			//Its string of user_groups ids, separated by column
			String passwordProtected = group.getPasswordProtected();
			if (Tools.isEmpty(passwordProtected)) continue;

			//Split string to get ids
			String[] idsArray = passwordProtected.split(",");

			//Loop array of ids, and if id from passwordProtected if same as userGroupId push group to result list
			for(int i = 0; i < idsArray.length; i++) {

				if(Integer.parseInt(idsArray[i]) == userGroupId) {
					result.add(group);
				}
			}
		}

		return filterGroupsByCurrentDomain(result);
	}

	/**
	 * Vrati JPA podmienku pre ziskanie naposledy upravenych stranok
	 * @param userId
	 * @return
	 */
	private static Specification<DocDetails> getRecentPagesConditions(int userId) {
		return (Specification<DocDetails>) (root, query, builder) -> {
			final List<Predicate> predicates = new ArrayList<>();

			int domainId = CloudToolsForCore.getDomainId();
			Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
			String trashDirName = propSystem.getText("config.trash_dir");

			predicates.add(builder.equal(root.get("authorId"), userId));
			predicates.add(builder.notLike(root.get("virtualPath"), "/files/%"));
			//toto uz nedavame, chceme zobrazit aj rozpracovane/vypnute stranky predicates.add(builder.isTrue(root.get("available")));

			if (InitServlet.isTypeCloud()) predicates.add(builder.equal(root.get("rootGroupL1"), domainId));
			else if (domainId > 0) {
				//ziskaj zoznam ROOT adresarov v zadanej domene
				GroupsDB groupsDB = GroupsDB.getInstance();
				GroupDetails domainGroup = groupsDB.getGroup(domainId);
				if (domainGroup != null && Tools.isNotEmpty(domainGroup.getDomainName())) {
					List<GroupDetails> rootGroups = groupsDB.getGroups(0);
					List<Integer> groupIds = new ArrayList<>();
					for (GroupDetails rootGroup : rootGroups) {
						if (rootGroup.getDomainName().equalsIgnoreCase(domainGroup.getDomainName())==false) continue;

						groupIds.add(rootGroup.getGroupId());
					}
					if (groupIds.isEmpty()==false) {
						predicates.add(root.get("rootGroupL1").in(groupIds));
					}

					//Vylúč všetky kôš adresáre
					List<Integer> groupIdsTrash = new ArrayList<>();
					for (GroupDetails group : groupsDB.getGroupsAll()) {
						if (trashDirName.equals(group.getFullPath())==false) continue;

						groupIdsTrash.add(group.getGroupId());
					}
					if (groupIdsTrash.isEmpty()==false) {
						predicates.add(builder.not(root.get(GROUP_ID).in(groupIdsTrash)));
						predicates.add(builder.not(root.get("rootGroupL2").in(groupIdsTrash)));
					}
				}
			}
			//Set order
			query.orderBy(builder.desc(root.get("dateCreated")));

			return builder.and(predicates.toArray(new Predicate[predicates.size()]));
		};
	}

	/**
	 * Vrati JPA podmienku pre zobrazenie podla zadaneho groupId a pripadne rekurzivneho zobrazenia
	 * @param groupId
	 * @param recursive
	 * @return
	 */
	public static List<Predicate> getGroupIdCondition(int groupId, boolean recursive, Root<DocDetails> root, CriteriaBuilder builder) {
		final List<Predicate> predicates = new ArrayList<>();

		if (recursive) {
			GroupsDB groupsDB = GroupsDB.getInstance();
			List<GroupDetails> subGroups = groupsDB.getGroupsTree(groupId, true, true);
			List<Integer> groupIds = subGroups.stream().map(GroupDetails::getGroupId).toList();
			if (groupIds.size()==1) {
				//ak sa jedna o posledny uzol daj to ako klasicku where kvoli efektivite
				predicates.add(builder.equal(root.get(GROUP_ID), groupIds.get(0)));
			} else if (groupIds.isEmpty()==false) {
				predicates.add(root.get(GROUP_ID).in(groupIds));
			}
		} else if (groupId>0)  {
			predicates.add(builder.equal(root.get(GROUP_ID), groupId));
		}
		return predicates;
	}

	/**
	 * Vykona nastavenie EditorFields atributov pred vratenim z REST rozhrania
	 * @param entity
	 * @param action
	 * @param request
	 * @return
	 */
    public static DocBasic processFromEntity(DocBasic entity, ProcessItemAction action,  HttpServletRequest request, boolean addFields) {

        int groupId = Tools.getIntValue(request.getParameter(GROUP_ID), Constants.getInt(ROOT_GROUP_ID));

        if (ProcessItemAction.GETONE.equals(action) && entity==null) {
            entity = new DocDetails();
        }

        if(entity != null) {

			if (ProcessItemAction.GETONE.equals(action)==false) entity.setData(DATA_NOT_LOADED);

			//Get doc author
			UserDetails user = UsersDB.getUser(entity.getAuthorId());
			if (user != null) {
				entity.setAuthorName(user.getFullName());
				entity.setAuthorEmail(user.getEmail());
				entity.setAuthorPhoto(user.getPhoto());
			} else {
				entity.setAuthorName("");
				entity.setAuthorEmail("");
				entity.setAuthorPhoto("");
			}

			if(groupId == Constants.getInt("systemPagesRecentPages")) {
				//There is no need edit for this doc's
			} else {
				boolean linkTypeHtml = false;
				if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML) {
					linkTypeHtml = true;
				}

				if (linkTypeHtml) {
					entity.setDocLink(entity.getVirtualPath());
				}

				//toto nerobime, lebo tam ma byt skutocne hodnota z DB entity.setNavbar(groupsDB.getNavbarNoHref(entity.getGroupId()));
			}

            DocEditorFields def = entity.getEditorFields();
			if (def == null) def = new DocEditorFields();
			boolean loadSubQueries = ProcessItemAction.GETONE.equals(action);
			def.fromDocDetails(entity, loadSubQueries, addFields);
			entity.setEditorFields(def);
        }
        return entity;
    }

	/**
	 * Vrati Templates dostupne pre dane groupId
	 */
	public List<TemplateDetails> getTemplates(int mustHaveTempId, boolean recursive)
	{
		//najskor potrebujeme zoznam parent skupin
		GroupsDB groupsDB = GroupsDB.getInstance();
		List<GroupDetails> parentGroups = groupsDB.getParentGroups(groupId);

		List<TemplateDetails> allTemps = TemplatesDB.getInstance().getTemplates();

	   Set<TemplateDetails> ret = new HashSet<> ();

	   //Loop all Templates and for each one call isGroupAvailable method (method will return true if this Template is availaible)
	   for (TemplateDetails temp : allTemps) {

		  if (temp.getTempId()<1) continue;

		  //List of Group Ids where temp is available
		  int[] tempAvailableGroups = temp.getAvailableGroupsInt();

		  //isGroupAvailable param "recursive" is set for getting Templates for child (all subfolders - if recursive is true)
		  if (tempAvailableGroups.length == 0 || groupId == -1 || temp.getTempId() == mustHaveTempId || isGroupAvailable(tempAvailableGroups, parentGroups, recursive)) {
			  ret.add(temp);
		  }
	   }

	   if (ret.isEmpty()) {
		   ret.add(allTemps.get(0));
	   }

	   List<TemplateDetails> sortedRet = ret.stream().sorted((e1, e2) -> e1.getTempName().compareTo(e2.getTempName())).toList();

	   return (new ArrayList<TemplateDetails>(sortedRet));
	}

	/**
	 * availableGroups - array of int Ids represent in which groups is Template available
	 * groups - list of parent groups of actual selected group in jsTree
	 * recursive - True (check if Template is available not only for parent groups but also for child group (konjuction) of selected group in jsTree)
	 */
	private boolean isGroupAvailable(int[] availableGroups, List<GroupDetails> groups, boolean recursive)
	{
		//check availability for parent groups
		for (int availableGroupId : availableGroups) {
			for (GroupDetails group : groups) {
				if (group.getGroupId() == availableGroupId) return true;
			}
		}

		//check availability for child groups
		if(recursive) {

			GroupsDB groupsDB = GroupsDB.getInstance();

			for (int availableGroupId : availableGroups) {

				int[] tmpParentGroupIds = Tools.getTokensInt(groupsDB.getParents(availableGroupId), ",");

				for(int tmpParentGroupId : tmpParentGroupIds) {
					if(tmpParentGroupId == groupId) return true;
				}
			}
		}

		return false;
	}

	/**
	 * Vrati zoznam web stranok podla zadanych kriterii v options objekte pre pouzitie v DT
	 * @param options
	 * @return
	 */
    public static DatatablePageImpl<DocDetails> getAllItems(GetAllItemsDocOptions options) {
        Page<DocDetails> page = null;

		if(options.getGroupId() == Constants.getInt("systemPagesRecentPages")) {
			Specification<DocDetails> spec = WebpagesService.getRecentPagesConditions(options.getUserId());
			//Combine spec (recent pages) with columnsSpecification (serch throu columns)
			if(options.getColumnsSpecification() != null) {
				spec = spec.and(options.getColumnsSpecification());
			}
			page = options.getDocDetailsRepository().findAll(spec, options.getPageable());
		} else if(options.isUserGroupIdRequested()) {
            //chceme vratit stranky podla zadaneho ID skupiny pouzivatelov, pouziva sa na zoznam stranok s danou skupinou
            page = options.getDocDetailsRepository().findAllByPasswordProtectedLike(""+options.getUserGroupId(), options.getUserGroupId()+",%", "%,"+options.getUserGroupId(), "%," + options.getUserGroupId() + ",%", options.getPageable());
        } else if(options.isTempIdRequested()) {
			//We want to return web pages that use specific template (by tempId)
			page = options.getDocDetailsRepository().findAllByTempId(options.getTempId(), options.getPageable());
		} else if("true".equals(options.getRequest().getParameter("auditVersion"))) {
			/** We want all web pages sorted by date of change **/

			if(!options.getCurrentUser().isEnabledItem("cmp_adminlog")) throw new IllegalArgumentException("Access is denied");
			page = options.getDocDetailsRepository().findAllByOrderByDateCreatedDesc(options.getPageable());

		} else {
			if (GroupsDB.isGroupEditable(options.getCurrentUser(), options.getGroupId())) {
				Map<String, String> params = new HashMap<>();
				if (options.getRequest()!=null) {
					params.putAll(DatatableRestControllerV2.getParamsMap(options.getRequest()));
				}
				//override groupId from options
				params.put(GROUP_ID, "" + options.getGroupId());

				@SuppressWarnings("java:S1602")
				Specification<DocDetails> spec = (Specification<DocDetails>) (root, query, builder) -> {
					final List<Predicate> predicates = new ArrayList<>();
					addSpecSearch(params, predicates, root, builder, options.getCurrentUser());
					return builder.and(predicates.toArray(new Predicate[predicates.size()]));
				};

				page = options.getDocDetailsRepository().findAll(spec, options.getPageable());
			} else {
				//pridaj stranky ak ma specialne nastavene z tohto adresara
				if (Tools.isNotEmpty(options.getCurrentUser().getEditablePages())) {
					List<DocDetails> docs = UserTools.getEditablePages(options.getCurrentUser().getEditablePages());
					List<DocDetails> availableDocs = new ArrayList<>();
            		for (DocDetails doc : docs) {
						if (doc.getGroupId() == options.getGroupId()) availableDocs.add(doc);
					}
					if (availableDocs.isEmpty()==false) page = new DatatablePageImpl<>(availableDocs);
				}
			}
		}

		return preparePage(page, options);
    }

	public static DatatablePageImpl<DocDetails> preparePage(Page<DocDetails> page, GetAllItemsDocOptions options) {
        DatatablePageImpl<DocDetails> pageImpl;

		if (page != null) pageImpl = new DatatablePageImpl<>(page);
		else pageImpl = new DatatablePageImpl<>(new ArrayList<>());

        addOptions(pageImpl, options);

        return pageImpl;
    }

	/**
	 * Add options to DatatablePage object
	 * @param pageImpl - current response Page object
	 * @param options - options object
	 */
	public static void addOptions(DatatablePageImpl<DocDetails> pageImpl, GetAllItemsDocOptions options) {
		addOptions(pageImpl, options, false);
	}

	/**
	 * Add options to DatatablePage object
	 * @param pageImpl - current response Page object
	 * @param options - options object
	 * @param forceGroupId - if true, force options.groupId to WebpagesService even if it's < 1
	 */
	public static void addOptions(DatatablePageImpl<DocDetails> pageImpl, GetAllItemsDocOptions options, boolean forceGroupId) {
		Prop prop = Prop.getInstance(options.getRequest());
        WebpagesService ws = new WebpagesService(options.getGroupId(), options.getCurrentUser(), prop, options.getRequest());
		if (forceGroupId) ws.groupId = options.getGroupId();

		pageImpl.addOptions("tempId", ws.getTemplates(options.isRecursiveSubfolders()), "tempName", "tempId", true);
        pageImpl.addOptions("menuDocId,rightMenuDocId", ws.getMenuList(true), TITLE, DOC_ID, false);
        pageImpl.addOptions("headerDocId,footerDocId", ws.getHeaderList(true), TITLE, DOC_ID, false);
		pageImpl.addOptions("tempFieldADocId,tempFieldBDocId,tempFieldCDocId,tempFieldDDocId", ws.getHeaderFooterMenuList(true), TITLE, DOC_ID, false);
        pageImpl.addOptions("editorFields.emails", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_EMAIL), "userGroupName", USER_GROUP_ID, false);
        pageImpl.addOptions("editorFields.permisions", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS), "userGroupName", USER_GROUP_ID, false);
        pageImpl.addOptions("perexGroups", ws.getPerexGroups(options.isRecursiveSubfolders()), "label", "value", false);

		//optiony pre ikonu
		pageImpl.addOptions("editorFields.statusIcons", getStatusIconOptions(options, prop), "label", "value", false);

		//attributes group
		if (options.getDocAtrDefRepository()!=null) {
			pageImpl.addOptions("editorFields.attrGroup", options.getDocAtrDefRepository().findDistinctGroups(CloudToolsForCore.getDomainId()) , "", "", false);
		}

		boolean addFields = true;
        for (DocDetails entity : pageImpl.getContent()) {
			WebpagesService.processFromEntity(entity, ProcessItemAction.GETALL, options.getRequest(), addFields);
			addFields = false;
		}
	}

	/**
	 * Vrati option pre DT so zoznamom stavovych ikon
	 * riesi kontrolu prav na app abtesting (ikony sa zobrazia len ak ma pouzivatel pravo)
	 * @param options
	 * @param prop
	 * @return
	 */
	private static List<LabelValue> getStatusIconOptions(GetAllItemsDocOptions options, Prop prop) {
		List<LabelValue> icons = new ArrayList<>();

		icons.add(new LabelValue("<i class=\"ti ti-star\"></i> "+prop.getText("editor.main_site"), "searchDefaultPage"));
		icons.add(new LabelValue("<i class=\"ti ti-map-pin\"></i> "+prop.getText("webpages.icons.showInMenu"), "showInMenu:true"));
		icons.add(new LabelValue("<i class=\"ti ti-map-pin-off\"></i> "+prop.getText("webpages.icons.notShowInMenu"), "showInMenu:false"));
		icons.add(new LabelValue("<i class=\"ti ti-lock\"></i> "+prop.getText("webpages.icons.onlyForLogged"), "passwordProtected:notEmpty"));
		icons.add(new LabelValue("<span style=\"color: #FF4B58\">"+prop.getText("webpages.icons.disabled")+"</span>", "available:false"));
		icons.add(new LabelValue("<i class=\"ti ti-external-link\"></i> "+prop.getText("webpages.icons.externalLink"), "externalLink:notEmpty"));
		icons.add(new LabelValue("<i class=\"ti ti-eye-off\"></i> "+prop.getText("webpages.icons.notSearchable"), "searchable:false"));

		if (options.getCurrentUser().isEnabledItem("cmp_abtesting")) {
			icons.add(new LabelValue("<i class=\"ti ti-a-b\"></i> "+prop.getText("webpages.icons.avariant"), "virtualPath:!%"+Constants.getString("ABTestingName")+"%"));
			icons.add(new LabelValue("<i class=\"ti ti-a-b\"></i> "+prop.getText("webpages.icons.bvariant"), "virtualPath:%"+Constants.getString("ABTestingName")+"%"));
		}

		return icons;
	}

	/**
	 * Vrati DocDetails (ako docDB.getBasicDocDetails) zo zadanej URL adresy
	 * Ta moze byt v tvare:
	 * http://domena.sk/adresar/stranka.html?nejakyParameter=aaa
	 * http://domena.sk/showdoc.do?docid=xxx&amp;nejakyParameter=aaa
	 * /adresar/stranka.html
	 * /showdoc.do?docid=xxx
	 * @param url
	 * @return
	 */
	public static DocDetails getBasicDocFromUrl(String url) {
		DocDetails doc = null;
		try {
			if (Tools.isNotEmpty(url)) {
				if (url.startsWith("http")==false) {
					url = "http://"+CloudToolsForCore.getDomainName()+url;
				}
				DocDB docDB = DocDB.getInstance();
				int to = url.indexOf("/", 8);
				if (to==-1) to = url.indexOf(":", 8);
				if (to==-1) to = url.indexOf("?", 8);

				String domainName = url.substring(url.indexOf("://")+3, to);
				int portDelimiter = domainName.indexOf(":");
				if (portDelimiter > 0) domainName = domainName.substring(0, portDelimiter);

				to = url.indexOf("/", 8);
				String path = "/";
				if (to>0) path = url.substring(to);
				path = Tools.replace(path, "//", "/");

				int docId = -1;
				if (path.startsWith("/showdoc.do")) {
					docId = Tools.getIntValue(Tools.getParameterFromUrl(path, "docid"), -1);
				} else {
					String pathNoParams = path;
					int i = pathNoParams.indexOf("?");
					if (i>0) pathNoParams = pathNoParams.substring(0, i);

					docId = docDB.getDocIdFromURLImpl(pathNoParams, domainName);
				}
				if (docId > 0) {
					doc = docDB.getBasicDocDetails(docId, false);
				}
			}
		} catch (Exception e) {
			Logger.error(WebpagesService.class, e);
		}
		return doc;
	}

	/**
	 * Overi, ci zadany pouzivatel ma zapnute zobrazovanie web stranok v stromovej strukture
	 * @param user
	 * @return
	 */
	public static boolean isTreeShowPages(UserDetails user) {
		//ak je zapnute zobrazenie zoznamu stranok pre novu stranku musim spravit reload
        //ostatne ako zmena adresara vyvolava reload uz standardne
        AdminSettingsService ass = new AdminSettingsService(user);
        return ass.getJsonBooleanValue(ADMIN_SETTINGS_KEY, "showPages");
	}

	public static int getUserFirstEditableGroup(Identity user)
	{
		int[] editableGroups = Tools.getTokensInt(user.getEditableGroups(), ",");
		if (editableGroups!=null && editableGroups.length>0)
		{
			for (int groupId : editableGroups)
			{
				if (groupId > 0)
				{
					return groupId;
				}
			}
		}
		return -1;
	}

	/**
	 * Vrati posledne zapamatane groupId pre daneho pouzivatela, alebo prve jeho nastavene podla prav, alebo defaultne
	 * @param user
	 * @param request
	 * @return
	 */
	public static int getUserLastGroupId(Identity user, HttpServletRequest request)
	{
		HttpSession session = request.getSession();

		int group_id = Constants.getInt(ROOT_GROUP_ID);

		int groupId = getUserFirstEditableGroup(user);
		if (groupId > 0)
		{
			group_id = groupId;
		}

		try
		{
			if (request.getParameter("groupid") != null)
			{
				group_id = Integer.parseInt(request.getParameter("groupid"));
			}
			else
			{
				//skus ziskat data zo session
				if (session.getAttribute(Constants.SESSION_GROUP_ID) != null)
				{
					group_id = Integer.parseInt((String) session.getAttribute(Constants.SESSION_GROUP_ID));
				}

			}
		}
		catch (Exception ex)
		{

		}

		return group_id;
	}

	/**
	 * Get and process params as searchAuthorName, userGroupId etc.
	 * @param specSearch
	 * @param params
	 * @param predicates
	 * @param root
	 * @param builder
	 * @return String value of processed param groupIdList
	 */
	public static String addBaseSpecSearch(SpecSearch<DocDetails> specSearch, Map<String, String> params, List<Predicate> predicates, Root<DocDetails> root, CriteriaBuilder builder) {
		//remove groupId predicate which was auto binded, it will be set later in this method depending on recursive attribute
		JpaTools.removePredicateWithName(GROUP_ID, predicates);

        //vyhladanie na zaklade Meno autora, hladane v DB tabulke nasledne v stlpci authorId
        String searchAuthorName = params.get("searchAuthorName");
        if (searchAuthorName != null)
            specSearch.addSpecSearchUserFullName(searchAuthorName, "authorId", predicates, root, builder);

        String permissions = params.get("searchEditorFields.permisions");
        if (permissions != null)
            specSearch.addSpecSearchPasswordProtected(permissions, PASSWORD_PROTECTED, predicates, root, builder);

        String emails = params.get("searchEditorFields.emails");
        if (emails != null)
            specSearch.addSpecSearchPasswordProtected(emails, PASSWORD_PROTECTED, predicates, root, builder);

        int userGroupId = Tools.getIntValue(params.get(USER_GROUP_ID), -1);
        if (userGroupId > 0)
            specSearch.addSpecSearchPasswordProtected(userGroupId, PASSWORD_PROTECTED, predicates, root, builder);

		String groupIdListParam = params.get("groupIdList");
		if (Tools.isEmpty(groupIdListParam) && Tools.isNotEmpty(params.get(GROUP_ID))) {
			groupIdListParam = params.get(GROUP_ID);
			if ("true".equals(params.get("recursive"))) groupIdListParam+="*";
		}

		return groupIdListParam;
	}

	/**
	 * Add special conditions to search query based on request parameters
	 */
	public static void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<DocDetails> root, CriteriaBuilder builder, Identity user) {

        SpecSearch<DocDetails> specSearch = new SpecSearch<>();
        GroupsDB groupsDB = GroupsDB.getInstance();

		String groupIdListParam = addBaseSpecSearch(specSearch, params, predicates, root, builder);

		String[] groupIdListArray = Tools.getTokens(groupIdListParam, ",", true);
		int groupId;
		if (groupIdListArray.length>0) {
			List<Integer> groupIds = new ArrayList<>();
			for (String id : groupIdListArray) {
				if (id.endsWith("*") && id.length()>1) {

					groupId = Tools.getIntValue(id.substring(0, id.length()-1), -1);
					GroupDetails baseGroup = groupsDB.getGroup(groupId);
					//to filter FullTextIndex of files
					final boolean baseGroupIsFiles = baseGroup.getFullPath().contains("/files");
					List<GroupDetails> subGroups = groupsDB.getGroupsTree(groupId, true, true);

					groupIds.addAll(subGroups.stream()
						.filter(g -> baseGroupIsFiles || !g.getFullPath().contains("/files"))
						.map(g -> g.getGroupId())
						.toList());

				} else {

					groupIds.add(Tools.getIntValue(id, -1));

				}
			}
			if (groupIds.size()==1) {
				predicates.add(builder.equal(root.get(GROUP_ID), groupIds.get(0)));
			} else if (groupIds.size()>1) {
				predicates.add(root.get(GROUP_ID).in(groupIds));
			}

			//filter iba hlavnych stranok adresarov
			String searchStatusIcon = params.get("searchEditorFields.statusIcons");
			if ("searchDefaultPage".equals(searchStatusIcon) && groupIds.size()>0) {
				//ziskaj zoznam default_doc_id pre zvolene adresare
				String ids = groupIds.stream().map(String::valueOf).collect(Collectors.joining(","));
				List<Integer> defaultDocIds = (new SimpleQuery()).forListInteger("SELECT DISTINCT default_doc_id FROM groups WHERE group_id IN ("+ids+")");
				//pridaj to ako predikat
				predicates.add(root.get("id").in(defaultDocIds));
			}
		}
    }

	public static DocDetails getOneItem(long id, int groupId, int historyId, EditorFacade editorFacade, DocAtrDefRepository docAtrDefRepository, List<NotifyBean> notifyList, HttpServletRequest request) {
		if (groupId == Constants.getInt("systemPagesDocsToApprove")) {
            //pre tento pripad mame otocene docid a historyid, ale principialne dostavame v id hodnotu historyid, takze to potrebujeme takto nacitat
            historyId = (int)id;
            //ziskaj docid podla historyid
            id = (new SimpleQuery()).forInt("SELECT doc_id FROM documents_history WHERE history_id=?", historyId);
        }

        Prop prop = Prop.getInstance(request);

		DocDetails doc = editorFacade.getDocForEditor((int) id, historyId, groupId);

        if(id == -1) {
            doc.setGenerateUrlFromTitle(true);
        }

        if (ContextFilter.isRunning(request)) {
            // do editoru nahrame texty s pridanymi linkami
            doc.setData(ContextFilter.addContextPath(request.getContextPath(), doc.getData()));
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
                notifyList.add(notify);
            }
            request.getSession().removeAttribute("docHistory");
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

        String newPageTitleKey = request.getParameter("newPageTitleKey");
        if (Tools.isNotEmpty(newPageTitleKey) && doc.getDocId()<1) {
            doc.setTitle(prop.getText(newPageTitleKey));
        }

        return doc;
	}

	public static String getTreeSortType(UserDetails user) {
        AdminSettingsService ass = new AdminSettingsService(user);
		String sortType = ass.getJsonValue(ADMIN_SETTINGS_KEY, "treeSortType");

		if(sortType == null || Tools.isEmpty(sortType) == true) return "priority";
		else return sortType;
	}

	public static boolean isTreeSortOrderAsc(UserDetails user) {
		AdminSettingsService ass = new AdminSettingsService(user);
		return ass.getJsonBooleanValue(ADMIN_SETTINGS_KEY, "treeSortOrderAsc");
	}
}