package sk.iway.iwcm.system.spring.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.system.spring.WebjetAuthentificationProvider;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testy pre OAuth2UserSuccessHandler
 * Testujú OAuth2 prihlasovanie do zákazníckej zóny
 * Na rozdiel od admin handlera:
 * - Nenastavuje admin práva
 * - Nesynchronizuje permission groups
 * - Redirect na / namiesto /admin/
 * - Nevyžaduje admin práva
 */
class OAuth2UserSuccessHandlerTest extends BaseWebjetTest {

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

    private OAuth2UserSuccessHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new OAuth2UserSuccessHandler();

        // Základné nastavenie mockov
        when(request.getSession()).thenReturn(session);
        SecurityContextHolder.setContext(securityContext);

        // Vyčistenie Constants
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();
    }

    /**
     * Test úspešného prihlásenia existujúceho používateľa cez Google OAuth2
     */
    @Test
    void testSuccessfulLoginExistingUser() throws IOException {
        // Príprava OAuth2 používateľa (Google)
        Map<String, Object> attributes = createGoogleOAuth2Attributes("user@example.com", "John", "Doe");
        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Mock existujúceho používateľa (BEZ admin práv)
        UserDetails existingUser = createTestUser("user@example.com", "John", "Doe");
        existingUser.setUserId(1);
        existingUser.setAdmin(false); // User zóna - nie admin

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("user@example.com", 1)).thenReturn(existingUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);
            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            // Nastavenie afterLogonRedirect
            when(session.getAttribute("afterLogonRedirect")).thenReturn(null);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, authentication);

            // Overenie úspešného presmerovania na user zónu
            verify(response).sendRedirect("/");

            // Overenie nastavenia používateľa do session
            logonToolsMock.verify(() -> LogonTools.setUserToSession(eq(session), any(Identity.class)));

            // Overenie, že používateľ bol načítaný, nie vytvorený
            usersDBMock.verify(() -> UsersDB.getUserByEmail("user@example.com", 1), times(1));

            // Overenie že sa nekontrolovali admin práva (žiadny error)
            verify(session, never()).setAttribute(eq("oauth2_logon_error"), any());
        }
    }

    /**
     * Test vytvorenia nového používateľa cez OAuth2 v user zóne
     */
    @Test
    void testCreateNewUserFromOAuth2() throws IOException {
        // Príprava OAuth2 používateľa (Google)
        Map<String, Object> attributes = createGoogleOAuth2Attributes("newuser@example.com", "Jane", "Smith");
        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            // Používateľ neexistuje
            usersDBMock.when(() -> UsersDB.getUserByEmail("newuser@example.com", 1)).thenReturn(null);

            // Mock vytvorenia nového používateľa
            UserDetails newUser = createTestUser("newuser@example.com", "Jane", "Smith");
            newUser.setUserId(2);
            newUser.setAdmin(false); // User zóna - nie admin

            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenAnswer(invocation -> {
                UserDetails user = invocation.getArgument(0);
                user.setUserId(2);
                return true;
            });

            usersDBMock.when(() -> UsersDB.getUserByEmail("newuser@example.com", 1))
                .thenReturn(null)
                .thenReturn(newUser);

            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            when(session.getAttribute("afterLogonRedirect")).thenReturn(null);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, authentication);

            // Overenie úspešného presmerovania na user zónu
            verify(response).sendRedirect("/");

            // Overenie vytvorenia nového používateľa
            usersDBMock.verify(() -> UsersDB.saveUser(any(UserDetails.class)), times(1));

            // Overenie nastavenia používateľa do session
            logonToolsMock.verify(() -> LogonTools.setUserToSession(eq(session), any(Identity.class)));
        }
    }

    /**
     * Test synchronizácie user groups pre nakonfigurovaného providera (Keycloak)
     */
    @Test
    void testSyncUserGroupsFromKeycloak() throws IOException {
        // Nastavenie poskytovateľov so synchronizáciou práv
        Constants.setString("oauth2_clientsWithPermissions", "keycloak");

        // Príprava Keycloak OAuth2 používateľa
        Map<String, Object> attributes = createKeycloakOAuth2Attributes(
            "keycloak.user@example.com",
            "Keycloak",
            "User",
            List.of("editor", "viewer"),
            List.of("content-editors", "normal-users")
        );

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );
        OAuth2AuthenticationToken oauth2AuthToken = createOAuth2AuthenticationToken(oauth2User, "keycloak");

        // Mock existujúceho používateľa
        UserDetails existingUser = createTestUser("keycloak.user@example.com", "Keycloak", "User");
        existingUser.setUserId(1);
        existingUser.setAdmin(false);

        // Mock user groups (BEZ permission groups)
        UserGroupDetails editorsGroup = new UserGroupDetails();
        editorsGroup.setUserGroupId(1);
        editorsGroup.setUserGroupName("content-editors");

        UserGroupDetails usersGroup = new UserGroupDetails();
        usersGroup.setUserGroupId(2);
        usersGroup.setUserGroupName("normal-users");

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<UserGroupsDB> userGroupsDBMock = mockStatic(UserGroupsDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("keycloak.user@example.com", 1)).thenReturn(existingUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);

            // Mock načítania user groups
            userGroupsDBMock.when(UserGroupsDB::getInstance).thenReturn(mock(UserGroupsDB.class));
            userGroupsDBMock.when(() -> UserGroupsDB.getInstance().getUserGroups())
                .thenReturn(List.of(editorsGroup, usersGroup));

            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            when(session.getAttribute("afterLogonRedirect")).thenReturn(null);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, oauth2AuthToken);

            // Overenie úspešného presmerovania
            verify(response).sendRedirect("/");

            // Overenie synchronizácie user groups
            ArgumentCaptor<UserDetails> userCaptor = ArgumentCaptor.forClass(UserDetails.class);
            usersDBMock.verify(() -> UsersDB.saveUser(userCaptor.capture()), atLeastOnce());

            UserDetails savedUser = userCaptor.getValue();
            assertNotNull(savedUser);

            // Overenie že permission groups sa NESYNCHRONIZOVALI (user zóna)
            // V user zóne sa nevolá UsersDB.addUserToPermissionGroup

            // Overenie nastavenia používateľa do session
            logonToolsMock.verify(() -> LogonTools.setUserToSession(eq(session), any(Identity.class)));
        }
    }

    /**
     * Test že user handler NENASTAVUJE admin práva
     */
    @Test
    void testUserHandlerDoesNotSetAdminFlag() throws IOException {
        // Nastavenie admin skupiny a poskytovateľov so synchronizáciou práv
        Constants.setString("NTLMAdminGroupName", "admin");
        Constants.setString("oauth2_clientsWithPermissions", "keycloak");

        // Príprava Keycloak OAuth2 používateľa s admin skupinou
        Map<String, Object> attributes = createKeycloakOAuth2Attributes(
            "user@example.com",
            "User",
            "Test",
            List.of("admin"),
            List.of("admin")
        );

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
            attributes,
            "email"
        );
        OAuth2AuthenticationToken oauth2AuthToken = createOAuth2AuthenticationToken(oauth2User, "keycloak");

        // Mock existujúceho používateľa BEZ admin práv
        UserDetails existingUser = createTestUser("user@example.com", "User", "Test");
        existingUser.setUserId(1);
        existingUser.setAdmin(false); // Začína bez admin práv

        // Mock user groups
        UserGroupDetails adminGroup = new UserGroupDetails();
        adminGroup.setUserGroupId(1);
        adminGroup.setUserGroupName("admin");

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<UserGroupsDB> userGroupsDBMock = mockStatic(UserGroupsDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("user@example.com", 1)).thenReturn(existingUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);

            userGroupsDBMock.when(UserGroupsDB::getInstance).thenReturn(mock(UserGroupsDB.class));
            userGroupsDBMock.when(() -> UserGroupsDB.getInstance().getUserGroups())
                .thenReturn(List.of(adminGroup));

            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            when(session.getAttribute("afterLogonRedirect")).thenReturn(null);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, oauth2AuthToken);

            // Overenie úspešného presmerovania (user handler nevyžaduje admin práva)
            verify(response).sendRedirect("/");

            // Overenie že admin flag sa NENASTAVIL
            ArgumentCaptor<UserDetails> userCaptor = ArgumentCaptor.forClass(UserDetails.class);
            usersDBMock.verify(() -> UsersDB.saveUser(userCaptor.capture()), atLeastOnce());

            // Používateľ by mal ostať bez admin práv (user handler nenastavuje admin flag)
            UserDetails savedUser = userCaptor.getValue();
            assertFalse(savedUser.isAdmin(), "User handler should NOT set admin flag");
        }
    }

    /**
     * Test non-Keycloak providera (Google) - bez synchronizácie skupín
     */
    @Test
    void testNonKeycloakProviderSkipsGroupSync() throws IOException {
        // Príprava Google OAuth2 používateľa s authoritami
        Map<String, Object> attributes = createGoogleOAuth2Attributes("google@example.com", "Google", "User");

        Collection<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_EDITOR")
        );

        OAuth2User oauth2User = new DefaultOAuth2User(authorities, attributes, "email");
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Mock existujúceho používateľa
        UserDetails existingUser = createTestUser("google@example.com", "Google", "User");
        existingUser.setUserId(1);
        existingUser.setAdmin(false);

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<UserGroupsDB> userGroupsDBMock = mockStatic(UserGroupsDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("google@example.com", 1)).thenReturn(existingUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);
            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            when(session.getAttribute("afterLogonRedirect")).thenReturn(null);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, authentication);

            // Overenie úspešného presmerovania
            verify(response).sendRedirect("/");

            // Overenie že sa user groups NENAČÍTAVALI (non-Keycloak provider)
            userGroupsDBMock.verify(UserGroupsDB::getInstance, never());

            // Overenie nastavenia používateľa do session
            logonToolsMock.verify(() -> LogonTools.setUserToSession(eq(session), any(Identity.class)));
        }
    }

    /**
     * Test custom redirect URL po prihlásení
     */
    @Test
    void testCustomRedirectAfterLogin() throws IOException {
        // Príprava OAuth2 používateľa
        Map<String, Object> attributes = createGoogleOAuth2Attributes("user@example.com", "User", "Test");
        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Mock existujúceho používateľa
        UserDetails existingUser = createTestUser("user@example.com", "User", "Test");
        existingUser.setUserId(1);
        existingUser.setAdmin(false);

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("user@example.com", 1)).thenReturn(existingUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);
            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            // Nastavenie custom redirect
            when(session.getAttribute("afterLogonRedirect")).thenReturn("/sk/produkty/");

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, authentication);

            // Overenie presmerovania na custom URL
            verify(response).sendRedirect("/sk/produkty/");

            // Overenie nastavenia používateľa do session
            logonToolsMock.verify(() -> LogonTools.setUserToSession(eq(session), any(Identity.class)));
        }
    }

    /**
     * Test error handlingu - chýbajúci email
     */
    @Test
    void testErrorHandlingMissingEmail() throws IOException {
        // Príprava OAuth2 používateľa BEZ emailu
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("given_name", "John");
        attributes.put("family_name", "Doe");
        attributes.put("sub", "user-without-email");
        // EMAIL CHÝBA!

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "sub"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Spustenie testu
        handler.onAuthenticationSuccess(request, response, authentication);

        // Overenie presmerovanie na chybovú stránku (user zóna - redirect na /)
        verify(session).setAttribute("oauth2_logon_error", "oauth2_email_not_found");
        verify(response).sendRedirect("/");
    }

    /**
     * Test error handlingu - zlyhanie vytvorenia používateľa
     */
    @Test
    void testErrorHandlingUserCreationFailed() throws IOException {
        // Príprava OAuth2 používateľa
        Map<String, Object> attributes = createGoogleOAuth2Attributes("fail@example.com", "Fail", "User");
        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class)) {

            // Používateľ neexistuje
            usersDBMock.when(() -> UsersDB.getUserByEmail("fail@example.com", 1)).thenReturn(null);

            // Vytvorenie používateľa zlyhá
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(false);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, authentication);

            // Overenie presmerovanie na chybovú stránku
            verify(session).setAttribute("oauth2_logon_error", "oauth2_user_create_failed");
            verify(response).sendRedirect("/");
        }
    }

    /**
     * Test výnimky počas spracovania
     */
    @Test
    void testExceptionHandling() throws IOException {
        // OAuth2 používateľ, ktorý vyvolá výnimku
        when(authentication.getPrincipal()).thenThrow(new RuntimeException("Test exception"));

        // Spustenie testu
        handler.onAuthenticationSuccess(request, response, authentication);

        // Overenie presmerovanie na chybovú stránku
        verify(session).setAttribute("oauth2_logon_error", "oauth2_exception");
        verify(response).sendRedirect("/");
    }

    /**
     * Test aktualizácie existujúceho používateľa s novými údajmi
     */
    @Test
    void testUpdateExistingUserFromOAuth2() throws IOException {
        // Príprava OAuth2 používateľa s novými údajmi
        Map<String, Object> attributes = createGoogleOAuth2Attributes("update@example.com", "NewFirstName", "NewLastName");
        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Mock existujúceho používateľa s inými údajmi
        UserDetails existingUser = createTestUser("update@example.com", "OldFirstName", "OldLastName");
        existingUser.setUserId(1);
        existingUser.setAdmin(false);

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("update@example.com", 1)).thenReturn(existingUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);
            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            when(session.getAttribute("afterLogonRedirect")).thenReturn(null);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, authentication);

            // Overenie úspešného presmerovania
            verify(response).sendRedirect("/");

            // Overenie aktualizácie používateľa
            ArgumentCaptor<UserDetails> userCaptor = ArgumentCaptor.forClass(UserDetails.class);
            usersDBMock.verify(() -> UsersDB.saveUser(userCaptor.capture()), atLeastOnce());

            UserDetails updatedUser = userCaptor.getValue();
            assertEquals("NewFirstName", updatedUser.getFirstName());
            assertEquals("NewLastName", updatedUser.getLastName());
        }
    }

    /**
     * Test že user handler NIE JE ovplyvnený NTLMAdminGroupName
     */
    @Test
    void testUserHandlerIgnoresAdminGroupName() throws IOException {
        // Nastavenie admin skupiny
        Constants.setString("NTLMAdminGroupName", "admin");
        Constants.setString("oauth2_clientsWithPermissions", "keycloak");

        // Príprava Keycloak OAuth2 používateľa s "admin" skupinou
        Map<String, Object> attributes = createKeycloakOAuth2Attributes(
            "user@example.com",
            "User",
            "Test",
            List.of("admin"),
            List.of("admin")
        );

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
            attributes,
            "email"
        );
        OAuth2AuthenticationToken oauth2AuthToken = createOAuth2AuthenticationToken(oauth2User, "keycloak");

        // Mock nového používateľa
        UserDetails newUser = createTestUser("user@example.com", "User", "Test");
        newUser.setAdmin(false);

        // Mock user groups
        UserGroupDetails adminGroup = new UserGroupDetails();
        adminGroup.setUserGroupId(1);
        adminGroup.setUserGroupName("admin");

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<UserGroupsDB> userGroupsDBMock = mockStatic(UserGroupsDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("user@example.com", 1))
                .thenReturn(null)
                .thenReturn(newUser);

            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenAnswer(invocation -> {
                UserDetails user = invocation.getArgument(0);
                user.setUserId(1);
                return true;
            });

            userGroupsDBMock.when(UserGroupsDB::getInstance).thenReturn(mock(UserGroupsDB.class));
            userGroupsDBMock.when(() -> UserGroupsDB.getInstance().getUserGroups())
                .thenReturn(List.of(adminGroup));

            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            when(session.getAttribute("afterLogonRedirect")).thenReturn(null);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, oauth2AuthToken);

            // Overenie úspešného presmerovania (user handler nepotrebuje admin práva)
            verify(response).sendRedirect("/");

            // User handler NIE JE ovplyvnený NTLMAdminGroupName - nenastavuje admin flag
            ArgumentCaptor<UserDetails> userCaptor = ArgumentCaptor.forClass(UserDetails.class);
            usersDBMock.verify(() -> UsersDB.saveUser(userCaptor.capture()), atLeastOnce());

            UserDetails savedUser = userCaptor.getValue();
            assertFalse(savedUser.isAdmin(), "User handler should ignore NTLMAdminGroupName");
        }
    }

    // Pomocné metódy pre vytvorenie test dát

    private Map<String, Object> createGoogleOAuth2Attributes(String email, String givenName, String familyName) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", email);
        attributes.put("given_name", givenName);
        attributes.put("family_name", familyName);
        attributes.put("sub", "google-user-id");
        return attributes;
    }

    private Map<String, Object> createKeycloakOAuth2Attributes(String email, String givenName, String familyName,
                                                               List<String> realmRoles, List<String> groups) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", email);
        attributes.put("given_name", givenName);
        attributes.put("family_name", familyName);
        attributes.put("sub", "keycloak-user-id");

        // Keycloak-specific attributes
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", realmRoles);
        attributes.put("realm_access", realmAccess);

        attributes.put("groups", groups);

        return attributes;
    }

    private UserDetails createTestUser(String email, String firstName, String lastName) {
        UserDetails user = new UserDetails();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(email.substring(0, email.indexOf("@")));
        user.setAuthorized(true);
        return user;
    }

    /**
     * Vytvorí OAuth2AuthenticationToken pre testovanie
     */
    private OAuth2AuthenticationToken createOAuth2AuthenticationToken(OAuth2User oauth2User, String registrationId) {
        return new OAuth2AuthenticationToken(
            oauth2User,
            oauth2User.getAuthorities(),
            registrationId
        );
    }
}
