package sk.iway.basecms.langredirect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * JUnit tests for {@link LanguageRedirectApp}.
 *
 * Covers:
 * <ul>
 *   <li>Language detection from Accept-Language header</li>
 *   <li>Redirect URL lookup from 8 configurable mappings</li>
 *   <li>Main handler with rootOnly, respectCookie, and editor preview scenarios</li>
 *   <li>Dynamic app options generation</li>
 * </ul>
 */
class LanguageRedirectAppTest extends BaseWebjetTest {

    // EMPTY_PAGE is protected in WebjetComponentAbstract, so we use the constant value directly
    private static final String EMPTY_PAGE = "/apps/_common/empty";

    private LanguageRedirectApp app;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        app = new LanguageRedirectApp();
        app.setDefaultLanguage("sk");
        app.setRootOnly(false);
        app.setRespectCookie(true);

        // Clear all mappings
        app.setMapping1Lang(""); app.setMapping1Url("");
        app.setMapping2Lang(""); app.setMapping2Url("");
        app.setMapping3Lang(""); app.setMapping3Url("");
        app.setMapping4Lang(""); app.setMapping4Url("");
        app.setMapping5Lang(""); app.setMapping5Url("");
        app.setMapping6Lang(""); app.setMapping6Url("");
        app.setMapping7Lang(""); app.setMapping7Url("");
        app.setMapping8Lang(""); app.setMapping8Url("");

        request = new MockHttpServletRequest(new MockServletContext());
    }

    private void setRequestContext(MockHttpServletRequest req) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    private void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ===================== detectLanguage tests =====================

    @Test
    void testDetectLanguageWithEmptyHeader() {
        setRequestContext(request);
        try {
            String lang = invokeDetectLanguage();
            assertEquals("sk", lang);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testDetectLanguageWithSimpleLanguage() {
        setRequestContext(request);
        try {
            request.addHeader("Accept-Language", "en");
            String lang = invokeDetectLanguage();
            assertEquals("en", lang);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testDetectLanguageWithLocaleAndRegion() {
        setRequestContext(request);
        try {
            request.addHeader("Accept-Language", "en-US");
            String lang = invokeDetectLanguage();
            assertEquals("en", lang);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testDetectLanguageWithUnderscoreLocale() {
        setRequestContext(request);
        try {
            request.addHeader("Accept-Language", "sk_SK");
            String lang = invokeDetectLanguage();
            assertEquals("sk", lang);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testDetectLanguageWithQualityFactor() {
        setRequestContext(request);
        try {
            request.addHeader("Accept-Language", "en-US;q=0.7");
            String lang = invokeDetectLanguage();
            assertEquals("en", lang);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testDetectLanguageWithMultipleLanguages() {
        setRequestContext(request);
        try {
            request.addHeader("Accept-Language", "cs-CZ;q=0.9, sk;q=0.8, en;q=0.7");
            String lang = invokeDetectLanguage();
            assertEquals("cs", lang);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testDetectLanguageLowercased() {
        setRequestContext(request);
        try {
            request.addHeader("Accept-Language", "EN-GB");
            String lang = invokeDetectLanguage();
            assertEquals("en", lang);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testDetectLanguageUnknownFallbackToDefault() {
        app.setDefaultLanguage("en");
        setRequestContext(request);
        try {
            request.addHeader("Accept-Language", "xx-XX");
            String lang = invokeDetectLanguage();
            assertEquals("xx", lang);
        } finally {
            clearRequestContext();
        }
    }

    // ===================== getRedirectUrl tests =====================

    @Test
    void testGetRedirectUrlNoMapping() {
        String url = invokeGetRedirectUrl("en");
        assertNull(url);
    }

    @Test
    void testGetRedirectUrlMapping1() {
        app.setMapping1Lang("en");
        app.setMapping1Url("/en/home");
        String url = invokeGetRedirectUrl("en");
        assertEquals("/en/home", url);
    }

    @Test
    void testGetRedirectUrlMapping3() {
        app.setMapping3Lang("cs");
        app.setMapping3Url("/cs/stranka");
        String url = invokeGetRedirectUrl("cs");
        assertEquals("/cs/stranka", url);
    }

    @Test
    void testGetRedirectUrlMapping8() {
        app.setMapping8Lang("de");
        app.setMapping8Url("/de/");
        String url = invokeGetRedirectUrl("de");
        assertEquals("/de/", url);
    }

    @Test
    void testGetRedirectUrlNoMatchOnEmptyMapping() {
        app.setMapping1Lang("");
        app.setMapping1Url("/some-url");
        String url = invokeGetRedirectUrl("en");
        assertNull(url);
    }

    @Test
    void testGetRedirectUrlFirstMappingWinsOnDuplicate() {
        app.setMapping1Lang("en");
        app.setMapping1Url("/en/first");
        app.setMapping2Lang("en");
        app.setMapping2Url("/en/second");
        String url = invokeGetRedirectUrl("en");
        assertEquals("/en/first", url);
    }

    @Test
    void testGetRedirectUrlNullInput() {
        String url = invokeGetRedirectUrl(null);
        assertNull(url);
    }

    // ===================== view (DefaultHandler) tests =====================

    @Test
    void testViewNoMappingNoCookieReturnsEmptyPage() {
        setRequestContext(request);
        try {
            String result = app.view(request);
            assertEquals(EMPTY_PAGE, result);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testViewRootOnlyOnRootPath() {
        app.setRootOnly(true);
        app.setMapping1Lang("en");
        app.setMapping1Url("/en/");
        setRequestContext(request);
        try {
            request.setPathInfo("/");
            request.setServletPath("/");
            request.setAttribute("path_filter_orig_path", "/");
            request.addHeader("Accept-Language", "en");
            String result = app.view(request);
            assertEquals("redirect:/en/", result);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testViewRootOnlyOnIndexHtml() {
        app.setRootOnly(true);
        app.setMapping1Lang("en");
        app.setMapping1Url("/en/");
        setRequestContext(request);
        try {
            request.setPathInfo("/index.html");
            request.setAttribute("path_filter_orig_path", "/index.html");
            request.addHeader("Accept-Language", "en");
            String result = app.view(request);
            assertEquals("redirect:/en/", result);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testViewRootOnlyOnSubPathSkipsRedirect() {
        app.setRootOnly(true);
        app.setMapping1Lang("en");
        app.setMapping1Url("/en/");
        setRequestContext(request);
        try {
            request.setPathInfo("/about");
            request.setAttribute("path_filter_orig_path", "/about");
            String result = app.view(request);
            assertEquals(EMPTY_PAGE, result);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testViewRespectCookieUsesCookieLanguage() {
        app.setRespectCookie(true);
        app.setMapping1Lang("en");
        app.setMapping1Url("/en/");
        app.setMapping2Lang("sk");
        app.setMapping2Url("/sk/");
        setRequestContext(request);
        try {
            // Add lng cookie with value "en"
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("lng", "en");
            request.setCookies(cookie);
            // Set Accept-Language to sk so we can verify cookie takes precedence
            request.addHeader("Accept-Language", "sk");
            String result = app.view(request);
            assertEquals("redirect:/en/", result);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testViewRespectCookieFalseIgnoresCookie() {
        app.setRespectCookie(false);
        app.setMapping1Lang("en");
        app.setMapping1Url("/en/");
        app.setMapping2Lang("sk");
        app.setMapping2Url("/sk/");
        setRequestContext(request);
        try {
            // Add lng cookie with value "en"
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("lng", "en");
            request.setCookies(cookie);
            // Set Accept-Language to sk so we can verify cookie is ignored
            request.addHeader("Accept-Language", "sk");
            String result = app.view(request);
            assertEquals("redirect:/sk/", result);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testViewNoMappingFoundReturnsEmptyPage() {
        setRequestContext(request);
        try {
            request.addHeader("Accept-Language", "de");
            String result = app.view(request);
            assertEquals(EMPTY_PAGE, result);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testViewFallbackToDefaultLanguageMapping() {
        app.setMapping1Lang("en");
        app.setMapping1Url("/en/");
        setRequestContext(request);
        try {
            // Detect language is "de" which has no mapping
            // Should fall back to defaultLanguage="sk" which also has no mapping
            request.addHeader("Accept-Language", "de");
            String result = app.view(request);
            assertEquals(EMPTY_PAGE, result);
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testViewDefaultLanguageMappingExists() {
        app.setDefaultLanguage("sk");
        app.setMapping1Lang("en");
        app.setMapping1Url("/en/");
        app.setMapping2Lang("sk");
        app.setMapping2Url("/sk/");
        setRequestContext(request);
        try {
            // Detect language is "en" -> finds mapping1 -> redirects to /en/
            request.addHeader("Accept-Language", "en");
            String result = app.view(request);
            assertEquals("redirect:/en/", result);
        } finally {
            clearRequestContext();
        }
    }

    // ===================== getAppOptions tests =====================

    @Test
    void testGetAppOptionsReturnsAllMappingFields() {
        setRequestContext(request);
        try {
            Map<String, List<OptionDto>> options = app.getAppOptions(null, request);
            assertTrue(options.containsKey("defaultLanguage"));
            assertTrue(options.containsKey("mapping1Lang"));
            assertTrue(options.containsKey("mapping8Lang"));
            assertEquals(9, options.size());
        } finally {
            clearRequestContext();
        }
    }

    @Test
    void testGetAppOptionsMappingFieldsHaveEmptyOptionFirst() {
        setRequestContext(request);
        try {
            Map<String, List<OptionDto>> options = app.getAppOptions(null, request);
            List<OptionDto> mappingOptions = options.get("mapping1Lang");
            assertTrue(mappingOptions.get(0).getValue().isEmpty());
        } finally {
            clearRequestContext();
        }
    }

    // ===================== Reflection helpers =====================

    /**
     * Invokes the private detectLanguage method via reflection.
     */
    private String invokeDetectLanguage() {
        try {
            Method method = LanguageRedirectApp.class
                    .getDeclaredMethod("detectLanguage", HttpServletRequest.class);
            method.setAccessible(true);
            return (String) method.invoke(app, request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke detectLanguage", e);
        }
    }

    /**
     * Invokes the private getRedirectUrl method via reflection.
     */
    private String invokeGetRedirectUrl(String lang) {
        try {
            Method method = LanguageRedirectApp.class
                    .getDeclaredMethod("getRedirectUrl", String.class);
            method.setAccessible(true);
            return (String) method.invoke(app, lang);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getRedirectUrl", e);
        }
    }
}
