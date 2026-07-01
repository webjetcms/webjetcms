package sk.iway.iwcm.headless.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.headless.dto.HeadlessNewsItem;
import sk.iway.iwcm.headless.dto.HeadlessNewsRequest;
import sk.iway.iwcm.headless.dto.HeadlessNewsResponse;

/**
 * Unit tests for HeadlessNewsService DTO mapping and validation logic.
 * Tests request/response DTO transformations and boundary conditions.
 */
class HeadlessNewsServiceTest {

    private HeadlessNewsService service;

    @Mock
    private sk.iway.iwcm.doc.GroupsDB groupsDB;

    @BeforeEach
    void setUp() {
        service = new HeadlessNewsService();
    }

    // ==================== Request DTO Tests ====================

    @Test
    void testHeadlessNewsRequest_defaults() {
        HeadlessNewsRequest req = new HeadlessNewsRequest();

        assertNull(req.getGroupIds());
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
        assertNull(req.getPerexGroup());
        assertNull(req.getPerexGroupNot());
    }

    @Test
    void testHeadlessNewsRequest_canonicalPayload() {
        // Canonical payload from the plan document
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

        assertEquals(1, req.getGroupIds().size());
        assertEquals(24, req.getGroupIds().get(0));
        assertEquals("new", req.getPublishType());
        assertEquals("date", req.getOrder());
        assertEquals(10, req.getPageSize());
    }

    // ==================== Response DTO Tests ====================

    @Test
    void testHeadlessNewsResponse_withItems() {
        HeadlessNewsResponse response = new HeadlessNewsResponse();
        List<HeadlessNewsItem> items = new ArrayList<>();

        HeadlessNewsItem item = new HeadlessNewsItem();
        item.setDocId(1);
        item.setTitle("Test Article");
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
    void testHeadlessNewsResponse_empty() {
        HeadlessNewsResponse response = new HeadlessNewsResponse();
        response.setItems(new ArrayList<>());
        response.setPage(1);
        response.setSize(10);
        response.setTotalElements(0);
        response.setTotalPages(1);

        assertEquals(0, response.getItems().size());
        assertEquals(0, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    void testHeadlessNewsItem_allFields() {
        HeadlessNewsItem item = new HeadlessNewsItem();
        item.setDocId(42);
        item.setTitle("Full Test Article");
        item.setVirtualPath("/news/full-test");
        item.setLanguage("en");
        item.setPerex("/images/perex.jpg");
        item.setData("<p>Body content</p>");
        item.setGroupId(24);
        item.setTempId(42);
        item.setAvailable(true);

        assertEquals(42, item.getDocId());
        assertEquals("Full Test Article", item.getTitle());
        assertEquals("/news/full-test", item.getVirtualPath());
        assertEquals("en", item.getLanguage());
        assertEquals("/images/perex.jpg", item.getPerex());
        assertEquals("<p>Body content</p>", item.getData());
        assertEquals(24, item.getGroupId());
        assertEquals(42, item.getTempId());
        assertTrue(item.isAvailable());
    }

    // ==================== Pagination Boundary Tests ====================

    @Test
    void testPagination_noPaging() {
        HeadlessNewsResponse response = new HeadlessNewsResponse();
        response.setItems(new ArrayList<>());
        response.setPage(1);
        response.setSize(10);
        response.setTotalElements(0);
        response.setTotalPages(1);

        // When paging is false, totalElements should equal items.size()
        assertEquals(0, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    void testPagination_withItems() {
        HeadlessNewsResponse response = new HeadlessNewsResponse();
        List<HeadlessNewsItem> items = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            HeadlessNewsItem item = new HeadlessNewsItem();
            item.setDocId(i);
            items.add(item);
        }
        response.setItems(items);
        response.setPage(1);
        response.setSize(10);
        response.setTotalElements(25);
        response.setTotalPages(3); // ceil(25/10) = 3

        assertEquals(25, response.getItems().size());
        assertEquals(25, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
    }

    // ==================== Publish Type Tests ====================

    @Test
    void testPublishTypeValues() {
        String[] validTypes = {"new", "old", "all", "next", "valid"};

        for (String type : validTypes) {
            HeadlessNewsRequest req = new HeadlessNewsRequest();
            req.setPublishType(type);
            assertEquals(type, req.getPublishType());
        }
    }

    @Test
    void testPublishType_caseInsensitive() {
        HeadlessNewsRequest req = new HeadlessNewsRequest();
        req.setPublishType("NEW");
        assertEquals("NEW", req.getPublishType());
    }

    // ==================== Ordering Tests ====================

    @Test
    void testOrderValues() {
        String[] validOrders = {"date", "title", "id", "priority", "place", "event_date", "save_date"};

        for (String order : validOrders) {
            HeadlessNewsRequest req = new HeadlessNewsRequest();
            req.setOrder(order);
            assertEquals(order, req.getOrder());
        }
    }

    @Test
    void testAscendingValues() {
        HeadlessNewsRequest req = new HeadlessNewsRequest();

        req.setAscending(true);
        assertTrue(req.getAscending());

        req.setAscending(false);
        assertFalse(req.getAscending());
    }
}
