package sk.iway.iwcm.system.spring;

class WebjetBootstrapModeMismatchException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    private final WebjetBootstrapMode requiredMode;

    WebjetBootstrapModeMismatchException(WebjetBootstrapMode selectedMode, boolean initialized) {
        super("WebJET bootstrap mode changed after bean definitions were selected: selected="
            + selectedMode.getPropertyValue() + ", initialized=" + initialized + ". The context must be rebuilt.");
        this.requiredMode = initialized ? WebjetBootstrapMode.PRODUCTION : WebjetBootstrapMode.SETUP;
    }

    WebjetBootstrapMode getRequiredMode() {
        return requiredMode;
    }

    static WebjetBootstrapMode findRequiredMode(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof WebjetBootstrapModeMismatchException mismatchException) {
                return mismatchException.getRequiredMode();
            }
            cause = cause.getCause();
        }
        return null;
    }
}
