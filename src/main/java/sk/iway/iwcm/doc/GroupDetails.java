package sk.iway.iwcm.doc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.beanutils.BeanUtils;
import sk.iway.iwcm.*;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.tags.JSEscapeTag;
import sk.iway.iwcm.users.UsersDB;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotBlank;

import java.util.List;
import java.util.StringTokenizer;

/**
 *  drzi zaznam z tabulky Groups
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.9 $
 *@created      $Date: 2004/03/15 21:34:42 $
 *@modified     $Date: 2004/03/15 21:34:42 $
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupDetails implements Cloneable, DocGroupInterface
{
	@DataTableColumn(
			inputType = DataTableColumnType.ID,
			data = "groupId",
			name = "id"
	)
	private int groupId;

	@DataTableColumn(
			inputType = DataTableColumnType.OPEN_EDITOR,
			title = "[[#{editor.directory_name}]]",
			editor = {
					@DataTableColumnEditor(
							type = "text",
							tab = "basic",
							attr = {
									@DataTableColumnEditorAttr(key = "data-dt-validation", value = "true"),
									@DataTableColumnEditorAttr(key = "data-dt-escape-slash", value = "true")
							}
					)
			}
	)
	@NotBlank
	private String groupName;


	@DataTableColumn(
			inputType = DataTableColumnType.TEXT,
			title = "[[#{webpages.editor.titleForMenu}]]",
			visible = false,
			editor = {
					@DataTableColumnEditor(
							type = "text",
							tab = "basic",
							attr = {
							@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after"),
							@DataTableColumnEditorAttr(key = "data-dt-validation", value = "true"),
							@DataTableColumnEditorAttr(key = "data-dt-escape-slash", value = "true")
					})
			}
	)
	@NotBlank
	private String navbarName = "";

	@DataTableColumn(
			renderFormat = "dt-format-text",
			title = "[[#{components.groupEdit.url_info}]]",
			className = "DTE_Field_Has_Checkbox",
			visible = true,
			editor = {
					@DataTableColumnEditor(
							type = "text",
							tab = "basic"
					)
			}
	)
	private String urlDirName;

	@DataTableColumn(
		renderFormat = "dt-format-text",
		title = "[[#{groupedit.domain}]]",
		className = "DTE_Field_Has_Checkbox",
		visible = false,
		perms = "multiDomain",
		editor = {
			@DataTableColumnEditor(
					type = "text",
					tab = "basic"
			)
		}
	)
	private String domainName = null;

	private int defaultDocId = 0;

	@DataTableColumn(
			renderFormat = "dt-format-select",
			title = "[[#{groupedit.folder_public_availability}]]",
			className = "DTE_Field_Has_Checkbox",
			visible = false,
			editor = {
					@DataTableColumnEditor(
							type = "select",
							tab = "basic",
							attr = {
								@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before")
							}
					)
			}
	)
	private boolean internal = false;

	@DataTableColumn(
		inputType = DataTableColumnType.NUMBER,
		title = "[[#{editor.sort_order}]]",
		tab = "menu",
		visible = true,
		editor = {
			@DataTableColumnEditor(
				message = "[[#{groupedit.displayOrderHint}]]"
			)
		}
	)
	private int sortPriority;

	@DataTableColumn(
			renderFormat = "dt-format-select",
			title = "[[#{groupedit.template}]]",
			className = "DTE_Field_Has_Checkbox",
			visible = true,
			editor = {
					@DataTableColumnEditor(
							type = "select",
							tab = "template"
					)
			}
	)
	private int tempId;

	@DataTableColumn(
			renderFormat = "dt-format-checkbox",
			title = "[[#{}]]",
			visible = false,
			editor = {
					@DataTableColumnEditor(
							type = "checkbox",
							tab = "template",
							message = "[[#{groupedit.force_the_use_of_group_template.tooltip}]]",
							attr = {
									@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
							},
							options = {
									@DataTableColumnEditorAttr(key = "groupedit.force_the_use_of_group_template", value = "true")
							}
					)
			}
	)
	private boolean forceTheUseOfGroupTemplate = false;

	@DataTableColumn(
			renderFormat = "dt-format-select",
			title = "[[#{groupedit.language}]]",
			className = "DTE_Field_Has_Checkbox",
			visible = false,
			editor = {
					@DataTableColumnEditor(
							type = "select",
							tab = "template"
					)
			}
	)
	private String lng = "";

	@DataTableColumn(
			renderFormat = "dt-format-select",
			title = "[[#{groupedit.new_page_template}]]",
			className = "DTE_Field_Has_Checkbox",
			visible = false,
			editor = {
					@DataTableColumnEditor(
							type = "select",
							tab = "template"
					)
			}
	)
	private int newPageDocIdTemplate = 0;

	@DataTableColumn(
			renderFormat = "dt-format-textarea",
			title = "[[#{groupedit.htmlHead}]]",
			visible = false,
			editor = {
					@DataTableColumnEditor(
						type = "textarea",
						tab = "template",
						attr = {
							@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before")
						}
					)
			}
	)
	private String htmlHead = "";

	@DataTableColumn(
			renderFormat = "dt-format-text",
			title = "[[#{groupedit.field_a}]]",
			visible = false,
			editor = {
					@DataTableColumnEditor(
							type = "text",
							label = "[[#{groupedit.field_a}]]",
							tab = "fields"
					)
			}
	)
	private String fieldA = "";

	@DataTableColumn(
			renderFormat = "dt-format-text",
			title = "[[#{groupedit.field_b}]]",
			visible = false,
			editor = {
					@DataTableColumnEditor(
							type = "text",
							label = "[[#{groupedit.field_b}]]",
							tab = "fields"
					)
			}
	)
	private String fieldB = "";

	@DataTableColumn(
			renderFormat = "dt-format-text",
			title = "[[#{groupedit.field_c}]]",
			visible = false,
			editor = {
					@DataTableColumnEditor(
							type = "text",
							label = "[[#{groupedit.field_c}]]",
							tab = "fields"
					)
			}
	)
	private String fieldC = "";

	@DataTableColumn(
			renderFormat = "dt-format-text",
			title = "[[#{groupedit.field_d}]]",
			visible = false,
			editor = {
					@DataTableColumnEditor(
							type = "text",
							label = "[[#{groupedit.field_d}]]",
							tab = "fields"
					)
			}
	)
	private String fieldD = "";

	@DataTableColumn(
		inputType = DataTableColumnType.SELECT,
		tab = "menu",
		title = "editor.menu_type_menu",
		className = "DTE_Field_Has_Checkbox",
		sortAfter = "editorFields.forcePriorityRecalculation",
		hidden = true,
		editor = {
				@DataTableColumnEditor(
						attr = {
							@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
							@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.menu_type_notlogged")
						}
				)
		}
	)
	private int menuType = 1;

	@DataTableColumn(
		inputType = DataTableColumnType.SELECT,
		tab = "menu",
		title = "editor.menu_type_navbar",
		className = "DTE_Field_Has_Checkbox",
		hidden = true
	)
	private Integer showInNavbar;

	@DataTableColumn(
		inputType = DataTableColumnType.SELECT,
		tab = "menu",
		title = "editor.menu_type_site_map",
		className = "DTE_Field_Has_Checkbox",
		hidden = true
	)
	private Integer showInSitemap;

	@DataTableColumn(
		inputType = DataTableColumnType.SELECT,
		tab = "menu",
		title = "editor.menu_type_menu",
		className = "DTE_Field_Has_Checkbox",
		hidden = true,
		editor = {
				@DataTableColumnEditor(
						attr = {
							@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.menu_type_logged"),
						}
				)
		}
	)
	private int loggedMenuType = -1;

	@DataTableColumn(
		inputType = DataTableColumnType.SELECT,
		tab = "menu",
		title = "[[#{editor.menu_type_navbar}]]",
		className = "DTE_Field_Has_Checkbox",
		hidden = true
	)
	private Integer loggedShowInNavbar;

	@DataTableColumn(
		inputType = DataTableColumnType.SELECT,
		tab = "menu",
		title = "[[#{editor.menu_type_site_map}]]",
		className = "DTE_Field_Has_Checkbox",
		hidden = true
	)
	private Integer loggedShowInSitemap;

	private String passwordProtected;

	private int parentGroupId = 0;
	private String navbar = "";
	@DataTableColumn(
		inputType = DataTableColumnType.TEXT,
		title = "sync.path",
		hiddenEditor = true,
		visible = false
	)
	private String fullPath;
	public static final int MENU_TYPE_HIDDEN = 0;
	public static final int MENU_TYPE_NORMAL = 1;
	public static final int MENU_TYPE_ONLYDEFAULT = 2;
	public static final int MENU_TYPE_NOSUB = 3;
	private int syncId;
	private int syncStatus;
	private int logonPageDocId;
	private String installName = null;

	private int linkGroupId;

	private boolean hiddenInAdmin = false;

	@DataTableColumnNested
	private GroupEditorField editorFields = null;

	public GroupDetails()
	{
		//empty constructor
	}

	/**
	 * Konstruktor z cesty, napr. /Jet portal 4/Úvodná stránka
	 * Pouziva sa pri importe a inych operaciach kde dostaneme cestu k adresaru
	 * @param path
	 */
	public GroupDetails(String path)
	{
		this(GroupsDB.getInstance().getCreateGroup(path));
	}

	/**
	 * skopiruje do seba skupinu copyFrom
	 * @param copyFrom
	 */
	public GroupDetails(GroupDetails copyFrom)
	{
		try
		{
			if (copyFrom != null) BeanUtils.copyProperties(this, copyFrom);
		}
		catch (Exception ex)
		{
			Logger.error(GroupDetails.class, ex);
		}
	}

	public boolean isAvailableForUser(Identity user)
	{
		if (Tools.isEmpty(getPasswordProtected()))
		{
			return(true);
		}
		if (user == null) return(false);

		StringTokenizer st = new StringTokenizer(getPasswordProtected(), ",");
		int id;
		while (st.hasMoreTokens())
		{
			id = Tools.getIntValue(st.nextToken(), -1);
			if (id > 0 && user.isInUserGroup(id))
			{
				return(true);
			}
		}

		return(false);
	}

	public boolean isAvailableForUserGroup(int userGroupId)
	{
		if (Tools.isEmpty(getPasswordProtected()))
		{
			return(true);
		}
		StringTokenizer st = new StringTokenizer(getPasswordProtected(), ",");
		int id;
		while (st.hasMoreTokens())
		{
			id = Tools.getIntValue(st.nextToken(), -1);
			if (id == userGroupId)
			{
				return(true);
			}
		}

		return(false);
	}

	/**
	 *  Gets the groupId attribute of the GroupDetails object
	 *
	 *@return    The groupId value
	 */
	public int getGroupId()
	{
		return groupId;
	}

	/**
	 *  Sets the groupId attribute of the GroupDetails object
	 *
	 *@param  newGroupId  The new groupId value
	 */
	public void setGroupId(int newGroupId)
	{
		groupId = newGroupId;
	}

	/**
	 * Nastavenie ID podla long hodnoty, potrebne pre DT/duplikovanie
	 * @param id
	 */
	public void setGroupId(Long id)
	{
		if (id == null) groupId = -1;
		else groupId = (int)id.longValue();
	}

	/**
	 *  Sets the groupName attribute of the GroupDetails object
	 *
	 *@param  newGroupName  The new groupName value
	 */
	public void setGroupName(String newGroupName)
	{
		groupName = GroupsDB.sanitizeGroupName(newGroupName, false);
	}

	/**
	 *  Gets the groupName attribute of the GroupDetails object
	 *
	 *@return    The groupName value
	 */
	public String getGroupName()
	{
		if (groupName==null) groupName = "";
		return groupName;
	}

	/**
	 *  Gets the groupNameShort attribute of the GroupDetails object
	 *
	 *@return    The groupNameShort value
	 */
	public String getGroupNameShort()
	{
		String ret = getGroupNameJS();
		if (parentGroupId!=0 && groupName.length() > 200)
		{
			//skracujeme len podadresare, nie parent!
			ret = ret.substring(0, 200) + "...";
		}

		if (defaultDocId>0 || navbar.indexOf("<a")!=-1)
		{
			return(ret);
		}
		return("!"+ret);
	}

	public String getGroupNameShortNoJS()
	{
		String ret = getGroupName();
		if (parentGroupId!=0 && groupName.length() > 200)
		{
			//skracujeme len podadresare, nie parent!
			ret = ret.substring(0, 19) + "...";
		}

		if (defaultDocId>0 || navbar.indexOf("<a")!=-1)
		{
			return(ret);
		}
		return("!"+ret);
	}

	public String getGroupNameJS()
	{
		return(JSEscapeTag.jsEscape(groupName));
	}



	/**
	 *  Vrati string s ID aj Menom grupy, prvych 15 znakov je ID, potom je meno
	 *
	 *@return    The groupIdName value
	 */
	public String getGroupIdName()
	{
		String ret = groupId + "                                                                               ";
		ret = ret.substring(0, 15) + fullPath;
		if (ret.indexOf('\'')!=-1)
		{
			ret = Tools.replace(ret, "'", " ");
		}
		if (ret.indexOf('\"')!=-1)
		{
			ret = Tools.replace(ret, "\"", " ");
		}
		return (ret);
	}

	/**
	 *  Sets the internal attribute of the GroupDetails object
	 *
	 *@param  newInternal  The new internal value
	 */
	public void setInternal(boolean newInternal)
	{
		internal = newInternal;
	}

	/**
	 *  Gets the internal attribute of the GroupDetails object
	 *
	 *@return    The internal value
	 */
	public boolean isInternal()
	{
		return internal;
	}

	/**
	 *  Sets the parentGroupId attribute of the GroupDetails object
	 *
	 *@param  newParentGroupId  The new parentGroupId value
	 */
	public void setParentGroupId(int newParentGroupId)
	{
		parentGroupId = newParentGroupId;
	}

	/**
	 *  Gets the parentGroupId attribute of the GroupDetails object
	 *
	 *@return    The parentGroupId value
	 */
	public int getParentGroupId()
	{
		return parentGroupId;
	}

	/**
	 *  Sets the navbar attribute of the GroupDetails object
	 *
	 *@param  navbar  The new navbar value
	 */
	public void setNavbar(String navbar)
	{
		this.navbarName = navbar;
		this.navbar = navbar;
	}

	/**
	 *  Gets the navbar attribute of the GroupDetails object
	 *
	 *@return    The navbar value
	 */
	public String getNavbar()
	{
		if (navbar == null || navbar.length()<1)
		{
			navbar = getGroupName();
		}

		if (defaultDocId > 0 && navbar.length() > 1 && (navbar.indexOf("<a") < 0 || navbar.toLowerCase().indexOf("<aparam") >= 0))
		{
			DocDB docDB = DocDB.getInstance();
			//odstrani <aparam>
			String text = navbar.replaceAll("(?i)<aparam.*>","");
			return("<a href='"+docDB.getDocLink(defaultDocId)+"'>" + text + "</a>");
		}
		else
		{
			return(navbar);
		}
	}

	/**
	 *  Gets the navbarName attribute of the GroupDetails object
	 *
	 *@return    The navbarName value
	 */
	public String getNavbarName()
	{
		if (Tools.isNotEmpty(navbarName))
		{
			//Logger.println(this,"navbarname="+navbarName);
			return (navbarName);
		}
		//Logger.println(this,"groupName="+groupName);
		return groupName;
	}

	public void setNavbarName(String navbarName)
	{
		this.navbarName = navbarName;
	}

	/**
	 * Vrati navbarName bez APARAM atributu (al je zadane)
	 * @return
	 */
	public String getNavbarNameNoAparam()
	{
		String name = getNavbarName();
		return name.replaceAll("(?i)<aparam.*>","");
	}

	/**
	 *  Sets the defaultDocId attribute of the GroupDetails object
	 *
	 *@param  newDefaultDocId  The new defaultDocId value
	 */
	public void setDefaultDocId(int newDefaultDocId)
	{
		defaultDocId = newDefaultDocId;
		//set navbar
		if (defaultDocId > 0 && navbar != null && navbar.length() > 1 && navbar.indexOf("<a") < 0)
		{
			DocDB docDB = DocDB.getInstance();
			navbar = "<a href='" + docDB.getDocLink(defaultDocId) + "'>" + navbar + "</a>";
		}
	}

	/**
	 * Toto potrebujem na skonstruovanie DocDB (aby sa mi nevolala rekurzivne getInstance())
	 * @param defaultDocId
	 */
	public void setDefaultDocIdNoNavbar(int defaultDocId)
	{
		this.defaultDocId = defaultDocId;
	}

	/**
	 *  Gets the defaultDocId attribute of the GroupDetails object
	 *
	 *@return    The defaultDocId value
	 */
	public int getDefaultDocId()
	{
		return defaultDocId;
	}

	/**
	 * Vrati objekt hlavna stranka adresara
	 * @return
	 */
	@JsonIgnore
	public DocDetails getDefaultDoc() {
		return DocDB.getInstance().getDoc(defaultDocId);
	}

	/**
	 * Vrati virtualnu cestu defaultnej stranky adresara
	 * @return
	 */
	public String getVirtualPath()
	{
		return DocDB.getInstance().getDocLink(defaultDocId);
	}

	/**
	 *  Sets the tempId attribute of the GroupDetails object
	 *
	 *@param  newTempId  The new tempId value
	 */
	public void setTempId(int newTempId)
	{
		tempId = newTempId;
	}

	/**
	 *  Gets the tempId attribute of the GroupDetails object
	 *
	 *@return    The tempId value
	 */
	public int getTempId()
	{
		return tempId;
	}

	/**
	 *  Cela cesta ku grupe, napr. /iwcm/produkty/hlasove
	 *
	 *@param  fullPath  The new fullPath value
	 */
	public void setFullPath(String fullPath)
	{
		this.fullPath = fullPath;
	}

	/**
	 *  Gets the fullPath attribute of the GroupDetails object
	 *
	 *@return    The fullPath value
	 */
	public String getFullPath()
	{
		return fullPath;
	}

	public String getParentFullPath()
	{
		if (fullPath == null) {
			return null;
		}

		int i = fullPath.lastIndexOf('/');
		if (i > 0)
		{
			return(fullPath.substring(0, i));
		}
		if (i == 0 && fullPath.length()>1) return("/");
		return(null);
	}

	/**
	 *  Sets the sortPriority attribute of the GroupDetails object
	 *
	 *@param  sortPriority  The new sortPriority value
	 */
	public void setSortPriority(int sortPriority)
	{
		this.sortPriority = sortPriority;
	}

	/**
	 *  Gets the sortPriority attribute of the GroupDetails object
	 *
	 *@return    The sortPriority value
	 */
	public int getSortPriority()
	{
		return sortPriority;
	}

	/**
	 *  Sets the passwordProtected attribute of the GroupDetails object
	 *
	 *@param  passwordProtected  The new passwordProtected value
	 */
	public void setPasswordProtected(String passwordProtected)
	{
		if (Tools.isEmpty(passwordProtected) || ",".equals(passwordProtected))
		{
			this.passwordProtected = null;
		}
		else this.passwordProtected = passwordProtected;
	}

	/**
	 * Prida adresar do zadanej skupiny pouzivatelov (s overenim, ci tam uz nie je)
	 * @param userGroupId
	 */
	public void addToUserGroup(int userGroupId)
	{
		String ret = getPasswordProtected();
		if(!isAvailableForUserGroup(userGroupId)) {
			ret += ","+userGroupId;
		} else if (Tools.isEmpty(ret)) {
			// bugfix v pripade ak adresar nema ziadne pouzivatelske skupiny predchadzajuci if bude vzdy false
			ret = ret + "," + userGroupId;
		}
		if(ret.startsWith(",")) ret = ret.substring(1);
		setPasswordProtected(ret);
	}

	/**
	 *  Gets the passwordProtected attribute of the GroupDetails object
	 *
	 *@return    The passwordProtected value
	 */
	public String getPasswordProtected()
	{
		return passwordProtected;
	}

	public String getUrlDirName()
	{
		if (urlDirName == null || urlDirName.length()<1)
		{
			urlDirName = getNavbarNameNoAparam();
			urlDirName = DB.internationalToEnglish(urlDirName).toLowerCase();
			urlDirName = DocTools.removeCharsDir(urlDirName, true).toLowerCase();

			if ("slovensky".equals(urlDirName)) {
				urlDirName = "sk";
				sortPriority = 10;
			}
			else if ("cesky".equals(urlDirName)) {
				urlDirName = "cz";
				sortPriority = 20;
			}
			else if ("english".equals(urlDirName)) {
				urlDirName = "en";
				sortPriority = 30;
			}
			else if ("deutsch".equals(urlDirName)) {
				urlDirName = "de";
				sortPriority = 40;
			}
		}
		return urlDirName;
	}
	public void setUrlDirName(String urlDirName)
	{
		this.urlDirName = urlDirName;
	}

	/**
	 * @return Returns the syncId.
	 */
	public int getSyncId()
	{
		return syncId;
	}
	/**
	 * @param syncId The syncId to set.
	 */
	public void setSyncId(int syncId)
	{
		this.syncId = syncId;
	}
	/**
	 * @return Returns the syncStatus.
	 */
	public int getSyncStatus()
	{
		return syncStatus;
	}
	/**
	 * @param syncStatus The syncStatus to set.
	 */
	public void setSyncStatus(int syncStatus)
	{
		this.syncStatus = syncStatus;
	}
	/**
	 * @return Returns the htmlHead.
	 */
	public String getHtmlHead()
	{
		return htmlHead;
	}
	/**
	 * @param htmlHead The htmlHead to set.
	 */
	public void setHtmlHead(String htmlHead)
	{
		this.htmlHead = htmlHead;
	}
	/**
	 * @return Returns the logonPageDocId.
	 */
	public int getLogonPageDocId()
	{
		return logonPageDocId;
	}
	/**
	 * @param logonPageDocId The logonPageDocId to set.
	 */
	public void setLogonPageDocId(int logonPageDocId)
	{
		this.logonPageDocId = logonPageDocId;
	}

	public String getDomainName()
	{
		return domainName;
	}

	public void setDomainName(String domainName)
	{
		//domainName should be always lowercase
		if (domainName != null) domainName = domainName.toLowerCase();
		this.domainName = domainName;
	}

	public String getFieldA()
	{
		return fieldA;
	}

	public void setFieldA(String fieldA)
	{
		this.fieldA = fieldA;
	}

	public String getFieldB()
	{
		return fieldB;
	}

	public void setFieldB(String fieldB)
	{
		this.fieldB = fieldB;
	}

	public String getFieldC()
	{
		return fieldC;
	}

	public void setFieldC(String fieldC)
	{
		this.fieldC = fieldC;
	}

	public String getFieldD()
	{
		return fieldD;
	}

	public void setFieldD(String fieldD)
	{
		this.fieldD = fieldD;
	}

	public String getInstallName()
	{
		return installName;
	}

	public void setInstallName(String installName)
	{
		this.installName = installName;
	}

	public int getNewPageDocIdTemplate()
	{
		return newPageDocIdTemplate;
	}

	public void setNewPageDocIdTemplate(int newPageDocIdTemplate)
	{
		this.newPageDocIdTemplate = newPageDocIdTemplate;
	}

	public int getLinkGroupId()
	{
		return linkGroupId;
	}

	public void setLinkGroupId(int linkGroupId)
	{
		this.linkGroupId = linkGroupId;
	}

	public String getLng()
	{
		return lng;
	}

	public void setLng(String lng)
	{
		this.lng = lng;
	}

	public boolean isHiddenInAdmin()
	{
		return hiddenInAdmin;
	}

	public void setHiddenInAdmin(boolean hiddenInAdmin)
	{
		this.hiddenInAdmin = hiddenInAdmin;
	}

	@JsonIgnore
	public List<DocDetails> getDocuments()
	{
		return DocDB.getInstance().getDocByGroup(groupId);
	}

	public static GroupDetails getById(int id)
	{
		return GroupsDB.getInstance().getGroup(id);
	}


	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	@Override
	public String toString()
	{
		return fullPath+", ID => "+getGroupId();
	}

	public boolean isForceTheUseOfGroupTemplate() {
		return forceTheUseOfGroupTemplate;
	}

	public void setForceTheUseOfGroupTemplate(boolean forceTheUseOfGroupTemplate) {
		this.forceTheUseOfGroupTemplate = forceTheUseOfGroupTemplate;
	}

	public GroupEditorField getEditorFields() {
		return editorFields;
	}

	public void setEditorFields(GroupEditorField editorFields) {
		this.editorFields = editorFields;
	}

	// ** Pridane atributy do DB ** // ticket 54205

	//Not logged user
	public int getMenuType() {
		return menuType;
	}

	public void setMenuType(int menuType) {
		this.menuType = menuType;
	}

	public Integer getShowInNavbar() {
		return showInNavbar;
	}

	public void setShowInNavbar(Integer showInNavbar) {
		this.showInNavbar = showInNavbar;
	}

	public Integer getShowInSitemap() {
		return showInSitemap;
	}

	public void setShowInSitemap(Integer showInSitemap) {
		this.showInSitemap = showInSitemap;
	}

	//Logged user
	public int getLoggedMenuType() {
		return loggedMenuType;
	}

	public void setLoggedMenuType(int loggedMenuType) {
		this.loggedMenuType = loggedMenuType;
	}

	public Integer getLoggedShowInNavbar() {
		return loggedShowInNavbar;
	}

	public void setLoggedShowInNavbar(Integer loggedShowInNavbar) {
		this.loggedShowInNavbar = loggedShowInNavbar;
	}

	public Integer getLoggedShowInSitemap() {
		return loggedShowInSitemap;
	}

	public void setLoggedShowInSitemap(Integer loggedShowInSitemap) {
		this.loggedShowInSitemap = loggedShowInSitemap;
	}

	/**
	 * Vrati sposob zobrazenia (GroupDetails.MENU_TYPE_XXX) v navigacnom menu (automaticky detekuje, ci je prihlaseny pouzivatel, alebo nie)
	 * @param session
	 * @return
	 */
	public int getMenuType(HttpSession session) {

		if (session != null && session.getAttribute("menuDisabledGroup"+getGroupId())!=null) return GroupDetails.MENU_TYPE_HIDDEN;

		if (loggedMenuType < 0 || session == null) return(menuType);
		Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
		if (user == null) return(menuType);

		//skontroluj, ci je user v niektorej z tychto skupin
		if (isAvailableForUser(user))
		{
			//Logger.debug(GroupDetails.class, this.getGroupIdName()+" logged:"+loggedMenuType);
			return(loggedMenuType);
		}

		//Logger.debug(GroupDetails.class, this.getGroupIdName()+" final return:"+menuType);
		return(menuType);
	}

	/**
	 * Vrati sposob zobrazenia (GroupDetails.MENU_TYPE_XXX) navigacnej listy (automaticky detekuje, ci je prihlaseny pouzivatel, alebo nie)
	 * @param session
	 * @return
	 */
	public int getShowInNavbar(HttpSession session) {

		Identity user = UsersDB.getCurrentUser(session);
		//Not logged
		if(user == null || loggedShowInNavbar == null) {
			//0 - same as menu
			if(showInNavbar == null) return getMenuType(session);
			return showInNavbar.intValue();
		}

		return loggedShowInNavbar.intValue();
	}

	/**
	 * Vrati sposob zobrazenia (GroupDetails.MENU_TYPE_XXX) v mape stranok (automaticky detekuje, ci je prihlaseny pouzivatel, alebo nie)
	 * @param request
	 * @return
	 */
	public int getShowInSitemap(HttpServletRequest request) {
		if (request != null) return getShowInSitemap(request.getSession());
		return getShowInSitemap((HttpSession)null);
	}

	/**
	 * Vrati sposob zobrazenia (GroupDetails.MENU_TYPE_XXX) v mape stranok (automaticky detekuje, ci je prihlaseny pouzivatel, alebo nie)
	 * @param session
	 * @return
	 */
	public int getShowInSitemap(HttpSession session) {

		Identity user = UsersDB.getCurrentUser(session);
		//Not logged
		if(user == null || loggedShowInSitemap == null) {
			//0 - same as menu
			if(showInSitemap == null) return getMenuType(session);
			return showInSitemap.intValue();
		}

		return loggedShowInSitemap.intValue();
	}
}