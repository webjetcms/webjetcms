package sk.iway.iwcm.components.importWebPages; //NOSONAR

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;

import jxl.Cell;
import jxl.Sheet;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.xls.ExcelImportJXL;

/**
 *  ImportStructureExcel.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.23 $
 *@created      Date: 18.5.2005 14:26:32
 *@modified     $Date: 2010/01/20 10:12:54 $
 */
public class ImportStructureExcel extends ExcelImportJXL
{
	private Map<String, List<String> > gestored = new HashMap<>();
	private Map<String, List<String> > coordinated = new HashMap<>();
	private Map<String, List<String> > highCoordinated = new HashMap<>();
	private Map<String, List<String> > coordinatedByGestor = new HashMap<>();
	private Map<String, GroupDetails> codeToGroupsMapping = new HashMap<>();
	private List<String> alreadyClearedAdmins = new ArrayList<>();

	private Identity userDefaultGestor = null;
	private Identity userDefaultKoordinator = null;
	private Map<String, String> docContentMap = new HashMap<>();

	int rootGroupId = 1;
	GroupDetails rootGroup = null;
	String basePath;
	boolean createWebPages = true;

	boolean foundStart = false;
	boolean foundEnd = false;

	boolean downloadData = false;
	String cropHtmlStart = "";
	String cropHtmlEnd = "";

	String[] groupNameLevels;


	String sTmp;
	String groupName;

	String levelNumber;

	boolean levelAsPriority = true;

	int[] autoLevelNumbers = new int[100];
	int autoLevelLast = 0;

	public ImportStructureExcel(InputStream in, HttpServletRequest request, PrintWriter out)
	{
		super(in, request, out);

		//ziskaj ID adresara, kam sa importuje
		int groupId = Constants.getInt("rootGroupId");
		try
		{
			String p = request.getParameter("parentGroupIdString");
			if (Tools.isNotEmpty(p))
			{
				groupId = Tools.getIntValue(p, -1);
			}
			if (groupId < 1)
			{
				//skus ziskat data zo session
				if (request.getSession().getAttribute(Constants.SESSION_GROUP_ID) != null)
				{
					groupId = Integer.parseInt((String) request.getSession().getAttribute(Constants.SESSION_GROUP_ID));
				}
			}
		}
		catch (Exception ex)
		{
		}
		rootGroupId = groupId;
		rootGroup = GroupDetails.getById(rootGroupId);

		if ("yes".equals(request.getParameter("createPages")))
		{
			createWebPages = true;
		}
		else
		{
			createWebPages = false;
		}

		if ("true".equals(request.getParameter("levelAsPriority"))) levelAsPriority = true;
		else levelAsPriority = false;

		GroupsDB groupsDB = GroupsDB.getInstance();

		basePath = groupsDB.getGroupNamePath(rootGroupId);

		groupNameLevels = new String[20];

		if ("yes".equals(request.getParameter("downloadData")))
		{
			downloadData = true;

			cropHtmlStart = fixCRLF(request.getParameter("cropHtmlStart"));
			cropHtmlEnd = fixCRLF(request.getParameter("cropHtmlEnd"));
		}
	}
	@Override
	protected void afterImportJob(Prop prop)
	{
		for (String gestor : gestored.keySet())
		{
			clearPrivilegesFor(gestor);
			saveGestor(prop, gestor);
		}

		for (String coordinator : coordinated.keySet())
		{
			clearPrivilegesFor(coordinator);
			saveCoordinator(prop, coordinator, UsersDB.APPROVE_APPROVE);
		}

		for (String coordinator : highCoordinated.keySet())
		{
			clearPrivilegesFor(coordinator);
			saveCoordinator(prop, coordinator, UsersDB.APPROVE_LEVEL2);
		}

		for (String coordinator : coordinatedByGestor.keySet())
		{
			clearPrivilegesFor(coordinator);
			saveCoordinator(prop, coordinator, UsersDB.APPROVE_NONE);
		}

		for (String u : alreadyClearedAdmins)
			resolveAndCreateWritableFolders(u);

		//refreshni okno
		out.println("<script type='text/javascript'>try { window.opener.parent.leftFrame.window.location.href='/admin/skins/webjet6/groupslist-tree.jsp?rnd="+Tools.getNow()+"'; } catch (ex) {}</script>");
	}

	@SuppressWarnings("java:S1643")
	@Override
	protected void saveRow(Connection db_conn, Cell[] row, Sheet sheet, Prop prop) throws Exception
	{
		try
		{
		if (row.length<1)
		{
			return;
		}

		//println(getValue(row[0])+" Primárna navigácia "+"Primárna navigácia".equals(getValue(row[0])));

		if ("Primárna navigácia".equals(getValue(row[0])))
		{
			foundStart = true;
			autoLevelReset(0);
			return;
		}
		if ("Sekundárna navigácia".equals(getValue(row[0]).trim()) || "Pomocná navigácia".equals(getValue(row[0]).trim()))
		{
			//foundEnd = true;
			groupNameLevels[1] = getValue(row[0]);
			return;
		}
		if ("ID stránky".equals(getValue(row[0]).trim()))
		{
			setHeader(row);
		}

		if (foundStart == false || foundEnd == true)
		{
			return;
		}

		levelNumber = getValue(row[0]);

		if ("-".equals(levelNumber))
		{
			levelNumber = "";
			for (int i=1; i<row.length; i++)
			{
				String value = getValue(row[i]);
				if (Tools.isNotEmpty(value))
				{
					autoLevelNumbers[i-1] = autoLevelNumbers[i-1] + 1;

					if (Tools.isEmpty(levelNumber)) levelNumber += Integer.toString(autoLevelNumbers[i-1]);
					else levelNumber += "."+autoLevelNumbers[i-1];

					autoLevelReset(i);
					break;
				}
				else
				{
					if (Tools.isEmpty(levelNumber)) levelNumber += Integer.toString(autoLevelNumbers[i-1]);
					else levelNumber += "."+autoLevelNumbers[i-1];
				}
			}
		}

		if (Tools.isEmpty(levelNumber))
		{
			return;
		}

		int sortPriority = -1;
		if (levelAsPriority)
		{
			sortPriority = Tools.getIntValue(Tools.replace(levelNumber, ".", "").trim(), -1);
			if (sortPriority > 0) sortPriority = sortPriority * 10;
		}

		int currentLevel;
		for (currentLevel = 1; currentLevel < 7; currentLevel++)
		{
			groupName = getValue(row[currentLevel]);
			if (Tools.isNotEmpty(groupName))
			{
				groupName = groupName.trim();
				//nemoze tam byt lomitko, lebo inak to bude ako dalsi podadresar
				groupName = groupName.replace('/', ' ');
				break;
			}
		}

		if (levelNumber.startsWith("0."))
		{
			String[] data = levelNumber.split("\\.");
			int level = data.length;
			currentLevel = level;
			groupNameLevels[1] = "Sekundárna navigácia";
		}
		else if (currentLevel == 1)
		{
			//detekuj level podla cisel
			if (levelNumber.endsWith(".")) levelNumber = levelNumber.substring(0, levelNumber.length()-1);
			String[] data = levelNumber.split("\\.");
			int level = data.length;
			//ak je to 1.0 tak je to v podstate 1 uroven, nie druha
			if (levelNumber.endsWith(".0"))
			{
				level = level - 1;
			}
			currentLevel = level;
		}

		//vyskladaj celu cestu
		groupNameLevels[currentLevel] = groupName;

		String groupPath = getGroupPath(currentLevel);

		println(levelNumber+" ["+currentLevel+"] "+groupPath, rowCounter);
		print("<div style='padding-left: 20px;'>");

		GroupsDB groupsDB = GroupsDB.getInstance();
		GroupDetails group = groupsDB.getCreateGroup(groupPath);


		//VUB specific - gestor can edit, coordinator must acknowledge his changes
		// and so does high coordinator coordinator's changes
		// Firstly, we collect them all and act after import - see afterImportJob
		String groupCode = getValue( row[0] );
		if (groupCode != null)
			groupCode = stripRedundantCode(groupCode);

		String workflowType = getValue(row,"Workflow");

		if (Tools.isNotEmpty(groupCode))
		{
			String maybeGestor = getValue(row, "Gestor");
			String maybeCoordinator = getValue(row, "Koordinátor");
			String maybeHighCoordinator = getValue(row,"Obsahový správca");

			//put this group into his list of gestored groups
			addGroupsToPersons(groupCode, maybeGestor,gestored);
			addGroupsToPersons(groupCode, maybeCoordinator,gestored);
			addGroupsToPersons(groupCode, maybeHighCoordinator,gestored);
			if (workflowType != null && !workflowType.startsWith("GK"))
				addGroupsToPersons(groupCode, maybeGestor,coordinatedByGestor);
			//the same for coordinators
			addGroupsToPersons(groupCode, maybeCoordinator,coordinated);
			//and the same for high coordinators
			addGroupsToPersons(groupCode, maybeHighCoordinator,highCoordinated);
			codeToGroupsMapping.put(groupCode, group);
		}


		if (group != null && createWebPages==true)
		{
			String data = "";

			if (row.length>5 && downloadData)
			{
				String url = getValue(row, "Súčasná adresa (ak existuje)");
				try
				{
					if (Tools.isNotEmpty(url) && url.startsWith("http"))
					{

						data = fixCRLF(Tools.downloadUrl(url));

						if (data == null) data = "";

						if (Tools.isNotEmpty(cropHtmlStart))
						{
							int startIndex = data.indexOf(cropHtmlStart);

							Logger.println(this,"Orezavam zaciatok, index = " + startIndex);
							if (startIndex != -1) data = data.substring(startIndex+cropHtmlStart.length());
						}
						if (Tools.isNotEmpty(cropHtmlEnd))
						{
							int endIndex = data.indexOf(cropHtmlEnd);

							Logger.println(this,"Orezavam koniec, index = " + endIndex);
							if (endIndex > 0) data = data.substring(0, endIndex);
						}

						println(prop.getText("components.import_web_pages.xls.page_downloaded", url), -1);
					}
				}
				catch (RuntimeException ex)
				{
					sk.iway.iwcm.Logger.error(ex);
					printlnError(prop.getText("components.import_web_pages.xls.error_crop_data")+": "+ex.getMessage(), rowCounter);
				}
			}

			int setTempId = -1;
			String templateName = getValue(row, "Šablóna");
			if (Tools.isNotEmpty(templateName))
			{
				TemplatesDB tempDB = TemplatesDB.getInstance();
				TemplateDetails temp = tempDB.getTemplate(templateName);
				if (temp == null)
				{
					printlnError(prop.getText("components.import_web_pages.xls.temp_not_exists", templateName), rowCounter);
				}
				else
				{
					println(prop.getText("components.import_web_pages.xls.setting_template", temp.getTempName()), -1);
					setTempId = temp.getTempId();
				}
			}

			if (levelAsPriority && sortPriority > 0)
			{
				group.setSortPriority(sortPriority);
				if (setTempId>0) group.setTempId(setTempId);
				groupsDB.setGroup(group);
			}

			//vytvor prazdnu stranku
			EditorForm ef = EditorDB.getEditorForm(request, -1, -1, group.getGroupId());
			ef.setTitle(groupName);
			ef.setNavbar(groupName);
			ef.setExternalLink("");
			ef.setSearchable(true);
			ef.setAvailable(true);
			ef.setCacheable(false);

			DocDB docDB = DocDB.getInstance();
			//	zisti kolko je v grupe stranok a zvys pocitadlo
			ef.setSortPriority(0);
			for (DocDetails doc2 : docDB.getDocByGroup(group.getGroupId()))
			{
				if (doc2!=null)
				{
					if (ef.getSortPriority()<doc2.getSortPriority())
					{
						ef.setSortPriority(doc2.getSortPriority());
					}
					//aktualizuj existujucu stranku
					if (doc2.getTitle().equals(ef.getTitle()))
					{
						println("<font color='orange'>["+prop.getText("components.import_web_pages.xls.page_will_update")+"]</font>", -1);
						//ef.setDocId(doc2.getDocId());
						ef = EditorDB.getEditorForm(request, doc2.getDocId(), -1, group.getGroupId());
					}
				}
			}

			if (levelAsPriority && sortPriority > 0)
			{
				ef.setSortPriority(sortPriority);
			}
			else if (ef.getDocId()<1)
			{
				ef.setSortPriority(ef.getSortPriority()+10);
			}

			ef.setPublish("1");
			if (Tools.isNotEmpty(data)) ef.setData(data);

			Identity user = UsersDB.getCurrentUser(request);
			ef.setAuthorId(user.getUserId());

			if (setTempId>0) ef.setTempId(setTempId);

			String[] fields = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};
			for (String pismeno : fields)
			{
				String value = getValue(row, "Pole "+pismeno);
				if (Tools.isNotEmpty(value)) BeanUtils.setProperty(ef, "field"+pismeno, value);
			}

			String keywords = getValue(row, "Klúčové slová");
			if (Tools.isNotEmpty(keywords))
			{
				String perexGroups = null;
				StringTokenizer st = new StringTokenizer(keywords, ",");
				while (st.hasMoreTokens())
				{
					String keyword = st.nextToken().trim();
					PerexGroupBean perexGroup = docDB.getPerexGroup(-1, keyword);
					if (perexGroup != null)
					{
						if (perexGroups == null) perexGroups = Integer.toString(perexGroup.getPerexGroupId());
						else perexGroups += ","+perexGroup.getPerexGroupId();
					}
				}
				if (Tools.isNotEmpty(perexGroups)) ef.setPerexGroupString(perexGroups);
			}

			if (Tools.isEmpty(ef.getData()) || ef.getData().length() < 20)
			{
				if (Tools.isEmpty(templateName))
				{
					TemplatesDB tempDB = TemplatesDB.getInstance();
					TemplateDetails temp = tempDB.getTemplate(ef.getTempId());
					if (temp != null) templateName = temp.getTempName();
				}

				if (Tools.isNotEmpty(templateName))
				{
					String html = getDocDefaultContent(templateName);
					if (Tools.isNotEmpty(html) && html.length() > 20)
					{
						println("Nastavujem content podľa "+templateName);
						ef.setData(html);
					}

				}
			}

			int saveStatus = EditorDB.saveEditorForm(ef, request);

			EditorDB.cleanSessionData(request);

			if (saveStatus > 0)
			{
				setDefaultDocId(group.getGroupId(), ef.getDocId());
				println("<font color='green'>["+prop.getText("components.import_web_pages.xls.page_saved")+"]</font>", -1);
			}
			else
			{
				printlnError(prop.getText("components.import_web_pages.xls.page_not_saved"), rowCounter);
			}
		}
		print("</div>");
		}
		catch (Exception e)
		{
			printlnError("Row import failed because of "+e.getMessage());
		}
	}


	private boolean setDefaultDocId(int groupId, int docId)
	{
		boolean ret = false;
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("UPDATE groups SET default_doc_id=? WHERE group_id=?");
			ps.setInt(1, docId);
			ps.setInt(2, groupId);
			ps.execute();
			ps.close();
			ps = null;
			ret = true;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return(ret);
	}

	private String getGroupPath(int currentLevel)
	{
		StringBuilder path = new StringBuilder(basePath);
		int i;
		for (i=1; i<=currentLevel; i++)
		{
			path.append('/').append(groupNameLevels[i]);
		}

		Logger.println(this,"getGroupPath("+currentLevel+")="+path);

		return path.toString();
	}

	/**
	 * Odsrani CR z textu (necha len LF), aby to bolo rovnake
	 * @param text
	 * @return
	 */
	private static String fixCRLF(String text)
	{
		text = Tools.replace(text, "\r", "");

		return(text);
	}

	public static String listToString(List<String> list)
	{
		String ret = null;
		for (String l : list)
		{
			if (ret == null) ret = l;
			else ret += ","+l; //NOSONAR
		}
		return ret;
	}


	public static List<String> collapseGroups(List<String> groups)
	{
		Logger.debug(ImportStructureExcel.class, "Collapse groups: "+listToString(groups));

		//sort the codes
		Collections.sort(groups,new Comparator<String>(){
			@Override
			public int compare(String group1, String group2)
			{
				String[] group1Levels = group1.split("\\.");
				String[] group2Levels = group2.split("\\.");
				int maxLevels = group1Levels.length > group2Levels.length ? group2Levels.length : group1Levels.length;

				for (int levelIndex = 0;levelIndex < maxLevels;levelIndex++)
				{
					int first = Integer.parseInt( group1Levels[ levelIndex ] );
					int second = Integer.parseInt( group2Levels[ levelIndex ] );
					if ( (first - second) != 0 )
						return first - second;
				}
				//still no answer - return the one with less dots
				return group1Levels.length > group2Levels.length ? 1 : -1;
			}
		});

		/* povodny kod od marosa, ktory orezaval ak mal user pravo na parent - odrezal vsetky subs
		int size = groups.size();

		for (int groupIndex = 0;groupIndex < size; groupIndex++)
		{
			//devourChildren(groups, groupIndex );
			size = groups.size();
		}
		*/
		return groups;
	}

	/**
	 * Spoji adresare smerom hore, ak mam pristup na vsetky podadresare nejakeho adresara spoji to do jedneho
	 * @param groups - zoznam ID adresarov
	 * @return
	 */
	public static String collapseGroupsUp(String groupsStr)
	{
		Logger.debug(ImportStructureExcel.class, "Collapse groups UP: "+groupsStr);
		String[] groups = Tools.getTokens(groupsStr, ",");

		Map<String, String> allreadyHas = new Hashtable<>();

		Map<Integer, Integer> groupsIdTable = new Hashtable<>();
		for (String group : groups)
		{
			int groupId = Tools.getIntValue(group, -1);
			groupsIdTable.put(Integer.valueOf(groupId), Integer.valueOf(groupId));
		}

		GroupsDB groupsDB = GroupsDB.getInstance();
		for (String group : groups)
		{
			int groupId = Tools.getIntValue(group, -1);
			int[] groupIdArr = new int[1];
			groupIdArr[0] = groupId;
			int[] childs = groupsDB.expandGroupIdsToChilds(groupIdArr, true);

			boolean someMissing = false;
			for (int childId : childs)
			{
				if (groupsIdTable.get(Integer.valueOf(childId))==null)
				{
					//takehoto potomka nema, nemozeme orezat
					Logger.debug(ImportStructureExcel.class, "collapseGroups: missing "+childId+" parent="+groupId);
					someMissing = true;
					break;
				}
			}
			if (!someMissing)
			{
				//mozeme poznacit ako duplicitu
				for (int childId : childs)
				{
					//rodica musime nechat
					if (groupId == childId) continue;

					String childIdString = Integer.toString(childId);
					allreadyHas.put(childIdString, childIdString);
				}
			}
		}

		//zrus duplicity
		List<String> groupsNew = new ArrayList<>();
		for (String groupId : groups)
		{
			if (allreadyHas.get(groupId)==null)
			{
				allreadyHas.put(groupId, groupId);
				groupsNew.add(groupId);
			}
		}

		String ret = listToString(groupsNew);
		Logger.debug(ImportStructureExcel.class, "Collapse groups UP done: "+ret);
		return ret;
	}

	private void addGroupsToPersons(String groupCode, String maybePerson,Map<String,List<String>> collection)
	{
			if (isNotEmpty(maybePerson))
			{
				String[] persons = maybePerson.split(",");
				for (String person : persons)
				{
					person = person.trim();
					if (collection.get(person) == null)
						collection.put(person, new ArrayList<>() );
					collection.get(person).add(groupCode);
				}
			}
	}

	private String stripRedundantCode(String groupCode)
	{

		while (groupCode.endsWith(".0"))
		{
			groupCode = groupCode.substring(0, groupCode.lastIndexOf(".0") );
		}
		return groupCode;
	}

	/**
	 * Creates/loads gestor as a user and sets his editable groups.
	 */
	private void saveGestor(Prop prop, String gestor)
	{
		Logger.debug(ImportStructureExcel.class, "Saving gestor: "+gestor);
		println("Saving editable groups for: "+gestor);
		try
		{
			//if he does not exist - create him
			if ( UsersDB.getUser(gestor) == null )
				createUser(gestor);
			UserDetails gestorUser = UsersDB.getUser(gestor);

			List<String> groups = gestored.get(gestor);
			//remove redundant groups
			groups = collapseGroups(groups);
			//build editableGroups string
			StringBuilder editableGroups = new StringBuilder( gestorUser.getEditableGroups() );
			for (String code : groups)
			{
				GroupDetails group = codeToGroupsMapping.get(code);

				if (editableGroups.length() > 0)
					editableGroups.append(',');
				editableGroups.append( group.getGroupId() );
			}
			String collapsedGroups = collapseGroupsUp(editableGroups.toString());

			if (highCoordinated.get(gestor)!=null)
			{
				//koordinator ma pristup vsade, takze mu pridame aj root folder
				collapsedGroups = rootGroupId+","+collapsedGroups;
			}

			gestorUser.setEditableGroups(collapsedGroups);

			//and save changes
			UsersDB.saveUser(gestorUser);

			if (highCoordinated.get(gestor)==null)
			{
				//vsetkym okrem koordinatora nastavme prava
				copyPerms(gestorUser, getUserDefaultGestor());
			}
		}
		catch (Exception e)
		{
			printlnError(prop.getText("components.import_web_pages.xls.error_mapping",gestor)+": "+e.getMessage() );
		}
	}



	/**
	 * Creates/loads coordinator as a user and sets his editable groups.
	 */
	private void saveCoordinator(Prop prop, String coordinator,int approveLevel)
	{
		Logger.debug(ImportStructureExcel.class, "Saving coordinator: "+coordinator);
		println("Saving user: "+coordinator);
		try
		{
			//if he does not exist - create him
			if ( UsersDB.getUser(coordinator) == null )
				createUser(coordinator);
			UserDetails coordinatorUser = UsersDB.getUser(coordinator);

			List<String> groups = null;
			switch (approveLevel) //NOSONAR
			{
				case UsersDB.APPROVE_LEVEL2 :	groups = highCoordinated.get(coordinator);	break;
				case UsersDB.APPROVE_APPROVE:	groups = coordinated.get(coordinator); 		break;
				case UsersDB.APPROVE_NONE: groups = coordinatedByGestor.get(coordinator);	break;
			}
			//remove redundant groups
			if (groups != null) groups = collapseGroups(groups);
			else
				groups = Collections.emptyList();
			List<String> groupIdsList = new ArrayList<>();
			for (String code : groups)
			{
				GroupDetails group = codeToGroupsMapping.get(code);
				if (UsersDB.getApproveMode(coordinatorUser.getUserId(), group.getGroupId()) == -1)
				{
					groupIdsList.add(Integer.toString(group.getGroupId()));
				}
			}

			if (highCoordinated.get(coordinator)!=null)
			{
				//koordinator musi mat pravo menit cokolvek v root foldri
				UsersDB.addApproveGroup(coordinatorUser.getUserId(), rootGroupId, UsersDB.APPROVE_NONE);
			}

			//collapse UP
			String groupIds = collapseGroupsUp(listToString(groupIdsList));
			int[] groupIdsArr = Tools.getTokensInt(groupIds, ",");
			for (int groupId : groupIdsArr)
			{
				UsersDB.addApproveGroup(coordinatorUser.getUserId(), groupId, approveLevel);
			}

			//and save changes
			UsersDB.saveUser(coordinatorUser);

			if (highCoordinated.get(coordinator)==null)
			{
				//ak to nie je koordinator, nastav prava
				if (approveLevel == UsersDB.APPROVE_NONE && coordinated.get(coordinator)==null && highCoordinated.get(coordinator)==null) copyPerms(coordinatorUser, getUserDefaultGestor());
				else copyPerms(coordinatorUser, getUserDefaultKoordinator());
			}
		}
		catch (Exception e)
		{
			printlnError(prop.getText("components.import_web_pages.xls.error_mapping",coordinator)+": "+e.getMessage() );
		}
	}

	/**
	 * Removes children of the given element from the given list
	 */
	public static void devourChildren(List<String> groups, int index)
	{
		for (int childIndex = index+1;childIndex<groups.size();childIndex++ )
		{
			if ( isChild( groups.get(index), groups.get(childIndex) ) )
			{
				groups.remove(childIndex);
				childIndex--; //NOSONAR
			}
			else
				return;
		}
	}

	/**
	 * Returns, whether the second parameter
	 * is child of the first one in case both
	 * are represented in decimal dotted notation
	 */
	public static boolean isChild(String parent, String child)
	{
		return child.startsWith(parent);
	}

	/**
	 * Vytvori usera, ak pozna iba login
	 */
	private void createUser(String gestor)
	{
		UserDetails usr = new UserDetails();
		usr.setAdmin(true);
		usr.setLogin(gestor);
		usr.setFirstName(gestor);
		usr.setLastName(gestor);
		usr.setAuthorized(true);
		usr.setPasswordPlain(gestor);
		UsersDB.saveUser(usr);
		println("New user: "+gestor );
	}

	private static boolean isNotEmpty(String s)
	{
		return Tools.isNotEmpty(s) && !"N/A".equals(s);
	}


	private void createDirectoriesAndAccess(GroupDetails group,UserDetails usr)
	{
		String groupPath = group.getFullPath();
		if (isNotEmpty(group.getDomainName()))
		{
			String domainAlias = Constants.getString("multiDomainAlias:"+group.getDomainName());
			if (Tools.isNotEmpty(domainAlias))
			{
				int i = groupPath.indexOf('/', 2);
				//odstranime prvy folder, to je nas alias
				if (i > 0) groupPath = groupPath.substring(i);
				groupPath = "/"+domainAlias+groupPath; //NOSONAR
			}
		}
		groupPath = DocTools.removeCharsDir(DB.internationalToEnglish(groupPath).toLowerCase(), true);
		IwcmFile imagesDirectory = new IwcmFile( Tools.getRealPath("/images"+groupPath) );
		IwcmFile filesDirectory = new IwcmFile( Tools.getRealPath("/files"+groupPath) );
		imagesDirectory.mkdirs();
		filesDirectory.mkdirs();

		if (!usr.getWritableFolders().contains("/images"+groupPath))
		{
			String writableFolders = usr.getWritableFolders();
			if (Tools.isEmpty(writableFolders))
				writableFolders = "/images"+groupPath+"*";
			else
				writableFolders += System.getProperty("line.separator")+"/images"+groupPath+"*";
			usr.setWritableFolders(writableFolders);
		}

		if (!usr.getWritableFolders().contains("/files"+groupPath))
		{
			String writableFolders = usr.getWritableFolders();
			if (Tools.isEmpty(writableFolders))
				writableFolders = "/files"+groupPath+"*";
			else
				writableFolders += System.getProperty("line.separator")+"/files"+groupPath+"*";
			usr.setWritableFolders(writableFolders);
		}
	}

	/**
	 * Nastavi pouzivatelovi prava na moduly podla grouPusera
	 * @param user
	 * @param groupUser
	 */
	public void copyPerms(UserDetails user, Identity groupUser)
	{
		if (user.getUserId()>0 && groupUser.getDisabledItemsTable()!=null && groupUser.getDisabledItemsTable().size()>0)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			try
			{
				db_conn = DBPool.getConnection();

				ps = db_conn.prepareStatement("DELETE FROM user_disabled_items WHERE user_id=?");
				ps.setInt(1, user.getUserId());
				ps.execute();
				ps.close();

				ps = db_conn.prepareStatement("INSERT INTO user_disabled_items (user_id, item_name) VALUES (?, ?)");

				Iterator<String> e = groupUser.getDisabledItemsTable().keySet().iterator();
				while (e.hasNext())
				{
					String key = e.next();

					ps.setInt(1, user.getUserId());
					ps.setString(2, key);
					ps.execute();

				}
				ps.close();

				db_conn.close();
				ps = null;
				db_conn = null;
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
		}
	}

	/**
	 * User s loginom gestorGrpAdmin definuje default prava k modulom pre gestora
	 * @return
	 */
	public Identity getUserDefaultGestor()
	{
		if (userDefaultGestor == null)
		{
			userDefaultGestor = new Identity(UsersDB.getUser("gestorGrpAdmin"));
			if (userDefaultGestor.getUserId()<1) userDefaultGestor = new Identity();
			else UsersDB.setDisabledItems(userDefaultGestor);
		}

		return userDefaultGestor;
	}

	/**
	 * User s loginom koordinatorGrpAdmin definuje default prava k modulom pre koordinatora
	 * @return
	 */
	public Identity getUserDefaultKoordinator()
	{
		if (userDefaultKoordinator == null)
		{
			userDefaultKoordinator = new Identity(UsersDB.getUser("koordinatorGrpAdmin"));
			if (userDefaultKoordinator.getUserId()<1) userDefaultKoordinator = new Identity();
			else UsersDB.setDisabledItems(userDefaultKoordinator);
		}

		return userDefaultKoordinator;
	}

	/**
	 * Podla nazvu sablony vyhlada v adresari System->Sablony default content pre stranku
	 * @param tempName
	 * @return
	 */
	public String getDocDefaultContent(String tempName)
	{
		String content = docContentMap.get(tempName);
		if (content == null)
		{
			DocDB docDB = DocDB.getInstance();
			DocDetails doc = docDB.getDocByTitle(tempName, Constants.getInt("tempGroupId"));
			if (doc != null) content = doc.getData();
			if (content == null) content = "<p>&nbsp;</p>";

			docContentMap.put(tempName, content);
		}
		return content;
	}

	private void clearPrivilegesFor(String login)
	{
		if (alreadyClearedAdmins.contains(login))
			return;

		alreadyClearedAdmins.add(login);

		//no need to delete anything in case user doesn't even exist
		if ( UsersDB.getUser(login) == null )
			return;

		UserDetails user = UsersDB.getUser(login);
		//delete those groups which are under the imported ROOT
		//------------------DELETING APPROVE GROUPS---------------------
		List<Number> approveGroups = UsersDB.getApproveGroups(user.getUserId());

		for (Number groupId : approveGroups)
		{
			GroupDetails group = GroupDetails.getById(groupId.intValue());
			if (group == null ||  group.getFullPath().startsWith( rootGroup.getFullPath() ))
				UsersDB.deleteApproveGroup(user.getUserId(), groupId.intValue());
		}
		//the same with editable groups
		//------------------DELETING EDITABLE GROUPS-----------------------
		String editableGroups = user.getEditableGroups();
		StringBuilder editableGroupsAfter = new StringBuilder(editableGroups.length());
		for (String groupIdString : editableGroups.split(","))
		{
			groupIdString = groupIdString.trim();
			if (!Tools.isInteger(groupIdString))
				continue;

			Integer groupId = Integer.valueOf(groupIdString);
			GroupDetails group = GroupDetails.getById(groupId);

			if (group != null) Logger.debug(ImportStructureExcel.class, "clearPrivileges - testing "+group.getFullPath()+" vs "+rootGroup.getFullPath());

			if (group != null && !group.getFullPath().startsWith( rootGroup.getFullPath() ))
			{
				editableGroupsAfter.append(',');
				editableGroupsAfter.append(groupId);
			}
		}
		//delete the first comma
		if (editableGroupsAfter.length() > 0)
			editableGroupsAfter = new StringBuilder(editableGroupsAfter.substring(1));

		user.setEditableGroups(editableGroupsAfter.toString());

		//writable folders will be generated out of the editable pages structure
		user.setWritableFolders("");
		println("Writable folders, editable groups and approve Groups for "+login+" has been cleared according to the root: "+rootGroup.getFullPath());
		UsersDB.saveUser(user);
	}

	private void resolveAndCreateWritableFolders(String login)
	{
		UserDetails user = UsersDB.getUser(login);

		if (highCoordinated.get(login)!=null)
		{
			//koordinatorovi nenastavujem nic
			return;
		}

		for (String groupIdString : user.getEditableGroups().split(","))
		{
			if (!Tools.isInteger(groupIdString))
				continue;
			Integer groupId = Integer.valueOf(groupIdString);
			createDirectoriesAndAccess( GroupDetails.getById(groupId) , user);
		}
		println("Creating writable folders for "+login);
		UsersDB.saveUser(user);
	}

	private void autoLevelReset(int start)
	{
		for (int i=start; i<autoLevelNumbers.length; i++)
		{
			autoLevelNumbers[i] = 0;
		}
	}
}
