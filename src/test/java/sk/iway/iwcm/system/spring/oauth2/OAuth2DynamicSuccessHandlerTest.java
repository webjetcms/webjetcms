package sk.iway.iwcm.system.spring.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.system.spring.WebjetAuthentificationProvider;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for OAuth2DynamicSuccessHandler
 * Tests dynamic delegation to admin or user handler based on session attribute
 */
class OAuth2DynamicSuccessHandlerTest extends BaseWebjetTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private OAuth2DynamicSuccessHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new OAuth2DynamicSuccessHandler();

        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        SecurityContextHolder.setContext(securityContext);

        // Clear Constants
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();
    }

    // ============= Admin handler delegation tests =============

    /**
     * Test that admin handler is called when session attribute indicates admin login
     */
    @Test
    void testDelegatesToAdminHandler() throws Exception {
        // Set session attribute for admin login
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(true);

        // Prepare OAuth2 user with email
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "admin@example.com");
        attributes.put("given_name", "Admin");
        attributes.put("family_name", "User");
        attributes.put("sub", "admin-user-id");

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Mock existing admin user
        UserDetails adminUser = new UserDetails();
        adminUser.setUserId(1);
        adminUser.setEmail("admin@example.com");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setLogin("admin");
        adminUser.setAuthorized(true);
        adminUser.setAdmin(true);

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("admin@example.com", 1)).thenReturn(adminUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);
            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            handler.onAuthenticationSuccess(request, response, authentication);

            // Verify admin redirect
            verify(response).sendRedirect("/admin/");

            // Verify session attribute was cleared
            verify(session).removeAttribute("oauth2_is_admin_section");
        }
    }

    // ============= User handler delegation tests =============

    /**
     * Test that user handler is called when session attribute indicates user login
     */
    @Test
    void testDelegatesToUserHandler() throws Exception {
        // Set session attribute for user login
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        // Prepare OAuth2 user with email
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "user@example.com");
        attributes.put("given_name", "Regular");
        attributes.put("family_name", "User");
        attributes.put("sub", "user-id");

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Mock existing user
        UserDetails regularUser = new UserDetails();
        regularUser.setUserId(2);
        regularUser.setEmail("user@example.com");
        regularUser.setFirstName("Regular");
        regularUser.setLastName("User");
        regularUser.setLogin("user");
        regularUser.setAuthorized(true);
        regularUser.setAdmin(false);

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("user@example.com", 1)).thenReturn(regularUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);
            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            when(session.getAttribute("afterLogonRedirect")).thenReturn(null);

            handler.onAuthenticationSuccess(request, response, authentication);

            // Verify user redirect (default /)
            verify(response).sendRedirect("/");

            // Verify session attribute was cleared
            verify(session).removeAttribute("oauth2_is_admin_section");
        }
    }

    /**
     * Test that user handler is called when session attribute is null (defaults to user)
     */
    @Test
    void testDefaultsToUserHandler() throws Exception {
        // No session attribute set (null)
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(null);

        // Prepare OAuth2 user
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "user@example.com");
        attributes.put("given_name", "User");
        attributes.put("family_name", "Test");
        attributes.put("sub", "user-id");

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Mock existing user
        UserDetails user = new UserDetails();
        user.setUserId(1);
        user.setEmail("user@example.com");
        user.setFirstName("User");
        user.setLastName("Test");
        user.setLogin("user");
        user.setAuthorized(true);
        user.setAdmin(false);

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("user@example.com", 1)).thenReturn(user);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);
            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            when(session.getAttribute("afterLogonRedirect")).thenReturn(null);

            handler.onAuthenticationSuccess(request, response, authentication);

            // Should default to user handler (redirects to /)
            verify(response).sendRedirect("/");
        }
    }

    // ============= RegisterDataContext tests =============

    /**
     * Test that SetCharacterEncodingFilter.registerDataContext is called
     */
    @Test
    void testRegisterDataContextIsCalled() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "user@example.com");
        attributes.put("sub", "user-id");

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Mock existing user
        UserDetails user = new UserDetails();
        user.setUserId(1);
        user.setEmail("user@example.com");
        user.setLogin("user");
        user.setAuthorized(true);
        user.setAdmin(false);

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("user@example.com", 1)).thenReturn(user);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);
            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            when(session.getAttribute("afterLogonRedirect")).thenReturn(null);

            handler.onAuthenticationSuccess(request, response, authentication);

            // Verify registerDataContext was called (at least by dynamic handler, possibly also by delegated handler)
            filterMock.verify(() -> SetCharacterEncodingFilter.registerDataContext(request), atLeastOnce());
        }
    }

    // ============= Attribute clearing tests =============

    /**
     * Test that session attribute is cleared after determining login type
     */
    @Test
    void testSessionAttributeCleared() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(true);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "admin@example.com");
        attributes.put("sub", "admin-id");

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        UserDetails adminUser = new UserDetails();
        adminUser.setUserId(1);
        adminUser.setEmail("admin@example.com");
        adminUser.setLogin("admin");
        adminUser.setAuthorized(true);
        adminUser.setAdmin(true);

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("admin@example.com", 1)).thenReturn(adminUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);
            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            handler.onAuthenticationSuccess(request, response, authentication);

            // Verify attribute is removed after use
            verify(session).removeAttribute("oauth2_is_admin_section");
        }
    }

    // ============= Error handling delegation tests =============

    /**
     * Test that error from admin handler is properly propagated
     */
    @Test
    void testAdminHandlerErrorPropagation() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(true);

        // OAuth2 user without email - will cause error in admin handler
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("given_name", "Admin");
        attributes.put("sub", "admin-id");
        // No email!

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
            attributes,
            "sub"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class)) {

            handler.onAuthenticationSuccess(request, response, authentication);

            // Admin handler should set error and redirect to admin logon
            verify(session).setAttribute("oauth2_logon_error", "oauth2_email_not_found");
            verify(response).sendRedirect("/admin/logon/");
        }
    }

    /**
     * Test that error from user handler is properly propagated
     */
    @Test
    void testUserHandlerErrorPropagation() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        // OAuth2 user without email - will cause error in user handler
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("given_name", "User");
        attributes.put("sub", "user-id");
        // No email!

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "sub"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class)) {

            handler.onAuthenticationSuccess(request, response, authentication);

            // User handler should set error and redirect to /
            verify(session).setAttribute("oauth2_logon_error", "oauth2_email_not_found");
            verify(response).sendRedirect("/");
        }
    }

    // ============= Custom redirect tests =============

    /**
     * Test user handler with custom afterLogonRedirect
     */
    @Test
    void testUserHandlerWithCustomRedirect() throws Exception {
        when(session.getAttribute("oauth2_is_admin_section")).thenReturn(false);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "user@example.com");
        attributes.put("sub", "user-id");

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        UserDetails user = new UserDetails();
        user.setUserId(1);
        user.setEmail("user@example.com");
        user.setLogin("user");
        user.setAuthorized(true);
        user.setAdmin(false);

        try (MockedStatic<SetCharacterEncodingFilter> filterMock = mockStatic(SetCharacterEncodingFilter.class);
             MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("user@example.com", 1)).thenReturn(user);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);
            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            // Set custom redirect
            when(session.getAttribute("afterLogonRedirect")).thenReturn("/sk/objednavky/");

            handler.onAuthenticationSuccess(request, response, authentication);

            // Should redirect to custom URL
            verify(response).sendRedirect("/sk/objednavky/");
        }
    }
}
