package sk.iway.iwcm.admin.layout;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.users.UsersDB;

/** Supplies the same permission-filtered diagnostics to the Help menu and feedback email. */
public class SystemInfo {
    @Getter private final String versionNumber;
    @Getter private final String buildDate;
    @Getter private final String server;
    @Getter private final String browser;
    private final String versionDetails;
    private final String domain;
    private final String environment;
    private final Prop prop;

    /** Reads diagnostics for the current user and the selected content domain. */
    public SystemInfo(HttpServletRequest request, String currentDomain) {
        prop = Prop.getInstance(request);
        String[] version = InitServlet.getActualVersion().split(" ", 3);
        String date = version.length > 1 ? version[1] : "";
        try {
            date = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                .format(DateTimeFormatter.ofPattern("d. M. yyyy"));
        } catch (DateTimeParseException ignored) { /* Unexpanded development versions retain their original date. */ }
        versionNumber = version[0];
        buildDate = date + (version.length > 2 ? " " + version[2] : "");
        versionDetails = version[0] + (buildDate.isEmpty() ? "" : ", " + buildDate);
        server = getServer(UsersDB.getCurrentUser(request));
        environment = new EnvironmentBadge().getName();
        domain = currentDomain;
        BrowserDetector detector = BrowserDetector.getInstance(request);
        String platform = detector.getBrowserPlatform();
        if ("Mac OS X".equals(platform)) platform = "macOS";
        browser = detector.getBrowserName() + " " + detector.getBrowserVersionShort() + " · " + platform;
    }

    /** Returns the cluster node only to users allowed to manage system configuration. */
    public static String getServer(Identity user) {
        if (user == null || !user.isEnabledItem("menuConfig")) return "";
        String node = System.getProperty("iway.tomcat");
        return Tools.isNotEmpty(node) ? node : Constants.getString("clusterMyNodeName");
    }

    /** Returns installation diagnostics shared by the clipboard and feedback. */
    public String getSummary() {
        return "WebJET CMS (" + InitServlet.getProductEdition() + ")\n"
            + prop.getText("admin.help.version.js") + ": " + versionDetails + "\n"
            + (server.isEmpty() ? "" : prop.getText("admin.help.server.js") + ": " + server + "\n")
            + prop.getText("admin.help.domain.js") + ": " + domain + (environment.isEmpty() ? "" : " (" + environment + ")");
    }

    /** Adds the browser origin when the domains differ, retaining the page path, query and fragment. */
    public String getText(String pageUrl) {
        String pageAddress = pageUrl;
        String cmsDomain = null;
        String cmsOrigin = null;
        try {
            URI uri = URI.create(pageUrl);
            cmsDomain = uri.getHost();
            String scheme = uri.getScheme();
            cmsOrigin = (scheme == null ? "" : scheme + "://") + cmsDomain;
            int port = uri.getPort();
            boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
            if (port >= 0 && !defaultPort) cmsOrigin += ":" + port;
            pageAddress = uri.getRawPath() == null ? "" : uri.getRawPath();
            if (uri.getRawQuery() != null) pageAddress += "?" + uri.getRawQuery();
            if (uri.getRawFragment() != null) pageAddress += "#" + uri.getRawFragment();
        } catch (IllegalArgumentException ignored) { /* Preserve addresses from legacy clients that are not valid URIs. */ }
        String cmsDomainLine = Tools.isNotEmpty(cmsDomain) && !domain.equalsIgnoreCase(cmsDomain)
            ? "\n" + prop.getText("admin.help.cmsDomain.js") + ": " + cmsOrigin : "";
        return getSummary() + cmsDomainLine + "\n" + prop.getText("admin.help.pagePath.js") + ": " + pageAddress
            + "\n" + prop.getText("admin.help.browser.js") + ": " + browser;
    }
}
