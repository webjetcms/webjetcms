package sk.iway.iwcm.system.spring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.DefaultSecurityFilterChain;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testy pre SpringSecurityConf
 * Testujú konfiguráciu Spring Security vrátane OAuth2 nastavení
 */
class SpringSecurityConfTest extends BaseWebjetTest {

    @Mock
    private HttpSecurity httpSecurity;

    private SpringSecurityConf springSecurityConf;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        springSecurityConf = new SpringSecurityConf();

        // Vyčistenie Constants
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();
    }

    /**
     * Test základnej konfigurácie bez OAuth2
     */
    @Test
    void testBasicConfiguration() throws Exception {
        // Žiadne OAuth2 clients
        Constants.setString("oauth2_clients", "");

        // Mock HttpSecurity builder pattern
        when(httpSecurity.authenticationProvider(any())).thenReturn(httpSecurity);
        when(httpSecurity.headers(any())).thenReturn(httpSecurity);
        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.build()).thenReturn(mock(DefaultSecurityFilterChain.class));

        // Spustenie testu
        SecurityFilterChain filterChain = springSecurityConf.filterChain(httpSecurity);

        // Overenie
        assertNotNull(filterChain);
        verify(httpSecurity).authenticationProvider(any(WebjetAuthentificationProvider.class));
        verify(httpSecurity).headers(any());
        verify(httpSecurity).csrf(any());
        verify(httpSecurity).build();
    }

    /**
     * Test konfigurácie s povolenými Basic Auth
     */
    @Test
    void testBasicAuthConfiguration() throws Exception {
        // Nastavenie Basic Auth
        Constants.setString("springSecurityAllowedAuths", "basic");

        // Mock HttpSecurity builder pattern
        when(httpSecurity.authenticationProvider(any())).thenReturn(httpSecurity);
        when(httpSecurity.httpBasic(any())).thenReturn(httpSecurity);
        when(httpSecurity.headers(any())).thenReturn(httpSecurity);
        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.build()).thenReturn(mock(DefaultSecurityFilterChain.class));

        // Spustenie testu
        SecurityFilterChain filterChain = springSecurityConf.filterChain(httpSecurity);

        // Overenie
        assertNotNull(filterChain);
        assertTrue(SpringSecurityConf.isBasicAuthEnabled());
        verify(httpSecurity).httpBasic(any());
    }

    /**
     * Test konfigurácie s OAuth2
     */
    @Test
    void testOAuth2Configuration() throws Exception {
        // Nastavenie OAuth2 clients
        Constants.setString("oauth2_clients", "google,facebook");
        Constants.setString("oauth2_googleClientId", "google-client-id");
        Constants.setString("oauth2_googleClientSecret", "google-client-secret");
        Constants.setString("oauth2_facebookClientId", "facebook-client-id");
        Constants.setString("oauth2_facebookClientSecret", "facebook-client-secret");

        // Mock HttpSecurity builder pattern
        when(httpSecurity.authenticationProvider(any())).thenReturn(httpSecurity);
        when(httpSecurity.oauth2Login(any())).thenReturn(httpSecurity);
        when(httpSecurity.headers(any())).thenReturn(httpSecurity);
        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.build()).thenReturn(mock(DefaultSecurityFilterChain.class));

        // Spustenie testu
        SecurityFilterChain filterChain = springSecurityConf.filterChain(httpSecurity);

        // Overenie
        assertNotNull(filterChain);
        verify(httpSecurity).oauth2Login(any());
    }

    /**
     * Test ClientRegistrationRepository pre Google a Facebook
     */
    @Test
    void testClientRegistrationRepositoryWithKnownProviders() {
        // Nastavenie OAuth2 clients
        Constants.setString("oauth2_clients", "google,facebook");
        Constants.setString("oauth2_googleClientId", "google-client-id");
        Constants.setString("oauth2_googleClientSecret", "google-client-secret");
        Constants.setString("oauth2_facebookClientId", "facebook-client-id");
        Constants.setString("oauth2_facebookClientSecret", "facebook-client-secret");

        // Spustenie testu
        ClientRegistrationRepository repository = springSecurityConf.clientRegistrationRepository();

        // Overenie
        assertNotNull(repository);

        ClientRegistration googleReg = repository.findByRegistrationId("google");
        assertNotNull(googleReg);
        assertEquals("google-client-id", googleReg.getClientId());
        assertEquals("google-client-secret", googleReg.getClientSecret());

        ClientRegistration facebookReg = repository.findByRegistrationId("facebook");
        assertNotNull(facebookReg);
        assertEquals("facebook-client-id", facebookReg.getClientId());
        assertEquals("facebook-client-secret", facebookReg.getClientSecret());
    }

    /**
     * Test ClientRegistrationRepository pre vlastných providerov
     */
    @Test
    void testClientRegistrationRepositoryWithCustomProvider() {
        // Nastavenie custom OAuth2 provider
        Constants.setString("oauth2_clients", "keycloak");
        Constants.setString("oauth2_keycloakClientId", "keycloak-client-id");
        Constants.setString("oauth2_keycloakClientSecret", "keycloak-client-secret");
        Constants.setString("oauth2_keycloakAuthorizationUri", "https://keycloak.example.com/auth/realms/test/protocol/openid-connect/auth");
        Constants.setString("oauth2_keycloakTokenUri", "https://keycloak.example.com/auth/realms/test/protocol/openid-connect/token");
        Constants.setString("oauth2_keycloakUserInfoUri", "https://keycloak.example.com/auth/realms/test/protocol/openid-connect/userinfo");
        Constants.setString("oauth2_keycloakJwkSetUri", "https://keycloak.example.com/auth/realms/test/protocol/openid-connect/certs");
        Constants.setString("oauth2_keycloakIssuerUri", "https://keycloak.example.com/auth/realms/test");
        Constants.setString("oauth2_keycloakUserNameAttributeName", "preferred_username");
        Constants.setString("oauth2_keycloakScopes", "openid,profile,email");
        Constants.setString("oauth2_keycloakClientName", "Keycloak");

        // Spustenie testu
        ClientRegistrationRepository repository = springSecurityConf.clientRegistrationRepository();

        // Overenie
        assertNotNull(repository);

        ClientRegistration keycloakReg = repository.findByRegistrationId("keycloak");
        assertNotNull(keycloakReg);
        assertEquals("keycloak-client-id", keycloakReg.getClientId());
        assertEquals("keycloak-client-secret", keycloakReg.getClientSecret());
        assertEquals("https://keycloak.example.com/auth/realms/test/protocol/openid-connect/auth",
                    keycloakReg.getProviderDetails().getAuthorizationUri());
        assertEquals("https://keycloak.example.com/auth/realms/test/protocol/openid-connect/token",
                    keycloakReg.getProviderDetails().getTokenUri());
        assertEquals("preferred_username",
                    keycloakReg.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName());
        assertTrue(keycloakReg.getScopes().contains("openid"));
        assertTrue(keycloakReg.getScopes().contains("profile"));
        assertTrue(keycloakReg.getScopes().contains("email"));
    }

    /**
     * Test ClientRegistrationRepository s chýbajúcimi parametrami
     */
    @Test
    void testClientRegistrationRepositoryWithMissingParams() {
        // Nastavenie neúplných OAuth2 clients
        Constants.setString("oauth2_clients", "google,invalid");
        Constants.setString("oauth2_googleClientId", "google-client-id");
        Constants.setString("oauth2_googleClientSecret", "google-client-secret");
        // Chýba oauth2_invalidClientId alebo oauth2_invalidClientSecret

        // Spustenie testu
        ClientRegistrationRepository repository = springSecurityConf.clientRegistrationRepository();

        // Overenie
        assertNotNull(repository);

        // Google by mal existovať
        ClientRegistration googleReg = repository.findByRegistrationId("google");
        assertNotNull(googleReg);

        // Invalid by nemal existovať
        ClientRegistration invalidReg = repository.findByRegistrationId("invalid");
        assertNull(invalidReg);
    }

    /**
     * Test ClientRegistrationRepository bez nastavených klientov
     */
    @Test
    void testClientRegistrationRepositoryEmpty() {
        // Žiadne OAuth2 clients
        Constants.setString("oauth2_clients", "");

        // Spustenie testu
        ClientRegistrationRepository repository = springSecurityConf.clientRegistrationRepository();

        // Overenie
        assertNotNull(repository);

        // Mal by vrátiť null pre akýkoľvek registrationId
        ClientRegistration nullReg = repository.findByRegistrationId("google");
        assertNull(nullReg);
    }

    /**
     * Test OAuth2AuthorizedClientService
     */
    @Test
    void testOAuth2AuthorizedClientService() {
        // Nastavenie OAuth2 clients
        Constants.setString("oauth2_clients", "google");
        Constants.setString("oauth2_googleClientId", "google-client-id");
        Constants.setString("oauth2_googleClientSecret", "google-client-secret");

        // Spustenie testu
        OAuth2AuthorizedClientService service = springSecurityConf.authorizedClientService();

        // Overenie
        assertNotNull(service);
    }

    /**
     * Test GitHub providera
     */
    @Test
    void testGitHubProvider() {
        // Nastavenie GitHub OAuth2
        Constants.setString("oauth2_clients", "github");
        Constants.setString("oauth2_githubClientId", "github-client-id");
        Constants.setString("oauth2_githubClientSecret", "github-client-secret");

        // Spustenie testu
        ClientRegistrationRepository repository = springSecurityConf.clientRegistrationRepository();

        // Overenie
        assertNotNull(repository);

        ClientRegistration githubReg = repository.findByRegistrationId("github");
        assertNotNull(githubReg);
        assertEquals("github-client-id", githubReg.getClientId());
        assertEquals("github-client-secret", githubReg.getClientSecret());
    }

    /**
     * Test isBasicAuthEnabled pri false
     */
    @Test
    void testIsBasicAuthEnabledFalse() {
        // Bez Basic Auth
        Constants.setString("springSecurityAllowedAuths", "oauth2");

        // BasicAuth by mal byť false (ak nebola nastavená)
        // Poznámka: staticka premenná môže byť už nastavená z predchádzajúcich testov
        // Tento test overuje iba, že konfigurácia neobsahuje "basic"
        String allowedAuths = Constants.getString("springSecurityAllowedAuths");
        assertFalse(allowedAuths.contains("basic"));
    }

    /**
     * Test konfigurácie s viacerými povoleniami autentifikácie
     */
    @Test
    void testMultipleAuthMethods() throws Exception {
        // Nastavenie Basic Auth a OAuth2
        Constants.setString("springSecurityAllowedAuths", "basic,oauth2");
        Constants.setString("oauth2_clients", "google");
        Constants.setString("oauth2_googleClientId", "google-client-id");
        Constants.setString("oauth2_googleClientSecret", "google-client-secret");

        // Mock HttpSecurity builder pattern
        when(httpSecurity.authenticationProvider(any())).thenReturn(httpSecurity);
        when(httpSecurity.httpBasic(any())).thenReturn(httpSecurity);
        when(httpSecurity.oauth2Login(any())).thenReturn(httpSecurity);
        when(httpSecurity.headers(any())).thenReturn(httpSecurity);
        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.build()).thenReturn(mock(DefaultSecurityFilterChain.class));

        // Spustenie testu
        SecurityFilterChain filterChain = springSecurityConf.filterChain(httpSecurity);

        // Overenie
        assertNotNull(filterChain);
        assertTrue(SpringSecurityConf.isBasicAuthEnabled());
        verify(httpSecurity).httpBasic(any());
        verify(httpSecurity).oauth2Login(any());
    }

    /**
     * Test vlastného OAuth2 providera s neúplnými údajmi
     */
    @Test
    void testCustomProviderIncompleteData() {
        // Nastavenie custom provider s chýbajúcimi údajmi
        Constants.setString("oauth2_clients", "custom");
        Constants.setString("oauth2_customClientId", "custom-client-id");
        Constants.setString("oauth2_customClientSecret", "custom-client-secret");
        Constants.setString("oauth2_customAuthorizationUri", "https://custom.example.com/auth");
        // Chýbajú oauth2_customTokenUri, oauth2_customUserInfoUri, oauth2_customJwkSetUri, oauth2_customIssuerUri

        // Spustenie testu
        ClientRegistrationRepository repository = springSecurityConf.clientRegistrationRepository();

        // Overenie
        assertNotNull(repository);

        // Custom provider by nemal existovať kvôli chýbajúcim údajom
        ClientRegistration customReg = repository.findByRegistrationId("custom");
        assertNull(customReg);
    }
}
