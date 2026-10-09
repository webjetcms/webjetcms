package sk.iway.iwcm.users.devices;

import java.io.Serializable;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.stat.SessionDetails;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/** Integrates shared device history with administrator authentication, cookies, notices and email links. */
@Service
public class AdminDeviceService {
    public static final String SESSION_DEVICE_ID = "wjdeviceId";
    public static final String VERIFICATION_URL = "/admin/logon/device/";
    public static final String PENDING_LOGIN = "adminUser_waitingForDevice";
    public static final String VERIFICATION_MESSAGE = "adminDeviceVerificationMessage";
    public static final String SECOND_FACTOR_VERIFIED = "adminDeviceSecondFactorVerified";
    static final String COOKIE_NAME = "wjdevice";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final SecureRandom RANDOM = new SecureRandom();
    // Keep E2E exclusions immutable and outside runtime configuration.
    private static final Set<String> IGNORED_DEVICE_DOMAINS = Set.of("demo.webjetcms.sk", "*.interway.sk", "localhost");
    private static final Set<String> IGNORED_DEVICE_USER_AGENTS = Set.of(
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/131.0.6778.33 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/131.0.6778.33 Safari/537.36"
    );
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

    /** Holds the authenticated identity outside both login contexts until the email code succeeds. */
    public record PendingLogin(Identity user, long deviceId, String tokenHash, String nonce, long expiresAt) implements Serializable { }

    /** Checks browser blocking after credentials/factors and before any successful-login callbacks. */
    public static boolean requireVerification(HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin() || user.getUserId() <= 0
            || !Constants.getBoolean("adminNewDeviceDetectionEnabled")) return false;
        if (readToken(request) == null) return false;
        try {
            return Tools.getSpringBean("adminDeviceService", AdminDeviceService.class).prepareVerification(user, request);
        } catch (RuntimeException exception) {
            LogonTools.clearUserFromSession(request.getSession());
            throw exception;
        }
    }

    /** Leaves ordinary browsers alone; test-browser exclusions do not bypass an existing block. */
    boolean prepareVerification(Identity user, HttpServletRequest request) {
        String token = readToken(request);
        if (token == null) return false;
        String tokenHash = hashToken(token);
        DeviceEntity device = deviceService.findByTokenHash(user.getUserId(), tokenHash);
        if (device == null || device.getReportedAt() == null) return false;
        HttpSession session = request.getSession();
        PendingLogin pending = getPendingLogin(request);
        if (pending == null || pending.user().getUserId() != user.getUserId() || pending.deviceId() != device.getId()) {
            byte[] nonce = new byte[32];
            RANDOM.nextBytes(nonce);
            pending = new PendingLogin(user, device.getId(), tokenHash, Base64.getUrlEncoder().withoutPadding().encodeToString(nonce),
                clock.millis() + Duration.ofMinutes(15).toMillis());
            session.setAttribute(PENDING_LOGIN, pending);
        }
        LogonTools.clearUserFromSession(session);
        requestUnblockCode(request);
        return true;
    }

    /** Returns only a live challenge for the browser which started the login. */
    public PendingLogin getPendingLogin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(PENDING_LOGIN) instanceof PendingLogin pending)) return null;
        String token = readToken(request);
        if (pending.expiresAt() <= clock.millis() || token == null || !pending.tokenHash().equals(hashToken(token))) {
            session.removeAttribute(PENDING_LOGIN);
            session.removeAttribute(VERIFICATION_MESSAGE);
            return null;
        }
        return pending;
    }

    /** Sends a login code using the existing device fields, mail layout and limits. */
    public void requestUnblockCode(HttpServletRequest request) {
        PendingLogin pending = getPendingLogin(request);
        if (pending == null) return;
        String message = "admin.dashboard.newDevice.codeSendError.js";
        try {
            DeviceEntity device = deviceService.findEvent(pending.user().getUserId(), pending.deviceId());
            if (device != null && device.getReportedAt() != null && Tools.isNotEmpty(pending.user().getEmail())) {
                String code = String.format(java.util.Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
                if (!deviceService.issueUnblockCode(pending.user().getUserId(), pending.deviceId(), unblockHash(pending, code), clock.millis()))
                    message = "admin.logon.device.codeWait";
                else if (sendVerificationEmail(pending.user(), request, device, null, code))
                    message = "admin.dashboard.newDevice.codeSent.js";
            }
        } catch (RuntimeException exception) {
            Logger.error(AdminDeviceService.class, "Cannot send device unblock code", exception);
        }
        request.getSession().setAttribute(VERIFICATION_MESSAGE, message);
    }

    /** Consumes a session-bound code, reloads the account and restores authentication only on success. */
    public boolean verifyUnblockCode(HttpServletRequest request, String code) {
        PendingLogin pending = getPendingLogin(request);
        if (pending == null || code == null || !code.matches("[0-9]{6}")) return false;
        UserDetails account = UsersDB.getUser(pending.user().getUserId());
        if (account == null || !account.isAdmin() || !account.isAuthorized()
            || !java.util.Objects.equals(account.getEmail(), pending.user().getEmail())) return false;
        if (deviceService.unblock(account.getUserId(), pending.deviceId(), unblockHash(pending, code), clock.millis()) == null) return false;
        HttpSession session = request.getSession();
        session.removeAttribute(PENDING_LOGIN);
        session.removeAttribute(VERIFICATION_MESSAGE);
        request.changeSessionId();
        Identity user = new Identity(account);
        user.setValid(true);
        LogonTools.logonUserWithAllChecks(user, request);
        return true;
    }

    private static String unblockHash(PendingLogin pending, String code) {
        return hashToken("device-unblock:" + pending.user().getUserId() + ":" + pending.deviceId() + ":" + pending.nonce() + ":" + code);
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
        request.getSession().removeAttribute(SESSION_DEVICE_ID);
        if (isIgnoredDevice(request)) return;
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
        String tokenHash = hashToken(token);
        DeviceEntity event = deviceService.recordLogin(user.getUserId(), tokenHash, now,
            now - Duration.ofDays(maxAgeDays).toMillis(), bounded(browser.getBrowserName(), 128), bounded(browser.getBrowserVersion(), 64),
            bounded(operatingSystem, 128), bounded(Tools.getRemoteIP(request), 64), AdminLoginLocation.getLocation(request.getSession()));

        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(Tools.isSecure(request));
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge((int) Duration.ofDays(maxAgeDays).toSeconds());
        Tools.addCookie(cookie, response, request);
        DeviceEntity device = event != null ? event : deviceService.findByTokenHash(user.getUserId(), tokenHash);
        if (device != null && Boolean.TRUE.equals(request.getAttribute(SECOND_FACTOR_VERIFIED))) {
            device = deviceService.confirmAfterSecondFactor(user.getUserId(), device.getId(), now);
            if (event != null) event = device;
        }
        if (device != null) {
            request.getSession().setAttribute(SESSION_DEVICE_ID, device.getId());
            request.getSession().setAttribute(AdminLoginLocation.DEVICE_LOGIN_TIME, now);
        }
        if (event != null) sendNotification(user, request, event);
    }

    /** Matches a complete User-Agent and login hostname against immutable E2E exclusions. */
    private static boolean isIgnoredDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || !IGNORED_DEVICE_USER_AGENTS.contains(userAgent)) return false;

        String serverName = Tools.getServerName(request, false);
        if (serverName == null) return false;
        for (String domain : IGNORED_DEVICE_DOMAINS) {
            if (domain.startsWith("*.")) {
                String suffix = domain.substring(1);
                if (serverName.length() > suffix.length() && serverName.endsWith(suffix)) return true;
            } else if (serverName.equals(domain)) {
                return true;
            }
        }
        return false;
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

    /** Returns a bounded page of the authenticated administrator's devices, most recently used first. */
    public Page<DeviceEntity> getDevices(Identity user, int page) {
        requireAdministrator(user);
        var pageable = PageRequest.of(Math.max(0, page), 20, Sort.by(Sort.Direction.DESC, "lastSeen", "id"));
        if (!Constants.getBoolean("adminNewDeviceDetectionEnabled")) return Page.empty(pageable);
        return deviceService.findDevices(user.getUserId(), pageable);
    }

    /** Confirms an owned event only with the secret delivered to the account's email address. */
    public DeviceEntity confirm(Identity user, String eventId, String proof, boolean code) {
        requireAdministrator(user);
        long id = deviceId(eventId);
        if (!Constants.getBoolean("adminNewDeviceDetectionEnabled") || id <= 0) return null;
        if (proof == null || !(code ? proof.matches("[0-9]{6}") : TOKEN_PATTERN.matcher(proof).matches())) return null;
        return deviceService.confirm(user.getUserId(), id, confirmationHash(user.getUserId(), id, proof, code), clock.millis(), code);
    }

    /** Sends a new code without exposing it in the response or changing device trust. */
    public void requestCode(Identity user, String eventId, HttpServletRequest request) {
        DeviceEntity device = findEvent(user, eventId);
        if (device == null || device.getConfirmedAt() != null || device.getReportedAt() != null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (Tools.isEmpty(user.getEmail())) throw new ResponseStatusException(HttpStatus.CONFLICT);
        String code = String.format(java.util.Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        if (!deviceService.issueConfirmation(user.getUserId(), device.getId(),
                confirmationHash(user.getUserId(), device.getId(), code, true), clock.millis(), true))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        if (!sendVerificationEmail(user, request, device, null, code)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
    }

    static String confirmationHash(int userId, long id, String proof, boolean code) {
        return hashToken((code ? "device-code:" : "device-link:") + userId + ":" + id + ":" + proof);
    }

    /** Blocks the owned browser and signs out its known sessions using the ordinary logout mechanism. */
    public DeviceEntity report(Identity user, String eventId) {
        requireAdministrator(user);
        long id = deviceId(eventId);
        if (!Constants.getBoolean("adminNewDeviceDetectionEnabled") || id <= 0) return null;
        DeviceEntity device = deviceService.report(user.getUserId(), id, clock.millis());
        if (device != null) {
            Set<String> sessionIds = new HashSet<>();
            for (SessionDetails session : SessionHolder.getInstance().getList()) {
                if (session.getLoggedUserId() == user.getUserId() && Long.valueOf(id).equals(session.getDeviceId()))
                    sessionIds.add(session.getSessionId());
            }
            for (SessionDetails session : SessionClusterService.getSessionsForUsers(Set.of(user.getUserId()))) {
                if (Long.valueOf(id).equals(session.getDeviceId())) sessionIds.add(session.getSessionId());
            }
            for (String sessionId : sessionIds) SessionHolder.getInstance().invalidateSession(user.getUserId(), sessionId);
        }
        return device;
    }

    /** Adds fresh, account-owned confirmation state to session bootstrap data without exposing browser tokens. */
    public void addSessionDeviceStatus(Identity user, JsonNode sessionInfo) {
        requireAdministrator(user);
        if (!Constants.getBoolean("adminNewDeviceDetectionEnabled")) return;
        Set<Long> ids = new HashSet<>();
        for (JsonNode cluster : sessionInfo.path("userSessions")) {
            for (JsonNode session : cluster.path("userSessions")) {
                long id = session.path("deviceId").asLong();
                if (id > 0) ids.add(id);
            }
        }
        Map<Long, Boolean> confirmed = new HashMap<>();
        for (DeviceEntity device : deviceService.findByIds(user.getUserId(), ids)) {
            confirmed.put(device.getId(), device.getConfirmedAt() != null);
        }
        for (JsonNode cluster : sessionInfo.path("userSessions")) {
            for (JsonNode session : cluster.path("userSessions")) {
                Boolean state = confirmed.get(session.path("deviceId").asLong());
                if (state != null) ((ObjectNode) session).put("deviceConfirmed", state);
            }
        }
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
        if (event.getConfirmedAt() != null) {
            sendVerificationEmail(user, request, event, null, null);
            return;
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        if (!deviceService.issueConfirmation(user.getUserId(), event.getId(),
                confirmationHash(user.getUserId(), event.getId(), token, false), clock.millis(), false)) return;
        if (!sendVerificationEmail(user, request, event, token, null))
            Logger.error(AdminDeviceService.class, "Cannot queue new-device notification for user " + user.getUserId());
    }

    /** Reuses the notification layout and configured password-reset sender for both email proofs. */
    boolean sendVerificationEmail(Identity user, HttpServletRequest request, DeviceEntity event, String token, String code) {
        Prop prop = Prop.getInstance(request);
        String baseHref = Tools.getBaseHref(request);
        String link = baseHref + "/admin/v9/?securityEvent=" + event.getId();
        String domain = Tools.getServerName(request);
        String environment = Constants.getStringExecuteMacro("dashboardEnvironmentName");
        String fromName = SendMail.getDefaultSenderName("passwordReset", Tools.getRequestAttribute(request, "sendPasswordFromName", user.getFullName()));
        String fromEmail = SendMail.getDefaultSenderEmail("passwordReset", Tools.getRequestAttribute(request, "sendPasswordFromEmail", user.getEmail()));
        String actionStyle = "display:inline-block;color:#ffffff!important;-webkit-text-fill-color:#ffffff!important;padding:12px 16px;border-radius:6px;text-decoration:none;font-weight:bold;line-height:20px;text-align:center;";
        // Declare both schemes so Apple Mail uses our colors instead of automatically recoloring the button.
        String message = "<!doctype html><html><head><meta charset=\"UTF-8\">"
            + "<meta name=\"color-scheme\" content=\"light dark\"><meta name=\"supported-color-schemes\" content=\"light dark\">"
            + "<style>:root{color-scheme:light dark;supported-color-schemes:light dark}"
            + "@media(prefers-color-scheme:dark){"
            + ".email-body{background:#272727!important;color:#f3f3f6!important}"
            + ".email-details{background:#39393b!important;color:#f3f3f6!important}"
            + ".email-action{background:#e00028!important;color:#ffffff!important;-webkit-text-fill-color:#ffffff!important}"
            + ".email-action-confirm{background:#00856f!important}"
            + "}</style></head><body class=\"email-body\" style=\"font-family:Arial,sans-serif;background:#ffffff;color:#272727\">"
            + "<p>" + Tools.escapeHtml(prop.getText("admin.newDevice.email.greeting", user.getFirstName())) + "</p>"
            + "<p>" + Tools.escapeHtml(prop.getText(event.getReportedAt() == null ? "admin.newDevice.email.intro" : "admin.logon.device.emailIntro")) + "</p>"
            + "<div class=\"email-details\" style=\"background:#f3f3f6;padding:16px;border-radius:6px\">"
            + emailLine(prop, "device", join(event.getBrowserName(), event.getBrowserVersion()) + " · " + event.getOperatingSystem())
            + emailLine(prop, "location", event.getLocation() == null ? prop.getText("admin.dashboard.locationUnknown.js") : event.getLocation())
            + emailLine(prop, "ip", event.getIpAddress())
            + emailLine(prop, "time", Tools.formatDateTime(event.getLastSeen().toEpochMilli()))
            + emailLine(prop, "environment", domain + (Tools.isEmpty(environment) ? "" : " (" + environment + ")"))
            + "</div>" + (code == null ? (token == null ? "" : "<p>" + Tools.escapeHtml(prop.getText("admin.newDevice.email.linkExpiry")) + "</p>")
                : "<p>" + Tools.escapeHtml(prop.getText(event.getReportedAt() == null ? "admin.newDevice.email.codeIntro" : "admin.logon.device.emailCodeIntro")) + "</p><p style=\"font-size:28px;font-weight:bold;letter-spacing:4px\">"
                + code + "</p><p>" + Tools.escapeHtml(prop.getText("admin.newDevice.email.codeExpiry")) + "</p>")
            + "<p>" + Tools.escapeHtml(prop.getText(event.getConfirmedAt() == null ? "admin.newDevice.email.instruction" : "admin.newDevice.email.verifiedInstruction")) + "</p>"
            + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>"
            + (token != null ? "<td style=\"padding-right:8px\"><a class=\"email-action email-action-confirm\" style=\"" + actionStyle
                + "background:#00BE9F\" href=\"" + Tools.escapeHtml(link + "&deviceConfirmation=" + token) + "\">"
                + Tools.escapeHtml(prop.getText("admin.newDevice.email.confirm")) + "</a></td>" : "")
            + "<td><a class=\"email-action\" style=\"" + actionStyle + "background:#E00028\" href=\""
            + Tools.escapeHtml(link) + "\">" + Tools.escapeHtml(prop.getText("admin.newDevice.email.action")) + "</a></td></tr></table></body></html>";
        return SendMail.sendLater(fromName, fromEmail, user.getEmail(), null, null, null,
            prop.getText(code == null ? "admin.newDevice.email.subject" : "admin.newDevice.email.codeSubject", domain), message, baseHref, null, null);
    }

    private static String emailLine(Prop prop, String key, String value) {
        return "<p style=\"margin:0 0 8px\">" + Tools.escapeHtml(prop.getText("admin.newDevice.email." + key))
            + ": " + Tools.escapeHtml(value == null ? "" : value) + "</p>";
    }
}
