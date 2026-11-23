package sk.iway.iwcm.admin.layout;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.users.devices.AdminDeviceService;
import sk.iway.iwcm.users.devices.DeviceEntity;

/** Verifies that menu counts use the dialog sources and never expose account secrets. */
class UserMenuRestControllerTest {
    private final UserDetailsRepository repository = mock(UserDetailsRepository.class);
    private final AdminDeviceService devices = mock(AdminDeviceService.class);
    private final UserMenuRestController controller = new UserMenuRestController(repository, devices);
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    /** Remote sessions and outstanding login descriptions are scoped to the account and exclude secrets. */
    @Test
    void usesAllClusterNodesAndSafeLoginNotices() throws Exception {
        Identity user = new Identity();
        user.setUserId(7);
        user.setAdmin(true);
        request.setParameter("userId", "99");
        var mapper = new ObjectMapper();
        var nodes = mapper.createArrayNode();
        nodes.addObject().put("cluster", "webjet1").putArray("userSessions").addObject().put("sessionId", "autotest-local");
        nodes.addObject().put("cluster", "webjet2b").putArray("userSessions").addObject().put("sessionId", "autotest-remote");
        when(repository.getMobileDeviceByUserId(7L)).thenReturn("autotest-secret");
        var device = new DeviceEntity();
        device.setBrowserName("Firefox");
        device.setBrowserVersion("131.0");
        device.setOperatingSystem("Windows 11");
        device.setLocation("Košice, SK");
        device.setLastSeen(Instant.parse("2026-10-13T19:14:00Z"));
        device.setTokenHash("autotest-token");
        device.setCodeHash("autotest-code");
        when(devices.getActiveEvents(user)).thenReturn(List.of(device, device));
        try (var users = mockStatic(UsersDB.class); var constants = mockStatic(Constants.class);
             var tools = mockStatic(Tools.class, CALLS_REAL_METHODS); var cluster = mockStatic(SessionClusterService.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            constants.when(() -> Constants.getBoolean("2factorAuthEnabled")).thenReturn(true);
            constants.when(() -> Constants.getBoolean("adminNewDeviceDetectionEnabled")).thenReturn(true);
            cluster.when(() -> SessionClusterService.getUserSessionsAllNodes(7)).thenReturn(nodes);

            var response = controller.status(request);
            var status = response.getBody();
            assertEquals("no-store", response.getHeaders().getCacheControl());
            assertEquals(2, status.sessionCount());
            assertTrue(status.twoFactorEnabled());
            assertEquals(2, status.newLogins().size());
            assertEquals("Firefox", status.newLogins().get(0).browserName());
            assertEquals(device.getLastSeen().toEpochMilli(), status.newLogins().get(0).lastSeen());
            var json = mapper.valueToTree(status);
            assertEquals(3, json.size());
            assertEquals(5, json.path("newLogins").get(0).size());
            assertFalse(json.toString().contains("autotest"));
            verify(repository).getMobileDeviceByUserId(7L);
            verify(devices).getActiveEvents(user);
            verifyNoMoreInteractions(devices);
            cluster.verify(() -> SessionClusterService.getUserSessionsAllNodes(7));
            cluster.verifyNoMoreInteractions();

            when(repository.getMobileDeviceByUserId(7L)).thenReturn("");
            assertFalse(controller.status(request).getBody().twoFactorEnabled(), "Each opening must read the saved status again.");
        }
    }

    /** Disabled features have no storage lookups and return no login notices. */
    @Test
    void skipsDisabledFeatures() {
        Identity user = new Identity();
        user.setUserId(7);
        user.setAdmin(true);
        try (var users = mockStatic(UsersDB.class); var constants = mockStatic(Constants.class);
             var cluster = mockStatic(SessionClusterService.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            cluster.when(() -> SessionClusterService.getUserSessionsAllNodes(7)).thenReturn(new ObjectMapper().createArrayNode());
            var status = controller.status(request).getBody();
            assertTrue(status.newLogins().isEmpty());
            verifyNoInteractions(repository, devices);
        }
    }

    /** Missing or non-administrator identities cannot read any menu data. */
    @Test
    void rejectsUnauthorizedRequestsBeforeReadingSources() {
        try (var users = mockStatic(UsersDB.class); var cluster = mockStatic(SessionClusterService.class)) {
            assertThrows(AccessDeniedException.class, () -> controller.status(request));
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(new Identity());
            assertThrows(AccessDeniedException.class, () -> controller.status(request));
            verifyNoInteractions(repository, devices);
            cluster.verifyNoInteractions();
        }
    }

    /** Storage failure propagates instead of returning zero sessions as a successful result. */
    @Test
    void doesNotTurnUnavailableClusterDataIntoZero() {
        Identity user = new Identity();
        user.setAdmin(true);
        user.setUserId(7);
        try (var users = mockStatic(UsersDB.class); var cluster = mockStatic(SessionClusterService.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            cluster.when(() -> SessionClusterService.getUserSessionsAllNodes(7)).thenThrow(new IllegalStateException("Unavailable"));
            assertThrows(IllegalStateException.class, () -> controller.status(request));
        }
    }
}
