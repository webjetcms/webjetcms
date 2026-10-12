package sk.iway.iwcm.admin.layout;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.UsersDB;

/** Verifies diagnostic formatting and server disclosure for the Help menu and feedback. */
class SystemInfoTest extends BaseWebjetTest {
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final Identity user = new Identity();
    private final Map<String, String> original = new HashMap<>();
    private String originalTomcat;
    private MockedStatic<InitServlet> versions;
    private MockedStatic<UsersDB> users;
    private MockedStatic<BrowserDetector> browsers;

    @BeforeEach
    void setUp() {
        for (String key : new String[] { "dashboardEnvironmentName", "clusterMyNodeName" }) {
            original.put(key, Constants.getString(key));
        }
        originalTomcat = System.getProperty("iway.tomcat");
        System.setProperty("iway.tomcat", "pinboy3.tomcat_au4");
        Constants.setString("dashboardEnvironmentName", "DEV");
        Constants.setString("clusterMyNodeName", "autotest-node");
        request.setParameter("language", "sk");
        user.setDisabledItemsTable(new HashMap<>());
        versions = mockStatic(InitServlet.class);
        versions.when(InitServlet::getActualVersion).thenReturn("2026.18.28-jakarta.026 23.09.2026 14:16:41");
        versions.when(InitServlet::getProductEdition).thenReturn("Enterprise");
        users = mockStatic(UsersDB.class);
        users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
        BrowserDetector detector = mock(BrowserDetector.class);
        when(detector.getBrowserName()).thenReturn("Chrome");
        when(detector.getBrowserVersionShort()).thenReturn("153");
        when(detector.getBrowserPlatform()).thenReturn("Mac OS X");
        browsers = mockStatic(BrowserDetector.class);
        browsers.when(() -> BrowserDetector.getInstance(request)).thenReturn(detector);
    }

    @AfterEach
    void tearDown() {
        browsers.close();
        users.close();
        versions.close();
        original.forEach(Constants::setString);
        if (originalTomcat == null) System.clearProperty("iway.tomcat");
        else System.setProperty("iway.tomcat", originalTomcat);
    }

    /** Keeps the footer concise while retaining build seconds and selected domain in diagnostics. */
    @Test
    void formatsCompleteDiagnostics() {
        SystemInfo info = new SystemInfo(request, "demo.webjetcms.sk");
        assertEquals("2026.18.28-jakarta.026", info.getVersionNumber());
        assertEquals("23. 9. 2026 14:16:41", info.getBuildDate());
        assertEquals("WebJET CMS (Enterprise)\n"
            + "Verzia: 2026.18.28-jakarta.026, 23. 9. 2026 14:16:41\n"
            + "Server: pinboy3.tomcat_au4\n"
            + "Doména: demo.webjetcms.sk (DEV)\n"
            + "Stránka: /admin/v9/\n"
            + "Prehliadač: Chrome 153 · macOS", info.getText("/admin/v9/"));
    }

    /** Keeps both domains when different and preserves encoded query and fragment values. */
    @Test
    void addsCmsDomainAndCompletePageAddress() {
        SystemInfo info = new SystemInfo(request, "demo.webjetcms.sk");
        String diagnostics = info.getText("http://iwcm.interway.sk/admin/v9/?docid=123&value=a%2Fb#editor%23tab");
        assertTrue(diagnostics.contains("Doména: demo.webjetcms.sk (DEV)\nCMS doména: http://iwcm.interway.sk\n"));
        assertTrue(diagnostics.contains("Stránka: /admin/v9/?docid=123&value=a%2Fb#editor%23tab\n"));
    }

    /** Matches the browser origin, including non-default ports for both HTTP and HTTPS. */
    @Test
    void includesCmsProtocolAndPort() {
        SystemInfo info = new SystemInfo(request, "demo.webjetcms.sk");
        for (String[] origin : new String[][] {
            { "http://iwcm.interway.sk", "http://iwcm.interway.sk" },
            { "https://iwcm.interway.sk", "https://iwcm.interway.sk" },
            { "http://iwcm.interway.sk:8080", "http://iwcm.interway.sk:8080" },
            { "https://iwcm.interway.sk:8443", "https://iwcm.interway.sk:8443" },
            { "http://iwcm.interway.sk:80", "http://iwcm.interway.sk" },
            { "https://iwcm.interway.sk:443", "https://iwcm.interway.sk" }
        }) {
            assertTrue(info.getText(origin[0] + "/admin/v9/").contains("\nCMS doména: " + origin[1] + "\n"));
        }
    }

    /** Compares domain names without case differences or an administration port. */
    @Test
    void omitsMatchingCmsDomain() {
        SystemInfo info = new SystemInfo(request, "demo.webjetcms.sk");
        assertFalse(info.getText("http://DEMO.webjetcms.sk:8080/admin/v9/").contains("CMS doména:"));
    }

    /** Prevents node disclosure in both the visible footer getter and diagnostic text. */
    @Test
    void omitsServerWithoutSystemPermission() {
        user.addDisabledItem("menuConfig");
        SystemInfo info = new SystemInfo(request, "demo.webjetcms.sk");
        LayoutBean layout = new LayoutBean();
        layout.setUser(user);
        assertEquals("", layout.getProductServer());
        assertEquals("", info.getServer());
        assertFalse(info.getText("/admin/v9/").contains("Server:"));
        assertFalse(info.getSummary().contains("pinboy3"));
        assertEquals("", SystemInfo.getServer(null));
    }

    /** Uses the cluster node fallback and omits an environment that has no header badge. */
    @Test
    void supportsClusterFallbackAndHiddenEnvironment() {
        System.clearProperty("iway.tomcat");
        Constants.setString("dashboardEnvironmentName", "");
        SystemInfo info = new SystemInfo(request, "demo.webjetcms.sk");
        assertEquals("autotest-node", info.getServer());
        assertTrue(info.getSummary().endsWith("Doména: demo.webjetcms.sk"));
    }

    /** Retains versions without build metadata, including development placeholders. */
    @Test
    void supportsVersionsWithoutBuildDate() {
        versions.when(InitServlet::getActualVersion).thenReturn("autotest-version");
        SystemInfo info = new SystemInfo(request, "demo.webjetcms.sk");
        assertEquals("autotest-version", info.getVersionNumber());
        assertEquals("", info.getBuildDate());
    }
}
