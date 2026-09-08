package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.logon.AdminLogonController;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

class ApiTokenAuthFilterTest {

    private static final String TOKEN_HEADER = "X-Api-Token";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void invalidatesSessionCreatedDuringApiTokenAuthentication() throws Exception {
        withSuccessfulApiTokenAuthentication(adminLogonController -> {
            MockHttpServletRequest request = apiTokenRequest();
            AtomicReference<MockHttpSession> authenticatedSession = new AtomicReference<>();
            assertNull(request.getSession(false));

            new ApiTokenAuthFilter().doFilter(request, new MockHttpServletResponse(),
                captureAuthenticatedSession(request, authenticatedSession));

            assertTrue(authenticatedSession.get().isInvalid());
            assertNull(request.getSession(false));
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        });
    }

    @Test
    void invalidatesAuthenticatedSessionWhenDownstreamFilterThrows() throws Exception {
        withSuccessfulApiTokenAuthentication(adminLogonController -> {
            MockHttpServletRequest request = apiTokenRequest();
            AtomicReference<MockHttpSession> authenticatedSession = new AtomicReference<>();

            assertThrows(ServletException.class, () -> new ApiTokenAuthFilter().doFilter(
                request,
                new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> {
                    authenticatedSession.set((MockHttpSession) request.getSession(false));
                    throw new ServletException("downstream failure");
                }
            ));

            assertNotNull(authenticatedSession.get());
            assertTrue(authenticatedSession.get().isInvalid());
            assertNull(request.getSession(false));
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        });
    }

    @Test
    void invalidatesSessionWhenPostAuthenticationSetupThrows() throws Exception {
        withSuccessfulApiTokenAuthentication(adminLogonController -> {
            adminLogonController.when(() -> AdminLogonController.determineRootWebPageDirectory(
                any(HttpSession.class), any(Identity.class)
            )).thenThrow(new IllegalStateException("post-authentication failure"));
            MockHttpServletRequest request = apiTokenRequest();
            AtomicReference<MockHttpSession> authenticatedSession = new AtomicReference<>();

            new ApiTokenAuthFilter().doFilter(request, new MockHttpServletResponse(),
                captureAuthenticatedSession(request, authenticatedSession));

            assertTrue(authenticatedSession.get().isInvalid());
            assertNull(request.getSession(false));
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        });
    }

    private FilterChain captureAuthenticatedSession(MockHttpServletRequest request,
            AtomicReference<MockHttpSession> authenticatedSession) {
        return (servletRequest, servletResponse) -> {
            authenticatedSession.set((MockHttpSession) request.getSession(false));
            assertNotNull(authenticatedSession.get());
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        };
    }

    private MockHttpServletRequest apiTokenRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TOKEN_HEADER, "api-user:secret");
        return request;
    }

    private void withSuccessfulApiTokenAuthentication(ApiTokenTestAction testAction) throws Exception {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
             MockedStatic<UsersDB> usersDB = mockStatic(UsersDB.class);
             MockedStatic<SetCharacterEncodingFilter> characterEncoding =
                mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<Prop> prop = mockStatic(Prop.class);
             MockedStatic<LogonTools> logonTools = mockStatic(LogonTools.class);
             MockedStatic<Logger> logger = mockStatic(Logger.class);
             MockedStatic<AdminLogonController> adminLogonController =
                mockStatic(AdminLogonController.class)) {
            constants.when(() -> Constants.getString("springSecurityAllowedAuths"))
                .thenReturn("api-token");
            constants.when(() -> Constants.getString("logonTokenHeaderName"))
                .thenReturn(TOKEN_HEADER);
            usersDB.when(() -> UsersDB.getUser("api-user")).thenReturn(mock(UserDetails.class));
            Authentication authentication = mock(Authentication.class);
            logonTools.when(() -> LogonTools.setUserToSession(
                any(HttpSession.class), any(Identity.class)
            )).thenAnswer(invocation -> {
                HttpSession session = invocation.getArgument(0);
                session.setAttribute(Constants.USER_KEY, invocation.getArgument(1));
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);
                return authentication;
            });

            testAction.run(adminLogonController);
        }
    }

    @FunctionalInterface
    private interface ApiTokenTestAction {
        void run(MockedStatic<AdminLogonController> adminLogonController) throws Exception;
    }
}
