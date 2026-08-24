package sk.iway.iwcm.system.spring;

class WebjetBootstrapModeMismatchException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    WebjetBootstrapModeMismatchException(WebjetBootstrapMode selectedMode, boolean initialized) {
        super("WebJET setup mode was requested, but the existing installation initialized successfully: selected="
            + selectedMode.getPropertyValue() + ", initialized=" + initialized
            + ". Disable webjet.setup.enabled, remove webjet.setup.token, and restart the application server.");
    }
}
