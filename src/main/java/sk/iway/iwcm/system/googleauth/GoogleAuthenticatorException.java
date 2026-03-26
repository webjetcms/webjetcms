package sk.iway.iwcm.system.googleauth;
/**
 * Date: 12/02/14
 * Time: 13:36
 *
 * @author Enrico M. Crisostomo
 */
public class GoogleAuthenticatorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Builds an exception with the provided error message.
     *
     * @param message the error message.
     */
    public GoogleAuthenticatorException(String message) {
        super(message);
    }

    /**
     * Builds an exception with the provided error mesasge and
     * the provided cuase.
     *
     * @param message the error message.
     * @param cause   the cause.
     */
    public GoogleAuthenticatorException(String message, Throwable cause) {
        super(message, cause);
    }
}