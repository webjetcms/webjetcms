package sk.iway.iwcm.stat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.users.UsersDB;

/** Verifies the browser and operating-system labels captured when a session is created. */
class SessionMetadataTest {
    /** Browser versions stay outside the label, and platform data comes from the existing detector. */
    @Test
    void recordsSeparateBrowserAndOperatingSystemNames() {
        var request = new MockHttpServletRequest();
        var detector = mock(BrowserDetector.class);
        when(detector.getBrowserName()).thenReturn("Chrome");
        when(detector.getBrowserVersionShort()).thenReturn("154");
        when(detector.getBrowserPlatform()).thenReturn("Mac OS X");
        try (var browsers = mockStatic(BrowserDetector.class); var domains = mockStatic(CloudToolsForCore.class);
             var users = mockStatic(UsersDB.class)) {
            browsers.when(() -> BrowserDetector.getInstance(request)).thenReturn(detector);
            domains.when(CloudToolsForCore::getDomainName).thenReturn("autotest.example");
            SessionHolder holder = new SessionHolder();
            assertTrue(holder.set("autotest-session", "/admin/v9/", request));
            assertEquals("Chrome", holder.get("autotest-session").getBrowserName());
            assertEquals("Mac OS X", holder.get("autotest-session").getOperatingSystem());
            verify(detector, never()).getBrowserVersionShort();
        }
    }
}
