package sk.iway.iwcm.admin.layout;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    /** Remote-node sessions and device totals beyond the first page are included for the current account. */
    @Test
    void usesAllClusterNodesAndFullDevicePageTotal() throws Exception {
        Identity user = new Identity();
        user.setUserId(7);
        user.setAdmin(true);
        request.setParameter("userId", "99");
        var mapper = new ObjectMapper();
        var nodes = mapper.createArrayNode();
        nodes.addObject().put("cluster", "webjet1").putArray("userSessions").addObject().put("sessionId", "autotest-local");
        nodes.addObject().put("cluster", "webjet2b").putArray("userSessions").addObject().put("sessionId", "autotest-remote");
        when(repository.getMobileDeviceByUserId(7L)).thenReturn("autotest-secret");
        when(devices.getDevices(user, 0)).thenReturn(new PageImpl<>(List.of(new DeviceEntity()), PageRequest.of(0, 20), 35));
        when(devices.getActiveEvents(user)).thenReturn(List.of(new DeviceEntity()));
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
            assertEquals(35L, status.deviceCount());
            assertTrue(status.twoFactorEnabled());
            assertEquals(1, status.newLoginCount());
            var json = mapper.valueToTree(status);
            assertEquals(4, json.size());
            assertFalse(json.toString().contains("autotest"));
            verify(repository).getMobileDeviceByUserId(7L);
            verify(devices).getDevices(user, 0);
            cluster.verify(() -> SessionClusterService.getUserSessionsAllNodes(7));
            cluster.verifyNoMoreInteractions();

            when(repository.getMobileDeviceByUserId(7L)).thenReturn("");
            assertFalse(controller.status(request).getBody().twoFactorEnabled(), "Each opening must read the saved status again.");
        }
    }

    /** Disabled features have no storage lookups and do not present a misleading device count. */
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
            assertNull(status.deviceCount());
            assertEquals(0, status.newLoginCount());
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
