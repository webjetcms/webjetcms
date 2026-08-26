package sk.iway.iwcm.inquiry;

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

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.common.CloudToolsForCore;

class InquiryDBTest
{
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"answer_id DESC", "(SELECT password FROM users)", "ia.answer_id--", "DBMS_RANDOM.VALUE", "unknown.answer_id"})
	void resolveOrderByUsesDefaultForInvalidValues(String orderBy)
	{
		assertEquals("ia.answer_id", InquiryDB.resolveOrderBy(orderBy));
	}

	@Test
	void resolveOrderByPrefixesUnqualifiedColumn()
	{
		assertEquals("ia.answer_clicks", InquiryDB.resolveOrderBy("answer_clicks"));
	}

	@Test
	void resolveOrderByPreservesQualifiedColumn()
	{
		assertEquals("i.question_id", InquiryDB.resolveOrderBy("i.question_id"));
	}

	@Test
	void resolveOrderByBindsArbitraryColumnToDefaultAlias()
	{
		assertEquals("ia.WJINJECTMARKER99", InquiryDB.resolveOrderBy("WJINJECTMARKER99"));
	}

	@Test
	void getInquiryIdsBindsCommaAndPlusSeparatedGroups() throws Exception
	{
		HttpServletRequest request = mock(HttpServletRequest.class);
		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		ResultSet resultSet = mock(ResultSet.class);

		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(false);

		try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
			MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class))
		{
			dbPool.when(() -> DBPool.getDBName(request)).thenReturn("test");
			dbPool.when(() -> DBPool.getConnection("test")).thenReturn(connection);
			cloudTools.when(() -> CloudToolsForCore.getDomainIdSqlWhere(true)).thenReturn(" AND domain_id = 1");

			assertTrue(InquiryDB.getInquiryIds("alpha,O'Brien+gamma", request, true).isEmpty());
		}

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		verify(connection).prepareStatement(sqlCaptor.capture());
		String sql = sqlCaptor.getValue();
		assertTrue(sql.contains("question_group IN (?, ?, ?)"));
		assertFalse(sql.contains("alpha"));
		assertFalse(sql.contains("O'Brien"));
		assertEquals(5L, sql.chars().filter(character -> character == '?').count());
		verify(statement).setString(1, "alpha");
		verify(statement).setString(2, "O'Brien");
		verify(statement).setString(3, "gamma");
		verify(statement).setTimestamp(eq(4), any(Timestamp.class));
		verify(statement).setTimestamp(eq(5), any(Timestamp.class));
	}

	@Test
	void getInquiryIdsKeepsInjectionPayloadOutOfSql() throws Exception
	{
		String injectedGroup = ") OR (? IS NOT NULL AND ? IS NOT NULL) -- ";
		HttpServletRequest request = mock(HttpServletRequest.class);
		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		ResultSet resultSet = mock(ResultSet.class);

		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(false);

		try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
			MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class))
		{
			dbPool.when(() -> DBPool.getDBName(request)).thenReturn("test");
			dbPool.when(() -> DBPool.getConnection("test")).thenReturn(connection);
			cloudTools.when(() -> CloudToolsForCore.getDomainIdSqlWhere(true)).thenReturn(" AND domain_id = 1");

			assertTrue(InquiryDB.getInquiryIds("x\\,|+" + injectedGroup, request, true).isEmpty());
		}

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		verify(connection).prepareStatement(sqlCaptor.capture());
		String sql = sqlCaptor.getValue();
		assertTrue(sql.contains("question_group IN (?, ?, ?)"));
		assertFalse(sql.contains(injectedGroup));
		assertFalse(sql.contains("--"));
		assertEquals(5L, sql.chars().filter(character -> character == '?').count());
		verify(statement).setString(1, "x\\");
		verify(statement).setString(2, "|");
		verify(statement).setString(3, injectedGroup);
		verify(statement).setTimestamp(eq(4), any(Timestamp.class));
		verify(statement).setTimestamp(eq(5), any(Timestamp.class));
	}

	@Test
	void getOldInquiryKeepsInjectionPayloadOutOfSql() throws Exception
	{
		String injectedGroup = ") OR 1=1 -- ";
		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		ResultSet resultSet = mock(ResultSet.class);

		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(false);

		try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
			MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class))
		{
			dbPool.when(DBPool::getConnection).thenReturn(connection);
			cloudTools.when(() -> CloudToolsForCore.getDomainIdSqlWhere(true)).thenReturn(" AND domain_id = 1");

			assertTrue(InquiryDB.getOldInquiry("x\\," + injectedGroup, true).isEmpty());
		}

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		verify(connection).prepareStatement(sqlCaptor.capture());
		String sql = sqlCaptor.getValue();
		assertTrue(sql.contains("question_group IN (?, ?)"));
		assertTrue(sql.contains("ORDER BY date_from ASC, question_id ASC"));
		assertFalse(sql.contains(injectedGroup));
		assertFalse(sql.contains("--"));
		assertEquals(2L, sql.chars().filter(character -> character == '?').count());
		verify(statement).setString(1, "x\\");
		verify(statement).setString(2, injectedGroup);
	}
}
