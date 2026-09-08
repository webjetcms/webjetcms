package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
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
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        String originalInstallName = Constants.getInstallName();
        try {
            Constants.setInstallName(TEST_INSTALL_NAME);
            applicationContext.setServletContext(new MockServletContext());
            applicationContext.register(TestConfiguration.class);
            applicationContext.refresh();
            return applicationContext;
        } finally {
            Constants.setInstallName(originalInstallName);
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
    @Import(SecurityProbeController.class)
    static class TestConfiguration {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return new TestSpringSecurityConf().filterChain(http, null, null, null);
        }
    }

    static class TestSpringSecurityConf extends SpringSecurityConf {

        @Override
        protected void configureSecurity(HttpSecurity http, String className) {
            try {
                if (BaseSpringConfig.class.getName().equals(className)) {
                    new BaseSpringConfig().configureSecurity(http);
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
