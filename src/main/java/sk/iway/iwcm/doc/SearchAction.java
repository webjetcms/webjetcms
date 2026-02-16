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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

	/**
	 * Identifikator 'score' pri pouziti oracletext hodnota 10 je cisto nahodna, ide o to aby v dotaze bolo pouzite to iste cislo :)
	 */
	private static int ORACLE_TEXT_CONTAINS_IDENTIFIER = 10; //NOSONAR

	public static String search(HttpServletRequest request, HttpServletResponse response)
	{
		PageParams pageParams = new PageParams(request);

		if (Constants.getBoolean("luceneAsDefaultSearch") || "true".equals(request.getParameter("useLucene")) || pageParams.getBooleanValue("useLucene", false))
		{
			//parameter useLucene=true je mozne pouzit pri porovnani standardneho a lucene vyhladavania
			return LuceneSearchAction.search(request);
		}

		Identity user = UsersDB.getCurrentUser(request);

		String forward = "success";
		String error = "error";
		boolean english = false;
		if (request.getParameter("lng") != null)
		{
			english = true;
			forward = "english";
		}
		String pForward = request.getParameter("forward");
		if (pForward != null && pForward.endsWith(".jsp"))
		{
			forward = "/templates/" + pForward;
		}
		String searchGroupsParam = getParamAttribute("rootGroup", request, Integer.toString(Constants.getInt("rootGroupId")));
		if (searchGroupsParam!=null) searchGroupsParam = searchGroupsParam.replace('+', ',');
		String[] groupIdParams = request.getParameterValues("groupId");
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

		boolean searchDetaultInTitle = Constants.getBoolean("searchDetaultInTitle") || "true".equals(request.getParameter("searchDetaultInTitle"));

		boolean searchAlsoProtectedPages = pageParams.getBooleanValue("searchAlsoProtectedPages", false);

		String searchFileNameQuery = null;

		StringTokenizer st = new StringTokenizer(searchGroupsParam, ",+; ");
		GroupsDB groupsDB = GroupsDB.getInstance();
		//tu si poznacim zadane root groups a tie nebudem neskor povazovat za internal folders
		Map<Integer, Integer> rootGroupIdsTable = new Hashtable<>();
		List<DocDetails> docsInGroups = null; //vsetky stranky v zadanych adresaroch
		List<GroupDetails> searchGroupsArray = null;
		DebugTimer dtt = new DebugTimer("SearchAction DUPLICITY");
		//ak je TRUE, tak chcem kontrolovat duplicitu pre multikategorie
		boolean checkDuplicity = pageParams.getBooleanValue("checkDuplicity", false);
		DocDB docDB = DocDB.getInstance();
		while (st.hasMoreTokens())
		{
			try
			{
				//schvalne je tu Integer.parseInt aby to pripadne padlo na exception
				int searchRootGroupId = Integer.parseInt(st.nextToken());

				rootGroupIdsTable.put(Integer.valueOf(searchRootGroupId), Integer.valueOf(1));

				//v stlpci file_name mame ulozenu cestu k adresaru, nieco ako /Slovensky/InterWay/Produkty/Nejaky Produkt/
				//nad tym limitujeme adresare (interne adresare to maju v databaze prazdne)

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
							//ziskam zoznam docId, ktore su rovnake k danej stranke
							slavesMasterForDoc = null;

							if(slavesMasterForDoc == null) slavesMasterForDoc = new ArrayList<>();

							Integer masterId = docDB.getSlavesMasterMappings().get(docId);
							if(masterId != null)
							{
								//ak sa master nachadza v zozname stranok, tak ponecham master miesto slave a vymazem vsetky slave stranky danej master stranky
								//v opacnom pripade vymazem vsetky ostatne slaves
								boolean containsMaster = docsInGroupsHm.get(masterId) != null && docsInGroupsHm.get(masterId) == Boolean.TRUE;
								Integer[] slaves = docDB.getMasterMappings().get(masterId);
								if(slaves != null)
								{
									//aby preskocilo pri iteracii master
									if(containsMaster) docsInGroupsHm.put(masterId, Boolean.FALSE);
									for(Integer slave : slaves)
										if(containsMaster || (!containsMaster && slave.intValue() != docId.intValue())) slavesMasterForDoc.add(slave);
								}
							}
							else
							{
								//ak docId je master, tak pridam do duplicitnych vsetky jeho slaves
								Integer[] slaves = docDB.getMasterMappings().get(docId);
								if(slaves != null) slavesMasterForDoc.addAll(Arrays.asList(slaves));
							}
							//odstranim ich zo zonamu stranok
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

		//Logger.println(this,"searchGroups=" + searchGroups);
		int perPage = 10;
		try
		{
			if (getParamAttribute("perpage", request, "10") != null)
			{
				perPage = Integer.parseInt(getParamAttribute("perpage", request, "10"));
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		String rq = request.getParameter("index");
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
		// **********************************
		//String words = my_form.getWords();
		String words = request.getParameter("words");
		if (Tools.isEmpty(words))
		{
			words = (String)request.getAttribute("words");
		   if (words == null) words = "";
		}
		String text = request.getParameter("text");
		if (Tools.isNotEmpty(text))
		{
			words = text;
		}
      if ("tatrabanka".equals(Constants.getInstallName()))
      {
         //fix na TB, ale inak rozumne som to nevedel spravit
         //vo fulltexte to mame ako TatraPay TB cize s medzerou kvoli sup
         words = Tools.replace(words, "TB", " TB");
      }

		//aby sme vedeli vnutit slovo pre vyhladavanie
		if (Tools.isNotEmpty((String)request.getAttribute("forceWords")))
		{
			words = (String)request.getAttribute("forceWords");
		}

		//Logger.println(this,"Search words1="+words);

		//Logger.println(this,"Search words ASC="+words);
		//Logger.println(this,"Perpage=" + perPage + ", words=" + words);
		String pom;
		int i;
		words = words.replace('\'', ' ');
		//words = words.replace('\"', ' '); - zakomentovane aby sa dal hladat vyraz
		words = words.replace(',', ' ');
		words = words.replace('.', ' ');
		words = words.replace(';', ' ');

		//nebezpecne znaky odpaz
		if (Tools.isNotEmpty(Constants.getString("searchActionOmitCharacters")))
		{
			words = words.replaceAll("["+Constants.getString("searchActionOmitCharacters")+"]", "");
		}

		//	otestuj, ci su zadane fields parametre
		boolean hasInputParams = hasInputParams(request);

		if ("***".equals(request.getAttribute("forceWords")))
		{
			words = "";
			hasInputParams = true;
		}

		request.removeAttribute("forceWords");

		//toto je zadany text na vyhladavanie bez nebezpecnych znakov
		String wordsAscii = DB.internationalToEnglish(words);
		// odstranenie jedno - dvoj znakovych slov
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
		//Logger.println(this,"Perpage=" + perPage + ", words=" + words);
		long wait;

		Logger.debug(null, "Search in Title: " +index);

		if (words.length() == 0 && hasInputParams==false)
		{
			//nemame co vyhladavat

			//session.setAttribute("words", "");
			request.setAttribute("aList", new ArrayList<SearchDetails>());
			//session.setAttribute("offset", Integer.valueOf(0));
			//session.setAttribute("length", Integer.valueOf(perPage));
			request.setAttribute("wrong", "true");
			request.setAttribute("emptyrequest", "true");
			return (forward);
		}

		//ochrana proti DOS utoku
		if (request.getAttribute("disableSearchSpamProtection")==null && request.getAttribute("forceWords")==null && rq == null)	//len ak ide o samotne vyhladavanie, nie o zobrazenie dalsej stranky vyhladavania
		{
			if ((wait=SpamProtection.getWaitTimeout("search", request)) != 0)
			{
				//prekrocil sa hodinovy limit
				request.setAttribute("wrong", "true");
				request.setAttribute("crossHourlyLimit", "true");
				request.setAttribute("wait", (wait/60/1000));
				return (forward);
			}

			if (!SpamProtection.canPost("search", "", request))
			{
				//prekrocil sa cas medzi dvoma prehladavaniami
				request.setAttribute("wrong", "true");
				request.setAttribute("crossTimeout", "true");
				return (forward);
			}
		}

		String sesWord;
		sesWord = (String) request.getAttribute("words");
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
				//	teraz hladame zaroven aj v title
				if (searchDetaultInTitle && request.getParameter("words")!=null) sql.append(" OR title LIKE ? ");
				sql.append(") ");

				sqlTotalResults = "SELECT count(d.doc_id) as totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE (CONTAINS(data_asc, ?) ";
				//	teraz hladame zaroven aj v title
				if (searchDetaultInTitle && request.getParameter("words")!=null) sqlTotalResults += " OR title LIKE ? ";
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
						//ked je prazdne oracleKeywords, tak to musi ist takot, inak hlasi: text query parser syntax error on line 1, column 1
						sql.append("SELECT * FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE");
						sqlTotalResults = "SELECT count(d.doc_id) as totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE";
					}
						//	teraz hladame zaroven aj v title
				}
				else
				{
					//ak mame viac slov pre LIKE command nahradime medzeru za percento pre hladanie viac slov
					String wordsForLike = Tools.replace(words, " ", "%");
					String wordsAsciiForLike = Tools.replace(wordsAscii, " ", "%");

					sql.append("SELECT ").append(D_DOCUMENT_FIELDS).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE (data_asc LIKE '%").append(DB.removeSlashes(wordsAsciiForLike.toLowerCase())).append("%' ");
					//	teraz hladame zaroven aj v title
					if (searchDetaultInTitle && request.getParameter("words")!=null) sql.append(" OR title LIKE '%").append(DB.removeSlashes(wordsForLike)).append("%' ");
					sql.append(") ");

					sqlTotalResults = "SELECT count(d.doc_id) as totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE (data_asc LIKE '%" + DB.removeSlashes(wordsAsciiForLike.toLowerCase()) + "%' ";
					//	teraz hladame zaroven aj v title
					if (searchDetaultInTitle && request.getParameter("words")!=null) sqlTotalResults += " OR title LIKE '%" + DB.removeSlashes(wordsForLike) + "%' ";
					sqlTotalResults += ") ";
				}

				//Logger.println(SearchAction.class,"search ORA sql:\n"+sql);
				//Logger.println(SearchAction.class,"search ORA sqlTotalResults:\n"+sqlTotalResults);
			}
			else if (Constants.DB_TYPE == Constants.DB_PGSQL)
			{
				//PostgreSQL
				sql.append("SELECT ").append((perexGroupUseJoin ? "DISTINCT " : "")+D_DOCUMENT_FIELDS).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE ( to_tsvector(data_asc) @@ to_tsquery(?) ");

				if (searchDetaultInTitle)
				{
					sql.append(" OR data_asc ILIKE ? ");
					//	teraz hladame zaroven aj v title
					if (request.getParameter("words")!=null) sql.append(" OR title ILIKE ? ");
				}
				sql.append(") ");

				sqlTotalResults = "SELECT count("+(perexGroupUseJoin ? "DISTINCT " : "")+"d.doc_id) AS totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE ( to_tsvector(data_asc) @@ to_tsquery(?) ";

				if (searchDetaultInTitle)
				{
					sqlTotalResults += " OR data_asc ILIKE ? ";
					//	teraz hladame zaroven aj v title
					if (request.getParameter("words")!=null) sqlTotalResults += " OR title ILIKE ? ";
				}
				sqlTotalResults += ") ";

				if (words.length()==0)
				{
					sql.delete(0, sql.length());
					sql.append("SELECT ").append((perexGroupUseJoin ? "DISTINCT " : "")+D_DOCUMENT_FIELDS).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE title IS NOT NULL ");
					sqlTotalResults = "SELECT count("+(perexGroupUseJoin ? "DISTINCT " : "")+"d.doc_id) AS totalr " + " FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE title IS NOT NULL ";
				}
			}
			else
			{
				//MySQL
				sql.append("SELECT ").append((perexGroupUseJoin ? "DISTINCT " : "")+D_DOCUMENT_FIELDS).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE ( MATCH(title, data_asc) AGAINST (? IN BOOLEAN MODE) ");

				if (searchDetaultInTitle)
				{
					sql.append(" OR data_asc LIKE ? ");
					//	teraz hladame zaroven aj v title
					if (request.getParameter("words")!=null) sql.append(" OR title LIKE ? ");
				}
				sql.append(") ");

				sqlTotalResults = "SELECT count("+(perexGroupUseJoin ? "DISTINCT " : "")+"d.doc_id) AS totalr FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE ( MATCH(title, data_asc) AGAINST (? IN BOOLEAN MODE) ";

				if (searchDetaultInTitle)
				{
					sqlTotalResults += " OR data_asc LIKE ? ";
					//	teraz hladame zaroven aj v title
					if (request.getParameter("words")!=null) sqlTotalResults += " OR title LIKE ? ";
				}
				sqlTotalResults += ") ";

				if (words.length()==0)
				{
					sql.delete(0, sql.length());
					sql.append("SELECT ").append((perexGroupUseJoin ? "DISTINCT " : "")+D_DOCUMENT_FIELDS).append(" FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE title IS NOT NULL ");
					sqlTotalResults = "SELECT count("+(perexGroupUseJoin ? "DISTINCT " : "")+"d.doc_id) AS totalr " + " FROM documents d"+(perexGroupUseJoin ? " LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id" : "")+" WHERE title IS NOT NULL ";
				}
			}
			//Logger.println(this,"en groups="+en_groups);
			if (Tools.isNotEmpty(searchFileNameQuery))
			{
				//v stlpce file_name mame ulozenu cestu k adresaru, nieco ako /Slovensky/InterWay/Produkty/Nejaky Produkt/
				//nad tym limitujeme adresare
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
					//skupiny pre web stranky
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

			//dodatocne parametre
			sql.append(addInputParamsSQL(request));
			sqlTotalResults += addInputParamsSQL(request);

			sql.append(" AND searchable="+DB.getBooleanSql(true)+" AND available="+DB.getBooleanSql(true)+" AND (external_link IS NULL OR external_link NOT LIKE '/files/protected%') ");

			if (Constants.getBoolean("multiDomainEnabled")) {
				//we must filter by domain using root_group_l1
				String domainName = DocDB.getDomain(request);
				if (Tools.isNotEmpty(domainName)) {
					List<Integer> rootGroupIds = new ArrayList<>();

					List<GroupDetails> rootGroups = GroupsDB.getRootGroups();
					for (GroupDetails rootGroup : rootGroups) {
						if (domainName.equals(rootGroup.getDomainName())) {
							//add ID to list
							rootGroupIds.add(rootGroup.getGroupId());
						}
					}

					//add ids into SQL query as root_group_l1 IN (...)
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

			String searchWhereSql = (String)request.getAttribute("searchWhereSql");
			if (Tools.isNotEmpty(searchWhereSql))
			{
				sql.append(' ').append(searchWhereSql);
				sqlTotalResults += " "+searchWhereSql;
				request.removeAttribute("searchWhereSql");
			}

			String afterWhere =  sql.toString().indexOf("WHERE") != -1 ? sql.toString().substring(sql.toString().indexOf("WHERE")) : "";
			boolean notFoundPublishStartEnd = afterWhere.indexOf("publish_start") == -1 && afterWhere.indexOf("publish_end") == -1;
			request.setAttribute("notFoundPublishStartEnd", String.valueOf(notFoundPublishStartEnd));
			if(notFoundPublishStartEnd && Constants.getBoolean("showOnlyActualPublishedDoc"))
			{
				String sqlTmp = " AND (publish_start IS NULL OR publish_start <= ?) AND (publish_end IS NULL OR publish_end >= ?) ";
				sql.append(sqlTmp);
				sqlTotalResults += sqlTmp;
			}

			//ak mame nejake duplicity, vynecham z vyhladavania
			if(Tools.isNotEmpty(docIdsNotIn.toString()))
			{
				sql.append(" AND d.doc_id NOT IN ("+docIdsNotIn.substring(0, docIdsNotIn.length()-1)+") ");
				sqlTotalResults += " AND d.doc_id NOT IN ("+docIdsNotIn.substring(0, docIdsNotIn.length()-1)+") ";
			}

			String order_var = "ASC";
			String order = getParamAttribute("order", request, "asc");
			String orderType = getParamAttribute("orderType", request, "sort_priority");
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
					//sortni to podla score
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

			//dalsie order by
			for (i=2; i<=5; i++)
			{
				orderType = getParamAttribute("orderType"+i, request, null);
				if (Tools.isNotEmpty(orderType))
				{
					order_var = "ASC";
					order = getParamAttribute("order"+i, request, "asc");
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
							//sortni to podla score
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
				//vsetky normalne DB okrem MSSQL nieco taketo poznaju
				if (Constants.DB_TYPE == Constants.DB_PGSQL) sql.append(" OFFSET ").append(index).append(" LIMIT ").append((perPage * 3 + 1));
				else sql.append(" LIMIT ").append(index).append(", ").append((perPage * 3 + 1));

				sql.append(' ');
				//ps.setInt(3, index);
				//ps.setInt(4, perPage + 1);
			}
			sTok = new StringTokenizer(words);
			if(sql != null) //toto je kvoli Oracle verzii
				sql = new StringBuilder(Tools.replace(sql.toString(), "WHERE AND", "WHERE"));
			if(Tools.isNotEmpty(sqlTotalResults))
				sqlTotalResults = Tools.replace(sqlTotalResults, "WHERE AND", "WHERE");

			// text to find
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
				//pre oracle to mam rovno v query

				//musime este doplnit hodnoty pre parametre v requeste
				psIndex = addInputParamsSQL(request, ps, psIndex);
			}
			else if (Constants.DB_TYPE == Constants.DB_MYSQL)
			{
				//Logger.println(this,"nastavujem params: " + wordsMysql + " " + index + " " + perPage);
				if (words.length()>0)
				{
					ps.setString(psIndex++, wordsMysql);
					if (searchDetaultInTitle)
					{
						ps.setString(psIndex++, "%"+wordsAscii+"%");
						if (request.getParameter("words")!=null) ps.setString(psIndex++, "%"+words+"%");
					}
				}
				//jeeff: toto som presunul rovno do SQL query, novemu MySQL driveru
				// to robilo problem (mozno len bug)
				//ps.setInt(3, index);
				//ps.setInt(4, perPage + 1);

				psIndex = addInputParamsSQL(request, ps, psIndex);
			}
			else if (Constants.DB_TYPE == Constants.DB_PGSQL)
			{
				//Logger.println(this,"nastavujem params: " + wordsMysql + " " + index + " " + perPage);
				if (words.length()>0)
				{
					ps.setString(psIndex++, Tools.replace(DB.internationalToEnglish(words.trim()).toLowerCase(), " ", " & "));
					if (searchDetaultInTitle)
					{
						ps.setString(psIndex++, wordsAscii);
						if (request.getParameter("words")!=null) ps.setString(psIndex++, words);
					}
				}
				psIndex = addInputParamsSQL(request, ps, psIndex);
			}
			else
			{
				if (words.length()>0)
				{
					//MSSQL vetva
					StringBuilder mssqlContainsMatcher = toMssqlContainsSearch(wordsAscii);

					if(mssqlContainsMatcher.length() > 0)
					{
						ps.setString(psIndex++, mssqlContainsMatcher.toString());
						//	title
						if (searchDetaultInTitle && request.getParameter("words")!=null) ps.setString(psIndex++, "%"+wordsAscii+"%");
					}
					else
					{
						ps.setString(psIndex++, wordsAscii);
					}
				}
				psIndex = addInputParamsSQL(request, ps, psIndex);
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
				//movni sa
				if (index > 0)
				{
					//Logger.println(this,"RS move: "+index);
					for (i = 0; i < index; i++)
					{
						//rs.absolute(index);
						rs.next();
					}
				}
			}
			aList = new ArrayList<>();
			SearchDetails qDet;
			String link;

			String ttf = getTextToFind(request);
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

				//vymazeme mu data, ak by sa nieco pototo aby sa nezobrazila cela stranka
				qDet.setData("");

				GroupDetails group = groupsDB.getGroup(qDet.getGroupId());

				//nastav prava stranke (aby sa dalo detekovat vo vysledkoch a zobrazit napr. obrazok zamku)
				if (Tools.isEmpty(qDet.getPasswordProtected()) && group != null && Tools.isNotEmpty(group.getPasswordProtected()))
				{
					qDet.setPasswordProtected(group.getPasswordProtected());
				}

				//dokumenty v internal foldroch neprehladavame (okrem root groups)
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
					//group uz neexistuje...
					link = null;
				}
				try
				{
					if (qDet.getExternalLink().startsWith("/files/"))
					{
						//navbar pre subory generujem bez moznosti kliknutia na odkaz
						String[] fileLink = MultiDomainFilter.fixDomainPaths(qDet.getExternalLink(), request).split("\\/");
						link = "";
						for (int p=0; p<fileLink.length-1; p++)
						{
							String part = fileLink[p];
							if (Tools.isEmpty(link)) link = part;
							else link += " "+Constants.getString("navbarSeparator")+" "+part; //NOSONAR
						}
						//skontroluj ci subor skutocne existuje
						if (request.getAttribute("searchDontCheckFile")==null)
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
						//ak je aktivny file archiv nahrad meno suboru za virtual file name
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

				SearchSnippet searchSnippet = new SearchSnippet(qDet, ttf, request);
				String snippet = searchSnippet.getSnippet();

				if (Tools.isEmpty(snippet)) snippet = "&nbsp;";
				qDet.setData(snippet);
				dt.diff("after snippet, size="+qDet.getData().length());

				//Logger.println(this,"PRIDAVAM DO ZOZNAMU: " + qDet.getTitle());
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
			request.setAttribute("wrong", "true");
			request.setAttribute("notfound", "true");
			request.setAttribute("err_msg", "Chyba pri práci s databázou..." + e.getMessage());
			return (error);
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
				request.setAttribute("err_msg", "Chyba pri práci s databázou: " + e.getMessage());
			}
		}

		totalResults = getResultsCount(sqlTotalResults, request, words, wordsAscii, wordsMysql, aList.size(), hasAnotherPage);
		request.setAttribute("words", words);
		request.setAttribute("aList", aList);
		if (aList.isEmpty())
		{
			request.setAttribute("wrong", "true");
			request.setAttribute("notfound", "true");
			return (forward);
		}
		String paramsLink = "";
		StringBuilder paramsLinkBuilder = new StringBuilder();
		Enumeration<String> e = request.getParameterNames();
		String name;
		while (e.hasMoreElements())
		{
			name = e.nextElement();
			if ("index".equals(name) || "docid".equals(name)) continue;

			String[] values = request.getParameterValues(name);
		   for (int v=0; v<values.length; v++)
		   {
		   	paramsLinkBuilder.append("&amp;").append(Tools.URLEncode(name)).append('=').append(Tools.URLEncode(values[v]));
		   }
		}
		paramsLink = paramsLinkBuilder.toString();
		request.setAttribute("paramsLink", paramsLink);

		//vypocitaj celkove pocty
		request.setAttribute("fromIndex", Integer.toString( (index + 1)));
		int toIndex = index + aList.size();
		request.setAttribute("toIndex", Integer.toString(toIndex));
		//
		if(pocetVynechanychSuborov > 0 && totalResults >= pocetVynechanychSuborov)
        {
            totalResults = totalResults - pocetVynechanychSuborov;
        }
		Logger.debug(SearchAction.class, "totalResults="+totalResults);
		request.setAttribute("totalResults", Integer.toString(totalResults));

		//vytvor pages
		preparePages(request, perPage, index, totalResults, paramsLink,"search.do");

		if (totalResults > toIndex)
		{
			if (!english)
			{
				request.setAttribute("next", "<a href=\"search.do?index=" + (index + perPage) + paramsLink + "\"> Ďalej &gt;&gt;&gt; </a>");
			}
			else
			{
				request.setAttribute("next", "<a href=\"search.do?index=" + (index + perPage) + paramsLink + "\"> Next &gt;&gt;&gt; </a>");
			}

			request.setAttribute("nextHref", "search.do?index=" + (index + perPage) + paramsLink);
		}
		else
		{
			//request.setAttribute("next", NEXT);
		}
		if (index != 0)
		{
			if (!english)
			{
				request.setAttribute("prev", "<a href=\"search.do?index=" + (index - perPage) + paramsLink + "\"> &lt;&lt;&lt; Späť </a>");
			}
			else
			{
				request.setAttribute("prev", "<a href=\"search.do?index=" + (index - perPage) + paramsLink + "\"> &lt;&lt;&lt; Back </a>");
			}
			request.setAttribute("prevHref", "search.do?index=" + (index - perPage)	+ paramsLink);
		}
		else
		{
			//request.setAttribute("prev", PREV);
		}

		String searchTerm = request.getParameter("words");
		if (Tools.isEmpty(searchTerm)) searchTerm = request.getParameter("text");
		if (Tools.isNotEmpty(searchTerm))
		{
			StatDB.addStatSearchLocal(searchTerm, Tools.getIntValue(request.getParameter("docid"), -1), request);
		}

		return (forward);
	}

	protected static void preparePages(HttpServletRequest request, int perPage, int index, int totalResults, String paramsLink, String searchAction)
	{
		List<LabelValueDetails> pages = new LinkedList<>();
		request.removeAttribute("pages");
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
						lv_page = new LabelValueDetails(Integer.toString(p), "");//aktualna strana
					}
					else
					{
						lv_page = new LabelValueDetails(Integer.toString(p), searchAction+"?index=" + (start) + paramsLink);
					}
					pages.add(lv_page);
					p++;
					start = start + perPage;
				}
			}
			if (pages.size()>1) request.setAttribute("pages", pages);
		}
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
}