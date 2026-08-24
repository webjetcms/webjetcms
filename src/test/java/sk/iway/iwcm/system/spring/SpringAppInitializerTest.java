package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;

class SpringAppInitializerTest {

    private final SpringAppInitializer springAppInitializer = new SpringAppInitializer();

    @Test
    void failedProductionInitializationFailsClosedAndCleansUp() {
        WebjetBootstrapState bootstrapState = WebjetBootstrapState.pending(WebjetBootstrapMode.PRODUCTION);
        WebjetInitializationActions initializationActions = mock(WebjetInitializationActions.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(initializationActions.initialize(servletContext)).thenReturn(false);

        ServletContextInitializer coreInitializer = springAppInitializer.springAppInitializerOnStartup(
            bootstrapState, initializationActions, applicationContext
        );

        assertThrows(WebjetBootstrapUnavailableException.class,
            () -> coreInitializer.onStartup(servletContext));

        assertTrue(bootstrapState.isCoreInitializationAttempted());
        assertFalse(bootstrapState.isCoreInitialized());
        assertFalse(bootstrapState.isPostInitializationCompleted());
        verify(initializationActions).initialize(servletContext);
        verify(initializationActions).cleanupAfterRejectedCoreInitialization(false);
        verify(initializationActions, never()).initializeAfterSpring();
    }

    @Test
    void successfulProductionInitializationRunsPostInitializationOnlyWhenApplicationIsReady() throws Exception {
        WebjetBootstrapState bootstrapState = WebjetBootstrapState.pending(WebjetBootstrapMode.PRODUCTION);
        WebjetInitializationActions initializationActions = mock(WebjetInitializationActions.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(initializationActions.initialize(servletContext)).thenReturn(true);
        when(initializationActions.initializeAfterSpring()).thenReturn(true);

        ServletContextInitializer coreInitializer = springAppInitializer.springAppInitializerOnStartup(
            bootstrapState, initializationActions, applicationContext
        );
        ApplicationListener<ApplicationReadyEvent> readyListener =
            springAppInitializer.webjetApplicationReadyListener(bootstrapState, initializationActions);

        coreInitializer.onStartup(servletContext);

        assertTrue(bootstrapState.isCoreInitializationAttempted());
        assertTrue(bootstrapState.isCoreInitialized());
        assertFalse(bootstrapState.isPostInitializationCompleted());
        verify(initializationActions, never()).initializeAfterSpring();

        verify(initializationActions, times(1)).initialize(servletContext);

        readyListener.onApplicationEvent(mock(ApplicationReadyEvent.class));

        assertTrue(bootstrapState.isPostInitializationCompleted());
        verify(initializationActions).initializeAfterSpring();

        readyListener.onApplicationEvent(mock(ApplicationReadyEvent.class));
        verify(initializationActions, times(1)).initializeAfterSpring();
    }

    @Test
    void setupModeNeverRunsProductionPostInitialization() throws Exception {
        WebjetBootstrapState bootstrapState = WebjetBootstrapState.initialized(WebjetBootstrapMode.SETUP, false);
        WebjetInitializationActions initializationActions = mock(WebjetInitializationActions.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        ServletContext servletContext = mock(ServletContext.class);

        ServletContextInitializer coreInitializer = springAppInitializer.springAppInitializerOnStartup(
            bootstrapState, initializationActions, applicationContext
        );
        ApplicationListener<ApplicationReadyEvent> readyListener =
            springAppInitializer.webjetApplicationReadyListener(bootstrapState, initializationActions);

        coreInitializer.onStartup(servletContext);
        readyListener.onApplicationEvent(mock(ApplicationReadyEvent.class));

        assertTrue(bootstrapState.isCoreInitializationAttempted());
        assertFalse(bootstrapState.isCoreInitialized());
        assertFalse(bootstrapState.isPostInitializationCompleted());
        verify(initializationActions, never()).initialize(servletContext);
        verify(initializationActions, never()).initializeAfterSpring();
    }

    @Test
    void setupModeLogsInstructionsWhenApplicationIsReady() {
        WebjetBootstrapState bootstrapState = WebjetBootstrapState.initialized(WebjetBootstrapMode.SETUP, false);
        WebjetInitializationActions initializationActions = mock(WebjetInitializationActions.class);
        ApplicationListener<ApplicationReadyEvent> readyListener =
            springAppInitializer.webjetApplicationReadyListener(bootstrapState, initializationActions);

        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            readyListener.onApplicationEvent(mock(ApplicationReadyEvent.class));

            logger.verify(() -> Logger.info(
                SpringBootStarter.class, "Spring Boot context started successfully"
            ));
            logger.verify(() -> Logger.info(
                SpringBootStarter.class, SpringAppInitializer.WEBJET_STARTED_MESSAGE
            ));
            logger.verify(() -> Logger.info(
                SpringBootStarter.class, SpringAppInitializer.SETUP_STARTUP_INSTRUCTIONS
            ));
        }

        verify(initializationActions, never()).initializeAfterSpring();
    }

    @Test
    void productionModeDoesNotLogSetupInstructions() {
        WebjetBootstrapState bootstrapState = WebjetBootstrapState.initialized(WebjetBootstrapMode.PRODUCTION, true);
        WebjetInitializationActions initializationActions = mock(WebjetInitializationActions.class);
        when(initializationActions.initializeAfterSpring()).thenReturn(true);
        ApplicationListener<ApplicationReadyEvent> readyListener =
            springAppInitializer.webjetApplicationReadyListener(bootstrapState, initializationActions);

        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            readyListener.onApplicationEvent(mock(ApplicationReadyEvent.class));

            logger.verify(() -> Logger.info(
                SpringBootStarter.class, SpringAppInitializer.WEBJET_STARTED_MESSAGE
            ));
            logger.verify(() -> Logger.info(
                SpringBootStarter.class, SpringAppInitializer.SETUP_STARTUP_INSTRUCTIONS
            ), never());
        }

        verify(initializationActions).initializeAfterSpring();
    }

    @Test
    void pendingSetupModeInitializesCoreFromStartedServletContext() throws Exception {
        WebjetBootstrapState bootstrapState = WebjetBootstrapState.pending(WebjetBootstrapMode.SETUP);
        WebjetInitializationActions initializationActions = mock(WebjetInitializationActions.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(initializationActions.initialize(servletContext)).thenReturn(false);

        ServletContextInitializer coreInitializer = springAppInitializer.springAppInitializerOnStartup(
            bootstrapState, initializationActions, applicationContext
        );

        coreInitializer.onStartup(servletContext);

        assertTrue(bootstrapState.isCoreInitializationAttempted());
        assertFalse(bootstrapState.isCoreInitialized());
        verify(initializationActions).initialize(servletContext);
        verify(initializationActions, never()).cleanupAfterRejectedCoreInitialization(anyBoolean());
        verify(initializationActions, never()).initializeAfterSpring();
    }

    @Test
    void setupModeRejectsAndCleansUpAnAlreadyInitializedInstallation() {
        WebjetBootstrapState bootstrapState = WebjetBootstrapState.pending(WebjetBootstrapMode.SETUP);
        WebjetInitializationActions initializationActions = mock(WebjetInitializationActions.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(initializationActions.initialize(servletContext)).thenReturn(true);

        ServletContextInitializer coreInitializer = springAppInitializer.springAppInitializerOnStartup(
            bootstrapState, initializationActions, applicationContext
        );

        assertThrows(WebjetBootstrapModeMismatchException.class,
            () -> coreInitializer.onStartup(servletContext));

        verify(initializationActions).initialize(servletContext);
        verify(initializationActions).cleanupAfterRejectedCoreInitialization(true);
        verify(initializationActions, never()).initializeAfterSpring();
    }

    @Test
    void failedPostInitializationCleansUpBackgroundServices() {
        WebjetInitializationActions initializationActions = new WebjetInitializationActions();

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class)) {
            initServlet.when(InitServlet::isWebjetInitialized).thenReturn(false);

            assertFalse(initializationActions.initializeAfterSpring());

            initServlet.verify(InitServlet::setSpringInitialized);
            initServlet.verify(InitServlet::initAfterSpring);
            initServlet.verify(InitServlet::cleanupAfterFailedSpringInitialization);
        }
    }

    @Test
    void postInitializationExceptionCleansUpBackgroundServices() {
        WebjetInitializationActions initializationActions = new WebjetInitializationActions();
        IllegalStateException failure = new IllegalStateException("post-initialization failed");

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class)) {
            initServlet.when(InitServlet::initAfterSpring).thenThrow(failure);

            assertThrows(IllegalStateException.class, initializationActions::initializeAfterSpring);

            initServlet.verify(InitServlet::cleanupAfterFailedSpringInitialization);
        }
    }
}
