package sk.iway.iwcm.system.spring.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for OAuth2DynamicErrorHandler
 * Tests error handling during OAuth2 authentication failures
 */
class OAuth2DynamicErrorHandlerTest extends BaseWebjetTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    private OAuth2DynamicErrorHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new OAuth2DynamicErrorHandler();

        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);

        // Clear Constants
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();
    }

    // ============= Admin login error handling =============

    /**
     * Test error handling for admin login redirects to /admin/logon/
     * Note: OAuth2AuthenticationException.getMessage() returns "[error_code] description"
     */
    @Test
    void testErrorHandlingAdminLogin() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(true);

        // Create exception with message containing "access_denied"
        AuthenticationException exception = new OAuth2AuthenticationException(
            new OAuth2Error("access_denied", "Access denied by user", null),
            "access_denied error"
        );

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<Adminlog> adminlogMock = mockStatic(Adminlog.class)) {

            handler.onAuthenticationFailure(request, response, exception);

            verify(session).setAttribute("oauth2_logon_error", "oauth2_access_denied");
            verify(response).sendRedirect("/admin/logon/");
        }
    }

    // ============= User login error handling =============

    /**
     * Test error handling for user login redirects to /
     */
    @Test
    void testErrorHandlingUserLogin() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        // Create exception with message containing "invalid_token"
        AuthenticationException exception = new OAuth2AuthenticationException(
            new OAuth2Error("invalid_token", "Invalid token", null),
            "invalid_token error"
        );

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<Adminlog> adminlogMock = mockStatic(Adminlog.class)) {

            handler.onAuthenticationFailure(request, response, exception);

            verify(session).setAttribute("oauth2_logon_error", "oauth2_invalid_token");
            verify(response).sendRedirect("/");
        }
    }

    /**
     * Test error handling when session attribute is null (defaults to user login)
     */
    @Test
    void testErrorHandlingNoSessionAttribute() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(null);

        // Create exception with message containing "server_error"
        AuthenticationException exception = new OAuth2AuthenticationException(
            new OAuth2Error("server_error", "Server error", null),
            "server_error occurred"
        );

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<Adminlog> adminlogMock = mockStatic(Adminlog.class)) {

            handler.onAuthenticationFailure(request, response, exception);

            verify(session).setAttribute("oauth2_logon_error", "oauth2_server_error");
            verify(response).sendRedirect("/");
        }
    }

    // ============= Error code extraction tests =============

    /**
     * Test error code extraction for invalid_token
     */
    @Test
    void testErrorCodeInvalidToken() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        // The error code detection looks for "invalid_token" in the message
        AuthenticationException exception = new OAuth2AuthenticationException(
            new OAuth2Error("invalid_token", "The token is invalid", null),
            "invalid_token error"
        );

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<Adminlog> adminlogMock = mockStatic(Adminlog.class)) {

            handler.onAuthenticationFailure(request, response, exception);

            verify(session).setAttribute("oauth2_logon_error", "oauth2_invalid_token");
        }
    }

    /**
     * Test error code extraction for invalid_grant
     */
    @Test
    void testErrorCodeInvalidGrant() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        // The error code detection looks for "invalid_grant" in the message
        AuthenticationException exception = new OAuth2AuthenticationException(
            new OAuth2Error("invalid_grant", "The authorization grant is invalid", null),
            "invalid_grant error"
        );

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<Adminlog> adminlogMock = mockStatic(Adminlog.class)) {

            handler.onAuthenticationFailure(request, response, exception);

            verify(session).setAttribute("oauth2_logon_error", "oauth2_invalid_token");
        }
    }

    /**
     * Test error code extraction for access_denied
     */
    @Test
    void testErrorCodeAccessDenied() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        // The error code detection looks for "access_denied" in the message
        AuthenticationException exception = new OAuth2AuthenticationException(
            new OAuth2Error("access_denied", "The user denied access", null),
            "access_denied by user"
        );

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<Adminlog> adminlogMock = mockStatic(Adminlog.class)) {

            handler.onAuthenticationFailure(request, response, exception);

            verify(session).setAttribute("oauth2_logon_error", "oauth2_access_denied");
        }
    }

    /**
     * Test error code extraction for server_error
     */
    @Test
    void testErrorCodeServerError() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        // The error code detection looks for "server_error" in the message
        AuthenticationException exception = new OAuth2AuthenticationException(
            new OAuth2Error("server_error", "The server encountered an error", null),
            "server_error occurred"
        );

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<Adminlog> adminlogMock = mockStatic(Adminlog.class)) {

            handler.onAuthenticationFailure(request, response, exception);

            verify(session).setAttribute("oauth2_logon_error", "oauth2_server_error");
        }
    }

    /**
     * Test error code extraction for generic OAuth2 error
     */
    @Test
    void testErrorCodeGenericOAuth2() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        // Unknown error code - should fall through to generic "oauth2_authentication_failed"
        AuthenticationException exception = new OAuth2AuthenticationException(
            new OAuth2Error("unknown_error", "Some unknown error", null),
            "Unknown OAuth2 error"
        );

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<Adminlog> adminlogMock = mockStatic(Adminlog.class)) {

            handler.onAuthenticationFailure(request, response, exception);

            // Generic OAuth2 error without specific mapping
            verify(session).setAttribute("oauth2_logon_error", "oauth2_authentication_failed");
        }
    }

    /**
     * Test error code extraction for non-OAuth2 AuthenticationException
     */
    @Test
    void testErrorCodeNonOAuth2Exception() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        // Custom AuthenticationException subclass (not OAuth2AuthenticationException)
        AuthenticationException exception = new AuthenticationException("General authentication error") {};

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<Adminlog> adminlogMock = mockStatic(Adminlog.class)) {

            handler.onAuthenticationFailure(request, response, exception);

            verify(session).setAttribute("oauth2_logon_error", "oauth2_authentication_failed");
        }
    }

    // ============= Logging tests =============

    /**
     * Test that Adminlog is called during error handling
     */
    @Test
    void testAdminlogIsCalled() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(true);

        AuthenticationException exception = new OAuth2AuthenticationException(
            new OAuth2Error("access_denied", "Access denied", null),
            "access_denied error"
        );

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<Adminlog> adminlogMock = mockStatic(Adminlog.class)) {

            handler.onAuthenticationFailure(request, response, exception);

            adminlogMock.verify(() -> Adminlog.add(eq(Adminlog.TYPE_USER_LOGON), contains("OAuth2 authentication failed"), eq(-1), eq(-1)));
        }
    }

    /**
     * Test that SetCharacterEncodingFilter.registerDataContext is called
     */
    @Test
    void testRegisterDataContextIsCalled() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        AuthenticationException exception = new OAuth2AuthenticationException(
            new OAuth2Error("error", "Error", null),
            "generic error"
        );

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<Adminlog> adminlogMock = mockStatic(Adminlog.class)) {

            handler.onAuthenticationFailure(request, response, exception);

            filterMock.verify(() -> SetCharacterEncodingFilter.registerDataContext(request));
        }
    }
}
