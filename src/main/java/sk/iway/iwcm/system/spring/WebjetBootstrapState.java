package sk.iway.iwcm.system.spring;

final class WebjetBootstrapState {

    static final String BEAN_NAME = "webjetBootstrapState";

    private final WebjetBootstrapMode mode;
    private boolean coreInitializationAttempted;
    private boolean coreInitialized;
    private boolean postInitializationCompleted;

    private WebjetBootstrapState(WebjetBootstrapMode mode, boolean coreInitializationAttempted,
            boolean coreInitialized) {
        this.mode = mode;
        this.coreInitializationAttempted = coreInitializationAttempted;
        this.coreInitialized = coreInitialized;
    }

    static WebjetBootstrapState pending(WebjetBootstrapMode mode) {
        return new WebjetBootstrapState(mode, false, false);
    }

    static WebjetBootstrapState initialized(WebjetBootstrapMode mode, boolean coreInitialized) {
        return new WebjetBootstrapState(mode, true, coreInitialized);
    }

    WebjetBootstrapMode getMode() {
        return mode;
    }

    synchronized boolean isCoreInitializationAttempted() {
        return coreInitializationAttempted;
    }

    synchronized boolean isCoreInitialized() {
        return coreInitialized;
    }

    synchronized void recordCoreInitialization(boolean initialized) {
        if (coreInitializationAttempted) {
            throw new IllegalStateException("WebJET core initialization was already attempted");
        }
        coreInitializationAttempted = true;
        coreInitialized = initialized;
    }

    synchronized boolean isPostInitializationCompleted() {
        return postInitializationCompleted;
    }

    synchronized void recordPostInitializationCompleted() {
        postInitializationCompleted = true;
    }
}
