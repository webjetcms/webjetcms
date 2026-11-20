package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;

/** Verifies authorization failures with Basic authentication enabled, including browser fetch requests. */
class GlobalExceptionHandlerTest {

    private static final String REST_URL = "/admin/rest/sessions/administrators";
    private static final String WEB_URL = "/permission-protected-page";
    private AnnotationConfigWebApplicationContext applicationContext;
    private MockedStatic<SpringSecurityConf> securityConfiguration;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        securityConfiguration = mockStatic(SpringSecurityConf.class);
        securityConfiguration.when(SpringSecurityConf::isBasicAuthEnabled).thenReturn(true);
        applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.setServletContext(new MockServletContext());
        applicationContext.register(TestConfiguration.class);
        applicationContext.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).apply(springSecurity()).build();
    }

    @AfterEach
    void tearDown() {
        applicationContext.close();
        securityConfiguration.close();
        SecurityContextHolder.clearContext();
    }

    /** Missing permissions must not ask an already authenticated browser to log in again. */
    @ParameterizedTest
    @ValueSource(strings = {"*/*", "application/json"})
    void authenticatedRestDenialReturnsForbiddenWithoutChallenge(String accept) throws Exception {
        mockMvc.perform(get(REST_URL).accept(accept).with(user("administrator").roles("Group_admin")))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
            .andExpect(content().string("{\"error\":\"Access Denied\",\"status\":403}"));
    }

    @Test
    void basicAuthenticatedUserWithoutPermissionReturnsForbidden() throws Exception {
        mockMvc.perform(get(REST_URL).with(httpBasic("api-user", "test-password")))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void authenticatedWebDenialForwardsToForbiddenPageWithoutChallenge() throws Exception {
        mockMvc.perform(get(WEB_URL).accept(MediaType.TEXT_HTML).with(user("administrator")))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
            .andExpect(forwardedUrl("/403.jsp"));
    }

    @Test
    void authorizedUserStillReceivesProtectedData() throws Exception {
        mockMvc.perform(get(REST_URL).with(user("administrator").authorities(
                () -> "welcomeShowLoggedAdmins")))
            .andExpect(status().isOk())
            .andExpect(content().string("protected data"));
    }

    @Test
    void anonymousRestDenialStillChallengesWhenBasicIsEnabled() throws Exception {
        mockMvc.perform(get(REST_URL))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Secure Area\""))
            .andExpect(content().string("{\"error\":\"Authentication required\",\"status\":401}"));
    }

    @Test
    void anonymousWebDenialStillChallengesWhenBasicIsEnabled() throws Exception {
        mockMvc.perform(get(WEB_URL).accept(MediaType.TEXT_HTML))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Secure Area\""))
            .andExpect(forwardedUrl("/403.jsp"));
    }

    @Test
    void anonymousRestDenialRemainsForbiddenWhenBasicIsDisabled() throws Exception {
        securityConfiguration.when(SpringSecurityConf::isBasicAuthEnabled).thenReturn(false);
        mockMvc.perform(get(REST_URL))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    /** Legacy session identities also count as authenticated if the Spring context is unavailable. */
    @Test
    void legacySessionIdentityWithoutSpringAuthenticationDoesNotReceiveChallenge() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", REST_URL);
        request.getSession().setAttribute(Constants.USER_KEY, new Identity());
        MockHttpServletResponse response = new MockHttpServletResponse();

        new GlobalExceptionHandler().handleAccessDenied(new AccessDeniedException("Missing permission"), request, response);

        assertEquals(403, response.getStatus());
        assertNull(response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @Import({ProtectedController.class, GlobalExceptionHandler.class})
    static class TestConfiguration {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .httpBasic(basic -> {})
                .build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(User.withUsername("api-user")
                .password("{noop}test-password").roles("Group_admin").build());
        }
    }

    @RestController
    public static class ProtectedController {

        @GetMapping({REST_URL, WEB_URL})
        @PreAuthorize("hasAuthority('welcomeShowLoggedAdmins')")
        public String protectedData() {
            return "protected data";
        }
    }
}
