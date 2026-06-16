package sk.iway.aceintegration;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures embedded Tomcat to expose both HTTP (port 80) and HTTPS (port 443).
 *
 * In Spring Boot 4.x, when server.ssl.enabled=true, server.port refers to the
 * HTTPS port. This customizer adds an explicit HTTP connector on port 80
 * while keeping the default HTTPS connector on port 443.
 *
 * This reproduces the old gretty behavior:
 *   httpPort = 80
 *   httpsEnabled = true
 *   httpsPort = 443
 */
@Configuration
public class TomcatPortConfiguration {

    /**
     * Adds an HTTP connector on port 80 alongside the default HTTPS connector on 443.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatHttpConnectorCustomizer() {
        return factory -> {
            // Create HTTP connector on port 80
            Connector httpConnector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            httpConnector.setScheme("http");
            httpConnector.setSecure(false);
            httpConnector.setPort(80);
            httpConnector.setRedirectPort(443);
            factory.addAdditionalConnectors(httpConnector);
        };
    }
}
