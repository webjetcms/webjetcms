package sk.iway.iwcm.headless.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.headless.dto.FieldError;
import sk.iway.iwcm.headless.dto.FormResult;
import sk.iway.iwcm.headless.dto.FormSubmitRequest;
import sk.iway.iwcm.headless.dto.SearchResultItem;
import sk.iway.iwcm.headless.dto.SearchResults;
import sk.iway.iwcm.headless.service.HeadlessFormActionService;
import sk.iway.iwcm.headless.service.HeadlessSearchService;

import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for headless actions (forms submit and search).
 * Supports POST /rest/headless/v1/actions/forms/submit
 * and GET /rest/headless/v1/actions/search.
 */
@RestController
@RequestMapping("/rest/headless/v1/actions")
@Tag(name = "Headless Actions", description = "Form submission and search endpoints for headless consumption")
public class HeadlessActionsRestController {

    @Autowired
    private HeadlessFormActionService headlessFormActionService;

    @Autowired
    private HeadlessSearchService headlessSearchService;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    // ==================== Forms Endpoints ====================

    /**
     * Submit a form via the headless API.
     *
     * @param request   the form submission request body
     * @param httpRequest the HTTP request
     * @param response  the HTTP response
     * @return FormResult with success/error status
     */
    @PostMapping(value = "/forms/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Submit a form",
            description = "Submits a form through the headless API. Supports form identification by formId, formName, or componentKey.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Form processed successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation errors")
            }
    )
    public ResponseEntity<FormResult> submitForm(
            @RequestBody FormSubmitRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        // Validate request body
        List<FieldError> validationErrors = validateFormSubmitRequest(request);
        if (!validationErrors.isEmpty()) {
            return createValidationErrorResponse(validationErrors);
        }

        // Process form submission via service
        FormResult formResult = headlessFormActionService.processFormSubmission(
                request, httpRequest, response);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        try {
            String json = objectMapper.writeValueAsString(formResult);
            // Parse back to FormResult for proper typing
            FormResult result = objectMapper.readValue(json, FormResult.class);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            FormResult errorResult = new FormResult();
            errorResult.setSuccess(false);
            errorResult.setMessage("Failed to process form submission.");
            return new ResponseEntity<>(errorResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== Search Endpoints ====================

    /**
     * Search documents via the headless API.
     *
     * @param q         search query (required)
     * @param page      page number (default 0)
     * @param size      page size (default 20, max 100)
     * @param scope     optional scope filter
     * @param lng       optional language filter
     * @param httpRequest the HTTP request
     * @param response  the HTTP response
     * @return SearchResults with paged results
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Search documents",
            description = "Searches documents through the headless API with pagination support.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Search results returned"),
                    @ApiResponse(responseCode = "400", description = "Invalid query")
            }
    )
    public ResponseEntity<SearchResults> search(
            @Parameter(required = true) @RequestParam(name = "q", required = true) String q,
            @Parameter(description = "Page number (0-based)") @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @Parameter(description = "Page size (default 20, max 100)") @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            @Parameter(description = "Scope filter") @RequestParam(name = "scope", required = false) String scope,
            @Parameter(description = "Language filter") @RequestParam(name = "lng", required = false) String lng,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        // Validate query
        if (Tools.isEmpty(q)) {
            return createSearchErrorResponse(400, "Bad Request", "Search query 'q' is required.");
        }

        // Validate page size
        if (size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }

        // Build pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "doc_id"));

        // Execute search via service
        Page<sk.iway.iwcm.doc.DocDetails> docPage = headlessSearchService.searchDocuments(
                q.trim(), pageable, scope, lng);

        // Build response
        SearchResults searchResults = new SearchResults();
        searchResults.setPage(page);
        searchResults.setSize(size);
        searchResults.setTotalElements(docPage.getTotalElements());
        searchResults.setTotalPages(docPage.getTotalPages());

        List<SearchResultItem> items = new ArrayList<>();
        for (sk.iway.iwcm.doc.DocDetails doc : docPage.getContent()) {
            SearchResultItem item = new SearchResultItem();
            item.setDocId(doc.getDocId());
            item.setTitle(doc.getTitle());
            item.setVirtualPath(doc.getVirtualPath());
            item.setPerex(doc.getPerexImage());

            // Extract snippet from doc_data
            String snippet = extractSnippet(doc.getData(), q);
            item.setSnippet(snippet);

            // Language is determined by the request parameter 'lng', not by the document

            items.add(item);
        }

        searchResults.setItems(items);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        return new ResponseEntity<>(searchResults, HttpStatus.OK);
    }

    // ==================== Helpers ====================

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

        // Strip HTML tags for snippet extraction
        String text = data.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        int idx = lowerText.indexOf(lowerQuery);
        if (idx < 0) {
            // No match, return beginning
            return text.length() > 200 ? text.substring(0, 200) + "..." : text;
        }

        // Extract context around the query
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

    private ResponseEntity<FormResult> createValidationErrorResponse(List<FieldError> fieldErrors) {
        FormResult errorResult = new FormResult();
        errorResult.setSuccess(false);
        errorResult.setMessage("Validation failed.");
        errorResult.setFieldErrors(fieldErrors);
        return new ResponseEntity<>(errorResult, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<SearchResults> createSearchErrorResponse(int status, String error, String message) {
        SearchResults results = new SearchResults();
        results.setItems(new ArrayList<>());
        return new ResponseEntity<>(results, org.springframework.http.HttpStatus.valueOf(status));
    }
}
