package sk.iway.iwcm.database;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 *  SimpleQueryResolver.java
 *
 *	 A class encapsulating some simple database operations, like retrieving
 *		a single integer, single object, a list of single columns, etc.
 *
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: thaber $
 *@version      $Revision: 1.6 $
 *@created      Date: 22.7.2009 12:35:57
 *@modified     $Date: 2010/01/14 12:29:46 $
 */
public class SimpleQuery
{
	private String databaseName;

	public SimpleQuery()
	{
		this("iwcm");
	}

	public SimpleQuery(String databaseName)
	{
		this.databaseName = databaseName;
	}

	public void execute(String sql, Object... arguments) {
		executeImpl(sql, false, arguments);
	}

	/**
	 * Execute SQL and returns updateCount (count of changed rows)
	 * @param sql
	 * @param arguments
	 * @return
	 */
	public int executeWithUpdateCount(String sql, Object... arguments) {
		return executeImpl(sql, true, arguments);
	}

	private int executeImpl(String sql, boolean checkupdateCount, Object... arguments) {
		String exceptionMessage = null;
		Connection db_conn = null;
		PreparedStatement ps = null;
		String params = null;
		int updateCount = -1;

		try
		{
			db_conn = DBPool.getConnection(databaseName);
			ps = db_conn.prepareStatement(sql);

			params = bindParameters(ps, arguments);

			ps.execute();

			if (checkupdateCount) {
				try {
					//Get and store number of updated columns
					updateCount = ps.getUpdateCount();
				} catch (Exception ex) {}
			}

			if (sql.startsWith("INSERT INTO monitoring") ||
				 sql.startsWith("DELETE FROM cluster_refresher WHERE refresh_time<=?") ||
				 sql.startsWith("SELECT schedule_id FROM groups_scheduler") ||
				 sql.startsWith("SELECT DISTINCT node_name FROM monitoring") ||
				 params.contains("statDistinctUsers-%, ") ||
				 params.contains("statSessions-%, ")
			) {
				//toto nelogujeme, zbytocne to zaplna log
			} else {
				Logger.debug(SimpleQuery.class, sql + params);
			}
			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;

			//Return number of updated columns
			return updateCount;
		}
		catch (Exception ex)
		{
			exceptionMessage = ex.getMessage();
			IllegalStateException exception = new IllegalStateException(exceptionMessage);
			exception.initCause(ex);
			Logger.error(SimpleQuery.class, "ERROR SQL: "+ sql + params);
			throw exception;
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
				//Return number of updated columns
				return updateCount;
			}
			catch (Exception ex2)
			{
			}
		}
	}

	@SuppressWarnings("unchecked")
	public int forInt(String sql, Object... parameters)
	{
		List<Object> result = forList(sql, parameters);
		if (result.isEmpty()) return 0;

		Object returned = result.get(0);

		if (returned == null)
			return 0;
		//convenient when the result is Long - often appears at table id's
		if (returned instanceof Number)
		{
			return ((Number)returned).intValue();
		}
		throw new IllegalStateException("Return value for SQL is neither a number, nor NULL");
	}

	@SuppressWarnings("unchecked")
	public long forLong(String sql, Object... parameters)
	{
		List<Object> result = forList(sql, parameters);
		if (result.isEmpty()) return 0;

		Object returned = result.get(0);

		if (returned == null)
			return 0;

		if (returned instanceof Number)
			return ((Number)returned).longValue();

		throw new IllegalStateException("Return value for SQL is neither a number, nor NULL");
	}

	@SuppressWarnings("unchecked")
	public String forString(String sql, Object... parameters)
	{
		List<Object> result = forList(sql, parameters);
		if (result.isEmpty()) return null;

		Object o = result.get(0);
		if (o == null)
			return null;

		if (o instanceof Clob)
		{
			try
			{
				Clob clob = (Clob) o;
				long length = clob.length();
				return clob.getSubString(1, (int) length);
			}
			catch (SQLException ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		return result.get(0).toString();
	}

	@SuppressWarnings("unchecked")
	public Object forObject(String sql, Object... parameters)
	{
		List<Object> result = forList(sql, parameters);
		if (result.isEmpty()) return null;
		return result.get(0);
	}

	@SuppressWarnings("unchecked")
	public double forDouble(String sql, Object...parameters)
	{
		List<Object> results = forList(sql, parameters);
		if (results.isEmpty()) return 0;
		Number result = (Number)results.get(0);
		return result.doubleValue();
	}

	@SuppressWarnings("unchecked")
	public BigDecimal forBigDecimal(String sql, Object...parameters)
	{
		List<Object> results = forList(sql, parameters);
		if (results.isEmpty()) return null;
		if (results.get(0) == null)
			return BigDecimal.ZERO;
		return (BigDecimal)results.get(0);
	}

	@SuppressWarnings("unchecked")
	public Boolean forBoolean(String sql, Object...parameters)
	{
		List<Object> results = forList(sql, parameters);
		if (results.isEmpty()) return false;
		if (results.get(0) == null)
			return false;

		Object o = results.get(0);
		if (o instanceof Boolean)
			return (Boolean)o;
		else if (o instanceof Number) {
			return ((Number)o).intValue() == 1;
		} else if (o instanceof String) {
			return "true".equals(o) || "1".equals(o);
		} else {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public Boolean forBooleanWithNull(String sql, Object...parameters)
	{
		List<Object> results = forList(sql, parameters);
		if (results.isEmpty()) return null;
		if (results.get(0) == null) return null;
		Object o = results.get(0);
		if (o instanceof Boolean)
			return (Boolean)o;
		else if (o instanceof Number) {
			return ((Number)o).intValue() == 1;
		} else if (o instanceof String) {
			return "true".equals(o) || "1".equals(o);
		} else {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Integer> forListInteger(String sql, Object... parameters)
	{
		return (List<Integer>)forList(sql, parameters);
	}

	@SuppressWarnings("unchecked")
	public List<Number> forListNumber(String sql, Object... parameters)
	{
		return (List<Number>)forList(sql, parameters);
	}

	@SuppressWarnings("unchecked")
	public List<String> forListString(String sql, Object... parameters)
	{
		return (List<String>)forList(sql, parameters);
	}

	@SuppressWarnings({"rawtypes"})
	public List forList(String sql, Object... parameters)
	{
		String exceptionMessage = null;
		List<Object> toBeReturned = new ArrayList<Object>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String params = null;
		try
		{
			db_conn = DBPool.getConnection(databaseName);
			ps = db_conn.prepareStatement(sql);

			params = bindParameters(ps, parameters);

			rs = ps.executeQuery();

			/*
			 * jeeff: niekedy robilo problem ked sa nepodarilo ziskat metadata, v zasade nas to tu nema co trapit
			int columnCount = rs.getMetaData().getColumnCount();
			if (columnCount > 1)
				throw new IllegalArgumentException("Sql queries for more than one column");
			*/

			while (rs.next())
			{
				Object returnedObject = rs.getObject(1);
				if(returnedObject instanceof net.sourceforge.jtds.jdbc.ClobImpl)
				{
					returnedObject = rs.getString(1);
				}
				toBeReturned.add(returnedObject);
			}

			if (sql.indexOf("cluster_refresher")==-1)
			{
				Logger.debug(SimpleQuery.class, sql+params);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
			return toBeReturned;
		}
		catch (Exception ex)
		{
			exceptionMessage = ex.getMessage();
			IllegalStateException exception = new IllegalStateException(exceptionMessage);
			exception.initCause(ex);
			throw exception;
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

	public boolean executeInTransaction(List<String> sqlCommands, List<Object[]> theirParameters)
	{
		String exceptionMessage = null;
		if (sqlCommands.size() != theirParameters.size())
			throw new IllegalArgumentException("Cannot match sqlCommands to their parameters. Their sizes are not equal");

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection(databaseName);
			db_conn.setAutoCommit(false);

			for(int commandIndex = 0; commandIndex < sqlCommands.size(); commandIndex++)
			{
				String sql = sqlCommands.get(commandIndex);
				ps = db_conn.prepareStatement(sql);

				Object[] parameters = theirParameters.get(commandIndex);

				if (parameters != null)
				{
					int parameterIndex = 1;
					for (Object parameter : parameters)
						ps.setObject(parameterIndex++, parameter);
				}

				ps.execute();
				ps.close();
			}

			db_conn.commit();
			db_conn.setAutoCommit(true);
			db_conn.close();
			db_conn = null;
			ps = null;
			return true;
		}
		catch (Exception ex)
		{
			exceptionMessage = ex.getMessage();
			IllegalStateException exception = new IllegalStateException(exceptionMessage);
			exception.initCause(ex);
			throw exception;
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
				{
					db_conn.rollback();
					db_conn.setAutoCommit(true);
					db_conn.close();
				}
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Date forDate(String sql, Object...parameters)
	{
		List<Object> results = forList(sql, parameters);
		if (results.isEmpty()) return null;

		Date result = null;
		try {
			// toto vracalo v pripade MSSQL string vo formate 2020-03-23 20:00:00.0000000 co samozrejme neslo takto precastovat, takze som si tu dovolil mensiu upravu. suvisi s sk.iway.iwcm.system.UrlRedirectDB.getDateOfNextChange
			result = (Date)results.get(0);
		} catch(java.lang.ClassCastException e) {
			if(results.get(0) instanceof String) {
				//
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSS");
				try {
					result = df.parse((String) results.get(0));
				} catch(ParseException ex) {
					sk.iway.iwcm.Logger.error(e);
				}
			}
		}

		return result;
	}

	/**
	 * Nabinduje parametre pre preparedStatement, Date sa musia bindovat cez setTimestamp, inak sa vracaju zle vysledky
	 * vrati String bindnutych parametrov, ak je logLevel DEBUG
	 * @param ps
	 * @param parameters
	 * @return
	 * @throws SQLException
	 */
	public static String bindParameters(PreparedStatement ps, Object...parameters) throws SQLException
	{
		StringBuilder paramsLog = new StringBuilder(" ");

		int parameterIndex = 1;
		for (Object parameter : parameters)
		{
			if (parameter instanceof Date)
			{
				Date date = (Date)parameter;
				ps.setTimestamp(parameterIndex++, new Timestamp(date.getTime()));
			}
			else if (parameter instanceof Calendar)
			{
				Calendar date = (Calendar) parameter;
				ps.setTimestamp(parameterIndex++, new Timestamp(date.getTime().getTime()));
			}
			else
			{
				ps.setObject(parameterIndex++, parameter);
			}

			if (Logger.isLevel(Logger.DEBUG))
			{
				if (paramsLog.length()>1) paramsLog.append(", ");
				if (parameter == null) paramsLog.append("null");
				else paramsLog.append(String.valueOf(parameter));
			}
		}

		return paramsLog.toString();
	}
}
