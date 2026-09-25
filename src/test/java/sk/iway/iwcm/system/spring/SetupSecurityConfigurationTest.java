package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class SetupSecurityConfigurationTest {

    private static final String SETUP_TOKEN = "0123456789abcdef";
    private static final String SETUP_URL = "/wjerrorpages/setup/probe";
    private static final String SETUP_FINISH_URL = "/wjerrorpages/setup/finish";

    @Test
    void missingOrShortSetupTokenPreventsSecurityContextStartup() {
        assertTokenConfigurationFailure(null);
        assertTokenConfigurationFailure("too-short");
        assertTokenConfigurationFailure("x".repeat(WebjetSetupProperties.MINIMUM_TOKEN_LENGTH - 1));
        assertTokenConfigurationFailure(" ".repeat(WebjetSetupProperties.MINIMUM_TOKEN_LENGTH));
    }

    @Test
    void setupLoginProtectsRemoteSetupAndDeniesUnrelatedUrls() throws Exception {
        try (AnnotationConfigWebApplicationContext applicationContext = createContext(SETUP_TOKEN)) {
            assertTrue(applicationContext.containsBean("springSecurityFilterChain"));
            assertTrue(applicationContext.containsBean("setupSecurityFilterChainRegistration"));
            DelegatingFilterProxyRegistrationBean securityRegistration = applicationContext.getBean(
                "setupSecurityFilterChainRegistration", DelegatingFilterProxyRegistrationBean.class
            );
            assertEquals(Ordered.HIGHEST_PRECEDENCE + 5, securityRegistration.getOrder());
            assertEquals(EnumSet.allOf(DispatcherType.class), securityRegistration.determineDispatcherTypes());

            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();

            mockMvc.perform(get(SETUP_URL).header("Host", "remote.example"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(unauthenticated());

            mockMvc.perform(formLogin().user("setup").password("incorrect-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());

            MvcResult login = mockMvc.perform(formLogin().user("setup").password(SETUP_TOKEN))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/wjerrorpages/setup/setup"))
                .andExpect(authenticated().withUsername("setup").withRoles("SETUP"))
                .andReturn();
            MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

            mockMvc.perform(get(SETUP_URL).session(session).header("Host", "remote.example"))
                .andExpect(status().isOk())
                .andExpect(content().string("setup"));

            mockMvc.perform(get("/unrelated").session(session))
                .andExpect(status().isForbidden());
        }
    }

    private void assertTokenConfigurationFailure(String token) {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> createContext(token));
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertTrue(cause instanceof IllegalStateException);
        assertTrue(cause.getMessage().contains(WebjetSetupProperties.TOKEN_PROPERTY));
    }

    @Test
    void setupPostRequiresCsrf() throws Exception {
        try (AnnotationConfigWebApplicationContext applicationContext = createContext(SETUP_TOKEN)) {
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();

            mockMvc.perform(post(SETUP_URL).with(user("setup").roles("SETUP")))
                .andExpect(status().isForbidden());

            mockMvc.perform(post(SETUP_URL).with(user("setup").roles("SETUP")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("setup"));
        }
    }

    @Test
    void completingSetupCanInvalidateTheAuthenticatedSession() throws Exception {
        try (AnnotationConfigWebApplicationContext applicationContext = createContext(SETUP_TOKEN)) {
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();

            MvcResult login = mockMvc.perform(formLogin().user("setup").password(SETUP_TOKEN))
                .andExpect(authenticated().withUsername("setup").withRoles("SETUP"))
                .andReturn();
            MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

            mockMvc.perform(post(SETUP_FINISH_URL).session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("finished"));

            assertTrue(session.isInvalid());
            mockMvc.perform(get(SETUP_URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        }
    }

    private AnnotationConfigWebApplicationContext createContext(String token) {
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.setServletContext(new MockServletContext());
        if (token != null) {
            TestPropertyValues.of(WebjetSetupProperties.TOKEN_PROPERTY + "=" + token)
                .applyTo(applicationContext);
        }
        applicationContext.register(TestConfiguration.class);
        try {
            applicationContext.refresh();
            return applicationContext;
        } catch (RuntimeException ex) {
            applicationContext.close();
            throw ex;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({
        SetupApplicationConfiguration.SetupSecurityConfiguration.class,
        SetupProbeController.class
    })
    static class TestConfiguration {
    }

    @RestController
    static class SetupProbeController {

        @GetMapping({SETUP_URL, "/unrelated"})
        String getProbe() {
            return "setup";
        }

        @PostMapping(SETUP_URL)
        String postProbe() {
            return "setup";
        }

        @PostMapping(SETUP_FINISH_URL)
        String finishSetup(HttpServletRequest request) {
            request.getSession(false).invalidate();
            return "finished";
        }
    }
}
