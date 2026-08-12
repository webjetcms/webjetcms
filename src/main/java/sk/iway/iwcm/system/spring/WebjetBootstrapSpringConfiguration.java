package sk.iway.iwcm.system.spring;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.ConfDB;

record WebjetBootstrapSpringConfiguration(String installName, String logInstallName,
        String springAddPackages) {

    static final String INSTALL_NAME_PROPERTY = "webjet.bootstrap.spring.install-name";
    static final String LOG_INSTALL_NAME_PROPERTY = "webjet.bootstrap.spring.log-install-name";
    static final String ADD_PACKAGES_PROPERTY = "webjet.bootstrap.spring.add-packages";

    private static final String INSTALL_NAME = "installName";
    private static final String LOG_INSTALL_NAME = "logInstallName";
    private static final String SPRING_ADD_PACKAGES = "springAddPackages";

    WebjetBootstrapSpringConfiguration {
        installName = normalize(installName);
        logInstallName = normalize(logInstallName);
        springAddPackages = normalize(springAddPackages);
    }

    static WebjetBootstrapSpringConfiguration empty() {
        return new WebjetBootstrapSpringConfiguration("", "", "");
    }

    static WebjetBootstrapSpringConfiguration fromConstants() {
        return new WebjetBootstrapSpringConfiguration(
            Constants.getInstallName(),
            Constants.getLogInstallName(),
            Constants.getString(SPRING_ADD_PACKAGES)
        );
    }

    static WebjetBootstrapSpringConfiguration fromDatabaseValues(Map<String, String> databaseValues,
            Environment environment) {
        return new WebjetBootstrapSpringConfiguration(
            resolveValue(INSTALL_NAME, databaseValues, environment),
            resolveValue(LOG_INSTALL_NAME, databaseValues, environment),
            resolveValue(SPRING_ADD_PACKAGES, databaseValues, environment)
        );
    }

    static WebjetBootstrapSpringConfiguration fromEnvironment(Environment environment) {
        return new WebjetBootstrapSpringConfiguration(
            environment.getProperty(INSTALL_NAME_PROPERTY),
            environment.getProperty(LOG_INSTALL_NAME_PROPERTY),
            environment.getProperty(ADD_PACKAGES_PROPERTY)
        );
    }

    void addProperties(Map<String, Object> properties) {
        properties.put(INSTALL_NAME_PROPERTY, installName);
        properties.put(LOG_INSTALL_NAME_PROPERTY, logInstallName);
        properties.put(ADD_PACKAGES_PROPERTY, springAddPackages);
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

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return "-".equals(normalized) ? "" : normalized;
    }
}
