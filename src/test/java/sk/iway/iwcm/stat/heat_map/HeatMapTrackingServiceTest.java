package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.stat.StatisticsMode;

/** Exercises the untrusted cookie boundary independently of the destination page. */
class HeatMapTrackingServiceTest {
    private static final String ID = "0123456789abcdef0123456789abcdef";
    private static final String NAME = HeatMapTrackingService.COOKIE_PREFIX + ID;
    private static final long NOW = 1789387200L;

    /** Invalid identifiers, coordinates, widths, timestamps, versions and oversized values are rejected. */
    @Test
    void validatesEveryCookieFieldAndAge() {
        assertEquals(new HeatMapEvent(ID, 123, 390, NOW, 42, 2500),
                HeatMapTrackingService.parseCookie(NAME, "v1.123.390." + NOW + ".42.2500", NOW));
        for (String value : new String[] { "v2.123.390." + NOW + ".42.2500", "v1.0.390." + NOW + ".42.2500",
                "v1.123.0." + NOW + ".42.2500", "v1.123.16385." + NOW + ".42.2500",
                "v1.123.390." + NOW + ".-1.2500", "v1.123.390." + NOW + ".1.1000001",
                "v1.123.390." + (NOW - 86401) + ".1.1", "v1.123.390." + (NOW + 301) + ".1.1",
                "v1.123.390." + NOW + ".1.1.extra", "x".repeat(129) }) {
            assertNull(HeatMapTrackingService.parseCookie(NAME, value, NOW), value);
        }
        assertNull(HeatMapTrackingService.parseCookie(NAME.toUpperCase(), "v1.123.390." + NOW + ".1.1", NOW));
    }

    /** Automatic processing cannot insert a second copy after a legacy JSP has emitted the tracker. */
    @Test
    void injectsOnceInCompleteHtmlOnly() {
        String script = "<script id=\"wj-heatmap-tracker\" src=\"/tracker.js\"></script>";
        String html = "<!doctype html><html><body>Page</BODY></html>";
        String result = HeatMapTrackingService.insertBeforeBodyEnd(html, script);
        assertTrue(result.contains(script + "</BODY>"));
        assertEquals(result, HeatMapTrackingService.insertBeforeBodyEnd(result, script));
        assertEquals("fragment", HeatMapTrackingService.insertBeforeBodyEnd("fragment", script));
    }

    /** Unrelated cookies and previews cause no database or configuration lookup. */
    @Test
    void ignoresRequestsWithoutEventsAndPreviews() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("ordinary", "value"));
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class)) {
            HeatMapTrackingService.receiveCookies(request, response);
            request.setCookies(new Cookie(NAME, "v1.123.390." + NOW + ".1.1"));
            request.setAttribute(HeatMapTrackingService.PREVIEW_ATTRIBUTE, true);
            HeatMapTrackingService.receiveCookies(request, response);
            constants.verifyNoInteractions();
            storage.verifyNoInteractions();
            assertEquals(0, response.getCookies().length);
        }
    }

    /** Consent withdrawal discards only received event cookies and never reaches storage. */
    @Test
    void clearsReceivedEventsAfterConsentWithdrawal() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/destination.html");
        request.setCookies(new Cookie(NAME, "v1.123.390." + NOW + ".1.1"), new Cookie("ordinary", "value"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class)) {
            constants.when(() -> Constants.getString("gdprCookieName")).thenReturn("consent");
            HeatMapTrackingService.receiveCookies(request, response);
            storage.verifyNoInteractions();
            assertEquals(1, response.getCookies().length);
            Cookie cleared = response.getCookies()[0];
            assertEquals(NAME, cleared.getName());
            assertEquals(0, cleared.getMaxAge());
            assertEquals("/", cleared.getPath());
            assertEquals("Lax", cleared.getAttribute("SameSite"));
            assertNull(cleared.getDomain());
        }
    }

    /** Source-page policy decides collection even when the destination is untracked; foreign domains are rejected. */
    @Test
    void validatesSourceAndHostThenConsumesRequestOnlyOnce() {
        long now = System.currentTimeMillis() / 1000;
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/apps/public/untracked.html");
        request.setServerName("public.example");
        request.setCookies(new Cookie(NAME, "v1.123.390." + now + ".42.2500"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        DocDetails document = mock(DocDetails.class);
        when(document.getGroupId()).thenReturn(5);
        when(document.isAvailable()).thenReturn(true);
        DocDB docs = mock(DocDB.class);
        when(docs.getBasicDocDetails(123, false)).thenReturn(document);
        GroupDetails group = mock(GroupDetails.class);
        when(group.getDomainName()).thenReturn("PUBLIC.EXAMPLE");
        GroupsDB groups = mock(GroupsDB.class);
        when(groups.getGroup(5)).thenReturn(group);
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<BrowserDetector> browsers = mockStatic(BrowserDetector.class);
                MockedStatic<DocDB> docLookup = mockStatic(DocDB.class);
                MockedStatic<GroupsDB> groupLookup = mockStatic(GroupsDB.class);
                MockedStatic<StatisticsMode> modes = mockStatic(StatisticsMode.class);
                MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class)) {
            constants.when(() -> Constants.getBoolean("gdprAllowAllCookies")).thenReturn(true);
            constants.when(() -> Constants.getBoolean("statEnableClickTracking")).thenReturn(true);
            browsers.when(() -> BrowserDetector.isStatIpAllowedFast(request)).thenReturn(true);
            docLookup.when(DocDB::getInstance).thenReturn(docs);
            groupLookup.when(GroupsDB::getInstance).thenReturn(groups);
            modes.when(() -> StatisticsMode.forDocument(document)).thenReturn(StatisticsMode.HEATMAP);
            HeatMapTrackingService.receiveCookies(request, response);
            HeatMapTrackingService.receiveCookies(request, response);
            storage.verify(() -> HeatMapStorage.record(new HeatMapEvent(ID, 123, 390, now, 42, 2500), "public.example"));
            assertEquals(1, response.getCookies().length);
            MockHttpServletRequest other = new MockHttpServletRequest("GET", "/untracked.html");
            other.setServerName("other.example");
            other.setCookies(request.getCookies());
            browsers.when(() -> BrowserDetector.isStatIpAllowedFast(other)).thenReturn(true);
            HeatMapTrackingService.receiveCookies(other, new MockHttpServletResponse());
            storage.verifyNoMoreInteractions();

            MockHttpServletRequest excluded = new MockHttpServletRequest("GET", "/untracked.html");
            excluded.setServerName("public.example");
            excluded.setCookies(request.getCookies());
            MockHttpServletResponse excludedResponse = new MockHttpServletResponse();
            HeatMapTrackingService.receiveCookies(excluded, excludedResponse);
            storage.verifyNoMoreInteractions();
            assertEquals(1, excludedResponse.getCookies().length, "Excluded IP events are discarded");

            storage.clearInvocations();
            MockHttpServletRequest overflow = new MockHttpServletRequest("GET", "/public.html");
            overflow.setServerName("public.example");
            Cookie[] queue = new Cookie[18];
            for (int index = 0; index < queue.length; index++) {
                queue[index] = new Cookie(HeatMapTrackingService.COOKIE_PREFIX + String.format("%032x", index),
                        "v1.123.390." + (now - 18 + index) + ".42.2500");
            }
            overflow.setCookies(queue);
            browsers.when(() -> BrowserDetector.isStatIpAllowedFast(overflow)).thenReturn(true);
            MockHttpServletResponse overflowResponse = new MockHttpServletResponse();
            HeatMapTrackingService.receiveCookies(overflow, overflowResponse);
            storage.verify(() -> HeatMapStorage.record(any(HeatMapEvent.class), eq("public.example")), times(16));
            storage.verify(() -> HeatMapStorage.record(new HeatMapEvent(String.format("%032x", 0), 123, 390,
                    now - 18, 42, 2500), "public.example"), never());
            storage.verify(() -> HeatMapStorage.record(new HeatMapEvent(String.format("%032x", 17), 123, 390,
                    now - 1, 42, 2500), "public.example"));
            assertEquals(18, overflowResponse.getCookies().length, "Only received cookie names are expired");
        }
    }

    /** Bootstrap stays cache-safe across IPs, while preview attributes and disabled page modes suppress it. */
    @Test
    void keepsBootstrapCacheSafeAndSuppressesItInPreview() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/page.html");
        request.setAttribute("doc_id", 123);
        BrowserDetector browser = mock(BrowserDetector.class);
        try (MockedStatic<BrowserDetector> browsers = mockStatic(BrowserDetector.class);
                MockedStatic<StatisticsMode> modes = mockStatic(StatisticsMode.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            browsers.when(() -> BrowserDetector.getInstance(request)).thenReturn(browser);
            modes.when(() -> StatisticsMode.forDocument(123)).thenReturn(StatisticsMode.HEATMAP);
            String excludedIpBootstrap = HeatMapTrackingService.bootstrap(request);
            browsers.when(() -> BrowserDetector.isStatIpAllowedFast(request)).thenReturn(true);
            String script = HeatMapTrackingService.bootstrap(request);
            assertEquals(excludedIpBootstrap, script);
            assertTrue(script.contains("src=\"/components/stat/heat-map-tracker.js\""));
            assertTrue(script.contains("data-doc-id=\"123\""));
            request.setAttribute(HeatMapTrackingService.PREVIEW_ATTRIBUTE, true);
            assertEquals("", HeatMapTrackingService.bootstrap(request));
            request.removeAttribute(HeatMapTrackingService.PREVIEW_ATTRIBUTE);
            modes.when(() -> StatisticsMode.forDocument(123)).thenReturn(StatisticsMode.OFF);
            assertEquals("", HeatMapTrackingService.bootstrap(request));
        }
    }
}
