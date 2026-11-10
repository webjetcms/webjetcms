package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.i18n.Prop;

class HistoryDBTest
{
	@ParameterizedTest
	@ValueSource(ints = {Constants.DB_ORACLE, Constants.DB_MYSQL})
	void historyQueriesSplitMoreThanOneThousandIds(int databaseType) throws Exception
	{
		int originalDatabaseType = Constants.DB_TYPE;
		Connection connection = mock(Connection.class);
		PreparedStatement idsStatement = mock(PreparedStatement.class);
		PreparedStatement historyStatement = mock(PreparedStatement.class);
		ResultSet ids = mock(ResultSet.class);
		ResultSet history = mock(ResultSet.class);
		AtomicInteger currentId = new AtomicInteger();
		List<String> historyQueries = new ArrayList<>();
		when(ids.next()).thenAnswer(invocation -> currentId.incrementAndGet() <= 1001);
		when(ids.getInt("history_id")).thenAnswer(invocation -> currentId.get());
		when(idsStatement.executeQuery()).thenReturn(ids);
		when(historyStatement.executeQuery()).thenReturn(history);
		when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
			String sql = invocation.getArgument(0);
			if (sql.equals("SELECT history_id FROM documents_history WHERE doc_id=?")) return idsStatement;
			historyQueries.add(sql);
			return historyStatement;
		});

		try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
			MockedStatic<Prop> prop = mockStatic(Prop.class);
			MockedStatic<DocDB> documents = mockStatic(DocDB.class))
		{
			Constants.DB_TYPE = databaseType;
			dbPool.when(() -> DBPool.getConnection("iwcm")).thenReturn(connection);
			prop.when(Prop::getInstance).thenReturn(mock(Prop.class));
			documents.when(() -> DocDB.getDocumentFields(false)).thenReturn("d.doc_id");

			new HistoryDB("iwcm").getHistory(42);

			assertEquals(2, historyQueries.size(), "Both published and unpublished history must be queried");
			String expectedIds = IntStream.rangeClosed(1, 1001).mapToObj(Integer::toString).collect(Collectors.joining(","));
			for (int i = 0; i < historyQueries.size(); i++)
			{
				String sql = historyQueries.get(i);
				Matcher lists = Pattern.compile("(?:d\\.)?history_id IN \\(([^)]+)\\)").matcher(sql);
				List<String> chunks = new ArrayList<>();
				while (lists.find())
				{
					String chunk = lists.group(1);
					assertTrue(chunk.split(",").length <= 1000, "An IN list must not exceed Oracle's 1000-expression limit");
					chunks.add(chunk);
				}
				assertEquals(expectedIds, String.join(",", chunks), "Every history ID must be retained in order");
				assertEquals(2, chunks.size());
				assertTrue(sql.contains("(d.history_id IN ("));
				assertTrue(sql.contains(") OR d.history_id IN (1001)) AND d.publicable=" + DB.getBooleanSql(i == 0)),
					"The publicable filter must apply to every IN chunk");
				assertTrue(sql.endsWith("ORDER BY history_id DESC "));
			}
		}
		finally
		{
			Constants.DB_TYPE = originalDatabaseType;
		}
	}
}
