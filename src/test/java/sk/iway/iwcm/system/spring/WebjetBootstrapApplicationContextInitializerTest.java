package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.HttpEncodingAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.MultipartAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletRegistrationBean;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.context.support.GenericWebApplicationContext;

import sk.iway.iwcm.Constants;
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

    @Test
    void embeddedServletInfrastructureUsesBootstrapValuesBeforeCoreInitialization() {
        WebjetBootstrapModeDetector modeDetector = mock(WebjetBootstrapModeDetector.class);
        WebjetInitializationActions initializationActions = mock(WebjetInitializationActions.class);

        try (GenericWebApplicationContext applicationContext = new GenericWebApplicationContext()) {
            TestPropertyValues.of(
                "spring.servlet.encoding.force=true",
                "spring.servlet.multipart.file-size-threshold=64KB"
            ).applyTo(applicationContext);
            WebjetBootstrapSpringConfiguration springConfiguration =
                WebjetBootstrapSpringConfiguration.fromDatabaseValues(Map.of(
                    "defaultEncoding", "windows-1250",
                    "stripes.FileUpload.MaximumPostSize", "37m"
                ), applicationContext.getEnvironment());
            when(modeDetector.detect(applicationContext.getEnvironment())).thenReturn(
                new WebjetBootstrapModeDetector.Detection(
                    WebjetBootstrapMode.PRODUCTION, springConfiguration
                )
            );

            new WebjetBootstrapApplicationContextInitializer(
                null, modeDetector, initializationActions
            ).initialize(applicationContext);
            new AnnotatedBeanDefinitionReader(applicationContext).register(
                HttpEncodingAutoConfiguration.class,
                MultipartAutoConfiguration.class,
                SpringBootStarter.ProductionServletInfrastructureConfiguration.class,
                SpringBootStarter.ProductionServletConfiguration.class
            );
            applicationContext.refresh();

            assertServletInfrastructure(applicationContext, "windows-1250", 37_000_000L);
            assertSame(applicationContext.getBean(CharacterEncodingFilter.class), applicationContext.getBean(
                "characterEncodingFilterRegistration", FilterRegistrationBean.class
            ).getFilter());
            MultipartConfigElement multipartConfig = applicationContext.getBean(MultipartConfigElement.class);
            assertSame(multipartConfig, applicationContext.getBean(
                "multipleFileUploadServletRegistration", ServletRegistrationBean.class
            ).getMultipartConfig());
            assertSame(multipartConfig, applicationContext.getBean(
                "xhrFileUploadServletRegistration", ServletRegistrationBean.class
            ).getMultipartConfig());
            assertSame(multipartConfig, applicationContext.getBean(
                "adminUploadServletRegistration", ServletRegistrationBean.class
            ).getMultipartConfig());
            WebjetBootstrapState bootstrapState = applicationContext.getBean(
                WebjetBootstrapState.BEAN_NAME, WebjetBootstrapState.class
            );
            assertFalse(bootstrapState.isCoreInitializationAttempted());
            assertSame(springConfiguration, applicationContext.getBean(
                WebjetBootstrapSpringConfiguration.BEAN_NAME,
                WebjetBootstrapSpringConfiguration.class
            ));

            verify(modeDetector).detect(applicationContext.getEnvironment());
            verifyNoInteractions(initializationActions);
        }
    }

    @Test
    void embeddedServletInfrastructureUsesBootBooleanPropertySemantics() {
        try (GenericWebApplicationContext applicationContext = new GenericWebApplicationContext()) {
            TestPropertyValues.of(
                WebjetBootstrapMode.PROPERTY_NAME + "=production",
                "spring.servlet.encoding.enabled=off",
                "spring.servlet.multipart.enabled=off"
            ).applyTo(applicationContext);
            new AnnotatedBeanDefinitionReader(applicationContext).register(
                HttpEncodingAutoConfiguration.class,
                MultipartAutoConfiguration.class,
                SpringBootStarter.ProductionServletInfrastructureConfiguration.class,
                SpringBootStarter.ProductionServletConfiguration.class
            );

            applicationContext.refresh();

            assertTrue(applicationContext.getBeansOfType(CharacterEncodingFilter.class).isEmpty());
            assertTrue(applicationContext.getBeansOfType(MultipartConfigElement.class).isEmpty());
            assertFalse(applicationContext.containsBean("characterEncodingFilterRegistration"));
            assertFalse(applicationContext.containsBean("multipleFileUploadServletRegistration"));
            assertFalse(applicationContext.containsBean("xhrFileUploadServletRegistration"));
            assertFalse(applicationContext.containsBean("adminUploadServletRegistration"));
        }
    }

    @Test
    void externalWarServletInfrastructureUsesConstantsLoadedDuringBootstrap() {
        WebjetBootstrapModeDetector modeDetector = mock(WebjetBootstrapModeDetector.class);
        WebjetInitializationActions initializationActions = mock(WebjetInitializationActions.class);
        when(initializationActions.initialize(any(ServletContext.class))).thenReturn(true);

        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(Constants::getInstallName).thenReturn("dynamicinstall");
            constants.when(Constants::getLogInstallName).thenReturn("dynamiclog");
            constants.when(() -> Constants.getString("springAddPackages")).thenReturn("");
            constants.when(() -> Constants.getString("defaultEncoding")).thenReturn("windows-1251");
            constants.when(() -> Constants.getString("stripes.FileUpload.MaximumPostSize"))
                .thenReturn("38m");

            new WebApplicationContextRunner()
                .withInitializer(new WebjetBootstrapApplicationContextInitializer(
                    null, modeDetector, initializationActions
                ))
                .withConfiguration(AutoConfigurations.of(
                    HttpEncodingAutoConfiguration.class,
                    MultipartAutoConfiguration.class,
                    DispatcherServletAutoConfiguration.class
                ))
                .withPropertyValues(
                    "spring.servlet.encoding.force=true",
                    "spring.servlet.multipart.file-size-threshold=64KB"
                )
                .withUserConfiguration(
                    SpringBootStarter.ProductionServletInfrastructureConfiguration.class
                )
                .run(applicationContext -> {
                    assertServletInfrastructure(applicationContext, "windows-1251", 38_000_000L);
                    assertSame(applicationContext.getBean(MultipartConfigElement.class),
                        applicationContext.getBean(DispatcherServletRegistrationBean.class)
                            .getMultipartConfig());
                });
        }

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
            assertEquals(springConfiguration.defaultEncoding(), applicationContext.getEnvironment()
                .getProperty(WebjetBootstrapSpringConfiguration.DEFAULT_ENCODING_PROPERTY));
            assertEquals(springConfiguration.maximumFileSizeBytes(), applicationContext.getEnvironment()
                .getProperty(WebjetBootstrapSpringConfiguration.MAXIMUM_FILE_SIZE_PROPERTY, Long.class));
            assertEquals(springConfiguration.maximumRequestSizeBytes(), applicationContext.getEnvironment()
                .getProperty(WebjetBootstrapSpringConfiguration.MAXIMUM_REQUEST_SIZE_PROPERTY, Long.class));
            assertEquals(springConfiguration.defaultEncoding(), applicationContext.getEnvironment()
                .getProperty("spring.servlet.encoding.charset"));
            assertEquals(springConfiguration.maximumFileSizeBytes() + "B", applicationContext.getEnvironment()
                .getProperty("spring.servlet.multipart.max-file-size"));
            assertEquals(springConfiguration.maximumRequestSizeBytes() + "B", applicationContext.getEnvironment()
                .getProperty("spring.servlet.multipart.max-request-size"));
            assertSame(springConfiguration, applicationContext.getBeanFactory()
                .getSingleton(WebjetBootstrapSpringConfiguration.BEAN_NAME));
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

    private void assertServletInfrastructure(org.springframework.context.ApplicationContext applicationContext,
            String expectedEncoding, long expectedMaximumPostSize) {
        CharacterEncodingFilter characterEncodingFilter =
            applicationContext.getBean(CharacterEncodingFilter.class);
        assertEquals(expectedEncoding, characterEncodingFilter.getEncoding());
        assertTrue(characterEncodingFilter.isForceRequestEncoding());
        assertTrue(characterEncodingFilter.isForceResponseEncoding());

        MultipartConfigElement multipartConfig = applicationContext.getBean(MultipartConfigElement.class);
        assertEquals(expectedMaximumPostSize, multipartConfig.getMaxFileSize());
        assertEquals(expectedMaximumPostSize, multipartConfig.getMaxRequestSize());
        assertEquals(65_536, multipartConfig.getFileSizeThreshold());
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
