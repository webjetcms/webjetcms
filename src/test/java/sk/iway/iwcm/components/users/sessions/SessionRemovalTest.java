package sk.iway.iwcm.components.users.sessions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.users.UsersDB;

/** Verifies session ownership and local versus queued cluster logout responses. */
class SessionRemovalTest {
    private final SessionRestController controller = new SessionRestController(new SessionService());
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final SessionHolder holder = mock(SessionHolder.class);
    private final Identity user = mock(Identity.class);
    private final ObjectMapper mapper = new ObjectMapper();

    /** Last activity survives the cluster JSON round trip without exposing internal page-tracking fields. */
    @Test
    void includesLastActivityInSessionJson() throws Exception {
        SessionDetails session = new SessionDetails();
        session.setSessionId("autotest-session");
        session.setLastActivity(123456789L);
        session.setLastURL("/private-autotest-page");
        session.setBrowserName("Chrome");
        session.setOperatingSystem("macOS");
        var json = mapper.valueToTree(session);
        assertEquals(123456789L, json.path("lastActivity").asLong());
        assertFalse(json.has("lastURL"));
        assertFalse(json.has("lastActivityAsDate"));
        assertEquals("Chrome", json.path("browserName").asText());
        assertEquals("macOS", mapper.treeToValue(json, SessionDetails.class).getOperatingSystem());
        assertEquals(123456789L, mapper.treeToValue(json, SessionDetails.class).getLastActivity());
    }

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

            var success = controller.logoutSession("owned", request);
            assertTrue(success.isSuccess());
            assertFalse(success.isPending());
            assertFalse(controller.logoutSession("foreign", request).isSuccess());
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
            assertFalse(controller.logoutSession(request.getSession().getId(), request).isSuccess());
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(null);
            assertThrows(AccessDeniedException.class, () -> controller.logoutSession("other", request));
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

            var accepted = controller.logoutSession("remote-owned", request);
            assertTrue(accepted.isSuccess());
            assertTrue(accepted.isPending());
            assertFalse(controller.logoutSession("remote-foreign", request).isSuccess());
            assertFalse(controller.logoutSession("missing", request).isSuccess());
            verify(holder).invalidateSession(7, "remote-owned");
            verify(holder, never()).invalidateSession(7, "remote-foreign");
            verify(holder, never()).invalidateSession(7, "missing");
        }
    }
}
