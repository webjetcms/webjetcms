package sk.iway.iwcm.stat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.inquiry.AnswerForm;
import sk.iway.iwcm.inquiry.InquiryDB;

/**
 *
 *  StatGraphDB.java
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.13 $
 *@created      Date: 10.12.2004 14:33:14
 */
public class StatGraphDB
{
	protected StatGraphDB() {
		//utility class
	}

	/**
	 * Povodne sa zobrazovali iba domeny, napr. .com, .sk a podobne.
	 * Stara funkcionalita sa zachovava pomocou tohoto volania.
	 *
	 * @see 	StatGraphDB#getCountryTimeData(int, java.util.Date, java.util.Date, String, boolean, HttpServletRequest)
	 * @param max_size	int  kolko zaznamov chceme zobrazit
	 * @param from 		Date odkedy
	 * @param to 			Date dokedy
	 * @param groupIdsQuery
	 * @return TimeSeriesCollection zoznam podkladov pre graf
	 */
	public static Map<String, Map<Date, Number>> getCountryTimeData(int max_size, java.util.Date from, java.util.Date to, String groupIdsQuery)
	{
		HttpServletRequest noRequest = null;
		return getCountryTimeData(max_size, from,to,groupIdsQuery,COUNTRY_NAMES_AS_TLD,noRequest);
	}

	//konstanty pre lepsiu citatelnost
	public static final boolean COUNTRY_NAMES_AS_TLD = true;

	public static final boolean FULL_COUNTRY_NAMES = false;


	public static Map<String, Map<Date, Number>> getCountryTimeData(int maxRows, java.util.Date from, java.util.Date to, String groupIdsQuery,boolean countryNameAsTld,HttpServletRequest request)
	{
		return getCountryTimeData(maxRows, from, to, groupIdsQuery, countryNameAsTld, request, false);
	}

	/**
	 *  Gets the countryTimeData attribute of the StatDB object
	 *
	 *@param  maxRows  Description of the Parameter
	 *@param  from      Description of the Parameter
	 *@param  to        Description of the Parameter
	 *@return           The countryTimeData value
	 */
	public static Map<String, Map<Date, Number>> getCountryTimeData(int maxRows, java.util.Date from, java.util.Date to, String groupIdsQuery,boolean countryNameAsTld,HttpServletRequest request, boolean withoutBots)
	{
		//TimeSeriesCollection collection = new TimeSeriesCollection();

		if (groupIdsQuery == null)
		{
			groupIdsQuery = "";
		}

		Prop texts = (request != null ? Prop.getInstance(request) : Prop.getInstance("sk"));

		List<Column> topCountry = StatTableDB.getCountry(maxRows, from, to, groupIdsQuery, withoutBots);

		//uprav max_size podla realneho poctu zaznamov
		maxRows = topCountry.size();
		Map<String, Map<Date, Number>> timeSeriesTable = new Hashtable<>();

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

				//ok mame zoznam top pages, vytvor data pre graf
				int count = 0;

				Calendar cal = Calendar.getInstance();
				cal.setFirstDayOfWeek(Calendar.MONDAY);
				String sql = "SELECT "+StatNewDB.getDMYSelect("view_time")+", count(country) as views FROM stat_views"+suffixes[s]+" WHERE country=? AND view_time>=? AND view_time<? " + groupIdsQuery + whitelistedQuery;
				sql += " GROUP BY "+StatNewDB.getDMYGroupBy("view_time");

				Logger.debug(StatGraphDB.class, "getCountryTimeData sql="+sql);

				while (count < maxRows)
				{
					Column col2 = topCountry.get(count);

					//alebo chceme ako meno plny nazov krajiny
					String key;

					if (!countryNameAsTld) key = texts.getText ("stat.countries.tld."+col2.getColumn1());
					else key = col2.getColumn1();
					Map<Date, Number> bts = timeSeriesTable.get(key);
					if (bts == null)
					{
						bts = new HashMap<>();
						//collection.addSeries(bts);
						timeSeriesTable.put(key, bts);
					}

					ps = StatNewDB.prepareStatement(db_conn, sql);
					ps.setString(1, col2.getColumn1());
					ps.setTimestamp(2, new Timestamp(from.getTime()));
					ps.setTimestamp(3, new Timestamp(to.getTime()));
					rs = ps.executeQuery();
					while (rs.next())
					{
						cal.set(Calendar.YEAR, rs.getInt("vt_year"));
						cal.set(Calendar.MONTH, rs.getInt("vt_month")-1);
						cal.set(Calendar.DATE, rs.getInt("vt_day"));

						Logger.debug(StatGraphDB.class, "Adding "+Tools.formatDate(cal.getTimeInMillis())+" views="+rs.getInt("views"));

						try
						{
							bts.put(new java.util.Date(cal.getTime().getTime()), Integer.valueOf(rs.getInt("views")));
						}
						catch (Exception ex)
						{
							Logger.error(StatGraphDB.class,"getCountryTimeData: period allready exist: "+cal.getTime().toString());
							sk.iway.iwcm.Logger.error(ex);
						}
					}
					rs.close();
					ps.close();
					count++;
				}

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
				}
			}
		}

		return (timeSeriesTable);
	}

	public static Map<String, Map<Date, Number>> getBrowserTimeData(int maxRows, java.util.Date from, java.util.Date to, String groupIdsQuery)
	{
		return getBrowserTimeData(maxRows, from, to, groupIdsQuery, false);
	}

	/**
	 *  Gets the browserTimeData attribute of the StatDB object
	 *
	 *@param  maxRows  Description of the Parameter
	 *@param  from      Description of the Parameter
	 *@param  to        Description of the Parameter
	 *@return           The browserTimeData value
	 */
	public static Map<String, Map<Date, Number>> getBrowserTimeData(int maxRows, java.util.Date from, java.util.Date to, String groupIdsQuery, boolean withoutBots)
	{
		//TimeSeriesCollection collection = new TimeSeriesCollection();

		if (groupIdsQuery == null)
		{
			groupIdsQuery = "";
		}

		List<Column> topBrowsers = StatTableDB.getBrowser(maxRows, from, to, groupIdsQuery, withoutBots);

		//uprav max_size podla realneho poctu zaznamov
		maxRows = topBrowsers.size();

		Map<String, Map<Date, Number>> timeSeriesTable = new Hashtable<>();

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

				//ok mame zoznam top pages, vytvor data pre graf
				int count = 0;

				Calendar cal = Calendar.getInstance();
				cal.setFirstDayOfWeek(Calendar.MONDAY);
				String sql = "SELECT "+StatNewDB.getDMYSelect("view_time")+", count(browser_ua_id) as views FROM stat_views"+suffixes[s]+" WHERE browser_ua_id=? AND platform_id=? AND view_time>=? AND view_time<? " + groupIdsQuery + whitelistedQuery;
				sql += " GROUP BY "+StatNewDB.getDMYGroupBy("view_time");

				Logger.debug(StatGraphDB.class, "getBrowserTimeData sql="+sql);

				while (count < maxRows)
				{
					Column col = topBrowsers.get(count);

					String key = col.getColumn1() + " (" + col.getColumn2() + ")";
					Map<Date, Number> bts = timeSeriesTable.get(key);
					if (bts == null)
					{
						bts = new HashMap<>();
						//collection.addSeries(bts);
						timeSeriesTable.put(key, bts);
					}
					ps = StatNewDB.prepareStatement(db_conn, sql);
					ps.setInt(1, col.getIntColumn1());
					ps.setInt(2, col.getIntColumn2());
					ps.setTimestamp(3, new Timestamp(from.getTime()));
					ps.setTimestamp(4, new Timestamp(to.getTime()));
					rs = ps.executeQuery();
					while (rs.next())
					{
						cal.set(Calendar.YEAR, rs.getInt("vt_year"));
						cal.set(Calendar.MONTH, rs.getInt("vt_month")-1);
						cal.set(Calendar.DATE, rs.getInt("vt_day"));

						try
						{
							bts.put(new java.util.Date(cal.getTime().getTime()), Integer.valueOf(rs.getInt("views")));
						}
						catch (Exception ex)
						{
							Logger.error(StatGraphDB.class,"getBrowserTimeData: period allready exist: "+cal.getTime().toString());
							sk.iway.iwcm.Logger.error(ex);
						}
					}
					rs.close();
					ps.close();
					count++;
				}

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
		}

		return (timeSeriesTable);
	}

	/**
	 *  Gets the InquiryTimeData attribute of the StatDB object
	 *
	 *@param  maxRows  Description of the Parameter
	 *@param  from      Description of the Parameter
	 *@param  to        Description of the Parameter
	 *@param questionId
	 *@param request
	 *@return           The inquiryTimeData value
	 */
	public static Map<String,  Map<Date, Number>> getInquiryTimeData(int maxRows, java.util.Date from, java.util.Date to, int questionId, int userId, HttpServletRequest request)
	{
		//TimeSeriesCollection collection = new TimeSeriesCollection();

		List<AnswerForm> answers = InquiryDB.getAnswers(questionId, request);	//ziskam odpovede

		//Map<String, TimeSeries> timeSeriesTable = new Hashtable<String, TimeSeries>();
		Map<String,  Map<Date, Number>> timeSeriesTable = new Hashtable<>();

			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			for(AnswerForm af: answers){
			try
			{
				db_conn = DBPool.getConnection();

				//ok mame zoznam top pages, vytvor data pre graf

				Calendar cal = Calendar.getInstance();
				cal.setFirstDayOfWeek(Calendar.MONDAY);
				String sql = "SELECT "+StatNewDB.getDMYSelect("create_date")+", count(answer_id) as answers FROM inquiry_users WHERE create_date>=? AND create_date<? AND answer_id = ?";
				if(userId >= 0) sql += " AND user_id = ? ";
				if(userId == -1) sql += " AND user_id = -1 ";	//neprihlaseni pouzivatelia
				if(userId == -2) sql += " AND user_id >= 0 ";	//prihlaseni pouzivatelia
				sql += " GROUP BY "+StatNewDB.getDMYGroupBy("create_date");

				Logger.debug(StatGraphDB.class, "getInquiryTimeData sql="+sql);

					String key = af.getAnswerString();
					Map<Date, Number> bts = timeSeriesTable.get(key);
					if (bts == null)
					{
						bts = new HashMap<>();
						//collection.addSeries(bts);
						timeSeriesTable.put(key, bts);
					}
					ps = StatNewDB.prepareStatement(db_conn, sql);
					ps.setTimestamp(1, new Timestamp(from.getTime()));
					ps.setTimestamp(2, new Timestamp(to.getTime()));
					ps.setInt(3, af.getAnswerID());
					if(userId >= 0) ps.setInt(4, userId);
					rs = ps.executeQuery();
					while (rs.next())
					{
						cal.set(Calendar.YEAR, rs.getInt("vt_year"));
						cal.set(Calendar.MONTH, rs.getInt("vt_month")-1);
						cal.set(Calendar.DATE, rs.getInt("vt_day"));

						try
						{
							bts.put(new java.util.Date(cal.getTime().getTime()), Integer.valueOf(rs.getInt("answers")));
						}
						catch (Exception ex)
						{
							Logger.error(StatGraphDB.class,"getInquiryTimeData: period allready exist: "+cal.getTime().toString());
							sk.iway.iwcm.Logger.error(ex);
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
				}
			}
		}
		//return (collection);
		return (timeSeriesTable);
	}

	/**
	 *  Gets the InquiryPieData attribute of the StatDB object
	 *
	 *@param  from  Description of the Parameter
	 *@param  to    Description of the Parameter
	 *@param  questionId    Description of the Parameter
	 *@param  request    Description of the Parameter
	 *@return       The inquiryPieData value
	 */

	public static Map<String, Number> getInquiryPieData(java.util.Date from, java.util.Date to, int questionId, int userId, Prop prop, HttpServletRequest request)
	{
		Map<String, Number> map = new HashMap<>();
		List<AnswerForm> answers = InquiryDB.getAnswers(questionId, request);	//ziskam odpovede
		for(AnswerForm af: answers){
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String sql = "SELECT count(answer_id) as answers FROM inquiry_users WHERE create_date>=? AND create_date<? AND answer_id = ?";
				if(userId >= 0) sql += " AND user_id = ? ";
				if(userId == -1) sql += " AND user_id = -1 ";	//neprihlaseni pouzivatelia
				if(userId == -2) sql += " AND user_id >= 0 ";	//prihlaseni pouzivatelia
				ps = StatNewDB.prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));
				ps.setInt(3, af.getAnswerID());
				if(userId >= 0) ps.setInt(4, userId);
				rs = ps.executeQuery();	//pocet odpovedi pre dane rozmedzie

				while (rs.next())
				{
					map.put(af.getAnswerString(), Integer.valueOf(rs.getInt("answers")));
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
		}

		map = StatDB.sortByValue(map);

		return (map);
	}
}