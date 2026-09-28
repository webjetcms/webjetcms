package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.i18n.Prop;

/** Verifies migrated overview providers, module permissions and bounded safe projections. */
class DashboardOverviewWidgetsTest {
    private final DashboardWidgetDataService service = new DashboardWidgetDataService(null, null, null);

    /** Every migrated endpoint checks its original permission before reading application data. */
    @Test
    void deniesMissingPermissionsBeforeAnyDataAccess() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        Map<String, String> permissions = Map.of("audit", "cmp_adminlog");
        try (var database = mockStatic(DBPool.class)) {
            permissions.forEach((type, permission) -> {
                assertThrows(AccessDeniedException.class, () -> service.load(type, 7, null, null, user, "current.example"));
                when(user.isEnabledItem(permission)).thenReturn(true);
                DashboardWidgetDataService.authorize(type, user);
                when(user.isEnabledItem(permission)).thenReturn(false);
            });
            database.verifyNoInteractions();
        }
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
            assertThrows(IllegalStateException.class, () -> service.load("audit", 7, null, null, user, null));
        }
    }
}
