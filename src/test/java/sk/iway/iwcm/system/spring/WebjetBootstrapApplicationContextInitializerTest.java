package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

import sk.iway.iwcm.setup.SetupModeCondition;

class WebjetBootstrapApplicationContextInitializerTest {

    private static final String CUSTOM_AUTO_CONFIGURATION_EXCLUSION = "com.example.CustomAutoConfiguration";

    @Test
    void selectsSetupModeBeforeConfigurationParsing() {
        assertModeSelectedBeforeConfigurationParsing(WebjetBootstrapMode.SETUP);
    }

    @Test
    void selectsProductionModeBeforeConfigurationParsing() {
        assertModeSelectedBeforeConfigurationParsing(WebjetBootstrapMode.PRODUCTION);
    }

    @Test
    void setupModeDoesNotEnableDefaultBootSecurity() {
        WebjetBootstrapModeDetector modeDetector = mock(WebjetBootstrapModeDetector.class);
        WebjetInitializationActions initializationActions = mock(WebjetInitializationActions.class);
        when(initializationActions.initialize(any(ServletContext.class)))
            .thenReturn(false);

        new WebApplicationContextRunner()
            .withInitializer(new WebjetBootstrapApplicationContextInitializer(
                null, modeDetector, initializationActions
            ))
            .withUserConfiguration(SecurityAutoConfigurationProbe.class)
            .run(applicationContext -> {
                assertTrue(applicationContext.getBeansOfType(SecurityFilterChain.class).isEmpty());
                assertTrue(applicationContext.getBeansOfType(UserDetailsService.class).isEmpty());
                assertFalse(applicationContext.containsBean("securityFilterChainRegistration"));
            });

        verifyNoInteractions(modeDetector);
        verify(initializationActions).initialize(any(ServletContext.class));
    }

    private void assertModeSelectedBeforeConfigurationParsing(WebjetBootstrapMode expectedMode) {
        WebjetBootstrapModeDetector modeDetector = mock(WebjetBootstrapModeDetector.class);
        WebjetInitializationActions initializationActions = mock(WebjetInitializationActions.class);
        WebjetBootstrapSpringConfiguration springConfiguration = expectedMode == WebjetBootstrapMode.PRODUCTION
            ? new WebjetBootstrapSpringConfiguration(
                "dynamicinstall", "dynamiclog", "com.example.webjetadditional"
            )
            : WebjetBootstrapSpringConfiguration.empty();

        try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {
            TestPropertyValues.of(
                "spring.autoconfigure.exclude=" + CUSTOM_AUTO_CONFIGURATION_EXCLUSION
            ).applyTo(applicationContext);
            when(modeDetector.detect(applicationContext.getEnvironment())).thenReturn(
                new WebjetBootstrapModeDetector.Detection(expectedMode, springConfiguration)
            );

            WebjetBootstrapApplicationContextInitializer initializer =
                new WebjetBootstrapApplicationContextInitializer(null, modeDetector, initializationActions);
            initializer.initialize(applicationContext);

            assertEquals(expectedMode.getPropertyValue(),
                applicationContext.getEnvironment().getProperty(WebjetBootstrapMode.PROPERTY_NAME));
            assertEquals(springConfiguration.installName(), applicationContext.getEnvironment()
                .getProperty(WebjetBootstrapSpringConfiguration.INSTALL_NAME_PROPERTY));
            assertEquals(springConfiguration.logInstallName(), applicationContext.getEnvironment()
                .getProperty(WebjetBootstrapSpringConfiguration.LOG_INSTALL_NAME_PROPERTY));
            assertEquals(springConfiguration.springAddPackages(), applicationContext.getEnvironment()
                .getProperty(WebjetBootstrapSpringConfiguration.ADD_PACKAGES_PROPERTY));
            assertSetupSecurityAutoConfigurationsAreExcluded(applicationContext, expectedMode);

            WebjetBootstrapState bootstrapState = (WebjetBootstrapState) applicationContext
                .getBeanFactory()
                .getSingleton(WebjetBootstrapState.BEAN_NAME);
            assertEquals(expectedMode, bootstrapState.getMode());
            assertFalse(bootstrapState.isCoreInitializationAttempted());

            new AnnotatedBeanDefinitionReader(applicationContext).register(
                SetupModeProbeConfiguration.class,
                ProductionModeProbeConfiguration.class,
                LegacySetupModeProbeConfiguration.class
            );
            applicationContext.refresh();

            assertEquals(expectedMode == WebjetBootstrapMode.SETUP,
                applicationContext.containsBean("setupModeProbe"));
            assertEquals(expectedMode == WebjetBootstrapMode.PRODUCTION,
                applicationContext.containsBean("productionModeProbe"));
            assertEquals(expectedMode == WebjetBootstrapMode.SETUP,
                applicationContext.containsBean("legacySetupModeProbe"));

            verify(modeDetector).detect(applicationContext.getEnvironment());
            verifyNoInteractions(initializationActions);
        }
    }

    private void assertSetupSecurityAutoConfigurationsAreExcluded(GenericApplicationContext applicationContext,
            WebjetBootstrapMode expectedMode) {
        String exclusions = applicationContext.getEnvironment().getProperty("spring.autoconfigure.exclude", "");
        assertTrue(exclusions.contains(CUSTOM_AUTO_CONFIGURATION_EXCLUSION));
        if (expectedMode == WebjetBootstrapMode.SETUP) {
            assertTrue(exclusions.contains(ServletWebSecurityAutoConfiguration.class.getName()));
            assertTrue(exclusions.contains(SecurityFilterAutoConfiguration.class.getName()));
            assertTrue(exclusions.contains(UserDetailsServiceAutoConfiguration.class.getName()));
        } else {
            assertEquals(CUSTOM_AUTO_CONFIGURATION_EXCLUSION, exclusions);
        }
    }

    @ConditionalOnProperty(
        name = WebjetBootstrapMode.PROPERTY_NAME,
        havingValue = WebjetBootstrapMode.SETUP_VALUE
    )
    static class SetupModeProbeConfiguration {

        @Bean
        String setupModeProbe() {
            return WebjetBootstrapMode.SETUP_VALUE;
        }
    }

    @ConditionalOnProperty(
        name = WebjetBootstrapMode.PROPERTY_NAME,
        havingValue = WebjetBootstrapMode.PRODUCTION_VALUE
    )
    static class ProductionModeProbeConfiguration {

        @Bean
        String productionModeProbe() {
            return WebjetBootstrapMode.PRODUCTION_VALUE;
        }
    }

    @SuppressWarnings("deprecation")
    @Conditional(SetupModeCondition.class)
    static class LegacySetupModeProbeConfiguration {

        @Bean
        String legacySetupModeProbe() {
            return WebjetBootstrapMode.SETUP_VALUE;
        }
    }

    @EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
    static class SecurityAutoConfigurationProbe {
    }
}
