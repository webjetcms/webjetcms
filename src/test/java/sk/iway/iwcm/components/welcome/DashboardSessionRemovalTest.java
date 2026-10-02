package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.logon.AdminLogonController;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.users.UsersDB;

/** Verifies session ownership and local versus queued cluster logout responses. */
class DashboardSessionRemovalTest {
    private final AdminLogonController controller = new AdminLogonController(null);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final SessionHolder holder = mock(SessionHolder.class);
    private final Identity user = mock(Identity.class);
    private final ObjectMapper mapper = new ObjectMapper();

    /** A local owned session is invalidated, while another user's session never reaches propagation. */
    @Test
    void removesOnlyTheAuthenticatedUsersOtherLocalSession() throws Exception {
        when(user.isAdmin()).thenReturn(true);
        when(user.getUserId()).thenReturn(7);
        SessionDetails owned = new SessionDetails();
        owned.setLoggedUserId(7);
        SessionDetails foreign = new SessionDetails();
        foreign.setLoggedUserId(8);
        when(holder.get("owned")).thenReturn(owned);
        when(holder.get("foreign")).thenReturn(foreign);
        when(holder.invalidateSession(7, "owned")).thenReturn(true);
        try (var users = mockStatic(UsersDB.class); var holders = mockStatic(SessionHolder.class);
             var cluster = mockStatic(SessionClusterService.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            holders.when(SessionHolder::getInstance).thenReturn(holder);

            var success = mapper.readTree(controller.removeSession("owned", request));
            assertTrue(success.path("success").asBoolean());
            assertFalse(success.path("pending").asBoolean());
            assertFalse(mapper.readTree(controller.removeSession("foreign", request)).path("success").asBoolean());
            verify(holder).invalidateSession(7, "owned");
            verify(holder, never()).invalidateSession(7, "foreign");
            cluster.verifyNoInteractions();
        }
    }

    /** The current session is rejected on the server before any lookup or cluster command. */
    @Test
    void refusesTheCurrentSessionAndUnauthenticatedRequests() throws Exception {
        when(user.isAdmin()).thenReturn(true);
        try (var users = mockStatic(UsersDB.class); var holders = mockStatic(SessionHolder.class);
             var cluster = mockStatic(SessionClusterService.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            assertFalse(mapper.readTree(controller.removeSession(request.getSession().getId(), request)).path("success").asBoolean());
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(null);
            assertFalse(mapper.readTree(controller.removeSession("other", request)).path("success").asBoolean());
            holders.verifyNoInteractions();
            cluster.verifyNoInteractions();
        }
    }

    /** A remote session is queued only after its owner and ID match the server's cluster data. */
    @Test
    void acceptsAnOwnedRemoteSessionWithoutClaimingImmediateRemoval() throws Exception {
        when(user.isAdmin()).thenReturn(true);
        when(user.getUserId()).thenReturn(7);
        var nodes = mapper.createArrayNode();
        var sessions = nodes.addObject().putArray("userSessions");
        sessions.addObject().put("sessionId", "remote-owned").put("loggedUserId", 7);
        sessions.addObject().put("sessionId", "remote-foreign").put("loggedUserId", 8);
        try (var users = mockStatic(UsersDB.class); var holders = mockStatic(SessionHolder.class);
             var cluster = mockStatic(SessionClusterService.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            holders.when(SessionHolder::getInstance).thenReturn(holder);
            cluster.when(() -> SessionClusterService.getUserSessionsAllNodes(7)).thenReturn(nodes);

            var accepted = mapper.readTree(controller.removeSession("remote-owned", request));
            assertTrue(accepted.path("success").asBoolean());
            assertTrue(accepted.path("pending").asBoolean());
            assertFalse(mapper.readTree(controller.removeSession("remote-foreign", request)).path("success").asBoolean());
            assertFalse(mapper.readTree(controller.removeSession("missing", request)).path("success").asBoolean());
            verify(holder).invalidateSession(7, "remote-owned");
            verify(holder, never()).invalidateSession(7, "remote-foreign");
            verify(holder, never()).invalidateSession(7, "missing");
        }
    }
}
