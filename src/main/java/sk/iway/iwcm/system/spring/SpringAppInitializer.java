package sk.iway.iwcm.system.spring;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.doc.DebugTimer;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import net.sourceforge.stripes.controller.StripesFilterIway;
import org.springframework.web.WebApplicationInitializer;
import sk.iway.iwcm.PathFilter;

/**
 * Legacy WebApplicationInitializer for WebJET CMS.
 *
 * After migration to Spring Boot 4.x, this class is simplified:
 * - Manual AnnotationConfigWebApplicationContext and ContextLoaderListener are REMOVED
 *   (they caused "multiple ContextLoader* definitions" errors with Spring Boot)
 * - Manual DispatcherServlet registration is REMOVED (Spring Boot auto-configures this)
 * - Manual filter registrations are REMOVED (Spring Boot handles these via @Bean methods)
 *
 * This class now only registers Stripes framework filters, which are independent of
 * the Spring application context. All other initialization is handled by Spring Boot
 * through {@link SpringBootStarter}.
 *
 * For embedded server: ./gradlew bootRun
 * For external Tomcat 11 deployment: ./gradlew war
 */
public class SpringAppInitializer implements WebApplicationInitializer
{
	private static DebugTimer dtGlobal = null;

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException
	{
		dtGlobal = new DebugTimer("WebJET.init");

		// Initialize WebJET core (called by InitServlet during startup as well)
		InitServlet.initializeWebJET(dtGlobal, servletContext);

		// Register Stripes framework filter (independent of Spring context)
		FilterRegistration.Dynamic stripesFilter = servletContext.addFilter(
				"StripesFilter", new StripesFilterIway());
		stripesFilter.addMappingForUrlPatterns(
				java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST), true, "/*");
		stripesFilter.addMappingForUrlPatterns(
				java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST), true, "StripesDispatcher");

		// Register Virtual Path Filter
		servletContext.addFilter("Virtual Path Filter", new PathFilter())
				.addMappingForUrlPatterns(
						java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST), true, "/*");

		// Note: The following have been removed and are now handled by Spring Boot:
		// - AnnotationConfigWebApplicationContext / ContextLoaderListener (caused "multiple ContextLoader" error)
		// - DispatcherServlet (Spring Boot auto-configures this)
		// - CharacterEncodingFilter (configured via application properties)
		// - springSecurityFilterChain (Spring Security auto-configures via @Bean in SpringSecurityConf)
		// - RequestContextListener (Spring Boot auto-configures this)
		// - MultipartConfigElement (configured via application properties)
		// - Manual package scanning (handled by @ComponentScan in SpringBootStarter)

		InitServlet.setSpringInitialized();
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
