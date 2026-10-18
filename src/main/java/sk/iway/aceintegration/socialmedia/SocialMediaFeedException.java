package sk.iway.aceintegration.socialmedia;

import java.io.IOException;
import java.util.Locale;

/** Carries diagnostic details that are safe to log without upstream text or credentials. */
final class SocialMediaFeedException extends IOException {
    private static final long serialVersionUID = 1L;

    /** Only field names requested by this integration may appear in diagnostics. */
    enum FacebookField {
        ID, MESSAGE, CREATED_TIME, PERMALINK_URL, FULL_PICTURE, ATTACHMENTS, TITLE, DESCRIPTION, PUBLISHED_POSTS;

        String apiName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    enum Reason {
        MISSING_FACEBOOK_TOKEN("Facebook Page token is not configured"),
        NO_AVAILABLE_POSTS("No social provider could be read and no cached posts are available"),
        HTTP_ERROR("Upstream HTTP request failed"),
        FACEBOOK_API_ERROR("Facebook API returned an error"),
        NONEXISTENT_FACEBOOK_FIELD("Requested Facebook field does not exist for this object"),
        FACEBOOK_OBJECT_IS_USER("Configured Facebook object is a User instead of a Page; verify socialMediaFacebookPageId"),
        FACEBOOK_OBJECT_UNAVAILABLE("Facebook object is unavailable; verify socialMediaFacebookPageId and Page permissions"),
        UNAPPROVED_RESOURCE("Unapproved feed resource host"),
        TOKEN_HOST_MISMATCH("Refusing to send a Page token to another host"),
        INVALID_RESPONSE_SIZE("Invalid response size"),
        RESPONSE_TOO_LARGE("Response exceeds size limit"),
        TOO_MANY_REDIRECTS("Too many image redirects");

        private final String description;

        Reason(String description) {
            this.description = description;
        }
    }

    SocialMediaFeedException(Reason reason) {
        this(reason, null, null, null);
    }

    SocialMediaFeedException(Reason reason, Integer httpStatus, Integer graphCode, Integer graphSubcode) {
        this(reason, httpStatus, graphCode, graphSubcode, null);
    }

    SocialMediaFeedException(Reason reason, Integer httpStatus, Integer graphCode, Integer graphSubcode, FacebookField field) {
        super(message(reason, httpStatus, graphCode, graphSubcode) + (field == null ? "" : "; requested field " + field.apiName()));
    }

    private static String message(Reason reason, Integer status, Integer code, Integer subcode) {
        StringBuilder message = new StringBuilder(reason.description);
        if (status != null) message.append("; HTTP ").append(status);
        if (code != null) message.append("; Facebook code ").append(code);
        if (subcode != null) message.append("; Facebook subcode ").append(subcode);
        return message.toString();
    }

    /** Returns only internally constructed diagnostics or the class of an unexpected exception. */
    static String safeSummary(Exception exception) {
        return exception instanceof SocialMediaFeedException ? exception.getMessage() : exception.getClass().getSimpleName();
    }
}
