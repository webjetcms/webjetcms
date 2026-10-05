package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.stripes.AfterLogonLogoffInterceptor;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.devices.AdminDeviceService;

/** Verifies the shared post-login boundary, callback ordering and request-scoped deduplication. */
class LogonToolsAfterSuccessLogonTest {
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final Identity user = new Identity();
    private MockedStatic<Constants> constants;
    private MockedStatic<AdminDeviceService> devices;

    @BeforeEach
    void prepareLogin() {
        constants = mockStatic(Constants.class);
        constants.when(() -> Constants.getString(anyString())).thenReturn("");
        constants.when(Constants::getServletContext).thenReturn(request.getServletContext());
        constants.when(() -> Constants.getString("stripesLogonLogoffInterceptorClass")).thenReturn(LoginInterceptor.class.getName());
        constants.when(() -> Constants.getString("afterLogonMethod")).thenReturn(CustomAfterLogon.class.getName() + ".afterLogon");
        devices = mockStatic(AdminDeviceService.class);
        devices.when(() -> AdminDeviceService.recordSuccessfulLogin(user, request, response))
            .thenAnswer(invocation -> {
                calls(request).add("device");
                return null;
            });
        user.setUserId(7);
        request.getSession().setAttribute(Constants.USER_KEY, user);
        request.setAttribute("calls", new ArrayList<String>());
    }

    @AfterEach
    void releaseMocks() {
        devices.close();
        constants.close();
    }

    /** Repeated hooks without an incoming browser cookie execute every post-login action only once. */
    @Test
    void runsAllActionsOnceUsingTheSessionIdentity() {
        LogonTools.afterSuccessLogon(request, response);
        LogonTools.afterSuccessLogon(request, response);

        assertEquals(List.of("interceptor", "device", "custom"), calls(request));
        assertSame(user, request.getAttribute("interceptorUser"));
        assertSame(user, request.getAttribute("customUser"));
        assertSame(response, request.getAttribute("customResponse"));
        devices.verify(() -> AdminDeviceService.recordSuccessfulLogin(user, request, response));
    }

    /** A later login in the same session runs the callbacks again because its request is distinct. */
    @Test
    void processesAnotherRequestInTheSameSession() {
        LogonTools.afterSuccessLogon(request, response);
        var laterRequest = new MockHttpServletRequest();
        laterRequest.setSession(request.getSession());
        laterRequest.setAttribute("calls", new ArrayList<String>());

        LogonTools.afterSuccessLogon(laterRequest, response);

        assertEquals(List.of("interceptor", "custom"), calls(laterRequest));
        devices.verify(() -> AdminDeviceService.recordSuccessfulLogin(user, laterRequest, response));
    }

    /** Deduplication for one account must not suppress post-login actions for another account. */
    @Test
    void processesAnotherAccountInTheSameRequest() {
        LogonTools.afterSuccessLogon(request, response);
        Identity anotherUser = new Identity();
        anotherUser.setUserId(8);
        request.getSession().setAttribute(Constants.USER_KEY, anotherUser);

        LogonTools.afterSuccessLogon(request, response);

        assertEquals(List.of("interceptor", "device", "custom", "interceptor", "custom"), calls(request));
        assertSame(anotherUser, request.getAttribute("customUser"));
        devices.verify(() -> AdminDeviceService.recordSuccessfulLogin(anotherUser, request, response));
    }

    /** Missing session identity performs no actions and does not mark the request as completed. */
    @Test
    void waitsUntilTheUserIsInTheSession() {
        request.getSession().removeAttribute(Constants.USER_KEY);
        LogonTools.afterSuccessLogon(request, response);
        assertTrue(calls(request).isEmpty());
        devices.verifyNoInteractions();

        request.getSession().setAttribute(Constants.USER_KEY, user);
        LogonTools.afterSuccessLogon(request, response);
        assertEquals(List.of("interceptor", "device", "custom"), calls(request));
    }

    /** Callbacks retain the captured identity when an interceptor changes the session. */
    @Test
    void retainsTheIdentityWhenTheInterceptorClearsTheSession() {
        request.setAttribute("clearSession", true);

        LogonTools.afterSuccessLogon(request, response);

        assertEquals(List.of("interceptor", "device", "custom"), calls(request));
        assertSame(user, request.getAttribute("customUser"));
        devices.verify(() -> AdminDeviceService.recordSuccessfulLogin(user, request, response));
    }

    /** An interceptor failure does not reject the login or prevent the remaining actions. */
    @Test
    void continuesAfterAnInterceptorFailure() {
        constants.when(() -> Constants.getString("stripesLogonLogoffInterceptorClass")).thenReturn(FailingLoginInterceptor.class.getName());

        try (var logger = mockStatic(Logger.class)) {
            assertDoesNotThrow(() -> LogonTools.afterSuccessLogon(request, response));
            LogonTools.afterSuccessLogon(request, response);
        }

        assertEquals(List.of("interceptor", "device", "custom"), calls(request));
        devices.verify(() -> AdminDeviceService.recordSuccessfulLogin(user, request, response), times(1));
    }

    /** Session refreshes and technical identity assignment do not trigger post-login actions. */
    @Test
    void settingTheSessionIdentityDoesNotRunPostLoginActions() {
        LogonTools.setUserToSession(request.getSession(), user);
        LogonTools.setUserToSession(request.getSession(), null);

        assertTrue(calls(request).isEmpty());
        devices.verifyNoInteractions();
    }

    @SuppressWarnings("unchecked")
    private static List<String> calls(HttpServletRequest request) {
        return (List<String>) request.getAttribute("calls");
    }

    /** Records callback arguments and optionally simulates a custom session replacement. */
    public static class LoginInterceptor implements AfterLogonLogoffInterceptor {
        @Override
        public boolean logon(UserDetails user, HttpServletRequest request) {
            calls(request).add("interceptor");
            request.setAttribute("interceptorUser", user);
            if (Boolean.TRUE.equals(request.getAttribute("clearSession"))) request.getSession().removeAttribute(Constants.USER_KEY);
            return true;
        }

        @Override
        public boolean logoff(UserDetails user, HttpServletRequest request) {
            return true;
        }
    }

    /** Simulates an installed login interceptor that fails after receiving the login notification. */
    public static class FailingLoginInterceptor extends LoginInterceptor {
        @Override
        public boolean logon(UserDetails user, HttpServletRequest request) {
            super.logon(user, request);
            throw new IllegalStateException("Autotest interceptor failure");
        }
    }

    /** Captures the arguments passed to the configured afterLogonMethod extension. */
    public static class CustomAfterLogon {
        public void afterLogon(Identity user, HttpServletRequest request, HttpServletResponse response) {
            calls(request).add("custom");
            request.setAttribute("customUser", user);
            request.setAttribute("customResponse", response);
        }
    }
}
