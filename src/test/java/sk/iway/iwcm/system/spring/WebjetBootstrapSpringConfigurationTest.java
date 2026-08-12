package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class WebjetBootstrapSpringConfigurationTest {

    @Test
    void createsEmbeddedBootstrapSnapshotFromDatabaseValues() {
        WebjetBootstrapSpringConfiguration configuration =
            WebjetBootstrapSpringConfiguration.fromDatabaseValues(Map.of(
                "installName", "customer",
                "logInstallName", "customer-log",
                "springAddPackages", "com.example.one, com.example.two,com.example.one"
            ), environmentWithOverrides(Map.of(), Map.of()));

        assertEquals("customer", configuration.installName());
        assertEquals("customer-log", configuration.logInstallName());
        assertArrayEquals(new String[] {"com.example.one", "com.example.two"},
            configuration.getAdditionalPackages());
    }

    @Test
    void bootstrapPropertiesOverrideDatabaseValues() {
        MockEnvironment environment = environmentWithOverrides(
            Map.of("webjet.installName", "system-customer"),
            Map.of("webjet_installName", "environment-customer")
        ).withProperty("server.servlet.context-parameters.webjet_springAddPackages",
            "com.example.context");

        WebjetBootstrapSpringConfiguration configuration =
            WebjetBootstrapSpringConfiguration.fromDatabaseValues(Map.of(
                "installName", "database-customer",
                "springAddPackages", "com.example.database"
            ), environment);

        assertEquals("environment-customer", configuration.installName());
        assertEquals("com.example.context", configuration.springAddPackages());
    }

    @Test
    void dashOverrideDisablesDynamicConfiguration() {
        WebjetBootstrapSpringConfiguration configuration =
            WebjetBootstrapSpringConfiguration.fromDatabaseValues(Map.of(
                "installName", "-",
                "springAddPackages", "-"
            ), environmentWithOverrides(Map.of(), Map.of()));

        assertEquals("", configuration.installName());
        assertArrayEquals(new String[0], configuration.getAdditionalPackages());
    }

    private MockEnvironment environmentWithOverrides(Map<String, Object> systemProperties,
            Map<String, Object> systemEnvironment) {
        return new MockEnvironment() {
            @Override
            public Map<String, Object> getSystemProperties() {
                return systemProperties;
            }

            @Override
            public Map<String, Object> getSystemEnvironment() {
                return systemEnvironment;
            }
        };
    }
}
