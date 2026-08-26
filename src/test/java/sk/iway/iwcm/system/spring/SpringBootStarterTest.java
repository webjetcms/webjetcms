package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

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
}
