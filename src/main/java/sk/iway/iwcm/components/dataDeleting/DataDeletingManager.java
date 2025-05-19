package sk.iway.iwcm.components.dataDeleting; //NOSONAR

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.StatDB;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.system.ConfDB;

/**
 *	DataDeletingManager.java - vykonava pracu s databazou, maze udaje pre DataDeletingAjaxAction.java
 *	@Title        webjet4
 *	@Company      Interway s.r.o. (www.interway.sk)
 *	@Copyright    Interway s.r.o. (c) 2001-2008
 *	@author       $Author: jeeff $
 *	@version      $Revision: 1.6 $
 *	@created      Date: 26.6.2009 14:55:51
 *	@modified     $Date: 2010/01/20 10:12:54 $
 */

public class DataDeletingManager
{
	private static SimpleQuery query = new SimpleQuery();

	private static Collection<Integer> allowedAdminlogTypes = new ArrayList<>();
	static{
		allowedAdminlogTypes.add(Adminlog.TYPE_SE_SITEMAP);
		allowedAdminlogTypes.add(Adminlog.TYPE_FORMMAIL);
		allowedAdminlogTypes.add(Adminlog.TYPE_SENDMAIL);
		allowedAdminlogTypes.add(Adminlog.TYPE_RUNTIME_ERROR);
		allowedAdminlogTypes.add(Adminlog.TYPE_JSPERROR);
		allowedAdminlogTypes.add(Adminlog.TYPE_SQLERROR);
		allowedAdminlogTypes.add(Adminlog.TYPE_CRON);
		allowedAdminlogTypes.add(Adminlog.TYPE_CLIENT_SPECIFIC);
	}

	private DataDeletingManager() {
		//utiltity class
	}

	/**
 	 * Vymaze vsetky zaznamy v danom casovom obdobi z tabulky table
 	 *
 	 * @param table 		- 	nazov tabulky, z ktorej chceme data vymazat
 	 * @param startDate 	- 	datum zaciatku, od ktoreho chceme data vymazat
 	 * @param endDate		- 	datum konca, do ktoreho chceme vsetko vymazat
	 * @param isActual	informacia o tom, ci sa maju vymazat pri tabulke documents_history vymazat aj aktualne podoby stranky (true - vymazu sa, false - nevymazu sa)
 	 *
 	 * @return -1 ak nastala chyba pri spojeni s databazou, inak pocet vymazanych riadkov
 	 */
	public static int deleteData(String table, Date startDate, Date endDate, boolean isActual, int type)
	{
		return deleteData(table, startDate, endDate, isActual, type, true);
	}

	public static int deleteData(String table, Date startDate, Date endDate, boolean isActual, int type, boolean optimizeTable)
	{
		int retValue = -1;

		Connection db_conn = null;
		PreparedStatement ps = null;

		String sql = DataDeletingManager.getSqlQueryForTable(table, startDate, endDate, "delete", isActual);

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);

			int psCounter = 1;

			/**
			 * posunutie daneho datumu na koniec dna z polnoci na 23:59:59 daneho dna kvoli jeho zahrnutiu do filtra vymazania
			 */
			//System.out.println("DEBUG -- DataDeleting -- STARTDATE: \t" + new Timestamp(startDate.getTime()));
			Calendar cal = new GregorianCalendar();
			cal.setTime(startDate);
			int startDay = cal.get(Calendar.DAY_OF_MONTH);
			int startMonth = cal.get(Calendar.MONTH)+1;
			int startYear = cal.get(Calendar.YEAR);
			cal.setTime(endDate);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			int endDay = cal.get(Calendar.DAY_OF_MONTH);
			int endMonth = cal.get(Calendar.MONTH)+1;
			int endYear = cal.get(Calendar.YEAR);
			//System.out.println("DEBUG -- DataDeleting -- ENDDATE: \t" + new Timestamp(cal.getTime().getTime()));
			//ak to nie je pripad, kedy sql statement vysklada funkcia StatDB.getYearTimeSQL(startDate, endDate, true), tak nahrad otazniky
			if (sql.indexOf("week") == -1)
			{
				if (table.indexOf("stat_clicks") != -1){
					if(("stat_clicks_"+startYear+"_"+startMonth).compareTo(("stat_clicks_"+endYear+"_"+endMonth))==0){	//ak sa jedna o jeden mesiac a rok
						//Logger.debug(null, "Jeden mesiac a rok");
						ps.setInt(psCounter++, startDay);
						ps.setInt(psCounter++, endDay);
					}
					else if(table.compareTo("stat_clicks_"+startYear+"_"+startMonth)==0){	//prvy mesiac
						//Logger.debug(null, "Mazem prvy mesiac v prvom roku: "+sql+startYear+startMonth+startDay);
						ps.setInt(psCounter++, startDay);
						ps.setInt(psCounter++, 32);	//zvysok mesiaca
					}
					else if(table.compareTo("stat_clicks_"+endYear+"_"+endMonth)==0){	//posledny mesiac
						//Logger.debug(null, "mazem posledny mesiac posledneho roku: "+sql+endYear+endMonth+endDay);
						ps.setInt(psCounter++, 0);
						ps.setInt(psCounter++, endDay);
					}
					else { //vsetky ostatne pripady
						//Logger.debug(null, "mazem vsetko medzi");
						ps.setInt(psCounter++, 0);
						ps.setInt(psCounter++, 32);
					}
				} else{
					ps.setTimestamp(psCounter++, new Timestamp(startDate.getTime()));
					ps.setTimestamp(psCounter++, new Timestamp(cal.getTime().getTime()));
				}

			}

			if (sql.indexOf("actual") != -1)
				ps.setBoolean(psCounter++, isActual);

			if(sql.indexOf(ConfDB.ADMINLOG_TABLE_NAME)!= -1){
				//typ adminlogu
				ps.setInt(psCounter++, type);
				table = ConfDB.ADMINLOG_TABLE_NAME;
			}

			if(sql.indexOf("WEBJET_ADMINLOG")!= -1){	//v pripade oracle databazy
				//typ adminlogu
				ps.setInt(psCounter++, type);
				table = "WEBJET_ADMINLOG";
			}

			boolean existTable = true;

			try
			{
				retValue = ps.executeUpdate();

				if (
						(
							table.indexOf("stat_views_") != -1 ||
							table.indexOf("stat_error_") != -1 ||
							table.indexOf("stat_from_") != -1 ||
							table.indexOf("stat_searchengine_") != -1 ||
							table.indexOf("stat_clicks_") != -1
						)
							&& Constants.getBoolean("statEnableTablePartitioning")
					)
				{
	   			if (DataDeletingManager.isTableEmpty(table))
	   			{
	   				Logger.debug(DataDeletingManager.class, "DEBUG: DataDELETING - Dropla sa tabulka  " + table + " -> " + DataDeletingManager.dropTable(table));
	   				Adminlog.add(Adminlog.TYPE_DATA_DELETING, "DataDeleting: Table " + table + " was dropped because it was empty.", -1, -1);
	   				existTable = false;
	   			}
				}
			}
			catch(Exception e)
			{
				if(e.getMessage().indexOf("doesn't exist")!=-1 || e.getMessage().indexOf("not exist")!=-1)
				{
					Logger.debug(DataDeletingManager.class, "DEBUG: DataDELETING - TABULKA " + table + " NEEXISTUJE!!!!");	// nastava jedine pri rozdelovani tabulky stat_views
					return 0;
				}
				else {
					Logger.debug(DataDeletingManager.class, "DEBUG: Neznama chyba!!!!");	// nastava jedine pri rozdelovani tabulky stat_views
					return 0;
				}
			}

			ps.close();

			if(optimizeTable && existTable)
			{
				if(Constants.DB_TYPE == Constants.DB_MYSQL)
				{
					Logger.debug(DataDeletingManager.class, "Optimalizujem mysql tabulku "+table);
					ps = db_conn.prepareStatement("OPTIMIZE TABLE "+table);
					ps.execute();
					ps.close();
				}
				else if(Constants.DB_TYPE == Constants.DB_PGSQL)
				{
					Logger.debug(DataDeletingManager.class, "Optimalizujem pgsql tabulku "+table);
					ps = db_conn.prepareStatement("REINDEX TABLE "+table);
					ps.execute();
					ps.close();
				}
				else if(Constants.DB_TYPE == Constants.DB_MSSQL)
				{
					Logger.debug(DataDeletingManager.class, "Optimalizujem mssql tabulku "+table);
					ps = db_conn.prepareStatement("ALTER INDEX ALL ON "+table+" REORGANIZE");
					ps.execute();
					ps.close();
				}
				else if(Constants.DB_TYPE == Constants.DB_ORACLE)
				{
					Logger.debug(DataDeletingManager.class, "Optimalizujem oracle tabulku "+table);
					ps = db_conn.prepareStatement("alter table "+table+" enable row movement");
					ps.execute();
					ps.close();
					ps = db_conn.prepareStatement("alter table "+table+" shrink space");
					ps.execute();
					ps.close();
					ps = db_conn.prepareStatement("alter table "+table+" disable row movement");
					ps.execute();
					ps.close();
				}

			}

			db_conn.close();

			ps = null;
			db_conn = null;


			/**
			 * Pridanie logu do auditu o vymazani dat
			 */
			if(!table.equals("_adminlog_"))
				Adminlog.add(Adminlog.TYPE_DATA_DELETING, "DataDeleting: Data from " + Tools.formatDate(startDate) + " to " + Tools.formatDate(endDate) + " from table " + table +" were deleted.", -1, -1);
			else{
				Adminlog.add(Adminlog.TYPE_DATA_DELETING, "DataDeleting: Data from " + Tools.formatDate(startDate) + " to " + Tools.formatDate(endDate) + " from table " + table + " of type "+ Prop.getInstance().getText("components.adminlog."+ Integer.toString(type)) +" were deleted.", type, -1);
			}



		}
		catch (SQLException e)
		{
			retValue = -1;
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return retValue;
	}

	/**
	 * Zistuje, ci existuje tabulka tableName
	 *
	 * @param tableName	nazov tabulky
	 *
	 * @return true, ak tabulka existuje, inak vrati false
	 */
	public static boolean existTable(String tableName)
	{
		boolean retValue = false;

		Connection dbConn = null;
		try
		{
			dbConn = DBPool.getConnection();

			DatabaseMetaData dbm = dbConn.getMetaData();
			ResultSet tables = dbm.getTables(null, null, tableName, null);

			if (tables.next())
				retValue = true;
			else
				retValue = false;

			dbConn.close();
			dbConn = null;

		}
		catch (SQLException e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			try
			{
				if (dbConn != null)
					dbConn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
		return retValue;
	}

	/**
	 * Funkcia, ktora vrati spravne sql query pre danu tabulku, kedze nazvy datumov sa v skoro kazdej tabulke lisia (napr. create_date, date_created...).
	 *
	 * @param table		nazov tabulky, z ktorej chceme udaje vymazat
	 * @param startDate	zaciatok obdobia, od ktoreho chceme vymazat udaje
	 * @param endDate		koniec obdobia, do ktoreho chceme vymazat
	 * @param type			urcuje aky typ query to je napr. delete alebo select
	 * @param isActual	informacia o tom, ci sa maju vymazat pri tabulke documents_history vymazat aj aktualne podoby stranky (true - vymazu sa, false - nevymazu sa)
	 *
	 * @return Spravny sql dopyt na danu tabulku, ktorym sa vymazu zvolene udaje.
	 */
	private static String getSqlQueryForTable(String table, Date startDate, Date endDate, String type, boolean isActual)
	{
		StringBuilder sql = new StringBuilder();

		if ("delete".equals(type))
			sql.append("DELETE");
		else
			sql.append("SELECT COUNT(*)");
		if(!table.equals("audit"))
			sql.append(" FROM " + table + " ");
		else
		{
			if(Constants.DB_TYPE == Constants.DB_ORACLE)
			{
				sql.append(" FROM WEBJET_ADMINLOG ");
			}
			else
			{
				sql.append(" FROM ").append(ConfDB.ADMINLOG_TABLE_NAME).append(" ");
			}
		}


		if ("documents_history".equals(table))
		{
			if (isActual)
				sql.append("WHERE date_created BETWEEN ? AND ?");
			else
				sql.append("WHERE date_created BETWEEN ? AND ? AND actual = ?");
		}

		else if ("emails".equals(table))
			sql.append("WHERE sent_date BETWEEN ? AND ?");

		else if ("monitoring".equals(table))
			sql.append("WHERE date_insert BETWEEN ? AND ?");

		else if (table.indexOf("stat_from") != -1)
			sql.append("WHERE from_time BETWEEN ? AND ?");

		else if (table.indexOf("stat_searchengine") != -1)
			sql.append("WHERE search_date BETWEEN ? AND ?");

		else if (table.indexOf("banner_stat_") != -1)
			sql.append("WHERE insert_date BETWEEN ? AND ?");

		else if (table.indexOf("stat_views") != -1)
			sql.append("WHERE view_time BETWEEN ? AND ?");

		else if (table.indexOf("audit") != -1)
		{
			sql.append("WHERE create_date BETWEEN ? AND ? AND log_type = ? ");
		}

		else if (table.indexOf("stat_clicks") != -1)
			sql.append("WHERE day_of_month BETWEEN ? AND ?");
		else
			sql.append(StatDB.getYearTimeSQL(startDate, endDate, true));

		//System.out.println("\n\nDEBUG: sql - data deleting - " + sql + "\n\n");

		return sql.toString();
	}

	/**
	 * Funkcia, ktora zisti rozsah tabuliek stat_views a pre kazdu zavola mazaciu metodu.
	 *
	 * @param startDate	zaciatok obdobia, od ktoreho chceme vymazat udaje
	 * @param endDate		koniec obdobia, do ktoreho chceme vymazat
	 *
	 * @return	Pocet vymazanych riadkov vo vsetkych tabulkach stat_views, ktore zodpovedaju vstupnym datumom.
	 */
	public static int deleteTablePartitioning(Date startDate, Date endDate)
	{
		return deleteTablePartitioning("stat_views", startDate, endDate, false);
	}

	/**
	 * Funkcia, ktora zisti rozsah tabuliek tabulky namePartitioningTable a pre kazdu zavola mazaciu metodu.
	 *
	 * @param namePartitioningTable	nazov rozdelovanej tabulky(zatial sa rozdeluju stat_views, stat_error, stat_searchengine a stat_from)
	 * @param startDate					zaciatok obdobia, od ktoreho chceme vymazat udaje
	 * @param endDate						koniec obdobia, do ktoreho chceme vymazat
	 *
	 * @return	Pocet vymazanych riadkov vo vsetkych tabulkach tabulky namePartitioningTable, ktore zodpovedaju vstupnym datumom.
	 */
	public static int deleteTablePartitioning(String namePartitioningTable, Date startDate, Date endDate, boolean optimizeTable)
	{
		int numberDelRows = 0;

		String[] suffixes = StatNewDB.getTableSuffix(namePartitioningTable, startDate.getTime(), endDate.getTime());
		for (int i = 0; i < suffixes.length; i++){
			numberDelRows += DataDeletingManager.deleteData(namePartitioningTable + suffixes[i], startDate, endDate, false,-1, optimizeTable);
		}

		return numberDelRows;
	}

	/**
	 * Funkcia, ktora zisti rozsah tabuliek stat_views a pre kazdu zavola metodu, ktora zisti pocet poloziek, ktore chce user vymazat.
	 *
	 * @param startDate	zaciatok obdobia, od ktoreho chceme vymazat udaje
	 * @param endDate		koniec obdobia, do ktoreho chceme vymazat
	 *
	 * @return	Pocet riadkov vo vsetkych tabulkach stat_views, ktore zodpovedaju vstupnym datumom a ktore chce user vymazat.
	 */
	public static int checkTablePartitioning(Date startDate, Date endDate)
	{
		return checkTablePartitioning("stat_views", startDate, endDate);
	}

	/**
	 * Funkcia, ktora zisti rozsah tabuliek rozdelovanej tabulky a pre kazdu zavola metodu, ktora zisti pocet poloziek, ktore chce user vymazat.
	 *
	 * @param namePartitioningTable	nazov rozdelovanej tabulky(zatial sa rozdeluju stat_views, stat_error, stat_searchengine a stat_from)
	 * @param startDate					zaciatok obdobia, od ktoreho chceme vymazat udaje
	 * @param endDate						koniec obdobia, do ktoreho chceme vymazat
	 *
	 * @return	Pocet riadkov vo vsetkych tabulkach rozdelovanej tabulky namePartitioningTable, ktore zodpovedaju vstupnym datumom a ktore chce user vymazat.
	 */
	public static int checkTablePartitioning(String namePartitioningTable, Date startDate, Date endDate)
	{
		int numberToDelRows = 0;
		String[] suffixes = StatNewDB.getTableSuffix(namePartitioningTable, startDate.getTime(), endDate.getTime());
		for (int i = 0; i < suffixes.length; i++)
			numberToDelRows += DataDeletingManager.checkData(namePartitioningTable + suffixes[i], startDate, endDate, false, -1);

		return numberToDelRows;
	}

	/**
 	 * Zisti pocty, ktore chce pouzivatel vymazat v danom casovom obdobi z tabulky table
 	 *
 	 * @param table 		- nazov tabulky, z ktorej chceme data vymazat
 	 * @param startDate 	- datum zaciatku, od ktoreho chceme data vymazat
 	 * @param endDate		- datum konca, do ktoreho chceme vsetko vymazat
 	 *
 	 * @return -1 ak nastala chyba pri spojeni s databazou, inak pocet vymazanych riadkov
 	 */
	public static int checkData(String table, Date startDate, Date endDate, boolean isActual, int type)
	{
		String sql = DataDeletingManager.getSqlQueryForTable(table, startDate, endDate, "select", isActual);

		/**
		 * posunutie daneho datumu na koniec dna z polnoci na 23:59:59 daneho dna kvoli jeho zahrnutiu do filtra vymazania
		 */
		//System.out.println("DEBUG -- DataDeleting -- STARTDATE: \t" + new Timestamp(startDate.getTime()));
		Calendar cal = new GregorianCalendar();
		cal.setTime(startDate);
		int startDay = cal.get(Calendar.DAY_OF_MONTH);
		int startMonth = cal.get(Calendar.MONTH)+1;
		int startYear = cal.get(Calendar.YEAR);
		cal.setTime(endDate);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		int endDay = cal.get(Calendar.DAY_OF_MONTH);
		int endMonth = cal.get(Calendar.MONTH)+1;
		int endYear = cal.get(Calendar.YEAR);
		//System.out.println("DEBUG -- DataDeleting -- ENDDATE: \t" + new Timestamp(cal.getTime().getTime()));


		//check if type is allowed
		if(table.equals("audit")){
			if(isTypeAllowed(type))
				return query.forInt(sql, new Timestamp(startDate.getTime()), new Timestamp(cal.getTime().getTime()), type);
			else
				return -1;
		}

		// ak je to pripad kontroly aktualnej podoby stranky v tabulke documents_history
		if (sql.indexOf("actual") != -1)
		{
			//System.out.println("DEBUG: " + sql + "\t" + new Timestamp(startDate.getTime()) + " - " + new Timestamp(cal.getTime().getTime()) + " -- " + isActual);
			return query.forInt(sql, new Timestamp(startDate.getTime()), new Timestamp(cal.getTime().getTime()), Boolean.valueOf(isActual));
		}

		//ak to nie je pripad, kedy sql statement vysklada funkcia StatDB.getYearTimeSQL(startDate, endDate, true), tak nahrad otazniky
		if (sql.indexOf("week") == -1)
		{
			//System.out.println(sql + "\t" + new Timestamp(startDate.getTime()) + " - " + new Timestamp(cal.getTime().getTime()));
			try
			{
				if (table.indexOf("stat_clicks") != -1){
					if(("stat_clicks_"+startYear+"_"+startMonth).compareTo(("stat_clicks_"+endYear+"_"+endMonth))==0){	//ak sa jedna o jeden mesiac a rok
						//Logger.debug(null, "Jeden mesiac a rok");
						return query.forInt(sql, startDay, endDay);
					}
					else if(table.compareTo("stat_clicks_"+startYear+"_"+startMonth)==0){	//prvy mesiac
						//Logger.debug(null, "Overujem prvy mesiac v prvom roku: "+sql+startYear+startMonth+startDay);
						return query.forInt(sql, startDay, 32);
					}
					else if(table.compareTo("stat_clicks_"+endYear+"_"+endMonth)==0){	//posledny mesiac
						//Logger.debug(null, "Overujem posledny mesiac posledneho roku: "+sql+endYear+endMonth+endDay);
						return query.forInt(sql, 0, endDay);
					}
					else { //vsetky ostatne pripady
						//Logger.debug(null, "Overujem vsetko medzi");
						return query.forInt(sql, 0, 32);
					}
				} else{
					return query.forInt(sql, new Timestamp(startDate.getTime()), new Timestamp(cal.getTime().getTime()));
				}
			}
			catch (Exception e)
			{
				return 0;	// tabulka neexistuje, bola vymazana. nastava hlavne pri stats_view pri particiovani
			}
		}

		try
		{
			//System.out.println(sql);
			return query.forInt(sql);
		}
		catch (Exception e)
		{
			return 0;	// tabulka neexistuje, bola vymazana. nastava hlavne pri particiovanych tabulkach
		}
	}

	private static boolean isTypeAllowed(int type)
	{
		return allowedAdminlogTypes.contains(type);
	}

	/**
	 * Zisti, ci tabulka, z ktorej sa prave mazalo je prazdna. Tato funkcia sa moze volat len pre tabulky stat_views v pripade ich rozdelovania - statEnableTablePartitioning = true.
	 *
	 * @param table	tabulka, o ktorej chceme zistit, ci je prazdna
	 * @return	true ak je tabulka prazdna, inak false
	 */
	private static boolean isTableEmpty(String table)
	{
		if (
				(
					table.indexOf("stat_views_") == -1 &&
					table.indexOf("stat_error_") == -1 &&
					table.indexOf("stat_from_") == -1 &&
					table.indexOf("stat_searchengine_") == -1 &&
					table.indexOf("stat_clicks_") != -1
				)
					|| !Constants.getBoolean("statEnableTablePartitioning")
			)
			return false;

		if (query.forInt("SELECT COUNT(*) FROM " + table) == 0)
			return true;

		return false;
	}

	/**
	 * Zmazanie celej tabulky (drop) v pripade, ze je prazdna. Tato funkcia sa moze volat len pre tabulky stat_views v pripade ich rozdelovania - statEnableTablePartitioning = true.
	 *
	 * @param table	tabulka, ktoru chceme vymazat
	 * @return	true ak sa vymazanie podarilo, inak false
	 */
	private static boolean dropTable(String table)
	{
		if (!table.startsWith("stat_views_") || !Constants.getBoolean("statEnableTablePartitioning"))
		{
			if (
				(
					table.indexOf("stat_views_") == -1 &&
					table.indexOf("stat_error_") == -1 &&
					table.indexOf("stat_from_") == -1 &&
					table.indexOf("stat_searchengine_") == -1 &&
					table.indexOf("stat_clicks_") != -1
				)
					|| !Constants.getBoolean("statEnableTablePartitioning")
			) {
				return false;
			}
		}
		try
		{
			query.execute("DROP TABLE " + table);
			return true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			return false;
		}
	}

	/**
	 * Funkcia, ktora vrati list, v ktorom su ulozene informacie o pocte emailov za jednotlive mesiace z rozsahu, ktory si zvolil user.
	 * Ak sa v mesiaci nenachadza ziadny email, tak sa nevrati resp. vrati sa len informacia o tych mesiacoch, v ktorych je pocet emailov >=1.
	 *
	 * @param startDate	zaciatok rozsahu
	 * @param endDate		koniec rozsahu
	 *
	 * @return	list instancii Column, kde v getColumn1() je ulozena informacia o mesiaci v tvare MM/YYYY a v getIntColumn1() samotny pocet emailov v danom obdobi.
	 */
	public static List<Column> getEmailsGroupedByMonth(Date startDate, Date endDate)
	{
		List<Column> emailMonths = new ArrayList<>();

		String sql = "SELECT MONTH(sent_date), YEAR(sent_date), COUNT(*) from emails WHERE sent_date BETWEEN ? AND ?" +
				" GROUP BY MONTH(sent_date), YEAR(sent_date) ORDER BY YEAR(sent_date), MONTH(sent_date)";

		if (Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL)
			sql = "SELECT TO_CHAR(sent_date, 'MM'), TO_CHAR(sent_date, 'YYYY'), COUNT(*) from emails WHERE sent_date BETWEEN ? AND ?" +
			" GROUP BY TO_CHAR(sent_date, 'MM'), TO_CHAR(sent_date, 'YYYY') ORDER BY TO_CHAR(sent_date, 'YYYY'), TO_CHAR(sent_date, 'MM')";


		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			int psCounter = 1;
			ps.setTimestamp(psCounter++, new Timestamp(startDate.getTime()));
			ps.setTimestamp(psCounter++, new Timestamp(endDate.getTime()));
			rs = ps.executeQuery();

			while (rs.next())
			{
				Column col = new Column();

				col.setColumn1(rs.getInt(1) + "/" + rs.getInt(2));
				col.setIntColumn1(rs.getInt(3));

				emailMonths.add(col);
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

		return emailMonths;
	}

	/**
	 * Funkcia, ktora zo ziskaneho List z funkcie getEmailsGroupedByMonth spravi jeden retazec, ktory sa posle spat na ajax request a zobrazi sa v prisluchajucom divku.
	 * Sformatuje vystup do tvaru MM/YYYY a zgrupi do odsekov podla rokov.
	 *
	 * @param emails list instancii Column, kde v getColumn1() je ulozena informacia o mesiaci v tvare MM/YYYY a v getIntColumn1() samotny pocet emailov v danom obdobi.
	 *
	 * @return String v tvare MM/YYYY: 'pocet emailov' <br /> MM/YYYY: 'pocet emailov' <br /> ...
	 */
	public static String getEmailsGroupedString(List<Column> emails)
	{
		StringBuilder retValue = new StringBuilder();
		if (emails == null || emails.size() < 1)
			return retValue.toString();
		String monthEmail = emails.get(0).getColumn1();
		String year = monthEmail.substring(monthEmail.length()-4);
		String delimiter = "";
		int index = 0;

		DecimalFormatSymbols separators = new DecimalFormatSymbols(Locale.getDefault());
		separators.setGroupingSeparator(' ');
		DecimalFormat nf = new DecimalFormat("###,###,###",separators); // kvoli lepsej citatelnosti dlhych cisel, po kazdej tisicke

		for (int i = 0; i < emails.size(); i++)
		{
			index = i + 1;
			if (index >= emails.size())
				index = i;

			monthEmail = emails.get(i).getColumn1();

			if (!year.equals(emails.get(index).getColumn1().substring(emails.get(index).getColumn1().length()-4))) //rozdelenie podla rokov
			{
				year = emails.get(index).getColumn1().substring(emails.get(index).getColumn1().length()-4);
				delimiter = "<br />";
			}

			if (monthEmail.length() == 6)	//zarovnanie na MM/YYYY z M/YYYY
				monthEmail = 0 + monthEmail; //NOSONAR
			retValue.append(monthEmail).append(": ").append(nf.format(emails.get(i).getIntColumn1())).append("<br />").append(delimiter);
			delimiter = "";
		}
		return retValue.toString();
	}
}