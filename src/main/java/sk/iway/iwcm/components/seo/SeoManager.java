package sk.iway.iwcm.components.seo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.seo.rest.SeoService;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.users.UsersDB;

/**
 * SeoManager.java - trieda sluziaca na pracu v komponente SEO, rozne zvacsa staticke metody vyuzivajuce sa v komponente SEO
 *	@Title        webjet4
 *	@Company      Interway s.r.o. (www.interway.sk)
 *	@Copyright    Interway s.r.o. (c) 2001-2008
 *	@author       $Author: jeeff $
 *	@version      $Revision: 1.11 $
 *	@created      Date: 13.03.2009 14:55:51
 *	@modified     $Date: 2010/02/17 11:36:48 $
 */

public class SeoManager
{
	/**
	 * Toto sa vola z crontabu raz za den, kontroluje pozicie klucovych slov na
	 * google.com pre stranku Constants.getString("webSiteGooglePosition")
	 *
	 * @param args
	 */
	public static void main(String[] args)
	{
		Logger.debug(SeoManager.class,"SeoService.saveKeywordsPositions() - started");
		SeoService.saveKeywordsPositions();
		Logger.debug(SeoManager.class,"SeoService.saveKeywordsPositions() - finished");
	}

	/**
	 * Funkcia prida zaznam noveho vyhladavacieho stroja do tabulky seo_bots, ak taky neexistuje <br />
	 * Ak taky uz existuje, len zvysi pocet navstev o jeden a zmeni datum poslednej navstevy na aktualny cas
	 *
	 * @param browserName nazov browsera, ktory si prehliada stranky webJETu identifikovany ako vyhladavaci stroj
	 * @return true ak sa insert (update) podaril, inak false
	 */
	public static boolean addSearchEngineVisit(String browserName)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;

		int updated = 0;
		Calendar cal = Calendar.getInstance();

		if (Tools.isNotEmpty(browserName)) {
			try
			{
				db_conn = DBPool.getConnection();
				String sql="UPDATE seo_bots SET visit_count = visit_count + 1, last_visit = ? WHERE name = ?";
				ps = db_conn.prepareStatement(sql);
				ps.setTimestamp(1, new Timestamp(cal.getTimeInMillis()));
				ps.setString(2, browserName);
				updated = ps.executeUpdate();
				ps.close();
				if (updated < 1)
				{
					sql = "INSERT INTO seo_bots (name,last_visit, visit_count) VALUES (?, ?, 1)";
					ps = db_conn.prepareStatement(sql);
					ps.setString(1, browserName);
					ps.setTimestamp(2, new Timestamp(cal.getTimeInMillis()));
					ps.execute();
					ps.close();
				}
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
		return true;
	}

	/**
	 * Funkcia, ktora vrati identifikator vyhladavacieho stroja z tabulky seo_bots na zaklade jeho mena
	 *
	 * @param browserName meno vyhladavacieho bota, z ktoreho chceme urcit jeho identifikator
	 * @return identifikator daneho vyhladavacieho stroja, ak taky stroj neexistuje, vrati 0
	 */
	public static int getSearchEngineId(String browserName)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		int seoEnginesId = 0;

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT seo_bots_id FROM seo_bots WHERE name = ?");
			ps.setString(1, browserName);
			rs = ps.executeQuery();

			while (rs.next())
				seoEnginesId = rs.getInt("seo_bots_id");

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
			}
		}

		return seoEnginesId;
	}

	/**
	 * Funkcia, ktora vrati identifikator klucoveho slova z tabulky seo_keywords na zaklade jeho mena
	 *
	 * @param seoKeywordName nazov klucoveho slova, z ktoreho chceme urcit jeho identifikator
	 * @return identifikator daneho klucoveho slova, ak take slovo neexistuje, vrati 0
	 */
	public static int getSeoKeywordId(String seoKeywordName)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		int seoKeywordId = 0;

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT seo_keyword_id FROM seo_keywords WHERE name = ?");
			ps.setString(1, seoKeywordName);
			rs = ps.executeQuery();

			while (rs.next())
				seoKeywordId = rs.getInt("seo_keyword_id");

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
			}
		}

		return seoKeywordId;
	}

	/**
	 * Funkcia, ktora vrati aktualnu (najnovsie zaznamenanu) poziciu daneho klucoveho slova vo vyhladavani pre jeho domenu na vyhladavaci, ktory bol urceny pri vytvarani
	 *
	 * @param 	seoKeywordId identifikator klucoveho slova, ktoreho aktualnu poziciu chceme zistit
	 * @return 	aktualna (najnovsie zaznamenana) pozicia daneho klucoveho slova vo vyhladavani pre jeho domenu na vyhladavaci, ktory bol urceny pri vytvarani
	 */
	public static Column getActualPosition(int seoKeywordId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		Column actualPosition = new Column();

		try
		{
			db_conn = DBPool.getConnection();

			String sql = "SELECT position, search_datetime FROM seo_google_position WHERE keyword_id = ? ORDER BY search_datetime DESC";

			ps = StatNewDB.prepareStatement(db_conn, sql);
			ps.setInt(1, seoKeywordId);
			rs = ps.executeQuery();

			if (rs.next())
			{
				actualPosition.setIntColumn1(rs.getInt("position"));
				actualPosition.setDateColumn1(rs.getTimestamp("search_datetime"));
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
			}
		}

		return actualPosition;
	}

	/**
	 * Funkcia, ktora vrati nazov klucoveho slova z tabulky seo_keywords na zaklade jeho identifikatora
	 *
	 * @param seoKeywordId id klucoveho slova, z ktoreho chceme urcit jeho nazov
	 * @return meno daneho klucoveho slova, ak take slovo neexistuje, vrati null
	 */
	public static String getSeoKeywordName(int seoKeywordId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		String seoKeywordName = null;

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT name FROM seo_keywords WHERE seo_keyword_id = ?");
			ps.setInt(1, seoKeywordId);
			rs = ps.executeQuery();

			while (rs.next())
				seoKeywordName = rs.getString("name");

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
			}
		}

		return seoKeywordName;
	}

	/**
	 * Funkcia, ktora vrati naplneny bean klucoveho slova z tabulky seo_keywords na zaklade jeho identifikatora
	 *
	 * @param seoKeywordId id klucoveho slova, z ktoreho chceme urcit jeho vlastnosti
	 * @return klucove slovo, ak take slovo neexistuje, vrati null
	 */
	public static SeoKeyword getSeoKeyword(int seoKeywordId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		SeoKeyword seoKeyword = new SeoKeyword();

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM seo_keywords WHERE seo_keyword_id = ?");

			int psCounter = 1;

			ps.setInt(psCounter, seoKeywordId);
			rs = ps.executeQuery();

			while (rs.next())
			{
				seoKeyword.setName(rs.getString("name"));
				seoKeyword.setDomain(rs.getString("domain"));
				seoKeyword.setAuthor(UsersDB.getUser(rs.getInt("author")));
				seoKeyword.setSeoKeywordId(rs.getInt("seo_keyword_id"));
				seoKeyword.setCreatedTime(rs.getTimestamp("created_time"));
				seoKeyword.setSearchBot(rs.getString("search_bot"));
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
			}
		}

		return seoKeyword;
	}

	/**
	 * Funkcia, ktora vrati nazov vyhladavacieho stroja na zaklade jeho identifikatora z tabulky seo_bots
	 *
	 * @param 	browserId identifikator vyhladavacieho stroja, ktoreho meno sa chceme dozvediet
	 * @return 	nazov daneho vyhladavacieho stroja, ak neexistuje, vrati null
	 */
	public static String getSearchEngineName(int browserId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		String seoEnginesName = null;

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT name FROM seo_bots WHERE seo_bots_id = ?");
			ps.setInt(1, browserId);
			rs = ps.executeQuery();

			while (rs.next())
				seoEnginesName = rs.getString("name");

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
			}
		}

		return seoEnginesName;
	}

	/**
	 * Funkcia, ktora vrati vsetky klucove slova pouzivane pri SEO z tabulky seo_keywords pre daneho autora bez ohladu na domenu
	 *
	 * @param 	author 	identifikator pouzivatela, podla ktoreho sa filtruju klucove slova
	 * @return 	zoznam klucovych slov, ak je identifikator autora -1, vrati vsetky zaznamy klucovych slov
	 */
	public static List<SeoKeyword> getSeoKeywords(int author)
	{
		return (SeoManager.getSeoKeywords(author, "", ""));
	}

	/**
	 * Funkcia, ktora vrati vsetky klucove slova pouzivane pri SEO z tabulky seo_keywords bez ohladu na vyhladavaci stroj
	 *
	 * @param 	author 	identifikator pouzivatela, podla ktoreho sa filtruju klucove slova
	 * @param	domain	domena, podla ktorej sa maju vyfiltrovat klucove slova
	 *
	 * @return 	zoznam klucovych slov, ak je identifikator autora -1, vrati vsetky zaznamy klucovych slov
	 */
	public static List<SeoKeyword> getSeoKeywords(int author, String domain)
	{
		return (SeoManager.getSeoKeywords(author, domain, ""));
	}

	/**
	 * Funkcia, ktora vrati vsetky klucove slova pouzivane pri SEO z tabulky seo_keywords
	 *
	 * @param 	author 		identifikator pouzivatela, podla ktoreho sa filtruju klucove slova
	 * @param	domain		domena, podla ktorej sa maju vyfiltrovat klucove slova
	 * @param	searchBot	vyhladavac, podla ktoreho sa vyfiltruju klucove slova
	 *
	 *
	 * @return 	zoznam klucovych slov, ak je identifikator autora -1, vrati vsetky zaznamy klucovych slov
	 */
	public static List<SeoKeyword> getSeoKeywords(int author, String domain, String searchBot)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		if (domain == null)
			domain = "";
		if (searchBot == null)
			searchBot = "";

		List<SeoKeyword> seoKeywords = new ArrayList<>();
		String sql = "SELECT * FROM seo_keywords WHERE seo_keyword_id > 0";

		if (author > 0 && Tools.isNotEmpty(domain))
			sql +=" AND author=? AND domain=?";
		else if (author > 0 && Tools.isEmpty(domain))
			sql +=" AND author=?";
		else if (author < 0 && Tools.isNotEmpty(domain))
			sql +=" AND domain=?";

		if(Tools.isNotEmpty(searchBot))
			sql += " AND search_bot=?";

		sql += " ORDER BY created_time DESC";
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			int psCounter = 1;
			if (author > 0 && Tools.isNotEmpty(domain))
			{
				ps.setInt(psCounter++, author);
				ps.setString(psCounter++, domain);
			}
			else if (author > 0 && Tools.isEmpty(domain))
				ps.setInt(psCounter++, author);
			else if (author < 0 && Tools.isNotEmpty(domain))
				ps.setString(psCounter++, domain);
			if(Tools.isNotEmpty(searchBot))
				ps.setString(psCounter++, searchBot);

			rs = ps.executeQuery();

			while (rs.next())
			{
				SeoKeyword seoKeyword = new SeoKeyword();
				seoKeyword.setSeoKeywordId(rs.getInt("seo_keyword_id"));
				seoKeyword.setName(rs.getString("name"));
				seoKeyword.setDomain(rs.getString("domain"));
				seoKeyword.setCreatedTime(rs.getTimestamp("created_time"));
				seoKeyword.setAuthor(UsersDB.getUser(rs.getInt("author")));
				seoKeyword.setSearchBot(rs.getString("search_bot"));

				seoKeywords.add(seoKeyword);
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

		return (seoKeywords);
	}

	/**
	 * Funkcia, ktora vrati vsetky ROZNE klucove slova podla mena pouzivane pri SEO z tabulky seo_keywords
	 *
	 * @return 	zoznam ROZNYCH klucovych slov podla mena
	 */
	public static List<String> getDistinctSeoKeywords()
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		List<String> seoKeywords = new ArrayList<>();
		String sql;

		sql ="SELECT DISTINCT name FROM seo_keywords ORDER BY name ASC";

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);

			rs = ps.executeQuery();

			while (rs.next())
				seoKeywords.add(rs.getString("name"));

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
			}
		}

		return (seoKeywords);
	}

	/**
	 * Funkcia, ktora vrati vsetky klucove slova, ktore maju rovnake meno, ale lisia sa v domene a su pouzivane pri SEO z tabulky seo_keywords
	 *
	 * @param	name	nazov klucoveho slova, podla ktoreho hladame klucove slova, ktore sa rovnako volaju, ale maju prednastavenu inu domenu
	 *
	 * @return 	zoznam klucovych slov(bean SeoKeyword), ktore maju rovnake meno ako je vstupny parameter name, ale lisia sa v domene
	 */
	public static List<SeoKeyword> getSameSeoKeywords(String name)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		List<SeoKeyword> seoKeywords = new ArrayList<>();
		String sql;

		sql ="SELECT * FROM seo_keywords WHERE name = ?";

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);

			int psCounter = 1;
			ps.setString(psCounter++, name);

			rs = ps.executeQuery();

			while (rs.next())
			{
				SeoKeyword seoKeyword = new SeoKeyword();

				seoKeyword.setSeoKeywordId(rs.getInt("seo_keyword_id"));
				seoKeyword.setName(rs.getString("name"));
				seoKeyword.setDomain(rs.getString("domain"));
				seoKeyword.setCreatedTime(rs.getTimestamp("created_time"));
				seoKeyword.setAuthor(UsersDB.getUser(rs.getInt("author")));

				seoKeywords.add(seoKeyword);
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
			}
		}

		return (seoKeywords);
	}

	/**
	 * Funkcia, ktora vrati mnozinu udajov, ktore su potrebne na vytvorenie stlpcoveho grafu zobrazujuceho pocet
	 * vyhladavani klucovych slov (meno a pocet vyhladavani)
	 *
	 * @param from				dateTime, od ktoreho sa zapocitavaju vyhladavania
	 * @param to				dateTime, do ktoreho sa zapocitavaju vyhladavania
	 * @param serverName
	 * @param searchDocId
	 * @param groupIdsQuery		retazec, ktory sa ma pridat ku sql query na zaklade filtrovania servera, z ktoreho sa uskutocnilo
	 *
	 * @return 					mnozinu udajov, ktore su potrebne na vytvorenie stlpcoveho grafu zobrazujuceho pocet vyhladavani
	 * 							klucovych slov (meno a pocet vyhladavani)
	 */
	public static List<Column> getFilterSeoKeywords(java.util.Date from, java.util.Date to, String serverName, int searchDocId, String groupIdsQuery)
	{
		Map<String, Number> map = new HashMap<>();
		List<String> seoKeywords = SeoManager.getDistinctSeoKeywords();

		Connection db_conn = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		if (from != null && to != null)
		{
			String[] suffixes = StatNewDB.getTableSuffix("stat_searchengine", from.getTime(), to.getTime());
			for (int s=0; s<suffixes.length; s++)
			{
				for (String seoKeyword : seoKeywords)
				{
					db_conn = DBPool.getConnection();
					try
					{
						String sql = "SELECT query, COUNT(query) AS total FROM stat_searchengine"+suffixes[s]+" WHERE doc_id >= 0 AND query = ?";

						//if (from != null && to != null)
						   sql += " AND search_date >= ? AND search_date <= ? ";

						if (Tools.isNotEmpty(serverName))
							sql += " AND server = ? ";

						if (searchDocId > 0)
							sql += " AND doc_id = ? ";
						if (groupIdsQuery != null)
							sql += groupIdsQuery;

						sql += " GROUP BY query";
						sql += " ORDER BY total DESC";
						//System.out.println("\n"+seoKeyword+"\n" + sql + "\n\n");
						ps = db_conn.prepareStatement(sql);
						int psCounter = 1;

						ps.setString(psCounter++, seoKeyword);

//						if (from != null && to != null)
//						{
							ps.setTimestamp(psCounter++, new Timestamp(from.getTime()));
							ps.setTimestamp(psCounter++, new Timestamp(to.getTime()));
//						}
						if (Tools.isNotEmpty(serverName))
							ps.setString(psCounter++, serverName);

						if (searchDocId > 0)
							ps.setInt(psCounter++, searchDocId);

						rs = ps.executeQuery();

						boolean isAdded = false;
						while(rs.next())
						{
							String key = seoKeyword.toLowerCase();
							Number currentValue = map.get(key);

							if (currentValue == null) map.put(key, Integer.valueOf(rs.getInt("total")));
							else map.put(key, Integer.valueOf(rs.getInt("total")+currentValue.intValue()));

							isAdded = true;
						}
						if(!isAdded)
						{
							String key = seoKeyword.toLowerCase();
							Number currentValue = map.get(key);

							if (currentValue == null) map.put(key, 0);
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
			}
		}

		List<Column> filterSeoKeywords = new ArrayList<>();

		try
		{
			//je mozne, ze tam mame viac ako max zaznamov, upracme a povazujme to za "ostatne"
			map = StatDB.sortByValue(map);

			Set<Map.Entry<String, Number>> set = map.entrySet();
			for (Map.Entry<String, Number> me : set){
				String key = me.getKey();
				Number value = me.getValue();

				Logger.debug(StatDB.class, "getFilterSeoKeywords: key="+key+" value="+value);

				Column col = new Column();
				col.setColumn1(key);
				col.setIntColumn1(value.intValue());

				filterSeoKeywords.add(col);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}



		return (filterSeoKeywords);
	}

	/**
	 * Funkcia, ktora vrati List, v ktorom budu ulozene dvojice: nazov klucoveho slova a pocet vyskytov na stranke resp. skupiny stranok
	 *
	 * @param searchDocId	Identifikator stranky, na ktorej zisti vyskyt klucovych slov
	 * @param groupIdsQuery	Retazec, ktory sa pridava na koniec query, zisti vyskyt klucovych slov v skupine stranok

	 * @return 					Vrati List, v ktorom budu ulozene dvojice: nazov klucoveho slova a pocet vyskytov na stranke resp. skupiny stranok
	 */
	public static List<Column> getNumberSeoKeywordsOnPage(int searchDocId, String groupIdsQuery)
	{
		List<Column> filterSeoKeywords = new ArrayList<>();
		List<String> seoKeywords = SeoManager.getDistinctSeoKeywords();

		if(seoKeywords.isEmpty() || groupIdsQuery == null)
			return filterSeoKeywords;

		int counter = 0;
		int tempCount = 0;

		Connection db_conn = DBPool.getConnection();
		ResultSet rs = null;
		PreparedStatement ps = null;

		try
		{
			for (String seoKeyword : seoKeywords)
			{
				String sql = "SELECT data_asc, title FROM documents WHERE (data_asc LIKE ? OR title LIKE ?) ";

				if (searchDocId > 0)
					sql += " AND doc_id = ? ";
				else if (Tools.isNotEmpty(groupIdsQuery) && groupIdsQuery != null)
					sql += (" " + groupIdsQuery);

				ps = StatNewDB.prepareStatement(db_conn, sql);

				int psCounter = 1;
				ps.setString(psCounter++, "%"+DB.internationalToEnglish(seoKeyword.toLowerCase())+"%");
				ps.setString(psCounter++, "%"+seoKeyword+"%");

				if (searchDocId > 0)
					ps.setInt(psCounter++, searchDocId);

				rs = ps.executeQuery();

				//data.clear();
				tempCount = 0;
				counter = 0;
				int counter2 = 0;
				while (rs.next())
				{
					counter += Tools.getNumberSubstring((DB.internationalToEnglish(rs.getString("title")) + " " + DB.getDbString(rs, "data_asc")), DB.internationalToEnglish(seoKeyword.toLowerCase()));
					counter2 += getNumberSubstringNoBoundary((DB.internationalToEnglish(rs.getString("title")) + " " + DB.getDbString(rs, "data_asc")), DB.internationalToEnglish(seoKeyword.toLowerCase()));
					tempCount++;
				}

				Column col = new Column();
				col.setColumn1(seoKeyword.toLowerCase());
				col.setIntColumn2(tempCount);
				col.setIntColumn1(counter);
				col.setIntColumn3(counter2);

				filterSeoKeywords.add(col);
			}

			if (rs != null) rs.close();
			if (ps != null) ps.close();
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

		return (filterSeoKeywords);
	}

	public static int getNumberSubstringNoBoundary(String src, String subString)
	{
		if (src == null)
			return (-1);

		if (src.indexOf(subString) == -1 || src.isEmpty() || subString.isEmpty())
			return (0);

	   int counter = 0;
	   Pattern p = Pattern.compile(subString);
	   Matcher m = p.matcher(src); // v com sa to ma matchovat

	   while(m.find())
	   	counter++;

		return (counter);
	}

	/**
	 * Funkcia, ktora vrati vsetkych roznych pouzivatelov, ktori pridali klucove slovo kvoli moznosti filtracie
	 *
	 * @return zoznam unikatnych pouzivatelov, ktori pridali klucove slovo
	 */
	public static List<Column> getUniqueAuthors()
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		List<Column> seoUsers = new ArrayList<>();
		String sql ="SELECT DISTINCT author FROM seo_keywords";
		int tempAuthor;

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();

			while (rs.next())
			{
				Column seoUser = new Column();
				tempAuthor = rs.getInt("author");
				seoUser.setIntColumn1(tempAuthor);
				seoUser.setColumn1(UsersDB.getUser(tempAuthor).getFullName());

				seoUsers.add(seoUser);
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
			}
		}

		return (seoUsers);
	}

	/**
	 * Funkcia, ktora vrati vsetky rozne domeny, pre ktore sa pridali klucove slova kvoli moznosti filtracie
	 *
	 * @return zoznam unikatnych domen, pre ktore sa pridali klucove slova
	 */
	public static List<String> getUniqueDomains()
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		List<String> seoDomains = new ArrayList<>();
		String sql ="SELECT DISTINCT domain FROM seo_keywords";

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();

			while (rs.next())
				seoDomains.add(rs.getString("domain"));

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
			}
		}

		return (seoDomains);
	}

	/**
	 * Funkcia, ktora vrati vsetky rozne vyhladavace, pre ktore sa pridali klucove slova kvoli moznosti filtracie
	 *
	 * @return zoznam unikatnych vyhladavacov, pre ktore sa pridali klucove slova
	 */
	public static List<String> getUniqueSearchBots()
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		List<String> seoSearchBots = new ArrayList<>();
		String sql ="SELECT DISTINCT search_bot FROM seo_keywords";

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();

			while (rs.next())
				seoSearchBots.add(DB.getDbString(rs, "search_bot"));

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
			}
		}

		return (seoSearchBots);
	}

	/**
 	 * Vymaze klucove slovo z tabulky seo_keywords
 	 *
 	 * @param seoKeywordId - identifikacne cislo klucoveho slova, ktore chceme vymazat
 	 * @return true ak vymazanie z databazy prebehlo v poriadku, inak false
 	 */
	public static boolean deleteSeoKeyword(int seoKeywordId)
	{
		boolean returnValue = false; //neuspech

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM seo_keywords WHERE seo_keyword_id = ?");

			int psCounter = 1;
			ps.setInt(psCounter++, seoKeywordId);

			int delRows = ps.executeUpdate();

			if (delRows > 0)
				returnValue = true;

			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;
		}
		catch (SQLException e)
		{
			returnValue = false; // nepodarilo sa vymazat
			sk.iway.iwcm.Logger.error(e);
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
		return returnValue;
	}

	/**
	 * Ulozi nove klucove slovo do databazy
	 *
	 * @param  	seoKeyword		bean s vlastnostami klucoveho slova
	 * @param	loggedUserId 	pouzivatel, ktory slovo vytvoril a ulozil
	 * @return	true, ak sa zapis zaznamu do tabulky prebehne v poriadku, inak false
	 */
	public static boolean saveSeoKeyword(SeoKeyword seoKeyword, int loggedUserId)
	{
		boolean saveOK = false;

		if (seoKeyword == null)
			return saveOK;

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			String sql = "INSERT INTO seo_keywords (name, domain, created_time, author, search_bot) VALUES (?, ?, ?, ?, ?)";

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);

			int psCounter = 1;
			ps.setString(psCounter++, seoKeyword.getName());
			ps.setString(psCounter++, seoKeyword.getDomain());
			ps.setTimestamp(psCounter++, new Timestamp(Tools.getNow()));
			ps.setInt(psCounter++, loggedUserId);
			ps.setString(psCounter++, seoKeyword.getSearchBot());

			int insRows = ps.executeUpdate();

			if (insRows > 0)
				saveOK = true;

			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;

		}
		catch (Exception ex)
		{
			saveOK = false;
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

		return saveOK;
	}

	/**
	 * Updejtne informacie o klucovom slove
	 *
	 * @param  	seoKeyword		bean s vlastnostami klucoveho slova
	 * @param	loggedUserId 	pouzivatel, ktory slovo vytvoril a ulozil
	 * @param	seoKeywordId	id klucoveho slova, ktore chceme aktualizovat
	 *
	 * @return	true, ak sa zapis zaznamu do tabulky prebehne v poriadku, inak false
	 */
	public static boolean saveSeoKeyword(SeoKeyword seoKeyword, int loggedUserId, int seoKeywordId)
	{
		boolean saveOK = false;

		if (seoKeyword == null)
			return saveOK;

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			String sql = "UPDATE seo_keywords SET name = ?, domain = ?, created_time = ?, author = ?, search_bot = ? WHERE seo_keyword_id = ?";

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);

			int psCounter = 1;
			ps.setString(psCounter++, seoKeyword.getName());
			ps.setString(psCounter++, seoKeyword.getDomain());
			ps.setTimestamp(psCounter++, new Timestamp(Tools.getNow()));
			ps.setInt(psCounter++, loggedUserId);
			ps.setString(psCounter++, seoKeyword.getSearchBot());
			ps.setInt(psCounter++, seoKeywordId);

			int insRows = ps.executeUpdate();

			if (insRows > 0)
				saveOK = true;

			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;

		}
		catch (Exception ex)
		{
			saveOK = false;
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

		return saveOK;
	}

	/**
	 * Skontroluje, ci dane klucove slovo sa uz nenachadza v databaze
	 *
	 * @param 	seoKeyword bean s vlastnostami klucoveho slova (nazov, domena a vyhladavac)
	 * @return	true, ak sa klucove slovo v tabulke nenachadza, inak false
	 */
	public static boolean isKeywordNonExist(SeoKeyword seoKeyword)
	{
		boolean returnValue = true;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			String sql = "SELECT * FROM seo_keywords WHERE name = ? AND domain = ? AND search_bot = ?";

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);

			ps.setString(1, seoKeyword.getName());
			ps.setString(2, seoKeyword.getDomain());
			ps.setString(3, seoKeyword.getSearchBot());

			rs = ps.executeQuery();
			if (rs.next())
				returnValue = false;

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

		return returnValue;
	}

	/**
	 * Funkcia, ktora vrati zoznam nazvov klucovych slov v tvare ('keyword1', 'keyword2', ... , 'keywordN') kvoli
	 * doplneniu do sql query ku klauzule IN vo funkcii StatDB.getKeywordsBarData()
	 *
	 * @return zoznam nazvov klucovych slov zapisanych za sebou v Stringu oddelenych ciarkami
	 */
	public static String getKeywordsInQuery()
	{
		StringBuilder returnString = new StringBuilder("(");

		List<String> seoKeywords = SeoManager.getDistinctSeoKeywords();
		if (seoKeywords.size() == 0)
			return ("('')");
		for (String seoKeyword : seoKeywords)
		{
			returnString.append('\'').append(seoKeyword).append("', ");
		}
		return (returnString.substring(0, (returnString.length() - 2)) + ")"); // odstrani poslednu ciarku a zakonci zatvorku
	}

	/**
	 * Funkcia, ktora vrati zoznam nazvov klucovych slov v poli String[] - vyuzitie v triede StatDB a vo funkcii
	 * getKeywordsBarData na vykreslenie grafu
	 *
	 * @return zoznam nazvov klucovych slov zapisanych v poli String[]
	 */
	public static String[] getKeywordsNameInArray()
	{
		List<SeoKeyword> seoKeywords = SeoManager.getSeoKeywords(-1);
		String[] returnArray = new String[seoKeywords.size()];

		for (int i = 0; i < returnArray.length; i++)
			returnArray[i] = seoKeywords.get(i).getName();

		return (returnArray);
	}

	/**
	 * Funkcia, ktora vrati zoznam nazvov klucovych slov oddelenych bodkociarkou - vyuzitie v triede
	 * EditorForm pri nastavovani defaultnych klucovych slov pre kazdy dokument
	 * @return
	 */
	public static String getKeywordNames()
	{
		StringBuilder text = new StringBuilder();

		for (String keyword : getKeywordsNameInArray())
			text.append(keyword).append(';');

		return text.toString();
	}

	/**
	 * Funkcia, ktora precisti zadanu domenu pre dane klucove slovo o substring "www" a "http://" pripadne ich kombinaciu
	 * kvoli jedinecnosti klucovych slov, ktore sa identifikuju prave dvojicou nazov domena
	 *
	 * @param 	seoKeyword bean klucoveho slova
	 * @return	bean {@link SeoKeyword} s upravenou domenou
	 */
	public static SeoKeyword cleanupDomain(SeoKeyword seoKeyword)
	{
		String domain = seoKeyword.getDomain();
		domain = Tools.replace(domain, "http://", "");
		domain = Tools.replace(domain, "www.", "");

		seoKeyword.setDomain(domain);

		return (seoKeyword);
	}
}

