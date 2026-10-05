package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.unit.DataSize;

class WebjetBootstrapSpringConfigurationTest {

    @Test
    void createsEmbeddedBootstrapSnapshotFromDatabaseValues() {
        WebjetBootstrapSpringConfiguration configuration =
            WebjetBootstrapSpringConfiguration.fromDatabaseValues(Map.of(
                "installName", "customer",
                "logInstallName", "customer-log",
                "springAddPackages", "com.example.one, com.example.two,com.example.one",
                "defaultEncoding", "windows-1250",
                "stripes.FileUpload.MaximumPostSize", "37m"
            ), environmentWithOverrides(Map.of(), Map.of()));

        assertEquals("customer", configuration.installName());
        assertEquals("customer-log", configuration.logInstallName());
        assertEquals("windows-1250", configuration.defaultEncoding());
        assertEquals(37_000_000L, configuration.maximumFileSizeBytes());
        assertEquals(37_000_000L, configuration.maximumRequestSizeBytes());
        assertArrayEquals(new String[] {"com.example.one", "com.example.two"},
            configuration.getAdditionalPackages());
    }

    @Test
    void bootstrapPropertiesOverrideDatabaseValues() {
        MockEnvironment environment = environmentWithOverrides(
            Map.of(
                "webjet.installName", "system-customer",
                "webjet.defaultEncoding", "iso-8859-2",
                "webjet.stripes.FileUpload.MaximumPostSize", "41m"
            ),
            Map.of(
                "webjet_installName", "environment-customer",
                "webjet_defaultEncoding", "windows-1251",
                "webjet_stripes.FileUpload.MaximumPostSize", "42m"
            )
        )
            .withProperty("server.servlet.context-parameters.webjet_springAddPackages",
                "com.example.context")
            .withProperty("server.servlet.context-parameters.webjet_defaultEncoding", "windows-1252")
            .withProperty(
                "server.servlet.context-parameters.webjet_stripes.FileUpload.MaximumPostSize", "43m");

        WebjetBootstrapSpringConfiguration configuration =
            WebjetBootstrapSpringConfiguration.fromDatabaseValues(Map.of(
                "installName", "database-customer",
                "springAddPackages", "com.example.database",
                "defaultEncoding", "windows-1250",
                "stripes.FileUpload.MaximumPostSize", "40m"
            ), environment);

        assertEquals("environment-customer", configuration.installName());
        assertEquals("com.example.context", configuration.springAddPackages());
        assertEquals("windows-1252", configuration.defaultEncoding());
        assertEquals(43_000_000L, configuration.maximumFileSizeBytes());
        assertEquals(43_000_000L, configuration.maximumRequestSizeBytes());
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

    @Test
    void missingServletValuesUseLegacyDefaults() {
        WebjetBootstrapSpringConfiguration configuration =
            WebjetBootstrapSpringConfiguration.fromDatabaseValues(
                Map.of(), environmentWithOverrides(Map.of(), Map.of()));

        assertEquals("utf-8", configuration.defaultEncoding());
        assertEquals(5_000_000_000L, configuration.maximumFileSizeBytes());
        assertEquals(5_000_000_000L, configuration.maximumRequestSizeBytes());
        assertEquals(5_000_000_000L,
            WebjetBootstrapSpringConfiguration.parseMaximumPostSize("invalid"));
        assertEquals(5_000_000_000L,
            WebjetBootstrapSpringConfiguration.parseMaximumPostSize("1mm"));
    }

    @Test
    void nativeSpringPropertiesOverrideLegacyServletValues() {
        MockEnvironment environment = environmentWithOverrides(Map.of(), Map.of())
            .withProperty("spring.servlet.encoding.charset", "UTF-16")
            .withProperty("spring.servlet.multipart.max-file-size", "41MB")
            .withProperty("spring.servlet.multipart.max-request-size", "42MB");

        WebjetBootstrapSpringConfiguration configuration =
            WebjetBootstrapSpringConfiguration.fromDatabaseValues(Map.of(
                "defaultEncoding", "windows-1250",
                "stripes.FileUpload.MaximumPostSize", "40m"
            ), environment);

        assertEquals("UTF-16", configuration.defaultEncoding());
        assertEquals(DataSize.ofMegabytes(41).toBytes(), configuration.maximumFileSizeBytes());
        assertEquals(DataSize.ofMegabytes(42).toBytes(), configuration.maximumRequestSizeBytes());
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
