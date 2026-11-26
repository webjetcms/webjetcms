package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/** Verifies repeated-login blocking with the actual cache in one servlet context. */
class LogonToolsBlockedLoginTest {

    /** The sixth recorded failure must remain blocked after the short delay expires. */
    @Test
    void enforcesTheRepeatedFailureThreshold() {
        var request = new MockHttpServletRequest();
        try (var constants = mockStatic(Constants.class)) {
            constants.when(Constants::getServletContext).thenReturn(request.getServletContext());
            constants.when(() -> Constants.getInt("logonLoginBlockedDelay")).thenReturn(60);
            constants.when(() -> Constants.getInt("logonBlockedAfterUnsuccessCount")).thenReturn(5);
            constants.when(() -> Constants.getInt("logonBlockedDelay")).thenReturn(10);
            for (int attempt = 1; attempt <= 5; attempt++) {
                LogonTools.setLoginBlocked(request);
            }
            assertTrue(LogonTools.isLoginBlocked(request));
            assertFalse(isBlockedAfter(request, 11));
            LogonTools.setLoginBlocked(request);
            assertTrue(isBlockedAfter(request, 11));
            assertFalse(isBlockedAfter(request, 61));
        }
    }

    private boolean isBlockedAfter(MockHttpServletRequest request, int seconds) {
        long now = System.currentTimeMillis() + seconds * 1000L;
        try (var tools = mockStatic(Tools.class, CALLS_REAL_METHODS)) {
            tools.when(Tools::getNow).thenReturn(now);
            return LogonTools.isLoginBlocked(request);
        }
    }
}
