package sk.iway.iwcm.system.spring;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DebugTimer;

/**
 * Spring Boot lifecycle configuration for WebJET CMS startup.
 *
 * Completes WebJET initialization after the bootstrap mode was selected.
 *
 * Core initialization needs a started ServletContext and therefore runs from a
 * ServletContextInitializer before the other servlet/filter registrations.
 * Production post-initialization runs after context refresh and before
 * ApplicationRunner and CommandLineRunner beans.
 */
@Configuration(proxyBeanMethods = false)
public class SpringAppInitializer
{
	static final String WEBJET_STARTED_MESSAGE = "=== WebJET CMS started ===";
	static final String SETUP_STARTUP_INSTRUCTIONS = """
		WebJET setup is running:
		  URL path: /wjerrorpages/setup/setup
		  Username: setup
		  Password: configured setup token (webjet.setup.token / WEBJET_SETUP_TOKEN; value is not printed)
		  After setup, set webjet.setup.enabled=false, remove webjet.setup.token, and fully restart the application server.""";
	static final String LICENSE_RECOVERY_STARTUP_INSTRUCTIONS = """
		WebJET license recovery is running:
		  URL path: /wjerrorpages/setup/license
		  Enter a valid WebJET administrator username, password, and the new license number.
		  After updating the license, fully restart the application server.""";

	private static volatile DebugTimer dtGlobal = null;

	@Bean
	WebjetInitializationActions webjetInitializationActions() {
		return new WebjetInitializationActions();
	}

	@Bean
	ServletContextInitializer springAppInitializerOnStartup(WebjetBootstrapState bootstrapState,
			WebjetInitializationActions initializationActions, ApplicationContext applicationContext) {
		return new OrderedServletContextInitializer(
			servletContext -> initializeCore(
				servletContext, bootstrapState, initializationActions, applicationContext
			)
		);
	}

	@Bean
	ApplicationListener<ApplicationStartedEvent> webjetApplicationStartedListener(
			WebjetBootstrapState bootstrapState, WebjetInitializationActions initializationActions,
			ApplicationContext applicationContext) {
		return new WebjetApplicationStartedListener(
			bootstrapState, initializationActions, applicationContext
		);
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> webjetApplicationReadyListener(
			WebjetBootstrapState bootstrapState, ApplicationContext applicationContext) {
		return event -> {
			if (event.getApplicationContext() == applicationContext) {
				logApplicationReady(bootstrapState);
			}
		};
	}

	private void initializeCore(ServletContext servletContext, WebjetBootstrapState bootstrapState,
			WebjetInitializationActions initializationActions, ApplicationContext applicationContext) {
		initializeCoreIfNecessary(servletContext, bootstrapState, initializationActions);

		servletContext.setAttribute("springContext", applicationContext);
		Logger.info(SpringAppInitializer.class, "Set Spring ApplicationContext into ServletContext");
	}

	private void initializeCoreIfNecessary(ServletContext servletContext, WebjetBootstrapState bootstrapState,
			WebjetInitializationActions initializationActions) {
		if (bootstrapState.isCoreInitializationAttempted()) {
			validateBootstrapMode(bootstrapState, initializationActions);
			return;
		}
		bootstrapState.recordCoreInitialization(initializationActions.initialize(servletContext));
		validateBootstrapMode(bootstrapState, initializationActions);
	}

	private void validateBootstrapMode(WebjetBootstrapState bootstrapState,
			WebjetInitializationActions initializationActions) {
		boolean coreInitialized = bootstrapState.isCoreInitialized();
		if (bootstrapState.getMode() == WebjetBootstrapMode.PRODUCTION && coreInitialized == false) {
			boolean licenseRecoveryRequired = initializationActions.isLicenseRecoveryRequired();
			if (licenseRecoveryRequired) {
				throw new WebjetLicenseRecoveryRequiredException();
			}
			initializationActions.cleanupAfterRejectedCoreInitialization(false);
			throw new WebjetBootstrapUnavailableException("core initialization did not complete");
		}
		if (bootstrapState.getMode() == WebjetBootstrapMode.LICENSE_RECOVERY
				&& coreInitialized == false
				&& initializationActions.isLicenseRecoveryRequired() == false) {
			initializationActions.cleanupAfterRejectedCoreInitialization(false);
			throw new WebjetBootstrapUnavailableException("license recovery initialization is unavailable");
		}
		if (bootstrapState.getMode() != WebjetBootstrapMode.PRODUCTION && coreInitialized) {
			initializationActions.cleanupAfterRejectedCoreInitialization(true);
			throw new WebjetBootstrapModeMismatchException(
				bootstrapState.getMode(), coreInitialized
			);
		}
	}

	private static void initializeAfterRefresh(WebjetBootstrapState bootstrapState,
			WebjetInitializationActions initializationActions) {
		if (bootstrapState.getMode() != WebjetBootstrapMode.PRODUCTION) {
			return;
		}
		if (bootstrapState.isCoreInitializationAttempted() == false || bootstrapState.isCoreInitialized() == false) {
			throw new IllegalStateException("WebJET production context started without successful core initialization");
		}
		if (bootstrapState.isPostInitializationCompleted()) {
			return;
		}

		if (initializationActions.initializeAfterSpring() == false) {
			throw new IllegalStateException("WebJET post-initialization did not complete successfully");
		}
		bootstrapState.recordPostInitializationCompleted();
	}

	private void logApplicationReady(WebjetBootstrapState bootstrapState) {
		Logger.info(SpringBootStarter.class, "Spring Boot context started successfully");
		Logger.info(SpringBootStarter.class, WEBJET_STARTED_MESSAGE);
		if (bootstrapState.getMode() == WebjetBootstrapMode.SETUP) {
			Logger.info(SpringBootStarter.class, SETUP_STARTUP_INSTRUCTIONS);
		} else if (bootstrapState.getMode() == WebjetBootstrapMode.LICENSE_RECOVERY) {
			Logger.info(SpringBootStarter.class, LICENSE_RECOVERY_STARTUP_INSTRUCTIONS);
		}
	}

	static void startDebugTimer() {
		dtGlobal = new DebugTimer("WebJET.init");
	}

	static DebugTimer getDebugTimer() {
		if (dtGlobal == null) {
			startDebugTimer();
		}
		return dtGlobal;
	}

	private static final class WebjetApplicationStartedListener
			implements ApplicationListener<ApplicationStartedEvent>, Ordered {

		private final WebjetBootstrapState bootstrapState;
		private final WebjetInitializationActions initializationActions;
		private final ApplicationContext applicationContext;

		private WebjetApplicationStartedListener(WebjetBootstrapState bootstrapState,
				WebjetInitializationActions initializationActions, ApplicationContext applicationContext) {
			this.bootstrapState = bootstrapState;
			this.initializationActions = initializationActions;
			this.applicationContext = applicationContext;
		}

		@Override
		public void onApplicationEvent(ApplicationStartedEvent event) {
			if (event.getApplicationContext() == applicationContext) {
				initializeAfterRefresh(bootstrapState, initializationActions);
			}
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

		@Override
		public boolean supportsAsyncExecution() {
			return false;
		}
	}

	private static final class OrderedServletContextInitializer implements ServletContextInitializer, Ordered {

		private final ServletContextInitializer delegate;

		private OrderedServletContextInitializer(ServletContextInitializer delegate) {
			this.delegate = delegate;
		}

		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			delegate.onStartup(servletContext);
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}
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
