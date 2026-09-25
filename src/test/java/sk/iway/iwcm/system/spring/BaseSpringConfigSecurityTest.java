package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import sk.iway.iwcm.Constants;

class BaseSpringConfigSecurityTest {

    private static final String TEST_INSTALL_NAME = "securitytest";
    private static final String TEST_CUSTOMER_CLASS = "sk.iway." + TEST_INSTALL_NAME + ".SpringConfig";
    private static final String PRIVATE_URL = "/private/rest/security-probe";
    private static final String ADMIN_URL = "/admin/rest/security-probe";
    private static final String SWAGGER_URL = "/swagger-ui/security-probe";
    private static final String CUSTOMER_URL = "/customer/security-probe";
    private static final String PUBLIC_URL = "/public/security-probe";

    @Test
    void privateRestRequiresAuthentication() throws Exception {
        try (AnnotationConfigWebApplicationContext applicationContext = createContext()) {
            MockMvc mockMvc = createMockMvc(applicationContext);

            mockMvc.perform(get(PRIVATE_URL))
                .andExpect(status().isForbidden());

            mockMvc.perform(get(PRIVATE_URL).with(user("authenticated-user")))
                .andExpect(status().isOk())
                .andExpect(content().string("private"));
        }
    }

    @Test
    void adminAndSwaggerRequireGroupAdminRole() throws Exception {
        try (AnnotationConfigWebApplicationContext applicationContext = createContext()) {
            MockMvc mockMvc = createMockMvc(applicationContext);

            mockMvc.perform(get(ADMIN_URL))
                .andExpect(status().isForbidden());
            mockMvc.perform(get(ADMIN_URL).with(user("wrong-role").roles("USER")))
                .andExpect(status().isForbidden());
            mockMvc.perform(get(ADMIN_URL).with(user("admin").roles("Group_admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));

            mockMvc.perform(get(SWAGGER_URL).with(user("wrong-role").roles("USER")))
                .andExpect(status().isForbidden());
            mockMvc.perform(get(SWAGGER_URL).with(user("admin").roles("Group_admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("swagger"));
        }
    }

    @Test
    void customerRulesAreAppliedBeforePublicFallback() throws Exception {
        try (AnnotationConfigWebApplicationContext applicationContext = createContext()) {
            MockMvc mockMvc = createMockMvc(applicationContext);

            mockMvc.perform(get(CUSTOMER_URL))
                .andExpect(status().isForbidden());
            mockMvc.perform(get(CUSTOMER_URL).with(user("wrong-role").roles("USER")))
                .andExpect(status().isForbidden());
            mockMvc.perform(get(CUSTOMER_URL).with(user("customer-admin").roles("Customer_admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("customer"));
        }
    }

    @Test
    void unmatchedRequestsRemainPublic() throws Exception {
        try (AnnotationConfigWebApplicationContext applicationContext = createContext()) {
            createMockMvc(applicationContext).perform(get(PUBLIC_URL))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
        }
    }

    /** Verifies the legacy admin response for missing sessions and rejected API tokens. */
    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void adminSessionFailuresNeverChallengeForBasicAuthentication(boolean basicEnabled, boolean oauthEnabled) throws Exception {
        try (AnnotationConfigWebApplicationContext applicationContext = createContext(basicEnabled, oauthEnabled)) {
            MockMvc mockMvc = createMockMvc(applicationContext);

            mockMvc.perform(post("/admin/rest/refresher").header("X-Requested-With", "XMLHttpRequest")
                    .header("X-CSRF-Token", "expired-session-token"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.status").value(403));
            mockMvc.perform(get(ADMIN_URL).header(HttpHeaders.ACCEPT, "application/json, text/plain, */*")
                    .header("x-auth-token", "invalid-token"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.status").value(403));
            mockMvc.perform(get("/cms" + ADMIN_URL).contextPath("/cms"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.status").value(403));
            mockMvc.perform(get(ADMIN_URL).header(HttpHeaders.ACCEPT, "text/html"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
            if (!basicEnabled) {
                mockMvc.perform(get(ADMIN_URL).with(httpBasic("api-admin", "valid-password")))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                    .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
            }
            if (oauthEnabled) {
                mockMvc.perform(get(CUSTOMER_URL).header(HttpHeaders.ACCEPT, "text/html"))
                    .andExpect(status().isFound())
                    .andExpect(header().string(HttpHeaders.LOCATION, "/admin/logon/"));
            }
        }
    }

    /** Verifies that session compatibility does not disable explicit Basic API authentication. */
    @ParameterizedTest
    @CsvSource({"false", "true"})
    void explicitBasicAuthenticationStillAuthenticatesAndChallenges(boolean oauthEnabled) throws Exception {
        try (AnnotationConfigWebApplicationContext applicationContext = createContext(true, oauthEnabled)) {
            MockMvc mockMvc = createMockMvc(applicationContext);

            mockMvc.perform(get(ADMIN_URL).with(httpBasic("api-admin", "valid-password")))
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
            mockMvc.perform(get(ADMIN_URL).with(httpBasic("api-admin", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Secure Area\""))
                .andExpect(jsonPath("$.status").value(401));
            mockMvc.perform(get(ADMIN_URL).header(HttpHeaders.AUTHORIZATION, "Basic invalid!"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE));
        }
    }

    /** Verifies URL permission failures with Basic support enabled. */
    @Test
    void authenticatedAccessDenialNeverChallengesForBasicAuthentication() throws Exception {
        try (AnnotationConfigWebApplicationContext applicationContext = createContext(true)) {
            MockMvc mockMvc = createMockMvc(applicationContext);

            mockMvc.perform(get(ADMIN_URL).with(user("wrong-role").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
        }
    }

    @Test
    void customerSecurityConfigurationFailureStopsStartup() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> new SpringSecurityConf().configureSecurity(
                null, FailingSecurityConfiguration.class.getName()
            ));

        assertTrue(exception.getMessage().contains(FailingSecurityConfiguration.class.getName()));
    }

    @Test
    void missingOptionalSecurityConfigurationDoesNotStopStartup() {
        assertDoesNotThrow(() -> new SpringSecurityConf().configureSecurity(
            null, "sk.iway.missing.OptionalSecurityConfiguration"
        ));
    }

    private AnnotationConfigWebApplicationContext createContext() {
        return createContext(false);
    }

    private AnnotationConfigWebApplicationContext createContext(boolean basicEnabled) {
        return createContext(basicEnabled, false);
    }

    private AnnotationConfigWebApplicationContext createContext(boolean basicEnabled, boolean oauthEnabled) {
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        String originalInstallName = Constants.getInstallName();
        boolean originalPassKeyEnabled = Constants.getBoolean("password_passKeyEnabled");
        String originalOauthClients = Constants.getString("oauth2_clients");
        String originalAllowedAuths = Constants.getString("springSecurityAllowedAuths");
        try {
            Constants.setString("oauth2_clients", oauthEnabled ? "google" : "");
            Constants.setInstallName(TEST_INSTALL_NAME);
            Constants.setBoolean("password_passKeyEnabled", false);
            Constants.setString("springSecurityAllowedAuths", basicEnabled ? "basic" : "");
            applicationContext.setServletContext(new MockServletContext());
            applicationContext.register(TestConfiguration.class);
            applicationContext.refresh();
            return applicationContext;
        } finally {
            Constants.setString("oauth2_clients", originalOauthClients);
            Constants.setInstallName(originalInstallName);
            Constants.setBoolean("password_passKeyEnabled", originalPassKeyEnabled);
            Constants.setString("springSecurityAllowedAuths", originalAllowedAuths);
        }
    }

    private MockMvc createMockMvc(AnnotationConfigWebApplicationContext applicationContext) {
        return MockMvcBuilders.webAppContextSetup(applicationContext)
            .apply(springSecurity())
            .build();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityProbeController.class, GlobalExceptionHandler.class})
    static class TestConfiguration {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return new TestSpringSecurityConf().filterChain(http, null, null, null);
        }
    }

    static class TestSpringSecurityConf extends SpringSecurityConf {

        @Override
        public ClientRegistrationRepository clientRegistrationRepository() {
            // A local registration exercises OAuth2 entry-point selection without contacting a provider.
            return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId("test-client").clientSecret("test-secret").build());
        }


        @Override
        protected void configureSecurity(HttpSecurity http, String className) {
            try {
                if (BaseSpringConfig.class.getName().equals(className)) {
                    new BaseSpringConfig().configureSecurity(http);
                    // Exercise the real Basic filter without database authentication or login side effects.
                    http.authenticationManager(authentication -> {
                        if (!"api-admin".equals(authentication.getName()) ||
                                !"valid-password".equals(authentication.getCredentials())) {
                            throw new BadCredentialsException("Invalid test credentials");
                        }
                        return UsernamePasswordAuthenticationToken.authenticated(authentication.getPrincipal(),
                            null, AuthorityUtils.createAuthorityList("ROLE_Group_admin"));
                    });
                } else if (TEST_CUSTOMER_CLASS.equals(className)) {
                    http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(CUSTOMER_URL).hasRole("Customer_admin")
                    );
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to configure test security", ex);
            }
        }
    }

    @RestController
    static class SecurityProbeController {

        @GetMapping(PRIVATE_URL)
        String privateProbe() {
            return "private";
        }

        @GetMapping(ADMIN_URL)
        String adminProbe() {
            return "admin";
        }

        @GetMapping(SWAGGER_URL)
        String swaggerProbe() {
            return "swagger";
        }

        @GetMapping(CUSTOMER_URL)
        String customerProbe() {
            return "customer";
        }

        @GetMapping(PUBLIC_URL)
        String publicProbe() {
            return "public";
        }
    }

    static class FailingSecurityConfiguration implements ConfigurableSecurity {

        @Override
        public void configureSecurity(HttpSecurity http) {
            throw new IllegalStateException("Intentional test failure");
        }
    }
}
