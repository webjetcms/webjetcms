package sk;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.system.monitoring.ExecutionTimeMonitor;

/**
 * Records SQL execution time from prepared-statement execution until close.
 */
@Aspect
public class SqlPerformance
{
	private final Map<Integer, Long> sqlExecutionStarts = new ConcurrentHashMap<>();
	private final Map<Integer, String> sqlsByPreparedStatementHashCodes = new ConcurrentHashMap<>();

	@Pointcut(value = "within(sk.iway..*) && target(connection) && " +
		"(call(* prepareStatement(java.lang.String)) || call(* prepareStatement(java.lang.String, int, int)))", argNames = "connection")
	public void sqlCreation(Connection connection)
	{
	}

	@Pointcut(value = "within(sk.iway..*) && target(statement) && " +
		"(call(* executeUpdate()) || call(* executeQuery()) || call(* execute()))", argNames = "statement")
	public void sqlStart(PreparedStatement statement)
	{
	}

	@Pointcut(value = "within(sk.iway..*) && target(statement) && call(* close())", argNames = "statement")
	public void sqlEnd(PreparedStatement statement)
	{
	}

	@Around(value = "sqlCreation(connection)", argNames = "connection")
	public PreparedStatement rememberSql(ProceedingJoinPoint joinPoint, Connection connection) throws Throwable
	{
		String sql = joinPoint.getArgs()[0].toString();
		PreparedStatement statement = (PreparedStatement) joinPoint.proceed();
		sqlsByPreparedStatementHashCodes.put(statement.hashCode(), sql);
		return statement;
	}

	@Before(value = "sqlStart(statement)", argNames = "statement")
	public void measureStart(PreparedStatement statement)
	{
		if (InitServlet.isWebjetInitialized()==false) return;
		sqlExecutionStarts.put(statement.hashCode(), System.currentTimeMillis());
	}

	@Before(value = "sqlEnd(statement)", argNames = "statement")
	public void measureEnd(PreparedStatement statement)
	{
		if (InitServlet.isWebjetInitialized()==false) return;
		try
		{
			Long start = sqlExecutionStarts.get(statement.hashCode());
			if (start == null) return;

			String sql = sqlsByPreparedStatementHashCodes.get(statement.hashCode());
			if (sql != null)
			{
				long timeTaken = System.currentTimeMillis() - start;
				ExecutionTimeMonitor.recordSqlExecution(sql, timeTaken);
			}
		}
		finally
		{
			sqlExecutionStarts.remove(statement.hashCode());
			sqlsByPreparedStatementHashCodes.remove(statement.hashCode());
		}
	}
}
