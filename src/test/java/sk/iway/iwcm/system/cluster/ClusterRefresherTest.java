package sk.iway.iwcm.system.cluster;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

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

import net.sourceforge.stripes.controller.StripesFilterIway;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.fulltext.indexed.Documents;
import sk.iway.iwcm.system.jpa.JpaTools;

class ClusterRefresherTest {

    private static final String DOC_PREFIX = "sk.iway.iwcm.doc.DocDB-";
    private static final String STARTUP_MAX_SQL = "SELECT MAX(cluster_refresh_id) FROM cluster_refresher WHERE (node_name=? OR node_name=?) ";
    private static final String NEXT_MAX_SQL = "SELECT MAX(cluster_refresh_id) FROM cluster_refresher WHERE (node_name=? OR node_name=?) AND cluster_refresh_id>?";
    private static final String STARTUP_SQL = "SELECT class_name FROM cluster_refresher WHERE (node_name=? OR node_name=?) AND cluster_refresh_id<=? AND class_name LIKE ? ORDER BY cluster_refresh_id ASC";
    private static final String RECENT_SQL = "SELECT class_name FROM cluster_refresher WHERE (node_name=? OR node_name=?)  AND cluster_refresh_id>? AND cluster_refresh_id<=? ORDER BY cluster_refresh_id ASC";
    private static final String NAMED_SQL = "SELECT class_name FROM cluster_refresher WHERE node_name=? ORDER BY cluster_refresh_id ASC";
    private static final String ACK_SQL = "DELETE FROM cluster_refresher WHERE node_name=? AND class_name=? AND refresh_time<=?";

    private MockedStatic<Constants> constants;
    private MockedStatic<DocDB> documents;
    private MockedStatic<Logger> logger;
    private MockedStatic<Adminlog> adminlog;
    private MockedStatic<JpaTools> jpaTools;
    private MockedConstruction<Timer> timers;
    private DocDB docDB;
    private String originalThreadName;
    private final AtomicInteger startupReads = new AtomicInteger();
    private final AtomicInteger recentReads = new AtomicInteger();
    private final List<String> acknowledgements = new ArrayList<>();

    @BeforeEach
    void setUp() {
        originalThreadName = Thread.currentThread().getName();
        // Adminlog's static configuration needs the real Constants during initialization.
        adminlog = mockStatic(Adminlog.class);
        constants = mockStatic(Constants.class);
        documents = mockStatic(DocDB.class);
        logger = mockStatic(Logger.class);
        jpaTools = mockStatic(JpaTools.class);
        timers = mockConstruction(Timer.class);
        docDB = mock(DocDB.class);
        documents.when(DocDB::getInstance).thenReturn(docDB);
        constants.when(() -> Constants.getInt("clusterRefreshTimeout")).thenReturn(5000);
        constants.when(() -> Constants.getString("clusterMyNodeName")).thenReturn("node2");
    }

    @AfterEach
    void tearDown() {
        if (originalThreadName != null) Thread.currentThread().setName(originalThreadName);
        if (timers != null) timers.closeOnDemand();
        if (jpaTools != null) jpaTools.closeOnDemand();
        if (logger != null) logger.closeOnDemand();
        if (documents != null) documents.closeOnDemand();
        if (constants != null) constants.closeOnDemand();
        if (adminlog != null) adminlog.closeOnDemand();
    }

    @Test
    void doesNotStartStandaloneMaintenanceTimer() {
        constants.when(() -> Constants.getBoolean("luceneIncrementalDocumentIndexing")).thenReturn(true);

        new ClusterRefresher();

        assertTrue(timers.constructed().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void startupCleanupPreservesOnlyDocumentNotificationsWhenIncrementalIndexingIsEnabled(boolean enabled) {
        constants.when(() -> Constants.getBoolean("luceneIncrementalDocumentIndexing")).thenReturn(enabled);

        try (MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class)) {
            ClusterDB.cleanup();

            assertEquals(1, queries.constructed().size());
            if (enabled) {
                verify(queries.constructed().get(0)).execute(
                    "DELETE FROM cluster_refresher WHERE node_name=? AND (class_name IS NULL OR class_name NOT LIKE ?)",
                    "node2", DOC_PREFIX + "%");
                verify(queries.constructed().get(0), never()).execute(
                    "DELETE FROM cluster_refresher WHERE node_name=?", "node2");
            } else {
                verify(queries.constructed().get(0)).execute("DELETE FROM cluster_refresher WHERE node_name=?", "node2");
            }
        }
    }

    @Test
    void autoRestartReplaysRetainedDocumentIdsOnceAndProcessesEventsAfterStartupBoundary() {
        enableAutoMode(true);

        try (MockedConstruction<SimpleQuery> queries = autoQueries(
                List.of(DOC_PREFIX + "10", DOC_PREFIX + "10", DOC_PREFIX + "20"), List.of(DOC_PREFIX + "30"))) {
            ClusterRefresher refresher = new ClusterRefresher();
            refresher.run();
            refresher.run();

            verify(docDB).updateInternalCaches(10);
            verify(docDB).updateInternalCaches(20);
            verify(docDB).updateInternalCaches(30);
            assertEquals(1, startupReads.get());
            assertEquals(1, recentReads.get());
            assertEquals(List.of(DOC_PREFIX + "10", DOC_PREFIX + "20", DOC_PREFIX + "30"), acknowledgements);
            refresher.cancelTask();
            verify(timers.constructed().get(0)).cancel();
        }
    }

    @Test
    void autoStartupKeepsExistingMaxBehaviorWhenIncrementalIndexingIsDisabled() {
        enableAutoMode(false);

        try (MockedConstruction<SimpleQuery> queries = autoQueries(List.of(DOC_PREFIX + "10"), List.of(DOC_PREFIX + "30"))) {
            new ClusterRefresher().run();

            verify(docDB, never()).updateInternalCaches(10);
            verify(docDB).updateInternalCaches(30);
            assertEquals(0, startupReads.get());
        }
    }

    @Test
    void autoRestartRetriesFailedRetainedDocumentsWithoutBlockingNewEvents() {
        enableAutoMode(true);
        doThrow(new IllegalStateException("Index is locked")).doNothing().when(docDB).updateInternalCaches(10);

        try (MockedConstruction<SimpleQuery> queries = autoQueries(List.of(DOC_PREFIX + "10"), List.of(DOC_PREFIX + "30"))) {
            ClusterRefresher refresher = new ClusterRefresher();
            assertDoesNotThrow(refresher::run);
            assertEquals(List.of(DOC_PREFIX + "30"), acknowledgements);
            assertDoesNotThrow(refresher::run);
            refresher.run();

            verify(docDB, times(2)).updateInternalCaches(10);
            verify(docDB).updateInternalCaches(30);
            assertEquals(2, startupReads.get());
            assertEquals(List.of(DOC_PREFIX + "30", DOC_PREFIX + "10"), acknowledgements);
        }
    }

    @Test
    void autoCursorDoesNotSkipFailedNewDocumentRefresh() {
        enableAutoMode(true);
        doThrow(new IllegalStateException("Index is locked")).doNothing().when(docDB).updateInternalCaches(30);

        try (MockedConstruction<SimpleQuery> queries = autoQueries(List.of(), List.of(DOC_PREFIX + "30"))) {
            ClusterRefresher refresher = new ClusterRefresher();
            refresher.run();
            assertTrue(acknowledgements.isEmpty());
            refresher.run();
            refresher.run();

            verify(docDB, times(2)).updateInternalCaches(30);
            assertEquals(1, startupReads.get());
            assertEquals(2, recentReads.get());
            assertEquals(List.of(DOC_PREFIX + "30"), acknowledgements);
        }
    }

    @ParameterizedTest
    @CsvSource({"startup, false", "recent, true", "named, true"})
    void retriesDocumentWhenApplicationRenderingBecomesReady(String queue, boolean webjetInitiallyReady) throws Exception {
        int docId = 30;
        String event = DOC_PREFIX + docId;
        constants.when(() -> Constants.getString("clusterNames")).thenReturn("named".equals(queue) ? "node1,node2" : "auto");
        constants.when(() -> Constants.getBoolean("luceneIncrementalDocumentIndexing")).thenReturn(true);
        constants.when(() -> Constants.getBoolean("fulltextExecuteApps")).thenReturn(true);
        constants.when(Constants::getServletContext).thenReturn(new MockServletContext());
        doCallRealMethod().when(docDB).updateInternalCaches(docId);
        when(docDB.getBasicDocDetails(docId, false)).thenReturn(new DocDetails());
        ReflectionTestUtils.setField(docDB, "cachedDocs", new HashMap<Integer, DocDetails>());

        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet row = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(row);
        when(row.next()).thenReturn(true);
        when(row.getInt("doc_id")).thenReturn(docId);
        when(row.getBoolean("available")).thenReturn(true);
        when(row.getBoolean("searchable")).thenReturn(true);
        when(row.getTimestamp("date_created")).thenReturn(new Timestamp(0));

        try (MockedConstruction<SimpleQuery> queries = switch (queue) {
                    case "startup" -> autoQueries(List.of(event), List.of());
                    case "recent" -> autoQueries(List.of(), List.of(event));
                    case "named" -> namedQueries(event);
                    default -> throw new AssertionError(queue);
                };
                MockedStatic<DBPool> database = mockStatic(DBPool.class);
                MockedStatic<GroupsDB> groups = mockStatic(GroupsDB.class);
                MockedStatic<Cache> caches = mockStatic(Cache.class);
                MockedStatic<InitServlet> webjet = mockStatic(InitServlet.class);
                MockedStatic<StripesFilterIway> stripes = mockStatic(StripesFilterIway.class);
                MockedStatic<Documents> index = mockStatic(Documents.class)) {
            database.when(DBPool::getConnection).thenReturn(connection);
            groups.when(GroupsDB::getInstance).thenReturn(mock(GroupsDB.class));
            caches.when(Cache::getInstance).thenReturn(mock(Cache.class));
            webjet.when(InitServlet::isWebjetInitialized).thenReturn(webjetInitiallyReady);
            ClusterRefresher refresher = new ClusterRefresher();

            refresher.run();
            assertTrue(acknowledgements.isEmpty());
            index.verifyNoInteractions();
            verify(docDB, never()).readPagesToPublic();
            stripes.verify(StripesFilterIway::ensureInitialized, times(webjetInitiallyReady ? 1 : 0));

            webjet.when(InitServlet::isWebjetInitialized).thenReturn(true);
            stripes.when(StripesFilterIway::ensureInitialized).thenReturn(true);
            refresher.run();

            index.verify(() -> Documents.updateSingleDocument(docId));
            assertEquals(List.of(event), acknowledgements);
        }
    }

    @Test
    void namedModeDoesNotAcknowledgeFailedDocumentRefresh() {
        constants.when(() -> Constants.getString("clusterNames")).thenReturn("node1,node2");
        doThrow(new IllegalStateException("Index is locked")).doNothing().when(docDB).updateInternalCaches(10);

        try (MockedConstruction<SimpleQuery> queries = namedQueries(DOC_PREFIX + "10")) {
            ClusterRefresher refresher = new ClusterRefresher();
            refresher.run();
            assertTrue(acknowledgements.isEmpty());
            refresher.run();

            verify(docDB, times(2)).updateInternalCaches(10);
            assertEquals(List.of(DOC_PREFIX + "10"), acknowledgements);
        }
    }

    private void enableAutoMode(boolean incrementalIndexing) {
        constants.when(() -> Constants.getString("clusterNames")).thenReturn("auto");
        constants.when(() -> Constants.getBoolean("luceneIncrementalDocumentIndexing")).thenReturn(incrementalIndexing);
    }

    private MockedConstruction<SimpleQuery> namedQueries(String event) {
        return mockConstruction(SimpleQuery.class, (query, context) -> {
            when(query.forListString(NAMED_SQL, "node2")).thenReturn(List.of(event));
            recordAcknowledgements(query);
        });
    }

    private MockedConstruction<SimpleQuery> autoQueries(List<String> startupEvents, List<String> recentEvents) {
        return mockConstruction(SimpleQuery.class, (query, context) -> {
            when(query.forInt(STARTUP_MAX_SQL, "auto", "node2")).thenReturn(100);
            when(query.forInt(NEXT_MAX_SQL, "auto", "node2", 100)).thenReturn(110);
            when(query.forInt(NEXT_MAX_SQL, "auto", "node2", 110)).thenReturn(0);
            when(query.forListString(STARTUP_SQL, "auto", "node2", 100, DOC_PREFIX + "%")).thenAnswer(invocation -> {
                startupReads.incrementAndGet();
                return startupEvents;
            });
            when(query.forListString(RECENT_SQL, "auto", "node2", 100, 110)).thenAnswer(invocation -> {
                recentReads.incrementAndGet();
                return recentEvents;
            });
            recordAcknowledgements(query);
        });
    }

    private void recordAcknowledgements(SimpleQuery query) {
        doAnswer(invocation -> {
            acknowledgements.add(invocation.getArgument(2));
            return null;
        }).when(query).execute(eq(ACK_SQL), eq("node2"), anyString(), any(Timestamp.class));
        doAnswer(invocation -> {
            throw new AssertionError("Broadcast notifications must remain available to other nodes");
        }).when(query).execute(eq(ACK_SQL), eq("auto"), anyString(), any(Timestamp.class));
    }
}
