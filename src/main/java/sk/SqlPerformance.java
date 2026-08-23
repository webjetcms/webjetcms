package sk;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

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
	private static final long NOT_STARTED = Long.MIN_VALUE;
	private static final long CLOSED = Long.MIN_VALUE + 1;

	private final ConcurrentMap<StatementKey, SqlMeasurement> measurements = new ConcurrentHashMap<>();

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
		measurements.put(new StatementKey(statement), new SqlMeasurement(sql));
		return statement;
	}

	@Before(value = "sqlStart(statement)", argNames = "statement")
	public void measureStart(PreparedStatement statement)
	{
		if (InitServlet.isWebjetInitialized()==false) return;

		SqlMeasurement measurement = measurements.get(new StatementKey(statement));
		if (measurement != null)
		{
			measurement.start(System.currentTimeMillis());
		}
	}

	@Before(value = "sqlEnd(statement)", argNames = "statement")
	public void measureEnd(PreparedStatement statement)
	{
		SqlMeasurement measurement = measurements.remove(new StatementKey(statement));
		if (measurement == null) return;

		long start = measurement.close();
		if (InitServlet.isWebjetInitialized() && start != NOT_STARTED && start != CLOSED)
		{
			long timeTaken = System.currentTimeMillis() - start;
			ExecutionTimeMonitor.recordSqlExecution(measurement.sql, timeTaken);
		}
	}

	private static final class StatementKey
	{
		private final PreparedStatement statement;
		private final int hashCode;

		private StatementKey(PreparedStatement statement)
		{
			this.statement = statement;
			hashCode = System.identityHashCode(statement);
		}

		@Override
		public boolean equals(Object object)
		{
			if (this == object) return true;
			if (object instanceof StatementKey == false) return false;

			StatementKey other = (StatementKey) object;
			return statement == other.statement;
		}

		@Override
		public int hashCode()
		{
			return hashCode;
		}
	}

	private static final class SqlMeasurement
	{
		private final String sql;
		private final AtomicLong startedAt = new AtomicLong(NOT_STARTED);

		private SqlMeasurement(String sql)
		{
			this.sql = sql;
		}

		private void start(long startTime)
		{
			long currentStart;
			do
			{
				currentStart = startedAt.get();
				if (currentStart == CLOSED) return;
			}
			while (startedAt.compareAndSet(currentStart, startTime)==false);
		}

		private long close()
		{
			return startedAt.getAndSet(CLOSED);
		}
	}
}
