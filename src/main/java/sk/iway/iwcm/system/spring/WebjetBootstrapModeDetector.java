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
                throw new WebjetBootstrapUnavailableException("database connection is unavailable");
            }

            Map<String, String> databaseValues = InitServlet.getDatabaseValues(connection);
            if (databaseValues.isEmpty()) {
                throw new WebjetBootstrapUnavailableException("configuration table is empty or unavailable");
            }

            Logger.info(WebjetBootstrapModeDetector.class, "WebJET production mode selected by database preflight");
            return new Detection(
                WebjetBootstrapMode.PRODUCTION,
                WebjetBootstrapSpringConfiguration.fromDatabaseValues(databaseValues, environment)
            );
        } catch (WebjetBootstrapUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            Logger.error(WebjetBootstrapModeDetector.class, ex);
            throw new WebjetBootstrapUnavailableException("database preflight failed", ex);
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

        static Detection setup(Environment environment) {
            return new Detection(
                WebjetBootstrapMode.SETUP,
                WebjetBootstrapSpringConfiguration.empty(environment)
            );
        }
    }
}
