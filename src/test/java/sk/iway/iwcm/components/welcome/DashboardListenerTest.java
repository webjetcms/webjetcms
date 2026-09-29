package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/** Verifies authenticated bootstrap ownership and complete initial data. */
class DashboardListenerTest {
    /** Injects account/domain data in both cloud modes and distinct administrator names only with the required permission. */
    @ParameterizedTest
    @CsvSource({
        "false, false, -1",
        "false, true, 42",
        "true, false, -1",
        "true, true, 42"
    })
    void embedsInitialDataForCurrentAccountAndDomain(boolean showLoggedAdmins, boolean cloudMode, int expectedStatRootGroupId) throws Exception {
        var settings = mock(DashboardSettingsService.class);
        var notices = mock(DashboardNoticeService.class);
        var listener = new DashboardListener(settings, notices);
        var request = new MockHttpServletRequest();
        request.setParameter("userId", "999");
        request.setParameter("domainId", "999");
        var user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isEnabledItem("welcomeShowLoggedAdmins")).thenReturn(showLoggedAdmins);
        when(user.getUserId()).thenReturn(7);
        when(user.getFirstName()).thenReturn("Autotest");
        var preferences = new DashboardSettingsDto();
        preferences.setConfigured(true);
        preferences.getDomainOptions().put("autotest-form", Map.of("formName", "Contact"));
        when(settings.load(7, "42")).thenReturn(preferences);
        when(notices.load(user, request)).thenReturn(List.of(Map.of("id", "autotest-notice", "bodyHtml", "<p>Warning</p>")));
        String sessionId = request.getSession().getId();
        SessionHolder holder = mock(SessionHolder.class);
        List<SessionDetails> adminSessions = new ArrayList<>();
        for (int id : List.of(1, 1, 2, 3, 4, 5, 6, 7, 8)) {
            SessionDetails session = mock(SessionDetails.class);
            when(session.getLoggedUserId()).thenReturn(id);
            when(session.isAdmin()).thenReturn(id != 8);
            adminSessions.add(session);
        }
        when(holder.getList()).thenReturn(adminSessions);
        var model = new ModelMap();
        try (var users = mockStatic(UsersDB.class);
             var installation = mockStatic(InitServlet.class);
             var domains = mockStatic(CloudToolsForCore.class);
             var docs = mockStatic(DocDB.class);
             var sessions = mockStatic(SessionClusterService.class);
             var holders = mockStatic(SessionHolder.class);
             var menus = mockConstruction(MenuService.class, (menu, context) -> when(menu.getMenu()).thenReturn(List.of()))) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            installation.when(InitServlet::isTypeCloud).thenReturn(cloudMode);
            holders.when(SessionHolder::getInstance).thenReturn(holder);
            for (int id = 1; id <= 7; id++) {
                UserDetails active = mock(UserDetails.class);
                when(active.isAdmin()).thenReturn(true);
                when(active.getFullName()).thenReturn("Administrator " + id);
                when(active.getEmail()).thenReturn("admin" + id + "@example.test");
                int userId = id;
                users.when(() -> UsersDB.getUserCached(userId)).thenReturn(active);
            }
            domains.when(() -> CloudToolsForCore.getRootGroupId(request)).thenReturn(42);
            docs.when(() -> DocDB.getDomain(request)).thenReturn("current.example");

            sessions.when(() -> SessionClusterService.getSessionInfo(sessionId, 7))
                .thenReturn("{\"currentSessionId\":\"" + sessionId + "\",\"userSessions\":[]}");

            listener.setOverviewData(new WebjetEvent<>(new ThymeleafEvent("dashboard", null, model, null, request), null));

            var data = new ObjectMapper().readTree((String) model.get("overviewData"));
            assertEquals("Autotest", data.path("userName").asText());
            assertEquals("current.example", data.path("currentDomain").asText());
            assertEquals(expectedStatRootGroupId, data.path("statRootGroupId").asInt());
            assertTrue(data.path("dashboardMenu").isArray());
            assertTrue(data.path("settings").path("configured").asBoolean());
            assertEquals("Contact", data.path("settings").path("domainOptions").path("autotest-form").path("formName").asText());
            assertEquals("autotest-notice", data.path("notices").get(0).path("id").asText());
            assertEquals(sessionId, data.path("currentSessions").path("currentSessionId").asText());
            if (showLoggedAdmins) {
                var admins = data.path("loggedAdmins");
                assertEquals(7, admins.size());
                assertEquals(new ObjectMapper().valueToTree(Map.of("userId", 1, "fullName", "Administrator 1", "email", "admin1@example.test")), admins.get(0));
                users.verify(() -> UsersDB.getUserCached(1), times(1));
                users.verify(() -> UsersDB.getUserCached(8), never());
            } else {
                assertFalse(data.has("loggedAdmins"));
                holders.verifyNoInteractions();
                verifyNoInteractions(holder);
                users.verify(() -> UsersDB.getUserCached(anyInt()), never());
            }
            verify(settings).load(7, "42");
            verify(notices).load(user, request);
            sessions.verify(() -> SessionClusterService.getSessionInfo(sessionId, 7));
            verifyNoMoreInteractions(settings, notices);
        }
    }

    /** Unauthenticated and non-administrator requests cannot receive dashboard data. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void skipsUnauthorizedUsers(boolean authenticated) {
        var settings = mock(DashboardSettingsService.class);
        var notices = mock(DashboardNoticeService.class);
        var request = new MockHttpServletRequest();
        var model = new ModelMap();
        try (var users = mockStatic(UsersDB.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(authenticated ? mock(Identity.class) : null);
            new DashboardListener(settings, notices).setOverviewData(
                new WebjetEvent<>(new ThymeleafEvent("dashboard", null, model, null, request), null));
            assertFalse(model.containsKey("overviewData"));
            verifyNoInteractions(settings, notices);
            assertNull(request.getSession(false));
        }
    }
}
