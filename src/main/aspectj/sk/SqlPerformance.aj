package sk;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.system.monitoring.ExecutionTimeMonitor;

/**
 *  SqlPerformance.aj
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Date: 19.8.2009 18:32:58
 *@modified     $Date: 2009/11/16 08:48:48 $
 */
public aspect SqlPerformance
{
	public pointcut sqlCreation(Connection connection):
		within(sk.iway..*) && !within(sk.iway.iwcm.update.UpdateAction) && !within(sk.iway.cps..*) && target(connection) &&
		(call(* prepareStatement(String)) || call(* prepareStatement(String, int, int)));
		
	public pointcut sqlStart(PreparedStatement statement):
		within(sk.iway..*) && !within(sk.iway.iwcm.update.UpdateAction) && !within(sk.iway.cps..*) && target(statement) &&
		(call(* executeUpdate()) || call(* executeQuery()) || call (* execute()));
		
	public pointcut sqlEnd(PreparedStatement statement):
		within(sk.iway..*) && !within(sk.iway.iwcm.update.UpdateAction) && !within(sk.iway.cps..*) && target(statement) &&
		call(* close());
	
	private Map<Integer, Long> sqlExecutionStarts = new ConcurrentHashMap<Integer, Long>();
	private Map<Integer, String> sqlsByPreparedStatementhashCodes = new ConcurrentHashMap<Integer, String>();
	
	PreparedStatement around(Connection connection): sqlCreation(connection)
	{
		String sql = thisJoinPoint.getArgs()[0].toString();
		Object statement = proceed(connection);
		sqlsByPreparedStatementhashCodes.put(statement.hashCode(), sql);
		return (PreparedStatement)statement;
	}
	
	before(PreparedStatement statement): sqlStart(statement)
	{		
		measureStart(statement);
	}
	
	void measureStart(PreparedStatement statement)
	{
		if (InitServlet.isWebjetInitialized()==false) return;
		sqlExecutionStarts.put(statement.hashCode(), System.currentTimeMillis());
	}
	
	before(PreparedStatement statement): sqlEnd(statement)
	{
		measureEnd(statement);
	}
	
	void measureEnd(PreparedStatement statement)
	{
		if (InitServlet.isWebjetInitialized()==false) return;
		try
		{
			Long start = sqlExecutionStarts.get(statement.hashCode());
			if (start == null)
				return;
	
			String sql = sqlsByPreparedStatementhashCodes.get(statement.hashCode());
			if (sql != null)
			{
				long timeTaken = System.currentTimeMillis() - start.longValue();			
				ExecutionTimeMonitor.recordSqlExecution(sql, timeTaken);
			}
		}
		finally
		{
			sqlExecutionStarts.remove(statement.hashCode());
			sqlsByPreparedStatementhashCodes.remove(statement.hashCode());
		}
	}
}