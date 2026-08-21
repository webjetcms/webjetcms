package sk.iway.iwcm.aspect;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.database.SimpleQuery;

class AspectExceptionTest
{
	private static final String SQL = "SELECT aspect_exception_test";
	private static final String EXCEPTION_MESSAGE = "aspect-exception-test";

	@Test
	void logsCaughtJdbcException()
		throws SQLException
	{
		Cache cache = mock(Cache.class);
		Connection connection = mock(Connection.class);
		when(connection.prepareStatement(SQL)).thenThrow(new SQLException(EXCEPTION_MESSAGE));

		try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
			MockedStatic<Cache> cacheStatic = mockStatic(Cache.class);
			MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
			MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class))
		{
			initServlet.when(InitServlet::isWebjetInitialized).thenReturn(true);
			cacheStatic.when(Cache::getInstance).thenReturn(cache);
			dbPool.when(() -> DBPool.getConnection("iwcm")).thenReturn(connection);

			new SimpleQuery().execute(SQL);

			adminlog.verify(() -> Adminlog.add(
				eq(Adminlog.TYPE_SQLERROR),
				argThat(description -> description.contains(EXCEPTION_MESSAGE) &&
					description.contains("source: sk.iway.iwcm.database.SimpleQuery:")),
				eq(-1),
				eq(-1)));
		}
	}
}
