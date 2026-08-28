package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 *  AdminlogJUnit.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 14.04.2010 11:29:31
 *@modified     $Date: 2009/12/11 15:42:33 $
 */
public class AdminlogTest extends BaseWebjetTest
{
	private static final String LAST_DATE_BY_SUB_IDS_SQL = "SELECT max(create_date) as create_date FROM "
		+ ConfDB.ADMINLOG_TABLE_NAME
		+ " WHERE user_id=? AND log_type=? AND sub_id1=? AND sub_id2=?";

	@Test
	public void typesAutoloading()
	{
		Integer[] types = Adminlog.getTypes();
		assertFalse(ArrayUtils.contains(types, null));
		assertTrue(ArrayUtils.getLength(types)>=122);
	}

	@Test
	void getLastDateShouldFilterByBothSubIds() throws Exception
	{
		int userId = 123;
		int subId1 = 456;
		int subId2 = 789;
		Timestamp expectedTimestamp = new Timestamp(1_700_000_000_000L);

		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		ResultSet result = mock(ResultSet.class);

		when(connection.prepareStatement(LAST_DATE_BY_SUB_IDS_SQL)).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(result);
		when(result.next()).thenReturn(true);
		when(result.getTimestamp("create_date")).thenReturn(expectedTimestamp);

		try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class))
		{
			dbPool.when(DBPool::getConnection).thenReturn(connection);

			assertEquals(expectedTimestamp.getTime(),
				Adminlog.getLastDate(Adminlog.TYPE_USER_CHANGE_PASSWORD, userId, subId1, subId2));
		}

		verify(connection).prepareStatement(LAST_DATE_BY_SUB_IDS_SQL);
		verify(statement).setInt(1, userId);
		verify(statement).setInt(2, Adminlog.TYPE_USER_CHANGE_PASSWORD);
		verify(statement).setInt(3, subId1);
		verify(statement).setInt(4, subId2);
	}
}
