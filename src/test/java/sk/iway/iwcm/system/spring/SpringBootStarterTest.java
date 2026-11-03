package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.web.context.WebApplicationContext;

class SpringBootStarterTest {

    @Test
    void invalidLicenseRebuildsEmbeddedApplicationInLicenseRecoveryMode() {
        @SuppressWarnings("unchecked")
        Function<WebjetBootstrapMode, SpringApplicationBuilder> applicationFactory = mock(Function.class);
        SpringApplicationBuilder productionApplication = mock(SpringApplicationBuilder.class);
        SpringApplicationBuilder recoveryApplication = mock(SpringApplicationBuilder.class);
        RuntimeException wrappedRecoveryRequest = new IllegalStateException(
            "application startup failed", new WebjetLicenseRecoveryRequiredException()
        );
        String[] args = {"--server.port=0"};

        when(applicationFactory.apply(null)).thenReturn(productionApplication);
        when(applicationFactory.apply(WebjetBootstrapMode.LICENSE_RECOVERY)).thenReturn(recoveryApplication);
        when(productionApplication.run(args)).thenThrow(wrappedRecoveryRequest);

        SpringBootStarter.runApplication(args, applicationFactory);

        verify(applicationFactory).apply(null);
        verify(applicationFactory).apply(WebjetBootstrapMode.LICENSE_RECOVERY);
        verify(productionApplication).run(args);
        verify(recoveryApplication).run(args);
    }

    @Test
    void unrelatedEmbeddedStartupFailureIsNotRetried() {
        @SuppressWarnings("unchecked")
        Function<WebjetBootstrapMode, SpringApplicationBuilder> applicationFactory = mock(Function.class);
        SpringApplicationBuilder productionApplication = mock(SpringApplicationBuilder.class);
        RuntimeException startupFailure = new IllegalStateException("application startup failed");

        when(applicationFactory.apply(null)).thenReturn(productionApplication);
        when(productionApplication.run(any(String[].class))).thenThrow(startupFailure);

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> SpringBootStarter.runApplication(null, applicationFactory));

        assertSame(startupFailure, thrown);
        verify(applicationFactory, times(1)).apply(null);
        verify(applicationFactory, never()).apply(WebjetBootstrapMode.LICENSE_RECOVERY);
    }

    @Test
    void commandLineArgumentsArePassedOnlyToApplicationRun() {
        SpringApplicationBuilder application = mock(SpringApplicationBuilder.class);
        String[] args = {"--server.port=0", "--spring.profiles.active=production"};

        SpringBootStarter.runApplication(application, args);

        verify(application).run(args);
        verify(application, never()).profiles(any(String[].class));
    }

    @Test
    void nullArgumentsAreNormalizedToAnEmptyArray() {
        SpringApplicationBuilder application = mock(SpringApplicationBuilder.class);

        SpringBootStarter.runApplication(application, null);

        verify(application).run(new String[0]);
    }

    @Test
    void embeddedTomcatAllowsTheExpectedNumberOfMultipartParts() throws Exception {
        assertEquals("1000", PropertiesLoaderUtils.loadProperties(
            new ClassPathResource("application.properties")
        ).getProperty("server.tomcat.max-part-count"));
    }

    @Test
    void embeddedServerDefaultsUseSpringBoot4ServerProperties() throws Exception {
        Properties applicationProperties = PropertiesLoaderUtils.loadProperties(
            new ClassPathResource("application.properties")
        );
        Properties relevantServerProperties = new Properties();
        relevantServerProperties.setProperty("server.mime-mappings.properties",
            applicationProperties.getProperty("server.mime-mappings.properties"));
        relevantServerProperties.setProperty("server.servlet.session.persistent",
            applicationProperties.getProperty("server.servlet.session.persistent"));
        relevantServerProperties.setProperty("server.servlet.session.store-dir",
            applicationProperties.getProperty("server.servlet.session.store-dir"));
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
            new PropertiesPropertySource("webjetServerDefaults", relevantServerProperties)
        );

        ServerProperties serverProperties = Binder.get(environment)
            .bind("server", ServerProperties.class)
            .get();

        assertEquals("text/plain", serverProperties.getMimeMappings().get("properties"));
        assertTrue(serverProperties.getServlet().getSession().isPersistent());
        assertEquals(
            Path.of(System.getProperty("user.dir"), "work", "sessions").toFile(),
            serverProperties.getServlet().getSession().getStoreDir()
        );
        assertEquals("true", applicationProperties.getProperty(
            WebjetEmbeddedTomcatConfiguration.HTTP_REDIRECT_ENABLED_PROPERTY));
        assertEquals("80", applicationProperties.getProperty(
            WebjetEmbeddedTomcatConfiguration.HTTP_REDIRECT_PORT_PROPERTY));
        assertNull(applicationProperties.getProperty("server.tomcat.redirect-port"));
    }

    @Test
    void errorHandlingUsesSpringBoot4MvcProperties() throws Exception {
        Properties applicationProperties = PropertiesLoaderUtils.loadProperties(
            new ClassPathResource("application.properties")
        );
        WebProperties webProperties = new Binder(new MapConfigurationPropertySource(applicationProperties))
            .bind("spring.web", WebProperties.class)
            .get();

        assertFalse(webProperties.getError().getWhitelabel().isEnabled());
        assertEquals("/error", webProperties.getError().getPath());
        assertNull(applicationProperties.getProperty("server.error.whitelabel.enabled"));
        assertNull(applicationProperties.getProperty("server.error.path"));
    }

    @Test
    void externalWarDelegatesErrorPagesToServletContainer() {
        CapturingSpringBootStarter starter = new CapturingSpringBootStarter();

        Set<Object> applicationSources = starter.captureApplicationSources(mock(ServletContext.class));

        assertEquals(Set.of(SpringBootStarter.class), applicationSources);
    }

    private static class CapturingSpringBootStarter extends SpringBootStarter {

        private Set<Object> applicationSources;

        Set<Object> captureApplicationSources(ServletContext servletContext) {
            createRootApplicationContext(servletContext);
            return applicationSources;
        }

        @Override
        protected WebApplicationContext run(SpringApplication application) {
            applicationSources = application.getAllSources();
            return mock(WebApplicationContext.class);
        }
    }
}
