package sk.iway.iwcm.stat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.util.ResponseUtils;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConfDB;

/**
 *  StatNewDB.java
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.40 $
 *@created      Date: 10.1.2005 15:13:34
 *@modified     $Date: 2010/01/20 10:13:22 $
 */
@SuppressWarnings({"java:S2479", "java:S1643"})
public class StatNewDB
{

	private static Map<String, String> dateFunc;

	protected StatNewDB() {
		//utility class
	}

	static
	{
		dateFunc = new Hashtable<>();
		dateFunc.put("mysql_hour", "HOUR(field)");
		dateFunc.put("mssql_hour", "DATEPART(hour, field)");
		dateFunc.put("ora_hour",   "TO_CHAR(field, 'HH24')");
		dateFunc.put("pgsql_hour",   "TO_CHAR(field, 'HH24')");
	}

	/**
	 * Vytvori pole suffixov tabuliek v rozsahu zadanych datumov
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 */
	public static String[] getTableSuffix(long dateFrom, long dateTo)
	{
		return getTableSuffix(null, dateFrom, dateTo);
	}

	private static boolean isPartitioningAllowedFor(String tableName)
	{
		return Constants.getBoolean("statEnableTablePartitioning") || "stat_clicks".equals(tableName);
	}

	/**
	 * Vytvori pole suffixov tabuliek v rozsahu zadanych datumov pre zadanu tabulku
	 * @param tableName
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 */
	public static String[] getTableSuffix(String tableName, long dateFrom, long dateTo)
	{
		if (isPartitioningAllowedFor(tableName)==false)
		{
			String[] suffix = new String[1];
			suffix[0] = "";
			return suffix;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(dateFrom);
		cal.set(Calendar.DATE, 1);

		List<String> suffixList = new ArrayList<>();

		if (tableName != null && !"stat_clicks".equals(tableName) && !"stat_views".equals(tableName))
		{
			String convertDate = Constants.getString("statTablePartitioningDate-"+tableName);
			if (Tools.isNotEmpty(convertDate))
			{
				long date = DB.getTimestamp(convertDate);
				if (date > 100000 && date > dateFrom)
				{
					cal.setTimeInMillis(date);
					//v liste musime ponechat aj povodnu tabulku stat_from
					suffixList.add("");
				}
			}
			else
			{
				//na tabulke este nie je vytvorena particia
				String[] suffix = new String[1];
				suffix[0] = "";
				return suffix;
			}
		}

		while (cal.getTimeInMillis() <= dateTo)
		{
			String suffix = "_"+cal.get(Calendar.YEAR)+"_"+(cal.get(Calendar.MONTH)+1);
			suffixList.add(suffix);
			cal.set(Calendar.DATE, 1);
			cal.add(Calendar.MONTH, 1);
		}

		//skonvertuj na pole
		String[] suffixArray = new String[suffixList.size()];
		for (int i=0; i<suffixArray.length; i++)
		{
			suffixArray[i] = suffixList.get(i);
			//Logger.debug(StatNewDB.class, "Suffix ("+tableName+"): "+suffixArray[i]);
		}

		return suffixArray;
	}


	public static String getTableSuffix(String tableName)
	{
		if (isPartitioningAllowedFor(tableName)==false) return "";

		String convertDate = Constants.getString("statTablePartitioningDate-"+tableName);

		if (Tools.isEmpty(convertDate))
		{
			//prave sme to vytvorili
			String actualDateTime = Tools.formatDateTime(Tools.getNow());
			Constants.setString("statTablePartitioningDate-"+tableName, actualDateTime);
			ConfDB.setName("statTablePartitioningDate-"+tableName, actualDateTime);
		}

		Calendar cal = Calendar.getInstance();
		return "_"+cal.get(Calendar.YEAR)+"_"+(cal.get(Calendar.MONTH)+1);
	}

	/**
	 * Skontroluje v popise chyby ci nastala chyba, ze neexistuje partitioning tabulka
	 * ak ano vytvori ju a vrati true, inak vrati false. Takato chyba nastava v statistike
	 * ked je nastavene obdobie, pre ktore este neexistuje vytvorena tabulka.
	 * @param errorMessage
	 * @param suffix
	 * @return
	 */
	public static boolean createStatTablesFromError(String errorMessage, String suffix)
	{
		return createStatTablesFromError(errorMessage, suffix, "stat_views");
	}

	public static boolean createStatTablesFromError(String errorMessage, String suffix, String tableName)
	{
		if (Tools.isEmpty(errorMessage)) return false;
		if (isPartitioningAllowedFor(tableName))
		{
			//ORA-00942 je chybovy kod chybajucej tabulky v Oracle - samotna hlaska moze byt internacionalizovana
			if (errorMessage.contains("Invalid object name") || errorMessage.contains("doesn't exist") || errorMessage.contains("not exist") || errorMessage.contains("ORA-00942"))
			{
				createStatTable(tableName, suffix);
				return true;
			}
		}
		return false;
	}

	/**
	 * Vytvori a nastavi default parametre pre PS - cursor a fetch size (setri pamat)
	 * @param dbConn
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public static PreparedStatement prepareStatement(Connection dbConn, String sql) throws SQLException
	{
		DBPool.setTransactionIsolationReadUNCommited(dbConn);
		PreparedStatement ps = dbConn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

		ps.setFetchSize(1);

		return ps;
	}

	/**
	 * Vrati SQL prikaz pre vytvorenie danej tabulky
	 * @param tableName
	 * @param suffix
	 * @return
	 */
	private static String getCreateStatTableSqlCommand(String tableName, String suffix, int DB_TYPE)
	{
		String procedureName = Constants.getString("statTableCreateProcedureName");
		if (Tools.isNotEmpty(procedureName) && procedureName.length()>2)
		{
			procedureName = Tools.replace(procedureName, "{1}", tableName);
			procedureName = Tools.replace(procedureName, "{2}", suffix);
			return procedureName;
		}

		String sql = null;
		if ("stat_error".equals(tableName))
		{
			if (DB_TYPE == Constants.DB_MYSQL)
			{
				sql = "CREATE TABLE stat_error"+suffix+" ("+
				"year int unsigned NOT NULL,"+
				"week int unsigned NOT NULL,"+
				"url varchar(255),"+
				"query_string varchar(255),"+
				"count int unsigned DEFAULT 0,"+
				"domain_id int unsigned DEFAULT 0,"+
				"KEY i_stat_error"+suffix+" (year, week)"+
				") ENGINE="+Constants.getString("mariaDbDefaultEngine");
			}
			else if (DB_TYPE == Constants.DB_MSSQL || DB_TYPE == Constants.DB_PGSQL)
			{
				sql = "CREATE TABLE stat_error"+suffix+" ("+
				"year int NOT NULL,"+
				"week int NOT NULL,"+
				"url nvarchar(255),"+
				"query_string nvarchar(255),"+
				"count int DEFAULT 0,"+
				"domain_id int DEFAULT 0"+
				");"+
				"CREATE INDEX IX_yw"+suffix+" ON stat_error"+suffix+" (year, week);";
			}
			else if (DB_TYPE == Constants.DB_ORACLE)
			{
				sql = "CREATE TABLE stat_error"+suffix+" ("+
				"year INTEGER NOT NULL,"+
				"week INTEGER NOT NULL,"+
				"url nvarchar2(255),"+
				"query_string nvarchar2(255),"+
				"count INTEGER DEFAULT 0,"+
				"domain_id INTEGER DEFAULT 0"+
				");"+
				"CREATE INDEX IX_ywse"+suffix+" ON stat_error"+suffix+" (year, week);";
			}
		}
		else if ("stat_from".equals(tableName))
		{
			if (DB_TYPE == Constants.DB_MYSQL)
			{
				sql = "CREATE TABLE stat_from"+suffix+" ("+
				"from_id int unsigned NOT NULL AUTO_INCREMENT,"+
				"browser_id bigint unsigned,"+
				"session_id bigint unsigned,"+
				"referer_server_name varchar(255),"+
				"referer_url varchar(255),"+
				"from_time datetime,"+
				"doc_id int,"+
				"group_id int unsigned NOT NULL DEFAULT 0,"+
				"PRIMARY KEY (from_id)"+
				") ENGINE="+Constants.getString("mariaDbDefaultEngine");
			}
			else if (DB_TYPE == Constants.DB_MSSQL || DB_TYPE == Constants.DB_PGSQL)
			{
				sql = "CREATE TABLE stat_from"+suffix+" ("+
				"from_id int NOT NULL identity(1,1),"+
				"browser_id bigint,"+
				"session_id  bigint,"+
				"referer_server_name nvarchar(255),"+
				"referer_url nvarchar(255),"+
				"from_time datetime,"+
				"doc_id int,"+
				"group_id int NOT NULL DEFAULT 0,"+
				"PRIMARY KEY (from_id))";
			}
			else if (DB_TYPE == Constants.DB_ORACLE)
			{
				sql = "CREATE TABLE stat_from"+suffix+" ("+
				"from_id INTEGER NOT NULL,"+
				"browser_id INTEGER,"+
				"session_id  INTEGER,"+
				"referer_server_name nvarchar2(255),"+
				"referer_url nvarchar2(255),"+
				"from_time DATE,"+
				"doc_id INTEGER,"+
				"group_id INTEGER NOT NULL"+
				");"+
				"ALTER TABLE stat_from"+suffix+" add CONSTRAINT pk_stat_from"+suffix+" PRIMARY KEY (from_id);"+
				"CREATE SEQUENCE S_stat_from"+suffix+" START WITH 1;"+
				"CREATE TRIGGER T_stat_from"+suffix+" BEFORE INSERT ON stat_from"+suffix+" "+
				"FOR EACH ROW \n"+
				"	DECLARE \n"+
				"		val INTEGER|\n"+
				"	BEGIN\n"+
				"		IF :new.from_id IS NULL THEN\n"+
				"			SELECT S_stat_from"+suffix+".nextval into val FROM dual|\n"+
				"			:new.from_id:= val|\n"+
				"		END IF|\n"+
				"	END|\n"+
				";";
			}
		}
		else if ("stat_searchengine".equals(tableName))
		{
			if (DB_TYPE == Constants.DB_MYSQL)
			{
				sql = "CREATE TABLE stat_searchengine"+suffix+" ("+
				"search_date datetime NOT NULL, "+
				"server varchar(16) NOT NULL,"+
				"query varchar(64) NOT NULL,"+
				"doc_id int NOT NULL,"+
				"remote_host varchar(255),"+
				"group_id int(10) unsigned NOT NULL"+
				") ENGINE="+Constants.getString("mariaDbDefaultEngine");
			}
			else if (DB_TYPE == Constants.DB_MSSQL || DB_TYPE == Constants.DB_PGSQL)
			{
				sql = "CREATE TABLE stat_searchengine"+suffix+" ("+
				"search_date datetime NOT NULL, "+
				"server nvarchar(16) NOT NULL,"+
				"query nvarchar(64) NOT NULL,"+
				"doc_id int NOT NULL,"+
				"remote_host nvarchar(255),"+
				"group_id int NOT NULL"+
				")";
			}
			else if (DB_TYPE == Constants.DB_ORACLE)
			{
				sql = "CREATE TABLE stat_searchengine"+suffix+" ("+
				"search_date date NOT NULL, "+
				"server nvarchar2(16) NOT NULL,"+
				"query nvarchar2(64) NOT NULL,"+
				"doc_id integer NOT NULL,"+
				"remote_host nvarchar2(255),"+
				"group_id integer NOT NULL"+
				")";
			}
		}
		else if ("stat_views".equals(tableName))
		{
			if (DB_TYPE == Constants.DB_MYSQL)
			{
				sql = "CREATE TABLE stat_views"+suffix+" (" +
				"view_id int unsigned NOT NULL auto_increment," +
				"browser_id bigint unsigned," +
				"session_id bigint unsigned," +
				"doc_id int," +
				"last_doc_id int," +
				"view_time datetime," +
				"group_id int unsigned," +
				"last_group_id int unsigned," +
				"browser_ua_id int, "+
				"platform_id int, "+
				"subplatform_id int,"+
				"country varchar(4), "+
				"PRIMARY KEY  (view_id)" +
				//",KEY i_group_id (group_id)," +
				//"KEY i_doc_id (doc_id)" +
				") ENGINE="+Constants.getString("mariaDbDefaultEngine");
			}
			else if (DB_TYPE == Constants.DB_MSSQL || DB_TYPE == Constants.DB_PGSQL)
			{
				sql = "CREATE TABLE stat_views"+suffix+" ("+
					"view_id int NOT NULL identity(1,1), "+
					"browser_id bigint, "+
					"session_id bigint, "+
					"doc_id int, "+
					"last_doc_id int, "+
					"view_time datetime NULL, "+
					"group_id int," +
					"last_group_id int," +
					"browser_ua_id int, "+
					"platform_id int, "+
					"subplatform_id int,"+
					"country varchar(4), "+
					"PRIMARY KEY (view_id))";
			}
			else if (DB_TYPE == Constants.DB_ORACLE)
			{
				sql = "CREATE TABLE stat_views"+suffix+" (" +
						"view_id INTEGER NOT NULL, " +
						"browser_id INTEGER, " +
						"session_id INTEGER, " +
						"doc_id INTEGER, " +
						"last_doc_id INTEGER, " +
						"view_time DATE NULL," +
						"group_id INTEGER, " +
						"last_group_id INTEGER, " +
						"browser_ua_id INTEGER, "+
						"platform_id INTEGER, "+
						"subplatform_id INTEGER,"+
						"country VARCHAR(4), "+
						"PRIMARY KEY (view_id));" +
						"CREATE SEQUENCE S_stat_views"+suffix+" START WITH 1;" +
						"CREATE TRIGGER T_stat_views"+suffix+" BEFORE INSERT ON stat_views"+suffix+" " +
						"FOR EACH ROW \n" +
						"	DECLARE \n" +
						"	    val INTEGER|\n" +
						"	BEGIN\n" +
						"		IF :new.view_id IS NULL THEN\n" +
						"			SELECT S_stat_views"+suffix+".nextval into val FROM dual| \n" +
						"			:new.view_id:= val|\n" +
						"		END IF|\n" +
						"	END|\n" +
						";";
			}
		}
		else if ("stat_clicks".equals(tableName))
		{
			if (DB_TYPE == Constants.DB_MYSQL)
			{
				sql = "CREATE TABLE stat_clicks"+suffix+" ("+
						"stat_click_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,"+
						"document_id INT,"+
						"x INT,"+
						"y INT,"+
						"day_of_month INT) ENGINE="+Constants.getString("mariaDbDefaultEngine")+";";
			}
			else if (DB_TYPE == Constants.DB_MSSQL || DB_TYPE == Constants.DB_PGSQL)
			{
				sql = "CREATE TABLE stat_clicks"+suffix+" ("+
						"stat_click_id INT identity(1,1) NOT NULL,"+
						"document_id INT,"+
						"x INT,"+
						"y INT,"+
						"day_of_month INT);";
			}
			else if (DB_TYPE == Constants.DB_ORACLE)
			{
				sql = "CREATE TABLE stat_clicks"+suffix+" ("+
							"stat_click_id INT NOT NULL,"+
							"document_id INTEGER,"+
							"x INTEGER,"+
							"y INTEGER,"+
							"day_of_month INTEGER);"+

						"CREATE SEQUENCE S_stat_clicks"+suffix+" START WITH 1;"+
						"	CREATE TRIGGER T_stat_clicks"+suffix+" BEFORE INSERT ON stat_clicks"+suffix+
						"		FOR EACH ROW  "+
						"			DECLARE"+
						" 				val INTEGER|"+
						"			BEGIN"+
						"				IF :new.stat_click_id IS NULL THEN"+
						"				SELECT S_stat_clicks"+suffix+".nextval into val FROM dual|"+
						"					:new.stat_click_id:= val|"+
						"				END IF|"+
						"			END|;";
			}
			sql += "CREATE INDEX to_document_"+suffix+" ON stat_clicks"+suffix+"(document_id);";
		}
		if (DB_TYPE == Constants.DB_PGSQL && sql != null) {
			int i = sql.indexOf("ENGINE=");
			if (i > 0) sql = sql.substring(0, i);

			//remove unsigned
			sql = Tools.replace(sql, "unsigned", "");
			//replace nvarchar
			sql = Tools.replace(sql, "nvarchar", "varchar");
			//update MS SQL identity
			sql = Tools.replace(sql, "identity(1,1)", "GENERATED BY DEFAULT AS IDENTITY");
			//update datetime
			sql = Tools.replace(sql, "datetime", "timestamp");
		}
		return sql;
	}

	/**
	 * Vytvori tabulku stat_views_X_Y (ak treba)
	 * @param db_conn
	 * @param suffix - suffix tabulky, alebo null (vytvori sa suffix pre aktualny mesiac)
	 */
	public static void createStatTable(String tableName, String suffix)
	{
		if (isPartitioningAllowedFor(tableName)==false && suffix == null)
		{
			//podmienka na suffix==null je tu kvoli admin_db_convert.jsp kedy to este nie je nastavene
			//ale suffix sa posiela
			return;
		}

		if (suffix == null)
		{
			Calendar cal = Calendar.getInstance();
			suffix = "_"+cal.get(Calendar.YEAR)+"_"+(cal.get(Calendar.MONTH)+1);
		}

		Logger.debug(StatNewDB.class, "Creating table: "+tableName+suffix);

		String sql = null;
		Connection db_conn = null;
		Statement ps = null;
		try
		{
			db_conn = DBPool.getConnection();

			sql = getCreateStatTableSqlCommand(tableName, suffix, Constants.DB_TYPE);

			if (Tools.isEmpty(sql)) return;

			StringTokenizer st = new StringTokenizer(sql, ";");
			while (st.hasMoreTokens())
			{
				sql = st.nextToken().trim();
				if (Tools.isNotEmpty(sql))
				{
					if (Constants.DB_TYPE == Constants.DB_ORACLE)
					{
						sql = Tools.replace(sql, "|", ";");
					   if (sql.indexOf("TRIGGER")!=-1)
					   {
					   	//sql = sql.replace('\n', ' ');
					   	//sql = sql.replace('\r', ' ');
					   	sql = sql.replace('\t', ' ');
					   }
					}
					try
					{
						Logger.debug(StatNewDB.class, "createStatTable: "+sql);

						ps = db_conn.createStatement();
						ps.execute(sql);
						ps.close();
					}
					catch (SQLException e)
					{
						String message = e.getMessage().toLowerCase();
						if (message != null &&
								(message.indexOf("already")!=-1 ||
								 message.indexOf("duplicate column name")!=-1 ||
								 message.indexOf("is specified more than once")!=-1 ||
								 message.indexOf("duplicate entry")!=-1 ||
								 message.indexOf("already an object")!=-1 ||
								 message.indexOf("tabuľke existuje")!=-1 ||
								 message.indexOf("duplicate key")!=-1 ||
								 message.indexOf("názov už používa existujúci objekt")!=-1 ||
								 message.indexOf("už existuje")!=-1 ||
								 message.indexOf("existujúci objekt")!=-1))
						{
							//uz existuje, je to OK
						}
						else
						{
							Logger.println(StatNewDB.class, "SQL: "+sql);
							sk.iway.iwcm.Logger.error(e);
						}
					}
					finally
					{
					   try
					   {
					      if (ps!=null) ps.close();
					   }
					   catch (Exception ex2)
					   {

					   }
					}
				}
			}

		   if (ps != null) ps.close();
		   if (db_conn != null) db_conn.close();

		   ps = null;
		   db_conn = null;
		}
		catch (SQLException e)
		{
			String message = e.getMessage().toLowerCase();
			if (message != null &&
					(message.indexOf("already")!=-1 ||
					 message.indexOf("duplicate column name")!=-1 ||
					 message.indexOf("is specified more than once")!=-1 ||
					 message.indexOf("duplicate entry")!=-1 ||
					 message.indexOf("already an object")!=-1 ||
					 message.indexOf("tabuľke existuje")!=-1 ||
					 message.indexOf("duplicate key")!=-1 ||
					 message.indexOf("názov už používa existujúci objekt")!=-1 ||
					 message.indexOf("už existuje")!=-1))
			{
				//uz existuje, je to OK
			}
			else
			{
				Logger.println(StatNewDB.class, "SQL: "+sql);
				sk.iway.iwcm.Logger.error(e);
			}
		}
		catch (Exception ex)
		{
		   sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
		   try
		   {
		      if (ps!=null) ps.close();
		      if (db_conn!=null) db_conn.close();
		   }
		   catch (Exception ex2)
		   {

		   }
		}
	}

	/**
	 * Vrati SQL volanie pre vypis vt_day, vt_month a vt_year pre jednotlive DB
	 * @param fieldName
	 * @return
	 */
	public static String getDMYSelect(String fieldName)
	{
		String sql = "DAYOFMONTH("+fieldName+") AS vt_day, MONTH("+fieldName+") AS vt_month, YEAR("+fieldName+") AS vt_year";

		if (Constants.DB_TYPE == Constants.DB_MSSQL)
		{
			sql = "DAY("+fieldName+") AS vt_day, MONTH("+fieldName+") AS vt_month, YEAR("+fieldName+") AS vt_year";
		}
		else if (Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL)
		{
			sql = "TO_CHAR("+fieldName+", 'DD') AS vt_day, TO_CHAR("+fieldName+", 'MM') AS vt_month, TO_CHAR("+fieldName+", 'YYYY') AS vt_year";
		}

		return(sql);
	}

	/**
	 * Vrati SQL volanie do SELECTU pre vypis vt_year a vt_week
	 * @param fieldName
	 * @return
	 */
	public static String getYWSelect(String fieldName)
	{
		String sql = "YEAR("+fieldName+") AS vt_year, WEEKOFYEAR("+fieldName+") AS vt_week";

		if (Constants.DB_TYPE == Constants.DB_MSSQL)
		{
			sql = "YEAR("+fieldName+") AS vt_year, DATEPART(week, "+fieldName+") AS vt_week";
		}
		else if (Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL)
		{
			sql = "TO_CHAR("+fieldName+", 'YYYY') AS vt_year, TO_CHAR("+fieldName+", 'IW') AS vt_week";
		}

		return(sql);
	}

	public static String getYMSelect(String fieldName)
	{
		String sql = "YEAR("+fieldName+") AS vt_year, MONTH("+fieldName+") AS vt_month";

		if (Constants.DB_TYPE == Constants.DB_MSSQL)
		{
			sql = "YEAR("+fieldName+") AS vt_year, MONTH("+fieldName+") AS vt_month";
		}
		else if (Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL)
		{
			sql = "TO_CHAR("+fieldName+", 'YYYY') AS vt_year, TO_CHAR("+fieldName+", 'MM') AS vt_month";
		}

		return(sql);
	}

	/**
	 * Vrati SQL pre datum pre rozne DB
	 * @param type
	 * @param fieldName
	 * @param as
	 * @return
	 */
	public static String getDateSelect(String type, String fieldName, String as)
	{
		String db="mysql";
		if (Constants.DB_TYPE == Constants.DB_MSSQL) db = "mssql";
		else if (Constants.DB_TYPE == Constants.DB_ORACLE) db = "ora";
		else if (Constants.DB_TYPE == Constants.DB_PGSQL) db = "pgsql";

		String sql = dateFunc.get(db+"_"+type);
		sql = Tools.replace(sql, "field", fieldName);
		if (as != null) sql = sql + " AS " + as;
		return(sql);
	}

	public static String getDateGroupBy(String type, String fieldName, String as)
	{
		String db = null;
		if (Constants.DB_TYPE == Constants.DB_MSSQL) db = "mssql";
		else if (Constants.DB_TYPE == Constants.DB_ORACLE) db = "ora";
		else if (Constants.DB_TYPE == Constants.DB_PGSQL) db = "pgsql";
		else
		{
			//mysql
			return(as);
		}

		String sql = dateFunc.get(db+"_"+type);
		sql = Tools.replace(sql, "field", fieldName);
		return(sql);
	}

	public static String getDMYGroupBy(String fieldName)
	{
		String sql = "vt_day, vt_month, vt_year";

		if (Constants.DB_TYPE == Constants.DB_MSSQL)
		{
			sql = "DAY("+fieldName+"), MONTH("+fieldName+"), YEAR("+fieldName+")";
		}
		else if (Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL)
		{
			sql = "TO_CHAR("+fieldName+", 'DD'), TO_CHAR("+fieldName+", 'MM'), TO_CHAR("+fieldName+", 'YYYY')";
		}

		return(sql);
	}

	public static String getYWGroupBy(String fieldName)
	{
		String sql = "vt_year, vt_week";

		if (Constants.DB_TYPE == Constants.DB_MSSQL)
		{
			sql = "YEAR("+fieldName+"), DATEPART(week, "+fieldName+")";
		}
		else if (Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL)
		{
			sql = "TO_CHAR("+fieldName+", 'YYYY'), TO_CHAR("+fieldName+", 'IW')";
		}

		return(sql);
	}

	public static String getYMGroupBy(String fieldName)
	{
		String sql = "vt_year, vt_month";

		if (Constants.DB_TYPE == Constants.DB_MSSQL)
		{
			sql = "YEAR("+fieldName+"), MONTH("+fieldName+")";
		}
		else if (Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_PGSQL)
		{
			sql = "TO_CHAR("+fieldName+", 'YYYY'), TO_CHAR("+fieldName+", 'MM')";
		}

		return(sql);
	}


	public static List<Column> getTopPages(int max_size, java.util.Date from, java.util.Date to, int rootGroupId, String skipDocIds)
	{
		return getTopPages(max_size, from, to, rootGroupId, skipDocIds, false);
	}

	/**
	 * Vygeneruje tabulku videni, sedeni, roznych userov pre top stranky za zvolene obdobie
	 *
	 * @param max_size
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @param skipDocIds
	 * @return
	 */
	public static List<Column> getTopPages(int max_size, java.util.Date from, java.util.Date to, int rootGroupId, String skipDocIds, boolean withoutBots)
	{
		//pri rozdeleni statistik nemusi byt presne z dovodu max_size citaneho podla prveho obdobia

		List<Column> ret = new ArrayList<>();

		DebugTimer timer = new DebugTimer("StatNewDB.getTopPages");

		Column col;
		int count = 0;
		GroupsDB groupsDB = GroupsDB.getInstance();
		int docId;
		DocDB docDB = DocDB.getInstance();
		DocDetails bdd;

		Map<Integer, Column> colTable = new Hashtable<>();

		String whitelistedQuery = "";
		if(withoutBots)
			whitelistedQuery = getWhiteListedUAQuery();

		String[] suffixes = getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			count = 0;

			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try
			{
				db_conn = DBPool.getConnectionReadUncommited();

				//pocet sedeni
				//sql = "SELECT DISTINCT doc_id, COUNT(DISTINCT session_id) AS pocet_sedeni FROM stat_ views GROUP BY doc_id ORDER BY pocet_sedeni DESC";

				//pocet videni
				//sql = "SELECT s.doc_id, d.title, d.group_id, count(view_time) as pocet_videni FROM stat_ views s, documents d WHERE s.doc_id=d.doc_id GROUP BY s.doc_id ORDER BY pocet_videni DESC";

				//spolocny vypis

				//SQL robime na 2x - najskor zoznam a az potom join na documents, inak to v MySQL pri velkom pocte zaznamov trvalo strasne dlho
				String sql = "SELECT s.doc_id, COUNT(s.doc_id) as views, COUNT(DISTINCT s.session_id) AS sessions," +
						" COUNT(DISTINCT s.browser_id) AS unique_users" +
						" FROM stat_views"+suffixes[s]+" s" +
						" WHERE s.view_time>=? AND s.view_time<=?";

				/*sql = "SELECT s.doc_id, s.group_id, COUNT(s.doc_id) as views, COUNT(s.session_id) AS sessions," +
				" COUNT(s.browser_id) AS unique_users" +
				" FROM stat_ views s" +
				" WHERE s.view_time>=? AND s.view_time<=?";*/

				if (skipDocIds != null && skipDocIds.length() > 0)
				{
					sql += " AND s.doc_id NOT IN ("+skipDocIds+") ";
				}
				sql += StatDB.getRootGroupWhere("s.group_id", rootGroupId);
				sql += whitelistedQuery;
				sql += " GROUP BY s.doc_id";
				sql += " ORDER BY views DESC";

				Logger.debug(StatNewDB.class,"GetTopPages: "+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));

				timer.diff("before RS");
				rs = ps.executeQuery();
				timer.diff("after RS");

				//iteruj cez riadky
				while (rs.next() && count < max_size)
				{
					docId = rs.getInt("doc_id");
					col = colTable.get(docId);
					if (col == null)
					{
						col = new Column();
						col.setColumn1(Integer.toString(docId));
						bdd = docDB.getBasicDocDetails(docId, true);
						col.setColumn2(bdd.getTitle());
						col.setIntColumn3(rs.getInt("views"));
						col.setIntColumn4(rs.getInt("sessions"));
						col.setIntColumn5(rs.getInt("unique_users"));
						col.setColumn6(groupsDB.getPath(bdd.getGroupId())+"/"+col.getColumn2());
						col.setColumn7("/apps/stat/admin/top-details/?docId="+docId+"&title="+Tools.URLEncode(col.getColumn6()));//link
						ret.add(col);
						colTable.put(docId, col);
					}
					else
					{
						//zvys hodnoty
						col.setIntColumn3(col.getIntColumn3()+rs.getInt("views"));
						col.setIntColumn4(col.getIntColumn4()+rs.getInt("sessions"));
						col.setIntColumn5(col.getIntColumn5()+rs.getInt("unique_users"));
					}
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

		//usporiadaj podla poctu videni
		Collections.sort(ret, new Comparator<Column>() {
			@Override
			public int compare(Column c1, Column c2)
			{
				return (c2.getIntColumn3() - c1.getIntColumn3());
			}

		});

		timer.diff("done");

		//orez to na max pocet zaznamov
		List<Column> ret2 = new ArrayList<>();
		count = 0;
		Iterator<Column> iter = ret.iterator();
		while (iter.hasNext() && count++<max_size)
		{
			ret2.add(iter.next());
		}

		return (ret2);
	}

	public static List<Column> getTopPagesIn(int max_size, java.util.Date from, java.util.Date to, int rootGroupId, String skipDocIds)
	{
		return getTopPagesIn(max_size, from, to, rootGroupId, skipDocIds, false);
	}

	/**
	 * Vrati zoznam nanavstevovanejsich vstupnych stranok
	 * @param max_size
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @param skipDocIds
	 * @return
	 */
	public static List<Column> getTopPagesIn(int max_size, java.util.Date from, java.util.Date to, int rootGroupId, String skipDocIds, boolean withoutBots)
	{
		//pri rozdeleni statistik nemusi byt presne z dovodu max_size citaneho podla prveho obdobia

		List<Column> ret = new ArrayList<>();

		DebugTimer timer = new DebugTimer("StatNewDB.getTopPagesIn");

		Column col;
		int count = 0;
		GroupsDB groupsDB = GroupsDB.getInstance();
		int docId;
		DocDB docDB = DocDB.getInstance();
		DocDetails bdd;

		Map<Integer, Column> colTable = new Hashtable<>();

		String whitelistedQuery = "";
		if(withoutBots)
			whitelistedQuery = getWhiteListedUAQuery();

		String[] suffixes = getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			count = 0;

			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try
			{
				db_conn = DBPool.getConnectionReadUncommited();

				//pocet sedeni
				//sql = "SELECT DISTINCT doc_id, COUNT(DISTINCT session_id) AS pocet_sedeni FROM stat_ views GROUP BY doc_id ORDER BY pocet_sedeni DESC";

				//pocet videni
				//sql = "SELECT s.doc_id, d.title, d.group_id, count(view_time) as pocet_videni FROM stat_ views s, documents d WHERE s.doc_id=d.doc_id GROUP BY s.doc_id ORDER BY pocet_videni DESC";

				//spolocny vypis

				//SQL robime na 2x - najskor zoznam a az potom join na documents, inak to v MySQL pri velkom pocte zaznamov trvalo strasne dlho
				String sql = "SELECT s.doc_id, COUNT(s.doc_id) as views, COUNT(DISTINCT s.session_id) AS sessions," +
						" COUNT(DISTINCT s.browser_id) AS unique_users" +
						" FROM stat_views"+suffixes[s]+" s" +
						" WHERE s.view_time>=? AND s.view_time<=? AND last_doc_id=-1";

				/*sql = "SELECT s.doc_id, s.group_id, COUNT(s.doc_id) as views, COUNT(s.session_id) AS sessions," +
				" COUNT(s.browser_id) AS unique_users" +
				" FROM stat_ views s" +
				" WHERE s.view_time>=? AND s.view_time<=?";*/

				if (skipDocIds != null && skipDocIds.length() > 0)
				{
					sql += " AND s.doc_id NOT IN ("+skipDocIds+") ";
				}
				sql += StatDB.getRootGroupWhere("s.group_id", rootGroupId);
				sql += whitelistedQuery;
				sql += " GROUP BY s.doc_id";
				sql += " ORDER BY views DESC";

				Logger.debug(StatNewDB.class,"GetTopPages: "+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));

				timer.diff("before RS");
				rs = ps.executeQuery();
				timer.diff("after RS");

				//iteruj cez riadky
				while (rs.next() && count < max_size)
				{
					docId = rs.getInt("doc_id");
					col = colTable.get(docId);
					if (col == null)
					{
						col = new Column();
						col.setColumn1(Integer.toString(docId));
						bdd = docDB.getBasicDocDetails(docId, true);
						col.setColumn2(bdd.getTitle());
						col.setIntColumn3(rs.getInt("views"));
						col.setIntColumn4(rs.getInt("sessions"));
						col.setIntColumn5(rs.getInt("unique_users"));
						col.setColumn6(groupsDB.getPath(bdd.getGroupId())+"/"+col.getColumn2());
						col.setColumn7("/apps/stat/admin/top-details/?docId="+docId+"&title="+Tools.URLEncode(col.getColumn6()));//link
						ret.add(col);
						colTable.put(docId, col);
					}
					else
					{
						//zvys hodnoty
						col.setIntColumn3(col.getIntColumn3()+rs.getInt("views"));
						col.setIntColumn4(col.getIntColumn4()+rs.getInt("sessions"));
						col.setIntColumn5(col.getIntColumn5()+rs.getInt("unique_users"));
					}
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

		//usporiadaj podla poctu videni
		Collections.sort(ret, new Comparator<Column>() {
			@Override
			public int compare(Column c1, Column c2)
			{
				return (c2.getIntColumn3() - c1.getIntColumn3());
			}

		});

		timer.diff("done");

		//orez to na max pocet zaznamov
		List<Column> ret2 = new ArrayList<>();
		count = 0;
		Iterator<Column> iter = ret.iterator();
		while (iter.hasNext() && count++<max_size)
		{
			ret2.add(iter.next());
		}

		return (ret2);
	}

	public static List<Column> getTopPagesOut(int max_size, java.util.Date from, java.util.Date to, int rootGroupId, String skipDocIds)
	{
		return getTopPagesOut(max_size, from, to, rootGroupId, skipDocIds, false);
	}

	/**
	 * Ziska z databazy zoznam stranok, ktore boli posledne vramci session
	 * @param max_size
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @param skipDocIds
	 * @return
	 */
	public static List<Column> getTopPagesOut(int max_size, java.util.Date from, java.util.Date to, int rootGroupId, String skipDocIds, boolean withoutBots)
	{
		DebugTimer timer = new DebugTimer("StatNewDB.getTopPagesOut");

		Map<Long, Integer> sessionsTable = new Hashtable<>();

		String whitelistedQuery = "";
		if(withoutBots)
			whitelistedQuery = getWhiteListedUAQuery();

		String[] suffixes = getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try
			{
				db_conn = DBPool.getConnectionReadUncommited();

				String sql = "SELECT session_id, doc_id" +
						" FROM stat_views"+suffixes[s]+" s" +
						" WHERE s.view_time>=? AND s.view_time<=? AND last_doc_id=-1";

				if (skipDocIds != null && skipDocIds.length() > 0)
				{
					sql += " AND s.doc_id NOT IN ("+skipDocIds+") ";
				}
				sql += StatDB.getRootGroupWhere("s.group_id", rootGroupId);
				sql += whitelistedQuery;
				sql += " ORDER BY view_id ASC";

				Logger.debug(StatNewDB.class,"GetTopPagesOut: "+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));

				timer.diff("before RS");
				rs = ps.executeQuery();
				timer.diff("after RS");

				//iteruj cez riadky
				while (rs.next())
				{
					Long sessionId = rs.getLong("session_id");
					int docId = rs.getInt("doc_id");

					sessionsTable.put(sessionId, docId);
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

		GroupsDB groupsDB = GroupsDB.getInstance();
		DocDB docDB = DocDB.getInstance();
		DocDetails bdd;

		//v hashtable mame zoznam vsetkych objektov, treba spocitat podla poctu jednotlivych kusov
		Map<Integer, Column> colTable = new Hashtable<>();
		List<Column> ret = new ArrayList<>();
		Column col;
		for (Map.Entry<Long, Integer> entry : sessionsTable.entrySet())
		{
			Integer docId = entry.getValue();

			col = colTable.get(docId);
			if (col == null)
			{
				col = new Column();
				col.setColumn1(Integer.toString(docId));
				bdd = docDB.getBasicDocDetails(docId, true);
				col.setColumn2(bdd.getTitle());
				col.setIntColumn5(1);
				col.setColumn6(groupsDB.getPath(bdd.getGroupId())+"/"+col.getColumn2());
				col.setColumn7("/apps/stat/admin/top-details/?docId="+docId+"&title="+Tools.URLEncode(col.getColumn6()));//link
				ret.add(col);
				colTable.put(docId, col);
			}
			else
			{
				//zvys hodnoty
				col.setIntColumn5(col.getIntColumn5()+1);
			}
		}

		//usporiadaj podla poctu videni
		Collections.sort(ret, new Comparator<Column>() {
			@Override
			public int compare(Column c1, Column c2)
			{
				return (c2.getIntColumn5() - c1.getIntColumn5());
			}

		});

		timer.diff("done");

		//orez to na max pocet zaznamov
		List<Column> ret2 = new ArrayList<>();
		int count = 0;
		Iterator<Column> iter = ret.iterator();
		while (iter.hasNext() && count++<max_size)
		{
			ret2.add(iter.next());
		}

		return (ret2);
	}

	/**
	 * Vygeneruje pre zvolene docID tabulku videni, sedeni, roznych userov za zvolene obdobie
	 * podla DNI
	 *
	 * @param docId
	 * @param from
	 * @param to
	 * @return
	 */
	public static List<Column> getPageViews(int docId, java.util.Date from, java.util.Date to)
	{
		return getPageViews(docId, from, to, -1);
	}

	/**
	 * Vygeneruje pre zvolene docID tabulku videni, sedeni, roznych userov za zvolene obdobie
	 * podla DNI a podla referera
	 *
	 * @param docId
	 * @param from
	 * @param to
	 * @param lastDocId - referer, ak je nastavene na -1, neberie sa do uvahy
	 * @return
	 */
	public static List<Column> getPageViews(int docId, java.util.Date from, java.util.Date to, int lastDocId)
	{
		List<Column> ret = new ArrayList<>();
		if (docId < 1) return ret;

		String[] suffixes = getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();
				String sql = "SELECT s.doc_id, "+StatNewDB.getDMYSelect("view_time")+", COUNT(s.doc_id) AS views, COUNT(DISTINCT s.session_id) AS sessions, COUNT(DISTINCT s.browser_id) AS unique_users";
				sql +=" FROM stat_views"+suffixes[s]+" s WHERE s.doc_id=? AND s.view_time>=? AND s.view_time<=?";
				if(lastDocId != -1)
				{
					sql += " AND s.last_doc_id=?";
				}
				sql+=	" GROUP BY s.doc_id, "+StatNewDB.getDMYGroupBy("view_time")+" ORDER BY vt_year, vt_month, vt_day";

				Logger.debug(StatNewDB.class, "getPageViews: "+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setInt(1,docId);
				ps.setTimestamp(2, new Timestamp(from.getTime()));
				ps.setTimestamp(3, new Timestamp(to.getTime()));
				if(lastDocId != -1)
				{
					ps.setInt(4,lastDocId);
				}
				rs = ps.executeQuery();

				//iteruj cez riadky
				Column col;
				while (rs.next())
				{
					col = new Column();
					col.setIntColumn1(rs.getInt("vt_year"));
					col.setIntColumn2(rs.getInt("vt_month"));
					col.setIntColumn3(rs.getInt("vt_day"));
					col.setIntColumn4(rs.getInt("views"));
					col.setIntColumn5(rs.getInt("sessions"));
					col.setIntColumn6(rs.getInt("unique_users"));
					ret.add(col);
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

	/**
	 * Vrati statistiku podla tyzdnov za dane obdobie a adresar
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @return
	 */
	public static List<Column> getWeekViews(java.util.Date from, java.util.Date to, int rootGroupId)
	{
		return getWeekViews(from, to, String.valueOf(rootGroupId), false);
	}

	public static List<Column> getWeekViews(java.util.Date from, java.util.Date to, int rootGroupId, boolean withoutBots)
	{
		return getWeekViews(from, to, String.valueOf(rootGroupId), withoutBots);
	}

	public static List<Column> getWeekViews(java.util.Date from, java.util.Date to, String groupIds)
	{
		return getWeekViews(from, to, groupIds, false);
	}

	/**
	 * Vrati statistiku podla tyzdnov za dane obdobie a adresar
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @param groupIds
	 * @return
	 */
	public static List<Column> getWeekViews(java.util.Date from, java.util.Date to, String groupIds, boolean withoutBots)
	{
		List<Column> ret = new ArrayList<>();
		Map<String, Column> colTable = new Hashtable<>();

		String whitelistedQuery = "";
		if(withoutBots)
			whitelistedQuery = getWhiteListedUAQuery();

		String[] suffixes = getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String sql = "SELECT " + getYWSelect("s.view_time")+", ";

				sql += " COUNT(s.doc_id) AS views, COUNT(DISTINCT s.session_id) AS sessions, COUNT(DISTINCT s.browser_id) AS unique_users" +
			 			" FROM stat_views"+suffixes[s]+" s" +
						" WHERE s.view_time>=? AND s.view_time<=?";

				sql += StatDB.getRootGroupWhere("s.group_id", groupIds);
				sql += whitelistedQuery;

				sql += " GROUP BY "+getYWGroupBy("s.view_time");

				//sql += " ORDER BY  vt_year ASC, vt_week ASC";

				Logger.debug(StatNewDB.class,"getViews: "+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));

				rs = ps.executeQuery();
				//iteruj cez riadky
				Column col;

				while (rs.next())
				{
					int year = rs.getInt("vt_year");
					int week = rs.getInt("vt_week");
					String key = year+"-"+week;
					col = colTable.get(key);
					if (col == null)
					{
						col = new Column();
						col.setIntColumn1(year);
						col.setIntColumn2(week);
						col.setIntColumn3(rs.getInt("views"));
						col.setIntColumn4(rs.getInt("sessions"));
						col.setIntColumn5(rs.getInt("unique_users"));

						//spatna kompatibilita
						col.setColumn1(Integer.toString(col.getIntColumn1()));
						col.setColumn2(Integer.toString(col.getIntColumn2()));
						col.setColumn3(Integer.toString(col.getIntColumn3()));
						col.setColumn4(Integer.toString(col.getIntColumn4()));
						col.setColumn5(Integer.toString(col.getIntColumn5()));
						ret.add(col);
						colTable.put(key, col);
					}
					else
					{
						col.setIntColumn3(col.getIntColumn3()+rs.getInt("views"));
						col.setIntColumn4(col.getIntColumn4()+rs.getInt("sessions"));
						col.setIntColumn5(col.getIntColumn5()+rs.getInt("unique_users"));

						col.setColumn3(Integer.toString(col.getIntColumn3()));
						col.setColumn4(Integer.toString(col.getIntColumn4()));
						col.setColumn5(Integer.toString(col.getIntColumn5()));
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

	/**
	 * Vrati statistiku podla dni za dane obdobie
	 *
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @return
	 */
	public static List<Column> getDayViews(java.util.Date from, java.util.Date to, int rootGroupId)
	{
		return getDayViews(from, to, String.valueOf(rootGroupId));
	}

	public static List<Column> getDayViews(java.util.Date from, java.util.Date to, int rootGroupId, boolean filterBotsOut)
	{
		return getDayViews(from, to, String.valueOf(rootGroupId), filterBotsOut);
	}

	public static List<Column> getDayViews(java.util.Date from, java.util.Date to, String groupIds)
	{
		return getDayViews(from, to, groupIds, false);
	}

	/**
	 * Vrati statistiku podla dni za dane obdobie
	 *
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @param groupIds
	 * @return
	 */
	public static List<Column> getDayViews(java.util.Date from, java.util.Date to, String groupIds, boolean withoutBots)
	{
		List<Column> ret = new ArrayList<>();
		Integer[] data;
		Map<String, Integer[]> dates = new Hashtable<>();

		Calendar cal = Calendar.getInstance();
		Column col;

		String whitelistedQuery = "";
		if(withoutBots)
			whitelistedQuery = getWhiteListedUAQuery();

		String[] suffixes = getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String sql = "SELECT "+getDMYSelect("s.view_time")+", COUNT(s.doc_id) AS views, COUNT(DISTINCT s.session_id) AS sessions, COUNT(DISTINCT s.browser_id) AS unique_users";

				sql += " FROM stat_views"+suffixes[s]+" s" +
						" WHERE s.view_time >= ? AND s.view_time <= ? ";

				sql += StatDB.getRootGroupWhere("s.group_id", groupIds);
				sql += whitelistedQuery;

				sql += " GROUP BY "+getDMYGroupBy("s.view_time");
				//sql += " ORDER BY vt_year, vt_month, vt_day"; - nepotrebujeme

				Logger.debug(StatNewDB.class,"getDayViews: "+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));

				rs = ps.executeQuery();
				//iteruj cez riadky
				while (rs.next())
				{
					cal.clear();
					cal.setFirstDayOfWeek(Calendar.MONDAY);
					cal.set(Calendar.YEAR, rs.getInt("vt_year"));
					cal.set(Calendar.MONTH, rs.getInt("vt_month")-1);
					cal.set(Calendar.DAY_OF_MONTH, rs.getInt("vt_day"));

					data = new Integer[3];
					data[0] = Integer.valueOf(rs.getInt("views"));
					data[1] = Integer.valueOf(rs.getInt("sessions"));
					data[2] = Integer.valueOf(rs.getInt("unique_users"));
					//Logger.debug(StatNewDB.class, "Nastavujem: " + Tools.formatDate(cal.getTime()));
					dates.put(Tools.formatDate(cal.getTime()), data);
					//Logger.debug(this,"Data: "+data[0]+" "+data[1]+" "+data[2]);
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

		try
		{
			//Logger.debug(this,"Hashtable: "+dates.size()+ "   Count: "+count);

			Calendar calTo = Calendar.getInstance();
			calTo.setFirstDayOfWeek(Calendar.MONDAY);
			calTo.setTime(to);

			cal.clear();
			cal.setFirstDayOfWeek(Calendar.MONDAY);
			cal.setTimeInMillis(from.getTime());

			//Logger.debug(this,"Date: "+cal.getTime());

			while (cal.getTimeInMillis() <= calTo.getTimeInMillis())
			{
				//Logger.debug(StatNewDB.class, "Ziskavam: " + Tools.formatDate(cal.getTime()));
				data = dates.get(Tools.formatDate(cal.getTime()));
				//Logger.debug(this,"Datum: "+cal.getTime()+"  Data: "+dates.get(cal.getTime()));
				if (data != null)
				{
					col = new Column();
					col.setDateColumn1(cal.getTime());
					col.setIntColumn2(data[0].intValue());
					col.setIntColumn3(data[1].intValue());
					col.setIntColumn4(data[2].intValue());
					ret.add(col);
				}
				else
				{
					col = new Column();
					col.setDateColumn1(cal.getTime());
					col.setIntColumn2(0);
					col.setIntColumn3(0);
					col.setIntColumn4(0);
					ret.add(col);
				}

				cal.add(Calendar.DAY_OF_YEAR, 1);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return (ret);
	}

	/**
	 * Vrati statistiku podla hodin za dane obdobie
	 *
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @return
	 */
	public static List<Column> getHours(java.util.Date from, java.util.Date to, int rootGroupId)
	{
		return getHours(from, to, String.valueOf(rootGroupId), false);
	}

	public static List<Column> getHours(java.util.Date from, java.util.Date to, int rootGroupId, boolean withoutBots)
	{
		return getHours(from, to, String.valueOf(rootGroupId), withoutBots);
	}

	public static List<Column> getHours(java.util.Date from, java.util.Date to, String groupIds)
	{
		return getHours(from, to, groupIds, false);
	}

	/**
	 * Vrati statistiku podla hodin za dane obdobie
	 *
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @param groupIds
	 * @return
	 */
	public static List<Column> getHours(java.util.Date from, java.util.Date to, String groupIds, boolean withoutBots)
	{
		List<Column> ret = new ArrayList<>();
		int[][] data = new int[24][3];
		int i;
		int j;

		for (i = 0; i < 24; i++)
		{
			for (j = 0; j < 3; j++)
			{
				data[i][j] = 0;
			}
		}

		Column col;

		String whitelistedQuery = "";
		if(withoutBots)
			whitelistedQuery = getWhiteListedUAQuery();

		String[] suffixes = getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String sql = "SELECT "+getDateSelect("hour", "s.view_time", "vt_hour")+", COUNT(s.doc_id) AS views, COUNT(DISTINCT s.session_id) AS sessions, COUNT(DISTINCT s.browser_id) AS unique_users";

				sql+=	" FROM stat_views"+suffixes[s]+" s" +
						" WHERE s.view_time>=? AND s.view_time<=?";

				sql += StatDB.getRootGroupWhere("s.group_id", groupIds);
				sql += whitelistedQuery;

				sql += " GROUP BY "+getDateGroupBy("hour", "s.view_time", "vt_hour");
				//sql += " ORDER BY  vt_hour";

				//Logger.debug(this,"GetTopPages: "+sql);
				Logger.debug(StatNewDB.class, "getHours sql: "+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));

				rs = ps.executeQuery();
				//iteruj cez riadky

				while (rs.next())
				{
					int vtHour = rs.getInt("vt_hour");
					if (vtHour >= 0 && vtHour < 24)
					{
						data[vtHour][0] = data[vtHour][0] + rs.getInt("views");
						data[vtHour][1] = data[vtHour][1] + rs.getInt("sessions");
						data[vtHour][2] = data[vtHour][2] + rs.getInt("unique_users");
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

		try
		{
			for (i = 0; i < 24; i++)
			{
				col = new Column();
				col.setIntColumn1(i);
				col.setIntColumn2(data[i][0]);
				col.setIntColumn3(data[i][1]);
				col.setIntColumn4(data[i][2]);
				ret.add(col);
			}

		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return (ret);
	}

	/**
	 * Vrati statistiku podla dni za dane obdobie
	 *
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @return
	 */
	public static List<Column> getMonthViews(java.util.Date from, java.util.Date to, int rootGroupId)
	{
		return getMonthViews(from, to, String.valueOf(rootGroupId), false);
	}

	public static List<Column> getMonthViews(java.util.Date from, java.util.Date to, int rootGroupId, boolean withoutBots)
	{
		return getMonthViews(from, to, String.valueOf(rootGroupId), withoutBots);
	}

	public static List<Column> getMonthViews(java.util.Date from, java.util.Date to, String groupIds)
	{
		return getMonthViews(from, to, groupIds, false);
	}

	/**
	 * Vrati statistiku podla dni za dane obdobie
	 *
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @param groupIds - id adresarov oddelenych ciarkou
	 * @return
	 */
	public static List<Column> getMonthViews(java.util.Date from, java.util.Date to, String groupIds, boolean withoutBots)
	{
		List<Column> ret = new ArrayList<>();
		Map<String, Integer[]> dates = new Hashtable<>();

		Calendar cal = Calendar.getInstance();
		Column col;

		String whitelistedQuery = "";
		if(withoutBots)
			whitelistedQuery = getWhiteListedUAQuery();

		String[] suffixes = getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String sql = "SELECT "+getYMSelect("s.view_time")+", COUNT(s.doc_id) AS views, COUNT(DISTINCT s.session_id) AS sessions, COUNT(DISTINCT s.browser_id) AS unique_users";

				sql += " FROM stat_views"+suffixes[s]+" s" +
						" WHERE s.view_time>=? AND s.view_time<=? ";

				sql += StatDB.getRootGroupWhere("s.group_id", groupIds);
				sql += whitelistedQuery;

				sql += " GROUP BY "+getYMGroupBy("s.view_time");
				//sql += " ORDER BY vt_year, vt_month";

				Logger.debug(StatNewDB.class,"getDayViews: "+sql);
				cal.setTimeInMillis(from.getTime());
				cal.set(Calendar.DATE, 1);

				ps = prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(cal.getTimeInMillis()));

				cal.setTimeInMillis(to.getTime());
				cal.set(Calendar.DATE, 1);
				cal.add(Calendar.MONTH, 1);
				cal.add(Calendar.DATE, -1);

				ps.setTimestamp(2, new Timestamp(cal.getTimeInMillis()));

				rs = ps.executeQuery();
				//iteruj cez riadky
				while (rs.next())
				{
					cal.clear();
					cal.setFirstDayOfWeek(Calendar.MONDAY);
					cal.set(Calendar.YEAR, rs.getInt("vt_year"));
					cal.set(Calendar.MONTH, rs.getInt("vt_month")-1);
					cal.set(Calendar.DAY_OF_MONTH, 1);

					Integer[] data = new Integer[3];
					data[0] = Integer.valueOf(rs.getInt("views"));
					data[1] = Integer.valueOf(rs.getInt("sessions"));
					data[2] = Integer.valueOf(rs.getInt("unique_users"));
					//Logger.debug(StatNewDB.class, "Nastavujem: " + Tools.formatDate(cal.getTime()));

					dates.put(cal.get(Calendar.MONTH)+"."+cal.get(Calendar.YEAR) , data);
					//Logger.debug(this,"Data: "+data[0]+" "+data[1]+" "+data[2]);
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

		try
		{
			//Logger.debug(this,"Hashtable: "+dates.size()+ "   Count: "+count);

			Calendar calTo = Calendar.getInstance();
			calTo.setFirstDayOfWeek(Calendar.MONDAY);
			calTo.setTime(to);

			cal.clear();
			cal.setFirstDayOfWeek(Calendar.MONDAY);
			cal.setTimeInMillis(from.getTime());

			//Logger.debug(this,"Date: "+cal.getTime());

			while (cal.getTimeInMillis() <= calTo.getTimeInMillis())
			{
				//Logger.debug(StatNewDB.class, "Ziskavam: " + Tools.formatDate(cal.getTime()));
				Integer[] data = dates.get(cal.get(Calendar.MONTH)+"."+cal.get(Calendar.YEAR));
				//Logger.debug(this,"Datum: "+cal.getTime()+"  Data: "+dates.get(cal.getTime()));
				if (data != null)
				{
					col = new Column();
					col.setDateColumn1(cal.getTime());
					col.setIntColumn1(cal.get(Calendar.YEAR));
					col.setIntColumn2(cal.get(Calendar.MONTH)+1);
					col.setIntColumn3(data[0].intValue());
					col.setIntColumn4(data[1].intValue());
					col.setIntColumn5(data[2].intValue());
					ret.add(col);
				}
				else
				{
					col = new Column();
					col.setDateColumn1(cal.getTime());
					col.setIntColumn1(cal.get(Calendar.YEAR));
					col.setIntColumn2(cal.get(Calendar.MONTH)+1);
					col.setIntColumn3(0);
					col.setIntColumn4(0);
					col.setIntColumn5(0);
					ret.add(col);
				}

				cal.add(Calendar.MONTH, 1);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return (ret);
	}


	/**
	 *
	 * @param from
	 * @param to
	 * @param maxResults
	 * @return
	 */
	public static List<Column> getStatReferer(java.util.Date from, java.util.Date to, int maxRows, String groupIdsQuery)
	{
		if (groupIdsQuery==null) groupIdsQuery = "";

		Map<String, Number> map = new HashMap<>();

		String[] suffixes = StatNewDB.getTableSuffix("stat_from", from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				String sql;
				db_conn = DBPool.getConnection();

				sql = "SELECT referer_server_name, COUNT(referer_server_name) AS ref_views" +
			 			" FROM stat_from"+suffixes[s] +
						" WHERE from_time>=? AND from_time<=? " + groupIdsQuery +
						" GROUP BY referer_server_name" +
						" ORDER BY ref_views DESC";

				Logger.debug(StatNewDB.class,"getStatReferer: "+sql+" start="+Tools.formatDateTime(from)+" end="+Tools.formatDateTime(to));

				ps = prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));

				rs = ps.executeQuery();
				//iteruj cez riadky
				while (rs.next())
				{
					String key = ResponseUtils.filter(DB.getDbString(rs, "referer_server_name"));
					Number currentValue = map.get(key);

					if (currentValue == null) map.put(key, Integer.valueOf(rs.getInt("ref_views")));
					else map.put(key, Integer.valueOf(rs.getInt("ref_views")+currentValue.intValue()));
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

			//Logger.debug(StatDB.class, "getStatReferer: key="+key+" value="+value);

			Column col = new Column();
			col.setColumn1(key);
			col.setColumn2(col.getColumn1());
			col.setIntColumn2(value.intValue());

			ret.add(col);
		}

		return (ret);
	}

	/**
	 * Vrati pocet unikatnych pouzivatelov za stanovene obdobie z tab. stat_ views
	 *
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @return
	 */
	public static int getUniqueUsersFromStatViews(java.util.Date from, java.util.Date to, int rootGroupId)
	{
		return getUniqueUsersFromStatViews(from, to, String.valueOf(rootGroupId), false);
	}

	public static int getUniqueUsersFromStatViews(java.util.Date from, java.util.Date to, int rootGroupId, boolean withoutBots)
	{
		return getUniqueUsersFromStatViews(from, to, String.valueOf(rootGroupId), withoutBots);
	}

	public static int getUniqueUsersFromStatViews(java.util.Date from, java.util.Date to, String groupIds)
	{
		return getUniqueUsersFromStatViews(from, to, groupIds, false);
	}

	/**
	 * Vrati pocet unikatnych pouzivatelov za stanovene obdobie z tab. stat_ views
	 *
	 * @param from
	 * @param to
	 * @param rootGroupId
	 * @param groupIds
	 * @return
	 */
	public static int getUniqueUsersFromStatViews(java.util.Date from, java.util.Date to, String groupIds, boolean withoutBots)
	{
		int ret = 0;
		String[] suffixes = getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String whitelistedQuery = "";
				if(withoutBots)
					whitelistedQuery = getWhiteListedUAQuery();

				String sql = "SELECT COUNT(DISTINCT s.browser_id) AS unique_users" +
	 			" FROM stat_views"+suffixes[s]+" s" +
				" WHERE s.view_time>=? AND s.view_time<=?";

				sql += StatDB.getRootGroupWhere("s.group_id", groupIds);
				sql += whitelistedQuery;

				Logger.debug(StatNewDB.class, "getUniqueUsersFromStatViews sql:"+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));
				rs = ps.executeQuery();

				while (rs.next())
				{
					int uniq = rs.getInt("unique_users");
					ret += uniq;
					//Logger.debug(StatNewDB.class, "uniq="+uniq+" ret="+ret);
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

	/**
	 * Vygeneruje pre zvolene docID tabulku videni, sedeni, roznych userov za zvolene obdobie
	 * podla TYZDNOV
	 *
	 * @param from
	 * @param to
	 * @param docId
	 * @return
	 */
	public static List<Column> getViewsForDoc(java.util.Date from, java.util.Date to, int docId)
	{
		List<Column> ret = new ArrayList<>();

		if (docId < 1) return ret;

		Map<String, Column> colTable = new Hashtable<>();

		String[] suffixes = getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String sql = "SELECT "+getYWSelect("s.view_time")+",COUNT(s.doc_id) AS views, COUNT(DISTINCT s.session_id) AS sessions, COUNT(DISTINCT s.browser_id) AS unique_users";
				sql +=" FROM stat_views"+suffixes[s]+" s WHERE s.view_time>=? AND s.view_time<=? AND s.doc_id=?";
				sql += " GROUP BY "+getYWGroupBy("s.view_time")+" ORDER BY  vt_year ASC, vt_week ASC";

				Logger.debug(StatNewDB.class, "getViewsForDoc sql:"+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(from.getTime()));
				ps.setTimestamp(2, new Timestamp(to.getTime()));
				ps.setInt(3, docId);

				rs = ps.executeQuery();
				//iteruj cez riadky
				Column col;

				while (rs.next())
				{
					int year = rs.getInt("vt_year");
					int week = rs.getInt("vt_week");

					String key = year+"-"+week;

					col = colTable.get(key);
					if (col == null)
					{
						col = new Column();
						col.setIntColumn1(year);
						col.setIntColumn2(week);
						col.setIntColumn3(rs.getInt("views"));
						col.setIntColumn4(rs.getInt("sessions"));
						col.setIntColumn5(rs.getInt("unique_users"));
						ret.add(col);
						colTable.put(key, col);
					}
					else
					{
						col.setIntColumn3(col.getIntColumn3()+rs.getInt("views"));
						col.setIntColumn4(col.getIntColumn4()+rs.getInt("sessions"));
						col.setIntColumn5(col.getIntColumn5()+rs.getInt("unique_users"));
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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


	/**
	 * Vygeneruje pre zvolenu rootGroupID tabulku videni, sedeni, roznych userov za zvolene obdobie
	 *
	 * @param backInterval - pocet mesiacov spat, za ktore sa ma vygenerovat statistika
	 * @param rootGroupId
	 * @return
	 */
	public static List<Column> getMonthViewsForDoc(int backInterval, int docId)
	{
		List<Column> ret = new ArrayList<>();

		Calendar cal = Calendar.getInstance();
		long to = cal.getTimeInMillis();
		cal.set(Calendar.DATE, 1);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);

		cal.add(Calendar.MONTH, -backInterval);
		long from = cal.getTimeInMillis();

		Column col;
		Map<String, Column> colTable = new Hashtable<>();

		String[] suffixes = getTableSuffix(from, to);
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String sql = "SELECT "+getYMSelect("s.view_time")+", COUNT(s.doc_id) AS views, COUNT(DISTINCT s.session_id) AS sessions, COUNT(DISTINCT s.browser_id) AS unique_users" +
	 			" FROM stat_views"+suffixes[s]+" s" +
				" WHERE s.view_time>=? AND s.view_time<=? AND s.doc_id=?";

				sql += " GROUP BY "+getYMGroupBy("s.view_time")+" ORDER BY  vt_year DESC, vt_month DESC";

				Logger.debug(StatNewDB.class, "getMonthViewsForDoc sql:"+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setTimestamp(1, new Timestamp(cal.getTimeInMillis()));
				ps.setTimestamp(2, new Timestamp(Tools.getNow()));
				ps.setInt(3, docId);

				rs = ps.executeQuery();
				//iteruj cez riadky

				while (rs.next())
				{
					int month = rs.getInt("vt_month");
					int year = rs.getInt("vt_year");
					String key = year+"-"+month;

					col = new Column();
					col.setIntColumn1(year);
					col.setIntColumn2(month);
					col.setIntColumn3(rs.getInt("views"));
					col.setIntColumn4(rs.getInt("sessions"));
					col.setIntColumn5(rs.getInt("unique_users"));

					colTable.put(key, col);
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

		//vytvorime list a doplnime chybajuce (ak nejake su) mesiace
		try
		{
			while (cal.getTimeInMillis() < to)
			{
				String key = cal.get(Calendar.YEAR)+"-"+(cal.get(Calendar.MONTH)+1);
				col = colTable.get(key);

				if (col == null)
				{
					col = new Column();
					col.setIntColumn1(cal.get(Calendar.YEAR));
					col.setIntColumn2(cal.get(Calendar.MONTH)+1);
					col.setIntColumn3(0);
					col.setIntColumn4(0);
					col.setIntColumn5(0);
				}

				ret.add(col);
				cal.add(Calendar.MONTH, 1);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return (ret);
	}
	/**
	 * Vygeneruje pre zvolene docID tabulku kolko na dane docId prislo navstev z ktoreho docId
	 *
	 * @param docId
	 * @param from
	 * @param to
	 * @return
	 */
	public static List<Column> getIncomingStats(int docId, java.util.Date from, java.util.Date to, String groupIdsQuery, HttpServletRequest request)
	{
		if (groupIdsQuery==null) groupIdsQuery = "";

		List<Column> ret = new ArrayList<>();
		Map<Integer, Column> colTable = new Hashtable<>();
		GroupsDB groupsDB = GroupsDB.getInstance();
		DocDB docDB = DocDB.getInstance();
		Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);

		DocDetails bdd;
		int pocetPriamychPristupov = 0;


		//najskor nacitaj priame pristupy
		if (true)
		{
			//v ife to mame, aby neboli problemy s nazvami premennych

			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				String sql="SELECT count(doc_id) AS pocet, referer_server_name, referer_url FROM stat_from "+
				"WHERE doc_id=? AND from_time >= ? AND from_time <= ? " + groupIdsQuery +
				"GROUP BY referer_server_name, referer_url "+
				"ORDER BY pocet desc";

				Logger.debug(StatNewDB.class, "getIncomingStats sql: "+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setInt(1,docId);
				ps.setTimestamp(2, new Timestamp(from.getTime()));
				ps.setTimestamp(3, new Timestamp(to.getTime()));
				rs = ps.executeQuery();
				while(rs.next())
				{
					Column col = new Column();
					col.setIntColumn2(rs.getInt("pocet"));
					pocetPriamychPristupov+=col.getIntColumn2();
					col.setColumn1(rs.getString("referer_server_name")+rs.getString("referer_url"));
					col.setColumn2("http://"+col.getColumn1());
					ret.add(col);
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

		//nacitaj udaje statistiky
		String[] suffixes = getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnectionReadUncommited();

				String sql="SELECT count(doc_id) AS pocet, last_doc_id "+
				"FROM stat_views"+suffixes[s]+" "+
				"WHERE doc_id=? AND view_time >= ? AND view_time <= ? "+
				"GROUP BY last_doc_id "+
				"ORDER BY pocet desc";

				Logger.debug(StatNewDB.class, "getIncomingStats sql: "+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setInt(1,docId);
				ps.setTimestamp(2, new Timestamp(from.getTime()));
				ps.setTimestamp(3, new Timestamp(to.getTime()));
				rs = ps.executeQuery();

				while (rs.next())
				{
					int lastDocId=rs.getInt("last_doc_id");
					int pocet=rs.getInt("pocet");

					if(lastDocId==-1)
					{
						pocet = pocet - pocetPriamychPristupov;
					}

					Column col = colTable.get(lastDocId);
					if (col == null)
					{
						col = new Column();
						col.setIntColumn2(pocet);

						bdd = docDB.getBasicDocDetails(lastDocId, true);
						String title=groupsDB.getPath(bdd.getGroupId())+"/"+bdd.getTitle();
						col.setColumn1(title);
						if(lastDocId==-1)
						{
							col.setColumn1(prop.getText("stat_doc.directAccess"));
						}
						col.setColumn2("/apps/stat/admin/top-details/?docId="+lastDocId+"&title="+Tools.URLEncode(title));
						ret.add(col);
						colTable.put(lastDocId, col);
					}
					else
					{
						col.setIntColumn2(col.getIntColumn2()+pocet);
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

		//usporiadaj
		Collections.sort(ret, new Comparator<Column>()
		{
			@Override
			public int compare(Column c1, Column c2)
			{
				return (c2.getIntColumn2() - c1.getIntColumn2());
			}
		});

		//ak je tam nieco zaporne (vyradenie priamych pristupov) vyhod
		List<Column> ret2 = new ArrayList<>();
		for (Column col : ret)
		{
			if (col.getIntColumn2()>0) ret2.add(col);
		}

		return (ret2);
	}
	/**
	 * Vygeneruje pre zvolene docID tabulku kolko z docId navstev islo na ktore docId
	 *
	 * @param docId
	 * @param from
	 * @param to
	 * @return
	 */
	public static List<Column> getOutgoingStats(int docId, java.util.Date from, java.util.Date to)
	{
		List<Column> ret = new ArrayList<>();
		Map<Integer, Column> colTable = new Hashtable<>();
		GroupsDB groupsDB = GroupsDB.getInstance();
		DocDB docDB = DocDB.getInstance();
		DocDetails bdd;
		Column col;

		String[] suffixes = getTableSuffix(from.getTime(), to.getTime());
		for (int s=0; s<suffixes.length; s++)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnectionReadUncommited();
				String sql="SELECT count(last_doc_id) AS pocet, doc_id "+
				"FROM stat_views"+suffixes[s]+" "+
				"WHERE last_doc_id= ? AND view_time >= ? AND view_time <= ? "+
				"GROUP BY doc_id "+
				"ORDER BY pocet desc";

				Logger.debug(StatNewDB.class, "getOutgoingStats sql: "+sql);

				ps = prepareStatement(db_conn, sql);
				ps.setInt(1,docId);
				ps.setTimestamp(2, new Timestamp(from.getTime()));
				ps.setTimestamp(3, new Timestamp(to.getTime()));
				rs = ps.executeQuery();

				while (rs.next())
				{
					int docId2 = rs.getInt("doc_id");
					col = colTable.get(docId2);
					if (col == null)
					{
						col = new Column();
						col.setIntColumn2(rs.getInt("pocet"));
						bdd = docDB.getBasicDocDetails(docId2, true);
						String title=groupsDB.getPath(bdd.getGroupId())+"/"+bdd.getTitle();
						col.setColumn1(title);
						col.setColumn2("/apps/stat/admin/top-details/?docId="+rs.getInt("doc_id")+"&title="+Tools.URLEncode(title));
						ret.add(col);
						colTable.put(docId2, col);
					}
					else
					{
						col.setIntColumn2(col.getIntColumn2()+rs.getInt("pocet"));
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
				if (!createStatTablesFromError(ex.getMessage(), suffixes[s])) sk.iway.iwcm.Logger.error(ex);
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

		//usporiadaj
		Collections.sort(ret, new Comparator<Column>()
		{
			@Override
			public int compare(Column c1, Column c2)
			{
				return (c2.getIntColumn2() - c1.getIntColumn2());
			}
		});

		return (ret);
	}

	/**
	 * Funkcia, ktora vrati zoznam vsetkych pristupov daneho registrovaneho pouzivatela zoradenych od najnovsieho po najstarsi.
	 * <br /> Zoznam obshauje identifikacne cislo prihlasenia (intColumn1), prehliadany dokument (column1), posledne prehliadany dokument (last_doc),
	 * nazov skupiny, do ktorej patri prehliadana stranka (column3) a cas pristupu (dateColumn1)
	 *
	 * @param from datetime, od ktoreho chceme vyhladavat zobrazenia stranok
	 * @param to datetime, do ktoreho chceme vyhladavat zobrazenia stranok
	 * @param rootGroupId identifikator skupiny, ktoru chceme filtrovat. Ak sa rovna -1, tak to znamena, ze chceme vyhladavat vo vsetkych skupinach
	 * @return List naplneny jednotlivymi zobrazeniami stranok pre urceneho registrovaneho pouzivatela s roznymi vlastnostami
	 */
	public static List<Column> getUserStatViews(int userId, java.util.Date from, java.util.Date to, int rootGroupId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Column col;
		List<Column> userStatViews = new ArrayList<>();
		String sql;

		String[] suffixes = StatNewDB.getTableSuffix(from.getTime(), to.getTime());
		for (int s = (suffixes.length-1); s >= 0; s = s - 1)	// potrebujem opacne zoradene pole
		{
			sql = "SELECT s.session_id AS session, documents.title AS doc, docs.title AS last_doc, groups.group_name, s.view_time ";
			sql += "FROM stat_views" + suffixes[s] + " s";
			sql += " LEFT JOIN documents ON s.doc_id = documents.doc_id LEFT JOIN documents docs ON s.last_doc_id = docs.doc_id LEFT JOIN groups ON s.group_id = groups.group_id ";
			sql += "WHERE s.browser_id = ? AND s.view_time >= ? AND s.view_time <= ? ";
			sql += StatDB.getRootGroupWhere("s.group_id", rootGroupId);
			sql += " ORDER BY s.view_time DESC";

			Logger.debug(StatNewDB.class,"getUserStatViews: "+sql);

			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement(sql);
				ps.setInt(1, userId);
				ps.setTimestamp(2, new Timestamp(from.getTime()));
				ps.setTimestamp(3, new Timestamp(to.getTime()));

				rs = ps.executeQuery();
				//iteruj cez riadky

				while (rs.next())
				{
					col = new Column();

					col.setColumn4(String.valueOf(rs.getLong("session")));
					col.setColumn1(rs.getString("doc"));
					col.setColumn2(rs.getString("last_doc"));
					col.setColumn3(rs.getString("group_name"));
					col.setDateColumn1(rs.getTimestamp("view_time"));

					userStatViews.add(col);
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

		return (userStatViews);
	}

	/**
	 * Grants SELECT, UPDATE, INSERT, DELETE on specified table to supplied username
	 * @param tableName name of table (ie stat_error)
	 * @param suffix suffix for partitioning (ie _2012_10) _YYYY_MM
	 * @param publicWebDbUserName name of user to whom will be granted priviledges
	 *
	 * @return true if grant was successful, otherwise false
	 *
	 * @throws IllegalArgumentException if suffix, tableName od udername is null
	 * @throws IllegalStateException if there is exception while granting permissions
	 *
	 */
	public static boolean grantRightsToUser(String tableName, String suffix, String publicWebDbUserName)
	{
		if (suffix == null)
		{
			throw new IllegalArgumentException("Suffix can't be null");
		}
		if (tableName == null)
		{
			throw new IllegalArgumentException("tableName can't be null");
		}
		if (publicWebDbUserName == null)
		{
			throw new IllegalArgumentException("publicWebDbUserName can't be null");
		}
		boolean ret = false;
		Logger.debug(StatNewDB.class, "Granting rights on " + tableName+suffix + " to: " + publicWebDbUserName);
		try
		{
			new SimpleQuery().execute("GRANT SELECT, UPDATE, INSERT, DELETE ON ? TO ?" , tableName+suffix,publicWebDbUserName);
			Logger.debug(StatNewDB.class, "Grant sucessfull");
			ret = true;
		}
		catch(IllegalStateException e)
		{
			Logger.debug(StatNewDB.class, "Failed to grant permissions. Cause: "+e.getMessage() );
		}
		return ret;
	}

	/**
	 * Checks if there exists table with spacified name in DB
	 * method ignores case
	 * @param tablename name of the table
	 * @return true if table with specified name exists in DB
	 *
	 * @throws IllegalArgumentException if tablename is null or empty
	 */
	public static boolean tableExists(String tablename)
	{
		if(Tools.isEmpty(tablename))
		{
			throw new IllegalArgumentException("Parameter tablename can't be empty!");
		}

		Connection con = null;
		ResultSet res = null;
		boolean found = false;
		try
		{
			con = DBPool.getConnection();
			java.sql.DatabaseMetaData meta = con.getMetaData();
	      res = meta.getTables(null, null, null, new String[] {"TABLE"});

	      while (res.next()) {

	      	if(res.getString("TABLE_NAME").equalsIgnoreCase(tablename))
	      	{
	      		found = true;
	      		return found;
	      	}
	      }
	      res.close();
	      res=null;
			con.close();
			con = null;
		}
		catch (Exception ex)
		{
			IllegalStateException exception = new IllegalStateException(ex.getMessage());
			exception.initCause(ex);
			throw exception;
		}
		finally
		{
			try
			{
				if (res != null)
					res.close();
				if (con != null)
					con.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return found;
	}

	public static String amChartsData(Map<String, Map<Date, Number>> data)
	{
		return amChartsData(data, false);
	}

	public static String amChartsData(Map<String, Map<Date, Number>> data, boolean monitoring)
	{
		String chartData = "[]";
		if(data!=null && data.size()>0)
		{
			Map<Date, Number[]> graph = new TreeMap<>();

			int i = 0;
			for(Map.Entry<String, Map<Date, Number>> e : data.entrySet())
			{
				for(Map.Entry<Date, Number> n : e.getValue().entrySet())
				{
					Date day = n.getKey();

					Number[] storedValues = graph.get(day);
					if(storedValues == null)
					{
						Number[] newValues = new Number[data.size()];
						newValues[i] = n.getValue();
						graph.put(day, newValues);
					}
					else
					{
						storedValues[i] = n.getValue();
						graph.put(day, storedValues);
					}
				}
				i++;
			}

			chartData = "[";
			for(Map.Entry<Date, Number[]> e : graph.entrySet())
			{
				chartData += "{\"date\": \"";
				if(monitoring)
					chartData += Tools.formatDateTime(e.getKey());
				else
					chartData += Tools.formatDate(e.getKey());
				chartData += "\"";

				for(int k=0; k<e.getValue().length; k++)
				{
					if(e.getValue()[k] != null)
					{
						chartData += ",\"value" + k + "\": ";
						if(monitoring)
							chartData += e.getValue()[k].intValue();
						else
							chartData += e.getValue()[k];
					}
				}

				chartData += "},";
			}
			if (chartData.endsWith(",")) chartData = chartData.substring(0, chartData.length()-1);
			chartData += "]";
		}

		return chartData;
	}

	/**
	 * vrati " AND browser_ua_id IN [povolene prehliadace]", alebo prazdny string
	 *
	 * @return
	 */
	public static String getWhiteListedUAQuery()
	{
		String cacheKey = "statistika-getWhiteListedUAQuery";
		Object o = Cache.getInstance().getObject(cacheKey);
		if(o != null)
			return (String) o;

		String result = "";
		String whitelistQuery = "";

		List<String> items = Arrays.asList(Constants.getString("whiteListedUA").split("\\s*,\\s*"));
		//List<String> items = Arrays.asList("chrome, firefox".split("\\s*,\\s*"));
		for(int i=0; i<items.size(); i++)
		{
			if(i!=items.size()-1)
				whitelistQuery += " "+DB.fixAiCiCol("value")+" LIKE ? OR ";
			else
				whitelistQuery += " "+DB.fixAiCiCol("value")+" LIKE ? ";
		}

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT stat_keys_id FROM stat_keys WHERE " + whitelistQuery);

			for(int i=0; i<items.size(); i++)
			{
				ps.setString(i+1, "%"+DB.fixAiCiValue(items.get(i))+"%");
			}

			rs = ps.executeQuery();
			result += "( ";
			boolean empty = true;
			while (rs.next())
			{
				empty = false;
				result += rs.getInt("stat_keys_id") + ", ";
			}
			if(empty)
				result = "";
			else
			{
				result = result.substring(0, result.length()-2);
				result += " )";
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
			result = "";
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

		if(Tools.isNotEmpty(result))
			result = " AND browser_ua_id IN " + result;

		Cache.getInstance().setObject(cacheKey, result, 1440);
		return result;
	}
}


