package sk.iway.iwcm.system.spring;

class WebjetBootstrapModeMismatchException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    WebjetBootstrapModeMismatchException(WebjetBootstrapMode selectedMode, boolean initialized) {
        super(message(selectedMode, initialized));
    }

    private static String message(WebjetBootstrapMode selectedMode, boolean initialized) {
        if (selectedMode == WebjetBootstrapMode.SETUP) {
            return "WebJET setup mode was requested, but the existing installation initialized successfully: selected="
                + selectedMode.getPropertyValue() + ", initialized=" + initialized
                + ". Disable webjet.setup.enabled, remove webjet.setup.token, and restart the application server.";
        }
        return "WebJET bootstrap mode changed after bean definitions were selected: selected="
            + selectedMode.getPropertyValue() + ", initialized=" + initialized
            + ". Restart the application server.";
    }
}
