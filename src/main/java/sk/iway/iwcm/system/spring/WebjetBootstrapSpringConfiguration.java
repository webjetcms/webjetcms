package sk.iway.iwcm.system.spring;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.ConfDB;

record WebjetBootstrapSpringConfiguration(String installName, String logInstallName,
        String springAddPackages, String defaultEncoding, long maximumFileSizeBytes,
        long maximumRequestSizeBytes) {

    static final String BEAN_NAME = "webjetBootstrapSpringConfiguration";
    static final String INSTALL_NAME_PROPERTY = "webjet.bootstrap.spring.install-name";
    static final String LOG_INSTALL_NAME_PROPERTY = "webjet.bootstrap.spring.log-install-name";
    static final String ADD_PACKAGES_PROPERTY = "webjet.bootstrap.spring.add-packages";
    static final String DEFAULT_ENCODING_PROPERTY = "webjet.bootstrap.servlet.default-encoding";
    static final String MAXIMUM_FILE_SIZE_PROPERTY = "webjet.bootstrap.servlet.maximum-file-size-bytes";
    static final String MAXIMUM_REQUEST_SIZE_PROPERTY = "webjet.bootstrap.servlet.maximum-request-size-bytes";

    private static final String INSTALL_NAME = "installName";
    private static final String LOG_INSTALL_NAME = "logInstallName";
    private static final String SPRING_ADD_PACKAGES = "springAddPackages";
    private static final String DEFAULT_ENCODING = "defaultEncoding";
    private static final String MAXIMUM_POST_SIZE = "stripes.FileUpload.MaximumPostSize";
    private static final String SPRING_DEFAULT_ENCODING_PROPERTY = "spring.servlet.encoding.charset";
    private static final String SPRING_MAXIMUM_FILE_SIZE_PROPERTY = "spring.servlet.multipart.max-file-size";
    private static final String SPRING_MAXIMUM_REQUEST_SIZE_PROPERTY = "spring.servlet.multipart.max-request-size";
    private static final String DEFAULT_ENCODING_VALUE = "utf-8";
    private static final long DEFAULT_MAXIMUM_POST_SIZE_BYTES = 5_000_000_000L;
    private static final Pattern LEGACY_DATA_SIZE_PATTERN = Pattern.compile("^([+-]?\\d+)([mg])$");

    WebjetBootstrapSpringConfiguration(String installName, String logInstallName,
            String springAddPackages) {
        this(installName, logInstallName, springAddPackages,
            DEFAULT_ENCODING_VALUE, DEFAULT_MAXIMUM_POST_SIZE_BYTES,
            DEFAULT_MAXIMUM_POST_SIZE_BYTES);
    }

    WebjetBootstrapSpringConfiguration {
        installName = normalize(installName);
        logInstallName = normalize(logInstallName);
        springAddPackages = normalize(springAddPackages);
        defaultEncoding = normalize(defaultEncoding);
        if (Tools.isEmpty(defaultEncoding)) {
            defaultEncoding = DEFAULT_ENCODING_VALUE;
        }
    }

    static WebjetBootstrapSpringConfiguration empty() {
        return new WebjetBootstrapSpringConfiguration("", "", "",
            DEFAULT_ENCODING_VALUE, DEFAULT_MAXIMUM_POST_SIZE_BYTES,
            DEFAULT_MAXIMUM_POST_SIZE_BYTES);
    }

    static WebjetBootstrapSpringConfiguration empty(Environment environment) {
        return new WebjetBootstrapSpringConfiguration("", "", "",
            resolveEncoding(DEFAULT_ENCODING_VALUE, environment),
            resolveDataSize(SPRING_MAXIMUM_FILE_SIZE_PROPERTY,
                DEFAULT_MAXIMUM_POST_SIZE_BYTES, environment),
            resolveDataSize(SPRING_MAXIMUM_REQUEST_SIZE_PROPERTY,
                DEFAULT_MAXIMUM_POST_SIZE_BYTES, environment));
    }

    static WebjetBootstrapSpringConfiguration fromConstants(Environment environment) {
        long maximumPostSizeBytes = parseMaximumPostSize(Constants.getString(MAXIMUM_POST_SIZE));
        return new WebjetBootstrapSpringConfiguration(
            Constants.getInstallName(),
            Constants.getLogInstallName(),
            Constants.getString(SPRING_ADD_PACKAGES),
            resolveEncoding(Constants.getString(DEFAULT_ENCODING), environment),
            resolveDataSize(SPRING_MAXIMUM_FILE_SIZE_PROPERTY, maximumPostSizeBytes, environment),
            resolveDataSize(SPRING_MAXIMUM_REQUEST_SIZE_PROPERTY, maximumPostSizeBytes, environment)
        );
    }

    static WebjetBootstrapSpringConfiguration fromDatabaseValues(Map<String, String> databaseValues,
            Environment environment) {
        long maximumPostSizeBytes = parseMaximumPostSize(
            resolveValue(MAXIMUM_POST_SIZE, databaseValues, environment)
        );
        return new WebjetBootstrapSpringConfiguration(
            resolveValue(INSTALL_NAME, databaseValues, environment),
            resolveValue(LOG_INSTALL_NAME, databaseValues, environment),
            resolveValue(SPRING_ADD_PACKAGES, databaseValues, environment),
            resolveEncoding(resolveValue(DEFAULT_ENCODING, databaseValues, environment), environment),
            resolveDataSize(SPRING_MAXIMUM_FILE_SIZE_PROPERTY, maximumPostSizeBytes, environment),
            resolveDataSize(SPRING_MAXIMUM_REQUEST_SIZE_PROPERTY, maximumPostSizeBytes, environment)
        );
    }

    static WebjetBootstrapSpringConfiguration fromEnvironment(Environment environment) {
        return new WebjetBootstrapSpringConfiguration(
            environment.getProperty(INSTALL_NAME_PROPERTY),
            environment.getProperty(LOG_INSTALL_NAME_PROPERTY),
            environment.getProperty(ADD_PACKAGES_PROPERTY),
            environment.getProperty(DEFAULT_ENCODING_PROPERTY, DEFAULT_ENCODING_VALUE),
            environment.getProperty(MAXIMUM_FILE_SIZE_PROPERTY, Long.class,
                DEFAULT_MAXIMUM_POST_SIZE_BYTES),
            environment.getProperty(MAXIMUM_REQUEST_SIZE_PROPERTY, Long.class,
                DEFAULT_MAXIMUM_POST_SIZE_BYTES)
        );
    }

    void addProperties(Map<String, Object> properties) {
        properties.put(INSTALL_NAME_PROPERTY, installName);
        properties.put(LOG_INSTALL_NAME_PROPERTY, logInstallName);
        properties.put(ADD_PACKAGES_PROPERTY, springAddPackages);
        properties.put(DEFAULT_ENCODING_PROPERTY, defaultEncoding);
        properties.put(MAXIMUM_FILE_SIZE_PROPERTY, maximumFileSizeBytes);
        properties.put(MAXIMUM_REQUEST_SIZE_PROPERTY, maximumRequestSizeBytes);
        properties.put(SPRING_DEFAULT_ENCODING_PROPERTY, defaultEncoding);
        properties.put(SPRING_MAXIMUM_FILE_SIZE_PROPERTY, maximumFileSizeBytes + "B");
        properties.put(SPRING_MAXIMUM_REQUEST_SIZE_PROPERTY, maximumRequestSizeBytes + "B");
    }

    String[] getAdditionalPackages() {
        Set<String> packages = new LinkedHashSet<>();
        String[] configuredPackages = StringUtils.tokenizeToStringArray(springAddPackages, ",");
        if (configuredPackages != null) {
            for (String configuredPackage : configuredPackages) {
                if (Tools.isNotEmpty(configuredPackage)) {
                    packages.add(configuredPackage);
                }
            }
        }
        return packages.toArray(String[]::new);
    }

    private static String resolveValue(String name, Map<String, String> databaseValues,
            Environment environment) {
        String value = databaseValues.get(name);

        String configuredValue = getSystemProperty(environment, "webjet." + name);
        if (Tools.isEmpty(configuredValue)) {
            configuredValue = getSystemProperty(environment, "webjet_" + name);
        }
        if (Tools.isNotEmpty(configuredValue)) {
            value = configuredValue;
        }

        String environmentValue = getSystemEnvironment(environment, "webjet_" + name);
        if (Tools.isNotEmpty(environmentValue)) {
            value = environmentValue;
        }

        String contextValue = environment.getProperty(
            "server.servlet.context-parameters.webjet_" + name
        );
        if (Tools.isNotEmpty(contextValue)) {
            value = contextValue;
        }
        return ConfDB.tryDecrypt(value);
    }

    private static String getProperty(Map<String, Object> properties, String name) {
        Object value = properties.get(name);
        return value == null ? null : value.toString();
    }

    private static String getSystemProperty(Environment environment, String name) {
        if (environment instanceof AbstractEnvironment abstractEnvironment) {
            return getProperty(abstractEnvironment.getSystemProperties(), name);
        }
        return System.getProperty(name);
    }

    private static String getSystemEnvironment(Environment environment, String name) {
        if (environment instanceof AbstractEnvironment abstractEnvironment) {
            return getProperty(abstractEnvironment.getSystemEnvironment(), name);
        }
        return System.getenv(name);
    }

    static long parseMaximumPostSize(String maximumPostSize) {
        if (Tools.isEmpty(maximumPostSize)) {
            return DEFAULT_MAXIMUM_POST_SIZE_BYTES;
        }
        String configuredSize = maximumPostSize.trim();
        try {
            Matcher matcher = LEGACY_DATA_SIZE_PATTERN.matcher(configuredSize);
            if (matcher.matches()) {
                long multiplier = "m".equals(matcher.group(2)) ? 1_000_000L : 1_000_000_000L;
                return Math.multiplyExact(Long.parseLong(matcher.group(1)), multiplier);
            }
            return Long.parseLong(configuredSize);
        } catch (ArithmeticException | NumberFormatException ex) {
            Logger.warn(WebjetBootstrapSpringConfiguration.class,
                "Invalid stripes.FileUpload.MaximumPostSize value '" + configuredSize
                    + "', using " + DEFAULT_MAXIMUM_POST_SIZE_BYTES + " bytes");
            return DEFAULT_MAXIMUM_POST_SIZE_BYTES;
        }
    }

    private static String resolveEncoding(String legacyEncoding, Environment environment) {
        String configuredEncoding = environment.getProperty(SPRING_DEFAULT_ENCODING_PROPERTY);
        return Tools.isNotEmpty(configuredEncoding) ? configuredEncoding : legacyEncoding;
    }

    private static long resolveDataSize(String propertyName, long legacySize, Environment environment) {
        String configuredSize = environment.getProperty(propertyName);
        if (Tools.isEmpty(configuredSize)) {
            return legacySize;
        }
        return DataSize.parse(configuredSize.trim()).toBytes();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return "-".equals(normalized) ? "" : normalized;
    }
}
