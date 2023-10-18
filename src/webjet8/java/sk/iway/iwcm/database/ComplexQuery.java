package sk.iway.iwcm.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;

/**
 *  ComplexQuery.java
 *  A class encapsulating complex ResultSet manipulation
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: jeeff thaber $
 *@version      $Revision: 1.3 $
 *@created      Date: 25.6.2010 14:27:41
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ComplexQuery
{
	private String sql;
	private Object[] params = {};
	private String database = "iwcm";
	private boolean streamingResultSet = false;
	private Integer MAX_SIZE = null;

	public ComplexQuery setSql(String sql)
	{
		this.sql = sql;
		return this;
	}

	public ComplexQuery setParams(Object... params)
	{
		this.params = params;
		return this;
	}

	public ComplexQuery setMaxSize(Integer MAX_SIZE)
	{
		this.MAX_SIZE = MAX_SIZE;
		return this;
	}

	private String insertSubstringAfterFirstWord(String originalString, String specificWord, String substringToInsert) {
        int index = originalString.indexOf(specificWord);
        if (index != -1) {
            // Found the specific word, insert the substring after it
            StringBuilder stringBuilder = new StringBuilder(originalString);
            stringBuilder.insert(index + specificWord.length(), substringToInsert);
            return stringBuilder.toString();
        } else {
            // The specific word was not found, return the original string as is
            return originalString;
        }
    }

	/**
	 * return list of object selected by provided query and params
	 * @param <T>
	 * @param mapper mapper object {@link Mapper}
	 * @return list of objects
	 */
	public <T> List<T> list(Mapper<T> mapper)
	{
		List<T> result = new ArrayList<T>();
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			if(MAX_SIZE != null && MAX_SIZE > 0) {
				if(sql.toUpperCase().startsWith("SELECT")) {
					if (Constants.DB_TYPE == Constants.DB_MYSQL) {
						//Using LIMIT
						if(!sql.toUpperCase().contains("LIMIT")) 
							sql += " LIMIT 0," + MAX_SIZE;
					} else if(Constants.DB_TYPE == Constants.DB_MSSQL) {
						//Using TOP
						if(!sql.toUpperCase().contains("TOP"))
							sql = insertSubstringAfterFirstWord(sql, "SELECT", " TOP " + MAX_SIZE + " ");
					} else if (Constants.DB_TYPE == Constants.DB_ORACLE) {
						//Using rownum
						if(!sql.toUpperCase().contains("rownum"))
							sql = insertSubstringAfterFirstWord(sql, "WHERE", " rownum < " + MAX_SIZE + " AND ");
					}
				}
			}

			db_conn = DBPool.getConnection(database);
			if (streamingResultSet)
			{
				DBPool.setTransactionIsolationReadUNCommited(db_conn);
				ps = db_conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

				if (Constants.DB_TYPE==Constants.DB_MYSQL) ps.setFetchSize(Integer.MIN_VALUE);
			   else ps.setFetchSize(1);
			}
			else
			{
				ps = db_conn.prepareStatement(sql);
			}

			for (int index = 0; index < params.length; index++)
			{
				ps.setObject(index+1, params[index]);
			}

			rs = ps.executeQuery();
			Logger.debug(ComplexQuery.class, "list: Executed query: "+sql);
			while (rs.next())
			{
				result.add(mapper.map(rs));
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
			boolean showError = true;
			if (sql.startsWith("SELECT MAX("))
			{
				//toto je chyba pri inicializacii pkey generatora a nevyzera to dobre v logu pri starte
				if (ex.getMessage().contains("Invalid object name") || ex.getMessage().contains("Invalid column name") || ex.getMessage().contains("ORA-00942") ||
                        ex.getMessage().contains("Unknown column") || ex.getMessage().contains("doesn't exist") || ex.getLocalizedMessage().contains("doesn't exist"))
				{
					showError = false;
				}
			}

			if (showError) sk.iway.iwcm.Logger.error(ex);
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
		return result;
	}
	/**
	 * Retrieves single object from query, throws Exception if query returns more than one result
	 * @param <T>
	 * @param mapper
	 * @return
	 */
	public <T> T singleResult(Mapper<T> mapper)
	{
		List<T> list = list(mapper);
		if (list.size()!=1)
		{
			throw new IllegalStateException("Result not of size one.");
		}
		return list.get(0);
	}
	public ComplexQuery setDatabase(String database) {
		this.database = database;
		return this;
	}

	public boolean isStreamingResultSet()
	{
		return streamingResultSet;
	}

	public void setStreamingResultSet(boolean streamingResultSet)
	{
		this.streamingResultSet = streamingResultSet;
	}
}
