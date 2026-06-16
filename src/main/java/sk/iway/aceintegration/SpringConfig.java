package sk.iway.aceintegration;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.apache.catalina.connector.Connector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Ukazkova konfiguracia Spring
 * http://docs.webjetcms.sk/latest/custom-apps/spring-config/
 */

@Configuration("aceintegrationSpringConfig")
@ComponentScan({
    "sk.iway.aceintegration",
})
public class SpringConfig {

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
