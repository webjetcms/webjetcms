package sk.iway.iwcm.system.spring;

import jakarta.servlet.ServletContext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DebugTimer;

/**
 * Spring Boot lifecycle configuration for WebJET CMS startup.
 *
 * Completes WebJET initialization after the bootstrap mode was selected.
 *
 * Core initialization needs a started ServletContext and therefore runs from a
 * ServletContextInitializer before the other servlet/filter registrations.
 * Production post-initialization is deliberately deferred until
 * ApplicationReadyEvent, after context refresh.
 */
@Configuration(proxyBeanMethods = false)
public class SpringAppInitializer
{
	private static volatile DebugTimer dtGlobal = null;

	@Bean
	WebjetInitializationActions webjetInitializationActions() {
		return new WebjetInitializationActions();
	}

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	ServletContextInitializer springAppInitializerOnStartup(WebjetBootstrapState bootstrapState,
			WebjetInitializationActions initializationActions, ApplicationContext applicationContext) {
		return servletContext -> initializeCore(
			servletContext, bootstrapState, initializationActions, applicationContext
		);
	}

	@Bean
	@ConditionalOnProperty(name = WebjetBootstrapMode.PROPERTY_NAME, havingValue = WebjetBootstrapMode.PRODUCTION_VALUE)
	ApplicationListener<ApplicationReadyEvent> webjetApplicationReadyListener(
			WebjetBootstrapState bootstrapState, WebjetInitializationActions initializationActions) {
		return event -> initializeAfterRefresh(bootstrapState, initializationActions);
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
			validateBootstrapMode(bootstrapState);
			return;
		}
		bootstrapState.recordCoreInitialization(initializationActions.initialize(servletContext));
		validateBootstrapMode(bootstrapState);
	}

	private void validateBootstrapMode(WebjetBootstrapState bootstrapState) {
		boolean productionMode = bootstrapState.getMode() == WebjetBootstrapMode.PRODUCTION;
		boolean coreInitialized = bootstrapState.isCoreInitialized();
		if (productionMode != coreInitialized) {
			throw new WebjetBootstrapModeMismatchException(
				bootstrapState.getMode(), coreInitialized
			);
		}
	}

	private void initializeAfterRefresh(WebjetBootstrapState bootstrapState,
			WebjetInitializationActions initializationActions) {
		if (bootstrapState.getMode() != WebjetBootstrapMode.PRODUCTION) {
			return;
		}
		if (bootstrapState.isCoreInitializationAttempted() == false || bootstrapState.isCoreInitialized() == false) {
			throw new IllegalStateException("WebJET production context became ready without successful core initialization");
		}
		if (bootstrapState.isPostInitializationCompleted()) {
			return;
		}

		if (initializationActions.initializeAfterSpring() == false) {
			throw new IllegalStateException("WebJET post-initialization did not complete successfully");
		}
		bootstrapState.recordPostInitializationCompleted();
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
