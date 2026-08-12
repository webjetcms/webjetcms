package sk.iway.iwcm.system.spring;

import java.sql.Connection;
import java.util.Map;

import org.springframework.core.env.Environment;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

class WebjetBootstrapModeDetector {

    Detection detect(Environment environment) {
        String contextDbName = firstNotEmpty(
            environment.getProperty("server.servlet.context-parameters.webjetDbname"),
            environment.getProperty("webjetDbname"),
            environment.getProperty("webjet.dbname")
        );
        InitServlet.setContextDbName(contextDbName);

        String dbName = firstNotEmpty(
            environment.getProperty("server.servlet.context-parameters.webjet_dbName"),
            environment.getProperty("webjet.dbName"),
            environment.getProperty("webjet_dbName")
        );
        if (Tools.isEmpty(dbName)) {
            dbName = "iwcm";
        }

        try (Connection connection = DBPool.getConnection(dbName)) {
            if (connection == null) {
                Logger.info(WebjetBootstrapModeDetector.class, "WebJET setup mode selected: database is unavailable");
                return Detection.setup();
            }

            Map<String, String> databaseValues = InitServlet.getDatabaseValues(connection);
            if (databaseValues.isEmpty()) {
                Logger.info(WebjetBootstrapModeDetector.class, "WebJET setup mode selected: configuration table is empty or unavailable");
                return Detection.setup();
            }

            Logger.info(WebjetBootstrapModeDetector.class, "WebJET production mode selected by database preflight");
            return new Detection(
                WebjetBootstrapMode.PRODUCTION,
                WebjetBootstrapSpringConfiguration.fromDatabaseValues(databaseValues, environment)
            );
        } catch (Exception ex) {
            Logger.error(WebjetBootstrapModeDetector.class, ex);
            return Detection.setup();
        }
    }

    private String firstNotEmpty(String... values) {
        for (String value : values) {
            if (Tools.isNotEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    record Detection(WebjetBootstrapMode mode,
            WebjetBootstrapSpringConfiguration springConfiguration) {

        static Detection setup() {
            return new Detection(
                WebjetBootstrapMode.SETUP,
                WebjetBootstrapSpringConfiguration.empty()
            );
        }
    }
}
