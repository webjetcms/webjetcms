package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.system.monitoring.CpuInfo;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/** Verifies migrated overview providers, module permissions and bounded safe projections. */
class DashboardOverviewWidgetsTest {
    private final DashboardWidgetDataService service = new DashboardWidgetDataService(null, null, null, null);

    /** Every migrated endpoint checks its original permission before reading application data. */
    @Test
    void deniesMissingPermissionsBeforeAnyDataAccess() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        Map<String, String> permissions = Map.of("changed-pages", "menuWebpages", "audit", "cmp_adminlog",
            "logged-admins", "welcomeShowLoggedAdmins", "server-memory", "cmp_server_monitoring", "server-cpu", "cmp_server_monitoring");
        try (var database = mockStatic(DBPool.class); var sessions = mockStatic(SessionHolder.class)) {
            permissions.forEach((type, permission) -> {
                assertThrows(AccessDeniedException.class, () -> service.load(type, 7, "sessions", null, null, user, "current.example", "session"));
                when(user.isEnabledItem(permission)).thenReturn(true);
                DashboardWidgetDataService.authorize(type, user);
                when(user.isEnabledItem(permission)).thenReturn(false);
            });
            database.verifyNoInteractions();
            sessions.verifyNoInteractions();
        }
    }

    /** Current location and editor rights are checked before filling the six-entry preview. */
    @Test
    void changedPagesFilterDeniedAndMovedPagesBeforeLimiting() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true);
        when(rows.getInt("doc_id")).thenReturn(1, 2, 3, 4, 5, 6, 7, 8, 9);
        when(rows.getInt("author_id")).thenReturn(42);
        when(rows.getString("perex_image")).thenReturn("https://external.example/image.jpg", "/images/page.png");
        when(rows.getTimestamp("date_created")).thenReturn(new Timestamp(12345));
        UserDetails author = mock(UserDetails.class);
        when(author.getFullName()).thenReturn("Another editor");
        Identity user = mock(Identity.class);
        DocDB docs = mock(DocDB.class);
        List<DocDetails> current = new ArrayList<>();
        for (int id = 1; id <= 9; id++) {
            DocDetails page = mock(DocDetails.class);
            when(page.getTitle()).thenReturn("Page " + id);
            when(page.getFullPath()).thenReturn("/Section/Page " + id);
            when(docs.getBasicDocDetails(id, false)).thenReturn(page);
            current.add(page);
        }
        try (var docStatic = mockStatic(DocDB.class); var access = mockStatic(DashboardRecentPagesService.class, CALLS_REAL_METHODS);
             var users = mockStatic(UsersDB.class)) {
            docStatic.when(DocDB::getInstance).thenReturn(docs);
            users.when(() -> UsersDB.getUserCached(42)).thenReturn(author);
            access.when(() -> DashboardRecentPagesService.isAccessible(any(DocDetails.class), eq(user), eq("current.example"))).thenAnswer(call -> current.indexOf(call.getArgument(0)) >= 2);
            var items = service.changedPages(connection, user, "current.example", new DashboardWidgetDataService.Scope(List.of(10), List.of(22), List.of(10, 20)));
            assertEquals(6, items.size());
            assertEquals(3, items.get(0).get("docId"));
            assertEquals(8, items.get(5).get("docId"));
            assertEquals("Another editor", items.get(0).get("userFullName"));
            assertEquals(12345L, items.get(0).get("date"));
            assertEquals("", items.get(0).get("perexImage"));
            assertEquals("/images/page.png", items.get(1).get("perexImage"));
            assertEquals("/admin/v9/webpages/web-pages-list/?docid=3", items.get(0).get("url"));
            verify(docs, never()).getBasicDocDetails(9, false);
        }
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("d.group_id IN (10,20) AND (d.group_id IN (10) OR d.doc_id IN (22))"));
        assertTrue(sql.getValue().contains("ORDER BY d.date_created DESC"));
        assertFalse(sql.getValue().contains("d.author_id=?"));
        verify(statement).setQueryTimeout(15);
    }

    /** Audit text is bounded and the projection excludes IP addresses and unrelated record metadata. */
    @Test
    void auditProjectsLocalizedTypeAuthorAndDateWithBoundedText() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        when(rows.getInt("log_id")).thenReturn(11);
        when(rows.getInt("log_type")).thenReturn(20);
        when(rows.getInt("user_id")).thenReturn(0);
        when(rows.getString("description")).thenReturn("<script>test</script>" + "x".repeat(200));
        when(rows.getTimestamp("create_date")).thenReturn(new Timestamp(1234));
        Prop prop = mock(Prop.class);
        when(prop.getText("components.adminlog.20")).thenReturn("Changed record");
        try (var translations = mockStatic(Prop.class)) {
            translations.when(Prop::getInstance).thenReturn(prop);
            var item = service.audit(connection).get(0);
            assertEquals("Changed record", item.get("type"));
            assertEquals(1234L, item.get("date"));
            assertEquals("", item.get("userFullName"));
            assertTrue(((String)item.get("description")).length() <= 140);
            assertFalse(item.containsKey("ip"));
            assertFalse(item.containsKey("hostname"));
        }
        verify(statement).setMaxRows(6);
        verify(statement).setQueryTimeout(15);
    }

    /** Multiple sessions do not duplicate people or expose their settings, credentials or session IDs. */
    @Test
    void loggedAdminsRetainsEveryDistinctAccountInANarrowProjection() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isEnabledItem("welcomeShowLoggedAdmins")).thenReturn(true);
        SessionHolder holder = mock(SessionHolder.class);
        List<SessionDetails> sessions = new ArrayList<>();
        for (int id : List.of(1, 1, 2, 3, 4, 5, 6, 7, 8)) {
            SessionDetails session = mock(SessionDetails.class);
            when(session.getLoggedUserId()).thenReturn(id);
            when(session.isAdmin()).thenReturn(id != 8);
            sessions.add(session);
        }
        when(holder.getList()).thenReturn(sessions);
        try (var sessionStatic = mockStatic(SessionHolder.class); var users = mockStatic(UsersDB.class)) {
            sessionStatic.when(SessionHolder::getInstance).thenReturn(holder);
            for (int id = 1; id <= 7; id++) {
                UserDetails active = mock(UserDetails.class);
                when(active.isAdmin()).thenReturn(true);
                when(active.getFullName()).thenReturn("Administrator " + id);
                when(active.getEmail()).thenReturn("admin" + id + "@example.test");
                int userId = id;
                users.when(() -> UsersDB.getUserCached(userId)).thenReturn(active);
            }
            var result = service.load("logged-admins", 7, "sessions", null, null, user, null, "session");
            assertEquals(7L, result.get("total"));
            List<?> items = (List<?>)result.get("items");
            assertEquals(7, items.size());
            assertEquals(Map.of("userId", 1, "fullName", "Administrator 1", "email", "admin1@example.test"), items.get(0));
            users.verify(() -> UsersDB.getUserCached(1), times(1));
            users.verify(() -> UsersDB.getUserCached(8), never());
        }
    }

    /** History stays on the current node and converts bytes to MB before chronological rendering. */
    @Test
    void monitoringHistoryUsesCurrentNodeWindowAndBoundedMetricProjection() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, true, false);
        when(rows.getTimestamp("date_insert")).thenReturn(new Timestamp(2000), new Timestamp(1000));
        when(rows.getLong("mem_total")).thenReturn(8L * 1024 * 1024);
        when(rows.getLong("mem_free")).thenReturn(2L * 1024 * 1024);
        var series = service.monitoringHistory(connection, true, 500, 2500, "current-node");
        assertEquals(Map.of("date", 1000L, "total", 8d, "free", 2d, "used", 6d), series.get(0));
        assertEquals(2000L, series.get(1).get("date"));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertTrue(sql.getValue().startsWith("SELECT date_insert, mem_total, mem_free FROM monitoring"));
        assertTrue(sql.getValue().contains("AND node_name=? ORDER BY"));
        assertFalse(sql.getValue().contains("node_name IS NULL"));
        verify(statement).setString(3, "current-node");
        verify(statement).setTimestamp(1, new Timestamp(500));
        verify(statement).setTimestamp(2, new Timestamp(2500));
        verify(statement).setMaxRows(120);
    }

    /** Oracle stores an unnamed node as NULL; both representations retain the same time window. */
    @Test
    void unnamedMonitoringNodeAcceptsEmptyAndNullWithoutIncludingNamedNodes() throws Exception {
        for (String node : new String[] {"", null}) {
            Connection connection = mock(Connection.class);
            PreparedStatement statement = mock(PreparedStatement.class);
            ResultSet rows = mock(ResultSet.class);
            when(connection.prepareStatement(anyString())).thenReturn(statement);
            when(statement.executeQuery()).thenReturn(rows);
            assertTrue(service.monitoringHistory(connection, false, 500, 2500, node).isEmpty());
            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            verify(connection).prepareStatement(sql.capture());
            assertTrue(sql.getValue().contains("WHERE date_insert>=? AND date_insert<=? AND (node_name IS NULL OR node_name=?) ORDER BY"));
            verify(statement).setString(3, "");
            verify(statement).setTimestamp(1, new Timestamp(500));
            verify(statement).setTimestamp(2, new Timestamp(2500));
        }
    }

    /** Disabled historical collection still supplies a real one-time snapshot and needs no domain. */
    @Test
    void monitoringWithoutHistoryReturnsCurrentValuesWithoutDatabaseAccess() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isEnabledItem("cmp_server_monitoring")).thenReturn(true);
        try (var constants = mockStatic(Constants.class); var database = mockStatic(DBPool.class);
             var cpu = mockConstruction(CpuInfo.class, (mock, context) -> {
                 when(mock.getCpuUsage()).thenReturn(25);
                 when(mock.getCpuUsageProcess()).thenReturn(10);
             })) {
            var memory = service.load("server-memory", 7, "sessions", null, null, user, null, "session");
            var point = (Map<?, ?>)((List<?>)memory.get("series")).get(0);
            assertTrue(((Number)point.get("total")).doubleValue() > 0);
            assertEquals(3600000L, (long)memory.get("to") - (long)memory.get("from"));
            var processor = service.load("server-cpu", 7, "sessions", null, null, user, null, "session");
            var processorPoint = (Map<?, ?>)((List<?>)processor.get("series")).get(0);
            assertEquals(25, processorPoint.get("system"));
            assertEquals(10, processorPoint.get("process"));
            database.verifyNoInteractions();
        }
    }

    /** Data failures remain failures rather than an empty audit preview. */
    @Test
    void auditDatabaseFailurePropagatesToTheSharedErrorHandler() throws Exception {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isEnabledItem("cmp_adminlog")).thenReturn(true);
        try (var database = mockStatic(DBPool.class)) {
            Connection connection = mock(Connection.class);
            when(connection.prepareStatement(anyString())).thenThrow(new java.sql.SQLException("Unavailable"));
            database.when(DBPool::getConnection).thenReturn(connection);
            assertThrows(IllegalStateException.class, () -> service.load("audit", 7, "sessions", null, null, user, null, "session"));
        }
    }
}
