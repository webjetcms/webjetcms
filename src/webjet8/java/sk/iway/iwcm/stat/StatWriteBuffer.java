package sk.iway.iwcm.stat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;

/**
 *  StatWriteBuffer.java
 *
 *  Acts as a synchronized buffer for Stat* inserts. Made this way in order to avoid
 *  database congestion caused by every user's HTTP request filling the database.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 6.7.2010 18:35:44
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class StatWriteBuffer
{
	//guards access to buffer
	private static final Lock lock = new ReentrantLock();
	private static Map<UpdateInsertSqlPair, List<Object[]>> buffer = new HashMap<>();
	//needed to construct a table from SQLException
	private static Map<UpdateInsertSqlPair, String> sqlToTable = new HashMap<>();
	private static int pendingCounter = 0;
	private static Random random = new Random();

	/**
	 * Adds an SQL statement into a buffer queue.
	 *
	 * @param sql SQL statement pushed into prepareStatement allocation
	 * @param table Base table name used for table creation if SQL fails. For inserting into stat_error_2010_07, pass "stat_error" as a parameter
	 * @param parameters Pushed into prepareStatement setObject calls
	 */
	public static void add(String sql, String table, Object...parameters)
	{
		UpdateInsertSqlPair insertOnly = new UpdateInsertSqlPair(null, sql);
		Logger.debug(StatWriteBuffer.class, String.format("Appending to buffer: %s ", sql));
		appendToBuffer(insertOnly, table, parameters);
	}

	/**
	 * Serves as a facade for a re-occuring pattern in stat tables:
	 * <code>
	 * 	int rowsTouched = executeUpdate();
	 * 	//insert new values into database in case there are none
	 * 	if (rowsTouched == 0)
	 * 		executeInsert();
	 * </code>
	 *
	 * Acts in a same way as add method
	 *
	 * <b>NOTE: insert and update must share the same number of parameters for this method to work correctly</b>
	 *
	 * @param SEE add(String, String, Object...)
	 */
	public static void addUpdateInsertPair(String update, String insert, String table, Object...parameters)
	{
		UpdateInsertSqlPair sql = new UpdateInsertSqlPair(update, insert);
		Logger.debug(StatWriteBuffer.class, String.format("Appending to buffer: %s ", sql.toString()));
		appendToBuffer(sql, table, parameters);
	}

	private static void appendToBuffer(UpdateInsertSqlPair sql, String table, Object... parameters)
	{
		lock.lock();
		try
		{
			List<Object[]> statements = buffer.get(sql);
			if (statements == null)
				statements = new ArrayList<>();
			statements.add(parameters);
			sqlToTable.put(sql, table);
			buffer.put(sql, statements);
			pendingCounter++;
		}
		finally
		{
			lock.unlock();
		}
	}


	private static Map<UpdateInsertSqlPair, List<Object[]>> releaseBuffer()
	{
		lock.lock();
		Map<UpdateInsertSqlPair, List<Object[]>> oldBuffer = buffer;
		buffer = new HashMap<>();
		pendingCounter = 0;
		return oldBuffer;
	}

	public static int size()
	{
		return pendingCounter;
	}


	/**
	 * Sweeps the buffer and flushes its contents into database.
	 * Launched by a background cron task.
	 */
	public static void main(String[] args)
	{
		try{
			//sk.iway.iwcm.Logger.debug(StatWriteBuffer.class, "About to flush statistics buffer");
			int statements = flushBuffer();
			if (statements > 0)
			{
				sk.iway.iwcm.Logger.debug(StatWriteBuffer.class, String.format("Flushing stats done: %d statements executed", statements));
			}
		}
		catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Flushes statistics data to database, effectively re-using previously
	 * prepared SQL statements for better performance.
	 *
	 * @return number of SQL statements executed
	 */
	private static int flushBuffer()
	{
		//v clustri dochadza ku konfliktom ked sa zapis spusti z cronu naraz, spravime nahodny sleep
		try
		{
			long rndSleep = random.nextInt(10000);
			Logger.println(StatWriteBuffer.class, "sleep for: "+rndSleep+" ms");
			Thread.sleep(rndSleep);
		}
		catch (InterruptedException e){sk.iway.iwcm.Logger.error(e);}

		int flushCounter = 0;
		Map<UpdateInsertSqlPair, List<Object[]>> oldBuffer = releaseBuffer();
		Map<UpdateInsertSqlPair, String> oldMapping = releaseSqlToTableMapping();
		lock.unlock();
		for (Entry<UpdateInsertSqlPair, List<Object[]>> entry : oldBuffer.entrySet())
		{
			//if the first attempt fails, the reason liess probably in a non-existing table
			//In that case, the method attempts to create the table from received SQL Exception
			boolean success = batchSave(entry.getKey(), entry.getValue(), oldMapping);
			if (!success)
				batchSave(entry.getKey(), entry.getValue(), oldMapping);
			flushCounter += entry.getValue().size();
		}
		return flushCounter;
	}

	private static Map<UpdateInsertSqlPair, String> releaseSqlToTableMapping()
	{
		Map<UpdateInsertSqlPair, String> sqlToTableName = sqlToTable;
		sqlToTable = new HashMap<>();
		return sqlToTableName;
	}

	private static boolean batchSave(UpdateInsertSqlPair sql, List<Object[]> statements, Map<UpdateInsertSqlPair, String> oldMapping)
	{
		Logger.debug(StatWriteBuffer.class, String.format("About to flush: %s", sql.toString()));
		Connection db_conn = null;
		PreparedStatement ps = null;
		PreparedStatement psFollowing = null;
		try
		{
			db_conn = DBPool.getConnection();
			boolean isPair = sql.firstSql != null;
			ps = db_conn.prepareStatement(isPair ? sql.firstSql : sql.followingSql);
			psFollowing = isPair ? db_conn.prepareStatement(sql.followingSql) :null;

			for (Object[] params : statements)
			{
				setParams(params, ps);
				int rowsTouched = ps.executeUpdate();
				if (isPair && rowsTouched == 0)
				{
					setParams(params, psFollowing);
					if (psFollowing!=null) psFollowing.execute();
				}
			}

			ps.close();
			ps = null;
			db_conn.close();
			db_conn = null;
			return true;
		}
		catch (Exception ex)
		{
			StatNewDB.createStatTablesFromError(ex.getMessage(), null, oldMapping.get(sql));
			sk.iway.iwcm.Logger.error(ex);
			return false;
		}
		finally{
			try{
				if (ps != null) ps.close();
				if (db_conn != null)	db_conn.close();
			}
			catch (Exception ex2){sk.iway.iwcm.Logger.error(ex2);}
		}
	}

	private static void setParams(Object[] params, PreparedStatement ps) throws SQLException
	{
		int parameterIndex = 1;
		for (Object param : params)
		{
			ps.setObject(parameterIndex++, param);
		}
	}
}