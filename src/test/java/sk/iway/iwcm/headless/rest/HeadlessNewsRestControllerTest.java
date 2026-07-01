package sk.iway.iwcm.headless.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.components.news.NewsActionBean;
import sk.iway.iwcm.components.news.NewsQuery;
import sk.iway.iwcm.headless.dto.FieldError;
import sk.iway.iwcm.headless.dto.HeadlessNewsItem;
import sk.iway.iwcm.headless.dto.HeadlessNewsRequest;
import sk.iway.iwcm.headless.dto.HeadlessNewsResponse;
import sk.iway.iwcm.headless.service.HeadlessNewsService;
import sk.iway.iwcm.system.spring.services.WebjetSecurityService;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for HeadlessNewsRestController.
 * Tests request validation, DTO serialization, and error handling.
 */
class HeadlessNewsRestControllerTest {

    @Mock
    private HeadlessNewsService headlessNewsService;

    @Mock
    private WebjetSecurityService webjetSecurityService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private HeadlessNewsRestController controller;

    @BeforeEach
    void setUp() {
        controller = new HeadlessNewsRestController(headlessNewsService);
    }

    // ==================== DTO Serialization Tests ====================

    @Test
    void testHeadlessNewsRequestSerialization() {
        HeadlessNewsRequest req = new HeadlessNewsRequest();
        List<Integer> groupIds = new ArrayList<>();
        groupIds.add(24);
        req.setGroupIds(groupIds);
        req.setAlsoSubGroups(false);
        req.setPublishType("new");
        req.setOrder("date");
        req.setAscending(false);
        req.setPaging(false);
        req.setPageSize(10);
        req.setOffset(0);
        req.setPerexNotRequired(false);
        req.setLoadData(false);
        req.setCheckDuplicity(false);
        req.setPerexGroup(new ArrayList<>());
        req.setPerexGroupNot(new ArrayList<>());

        assertEquals(24, req.getGroupIds().get(0));
        assertFalse(req.getAlsoSubGroups());
        assertEquals("new", req.getPublishType());
        assertEquals("date", req.getOrder());
        assertFalse(req.getAscending());
        assertFalse(req.getPaging());
        assertEquals(10, req.getPageSize());
        assertEquals(0, req.getOffset());
        assertFalse(req.getPerexNotRequired());
        assertFalse(req.getLoadData());
        assertFalse(req.getCheckDuplicity());
    }

    @Test
    void testHeadlessNewsItemSerialization() {
        HeadlessNewsItem item = new HeadlessNewsItem();
        item.setDocId(100);
        item.setTitle("Test News Article");
        item.setVirtualPath("/news/test-article");
        item.setLanguage("en");
        item.setPerex("Test perex image URL");
        item.setData("<p>News body content</p>");
        item.setGroupId(24);
        item.setTempId(42);
        item.setAvailable(true);
        item.setDateCreated(1704067200000L);

        assertEquals(100, item.getDocId());
        assertEquals("Test News Article", item.getTitle());
        assertEquals("/news/test-article", item.getVirtualPath());
        assertEquals("en", item.getLanguage());
        assertEquals("Test perex image URL", item.getPerex());
        assertEquals("<p>News body content</p>", item.getData());
        assertEquals(24, item.getGroupId());
        assertEquals(42, item.getTempId());
        assertTrue(item.isAvailable());
        assertEquals(1704067200000L, item.getDateCreated());
    }

    @Test
    void testHeadlessNewsResponseSerialization() {
        HeadlessNewsResponse response = new HeadlessNewsResponse();
        List<HeadlessNewsItem> items = new ArrayList<>();

        HeadlessNewsItem item = new HeadlessNewsItem();
        item.setDocId(1);
        item.setTitle("First News");
        items.add(item);

        response.setItems(items);
        response.setPage(1);
        response.setSize(10);
        response.setTotalElements(1);
        response.setTotalPages(1);

        assertEquals(1, response.getItems().size());
        assertEquals(1, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    void testHeadlessNewsResponseEmpty() {
        HeadlessNewsResponse response = new HeadlessNewsResponse();
        response.setItems(new ArrayList<>());
        response.setPage(0);
        response.setSize(0);
        response.setTotalElements(0);
        response.setTotalPages(0);

        assertEquals(0, response.getItems().size());
        assertEquals(0, response.getPage());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
    }

    // ==================== Validation Tests ====================

    @Test
    void testValidateRequest_missingGroupIds() {
        HeadlessNewsRequest req = new HeadlessNewsRequest();

        List<FieldError> errors = validateRequest(req);
        assertFalse(errors.isEmpty(), "Should have validation errors for missing groupIds");
        assertEquals(1, errors.size());
        assertEquals("groupIds", errors.get(0).getField());
    }

    @Test
    void testValidateRequest_emptyGroupIds() {
        HeadlessNewsRequest req = new HeadlessNewsRequest();
        req.setGroupIds(new ArrayList<>());

        List<FieldError> errors = validateRequest(req);
        assertFalse(errors.isEmpty(), "Should have validation errors for empty groupIds");
        assertEquals(1, errors.size());
    }

    @Test
    void testValidateRequest_validRequest() {
        HeadlessNewsRequest req = new HeadlessNewsRequest();
        List<Integer> groupIds = new ArrayList<>();
        groupIds.add(24);
        req.setGroupIds(groupIds);

        List<FieldError> errors = validateRequest(req);
        assertTrue(errors.isEmpty(), "Should pass validation with valid groupIds");
    }

    @Test
    void testValidateRequest_invalidPublishType() {
        HeadlessNewsRequest req = new HeadlessNewsRequest();
        List<Integer> groupIds = new ArrayList<>();
        groupIds.add(24);
        req.setGroupIds(groupIds);
        req.setPublishType("invalid_type");

        List<FieldError> errors = validateRequest(req);
        assertFalse(errors.isEmpty(), "Should fail validation for invalid publishType");
        assertEquals("publishType", errors.get(0).getField());
    }

    @Test
    void testValidateRequest_validPublishTypes() {
        String[] validTypes = {"new", "old", "all", "next", "valid"};

        for (String validType : validTypes) {
            HeadlessNewsRequest req = new HeadlessNewsRequest();
            List<Integer> groupIds = new ArrayList<>();
            groupIds.add(24);
            req.setGroupIds(groupIds);
            req.setPublishType(validType);

            List<FieldError> errors = validateRequest(req);
            assertTrue(errors.isEmpty(), "Should pass validation for publishType: " + validType);
        }
    }

    @Test
    void testValidateRequest_invalidPageSize() {
        HeadlessNewsRequest req = new HeadlessNewsRequest();
        List<Integer> groupIds = new ArrayList<>();
        groupIds.add(24);
        req.setGroupIds(groupIds);
        req.setPageSize(0);

        List<FieldError> errors = validateRequest(req);
        assertFalse(errors.isEmpty(), "Should fail validation for pageSize < 1");
        assertEquals("pageSize", errors.get(0).getField());
    }

    @Test
    void testValidateRequest_negativeOffset() {
        HeadlessNewsRequest req = new HeadlessNewsRequest();
        List<Integer> groupIds = new ArrayList<>();
        groupIds.add(24);
        req.setGroupIds(groupIds);
        req.setOffset(-5);

        List<FieldError> errors = validateRequest(req);
        assertFalse(errors.isEmpty(), "Should fail validation for negative offset");
        assertEquals("offset", errors.get(0).getField());
    }

    @Test
    void testValidateRequest_multipleErrors() {
        HeadlessNewsRequest req = new HeadlessNewsRequest();
        // Missing groupIds
        req.setPageSize(0); // Invalid pageSize
        req.setOffset(-10); // Negative offset

        List<FieldError> errors = validateRequest(req);
        assertEquals(3, errors.size(), "Should have 3 validation errors");
    }

    // ==================== Error Response Tests ====================

    @Test
    void testValidationErrorResponse() {
        List<FieldError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new FieldError("groupIds", "At least one group ID is required."));

        HeadlessNewsResponse errorResponse = new HeadlessNewsResponse();
        errorResponse.setItems(new ArrayList<>());
        errorResponse.setPage(0);
        errorResponse.setSize(0);
        errorResponse.setTotalElements(0);
        errorResponse.setTotalPages(0);

        assertEquals(0, errorResponse.getItems().size());
        assertEquals(0, errorResponse.getPage());
        assertEquals(0, errorResponse.getTotalElements());
        assertEquals(0, errorResponse.getTotalPages());
    }

    // ==================== Helper Methods ====================

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
}
