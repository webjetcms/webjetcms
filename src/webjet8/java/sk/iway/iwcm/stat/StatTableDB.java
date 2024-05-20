package sk.iway.iwcm.stat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;

/**
 *
 *  StatTableDB.java
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.29 $
 *@created      Date: 10.12.2005 14:34:04
 */
public class StatTableDB {

	protected StatTableDB() {
		//utility class
	}

	public static List<Column> getCountry(int maxRows, java.util.Date from, java.util.Date to, String groupIdsQuery)
	{
		return getCountry( maxRows, from, to, groupIdsQuery, false);
	}

	/**
	 *  statistika pristupov podla krajin usporiadana podla views
	 *
	 *@param  max_size  Description of the Parameter
	 *@param  from      Description of the Parameter
	 *@param  to        Description of the Parameter
	 *@return           The country value
	 */
	public static List<Column> getCountry(int maxRows, java.util.Date from, java.util.Date to, String groupIdsQuery, boolean withoutBots)
	{
		if (groupIdsQuery == null)
		{
			groupIdsQuery = "";
		}

		Map<String, Number> map = new HashMap<>();

		String whitelistedQuery = "";
		if(withoutBots)
			whitelistedQuery = StatNewDB.getWhiteListedUAQuery();

		String[] suffixes = StatNewDB.getTableSuffix(null, from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				String sql = "SELECT DISTINCT country, count(country) as views FROM stat_views"+suffixes[s]+" WHERE view_time>=? AND view_time<? " + groupIdsQuery + whitelistedQuery + " GROUP BY country";
				db_conn = DBPool.getConnection();
				ps = StatNewDB.prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));
				rs = ps.executeQuery();
				//iteruj cez riadky
				while (rs.next())
				{
					String key = rs.getString("country");

					if (key==null || "unk".equals(key)) key = "unkn";

					Number currentValue = map.get(key);

					if (currentValue == null) map.put(key, Integer.valueOf(rs.getInt("views")));
					else map.put(key, Integer.valueOf(rs.getInt("views")+currentValue.intValue()));
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
				if (ex.getMessage().indexOf("Invalid")==-1)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
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
		}

		List<Column> ret = new ArrayList<>();

		map = StatDB.sortByValue(map);

		Iterator<Map.Entry<String, Number>> iter = map.entrySet().iterator();
		Map.Entry<String, Number> me;
		int i = 0;
		while (iter.hasNext() && i++<maxRows)
		{
			me = iter.next();
			String key = me.getKey();
			Number value = me.getValue();

			//Logger.debug(StatDB.class, "getCountry: key="+key+" value="+value);

			Column col = new Column();
			col.setColumn1(key);
			col.setIntColumn2(value.intValue());
			ret.add(col);
		}

		return (ret);
	}

	public static List<Column> getNamedCountries(int max_size, java.util.Date from, java.util.Date to, String groupIdsQuery, String language)
	{
		return getNamedCountries(max_size, from, to, groupIdsQuery, language, false);
	}

	/**
	 * The same as getCountry, except in country name terms.
	 * The countries are not returned as a top level domain codes, but rather as
	 * a localized country name. In case such a TLD is not recognized, or in case
	 * it is a generic TLD( .com, .net, .org, .info,...) the TLD is returned.
	 *
	 */

	public static List<Column> getNamedCountries(int max_size, java.util.Date from, java.util.Date to, String groupIdsQuery, String language, boolean withoutBots)
	{
		List<Column> ret = getCountry(max_size, from, to, groupIdsQuery, withoutBots);
		//skus najst mapovanie TLD na nazov krajiny...ak sa nepodari, ponechaj povodnu
		for (Column countryStatRow : ret)
		{
			if (!("stat.countries.tld."+(countryStatRow).getColumn1()).     equals(Prop.getInstance(language).getText( "stat.countries.tld."+(countryStatRow).getColumn1()) ))
				(countryStatRow).setColumn1( Prop.getInstance(language).getText("stat.countries.tld."+(countryStatRow).getColumn1()) );
		}

		return ret;
	}

	public static List<Column> getBrowser(int maxRows, java.util.Date from, java.util.Date to, String groupIdsQuery)
	{
		return getBrowser( maxRows, from, to, groupIdsQuery, false);
	}

	/**
	 * Vrati zoznam prehliadacov a platforiem usporiadany podla poctu videni
	 * @param maxRows
	 * @param from
	 * @param to
	 * @param groupIdsQuery
	 * @return
	 */
	public static List<Column> getBrowser(int maxRows, java.util.Date from, java.util.Date to, String groupIdsQuery, boolean withoutBots)
	{
		if (groupIdsQuery == null)
		{
			groupIdsQuery = "";
		}

		Map<String, Number> map = new HashMap<>();

		String whitelistedQuery = "";
		if(withoutBots)
			whitelistedQuery = StatNewDB.getWhiteListedUAQuery();

		String[] suffixes = StatNewDB.getTableSuffix(null, from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();
				String sql = "SELECT DISTINCT browser_ua_id, platform_id, count(browser_ua_id) as views FROM stat_views"+suffixes[s]+" WHERE view_time>=? AND view_time<? " + groupIdsQuery + whitelistedQuery + " GROUP BY browser_ua_id, platform_id";
				ps = db_conn.prepareStatement(sql);
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));
				rs = ps.executeQuery();
				//iteruj cez riadky
				while (rs.next())
				{
					String key = rs.getInt("browser_ua_id")+";"+rs.getInt("platform_id");
					Number currentValue = map.get(key);

					if (currentValue == null) map.put(key, Integer.valueOf(rs.getInt("views")));
					else map.put(key, Integer.valueOf(rs.getInt("views")+currentValue.intValue()));
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

		List<Column> ret = new ArrayList<>();

		map = StatDB.sortByValue(map);

		Iterator<Map.Entry<String, Number>> iter = map.entrySet().iterator();
		Map.Entry<String, Number> me;
		int i = 0;
		while (iter.hasNext() && i++<maxRows)
		{
			me = iter.next();
			String key = me.getKey();
			Number value = me.getValue();

			//Logger.debug(StatDB.class, "getBrowser: key="+key+" value="+value);

			Column col = new Column();
			String[] values = key.split(";");
			if (values.length==2)
			{
				col.setIntColumn1(Tools.getIntValue(values[0], -1));
				col.setColumn1(StatDB.getStatKeyValue(col.getIntColumn1()));

				col.setIntColumn2(Tools.getIntValue(values[1], -1));
				col.setColumn2(StatDB.getStatKeyValue(col.getIntColumn2()));

				col.setIntColumn3(value.intValue());
				ret.add(col);
			}
		}

		return (ret);
	}


	/**
	 * Zoznam prihlaseni jednotlivych pouzivatelov zgrupenych podla pouzivatela.
	 * Dalej obsahuje zosumovane minuty jeho prihlasenia, scitane pocty prihlaseni a datum posledneho loginu.
	 *
	 * @param max_size	Maximalny pocet zaznamov, ktore sa vratia
	 * @param from			Datum, od ktoreho sa filtruju prihlasenia
	 * @param to			Datum, do ktoreho sa filtruju prihlasenia
	 *
	 * @return				Zoznam jednotlivych prihlaseni zgrupenych podla pouzivatela, ak sa citanie z databazy podari. Inak vrati prazdny zoznam.
	 */

	public static List<Column> getUsrlogon(int max_size, java.util.Date from, java.util.Date to)
	{
		List<Column> ret = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;


		String sql = "SELECT DISTINCT u.user_id, u.title as u_title, u.first_name, u.last_name, u.company, u.city, u.last_logon, u.is_admin, " +
		 				 "sum(s.views) as views, sum(s.view_minutes) as view_minutes, count(s.views) as views_count " +
		 				 "FROM stat_userlogon s,  users u " +
		 				 "WHERE s.user_id = u.user_id AND s.logon_time >= ? AND s.logon_time <= ? ";

		if (InitServlet.isTypeCloud())
		{
			sql += CloudToolsForCore.getDomainIdSqlWhere(true, "u");
		}

		sql += "GROUP BY u.user_id, u.is_admin, u.title, u.first_name, u.last_name, u.company, u.city, u.last_logon ";
		sql+= "ORDER BY u.is_admin DESC, view_minutes DESC";

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);

			ps.setTimestamp(1, new Timestamp(from.getTime()));
			ps.setTimestamp(2, new Timestamp(to.getTime()));

			rs = ps.executeQuery();

			Column col;
			int count = 0;
			while (rs.next() && count < max_size)
			{
				col = new Column();

				col.setColumn1(Integer.toString(rs.getInt("user_id")));
				col.setColumn2(DB.getFullName(rs));
				col.setColumn3(DB.getDbString(rs, "company"));
				col.setColumn4(DB.getDbString(rs, "city"));
				col.setIntColumn5(rs.getInt("views_count"));			// pocet novych prihlaseni. To su take, pri ktorych sa vytvorila nova session
				col.setIntColumn6(rs.getInt("view_minutes"));		// pocet minut, ktore bol pouzivatel prihlaseny
				col.setIntColumn7(rs.getInt("views"));					// pocet vsetkych prihlaseni, nielen tych pri ktorych sa vytvorila nova session
				col.setDateColumn1(rs.getTimestamp("last_logon"));
				col.setColumn8(Integer.toString(rs.getInt("views")));
				col.setBooleanColumn1(rs.getBoolean("is_admin"));

				ret.add(col);
				count++;
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

		return (ret);
	}

	/**
	 * Vrati zoznam prihlaseni pre daneho pouzivatela a pre dane casove obdobie
	 *
	 * @param max_size	Maximalny pocet zaznamov, ktore sa vratia. Aj ked select z tabulky ich vrati viac, system nacita iba maximalne max_size.
	 * @param userId		Identifikator pouzivatela, ktoreho zaznamy prihlaseni chceme ziskat.
	 * @param from			Zaciatok casoveho useku, od ktoreho chceme zaznamy prihlaseni.
	 * @param to			Koniec casoveho useku, do ktoreho chceme zaznamy prihlaseni.
	 *
	 * @return				Zoznam prihlaseni daneho pouzivatela s polozkami - cas prihlasenia(DateColumn1), pocet minut(IntColumn2) a meno pocitaca, z ktoreho sa prihlasil(hostname)
	 */
	public static List<Column> getUsrlogonDetails(int max_size, int userId, java.util.Date from, java.util.Date to)
	{
		List<Column> ret = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		String sql = "SELECT * FROM stat_userlogon WHERE user_id=? AND logon_time >= ? AND logon_time <= ? ORDER BY logon_time DESC";

		try
		{
			db_conn = DBPool.getConnection();

			ps = db_conn.prepareStatement(sql);
			ps.setInt(1, userId);
			ps.setTimestamp(2, new Timestamp(from.getTime()));
			ps.setTimestamp(3, new Timestamp(to.getTime()));

			rs = ps.executeQuery();

			Column col;
			int count = 0;
			while (rs.next() && count < max_size)
			{
				col = new Column();
				col.setDateColumn1(rs.getTimestamp("logon_time"));
				col.setIntColumn2(rs.getInt("view_minutes"));
				col.setColumn3(DB.getDbString(rs, "hostname"));

				ret.add(col);
				count++;
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

		return (ret);
	}

	/**
	 * Funkcia, ktora vrati chybne stranky ulozene v tabulke stat_error podla zadanych kriterii a parametrov
	 *
	 * @param max_size	maximalny pocet riadkov, ktore sa maju vratit
	 * @param from			datum, od ktoreho sa maju stranky filtrovat
	 * @param to			datum, do ktoreho sa maju stranky filtrovat
	 * @param url			cast Url, ktore obsahuje atribut url. Ak pouzivatel zada tento udaj, vyfiltruju sa vsetky stranky, ktore obsahuju zadanu url
	 *
	 * @return				Vrati sa zoznam stranok, ktore zodpovedaju filtrovacim parametrom, ak citanie z databazy prebehne v poriadku. Inak sa vrati prazdny zoznam.
	 */
	public static List<Column> getErrorPages(int max_size, java.util.Date from, java.util.Date to, String url)
	{
		List<Column> ret = new ArrayList<>();

		String[] suffixes = StatNewDB.getTableSuffix("stat_error", from.getTime(), to.getTime());
		for (int s=(suffixes.length-1); s>=0; s--)
		{

			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			String sql = "SELECT DISTINCT year, week, url, query_string, sum(count) as count FROM stat_error"+suffixes[s]+" ";
			sql += StatDB.getYearTimeSQL(from, to, true);

			if(Tools.isNotEmpty(url))
				sql += " AND url LIKE ? ";

			if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true)
			{
				sql += " AND (domain_id=0 OR domain_id="+CloudToolsForCore.getDomainId()+") ";
			}

			sql += " GROUP BY url, year, week, query_string ORDER BY year DESC, week DESC, count DESC";

			Logger.debug(StatTableDB.class, "getErrorPages sql:"+sql);

			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement(sql);

				if(Tools.isNotEmpty(url))
					ps.setString(1, "%" + url + "%");

				rs = ps.executeQuery();

				Column col;
				int count = 0;

				while (rs.next() && count < max_size)
				{
					col = new Column();
					col.setIntColumn1(rs.getInt("year"));
					col.setIntColumn2(rs.getInt("week"));
					col.setColumn3(DB.getDbString(rs, "url"));
					col.setColumn4(DB.getDbString(rs, "query_string"));
					col.setIntColumn5(rs.getInt("count"));
					ret.add(col);
					count++;
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
				logError(ex);
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
		}

		return (ret);
	}

	public static List<Column> getSearchEnginesQuery(int maxRows, java.util.Date from, java.util.Date to, String groupIdsQuery)
	{
		if (groupIdsQuery == null)
			groupIdsQuery = "";

		Map<String, Number> map = new HashMap<>();

		String[] suffixes;
		if (from != null && to != null) suffixes = StatNewDB.getTableSuffix("stat_searchengine", from.getTime(), to.getTime());
		else suffixes = StatNewDB.getTableSuffix("stat_searchengine", 0, Tools.getNow());

		for (int s=0; s<suffixes.length; s++)
		{

			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try
			{
				db_conn = DBPool.getConnection();
				String sql = "SELECT s.query, COUNT(s.query) AS total FROM stat_searchengine"+suffixes[s]+" s WHERE s.doc_id>=0 ";
				if (from != null && to != null)
				{
				   sql += " AND search_date >= ? AND search_date <= ? ";
				}
				sql += groupIdsQuery;

				if (sql.toLowerCase().indexOf("group by")==-1) sql += "GROUP BY s.query";

				ps = db_conn.prepareStatement(sql);
				if (from != null && to != null)
				{
					ps.setTimestamp(1, new Timestamp(from.getTime()));
					ps.setTimestamp(2, new Timestamp(to.getTime()));
				}
				rs = ps.executeQuery();
				//iteruj cez riadky
				while (rs.next() )
				{
					String key = DB.getDbString(rs, "query");
					Number currentValue = map.get(key);

					if (currentValue == null) map.put(key, Integer.valueOf(rs.getInt("total")));
					else map.put(key, Integer.valueOf(rs.getInt("total")+currentValue.intValue()));
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
		}

		List<Column> ret = new ArrayList<>();

		//je mozne, ze tam mame viac ako max zaznamov, upracme a povazujme to za "ostatne"
		map = StatDB.sortByValue(map);

		Iterator<Map.Entry<String, Number>> iter = map.entrySet().iterator();
		Map.Entry<String, Number> me;
		int i = 0;
		while (iter.hasNext() && i++<maxRows)
		{
			me = iter.next();
			String key = me.getKey();
			Number value = me.getValue();

			//Logger.debug(StatDB.class, "getSearchEnginesQuery: key="+key+" value="+value);

			Column col = new Column();
			col.setColumn3(key);
			col.setColumn2(col.getColumn3());
			col.setIntColumn1(value.intValue());

			ret.add(col);
		}

		return (ret);

	}

	/**
	 *  statistika pristupov podla nazvu servera
	 *
	 *@return   List s nazvom servera a poctom pristupov
	 */
	public static List<Column> getSearchEnginesCount(int max_size, java.util.Date from, java.util.Date to, String groupIdsQuery)
	{
		return (StatTableDB.getSearchEnginesCount(max_size, from, to, groupIdsQuery, ""));
	}

	/**
	 *  statistika pristupov podla nazvu servera
	 *
	 *@return   List s nazvom servera a poctom pristupov
	 */
	public static List<Column> getSearchEnginesCount(int maxRows, java.util.Date from, java.util.Date to, String groupIdsQuery, String seoKeyword)
	{
		Map<String, Number> map = new HashMap<>();

		String[] suffixes;
		if (from != null && to != null) suffixes = StatNewDB.getTableSuffix("stat_searchengine", from.getTime(), to.getTime());
		else suffixes = StatNewDB.getTableSuffix("stat_searchengine", 0, Tools.getNow());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try
			{
				if (groupIdsQuery == null)
					groupIdsQuery = "";

				db_conn = DBPool.getConnection();

				String sql = "SELECT server, COUNT(server) AS total FROM stat_searchengine"+suffixes[s]+" WHERE doc_id >= 0 " + groupIdsQuery;

				if (from != null && to != null)
				   sql += " AND search_date >= ? AND search_date <= ? ";

				if (seoKeyword != null && Tools.isNotEmpty(seoKeyword))
					sql += " AND query = ? ";

				sql += " GROUP BY server";
				sql += " ORDER BY total DESC";

				ps = db_conn.prepareStatement(sql);
				int psCount = 1;
				if (from != null && to != null)
				{
					ps.setTimestamp(psCount++, new Timestamp(from.getTime()));
					ps.setTimestamp(psCount++, new Timestamp(to.getTime()));
				}
				if (seoKeyword != null && Tools.isNotEmpty(seoKeyword))
					ps.setString(psCount++, seoKeyword);

				rs = ps.executeQuery();
				//iteruj cez riadky
				while (rs.next())
				{
					String key = DB.prepareString(DB.getDbString(rs, "server"), 25);
					Number currentValue = map.get(key);

					if (currentValue == null) map.put(key, Integer.valueOf(rs.getInt("total")));
					else map.put(key, Integer.valueOf(rs.getInt("total")+currentValue.intValue()));
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
		}

		List<Column> ret = new ArrayList<>();

		map = StatDB.sortByValue(map);

		Iterator<Map.Entry<String, Number>> iter = map.entrySet().iterator();
		Map.Entry<String, Number> me;
		int i = 0;
		while (iter.hasNext() && i++<maxRows)
		{
			me = iter.next();
			String key = me.getKey();
			Number value = me.getValue();

			//Logger.debug(StatDB.class, "getSearchEnginesCount: key="+key+" value="+value);

			Column col = new Column();
			col.setColumn1(key);
			col.setColumn2(col.getColumn1());
			col.setIntColumn2(value.intValue());
			ret.add(col);
		}

		return (ret);
	}

	/**
	 *	Vrati list, v ktorom budu ulozene bean s hodnotou datetime vyhladavania a pozicie
	 *
	 *	@param 	maxSize			maximalne kolko poslednych vyhladavani sa pouzije
	 *	@param	from				od ktoreho datumu vyberame zaznamy
	 *	@param	to					do ktoreho datumu vyberame zaznamy
	 *	@param	seoKeywordId	identifikator klucoveho slova, pre ktore chceme vyfiltrovat zaznamy
	 *
	 *	@return  List s beanmi, ktore obsahuju datetime vyhladavania a poziciu pre poslednych maxSize vyhladavani
	 */
	public static List<Column> getGooglePositionsList(int maxSize, java.util.Date from, java.util.Date to, int seoKeywordId)
	{
		List<Column> ret = new ArrayList<>();

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			db_conn = DBPool.getConnection();

			String sql = "SELECT position, search_datetime FROM seo_google_position WHERE seo_google_position_id >= 0 ";

			if (from != null && to != null)
			   sql += " AND search_datetime >= ? AND search_datetime <= ? ";

			if (seoKeywordId != 0)
				sql += " AND keyword_id = ? ";

			sql += " ORDER BY search_datetime DESC";

			ps = db_conn.prepareStatement(sql);

			int psCount = 1;
			if (from != null && to != null)
			{
				ps.setTimestamp(psCount++, new Timestamp(from.getTime()));
				ps.setTimestamp(psCount++, new Timestamp(to.getTime()));
			}
			if (seoKeywordId != 0)
				ps.setInt(psCount++, seoKeywordId);

			rs = ps.executeQuery();
			//iteruj cez riadky
			Column col;
			int count = 0;
			while (rs.next() && count < maxSize)
			{
				col = new Column();
				col.setDateColumn1(rs.getDate("search_datetime"));
				col.setIntColumn1(rs.getInt("position"));
				ret.add(col);
				count++;
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

		return (ret);
	}

	/**
	 *  zoznam vsetkych serverov pre docId
	 *
	 *@return   List s nazvom servera a poctom pristupov
	 */
	public static List<Column> getSearchEnginesNames(int docId, java.util.Date from, java.util.Date to, String groupIdsQuery)
	{
		List<String> map = new ArrayList<>();

		String[] suffixes = StatNewDB.getTableSuffix("stat_searchengine", from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String sql = "SELECT DISTINCT server FROM stat_searchengine"+suffixes[s]+" WHERE doc_id >= 0 " + groupIdsQuery;

			   sql += " AND search_date >= ? AND search_date <= ? ";

				if(docId!=-1)
					sql += "AND doc_id = "+docId;

				sql += " ORDER BY server";

				ps = db_conn.prepareStatement(sql);


				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));


				rs = ps.executeQuery();
				while (rs.next())
				{
					String server = DB.getDbString(rs, "server");
					if (map.contains(server)==false) map.add(server);
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

		Collections.sort(map);

		List<Column> ret = new ArrayList<>();
		for (String server : map)
		{
			Column col = new Column();
			col.setColumn1(server);
			ret.add(col);
		}

		return (ret);
	}
	/**
	 *  zoznam vsetkych docId a title k nim
	 *
	 *@return   List s nazvom servera a poctom pristupov
	 */
	public static List<Column> getSearchDocId(java.util.Date from, java.util.Date to, String server, String groupIdsQuery)
	{
		groupIdsQuery = Tools.replace(groupIdsQuery, "group_id", "s.group_id");

		Map<String, Number> map = new HashMap<>();

		String[] suffixes;
		if (from != null && to != null) suffixes = StatNewDB.getTableSuffix("stat_searchengine", from.getTime(), to.getTime());
		else suffixes = StatNewDB.getTableSuffix("stat_searchengine", 0, Tools.getNow());

		for (int s=0; s<suffixes.length; s++)
		{

			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String sql = "SELECT DISTINCT s.doc_id, d.title FROM stat_searchengine"+suffixes[s]+" s, documents d WHERE s.doc_id = d.doc_id " + groupIdsQuery;

				if (from != null && to != null)
				{
				   sql += " AND search_date >= ? AND search_date <= ? ";
				}
				if(server!=null&&!server.equals(""))
					sql += "AND s.server = ? ";

				sql += " ORDER BY d.title";

				ps = db_conn.prepareStatement(sql);

				if (from != null && to != null)
				{
					ps.setTimestamp(1, new Timestamp(from.getTime()));
					ps.setTimestamp(2, new Timestamp(to.getTime()));
				}
				if(server!=null&&!server.equals(""))
					ps.setString(3, server);

				rs = ps.executeQuery();
				//iteruj cez riadky
				while (rs.next())
				{
					String key = DB.getDbString(rs, "title");
					Number value = rs.getInt("doc_id");

					if (map.containsValue(value)==false) map.put(key, value);
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

		map = StatDB.sortByKey(map);

		List<Column> ret = new ArrayList<>();

		Set<Map.Entry<String, Number>> set = map.entrySet();
		for (Map.Entry<String, Number> me : set){
			String key = me.getKey();
			Number value = me.getValue();

			//Logger.debug(StatDB.class, "getSearchDocId: key="+key+" value="+value);

			Column col = new Column();
			col.setColumn2(key);
			col.setColumn1(value.toString());
			col.setIntColumn1(value.intValue());
			ret.add(col);
		}

		return (ret);
	}

	/**
	 * Funkcia, ktora vrati zoznam vsetkych existujucich docId a title k nim
	 *
	 *	@return   List s docId a title
	 */
	public static List<Column> getDocTitleId(String groupIdsQuery)
	{
		List<Column> ret = new ArrayList<>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			String sql = "SELECT doc_id, title FROM documents WHERE doc_id > 0" + groupIdsQuery;

			sql += " ORDER BY title";

			ps = db_conn.prepareStatement(sql);

			rs = ps.executeQuery();
			//iteruj cez riadky
			Column col;
			while (rs.next())
			{
				col = new Column();
				col.setColumn2(DB.getDbString(rs, "title"));
				col.setColumn1(DB.getDbString(rs, "doc_id"));
				ret.add(col);
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
		return (ret);
	}

	/**
	 *  zoznam vyhladavani pre danne query
	 *
	 *@return   List s nazvom servera a poctom pristupov
	 */
	public static List<Column> getQueries(java.util.Date from, java.util.Date to, String query)
	{
		List<Column> ret = new ArrayList<>();

		String[] suffixes = StatNewDB.getTableSuffix("stat_searchengine", from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String sql = "SELECT * FROM stat_searchengine"+suffixes[s]+" s WHERE "+DB.fixAiCiCol("s.query")+" = ?";


				sql += " AND search_date >= ? AND search_date <= ? ";


				sql += "ORDER BY search_date ASC";

				ps = db_conn.prepareStatement(sql);
				ps.setString(1, DB.fixAiCiValue(query));

				ps.setTimestamp(2, new Timestamp(from.getTime()));
				ps.setTimestamp(3, new Timestamp(to.getTime()));


				rs = ps.executeQuery();
				//iteruj cez riadky
				Column col;
				GroupsDB groupsDB = GroupsDB.getInstance();
				int docId;
				DocDB docDB = DocDB.getInstance();
				DocDetails bdd;
				while (rs.next())
				{
					col = new Column();

					docId = rs.getInt("doc_id");
					bdd = docDB.getBasicDocDetails(docId, true);
					col.setDateColumn1(rs.getTimestamp("search_date"));
					col.setColumn1(rs.getString("server"));
					col.setColumn2(groupsDB.getPath(bdd.getGroupId())+"/"+bdd.getTitle());
					col.setColumn3(rs.getString("remote_host"));

					ret.add(col);

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
		return (ret);
	}

	private static void logError(Exception ex) {
		String message = ex.getMessage();
		if (message.contains("Invalid object name") || message.contains("Invalid column name") || message.contains("ORA-00942") ||
			message.contains("Unknown column") || message.contains("doesn't exist") || ex.getLocalizedMessage().contains("doesn't exist"))
		{
			//table doesn't exists, do not log error
		} else {
			Logger.error(StatTableDB.class, ex);
		}
	}
}
