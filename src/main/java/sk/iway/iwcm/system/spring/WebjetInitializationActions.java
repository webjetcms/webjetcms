package sk.iway.iwcm.system.spring;

import jakarta.servlet.ServletContext;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;

class WebjetInitializationActions {

    boolean initialize(ServletContext servletContext) {
        return InitServlet.initializeWebJET(SpringAppInitializer.getDebugTimer(), servletContext);
    }

    boolean isLicenseRecoveryRequired() {
        return InitServlet.isLicenseInvalid();
    }

    boolean initializeAfterSpring() {
        InitServlet.setSpringInitialized();
        // Keep RuntimeException and Error handlers separate. AspectJ types a woven multi-catch
        // handler as Throwable, which is incompatible with AspectException's Exception advice.
        try {
            InitServlet.initAfterSpring();
            boolean initialized = InitServlet.isWebjetInitialized();
            if (initialized == false) {
                InitServlet.cleanupAfterFailedSpringInitialization();
            }
            return initialized;
        } catch (RuntimeException ex) { //NOSONAR - separate handlers are required for valid AspectJ bytecode
            InitServlet.cleanupAfterFailedSpringInitialization();
            throw ex;
        } catch (Error ex) { //NOSONAR - separate handlers are required for valid AspectJ bytecode
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
