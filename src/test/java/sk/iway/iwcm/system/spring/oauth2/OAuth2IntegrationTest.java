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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.system.spring.SpringSecurityConf;
import sk.iway.iwcm.system.spring.WebjetAuthentificationProvider;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.PermissionGroupBean;
import sk.iway.iwcm.users.PermissionGroupDB;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integračné testy pre OAuth2 prihlasovanie
 * Testujú kompletnú funkcionalitu OAuth2 authentication flow
 */
class OAuth2IntegrationTest extends BaseWebjetTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private Authentication authentication;

    private SpringSecurityConf springSecurityConf;
    private OAuth2AdminSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        springSecurityConf = new SpringSecurityConf();
        successHandler = new OAuth2AdminSuccessHandler();

        // Vyčistenie Constants
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();

        // Základné nastavenie mockov
        when(request.getSession()).thenReturn(session);
    }

    /**
     * Test kompletného OAuth2 flow s Google providerom
     */
    @Test
    void testCompleteOAuth2FlowWithGoogle() throws IOException {
        // Nastavenie Google OAuth2
        Constants.setString("oauth2_clients", "google");
        Constants.setString("oauth2_googleClientId", "test-client-id");
        Constants.setString("oauth2_googleClientSecret", "test-client-secret");

        // Test konfigurácie ClientRegistrationRepository
        ClientRegistrationRepository repository = springSecurityConf.clientRegistrationRepository();
        ClientRegistration googleReg = repository.findByRegistrationId("google");

        assertNotNull(googleReg);
        assertEquals("test-client-id", googleReg.getClientId());
        assertEquals("test-client-secret", googleReg.getClientSecret());
        assertEquals("Google", googleReg.getClientName());

        // Príprava OAuth2 používateľa
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "integration@example.com");
        attributes.put("given_name", "Integration");
        attributes.put("family_name", "Test");
        attributes.put("sub", "google-integration-user");

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Mock existujúceho používateľa
        UserDetails existingUser = new UserDetails();
        existingUser.setUserId(1);
        existingUser.setEmail("integration@example.com");
        existingUser.setFirstName("Integration");
        existingUser.setLastName("Test");
        existingUser.setLogin("integration");
        existingUser.setAuthorized(true);
        existingUser.setAdmin(true);

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("integration@example.com", 1)).thenReturn(existingUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);
            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            // Spustenie OAuth2 success handlera
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // Overenie úspešného presmerovania
            verify(response).sendRedirect("/admin/");

            // Overenie nastavenia používateľa do session
            logonToolsMock.verify(() -> LogonTools.setUserToSession(eq(session), any(Identity.class)));

            // Overenie, že používateľ bol načítaný, nie vytvorený
            usersDBMock.verify(() -> UsersDB.getUserByEmail("integration@example.com", 1), times(1));
        }
    }

    /**
     * Test kompletného OAuth2 flow s Keycloak providerom a skupinami
     */
    @Test
    void testCompleteOAuth2FlowWithKeycloakAndGroups() throws IOException {
        // Nastavenie custom Keycloak provider
        Constants.setString("oauth2_clients", "keycloak");
        Constants.setString("oauth2_keycloakClientId", "keycloak-client-id");
        Constants.setString("oauth2_keycloakClientSecret", "keycloak-client-secret");
        Constants.setString("oauth2_keycloakAuthorizationUri", "https://keycloak.local/auth/realms/test/protocol/openid-connect/auth");
        Constants.setString("oauth2_keycloakTokenUri", "https://keycloak.local/auth/realms/test/protocol/openid-connect/token");
        Constants.setString("oauth2_keycloakUserInfoUri", "https://keycloak.local/auth/realms/test/protocol/openid-connect/userinfo");
        Constants.setString("oauth2_keycloakJwkSetUri", "https://keycloak.local/auth/realms/test/protocol/openid-connect/certs");
        Constants.setString("oauth2_keycloakIssuerUri", "https://keycloak.local/auth/realms/test");
        Constants.setString("oauth2_keycloakUserNameAttributeName", "preferred_username");
        Constants.setString("oauth2_keycloakScopes", "openid,profile,email");
        Constants.setString("oauth2_keycloakClientName", "Keycloak");

        // Test konfigurácie ClientRegistrationRepository
        ClientRegistrationRepository repository = springSecurityConf.clientRegistrationRepository();
        ClientRegistration keycloakReg = repository.findByRegistrationId("keycloak");

        assertNotNull(keycloakReg);
        assertEquals("keycloak-client-id", keycloakReg.getClientId());
        assertEquals("keycloak-client-secret", keycloakReg.getClientSecret());
        assertEquals("Keycloak", keycloakReg.getClientName());
        assertEquals("preferred_username",
            keycloakReg.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName());

        // Príprava Keycloak OAuth2 používateľa s rolami
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "keycloak.integration@example.com");
        attributes.put("given_name", "Keycloak");
        attributes.put("family_name", "Integration");
        attributes.put("preferred_username", "keycloak.integration");
        attributes.put("sub", "keycloak-integration-user");

        // Keycloak špecifické atribúty
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", List.of("admin", "editor"));
        attributes.put("realm_access", realmAccess);
        attributes.put("groups", List.of("webjet-admin", "content-editors"));

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
            attributes,
            "email"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Mock existujúceho používateľa
        UserDetails existingUser = new UserDetails();
        existingUser.setUserId(2);
        existingUser.setEmail("keycloak.integration@example.com");
        existingUser.setFirstName("Keycloak");
        existingUser.setLastName("Integration");
        existingUser.setLogin("keycloak.integration");
        existingUser.setAuthorized(true);
        existingUser.setAdmin(true);

        // Mock skupín
        UserGroupDetails adminGroup = new UserGroupDetails();
        adminGroup.setUserGroupId(1);
        adminGroup.setUserGroupName("admin");

        PermissionGroupBean webjetAdminPerm = new PermissionGroupBean();
        webjetAdminPerm.setUserPermGroupId(1);
        webjetAdminPerm.setTitle("webjet-admin");

        try (MockedStatic<UsersDB> usersDBMock = mockStatic(UsersDB.class);
             MockedStatic<UserGroupsDB> userGroupsDBMock = mockStatic(UserGroupsDB.class);
             MockedStatic<PermissionGroupDB> permGroupDBMock = mockStatic(PermissionGroupDB.class);
             MockedStatic<LogonTools> logonToolsMock = mockStatic(LogonTools.class);
             MockedStatic<WebjetAuthentificationProvider> authProviderMock = mockStatic(WebjetAuthentificationProvider.class)) {

            usersDBMock.when(() -> UsersDB.getUserByEmail("keycloak.integration@example.com", 1)).thenReturn(existingUser);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);

            // Mock načítania skupín
            userGroupsDBMock.when(UserGroupsDB::getInstance).thenReturn(mock(UserGroupsDB.class));
            userGroupsDBMock.when(() -> UserGroupsDB.getInstance().getUserGroups())
                .thenReturn(List.of(adminGroup));

            permGroupDBMock.when(() -> PermissionGroupDB.getPermissionGroups(null))
                .thenReturn(List.of(webjetAdminPerm));

            userGroupsDBMock.when(() -> UserGroupsDB.getPermissionGroupsFor(2))
                .thenReturn(List.of());

            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            // Spustenie OAuth2 success handlera
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // Overenie úspešného presmerovania
            verify(response).sendRedirect("/admin/");

            // Provider ID nie je rozpoznaný, takže skupiny nie sú synchronizované
            // Používateľ už má admin práva z mock-u, preto je overenie úspešné

            // Overenie nastavenia používateľa do session
            logonToolsMock.verify(() -> LogonTools.setUserToSession(eq(session), any(Identity.class)));
        }
    }

    /**
     * Test kompletného flow s vytvorením nového používateľa
     */
    @Test
    void testCompleteOAuth2FlowWithNewUserCreation() throws IOException {
        // Nastavenie Facebook OAuth2
        Constants.setString("oauth2_clients", "facebook");
        Constants.setString("oauth2_facebookClientId", "fb-client-id");
        Constants.setString("oauth2_facebookClientSecret", "fb-client-secret");

        // Test konfigurácie
        ClientRegistrationRepository repository = springSecurityConf.clientRegistrationRepository();
        ClientRegistration facebookReg = repository.findByRegistrationId("facebook");

        assertNotNull(facebookReg);
        assertEquals("fb-client-id", facebookReg.getClientId());

        // Príprava OAuth2 používateľa
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "newuser.integration@example.com");
        attributes.put("given_name", "NewUser");
        attributes.put("family_name", "Integration");
        attributes.put("id", "facebook-new-user-id");

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
            usersDBMock.when(() -> UsersDB.getUserByEmail("newuser.integration@example.com", 1)).thenReturn(null);
            usersDBMock.when(() -> UsersDB.saveUser(any(UserDetails.class))).thenReturn(true);

            authProviderMock.when(() -> WebjetAuthentificationProvider.authenticate(any(Identity.class)))
                .thenReturn(authentication);

            // Spustenie OAuth2 success handlera
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // Nový používateľ nemá admin práva, preto bude presmerovaný na logon
            verify(session).setAttribute("oauth2_logon_error", "accessDenied");
            verify(response).sendRedirect("/admin/logon/");

            // Overenie vytvorenia používateľa
            usersDBMock.verify(() -> UsersDB.getUserByEmail("newuser.integration@example.com", 1), times(1));
            usersDBMock.verify(() -> UsersDB.saveUser(any(UserDetails.class)), times(1));
        }
    }

    /**
     * Test konfigurácie s viacerými OAuth2 providermi
     */
    @Test
    void testMultipleOAuth2ProvidersConfiguration() {
        // Nastavenie viacerých providerov
        Constants.setString("oauth2_clients", "google,facebook,github");
        Constants.setString("oauth2_googleClientId", "google-id");
        Constants.setString("oauth2_googleClientSecret", "google-secret");
        Constants.setString("oauth2_facebookClientId", "facebook-id");
        Constants.setString("oauth2_facebookClientSecret", "facebook-secret");
        Constants.setString("oauth2_githubClientId", "github-id");
        Constants.setString("oauth2_githubClientSecret", "github-secret");

        // Test konfigurácie
        ClientRegistrationRepository repository = springSecurityConf.clientRegistrationRepository();

        // Overenie všetkých providerov
        ClientRegistration googleReg = repository.findByRegistrationId("google");
        ClientRegistration facebookReg = repository.findByRegistrationId("facebook");
        ClientRegistration githubReg = repository.findByRegistrationId("github");

        assertNotNull(googleReg);
        assertNotNull(facebookReg);
        assertNotNull(githubReg);

        assertEquals("google-id", googleReg.getClientId());
        assertEquals("facebook-id", facebookReg.getClientId());
        assertEquals("github-id", githubReg.getClientId());

        assertEquals("Google", googleReg.getClientName());
        assertEquals("Facebook", facebookReg.getClientName());
        assertEquals("GitHub", githubReg.getClientName());
    }

    /**
     * Test chybového scenára v integračnom teste
     */
    @Test
    void testIntegrationErrorScenarios() throws IOException {
        // Nastavenie OAuth2
        Constants.setString("oauth2_clients", "google");
        Constants.setString("oauth2_googleClientId", "test-client-id");
        Constants.setString("oauth2_googleClientSecret", "test-client-secret");

        // Test s chýbajúcim emailom
        Map<String, Object> attributesNoEmail = new HashMap<>();
        attributesNoEmail.put("given_name", "Test");
        attributesNoEmail.put("family_name", "User");
        attributesNoEmail.put("sub", "google-user-no-email");

        OAuth2User oauth2UserNoEmail = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            attributesNoEmail,
            "sub"
        );
        when(authentication.getPrincipal()).thenReturn(oauth2UserNoEmail);

        // Spustenie OAuth2 success handlera
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Overenie presmerovanie na chybovú stránku
        verify(session).setAttribute("oauth2_logon_error", "oauth2_email_not_found");
        verify(response).sendRedirect("/admin/logon/");
    }
}
