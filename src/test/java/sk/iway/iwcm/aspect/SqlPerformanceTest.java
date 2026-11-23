package sk.iway.iwcm.aspect;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.SqlPerformance;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.monitoring.ExecutionTimeMonitor;

class SqlPerformanceTest
{
	private static final String FIRST_SQL = "SELECT sql_performance_first";
	private static final String SECOND_SQL = "SELECT sql_performance_second";
	private static final int SHARED_HASH_CODE = 42;
	private static final int CONCURRENT_STATEMENT_COUNT = 256;
	private static final int CONCURRENT_WORKER_COUNT = 16;

	@Test
	void keepsMeasurementsSeparateForStatementsWithSameHashCode()
		throws Throwable
	{
		SqlPerformance sqlPerformance = new SqlPerformance();
		PreparedStatement firstStatement = statementWithHashCode(SHARED_HASH_CODE);
		PreparedStatement secondStatement = statementWithHashCode(SHARED_HASH_CODE);

		try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
			MockedStatic<ExecutionTimeMonitor> executionTimeMonitor = mockStatic(ExecutionTimeMonitor.class))
		{
			initServlet.when(InitServlet::isWebjetInitialized).thenReturn(true);
			rememberSql(sqlPerformance, firstStatement, FIRST_SQL);
			rememberSql(sqlPerformance, secondStatement, SECOND_SQL);

			sqlPerformance.measureStart(firstStatement);
			sqlPerformance.measureStart(secondStatement);
			sqlPerformance.measureEnd(firstStatement);
			sqlPerformance.measureEnd(secondStatement);

			executionTimeMonitor.verify(() -> ExecutionTimeMonitor.recordSqlExecution(eq(FIRST_SQL), anyLong()), times(1));
			executionTimeMonitor.verify(() -> ExecutionTimeMonitor.recordSqlExecution(eq(SECOND_SQL), anyLong()), times(1));
			executionTimeMonitor.verifyNoMoreInteractions();
		}
	}

	@Test
	void doesNotInvokeJdbcHashCodeOrEquals()
		throws Throwable
	{
		SqlPerformance sqlPerformance = new SqlPerformance();
		PreparedStatement statement = statementRejectingHashCodeAndEquals();

		try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
			MockedStatic<ExecutionTimeMonitor> executionTimeMonitor = mockStatic(ExecutionTimeMonitor.class))
		{
			initServlet.when(InitServlet::isWebjetInitialized).thenReturn(true);
			rememberSql(sqlPerformance, statement, FIRST_SQL);

			sqlPerformance.measureStart(statement);
			sqlPerformance.measureEnd(statement);

			executionTimeMonitor.verify(() -> ExecutionTimeMonitor.recordSqlExecution(eq(FIRST_SQL), anyLong()), times(1));
		}
	}

	@Test
	void recordsConcurrentlyRememberedStatementsWithSameHashCodeIndependently()
		throws Throwable
	{
		SqlPerformance sqlPerformance = new SqlPerformance();
		List<PreparedStatement> statements = new ArrayList<>(CONCURRENT_STATEMENT_COUNT);
		List<String> sqls = new ArrayList<>(CONCURRENT_STATEMENT_COUNT);
		List<Future<?>> futures = new ArrayList<>(CONCURRENT_STATEMENT_COUNT);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_WORKER_COUNT);

		try
		{
			for (int i = 0; i < CONCURRENT_STATEMENT_COUNT; i++)
			{
				PreparedStatement statement = statementWithHashCode(SHARED_HASH_CODE);
				String sql = "SELECT sql_performance_concurrent_" + i;
				statements.add(statement);
				sqls.add(sql);
				futures.add(executor.submit(() -> {
					start.await();
					try
					{
						rememberSql(sqlPerformance, statement, sql);
					}
					catch (Throwable throwable)
					{
						throw new AssertionError(throwable);
					}
					return null;
				}));
			}

			start.countDown();
			for (Future<?> future : futures)
			{
				future.get(30, TimeUnit.SECONDS);
			}
		}
		finally
		{
			start.countDown();
			executor.shutdownNow();
		}

		try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
			MockedStatic<ExecutionTimeMonitor> executionTimeMonitor = mockStatic(ExecutionTimeMonitor.class))
		{
			initServlet.when(InitServlet::isWebjetInitialized).thenReturn(true);
			for (PreparedStatement statement : statements)
			{
				sqlPerformance.measureStart(statement);
				sqlPerformance.measureEnd(statement);
			}

			for (String sql : sqls)
			{
				executionTimeMonitor.verify(() -> ExecutionTimeMonitor.recordSqlExecution(eq(sql), anyLong()), times(1));
			}
			executionTimeMonitor.verifyNoMoreInteractions();
		}
	}

	@Test
	void removesRememberedSqlWhenStatementClosesBeforeInitialization()
		throws Throwable
	{
		SqlPerformance sqlPerformance = new SqlPerformance();
		PreparedStatement statement = statementWithHashCode(SHARED_HASH_CODE);
		AtomicBoolean initialized = new AtomicBoolean(false);

		try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
			MockedStatic<ExecutionTimeMonitor> executionTimeMonitor = mockStatic(ExecutionTimeMonitor.class))
		{
			initServlet.when(InitServlet::isWebjetInitialized).thenAnswer(invocation -> initialized.get());
			rememberSql(sqlPerformance, statement, FIRST_SQL);

			sqlPerformance.measureStart(statement);
			sqlPerformance.measureEnd(statement);

			initialized.set(true);
			sqlPerformance.measureStart(statement);
			sqlPerformance.measureEnd(statement);

			executionTimeMonitor.verifyNoInteractions();
		}
	}

	@Test
	void wovenSimpleQueryRecordsSqlFromPreparationUntilClose()
		throws SQLException
	{
		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		when(connection.prepareStatement(FIRST_SQL)).thenReturn(statement);

		try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
			MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
			MockedStatic<Logger> logger = mockStatic(Logger.class);
			MockedStatic<ExecutionTimeMonitor> executionTimeMonitor = mockStatic(ExecutionTimeMonitor.class))
		{
			initServlet.when(InitServlet::isWebjetInitialized).thenReturn(true);
			dbPool.when(() -> DBPool.getConnection("iwcm")).thenReturn(connection);

			new SimpleQuery().execute(FIRST_SQL);

			verify(statement).execute();
			verify(statement).close();
			executionTimeMonitor.verify(() -> ExecutionTimeMonitor.recordSqlExecution(eq(FIRST_SQL), anyLong()), times(1));
		}
	}

	private void rememberSql(SqlPerformance sqlPerformance, PreparedStatement statement, String sql)
		throws Throwable
	{
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getArgs()).thenReturn(new Object[] { sql });
		when(joinPoint.proceed()).thenReturn(statement);

		assertSame(statement, sqlPerformance.rememberSql(joinPoint, mock(Connection.class)));
	}

	private PreparedStatement statementWithHashCode(int hashCode)
	{
		return (PreparedStatement) Proxy.newProxyInstance(
			PreparedStatement.class.getClassLoader(),
			new Class<?>[] { PreparedStatement.class },
			(proxy, method, arguments) -> {
				if ("hashCode".equals(method.getName())) return hashCode;
				if ("equals".equals(method.getName())) return proxy == arguments[0];
				if ("toString".equals(method.getName())) return "PreparedStatement[hashCode=" + hashCode + "]";
				throw new UnsupportedOperationException(method.getName());
			});
	}

	private PreparedStatement statementRejectingHashCodeAndEquals()
	{
		return (PreparedStatement) Proxy.newProxyInstance(
			PreparedStatement.class.getClassLoader(),
			new Class<?>[] { PreparedStatement.class },
			(proxy, method, arguments) -> {
				if ("hashCode".equals(method.getName()) || "equals".equals(method.getName()))
				{
					throw new AssertionError("JDBC " + method.getName() + " must not be used as an identity key");
				}
				if ("toString".equals(method.getName())) return "PreparedStatement[identity-only]";
				throw new UnsupportedOperationException(method.getName());
			});
	}
}
