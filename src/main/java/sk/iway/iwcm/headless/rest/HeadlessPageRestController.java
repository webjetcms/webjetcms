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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.headless.dto.ErrorResponse;
import sk.iway.iwcm.headless.dto.FieldError;
import sk.iway.iwcm.headless.dto.NavigationItem;
import sk.iway.iwcm.headless.dto.PageResponse;
import sk.iway.iwcm.headless.service.HeadlessNavigationService;
import sk.iway.iwcm.headless.service.HeadlessPageService;
import sk.iway.iwcm.users.UsersDB;

import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for headless page retrieval and navigation with content negotiation.
 * Supports GET /rest/headless/v1/pages/by-path and GET /rest/headless/v1/navigation.
 */
@RestController
@RequestMapping("/rest/headless/v1")
@Tag(name = "Headless Pages", description = "Page retrieval endpoints for headless consumption")
public class HeadlessPageRestController extends sk.iway.iwcm.rest.RestController {

    private final HeadlessPageService headlessPageService;
    private final HeadlessNavigationService headlessNavigationService;
    private final ObjectMapper objectMapper;

    @Autowired
    public HeadlessPageRestController(
            HeadlessPageService headlessPageService,
            HeadlessNavigationService headlessNavigationService) {
        this.headlessPageService = headlessPageService;
        this.headlessNavigationService = headlessNavigationService;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== Page Endpoints ====================

    /**
     * Get page by virtual path with content negotiation.
     */
    @GetMapping(value = {"/pages/by-path", "/pages/by-path/{path:.+}"},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE})
    @Operation(
            summary = "Get page by path",
            description = "Retrieves a page by its virtual path with content negotiation support.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Page found"),
                    @ApiResponse(responseCode = "401", description = "IP not allowed"),
                    @ApiResponse(responseCode = "403", description = "Preview requested without admin session"),
                    @ApiResponse(responseCode = "404", description = "Page not found")
            }
    )
    public Object getPageByPath(
            @Parameter(required = true) @RequestParam(name = "path", required = true) String path,
            @Parameter(description = "Language override") @RequestParam(name = "lng", required = false, defaultValue = "") String lng,
            @Parameter(description = "Preview mode - requires admin session") @RequestParam(name = "preview", required = false, defaultValue = "false") String preview,
            HttpServletRequest request,
            HttpServletResponse response) {

        isIpAddressAllowed(request);
        path = normalizePath(path);

        if (Tools.isEmpty(path)) {
            return createErrorResponse(400, "Bad Request", "Path parameter is required.");
        }

        String domain = DocDB.getDomain(request);
        int docId = DocDB.getInstance().getVirtualPathDocId(path, domain);
        if (docId < 1) {
            return createErrorResponse(404, "Not Found", "Page not found: " + path);
        }

        boolean isPreview = "true".equalsIgnoreCase(preview);
        if (isPreview) {
            Identity user = UsersDB.getCurrentUser(request);
            if (user == null || !user.isAdmin()) {
                return createErrorResponse(403, "Forbidden", "Admin session required for preview mode.");
            }
        }

        PageResponse pageResponse = headlessPageService.resolvePage(path, lng, isPreview, request, response);
        if (pageResponse == null) {
            return createErrorResponse(404, "Not Found", "Page not found: " + path);
        }

        String acceptHeader = request.getHeader("Accept");
        if (isHtmlRequest(acceptHeader)) {
            String htmlBody = headlessPageService.renderPageBody(pageResponse.getDocId(), request, response);
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            response.setCharacterEncoding("UTF-8");
            return htmlBody;
        }

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        try {
            String json = objectMapper.writeValueAsString(pageResponse);
            return json;
        } catch (JsonProcessingException e) {
            return createErrorResponse(500, "Internal Server Error", "Failed to serialize page response.");
        }
    }

    /**
     * Get preview page by document ID (requires admin session).
     */
    @GetMapping(value = "/preview/pages/by-id", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE})
    @Operation(
            summary = "Get preview page by ID",
            description = "Retrieves a page by document ID for preview (unpublished/draft content). Admin session required.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Page found"),
                    @ApiResponse(responseCode = "403", description = "Admin session required"),
                    @ApiResponse(responseCode = "404", description = "Page not found")
            }
    )
    public Object getPreviewPageById(
            @Parameter(required = true) @RequestParam(name = "docId", required = true) int docId,
            @Parameter(description = "Language override") @RequestParam(name = "lng", required = false, defaultValue = "") String lng,
            HttpServletRequest request,
            HttpServletResponse response) {

        isIpAddressAllowed(request);

        // Strict admin session check
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin()) {
            return createErrorResponse(403, "Forbidden", "Admin session required for preview.");
        }

        // Load document with cache bypass (preview should show latest draft)
        DocDetails doc = DocDB.getInstance().getDoc(docId, -1, false);
        if (doc == null) {
            return createErrorResponse(404, "Not Found", "Document not found: " + docId);
        }

        // Build PageResponse
        PageResponse pageResponse = new PageResponse(
                doc.getDocId(),
                doc.getTitle(),
                doc.getVirtualPath(),
                getLanguage(doc, lng),
                headlessPageService.extractBody(doc, request)
        );

        pageResponse.setSeo(headlessPageService.buildSeoMetadata(doc));

        String acceptHeader = request.getHeader("Accept");
        if (isHtmlRequest(acceptHeader)) {
            String htmlBody = headlessPageService.renderPageBody(docId, request, response);
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            response.setCharacterEncoding("UTF-8");
            return htmlBody;
        }

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        try {
            String json = objectMapper.writeValueAsString(pageResponse);
            return json;
        } catch (JsonProcessingException e) {
            return createErrorResponse(500, "Internal Server Error", "Failed to serialize page response.");
        }
    }

    // ==================== Navigation Endpoints ====================

    /**
     * Get navigation tree/list.
     */
    @GetMapping(value = "/navigation", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get navigation tree",
            description = "Retrieves the navigation tree starting from a root group or path.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Navigation tree returned"),
                    @ApiResponse(responseCode = "401", description = "IP not allowed"),
                    @ApiResponse(responseCode = "404", description = "Root not found")
            }
    )
    public ResponseEntity<List<NavigationItem>> getNavigation(
            @Parameter(description = "Virtual path of the root group") @RequestParam(name = "rootPath", required = false) String rootPath,
            @Parameter(description = "Group ID to start from") @RequestParam(name = "rootGroupId", required = false) String rootGroupId,
            @Parameter(description = "Maximum depth (0 = unlimited)") @RequestParam(name = "depth", required = false, defaultValue = "0") int depth,
            @Parameter(description = "Language override") @RequestParam(name = "lng", required = false, defaultValue = "") String lng,
            HttpServletRequest request,
            HttpServletResponse response) {

        isIpAddressAllowed(request);
        if (Tools.isEmpty(rootPath) && Tools.isEmpty(rootGroupId)) {
            return createNavigationErrorResponse(400, "Bad Request",
                    "Either rootPath or rootGroupId parameter is required.");
        }

        int startGroupId;

        if (Tools.isNotEmpty(rootPath)) {
            startGroupId = resolveRootGroupId(rootPath, request);
        } else {
            try {
                startGroupId = Integer.parseInt(rootGroupId);
            } catch (NumberFormatException e) {
                return createNavigationErrorResponse(400, "Bad Request", "Invalid rootGroupId value.");
            }
        }

        if (startGroupId <= 0) {
            return createNavigationErrorResponse(404, "Not Found", "Root group not found.");
        }

        List<NavigationItem> navigation = headlessNavigationService.buildNavigation(
                rootPath, startGroupId, depth, lng, request.getSession());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        return new ResponseEntity<>(navigation, HttpStatus.OK);
    }

    // ==================== Helpers ====================

    private String normalizePath(String path) {
        if (Tools.isEmpty(path)) {
            return "";
        }
        path = path.trim();
        if (path.startsWith("/")==false) {
            path = "/" + path; //NOSONAR
        }
        return path;
    }

    private boolean isHtmlRequest(String acceptHeader) {
        if (Tools.isEmpty(acceptHeader)) {
            return false;
        }
        String lower = acceptHeader.toLowerCase();
        return lower.contains("text/html") && !lower.contains("application/json");
    }

    private int resolveRootGroupId(String rootPath, HttpServletRequest request) {
        if (Tools.isEmpty(rootPath)) {
            return 0;
        }
        String domain = DocDB.getDomain(request);
        int docId = DocDB.getInstance().getVirtualPathDocId(rootPath, domain);
        if (docId > 0) {
            DocDetails doc = DocDB.getInstance().getDoc(docId);
            if (doc != null) {
                //find parent groups
                List<GroupDetails> parents = GroupsDB.getInstance().getParentGroups(doc.getGroupId());
                if (parents != null && !parents.isEmpty()) {
                    return parents.get(parents.size() - 1).getGroupId(); // Return the top-level parent group ID
                }
            }
        }
        int domainId = CloudToolsForCore.getDomainId(); // Default to root group for the domain
        if (domainId > 0) {
            return domainId; // Return the domain's root group ID
        }
        return 0; // Default to root group
    }

    private String getLanguage(DocDetails doc, String lng) {
        if (Tools.isNotEmpty(lng)) {
            return lng;
        }
        // Use the lng parameter directly since DocDetails doesn't expose lng_code
        return sk.iway.iwcm.Constants.getString("defaultLanguage");
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(int status, String error, String message) {
        ErrorResponse errorResponse = new ErrorResponse(status, error, message);
        return new ResponseEntity<>(errorResponse, org.springframework.http.HttpStatus.valueOf(status));
    }

    private ResponseEntity<ErrorResponse> createValidationErrorResponse(List<FieldError> fieldErrors) {
        ErrorResponse errorResponse = new ErrorResponse(400, "Validation Error", "Request validation failed.");
        errorResponse.setFieldErrors(fieldErrors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<List<NavigationItem>> createNavigationErrorResponse(int status, String error, String message) {
        return new ResponseEntity<>(new ArrayList<>(), org.springframework.http.HttpStatus.valueOf(status));
    }
}
