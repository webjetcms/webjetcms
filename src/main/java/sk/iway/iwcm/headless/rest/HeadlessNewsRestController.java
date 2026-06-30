package sk.iway.iwcm.headless.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.headless.dto.ErrorResponse;
import sk.iway.iwcm.headless.dto.FieldError;
import sk.iway.iwcm.headless.dto.HeadlessNewsRequest;
import sk.iway.iwcm.headless.dto.HeadlessNewsResponse;
import sk.iway.iwcm.headless.service.HeadlessNewsService;

import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for headless news listing.
 * Accepts POST requests with NewsActionBean-compatible JSON parameters
 * and returns a list of DocDetails items with pagination metadata.
 *
 * Endpoint: POST /rest/headless/v1/news
 */
@RestController
@RequestMapping("/rest/headless/v1")
@Tag(name = "Headless News", description = "News listing endpoint for headless consumption")
public class HeadlessNewsRestController extends sk.iway.iwcm.rest.RestController {

    private final HeadlessNewsService headlessNewsService;
    private final ObjectMapper objectMapper;

    @Autowired
    public HeadlessNewsRestController(HeadlessNewsService headlessNewsService) {
        this.headlessNewsService = headlessNewsService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * List news via the headless API.
     *
     * @param request   the news request parameters (NewsActionBean-compatible)
     * @param httpRequest the HTTP request
     * @param response  the HTTP response
     * @return HeadlessNewsResponse with news items and pagination
     */
    @PostMapping(value = "/news", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "List news",
            description = "Lists news items via the headless API with NewsActionBean-compatible parameters.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "News list returned"),
                    @ApiResponse(responseCode = "400", description = "Validation errors"),
                    @ApiResponse(responseCode = "401", description = "IP not allowed")
            }
    )
    public ResponseEntity<HeadlessNewsResponse> listNews(
            @RequestBody HeadlessNewsRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        // Check IP whitelist first (inherited from RestController)
        isIpAddressAllowed(httpRequest);

        // Validate request body
        List<FieldError> validationErrors = validateRequest(request);
        if (!validationErrors.isEmpty()) {
            return createValidationErrorResponse(validationErrors);
        }

        // Execute news query via service
        HeadlessNewsResponse newsResponse = headlessNewsService.listNews(request);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        try {
            String json = objectMapper.writeValueAsString(newsResponse);
            HeadlessNewsResponse result = objectMapper.readValue(json, HeadlessNewsResponse.class);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            HeadlessNewsResponse errorResponse = new HeadlessNewsResponse();
            errorResponse.setItems(new ArrayList<>());
            errorResponse.setPage(0);
            errorResponse.setSize(0);
            errorResponse.setTotalElements(0);
            errorResponse.setTotalPages(0);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validates the headless news request.
     * Returns field-level errors for invalid/missing parameters.
     */
    private List<FieldError> validateRequest(HeadlessNewsRequest request) {
        List<FieldError> errors = new ArrayList<>();

        // groupIds is required - must provide at least one group
        if (request.getGroupIds() == null || request.getGroupIds().isEmpty()) {
            errors.add(new FieldError("groupIds", "At least one group ID is required."));
        }

        // Validate publishType if provided
        String publishType = request.getPublishType();
        if (publishType != null && !publishType.isEmpty()) {
            boolean valid = false;
            for (NewsActionBean.PublishType pt : NewsActionBean.PublishType.values()) {
                if (pt.name().equalsIgnoreCase(publishType)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                errors.add(new FieldError("publishType",
                        "Invalid publishType. Must be one of: new, old, all, next, valid."));
            }
        }

        // Validate order if provided
        String order = request.getOrder();
        if (order != null && !order.isEmpty()) {
            boolean valid = false;
            try {
                NewsQuery.OrderEnum.valueOf(order.toUpperCase());
                valid = true;
            } catch (IllegalArgumentException e) {
                // ignore, will default to date
            }
        }

        // Validate pageSize boundary
        Integer pageSize = request.getPageSize();
        if (pageSize != null && pageSize < 1) {
            errors.add(new FieldError("pageSize", "pageSize must be >= 1."));
        }

        // Validate offset boundary
        Integer offset = request.getOffset();
        if (offset != null && offset < 0) {
            errors.add(new FieldError("offset", "offset must be >= 0."));
        }

        return errors;
    }

    private ResponseEntity<ErrorResponse> createValidationErrorResponse(List<FieldError> fieldErrors) {
        ErrorResponse errorResponse = new ErrorResponse(400, "Validation Error", "Request validation failed.");
        errorResponse.setFieldErrors(fieldErrors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
