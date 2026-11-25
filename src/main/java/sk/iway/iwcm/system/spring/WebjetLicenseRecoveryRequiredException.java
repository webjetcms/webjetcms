package sk.iway.iwcm.system.spring;

final class WebjetLicenseRecoveryRequiredException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    WebjetLicenseRecoveryRequiredException() {
        super("WebJET production bootstrap detected an invalid license; rebuilding the context in license recovery mode");
    }

    static boolean isCausedBy(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof WebjetLicenseRecoveryRequiredException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
