package sk.iway.iwcm.components.users.sessions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/** Verifies administrator-management permission, domain visibility and cluster-wide session logout. */
class AdministratorLogoutTest {
    private final SessionRestController controller = new SessionRestController(new SessionService());
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final Identity actor = mock(Identity.class);
    private final SessionHolder holder = mock(SessionHolder.class);

    /** Local and remote sessions are deduplicated, queued correctly and audited without using single-logon settings. */
    @Test
    void invalidatesOnlyTheSelectedAdministratorsSessions() throws Exception {
        when(actor.isAdmin()).thenReturn(true);
        when(actor.isEnabledItem("users.edit_admins")).thenReturn(true);
        when(actor.getUserId()).thenReturn(7);
        when(actor.getLogin()).thenReturn("autotest-actor");
        SessionDetails local = session("local", 8, true);
        SessionDetails remote = session("remote", 8, true);
        when(holder.getList()).thenReturn(List.of(local, session("foreign", 9, true)));
        when(holder.invalidateSession(8, "local")).thenReturn(true);
        var target = mock(UserDetails.class);
        when(target.isAdmin()).thenReturn(true);
        when(target.getLogin()).thenReturn("autotest-target");
        try (var users = mockStatic(UsersDB.class); var holders = mockStatic(SessionHolder.class);
             var clusters = mockStatic(SessionClusterService.class); var audit = mockStatic(Adminlog.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(actor);
            users.when(() -> UsersDB.getUserCached(8)).thenReturn(target);
            holders.when(SessionHolder::getInstance).thenReturn(holder);
            clusters.when(() -> SessionClusterService.getSessionsForUsers(Set.of(8))).thenReturn(List.of(local, remote));
            var response = controller.logoutAdministrator(8, request);
            assertTrue(response.isSuccess());
            assertTrue(response.isPending());
            verify(holder).invalidateSession(8, "local");
            verify(holder).invalidateSession(8, "remote");
            verify(holder, never()).invalidateSession(9, "foreign");
            audit.verify(() -> Adminlog.add(Adminlog.TYPE_USER_LOGOFF,
                "Administrator autotest-actor ended all sessions of administrator autotest-target", 8, -1));
            assertEquals("@WebjetSecurityService.hasPermission('users.edit_admins')", SessionRestController.class
                .getMethod("logoutAdministrator", int.class, jakarta.servlet.http.HttpServletRequest.class).getAnnotation(PreAuthorize.class).value());
        }
    }

    /** Denied callers and own-account requests cannot look up or invalidate any session. */
    @Test
    void rejectsMissingPermissionAndTheCurrentAccount() {
        try (var users = mockStatic(UsersDB.class); var holders = mockStatic(SessionHolder.class);
             var clusters = mockStatic(SessionClusterService.class)) {
            assertThrows(AccessDeniedException.class, () -> controller.logoutAdministrator(8, request));
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(actor);
            when(actor.isAdmin()).thenReturn(true);
            assertThrows(AccessDeniedException.class, () -> controller.logoutAdministrator(8, request));
            when(actor.isEnabledItem("welcomeShowLoggedAdmins")).thenReturn(true);
            assertThrows(AccessDeniedException.class, () -> controller.logoutAdministrator(8, request));
            when(actor.isEnabledItem("users.edit_admins")).thenReturn(true);
            when(actor.getUserId()).thenReturn(7);
            assertThrows(IllegalArgumentException.class, () -> controller.logoutAdministrator(7, request));
            assertThrows(IllegalArgumentException.class, () -> controller.logoutAdministrator(-1, request));
            holders.verifyNoInteractions(); clusters.verifyNoInteractions();
        }
    }

    /** Public users, invalidated sessions and administrators outside the visible domain are excluded. */
    @Test
    void rejectsTargetsOutsideTheAuthorizedAdministratorList() {
        when(actor.isAdmin()).thenReturn(true);
        when(actor.isEnabledItem("users.edit_admins")).thenReturn(true);
        when(actor.getUserId()).thenReturn(7);
        SessionDetails invalid = session("invalid", 8, true);
        invalid.setRemoteAddr("INVALIDATE");
        when(holder.getList()).thenReturn(List.of(invalid, session("public", 8, false)));
        try (var users = mockStatic(UsersDB.class); var holders = mockStatic(SessionHolder.class);
             var clusters = mockStatic(SessionClusterService.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(actor);
            holders.when(SessionHolder::getInstance).thenReturn(holder);
            assertThrows(AccessDeniedException.class, () -> controller.logoutAdministrator(8, request));
            verify(holder, never()).invalidateSession(anyInt(), anyString());
            clusters.verifyNoInteractions();
        }
    }

    private static SessionDetails session(String id, int userId, boolean admin) {
        var session = new SessionDetails();
        session.setSessionId(id); session.setLoggedUserId(userId); session.setAdmin(admin);
        return session;
    }
}
