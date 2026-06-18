package sk.iway.iwcm.system.spring;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.doc.DebugTimer;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import net.sourceforge.stripes.controller.StripesFilterIway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import sk.iway.iwcm.PathFilter;

/**
 * WebApplicationInitializer for WebJET CMS Spring Boot startup.
 *
 * This class is the single initialization entry point for embedded Spring Boot mode.
 * It calls InitServlet.initializeWebJET() once during startup to set up the WebJET core.
 *
 * Servlet and filter registration is handled entirely by @Bean methods in
 * {@link SpringBootStarter}, so this class focuses only on core initialization.
 *
 * For embedded server: ./gradlew bootRun
 * For external Tomcat 11 deployment: ./gradlew war
 */
@Configuration
public class SpringAppInitializer
{
	private static DebugTimer dtGlobal = null;

	@Bean
    public org.springframework.boot.web.servlet.ServletContextInitializer springAppInitializerOnStartup() {
        return new org.springframework.boot.web.servlet.ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
				dtGlobal = new DebugTimer("WebJET.init");

				// Initialize WebJET core (called by InitServlet during startup as well)
				InitServlet.initializeWebJET(dtGlobal, servletContext);

				// Note: The following have been removed and are now handled by Spring Boot:
				// - AnnotationConfigWebApplicationContext / ContextLoaderListener (caused "multiple ContextLoader" error)
				// - DispatcherServlet (Spring Boot auto-configures this)
				// - CharacterEncodingFilter (configured via application properties)
				// - springSecurityFilterChain (Spring Security auto-configures via @Bean in SpringSecurityConf)
				// - RequestContextListener (Spring Boot auto-configures this)
				// - MultipartConfigElement (configured via application properties)
				// - Manual package scanning (handled by @ComponentScan in SpringBootStarter)
				InitServlet.setSpringInitialized();
				InitServlet.setWebjetInitialized();
			}
		};
	}

	/**
	 * Debug timing method - logs timing information for monitoring startup progress.
	 * Called from various places throughout the application for debug timing.
	 * @param message timing message to log
	 */
	public static void dtDiff(String message) {
		if (dtGlobal != null) {
			dtGlobal.diffInfo(message);
		}
	}
}
