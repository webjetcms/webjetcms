package sk.iway.iwcm.stat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.users.devices.AdminDeviceService;

/** Verifies device association propagation to cluster sessions without storing browser credentials. */
class SessionDeviceStateTest {
    /** Client Hints arriving after session creation update the session list and cluster once. */
    @Test
    void refreshesOperatingSystemAfterClientHintsArrive() {
        var request = new MockHttpServletRequest();
        request.addHeader("Sec-CH-UA-Platform", "\"Windows\"");
        request.addHeader("Sec-CH-UA-Platform-Version", "\"15.0.0\"");
        var user = mock(Identity.class);
        when(user.getUserId()).thenReturn(7);
        when(user.isAdmin()).thenReturn(true);
        request.getSession().setAttribute(Constants.USER_KEY, user);
        String sessionId = request.getSession().getId();
        var details = new SessionDetails();
        details.setLoggedUserId(7);
        details.setAdmin(true);
        details.setOperatingSystem("Windows");
        details.setOperatingSystemVersion("10");
        var holder = new SessionHolder();
        holder.getDataMap().put(sessionId, details);
        var detector = mock(BrowserDetector.class);
        when(detector.getBrowserPlatform()).thenReturn("Windows");
        when(detector.getBrowserSubplatform()).thenReturn("11");

        try (var constants = mockStatic(Constants.class); var tools = mockStatic(Tools.class);
             var domains = mockStatic(CloudToolsForCore.class); var users = mockStatic(UsersDB.class);
             var cluster = mockStatic(SessionClusterService.class); var browsers = mockStatic(BrowserDetector.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            browsers.when(() -> BrowserDetector.getInstance(request)).thenReturn(detector);
            assertTrue(holder.set(sessionId, "/admin/v9/", request));
            assertEquals("Windows", details.getOperatingSystem());
            assertEquals("11", details.getOperatingSystemVersion());
            cluster.verify(SessionClusterService::updateSessionData);
            holder.set(sessionId, "/admin/v9/", request);
            cluster.verify(SessionClusterService::updateSessionData, times(1));
        }
    }

    /** An association added after authentication is synchronized once and survives cluster serialization. */
    @Test
    void propagatesCompletedLoginDeviceAndClearsItForAnonymousSessions() throws Exception {
        var request = new MockHttpServletRequest();
        var user = mock(Identity.class);
        when(user.getUserId()).thenReturn(7);
        when(user.isAdmin()).thenReturn(true);
        request.getSession().setAttribute(Constants.USER_KEY, user);
        request.getSession().setAttribute(AdminDeviceService.SESSION_DEVICE_ID, 42L);
        String sessionId = request.getSession().getId();
        var details = new SessionDetails();
        details.setLoggedUserId(7);
        details.setAdmin(true);
        var holder = new SessionHolder();
        holder.getDataMap().put(sessionId, details);

        try (var constants = mockStatic(Constants.class); var tools = mockStatic(Tools.class);
             var domains = mockStatic(CloudToolsForCore.class); var users = mockStatic(UsersDB.class);
             var cluster = mockStatic(SessionClusterService.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            assertTrue(holder.set(sessionId, "/admin/v9/", request));
            assertEquals(42L, details.getDeviceId());
            cluster.verify(SessionClusterService::updateSessionData);
            var mapper = new ObjectMapper();
            assertEquals(42L, mapper.readValue(mapper.writeValueAsString(details), SessionDetails.class).getDeviceId());
            assertNull(mapper.readValue("{\"sessionId\":\"autotest-legacy\"}", SessionDetails.class).getDeviceId());
            holder.set(sessionId, "/admin/v9/", request);
            cluster.verify(SessionClusterService::updateSessionData, times(1));

            request.getSession().removeAttribute(Constants.USER_KEY);
            holder.set(sessionId, "/admin/v9/", request);
            assertNull(details.getDeviceId());
        }
    }
}
