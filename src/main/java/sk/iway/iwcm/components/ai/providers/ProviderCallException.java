package sk.iway.iwcm.components.ai.providers;

public class ProviderCallException extends Exception {
    public ProviderCallException(String message) {
        super(message);
    }

    public ProviderCallException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderCallException(Throwable cause) {
        super(cause);
    }

    @Override
    public String toString() {
        // Default: getClass().getName() + ": " + getLocalizedMessage()
        String msg = getLocalizedMessage();
        return msg == null ? getClass().getSimpleName() : msg;
    }
}
