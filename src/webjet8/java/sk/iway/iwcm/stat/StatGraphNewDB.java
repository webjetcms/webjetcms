package sk.iway.iwcm.stat;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.seo.Bot;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.monitoring.MonitoringBean;
//import java.util.*;
/**
 *  StatGraphNewDB.java
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.33 $
 *@created      Date: 11.1.2005 13:16:49
 *@modified     $Date: 2010/02/17 11:37:18 $
 */
public class StatGraphNewDB
{
	public static final int NUMBER_OF_VALUES = 2000;

	protected StatGraphNewDB() {
		//utility class
	}


//	======================== GENEROVANIE GRAFOV ===============================

	/**
	 *  Funkcia na naplnenie mnoziny dat, ktore sa pouziju pri vykresleni casoveho grafu historie navstev vyhladavacich botov
	 *
	 *@param  rows		  zoznam vsetkych vyhladavacich botov, ktori pristupili na stranku spolu s ich poctom navstev
	 *@param  max_size  Pocet prvych mx_size vyhladavacich botov s najvacsim poctom navstev
	 *@param  from      Od kedy je pocitana historia pristupov
	 *@param  to        Do kedy je pocitana historia pristupov
	 *@param  groupId   Identifikacne cislo adresara, z ktoreho chceme statistiku
	 *@return
	 */
	public static Map<String,  Map<Date, Number>> getBotsTimeData(List<Bot> rows, int max_size, java.util.Date from, java.util.Date to, int groupId)
	{
		Map<String,  Map<Date, Number>> collection = new Hashtable<>();

		Bot[] bots = new Bot[max_size];
		Map<Date, Integer> visitDays = new Hashtable<>();
		Date day;

		// na naplnenie prvych max_size botov sa pouzije uz existujuci zoznam vsetkych botov ulozeny v session
		int count = 0;
		for (Bot bot : rows)
		{
			if (count >= max_size)
				break;
			bots[count] = bot;
			count++;
		}
		max_size = count; //uprav max_size podla realneho poctu zaznamov

		//mame zoznam botov, potrebujeme historiu pristupov
		count = 0;
		Map<Date, Number> bts;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder sql;

		String[] suffixes = StatNewDB.getTableSuffix(from.getTime(), to.getTime());

		// pre kazdeho z max_size botov sa spocitaju jeho pristupy na stranku, jedna session = jeden pristup,

		while (count < max_size)
		{
			//bts = new TimeSeries(bots[count].getName());
			bts = new HashMap<>();

			visitDays.clear();
			for (int s=0; s<suffixes.length; s++)
			{
				db_conn = DBPool.getConnection();

				sql = new StringBuilder("SELECT MAX(s.view_time) AS visit_time FROM stat_views");
				sql.append(suffixes[s]).append(" s ");
				sql.append("WHERE s.browser_id = ? AND s.view_time >= ? AND s.view_time <= ? ");

				sql.append(StatDB.getRootGroupWhere("s.group_id", groupId));

				sql.append(" GROUP BY s.session_id");

				try
				{
					ps = StatNewDB.prepareStatement(db_conn, sql.toString());
					ps.setInt(1, bots[count].getBotId());
					ps.setTimestamp(2, new Timestamp(from.getTime()));
					ps.setTimestamp(3, new Timestamp(to.getTime()));

					rs = ps.executeQuery();
					while (rs.next())
					{
						day = rs.getTimestamp("visit_time");

						Calendar calendar = Calendar.getInstance();
						calendar.setTime(day);
						calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
						calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
						calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
						calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
						day = calendar.getTime();

						if (!visitDays.containsKey(day))
							visitDays.put(day, 1);
						else
						{
							visitDays.put(day, (visitDays.get(day)+1));
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
					if (!StatNewDB.createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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
			for (Map.Entry<Date, Integer> me: visitDays.entrySet())
			{
				try
				{
					bts.put(me.getKey(), me.getValue());
				}
				catch (Exception ex)
				{
					Logger.error(StatGraphDB.class,"getBotsTimeData: period already exist: "+me.getKey().toString());
					sk.iway.iwcm.Logger.error(ex);
				}
			}

			collection.put(bots[count].getName(), bts);

			count++;
		}
		return (collection);
	}

	/**
	 *  Funkcia na naplnenie mnoziny dat, ktore sa pouziju pri vykresleni casoveho grafu historie navstev vyhladavacich botov
	 *
	 *@param  rows		  zoznam vsetkych vyhladavacich botov, ktori pristupili na stranku spolu s ich poctom navstev
	 *@param  keyword
	 *@param  from      Od kedy je pocitana historia pristupov
	 *@param  to        Do kedy je pocitana historia pristupov
	 *@param  groupId   Identifikacne cislo adresara, z ktoreho chceme statistiku
	 *@return
	 */
	public static Map<String,  Map<Date, Number>> getPositionHistoryData(List<Column> rows, String keyword, java.util.Date from, java.util.Date to, int groupId)
	{
		Map<String,  Map<Date, Number>> collection = new Hashtable<>();

		Date visitDay;

		Map<Date, Number> bts;

		bts = new HashMap<>();

		for (Column position : rows)
		{
			visitDay = position.getDateColumn1();

			Calendar cal = Calendar.getInstance();
			cal.setTime(visitDay);
			cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
			cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
			cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
		  	cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
			visitDay = cal.getTime();

			try
			{
				bts.put(visitDay, position.getIntColumn1());
			}
			catch (Exception ex)
			{
				Logger.error(StatGraphDB.class,"getPositionHistoryData: period already exist: "+visitDay.toString());
				sk.iway.iwcm.Logger.error(ex);
			}
		}
		collection.put(keyword, bts);

		return (collection);
	}

	public static Map<String,  Map<Date, Number>> getTimeData( int max_size, java.util.Date from, java.util.Date to, int rootGroupId, String docList, String type)
	{
		return getTimeData(max_size, from, to, rootGroupId, docList, type, false);
	}

	/**
	 * Vytvori casovy graf s ciarami, ak docList obsahuje zoznam docID, tak sa vygeneruje graf so statistikou pre
	 * dane docID. V pripade ze je docList prazdny, beru sa do uvahy top stranky.
	 *
	 * @param max_size - udava pocet, kolko stranok sa ma v grafe zobrazit
	 * @param from - dolna hranica vybraneho obdobia pre zobrazenie statistiky
	 * @param to - horna hranica vybraneho obdobia pre zobrazenie statistiky
	 * @param rootGroupId - groupID, ktore sa budu brat do uvahy
	 * @param docList - zoznam docID, ktorym sa zobrazi statistika
	 * @param type - typ vypisu grafu (momentalne sa vykonava iba vyber top stranok)
	 * @return
	 */
	public static Map<String,  Map<Date, Number>> getTimeData( int max_size, java.util.Date from, java.util.Date to, int rootGroupId,
																	String docList, String type, boolean withoutBots)
	{
		Map<String,  Map<Date, Number>> collection = new Hashtable<>();

		Map<Integer, Column> topPagesTable = new Hashtable<>();
		List<Column> topPages = new ArrayList<>();

		String whitelistedQuery = "";
		if(withoutBots)
			whitelistedQuery = StatNewDB.getWhiteListedUAQuery();

		String[] suffixes = StatNewDB.getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				//TODO: pri rozdeleni statistik nemusi byt presne z dovodu max_size citaneho podla prveho obdobia

				db_conn = DBPool.getConnection();

				StringBuilder sql;
				if (Tools.isNotEmpty(docList) && "-1".equals(docList)==false)
				{
					sql = new StringBuilder("SELECT s.doc_id, COUNT(s.doc_id) AS views").append(
							" FROM stat_views").append(suffixes[s]).append(" s");


					StringTokenizer st = new StringTokenizer(docList, ",");
					int documentID;
					int poc = 0;
					sql.append(" WHERE view_time>=? AND view_time<=? AND s.doc_id IN (");

					while (st.hasMoreTokens())
					{
						documentID = Integer.parseInt(st.nextToken());
						if (documentID > 0)
						{
							if (poc > 0)
							{
								sql.append(", '").append(documentID).append('\'');
							}
							else
							{
								sql.append('\'').append(documentID).append('\'');
							}
							poc++;
						}
					}

					if (poc > 0) sql.append(") ");
					else sql.append("-1) ");

					sql.append( " GROUP BY s.doc_id");
					sql.append(" ORDER BY views DESC");
				}
				else
				{
					sql = new StringBuilder("SELECT s.doc_id, COUNT(s.doc_id) AS views").append(
							" FROM stat_views").append(suffixes[s]).append(" s").append(
							" WHERE view_time>=? AND view_time<=? ");
					sql.append(StatDB.getRootGroupWhere("s.group_id", rootGroupId));
					sql.append(whitelistedQuery);
					sql.append(" GROUP BY s.doc_id");
					sql.append(" ORDER BY views DESC");
				}

				Logger.debug(StatGraphNewDB.class,sql.toString());

				ps = StatNewDB.prepareStatement(db_conn, sql.toString());
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));

				rs = ps.executeQuery();

				int count = 0;
				while (rs.next() && count < max_size)
				{
					int docId = rs.getInt("doc_id");
					Column col = topPagesTable.get(docId);
					if (col==null)
					{
						col = new Column();
						col.setIntColumn1(docId);
						col.setIntColumn2(rs.getInt("views"));
						topPagesTable.put(Integer.valueOf(docId), col);
						topPages.add(col);
					}
					else
					{
						col.setIntColumn2(col.getIntColumn2()+rs.getInt("views"));
					}
					//Logger.println(this,"Top docs: "+rs.getInt("s.doc_id"));
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
				if (!StatNewDB.createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

		//usporiadaj podla poctu

		Collections.sort(topPages, new Comparator<Column>() {
			@Override
			public int compare(Column c1, Column c2)
			{
				return (c2.getIntColumn2() - c1.getIntColumn2());
			}

		});

		try
		{
			StringBuilder sql;

			DocDB docDB = DocDB.getInstance();

			Calendar cal = Calendar.getInstance();
			Map<Date, Number> bts_views;

			Iterator<Column> iter = topPages.iterator();
			int count = 0;
			while (iter.hasNext() && count++<max_size)
			{
				Column col = iter.next();
				int docId = col.getIntColumn1();
				//title = (String) topPages.get(docID);

				//bts_views = new TimeSeries(docDB.getBasicDocDetails(docId, true).getTitle());
				bts_views = new HashMap<>();

				for (int s=0; s<suffixes.length; s++)
				{
					Connection db_conn = null;
					PreparedStatement ps = null;
					ResultSet rs = null;
					try
					{
						db_conn = DBPool.getConnection();

						sql = new StringBuilder("SELECT s.doc_id, COUNT(s.doc_id) AS views, ");
						sql.append(StatNewDB.getDMYSelect("s.view_time"));
						sql.append(" FROM stat_views").append(suffixes[s]).append(" s WHERE s.doc_id=? AND s.view_time>=? AND s.view_time<=? ").append(whitelistedQuery);
						sql.append(" GROUP BY s.doc_id, ").append(StatNewDB.getDMYGroupBy("s.view_time")).append(" ORDER BY views DESC");

						Logger.debug(StatGraphNewDB.class, "sql 2:"+sql.toString());

						ps = StatNewDB.prepareStatement(db_conn, sql.toString());
						ps.setInt(1, docId);
						ps.setTimestamp(2, new Timestamp(from.getTime()));
						ps.setTimestamp(3, new Timestamp(to.getTime()));
						rs = ps.executeQuery();
						while (rs.next())
						{
							cal.clear();
							cal.setFirstDayOfWeek(Calendar.MONDAY);
							cal.set(Calendar.YEAR, rs.getInt("vt_year"));
							cal.set(Calendar.MONTH, rs.getInt("vt_month")-1);
							cal.set(Calendar.DAY_OF_MONTH, rs.getInt("vt_day"));
							try
							{
								Calendar calendar = Calendar.getInstance();
								calendar.setTime(cal.getTime());
								calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
								calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
								calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
								calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
								Date day = calendar.getTime();

								bts_views.put(day, Integer.valueOf(rs.getInt("views")));
							}
							catch (Exception ex)
							{
								Logger.error(StatGraphNewDB.class,"getTimeData: period allready exist: "+cal.getTime().toString());
								sk.iway.iwcm.Logger.error(ex);
							}
							//Logger.println(this,"------\nVypis RS:\nDocID: "+rs.getInt("doc_id")+"\nTITLE: "+title+"\nDATE: "+rs.getInt("vt_day")+"-"+rs.getInt("vt_month")+"-"+rs.getInt("vt_year")+"\nVIDENI: "+rs.getInt("views"));
							//Logger.println(this,"Calendar: "+cal.getTime());
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

				collection.put(docDB.getBasicDocDetails(docId, true).getTitle() ,bts_views);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return (collection);
	}

	/**
	 * Vytvori casovy graf s ciarami s informaciou o pocte udajov udanych v parametre type v jednotlive dni.
	 * Ak je type nastaveny na withCluster, tak potom sa vytvori graf o pocte sessions pre jednotlive uzly clustera. Inak sa vytvori vseobecna statistika.
	 *
	 * @param rows			zoznam informacii(MonitoringBean) z tabulky monitoring
	 * @param nodeName	Informacia o tom, ze ci sa statistika robi s ohladom na cluster(nodeName = nazov daneho uzla clusteru) alebo nie (nodeName = -1)
	 * @param type			Typ zaznamenanych udajov, pre ktore sa vykresli graf (sessions, cache, dbActive, dbIdle, freeMem, usedMem, totalMem, cpuUsage)
	 * @param request		Poziadavka, z ktorej sa urci aktualne pouzivane Properties
	 *
	 * @return
	 */
	public static Map<String,  Map<Date, Number>> getMonitoringViewsTimeData(List<MonitoringBean> rows, String nodeName, String type, HttpServletRequest request)
	{
		Map<String,  Map<Date, Number>> timeSeriesTable = new Hashtable<>();
		List<String> clusters = new ArrayList<>();
		//Map<Hour, Long> monHours = new Hashtable<Hour, Long>();			//kluc je hodina a hodnota je sucet hodnot typu monitorovacej informacie v danej hodine
		//Map<Hour, Integer> monNumbers = new Hashtable<Hour, Integer>();//kluc je hodina a hodnota je mnozstvo ulozenych informacii v danej hodine, aby sa dal vypocitat avg
		Map<Date, Long> monHours = new Hashtable<>();
		Map<Date, Integer> monNumbers = new Hashtable<>();

		if (Tools.isEmpty(nodeName))
			clusters = ClusterDB.getClusterNodeNamesExpandedAuto();
		else
			clusters.add(nodeName);

		if (clusters.size() < 1)
			clusters.add(Constants.getString("clusterMyNodeName"));

		HashMap<Date, Number> bts_viewsTotal = new HashMap<>();


		for (int i = 0; i < clusters.size(); i++)
		{
			Logger.debug(StatGraphNewDB.class, "Preparing data for node:"+clusters.get(i));

			if (rows != null && rows.size() > 0)
			{
				HashMap<Date, Number> bts_views = new HashMap<>();

				monHours.clear();
				monNumbers.clear();

				for (MonitoringBean monInfo : rows)
				{
					try
					{
						Calendar cal = Calendar.getInstance();
						cal.setTime(monInfo.getDateInsert());
					  	cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
						Date second = cal.getTime();

						//if(!"-1".equals(nodeName))	// Ak je to urcene pre jeden vybrany node
						if (monInfo.getNodeName()!=null && !monInfo.getNodeName().equals(clusters.get(i)))	// ak sa to nerovna prave danemu vybranemu node, ignoruj hodnotu
								continue;	// prejdi na dalsiu informaciu monInfo zo zoznamu

						if ("sessions".equals(type))
						{
							if(rows.size() > NUMBER_OF_VALUES) StatGraphNewDB.updateMaxValueInHashTable(monHours, monInfo, "Sessions");
							else addValue(bts_views, bts_viewsTotal, second, monInfo.getSessions());
						}
						else if ("cache".equals(type))
						{
							if(rows.size() > NUMBER_OF_VALUES) StatGraphNewDB.updateMaxValueInHashTable(monHours, monInfo, "Cache");
							else addValue(bts_views, bts_viewsTotal, second, monInfo.getCache());
						}

						else if ("dbActive".equals(type))
						{
							if(rows.size() > NUMBER_OF_VALUES) StatGraphNewDB.updateMaxValueInHashTable(monHours, monInfo, "DbActive");
							else addValue(bts_views, bts_viewsTotal, second, monInfo.getDbActive());
						}
						else if ("dbIdle".equals(type))
						{
							if(rows.size() > NUMBER_OF_VALUES) StatGraphNewDB.updateMaxValueInHashTable(monHours, monInfo, "DbIdle");
							else addValue(bts_views, bts_viewsTotal, second, monInfo.getDbIdle());
						}

						else if ("freeMem".equals(type))
						{
							if(rows.size() > NUMBER_OF_VALUES) StatGraphNewDB.updateMaxValueInHashTable(monHours, monInfo, "MemFree");
							else addValue(bts_views, bts_viewsTotal, second, monInfo.getMemFree()/1024/1024D);
						}
						else if ("usedMem".equals(type))
						{
							if(rows.size() > NUMBER_OF_VALUES) StatGraphNewDB.updateMaxValueInHashTable(monHours, monInfo, "UsedMem");
							else addValue(bts_views, bts_viewsTotal, second, monInfo.getUsedMem()/1024/1024D);
						}
						else if ("totalMem".equals(type))
						{
							if(rows.size() > NUMBER_OF_VALUES) StatGraphNewDB.updateMaxValueInHashTable(monHours, monInfo, "MemTotal");
							else addValue(bts_views, bts_viewsTotal, second, monInfo.getMemTotal()/1024/1024D);
						}
						else if ("cpuUsage".equals(type))
						{
							if(rows.size() > NUMBER_OF_VALUES) {StatGraphNewDB.addMonInfoToHashTable(monHours, monNumbers, monInfo, "CpuUsage"); }
							else {addValue(bts_views, bts_viewsTotal, second, monInfo.getCpuUsage()); }
						}
						else if ("processUsage".equals(type))
						{
							if(rows.size() > NUMBER_OF_VALUES) {StatGraphNewDB.addMonInfoToHashTable(monHours, monNumbers, monInfo, "ProcessUsage"); }
							else {addValue(bts_views, bts_viewsTotal, second, monInfo.getProcessUsage()); }
						}
					} catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
					}
				}

				//tu sa prerata ak je vela zaznamov na bod na "priemernu" hodnotu
				if (monHours != null && monHours.size() > 0)
				{
					Date tHour = null;
					for (Map.Entry<Date, Long> me : monHours.entrySet())
					{
						try
						{
							tHour = me.getKey();

							//Logger.debug(StatGraphNewDB.class, "tHour="+tHour.toString()+" value="+me.getValue());

							if (type.contains("Mem"))	// ak je udaj vyjadrujuci pamat, treba ho vydelit na zobrazenie MB
								addValue(bts_views, bts_viewsTotal, tHour, (((double)me.getValue() )/1024/1024));
							else if(type.contains("Usage")){
								addValue(bts_views, bts_viewsTotal, tHour, ((double)monHours.get(tHour) / monNumbers.get(tHour)));	//priemerna hodnota pre CpuUsage a ProcessUsage
							} else{
								Logger.debug(null, "addValue");
								addValue(bts_views, bts_viewsTotal, tHour, me.getValue());	//spocita hodnoty v jednotlivych clusteroch

							//System.out.println(tHour + ":" + ((double)monHours.get(tHour) / monNumbers.get(tHour)));
							}
						}
						catch (Exception ex)
						{
							sk.iway.iwcm.Logger.error(ex);
						}
					}
				}

				timeSeriesTable.put(clusters.get(i), bts_views);
				//collection.addSeries(bts_views);
			}

			//if("-1".equals(nodeName))
			//	break;
		}

		if (clusters.size()>1 && bts_viewsTotal!=null) timeSeriesTable.put("Total", bts_viewsTotal);

		return timeSeriesTable;
	}

	/**
	 * V clustri nam bezi iteracia viac krat preto musime hodnoty nascitat
	 * @param bts_views
	 * @param tHour
	 * @param value
	 */
	private static void addValue(HashMap<Date, Number> bts_views, HashMap<Date, Number> bts_viewsTotal, Date tHour, double value)
	{
		Number tmp = bts_views.get(tHour);
		if(tmp!=null)
			bts_views.put(tHour, tmp.doubleValue() + value);
		else
			bts_views.put(tHour, value);

		tmp = bts_viewsTotal.get(tHour);
		if(tmp!=null)
			bts_viewsTotal.put(tHour, tmp.doubleValue() + value);
		else
			bts_viewsTotal.put(tHour, value);
	}

	/**
	 * Funkcia, ktora prislusnu hodnotu z monInfo nacita a porovna s aktualnou max hodnotou v map a ked je mensia aktualizuje
	 *
	 * @param monHours	hash tabulka uchovavajuca si sucet danych hodnot pre jednotlive hodiny
	 * @param monInfo		monitoring bean uchovavajuci si monitorovaciu informaciu
	 * @param meth			metoda, ktora sa bude volat cez reflection na zaklade vstupneho parametra
	 *
	 * @author kmarton
	 */
	@SuppressWarnings("java:S3878")
	private static void updateMaxValueInHashTable(Map<Date, Long> monHours, MonitoringBean monInfo, String meth)
	{
		//Hour hour = new Hour(monInfo.getDateInsert());

		Calendar cal = Calendar.getInstance();
		cal.setTime(monInfo.getDateInsert());
		cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
		cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
	  	cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
		Date hour = cal.getTime();

		try
		{
         Class<MonitoringBean> c = MonitoringBean.class;
         Method m = c.getDeclaredMethod("get" + meth, new Class[]{});	//aby to bolo univerzalne - pre viacero metod -> viacero grafov

         Long actualValue = monHours.get(hour);
			if (actualValue == null)
				monHours.put(hour, ((Number)m.invoke(monInfo, new Object[]{})).longValue());
			else
			{
				long newValue = ((Number)m.invoke(monInfo, new Object[]{})).longValue();
				if (actualValue.longValue() < newValue)
				{
					monHours.put(hour, newValue);
				}
			}
      }
      catch (Exception e)
      {
         sk.iway.iwcm.Logger.error(e);
      }
	}


	/**
	 * Funkcia, ktora prislusnu hodnotu z monInfo nacita pomocou metody meth a ulozi do hash tabulky monHours a zvysi jej pocet v has tabulke monNumbers.
	 *
	 * @param monHours	hash tabulka uchovavajuca si sucet danych hodnot pre jednotlive hodiny
	 * @param monNumbers	hash tabulka uchovavajuca si pocet spocitanych hodnot v danej hodine
	 * @param monInfo		monitoring bean uchovavajuci si monitorovaciu informaciu
	 * @param meth			metoda, ktora sa bude volat cez reflection na zaklade vstupneho parametra
	 *
	 * @author kmarton
	 */
	@SuppressWarnings("java:S3878")
	private static void addMonInfoToHashTable(Map<Date, Long> monHours, Map<Date, Integer> monNumbers, MonitoringBean monInfo, String meth)
	{
		//Hour hour = new Hour(monInfo.getDateInsert());
		Calendar cal = Calendar.getInstance();
		cal.setTime(monInfo.getDateInsert());
		cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
		cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
	  	cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));

		Date hour = cal.getTime();

		try
		{
         Class<MonitoringBean> c = MonitoringBean.class;
         Method m = c.getDeclaredMethod("get" + meth, new Class[]{});

			if (!monHours.containsKey(hour))
				monHours.put(hour, ((Number)m.invoke(monInfo, new Object[]{})).longValue());
			else
			{
				monHours.put(hour, (monHours.get(hour) + ((Number)m.invoke(monInfo, new Object[]{})).longValue()));
			}

			if (!monNumbers.containsKey(hour))
				monNumbers.put(hour, 1);
			else
			{
				monNumbers.put(hour, (monNumbers.get(hour) + 1));
			}
      }
      catch (Exception e)
      {
         sk.iway.iwcm.Logger.error(e);
      }
	}

	/**
	 * Vytvori casovy graf s ciarami, ak docList obsahuje zoznam docID, tak sa vygeneruje graf so statistikou pre
	 * dane docID. V pripade ze je docList prazdny, beru sa do uvahy top stranky.
	 *
	 * @param title
	 * @param rows
	 * @param colIndex
	 * @param type - typ vypisu grafu (momentalne sa vykonava iba vyber top stranok)
	 * @return
	 */
	public static Map<String,  Map<Date, Number>> getViewMonthsTimeData(String title, List<Column> rows, int colIndex, String type, HttpServletRequest request)
	{
		//TimeSeriesCollection collection = new TimeSeriesCollection();
		Map<String,  Map<Date, Number>> timeSeriesTable = new Hashtable<>();
		try
		{

			Prop prop = Prop.getInstance(Constants.getServletContext(), request);
			String[] legend = {prop.getText("stat.graph.viewsTitle"), prop.getText("stat.graph.sessionsTitle"), prop.getText("stat.graph.unique_users")};

			if (rows != null)
			{

				//iteruj po jednotlivych riadkoch
				int value;
				int i = 0;
				Calendar cal = Calendar.getInstance();
				cal.clear();

				for (i=0; i<3; i++)
				{
					//TimeSeries bts_views = new TimeSeries(legend[i]);
					Map<Date, Number> bts_views = new HashMap<>();

					for (Column col : rows)
					{
						cal.set(Calendar.YEAR, col.getIntColumn1());
						cal.set(Calendar.MONTH, col.getIntColumn2()-1);
						cal.set(Calendar.DATE, 15);
						value = col.getIntColumn(i+3);

						try
						{
							//bts_views.add(new Day(new java.util.Date(cal.getTime().getTime())), value);
							bts_views.put(new java.util.Date(cal.getTime().getTime()), value);
						}
						catch (Exception ex)
						{
							Logger.error(StatGraphNewDB.class,"getTimeData: period allready exist: "+cal.getTime().toString());
							sk.iway.iwcm.Logger.error(ex);
						}
						//Logger.println(this,"Calendar: "+cal.getTime());
					}
					//collection.addSeries(bts_views);
					timeSeriesTable.put(legend[i], bts_views);

				}
			}

		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		//return (collection);
		return (timeSeriesTable);
	}
}
