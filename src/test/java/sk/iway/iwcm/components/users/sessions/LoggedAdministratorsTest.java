package sk.iway.iwcm.components.users.sessions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/** Verifies domain-limited projections, fresh activity, session deduplication and independent list permission. */
class LoggedAdministratorsTest {
    private final SessionService service = new SessionService();
    private final Identity actor = mock(Identity.class);
    private final SessionHolder holder = mock(SessionHolder.class);

    /** Only locally visible administrator accounts are expanded, with local metadata overriding the cluster snapshot. */
    @Test
    void summarizesVisibleAdministratorsWithoutExposingSessions() throws Exception {
        when(actor.isAdmin()).thenReturn(true);
        when(actor.isEnabledItem("welcomeShowLoggedAdmins")).thenReturn(true);
        when(actor.getUserId()).thenReturn(7);
        var current = session("current", 7, true, 100);
        var other = session("other", 8, true, 200);
        var stale = session("current", 7, true, 1);
        var remote = session("remote", 7, true, 90);
        remote.setBrowserName("Firefox"); remote.setOperatingSystem("macOS");
        var invalid = session("invalid", 9, true, 500);
        invalid.setRemoteAddr("INVALIDATE");
        when(holder.getList()).thenReturn(List.of(current, other, invalid, session("public", 10, false, 100), session("revoked", 11, true, 100)));
        var zulu = account("Zulu");
        var alpha = account("Alpha");
        try (var users = mockStatic(UsersDB.class); var holders = mockStatic(SessionHolder.class);
             var clusters = mockStatic(SessionClusterService.class)) {
            holders.when(SessionHolder::getInstance).thenReturn(holder);
            users.when(() -> UsersDB.getUserCached(7)).thenReturn(zulu);
            users.when(() -> UsersDB.getUserCached(8)).thenReturn(alpha);
            users.when(() -> UsersDB.getUserCached(11)).thenReturn(mock(UserDetails.class));
            clusters.when(() -> SessionClusterService.getSessionsForUsers(Set.of(7, 8))).thenReturn(List.of(stale, remote));
            var items = service.loggedAdministrators(actor);
            assertEquals(List.of("Alpha", "Zulu"), items.stream().map(LoggedAdministratorDto::getFullName).toList());
            var own = items.get(1);
            assertTrue(own.isCurrent());
            assertEquals(2, own.getSessionCount());
            assertEquals(100, own.getLastActivity());
            assertEquals(List.of("Chrome · Linux", "Firefox · macOS"), own.getClients());
            var json = new ObjectMapper().valueToTree(own);
            assertEquals("Zulu-login", json.path("login").asText());
            assertFalse(json.has("sessionId"));
            assertFalse(json.has("password"));
            assertFalse(json.has("userSessions"));
            users.verify(() -> UsersDB.getUserCached(9), never());
            users.verify(() -> UsersDB.getUserCached(10), never());

            current.setLastActivity(300);
            assertEquals(300, service.loggedAdministrators(actor).get(1).getLastActivity(), "Every call must use fresh local activity.");
        }
    }

    /** Administrator-edit permission does not grant list access, and denial occurs before any session lookup. */
    @Test
    void requiresListPermissionIndependentlyOfAdministratorManagement() throws Exception {
        when(actor.isAdmin()).thenReturn(true);
        when(actor.isEnabledItem("users.edit_admins")).thenReturn(true);
        try (var holders = mockStatic(SessionHolder.class); var clusters = mockStatic(SessionClusterService.class)) {
            assertThrows(AccessDeniedException.class, () -> service.loggedAdministrators(actor));
            assertThrows(AccessDeniedException.class, () -> service.loggedAdministrators(null));
            holders.verifyNoInteractions(); clusters.verifyNoInteractions();
        }
        assertEquals("@WebjetSecurityService.hasPermission('welcomeShowLoggedAdmins')", SessionRestController.class
            .getMethod("loggedAdministrators", jakarta.servlet.http.HttpServletRequest.class).getAnnotation(PreAuthorize.class).value());
    }

    /** Empty authorized lists do not expose remote-only accounts; local summaries survive a failed cluster read. */
    @Test
    void retainsLocalSummariesWhenTheClusterIsUnavailable() {
        when(actor.isAdmin()).thenReturn(true);
        when(actor.isEnabledItem("welcomeShowLoggedAdmins")).thenReturn(true);
        when(holder.getList()).thenReturn(List.of(), List.of(session("local", 7, true, 100)));
        var autotest = account("Autotest");
        try (var users = mockStatic(UsersDB.class); var holders = mockStatic(SessionHolder.class);
             var clusters = mockStatic(SessionClusterService.class)) {
            holders.when(SessionHolder::getInstance).thenReturn(holder);
            assertTrue(service.loggedAdministrators(actor).isEmpty());
            users.when(() -> UsersDB.getUserCached(7)).thenReturn(autotest);
            clusters.when(() -> SessionClusterService.getSessionsForUsers(Set.of(7))).thenThrow(new IllegalStateException("Autotest cluster failure"));
            assertEquals(1, service.loggedAdministrators(actor).get(0).getSessionCount());
        }
    }

    private static UserDetails account(String name) {
        var account = mock(UserDetails.class);
        when(account.isAdmin()).thenReturn(true);
        when(account.getFullName()).thenReturn(name);
        when(account.getLogin()).thenReturn(name + "-login");
        return account;
    }

    private static SessionDetails session(String id, int userId, boolean admin, long activity) {
        var session = new SessionDetails();
        session.setSessionId(id); session.setLoggedUserId(userId); session.setAdmin(admin); session.setLastActivity(activity);
        session.setBrowserName("Chrome"); session.setOperatingSystem("Linux");
        return session;
    }
}
