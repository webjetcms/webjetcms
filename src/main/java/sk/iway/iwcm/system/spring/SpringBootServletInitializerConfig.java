package sk.iway.iwcm.system.spring;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to integrate SpringAppInitializer with Spring Boot embedded server.
 * In Spring Boot, WebApplicationInitializer.onStartup() is not called automatically,
 * so we need to programmatically register the servlet and filters via ServletContextInitializer.
 */
@Configuration
public class SpringBootServletInitializerConfig {

    /**
     * Wraps SpringAppInitializer as a ServletContextInitializer so it gets called during Spring Boot startup.
     * This ensures that InitServlet.initializeWebJET() is called, which sets the ServletContext in Constants.
     */
    @Bean
    public org.springframework.boot.web.servlet.ServletContextInitializer springAppInitializer() {
        return new org.springframework.boot.web.servlet.ServletContextInitializer() {
            private final SpringAppInitializer initializer = new SpringAppInitializer();

            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                initializer.onStartup(servletContext);
            }
        };
    }
}
