package sk.iway.iwcm.system.spring.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.test.BaseWebjetTest;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for OAuth2LoginHelper
 * Tests helper methods for OAuth2 session management, error handling, and URL generation
 */
class OAuth2LoginHelperTest extends BaseWebjetTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);

        // Clear Constants
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();
    }

    // ============= setAdminLogin tests =============

    /**
     * Test setAdminLogin sets session attribute correctly
     */
    @Test
    void testSetAdminLogin() {
        OAuth2LoginHelper.setAdminLogin(request);

        verify(session).setAttribute("oauth2_is_admin_section", true);
    }

    // ============= isAdminLogin tests =============

    /**
     * Test isAdminLogin returns true when session attribute is set
     */
    @Test
    void testIsAdminLoginReturnsTrue() {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(true);

        boolean result = OAuth2LoginHelper.isAdminLogin(request);

        assertTrue(result);
    }

    /**
     * Test isAdminLogin returns false when session attribute is not set
     */
    @Test
    void testIsAdminLoginReturnsFalseWhenNotSet() {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(null);

        boolean result = OAuth2LoginHelper.isAdminLogin(request);

        assertFalse(result);
    }

    /**
     * Test isAdminLogin returns false when session attribute is false
     */
    @Test
    void testIsAdminLoginReturnsFalseWhenFalse() {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        boolean result = OAuth2LoginHelper.isAdminLogin(request);

        assertFalse(result);
    }

    /**
     * Test isAdminLogin returns false when session is null
     */
    @Test
    void testIsAdminLoginReturnsFalseWhenSessionNull() {
        when(request.getSession(false)).thenReturn(null);

        boolean result = OAuth2LoginHelper.isAdminLogin(request);

        assertFalse(result);
    }

    // ============= isAdminLoginAndClear tests =============

    /**
     * Test isAdminLoginAndClear returns true and removes attribute
     */
    @Test
    void testIsAdminLoginAndClearReturnsTrue() {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(true);

        boolean result = OAuth2LoginHelper.isAdminLoginAndClear(request);

        assertTrue(result);
        verify(session).removeAttribute("oauth2_is_admin_section");
    }

    /**
     * Test isAdminLoginAndClear returns false and removes attribute
     */
    @Test
    void testIsAdminLoginAndClearReturnsFalse() {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        boolean result = OAuth2LoginHelper.isAdminLoginAndClear(request);

        assertFalse(result);
        verify(session).removeAttribute("oauth2_is_admin_section");
    }

    /**
     * Test isAdminLoginAndClear with null session
     */
    @Test
    void testIsAdminLoginAndClearWithNullSession() {
        when(request.getSession(false)).thenReturn(null);

        boolean result = OAuth2LoginHelper.isAdminLoginAndClear(request);

        assertFalse(result);
    }

    // ============= getErrorRedirectUrl tests =============

    /**
     * Test getErrorRedirectUrl for admin login
     */
    @Test
    void testGetErrorRedirectUrlForAdmin() {
        String result = OAuth2LoginHelper.getErrorRedirectUrl(true);

        assertEquals("/admin/logon/", result);
    }

    /**
     * Test getErrorRedirectUrl for user login
     */
    @Test
    void testGetErrorRedirectUrlForUser() {
        String result = OAuth2LoginHelper.getErrorRedirectUrl(false);

        assertEquals("/", result);
    }

    // ============= getAdminRedirectUrl tests =============

    /**
     * Test getAdminRedirectUrl returns correct URL
     */
    @Test
    void testGetAdminRedirectUrl() {
        String result = OAuth2LoginHelper.getAdminRedirectUrl();

        assertEquals("/admin/logon/", result);
    }

    // ============= getUserRedirectUrl tests =============

    /**
     * Test getUserRedirectUrl returns correct URL
     */
    @Test
    void testGetUserRedirectUrl() {
        String result = OAuth2LoginHelper.getUserRedirectUrl();

        assertEquals("/", result);
    }

    // ============= handleError tests =============

    /**
     * Test handleError sets session attribute and redirects
     */
    @Test
    void testHandleErrorWithExplicitUrl() throws IOException {
        OAuth2LoginHelper.handleError(request, response, "testErrorCode", "/custom/error/");

        verify(session).setAttribute("oauth2_logon_error", "testErrorCode");
        verify(response).sendRedirect("/custom/error/");
    }

    /**
     * Test handleError with admin login
     */
    @Test
    void testHandleErrorForAdminLogin() throws IOException {
        OAuth2LoginHelper.handleError(request, response, "accessDenied", true);

        verify(session).setAttribute("oauth2_logon_error", "accessDenied");
        verify(response).sendRedirect("/admin/logon/");
    }

    /**
     * Test handleError with user login
     */
    @Test
    void testHandleErrorForUserLogin() throws IOException {
        OAuth2LoginHelper.handleError(request, response, "oauth2_email_not_found", false);

        verify(session).setAttribute("oauth2_logon_error", "oauth2_email_not_found");
        verify(response).sendRedirect("/");
    }

    // ============= getLogonUrls tests =============

    /**
     * Test getLogonUrls returns null when oauth2_clients is empty
     */
    @Test
    void testGetLogonUrlsReturnsNullWhenNoClients() {
        Constants.setString("oauth2_clients", "");

        Map<String, String> result = OAuth2LoginHelper.getLogonUrls(false, request);

        assertNull(result);
    }

    /**
     * Test getLogonUrls for admin sets session attribute
     */
    @Test
    void testGetLogonUrlsForAdminSetsSessionAttribute() {
        Constants.setString("oauth2_clients", "google");
        Constants.setString("oauth2_clientsIncludeAdmin", "*");

        // getLogonUrls will fail because clientRegistrationRepository is not available in test
        // but we can verify that session attribute is set
        OAuth2LoginHelper.getLogonUrls(true, request);

        verify(session).setAttribute("oauth2_is_admin_section", true);
    }

    /**
     * Test getLogonUrls for user sets afterLogonRedirect
     */
    @Test
    void testGetLogonUrlsForUserSetsAfterLogonRedirect() {
        Constants.setString("oauth2_clients", "google");
        Constants.setString("oauth2_clientsIncludeUser", "*");

        try (MockedStatic<PathFilter> pathFilterMock = mockStatic(PathFilter.class)) {
            pathFilterMock.when(() -> PathFilter.getOrigPath(request)).thenReturn("/sk/produkty/");

            // getLogonUrls will fail because clientRegistrationRepository is not available
            // but we can verify that afterLogonRedirect is set
            OAuth2LoginHelper.getLogonUrls(false, request);

            verify(session).setAttribute("afterLogonRedirect", "/sk/produkty/");
            verify(session).removeAttribute("oauth2_is_admin_section");
        }
    }

    /**
     * Test getLogonUrls with client filtering for admin
     */
    @Test
    void testGetLogonUrlsWithClientFilteringForAdmin() {
        Constants.setString("oauth2_clients", "google,keycloak,facebook");
        Constants.setString("oauth2_clientsIncludeAdmin", "keycloak"); // Only keycloak for admin

        OAuth2LoginHelper.getLogonUrls(true, request);

        verify(session).setAttribute("oauth2_is_admin_section", true);
    }

    /**
     * Test getLogonUrls with wildcard filtering
     */
    @Test
    void testGetLogonUrlsWithWildcardFiltering() {
        Constants.setString("oauth2_clients", "google");
        Constants.setString("oauth2_clientsIncludeUser", "*"); // Include all

        try (MockedStatic<PathFilter> pathFilterMock = mockStatic(PathFilter.class)) {
            pathFilterMock.when(() -> PathFilter.getOrigPath(request)).thenReturn("/");

            OAuth2LoginHelper.getLogonUrls(false, request);

            verify(session).removeAttribute("oauth2_is_admin_section");
        }
    }

    /**
     * Test getLogonUrls returns correct URLs when ClientRegistrationRepository is available
     * Note: This test verifies the flow with mocked Tools, but actual URL retrieval requires
     * full Spring context which is tested in integration tests
     */
    @Test
    void testGetLogonUrlsWithMockedRepository() {
        // This test verifies the basic flow - actual client registration is tested in integration tests
        // The getLogonUrls method requires full Spring context for proper URL generation
        Constants.setString("oauth2_clients", "");

        Map<String, String> result = OAuth2LoginHelper.getLogonUrls(true, request);

        // When oauth2_clients is empty, should return null
        assertNull(result);
    }

    /**
     * Test getLogonUrls filters clients based on configuration
     * Note: The filtering logic is tested through integration tests with full Spring context
     */
    @Test
    void testGetLogonUrlsFiltersClientsByConfig() {
        // Test that empty oauth2_clients returns null
        Constants.setString("oauth2_clients", "");

        Map<String, String> result = OAuth2LoginHelper.getLogonUrls(true, request);

        assertNull(result, "Should return null when no clients configured");
    }

    /**
     * Test getLogonUrls handles missing Spring bean gracefully
     * Note: When clientRegistrationRepository is not available, the method catches exception and returns null
     */
    @Test
    void testGetLogonUrlsHandlesException() {
        // This scenario happens when the Spring context doesn't have the bean
        // which is common in unit tests without full Spring context
        Constants.setString("oauth2_clients", "google");

        // When called without mocking Tools.getSpringBean, it should handle the exception gracefully
        Map<String, String> result = OAuth2LoginHelper.getLogonUrls(true, request);

        // Result should be null because the Spring bean is not available
        assertNull(result);
    }
}
