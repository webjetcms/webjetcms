package sk.iway.iwcm.components.welcome;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.stat.rest.BrowserIdentifierMigrationService;
import sk.iway.iwcm.system.ntlm.AuthenticationFilter;

/** Builds lightweight system notices for the dashboard template. */
@Service
public class DashboardNoticeService {
    private final UserDetailsRepository users;

    public DashboardNoticeService(UserDetailsRepository users) {
        this.users = users;
    }

    /**
     * Builds applicable system notices with trusted localized HTML and fixed application actions.
     * If an update error log exists, copies it to the protected download location used by its notice.
     *
     * @param user administrator whose permissions and two-factor authentication setup are checked
     * @param request request supplying the locale and optional Java version override for the tester account
     * @return notices containing an ID, severity, icon, title, HTML body and optional action
     * @throws AccessDeniedException if the user is missing or is not an administrator
     */
    public List<Map<String, Object>> load(Identity user, HttpServletRequest request) {
        if (user == null || !user.isAdmin()) throw new AccessDeniedException("Administrator login is required");
        Prop prop = Prop.getInstance(request);
        List<Map<String, Object>> notices = new ArrayList<>();

        if (Constants.getBoolean("2factorAuthEnabled") && Tools.isEmpty(Constants.getString("ldapProviderUrl"))
                && Tools.isEmpty(Constants.getString("adminLogonMethod")) && !AuthenticationFilter.weTrustIIS()) {
            String message = prop.getText("overview.2fa.warning");
            if (Tools.isNotEmpty(message) && message.length() > 2 && Tools.isEmpty(users.getMobileDeviceByUserId((long) user.getUserId()))) {
                notices.add(notice(prop, "twoFactor", "warning", "ti-shield-lock", message,
                    action(prop, "popup", "/admin/2factorauth.jsp", "button.setup")));
            }
        }
        if (!Constants.getBoolean("statWebJET7Converted")) {
            notices.add(notice(prop, "database", "warning", "ti-database", prop.getText("admin.update.databaseNotUpdatedToWebJET7"),
                action(prop, "link", "/admin/update/update_webjet7.jsp?act=fix", "admin.update.databaseNotUpdatedToWebJET7.startConversion")));
        }
        if (user.isEnabledItem("modUpdate|users.edit_admins") && !BrowserIdentifierMigrationService.isAllreadyUpdated()) {
            notices.add(notice(prop, "browserMigration", "warning", "ti-chart-bar", prop.getText("stat.browser-migration.warning"),
                action(prop, "link", "/admin/v9/settings/stat-browser-migration/", "stat.browser-migration.warning-start")));
        }

        IwcmFile logFile = new IwcmFile(Tools.getRealPath("/WEB-INF/update/error-log.txt"));
        if (logFile.exists()) {
            FileTools.copyFile(logFile, new IwcmFile(Tools.getRealPath("/files/protected/admin/error-log.txt")));
            notices.add(notice(prop, "update", "error", "ti-alert-circle", prop.getText("overview.update.showErrors"),
                action(prop, "link", "/files/protected/admin/error-log.txt", "admin.dashboard.notice.details.js")));
        }

        int requiredJavaVersion = Constants.getInt("javaMinimalVersion");
        String currentJavaVersion = System.getProperty("java.version");
        if ("tester".equals(user.getLogin()) && request.getParameter("javaVersion") != null) currentJavaVersion = request.getParameter("javaVersion");
        if (requiredJavaVersion > 0 && javaMajorVersion(currentJavaVersion) > 0 && javaMajorVersion(currentJavaVersion) < requiredJavaVersion) {
            notices.add(notice(prop, "java", "warning", "ti-server", prop.getText("system.javaVersionWarningText",
                Integer.toString(requiredJavaVersion), Tools.escapeHtml(currentJavaVersion)),
                action(prop, "help", "/install/versions", "admin.dashboard.notice.requirements.js")));
            notices.get(notices.size() - 1).put("description", prop.getText("admin.dashboard.notice.java.description.js",
                Integer.toString(requiredJavaVersion), currentJavaVersion));
        }

        long expiry = Constants.getLong("licenseExpiryDate");
        Calendar warningDate = Calendar.getInstance();
        warningDate.add(Calendar.MONTH, 2);
        if (expiry > 0 && warningDate.getTimeInMillis() >= expiry) {
            notices.add(notice(prop, "license", "warning", "ti-license", prop.getText("overview.license.expirationWarning",
                Tools.escapeHtml(Tools.formatDate(expiry))),
                action(prop, "link", "https://www.webjetcms.com", "admin.dashboard.notice.license.action.js")));
            notices.get(notices.size() - 1).put("description", prop.getText("admin.dashboard.notice.license.description.js", Tools.formatDate(expiry)));
        }
        if (Constants.getBoolean("useAmazonSES")) {
            notices.add(notice(prop, "amazonSes", "error", "ti-mail", prop.getText("overview.useAmazonSES.deprecated"),
                action(prop, "help", "/install/config/README", "menu.top.help")));
        }
        return notices;
    }

    private static Map<String, Object> notice(Prop prop, String id, String severity, String icon, String bodyHtml, Map<String, String> action) {
        Map<String, Object> notice = new LinkedHashMap<>();
        notice.put("id", id);
        notice.put("severity", severity);
        notice.put("icon", icon);
        notice.put("title", prop.getText("admin.dashboard.notice." + id + ".js"));
        notice.put("description", prop.getText("admin.dashboard.notice." + id + ".description.js"));
        notice.put("bodyHtml", bodyHtml);
        if (action != null) notice.put("action", action);
        return notice;
    }

    private static Map<String, String> action(Prop prop, String type, String url, String labelKey) {
        return Map.of("type", type, "url", url, "label", prop.getText(labelKey));
    }

    /**
     * Extracts the major Java version from current version strings or the legacy {@code 1.x} notation.
     *
     * @param version Java version string, or {@code null}
     * @return leading major version number, or {@code -1} if it cannot be parsed as an integer
     */
    static int javaMajorVersion(String version) {
        if (version == null) return -1;
        String normalized = version.startsWith("1.") ? version.substring(2) : version;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^[0-9]+").matcher(normalized);
        return matcher.find() ? Tools.getIntValue(matcher.group(), -1) : -1;
    }
}
