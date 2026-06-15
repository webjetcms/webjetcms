package sk.iway.iwcm.system.spring;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DebugTimer;

/**
 * Configuration to integrate SpringAppInitializer with Spring Boot embedded server.
 * Only calls InitServlet.initializeWebJET() to set up ServletContext in Constants.
 * The rest of SpringAppInitializer.onStartup() is skipped because Spring Boot
 * already handles Spring context, servlet registration, and filters.
 */
@Configuration
public class SpringBootServletInitializerConfig {

    /**
     * Calls InitServlet.initializeWebJET() to set up ServletContext in Constants.
     * This is the ONLY part of SpringAppInitializer that needs to run during Spring Boot startup.
     * All other initialization (Spring context, DispatcherServlet, filters) is handled by Spring Boot.
     */
    @Bean
    public org.springframework.boot.web.servlet.ServletContextInitializer springAppInitializer() {
        return new org.springframework.boot.web.servlet.ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                try {
                    // Create DebugTimer for WebJET initialization (matches original SpringAppInitializer behavior)
                    DebugTimer dtGlobal = new DebugTimer("WebJET.init");
                    InitServlet.initializeWebJET(dtGlobal, servletContext);
                    Logger.println(SpringBootServletInitializerConfig.class, "WebJET initialized successfully for Spring Boot");

                    InitServlet.setSpringInitialized();
                    InitServlet.setWebjetInitialized();
                } catch (Exception e) {
                    Logger.println(SpringBootServletInitializerConfig.class, "ERROR initializing WebJET: " + e.getMessage());
                    throw new ServletException("Failed to initialize WebJET", e);
                }
            }
        };
    }
}
