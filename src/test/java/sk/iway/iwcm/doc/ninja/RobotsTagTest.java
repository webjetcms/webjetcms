package sk.iway.iwcm.doc.ninja;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import sk.iway.iwcm.components.response_header.rest.ResponseHeaderService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.doc.DocBasic.FollowLinksMode;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.test.BaseWebjetTest;

@Execution(ExecutionMode.SAME_THREAD)
class RobotsTagTest extends BaseWebjetTest {

    private String originalXRobotsTagUrls;
    private String originalReferrerPolicy;

    @BeforeEach
    void setUp() {
        originalXRobotsTagUrls = Constants.getString("xRobotsTagUrls");
        originalReferrerPolicy = Constants.getString("refererPolicy");
        Constants.setString("refererPolicy", "");
        Constants.setString("xRobotsTagUrls", "/components/,NOT_SEARCHABLE_PAGE");
    }

    @AfterEach
    void tearDown() {
        Constants.setString("xRobotsTagUrls", originalXRobotsTagUrls);
        Constants.setString("refererPolicy", originalReferrerPolicy);
    }

    @Test
    void robotsDirectivesUseOnlyRestrictiveValues() {
        assertRobots(true, FollowLinksMode.SEARCHABLE, "all");
        assertRobots(true, FollowLinksMode.FOLLOW, "all");
        assertRobots(true, FollowLinksMode.NOFOLLOW, "nofollow");
        assertRobots(false, FollowLinksMode.SEARCHABLE, "noindex, nofollow");
        assertRobots(false, FollowLinksMode.FOLLOW, "noindex");
        assertRobots(false, FollowLinksMode.NOFOLLOW, "noindex, nofollow");
    }

    @Test
    void xRobotsTagHeaderUsesDocumentSettings() {
        DocDetails doc = createDoc(false, FollowLinksMode.FOLLOW);
        MockHttpServletResponse response = new MockHttpServletResponse();

        PathFilter.setXRobotsTagValue("NOT_SEARCHABLE_PAGE", response, doc);

        assertEquals("noindex", response.getHeader("X-Robots-Tag"));

        response = new MockHttpServletResponse();
        doc.setSearchable(true);
        PathFilter.setXRobotsTagValue("NOT_SEARCHABLE_PAGE", response, doc);

        assertEquals("all", response.getHeader("X-Robots-Tag"));
    }

    /** The preview uses runtime rules and never writes its header or locale to the REST response. */
    @Test
    void previewMatchesRuntimeHeadersAndConfiguration() {
        String oldRules = Constants.getString("responseHeaders");
        String oldValue = Constants.getString("xRobotsTagValue");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader("X-Robots-Tag", "admin-only");
        var locale = response.getLocale();
        request.setAttribute("contentLanguageHeaderSet", "original");
        try (var custom = Mockito.mockStatic(ResponseHeaderService.class, Mockito.CALLS_REAL_METHODS)) {
            custom.when(() -> ResponseHeaderService.setResponseHeaders(Mockito.anyString(), Mockito.any(), Mockito.any())).thenAnswer(call -> null);
            Constants.setString("responseHeaders", "");
            PathFilter.resetResponseHeaders();
            for (boolean searchable : new boolean[] { true, false }) {
                for (FollowLinksMode mode : FollowLinksMode.values()) {
                    DocDetails doc = createDoc(searchable, mode);
                    MockHttpServletResponse runtime = new MockHttpServletResponse();
                    PathFilter.setXRobotsTagValue("/test/page.html", runtime);
                    PathFilter.setXRobotsTagValue("NOT_SEARCHABLE_PAGE", runtime, doc);
                    assertEquals(runtime.getHeader("X-Robots-Tag"), PathFilter.previewPageHeaders("/test/page.html", doc, request, response).get("X-Robots-Tag"));
                }
            }
            DocDetails doc = createDoc(false, FollowLinksMode.FOLLOW);
            Constants.setString("xRobotsTagUrls", "");
            assertTrue(PathFilter.previewPageHeaders("/test/page.html", doc, request, response).isEmpty());
            Constants.setString("xRobotsTagUrls", "/test/");
            Constants.setString("xRobotsTagValue", "noarchive");
            assertEquals(Map.of("X-Robots-Tag", "noarchive"), PathFilter.previewPageHeaders("/test/page.html", doc, request, response));
            custom.when(() -> ResponseHeaderService.setResponseHeaders(Mockito.anyString(), Mockito.any(), Mockito.any())).thenAnswer(call -> {
                jakarta.servlet.http.HttpServletResponse target = call.getArgument(2);
                target.setHeader("x-robots-tag", "nosnippet");
                target.setLocale(java.util.Locale.ENGLISH);
                request.setAttribute("contentLanguageHeaderSet", true);
                return null;
            });
            assertEquals(Map.of("X-Robots-Tag", "nosnippet"), PathFilter.previewPageHeaders("/test/page.html", doc, request, response));
            Constants.setString("xRobotsTagUrls", "NOT_SEARCHABLE_PAGE");
            assertEquals(Map.of("X-Robots-Tag", "noindex"), PathFilter.previewPageHeaders("/test/page.html", doc, request, response));
            assertEquals("admin-only", response.getHeader("X-Robots-Tag"));
            assertEquals(locale, response.getLocale());
            assertEquals("original", request.getAttribute("contentLanguageHeaderSet"));
        } finally {
            Constants.setString("responseHeaders", oldRules);
            Constants.setString("xRobotsTagValue", oldValue);
            PathFilter.resetResponseHeaders();
        }
    }

    /** Referrer overrides and redirects follow runtime precedence without altering the REST response. */
    @Test
    void previewIncludesReferrerPolicyAndRedirects() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        DocDetails doc = createDoc(false, FollowLinksMode.FOLLOW);
        Constants.setString("refererPolicy", "same-origin");
        String oldRules = Constants.getString("responseHeaders");
        Constants.setString("responseHeaders", "");
        PathFilter.resetResponseHeaders();
        try (var custom = Mockito.mockStatic(ResponseHeaderService.class, Mockito.CALLS_REAL_METHODS)) {
            custom.when(() -> ResponseHeaderService.setResponseHeaders(Mockito.anyString(), Mockito.any(), Mockito.any())).thenAnswer(call -> null);
            assertEquals(Map.of("X-Robots-Tag", "noindex", "Referrer-Policy", "same-origin"), PathFilter.previewPageHeaders("/test/page.html", doc, request, response));
            Constants.setString("responseHeaders", "/test/:Referrer-Policy:no-referrer");
            PathFilter.resetResponseHeaders();
            assertEquals("no-referrer", PathFilter.previewPageHeaders("/test/page.html", doc, request, response).get("Referrer-Policy"));
            doc.setExternalLink("https://example.com/target");
            assertEquals(Map.of("Referrer-Policy", "no-referrer", "Location", "https://example.com/target"), PathFilter.previewPageHeaders("/test/page.html", doc, request, response));
            request.setAttribute("path_filter_query_string", "campaign=test");
            assertEquals("https://example.com/target?campaign=test", PathFilter.previewPageHeaders("/test/page.html", doc, request, response).get("Location"));
            doc.setExternalLink("/target?existing=yes");
            assertEquals(sk.iway.iwcm.Tools.getBaseHref(request) + "/target?existing=yes", PathFilter.previewPageHeaders("/test/page.html", doc, request, response).get("Location"));
            doc.setExternalLink("/target");
            request.setAttribute("path_filter_query_string", "docid=25");
            assertEquals(sk.iway.iwcm.Tools.getBaseHref(request) + "/target", PathFilter.previewPageHeaders("/test/page.html", doc, request, response).get("Location"));
            assertNull(response.getHeader("Location"));
            assertNull(response.getHeader("Referrer-Policy"));
            assertEquals(200, response.getStatus());
        } finally {
            Constants.setString("responseHeaders", oldRules);
            PathFilter.resetResponseHeaders();
        }
    }

    /** Relative redirect previews follow the unsaved folder selection, preserving the URL's other parts. */
    @Test
    void redirectPreviewUsesFolderDomain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("cms.example.com");
        request.setScheme("https");
        request.setSecure(true);
        request.setServerPort(8443);
        request.setContextPath("/cms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        DocDetails doc = createDoc(true, FollowLinksMode.FOLLOW);
        doc.setExternalLink("/target?key=value#section");
        String originalServerName = Constants.getString("httpServerName");
        int originalHttpsPort = Constants.getInt("httpsServerPort");
        String originalRules = Constants.getString("responseHeaders");
        Constants.setString("httpServerName", "");
        Constants.setInt("httpsServerPort", -1);
        Constants.setString("responseHeaders", "");
        PathFilter.resetResponseHeaders();
        GroupsDB groups = Mockito.mock(GroupsDB.class);
        Mockito.when(groups.getDomain(101)).thenReturn("public.example.com");
        Mockito.when(groups.getDomain(202)).thenReturn("other.example.com");
        try (var groupCache = Mockito.mockStatic(GroupsDB.class);
             var custom = Mockito.mockStatic(ResponseHeaderService.class, Mockito.CALLS_REAL_METHODS)) {
            groupCache.when(GroupsDB::getInstance).thenReturn(groups);
            custom.when(() -> ResponseHeaderService.setResponseHeaders(Mockito.anyString(), Mockito.any(), Mockito.any())).thenAnswer(call -> null);
            doc.setGroupId(101);
            assertEquals("https://public.example.com:8443/cms/target?key=value#section", PathFilter.previewPageHeaders("/page.html", doc, request, response).get("Location"));
            assertEquals("https://cms.example.com:8443/cms/target?key=value#section", sk.iway.iwcm.doc.ShowDoc.getExternalLinkRedirectUrl(doc, request), "The live redirect must keep the request domain");
            doc.setGroupId(202);
            assertEquals("https://other.example.com:8443/cms/target?key=value#section", PathFilter.previewPageHeaders("/page.html", doc, request, response).get("Location"));
            doc.setGroupId(303);
            assertEquals("https://cms.example.com:8443/cms/target?key=value#section", PathFilter.previewPageHeaders("/page.html", doc, request, response).get("Location"));
            doc.setGroupId(101);
            doc.setExternalLink("https://external.example.com/target");
            assertEquals(doc.getExternalLink(), PathFilter.previewPageHeaders("/page.html", doc, request, response).get("Location"));
        } finally {
            Constants.setString("httpServerName", originalServerName);
            Constants.setInt("httpsServerPort", originalHttpsPort);
            Constants.setString("responseHeaders", originalRules);
            PathFilter.resetResponseHeaders();
        }
    }

    private void assertRobots(boolean searchable, FollowLinksMode followLinksMode, String expected) {
        DocDetails doc = createDoc(searchable, followLinksMode);
        assertEquals(expected, PathFilter.getXRobotsTagValue(doc));
        assertEquals(expected, createPage(doc).getRobots());
    }

    private DocDetails createDoc(boolean searchable, FollowLinksMode followLinksMode) {
        DocDetails doc = new DocDetails();
        doc.setSearchable(searchable);
        doc.setFollowLinksMode(followLinksMode);
        return doc;
    }

    private Page createPage(DocDetails doc) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("docDetails", doc);
        Ninja ninja = Mockito.mock(Ninja.class);
        Mockito.when(ninja.getRequest()).thenReturn(request);
        return new Page(ninja);
    }
}
