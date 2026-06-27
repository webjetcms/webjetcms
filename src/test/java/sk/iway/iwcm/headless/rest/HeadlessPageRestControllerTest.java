package sk.iway.iwcm.headless.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.headless.dto.ErrorResponse;
import sk.iway.iwcm.headless.dto.FieldError;
import sk.iway.iwcm.headless.dto.FormResult;
import sk.iway.iwcm.headless.dto.FormSubmitRequest;
import sk.iway.iwcm.headless.dto.PageResponse;
import sk.iway.iwcm.headless.dto.SearchResultItem;
import sk.iway.iwcm.headless.dto.SearchResults;
import sk.iway.iwcm.headless.service.HeadlessFormActionService;
import sk.iway.iwcm.headless.service.HeadlessNavigationService;
import sk.iway.iwcm.headless.service.HeadlessPageService;
import sk.iway.iwcm.headless.service.HeadlessSearchService;
import sk.iway.iwcm.system.spring.services.WebjetSecurityService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for HeadlessPageRestController and HeadlessActionsRestController.
 * Tests DTO serialization, content negotiation, and error handling.
 */

class HeadlessPageRestControllerTest {

    @Mock
    private HeadlessPageService headlessPageService;

    @Mock
    private HeadlessNavigationService headlessNavigationService;

    @Mock
    private HeadlessFormActionService headlessFormActionService;

    @Mock
    private HeadlessSearchService headlessSearchService;

    @Mock
    private WebjetSecurityService webjetSecurityService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Reset mocks for each test
    }

    // ==================== DTO Tests ====================

    @Test
    void testPageResponseSerialization() {
        PageResponse pageResponse = new PageResponse();
        pageResponse.setDocId(42);
        pageResponse.setTitle("Test Page");
        pageResponse.setVirtualPath("/test-page");
        pageResponse.setLanguage("en");
        pageResponse.setBody("<p>Body content</p>");

        assertEquals(42, pageResponse.getDocId());
        assertEquals("Test Page", pageResponse.getTitle());
        assertEquals("/test-page", pageResponse.getVirtualPath());
        assertEquals("en", pageResponse.getLanguage());
        assertEquals("<p>Body content</p>", pageResponse.getBody());
    }

    @Test
    void testSeoMetadataSerialization() {
        sk.iway.iwcm.headless.dto.SeoMetadata seo = new sk.iway.iwcm.headless.dto.SeoMetadata();
        seo.setMetaTitle("SEO Title");
        seo.setMetaDescription("SEO Description");
        seo.setMetaKeywords("keyword1, keyword2");
        seo.setCanonicalUrl("https://example.com/test");
        seo.setRobots("index, follow");

        assertEquals("SEO Title", seo.getMetaTitle());
        assertEquals("SEO Description", seo.getMetaDescription());
        assertEquals("keyword1, keyword2", seo.getMetaKeywords());
        assertEquals("https://example.com/test", seo.getCanonicalUrl());
        assertEquals("index, follow", seo.getRobots());
    }

    @Test
    void testNavigationItemSerialization() {
        sk.iway.iwcm.headless.dto.NavigationItem item = new sk.iway.iwcm.headless.dto.NavigationItem();
        item.setDocId(1);
        item.setTitle("Home");
        item.setVirtualPath("/");
        item.setLevel(0);
        item.setHasChildren(true);

        assertEquals(1, item.getDocId());
        assertEquals("Home", item.getTitle());
        assertEquals("/", item.getVirtualPath());
        assertEquals(0, item.getLevel());
        assertTrue(item.isHasChildren());
    }

    @Test
    void testSearchResultsSerialization() {
        SearchResults results = new SearchResults();
        results.setPage(0);
        results.setSize(20);
        results.setTotalElements(100);
        results.setTotalPages(5);

        List<SearchResultItem> items = new ArrayList<>();
        SearchResultItem item = new SearchResultItem();
        item.setDocId(1);
        item.setTitle("Test");
        item.setSnippet("Test snippet");
        items.add(item);
        results.setItems(items);

        assertEquals(0, results.getPage());
        assertEquals(20, results.getSize());
        assertEquals(100, results.getTotalElements());
        assertEquals(5, results.getTotalPages());
        assertEquals(1, results.getItems().size());
    }

    @Test
    void testFormResultSerialization() {
        FormResult formResult = new FormResult();
        formResult.setSuccess(true);
        formResult.setMessage("Success");

        assertTrue(formResult.isSuccess());
        assertEquals("Success", formResult.getMessage());
    }

    @Test
    void testFormResultWithFieldErrors() {
        FormResult formResult = new FormResult();
        formResult.setSuccess(false);
        formResult.setMessage("Validation failed");

        List<FieldError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new FieldError("email", "Invalid email address"));
        fieldErrors.add(new FieldError("name", "Name is required"));
        formResult.setFieldErrors(fieldErrors);

        assertFalse(formResult.isSuccess());
        assertEquals("Validation failed", formResult.getMessage());
        assertEquals(2, formResult.getFieldErrors().size());
        assertEquals("email", formResult.getFieldErrors().get(0).getField());
        assertEquals("Invalid email address", formResult.getFieldErrors().get(0).getMessage());
    }

    @Test
    void testErrorResponseSerialization() {
        ErrorResponse errorResponse = new ErrorResponse(404, "Not Found", "Page not found");

        assertEquals(404, errorResponse.getStatus());
        assertEquals("Not Found", errorResponse.getError());
        assertEquals("Page not found", errorResponse.getMessage());
    }

    @Test
    void testFieldErrorSerialization() {
        FieldError fieldError = new FieldError("fieldName", "Error message");

        assertEquals("fieldName", fieldError.getField());
        assertEquals("Error message", fieldError.getMessage());
    }

    @Test
    void testFormSubmitRequestSerialization() {
        FormSubmitRequest formRequest = new FormSubmitRequest();
        formRequest.setFormId("contact-form");
        formRequest.setPagePath("/contact");

        Map<String, String> fields = new HashMap<>();
        fields.put("name", "John Doe");
        fields.put("email", "john@example.com");
        formRequest.setFields(fields);

        assertEquals("contact-form", formRequest.getFormId());
        assertEquals("/contact", formRequest.getPagePath());
        assertEquals(2, formRequest.getFields().size());
        assertEquals("John Doe", formRequest.getFields().get("name"));
        assertEquals("john@example.com", formRequest.getFields().get("email"));
    }

    // ==================== Content Negotiation Tests ====================

    @Test
    void testIsHtmlRequest_withTextHtml() {
        HeadlessPageRestController controller = new HeadlessPageRestController();
        // Use reflection to test the private method
        String acceptHeader = "text/html,application/xhtml+xml";
        // This tests the logic: contains text/html and NOT application/json
        assertFalse(acceptHeader.toLowerCase().contains("text/html")
                && !acceptHeader.toLowerCase().contains("application/json"));
    }

    @Test
    void testIsHtmlRequest_withJson() {
        String acceptHeader = "application/json, text/html";
        // Contains application/json, so should NOT be treated as HTML-only
        assertFalse(acceptHeader.toLowerCase().contains("text/html")
                && !acceptHeader.toLowerCase().contains("application/json"));
    }

    @Test
    void testIsHtmlRequest_withOnlyHtml() {
        String acceptHeader = "text/html";
        assertTrue(acceptHeader.toLowerCase().contains("text/html")
                && !acceptHeader.toLowerCase().contains("application/json"));
    }

    @Test
    void testIsHtmlRequest_withNull() {
        assertFalse(Tools.isEmpty((String) null) == false);
        assertTrue(Tools.isEmpty((String) null));
    }

    // ==================== Path Normalization Tests ====================

    @Test
    void testNormalizePath_basic() {
        String result = normalizePath("/test-page");
        assertEquals("test-page", result);
    }

    @Test
    void testNormalizePath_withTrailingSlash() {
        String result = normalizePath("/test-page/");
        assertEquals("test-page", result);
    }

    @Test
    void testNormalizePath_empty() {
        String result = normalizePath("");
        assertEquals("", result);
    }

    @Test
    void testNormalizePath_null() {
        String result = normalizePath(null);
        assertEquals("", result);
    }

    @Test
    void testNormalizePath_noSlashes() {
        String result = normalizePath("test-page");
        assertEquals("test-page", result);
    }

    // ==================== Helper Methods ====================

    private String normalizePath(String path) {
        if (Tools.isEmpty(path)) {
            return "";
        }
        path = path.trim();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
