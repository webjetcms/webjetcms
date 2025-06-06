package sk.iway.iwcm.doc;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.util.ResponseUtils;

import sk.iway.iwcm.*;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.editor.service.GroupsService;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.users.UserDetails;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static sk.iway.iwcm.Tools.isEmpty;

/**
 *  Drzi obsah tabulky groups
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.18 $
 *@created      $Date: 2004/03/25 17:36:09 $
 *@modified     $Date: 2004/03/25 17:36:09 $
 */
@SuppressWarnings({"java:S6905"})
public class GroupsDB extends DB
{

	private List<GroupDetails> groups;
	//cache pre zobrazenie stromovej struktury stranok (pri velkych sidlach sa to dlho a zbytocne nacitavalo dookola)
	private List<GroupDetails> groupsTreeAllCache = null;

	private Map<Integer, GroupDetails> idToGroups;
	private Map<String,Integer> domainIds;

	private final String serverName;

	public static final Comparator<GroupDetails> groupsDbSorter = new Comparator<GroupDetails>(){
		@Override
		public int compare(GroupDetails group1, GroupDetails group2)
		{
			return 	group1.getSortPriority() - group2.getSortPriority() != 0 ?
						group1.getSortPriority() - group2.getSortPriority() :
						group1.getGroupId() - group2.getGroupId();
		}
	};

	public static final Comparator<GroupDetails> nameSorter = new Comparator<GroupDetails>()
	{
		@Override
		public int compare(GroupDetails group1, GroupDetails group2)
		{
			return Tools.slovakCollator.compare(group1.getGroupName(), group2.getGroupName());
		}
	};


	/**
	 * vrati instanciu triedy
	 * @return
	 */
	public static GroupsDB getInstance()
	{
		return(getInstance(false));
	}

	/**
	 * vrati instanciu triedy
	 * @param forceRefresh
	 * @return
	 */
	public static GroupsDB getInstance(boolean forceRefresh)
	{
		return(getInstance(Constants.getServletContext(), forceRefresh, "iwcm")); //NOSONAR
	}

	/**
	 *  Gets the instance attribute of the GroupsDB class
	 *
	 *@param  servletContext  Description of the Parameter
	 *@param  force_refresh   Description of the Parameter
	 *@param  serverName      Description of the Parameter
	 *@return                 The instance value
	 *@deprecated - pouzite verziu getInstance(boolean forceRefresh)
	 */
	@Deprecated
	public static GroupsDB getInstance(javax.servlet.ServletContext servletContext, boolean force_refresh, String serverName)
	{
		//try to get it from server space
		if (force_refresh == false)
		{
			GroupsDB groupsDB = ((GroupsDB) servletContext.getAttribute(Constants.A_GROUPS_DB));
			//Logger.println(this,"GroupsDB: getting from server space");
			if (groupsDB != null && groupsDB.groups!=null) // fix MBO: ak groups je null, zbehne force refresh
			{
				return groupsDB;
			}
		}
		synchronized (GroupsDB.class)
		{
			if (force_refresh)
			{
				GroupsDB groupsDB = new GroupsDB(servletContext, serverName);
				//	remove
				servletContext.removeAttribute(Constants.A_GROUPS_DB);
				//save us to server space
				servletContext.setAttribute(Constants.A_GROUPS_DB, groupsDB);

				return groupsDB;
			}
			else
			{
				GroupsDB groupsDB = (GroupsDB) servletContext.getAttribute(Constants.A_GROUPS_DB);
				if (groupsDB == null)
				{
					groupsDB = new GroupsDB(servletContext, serverName);
					//	remove
					servletContext.removeAttribute(Constants.A_GROUPS_DB);
					//save us to server space
					servletContext.setAttribute(Constants.A_GROUPS_DB, groupsDB);

				}
				return groupsDB;
			}
		}
	}

	/**
	 *  Constructor for the GroupsDB object
	 *
	 *@param  servletContext  Description of the Parameter
	 *@param  serverName      Description of the Parameter
	 */
	private GroupsDB(javax.servlet.ServletContext servletContext, String serverName)
	{
		Logger.println(this,"GroupsDB: constructor [" + Constants.getInstallName()+"]");
		this.serverName = serverName;

		try
		{
			reloadGroups();
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		ClusterDB.addRefresh(GroupsDB.class);
		Cache.getInstance().removeObjectStartsWithName("GroupsDB.");
	}

	/**
	 *  Description of the Method
	 *
	 *@exception  Exception  Description of the Exception
	 */
	public void reloadGroups() throws Exception
	{
		DebugTimer dt = new DebugTimer("GroupsDB.reloadGroups");

		//ak vieme predchadzajucu velkost, tak si ju vopred predalokujeme
		List<GroupDetails> groupsHolderWhileLoading = new CopyOnWriteArrayList<>(); //Collections.synchronizedList(new ArrayList<>(groups!=null?groups.size():300));
		//ak existuje, tak si vopred predalokuj dost miesta pre kluce
		Map<Integer, GroupDetails> idToGroupsHolderWhileLoading = Collections.synchronizedMap(new HashMap<Integer, GroupDetails>(idToGroups!=null?idToGroups.size():300));
		Map<String,GroupDetails> domainIdsGroups = Collections.synchronizedMap(new HashMap<String, GroupDetails>(domainIds!=null?domainIds.size():100));
		Map<String,Integer> domainIdsHolderWhileLoading = Collections.synchronizedMap(new HashMap<String, Integer>(domainIds!=null?domainIds.size():100));

		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		java.sql.ResultSet rs = null;
		try
		{
			dt.diff("after objects");

			db_conn = DBPool.getConnection(serverName);
			String sql = "SELECT * FROM groups ORDER BY sort_priority, group_name";
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();

			dt.diff("after rs");

			while (rs.next())
			{
				GroupDetails group = fillFieldsByResultSet(rs);

				groupsHolderWhileLoading.add(group);
				idToGroupsHolderWhileLoading.put(group.getGroupId(), group);
				if(Tools.isNotEmpty(group.getDomainName()) && group.getParentGroupId() == 0)
				{
					GroupDetails domainGroup = domainIdsGroups.get(group.getDomainName());
					//ako domainId bereme groupId, ktore ma najnizsie ID, aby sa prvo vytvoreny priecinok v domene zachoval
					if (domainGroup==null || group.getGroupId()<domainGroup.getGroupId())
					{

						domainIdsGroups.put(group.getDomainName(), group);
						//pomocou konf. premennej domainId-www.domena.sk je mozne nastavit fixne ID, napr. ak zmazeme povodny korenovy priecinok domeny
						int constantDomainId = Constants.getInt("domainId-"+group.getDomainName());
						int domainId = group.getGroupId();
						if (constantDomainId>0) domainId = constantDomainId;
						domainIdsHolderWhileLoading.put(group.getDomainName(), domainId);
					}
				}
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;

			groups = groupsHolderWhileLoading;
			idToGroups = idToGroupsHolderWhileLoading;
			domainIds = domainIdsHolderWhileLoading;

			dt.diff("after db load");

			//nastav full path
			for (GroupDetails group : groups)
			{
				group.setFullPath(getPath(group.getGroupId()));
			}

			dt.diff("after full path");

			getAllSystemFolders(true);

			dt.diff("after system folders");
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}

		dt.diff("done");
	}

	/**
	 * Naplni objekt z result setu
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static GroupDetails fillFieldsByResultSet(java.sql.ResultSet rs) throws SQLException
	{
		GroupDetails group;
		group = new GroupDetails();
		group.setGroupId(rs.getInt("group_id"));
		group.setGroupName(getDbString(rs, "group_name"));
		group.setInternal(rs.getBoolean("internal"));
		group.setParentGroupId(rs.getInt("parent_group_id"));
		group.setNavbar(getDbString(rs, "navbar"));
		group.setDefaultDocId(rs.getInt("default_doc_id"));
		group.setTempId(rs.getInt("temp_id"));
		group.setSortPriority(rs.getInt("sort_priority"));
		group.setPasswordProtected(getDbString(rs, "password_protected"));
		group.setMenuType(rs.getInt("menu_type"));
		group.setUrlDirName(getDbString(rs, "url_dir_name"));
		group.setSyncId(rs.getInt("sync_id"));
		group.setSyncStatus(rs.getInt("sync_status"));
		group.setHtmlHead(DB.getDbString(rs, "html_head"));
		group.setLogonPageDocId(rs.getInt("logon_page_doc_id"));

		//	domain_name, new_page_docid_template, install_name, field_a, field_b, field_c, field_d
		group.setDomainName(DB.getDbString(rs, "domain_name"));
		group.setNewPageDocIdTemplate(rs.getInt("new_page_docid_template"));
		group.setInstallName(DB.getDbString(rs, "install_name"));
		group.setFieldA(DB.getDbString(rs, "field_a"));
		group.setFieldB(DB.getDbString(rs, "field_b"));
		group.setFieldC(DB.getDbString(rs, "field_c"));
		group.setFieldD(DB.getDbString(rs, "field_d"));

		group.setLoggedMenuType(rs.getInt("logged_menu_type"));
		group.setLinkGroupId(rs.getInt("link_group_id"));
		group.setLng(rs.getString("lng"));
		group.setHiddenInAdmin(rs.getBoolean("hidden_in_admin"));
		group.setForceTheUseOfGroupTemplate(rs.getBoolean("force_group_template"));

		DataAccessHelper.groupLoadData(rs, group);

		return group;
	}


	/**
	 * Naplni objekt GroupSchedulerDetails z result setu
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static GroupSchedulerDetails fillFieldsByResultSetFromScheduler(java.sql.ResultSet rs) throws SQLException
	{
		GroupSchedulerDetails groupScheduler;
		groupScheduler = new GroupSchedulerDetails();
		groupScheduler.setGroupId(rs.getInt("group_id"));
		groupScheduler.setGroupName(getDbString(rs, "group_name"));
		groupScheduler.setInternal(rs.getBoolean("internal"));
		groupScheduler.setParentGroupId(rs.getInt("parent_group_id"));
		groupScheduler.setNavbar(getDbString(rs, "navbar"));
		groupScheduler.setDefaultDocId(rs.getInt("default_doc_id"));
		groupScheduler.setTempId(rs.getInt("temp_id"));
		groupScheduler.setSortPriority(rs.getInt("sort_priority"));
		groupScheduler.setPasswordProtected(getDbString(rs, "password_protected"));
		groupScheduler.setMenuType(rs.getInt("menu_type"));
		groupScheduler.setUrlDirName(getDbString(rs, "url_dir_name"));
		groupScheduler.setSyncId(rs.getInt("sync_id"));
		groupScheduler.setSyncStatus(rs.getInt("sync_status"));
		groupScheduler.setHtmlHead(DB.getDbString(rs, "html_head"));
		groupScheduler.setLogonPageDocId(rs.getInt("logon_page_doc_id"));

		//	domain_name, new_page_docid_template, install_name, field_a, field_b, field_c, field_d
		groupScheduler.setDomainName(DB.getDbString(rs, "domain_name"));
		groupScheduler.setNewPageDocIdTemplate(rs.getInt("new_page_docid_template"));
		groupScheduler.setInstallName(DB.getDbString(rs, "install_name"));
		groupScheduler.setFieldA(DB.getDbString(rs, "field_a"));
		groupScheduler.setFieldB(DB.getDbString(rs, "field_b"));
		groupScheduler.setFieldC(DB.getDbString(rs, "field_c"));
		groupScheduler.setFieldD(DB.getDbString(rs, "field_d"));

		groupScheduler.setLoggedMenuType(rs.getInt("logged_menu_type"));
		groupScheduler.setLinkGroupId(rs.getInt("link_group_id"));

		//groupScheduler
		groupScheduler.setUserId(rs.getInt("user_id"));
		groupScheduler.setSaveDate(rs.getTimestamp("save_date"));
		groupScheduler.setScheduleId(rs.getInt("schedule_id"));
		groupScheduler.setWhenToPublish(rs.getTimestamp("when_to_publish"));

		groupScheduler.setLng(rs.getString("lng"));
		groupScheduler.setHiddenInAdmin(rs.getBoolean("hidden_in_admin"));
		groupScheduler.setForceTheUseOfGroupTemplate(rs.getBoolean("force_group_template"));

		DataAccessHelper.groupLoadData(rs, groupScheduler);

		return groupScheduler;
	}

	/**
	 *  Vrati zoznam skupin v danej skupine
	 *
	 *@param  parent  Description of the Parameter
	 *@return         The groups value
	 */
	public List<GroupDetails> getGroups(int parent)
	{
		List<GroupDetails> ret = new ArrayList<>();
		for (GroupDetails group : groups)
		{
			if (group.getParentGroupId() == parent)
			{
				ret.add(group);
			}
		}
		return (ret);
	}

	/**
	 * Vrati zoznam skupin v danej skupine dostupnych v menu
	 * @param parent
	 * @return
	 */
	public List<GroupDetails> getGroupsAvailableInMenu(int parent)
	{
		return getGroupsAvailableInMenu(parent, null);
	}

	 /**
	  * Vrati zoznam skupin v danej skupine dostupnych v menu, testuje menuType pre prihlaseneho usera
	  * @param parent
	  * @param session
	  * @return
	  */
	 public List<GroupDetails> getGroupsAvailableInMenu(int parent, HttpSession session)
	 {
		  List<GroupDetails> ret = new ArrayList<>();
		 for (GroupDetails group : groups)
		  {
				if (group.getParentGroupId() == parent && group.isInternal()==false && group.getMenuType(session)!=GroupDetails.MENU_TYPE_HIDDEN)
				{
					 ret.add(group);
				}
		  }
		  return (ret);
	 }

	/**
	 *  Vrati GroupDetails so zadanym ID
	 *
	 *@param  group_id - id adresara
	 *@return           The group value
	 */
	public GroupDetails getGroup(int group_id)
	{
		return idToGroups.get(group_id);
	}

	/**
	 * Ziska adresar podla zadanej full path
	 * @param fullPath
	 * @return
	 */
	public GroupDetails getGroupByPath(String fullPath)
	{
		return getGroupByPathAndDomain(fullPath, null);
	}

	public GroupDetails getGroupByPathAndDomain(String fullPath, String domainName) {
		if (fullPath.endsWith("/") && fullPath.length() > 1)
		{
			//Remove last character, slash "/"
			fullPath = fullPath.substring(0, fullPath.length()-1);
		}

		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		GroupDetails firstGroup = null;
		//Loop all groups to find match
		for (GroupDetails group : getGroupsAll())
		{
			//We found match be full path
			if (group.getFullPath().equals(fullPath))
			{
				//Check if domainName is set - IF yes, we REQUIRE match
				if(Constants.getBoolean("multiDomainEnabled") == true && Tools.isNotEmpty(domainName)) {
					if(domainName.equals(group.getDomainName())) return group;
				} else {
					//We are good with only fullPath match
					if (firstGroup == null) firstGroup = group;

					//If we have better match full path AND domainName, we return it
					if (rb != null && Tools.isNotEmpty(rb.getDomain()) && Constants.getBoolean("multiDomainEnabled")==true) {
						//mame presnu zhodu aj podla domenoveho mena
						if (rb.getDomain().equals(group.getDomainName())) return group;
					}
				}
			}
		}
		return firstGroup;
	}

	/**
	 *  Gets the group attribute of the GroupsDB object
	 *
	 *@param  groupName  Description of the Parameter
	 *@return           The group value
	 */
	public GroupDetails getGroup(String groupName)
	{
		return getGroup(groupName,false);
	}

	/**
	 *  Gets the group attribute of the GroupsDB object
	 *
	 *@param  groupName  Description of the Parameter
	 *@param internationalToEnglish ci sa maju nazvy adresarov upravit z international do english
	 *@return           The group value
	 */
	public GroupDetails getGroup(String groupName, boolean internationalToEnglish)
	{
		groupName = sanitizeGroupName(groupName, true);
		if (Tools.isNotEmpty(groupName))
		{
			for (GroupDetails group : getGroupsAll())
			{
				if(internationalToEnglish)
				{
					if (DB.internationalToEnglish(groupName).equalsIgnoreCase(DB.internationalToEnglish(group.getGroupName())))
					{
						return (group);
					}
				}
				else
				{
					if (groupName.equalsIgnoreCase(group.getGroupName()))
					{
						return (group);
					}
				}
			}
		}
		return (null);
	}

	/**
	 * Ziska adresar so zadanym menom v zadanom adresari
	 * @param groupName
	 * @param parentGroupId
	 * @return
	 */
	public GroupDetails getGroup(String groupName, int parentGroupId)
	{
		GroupDetails firstGroup = null;
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		groupName = sanitizeGroupName(groupName, true);
		if (Tools.isNotEmpty(groupName))
		{
			for (GroupDetails group : getGroupsAll())
			{
				if (group.getParentGroupId() == parentGroupId && groupName.equalsIgnoreCase(group.getGroupName()))
				{
					if (firstGroup==null) firstGroup = group;
					if (rb != null && Tools.isNotEmpty(rb.getDomain()) && Constants.getBoolean("multiDomainEnabled")==true) {
						//mame presnu zhodu aj podla domenoveho mena
						if (rb.getDomain().equals(group.getDomainName())) return group;
					}
				}
			}
		}
		return (firstGroup);
	}

	 /**
	  * Ziska adresar so zadanym menom v zadanom adresari, ak neexistuje tak ho vytvori
	  * @param groupName
	  * @param parentGroupId
	  * @return
	  */
	public GroupDetails getOrCreateGroup(String groupName, int parentGroupId)
	{
		 GroupDetails group = getGroup(groupName, parentGroupId);
		 if (group == null) {
			  group = addNewGroup(groupName, parentGroupId);
		 }
		 return group;
	}


	/**
	 *  Ziska stromovu strukturu adresarov
	 *
	 *@return    The groupsTree value
	 */
	public List<GroupDetails> getGroupsTree()
	{
		List<GroupDetails> p_groups = new ArrayList<>();
		getGroupsTree(0, p_groups, true);
		return (p_groups);
	}

	/**
	 * Ziska stromovu strukturu adresarov iba pre userom editovatelne adresare
	 * @param editableGroups
	 * @return
	 */
	public List<GroupDetails> getGroupsTree(String editableGroups)
	{
		//cache pre groupslist-tree.jsp
		if (Tools.isEmpty(editableGroups) && groupsTreeAllCache!=null)
		{
			Logger.debug(GroupsDB.class, "getGroupsTree - returning from cache");
			List<GroupDetails> cloned = new ArrayList<>();
			cloned.addAll(groupsTreeAllCache);
			return cloned;
		}

		List<GroupDetails> p_groups = new ArrayList<>();

		if (editableGroups != null && editableGroups.length() > 0)
		{
			StringTokenizer st = new StringTokenizer(editableGroups, ",");
			String id;
			int i_id;
			while (st.hasMoreTokens())
			{
				id = st.nextToken().trim();
				try
				{
					i_id = Integer.parseInt(id);
					//pridaj to do stromu editovatelnych stranok
					GroupDetails parentGroup = findGroup(i_id);
					GroupDetails parentGroupCopy = new GroupDetails(parentGroup);
					parentGroupCopy.setParentGroupId(0);
					if (parentGroup.getFullPath().length() > 200)
					{
						parentGroupCopy.setGroupName("..."+parentGroup.getFullPath().substring(parentGroup.getFullPath().length()-198));
					}
					else
					{
						parentGroupCopy.setGroupName(parentGroup.getFullPath());
					}
					p_groups.add(parentGroupCopy);
					getGroupsTree(i_id, p_groups, true);
				}
				catch (Exception ex)
				{

				}
			}
		}
		else
		{
			getGroupsTree(0, p_groups, true);

			if (Tools.isEmpty(editableGroups))
			{
				groupsTreeAllCache = new ArrayList<>();
				groupsTreeAllCache.addAll(p_groups);
				Logger.debug(GroupsDB.class, "getGroupsTree - SETTING cache");
			}
		}

		return (p_groups);
	}


	/**
	 *  Ziska stromovu strukturu adresarov
	 *
	 *@param  parent          rodicovsky adresar
	 *@param  includeParent  ak true, vratane rodica
	 *@param  includeInternals ak true, vratane internych adresarov
	 *@return                 The groupsTree value
	 */
	public List<GroupDetails> getGroupsTree(int parent, boolean includeParent, boolean includeInternals)
	{
		return getGroupsTree(parent, includeParent, includeInternals, false);
	}

	/**
	 * Ziska stromovu strukturu adresarov
	 * @param parent -  rodicovsky adresar
	 * @param includeParent - ak true, vratane rodica
	 * @param includeInternals - ak true, vratane internych adresarov
	 * @param onlyAvailableInMenu - ak true, iba dostupne v menu
	 * @return
	 */
	public List<GroupDetails> getGroupsTree(int parent, boolean includeParent, boolean includeInternals, boolean onlyAvailableInMenu)
	{
		return getGroupsTree(parent, includeParent, includeInternals, onlyAvailableInMenu, null);
	}

	/**
	 * Ziska stromovu strukturu adresarov
	 * @param parent -  rodicovsky adresar
	 * @param includeParent - ak true, vratane rodica
	 * @param includeInternals - ak true, vratane internych adresarov
	 * @param onlyAvailableInMenu - ak true, iba dostupne v menu
	 * @param maxDepth - maximalna hlbka stromu
	 * @return
	 */
	public List<GroupDetails> getGroupsTree(int parent, boolean includeParent, boolean includeInternals, boolean onlyAvailableInMenu, Integer maxDepth)
	{
		List<GroupDetails> p_groups = new ArrayList<>();
		if (includeParent)
		{
			GroupDetails parentGroup = findGroup(parent);
			if (parentGroup != null) p_groups.add(parentGroup);
		}
		getGroupsTree(parent, p_groups, includeInternals, onlyAvailableInMenu, maxDepth);
		return (p_groups);
	}

	/**
	 * Ziska stromovu strukturu adresarov. Rekurzivna metoda!
	 * @param parent
	 * @param p_groups
	 */
	public void getGroupsTree(int parent, List<GroupDetails> p_groups)
	{
		getGroupsTree(parent, p_groups, true);
	}

	/**
	 *  Ziska stromovu strukturu adresarov. Rekurzivna metoda!
	 *
	 *@param  parent    rodicovsky adresar
	 *@param  p_groups  array list, do ktoreho sa to plni
	 *@param  includeInternals ak true, vratane internych adresarov
	 */
	public void getGroupsTree(int parent, List<GroupDetails> p_groups, boolean includeInternals)
	{
		getGroupsTree(parent, p_groups, includeInternals, false);
	}

	/**
	 *
	 * @param parent
	 * @param p_groups
	 * @param includeInternals
	 * @param onlyAvailableInMenu
	 */
	public void getGroupsTree(int parent, List<GroupDetails> p_groups, boolean includeInternals, boolean onlyAvailableInMenu)
	{
		getGroupsTree(parent, p_groups, includeInternals, onlyAvailableInMenu, null);
	}

	/**
	 *
	 * @param parent
	 * @param p_groups
	 * @param includeInternals
	 * @param onlyAvailableInMenu
	 */
	public void getGroupsTree(int parent, List<GroupDetails> p_groups, boolean includeInternals, boolean onlyAvailableInMenu, Integer maxDepth)
	{
		Map<Integer, List<GroupDetails>> childsTable = prepareChildsTable();
		getGroupsTree(childsTable, parent, p_groups, includeInternals, onlyAvailableInMenu, maxDepth, 1);
	}

	/**
	 * Pripravi hash tabulku kde klucom je groupId a hodnotou je zoznam childov
	 * @return
	 */
	private Map<Integer, List<GroupDetails>> prepareChildsTable()
	{
		Map<Integer, List<GroupDetails>> childsTable = new Hashtable<>();

		for (GroupDetails group : groups)
		{
			List<GroupDetails> childsList = childsTable.get(group.getParentGroupId());
			if (childsList == null)
			{
				childsList = new ArrayList<>();
				childsTable.put(group.getParentGroupId(), childsList);
			}
			childsList.add(group);
		}

		return childsTable;
	}

	/**
	 * Do listu p_groups rekurzivne naplni zoznam childov pre zadane parent group id
	 * @param childsTable
	 * @param parent
	 * @param p_groups
	 * @param includeInternals
	 * @param onlyAvailableInMenu
	 * @param maxDepth
	 */
	private void getGroupsTree(Map<Integer, List<GroupDetails>> childsTable, int parent, List<GroupDetails> p_groups, boolean includeInternals, boolean onlyAvailableInMenu, Integer maxDepth, int iteration)
	{
		if(maxDepth != null && (iteration > maxDepth)) return;

		List<GroupDetails> childs = childsTable.get(parent);
		if (childs == null) return;

		if (iteration > 500) return;

		GroupDetails system = null;

		for (GroupDetails group : childs)
		{
			if (includeInternals == false && group.isInternal()) continue;
			if (onlyAvailableInMenu && group.getMenuType()==GroupDetails.MENU_TYPE_HIDDEN) continue;

			if ("cloud".equals(Constants.getInstallName()) && "System".equals(group.getGroupName()))
			{
				//system grupu presunieme uplne na koniec
				system = group;
				continue;
			}

			p_groups.add(group);

			if (onlyAvailableInMenu && group.getMenuType()==GroupDetails.MENU_TYPE_NOSUB) continue;

			//recursive call
			getGroupsTree(childsTable, group.getGroupId(), p_groups, includeInternals, onlyAvailableInMenu, maxDepth, iteration+1);
		}

		if (system != null)
		{
			p_groups.add(system);
			if (onlyAvailableInMenu && system.getMenuType()==GroupDetails.MENU_TYPE_NOSUB)
			{
				//continue;
			}
			else
			{
				//recursive call
				getGroupsTree(childsTable, system.getGroupId(), p_groups, includeInternals, onlyAvailableInMenu, maxDepth, iteration+1);
			}
		}
	}

	/**
	 * Vytvori List parent adresarov pre rendering v select boxe namiesto standardneho vyberu, kazde meno adresara je odsadene medzerami aby bola zrozumitelna struktura
	 * @param parent
	 * @param includeParent
	 * @param includeInternals
	 * @param onlyAvailableInMenu
	 * @return
	 */
	public List<LabelValueDetails> getGroupsTreeForSelect(int parent, boolean includeParent, boolean includeInternals, boolean onlyAvailableInMenu)
	{
		List<LabelValueDetails> list = new ArrayList<>();

		int level = 0;
		if (includeParent)
		{
			GroupDetails parentGroup = findGroup(parent);
			if (parentGroup != null) list.add(new LabelValueDetails(parentGroup.getGroupName(), String.valueOf(parentGroup.getGroupId())));
			level++;
		}

		Map<Integer, List<GroupDetails>> childsTable = prepareChildsTable();
		getGroupsTreeForSelect(childsTable, parent, list, includeInternals, onlyAvailableInMenu, level);

		return list;
	}

	/**
	 * Vytvori List parent adresarov pre rendering v select boxe namiesto standardneho vyberu, kazde meno adresara je odsadene medzerami aby bola zrozumitelna struktura
	 * @param childsTable
	 * @param parent
	 * @param list
	 * @param includeInternals
	 * @param onlyAvailableInMenu
	 * @param level
	 */
	private void getGroupsTreeForSelect(Map<Integer, List<GroupDetails>> childsTable, int parent, List<LabelValueDetails> list, boolean includeInternals, boolean onlyAvailableInMenu, int level)
	{
		List<GroupDetails> childs = childsTable.get(parent);
		if (childs == null) return;

		GroupDetails system = null;

		StringBuilder spacer = new StringBuilder();
		for (int i=0; i<level; i++)
		{
			spacer.append(Constants.NON_BREAKING_SPACE);
			spacer.append(Constants.NON_BREAKING_SPACE);
			spacer.append(Constants.NON_BREAKING_SPACE);
			spacer.append(Constants.NON_BREAKING_SPACE);
			spacer.append(Constants.NON_BREAKING_SPACE);
		}

		for (GroupDetails group : childs)
		{
			if (includeInternals == false && group.isInternal()) continue;
			if (onlyAvailableInMenu && group.getMenuType()==GroupDetails.MENU_TYPE_HIDDEN) continue;

			if ("cloud".equals(Constants.getInstallName()) && "System".equals(group.getGroupName()))
			{
				//system grupu presunieme uplne na koniec
				system = group;
				continue;
			}

			list.add(new LabelValueDetails(spacer.toString()+group.getGroupName(), String.valueOf(group.getGroupId())));

			if (onlyAvailableInMenu && group.getMenuType()==GroupDetails.MENU_TYPE_NOSUB) continue;

			//recursive call
			getGroupsTreeForSelect(childsTable, group.getGroupId(), list, includeInternals, onlyAvailableInMenu, level+1);
		}

		if (system != null)
		{
			list.add(new LabelValueDetails(spacer.toString()+system.getGroupName(), String.valueOf(system.getGroupId())));
			if (onlyAvailableInMenu && system.getMenuType()==GroupDetails.MENU_TYPE_NOSUB)
			{
				//continue;
			}
			else
			{
				//recursive call
				getGroupsTreeForSelect(childsTable, system.getGroupId(), list, includeInternals, onlyAvailableInMenu, level+1);
			}
		}
	}

	/**
	 *  najde GroupDetails pre zadane groupId
	 *
	 *@param  groupId  Description of the Parameter
	 *@return          Description of the Return Value
	 */
	public GroupDetails findGroup(int groupId)
	{
		return getGroup(groupId);
	}

	public static GroupDetails findGroup(List<GroupDetails> allGroups, int groupId)
	{
		for (GroupDetails group : allGroups)
		{
			if (group.getGroupId() == groupId)
			{
				return (group);
			}
		}
		return (null);
	}

	/**
	 * Ulozenie adresara do databazy
	 * @param group
	 * @return
	 */
	public boolean save(GroupDetails group)
	{
		return setGroup(group);
	}

	/**
	 * Ulozenie adresara do databazy
	 * @param group
	 * @return
	 */
	public boolean setGroup(GroupDetails group)
	{
		return setGroup(group, true);
	}

	/**
	 * Ulozenie adresara, ak je nastavene doNotPublishEvents na true nie su vyvolane udalosti
	 * @param group
	 * @param publishEvents - ak je true, su vyvolane udalosti (false potrebne ak napr. reagujeme na udalost a potrebujeme znova upravit adresar a nechceme aby doslo k zacykleniu)
	 * @return
	 */
	public boolean setGroup(GroupDetails group, boolean publishEvents)
	{
		if (publishEvents) (new WebjetEvent<GroupDetails>(group, WebjetEventType.ON_START)).publishEvent();

		if (InitServlet.isTypeCloud() && group.getGroupId()>0 && Tools.isNotEmpty(group.getDomainName()))
		{
			//kontrola prav
			GroupDetails oldGroup = getGroup(group.getGroupId());
			if (oldGroup == null || oldGroup.getDomainName().equals(CloudToolsForCore.getDomainName())==false)
			{
				return false;
			}
		}

		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			GroupDetails newGroup = (GroupDetails)group.clone();	//Hlasilo CloneNotSupportedException

			if (InitServlet.isTypeCloud())
			{
				newGroup.setDomainName(CloudToolsForCore.getDomainName());

				if (newGroup.getParentGroupId()>1)
				{
					GroupDetails parentGroup = getGroup(newGroup.getParentGroupId());
					if (parentGroup == null || parentGroup.getDomainName().equals(newGroup.getDomainName())==false)
					{
						newGroup.setParentGroupId(CloudToolsForCore.getDomainId());
					}
				}
			}

			//nahradim vyskyty "/" a "\" za "-"
			newGroup.setGroupName(Tools.replace(Tools.replace(newGroup.getGroupName(), "\\", "-"), "/", "&#47;"));
			newGroup.setNavbar(Tools.replace(Tools.replace(newGroup.getNavbarName(), "\\", "-"), "/", "&#47;"));

			//premenovanie nazvu hlavnej stranky Grupy, podla nazvu Grupy
			if(newGroup.getDefaultDocId() > 0 && GroupsService.canSyncTitle(newGroup)) {
				changeDocTitle(newGroup);
				DocDB.getInstance(true);
			}

			Logger.println(this,"GroupsDB.setGroup: " + newGroup.getGroupName());

			String[] additionalFields = DataAccessHelper.getGroupFields();
			StringBuilder addFieldsInsert = new StringBuilder();
			StringBuilder addFieldsInsertParams = new StringBuilder();
			StringBuilder addFieldsUpdate = new StringBuilder();
			if (additionalFields != null && additionalFields.length>0) {
				for (String field : additionalFields) {
					addFieldsInsert.append(", ").append(field);
					addFieldsInsertParams.append(", ?");
					addFieldsUpdate.append(", ").append(field).append("=?");
				}
			}

			String sql = "INSERT INTO groups (group_name, internal, parent_group_id, navbar, default_doc_id, temp_id, sort_priority, password_protected, menu_type, url_dir_name, html_head, logon_page_doc_id, domain_name, new_page_docid_template, install_name, field_a, field_b, field_c, field_d, logged_menu_type, link_group_id, lng, hidden_in_admin, force_group_template"+addFieldsInsert.toString()+") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"+addFieldsInsertParams.toString()+")";
			if (newGroup.getGroupId()>0)
			{
				sql = "UPDATE groups SET group_name=?, internal=?, parent_group_id=?, navbar=?, default_doc_id=?, temp_id=?, sort_priority=?, password_protected=?, menu_type=?, url_dir_name=?, sync_status=1, html_head=?, logon_page_doc_id=?, domain_name=?, new_page_docid_template=?, install_name=?, field_a=?, field_b=?, field_c=?, field_d=?, logged_menu_type=?, link_group_id=?, lng=?, hidden_in_admin=?, force_group_template=?"+addFieldsUpdate.toString()+" WHERE group_id=?";
			}

			db_conn = DBPool.getConnection();

			ps = db_conn.prepareStatement(sql);
			ps.setString(1, newGroup.getGroupName());
			ps.setBoolean(2, newGroup.isInternal());
			ps.setInt(3, newGroup.getParentGroupId());
			ps.setString(4, newGroup.getNavbarName());
			ps.setInt(5, newGroup.getDefaultDocId());
			ps.setInt(6, newGroup.getTempId());
			ps.setInt(7, newGroup.getSortPriority());
			ps.setString(8, newGroup.getPasswordProtected());
			//#20018, #15784 - pre Plusku ak je nazov noveho adresara cislo, tak daj Nezobrazovat a URL nazov adresara na pomlcku
			if(Tools.getIntValue(newGroup.getGroupName(),0) > 0 && Constants.getBoolean("editorGroupReplaceNumberWithDashInUrl"))
			{
				newGroup.setMenuType(0);
				newGroup.setUrlDirName("-");
				Logger.println(GroupsDB.class, "nazov adresara "+newGroup.getGroupName()+" {id:"+newGroup.getGroupId()+"} je cislo, nastavujem urlDirName na pomlcku a menuType=0");
			}

			ps.setInt(9, newGroup.getMenuType());
			ps.setString(10, newGroup.getUrlDirName());
			DB.setClob(ps, 11, newGroup.getHtmlHead());
			ps.setInt(12, newGroup.getLogonPageDocId());

			//domain_name, new_page_docid_template, install_name, field_a, field_b, field_c, field_d
			if (Constants.getBoolean("multiDomainEnabled") && newGroup.getParentGroupId()>0)
			{
				if (Tools.isEmpty(newGroup.getDomainName()) || Constants.getBoolean("multiDomainEnableNested")==false) {
					//ak sa niekde zabudla nastavit domena / alebo nie je zapnute nested, pouzijem parent
					GroupDetails parentGroup = getGroup(newGroup.getParentGroupId());
					if (parentGroup != null && Tools.isNotEmpty(parentGroup.getDomainName()))
					{
						newGroup.setDomainName(parentGroup.getDomainName());
					}
				}
			}
			ps.setString(13, newGroup.getDomainName());
			ps.setInt(14, newGroup.getNewPageDocIdTemplate());
			ps.setString(15, newGroup.getInstallName());
			ps.setString(16, newGroup.getFieldA());
			ps.setString(17, newGroup.getFieldB());
			ps.setString(18, newGroup.getFieldC());
			ps.setString(19, newGroup.getFieldD());

			ps.setInt(20, newGroup.getLoggedMenuType());
			ps.setInt(21, newGroup.getLinkGroupId());

			ps.setString(22, newGroup.getLng());
			ps.setBoolean(23, newGroup.isHiddenInAdmin());

			ps.setBoolean(24, newGroup.isForceTheUseOfGroupTemplate());

			int psCounter = DataAccessHelper.setGroupPreparedStatement(ps, newGroup, 25);

			if (newGroup.getGroupId()>0)
			{
				ps.setInt(psCounter++, newGroup.getGroupId());
			}

			ps.execute();
			ps.close();
			ps = null;

			boolean isNew = newGroup.getGroupId()<1;
			if (isNew)
			{
				//ziskaj group ID
				sql = "SELECT max(group_id) AS group_id FROM groups WHERE group_name=? AND parent_group_id=?";
				ps = db_conn.prepareStatement(sql);
				ps.setString(1, newGroup.getGroupName());
				ps.setInt(2, newGroup.getParentGroupId());
				rs = ps.executeQuery();
				if (rs.next())
				{
					newGroup.setGroupId(rs.getInt("group_id"));
					group.setGroupId(newGroup.getGroupId());
				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;
			}

			db_conn.close();
			db_conn = null;

			//ak su nastavene syncId tak zapis aj to, potrebne pre synchronizaciu struktury
			if (newGroup.getSyncId()>0) {
				new SimpleQuery().execute("UPDATE groups SET sync_id=? WHERE group_id=?", newGroup.getSyncId(), newGroup.getGroupId());
			}

			//groups_scheduler(history)
			int userId = -1;
			RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
			if(rb != null)
				userId = rb.getUserId();

			GroupPublisher.addRecord(newGroup, null, userId);
			//GroupPublisher.addRecord(newGroup, null);

			StringBuilder logMessage = new StringBuilder();
			//refresh data
			if (isNew)
			{
				insertGroupInCache(newGroup);
				//nastavovanie full pathu musi byt aj v if aj v else kvoli logovaniu...
				newGroup.setFullPath(getPath(newGroup.getGroupId()));
				group.setFullPath(newGroup.getFullPath());
				logMessage.append("Vytvoreny adresar ").append(newGroup.getFullPath());
			}
			else
			{
				//vymaz staru a vloz novu - mozu ist na ine miesto v zozname
				GroupDetails oldGroup = getGroup(newGroup.getGroupId());
				replaceGroupInCache(oldGroup, newGroup);
				newGroup.setFullPath(getPath(newGroup.getGroupId()));
				group.setFullPath(newGroup.getFullPath());

				BeanDiff diff = new BeanDiff().setNew(newGroup).setOriginal(oldGroup).blacklist("syncStatus", "groupIdName", "fullPath");
				logMessage.append("Zmeneny adresar: ").append(newGroup.getFullPath());
				logMessage.append('\n').append(new BeanDiffPrinter(diff).toString());
			}
			//zresetuj all tree cache
			groupsTreeAllCache = null;

			Adminlog.add(Adminlog.TYPE_GROUP, logMessage.toString(), newGroup.getGroupId(), newGroup.getParentGroupId());
			//oznam ostatnym, ze nastala zmena
			ClusterDB.addRefresh(GroupsDB.class);

			//aktualizuj FT fieldy
			DocDB.updateFileNameField(newGroup.getGroupId());

			//ak sa jedna o system adresar, refreshnem zoznam tychto stranok
			if("System".equalsIgnoreCase(newGroup.getGroupName())) getAllSystemFolders(true);

			if (publishEvents) (new WebjetEvent<GroupDetails>(newGroup, WebjetEventType.AFTER_SAVE)).publishEvent();

			return(true);
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return(false);
	}

	/**
	 *  Vrati meno grupy pre zadane group_id
	 *
	 *@param  groupId  id grupy
	 *@return          nazov grupy
	 */
	public String getGroupName(int groupId)
	{
		GroupDetails group = findGroup(groupId);
		if (group == null)
			return ("");
		return group.getGroupName();
	}

	/**
	 *  vrati zoznam potomkov
	 *
	 *@param  groupId     Description of the Parameter
	 *@param  noInternal  Description of the Parameter
	 *@return             Description of the Return Value
	 */
	public List<GroupDetails> findChilds(int groupId, boolean noInternal)
	{
		List<GroupDetails> ret = new ArrayList<>();
		for (GroupDetails group : groups)
		{
			if (group.getParentGroupId() == groupId)
			{
				if (noInternal == true)
				{
					if (!group.isInternal())
					{
						ret.add(group);
					}
				}
				else
				{
					ret.add(group);
				}

			}
		}
		return (ret);
	}

	/**
	 *  vrati cestu k zadanej skupine napr.: /iwcm/Produkty
	 *
	 *@param  groupId  Description of the Parameter
	 *@return          The path value
	 */
	public String getPath(int groupId)
	{
		if (groupId < 1) return("/");
		StringBuilder path = new StringBuilder();
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				path.insert(0, "/" + group.getGroupName());
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		return (path.toString());
	}

	/**
	 * Vrati zoznam GroupDetails ako cestu k zadanemu groupId
	 * @param groupId
	 * @return
	 */
	public List<GroupDetails> getPathList(int groupId)
	{
		List<GroupDetails> ret = new ArrayList<>();
		if (groupId < 1) return(ret);

		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				//pridavame na zaciatok
				ret.add(0, group);
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		return (ret);
	}

	public String getPathLinkForward(int groupId, String forward)
	{
		StringBuilder path = new StringBuilder();
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		String forwardLink = "";
		if (forward!=null)
		{
			forwardLink = "&forward="+forward;
		}
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				path.insert(0, "/<a class='groups' href='/admin/v9/webpages/web-pages-list/?groupid="+group.getGroupId()+forwardLink+"'>" + ResponseUtils.filter(group.getGroupName()) + "</a>");
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		return (path.toString());
	}

	/**
	 * Vrati cestu k suboru ak je v subor v ListGroup inak vrati cestu s groupid = 0 (root)
	 * @param groupId
	 * @param forward
	 * @param listGroup
	 * @return
	 */
	public String getPathLinkForwardExceptNotAllow(int groupId, String forward, List<GroupDetails> listGroup)
	{
		StringBuilder path = new StringBuilder();
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		String forwardLink = "";
		if (forward!=null)
		{
			forwardLink = "&forward="+forward;
		}
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				//is_allow
				String replaceID;
				if(  isAllow(listGroup, group.getGroupId())  )
					replaceID = group.getGroupId()+"";
				else
					replaceID = "0" ;
				path.insert(0, "/<a class='groups' href='/admin/v9/webpages/web-pages-list/?groupid="+replaceID+forwardLink+"'>" + group.getGroupName() + "</a>");
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		return (path.toString());
	}

	/**
	 * Vrati true ak v danom ListGroup je adresar s ID =  groupId
	 * @param ListGroup - zoznam vsetkych suborov
	 * @param groupId - id aktualneho adresara
	 */
	private static boolean isAllow(List<GroupDetails> listGroup,int groupId)
	{
		if(groupId == 0)
			return true;

		boolean finded = false;
		for(int i = 0; i < listGroup.size() ; i++)
		{
			if( (listGroup.get(i).getGroupId()+"").equals(groupId+"") )
				return true;
		}
		return finded;
	}
	/**
	 * Vrati naformatovanu cestu priorit k danemu adresaru (napr. 1.20.14)
	 * @param groupId
	 * @return
	 */
	public String getPathPriorityNumbers(int groupId)
	{
		StringBuilder path = new StringBuilder();
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				path.insert(0, group.getSortPriority() + ".");
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		return (path.toString());
	}

	/**
	 * Vrati retazec s cestou k adresaru (napr. /sk/nieco/podnieco).
	 * Pouziva UrlDirName groupDetails
	 * @param groupId
	 * @return
	 */
	public String getURLPath(int groupId)
	{
		return(getURLPathGroup(groups, groupId));
	}

	/**
	 * Vrati retazec s cestou k adresaru (napr. /sk/nieco/podnieco).
	 * @param allGroups
	 * @param groupId
	 * @return
	 */
	public static String getURLPathGroup(List<GroupDetails> allGroups, int groupId)
	{
		String[] retData = getURLPathDomainGroup(allGroups, groupId);
		return retData[0];
	}

	/**
	 * Vrati domenu pre zadane groupId
	 * @param groupId
	 * @return
	 */
	public String getDomain(int groupId)
	{
		if (InitServlet.isTypeCloud())
		{
			GroupDetails group = getGroup(groupId);
			if (group != null && Tools.isNotEmpty(group.getDomainName())) return group.getDomainName();
		}

		return(getDomain(groups, groupId));
	}

	/**
	 * Vrati domenu pre zadane groupId a zadany list
	 * @param allGroups
	 * @param groupId
	 * @return
	 */
	public static String getDomain(List<GroupDetails> allGroups, int groupId)
	{
		String[] retData = getURLPathDomainGroup(allGroups, groupId);
		return retData[1];
	}

	public static String[] getURLPathDomainGroup(List<GroupDetails> allGroups, int groupId)
	{
		Map<Integer, GroupDetails> table = new Hashtable<>();
		for (GroupDetails group : allGroups)
		{
			table.put(group.getGroupId(), group);
		}

		return getURLPathDomainGroup(table, groupId);
	}

	/**
	 * Vrati pole stringov, kde ret[0]=URL cesta k adresaru a ret[1]=nazov domeny root adresara
	 * @param allGroups
	 * @param groupId
	 * @return
	 */
	public static String[] getURLPathDomainGroup(Map<Integer, GroupDetails> allGroups, int groupId)
	{
		String[] retData = new String[2];

		StringBuilder path = new StringBuilder();
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;

		String domain = null;
		//pouzije sa ak je nastavene multiDomainEnableNested
		String domainOnFirstGroup = null;

		while (finished == false)
		{
			group = allGroups.get(Integer.valueOf(currentGroupId));

			if (group==null)
			{
				retData[0] = "--------------------------";
				retData[1] = "";
				return(retData);
			}
			else
			{
				if (domainOnFirstGroup == null && Tools.isNotEmpty(group.getDomainName()))
				{
					domainOnFirstGroup = group.getDomainName();
					domain = domainOnFirstGroup;
				}

				//Logger.println(this,"group="+group.getGroupId()+" p="+group.getParentGroupId()+" n="+group.getNavbarName());

				if (group.getUrlDirName().startsWith("/"))
				{
					//ak sa adresaru nastavi cesta zacinajuca / tak sa zrusi predchadzajuca URL cesta
					path.insert(0, group.getUrlDirName());
					finished = true;
				}
				else if("-".equals(group.getUrlDirName())==false)
				{
					path.insert(0, "/" + group.getUrlDirName());
				}
				currentGroupId = group.getParentGroupId();
			}


			//Logger.println(this,"path="+path);

			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
				if (group != null) domain = group.getDomainName();
			}
		}

		//ak sme skoncili iteraciu predcasne
		while (domain == null && depth++ < max_depth)
		{
			group = allGroups.get(Integer.valueOf(currentGroupId));
			if (group == null)
			{
				domain = "";
				break;
			}
			else
			{
				currentGroupId = group.getParentGroupId();

				if (currentGroupId == 0)
				{
					domain = group.getDomainName();
				}
			}
		}

		if (domain == null) domain="";

		if (Constants.getBoolean("multiDomainEnableNested")) domain = domainOnFirstGroup;

		retData[0] = Tools.replace(path.toString(), "//", "/");
		retData[1] = domain;
		return(retData);
	}

	/**
	 * Vrati cestu k adresaru s pouzitim NavbarName (napr. /Slovensky/O Nás/Kontakt)
	 * @param groupId
	 * @return
	 */
	public String getNavbarPath(int groupId)
	{
		StringBuilder path = new StringBuilder();
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;

		while (finished == false)
		{
			group = findGroup(currentGroupId);


			if (group==null)
			{
				return("--------------------------");
			}
			else
			{
				if ("-".equals(group.getUrlDirName())==false)
				{
					path.insert(0, "/" + group.getNavbarName());
				}
				currentGroupId = group.getParentGroupId();
			}


			//Logger.println(this,"path="+path);

			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}

		return (path.toString());
	}

	/**
	 * Vrati cestu k adresaru s pouzitim Title (napr. /Slovensky/O Nás/Kontakt)
	 * @param groupId
	 * @return
	 */
	public String getGroupNamePath(int groupId)
	{
		StringBuilder path = new StringBuilder();
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;

		while (finished == false)
		{
			group = findGroup(currentGroupId);

			if (group==null)
			{
				return("--------------------------");
			}
			else
			{
				path.insert(0, "/" + group.getGroupName());
				currentGroupId = group.getParentGroupId();
			}

			//Logger.println(this,"path="+path);

			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}

		return (path.toString());
	}

	/**
	 *  vrati navigacnu listu (podobne ako cestu) len je to iwcm > Produkty
	 *
	 *@param  groupId  Description of the Parameter
	 *@return          The navbar value
	 */
	public String getNavbar(int groupId)
	{
		return(getNavbar(groupId, -99, null));
	}

	/**
	 * Vrati navigacnu listup, pre zadane docId (aktualne) nezrenderuje odkaz
	 * @param groupId
	 * @param docId
	 * @param session
	 * @return
	 */
	public String getNavbar(int groupId, int docId, HttpSession session)
	{
		StringBuilder path = new StringBuilder();
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		while (finished == false && depth++<max_depth)
		{
			group = findGroup(currentGroupId);
			if (group != null && group.getGroupId()!=group.getParentGroupId())
			{
				if (group.isInternal()==true || group.getMenuType(session)==GroupDetails.MENU_TYPE_HIDDEN)
				{
					currentGroupId = group.getParentGroupId();
					continue;
				}

				if (group.getNavbar().length() > 1 && "&nbsp;".equals(group.getNavbarName())==false)
				{
					if (Constants.getBoolean("navbarRenderAllLinks")==false && group.getDefaultDocId()==docId)
					{
						String newPath = " "+Constants.getString("navbarSeparator")+" " + group.getNavbarName().replaceAll("(?i)<aparam.*>","");
						//ochrana pred duplikovanim cesty (ak mame root a v nom mame hlavnu stranku v podadresari s rovnakym nazvom)
						if (path.indexOf(newPath)!=0) path.insert(0, newPath);
					}
					else
					{
						String navbarName = group.getNavbar();
						if (navbarName.contains("*||")) navbarName = Tools.replace(navbarName, "*||", "</");
						if (navbarName.contains("*|")) navbarName = Tools.replace(navbarName, "*|", "<");
						if (navbarName.contains("|*")) navbarName = Tools.replace(navbarName, "|*", ">");
						String newPath = " "+Constants.getString("navbarSeparator")+" " + navbarName;
						if (path.indexOf(newPath)!=0) path.insert(0, newPath);
					}
				}
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		try
		{
			//odstran zobak na zaciatku
			if (path.indexOf(" "+Constants.getString("navbarSeparator")+" ")==0)
			{
				return path.toString().substring(Constants.getString("navbarSeparator").length() + 2).trim();
			}
		}
		catch (Exception ex)
		{
		}
		return (path.toString());
	}
	/**
	 * Vrati HTML kod pre Breadcrumb navigaciu vo formate schema.org
	 * @param groupId - ID adresara
	 * @param docId - ID aktualnej web stranky
	 * @param session
	 * @return
	 */
	public String getNavbarSchema(int groupId, int docId, HttpSession session)
	{
		StringBuilder htmlCode = new StringBuilder();
		DocDB docDB = DocDB.getInstance();

		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		int realMaxDepth = 1;

		// potrebujem zistit realny pocet urovni
		while (finished == false && depth++<max_depth)
		{
			group = findGroup(currentGroupId);
			if (group != null && group.getGroupId()!=group.getParentGroupId())
			{
				if (group.isInternal()==true || group.getMenuType(session)==GroupDetails.MENU_TYPE_HIDDEN)
				{
					currentGroupId = group.getParentGroupId();
					continue;
				}

				Map<String, String> aparams = parseAparam(group.getNavbarName());
				if (group.getDefaultDocId() > 0 && (!aparams.containsKey("class") || aparams.containsKey("class") && !aparams.get("class").equalsIgnoreCase("is-headline"))) {
					realMaxDepth++;
				}
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}

		depth = 0;
		currentGroupId = groupId;
		finished = false;
		int skippedCount = 0;

		while (finished == false && depth++<max_depth)
		{
			group = findGroup(currentGroupId);
			if (group != null && group.getGroupId()!=group.getParentGroupId())
			{
				if (group.isInternal()==true || group.getMenuType(session)==GroupDetails.MENU_TYPE_HIDDEN)
				{
					currentGroupId = group.getParentGroupId();
					skippedCount++;
					continue;
				}

				String groupNavbar = Tools.convertToHtmlTags(group.getNavbarName());

				Map<String, String> aparams = parseAparam(groupNavbar);
				if (group.getDefaultDocId() < 1 || aparams.containsKey("class") && aparams.get("class").equalsIgnoreCase("is-headline")) {
					currentGroupId = group.getParentGroupId();
					skippedCount++;
					continue;
				}

				if (group.getDefaultDocId() > 0 && groupNavbar.length() > 1 && (groupNavbar.indexOf("<a") < 0 || groupNavbar.toLowerCase().indexOf("<aparam") >= 0))
				{
					//odstrani <aparam>
					String text = groupNavbar.replaceAll("(?i)<aparam.*>", "");
					groupNavbar = "\n\t<li class=\"is-item\" itemprop=\"itemListElement\" itemscope itemtype=\"http://schema.org/ListItem\">" +
							"<a href='" + docDB.getDocLink(group.getDefaultDocId()) + "' class='navbar' itemprop=\"item\">" +
							"<span itemprop=\"name\">" + Tools.convertToHtmlTags(text) + "</span>" +
							"</a>" +
							"<meta itemprop=\"position\" content=\"" + (realMaxDepth - depth + skippedCount) + "\" /></li>";
				}

				if (groupNavbar.length() > 1 && "&nbsp;".equals(group.getNavbarName())==false)
				{
					if (Constants.getBoolean("navbarRenderAllLinks")==false && group.getDefaultDocId()==docId)
					{
						String text = group.getNavbarName().replaceAll("(?i)<aparam.*>", "");
						String newPath = Tools.convertToHtmlTags(text);

						//ochrana pred duplikovanim cesty (ak mame root a v nom mame hlavnu stranku v podadresari s rovnakym nazvom)
						if (htmlCode.indexOf(newPath)!=0) htmlCode.insert(0, newPath);
					}
					else
					{
						//tu nepotrebujeme nahradu, tu uz len pridavame moznosti dokopy
						String newPath = groupNavbar;

						if (htmlCode.indexOf(newPath)!=0) htmlCode.insert(0, newPath);
					}
				}
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		try
		{
			//odstran zobak na zaciatku
			if (htmlCode.indexOf(" "+Constants.getString("navbarSeparator")+" ")==0)
			{
				htmlCode = new StringBuilder(htmlCode.toString().substring(Constants.getString("navbarSeparator").length() + 2).trim());
			}
		}
		catch (Exception ex)
		{
		}

		htmlCode.insert(0, "\n<ol itemscope itemtype=\"http://schema.org/BreadcrumbList\">");
		htmlCode.append("\n</ol>");
		return (htmlCode.toString());
	}

	protected Map<String, String> parseAparam(String str) {
		if (!str.contains("<aparam")) {
			return Collections.emptyMap();
		}

		try {
			String string = str.substring(str.indexOf("<aparam"), str.length());
			string = string.substring(0, string.indexOf(">") - 1);
			string = string.substring(7, string.length()).trim();

			String[] arr = Tools.getTokens(string, "");
			Map<String, String> result = new HashMap<>();

			for (String item : arr) {
				String[] items = Tools.getTokens(item, "=");
				String value = items[1] != null ? Tools.replace(items[1], "\"", "") : "";
				result.put(items[0], value);
			}

			return result;
		}
		catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
			return Collections.emptyMap();
		}
	}

	/**
	 * Vrati HTML kod pre Breadcrumb navigaciu vo formate RDF
	 * http://support.google.com/webmasters/bin/answer.py?hl=en&amp;topic=1088474&amp;hlrm=en&amp;answer=185417&amp;ctx=topic
	 * @param groupId - ID adresara
	 * @param docId - ID aktualnej web stranky
	 * @param session
	 * @return
	 */
	public String getNavbarRDF(int groupId, int docId, HttpSession session)
	{
		StringBuilder htmlCode = new StringBuilder();
		DocDB docDB = DocDB.getInstance();

		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		while (finished == false && depth++<max_depth)
		{
			group = findGroup(currentGroupId);
			if (group != null && group.getGroupId()!=group.getParentGroupId())
			{
				if (group.isInternal()==true || group.getMenuType(session)==GroupDetails.MENU_TYPE_HIDDEN)
				{
					currentGroupId = group.getParentGroupId();
					continue;
				}

				String groupNavbar = group.getNavbarName();
				if (group.getDefaultDocId() > 0 && groupNavbar.length() > 1 && (groupNavbar.indexOf("<a") < 0 || groupNavbar.toLowerCase().indexOf("<aparam") >= 0))
				{
					//odstrani <aparam>
					String text = groupNavbar.replaceAll("(?i)<aparam.*>","");
					groupNavbar = "<span typeof=\"v:Breadcrumb\"><a href='"+docDB.getDocLink(group.getDefaultDocId())+"' class='navbar' rel=\"v:url\" property=\"v:title\">" + text + "</a></span>";
				}

				if (groupNavbar.length() > 1 && "&nbsp;".equals(group.getNavbarName())==false)
				{
					if (Constants.getBoolean("navbarRenderAllLinks")==false && group.getDefaultDocId()==docId)
					{
						String newPath = " "+Constants.getString("navbarSeparator")+" " + group.getNavbarName().replaceAll("(?i)<aparam.*>","");
						//ochrana pred duplikovanim cesty (ak mame root a v nom mame hlavnu stranku v podadresari s rovnakym nazvom)
						if (htmlCode.indexOf(newPath)!=0) htmlCode.insert(0, newPath);
					}
					else
					{
						String newPath = " "+Constants.getString("navbarSeparator")+" " + groupNavbar;
						if (htmlCode.indexOf(newPath)!=0) htmlCode.insert(0, newPath);
					}
				}
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		try
		{
			//odstran zobak na zaciatku
			if (htmlCode.indexOf(" "+Constants.getString("navbarSeparator")+" ")==0)
			{
				htmlCode = new StringBuilder(htmlCode.toString().substring(Constants.getString("navbarSeparator").length() + 2).trim());
			}
		}
		catch (Exception ex)
		{
		}

		htmlCode.insert(0, "<div class=\"breadcrumbrdf\" xmlns:v=\"http://rdf.data-vocabulary.org/#\">");
		htmlCode.append("</div>");
		return (htmlCode.toString());
	}


	/**
	 *  to iste ako getNavbar, len do ciest nedava linky (&lt;a href...)
	 *
	 *@param  groupId  Description of the Parameter
	 *@return          The navbarNoHref value
	 */
	public String getNavbarNoHref(int groupId)
	{
		StringBuilder path = new StringBuilder();
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				if (group.getNavbar().length() > 1)
				{
					path.insert(0, " > " + group.getNavbarName());
				}
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		try
		{
			//odstran zobak na zaciatku
			if (path.indexOf(" > ")==0)
			{
				path = new StringBuilder(path.toString().substring(3).trim());
			}
		}
		catch (Exception ex)
		{
		}

		path = Tools.replace(path, "href=", "hrf=");

		return (path.toString());
	}

	/**
	 *  vrati zoznam parent groups (pre pristupove prava)
	 *
	 *@param  groupId  Description of the Parameter
	 *@return          The parents value
	 */
	public String getParents(int groupId)
	{
		StringBuilder path = new StringBuilder("");
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				path.insert(0, group.getGroupId()).insert(0, ",");
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		try
		{
			//odstran zobak na zaciatku
			if (path.toString().startsWith(","))
			{
				path.deleteCharAt(0);
			}
		}
		catch (Exception ex)
		{
		}
		return (path.toString());
	}

	/**
	 * Vrati htmlHead atribut, hlada ho aj v nadradenych adresaroch
	 * @param groupId
	 * @return
	 */
	public String getHtmlHeadRecursive(int groupId)
	{
		String htmlHead = null;
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				if (group.getHtmlHead()!=null && group.getHtmlHead().length() > 1)
				{
					htmlHead = group.getHtmlHead();
					finished = true;
				}
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		if (htmlHead==null)
		{
			htmlHead = "";
		}
		return (htmlHead);
	}

	/**
	 * Rekurzivne vrati akukolvek property pre zadany adresar
	 * @param groupId
	 * @param propertyName
	 * @return
	 */
	public String getPropertyRecursive(int groupId, String propertyName)
	{
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		String value;
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				try
				{
					Object o = BeanUtils.getProperty(group, propertyName);
					//Logger.debug(GroupsDB.class, "o="+o.getClass());
					value = (String)o;
					//Logger.debug(GroupsDB.class, "value="+value);
					if (Tools.isNotEmpty(value) && "0".equals(value)==false)
					{
						return(value);
					}
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		return ("");
	}

	/**
	 * Rekurzivne vrati id adresara v ktorom zadana property nadobuda hladanu hodnotu
	 * @param groupId
	 * @param propertyName
	 * @param propertyValue
	 * @return
	 */
	public int getGroupIdByPropertyValue(int groupId, String propertyName, String propertyValue)
	{
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		String value;
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				try
				{
					Object o = BeanUtils.getProperty(group, propertyName);
					Logger.debug(GroupsDB.class, "o="+o.getClass());
					value = (String)o;
					Logger.debug(GroupsDB.class, "value="+value);
					if (Tools.isNotEmpty(value) && "0".equals(value)==false && propertyValue.indexOf(value) != -1)
					{
						return(currentGroupId);
					}
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		return -1;
	}

	/**
	 * Rekurzivne ziska docid prihlasovacieho dialogu
	 * @param groupId
	 * @return
	 */
	public int getRecursiveLogonPageDocId(int groupId)
	{
		int logonPageDocId = -1;
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				if (group.getLogonPageDocId() > 0)
				{
					//Logger.println(this,"logonPageDocId="+logonPageDocId+" gid="+group.getGroupId());
					logonPageDocId = group.getLogonPageDocId();
					finished = true;
				}
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		return (logonPageDocId);
	}

	/**
	 * vrati rootGrupu pre zadane groupId (pouziva sa koli jazykovym mutaciam)
	 * @param groupId
	 * @return
	 */
	public int getRoot(int groupId)
	{
		int root = groupId;
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				if (group.getNavbar().length() > 1)
				{
					root = group.getGroupId();
				}
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}
		return (root);
	}


	/**
	 *  Vrati List zo zoznamom rodicovskych skupin (cesta k root adresaru)
	 *  VYNECHA tie, ktore maju prazdnu hodnotu v poli navbar
	 *
	 *@param  groupId  Description of the Parameter
	 *@return          The parentGroups value
	 */
	public List<GroupDetails> getParentGroups(int groupId) {
		return getParentGroups(groupId, false);
	}

	/**
	 * Vrati zoznam rodicovskych adresarov (vratane zadaneho groupId)
	 * @param groupId
	 * @param includeWithEmptyNavbar - ak je false pridaju sa do zoznamu len adresare, ktore naju neprazdne pole navbar
	 * @return
	 */
	public List<GroupDetails> getParentGroups(int groupId, boolean includeWithEmptyNavbar)
	{
		boolean finished = false;
		int currentGroupId = groupId;
		int depth = 0;
		int max_depth = 30;
		GroupDetails group;
		List<GroupDetails> ret = new ArrayList<>();
		while (finished == false)
		{
			group = findGroup(currentGroupId);
			if (group != null)
			{
				if (includeWithEmptyNavbar || group.getNavbar().length() > 1)
				{
					//path = path + "," + group.getGroupId();
					ret.add(group);
				}
				currentGroupId = group.getParentGroupId();
			}
			else
			{
				//group doesn't exist
				finished = true;
			}
			depth++;
			if (depth > max_depth)
			{
				finished = true;
			}
			//we are on the root group
			if (currentGroupId == 0)
			{
				finished = true;
			}
		}

		return (ret);
	}

	/**
	 *  vrati nacitane skupiny, je to tu takto ako ArrayList kvoli legacy JSP kodu
	 *
	 *@return    The groups value
	 *@deprecated - pouzivajte verziu getGroupsAll
	 */
	@Deprecated
	public List<GroupDetails> getGroups()
	{
		List<GroupDetails> arlist = new ArrayList<>();
		arlist.addAll(groups);
		return arlist;
	}

	/**
	 * Vrati vsetky nacitane skupiny
	 * @return
	 */
	public List<GroupDetails> getGroupsAll()
	{
		return groups;
	}

	/**
	 *  skontroluje ci uz neexistuje skupina s danym menom v danej skupine
	 *
	 *@param  parentId  id rodica skupiny
	 *@param  name       meno novej skupiny
	 *@return            true ak uz existuje, false inak
	 */
	public boolean checkExist(int parentId, String name)
	{
		for (GroupDetails group : getGroupsAll())
		{
			if (group.getParentGroupId() == parentId && group.getGroupName().compareToIgnoreCase(name) == 0)
			{
				return (true);
			}
		}
		return (false);
	}

	/**
	 *  skontroluje ci uz existuje skupina s danym menom v danej skupine a vrati
	 *  jej id (ak existuje)
	 *
	 *@param  name             meno novej skupiny
	 *@param  parent_group_id  Description of the Parameter
	 *@return                  id skupiny ak existuje, -1 inak
	 */
	public int checkExist2(int parent_group_id, String name)
	{
		for (GroupDetails group : getGroupsAll())
		{
			if (group.getParentGroupId() == parent_group_id && group.getGroupName().compareToIgnoreCase(name) == 0)
			{
				return group.getGroupId();
			}
		}
		return -1;
	}

	/**
	 * Vrati String s rekurzivny zoznamom groupId, ktore mame zadane ako origPerexGroup. Pouziva sa v JSP
	 * strankach pri volani DocDB.getDocPerex()
	 * @param origGroupIds
	 * @return
	 */
	public static String getRecursiveGroupsSqlIn(String origGroupIds)
	{
		//ziskaj rekurzivne stranky
		StringTokenizer st = new StringTokenizer(origGroupIds, ",");
		int groupId;
		List<GroupDetails> childs;
		GroupsDB groupsDB = GroupsDB.getInstance();
		StringBuilder groupIds = null;
		while (st.hasMoreTokens())
		{
			try
			{
				groupId = Integer.parseInt(st.nextToken());
				childs = groupsDB.getGroupsTree(groupId, true, false);
				for (GroupDetails group : childs)
				{
					if (groupIds == null)
					{
						groupIds = new StringBuilder(String.valueOf(group.getGroupId()));
					}
					else
					{
						groupIds.append(",").append(String.valueOf(group.getGroupId()));
					}
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}
		if (groupIds==null) return null;
		return(groupIds.toString());
	}

	/**
	 * Vrati grupu podla fullPath (ak je zadane) alebo podla syncId (ak nenajde podla fullPath)
	 * @param fullPath - hodnota fullPath na remote serveri (alebo null, ak chceme hladat len podla syncId)
	 * @param syncId - hodnota groupId na remote serveri
	 * @return
	 */
	public GroupDetails getGroupBySync(String fullPath, int syncId)
	{

		if (Tools.isNotEmpty(fullPath))
		{
			for (GroupDetails group : groups)
			{
				if (group.getFullPath().equalsIgnoreCase(fullPath))
				{
					return(group);
				}
			}
		}

		if (syncId == 0) return(null);

		for (GroupDetails group : groups)
		{
			if (group.getSyncId() == syncId)
			{
				return(group);
			}
		}
		return (null);
	}

	/**
	 * Vrati/vytvori skupinu podla zadanej cesty
	 * @param path - cesta vo formate /adresar1/adresar2
	 * @return
	 */
	public GroupDetails getCreateGroup(String path)
	{
		//check if path is number, then get GroupDetails by ID
		try {
			int groupId = Integer.parseInt(path);
			return(getGroup(groupId));
		} catch(Exception ex) {
			//not a number, continue
		}

		int domainSeparatorIndex = path.indexOf(":");
		if (domainSeparatorIndex>0) {
			String domain = path.substring(0, domainSeparatorIndex);
			RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
			if (rb != null) {
				rb.setDomain(domain);
			}
			path = path.substring(domainSeparatorIndex+1);
		}

		if (path.contains("/")==false) {
			return null;
		}

		int actualParent = 0;
		if(InitServlet.isTypeCloud())
		{
			//ak sa jedna napr. o /System/Kos tak sa nastavi parent na ID hlavneho adresara namiesto na 0 (globalny System folder)
			if (path.startsWith("/"+CloudToolsForCore.getDomainName())==false)	actualParent = CloudToolsForCore.getDomainId();
		}
		else if (Constants.getBoolean("templatesUseDomainLocalSystemFolder")==true && path.startsWith("/System/"))
		{
			//overenie lokalneho /System/Kos adresara
			Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
			String trashDirName = propSystem.getText("config.trash_dir");
			if (path.equals(trashDirName))
			{
				GroupDetails localSystemGroup = getLocalSystemGroup();
				//pre pripad ze je /System priecinok niekde v subdomene, napr. /English/System/ zaciname hladat v /English priecinku (povazujeme ho za root)
				if (localSystemGroup != null && localSystemGroup.getParentGroupId()>0) actualParent = localSystemGroup.getParentGroupId();
			}
		}

		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		String actualGroupName;
		StringTokenizer st = new StringTokenizer(path, "/");
		GroupDetails actualGroup;
		GroupDetails parentGroup = null;
		while (st.hasMoreTokens())
		{
			actualGroupName = st.nextToken();

			//ak by boli za sebou nahodou //
			if (Tools.isEmpty(actualGroupName)) continue;

			if ("System".equals(actualGroupName) && actualParent==0 && Constants.getBoolean("templatesUseDomainLocalSystemFolder")) actualGroup = getLocalSystemGroup();
            else actualGroup = getGroup(actualGroupName, actualParent);

			if (actualGroup == null)
			{
				actualGroup = new GroupDetails();
				//vytvor skupinu, nastav prava podla parenta
				if (parentGroup != null)
				{
					try
					{
						BeanUtils.copyProperties(actualGroup, parentGroup);
					}
					catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
					}
					actualGroup.setParentGroupId(parentGroup.getGroupId());
					actualGroup.setSyncId(0);
				} else {
					if (rb != null && Tools.isNotEmpty(rb.getDomain()) && Constants.getBoolean("multiDomainEnabled")==true) {
						//nastav domenu
						actualGroup.setDomainName(rb.getDomain());
					}
				}

				actualGroup.setGroupId(-1);
				actualGroup.setDefaultDocId(-1);
				actualGroup.setGroupName(actualGroupName);
				actualGroup.setNavbar(actualGroupName);
				actualGroup.setUrlDirName(actualGroupName);

				actualGroup.setSortPriority(getDefaultSortPriority(actualGroup.getParentGroupId()));

				setGroup(actualGroup);
			}

			if (st.hasMoreTokens()==false)
			{
				//sme na konci
				return(actualGroup);
			}

			actualParent = actualGroup.getGroupId();
			parentGroup = actualGroup;
		}
		return(null);
	}

	/**
	 * Ziska docId sablony novej stranky pre zadany adresar (zvycajne vrati -1 = prazdna stranka)
	 * @param groupId
	 * @return
	 */
	public static int getNewPageDocIdTemplate(int groupId)
	{
		GroupsDB groupsDB = GroupsDB.getInstance();

		int newPageDocIdTemplate = Constants.getInt("newDocumentId");
		int groupNewPageDocIdTemplate = Tools.getIntValue(groupsDB.getPropertyRecursive(groupId, "newPageDocIdTemplate"), 0);
		if (groupNewPageDocIdTemplate!=0)
		{
			if (groupNewPageDocIdTemplate == -1)
			{
				//prehladaj podla nazvu sablony
				GroupDetails actualGroup = groupsDB.getGroup(groupId);
				int tempId = actualGroup.getTempId();
				TemplateDetails temp = TemplatesDB.getInstance().getTemplate(tempId);
				DocDB docDB = DocDB.getInstance();
				List<DocDetails> temps = docDB.getDocByGroup(Constants.getInt("tempGroupId"));
				if (temps!=null && temp!=null)
				{
					String tempName = DB.internationalToEnglish(temp.getTempName());
					for (DocDetails doc : temps)
					{
						if (DB.internationalToEnglish(doc.getTitle()).equalsIgnoreCase(tempName))
						{
							newPageDocIdTemplate = -doc.getDocId();
							break;
						}
					}
				}
			}
			else
			{
				newPageDocIdTemplate = -groupNewPageDocIdTemplate;
			}
		}
		return(newPageDocIdTemplate);
	}

	/**
	 * Vymazanie adresara
	 * @param groupId - id adresara
	 * @param request - request (treba kvoli admin logu, ale moze byt aj null)
	 * @return
	 */
	public static boolean deleteGroup(int groupId, HttpServletRequest request)
	{
		return deleteGroup(groupId, request, true, false);
	}
	/**
	 * Vymazanie adresara
	 * @param groupId - id adresara
	 * @param request - request (treba kvoli admin logu, ale moze byt aj null)
	 * @param includeParent - urci, ci ma pri vymazani brat aj rodicovsky adresar
	 * @param permanentlyDelete - nevlozi do kosa, ale priamo vymaze
	 * @return
	 */
	public static boolean deleteGroup(int groupId, HttpServletRequest request, boolean includeParent, boolean permanentlyDelete)
	{
		return deleteGroup(groupId, includeParent, permanentlyDelete, true);
	}

	private static void deleteGroupsApprove(String groups) {
		(new SimpleQuery()).execute("DELETE FROM groups_approve WHERE group_id IN ("+groups+")");
	}

	/**
	 * Vymazanie adresara
	 * @param groupId - id adresara
	 * @param includeParent - urci, ci ma pri vymazani brat aj rodicovsky adresar
	 * @param permanentlyDelete - nevlozi do kosa, ale priamo vymaze
	 * @param publishEvents - ak je true, su vyvolane udalosti (false potrebne ak napr. reagujeme na udalost a potrebujeme znova upravit adresar a nechceme aby doslo k zacykleniu)
	 * @return
	 */
	public static boolean deleteGroup(int groupId, boolean includeParent, boolean permanentlyDelete, boolean publishEvents)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		GroupsDB groupsDB = GroupsDB.getInstance();

		//kontrola prav
		GroupDetails group = groupsDB.getGroup(groupId);

		if (InitServlet.isTypeCloud())
		{
			if (group == null || group.getDomainName().equals(CloudToolsForCore.getDomainName())==false)
			{
				return false;
			}
		}

		try
		{
			if (group!=null && publishEvents) {
				(new WebjetEvent<GroupDetails>(group, WebjetEventType.ON_DELETE)).publishEvent();
			}

			//	zmazanie grupy
			db_conn = DBPool.getConnection();

			// zisti ci sme v adresari /System/Trash (kos), ak nie presun, inak vymaz
			Logger.debug(GroupsDB.class, "MAZEM: " + group);

			boolean foundSystemDir = false;
			GroupDetails trashGroupDetails = null;
			boolean disableHistory = Constants.getBoolean("editorDisableHistory");
			if (disableHistory == false)  {
				trashGroupDetails = groupsDB.getTrashGroup();
			}

			if (permanentlyDelete || trashGroupDetails == null || trashGroupDetails.getGroupId()==groupId || groupsDB.isInTrash(groupId))
			{
      			// ziskaj zoznam groups (tejto a podskupin)
				StringBuilder groups = new StringBuilder();
				//List subGroups = groupsDB.getGroups(my_form.getGroupId());
				List<GroupDetails> subGroups = groupsDB.getGroupsTree(groupId, includeParent, true);

				for (GroupDetails element : subGroups)
				{
					if (groups.length() > 0)
						groups.append(',');
					groups.append(element.getGroupId());
					//refresh cache
					groupsDB.removeGroupFromCache(element, false);

					if(foundSystemDir == false && "System".equalsIgnoreCase(element.getGroupName())) foundSystemDir = true;
				}

				if(Tools.isNotEmpty(groups))	//ak nezaratam rodicovsky adresa, moze byt groups prazdne v pripade, ak rodicovsky adresar nemal ziadne podadresare
				{
					//vymaz stranky
					String sql = "DELETE FROM documents WHERE group_id IN ("+groups+")";
					ps = db_conn.prepareStatement(sql);
					ps.executeUpdate();
					ps.close();
					ps = null;

					//vymaz adresare
					sql = "DELETE FROM groups WHERE group_id IN ("+groups+")";
					ps = db_conn.prepareStatement(sql);
					ps.executeUpdate();
					ps.close();
					ps = null;

					//Vymaz approve
					deleteGroupsApprove(groups.toString());
				}

				Adminlog.add(Adminlog.TYPE_GROUP, "Delete group: " + group, groupId, -1);
			}
			else
			{
      		// presun adresar do trashu
				String sql = "UPDATE groups SET parent_group_id=?, sync_status=1 WHERE group_id=?";
				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, trashGroupDetails.getGroupId());
				ps.setInt(2, groupId);
				ps.executeUpdate();
				ps.close();
				ps = null;

				Adminlog.add(Adminlog.TYPE_GROUP, "Delete group (move to trash): " + group, groupId, -1);

				//refresh cache
				groupsDB.findGroup(groupId).setParentGroupId( trashGroupDetails.getGroupId() );

				//deaktivuj vsetky stranky v tejto skupine a podskupinach
				StringBuilder groups = new StringBuilder();
				List<GroupDetails> subGroups = groupsDB.getGroupsTree(groupId, true, true);

				for (GroupDetails g : subGroups)
				{
					g.setFullPath(groupsDB.getPath(g.getGroupId()));

					if (groups.length() > 0)
						groups.append(',').append(g.getGroupId());
					else
						groups.append(g.getGroupId());

					if(foundSystemDir == false && "System".equalsIgnoreCase(g.getGroupName())) foundSystemDir = true;
				}
				// zakaz zobrazovanie stranok
				sql = "UPDATE documents SET available=?, sync_status=1 WHERE group_id IN ("+groups.toString()+")";
				ps = db_conn.prepareStatement(sql);
				ps.setBoolean(1, false);
				ps.executeUpdate();
				ps.close();
				ps = null;

				//aktualizuj FT stplce
				DocDB.updateFileNameField(groupId);
      		}

      		//ak sa jedna o system adresar, refreshnem zoznam tychto stranok
			if(foundSystemDir) groupsDB.getAllSystemFolders(true);

			db_conn.close();
			db_conn = null;
			//oznam ostatnym Node-om, ze sa nieco zmenilo
			ClusterDB.addRefresh(GroupsDB.class);
			ClusterDB.addRefresh(DocDB.class);
			GroupsDB.getInstance(true);
			DocDB.getInstance(true);

			if (group!=null && publishEvents) (new WebjetEvent<GroupDetails>(group, WebjetEventType.AFTER_DELETE)).publishEvent();

			return(true);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return(false);
	}

	/**
	 * Vrati TreeMap so zoznamom = retazec id adresarov oddelenych ciarkami pridelenych ku skupine obsiahnutej v hodnote kluca
	 * @return
	 */
	public static Map<Integer, String> getProtectedGroups()
	{
		GroupsDB groupsDB = GroupsDB.getInstance();
		List<GroupDetails> groups = groupsDB.getGroupsAll();
		TreeMap<Integer, String> tm = new TreeMap<>();
		for (GroupDetails groupDetails : groups)
		{
			if(Tools.isNotEmpty(groupDetails.getPasswordProtected()))
			{
				if(groupDetails.getPasswordProtected().indexOf(',') < 0)
				{
					if(tm.containsKey(Integer.valueOf(groupDetails.getPasswordProtected())))
					{
						String tmp = tm.get(Integer.valueOf(groupDetails.getPasswordProtected()));
						tm.put(Integer.valueOf(groupDetails.getPasswordProtected()), tmp.concat(groupDetails.getGroupId()+","));

					}else{
						tm.put(Integer.valueOf(groupDetails.getPasswordProtected()), groupDetails.getGroupId()+",");
					}
				}else{
					String[] tmp = groupDetails.getPasswordProtected().split(",");
					for (int i = 0; i < tmp.length; i++)
					{
						if(tm.containsKey(Integer.valueOf(tmp[i])))
						{
							String tmp2 = tm.get(Integer.valueOf(tmp[i]));
							tm.put(Integer.valueOf(tmp[i]), tmp2.concat(groupDetails.getGroupId()+","));

						}else{
							tm.put(Integer.valueOf(tmp[i]), groupDetails.getGroupId()+",");
						}
					}
				}
			}
		}
		/*
		for (Iterator iterator = tm.entrySet().iterator(); iterator.hasNext();)
		{
			Map.Entry<Integer, String> key = (Map.Entry<Integer, String>)iterator.next();
			System.out.println(">> KEY: "+key.getKey()+" > VALUE: "+key.getValue());
		}
		*/
		return tm;
	}

	/**
	 * Vrati zoznam ID podadresarov zadaneho adresara (vratane)
	 * napr 1,5,77,28
	 * @param groupId
	 * @return
	 */
	public String getSubgroupsIds(int groupId)
	{
		List<GroupDetails> subGroups = getGroupsTree(groupId, false, true);
		StringBuilder groupIds = new StringBuilder().append(groupId);
		for (GroupDetails grp : subGroups)
		{
			groupIds.append(',').append(grp.getGroupId());
		}

		return groupIds.toString();
	}

	/**
	 * Vrati zoznam ID adresarov so zadanou domenou (pouzitelne pre IN select)
	 * @param domainName
	 * @return
	 */
	public String getSubgroupsIds(String domainName)
	{
		if (Tools.isEmpty(domainName)) domainName = "";

		StringBuilder groupIds = new StringBuilder("-1");
		for (GroupDetails grp : groups)
		{
			if (domainName.equals(grp.getDomainName()))
			{
				groupIds.append(',').append(grp.getGroupId());
			}
		}

		if (Tools.isEmpty(groupIds.toString())) return "-1";

		return groupIds.toString();
	}

	/**
	 * expanduje zadany zoznam skupin na podadresare (vratane rodica), neprejde cez interne adresare
	 * @param rootGroups
	 * @return
	 */
	public int[] expandGroupIdsToChilds(int[] rootGroups)
	{
		return expandGroupIdsToChilds(rootGroups, false);
	}

	/**
	 * expanduje zadany zoznam skupin na podadresare (vratane rodica)
	 * @param rootGroups
	 * @param includeInternal
	 * @return
	 */
	public int[] expandGroupIdsToChilds(int[] rootGroups, boolean includeInternal)
	{
		if (rootGroups == null || rootGroups.length==0) return new int[0];

		StringBuilder searchGroups = null;
		for (int searchRootGroupId : rootGroups)
		{
			List<GroupDetails> searchGroupsArray = getGroupsTree(searchRootGroupId, true, true);
			for (GroupDetails group : searchGroupsArray)
			{
				if (group != null && (includeInternal || group.isInternal()==false))
				{
					//hladaj iba v grupach co nie su pass protected
					if (searchGroups == null)
					{
						searchGroups = new StringBuilder(String.valueOf(group.getGroupId()));
					}
					else
					{
						searchGroups.append(",").append(String.valueOf(group.getGroupId()));
					}
				}
			}
		}

		if (searchGroups == null) return new int[0];

		return Tools.getTokensInt(searchGroups.toString(), ",");
	}

	/**
	 * Vypocita hodnotu sortPriority pre novy adresar v zadanom sub adresari
	 * @param parentGroupId
	 * @return
	 */
	public int getDefaultSortPriority(int parentGroupId)
	{
		if (parentGroupId < 1 && Constants.getBoolean("enableStaticFilesExternalDir"))
		{
			//pre multidomain s externymi vraciame pre root natvrdo 100 aby sa sortovali podla abecedy
			return 100;
		}

		int maxSortOrder = 0;
		if (Constants.getBoolean("sortPriorityIncremental"))
		{
			GroupDetails parentGroup = getGroup(parentGroupId);
			if (parentGroup!=null)
			{
				maxSortOrder = (int)Math.ceil(parentGroup.getSortPriority()/10.0f) * 100;
			}
		}

		for (GroupDetails grp : getGroups(parentGroupId))
		{
			if (grp.getSortPriority() > maxSortOrder) maxSortOrder = grp.getSortPriority();
		}
		return maxSortOrder + Constants.getInt("sortPriorityIncrementGroup");
	}

	/**
	 * Removes a group from current cache
	 *
	 * @param group group, which will be erased from cache
	 */
	private void removeGroupFromCache(GroupDetails group, boolean isUpdateProcess)
	{
		try
		{
			synchronized(groups)
			{
				if (groups.remove(group)==false)
				{
					int index = Collections.binarySearch(groups, group, groupsDbSorter);
					if (index > 0)
						groups.remove(index);
					else
					{
						//zmenilo sa jej sort priority - nemozeme vyhladat binarne, musime sekvencne
						group = getGroup(group.getGroupId());
						index = groups.indexOf(group);
						groups.remove(index);
					}
				}
			}
			getGroupDetailsMap().remove(group.getGroupId());

			//zresetuj all tree cache
			groupsTreeAllCache = null;

			idToGroups.remove(group.getGroupId());

			if(Tools.isNotEmpty(group.getDomainName()) && group.getParentGroupId() == 0 && isUpdateProcess==false)
			{
				//najdi najnizsie groupId v tejto domene (domainId je najnizsie groupId v domene)
				int lowestGroupId = group.getGroupId();
				for (GroupDetails domainGroup : groups) {
					if (domainGroup.getParentGroupId()==0 && group.getDomainName().equals(domainGroup.getDomainName())) {
						if (domainGroup.getGroupId()<lowestGroupId) lowestGroupId = domainGroup.getGroupId();
					}
				}
				//ak mazana grupa je s najnizsim groupId, zmazme aj so zoznamu domen
				if (lowestGroupId==group.getGroupId()) {
					domainIds.remove(group.getDomainName());
					//musime aj tak spravit reload, lebo hladame teraz nove domainId
					GroupsDB.getInstance(true);
				}
			}
		}
		catch (ConcurrentModificationException|ArrayIndexOutOfBoundsException e)
		{
			//nastala nam chyba, najrozumnejsie je spravit nacitanie novej instancie
			GroupsDB.getInstance(true);
		}
	}

	/**
	 * Inserts a group into cache, preserving GroupsDB sort invariant
	 *
	 * @param newGroup a group to be inserted
	 */
	private void insertGroupInCache(GroupDetails newGroup)
	{
		try
		{
			synchronized(groups)
			{
				int index = Collections.binarySearch(groups, newGroup, groupsDbSorter);
				//vlastnost binarySearch - ak neobsahuje takyto prvok, tak vrati zaporny index
				//toho, kde by sa mal nachadzat zmenseny o 1
				if (index < 0)
					index = - index - 1;

				//double check
				Optional<GroupDetails> existing = groups.stream().filter(g -> g.getGroupId()==newGroup.getGroupId()).findFirst();
				if (existing.isPresent()==false) {
					groups.add(index, newGroup);
				}
			}
			getGroupDetailsMap().put(newGroup.getGroupId(), newGroup);

			//zresetuj all tree cache
			groupsTreeAllCache = null;

			if(Tools.isNotEmpty(newGroup.getDomainName()) && newGroup.getParentGroupId() == 0)
			{
			   if (domainIds.get(newGroup.getDomainName())==null) domainIds.put(newGroup.getDomainName(), newGroup.getGroupId());
			}
			idToGroups.put(newGroup.getGroupId(), newGroup);

			if (Tools.isNotEmpty(newGroup.getDomainName()))
			{
				//oznam DocDB ze mame taku domenu (vytvori hash tabulku pre danu domenu)
				DocDB docDB = DocDB.getInstance();
				docDB.getUrlsByUrlDomains(newGroup.getDomainName(), true);
			}
		}
		catch (ConcurrentModificationException|ArrayIndexOutOfBoundsException ex)
		{
			//nastala nam chyba, najrozumnejsie je spravit nacitanie novej instancie
			GroupsDB.getInstance(true);
		}
	}

	/**
	 * Replaces a group already existing in cache with a new one, preserving
	 * their natural sort.
	 * @param inCache old one
	 * @param notInCache new one
	 */
	private static void replaceGroupInCache(GroupDetails inCache,GroupDetails notInCache)
	{
		GroupsDB groupsDB = GroupsDB.getInstance();
		groupsDB.removeGroupFromCache(inCache, true);
		groupsDB = GroupsDB.getInstance();
		groupsDB.insertGroupInCache(notInCache);
	}

	private static Map<Integer ,GroupDetails> getGroupDetailsMap()
	{
		return getInstance().idToGroups;
	}

	/**
	 * Vrati korenove adresare
	 * @return
	 */
	public static List<GroupDetails> getRootGroups()
	{
		return new ComplexQuery().setSql("SELECT * FROM groups WHERE parent_group_id <= 0 ORDER BY sort_priority ASC, group_name ASC").list(new Mapper<GroupDetails>()
		{
			public GroupDetails map(ResultSet rs) throws SQLException
			{
				return fillFieldsByResultSet(rs);
			}

		});
	}

	public void resetGroupsTreeAllCache()
	{
		groupsTreeAllCache = null;
	}

	/**
	 * Test if at least one groupId is editable by user
	 * @param user
	 * @param groupIds
	 * @return
	 */
	public static boolean isGroupsEditable(UserDetails user, String groupIds) {
		int[] groupIdsInt = Tools.getTokensInt(groupIds, ",");
		return isGroupsEditable(user, groupIdsInt);
	}

	/**
	 * Test if at least one groupId is editable by user
	 * @param user
	 * @param groupIds
	 * @return
	 */
	public static boolean isGroupsEditable(UserDetails user, int[] groupIds) {
		if (groupIds == null || groupIds.length==0) return true;

		if (user == null) return false;

		for (int groupId : groupIds) {
			if (isGroupEditable(user, groupId)) return true;
		}

		return false;
	}

	/**
	 * Otestuje, ci zadany adresar je pouzivatelom nastaveny ako editovatelny (user.getEditableGroups)
	 * @param user
	 * @param groupId
	 * @return
	 */
	public static boolean isGroupEditable(UserDetails user, int groupId)
	{
		if (groupId < 1) return true;

		if (user == null) return false;
		if (Tools.isNotEmpty(user.getEditableGroups(true)))
		{
			GroupsDB groupsDB = GroupsDB.getInstance();
			String parentGroups = "," + groupId + "," + groupsDB.getParents(groupId)+",";
			StringTokenizer st = new StringTokenizer(user.getEditableGroups(true), ",");
			String id;
			int i_id;
			while (st.hasMoreTokens())
			{
				id = st.nextToken().trim();
				try
				{
					i_id = Integer.parseInt(id);
					if (parentGroups.indexOf("," + i_id + ",") != -1)
					{
						return true;
					}
				}
				catch (Exception ex)
				{

				}
			}
		}
		else
		{
			return true;
		}
		return false;
	}

	/**
	 * Testuje, ci moze user vidiet adresar, pouziva sa na overenie ked ma editaciu az niekde
	 * do poadresara, ale v FE potrebujeme zobrazit aj parentov v stromovej strukture
	 * @param user
	 * @param groupId
	 * @return
	 */
    public static boolean isGroupViewable(UserDetails user, int groupId) {
        if (groupId < 1) return true;

        if (user == null) return false;

        if (user.getEditableGroups() == null || user.getEditableGroups().isEmpty()) {
            return true;
        }

        List<GroupDetails> editableGroups = getGroupsList(user.getEditableGroups());
        GroupsDB groupsDB = GroupsDB.getInstance();
        for (GroupDetails editableGroup : editableGroups) {
            List<GroupDetails> parents = groupsDB.getParentGroups(editableGroup.getGroupId(), true);
            if (parents.stream().anyMatch(groupDetails -> groupDetails.getGroupId() == groupId)) {
                return true;
            }
        }

        //over aj zoznam pridelenych stranok
        if (Tools.isNotEmpty(user.getEditablePages())) {
            List<DocDetails> docs = UserTools.getEditablePages(user.getEditablePages());
            for (DocDetails doc : docs) {
                List<GroupDetails> parents = groupsDB.getParentGroups(doc.getGroupId());
                parents.add(doc.getGroup());
                if (parents.stream().anyMatch(groupDetails -> groupDetails.getGroupId() == groupId)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Vrati list GroupDetails na zaklade ciarkou oddeleneho zoznamu ID adresarov
     * @param tokens - ciarkou oddeleny zoznam adresarov (z UserDetails.editableGroups)
     * @return
     */
    public static List<GroupDetails> getGroupsList(String tokens) {
        List<GroupDetails> result = new ArrayList<>();

        if (Tools.isEmpty(tokens)) {
            return Collections.emptyList();
        }

        String[] ids = Tools.getTokens(tokens, ",");

        if (ids.length == 0) {
            return Collections.emptyList();
        }

        GroupsDB groupsDB = GroupsDB.getInstance();
        for (String id : ids) {
        	int intId = Tools.getIntValue(id, 0);
            GroupDetails group = groupsDB.getGroup(intId);
            if (group != null) {
                result.add(group);
            }
        }

        return result;
    }

	/**
	 * Zrusi z nazvu adresara nepovolene znaky
	 * @param groupName
	 * @return
	 */
	public static String sanitizeGroupName(String groupName, boolean alsoSlash)
	{
		if (isEmpty(groupName))
			return "";
		//znak / nemozeme nahradzat priamo v GroupDetails.setGroupName(), pretoze pre userov s obmedzenymi pravami sa potom zle zobrazoval adresar (lebo sa mu do nazvu klonuje cela cesta, napr. /Interway/Novinky/
		if (alsoSlash)
		{
			groupName = Tools.replace(Tools.replace(groupName, "\\", "-"), "/", "-");
		}

		return groupName.replace("/\n/gi", " ").replaceAll("/\\s+/gi", " ").replace('\\', '/').trim();
	}

	/**
	 * Vrati  id root adresara danej domeny
	 * @param domain meno domeny
	 * @return
	 */
	public static int getDomainId(String domain)
	{
		Integer id = getInstance().domainIds.get(domain);
		if(id == null) id = -1;
		return id;
	}

	/**
	 * Vrati adresar System ktory je v root adresari aktualnej domeny, teda napr. /www.interway.sk/System/
	 * @return
	 */
	public GroupDetails getLocalSystemGroup()
	{
		String groupName = "System";
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb == null)
		{
			return null;
		}

		String domain = rb.getDomain();
		if (Tools.isEmpty(domain)) return null;
		int rootGroupId = GroupsDB.getDomainId(domain);
		if (rootGroupId < 1) return null;

		for (GroupDetails group : groups)
		{
			if (group.getParentGroupId()==rootGroupId && groupName.equalsIgnoreCase(group.getGroupName()))
			{
				//vratime adresar, System v hlavnom priecinku domeny (stary system)
				return (group);
			}
			else if (group.getParentGroupId()<1 && groupName.equalsIgnoreCase(group.getGroupName()) && domain.equalsIgnoreCase(group.getDomainName()))
			{
				//ak sa jedna o ROOT priecinok System v aktualnej domene, vrat ten (WJ9 rezim)
				return group;
			}
		}

		return (null);
	}

	/**
	 * Vrati ID adresara System ktory je v root adresari aktualnej domeny, teda napr. /www.interway.sk/System/
	 * @return
	 */
	public int getLocalSystemGroupId()
	{
		GroupDetails localSystem = getLocalSystemGroup();
		if (localSystem != null) return localSystem.getGroupId();

		return -1;
	}

	/**
	 * Vytvori novy root adresar a nastavi mu domenu
	 * ak uz domena existuje, vrati -1
	 * @param localDomainName
	 * @return
	 */
	public int createLocalDomain(String localDomainName)
	{
		if (Tools.isEmpty(localDomainName)) return -1;

		localDomainName = localDomainName.toLowerCase();

		if(GroupsDB.getInstance().domainIds.containsKey(localDomainName))
		{
			Logger.debug(getClass(), "Domain already exists: " + localDomainName);
			return -1;
		}
		Logger.debug(getClass(), "Creating domain root folder for domain: " + localDomainName);
		int actualParent = 0;
		String actualGroupName;
		GroupDetails actualGroup;
		actualGroupName = localDomainName;
		actualGroup = getGroup(actualGroupName, actualParent);
		if (actualGroup == null)
		{
			actualGroup = new GroupDetails();
			actualGroup.setParentGroupId(0);
			actualGroup.setGroupId(-1);
			actualGroup.setDefaultDocId(-1);
			actualGroup.setGroupName(actualGroupName);
			actualGroup.setNavbar(actualGroupName);
			actualGroup.setUrlDirName("/");
			actualGroup.setSortPriority(getDefaultSortPriority(actualGroup.getParentGroupId()));
			actualGroup.setDomainName(localDomainName);
			setGroup(actualGroup);
			Logger.debug(getClass(), "Domain root folder for domain: " + localDomainName + " created, id: " + actualGroup.getGroupId());
		}
		else
		{
			Logger.debug(getClass(), "Domain root folder for domain: " + localDomainName + " already exists, id: " + actualGroup.getGroupId());
		}
		return actualGroup.getGroupId();
	}

	/**
	 * Zmeni nazov hlavnej stranky groupy podla nazvu grupy, v ktorej sa stranka nachadza
	 * @param gd - ID grupy ktorej ktorej GroupName sa nastavi ako Title pre document.
	 * 				Ak je null, nic sa nevykona.
	 */
	private boolean changeDocTitle(GroupDetails gd)
	{
		if(gd != null && gd.isInternal()==false && gd.getMenuType()!=GroupDetails.MENU_TYPE_HIDDEN && gd.getFullPath()!=null && gd.getFullPath().indexOf("/System")==-1 && gd.getParentGroupId()>0)
		{
			DocDetails docDetails = DocDB.getInstance().getDoc(gd.getDefaultDocId());
			if(docDetails != null && (docDetails.getTitle() != null && !gd.getGroupName().equals(docDetails.getTitle())))
			{
				Logger.debug(DocDB.class, "Renaming document: "+docDetails.getDocId()+" to name :"+gd.getGroupName());
				docDetails.setTitle(gd.getGroupName());
				docDetails.setNavbar(gd.getGroupName());
				DocDB.saveDoc(docDetails);
				return true;
			}
			return false;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Zmeni domenu pre adresar aj podadresare (pouzitie v cloud nodoch - zmena domeny
	 *
	 * @param oldDomain povodna domena
	 * @param newDomain nova domena
	 * @return
	 */
	public static boolean changeDomain(String oldDomain, String newDomain)
	{
		GroupsDB groupsDb = GroupsDB.getInstance();
		DocDB docDb = DocDB.getInstance();
		GroupDetails group = groupsDb.getGroup(oldDomain, 0);
		StringBuilder messageToAdminlog = new StringBuilder();
		messageToAdminlog.append("Pozadovana zmena domeny OLD: '").append(oldDomain).append("', NEW: '").append(newDomain).append("' \n");
	 	String groupIds = GroupsDB.getInstance().getSubgroupsIds(group.getGroupId());
	 	messageToAdminlog.append("Zoznam ID adresarov: ").append(groupIds).append("\n");
	 	int[] ids = Tools.getTokensInt(groupIds, ",");
	 	List<DocDetails> oldDocs = new ArrayList<>();

		for (int i=0;i<ids.length;i++)
		{
			List<DocDetails> docsInGroup = docDb.getBasicDocDetailsByGroup(ids[i], DocDB.ORDER_PRIORITY);
			if (docsInGroup!=null && docsInGroup.size()>0)
				oldDocs.addAll(docsInGroup);
		}
		for (DocDetails doc : oldDocs)
		{
			messageToAdminlog.append("Zmena domeny pre Doc (docId) ").append(doc.getDocId()).append(" :");
			if (doc.getFieldT()!=null && doc.getFieldT().equals(oldDomain))
			{
				messageToAdminlog.append("-povolena\n");
				doc.setFieldT(newDomain);
			} else
			{
				messageToAdminlog.append("-zamietnuta\n");
			}
		}

		Adminlog.add(Adminlog.TYPE_CLIENT_SPECIFIC, messageToAdminlog.toString(), 0, 0);
		SimpleQuery sq = new SimpleQuery();
	  	sq.execute("UPDATE groups SET domain_name=? WHERE group_id IN ("+groupIds+")", newDomain);
	  	sq.execute("UPDATE groups SET group_name=?, navbar=? WHERE group_id=?", newDomain, newDomain, group.getGroupId());
	  	sq.execute("UPDATE url_redirect SET domain_name=? WHERE domain_name=?", newDomain, oldDomain);

	  	//premenovanie liniek
	  	sq.execute("UPDATE documents SET external_link=REPLACE(external_link, ?, ?) WHERE group_id IN ("+groupIds+")", "http://"+oldDomain, "http://"+newDomain);
	  	sq.execute("UPDATE documents SET data=REPLACE(data, ?, ?) WHERE group_id IN ("+groupIds+")", "http://"+oldDomain, "http://"+newDomain);

	  	Adminlog.add(Adminlog.TYPE_CLIENT_SPECIFIC, "Changed domain for cloud web, old domain: '"+oldDomain+"', new domain: '"+newDomain+"'", 0, 0);

	  	docDb.changeUrlInUrlmap(oldDomain, newDomain);

	  	IwcmFile oldDir = new IwcmFile(FilePathTools.getDomainBaseFolder(oldDomain));
	  	IwcmFile newDir = new IwcmFile(FilePathTools.getDomainBaseFolder(newDomain));
	  	newDir.mkdirs();

	  	//toto asi nezbehne, kedze je to v roznych adresaroch, bude potrebne spravit move
	  	//oldDir.renameTo(newDir);
	  	try
		{
	  		FileTools.copyDirectory(oldDir, newDir);
	  		Adminlog.add(Adminlog.TYPE_CLIENT_SPECIFIC, "Changed domain, old domain: '"+oldDomain+"', new domain: '"+newDomain+"', files from "+oldDir.getAbsolutePath()+" to "+newDir.getAbsolutePath(), 0, 0);

	  		FileTools.deleteDirTree(oldDir);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

	  	//reloadneme singletona aby sa nam prejavili vsetky zmeny
	  	GroupsDB.getInstance(true);
	  	//toto by asi ani nebolo treba, ale pre istotu pridavam
	  	DocDB.getInstance(true);

	  	return true;
	}

	/**
	 * Vrati zoznam hlavnych adresarov pre prava pouzivatela
	 * @param editableGroups
	 * @return
	 */
	public List<GroupDetails> getRootGroups(String editableGroups)
	{
		List<GroupDetails> rootGroups = new ArrayList<>();

		if (Tools.isNotEmpty(editableGroups))
		{
			StringTokenizer st = new StringTokenizer(editableGroups, ",");
			String id;
			int i_id;
			while (st.hasMoreTokens())
			{
				id = st.nextToken().trim();
				try
				{
					i_id = Integer.parseInt(id);
					//pridaj to do stromu editovatelnych stranok
					GroupDetails parentGroup = findGroup(i_id);
					GroupDetails parentGroupCopy = new GroupDetails(parentGroup);
					parentGroupCopy.setParentGroupId(0);
					if (parentGroup.getFullPath().length() > 200)
					{
						parentGroupCopy.setGroupName("..."+parentGroup.getFullPath().substring(parentGroup.getFullPath().length()-198));
					}
					else
					{
						parentGroupCopy.setGroupName(parentGroup.getFullPath());
					}

					String userEditableTreeMenuType = Constants.getString("userEditableTreeMenuType", "rows");
					if ("tree".equalsIgnoreCase(userEditableTreeMenuType)) {

						GroupDetails root = getGroup(getRoot(parentGroup.getGroupId()));

						if (rootGroups.stream().noneMatch(r->r.getGroupId() == root.getGroupId())) {
							rootGroups.add(root);
						}
					}
					else {
						if (rootGroups.stream().noneMatch(r->r.getGroupId() == parentGroupCopy.getGroupId())) {
							rootGroups.add(parentGroupCopy);
						}
					}
				}
				catch (Exception ex)
				{

				}
			}
		}
		else
		{
			rootGroups.addAll(getRootGroups());
		}

		return (rootGroups);
	}

	/**
	 * vrati zoznam nazvov root domen pre ktore ma pouzivatel pravo
	 * @param user
	 * @return
	 */
	public List<String> getUserRootDomainNames(Identity user)
	{
		String editableGroups = user.getEditableGroups();

		if (Tools.isNotEmpty(user.getEditablePages())) {
			List<DocDetails> pages = DocDB.getMyPages(user);
			for (DocDetails doc : pages) {
				if (doc.getGroupId() > 0) {
					editableGroups += "," + doc.getGroupId();
				}
			}
		}

		List<String> ret = new ArrayList<>();
		for(GroupDetails gd : getRootGroups(editableGroups))
		{
			if(Tools.isNotEmpty(gd.getDomainName()) && ret.contains(gd.getDomainName()) == false)
				ret.add(gd.getDomainName());
		}

		return ret;
	}

	/**
	 * Otestuje ci ma adresar akehokolvek potomka - podadresar alebo web stranku
	 * @param groupId
	 * @return
	 */
	public boolean hasAnyChild(int groupId)
	{
		return hasAnyChild(groupId, true);
	}

	/**
	 * Otestuje, ci ma podadresar potomka
	 * @param groupId
	 * @param includePages - ak je true, testuju sa aj web stranky
	 * @return
	 */
	public boolean hasAnyChild(int groupId, boolean includePages)
	{
		for (GroupDetails group : groups)
		{
			if (group.getParentGroupId() == groupId)
			{
				return true;
			}
		}

		if (includePages)
		{
			List<DocDetails> docs = DocDB.getInstance().getBasicDocDetailsByGroup(groupId, -1);
			if (docs != null && docs.size() > 0) return true;
		}

		return false;
	}

	public GroupDetails addNewRootGroup(String newGroupName)
	{
		GroupDetails ctxGroup = getNewGroupDetails(newGroupName, 0);
		GroupsDB.getInstance().setGroup(ctxGroup);

		return ctxGroup;
	}

	/**
	 * Vytvori novy adresar so zadanym menom v danom parent adresari
	 * @param newGroupName
	 * @param parentGroupId
	 * @return
	 */
	public GroupDetails addNewGroup(String newGroupName, int parentGroupId)
	{
		GroupDetails ctxGroup = getNewGroupDetails(newGroupName, parentGroupId);
		GroupsDB.getInstance().setGroup(ctxGroup);

		return ctxGroup;
	}

	/**
	 * Pripravi groupDetails objekt pre ulozenie do zadaneho parenta
	 * Nastavi potrebne (dedene) atributy a objekty
	 */
	public GroupDetails getNewGroupDetails(String newGroupName, int parentGroupId) {
		GroupDetails parentGroup;
		if (parentGroupId>0) parentGroup = getGroup(parentGroupId);
		else parentGroup = getGroup(Constants.getInt("rootGroupId"));

		GroupDetails ctxGroup = null;
		try
		{
			ctxGroup = (GroupDetails)parentGroup.clone();
			ctxGroup.setParentGroupId(parentGroupId);
			ctxGroup.setDefaultDocId(-1);
			ctxGroup.setNewPageDocIdTemplate(-1);
			ctxGroup.setHtmlHead("");
			ctxGroup.setGroupName(newGroupName);
			ctxGroup.setNavbar(newGroupName);
			//set url dirname to blank and call getUrlDirName() to set it to default value
			ctxGroup.setUrlDirName("");
			ctxGroup.setUrlDirName(ctxGroup.getUrlDirName());

			ctxGroup.setSortPriority(GroupsDB.getInstance().getDefaultSortPriority(parentGroupId));

			if (parentGroupId == 0) {
				//pre root foldre toto dava zmysel
				ctxGroup.setInternal(false);
				ctxGroup.setMenuType(GroupDetails.MENU_TYPE_ONLYDEFAULT);
			}

			ctxGroup.setLogonPageDocId(-1);

			ctxGroup.setGroupId(-1);
			ctxGroup.setSyncId(0);
			ctxGroup.setSyncStatus(0);

			//ticket 11208
			if(Tools.isNotEmpty(ctxGroup.getDomainName()) && ctxGroup.getParentGroupId() < 1)
			{
				ctxGroup.setDomainName("www."+ctxGroup.getUrlDirName()+".sk");
			}

			if (parentGroupId>0) {
				//nekopirujeme field_a az field_d pretoze to je nepotrebne (daju sa ziskat rekurzivne)
				ctxGroup.setFieldA("");
				ctxGroup.setFieldB("");
				ctxGroup.setFieldC("");
				ctxGroup.setFieldD("");
			}
		}
		catch (CloneNotSupportedException e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return ctxGroup;
	}

	/**
	 * ziskanie vsetkych System adresarov
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<GroupDetails> getAllSystemFolders(boolean forceRefresh)
	{
		if(Constants.getBoolean("templatesUseRecursiveSystemFolder") == false) return null;

		String groupName = "System";
		String cacheName = "sk.iway.iwcm.doc.groupsdb.all-system-folders";
		int cacheInMinutes = Constants.getInt("allSystemFoldersCacheMinutes");

		Cache c = Cache.getInstance();
		if(forceRefresh == false)
		{
			Object o = c.getObject(cacheName);
			if (o instanceof List<?>)
			{
				Logger.debug(GroupsDB.class, "citam vsetky system adresare z cache (forceRefresh="+forceRefresh+"; cacheInMinutes="+cacheInMinutes+")");
				return (List<GroupDetails>)o;
			}
		}

		List<GroupDetails> systemFolders = null;
		if(groups != null)
		{
			for (GroupDetails group : groups)
			{
				if (group.isInternal() && groupName.equalsIgnoreCase(group.getGroupName()))
				{
					if(systemFolders == null) systemFolders = new ArrayList<>();
					systemFolders.add(group);
				}
			}
		}

		Logger.debug(GroupsDB.class, "pisem vsetky system adresare do cache (forceRefresh="+forceRefresh+"; cacheInMinutes="+cacheInMinutes+")");
		if(cacheInMinutes > 0) c.setObjectSeconds(cacheName, systemFolders, cacheInMinutes*60, true);

		return systemFolders;
	}

	/**
	 * Vrati prvy vyskyt adresara System od root adresara az po aktualny adresar groupId
	 * @return
	 */
	public GroupDetails getSystemGroupRecursive(int groupId)
	{
		List<GroupDetails> pathListGroups = getPathList(groupId);
		if(pathListGroups != null)
		{
			List<GroupDetails> getAllSystemFolders = getAllSystemFolders(false);
			if(getAllSystemFolders != null)
			{
				for (GroupDetails plGroup : pathListGroups)
				{
					for (GroupDetails group : getAllSystemFolders)
					{
						if (group.getParentGroupId() == plGroup.getGroupId())
						{
							return group;
						}
					}
				}
			}
		}

		return (null);
	}

	/**
	 * Vrati list vsetkych domen
	 * @return
	 */
	public List<String> getAllDomainsList(){
		HashSet<String> domainsSet=new HashSet<>();

		List<GroupDetails> allGroups = getGroupsAll();
		for(GroupDetails g: allGroups){
				if(g.getParentGroupId() == 0 && !g.getDomainName().isEmpty()){
					domainsSet.add(g.getDomainName());
				}
		}

		return new ArrayList<>(domainsSet);
	}

    /**
     * Nastav jazyk na podadresare
     *
     * @param parentGroupId
     * @param language
     */
    public void setLngToSubGroups(int parentGroupId, String language) {

        Connection db_conn = null;
        PreparedStatement ps = null;
        String groupIds = this.getSubgroupsIds(parentGroupId);

        Adminlog.add(Adminlog.TYPE_GROUP, "Force language [" + language + "] to subgroups: " + groupIds, parentGroupId, -1);
        try {
			db_conn = DBPool.getConnection();
            ps = db_conn.prepareStatement("UPDATE groups SET lng=? WHERE group_id IN (" + groupIds + ")");
            ps.setString(1, language);
            ps.execute();
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (db_conn != null)
                    db_conn.close();
            } catch (Exception ex) {
                sk.iway.iwcm.Logger.error(ex);
            }
        }
    }

    /**
     * nastav sposob zobrazovania a interny podadresarom
     * @param parentGroupId
     * @param internal
     * @param menuType
     * @param loggedMenuType
     */
    public void setInternalAndMenuTypeToSubgroups(int parentGroupId, boolean internal, int menuType, int loggedMenuType) {

        String groupMenuIds = this.getSubgroupsIds(parentGroupId);

        if (Tools.isNotEmpty(groupMenuIds)) {
            Connection db_conn = null;
            PreparedStatement ps = null;

            Adminlog.add(Adminlog.TYPE_GROUP, "Force Show Menu Setings to subgroups: " + groupMenuIds, parentGroupId, -1);
            try {
				db_conn = DBPool.getConnection();
                ps = db_conn.prepareStatement("UPDATE groups SET internal=?, menu_type=?, logged_menu_type=?  WHERE group_id IN (" + groupMenuIds + ")");
                ps.setBoolean(1, internal);
                ps.setInt(2, menuType);
                ps.setInt(3, loggedMenuType);
                ps.execute();
            } catch (Exception ex) {
                sk.iway.iwcm.Logger.error(ex);
            } finally {
                try {
                    if (ps != null)
                        ps.close();
                    if (db_conn != null)
                        db_conn.close();
                } catch (Exception ex) {
                    sk.iway.iwcm.Logger.error(ex);
                }
            }
        }
    }

    /**
     * nastav prava podadresarom
     */
    public void setPermissionToSubgroups(int parentGroupId, String passwordProtectedString) {

        Connection db_conn = null;
        PreparedStatement ps = null;
        String groupIds = this.getSubgroupsIds(parentGroupId);

        Adminlog.add(Adminlog.TYPE_GROUP, "Force perms to subgroups: " + groupIds + " perms=" + passwordProtectedString, parentGroupId, -1);

        try {
			db_conn = DBPool.getConnection();
            ps = db_conn.prepareStatement("UPDATE groups SET password_protected=? WHERE group_id IN (" + groupIds + ")");
            ps.setString(1, passwordProtectedString);
            ps.execute();
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (db_conn != null)
                    db_conn.close();
            } catch (Exception ex) {
                sk.iway.iwcm.Logger.error(ex);
            }
		  }

		  //mame zmeny je najlepsie refreshnut celu GroupsDB
		  GroupsDB.getInstance(true);
    }

    /**
     * nastav sablonu podadresarom
     * @param parentGroupId
     */
    public void setTemplateToSubgroups(int parentGroupId, int templateId) {

        Connection db_conn = null;
        PreparedStatement ps = null;

        String groupIds = this.getSubgroupsIds(parentGroupId);
        Adminlog.add(Adminlog.TYPE_GROUP, "Force template to subgroups: " + groupIds, parentGroupId, templateId);

        try {
			db_conn = DBPool.getConnection();
            ps = db_conn.prepareStatement("UPDATE groups SET temp_id=? WHERE group_id IN (" + groupIds + ")");
            ps.setInt(1, templateId);
            ps.execute();
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (db_conn != null)
                    db_conn.close();
            } catch (Exception ex) {
                sk.iway.iwcm.Logger.error(ex);
            }
        }
    }

    /**
     * nastav sablonu podstrankam
     */
    public void setTemplateToSubpages(int parentGroupId, int templateId) {
        Connection db_conn = null;
        PreparedStatement ps = null;

        String groupIds = this.getSubgroupsIds(parentGroupId);
        Adminlog.add(Adminlog.TYPE_GROUP, "Force template to subpages: " + groupIds, parentGroupId, templateId);

        try {
			db_conn = DBPool.getConnection();
            ps = db_conn.prepareStatement("UPDATE documents SET temp_id=? WHERE group_id IN (" + groupIds + ")");
            ps.setInt(1, templateId);
            ps.execute();
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (db_conn != null)
                    db_conn.close();
            } catch (Exception ex) {
                sk.iway.iwcm.Logger.error(ex);
            }
        }
    }

    /**
     * nastav zobrazenie pre podstranky
     * @param parentGroupId
     * @param loggedMenuTypeSubdocs
     */
    public void setMenuVisibilityToSubpages(int parentGroupId, String loggedMenuTypeSubdocs) {
        Connection db_conn = null;
        PreparedStatement ps = null;

        boolean allVisible = "allVisible".equals(loggedMenuTypeSubdocs);
        Adminlog.add(Adminlog.TYPE_GROUP, "MenuVisibility: all dir documents set to "+(!allVisible ? "NOT" : "")+" visible in menu", parentGroupId, -1);
        try {
			db_conn = DBPool.getConnection();
            ps = db_conn.prepareStatement("UPDATE documents SET show_in_menu=? WHERE group_id=?");
            ps.setBoolean(1, allVisible);
            ps.setInt(2,parentGroupId);
            ps.execute();
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (db_conn != null)
                    db_conn.close();
            } catch (Exception ex) {
                sk.iway.iwcm.Logger.error(ex);
            }
        }
    }

	/**
	 * Rekurzivne pregeneruje prioritu stranok a podadresarov.
	 * @param groupId
	 */
	public void regenerateSortPriority(int groupId) {

		int priority = GroupsDB.getInstance().getGroup(groupId).getSortPriority();

    	List<DocDetails> docDetailsList = DocDB.getInstance().getDocByGroup(groupId, DocDB.ORDER_PRIORITY, true, 0, 0, false, false);

		for (DocDetails docDetails : docDetailsList) {
			docDetails.setSortPriority(priority);
			priority += Constants.getInt("sortPriorityIncrementDoc");
			DocDB.saveDoc(docDetails);
		}

		List<GroupDetails> groupDetailsList = GroupsDB.getInstance().getGroups(groupId);

		priority = (int)Math.ceil(GroupsDB.getInstance().getGroup(groupId).getSortPriority()/10.0f) * 100;

		for (GroupDetails groupDetails : groupDetailsList) {
			priority += Constants.getInt("sortPriorityIncrementGroup");
			groupDetails.setSortPriority(priority);
			GroupsDB.getInstance().setGroup(groupDetails);
			this.regenerateSortPriority(groupDetails.getGroupId());
		}
	}

	/**
	 * Opravi poradie adresarov po presunuti zadaneho adresara cez drag/drop v adresari (precisluje poradie nasledovnych adresarov)
	 * @param group
	 * @param position
	 */
	public void fixGroupSortOrder(GroupDetails group, int position)
	{
		//ziskaj pocet adresarov v tomto adresari, tie musime odpocitat
		GroupsDB groupsDB = GroupsDB.getInstance();
		List<GroupDetails> groupsList = groupsDB.getGroups(group.getParentGroupId());
		if (position < 0) position = 0;

		//v adresari nic nie je, nemusime menit sort priority
		if (groupsList.size()==0) return;

		StringBuilder updateSortOrderList = null;
		int maxSortPriority = 0;
		for (GroupDetails g : groupsList)
		{
			maxSortPriority = g.getSortPriority();

			if (g.getGroupId() == group.getGroupId())
			{
				//netreba nic menit
				Logger.debug(GroupsDB.class, "fixGroupSortOrder, ZHODA groupID, position="+position);
				if (position == 0) return;
				continue;
			}

			position--;

			Logger.debug(GroupsDB.class, "fixGroupSortOrder, group="+g.getGroupName()+" "+g.getGroupId()+" position="+position);

			if (position == -1)
			{
				Logger.debug(GroupsDB.class, "fixGroupSortOrder, position==-1, setting priority: "+g.getSortPriority());
				group.setSortPriority(g.getSortPriority());
			}
			if (position < 0)
			{
				if (updateSortOrderList == null) updateSortOrderList = new StringBuilder(String.valueOf(g.getGroupId()));
				else updateSortOrderList.append(",").append(String.valueOf(g.getGroupId()));

				//update cache hodnoty
				g.setSortPriority(g.getSortPriority() + 10);
				setGroup(g);
			}

			Logger.debug(GroupsDB.class, "fixGroupSortOrder, toUpdate="+updateSortOrderList);
		}

		if (updateSortOrderList == null)
		{
			Logger.debug(GroupsDB.class, "updateSortOrderList, updatujem self, maxSortPriority="+maxSortPriority);
			group.setSortPriority(maxSortPriority + 10);
		}
	}

	/**
	 * Cached list of parentGroups
	 *
	 * @param groupId
	 * @return
	 */
	public static List<GroupDetails> getParentGroupsCached(int groupId)
	{
		final String CACHE_KEY =  "GroupsDB.getListParentGroups("+groupId+")";
		@SuppressWarnings("unchecked")
		List<GroupDetails> listParentGroups = (List<GroupDetails>) Cache.getInstance().getObject(CACHE_KEY);
		if(listParentGroups == null)
		{
			listParentGroups = GroupsDB.getInstance().getParentGroups(groupId);
			Cache.getInstance().setObject(CACHE_KEY, listParentGroups, Constants.getInt("GroupsDB.parentGroupsCacheMinutes", 10));
		}
		return listParentGroups;
	}

	/**
	 * Returns GroupDetails of /System/Trash folder, for multidomain returns domain specific Trash folder
	 * @return
	 */
	public GroupDetails getTrashGroup() {
		//tu sa vytvara adresar podla default jazyka, nie podla prihlaseneho pouzivatela!
		Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
		String trashDirName = propSystem.getText("config.trash_dir");
		GroupDetails trashGroupDetails = getCreateGroup(trashDirName);
		return trashGroupDetails;
	}

	/**
	 * Check if group is in trash
	 * @param groupId
	 * @return
	 */
	public boolean isInTrash(int groupId) {
		String path = getPath(groupId);
		GroupDetails group = getGroup(groupId);
		if (group == null) return true;

		//do not use getTrashGroup() here because it needs Request/Request bean to create missing trash group in domain folder
		Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
		String trashDirName = propSystem.getText("config.trash_dir");

		boolean isInTrash =
			DB.internationalToEnglish(path).toLowerCase().startsWith(DB.internationalToEnglish(trashDirName).toLowerCase());

		return isInTrash;
	}

	/**
	 * Expand domainId to root groups to use in SQL query WHERE root_group_l1 IN (...)
	 * @param domainId
	 * @return
	 */
	public String expandRootGroupL1(int domainId) {
		GroupDetails rootGroup = getGroup(domainId);
		if (rootGroup == null) return ""+domainId;
		String domainName = rootGroup.getDomainName();
		List<GroupDetails> rootGroups = getRootGroups();
		StringBuilder sb = new StringBuilder();
		for (GroupDetails group : rootGroups) {
			if (Tools.isEmpty(domainName) || domainName.equals(group.getDomainName())) {
				if (sb.isEmpty()==false) sb.append(",");
				sb.append(group.getGroupId());
			}
		}
		return sb.toString();
	}
}
