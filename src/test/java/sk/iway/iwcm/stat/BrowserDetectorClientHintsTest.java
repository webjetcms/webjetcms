package sk.iway.iwcm.stat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Cache;

/** Verifies Windows Client Hints, legacy fallback and isolation of cached browser detections. */
class BrowserDetectorClientHintsTest {
    private static final String EDGE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36 Edg/154.0.0.0";

    /** The platform hint is an API contract version, not the displayed Windows version. */
    @ParameterizedTest
    @CsvSource({"1.0.0, 10", "10.0.0, 10", "12.0.0, 10", "13.0.0, 11", "15.0.0, 11", "19.0.0, 11"})
    void refinesWindowsVersionWithoutChangingBrowser(String platformVersion, String expectedVersion) {
        var request = request("\"" + platformVersion + "\"");
        var detector = new BrowserDetector(EDGE);
        detector.parse(request);

        assertEquals("Windows", detector.getBrowserPlatform());
        assertEquals(expectedVersion, detector.getBrowserSubplatform());
        assertEquals("Edge", detector.getBrowserName());
        assertEquals("154.0", detector.getBrowserVersion());
        assertFalse(detector.isPhone());
        assertFalse(detector.isTablet());
    }

    /** Missing, unavailable and malformed hints leave legacy User-Agent detection usable. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"\"\"", "\"0.0.0\"", "13.0.0", "\"13\"", "\"13.invalid.0\"", "\"-13.0.0\"", "\"999999999999.0.0\"", "\"13.0.0\", \"10.0.0\""})
    void retainsLegacyVersionForUnusableHints(String platformVersion) {
        var detector = new BrowserDetector(EDGE);
        detector.parse(request(platformVersion));
        assertEquals("10", detector.getBrowserSubplatform());
        assertEquals("Edge", detector.getBrowserName());
    }

    /** A version hint alone or one belonging to another platform cannot identify Windows 11. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"\"macOS\"", "\"Android\"", "Windows"})
    void requiresWindowsPlatformHint(String platform) {
        var request = request("\"15.0.0\"");
        request.removeHeader("Sec-CH-UA-Platform");
        if (platform != null) request.addHeader("Sec-CH-UA-Platform", platform);
        var detector = new BrowserDetector(EDGE);
        detector.parse(request);
        assertEquals("10", detector.getBrowserSubplatform());
    }

    /** Client Hints do not overwrite a different operating system or an older Windows release. */
    @Test
    void preservesOtherLegacyPlatforms() {
        var detector = new BrowserDetector(EDGE.replace("Windows NT 10.0", "Windows NT 6.1"));
        detector.parse(request("\"15.0.0\""));
        assertEquals("Windows", detector.getBrowserPlatform());
        assertEquals("7", detector.getBrowserSubplatform());

        detector = new BrowserDetector(EDGE.replace("Windows NT 10.0; Win64; x64", "Macintosh; Intel Mac OS X 10_15_7"));
        detector.parse(request("\"15.0.0\""));
        assertEquals("Mac OS X", detector.getBrowserPlatform());
        assertEquals("10", detector.getBrowserSubplatform());
    }

    /** Later hints replace the session detector without changing another browser behind the same IP. */
    @Test
    void refreshesSessionAndSeparatesSharedCacheByWindowsVersion() {
        Map<String, Object> cached = new HashMap<>();
        Cache cache = mock(Cache.class);
        when(cache.getObject(anyString())).thenAnswer(call -> cached.get(call.getArgument(0)));
        doAnswer(call -> {
            cached.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(cache).setObjectSeconds(anyString(), any(), anyInt());

        try (var caches = mockStatic(Cache.class); var stats = mockStatic(StatDB.class)) {
            caches.when(Cache::getInstance).thenReturn(cache);
            stats.when(() -> StatDB.getStatKeyId("11")).thenReturn(111);
            var firstRequest = request(null);
            var legacy = BrowserDetector.getInstance(firstRequest);
            var secondRequest = request(null);
            assertSame(legacy, BrowserDetector.getInstance(secondRequest));

            firstRequest.addHeader("Sec-CH-UA-Platform-Version", "\"15.0.0\"");
            var windows11 = BrowserDetector.getInstance(firstRequest);
            assertEquals("11", windows11.getBrowserSubplatform());
            assertEquals(111, windows11.getSubplatformId());
            assertNotSame(legacy, windows11);
            assertEquals("10", BrowserDetector.getInstance(secondRequest).getBrowserSubplatform());
            assertSame(windows11, BrowserDetector.getInstance(request("\"19.0.0\"")));

            var windows10 = BrowserDetector.getInstance(request("\"10.0.0\""));
            assertEquals("10", windows10.getBrowserSubplatform());
            assertNotSame(windows11, windows10);

            firstRequest.removeHeader("Sec-CH-UA-Platform-Version");
            assertSame(windows11, BrowserDetector.getInstance(firstRequest));
            secondRequest.addHeader("Sec-CH-UA-Platform-Version", "\"15.0.0\"");
            assertSame(windows11, BrowserDetector.getInstance(secondRequest));
            assertSame(windows11, secondRequest.getSession().getAttribute(StatDB.BROWSER_DETECTOR));

            var forcedRequest = request(null);
            forcedRequest.setParameter("forceBrowserDetector", "tablet");
            assertTrue(BrowserDetector.getInstance(forcedRequest).isTablet());
            forcedRequest.removeParameter("forceBrowserDetector");
            forcedRequest.addHeader("Sec-CH-UA-Platform-Version", "\"15.0.0\"");
            var forcedWindows11 = BrowserDetector.getInstance(forcedRequest);
            assertEquals("11", forcedWindows11.getBrowserSubplatform());
            assertTrue(forcedWindows11.isTablet());
            assertFalse(windows11.isTablet());
        }
    }

    private MockHttpServletRequest request(String platformVersion) {
        var request = new MockHttpServletRequest();
        request.addHeader("User-Agent", EDGE);
        request.addHeader("Sec-CH-UA-Platform", "\"Windows\"");
        if (platformVersion != null) request.addHeader("Sec-CH-UA-Platform-Version", platformVersion);
        return request;
    }
}
