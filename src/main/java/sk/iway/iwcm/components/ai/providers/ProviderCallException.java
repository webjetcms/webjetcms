package sk.iway.iwcm.components.ai.providers;

import java.util.concurrent.CancellationException;

import com.webjetcms.ai.AiErrorCategory;
import com.webjetcms.ai.AiProviderException;

import lombok.Getter;

/** Preserves safe provider error metadata across CMS task and controller boundaries. */
@Getter
public class ProviderCallException extends Exception {
    private final AiErrorCategory errorCategory;
    private final Integer providerStatusCode;
    private final Boolean retryable;

    public ProviderCallException(String message) {
        this(message, null);
    }

    public ProviderCallException(String message, Throwable cause) {
        super(message, cause);
        if (cause instanceof ProviderCallException exception) {
            errorCategory = exception.getErrorCategory();
            providerStatusCode = exception.getProviderStatusCode();
            retryable = exception.getRetryable();
        } else if (cause instanceof AiProviderException exception) {
            errorCategory = exception.category();
            providerStatusCode = exception.statusCode() > 0 ? exception.statusCode() : null;
            retryable = exception.retryable();
        } else {
            errorCategory = cause instanceof CancellationException ? AiErrorCategory.CANCELLED : AiErrorCategory.UNKNOWN;
            providerStatusCode = null;
            retryable = errorCategory == AiErrorCategory.CANCELLED ? Boolean.FALSE : null;
        }
    }

    public ProviderCallException(Throwable cause) {
        this(cause == null ? null : cause.getLocalizedMessage(), cause);
    }

    @Override
    public String toString() {
        // Default: getClass().getName() + ": " + getLocalizedMessage()
        String msg = getLocalizedMessage();
        return msg == null ? getClass().getSimpleName() : msg;
    }
}
