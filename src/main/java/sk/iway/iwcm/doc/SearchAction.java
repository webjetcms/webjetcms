package sk.iway.iwcm.doc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.components.file_archiv.FileArchivatorBean;
import sk.iway.iwcm.components.file_archiv.FileArchivatorDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.rag.search.SemanticSearchService;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.users.UsersDB;

/**
 * full text search, ((z databazy selektuje vzdy len (perpage+1) zaznamov
 * (perpage==pocet zaznamov na stranku) ak posledny selektnuty zaznam != null
 * tak to je signal ze este nie sme na konci databazi a teda mozeme zviraznit
 * linku dalej>>> zvyraznovanie slov: v data_asc hladam vyskyt prveho slova vo
 * words, => zvyraznim aj ostatne slova ktore sa zhoduju s words else do
 * data_asc dam prvych EMPTYOUTPUT znakov ))
 *
 * @Title Interway Content Management
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2002
 * @author $Author: joruz $ + $Edited by joruz$
 * @version $Revision: 1.16 $
 * @created $Date: 2004/03/18 09:28:11 $
 * @modified $Date: 2004/03/18 09:28:11 $
 */
public class SearchAction
{
	private SearchAction()
	{
	}

	/**
	 * Identifikator 'score' pri pouziti oracletext hodnota 10 je cisto nahodna, ide o to aby v dotaze bolo pouzite to iste cislo :)
	 */
	private static int ORACLE_TEXT_CONTAINS_IDENTIFIER = 10; //NOSONAR

	private static String resolveForward(String lng, String pForward)
	{
		String forward = lng != null ? "english" : "success";
		if (pForward != null && pForward.endsWith(".jsp"))
		{
			forward = "/templates/" + pForward;
		}
		return forward;
	}

	public static SearchActionOutput search(SearchActionInput input)
	{
		Identity user = input.getUser();

		String forward = resolveForward(input.getParameter("lng"), input.getParameter("forward"));
		String error = "error";
		boolean english = input.getParameter("lng") != null;

		SearchActionOutput output = new SearchActionOutput();
		output.setForward(forward);

		String searchGroupsParam = getParamAttribute("rootGroup", input, Integer.toString(Constants.getInt("rootGroupId")));
		if (searchGroupsParam!=null) searchGroupsParam = searchGroupsParam.replace('+', ',');
		String[] groupIdParams = input.getParameterValues("groupId");
		if (groupIdParams != null && groupIdParams.length>0)
		{
			int i;
			searchGroupsParam = null;
			for (i=0; i<groupIdParams.length; i++)
			{
				if (i==0) searchGroupsParam = groupIdParams[i];
				else searchGroupsParam += ","+groupIdParams[i]; //NOSONAR
			}
		}

		boolean searchDetaultInTitle = Constants.getBoolean("searchDetaultInTitle") || "true".equals(input.getParameter("searchDetaultInTitle"));
		boolean searchAlsoProtectedPages = input.getBooleanValue("searchAlsoProtectedPages", false);

		String searchFileNameQuery = null;

		StringTokenizer st = new StringTokenizer(searchGroupsParam, ",+; ");
		GroupsDB groupsDB = GroupsDB.getInstance();
		Map<Integer, Integer> rootGroupIdsTable = new Hashtable<>();
		List<DocDetails> docsInGroups = null;
		List<GroupDetails> searchGroupsArray = null;
		DebugTimer dtt = new DebugTimer("SearchAction DUPLICITY");
		boolean checkDuplicity = input.getBooleanValue("checkDuplicity", false);
		DocDB docDB = DocDB.getInstance();
		while (st.hasMoreTokens())
		{
			try
			{
				int searchRootGroupId = Integer.parseInt(st.nextToken());

				rootGroupIdsTable.put(Integer.valueOf(searchRootGroupId), Integer.valueOf(1));

				GroupDetails fileNameGrp = groupsDB.getGroup(searchRootGroupId);
				if (fileNameGrp != null)
				{
					if (searchFileNameQuery == null) searchFileNameQuery = "file_name LIKE '"+DB.removeSlashes(groupsDB.getGroupNamePath(fileNameGrp.getGroupId()))+"%'";
					else searchFileNameQuery += " OR file_name LIKE '"+DB.removeSlashes(groupsDB.getGroupNamePath(fileNameGrp.getGroupId()))+"%'"; //NOSONAR
				}

				if(checkDuplicity)
				{
					searchGroupsArray = groupsDB.getGroupsTree(searchRootGroupId, true, false);
					for (GroupDetails group : searchGroupsArray)
					{
						if (group != null)
						{
							if(docsInGroups == null) docsInGroups = new ArrayList<>();
							docsInGroups.addAll(docDB.getBasicDocDetailsByGroup(Tools.getIntValue(group.getGroupId(),0), 0));
						}
					}
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}
		dtt.diff("after get all docs");
		StringBuffer docIdsNotIn = new StringBuffer("");
		if(checkDuplicity)
		{
			List<Integer> slavesMasterForDoc = null;

			if(docDB.getSlavesMasterMappings() != null && docsInGroups != null)
			{
				if(docsInGroups.size() > 0)
				{
					HashMap<Integer, Boolean> docsInGroupsHm = new HashMap<>();
					for(DocDetails dd : docsInGroups) docsInGroupsHm.put(Integer.valueOf(dd.getDocId()), Boolean.TRUE);

					for (Entry<Integer, Boolean> entry : docsInGroupsHm.entrySet())
					{
						if(entry.getValue() != Boolean.FALSE)
						{
							Integer docId = entry.getKey();
							slavesMasterForDoc = null;

							if(slavesMasterForDoc == null) slavesMasterForDoc = new ArrayList<>();

							Integer masterId = docDB.getSlavesMasterMappings().get(docId);
							if(masterId != null)
							{
								boolean containsMaster = docsInGroupsHm.get(masterId) != null && docsInGroupsHm.get(masterId) == Boolean.TRUE;
								Integer[] slaves = docDB.getMasterMappings().get(masterId);
								if(slaves != null)
								{
									if(containsMaster) docsInGroupsHm.put(masterId, Boolean.FALSE);
									for(Integer slave : slaves)
										if(containsMaster || (!containsMaster && slave.intValue() != docId.intValue())) slavesMasterForDoc.add(slave);
								}
							}
							else
							{
								Integer[] slaves = docDB.getMasterMappings().get(docId);
								if(slaves != null) slavesMasterForDoc.addAll(Arrays.asList(slaves));
							}
							if(slavesMasterForDoc != null && slavesMasterForDoc.size() > 0)
							{
								for(Integer sm: slavesMasterForDoc)
								{
									if(docsInGroupsHm.get(sm) != null)
									{
										docsInGroupsHm.put(sm, Boolean.FALSE);
										docIdsNotIn.append(String.valueOf(sm.intValue())+",");
									}
								}
							}
						}
					}
				}
			}
		}
		dtt.diff("after docIdsNotIn");

		int perPage = 10;
		try
		{
			if (getParamAttribute("perpage", input, "10") != null)
			{
				perPage = Integer.parseInt(getParamAttribute("perpage", input, "10"));
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		String rq = input.getParameter("index");
		int index;
		boolean hasAnotherPage;
		if (rq == null)
		{
			index = 0;
		}
		else
		{
			try
			{
				index = Integer.parseInt(rq);
			}
			catch (Exception e)
			{
				index = 0;
				sk.iway.iwcm.Logger.error(e);
			}
		}

		String words = input.getParameter("words");
		if (Tools.isEmpty(words))
		{
			words = (String)input.getAttribute("words");
		   if (words == null) words = "";
		}
		String text = input.getParameter("text");
		if (Tools.isNotEmpty(text))
		{
			words = text;
		}
      if ("tatrabanka".equals(Constants.getInstallName()))
      {
         words = Tools.replace(words, "TB", " TB");
      }

		if (Tools.isNotEmpty((String)input.getAttribute("forceWords")))
		{
			words = (String)input.getAttribute("forceWords");
		}

		String pom;
		int i;
		words = words.replace('\'', ' ');
		words = words.replace(',', ' ');
		words = words.replace('.', ' ');
		words = words.replace(';', ' ');

		if (Tools.isNotEmpty(Constants.getString("searchActionOmitCharacters")))
		{
			words = words.replaceAll("["+Constants.getString("searchActionOmitCharacters")+"]", "");
		}

		boolean hasInputParams = hasInputParams(input);

		if ("***".equals(input.getAttribute("forceWords")))
		{
			words = "";
			hasInputParams = true;
		}

		input.removeAttribute("forceWords");

		String wordsAscii = DB.internationalToEnglish(words);
		StringTokenizer sTok = new StringTokenizer(words);
		words = "";
		String wordsMysql = "";
		StringBuilder wordsMysqlBuilder = new StringBuilder();
		boolean quotes = false;
		StringBuilder wordsBuilder = new StringBuilder();
		while (sTok.hasMoreElements())
		{
			pom = sTok.nextToken();
			if (pom.length() >= Constants.getInt("searchActionMinimumWordLength"))
			{
				pom = pom.trim();
				if (Constants.DB_TYPE == Constants.DB_MYSQL)
				{
					if (quotes == false && pom.startsWith("\""))
					{
						quotes = true;
						wordsMysqlBuilder.append(' ').append(pom);
						if (pom.trim().endsWith("\"")) quotes = false;
					}
					else if (quotes == true && pom.endsWith("\""))
					{
						quotes = false;
						wordsMysqlBuilder.append(' ').append(pom);
					}
					else if (pom.startsWith("+") || pom.startsWith("-") || pom.startsWith("~") || pom.startsWith("<")
							|| pom.startsWith("(") || pom.startsWith("*") || pom.endsWith("*"))
					{
						wordsMysqlBuilder.append(' ').append(pom);
					}
					else
					{
						wordsMysqlBuilder.append(" +").append(pom);
					}
				}
				wordsBuilder.append(' ').append(pom);

			}
		}
		words = wordsBuilder.toString();
		words = words.trim();
		wordsMysql = wordsMysqlBuilder.toString();
		wordsMysql = DB.internationalToEnglish(wordsMysql).trim();

		Logger.debug(null, "Search in Title: " +index);

		if (words.length() == 0 && hasInputParams==false)
		{
			output.setResults(new ArrayList<SearchDetails>());
			output.setWrong(true);
			output.setEmptyRequest(true);
			return output;
		}

		String sesWord;
		sesWord = (String) input.getAttribute("words");
		if (sesWord == null)
		{
			sesWord = "";
		}
		List<SearchDetails> aList = null;
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		int aListCount = 0;
		int totalResults = 0;
		StringBuilder sql = new StringBuilder();
		String sqlTotalResults;

		String D_DOCUMENT_FIELDS = DocDB.getDocumentFields();
		if (Constants.getBoolean("fulltextExecuteApps")) D_DOCUMENT_FIELDS += ", data_asc";
		int pocetVynechanychSuborov = 0;
		try
		{
			boolean perexGroupUseJoin = Constants.getBoolean("perexGroupUseJoin") && Arrays.toString(SearchTools.checkInputParams).contains("keyword");

			if (Constants.DB_TYPE == Constants.DB_MSSQL)
			{
				int limit = index + (perPage * 3);
				sql.append("SELECT TOP ")
					.append(limit)
					.append(' ').append(D_DOCUMENT_FIELDS).append(", data_asc FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE (CONTAINS(data_asc, ?) ");
				if (searchDetaultInTitle && input.getParameter("words")!=null) sql.append(" OR title LIKE ? ");
				sql.append(") ");

				sqlTotalResults = "SELECT count(d.doc_id) as totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE (CONTAINS(data_asc, ?) ";
				if (searchDetaultInTitle && input.getParameter("words")!=null) sqlTotalResults += " OR title LIKE ? ";
				sqlTotalResults += ") ";

				if (words.length() == 0)
				{
					sql.delete(0, sql.length());
					sql.append("SELECT TOP ")
						.append(limit)
						.append(' ').append(D_DOCUMENT_FIELDS).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE title IS NOT NULL ");
					sqlTotalResults = "SELECT count(d.doc_id) as totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE title IS NOT NULL ";
				}
			}
			else if (Constants.DB_TYPE == Constants.DB_ORACLE)
			{
				wordsAscii = wordsAscii.replace('\'', ' ');

				if (Constants.getBoolean("searchUseOracleText"))
				{
					StringBuilder oracleKeywords = new StringBuilder();
					StringTokenizer sto = new StringTokenizer(wordsAscii);
					while (sto.hasMoreTokens())
					{
						String keyword = sto.nextToken().trim();

						if (Tools.isEmpty(keyword)) continue;

						if (oracleKeywords.length()!=0) oracleKeywords.append(" AND ");

						oracleKeywords.append(keyword);
					}

					if(Tools.isNotEmpty(oracleKeywords.toString()))
					{
						String oracleTextMinScore = Constants.getString("searchOracleTextMinScore", "1");
						sql.append("SELECT ").append(D_DOCUMENT_FIELDS).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE CONTAINS(data_asc, '").append(oracleKeywords.toString()).append("', "+ORACLE_TEXT_CONTAINS_IDENTIFIER+")>"+oracleTextMinScore+" ");

						sqlTotalResults = "SELECT count(d.doc_id) as totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE CONTAINS(data_asc, '"+oracleKeywords.toString()+"', "+ORACLE_TEXT_CONTAINS_IDENTIFIER+")>"+oracleTextMinScore+" ";
					}
					else
					{
						sql.append("SELECT * FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE");
						sqlTotalResults = "SELECT count(d.doc_id) as totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE";
					}
				}
				else
				{
					String wordsForLike = Tools.replace(words, " ", "%");
					String wordsAsciiForLike = Tools.replace(wordsAscii, " ", "%");

					sql.append("SELECT ").append(D_DOCUMENT_FIELDS).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE (data_asc LIKE '%").append(DB.removeSlashes(wordsAsciiForLike.toLowerCase())).append("%' ");
					if (searchDetaultInTitle && input.getParameter("words")!=null) sql.append(" OR title LIKE '%").append(DB.removeSlashes(wordsForLike)).append("%' ");
					sql.append(") ");

					sqlTotalResults = "SELECT count(d.doc_id) as totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE (data_asc LIKE '%" + DB.removeSlashes(wordsAsciiForLike.toLowerCase()) + "%' ";
					if (searchDetaultInTitle && input.getParameter("words")!=null) sqlTotalResults += " OR title LIKE '%" + DB.removeSlashes(wordsForLike) + "%' ";
					sqlTotalResults += ") ";
				}
			}
			else if (Constants.DB_TYPE == Constants.DB_PGSQL)
			{
				sql.append("SELECT ").append((perexGroupUseJoin ? "DISTINCT " : "")+D_DOCUMENT_FIELDS).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE ( to_tsvector(data_asc) @@ to_tsquery(?) ");

				if (searchDetaultInTitle)
				{
					sql.append(" OR data_asc ILIKE ? ");
					if (input.getParameter("words")!=null) sql.append(" OR title ILIKE ? ");
				}
				sql.append(") ");

				sqlTotalResults = "SELECT count("+(perexGroupUseJoin ? "DISTINCT " : "")+"d.doc_id) AS totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE ( to_tsvector(data_asc) @@ to_tsquery(?) ";

				if (searchDetaultInTitle)
				{
					sqlTotalResults += " OR data_asc ILIKE ? ";
					if (input.getParameter("words")!=null) sqlTotalResults += " OR title ILIKE ? ";
				}
				sqlTotalResults += ") ";

				if (words.length()==0)
				{
					sql.delete(0, sql.length());
					sql.append("SELECT ").append((perexGroupUseJoin ? "DISTINCT " : "")+D_DOCUMENT_FIELDS).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE title IS NOT NULL ");
					sqlTotalResults = "SELECT count("+(perexGroupUseJoin ? "DISTINCT " : "")+"d.doc_id) AS totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE title IS NOT NULL ";
				}
			}
			else
			{
				sql.append("SELECT ").append((perexGroupUseJoin ? "DISTINCT " : "")+D_DOCUMENT_FIELDS).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE ( MATCH(title, data_asc) AGAINST (? IN BOOLEAN MODE) ");

				if (searchDetaultInTitle)
				{
					sql.append(" OR data_asc LIKE ? ");
					if (input.getParameter("words")!=null) sql.append(" OR title LIKE ? ");
				}
				sql.append(") ");

				sqlTotalResults = "SELECT count("+(perexGroupUseJoin ? "DISTINCT " : "")+"d.doc_id) AS totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE ( MATCH(title, data_asc) AGAINST (? IN BOOLEAN MODE) ";

				if (searchDetaultInTitle)
				{
					sqlTotalResults += " OR data_asc LIKE ? ";
					if (input.getParameter("words")!=null) sqlTotalResults += " OR title LIKE ? ";
				}
				sqlTotalResults += ") ";

				if (words.length()==0)
				{
					sql.delete(0, sql.length());
					sql.append("SELECT ").append((perexGroupUseJoin ? "DISTINCT " : "")+D_DOCUMENT_FIELDS).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE title IS NOT NULL ");
					sqlTotalResults = "SELECT count("+(perexGroupUseJoin ? "DISTINCT " : "")+"d.doc_id) AS totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE title IS NOT NULL ";
				}
			}

			if (Tools.isNotEmpty(searchFileNameQuery))
			{
				sql.append(" AND (").append(searchFileNameQuery).append(") ");
				sqlTotalResults += " AND (" + searchFileNameQuery + ") ";
			}

			if (user == null && searchAlsoProtectedPages==false)
			{
				sql.append(" AND (password_protected IS null OR password_protected='') ");
				sqlTotalResults += " AND (password_protected IS null OR password_protected='') ";
			}
			else if (user != null && searchAlsoProtectedPages==false)
			{
				if (user.isAdmin()==false || Constants.getBoolean("adminCheckUserGroups"))
				{
					StringBuilder sqlProtected = new StringBuilder(" AND (password_protected IS null OR password_protected=''");
					StringTokenizer stu = new StringTokenizer(user.getUserGroupsIds(), ",");
					while (stu.hasMoreTokens())
					{
						int groupId = Tools.getIntValue(stu.nextToken(), -1);
						if (groupId > 0)
						{
							sqlProtected.append(" OR password_protected='").append(groupId).append("' OR password_protected LIKE '").append(groupId).append(",%' OR password_protected LIKE '%,")
							.append(groupId).append(",%' OR password_protected LIKE '%,").append(groupId).append('\'');
						}
					}
					sqlProtected.append(") ");
					sql.append(sqlProtected.toString());
					sqlTotalResults += sqlProtected.toString();
				}
			}

			sql.append(addInputParamsSQL(input));
			sqlTotalResults += addInputParamsSQL(input);

			sql.append(" AND searchable="+DB.getBooleanSql(true)+" AND available="+DB.getBooleanSql(true)+" AND (external_link IS NULL OR external_link NOT LIKE '/files/protected%') ");

			if (Constants.getBoolean("multiDomainEnabled")) {
				String domainName = input.getDomainName();
				if (Tools.isNotEmpty(domainName)) {
					List<Integer> rootGroupIds = new ArrayList<>();

					List<GroupDetails> rootGroups = GroupsDB.getRootGroups();
					for (GroupDetails rootGroup : rootGroups) {
						if (domainName.equals(rootGroup.getDomainName())) {
							rootGroupIds.add(rootGroup.getGroupId());
						}
					}

					if (rootGroupIds.size() > 0) {
						sql.append(" AND root_group_l1 IN (");
						sqlTotalResults += " AND root_group_l1 IN (";
						for (i = 0; i < rootGroupIds.size(); i++) {
							if (i > 0) {
								sql.append(",");
								sqlTotalResults += ",";
							}
							sql.append(rootGroupIds.get(i));
							sqlTotalResults += rootGroupIds.get(i);
						}
						sql.append(") ");
						sqlTotalResults += ") ";
					}
				}
			}

			String searchWhereSql = (String)input.getAttribute("searchWhereSql");
			if (Tools.isNotEmpty(searchWhereSql))
			{
				sql.append(' ').append(searchWhereSql);
				sqlTotalResults += " "+searchWhereSql;
				input.removeAttribute("searchWhereSql");
			}

			String afterWhere =  sql.toString().indexOf("WHERE") != -1 ? sql.toString().substring(sql.toString().indexOf("WHERE")) : "";
			boolean notFoundPublishStartEnd = afterWhere.indexOf("publish_start") == -1 && afterWhere.indexOf("publish_end") == -1;
			output.setNotFoundPublishStartEnd(notFoundPublishStartEnd);
			input.setAttribute("notFoundPublishStartEnd", String.valueOf(notFoundPublishStartEnd));
			if(notFoundPublishStartEnd && Constants.getBoolean("showOnlyActualPublishedDoc"))
			{
				String sqlTmp = " AND (publish_start IS NULL OR publish_start <= ?) AND (publish_end IS NULL OR publish_end >= ?) ";
				sql.append(sqlTmp);
				sqlTotalResults += sqlTmp;
			}

			if(Tools.isNotEmpty(docIdsNotIn.toString()))
			{
				sql.append(" AND d.doc_id NOT IN ("+docIdsNotIn.substring(0, docIdsNotIn.length()-1)+") ");
				sqlTotalResults += " AND d.doc_id NOT IN ("+docIdsNotIn.substring(0, docIdsNotIn.length()-1)+") ";
			}

			String order_var = "ASC";
			String order = getParamAttribute("order", input, "asc");
			String orderType = getParamAttribute("orderType", input, "sort_priority");
			if ("desc".equalsIgnoreCase(order))
			{
				order_var = "DESC";
			}
			if ("asc".equalsIgnoreCase(order))
			{
				order_var = "ASC";
			}
			if ("lastUpdate".equalsIgnoreCase(orderType))
			{
				orderType = "date_created";
			}
			else if ("sortPriority".equalsIgnoreCase(orderType))
			{
				orderType = "sort_priority";
				if (Constants.DB_TYPE == Constants.DB_ORACLE && Constants.getBoolean("searchUseOracleText"))
				{
					orderType = "SCORE("+ORACLE_TEXT_CONTAINS_IDENTIFIER+")";
				}
			}
			else if ("title".equalsIgnoreCase(orderType))
			{
				orderType = "title";
			}
			else if ("publishStart".equalsIgnoreCase(orderType))
			{
				orderType = "publish_start";
			}
			else if ("saveDate".equalsIgnoreCase(orderType))
			{
				orderType = "date_created";
			}
			sql.append(" ORDER BY ").append(orderType).append(' ').append(order_var);

			for (i=2; i<=5; i++)
			{
				orderType = getParamAttribute("orderType"+i, input, null);
				if (Tools.isNotEmpty(orderType))
				{
					order_var = "ASC";
					order = getParamAttribute("order"+i, input, "asc");
					if ("desc".equalsIgnoreCase(order))
					{
						order_var = "DESC";
					}

					if ("lastUpdate".equalsIgnoreCase(orderType))
					{
						orderType = "date_created";
					}
					else if ("sortPriority".equalsIgnoreCase(orderType))
					{
						orderType = "sort_priority";
						if (Constants.DB_TYPE == Constants.DB_ORACLE && Constants.getBoolean("searchUseOracleText"))
						{
							orderType = "SCORE("+ORACLE_TEXT_CONTAINS_IDENTIFIER+")";
						}
					}
					else if ("title".equalsIgnoreCase(orderType))
					{
						orderType = "title";
					}
					else if ("publishStart".equalsIgnoreCase(orderType))
					{
						orderType = "publish_start";
					}

					sql.append(", ").append(orderType).append(' ').append(order_var);
				}
			}

			sqlTotalResults += " AND searchable="+DB.getBooleanSql(true)+" AND available="+DB.getBooleanSql(true)+" AND (external_link IS NULL OR external_link NOT LIKE '/files/protected%') ";
			if (Constants.DB_TYPE == Constants.DB_MYSQL || Constants.DB_TYPE == Constants.DB_PGSQL)
			{
				if (Constants.DB_TYPE == Constants.DB_PGSQL) sql.append(" OFFSET ").append(index).append(" LIMIT ").append((perPage * 3 + 1));
				else sql.append(" LIMIT ").append(index).append(", ").append((perPage * 3 + 1));

				sql.append(' ');
			}
			sTok = new StringTokenizer(words);
			if(sql != null)
				sql = new StringBuilder(Tools.replace(sql.toString(), "WHERE AND", "WHERE"));
			if(Tools.isNotEmpty(sqlTotalResults))
				sqlTotalResults = Tools.replace(sqlTotalResults, "WHERE AND", "WHERE");

			Logger.debug(SearchAction.class,"words: " + words);
			Logger.debug(SearchAction.class,"wordsAscii: " + wordsAscii);
			Logger.debug(SearchAction.class,"wordsMysql: " + wordsMysql);
			Logger.debug(SearchAction.class,"search sql="+sql);
			int psIndex = 1;

			DebugTimer dt = new DebugTimer("SearchAction");

			db_conn = DBPool.getConnectionReadUncommited();

			ps = db_conn.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

			psIndex = 1;
			if (Constants.DB_TYPE == Constants.DB_ORACLE)
			{
				psIndex = addInputParamsSQL(input, ps, psIndex);
			}
			else if (Constants.DB_TYPE == Constants.DB_MYSQL)
			{
				if (words.length()>0)
				{
					ps.setString(psIndex++, wordsMysql);
					if (searchDetaultInTitle)
					{
						ps.setString(psIndex++, "%"+wordsAscii+"%");
						if (input.getParameter("words")!=null) ps.setString(psIndex++, "%"+words+"%");
					}
				}
				psIndex = addInputParamsSQL(input, ps, psIndex);
			}
			else if (Constants.DB_TYPE == Constants.DB_PGSQL)
			{
				if (words.length()>0)
				{
					ps.setString(psIndex++, Tools.replace(DB.internationalToEnglish(words.trim()).toLowerCase(), " ", " & "));
					if (searchDetaultInTitle)
					{
						ps.setString(psIndex++, wordsAscii);
						if (input.getParameter("words")!=null) ps.setString(psIndex++, words);
					}
				}
				psIndex = addInputParamsSQL(input, ps, psIndex);
			}
			else
			{
				if (words.length()>0)
				{
					StringBuilder mssqlContainsMatcher = toMssqlContainsSearch(wordsAscii);

					if(mssqlContainsMatcher.length() > 0)
					{
						ps.setString(psIndex++, mssqlContainsMatcher.toString());
						if (searchDetaultInTitle && input.getParameter("words")!=null) ps.setString(psIndex++, "%"+wordsAscii+"%");
					}
					else
					{
						ps.setString(psIndex++, wordsAscii);
					}
				}
				psIndex = addInputParamsSQL(input, ps, psIndex);
			}
			if(notFoundPublishStartEnd &&  Constants.getBoolean("showOnlyActualPublishedDoc"))
			{
				Date now = new Date();
				ps.setTimestamp(psIndex++, new Timestamp(now.getTime()));
				ps.setTimestamp(psIndex++, new Timestamp(now.getTime()));
			}
			rs = ps.executeQuery();

			dt.diff("after execute");

			if (Constants.DB_TYPE == Constants.DB_MSSQL || Constants.DB_TYPE == Constants.DB_ORACLE)
			{
				if (index > 0)
				{
					for (i = 0; i < index; i++)
					{
						rs.next();
					}
				}
			}
			aList = new ArrayList<>();
			SearchDetails qDet;
			String link;

			String ttf = getTextToFind(input);
			dt.diff("after prepare TTF");

			while (aList.size()<perPage && rs.next())
			{
				dt.diff("NEXT "+aListCount);

				aListCount++;
				if (aListCount == (perPage + 1))
				{
					Logger.debug(SearchAction.class,"aListCount="+aListCount+" perPage="+perPage);
					if (Constants.DB_TYPE == Constants.DB_MSSQL || Constants.DB_TYPE == Constants.DB_ORACLE)
					{
						break;
					}
					continue;
				}
				qDet = new SearchDetails();
				DocDB.getDocDetails(rs, qDet, false, true);

				dt.diff("after getDocDetails: docId=" + qDet.getDocId() + " title=" + qDet.getTitle() + " path=" + qDet.getVirtualPath() + " size="+qDet.getData().length());
				qDet.setDataOriginal(qDet.getData());

				if (Constants.getBoolean("fulltextExecuteApps"))
				{
					String dataAsc = DB.getDbString(rs, "data_asc");
					int separatorIndex = dataAsc.indexOf(EditorDB.RENDER_DATA_SEPARATOR);
					if (separatorIndex>0)
					{
						qDet.setDataOriginal(dataAsc.substring(0, separatorIndex));
					}
				}

				qDet.setData("");

				GroupDetails group = groupsDB.getGroup(qDet.getGroupId());

				if (Tools.isEmpty(qDet.getPasswordProtected()) && group != null && Tools.isNotEmpty(group.getPasswordProtected()))
				{
					qDet.setPasswordProtected(group.getPasswordProtected());
				}

				if (group != null)
				{
					if (rootGroupIdsTable.get(Integer.valueOf(group.getGroupId()))==null && group.isInternal())
					{
						Logger.debug(SearchAction.class, "preskakujem interny adresar: "+group.getGroupIdName());
						aListCount--;
                        pocetVynechanychSuborov++;
						continue;
					}
					link = groupsDB.getNavbar(group.getGroupId());
				}
				else
				{
					link = null;
				}
				try
				{
					if (qDet.getExternalLink().startsWith("/files/"))
					{
						String[] fileLink = MultiDomainFilter.fixDomainPaths(qDet.getExternalLink(), null).split("\\/");
						link = "";
						for (int p=0; p<fileLink.length-1; p++)
						{
							String part = fileLink[p];
							if (Tools.isEmpty(link)) link = part;
							else link += " "+Constants.getString("navbarSeparator")+" "+part; //NOSONAR
						}
						if (input.getAttribute("searchDontCheckFile")==null)
						{
							IwcmFile iwf = new IwcmFile(Tools.getRealPath(qDet.getExternalLink()));
							if (iwf.exists()==false)
							{
								Logger.debug(SearchAction.class, "Subor "+qDet.getExternalLink()+" neexistuje, preskakujem");
								aListCount--;
                                pocetVynechanychSuborov++;
								continue;
							}
						}
						FileArchivatorBean fab = FileArchivatorDB.getByUrl(qDet.getExternalLink());
						if (fab != null)
						{
							qDet.setTitle(fab.getVirtualFileName());
						}

					}
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
				}

				if (link == null)
				{
					qDet.setLink("");
				}
				else
				{
					qDet.setLink(link);
				}

				SearchSnippet searchSnippet = new SearchSnippet(qDet, ttf, input.getBooleanValue("fastSnippet", false));
				String snippet = searchSnippet.getSnippet();

				if (Tools.isEmpty(snippet)) snippet = "&nbsp;";
				qDet.setData(snippet);
				dt.diff("after snippet, size="+qDet.getData().length());

				aList.add(qDet);
			}

			hasAnotherPage = rs.next();

			dt.diff("RS iterated");

			rs.close();
			dt.diff("after rs.close");

			ps.close();
			dt.diff("after ps.close");

			db_conn.close();
			dt.diff("after db_conn.close");

			rs = null;
			ps = null;
			db_conn = null;

			dt.diff("done");
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			output.setWrong(true);
			output.setNotFound(true);
			output.setErrorMessage("Chyba pri práci s databázou...");
			output.setForward(error);
			return output;
		}
		finally
		{
			try
			{
				if (rs != null) try { rs.close(); } catch (Exception te) {sk.iway.iwcm.Logger.error(te);}
				if (ps != null) try { ps.close(); } catch (Exception te) {sk.iway.iwcm.Logger.error(te);}
				if (db_conn != null) try { db_conn.close(); } catch (Exception te) {}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
				output.setErrorMessage("Chyba pri práci s databázou...");
			}
		}

		totalResults = getResultsCount(sqlTotalResults, input, words, wordsAscii, wordsMysql, aList.size(), hasAnotherPage);
		output.setWords(words);
		output.setResults(aList);
		if (aList.isEmpty())
		{
			output.setWrong(true);
			output.setNotFound(true);
			return output;
		}
		StringBuilder paramsLinkBuilder = new StringBuilder();
		Enumeration<String> parameterNames = input.getParameterNames();
		String name;
		while (parameterNames.hasMoreElements())
		{
			name = parameterNames.nextElement();
			if ("index".equals(name) || "docid".equals(name)) continue;

			String[] values = input.getParameterValues(name);
		   for (int v=0; values != null && v<values.length; v++)
		   {
		   	paramsLinkBuilder.append("&amp;").append(Tools.URLEncode(name)).append('=').append(Tools.URLEncode(values[v]));
		   }
		}
		String paramsLink = paramsLinkBuilder.toString();
		output.setParamsLink(paramsLink);

		output.setFromIndex(index + 1);
		int toIndex = index + aList.size();
		output.setToIndex(toIndex);
		if(pocetVynechanychSuborov > 0 && totalResults >= pocetVynechanychSuborov)
        {
            totalResults = totalResults - pocetVynechanychSuborov;
        }
		Logger.debug(SearchAction.class, "totalResults="+totalResults);
		output.setTotalResults(totalResults);

		output.setPages(preparePages(perPage, index, totalResults, paramsLink,"search.do"));

		if (totalResults > toIndex)
		{
			if (!english)
			{
				output.setNext("<a href=\"search.do?index=" + (index + perPage) + paramsLink + "\"> Ďalej &gt;&gt;&gt; </a>");
			}
			else
			{
				output.setNext("<a href=\"search.do?index=" + (index + perPage) + paramsLink + "\"> Next &gt;&gt;&gt; </a>");
			}

			output.setNextHref("search.do?index=" + (index + perPage) + paramsLink);
		}
		if (index != 0)
		{
			if (!english)
			{
				output.setPrev("<a href=\"search.do?index=" + (index - perPage) + paramsLink + "\"> &lt;&lt;&lt; Späť </a>");
			}
			else
			{
				output.setPrev("<a href=\"search.do?index=" + (index - perPage) + paramsLink + "\"> &lt;&lt;&lt; Back </a>");
			}
			output.setPrevHref("search.do?index=" + (index - perPage) + paramsLink);
		}

		return output;
	}

	public static String search(HttpServletRequest request, HttpServletResponse response)
	{
		PageParams pageParams = new PageParams(request);

		String searchType = pageParams.getValue("searchType", "");
		if(Tools.isEmpty(searchType) || "auto".equals(searchType)) {
			// Empty and auto keep the globally configured search type.
			searchType = Constants.getString("searchType");
		}

		if ("lucene".equals(searchType) || Constants.getBoolean("luceneAsDefaultSearch") || "true".equals(request.getParameter("useLucene")) || pageParams.getBooleanValue("useLucene", false))
		{
			// useLucene=true allows comparing standard database search with Lucene search.
			return LuceneSearchAction.search(request);
		}
		if ("semantic".equals(searchType) || "hybrid".equals(searchType) || "true".equals(request.getParameter("useSemantic")) || pageParams.getBooleanValue("useSemantic", false))
		{
			if (isSemanticSearchAvailable()) {
				return SemanticSearchAction.search(request, response);
			}

			Logger.debug(SearchAction.class, "Semantic/hybrid search requested but semantic search is not available, falling back to database search");
		}
		// ELSE - standard database search

		String forward = resolveForward(request.getParameter("lng"), request.getParameter("forward"));
		long wait;
		if (request.getAttribute("disableSearchSpamProtection")==null && request.getAttribute("forceWords")==null && request.getParameter("index") == null)
		{
			if ((wait=SpamProtection.getWaitTimeout("search", request)) != 0)
			{
				request.setAttribute("wrong", "true");
				request.setAttribute("crossHourlyLimit", "true");
				request.setAttribute("wait", (wait/60/1000));
				return forward;
			}

			if (!SpamProtection.canPost("search", "", request))
			{
				request.setAttribute("wrong", "true");
				request.setAttribute("crossTimeout", "true");
				return forward;
			}
		}

		SearchActionInput input = SearchActionInput.fromRequest(request);
		input.setUser(UsersDB.getCurrentUser(request));
		input.setDomainName(DocDB.getDomain(request));
		SearchActionOutput output = search(input);
		output.applyToRequest(request);
		String searchTerm = request.getParameter("words");
		if (Tools.isEmpty(searchTerm)) searchTerm = request.getParameter("text");
		if (Tools.isNotEmpty(searchTerm))
		{
			StatDB.addStatSearchLocal(searchTerm, Tools.getIntValue(request.getParameter("docid"), -1), request);
		}

		return output.getForward();
	}

	protected static void preparePages(HttpServletRequest request, int perPage, int index, int totalResults, String paramsLink, String searchAction)
	{
		request.removeAttribute("pages");
		List<LabelValueDetails> pages = preparePages(perPage, index, totalResults, paramsLink, searchAction);
		if (pages.size()>1) request.setAttribute("pages", pages);
	}

	protected static List<LabelValueDetails> preparePages(int perPage, int index, int totalResults, String paramsLink, String searchAction)
	{
		List<LabelValueDetails> pages = new LinkedList<>();
		boolean paging = totalResults > perPage;
		int page = (index / perPage) + 1;
		if (paging)
		{
			LabelValueDetails lv_page;
			int p = 1;
			int start = 0;
			if (perPage > 0)
			{
				while (start < totalResults)
				{
					if (p == page)
					{
						lv_page = new LabelValueDetails(Integer.toString(p), "");
					}
					else
					{
						lv_page = new LabelValueDetails(Integer.toString(p), searchAction+"?index=" + start + paramsLink);
					}
					pages.add(lv_page);
					p++;
					start = start + perPage;
				}
			}
		}
		return pages;
	}

	private static StringBuilder toMssqlContainsSearch(String words)
	{
		String wordsMssql = words.replaceAll("[^A-Za-z0-9]", " ").replaceAll("\\s+", " ");
		StringBuilder mssqlContainsMatcher = new StringBuilder();
		//uniq
		String[] terms = new HashSet<String>(Arrays.asList(wordsMssql.split(" "))).toArray(new String[]{});

		for(String term : terms)
			mssqlContainsMatcher.append(" AND \"").append(term).append("*\"");
		mssqlContainsMatcher.delete(0, " AND ".length());
		return mssqlContainsMatcher;
	}

	/**
	 * Deprecated, pouzite priamo SearchTools.htmlToPlain
	 * prevod HTML do plain textu
	 *
	 * @param html
	 *           Description of the Parameter
	 * @return Description of the Return Value
	 * @deprecated - use SearchTools.htmlToPlain
	 */
	@Deprecated
	public static String htmlToPlain(String html)
	{
		return SearchTools.htmlToPlain(html);
	}

	/**
	 * Odstrani z HTML kodu riadiace bloky typu !INCLUDE(...)!, !PARAM(...)!
	 * @param html
	 * @return
	 * @deprecated - use SearchTools.removeCommands
	 */
	@Deprecated
	public static String removeCommands(String html)
	{
		return SearchTools.removeCommands(html);
	}



	protected static String getParamAttribute(String name, HttpServletRequest request, String defaultValue)
	{
		String ret = request.getParameter(name);
		if (ret == null)
		{
			ret = (String) request.getAttribute(name);
			if (ret == null)
				ret = defaultValue;
		}
		else
		{
			//nebezpecne znaky odpaz
			if (Constants.containsKey("searchActionOmitCharacters"))
			{
				ret = ret.replaceAll("["+Constants.getString("searchActionOmitCharacters")+"]", "");
			}
		}
		if (ret != null)
		{
			ret = ret.replace('\'', ' ');
			ret = ret.replace('\"', ' ');
			ret = ret.replace(',', ' ');
			if (name.indexOf("publish")==-1)
			{
				ret = ret.replace('.', ' ');
			}
			ret = ret.replace(';', ' ');
		}
		return (ret);
	}

	protected static String getParamAttribute(String name, SearchActionInput input, String defaultValue)
	{
		String ret = input.getParameter(name);
		if (ret == null)
		{
			Object attribute = input.getAttribute(name);
			ret = attribute instanceof String ? (String) attribute : defaultValue;
		}
		else if (Constants.containsKey("searchActionOmitCharacters"))
		{
			ret = ret.replaceAll("["+Constants.getString("searchActionOmitCharacters")+"]", "");
		}
		if (ret != null)
		{
			ret = ret.replace('\'', ' ');
			ret = ret.replace('\"', ' ');
			ret = ret.replace(',', ' ');
			if (name.indexOf("publish")==-1)
			{
				ret = ret.replace('.', ' ');
			}
			ret = ret.replace(';', ' ');
		}
		return ret;
	}

	/**
	 * Vrati parameter, alebo atribut bez escapovania zlych hodnot, je mozne nastavit len za pouzitia PreparedStatement
	 * @param name
	 * @param request
	 * @param defaultValue
	 * @return
	 */
	protected static String getParamAttributeUnsafe(String name, HttpServletRequest request, String defaultValue)
	{
		String ret = request.getParameter(name);
		if (ret == null)
		{
			ret = (String) request.getAttribute(name);
			if (ret == null)
				ret = defaultValue;
		}
		else
		{
			//nebezpecne znaky odpaz (typu %%% pre vyhladanie cohokolvek)
			if (Constants.containsKey("searchActionOmitCharacters"))
			{
				ret = ret.replaceAll("["+Constants.getString("searchActionOmitCharacters")+"]", "");
			}
		}
		return (ret);
	}

	protected static String getParamAttributeUnsafe(String name, SearchActionInput input, String defaultValue)
	{
		String ret = input.getParameter(name);
		if (ret == null)
		{
			Object attribute = input.getAttribute(name);
			ret = attribute instanceof String ? (String) attribute : defaultValue;
		}
		else if (Constants.containsKey("searchActionOmitCharacters"))
		{
			ret = ret.replaceAll("["+Constants.getString("searchActionOmitCharacters")+"]", "");
		}
		return ret;
	}


	/**
	 * Vrati pocet zaznamov vyhovujucich kriteriam vyhladavani na zaklade SQL dotazu.
	 * Pri nastaveni Constants.searchActionOptimize na true sa snazi tento pocet odhadnut a dotaz nevykonava.
	 */
	private static int getResultsCount(String sqlTotalResults, HttpServletRequest request, String words, String wordsAscii, String wordsMysql, int fetchedCount, boolean hasAnotherPage)
	{
		if (fetchedCount == 0)
			return 0;

		int resultsPerPage = Integer.parseInt(getParamAttribute("perpage", request, "10"));
		int index = request.getParameter("index") != null ? Integer.parseInt(request.getParameter("index")) : 0;

		if (Constants.getBoolean("searchActionOptimize") && hasAnotherPage)
			return index + resultsPerPage + resultsPerPage;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		int totalResults = 0;

		try
		{
			Logger.debug(SearchAction.class,"search sql TOTAL="+sqlTotalResults);

			db_conn = DBPool.getConnectionReadUncommited();

			int psIndex = 1;
			ps = db_conn.prepareStatement(sqlTotalResults);
			if (Constants.DB_TYPE == Constants.DB_ORACLE)
			{
				//pre oracle to mam rovno v query

				//musime este doplnit hodnoty pre parametre v requeste
				psIndex = addInputParamsSQL(request, ps, psIndex);
			}
			else if (Constants.DB_TYPE == Constants.DB_MYSQL)
			{
				if (words.length()>0)
				{
					ps.setString(psIndex++, wordsMysql);
					if (Constants.getBoolean("searchDetaultInTitle") || "true".equals(request.getParameter("searchDetaultInTitle")))
					{
						ps.setString(psIndex++, "%"+wordsAscii+"%");
						if (request.getParameter("words")!=null) ps.setString(psIndex++, "%"+words+"%");
					}
				}
				psIndex = addInputParamsSQL(request, ps, psIndex);
			}
			else if (Constants.DB_TYPE == Constants.DB_PGSQL)
			{
				if (words.length()>0)
				{
					ps.setString(psIndex++, Tools.replace(DB.internationalToEnglish(words).toLowerCase(), " ", " & "));
					if (Constants.getBoolean("searchDetaultInTitle") || "true".equals(request.getParameter("searchDetaultInTitle")))
					{
						ps.setString(psIndex++, "%"+wordsAscii+"%");
						if (request.getParameter("words")!=null) ps.setString(psIndex++, "%"+words+"%");
					}
				}
				psIndex = addInputParamsSQL(request, ps, psIndex);
			}
			else
			{
				if (words.length()>0)
				{
					//MSSQL
					StringBuilder mssqlSearch = toMssqlContainsSearch(wordsAscii);
					if (mssqlSearch.length() > 0)
					{
						ps.setString(psIndex++, mssqlSearch.toString());
					}
					else
					{
						ps.setString(psIndex++, wordsAscii);
					}
					//	title
					if ((Constants.getBoolean("searchDetaultInTitle") || "true".equals(request.getParameter("searchDetaultInTitle"))) && request.getParameter("words")!=null) ps.setString(psIndex++, "%"+words+"%");
				}

				psIndex = addInputParamsSQL(request, ps, psIndex);
			}
			if(request.getAttribute("notFoundPublishStartEnd") != null && "true".equals((String)request.getAttribute("notFoundPublishStartEnd")) &&  Constants.getBoolean("showOnlyActualPublishedDoc")) //NOSONAR
			{
				Date now = new Date();
				ps.setTimestamp(psIndex++, new Timestamp(now.getTime()));
				ps.setTimestamp(psIndex++, new Timestamp(now.getTime()));
			}
			//Logger.println(this,sqlTotalResults);
			rs = ps.executeQuery();
			if (rs.next())
			{
				totalResults = rs.getInt("totalr");
			}
			rs.close();
			ps.close();
			db_conn.close();

			rs = null;
			ps = null;
			db_conn = null;

			Logger.debug(SearchAction.class, "totalResults="+totalResults);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2){sk.iway.iwcm.Logger.error(ex2);}
		}

		return totalResults;
	}

	private static int getResultsCount(String sqlTotalResults, SearchActionInput input, String words, String wordsAscii, String wordsMysql, int fetchedCount, boolean hasAnotherPage)
	{
		if (fetchedCount == 0)
			return 0;

		int resultsPerPage = Integer.parseInt(getParamAttribute("perpage", input, "10"));
		int index = input.getParameter("index") != null ? Integer.parseInt(input.getParameter("index")) : 0;

		if (Constants.getBoolean("searchActionOptimize") && hasAnotherPage)
			return index + resultsPerPage + resultsPerPage;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int totalResults = 0;

		try
		{
			Logger.debug(SearchAction.class,"search sql TOTAL="+sqlTotalResults);
			db_conn = DBPool.getConnectionReadUncommited();
			int psIndex = 1;
			ps = db_conn.prepareStatement(sqlTotalResults);
			if (Constants.DB_TYPE == Constants.DB_ORACLE)
			{
				psIndex = addInputParamsSQL(input, ps, psIndex);
			}
			else if (Constants.DB_TYPE == Constants.DB_MYSQL)
			{
				if (words.isEmpty()==false)
				{
					ps.setString(psIndex++, wordsMysql);
					if (Constants.getBoolean("searchDetaultInTitle") || "true".equals(input.getParameter("searchDetaultInTitle")))
					{
						ps.setString(psIndex++, "%"+wordsAscii+"%");
						if (input.getParameter("words")!=null) ps.setString(psIndex++, "%"+words+"%");
					}
				}
				psIndex = addInputParamsSQL(input, ps, psIndex);
			}
			else if (Constants.DB_TYPE == Constants.DB_PGSQL)
			{
				if (words.isEmpty()==false)
				{
					ps.setString(psIndex++, Tools.replace(DB.internationalToEnglish(words).toLowerCase(), " ", " & "));
					if (Constants.getBoolean("searchDetaultInTitle") || "true".equals(input.getParameter("searchDetaultInTitle")))
					{
						ps.setString(psIndex++, "%"+wordsAscii+"%");
						if (input.getParameter("words")!=null) ps.setString(psIndex++, "%"+words+"%");
					}
				}
				psIndex = addInputParamsSQL(input, ps, psIndex);
			}
			else
			{
				if (words.isEmpty()==false)
				{
					StringBuilder mssqlSearch = toMssqlContainsSearch(wordsAscii);
					if (mssqlSearch.isEmpty()==false)
					{
						ps.setString(psIndex++, mssqlSearch.toString());
					}
					else
					{
						ps.setString(psIndex++, wordsAscii);
					}
					if ((Constants.getBoolean("searchDetaultInTitle") || "true".equals(input.getParameter("searchDetaultInTitle"))) && input.getParameter("words")!=null) {
						ps.setString(psIndex++, "%"+words+"%");
					}
				}
				psIndex = addInputParamsSQL(input, ps, psIndex);
			}
			if("true".equals(input.getAttribute("notFoundPublishStartEnd")) && Constants.getBoolean("showOnlyActualPublishedDoc"))
			{
				Date now = new Date();
				ps.setTimestamp(psIndex++, new Timestamp(now.getTime()));
				ps.setTimestamp(psIndex++, new Timestamp(now.getTime()));
			}
			rs = ps.executeQuery();
			if (rs.next())
			{
				totalResults = rs.getInt("totalr");
			}
			Logger.debug(SearchAction.class, "totalResults="+totalResults);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2){sk.iway.iwcm.Logger.error(ex2);}
		}

		return totalResults;
	}

	/**
	 * Vrati SQL prikaz pre pridanie parametrov field_a, field_b... (ak su zadane)
	 * @param request
	 * @return
	 */
	public static String addInputParamsSQL(HttpServletRequest request)
	{
		StringBuilder ret = new StringBuilder();
		int len = SearchTools.checkInputParams.length;
		int i;
		String param;

		//	pridanie publish_start a publish_end parametrov nerobime ak je nejaky rozumny dateText
		String dateText = getParamAttributeUnsafe("dateText", request, null);
		boolean checkPublishDates = false;
		if (dateText == null || "custom".equals(dateText))
		{
			checkPublishDates = true;
		}

		for (i=0; i<len; i++)
		{
			param = getParamAttributeUnsafe(SearchTools.checkInputParams[i], request, null);
			if (Tools.isNotEmpty(param))
			{
				if (SearchTools.checkInputParams[i].equals("publish_start"))
				{
					if (checkPublishDates)
					{
						ret.append(" AND ( (publish_start IS NOT NULL AND publish_start >= ?) OR (publish_start IS NULL AND date_created >= ?) ) ");
					}
				}
				else if (SearchTools.checkInputParams[i].equals("publish_start_lt"))
				{
					if (checkPublishDates)
					{
						ret.append(" AND ( (publish_start IS NOT NULL AND publish_start <= ?) OR (publish_start IS NULL AND date_created <= ?) ) ");
					}
				}
				else if (SearchTools.checkInputParams[i].equals("publish_end"))
				{
					if (checkPublishDates)
					{
						ret.append(" AND ( (publish_end IS NOT NULL AND publish_end <= ?) OR (publish_end IS NULL AND date_created <= ?) ) ");
					}
				}
				else if (SearchTools.checkInputParams[i].equals("publish_end_gt"))
				{
					if (checkPublishDates)
					{
						ret.append(" AND ( (publish_end IS NOT NULL AND publish_end >= ?) OR (publish_end IS NULL AND date_created >= ?) ) ");
					}
				}
				else if (SearchTools.checkInputParams[i].equals("temp_id"))
				{
					ret.append(" AND " + SearchTools.checkInputParams[i] + " = ? ");
				}
				else if (SearchTools.checkInputParams[i].equals("keyword"))
				{
					if(Constants.getBoolean("perexGroupUseJoin")) ret.append(" AND p.perex_group_id = ? ");
					else ret.append(" AND ( perex_group LIKE ? ) "); //perex skupiny maju osetrene vkladanie pomocou , a , na zaciatku a konci
				}
				else
				{
					//ak mame viac slov rozdel po slovach
					if (param != null) {
						StringTokenizer st = new StringTokenizer(param);
						if (st.countTokens()<2 || param.startsWith("\""))
						{
							ret.append(" AND " + SearchTools.checkInputParams[i] + " LIKE ?");
						}
						else
						{
							String mode = getInputParamMode(SearchTools.checkInputParams[i], request);
							ret.append(" AND (");
							while (st.hasMoreTokens())
							{
								st.nextToken();
								ret.append(SearchTools.checkInputParams[i]).append(" LIKE ? ");
								if (st.hasMoreTokens()) ret.append(mode).append(' ');
							}
							ret.append(") ");
						}
					}
				}
			}
		}
		//	textove vyjadrenie dateText = "today,lastWeek,lastMonth";
		if (checkPublishDates==false && Tools.isNotEmpty(dateText))
		{
			ret.append(" AND ( (publish_start IS NOT NULL AND publish_start >= ?) OR (publish_start IS NULL AND date_created >= ?) ) AND ( (publish_end IS NOT NULL AND publish_end <= ?) OR (publish_end IS NULL AND date_created <= ?) ) ");
		}
		return(ret).toString();
	}

	public static String addInputParamsSQL(SearchActionInput input)
	{
		StringBuilder ret = new StringBuilder();
		int len = SearchTools.checkInputParams.length;
		String dateText = getParamAttributeUnsafe("dateText", input, null);
		boolean checkPublishDates = dateText == null || "custom".equals(dateText);

		for (int i=0; i<len; i++)
		{
			String param = getParamAttributeUnsafe(SearchTools.checkInputParams[i], input, null);
			if (Tools.isNotEmpty(param))
			{
				if (SearchTools.checkInputParams[i].equals("publish_start"))
				{
					if (checkPublishDates) ret.append(" AND ( (publish_start IS NOT NULL AND publish_start >= ?) OR (publish_start IS NULL AND date_created >= ?) ) ");
				}
				else if (SearchTools.checkInputParams[i].equals("publish_start_lt"))
				{
					if (checkPublishDates) ret.append(" AND ( (publish_start IS NOT NULL AND publish_start <= ?) OR (publish_start IS NULL AND date_created <= ?) ) ");
				}
				else if (SearchTools.checkInputParams[i].equals("publish_end"))
				{
					if (checkPublishDates) ret.append(" AND ( (publish_end IS NOT NULL AND publish_end <= ?) OR (publish_end IS NULL AND date_created <= ?) ) ");
				}
				else if (SearchTools.checkInputParams[i].equals("publish_end_gt"))
				{
					if (checkPublishDates) ret.append(" AND ( (publish_end IS NOT NULL AND publish_end >= ?) OR (publish_end IS NULL AND date_created >= ?) ) ");
				}
				else if (SearchTools.checkInputParams[i].equals("temp_id"))
				{
					ret.append(" AND ").append(SearchTools.checkInputParams[i]).append(" = ? ");
				}
				else if (SearchTools.checkInputParams[i].equals("keyword"))
				{
					if(Constants.getBoolean("perexGroupUseJoin")) ret.append(" AND p.perex_group_id = ? ");
					else ret.append(" AND ( perex_group LIKE ? ) ");
				}
				else
				{
					StringTokenizer st = new StringTokenizer(param);
					if (st.countTokens()<2 || param.startsWith("\""))
					{
						ret.append(" AND ").append(SearchTools.checkInputParams[i]).append(" LIKE ?");
					}
					else
					{
						String mode = getInputParamMode(SearchTools.checkInputParams[i], input);
						ret.append(" AND (");
						while (st.hasMoreTokens())
						{
							st.nextToken();
							ret.append(SearchTools.checkInputParams[i]).append(" LIKE ? ");
							if (st.hasMoreTokens()) ret.append(mode).append(' ');
						}
						ret.append(") ");
					}
				}
			}
		}
		if (checkPublishDates==false && Tools.isNotEmpty(dateText))
		{
			ret.append(" AND ( (publish_start IS NOT NULL AND publish_start >= ?) OR (publish_start IS NULL AND date_created >= ?) ) AND ( (publish_end IS NOT NULL AND publish_end <= ?) OR (publish_end IS NULL AND date_created <= ?) ) ");
		}
		return ret.toString();
	}

	/**
	 * Prida parametre do PreparedStatementu
	 * @param request
	 * @param ps
	 * @return
	 * @throws SQLException
	 */
	public static int addInputParamsSQL(HttpServletRequest request, PreparedStatement ps, int psIndex) throws SQLException
	{
		int len = SearchTools.checkInputParams.length;
		int i;
		String param;

		//pridanie publish_start a publish_end parametrov nerobime ak je nejaky rozumny dateText
		String dateText = getParamAttributeUnsafe("dateText", request, null);
		boolean checkPublishDates = false;
		if (dateText == null || "custom".equals(dateText))
		{
			checkPublishDates = true;
		}

		for (i=0; i<len; i++)
		{
			param = getParamAttributeUnsafe(SearchTools.checkInputParams[i], request, null);
			if (Tools.isNotEmpty(param))
			{
				Logger.debug(SearchAction.class, "addPram "+psIndex+" "+ SearchTools.checkInputParams[i]+"="+param);
				if (SearchTools.checkInputParams[i].equals("publish_start"))
				{
					if (checkPublishDates)
					{
						long d = DB.getTimestamp(param, "0:00");
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
					}
				}
				else if (SearchTools.checkInputParams[i].equals("publish_start_lt"))
				{
					if (checkPublishDates)
					{
						long d = DB.getTimestamp(param);
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
					}
				}
				else if (SearchTools.checkInputParams[i].equals("publish_end"))
				{
					if (checkPublishDates)
					{
						long d = DB.getTimestamp(param, "23:59");
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
					}
				}
				else if (SearchTools.checkInputParams[i].equals("publish_end_gt"))
				{
					if (checkPublishDates)
					{
						long d = DB.getTimestamp(param);
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
					}
				}
				else if (SearchTools.checkInputParams[i].equals("temp_id"))
				{
					ps.setInt(psIndex++, Tools.getIntValue(param, 0));
				}
				else if (SearchTools.checkInputParams[i].equals("keyword"))
				{
					//skonvertuj keyword na ID skupiny
					int perexGroupId = 0;
					DocDB docDB = DocDB.getInstance();
					PerexGroupBean pgb = docDB.getPerexGroup(-1, param);
					if (pgb != null) perexGroupId = pgb.getPerexGroupId();

					if(Constants.getBoolean("perexGroupUseJoin")) ps.setInt(psIndex++, perexGroupId);
					else ps.setString(psIndex++, "%,"+perexGroupId+",%");
				}
				else
				{
					if (param != null) {
						StringTokenizer st = new StringTokenizer(param);
						if (st.countTokens()<2 || param.startsWith("\""))
						{
							ps.setString(psIndex++, "%"+Tools.replace(param, "\"", "")+"%");
						}
						else
						{
							while (st.hasMoreTokens())
							{
								ps.setString(psIndex++, "%"+st.nextToken()+"%");
							}
						}
					}
				}
			}
		}
		//	textove vyjadrenie dateText = "today,lastWeek,lastMonth";
		if (checkPublishDates==false && Tools.isNotEmpty(dateText))
		{
			Calendar cal = Calendar.getInstance();
			long dEnd = DB.getTimestamp(Tools.formatDate(cal.getTimeInMillis()), "23:59");
			if ("lastWeek".equals(dateText))
			{
				cal.add(Calendar.WEEK_OF_YEAR, -1);
			}
			else if ("lastMonth".equals(dateText))
			{
				cal.add(Calendar.MONTH, -1);
			}
			long dStart = DB.getTimestamp(Tools.formatDate(cal.getTimeInMillis()), "0:00");
			ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(dStart, 1970, 3000)));
			ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(dStart, 1970, 3000)));
			ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(dEnd, 1970, 3000)));
			ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(dEnd, 1970, 3000)));
		}
		return(psIndex);
	}

	public static int addInputParamsSQL(SearchActionInput input, PreparedStatement ps, int psIndex) throws SQLException
	{
		int len = SearchTools.checkInputParams.length;
		String dateText = getParamAttributeUnsafe("dateText", input, null);
		boolean checkPublishDates = dateText == null || "custom".equals(dateText);

		for (int i=0; i<len; i++)
		{
			String param = getParamAttributeUnsafe(SearchTools.checkInputParams[i], input, null);
			if (Tools.isNotEmpty(param))
			{
				Logger.debug(SearchAction.class, "addPram "+psIndex+" "+ SearchTools.checkInputParams[i]+"="+param);
				if (SearchTools.checkInputParams[i].equals("publish_start"))
				{
					if (checkPublishDates)
					{
						long d = DB.getTimestamp(param, "0:00");
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
					}
				}
				else if (SearchTools.checkInputParams[i].equals("publish_start_lt"))
				{
					if (checkPublishDates)
					{
						long d = DB.getTimestamp(param);
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
					}
				}
				else if (SearchTools.checkInputParams[i].equals("publish_end"))
				{
					if (checkPublishDates)
					{
						long d = DB.getTimestamp(param, "23:59");
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
					}
				}
				else if (SearchTools.checkInputParams[i].equals("publish_end_gt"))
				{
					if (checkPublishDates)
					{
						long d = DB.getTimestamp(param);
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
						ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(d, 1970, 3000)));
					}
				}
				else if (SearchTools.checkInputParams[i].equals("temp_id"))
				{
					ps.setInt(psIndex++, Tools.getIntValue(param, 0));
				}
				else if (SearchTools.checkInputParams[i].equals("keyword"))
				{
					int perexGroupId = 0;
					DocDB docDB = DocDB.getInstance();
					PerexGroupBean pgb = docDB.getPerexGroup(-1, param);
					if (pgb != null) perexGroupId = pgb.getPerexGroupId();

					if(Constants.getBoolean("perexGroupUseJoin")) ps.setInt(psIndex++, perexGroupId);
					else ps.setString(psIndex++, "%,"+perexGroupId+",%");
				}
				else
				{
					StringTokenizer st = new StringTokenizer(param);
					if (st.countTokens()<2 || param.startsWith("\""))
					{
						ps.setString(psIndex++, "%"+Tools.replace(param, "\"", "")+"%");
					}
					else
					{
						while (st.hasMoreTokens())
						{
							ps.setString(psIndex++, "%"+st.nextToken()+"%");
						}
					}
				}
			}
		}
		if (checkPublishDates==false && Tools.isNotEmpty(dateText))
		{
			Calendar cal = Calendar.getInstance();
			long dEnd = DB.getTimestamp(Tools.formatDate(cal.getTimeInMillis()), "23:59");
			if ("lastWeek".equals(dateText))
			{
				cal.add(Calendar.WEEK_OF_YEAR, -1);
			}
			else if ("lastMonth".equals(dateText))
			{
				cal.add(Calendar.MONTH, -1);
			}
			long dStart = DB.getTimestamp(Tools.formatDate(cal.getTimeInMillis()), "0:00");
			ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(dStart, 1970, 3000)));
			ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(dStart, 1970, 3000)));
			ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(dEnd, 1970, 3000)));
			ps.setTimestamp(psIndex++, new Timestamp(DB.getTimestampNotBeforeAfterYear(dEnd, 1970, 3000)));
		}
		return psIndex;
	}

	/**
	 * Vrati true, ak niektory z dodatocnych parametrov nie je prazdny
	 * @param request
	 * @return
	 */
	public static boolean hasInputParams(HttpServletRequest request)
	{
		int len = SearchTools.checkInputParams.length;
		int i;
		String param;
		for (i=0; i<len; i++)
		{
			param = getParamAttribute(SearchTools.checkInputParams[i], request, null);
			if (Tools.isNotEmpty(param))
			{
				return(true);
			}
		}
		//textove vyjadrenie dateText = "today,lastWeek,lastMonth";
		if (Tools.isNotEmpty(request.getParameter("dateText")))
		{
			return(true);
		}
		return(false);
	}

	public static boolean hasInputParams(SearchActionInput input)
	{
		int len = SearchTools.checkInputParams.length;
		for (int i=0; i<len; i++)
		{
			String param = getParamAttribute(SearchTools.checkInputParams[i], input, null);
			if (Tools.isNotEmpty(param))
			{
				return true;
			}
		}
		return Tools.isNotEmpty(input.getParameter("dateText"));
	}

	/**
	 *
	 * @param name
	 * @param request
	 * @return
	 */
	public static String getInputParamMode(String name, HttpServletRequest request)
	{
		String value = getParamAttribute(name+"_mode", request, "AND");
		if ("OR".equalsIgnoreCase(value)) return "OR";
		return "AND";
	}

	public static String getInputParamMode(String name, SearchActionInput input)
	{
		String value = getParamAttribute(name+"_mode", input, "AND");
		if ("OR".equalsIgnoreCase(value)) return "OR";
		return "AND";
	}

	/**
	 * Vrati komplet vsetky zadane texty pouzite pre highlight
	 * @param request
	 * @return
	 */
	public static String getTextToFind(HttpServletRequest request)
	{
		String words = getParamAttribute("words", request, null);
		String text = getParamAttribute("text", request, null);

		String ttf = words;
		if (text != null)
		{
			if (ttf == null) ttf = text;
			else ttf += " " + text;
		}


		int len = SearchTools.checkInputParams.length;
		int i;
		String param;
		for (i=0; i<len; i++)
		{
			param = getParamAttribute(SearchTools.checkInputParams[i], request, null);
			if (Tools.isNotEmpty(param))
			{
				if (ttf == null) ttf = param;
				else ttf += " " + param; //NOSONAR
			}
		}

		if (ttf == null) return "";

		return ttf;
	}

	public static String getTextToFind(SearchActionInput input)
	{
		String words = getParamAttribute("words", input, null);
		String text = getParamAttribute("text", input, null);

		String ttf = words;
		if (text != null)
		{
			if (ttf == null) ttf = text;
			else ttf += " " + text;
		}

		int len = SearchTools.checkInputParams.length;
		for (int i=0; i<len; i++)
		{
			String param = getParamAttribute(SearchTools.checkInputParams[i], input, null);
			if (Tools.isNotEmpty(param))
			{
				if (ttf == null) ttf = param;
				else ttf += " " + param;
			}
		}

		if (ttf == null) return "";
		return ttf;
	}

	/**
	 * Vrati true ak je pre danu stranku lepsie pouzit rychle generovanie snippetu
	 * @param doc
	 * @param request
	 * @return
	 */
	public static boolean shouldDoQuickSnippet(SearchDetails doc, HttpServletRequest request)
	{
		int searchQuickSnippetSize = Constants.getInt("searchQuickSnippetSize");
		boolean shouldDoQuickSnippet = doc.getDataOriginal().length()>searchQuickSnippetSize || (request!=null && "true".equals(request.getParameter("fastSnippet")));

		return shouldDoQuickSnippet;
	}

	public static boolean shouldDoQuickSnippet(SearchDetails doc, boolean fastSnippet)
	{
		int searchQuickSnippetSize = Constants.getInt("searchQuickSnippetSize");
		return doc.getDataOriginal().length()>searchQuickSnippetSize || fastSnippet;
	}

	/**
	 * Pokusi sa vyhladat vyraz v HTML kode tak, ze HTML kod odstrani a zrusi vsetky medzery
	 * Pre hladanie CardPayTB v html kode CardPay<sup>TB</sup>
	 * @param code
	 * @param searchTerm
	 * @return
	 */
	public static boolean containsIgnoreHtml(String code, String searchTerm)
	{
		if (Tools.isEmpty(code) || Tools.isEmpty(searchTerm)) return false;

		code = code.toLowerCase();
		searchTerm = searchTerm.toLowerCase();

		code = EditorDB.removeHtmlTagsKeepLength(code);
		searchTerm = EditorDB.removeHtmlTagsKeepLength(searchTerm);

		code = code.replaceAll("\\s+", " ");
		searchTerm = searchTerm.replaceAll("\\s+", " ");

		return code.contains(searchTerm);
	}

	/**
	 *
	 * @return
	 * @deprecated - use SearchTools.getCheckInputParams
	 */
	@Deprecated
    public static String[] getCheckInputParams() {
		return SearchTools.getCheckInputParams();
    }

	/**
	 * Checks whether semantic search service and vector store are available.
	 * If unavailable, caller should fall back to standard database search.
	 */
	private static boolean isSemanticSearchAvailable() {
		try {
			SemanticSearchService semanticSearchService = Tools.getSpringBean("semanticSearchService", SemanticSearchService.class);
			return semanticSearchService != null && semanticSearchService.isAvailable();
		} catch (RuntimeException ex) {
			Logger.error(SearchAction.class, "Failed to resolve semanticSearchService, falling back to database search: " + ex.getMessage());
			return false;
		}
	}
}
