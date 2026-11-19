package sk.iway.iwcm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;

class KalendarUdalostiDBTest
{
	private int originalDatabaseType;
	private int originalMaxItems;
	private boolean originalPerexGroupUseJoin;

	@BeforeEach
	void setUp()
	{
		originalDatabaseType = Constants.DB_TYPE;
		originalMaxItems = Constants.getInt("dbInClauseMaxItems");
		originalPerexGroupUseJoin = Constants.getBoolean("perexGroupUseJoin");
		Constants.DB_TYPE = Constants.DB_ORACLE;
		Constants.setInt("dbInClauseMaxItems", 2);
	}

	@AfterEach
	void tearDown()
	{
		Constants.DB_TYPE = originalDatabaseType;
		Constants.setInt("dbInClauseMaxItems", originalMaxItems);
		Constants.setBoolean("perexGroupUseJoin", originalPerexGroupUseJoin);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void perexFiltersKeepDateParameterOrderAndPortableRowLimit(boolean joined) throws Exception
	{
		Constants.setBoolean("perexGroupUseJoin", joined);
		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(mock(ResultSet.class));
		try (MockedStatic<DBPool> pool = mockStatic(DBPool.class))
		{
			pool.when(DBPool::getConnection).thenReturn(connection);
			ReflectionTestUtils.invokeMethod(KalendarUdalostiDB.class, "getEventsFromDB",
				false, "10+11", false, joined ? "7+invalid+9+0+12" : "7+9+12",
				KalendarUdalostiDB.SHOW_OLD, null, null, 0, true, 5);
		}

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(connection).prepareStatement(sql.capture());
		String query = sql.getValue();
		assertTrue(query.contains("WHERE (d.group_id IN (10,11)) AND d.available = "));
		assertTrue(query.endsWith(" AND (publish_end IS NOT null AND publish_end < ?) ORDER BY d.publish_start DESC"));
		assertFalse(query.contains(" LIMIT "));
		if (joined)
		{
			assertTrue(query.contains(" AND EXISTS (SELECT 1 FROM perex_group_doc p WHERE p.doc_id=d.doc_id AND "
				+ "(p.perex_group_id IN (?,?) OR p.perex_group_id IN (?))) AND "));
			assertEquals(4, query.chars().filter(character -> character == '?').count());
			verify(statement).setInt(1, 7);
			verify(statement).setInt(2, 9);
			verify(statement).setInt(3, 12);
			verify(statement).setTimestamp(eq(4), any(Timestamp.class));
		}
		else
		{
			assertFalse(query.contains("perex_group_doc"));
			assertEquals(10, query.chars().filter(character -> character == '?').count());
			String[] ids = {"7", "9", "12"};
			for (int i = 0; i < ids.length; i++)
			{
				verify(statement).setString(3 * i + 1, "%," + ids[i] + ",%");
				verify(statement).setString(3 * i + 2, "%," + ids[i]);
				verify(statement).setString(3 * i + 3, ids[i] + ",%");
			}
			verify(statement).setTimestamp(eq(10), any(Timestamp.class));
		}
		verify(statement).setMaxRows(5);
		verify(statement).executeQuery();
	}
}
