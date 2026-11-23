package sk.iway.aceintegration.socialmedia;

import java.io.IOException;
import java.util.Locale;

/**
 * Reports expected feed and transport failures using diagnostics safe for application logs.
 *
 * <p>Messages are built only from fixed reasons, numeric HTTP/Graph codes and allowlisted
 * Facebook field names. Upstream response text, request URLs and credentials are never accepted
 * as message components. The generator uses {@link #safeSummary(Exception)} to distinguish
 * these controlled messages from unexpected exceptions whose messages may contain sensitive data.</p>
 */
final class SocialMediaFeedException extends IOException {
    private static final long serialVersionUID = 1L;

    /**
     * Lists Graph API fields that may be named in missing-field diagnostics.
     * The HTTP client matches upstream messages against these identifiers instead of copying
     * arbitrary field names from the response into logs.
     */
    enum FacebookField {
        ID, MESSAGE, CREATED_TIME, PERMALINK_URL, FULL_PICTURE, ATTACHMENTS, TITLE, DESCRIPTION,
        MEDIA_TYPE, TYPE, URL, UNSHIMMED_URL, SUBATTACHMENTS, PUBLISHED_POSTS;

        String apiName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Defines fixed messages for configuration, provider, URL-policy and response-limit failures. */
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

    /**
     * Combines a fixed failure description with the numeric diagnostics available from the response.
     *
     * @param reason predefined failure category
     * @param status HTTP status code, or {@code null} when unavailable
     * @param code Graph API error code, or {@code null} when unavailable
     * @param subcode Graph API error subcode, or {@code null} when unavailable
     * @return diagnostic text containing no upstream message content
     */
    private static String message(Reason reason, Integer status, Integer code, Integer subcode) {
        StringBuilder message = new StringBuilder(reason.description);
        if (status != null) message.append("; HTTP ").append(status);
        if (code != null) message.append("; Facebook code ").append(code);
        if (subcode != null) message.append("; Facebook subcode ").append(subcode);
        return message.toString();
    }

    /**
     * Produces a log summary without exposing messages from unexpected exception types.
     *
     * @param exception non-null failure caught during feed processing
     * @return the controlled message of a feed exception, otherwise only the exception's simple class name
     */
    static String safeSummary(Exception exception) {
        return exception instanceof SocialMediaFeedException ? exception.getMessage() : exception.getClass().getSimpleName();
    }
}
