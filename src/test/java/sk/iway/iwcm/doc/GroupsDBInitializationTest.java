package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Optional;

import javax.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.system.cluster.ClusterDB;

class GroupsDBInitializationTest
{

	private ServletContext originalContext;
	private MockServletContext context;

	@BeforeEach
	void setUp()
	{
		originalContext = Constants.getServletContext();
		context = new MockServletContext();
		Constants.setServletContext(context);
	}

	@AfterEach
	void tearDown()
	{
		Constants.setServletContext(originalContext);
	}

	@ParameterizedTest
	@CsvSource({"false, false", "false, true", "true, false", "true, true"})
	void initializesGroupsAndNavbarAfterPublishIntervalExpires(boolean documentsFirst, boolean documentCacheExists) throws Exception
	{
		DocPublishService publication = spy(new DocPublishService());
		DocHistoryRepository history = mock(DocHistoryRepository.class);
		DocDetailsRepository documents = mock(DocDetailsRepository.class);
		when(history.getPublicableThatAreNotAwaitingToApprove()).thenReturn(Optional.empty());
		when(documents.findAllByDisableAfterEndTrue()).thenReturn(Collections.emptyList());
		ReflectionTestUtils.setField(publication, "dhr", history);
		ReflectionTestUtils.setField(publication, "ddr", documents);

		if (documentCacheExists)
		{
			DocDB docDB = mock(DocDB.class);
			prepareDocDB(docDB, publication);
			context.setAttribute(Constants.A_DOC_DB, docDB);
		}

		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		ResultSet resultSet = mock(ResultSet.class);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(true, false);
		when(resultSet.getString(anyString())).thenReturn("");
		when(resultSet.getString("group_name")).thenReturn("Home");
		when(resultSet.getString("navbar")).thenReturn("Home");
		when(resultSet.getString("domain_name")).thenReturn("example.com");
		when(resultSet.getInt("group_id")).thenReturn(7);
		when(resultSet.getInt("default_doc_id")).thenAnswer(invocation -> {
			// Simulate a slow database passing the five-second interval without sleeping.
			ReflectionTestUtils.setField(publication, "lastPublishCheck", 0L);
			return 42;
		});

		try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
			MockedStatic<InitServlet> init = mockStatic(InitServlet.class);
			MockedStatic<ClusterDB> cluster = mockStatic(ClusterDB.class);
			MockedStatic<Cache> cache = mockStatic(Cache.class);
			MockedConstruction<DocDB> createdDocuments = mockConstruction(DocDB.class,
				(docDB, construction) -> prepareDocDB(docDB, publication)))
		{
			dbPool.when(() -> DBPool.getConnection("iwcm")).thenReturn(connection);
			init.when(InitServlet::isWebjetInitialized).thenReturn(true);
			cache.when(Cache::getInstance).thenReturn(mock(Cache.class));

			if (documentsFirst) DocDB.getInstance();
			GroupsDB groups = GroupsDB.getInstance();
			if (documentsFirst == false) DocDB.getInstance();

			GroupDetails group = groups.getGroup(7);
			String expectedNavbar = "<a href='/en/home.html'>Home</a>";
			assertEquals(expectedNavbar, ReflectionTestUtils.getField(group, "navbar"));
			assertEquals(expectedNavbar, group.getNavbar());
			assertEquals("Home", group.getNavbarName());
			assertEquals(42, group.getDefaultDocId());
			assertEquals(7, GroupsDB.getDomainId("example.com"));
			assertSame(groups, context.getAttribute(Constants.A_GROUPS_DB));
			assertEquals(documentCacheExists ? 0 : 1, createdDocuments.constructed().size());
			assertTrue(publication.getPublicableDocs().isEmpty());
			verify(publication).checkWebpagesToPublish((DocDB)context.getAttribute(Constants.A_DOC_DB));
			verify(publication).refreshPagesToPublish();
			verify(statement).executeQuery();
			verify(connection).close();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void checksPublicationOutsideDocumentInitializationLock(boolean forceRefresh)
	{
		DocPublishService publication = mock(DocPublishService.class);
		try (MockedConstruction<DocDB> createdDocuments = mockConstruction(DocDB.class, (docDB, construction) -> {
			prepareDocDB(docDB, publication);
			doAnswer(invocation -> {
				assertFalse(Thread.holdsLock(DocDB.class));
				assertSame(docDB, context.getAttribute(Constants.A_DOC_DB));
				return null;
			}).when(publication).checkWebpagesToPublish(docDB);
		}))
		{
			DocDB docDB = DocDB.getInstance(forceRefresh);
			assertSame(createdDocuments.constructed().get(0), docDB);
			verify(publication).checkWebpagesToPublish(docDB);
		}
	}

	@Test
	void preservesCustomNavbarLink()
	{
		GroupDetails group = new GroupDetails();
		String navbar = "<a href='/custom.html' target='_blank'>Custom</a>";
		group.setNavbar(navbar);
		group.setDefaultDocId(42);
		assertEquals(navbar, group.getNavbar());
	}

	private void prepareDocDB(DocDB docDB, DocPublishService publication)
	{
		ReflectionTestUtils.setField(docDB, "urlsByUrlDomains", new Hashtable<>());
		ReflectionTestUtils.setField(docDB, "docPublishService", publication);
		when(docDB.getDocLink(42)).thenReturn("/en/home.html");
	}
}
