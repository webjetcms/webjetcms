package sk.iway.iwcm.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;

class CloudFilterTest
{
	private static final String CURRENT_DOMAIN = "current.example";

	@Test
	void filtersWovenGroupsDbExecutionInCloudMode()
	{
		GroupDetails currentDomainGroup = group(CURRENT_DOMAIN);
		List<GroupDetails> originalGroups = List.of(
			currentDomainGroup,
			group("other.example"),
			group(null));
		GroupsDB groupsDB = groupsDBWithGroups(originalGroups);

		try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
			MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class))
		{
			initServlet.when(InitServlet::isTypeCloud).thenReturn(true);
			cloudTools.when(CloudToolsForCore::getDomainName).thenReturn(CURRENT_DOMAIN.toUpperCase());

			assertEquals(List.of(currentDomainGroup), groupsDB.getGroupsAll());
		}
	}

	@Test
	void preservesWovenGroupsDbResultOutsideCloudMode()
	{
		List<GroupDetails> originalGroups = List.of(
			group(CURRENT_DOMAIN),
			group("other.example"));
		GroupsDB groupsDB = groupsDBWithGroups(originalGroups);

		try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class))
		{
			initServlet.when(InitServlet::isTypeCloud).thenReturn(false);

			assertSame(originalGroups, groupsDB.getGroupsAll());
		}
	}

	private static GroupsDB groupsDBWithGroups(List<GroupDetails> groups)
	{
		GroupsDB groupsDB = mock(GroupsDB.class, CALLS_REAL_METHODS);
		ReflectionTestUtils.setField(groupsDB, "groups", groups);
		return groupsDB;
	}

	private static GroupDetails group(String domainName)
	{
		GroupDetails group = new GroupDetails();
		group.setDomainName(domainName);
		return group;
	}
}
