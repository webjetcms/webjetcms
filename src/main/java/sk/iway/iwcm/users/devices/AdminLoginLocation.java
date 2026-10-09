package sk.iway.iwcm.users.devices;

import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import jakarta.servlet.http.HttpSession;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;

/**
 * Carries an optional, browser-supplied IP location through administrator login and its session lifecycle.
 *
 * <p>The login flow has a pre-login lookup and one fallback after authentication:</p>
 * <ol>
 *   <li>The login page runs {@code /admin/scripts/login-location.js}, which calls the external
 *       IP lookup URL from {@link #getLookupUrl()} directly from the browser. It posts only the
 *       city and country code to {@code /admin/logon/location/}, where
 *       {@link AdminLoginLocationController} delegates to {@link #prepare(HttpSession, Input)}.</li>
 *   <li>The validated hint is stored under {@link #PENDING}, with its receipt time.
 *       {@link sk.iway.iwcm.common.LogonTools} explicitly copies it when replacing the HTTP session,
 *       so it remains available through additional authentication steps.</li>
 *   <li>After authentication succeeds, {@code LogonTools.afterSuccessLogon} calls
 *       {@link #beginLogin(HttpSession)}. It consumes the hint if it is at most ten minutes old
 *       and exposes it through {@link #getLocation(HttpSession)} before {@link AdminDeviceService}
 *       records the device login and sends any new-device notification. {@link SessionHolder}
 *       also reads this location when updating session details.</li>
 *   <li>If the location is still missing, {@link sk.iway.iwcm.admin.layout.LayoutService} calls
 *       {@link #takeRetryUrl(HttpSession)} for the first rendered administration page, including
 *       direct editor links. The same script retries the lookup and posts to
 *       {@code /admin/rest/security/login-location}. The controller updates the current session
 *       and cluster summary through {@link #setLocation(HttpSession, String)}. It also asks
 *       {@link DeviceService} to fill the device's missing location only if its latest login
 *       still matches {@link #DEVICE_LOGIN_TIME}; this fallback does not resend the email.</li>
 * </ol>
 *
 * <p>Lookup failures do not delay or prevent authentication. The location is untrusted display
 * metadata, never evidence for authentication or device ownership. The browser supplies no account,
 * device or session identifiers; the controller derives the target from the caller's session.</p>
 *
 * <p>The {@code adminLoginLocationApiKey} setting selects the provider: an empty value uses
 * {@code ipwho.is}, a key uses {@code ipwhois.pro}, and {@code DISABLED} turns lookup off.
 * A configured key is intentionally included in the URL sent to the browser.</p>
 */
public final class AdminLoginLocation {
    public static final String PENDING = "adminLoginLocationPending";
    public static final String DEVICE_LOGIN_TIME = "adminLoginLocationDeviceTime";
    private static final String LOCATION = "adminLoginLocation";
    private static final String RETRIED = "adminLoginLocationRetried";
    private static final long MAX_AGE = 10 * 60 * 1000L;

    private AdminLoginLocation() { }

    /**
     * Contains the untrusted location fields submitted by the browser, without target identifiers.
     *
     * @param city city name returned by the IP lookup provider; validated before storage
     * @param countryCode two-letter country code; trimmed and converted to uppercase during validation
     */
    public record Input(String city, String countryCode) { }

    /**
     * Holds a validated hint while authentication is pending, including across session replacement.
     *
     * @param location normalized display text in the form {@code city, CC}
     * @param receivedAt server receipt time in epoch milliseconds, used to enforce the ten-minute lifetime
     */
    private record Pending(String location, long receivedAt) implements Serializable { }

    public static boolean isEnabled() {
        return !"DISABLED".equalsIgnoreCase(java.util.Objects.toString(Constants.getString("adminLoginLocationApiKey"), "").trim());
    }

    /**
     * Builds the external IP lookup URL for the browser without making a server-side request.
     *
     * <p>An empty API key selects the free endpoint; otherwise the URL includes the encoded paid
     * key, which is visible to the browser. Only success, city and country code are requested.</p>
     *
     * @return lookup URL, or {@code null} when the configuration is {@code DISABLED}
     */
    public static String getLookupUrl() {
        if (!isEnabled()) return null;
        String key = java.util.Objects.toString(Constants.getString("adminLoginLocationApiKey"), "").trim();
        return (key.isEmpty() ? "https://ipwho.is/?" : "https://ipwhois.pro/?key="
            + URLEncoder.encode(key, StandardCharsets.UTF_8) + "&") + "fields=success,city,country_code";
    }

    /**
     * Validates a pre-login hint and stages it in the session with the current server time.
     *
     * <p>The controller calls this for an unauthenticated session. A valid hint replaces any
     * pending hint; disabled lookup or invalid input leaves the session unchanged. The hint
     * becomes the login location only when {@link #beginLogin(HttpSession)} consumes it.</p>
     *
     * @param session non-null pre-login session to hold the pending hint
     * @param input browser-supplied city and country code; {@code null} or invalid input is ignored
     */
    public static void prepare(HttpSession session, Input input) {
        if (!isEnabled()) return;
        String location = validate(input);
        if (location != null) session.setAttribute(PENDING, new Pending(location, System.currentTimeMillis()));
    }

    /**
     * Starts a successful administrator login by consuming its pending location hint once.
     *
     * <p>Called after all required authentication steps and before device recording and notification.
     * The pending hint is removed regardless of validity. It becomes the current location only if
     * lookup is enabled and it is at most ten minutes old; otherwise any previous location is cleared.
     * The retry marker and device login timestamp are also cleared for the new login.</p>
     *
     * @param session non-null authenticated session containing any preserved pre-login hint
     */
    public static void beginLogin(HttpSession session) {
        Object pending = session.getAttribute(PENDING);
        session.removeAttribute(PENDING);
        session.removeAttribute(RETRIED);
        session.removeAttribute(DEVICE_LOGIN_TIME);
        String location = isEnabled() && pending instanceof Pending value
            && System.currentTimeMillis() - value.receivedAt() <= MAX_AGE ? value.location() : null;
        if (location == null) session.removeAttribute(LOCATION);
        else session.setAttribute(LOCATION, location);
    }

    public static String getLocation(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(LOCATION);
    }

    /**
     * Reserves one browser lookup retry when rendering an administration page without a location.
     *
     * <p>The reservation is recorded before returning the URL, so later page renders do not retry
     * even if the browser lookup fails. {@link #beginLogin(HttpSession)} resets this marker for
     * the next successful login.</p>
     *
     * @param session current administrator session, or {@code null} when no session exists
     * @return lookup URL for the reserved attempt, or {@code null} if the session is absent,
     *         lookup is disabled, a location is already present or a retry was already reserved
     */
    public static String takeRetryUrl(HttpSession session) {
        if (session == null) return null;
        if (!isEnabled() || getLocation(session) != null || session.getAttribute(RETRIED) != null) return null;
        session.setAttribute(RETRIED, Boolean.TRUE);
        return getLookupUrl();
    }

    /**
     * Validates and normalizes browser input into display text such as {@code Bratislava, SK}.
     *
     * <p>The trimmed city must contain 1 to 156 characters and no ISO control characters.
     * The trimmed, uppercased country code must contain exactly two ASCII letters. These checks
     * bound the input but do not verify the location or escape HTML; callers must render it as text.</p>
     *
     * @param input untrusted city and country code; may be {@code null}
     * @return normalized {@code city, CC} text, or {@code null} if either field is missing or invalid
     */
    public static String validate(Input input) {
        if (input == null || input.city() == null || input.countryCode() == null) return null;
        String city = input.city().trim();
        String country = input.countryCode().trim().toUpperCase(Locale.ROOT);
        if (city.isEmpty() || city.length() > 156 || city.chars().anyMatch(Character::isISOControl)
            || !country.matches("[A-Z]{2}")) return null;
        return city + ", " + country;
    }

    /**
     * Applies a location to the HTTP session and any existing session details after a fallback lookup.
     *
     * <p>If session details exist, their location is updated and administrator details trigger a
     * cluster summary update. This method does not validate input or update the persisted device;
     * the controller handles validation and delegates the device update to {@link DeviceService}.</p>
     *
     * @param session non-null session whose location is being updated
     * @param location validated display text, or {@code null} to clear the location
     */
    public static void setLocation(HttpSession session, String location) {
        if (location == null) session.removeAttribute(LOCATION);
        else session.setAttribute(LOCATION, location);
        SessionDetails details = SessionHolder.getInstance().get(session.getId());
        if (details != null) {
            details.setLocation(location);
            if (details.isAdmin()) SessionClusterService.updateSessionData();
        }
    }
}
