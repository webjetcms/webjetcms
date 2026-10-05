package sk.iway.iwcm.users.devices;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.users.UsersDB;

/** Integrates shared device history with administrator authentication, cookies, notices and email links. */
@Service
public class AdminDeviceService {
    static final String COOKIE_NAME = "wjAdminDevice";
    private static final String REQUEST_MARKER = AdminDeviceService.class.getName() + ".recorded";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final Pattern EVENT_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final SecureRandom RANDOM = new SecureRandom();
    private final DeviceService devices;
    private final Clock clock;

    @Autowired
    public AdminDeviceService(DeviceService devices) {
        this(devices, Clock.systemUTC());
    }

    AdminDeviceService(DeviceService devices, Clock clock) {
        this.devices = devices;
        this.clock = clock;
    }

    /**
     * Records a completed interactive login without allowing notification failures to deny access.
     * Call only after password policy and all required authentication factors have succeeded.
     *
     * @param user authenticated administrator
     * @param request successful authentication request
     * @param response response receiving the persistent browser cookie
     */
    public static void recordSuccessfulLogin(Identity user, HttpServletRequest request, HttpServletResponse response) {
        if (user == null || !user.isAdmin() || user.getUserId() <= 0 || request == null || response == null
                || !Constants.getBoolean("adminNewDeviceDetectionEnabled")) return;
        try {
            Tools.getSpringBean("adminDeviceService", AdminDeviceService.class).record(user, request, response);
        } catch (Exception exception) {
            Logger.error(AdminDeviceService.class, "Cannot record administrator device login (" + exception.getClass().getSimpleName() + ")");
        }
    }

    /** Applies the account-specific inactivity window once per completed authentication request. */
    void record(Identity user, HttpServletRequest request, HttpServletResponse response) {
        int domainId = UsersDB.getDomainId();
        String account = domainId + ":" + user.getUserId();
        synchronized (request) {
            if (account.equals(request.getAttribute(REQUEST_MARKER))) return;
            request.setAttribute(REQUEST_MARKER, account);
            String token = readToken(request);
            if (token == null) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            }
            long now = clock.millis();
            int maxAgeDays = maxAgeDays();
            BrowserDetector browser = new BrowserDetector(request.getHeader("User-Agent"));
            String operatingSystem = join(browser.getBrowserPlatform(), browser.getBrowserSubplatform());
            LoginEvent event = devices.recordLogin(user.getUserId(), domainId, hashToken(token), now,
                now - Duration.ofDays(maxAgeDays).toMillis(), bounded(browser.getBrowserName(), 128), bounded(browser.getBrowserVersion(), 64),
                bounded(operatingSystem, 128), bounded(Tools.getRemoteIP(request), 64));

            Cookie cookie = new Cookie(COOKIE_NAME, token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setSecure(Tools.isSecure(request));
            cookie.setAttribute("SameSite", "Lax");
            cookie.setMaxAge((int) Duration.ofDays(maxAgeDays).toSeconds());
            Tools.addCookie(cookie, response, request);
            if (event != null) sendNotification(user, request, event);
        }
    }

    /** Returns the configured inactivity window, bounded to a valid cookie lifetime. */
    static int maxAgeDays() {
        int configured = Constants.getInt("adminNewDeviceMaxAgeDays");
        return configured > 0 ? Math.min(configured, Integer.MAX_VALUE / 86_400) : 90;
    }

    /** Returns outstanding account-owned warnings; acknowledgement does not affect recognition. */
    public List<LoginEvent> activeEvents(Identity user) {
        requireAdministrator(user);
        if (!Constants.getBoolean("adminNewDeviceDetectionEnabled")) return List.of();
        return devices.findActive(user.getUserId(), UsersDB.getDomainId(), clock.millis());
    }

    /** Looks up a retained event without exposing whether another account owns its identifier. */
    public LoginEvent findEvent(Identity user, String eventId) {
        requireAdministrator(user);
        if (!Constants.getBoolean("adminNewDeviceDetectionEnabled") || !validEventId(eventId)) return null;
        return devices.findEvent(user.getUserId(), UsersDB.getDomainId(), eventId, clock.millis());
    }

    /** Confirms an owned event without extending the device's last-login time. */
    public LoginEvent confirm(Identity user, String eventId) {
        requireAdministrator(user);
        if (!Constants.getBoolean("adminNewDeviceDetectionEnabled") || !validEventId(eventId)) return null;
        return devices.confirm(user.getUserId(), UsersDB.getDomainId(), eventId, clock.millis());
    }

    /** Reports an owned event and forgets only that account's recognition of its browser. */
    public LoginEvent report(Identity user, String eventId) {
        requireAdministrator(user);
        if (!Constants.getBoolean("adminNewDeviceDetectionEnabled") || !validEventId(eventId)) return null;
        return devices.report(user.getUserId(), UsersDB.getDomainId(), eventId, clock.millis());
    }

    /**
     * Consumes a local administration return target, including a security-event query parameter.
     *
     * @param request authentication request with the saved return target in its session
     * @return validated local target or the administration overview
     */
    public static String getAfterLoginRedirect(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return "/admin/v9/";
        Object saved = session.getAttribute("adminAfterLogonRedirect");
        session.removeAttribute("adminAfterLogonRedirect");
        if (!(saved instanceof String target)) return "/admin/v9/";
        try {
            String decoded = URLDecoder.decode(target, StandardCharsets.UTF_8);
            if (decoded.chars().anyMatch(character -> character < 32 || character == 127 || character == '\\')) return "/admin/v9/";
            URI uri = URI.create(target);
            String path = uri.getPath();
            if (uri.isAbsolute() || uri.getRawAuthority() != null || path == null || !path.equals(uri.normalize().getPath())
                    || !path.startsWith("/") || path.matches(".*(?:^|/)\\.{1,2}(?:/|$).*")) return "/admin/v9/";
            if (path.startsWith("/admin/v9/") || path.startsWith("/admin/approve")
                    || (path.startsWith("/apps/") && path.contains("/admin/"))
                    || (path.startsWith("/components/") && path.contains("/admin"))) return target;
        } catch (IllegalArgumentException exception) {
            return "/admin/v9/";
        }
        return "/admin/v9/";
    }

    private static String readToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null
                    && TOKEN_PATTERN.matcher(cookie.getValue()).matches()) return cookie.getValue();
        }
        return null;
    }

    static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static boolean validEventId(String eventId) {
        return eventId != null && EVENT_PATTERN.matcher(eventId).matches();
    }

    private static void requireAdministrator(Identity user) {
        if (user == null || !user.isAdmin() || user.getUserId() <= 0) throw new AccessDeniedException("Administrator login is required");
    }

    private static String join(String first, String second) {
        return (first == null ? "" : first) + (second == null || second.isBlank() ? "" : " " + second);
    }

    private static String bounded(String value, int length) {
        if (value == null) return "";
        return value.substring(0, Math.min(value.length(), length));
    }

    /** Queues one localized message through the existing mail sender after persistence succeeds. */
    void sendNotification(Identity user, HttpServletRequest request, LoginEvent event) {
        if (Tools.isEmpty(user.getEmail())) {
            Logger.error(AdminDeviceService.class, "Cannot notify administrator without an email address: " + user.getUserId());
            return;
        }
        Prop prop = Prop.getInstance(request);
        String baseHref = Tools.getBaseHref(request);
        String link = baseHref.replaceAll("/+$", "") + "/admin/v9/?securityEvent=" + event.id();
        String domain = Tools.getServerName(request);
        String environment = Constants.getStringExecuteMacro("dashboardEnvironmentName");
        String fromName = SendMail.getDefaultSenderName("passwordReset", Tools.getRequestAttribute(request, "sendPasswordFromName", user.getFullName()));
        String fromEmail = SendMail.getDefaultSenderEmail("passwordReset", Tools.getRequestAttribute(request, "sendPasswordFromEmail", user.getEmail()));
        String message = "<!doctype html><html><body style=\"font-family:Arial,sans-serif;color:#272727\">"
            + "<p>" + Tools.escapeHtml(prop.getText("admin.newDevice.email.greeting", user.getFirstName())) + "</p>"
            + "<p>" + Tools.escapeHtml(prop.getText("admin.newDevice.email.intro")) + "</p>"
            + "<div style=\"background:#f3f3f6;padding:16px;border-radius:6px\">"
            + emailLine(prop, "device", join(event.browserName(), event.browserVersion()) + " · " + event.operatingSystem())
            + emailLine(prop, "ip", event.ipAddress())
            + emailLine(prop, "time", Tools.formatDateTime(event.createdAt()))
            + emailLine(prop, "environment", domain + (Tools.isEmpty(environment) ? "" : " (" + environment + ")"))
            + "</div><p>" + Tools.escapeHtml(prop.getText("admin.newDevice.email.instruction")) + "</p>"
            + "<p><a style=\"display:inline-block;background:#e00028;color:#fff;padding:12px 16px;border-radius:6px;text-decoration:none\" href=\""
            + Tools.escapeHtml(link) + "\">" + Tools.escapeHtml(prop.getText("admin.newDevice.email.action")) + "</a></p></body></html>";
        boolean queued = SendMail.sendLater(fromName, fromEmail, user.getEmail(), null, null, null,
            prop.getText("admin.newDevice.email.subject", domain), message, baseHref, null, null);
        if (!queued) Logger.error(AdminDeviceService.class, "Cannot queue new-device notification for user " + user.getUserId());
    }

    private static String emailLine(Prop prop, String key, String value) {
        return "<p style=\"margin:0 0 8px\">" + Tools.escapeHtml(prop.getText("admin.newDevice.email." + key))
            + ": " + Tools.escapeHtml(value == null ? "" : value) + "</p>";
    }
}
