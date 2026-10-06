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

/** Integrates shared device history with administrator authentication, cookies, notices and email links. */
@Service
public class AdminDeviceService {
    static final String COOKIE_NAME = "wjdevice";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final SecureRandom RANDOM = new SecureRandom();
    private final DeviceService deviceService;
    private final Clock clock;

    @Autowired
    public AdminDeviceService(DeviceService deviceService) {
        this(deviceService, Clock.systemUTC());
    }

    AdminDeviceService(DeviceService deviceService, Clock clock) {
        this.deviceService = deviceService;
        this.clock = clock;
    }

    /**
     * Records a completed interactive login without allowing notification failures to deny access.
     * Called by {@link sk.iway.iwcm.common.LogonTools#afterSuccessLogon(HttpServletRequest, HttpServletResponse)}
     * after password policy and all required authentication factors have succeeded.
     *
     * @param user authenticated administrator
     * @param request successful authentication request
     * @param response response receiving the persistent browser cookie
     */
    public static void recordSuccessfulLogin(Identity user, HttpServletRequest request, HttpServletResponse response) {
        if (user == null || !user.isAdmin() || user.getUserId() <= 0 || request == null || response == null
                || !Constants.getBoolean("adminNewDeviceDetectionEnabled")) return;
        try {
            Tools.getSpringBean("adminDeviceService", AdminDeviceService.class).saveDataAndCookie(user, request, response);
        } catch (Exception exception) {
            Logger.error(AdminDeviceService.class, "Cannot record administrator device login (" + exception.getClass().getSimpleName() + ")");
        }
    }

    /** Applies the account-specific inactivity window and renews the browser cookie. */
    private void saveDataAndCookie(Identity user, HttpServletRequest request, HttpServletResponse response) {
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
        DeviceEntity event = deviceService.recordLogin(user.getUserId(), hashToken(token), now,
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

    /** Returns the configured inactivity window, bounded to a valid cookie lifetime. */
    static int maxAgeDays() {
        int configured = Constants.getInt("adminNewDeviceMaxAgeDays");
        return configured > 0 ? Math.min(configured, Integer.MAX_VALUE / 86_400) : 90;
    }

    /** Returns outstanding account-owned warnings; acknowledgement does not affect recognition. */
    public List<DeviceEntity> getActiveEvents(Identity user) {
        requireAdministrator(user);
        if (!Constants.getBoolean("adminNewDeviceDetectionEnabled")) return List.of();
        return deviceService.findActive(user.getUserId(), clock.millis());
    }

    /** Looks up the device's current notice without exposing whether another account owns it. */
    public DeviceEntity findEvent(Identity user, String eventId) {
        requireAdministrator(user);
        long id = deviceId(eventId);
        if (!Constants.getBoolean("adminNewDeviceDetectionEnabled") || id <= 0) return null;
        return deviceService.findEvent(user.getUserId(), id);
    }

    /** Confirms an owned event without extending the device's last-login time. */
    public DeviceEntity confirm(Identity user, String eventId) {
        requireAdministrator(user);
        long id = deviceId(eventId);
        if (!Constants.getBoolean("adminNewDeviceDetectionEnabled") || id <= 0) return null;
        return deviceService.confirm(user.getUserId(), id, clock.millis());
    }

    /** Reports an owned event and forgets only that account's recognition of its browser. */
    public DeviceEntity report(Identity user, String eventId) {
        requireAdministrator(user);
        long id = deviceId(eventId);
        if (!Constants.getBoolean("adminNewDeviceDetectionEnabled") || id <= 0) return null;
        return deviceService.report(user.getUserId(), id, clock.millis());
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

    private static long deviceId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
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
    void sendNotification(Identity user, HttpServletRequest request, DeviceEntity event) {
        if (Tools.isEmpty(user.getEmail())) {
            Logger.error(AdminDeviceService.class, "Cannot notify administrator without an email address: " + user.getUserId());
            return;
        }
        Prop prop = Prop.getInstance(request);
        String baseHref = Tools.getBaseHref(request);
        String link = baseHref + "/admin/v9/?securityEvent=" + event.getId();
        String domain = Tools.getServerName(request);
        String environment = Constants.getStringExecuteMacro("dashboardEnvironmentName");
        String fromName = SendMail.getDefaultSenderName("passwordReset", Tools.getRequestAttribute(request, "sendPasswordFromName", user.getFullName()));
        String fromEmail = SendMail.getDefaultSenderEmail("passwordReset", Tools.getRequestAttribute(request, "sendPasswordFromEmail", user.getEmail()));
        // Declare both schemes so Apple Mail uses our colors instead of automatically recoloring the button.
        String message = "<!doctype html><html><head><meta charset=\"UTF-8\">"
            + "<meta name=\"color-scheme\" content=\"light dark\"><meta name=\"supported-color-schemes\" content=\"light dark\">"
            + "<style>:root{color-scheme:light dark;supported-color-schemes:light dark}"
            + "@media(prefers-color-scheme:dark){"
            + ".email-body{background:#272727!important;color:#f3f3f6!important}"
            + ".email-details{background:#39393b!important;color:#f3f3f6!important}"
            + ".email-action{background:#e00028!important;color:#ffffff!important;-webkit-text-fill-color:#ffffff!important}"
            + "}</style></head><body class=\"email-body\" style=\"font-family:Arial,sans-serif;background:#ffffff;color:#272727\">"
            + "<p>" + Tools.escapeHtml(prop.getText("admin.newDevice.email.greeting", user.getFirstName())) + "</p>"
            + "<p>" + Tools.escapeHtml(prop.getText("admin.newDevice.email.intro")) + "</p>"
            + "<div class=\"email-details\" style=\"background:#f3f3f6;padding:16px;border-radius:6px\">"
            + emailLine(prop, "device", join(event.getBrowserName(), event.getBrowserVersion()) + " · " + event.getOperatingSystem())
            + emailLine(prop, "ip", event.getIpAddress())
            + emailLine(prop, "time", Tools.formatDateTime(event.getCreateDate().toEpochMilli()))
            + emailLine(prop, "environment", domain + (Tools.isEmpty(environment) ? "" : " (" + environment + ")"))
            + "</div><p>" + Tools.escapeHtml(prop.getText("admin.newDevice.email.instruction")) + "</p>"
            + "<p><a class=\"email-action\" style=\"display:inline-block;background:#e00028;color:#ffffff!important;-webkit-text-fill-color:#ffffff!important;padding:12px 16px;border-radius:6px;text-decoration:none\" href=\""
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
