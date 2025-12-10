package sk.iway.iwcm.components.multistep_form.support;

public class SaveFormException extends Exception {
    public SaveFormException(String message) {
        super(message);
    }

    public SaveFormException(String message, Throwable cause) {
        super(message, cause);
    }

    public SaveFormException(Throwable cause) {
        super(cause);
    }
}
