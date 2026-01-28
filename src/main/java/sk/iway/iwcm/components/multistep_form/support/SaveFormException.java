package sk.iway.iwcm.components.multistep_form.support;

/**
 * Exception indicating a failure while saving a multistep form.
 *
 * <p>Provides context whether the error was caused by an end-user action
 * and an optional error redirect target to navigate
 * the user to a dedicated error page or step.</p>
 */
public class SaveFormException extends Exception {

    private boolean endUserTry = false;
    private String errorRedirect = null;

    public SaveFormException(String message, boolean endUserTry, String errorRedirect) {
        super(message);
        this.endUserTry = endUserTry;
        this.errorRedirect = errorRedirect;
    }

    public SaveFormException(String message, Throwable cause, boolean endUserTry, String errorRedirect) {
        super(message, cause);
        this.endUserTry = endUserTry;
        this.errorRedirect = errorRedirect;
    }

    public SaveFormException(Throwable cause, boolean endUserTry, String errorRedirect) {
        super(cause);
        this.endUserTry = endUserTry;
        this.errorRedirect = errorRedirect;
    }

    public boolean isEndUserTry() {
        return this.endUserTry;
    }

    public String getErrorRedirect() {
        if(this.errorRedirect == null) return null;
        return new String(this.errorRedirect);
    }
}
