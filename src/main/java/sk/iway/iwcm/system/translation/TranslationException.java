package sk.iway.iwcm.system.translation;

/** Signals a provider failure so callers can choose a source-text fallback and notify the user. */
public class TranslationException extends RuntimeException {
    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}
