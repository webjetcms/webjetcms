package sk.iway.aceintegration;

import org.apache.catalina.session.FileStore;
import org.apache.catalina.session.PersistentManager;
import org.springframework.boot.tomcat.TomcatContextCustomizer;
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

    /**
     * Configures file-based session persistence for embedded Tomcat.
     * Replaces the old META-INF/context.xml PersistentManager configuration
     * which is not used by Spring Boot 4.x embedded server.
     *
     * <p>Sessions are persisted to ${user.home}/.webjetcms/sessions/ on each
     * request and restored on server restart, so users stay logged in after
     * server restarts.</p>
     */
    @Bean
    public TomcatContextCustomizer tomcatSessionPersistenceCustomizer() {
        return context -> {
            PersistentManager pm = new PersistentManager();
            FileStore fs = new FileStore();
            fs.setDirectory(System.getProperty("user.home") + "/.webjetcms/sessions");
            pm.setStore(fs);
            context.setManager(pm);
        };
    }

}
