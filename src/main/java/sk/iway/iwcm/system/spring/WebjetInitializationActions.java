package sk.iway.iwcm.system.spring;

import jakarta.servlet.ServletContext;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;

class WebjetInitializationActions {

    boolean initialize(ServletContext servletContext) {
        return InitServlet.initializeWebJET(SpringAppInitializer.getDebugTimer(), servletContext);
    }

    boolean initializeAfterSpring() {
        InitServlet.setSpringInitialized();
        try {
            InitServlet.initAfterSpring();
            boolean initialized = InitServlet.isWebjetInitialized();
            if (initialized == false) {
                InitServlet.cleanupAfterFailedSpringInitialization();
            }
            return initialized;
        } catch (RuntimeException | Error ex) {
            InitServlet.cleanupAfterFailedSpringInitialization();
            throw ex;
        }
    }

    void cleanupAfterRejectedCoreInitialization(boolean coreInitialized) {
        if (coreInitialized) {
            try {
                InitServlet.cleanupAfterFailedSpringInitialization();
            } catch (RuntimeException ex) {
                Logger.error(WebjetInitializationActions.class, ex);
            }
        }
        try {
            new InitServlet().destroy();
        } catch (RuntimeException ex) {
            Logger.error(WebjetInitializationActions.class, ex);
        }
    }
}
