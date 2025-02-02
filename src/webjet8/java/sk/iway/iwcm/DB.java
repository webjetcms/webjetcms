package sk.iway.iwcm;

import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.RowSetDynaClass;
import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.struts.util.ResponseUtils;

import oracle.jdbc.driver.OracleConnection;
import oracle.sql.CLOB;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;

/**
 *  nadtrieda DB tried, ma zakladne uzitocne funkcie
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.14 $
 *@created      $Date: 2004/03/23 20:50:49 $
 *@modified     $Date: 2004/03/23 20:50:49 $
 */
@SuppressWarnings({"deprecation", "java:S1118"})
public class DB
{
	//zoznam stlpcov ktore mozu obsahovat HTML kod
	private static Set<String> htmlAllowedFields = null;

	/**
	 * Overi, ci je mozne v danom DB stlpci pouzit HTML kod, ak nie, tak sa pri citani rovno escapnu specialne znaky
	 * ako < alebo > aby nebolo mozne vykonat XSS utok
	 * https://thoughts-on-java.org/jpa-21-how-to-implement-type-converter/
	 * @param fieldName
	 * @return
	 */
	public static boolean isHtmlAllowed(String fieldName)
	{
		if (fieldName==null) return false;
		if (fieldName.contains("html")) return true;
		if (fieldName.contains("data")) return true;
		//fotogaleria
		if (fieldName.contains("_description_")) return true;
		if (fieldName.startsWith("field_")) return true;

		if (htmlAllowedFields == null)
		{
			//systemove stlpce
			String[] fields = Tools.getTokens(Constants.getString("xssHtmlAllowedFieldsSystem"), ",", true);
			Set<String> fieldsSet = new HashSet<>(Arrays.asList(fields));
			//zakaznicke stlpce
			fields = Tools.getTokens(Constants.getString("xssHtmlAllowedFields"), ",", true);
			for (String field : fields) {
				if (fieldsSet.contains(field)) continue;
				fieldsSet.add(field);
			}
			htmlAllowedFields = fieldsSet;
		}

		if (htmlAllowedFields.contains(fieldName)) return true;

		return false;
	}

	public static void resetHtmlAllowedFields()
	{
		htmlAllowedFields = null;
	}


	/**
	 * vrati prekodovany a trimnuty string z result setu (povodne sa pouzivalo kvoli Oracle), ak je filter true vykona aj ResponseUtils.filter
	 * @param db_result
	 * @param fieldName
	 * @param filter
	 * @return
	 * @throws SQLException
	 */
	public static String getDbString(java.sql.ResultSet db_result, String fieldName, boolean filter) throws SQLException
	{
		String ret = getDbStringImpl(db_result, fieldName);

		if (filter) return filterHtml(ret);

		return ret;
	}

	/**
	 *  vrati prekodovany a trimnuty string z result setu (povodne sa pouzivalo kvoli Oracle)
	 *
	 *@param  db_result      result set
	 *@param  fieldName      meno fieldu v databaze
	 *@return                prekodovany a trimnuty string
	 *@exception  Exception  Description of the Exception
	 */
	public static String getDbString(java.sql.ResultSet db_result, String fieldName) throws SQLException
	{
		String data = null;

		data = getDbStringImpl(db_result, fieldName);

		if (isHtmlAllowed(fieldName)==false)
		{
			if (data.contains("<") || data.contains(">"))
			{
				data = filterHtml(data);
			}
		}

		//ak su sifrovane skus desifrovat
		data = CryptoFactory.decrypt(data);
		return (data);
	}

	/**
	 * Nahradi nebezpecne HTML znaky za entity
	 * @param data
	 * @return
	 */
	public static String filterHtml(String data)
	{
		if (data == null) return null;

		if (data.contains("<") || data.contains(">"))
		{
			return ResponseUtils.filter(data);
		}
		return data;
	}

	private static String getDbStringImpl(java.sql.ResultSet db_result, String fieldName) throws SQLException
	{
		String data = null;

		data = db_result.getString(fieldName);

		if (data != null && data.length()>0)
		{
			//ochrana pred vykonanim prikazu v Exceli
			if (data.length()>5)
			{
				String retLC = data.substring(0, 4).toLowerCase();
				if (retLC.toLowerCase().startsWith("=cmd")) data = "-" + data.substring(1);
			}

			return (data.trim());
		}
		else
		{
			return ("");
		}
	}

	/**
	 * Vrati long hodnotu Timestampu (ak nie je null), inak vrati 0
	 * @param rs
	 * @param fieldName
	 * @return
	 * @throws Exception
	 */
	public static long getDbTimestamp(java.sql.ResultSet rs, String fieldName)
	{
		try
		{
			java.sql.Timestamp data = rs.getTimestamp(fieldName);
			if (data != null)
			{
				return (data.getTime());
			}
		}
		catch (SQLException ex)
		{

		}
		return (0);
	}

	public static Timestamp getDbTimestamp(long time)
	{
		if (time == 0)
		{
			return(null);
		}
		return(new Timestamp(time));
	}

	/**
	 *  vrati naformatovany datum z databazy
	 *
	 *@param  db_result      result set
	 *@param  fieldName      meno fieldu v databaze
	 *@return                naformatovany datum z databazy
	 *@exception  Exception  Description of the Exception
	 */
	public static String getDbDate(java.sql.ResultSet db_result, String fieldName) throws SQLException
	{
		try
		{
			if (db_result.getDate(fieldName) != null)
			{
				java.sql.Date dbDate = db_result.getDate(fieldName);
				java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(Constants.getString("dateFormat"));
				return (formatter.format(dbDate));
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return("");
	}

	public static Date getDate(java.sql.ResultSet db_result, String fieldName) throws SQLException
	{
		try
		{
			if (db_result.getDate(fieldName) != null)
			{
				return new Date(db_result.getTimestamp(fieldName).getTime());
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return null;
	}

	/**
	 *  vrati naformatovany cas z databazy
	 *
	 *@param  db_result      result set
	 *@param  fieldName      meno fieldu v databaze
	 *@return                naformatovany cas z databazy
	 *@exception  Exception  Description of the Exception
	 */
	public static String getDbTime(java.sql.ResultSet db_result, String fieldName) throws Exception
	{
		if (db_result.getDate(fieldName) != null)
		{
			java.sql.Timestamp dbDate = db_result.getTimestamp(fieldName);
			java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(Constants.getString("timeFormat"));
			return (formatter.format(dbDate));
		}
		else
		{
			return ("");
		}
	}

	/**
	 *  Gets the dbDateTime attribute of the DB class
	 *
	 *@param  db_result      Description of the Parameter
	 *@param  fieldName      Description of the Parameter
	 *@return                The dbDateTime value
	 *@exception  Exception  Description of the Exception
	 */
	public static String getDbDateTime(java.sql.ResultSet db_result, String fieldName) throws SQLException
	{
		return (getDbDateTime(db_result, fieldName, "iwcm"));
	}

	/**
	 *  Gets the dbDateTime attribute of the DB class
	 *
	 *@param  db_result      Description of the Parameter
	 *@param  fieldName      Description of the Parameter
	 *@param  serverName     Description of the Parameter
	 *@return                The dbDateTime value
	 *@exception  Exception  Description of the Exception
	 */
	public static String getDbDateTime(java.sql.ResultSet db_result, String fieldName, String serverName) throws SQLException
	{
		String ret = "";

		try
		{
			if (db_result.getTimestamp(fieldName) != null)
			{
				java.sql.Timestamp dbDate = db_result.getTimestamp(fieldName);
				java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(Constants.getString("dateTimeFormat"));
				ret =  formatter.format(dbDate);
			}
		}
		catch (SQLException ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return (ret);
	}


	public static long getTimestamp(String date, String time, String serverName)
	{
		return(getTimestamp(date, time));
	}

	public static long getTimestamp(String dateTime)
	{
		long timestamp = 0;
		try
		{
			if (Tools.isNotEmpty(dateTime))
			{
				//ak je datum vo formate 1. 5. 2005 14:30
				dateTime = Tools.replace(dateTime, ". ", ".");

				//ak je to zadane ako 26112009 uprav na lepsi format
				if (dateTime.length()==8 && dateTime.indexOf('.')==-1)
				{
					dateTime = dateTime.substring(0, 2)+"."+dateTime.substring(2,4)+"."+dateTime.substring(4);
				}

				StringTokenizer st = new StringTokenizer(dateTime);
				if(st.hasMoreTokens() && st.countTokens() == 4)
				{
					timestamp = DB.getTimestamp(st.nextToken()+"."+st.nextToken()+"."+st.nextToken(), st.nextToken());
				}
				else if(st.hasMoreTokens() && st.countTokens() == 3)
				{
					timestamp = DB.getTimestamp(st.nextToken()+"."+st.nextToken()+"."+st.nextToken(), "0:00");
				}
				else if(st.hasMoreTokens() && st.countTokens() == 2)
				{
					timestamp = DB.getTimestamp(st.nextToken(), st.nextToken());
				}
				else if (st.hasMoreTokens() && st.countTokens() == 1)
				{
					timestamp = DB.getTimestamp(st.nextToken(), "0:00");
				}


				//Calendar cal = Calendar.getInstance();
				//cal.setTimeInMillis(timestamp);
				//Logger.println(this,"Timestamp: " +cal.getTime());
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return timestamp;
	}


	/**
	 *  return sql timestamp from given date and time
	 *
	 *@param  date        datum
	 *@param  time        cas
	 *@return             The timestamp value
	 */
	public static long getTimestamp(String date, String time)
	{
		if (Tools.isEmpty(date))
		{
			return (0);
		}

		if (Tools.isEmpty(time))
		{
			time = "00:00:00";
		}

		Calendar tmpCalendar = Calendar.getInstance();
		tmpCalendar.clear();
		int day;
		int month;
		int year;
		int hour;
		int minute;
		int second;

		//try to parse date
		date = date.trim();
		date = date.replace('/', '.');
		date = date.replace(' ', '.');
		date = date.replace('-', '.');
		date = date.replace(',', '.');

		//ak je to zadane ako 26112009 uprav na lepsi format
		if (date.length()==8 && date.indexOf('.')==-1)
		{
			date = date.substring(0, 2)+"."+date.substring(2,4)+"."+date.substring(4);
		}

		boolean usFormat = false;
		if (Constants.getString("dateFormat").equalsIgnoreCase("mm-dd-yyyy"))
		{
			usFormat = true;
		}

		StringTokenizer st = new StringTokenizer(date, ".");
		if (st.hasMoreTokens())
		{
			try
			{
				day = Integer.parseInt(st.nextToken());
				if (usFormat)
				{
					tmpCalendar.set(Calendar.MONTH, day - 1);
				}
				else
				{
					tmpCalendar.set(Calendar.DAY_OF_MONTH, day);
				}
			}
			catch (Exception ex)
			{
			}
		}
		if (st.hasMoreTokens())
		{
			try
			{
				month = Integer.parseInt(st.nextToken());
				if (usFormat)
				{
					tmpCalendar.set(Calendar.DAY_OF_MONTH, month);
				}
				else
				{
					//if you dont know why, RTFM
					month = month - 1;
					tmpCalendar.set(Calendar.MONTH, month);
				}
			}
			catch (Exception ex)
			{
			}
		}
		if (st.hasMoreTokens())
		{
			try
			{
				year = Integer.parseInt(st.nextToken());
				if (year < 20)
				{
					year = 2000 + year;
				}
				tmpCalendar.set(Calendar.YEAR, year);
			}
			catch (Exception ex)
			{
			}
		}

		time = time.trim();
		if (time.length() < 3)
		{
			time = "00:00:00";
		}
		time = time.replace('.', ':');
		time = time.replace(',', ':');
		time = time.replace(' ', ':');
		time = time.replace('-', ':');

		st = new StringTokenizer(time, ":");
		if (st.hasMoreTokens())
		{
			try
			{
				hour = Integer.parseInt(st.nextToken());
				tmpCalendar.set(Calendar.HOUR_OF_DAY, hour);
			}
			catch (Exception ex)
			{
			}
		}
		if (st.hasMoreTokens())
		{
			try
			{
				minute = Integer.parseInt(st.nextToken());
				tmpCalendar.set(Calendar.MINUTE, minute);
				if (minute > 30)
				{
					tmpCalendar.set(Calendar.SECOND, 59);
					tmpCalendar.set(Calendar.MILLISECOND, 999);
				}
				else
				{
					tmpCalendar.set(Calendar.SECOND, 0);
					tmpCalendar.set(Calendar.MILLISECOND, 0);
				}
			}
			catch (Exception ex)
			{
			}
		}
		if (st.hasMoreTokens())
		{
			try
			{
				second = Integer.parseInt(st.nextToken());
				tmpCalendar.set(Calendar.SECOND, second);
				if (second > 30)
				{
					tmpCalendar.set(Calendar.MILLISECOND, 999);
				}
				else
				{
					tmpCalendar.set(Calendar.MILLISECOND, 0);
				}
			}
			catch (Exception ex)
			{
			}
		}
		else
		{
			tmpCalendar.set(Calendar.SECOND, 0);
		}
		tmpCalendar.set(Calendar.MILLISECOND, 0);
		return (tmpCalendar.getTime().getTime());
	}

	private static final String TAB00C0 =
			"AAAAAAACEEEEIIII" +
					"DNOOOOO\u00d7\u00d8UUUUYI\u00df" +
					"aaaaaaaceeeeiiii" +
					"\u00f0nooooo\u00f7\u00f8uuuuy\u00fey" +
					"AaAaAaCcCcCcCcDd" +
					"DdEeEeEeEeEeGgGg" +
					"GgGgHhHhIiIiIiIi" +
					"IiJjJjKkkLlLlLlL" +
					"lLlNnNnNnnNnOoOo" +
					"OoOoRrRrRrSsSsSs" +
					"SsTtTtTtUuUuUuUu" +
					"UuUuWwYyYZzZzZzF";
	private static final String[] TAB00C1 = "A,B,V,G,D,E,Zh,Z,I,J,K,L,M,N,O,P,R,S,T,U,F,H,C,Ch,Sh,Shh,\"\",Y,'',Je,Ju,Ja,a,b,v,g,d,e,zh,z,i,j,k,l,m,n,o,p,r,s,t,u,f,h,c,ch,sh,shh,\",y,',je,ju,ja,eh,jo".split(",");

	/**
	 *  prekoduje diakritiku a azbuku do cisteho ascii
	 *
	 *@param  source  Description of the Parameter
	 *@return         Description of the Return Value
	 */
	public static String internationalToEnglish(String source)
	{
		if (source == null)
		{
			//pozor, toto sa casto pouziva, nesmie to vratit null
			return ("");
		}

		char[] vysl = new char[source.length()*2];
		char one;
		int k = 0;
		for (int i = 0; i < source.length(); i++)
		{
			one = source.charAt(i);
			if (one >= '\u00c0' & one <= '\u017f') //NOSONAR
			{
				one = TAB00C0.charAt(one - '\u00c0');
			}
			if(one >= '\u0410' & one <= '\u0451') //NOSONAR azbuka
			{
				String tmp = TAB00C1[(one - '\u0410')];
				for (int j = 0; j < tmp.length(); j++)
				{
					vysl[k++] = tmp.charAt(j);
				}
				continue;
			}
			vysl[k++] = one;
		}
		return new String(vysl).trim();
	}

	/**
	 * Replace stringu na string
	 *
	 * @param src    zdrojovy string
	 * @param oldStr co sa ma nahradit
	 * @param newStr za co sa ma nahradit
	 * @return string src v ktorom je nahradene oldStr za newStr
	 * @deprecated replaced by {@link Tools#replace(String, String, String)}
	 */
	@Deprecated
	public static String replace(String src, String oldStr, String newStr)
	{
		return(Tools.replace(src, oldStr, newStr));
	}

	/**
	 *  nastavi pole text, alebo clob v preparedStatemente
	 *
	 *@param  ps             The new clob value
	 *@param  pos            The new clob value
	 *@param  data           The new clob value
	 *@exception  Exception  Description of the Exception
	 */
	public static void setClob(java.sql.PreparedStatement ps, int pos, String data) throws Exception
	{
		if (Constants.DB_TYPE == Constants.DB_ORACLE)
		{
			//ps.setString(pos, "&nbsp;");
			if(data!=null)
			{

				try
				{
					Connection db_conn = ps.getConnection();

					Connection nativeConn = null;
					if (db_conn instanceof DelegatingConnection)
					{
						nativeConn = ((DelegatingConnection)db_conn).getInnermostDelegate();
					}
					else if (db_conn instanceof OracleConnection)
					{
						nativeConn = db_conn;
					}
					//toto treba pre JBoss odkomentovat, ja v libkach nemam WrappedConnection
				   /*
				   else if (db_conn instanceof WrappedConnection)
				   {
					nativeConn = ((WrappedConnection)db_conn).getUnderlyingConnection();
				   }
				   */
					if (nativeConn != null)
					{
						Connection nativeCon = ((DelegatingConnection) db_conn).getInnermostDelegate();

						CLOB tempClob = CLOB.createTemporary(nativeCon, true, CLOB.DURATION_SESSION);
						// Open the temporary CLOB in readwrite mode to enable writing
						tempClob.open(CLOB.MODE_READWRITE);
						// Get the output stream to write
						Writer tempClobWriter = tempClob.getCharacterOutputStream();
						// Write the data into the temporary CLOB
						tempClobWriter.write(data);
						// Flush and close the stream
						tempClobWriter.flush();
						tempClobWriter.close();
						// Close the temporary CLOB
						tempClob.close();
						ps.setObject(pos, tempClob);
					}
					else
					{
						ps.setString(pos, data);
					}
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
				}

				//StringReader reader = new StringReader(data);
				//ps.setCharacterStream(pos, reader, data.length());
			}
			else
			{
				ps.setNull(pos, java.sql.Types.CLOB);
			}

		}
		else
		{
			ps.setString(pos, data);
		}
	}

	/**
	 *  Nastavi hodnotu do Clobu (pre Oracle)
	 *
	 *@param  data           Description of the Parameter
	 *@param  rs             Description of the Parameter
	 *@param  index          Description of the Parameter
	 *@exception  Exception  Description of the Exception
	 */
	/*public static void fillClob(java.sql.ResultSet rs, int index, String data)
			 throws Exception
	{
		Logger.println(this,"fill clob - rs: " + rs);
		CLOB clob = ((oracle.jdbc.OracleResultSet) rs).getCLOB(index);
		Writer outstream = clob.getCharacterOutputStream();
		outstream.write(data);
		outstream.close();
	}*/

	/**
	 *  odstrani apostrofy zo stringu, treba volat vzdy, ked sa nejaky parameter dava priamo do SQL prikazu
	 *
	 *@param  sqlParam
	 *@return
	 */
	public static String removeSlashes(String sqlParam)
	{
		if (sqlParam == null) return(sqlParam);

		sqlParam = sqlParam.replace('\'', ' ');
		return (sqlParam);
	}

	/**
	 *  Produces a string representation of a complete result set
	 *
	 *@param  rs             The ResultSet to be displayed
	 *@return                The string representation
	 *@throws  SQLException  to indicate a problem with the ResultSet
	 */
	public static String dumpResultSet(ResultSet rs) throws SQLException
	{
		StringBuilder buf = new StringBuilder();
		if (rs != null)
		{
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount() + 1;
			for (int i = 1; i < columnCount; i++)
			{
				buf.append(rsmd.getColumnLabel(i));
				buf.append("  ");
			}
			buf.append("\n===========================================\n");

			while (rs.next())
			{
				for (int i = 1; i < columnCount; i++)
				{
					buf.append(rs.getObject(i));
					buf.append("  ");
				}
				buf.append('\n');
			}
		}
		return buf.toString();
	}

	public static String getFullName(ResultSet rs) throws Exception
	{
		return(DB.getFullName(rs, "u_title"));
	}

	/**
	 *  Gets the fullName attribute of the DB object
	 *
	 *@param  rs  Description of the Parameter
	 *@return     The fullName value
	 */
	public static String getFullName(ResultSet rs, String titleName) throws Exception
	{
		StringBuilder ret = new StringBuilder();
		String title = getDbString(rs, titleName);
		String firstName = getDbString(rs, "first_name");
		String lastName = getDbString(rs, "last_name");

		if (title != null && title.length()>0)
		{
			ret.append(title).append(' ');
		}
		if (firstName != null && firstName.length()>0)
		{
			ret.append(firstName).append(' ');
		}
		if (lastName != null && lastName.length()>0)
		{
			ret.append(lastName).append(' ');
		}

		return(ret.toString().trim());
	}

	/**
	 * Oreze s tak, aby nebol dlhsi ako maxLen
	 * @param s
	 * @param maxLen
	 * @return
	 */
	public static String prepareString(String s, int maxLen)
	{
		if (s==null)
		{
			return(s);
		}

		if (s.length() > maxLen)
		{
			s = s.substring(0, maxLen-1);
		}

		return(s);
	}

	/**
	 * Vrati List DynaBeanov pre zadane SQL
	 * @param sql - SQL prikaz
	 * @return
	 */
	public static List<DynaBean> getDynaList(String sql)
	{
		return(getDynaList(sql, null, null));
	}

	public static List<DynaBean> getDynaList(String sql, List<?> params)
	{
		return getDynaList(sql, params, null);
	}

	public static List<DynaBean> getDynaList(String sql, List<?> params, String dbName)
	{
		try
		{
			Logger.debug(DB.class, "getting dynaList, query: " + sql);
			Connection db_conn = Tools.isNotEmpty(dbName) ? DBPool.getConnection(dbName) : DBPool.getConnection();
			if (db_conn != null)
				try
				{
					PreparedStatement ps = db_conn.prepareStatement(sql);
					try
					{
						if (params != null && params.size()>0)
						{
							int size = params.size();
							int i;
							Object o;
							for (i=1; i<=size; i++)
							{
								o = params.get(i-1);
								if (o instanceof String)
								{
									ps.setString(i, (String)o);
								}
								else if (o instanceof Integer)
								{
									ps.setInt(i, ((Integer)o).intValue());
								}
								else if (o instanceof Double)
								{
									ps.setDouble(i, ((Double)o).doubleValue());
								}
								else if (o instanceof Float)
								{
									ps.setFloat(i, ((Float)o).floatValue());
								}
								else if (o instanceof Long)
								{
									ps.setLong(i, ((Long)o).longValue());
								}
								else if (o instanceof Boolean)
								{
									ps.setBoolean(i, ((Boolean)o).booleanValue());
								}
								else if (o instanceof Timestamp)
								{
									ps.setTimestamp(i, (Timestamp)o);
								}
								else if (o instanceof java.sql.Date)
								{
									ps.setDate(i, (java.sql.Date)o);
								}
								else
								{
									ps.setObject(i, o);
								}
							}
						}
						ResultSet rs = ps.executeQuery();
						try
						{
							RowSetDynaClass rsdc = null;
							rsdc=new IwcmRowSetDynaClass(rs);
							Logger.debug(DB.class, "dynaBenas found: " + rsdc.getRows().size());
							return rsdc.getRows();
						}
						finally { rs.close(); }
					}
					finally { ps.close(); }
				}
				finally { db_conn.close(); }
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return(new ArrayList<DynaBean>());
	}

	public static int getIntValue(Object o, int defaultValue)
	{
		int ret = defaultValue;
		if (o instanceof Integer)
		{
			ret = ((Integer)o).intValue();
		}
		else if (o instanceof Long)
		{
			ret = (int)((Long)o).longValue();
		}

		return(ret);
	}

	public static long getLongValue(Object o, long defaultValue)
	{
		long ret = defaultValue;
		if (o instanceof Integer)
		{
			ret = ((Integer)o).intValue();
		}
		else if (o instanceof Long)
		{
			ret = ((Long)o).longValue();
		}

		return(ret);
	}


	public static void execute(String sql, Object... arguments)
	{
		new SimpleQuery().execute(sql, arguments);
	}

	public static int queryForInt(String sql, Object... parameters)
	{
		return new SimpleQuery().forInt(sql, parameters);
	}

	public static long queryForLong(String sql, Object... parameters)
	{
		return new SimpleQuery().forLong(sql, parameters);
	}

	public static String queryForString(String sql, Object... parameters)
	{
		return new SimpleQuery().forString(sql, parameters);
	}

	public static double queryForDouble(String sql, Object... parameters)
	{
		return new SimpleQuery().forDouble(sql, parameters);
	}

	public static BigDecimal queryForBigDecimal(String sql, Object...parameters)
	{
		return new SimpleQuery().forBigDecimal(sql, parameters);
	}

	@SuppressWarnings("rawtypes")
	public static List queryForList(String sql, Object... parameters)
	{
		return new SimpleQuery().forList(sql, parameters);
	}

	/**
	 * Vrati a skontroluje IN zoznam pre SQL tak, aby obsahoval len cisla, cize z "1,xxx,2,3" spravi "1,2,3"
	 * @param ids
	 * @return
	 */
	public static String getOnlyNumbersIn(String ids)
	{
		return getOnlyNumbersIn(ids,false);
	}

	/**
	 * Vrati a skontroluje IN zoznam pre SQL tak, aby obsahoval len cisla, cize z "1,xxx,2,3" spravi "1,2,3"
	 * ak sa jedna o id-cka {@link GroupDetails} tak v pripade ze je zapnuty cloud sa kontroluje aj ich dostupnost pre aktualnu domenu
	 * @param ids
	 * @param checkGroups indikuje ci sa jedna o id-cka GroupDetails
	 * @return
	 */
	public static String getOnlyNumbersIn(String ids, boolean checkGroups)
	{
		StringTokenizer st = new StringTokenizer (ids, ",");
		StringBuilder ret = new StringBuilder();
		GroupDetails group = null;
		String domain = CloudToolsForCore.getDomainName();
		while (st.hasMoreTokens())
		{
			try
			{
				String sid = st.nextToken();
				sid = sid.replace('(', ' ');
				sid = sid.replace(')', ')');
				sid = sid.trim();
				int id = Integer.parseInt(sid);
				if(checkGroups && InitServlet.isTypeCloud())
				{
					group = GroupsDB.getInstance().getGroup(id);
					if(group != null)
					{
						if(group.getDomainName().equalsIgnoreCase(domain))
						{
							if (ret.length()==0) ret = new StringBuilder(String.valueOf(id));
							else ret.append(',').append(id);
						}
					}
				}
				else
				{
					if (ret.length()==0) ret = new StringBuilder(String.valueOf(id));
					else ret.append(',').append(id);
				}


			}
			catch (Exception ex)
			{

			}
		}

		return ret.toString();
	}


	/**
	 * Upravi hodnotu timestampu tak, aby nebola pred zadanym rokom (kedze DB to tak nevedia mat)
	 * @param timestamp
	 * @param year
	 * @return
	 */
	public static long getTimestampNotBeforeYear(long timestamp, int year)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp);

		if (cal.get(Calendar.YEAR)<year)
		{
			cal.set(Calendar.YEAR, year);
		}

		return cal.getTimeInMillis();
	}

	/**
	 * Upravi hodnotu timestampu tak, aby nebola za zadanym rokom (kedze DB to tak nevedia mat)
	 * @param timestamp
	 * @param year
	 * @return
	 */
	public static long getTimestampNotAfterYear(long timestamp, int year)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp);

		if (cal.get(Calendar.YEAR)>year)
		{
			cal.set(Calendar.YEAR, year);
		}

		return cal.getTimeInMillis();
	}

	/**
	 * Upravi hodnotu timestampu tak, aby nebola pred a za zadanym rokom (kedze DB to tak nevedia mat)
	 * @param timestamp
	 * @param yearFrom
	 * @param yearTo
	 * @return
	 */
	public static long getTimestampNotBeforeAfterYear(long timestamp, int yearFrom, int yearTo)
	{
		timestamp = getTimestampNotBeforeYear(timestamp, yearFrom);
		return getTimestampNotAfterYear(timestamp, yearTo);
	}

	/**
	 * osetri HTML znacky v retazci - DEPRECATED, pouzi filterHtml
	 *
	 * @param escapedString
	 * @return*
	 * @deprecated - use filterHtml
	 */
	@Deprecated
	public static String filterEscaped(String escapedString)
	{
		return filterHtml(escapedString);
	}


	/**
	 * Vrati Integer hodnotu z databazy, pricom korektne vrati aj NULL
	 * @param rs
	 * @param colName
	 * @return
	 * @throws SQLException
	 */
	public static Integer getInteger(ResultSet rs, String colName) throws SQLException {
		int nValue = rs.getInt(colName);
		return rs.wasNull() ? null : nValue;
  	}

	/**
	 * Vrati Boolean hodnotu z databazy, pricom korektne vrati aj NULL
	 * @param rs
	 * @param colName
	 * @return
	 * @throws SQLException
	 */
	public static Boolean getBoolean(ResultSet rs, String colName) throws SQLException {
		boolean nValue = rs.getBoolean(colName);
		return rs.wasNull() ? null : nValue;
	}

	/**
	 * prepare SQL query for datatable search, usage:
	 *
	 * List<Object> params = new ArrayList<>();
	 * sql += DB.getSqlQueryDatatable("url", url, true, params);
	 * sql += DB.getSqlQueryDatatable("query_string", errorText, true, params);
	 * sql += DB.getSqlQueryDatatable("count", countRange, true, params);
	 * int psCounter = 1;
	 * psCounter = DB.getSqlParamsDatatable(params, ps, psCounter);
	 *
	 * @param fieldName - name of database column
	 * @param searchText - searched text from request
	 * @param addAnd - true to add AND before condition
	 * @param params - list of parameters for PreparedStatement
	 * @return - SQL WHERE query part
	 */
	public static String getSqlQueryDatatable(String fieldName, String searchText, boolean addAnd, List<Object> params) {

		if (Tools.isEmpty(searchText)) return "";

		StringBuilder sb = new StringBuilder();
		if (addAnd) sb.append(" AND ");
		sb.append(fieldName).append(" ");

		if (searchText.startsWith("range:") || searchText.startsWith("daterange:")) {

			boolean isDateRange = searchText.startsWith("daterange:");
			String value = searchText.substring(searchText.indexOf(":") + 1);

			if (value.startsWith("-")) {
				sb.append(" <= ? ");

				if (isDateRange) params.add(new Timestamp(Tools.getLongValue(value.substring(1), 0)));
				else params.add(Tools.getIntValue(value.substring(1), 0));
			} else if (value.contains("-")) {
				sb.append(" BETWEEN ? AND ? ");

				String[] values = Tools.getTokens(value, "-");
				if (isDateRange) {
					params.add(new Timestamp(Tools.getLongValue(values[0], 0)));
					params.add(new Timestamp(Tools.getLongValue(values[1], 0)));
				} else {
					params.add(Tools.getIntValue(values[0], 0));
					params.add(Tools.getIntValue(values[1], 0));
				}
			} else {
				sb.append(" >= ? ");

				if (isDateRange) params.add(new Timestamp(Tools.getLongValue(value, 0)));
				else params.add(Tools.getIntValue(value, 0));
			}
		} else {
			String operator = "LIKE";
			if (searchText.startsWith("^") && searchText.endsWith("$")) operator = "";

			sb.append(operator).append(" ? ");

			if (searchText.startsWith("^") && searchText.endsWith("$")) params.add(searchText);
			else if (searchText.startsWith("^")) params.add(searchText.substring(1) + "%");
			else if (searchText.endsWith("$")) params.add("%" + searchText.substring(0, searchText.length() - 1));
			else params.add("%" + searchText.substring(1, searchText.length() - 1) + "%");
		}

		return sb.toString();
	}

	/**
	 * Fill PreparedStatement with parameters
	 * @param params - list of Parameters
	 * @param ps - prepared statement
	 * @param psCounter - counter of parameters, starts with 1 for first parameter
	 * @return - new counter of parameters
	 * @throws SQLException
	 */
	public static int getSqlParamsDatatable(List<Object> params, PreparedStatement ps, int psCounter) throws SQLException {

		//set list of params to PreparedStatemend based of instanceof
		for (Object param : params) {
			if (param instanceof Timestamp) {
				ps.setTimestamp(psCounter++, (Timestamp) param);
			} else if (param instanceof Long) {
				ps.setLong(psCounter++, (Long) param);
			} else if (param instanceof Integer) {
				ps.setInt(psCounter++, (Integer) param);
			} else if (param instanceof String) {
				ps.setString(psCounter++, (String) param);
			} else if (param instanceof Boolean) {
				ps.setBoolean(psCounter++, (Boolean) param);
			}
		}

		return psCounter;
	}


}