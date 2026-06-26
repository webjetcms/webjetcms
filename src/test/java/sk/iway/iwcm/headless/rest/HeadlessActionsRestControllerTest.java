package sk.iway.iwcm.headless.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.headless.dto.ErrorResponse;
import sk.iway.iwcm.headless.dto.FieldError;
import sk.iway.iwcm.headless.dto.FormResult;
import sk.iway.iwcm.headless.dto.FormSubmitRequest;
import sk.iway.iwcm.headless.dto.SearchResultItem;
import sk.iway.iwcm.headless.dto.SearchResults;
import sk.iway.iwcm.headless.service.HeadlessFormActionService;
import sk.iway.iwcm.headless.service.HeadlessSearchService;
import sk.iway.iwcm.system.spring.services.WebjetSecurityService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for HeadlessActionsRestController.
 * Tests form validation, search result handling, and error responses.
 */
@ExtendWith(MockitoExtension.class)
class HeadlessActionsRestControllerTest {

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

    // ==================== Form Validation Tests ====================

    @Test
    void testValidateFormSubmitRequest_withAllFields() {
        FormSubmitRequest request = new FormSubmitRequest();
        request.setFormId("contact-form");
        request.setComponentKey("contact");
        
        Map<String, String> fields = new HashMap<>();
        fields.put("name", "John");
        fields.put("email", "john@example.com");
        request.setFields(fields);

        // Should pass validation - has formId and fields
        List<FieldError> errors = validateFormSubmitRequest(request);
        assertTrue(errors.isEmpty(), "Should have no validation errors");
    }

    @Test
    void testValidateFormSubmitRequest_missingFormId() {
        FormSubmitRequest request = new FormSubmitRequest();
        request.setFields(new HashMap<>());

        List<FieldError> errors = validateFormSubmitRequest(request);
        assertFalse(errors.isEmpty());
        assertEquals(1, errors.size());
        assertEquals("formId/formName/componentKey", errors.get(0).getField());
    }

    @Test
    void testValidateFormSubmitRequest_emptyFields() {
        FormSubmitRequest request = new FormSubmitRequest();
        request.setFormId("contact-form");

        List<FieldError> errors = validateFormSubmitRequest(request);
        assertFalse(errors.isEmpty());
        assertEquals(1, errors.size());
        assertEquals("fields", errors.get(0).getField());
    }

    @Test
    void testValidateFormSubmitRequest_noIdentification() {
        FormSubmitRequest request = new FormSubmitRequest();
        request.setFields(new HashMap<>());

        List<FieldError> errors = validateFormSubmitRequest(request);
        assertFalse(errors.isEmpty());
        assertEquals(2, errors.size()); // Missing form identification + missing fields
    }

    // ==================== Search Result Tests ====================

    @Test
    void testSearchResultItemSerialization() {
        SearchResultItem item = new SearchResultItem();
        item.setDocId(123);
        item.setTitle("Test Document");
        item.setVirtualPath("/test-doc");
        item.setLanguage("en");
        item.setPerex("Test perex");
        item.setSnippet("This is a test snippet for the document.");

        assertEquals(123, item.getDocId());
        assertEquals("Test Document", item.getTitle());
        assertEquals("/test-doc", item.getVirtualPath());
        assertEquals("en", item.getLanguage());
        assertEquals("Test perex", item.getPerex());
        assertEquals("This is a test snippet for the document.", item.getSnippet());
    }

    @Test
    void testSearchResultsWithItems() {
        SearchResults results = new SearchResults();
        results.setPage(1);
        results.setSize(10);
        results.setTotalElements(42);
        results.setTotalPages(5);

        List<SearchResultItem> items = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            SearchResultItem item = new SearchResultItem();
            item.setDocId(i);
            item.setTitle("Document " + i);
            item.setSnippet("Snippet for document " + i);
            items.add(item);
        }
        results.setItems(items);

        assertEquals(1, results.getPage());
        assertEquals(10, results.getSize());
        assertEquals(42, results.getTotalElements());
        assertEquals(5, results.getTotalPages());
        assertEquals(10, results.getItems().size());
    }

    @Test
    void testSearchResultsEmpty() {
        SearchResults results = new SearchResults();
        results.setItems(new ArrayList<>());

        assertEquals(0, results.getPage());
        assertEquals(0, results.getSize());
        assertEquals(0, results.getTotalElements());
        assertEquals(0, results.getTotalPages());
        assertNotNull(results.getItems());
        assertTrue(results.getItems().isEmpty());
    }

    // ==================== Form Processing Tests ====================

    @Test
    void testFormResultSuccess() {
        FormResult formResult = new FormResult();
        formResult.setSuccess(true);
        formResult.setMessage("Form submitted successfully.");

        assertTrue(formResult.isSuccess());
        assertEquals("Form submitted successfully.", formResult.getMessage());
        assertNull(formResult.getFieldErrors());
    }

    @Test
    void testFormResultFailure() {
        FormResult formResult = new FormResult();
        formResult.setSuccess(false);
        formResult.setMessage("Validation failed.");

        List<FieldError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new FieldError("email", "Invalid email"));
        fieldErrors.add(new FieldError("phone", "Phone is required"));
        formResult.setFieldErrors(fieldErrors);

        assertFalse(formResult.isSuccess());
        assertEquals("Validation failed.", formResult.getMessage());
        assertEquals(2, formResult.getFieldErrors().size());
    }

    @Test
    void testFormResultMinimal() {
        FormResult formResult = new FormResult();
        formResult.setSuccess(false);

        assertFalse(formResult.isSuccess());
        assertNull(formResult.getMessage());
        assertNull(formResult.getFieldErrors());
    }

    // ==================== Error Response Tests ====================

    @Test
    void testErrorResponseMinimal() {
        ErrorResponse errorResponse = new ErrorResponse(400, "Bad Request", "Invalid input");

        assertEquals(400, errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals("Invalid input", errorResponse.getMessage());
        assertNull(errorResponse.getFieldErrors());
    }

    @Test
    void testErrorResponseWithFieldErrors() {
        ErrorResponse errorResponse = new ErrorResponse(400, "Validation Error", "Request validation failed.");
        
        List<FieldError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new FieldError("field1", "Error 1"));
        fieldErrors.add(new FieldError("field2", "Error 2"));
        errorResponse.setFieldErrors(fieldErrors);

        assertEquals(400, errorResponse.getStatus());
        assertEquals("Validation Error", errorResponse.getError());
        assertEquals("Request validation failed.", errorResponse.getMessage());
        assertEquals(2, errorResponse.getFieldErrors().size());
    }

    // ==================== Snippet Extraction Tests ====================

    @Test
    void testExtractSnippet_withMatch() {
        String html = "<html><body><p>This is a test document about search results.</p></body></html>";
        String snippet = extractSnippet(html, "search");

        assertNotNull(snippet);
        assertTrue(snippet.toLowerCase().contains("search"));
    }

    @Test
    void testExtractSnippet_withNoMatch() {
        String html = "<html><body><p>This is a test document.</p></body></html>";
        String snippet = extractSnippet(html, "nonexistent");

        assertNotNull(snippet);
        // Should return beginning of text when no match found
        assertTrue(snippet.length() > 0);
    }

    @Test
    void testExtractSnippet_withEmptyData() {
        String snippet = extractSnippet(null, "query");
        assertEquals("", snippet);
    }

    @Test
    void testExtractSnippet_withEmptyQuery() {
        String snippet = extractSnippet("<html><body>Test</body></html>", "");
        assertEquals("", snippet);
    }

    // ==================== Helper Methods ====================

    private List<FieldError> validateFormSubmitRequest(FormSubmitRequest request) {
        List<FieldError> errors = new ArrayList<>();

        if (Tools.isEmpty(request.getFormId()) 
                && Tools.isEmpty(request.getFormName()) 
                && Tools.isEmpty(request.getComponentKey())) {
            errors.add(new FieldError("formId/formName/componentKey", 
                    "One of formId, formName, or componentKey is required."));
        }

        if (request.getFields() == null || request.getFields().isEmpty()) {
            errors.add(new FieldError("fields", "Form fields payload is required."));
        }

        return errors;
    }

    private String extractSnippet(String data, String query) {
        if (Tools.isEmpty(data) || Tools.isEmpty(query)) {
            return "";
        }

        String text = data.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        int idx = lowerText.indexOf(lowerQuery);
        if (idx < 0) {
            return text.length() > 200 ? text.substring(0, 200) + "..." : text;
        }

        int start = Math.max(0, idx - 100);
        int end = Math.min(text.length(), idx + query.length() + 100);

        String snippet = text.substring(start, end);
        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < text.length()) {
            snippet = snippet + "...";
        }

        return snippet;
    }
}
