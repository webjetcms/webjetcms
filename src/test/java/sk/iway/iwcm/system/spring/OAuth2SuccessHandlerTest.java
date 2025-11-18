package sk.iway.iwcm.system.spring;

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
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.PermissionGroupBean;
import sk.iway.iwcm.users.PermissionGroupDB;
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
 * Testy pre OAuth2SuccessHandler
 * Testujú rôzne scenáre prihlásenia cez OAuth2 vrátane vytvárania používateľov,
 * aktualizácie údajov a synchronizácie skupín
 */
class OAuth2SuccessHandlerTest extends BaseWebjetTest {

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

    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new OAuth2SuccessHandler();

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
        Map<String, Object> attributes = createGoogleOAuth2Attributes("test@example.com", "John", "Doe");
        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Mock existujúceho používateľa
        UserDetails existingUser = createTestUser("test@example.com", "John", "Doe");
        existingUser.setUserId(1);

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("test@example.com", 1)).thenReturn(existingUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);
            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, authentication);

            // Overenie
            verify(response).sendRedirect("/admin/");
            logonToolsMock.verify(() -> LogonTools.setUserToSession(eq(session), any(Identity.class)));
            verify(securityContext).setAuthentication(authentication);
        }
    }

    /**
     * Test vytvorenia nového používateľa cez OAuth2 (non-Keycloak)
     */
    @Test
    void testCreateNewUserFromOAuth2NonKeycloak() throws IOException {
        // Príprava OAuth2 používateľa (Google - non-Keycloak)
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

            // Zachytenie volania na vytvorenie používateľa
            ArgumentCaptor<UserDetails> userCaptor = ArgumentCaptor.forClass(UserDetails.class);
            usersDBMock.when(() -> UsersDB.saveUser(userCaptor.capture())).thenReturn(true);

            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, authentication);

            // Overenie vytvorenia používateľa
            UserDetails capturedUser = userCaptor.getValue();
            assertEquals("newuser@example.com", capturedUser.getEmail());
            assertEquals("Jane", capturedUser.getFirstName());
            assertEquals("Smith", capturedUser.getLastName());
            assertEquals("newuser", capturedUser.getLogin());
            assertTrue(capturedUser.isAuthorized());
            // Nový používateľ cez non-Keycloak provider NEMÁ automaticky admin práva
            assertFalse(capturedUser.isAdmin());

            verify(response).sendRedirect("/admin/");
        }
    }

    /**
     * Test vytvorenia nového používateľa cez Keycloak s admin skupinou
     */
    @Test
    void testCreateNewUserFromKeycloakWithAdminGroup() throws IOException {
        // Nastavenie admin skupiny a poskytovateľov so synchronizáciou práv v konštantách
        try (MockedStatic<Constants> constantsMock = mockStatic(Constants.class)) {
            constantsMock.when(() -> Constants.getString("NTLMAdminGroupName")).thenReturn("WebJETAdminGroup");
            constantsMock.when(() -> Constants.getString("oauth2_providersWithPermissions")).thenReturn("keycloak");

            // Príprava Keycloak OAuth2 používateľa s admin skupinou
            Map<String, Object> attributes = createKeycloakOAuth2Attributes(
                "admin@example.com",
                "Admin",
                "User",
                List.of("test_role"),
                List.of("WebJETAdminGroup", "normal-group")
            );

            OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "email"
            );
            OAuth2AuthenticationToken oauth2AuthToken = createOAuth2AuthenticationToken(oauth2User, "keycloak");

            try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
                 MockedStatic<UserGroupsDB> userGroupsDBMock = mockStatic(UserGroupsDB.class);
                 MockedStatic<PermissionGroupDB> permGroupDBMock = mockStatic(PermissionGroupDB.class);
                 MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
                 MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

                // Používateľ neexistuje
                usersDBMock.when(() -> UsersDB.getUserByEmail("admin@example.com", 1)).thenReturn(null);

                // Mock skupín
                UserGroupDetails adminGroup = new UserGroupDetails();
                adminGroup.setUserGroupId(1);
                adminGroup.setUserGroupName("WebJETAdminGroup");

                userGroupsDBMock.when(UserGroupsDB::getInstance).thenReturn(mock(UserGroupsDB.class));
                userGroupsDBMock.when(() -> UserGroupsDB.getInstance().getUserGroups())
                    .thenReturn(List.of(adminGroup));
                permGroupDBMock.when(() -> PermissionGroupDB.getPermissionGroups(null))
                    .thenReturn(List.of());
                userGroupsDBMock.when(() -> UserGroupsDB.getPermissionGroupsFor(anyInt()))
                    .thenReturn(List.of());

                // Zachytenie volania na vytvorenie používateľa
                ArgumentCaptor<UserDetails> userCaptor = ArgumentCaptor.forClass(UserDetails.class);
                usersDBMock.when(() -> UsersDB.saveUser(userCaptor.capture())).thenReturn(true);

                authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                    .thenReturn(oauth2AuthToken);

                // Spustenie testu
                handler.onAuthenticationSuccess(request, response, oauth2AuthToken);

                // Overenie vytvorenia používateľa
                List<UserDetails> capturedUsers = userCaptor.getAllValues();
                assertTrue(capturedUsers.size() >= 1, "At least one user save call should have been made");

                UserDetails newUser = capturedUsers.get(0); // Prvé volanie - vytvorenie používateľa
                assertEquals("admin@example.com", newUser.getEmail());
                assertEquals("Admin", newUser.getFirstName());
                assertEquals("User", newUser.getLastName());
                assertEquals("admin", newUser.getLogin());
                assertTrue(newUser.isAuthorized());
                // Pre Keycloak s admin skupinou má admin práva po aplikovaní práv
                if (capturedUsers.size() > 1) {
                    // Ak bolo viac volaní, posledné volanie má admin práva
                    UserDetails updatedUser = capturedUsers.get(capturedUsers.size() - 1);
                    assertTrue(updatedUser.isAdmin()); // Admin práva nastavené na základe skupiny
                } else {
                    // Ak bolo len jedno volanie, admin práva sú už nastavené
                    assertTrue(newUser.isAdmin()); // Admin práva nastavené na základe skupiny
                }

                verify(response).sendRedirect("/admin/");
            }
        }
    }

    /**
     * Test vytvorenia nového používateľa cez Keycloak BEZ admin skupiny
     */
    @Test
    void testCreateNewUserFromKeycloakWithoutAdminGroup() throws IOException {
        // Nastavenie admin skupiny a poskytovateľov so synchronizáciou práv v konštantách
        try (MockedStatic<Constants> constantsMock = mockStatic(Constants.class)) {
            constantsMock.when(() -> Constants.getString("NTLMAdminGroupName")).thenReturn("WebJETAdminGroup");
            constantsMock.when(() -> Constants.getString("oauth2_clientsWithPermissions")).thenReturn("keycloak");

            // Príprava Keycloak OAuth2 používateľa BEZ admin skupiny
            Map<String, Object> attributes = createKeycloakOAuth2Attributes(
                "user@example.com",
                "Regular",
                "User",
                List.of("test_role"),
                List.of("normal-group", "content-editors") // NEMÁ WebJETAdminGroup
            );

            OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "email"
            );
            OAuth2AuthenticationToken oauth2AuthToken = createOAuth2AuthenticationToken(oauth2User, "keycloak");

            try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
                 MockedStatic<UserGroupsDB> userGroupsDBMock = mockStatic(UserGroupsDB.class);
                 MockedStatic<PermissionGroupDB> permGroupDBMock = mockStatic(PermissionGroupDB.class);
                 MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
                 MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

                // Používateľ neexistuje
                usersDBMock.when(() -> UsersDB.getUserByEmail("user@example.com", 1)).thenReturn(null);

                // Mock skupín
                UserGroupDetails normalGroup = new UserGroupDetails();
                normalGroup.setUserGroupId(1);
                normalGroup.setUserGroupName("normal-group");

                userGroupsDBMock.when(UserGroupsDB::getInstance).thenReturn(mock(UserGroupsDB.class));
                userGroupsDBMock.when(() -> UserGroupsDB.getInstance().getUserGroups())
                    .thenReturn(List.of(normalGroup));
                permGroupDBMock.when(() -> PermissionGroupDB.getPermissionGroups(null))
                    .thenReturn(List.of());
                userGroupsDBMock.when(() -> UserGroupsDB.getPermissionGroupsFor(anyInt()))
                    .thenReturn(List.of());

                // Zachytenie volania na vytvorenie používateľa
                ArgumentCaptor<UserDetails> userCaptor = ArgumentCaptor.forClass(UserDetails.class);
                usersDBMock.when(() -> UsersDB.saveUser(userCaptor.capture())).thenReturn(true);

                authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                    .thenReturn(oauth2AuthToken);

                // Spustenie testu
                handler.onAuthenticationSuccess(request, response, oauth2AuthToken);

                // Overenie vytvorenia používateľa
                List<UserDetails> capturedUsers = userCaptor.getAllValues();
                assertTrue(capturedUsers.size() >= 1, "At least one user save call should have been made");

                UserDetails newUser = capturedUsers.get(0); // Prvé volanie - vytvorenie používateľa
                assertEquals("user@example.com", newUser.getEmail());
                assertEquals("Regular", newUser.getFirstName());
                assertEquals("User", newUser.getLastName());
                assertEquals("user", newUser.getLogin());
                assertTrue(newUser.isAuthorized());

                // Pre Keycloak BEZ admin skupiny nemá admin práva
                boolean hasAdmin = false;
                for (UserDetails user : capturedUsers) {
                    if (user.isAdmin()) {
                        hasAdmin = true;
                        break;
                    }
                }
                assertFalse(hasAdmin, "User should not have admin privileges without admin group");

                verify(response).sendRedirect("/admin/");
            }
        }
    }

    /**
     * Test aktualizácie existujúceho používateľa cez Keycloak
     */
    @Test
    void testUpdateExistingUserFromKeycloak() throws IOException {
        // Nastavenie poskytovateľov so synchronizáciou práv
        Constants.setString("oauth2_clientsWithPermissions", "keycloak");

        // Príprava Keycloak OAuth2 používateľa s rolami
        Map<String, Object> attributes = createKeycloakOAuth2Attributes(
            "keycloak@example.com",
            "Keycloak",
            "User",
            List.of("admin", "editor"),
            List.of("webjet-admin", "content-editors")
        );

        Collection<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ROLE_EDITOR")
        );

        OAuth2User oauth2User = new DefaultOAuth2User(authorities, attributes, "email");
        OAuth2AuthenticationToken oauth2AuthToken = createOAuth2AuthenticationToken(oauth2User, "keycloak");

        // Mock existujúceho používateľa
        UserDetails existingUser = createTestUser("keycloak@example.com", "Keycloak", "User");
        existingUser.setUserId(1);

        // Mock skupín
        UserGroupDetails adminGroup = new UserGroupDetails();
        adminGroup.setUserGroupId(1);
        adminGroup.setUserGroupName("admin");

        UserGroupDetails editorGroup = new UserGroupDetails();
        editorGroup.setUserGroupId(2);
        editorGroup.setUserGroupName("editor");

        PermissionGroupBean webjetAdminPerm = new PermissionGroupBean();
        webjetAdminPerm.setUserPermGroupId(1);
        webjetAdminPerm.setTitle("webjet-admin");

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<UserGroupsDB> userGroupsDBMock = mockStatic(UserGroupsDB.class);
             MockedStatic<PermissionGroupDB> permGroupDBMock = mockStatic(PermissionGroupDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("keycloak@example.com", 1)).thenReturn(existingUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);

            // Mock načítania skupín
            userGroupsDBMock.when(UserGroupsDB::getInstance).thenReturn(mock(UserGroupsDB.class));
            userGroupsDBMock.when(() -> UserGroupsDB.getInstance().getUserGroups())
                .thenReturn(List.of(adminGroup, editorGroup));

            permGroupDBMock.when(() -> PermissionGroupDB.getPermissionGroups(null))
                .thenReturn(List.of(webjetAdminPerm));

            userGroupsDBMock.when(() -> UserGroupsDB.getPermissionGroupsFor(1))
                .thenReturn(List.of());

            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(oauth2AuthToken);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, oauth2AuthToken);

            // Overenie
            verify(response).sendRedirect("/admin/");

            // Overenie pridania do skupín
            usersDBMock.verify(() -> UsersDB.addUserToPermissionGroup(1, 1));

            // Overenie uloženia používateľa so skupinami
            ArgumentCaptor<UserDetails> userCaptor = ArgumentCaptor.forClass(UserDetails.class);
            usersDBMock.verify(() -> UsersDB.saveUser(userCaptor.capture()), atLeastOnce());

            // Overenie, že používateľ bol pridaný do skupín
            UserDetails savedUser = userCaptor.getValue();
            assertNotNull(savedUser.getUserGroupsIds());
        }
    }

    /**
     * Test chybového scenára - chýbajúci email
     */
    @Test
    void testMissingEmail() throws IOException {
        // OAuth2 používateľ bez emailu
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("given_name", "John");
        attributes.put("family_name", "Doe");
        attributes.put("sub", "user-id-123");
        // Žiadny email

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "sub"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Spustenie testu
        handler.onAuthenticationSuccess(request, response, authentication);

        // Overenie presmerovanie na chybovú stránku
        verify(response).sendRedirect("/admin/logon.jsp?error=oauth2_email_not_found");
    }

    /**
     * Test chybového scenára - nepodarilo sa vytvoriť používateľa
     */
    @Test
    void testFailedUserCreation() throws IOException {
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
            // Vytvorenie zlyhá
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(false);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, authentication);

            // Overenie presmerovanie na chybovú stránku
            verify(response).sendRedirect("/admin/logon.jsp?error=user_create_failed");
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
        verify(response).sendRedirect("/admin/logon.jsp?error=oauth2_exception");
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

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("update@example.com", 1)).thenReturn(existingUser);

            // Zachytenie volania na aktualizáciu používateľa
            ArgumentCaptor<UserDetails> userCaptor = ArgumentCaptor.forClass(UserDetails.class);
            usersDBMock.when(() -> UsersDB.saveUser(userCaptor.capture())).thenReturn(true);

            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, authentication);

            // Overenie aktualizácie údajov
            List<UserDetails> capturedUsers = userCaptor.getAllValues();
            UserDetails updatedUser = capturedUsers.get(0); // Prvé volanie je pre aktualizáciu
            assertEquals("NewFirstName", updatedUser.getFirstName());
            assertEquals("NewLastName", updatedUser.getLastName());

            verify(response).sendRedirect("/admin/");
        }
    }

    /**
     * Test non-Keycloak providera (Google, Facebook) - bez synchronizácie skupín
     */
    @Test
    void testNonKeycloakProviderSkipsGroupSync() throws IOException {
        // Príprava Google OAuth2 používateľa s authoritami
        Map<String, Object> attributes = createGoogleOAuth2Attributes("google@example.com", "Google", "User");

        Collection<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        OAuth2User oauth2User = new DefaultOAuth2User(authorities, attributes, "email");
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Mock existujúceho používateľa
        UserDetails existingUser = createTestUser("google@example.com", "Google", "User");
        existingUser.setUserId(1);

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<UserGroupsDB> userGroupsDBMock = mockStatic(UserGroupsDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("google@example.com", 1)).thenReturn(existingUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);

            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            // Spustenie testu
            handler.onAuthenticationSuccess(request, response, authentication);

            // Overenie
            verify(response).sendRedirect("/admin/");

            // Overenie, že sa nepokúšalo načítavať skupiny (nie je Keycloak)
            userGroupsDBMock.verify(UserGroupsDB::getInstance, never());
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

        // Groups attribute
        attributes.put("groups", groups);

        // Resource access (for client-specific roles)
        Map<String, Object> resourceAccess = new HashMap<>();
        Map<String, Object> clientAccess = new HashMap<>();
        clientAccess.put("roles", List.of("webjet-user"));
        resourceAccess.put("webjet-client", clientAccess);
        attributes.put("resource_access", resourceAccess);

        return attributes;
    }

    private UserDetails createTestUser(String email, String firstName, String lastName) {
        UserDetails user = new UserDetails();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(email.substring(0, email.indexOf("@")));
        user.setAuthorized(true);
        user.setAdmin(true);
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
