package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;

class DocumentTreeInConditionTest
{
	private int originalDatabaseType;
	private int originalMaxItems;

	@BeforeEach
	void setUp()
	{
		originalDatabaseType = Constants.DB_TYPE;
		originalMaxItems = Constants.getInt("dbInClauseMaxItems");
		Constants.DB_TYPE = Constants.DB_ORACLE;
		Constants.setInt("dbInClauseMaxItems", 1000);
	}

	@AfterEach
	void tearDown()
	{
		Constants.DB_TYPE = originalDatabaseType;
		Constants.setInt("dbInClauseMaxItems", originalMaxItems);
	}

	@Test
	void inheritedMenuSettingsKeepTheWholeSubtreeAndBindPositions() throws Exception
	{
		GroupsDB groups = mock(GroupsDB.class, CALLS_REAL_METHODS);
		doReturn(tree(2, 1001, "example.com")).when(groups).getGroupsTree(1, false, true);
		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		try (MockedStatic<DBPool> pool = mockStatic(DBPool.class);
			MockedStatic<Adminlog> log = mockStatic(Adminlog.class);
			MockedStatic<GroupsDB> cache = mockStatic(GroupsDB.class))
		{
			pool.when(DBPool::getConnection).thenReturn(connection);
			groups.setInternalAndMenuTypeToSubgroups(1, true, 2, 3);
		}
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(connection).prepareStatement(sql.capture());
		assertIds(sql.getValue(), 1001);
		verify(statement).setBoolean(1, true);
		verify(statement).setInt(2, 2);
		verify(statement).setInt(3, 3);
		verify(statement).execute();
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void lastModifiedQueryPreservesCloudDomainFiltering(boolean allGroupsForeign) throws Exception
	{
		GroupsDB groups = mock(GroupsDB.class);
		List<GroupDetails> tree = new ArrayList<>(tree(1, 1001, allGroupsForeign ? "foreign.com" : "example.com"));
		tree.addAll(tree(1002, 1002, "foreign.com"));
		when(groups.getGroupsTree(1, true, false)).thenReturn(tree);
		for (GroupDetails group : tree) when(groups.getGroup(group.getGroupId())).thenReturn(group);
		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(mock(ResultSet.class));
		try (MockedStatic<DBPool> pool = mockStatic(DBPool.class);
			MockedStatic<GroupsDB> cache = mockStatic(GroupsDB.class);
			MockedStatic<InitServlet> init = mockStatic(InitServlet.class);
			MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class))
		{
			pool.when(DBPool::getConnection).thenReturn(connection);
			cache.when(GroupsDB::getInstance).thenReturn(groups);
			init.when(InitServlet::isTypeCloud).thenReturn(true);
			cloud.when(CloudToolsForCore::getDomainName).thenReturn("example.com");
			DocDB.getDateTimeCreatedString(1, false);
		}
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(connection).prepareStatement(sql.capture());
		if (allGroupsForeign) assertTrue(sql.getValue().endsWith("WHERE (1=0)"));
		else assertIds(sql.getValue(), 1001);
	}

	private static List<GroupDetails> tree(int first, int last, String domain)
	{
		return IntStream.rangeClosed(first, last).mapToObj(id -> {
			GroupDetails group = new GroupDetails();
			group.setGroupId(id);
			group.setDomainName(domain);
			return group;
		}).toList();
	}

	private static void assertIds(String sql, int size)
	{
		Matcher lists = Pattern.compile("group_id IN \\(([^)]+)\\)").matcher(sql);
		List<Integer> ids = new ArrayList<>();
		int chunks = 0;
		while (lists.find())
		{
			String[] values = lists.group(1).split(",");
			assertTrue(values.length <= 1000);
			for (String value : values) ids.add(Integer.parseInt(value));
			chunks++;
		}
		assertEquals(2, chunks);
		assertEquals(IntStream.rangeClosed(1, size).boxed().toList(), ids);
	}
}
