package sk.iway.iwcm.stat;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.util.ResponseUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.seo.SeoManager;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;

/**
 *  Description of the Class
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.19 $
 *@created      Streda, 2002, máj 22
 *@modified     $Date: 2004/03/24 16:01:05 $
 */
public class StatDB extends DB
{
	public static final String STAT_DB_KEY = "iwcm_stat_db";

	public static final String BROWSER_DETECTOR = "browser_detector";
	public static final String REMOTE_HOST = "remote_host";
	public static final String REMOTE_IP = "remote_ip";
	public static final String LOGON_TIME = "stat_logon_time";
	public static final String SESSION_GROUP_IDS_QUERY = "stat_session_groupids_query";
	public static final String SESSION_GROUP_ID = "stat_session_groupid";

	private static final Hashtable<String, String> searchEnginesTable = new Hashtable<>();

	static
	{
		searchEnginesTable.put("google.com", "q;utf-8");
		searchEnginesTable.put("google.cz", "q;utf-8");
		searchEnginesTable.put("google.sk", "q;utf-8");
		searchEnginesTable.put("google.co.uk", "q;utf-8");
		searchEnginesTable.put("google.at", "q;utf-8");
		searchEnginesTable.put("google.de", "q;utf-8");
		searchEnginesTable.put("google.pl", "q;utf-8");
		searchEnginesTable.put("seznam.cz", "q;utf-8");
		searchEnginesTable.put("centrum.cz", "q");
		searchEnginesTable.put("atlas.cz", "q");
		searchEnginesTable.put("webfast.cz", "q");
		searchEnginesTable.put("idnes.cz", "q");
		searchEnginesTable.put("redbox.cz", "qs");
		searchEnginesTable.put("quick.cz", "ftxt_query");
		searchEnginesTable.put("volny.cz", "search");
		searchEnginesTable.put("tiscali.cz", "query");
		searchEnginesTable.put("zoznam.sk", "s");
		searchEnginesTable.put("altavista.com", "q");
		searchEnginesTable.put("yahoo.com", "p;utf-8");
		searchEnginesTable.put("morfeo.cz", "q");
		searchEnginesTable.put("jyxo.cz", "s");
		searchEnginesTable.put("caramba.cz", "Text");
		searchEnginesTable.put("katedrala.cz", "KeyWords");
		searchEnginesTable.put("toplist.cz", "search");
		searchEnginesTable.put("zona.cz", "TERMS");
		searchEnginesTable.put("dmoz.org", "search");
		searchEnginesTable.put("msn.com", "q;utf-8");
		searchEnginesTable.put("zoohoo.cz", "q");
		searchEnginesTable.put("icq.com", "q;utf-8");
		searchEnginesTable.put("yo.cz", "q");
		searchEnginesTable.put("ask.com", "query");
		searchEnginesTable.put("aol.com", "query");
		searchEnginesTable.put("hotbot.com", "query");
		searchEnginesTable.put("lycos.com", "query");
		searchEnginesTable.put("overture.com", "Keywords");
		searchEnginesTable.put("mamma.com", "query");
		searchEnginesTable.put("azet.sk", "sq");
	}

	/**
	 * Hashtabulky pre koverziu povodnych nazvov na int hodnoty (pre browser typu MSIE 7.0->2)
	 */
	private Map<String, Integer> valueToIdTable;
	private Map<String, Integer> valueLCToIdTable;
	private Map<Integer, String> idToValueTable;


	public static StatDB getInstance()
	{
		return(getInstance(false));
	}

	/**
	 *  Gets the instance attribute of the StatDB class
	 */
	public static StatDB getInstance(boolean force_refresh)
	{
		//try to get it from server space
		if (force_refresh == false)
		{
			StatDB statDB = (StatDB)Constants.getServletContext().getAttribute(STAT_DB_KEY);
			if (statDB != null)
			{
				//Logger.println(this,"DocDB: getting from server space");
				return (statDB);
			}
		}
		synchronized (StatDB.class)
		{
			if (force_refresh)
			{
				return (new StatDB());
			}
			else
			{
				StatDB statDB = (StatDB)Constants.getServletContext().getAttribute(STAT_DB_KEY);
				if (statDB == null)
				{
					statDB = new StatDB();
				}
				return statDB;
			}
		}

	}


	/**
	 *  Constructor for the StatDB object
	 */
	private StatDB()
	{
		Logger.println(this,"StatDB: constructor ["+Constants.getInstallName()+"]");

		//nacitaj hash tabulky z DB
		reloadCacheTables();

		//save us to server space
		Constants.getServletContext().setAttribute(STAT_DB_KEY, this);
	}

	private void reloadCacheTables()
	{
		Map<String, Integer> valueToIdTableLocal = new Hashtable<>();
		Map<String, Integer> valueLCToIdTableLocal = new Hashtable<>();
		Map<Integer, String> idToValueTableLocal = new Hashtable<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			Logger.debug(StatDB.class, "reading table stat_keys");

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM stat_keys");
			rs = ps.executeQuery();
			while (rs.next())
			{
				String value = rs.getString("value");
				int id = rs.getInt("stat_keys_id");

				//pre istotu, aby sa nestala nejaka duplicita
				if (valueToIdTableLocal.get(value)==null)
				{
					valueToIdTableLocal.put(value, id);
					valueLCToIdTableLocal.put(value.toLowerCase(), id);
					idToValueTableLocal.put(id, value);
				}
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
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
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		valueToIdTable = valueToIdTableLocal;
		valueLCToIdTable = valueLCToIdTableLocal;
		idToValueTable = idToValueTableLocal;
	}

	public int getSetValue(String value)
	{
		value = DB.prepareString(value, 64);
		if (value == null) value = "";
		value = value.trim();

		if (Constants.DB_TYPE==Constants.DB_ORACLE && Tools.isEmpty(value)) value = "-";

		Integer id = valueToIdTable.get(value);
		if (id != null) return id.intValue();
		//ak nebolo najdene v primarnej hladaj bez ohladu na case
		id = valueLCToIdTable.get(value.toLowerCase());
		if (id != null) return id.intValue();

		//nebolo najdene, musime zapisat do DB
		synchronized (StatDB.class)
		{
			//double check
			id = valueToIdTable.get(value);
			if (id != null) return id.intValue();
			id = valueLCToIdTable.get(value.toLowerCase());
			if (id != null) return id.intValue();

			//skus najst v databaze aktualnu hodnotu, mozno ju zapisal medzi tym iny cluster node
			int statKeysId = (new SimpleQuery()).forInt("SELECT stat_keys_id FROM stat_keys WHERE value=?", value);
			if (statKeysId < 1)
			{
				statKeysId = PkeyGenerator.getNextValue("stat_keys");

				Connection db_conn = null;
				PreparedStatement ps = null;
				try
				{
					Logger.debug(StatDB.class, "setting stat_keys new value: id="+statKeysId+" value="+value);

					db_conn = DBPool.getConnection();
					ps = db_conn.prepareStatement("INSERT INTO stat_keys (stat_keys_id, value) VALUES (?, ?)");

					ps.setInt(1, statKeysId);
					ps.setString(2, value);
					ps.execute();
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

			//reloadni data
			reloadCacheTables();

			//pouzijeme radsej hodnotu z cache, mohla nastat Duplicate Entry exception a tym padom je hodnota statKeysId zla
			id = valueToIdTable.get(value);
			if (id != null) return id.intValue();
			//ak nebolo najdene v primarnej hladaj bez ohladu na case
			id = valueLCToIdTable.get(value.toLowerCase());
			if (id != null) return id.intValue();

			return statKeysId;
		}
	}

	public String getValue(int id)
	{
		String value = idToValueTable.get(id);
		if (value == null) return "???";

		if (Constants.DB_TYPE==Constants.DB_ORACLE && "-".equals(value)) value = "";

		return value;
	}

	/**
	 * Vrati stat_key (cache) ID pre zadany vyraz
	 */
	public static int getStatKeyId(String value)
	{
		StatDB statDB = StatDB.getInstance();
		return statDB.getSetValue(value);
	}

	/**
	 * Vrati stat_key (cache) value pre zadane id
	 */
	public static String getStatKeyValue(int id)
	{
		StatDB statDB = StatDB.getInstance();
		return ResponseUtils.filter(statDB.getValue(id));
	}

	/**
	 *  Gets the yearTimeSQL attribute of the StatDB object
	 */
	public static String getYearTimeSQL(java.util.Date from, java.util.Date to, boolean where)
	{
		String ret = "";
		if (from == null || to == null)
		{
			return (ret);
		}
		int s_year;
		int e_year;
		int s_week;
		int e_week;
		GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		cal.setTime(from);
		s_year = cal.get(Calendar.YEAR);
		s_week = cal.get(Calendar.WEEK_OF_YEAR);
		if (cal.get(Calendar.MONTH)==Calendar.JANUARY && s_week>50) s_week = 1;
		cal.setTime(to);
		e_year = cal.get(Calendar.YEAR);
		e_week = cal.get(Calendar.WEEK_OF_YEAR);

		//SimpleDateFormat sdf = new SimpleDateFormat(Constants.getDATE_TIME_FORMAT("iwcm"));

		if (s_year != e_year)
		{
			//toto je specialny pripad, musime to robit takto...
			cal.setTime(from);
			ret = " ( ";
			StringBuilder retBuf = new StringBuilder(ret);
			while (cal.getTime().getTime() <= to.getTime())
			{
				if (retBuf.length() > 5)
				{
					//uz sme nieco pridali
					retBuf.append(" OR ");
				}

				s_year = cal.get(Calendar.YEAR);
				s_week = cal.get(Calendar.WEEK_OF_YEAR);

				if (cal.get(Calendar.MONTH)==Calendar.DECEMBER)
				{
					//ak nam hlasi v decembri 1. tyzden, hlasi to uz tyzden z nasledovneho roka
					if (s_week == 1)
					{
						s_year = s_year+1;
					}
				}
				if (cal.get(Calendar.MONTH)==Calendar.JANUARY)
				{
					//ak nam hlasi v janurari vysoky tyzden, hlasi to este tyzden z minuleho roka
					if (s_week > 50)
					{
						s_year = s_year-1;
					}
				}


				retBuf.append(" (year=").append(s_year).append(" AND week=").append(s_week).append(" ) ");

				//Logger.println(this,sdf.format(cal.getTime()) + " (year="+s_year+" AND week="+s_week+" ) ");

				//zvys hodnotu
				cal.add(Calendar.DAY_OF_YEAR, 7);
				//Logger.println(this,cal.toString());

			}
			ret = retBuf.toString();
			ret += " ) ";
		}
		else
		{
			ret = " year>=" + s_year + " AND week>=" + s_week + " AND year<=" + e_year + " AND week<=" + e_week;
		}
		if (where)
		{
			ret = " WHERE" + ret;
		}
		else
		{
			ret = " AND" + ret;
		}
		return (ret);
	}

	/**
	 * Spravi obmedzenie na documents.groups tak, aby boli len v podadresaroch rootGroupId
	 */
	public static String getRootGroupWhere(String column, int rootGroupId)
	{
		if (InitServlet.isTypeCloud())
		{
            if (rootGroupId<1)
			{
                rootGroupId = CloudToolsForCore.getDomainId();
            } else {
                GroupDetails group = GroupsDB.getInstance().getGroup(rootGroupId);
                if (group==null || group.getDomainName().equals(CloudToolsForCore.getDomainName())==false)
				{
                    rootGroupId = CloudToolsForCore.getDomainId();
                }
            }
        }

		if (rootGroupId < 1)
		{
			return("");
		}
		//najdi child grupy
		GroupsDB groupsDB = GroupsDB.getInstance();
		List<GroupDetails> searchGroupsArray = groupsDB.getGroupsTree(rootGroupId, true, true);

		return getRootGroupWhereHelp(column, searchGroupsArray);
	}

	/**
	 * Spravi obmedzenie na documents.groups tak, aby boli len v podadresaroch groupsId
	 */
	public static String getRootGroupWhere(String column, String groupIds)
	{
		if (Tools.isEmpty(groupIds) || "-1".equals(groupIds))
		{
			return("");
		}

		//najdi child grupy
		int[] groupsIdArray = Tools.getTokensInt(groupIds, ",");
		GroupsDB groupsDB = GroupsDB.getInstance();
		List<GroupDetails> searchGroupsArray = new ArrayList<>();
		for(int groupId: groupsIdArray)
		{
			searchGroupsArray.addAll(groupsDB.getGroupsTree(groupId, true, true));
		}

		return getRootGroupWhereHelp(column, searchGroupsArray);
	}

	/**
	 * Pomocna metoda pre metodu getRootGroupWhere
	 * @param column
	 * @param searchGroupsArray
	 * @return
	 */
	private static String getRootGroupWhereHelp(String column, List<GroupDetails> searchGroupsArray)
	{
		String ret = "";
		try
		{
			StringBuilder searchGroups = null;
			for (GroupDetails group : searchGroupsArray)
			{
				if (group != null)
				{
					if (searchGroups == null)
					{
						searchGroups = new StringBuilder(Integer.toString(group.getGroupId()));
					}
					else
					{
						searchGroups.append(",").append(group.getGroupId());
					}
				}
			}
			if (searchGroups != null)
			{
				ret = " AND " + column + " IN ("+searchGroups.toString()+") ";
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return(ret);
	}

	public static void addAdmin(HttpServletRequest request)
	{
		//zaloguje pracu admina
		Calendar cal = Calendar.getInstance();
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		cal.setTime(new java.util.Date());
		int year = cal.get(Calendar.YEAR);
		int week = cal.get(Calendar.WEEK_OF_YEAR);

		//ak nam hlasi v decembri 1. tyzden, hlasi to uz tyzden z nasledovneho roka
		if (cal.get(Calendar.MONTH)==Calendar.DECEMBER && week == 1)
		{
				year++;
		}
		//ak nam hlasi v janurari vysoky tyzden, hlasi to este tyzden z minuleho roka
		if (cal.get(Calendar.MONTH)==Calendar.JANUARY && week > 50)
		{
			year--;
		}

		//zalogujeme logon
		addStatUserLogon(year, week, request);
	}

	/**
	 *  prida zaznam do statistiky
	 */
	public static void add(HttpSession session, HttpServletRequest request, HttpServletResponse response, int docId)
	{
		//nezalogujeme pri odoslani emailu
		String dmail = request.getHeader("dmail");
		if (dmail != null || "none".equals(Constants.getString("statMode")))
		{
			return;
		}

		Calendar cal = Calendar.getInstance();
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		cal.setTime(new java.util.Date());
		int year = cal.get(Calendar.YEAR);
		int week = cal.get(Calendar.WEEK_OF_YEAR);

		//ak nam hlasi v decembri 1. tyzden, hlasi to uz tyzden z nasledovneho roka
		if (cal.get(Calendar.MONTH)==Calendar.DECEMBER && week == 1)
		{
				year++;
		}
		//ak nam hlasi v janurari vysoky tyzden, hlasi to este tyzden z minuleho roka
		if (cal.get(Calendar.MONTH)==Calendar.JANUARY && week > 50)
		{
			year--;
		}

		int lastDocId = -1;
		if (session.getAttribute("stat_last_time") == null)
		{
			session.setAttribute("serverName", DBPool.getDBName(request));
		}
		else
		{
			try
			{
				lastDocId = ((Integer) session.getAttribute("stat_last_doc_id")).intValue();
			}
			catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		}

		BrowserDetector browser = BrowserDetector.getInstance(request);

		int groupId = Tools.getIntValue((String)request.getAttribute("group_id"), 0);
		if (groupId == 0) {
			DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
			if (doc != null) {
				groupId = doc.getGroupId();
			}
		}

		//otestuj, ci nie sme zakazana IP adresa
		try
		{
			//vypnutie logovania pre zadane IP
			//vypnutie logovania ak je povodna a nova stranka rovnaka (refresh)
			if (browser.isStatIpAllowed()==false || docId == lastDocId)
			{
				//zalogujeme iba logon
				addStatUserLogon(year, week, request);
				if (browser.isStatIpAllowed())
				{
					addStatSearchEngine(request, docId, groupId);
				}
				return;
			}
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}

		//ok
		try
		{
			session.setAttribute("stat_last_time", Double.valueOf(cal.getTime().getTime()));
			session.setAttribute("stat_last_doc_id", Integer.valueOf(docId));

			// setni data pre SessionListener - toto nefunguje vo WebSphere
			SessionHolder holder = SessionHolder.getInstance();
			holder.setLastDocId(session.getId(), docId);

			if (browser.isStatUserAgentAllowed())
			{
				addStatFrom(session, request, response, browser, docId, groupId);
				addStatSearchEngine(request, docId, groupId);
			}
			else
			{
				request.setAttribute("StatDB.isSearchEngine", "true");
				session.setMaxInactiveInterval(20);
			}
			/**
			 * stat_views sa uklada pre vsetkych navstevnikov stranky, rozdeluju sa na zaklade browser_id
			 */
			addStatViews(session, request, response, docId, lastDocId, browser);
			addStatUserLogon(year, week, request);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	/**
	 * Zapise chybu do databazy
	 */
	public static void addError(String url, String queryString)
	{
		if ("none".equals(Constants.getString("statMode")))
		{
			return;
		}

		if (Tools.isEmpty(url)) return;
		if (queryString == null) queryString = "";

		Calendar cal = Calendar.getInstance();
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		int year = cal.get(Calendar.YEAR);
		int week = cal.get(Calendar.WEEK_OF_YEAR);
		Object[] params = new Object[]{DB.prepareString(url, 255), DB.prepareString(queryString, 255), year, week, CloudToolsForCore.getDomainId()};

		String update = "UPDATE stat_error"+StatNewDB.getTableSuffix("stat_error")+" SET count=count+1 WHERE url=? AND query_string=? AND year=? AND week=? AND domain_id=?";
		String insert = "INSERT INTO stat_error"+StatNewDB.getTableSuffix("stat_error")+" (url, query_string, year, week, domain_id, count) VALUES (?, ?, ?, ?, ?, 1)";

		StatWriteBuffer.addUpdateInsertPair(update, insert, "stat_error", params);
	}

	private static Map<String, String> languageDomainTable = null;
	/**
	 * Ziska tabulku mapovania jazyka na domenu
	 * @return
	 */
	public static Map<String, String> getLanguageDomainTable()
	{
		synchronized (StatDB.class)
		{
			if (languageDomainTable!=null) return languageDomainTable;

			languageDomainTable = new Hashtable<>();
			String[] data = Constants.getString("statLanguageDomain").split(",");
			for (int i=0; i<data.length; i++)
			{
				String[] pair = data[i].split("=");
				if (pair.length==2)
				{
					languageDomainTable.put(pair[0], pair[1]);
				}
			}
		}
		//kvoli performance toto nechcem - return languageDomainTable == null ? null : (Map<String, String>) languageDomainTable.clone();
		return languageDomainTable;
	}

	/**
	 * Nastavenie (vymazanie) tabulky mapovania jazykov a domen
	 * @param newLanguageDomainTable
	 */
	public static void setLanguageDomainTable(Map<String, String> newLanguageDomainTable)
	{
		//do NOT clone() this, callers may modify the content!
		StatDB.languageDomainTable = newLanguageDomainTable;
	}

	/**
	 * prida do statistiky prihlasenie usera
	 */
	private static void addStatUserLogon(int year, int week, HttpServletRequest request)
	{
		boolean cookiesEnabled = Tools.canSetCookie("statisticke", request.getCookies());

		HttpSession session = request.getSession();

		Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
		//nezalogujeme usera pri odoslani emailu
		if (user==null || user.isAuthorized()==false || request.getHeader("dmail") != null)
		{
			return;
		}

		Calendar cal = Calendar.getInstance();
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		//zaokruhlenie kvoli DB modelu a UPDATE WHERE logon_time=?
		cal.set(Calendar.MILLISECOND, 0);

		Long logonTime = (Long) session.getAttribute(LOGON_TIME);
		if (logonTime != null)
		{
			if ("none".equals(Constants.getString("statMode")) || cookiesEnabled==false)
			{
				return;
			}

			long now = cal.getTime().getTime();
			int viewMinutes = (int)((now - logonTime.longValue()) / 60000);
			String sql = "UPDATE stat_userlogon SET views=views+1, view_minutes=? WHERE logon_time=? AND user_id=?";
			StatWriteBuffer.add(sql, "stat_userlogon", viewMinutes, new Timestamp(logonTime.longValue()), user.getUserId());
		}
		else
		{
			logonTime = Long.valueOf(cal.getTime().getTime());

			String sql = "UPDATE users SET last_logon=? WHERE user_id=?";
			StatWriteBuffer.add(sql, "users", new Timestamp(logonTime.longValue()), user.getUserId());
			session.setAttribute(LOGON_TIME, logonTime);

			if ("none".equals(Constants.getString("statMode")))
			{
				return;
			}

			if (cookiesEnabled) {
				sql = "INSERT INTO stat_userlogon (year, week, user_id, views, logon_time, view_minutes, hostname) VALUES (?, ?, ?, ?, ?, ?, ?)";
				StatWriteBuffer.add(sql, "stat_userlogon", year, week, user.getUserId(), 1, new Timestamp(logonTime.longValue()), 0, Tools.getRemoteHost(request));
			}
		}
	}

	private static void addStatSearchEngine(HttpServletRequest request, int docId, int groupId)
	{
		if ("none".equals(Constants.getString("statMode")))
		{
			return;
		}

		String referer = request.getHeader("referer");

		if (referer == null || referer.length() <= 10)
			return;

		referer = referer.replace("http://", "").replace("https://", "");

		//rozparsuj referer a najdi domenu 2 urovne
		int i = referer.indexOf('/');
		String host = null;
		String query = null;
		if (i > 0)
		{
			host = referer.substring(0, i);

			if (host.equals(Tools.getServerName(request)))
			{
				//ak je to odkaz z nasej stranky, preskakujeme
				return;
			}

			//uprav to, aby bola len domena 2 urovne
			StringTokenizer st = new StringTokenizer(host, ".");
			int len = st.countTokens();
			if (len > 2)
			{
				int counter = 0;
				host = null;
				String tmp;
				while (st.hasMoreTokens())
				{
					tmp = st.nextToken();
					if (counter++ > len-3)
					{
						if (host==null)
						{
							host = tmp;
						}
						else
						{
							host = host + "." + tmp; //NOSONAR
						}
					}
				}
			}

			query = referer.substring(i+1);
			i = query.indexOf('?');
			if (i!=-1)
			{
				query = query.substring(i+1);
			}
		}
		//Logger.println(this,"host="+host);
		//Logger.println(this,"query="+query);

		if (host==null || query==null)
		{
			return;
		}

		String variable = searchEnginesTable.get(host);
		if (variable == null && host.indexOf("google.")!=-1)
		{
			variable = searchEnginesTable.get("google.com");
		}
		if (variable != null)
		{
			String encoding = "windows-1250";
			int index = variable.indexOf(';');
			if (index !=-1)
			{
				encoding = variable.substring(index+1);
				variable = variable.substring(0, index);
			}

			Logger.println(StatDB.class,"v="+variable+" e="+encoding);

			String searchTerm = null;
			//rozparsuj query a najdi tam variable
			StringTokenizer st = new StringTokenizer(query, "&");
			String token;
			while (st.hasMoreTokens())
			{
				token = st.nextToken();
				if (token.startsWith(variable+"="))
				{
					searchTerm = token.substring(token.indexOf('=')+1);
					break;
				}
			}

			if (Tools.isNotEmpty(searchTerm))
			{
				Logger.println(StatDB.class,"searchTerm="+searchTerm);
				//skus to zdekodovat
				try
				{
					searchTerm = URLDecoder.decode(searchTerm, encoding);
				}
				catch (UnsupportedEncodingException e) {sk.iway.iwcm.Logger.error(e);}

				String remoteHost = "0.0.0.0";
				if (Tools.canSetCookie("statisticke", request.getCookies())) remoteHost = Tools.getRemoteHost(request);

				//ok, mame. Zapiseme do databazy
				String sql = "INSERT INTO stat_searchengine"+StatNewDB.getTableSuffix("stat_searchengine")+" (search_date, server, query, doc_id, remote_host, group_id) VALUES (?, ?, ?, ?, ?, ?)";
				StatWriteBuffer.add(sql, "stat_searchengine", new Timestamp(getCurrentTime(request)), DB.prepareString(host, 16), DB.prepareString(searchTerm, 64), docId, remoteHost, groupId);

				request.setAttribute("statDB.searchEngine.host", host);
				request.setAttribute("statDB.searchEngine.searchTerm", searchTerm);
			}
		}
	}


	/**
	 * Zapise statistiku vyhladavania priamo na web stranke (do statistiky Vyhladavace),
	 * ako Nazov vyhladavaca bude uvedene WebJET
	 * @param searchTerm - vyhladavany vyraz
	 * @param docId - docId stranky s vysledkami vyhladavania
	 * @param request - request (ziska sa z neho remote IP)
	 */
	public static void addStatSearchLocal(String searchTerm, int docId, HttpServletRequest request)
	{
		if (Tools.isEmpty(searchTerm)) return;

		DocDB docDB = DocDB.getInstance();
		int groupId = 0;
		DocDetails doc = docDB.getBasicDocDetails(docId, false);
		if (doc != null) groupId = doc.getGroupId();

		Logger.debug(StatDB.class,"searchTerm="+searchTerm);

		String remoteHost = "0.0.0.0";
		if (Tools.canSetCookie("statisticke", request.getCookies())) remoteHost = Tools.getRemoteHost(request);

		String sql = "INSERT INTO stat_searchengine"+StatNewDB.getTableSuffix("stat_searchengine")+" (search_date, server, query, doc_id, remote_host, group_id) VALUES (?, ?, ?, ?, ?, ?)";
		StatWriteBuffer.add(sql, "stat_searchengine", new Timestamp(getCurrentTime(request)), DB.prepareString("WebJET", 16), DB.prepareString(searchTerm, 64), docId, remoteHost, groupId);
	}

	/**
	 * Gets map of <query,count> from specified timespan
	 * @param from time from
	 * @param to time to
	 * @param maxSize max number of returned entries
	 * @param addToQuery query string added to query (must start with AND, ...)
	 * @param groupIdsQuery groupIdsQuery (like AND group_id = 122,...)
	 * @return
	 */
	private static Map<String, Number> getSearchQueriesData(java.util.Date from, java.util.Date to, int maxSize, String addToQuery, String groupIdsQuery)
	{
		Logger.debug(StatDB.class, "Getting search queries data...");
		if (groupIdsQuery==null) groupIdsQuery = "";
		int i;

		Map<String, Number> map = new HashMap<>();

		String[] suffixes = StatNewDB.getTableSuffix("stat_searchengine", from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();
				String sql = "SELECT query, COUNT(query) AS total FROM stat_searchengine"+suffixes[s]+" WHERE doc_id >= 0 " + groupIdsQuery;
			   sql += " AND search_date >= ? AND search_date <= ? ";

				if(addToQuery!=null)	sql += addToQuery;

				sql += " GROUP BY query";
				sql += " ORDER BY total DESC";


				ps = StatNewDB.prepareStatement(db_conn, sql);

				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));

				rs = ps.executeQuery();

				i = 0;
				while (rs.next() && i++ < maxSize)
				{
					String key = DB.prepareString(getDbString(rs, "query"), 25);
					Number currentValue = map.get(key);

					if (currentValue == null) map.put(key, Integer.valueOf(rs.getInt("total")));
					else map.put(key, Integer.valueOf(rs.getInt("total")+currentValue.intValue()));
				}
				rs.close();
				ps.close();
				db_conn.close();
				db_conn = null;
				rs = null;
				ps = null;
			}
			catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
			finally{
				try{
					if (rs != null) rs.close();
					if (ps != null) ps.close();
					if (db_conn != null) db_conn.close();
				}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
			}
		}
		Logger.debug(StatDB.class, "Done Getting search queries data. Dataset size: " + map.size());
		return map;
	}

	/**
	 * Funkcia, ktora vrati ciselny identifikator pouzivatela, zapisuje sa do databazy na rozlisenie roznych pouzivatelov <br />
	 * Ak je to prihlaseny pouzivatel, vrati jeho (userId + konstanta {@link Constants}.getInt("loggedUserBrowserId")) <br />
	 * Ak je to vyhladavaci stroj, vrati sa jeho Id z tabulky seo_bots <br />
	 * Ak je to neprihlaseny pouzivatel, vrati sa mu (vygenerovane cislo + {@link Constants}.getInt("unloggedUserBrowserId")) <br />
	 * Ak je to pouzivatel, ktory ma uz vygenerovane browserId a zapisane v cookies, tak sa vrati to <br /><br />
	 * Ak nema zapisane v cookies a je to neprihlaseny pouzivatel, tak sa mu vygenerovane browserId zapise do cookies,
	 * takisto sa vygenerovane browserId zapise aj do session pod atributom "statFromBrowserId"
	 *
	 * @param request
	 * @param response
	 * @return cislo typu long identifikujuce pouzivatela
	 */
	public static long getBrowserId(HttpServletRequest request, HttpServletResponse response)
	{
		return getBrowserId(request, response, null);
	}

	/**
	 * Funkcia, ktora vrati ciselny identifikator pouzivatela, zapisuje sa do databazy na rozlisenie roznych pouzivatelov <br />
	 * Ak je to prihlaseny pouzivatel, vrati jeho (userId + konstanta {@link Constants}.getInt("loggedUserBrowserId")) <br />
	 * Ak je to vyhladavaci stroj, vrati sa jeho Id z tabulky seo_bots <br />
	 * Ak je to neprihlaseny pouzivatel, vrati sa mu (vygenerovane cislo + {@link Constants}.getInt("unloggedUserBrowserId")) <br />
	 * Ak je to pouzivatel, ktory ma uz vygenerovane browserId a zapisane v cookies, tak sa vrati to <br /><br />
	 * Ak nema zapisane v cookies a je to neprihlaseny pouzivatel, tak sa mu vygenerovane browserId zapise do cookies,
	 * takisto sa vygenerovane browserId zapise aj do session pod atributom "statFromBrowserId"
	 *
	 * @param request
	 * @param response
	 * @return cislo typu long identifikujuce pouzivatela
	 */
	public static long getBrowserId(HttpServletRequest request, HttpServletResponse response, BrowserDetector browser)
	{
		long browserId = 0;
		HttpSession session = request.getSession();

		/**
		 * ak je to vyhladavaci stroj, priradi mu jeho ID v tabulke seo_engines
		 */
		if (browser == null) browser = BrowserDetector.getInstance(request);
		if (!browser.isStatUserAgentAllowed())
		{
			browserId = SeoManager.getSearchEngineId(browser.getBrowserName() + " " + browser.getBrowserVersion());
			if (session != null) session.setAttribute("statFromBrowserId", Long.valueOf(browserId));
		}

		if (Tools.canSetCookie("statisticke", request.getCookies())==false)
		{
			Cookie[] cookies = request.getCookies();
			if (cookies != null)
			{
				for (Cookie c : cookies)
				{
					if ("statBrowserIdNew".equals(c.getName()))
					{
						//zmaz statBrowserIdNew cookie
						Cookie statBrowserIdNew = new Cookie("statBrowserIdNew", "0");
						statBrowserIdNew.setPath("/");
						statBrowserIdNew.setMaxAge(0);
						statBrowserIdNew.setHttpOnly(true);
						if (Tools.isSecure(request) && Tools.isNotEmpty(Constants.getString("strictTransportSecurity")))
						{
							statBrowserIdNew.setSecure(true);
						}
						response.addCookie(statBrowserIdNew);
					}
				}
			}

			return browserId;
		}

		if (session != null)
		{
			Identity user = (Identity) session.getAttribute(Constants.USER_KEY);

			/**
			 * ak je to prihlaseny pouzivatel, tak sa mu priradi browserId ako konstanta + jeho userId
			 */
			if (user != null && Constants.getBoolean("useUserIdAsBrowserId"))
			{
				browserId = Constants.getInt("loggedUserBrowserId") + user.getUserId() + 0L;
				session.setAttribute("statFromBrowserId", Long.valueOf(browserId));

				return(browserId);
			}

			/**
			 * Teraz sa pokusime vydolovat browserId z session
			 */
			if (session.getAttribute("statFromBrowserId") != null)
			{
				try
				{
					browserId = ( (Long)session.getAttribute("statFromBrowserId")).longValue();

					if (Tools.getCookieValue(request.getCookies(), "statBrowserIdNew", null)!=null)
					{
						//ak uz ma cookie setnutu, tak rovno poslime, ak nema, musime dole setnut aj cookie, deje sa ked akceptuje statisticke cookie neskor
						return (browserId);
					}
				}
				catch (RuntimeException ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
			}
		}

		/**
		 * Iba v pripade neregistrovaneho pouzivatela, sa vyskusa, ci nema nahodou ulozene browserId v cookies
		 */
		Cookie[] cookies = request.getCookies();
		if (cookies != null)
		{
			int cLen = cookies.length;
			String cValue = null;

			for (int k = 0; k < cLen; k++)
			{
				if ("statBrowserIdNew".equals(cookies[k].getName()))
				{
					cValue = Tools.URLDecode(cookies[k].getValue());
				}
				//Logger.println(this,"cookie Name: "+cookies[k].getName()+"  value: "+cookies[k].getValue());
			}
			if (Tools.isNotEmpty(cValue))
			{
				browserId = Tools.getIntValue(cValue, -1);
			}
		}

		/**
		 * Ak vyslo browserId mensie ako konstanta pre neregistrovanych pouzivatelov, vygenerujeme nove Id.
		 * Tato situacia by nemala nikdy nastat, lebo pre registrovanych sa browserId pocita ako konstanta + userId
		 * a takisto pre vyhladavacie stroje. Situacia moze nastat ak sa SEO modul nasadil az po urcitej chvili
		 * pouzivania WebJET-u a v cookies su ulozene stare hodnoty, kedze cookies expiruje az po dvoch mesiacoch.
		 */
		if (browserId < Constants.getInt("unloggedUserBrowserId"))
		{
			Logger.debug(StatDB.class, "Generating sbid: " + Tools.getRemoteIP(request)+" ua="+request.getHeader("User-Agent"));
			browserId = Constants.getInt("unloggedUserBrowserId") + PkeyGenerator.getNextValueAsLong("stat_browser_id"); // pre neregnuteho usera sa vygeneruje cislo > Constants.getInt("unloggedUserBrowserId")
		}

		if (response != null)
		{
			//	uloz do cookies
			Cookie statBrowserIdNew = new Cookie("statBrowserIdNew", Long.toString(browserId));
			statBrowserIdNew.setPath("/");
			statBrowserIdNew.setMaxAge(60 * 24 * 3600);
			statBrowserIdNew.setHttpOnly(true);
			Tools.addCookie(statBrowserIdNew, response,request);
		}

		if (session != null) session.setAttribute("statFromBrowserId", Long.valueOf(browserId));

		return(browserId);
	}

	public static long getSessionId(HttpServletRequest request)
	{
		long sessionId = 0;

		HttpSession session = request.getSession();
		if (session != null && session.getAttribute("statFromSessionId") != null)
		{
			try
			{
				sessionId = ( (Long)session.getAttribute("statFromSessionId")).longValue();
			}
			catch (RuntimeException ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		if (sessionId < 1)
		{
			Logger.debug(StatDB.class, "Generating ssid: "+Tools.getRemoteIP(request)+" ua="+request.getHeader("User-Agent"));
			sessionId = PkeyGenerator.getNextValueAsLong("stat_session_id");
		}

		if (session != null) session.setAttribute("statFromSessionId", Long.valueOf(sessionId));

		return(sessionId);
	}

	/**
	 *  Prida do DB zaznam o adrese z ktorej prisiel user na stranku, browserId a sessionId
	 */
	public static void addStatFrom(HttpSession session, HttpServletRequest request, HttpServletResponse response, BrowserDetector browser, int docId, int groupId)
	{

		long browserId = getBrowserId(request, response, browser);
		long sessionId = getSessionId(request);

		//Logger.println(this,"browserId= "+browserId+ "  sessionId= "+sessionId);

		String referer = request.getHeader("referer");
		//Logger.println(this,"referer="+referer);
		if (Tools.isEmpty(referer) || referer.length()<10)
		{
			return;
		}

		//if (referer.startsWith("http://"))
		if (referer.indexOf("//") > 2)
		{
			referer = referer.substring(referer.indexOf("//") + 2);
		}
		//Logger.println(this,"referer="+referer);
		int i = referer.indexOf('/');
		String host = null;
		String url = null;
		String statNoReferer = Constants.getString("statNoReferer");

		if (i > 0)
		{
			host = referer.substring(0, i);
			host = Tools.replace(host, "http://", "");
			host = Tools.replace(host, "https://", "");
			if (host.indexOf(":")!=-1) host = host.substring(0, host.indexOf(":"));

			url = referer.substring(i);
			//Logger.println(this,"host: "+host+"  url: "+url);

			//ak je to odkaz z nasej stranky, preskakujeme
			if (host.equals(Tools.getServerName(request))) return;

			//pridaj aj forwarded-for
			String forwardedFor = request.getHeader("x-forwarded-host");
			if (Tools.isNotEmpty(forwardedFor))
			{
				if (host.equals(forwardedFor)) return;
			}

			String[] statNoRefs = getTokens(statNoReferer, ",");
			if (statNoRefs.length > 0)
			{
				for(int ind=0; ind<statNoRefs.length; ind++)
				{
					if (host.equals(statNoRefs[ind]))
					{
						//ak je to odkaz z nasej stranky, preskakujeme
						return;
					}
				}
			}
		}

		if (Tools.isEmpty(host) || Tools.isEmpty(url))
		{
			return;
		}

		//ok, mame. Zapiseme do databazy
		String sql = "INSERT INTO stat_from"+StatNewDB.getTableSuffix("stat_from")+" (browser_id, session_id, referer_server_name, referer_url, from_time, doc_id, group_id) " +
		"VALUES (?, ?, ?, ?, ?, ?, ?)";
		StatWriteBuffer.add(sql, "stat_from", browserId, sessionId, DB.prepareString(host, 254), DB.prepareString(url, 254), new Timestamp(getCurrentTime(request)), docId, groupId);
	}

	/**
	 * Zapis statistiky do stat_views
	 */
	public static void addStatViews(HttpSession session, HttpServletRequest request, HttpServletResponse response, int docId, int lastDocId, BrowserDetector browser)
	{
		if (docId < 1 || "old".equals(Constants.getString("statMode")) || request==null)
		{
			return;
		}

		//GDPR nemozeme sledovat statistiku, kym nam to nepovoli navstevnik
		boolean mustAnonymize = false;
		if (Tools.canSetCookie("statisticke", request.getCookies())==false) mustAnonymize = true;

		if (Constants.getBoolean("statEnableDocCount"))
		{
			//aktualizuj celkovy pocet videni stranky
			String sql = "UPDATE documents SET views_total=views_total+1 WHERE doc_id=?";
			List<Object> params = new ArrayList<>();
			params.add(docId);
			StatWriteBuffer.add(sql, "documents", params.toArray());
		}

		long browserId = getBrowserId(request, response, browser);
		long sessionId = getSessionId(request);

		//it's search bot, save correct browserId
		if (browserId>0 && browserId < Constants.getInt("loggedUserBrowserId")) mustAnonymize = false;

		DocDB docDB = DocDB.getInstance();

		String tableName = "stat_views";
		Calendar cal = Calendar.getInstance();
		if (Constants.getBoolean("statEnableTablePartitioning")==true)
		{
			tableName = tableName + "_"+cal.get(Calendar.YEAR)+"_"+(cal.get(Calendar.MONTH)+1);
		}

		//ok, mame. Zapiseme do databazy
		String sql = "INSERT INTO "+tableName+" (browser_id, session_id, doc_id, last_doc_id, view_time, group_id, last_group_id, browser_ua_id, platform_id, subplatform_id, country) " +
		"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		List<Object> params = new ArrayList<>();
		params.add(browserId);
		params.add(sessionId);
		params.add(docId);
		params.add(lastDocId);
		params.add(new Timestamp(getCurrentTime(mustAnonymize)));
		params.add(docDB.getBasicDocDetails(docId, true).getGroupId());
		params.add(docDB.getBasicDocDetails(lastDocId, true).getGroupId());
		if (browser == null)
		{
			params.add(0);
			params.add(0);
			params.add(0);
			params.add("unkn");
		}
		else
		{
			params.add(browser.getBrowserUaId());
			params.add(browser.getPlatformId());
			params.add(browser.getSubplatformId());
			params.add(browser.getCountry());
		}
		StatWriteBuffer.add(sql, "stat_views", params.toArray());
	}

	/**
	 * Vrati pole typu String, s jednotlivymi polozkami v retazci, ak sa retazec neda rozdelit, vrati prazdne pole
	 * @param groups 	- retazec, ktory sa ma rozparsovat
	 * @param delimiter
	 * @return
	 */
	public static String[] getTokens(String groups, String delimiter)
	{
		String[] ret = new String[0];
		StringTokenizer st;
		int i = 0;
		try
		{
			if (Tools.isNotEmpty(groups))
			{
				st = new StringTokenizer(groups, delimiter);
				ret = new String[st.countTokens()];
				while (st.hasMoreTokens())
				{
					ret[i++] = st.nextToken().trim();
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return(ret);

	}

	/**
	 * Metoda vrati mapu usporiadanu podla hodnot (values) od najvacsieho po najmensie
	 */
	@SuppressWarnings("java:S2293")
	public static <K, V extends Number> Map<K, V> sortByValue(Map<K, V> map)
	{
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>()
		{
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2)
			{
				return (o2.getValue().intValue() - o1.getValue().intValue());
			}
		});
		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list)
		{
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * Metoda vrati mapu usporiadanu podla klucov (key) od A po Z
	 */
	@SuppressWarnings("java:S2293")
	public static <K extends Comparable<K>, V> Map<K, V> sortByKey(Map<K, V> map)
	{
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>()
		{
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2)
			{
				return (o2.getKey().compareTo(o1.getKey()));
			}
		});
		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list)
		{
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * Gets max x tags ( see {@link Tag} ) for opencloud from daysFrom ago till now
	 * @param maxTags max tags returned
	 * @param daysFrom number of days from now to past
	 * @param searchUrlBase base url for search (i.e.: "/showdoc.do?docid=36&words=")
	 * @return collection of max maxTags tags from daysFrom days till now
	 *
	 * @throws IllegalArgumentException when maxTags or daysFrom are negative or zero, or searchUrlBase is null
	 */
	public static Collection<Column> getSearchCloudTags(int maxTags, int daysFrom, String searchUrlBase) throws IllegalArgumentException
	{
		if(maxTags < 1)
		{
			throw new IllegalArgumentException("maxTags must be positive number");
		}
		if(daysFrom < 1)
		{
			throw new IllegalArgumentException("daysFrom must be positive number");
		}
		if(searchUrlBase == null)
		{
			throw new IllegalArgumentException("searchUrlBase must not be null");
		}

		Collection<Column> tags = Collections.emptyList();

		Calendar to = Calendar.getInstance();
		Calendar from = Calendar.getInstance();
		from.add(Calendar.DAY_OF_YEAR, -daysFrom);

		Map<String, Number> map = getSearchQueriesData(from.getTime(), to.getTime(), maxTags, "", "");

		if(map.size() > 0)
		{
			tags = new ArrayList<>(map.size());
			for(Map.Entry<String, Number> entry : map.entrySet())
			{
				Column col = new Column();
				col.setColumn1(entry.getKey());
				col.setColumn2(searchUrlBase+entry.getKey());
				col.setDoubleColumn1(entry.getValue().doubleValue());
				tags.add(col);
			}
		}

		return tags;
	}

	/**
	 * Ak nie je povolena statisticka cookie vrati cas zaokruhleny na 15 minutovy interval
	 * @param request
	 * @return
	 */
	private static long getCurrentTime(HttpServletRequest request) {
		boolean mustAnonymize = false;
		if (Tools.canSetCookie("statisticke", request.getCookies())==false) mustAnonymize = true;
		return getCurrentTime(mustAnonymize);
	}

	/**
	 * Ak je mustAnonymize=true vrati cas zaokruhleny na 15 minutovy interval
	 * @param mustAnonymize
	 * @return
	 */
	private static long getCurrentTime(boolean mustAnonymize) {
		Calendar calendar = Calendar.getInstance();

		if (mustAnonymize==false) return calendar.getTimeInMillis();

		int unroundedMinutes = calendar.get(Calendar.MINUTE);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		int mod = unroundedMinutes % 15;
		calendar.add(Calendar.MINUTE, mod < 8 ? -mod : (15-mod));

		return calendar.getTimeInMillis();
	}
}