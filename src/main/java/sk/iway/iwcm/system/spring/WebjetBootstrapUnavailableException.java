package sk.iway.iwcm.system.spring;

class WebjetBootstrapUnavailableException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    WebjetBootstrapUnavailableException(String reason) {
        super(message(reason));
    }

    WebjetBootstrapUnavailableException(String reason, Throwable cause) {
        super(message(reason), cause);
    }

    private static String message(String reason) {
        return "WebJET production bootstrap failed: " + reason
            + ". Setup is never selected automatically. For a new installation, set webjet.setup.enabled=true "
            + "and configure webjet.setup.token before restarting the application server.";
    }
}
