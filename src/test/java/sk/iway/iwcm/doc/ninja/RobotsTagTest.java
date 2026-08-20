package sk.iway.iwcm.doc.ninja;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.doc.DocBasic.FollowLinksMode;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.test.BaseWebjetTest;

@Execution(ExecutionMode.SAME_THREAD)
class RobotsTagTest extends BaseWebjetTest {

    private String originalXRobotsTagUrls;

    @BeforeEach
    void setUp() {
        originalXRobotsTagUrls = Constants.getString("xRobotsTagUrls");
        Constants.setString("xRobotsTagUrls", "/components/,NOT_SEARCHABLE_PAGE");
    }

    @AfterEach
    void tearDown() {
        Constants.setString("xRobotsTagUrls", originalXRobotsTagUrls);
    }

    @Test
    void robotsDirectivesUseOnlyRestrictiveValues() {
        assertRobots(true, FollowLinksMode.SEARCHABLE, "all");
        assertRobots(true, FollowLinksMode.FOLLOW, "all");
        assertRobots(true, FollowLinksMode.NOFOLLOW, "nofollow");
        assertRobots(false, FollowLinksMode.SEARCHABLE, "noindex, nofollow");
        assertRobots(false, FollowLinksMode.FOLLOW, "noindex");
        assertRobots(false, FollowLinksMode.NOFOLLOW, "noindex, nofollow");
    }

    @Test
    void xRobotsTagHeaderUsesDocumentSettings() {
        DocDetails doc = createDoc(false, FollowLinksMode.FOLLOW);
        MockHttpServletResponse response = new MockHttpServletResponse();

        PathFilter.setXRobotsTagValue("NOT_SEARCHABLE_PAGE", response, doc);

        assertEquals("noindex", response.getHeader("X-Robots-Tag"));

        response = new MockHttpServletResponse();
        doc.setSearchable(true);
        PathFilter.setXRobotsTagValue("NOT_SEARCHABLE_PAGE", response, doc);

        assertEquals("all", response.getHeader("X-Robots-Tag"));
    }

    private void assertRobots(boolean searchable, FollowLinksMode followLinksMode, String expected) {
        DocDetails doc = createDoc(searchable, followLinksMode);
        assertEquals(expected, PathFilter.getXRobotsTagValue(doc));
        assertEquals(expected, createPage(doc).getRobots());
    }

    private DocDetails createDoc(boolean searchable, FollowLinksMode followLinksMode) {
        DocDetails doc = new DocDetails();
        doc.setSearchable(searchable);
        doc.setFollowLinksMode(followLinksMode);
        return doc;
    }

    private Page createPage(DocDetails doc) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("docDetails", doc);
        Ninja ninja = Mockito.mock(Ninja.class);
        Mockito.when(ninja.getRequest()).thenReturn(request);
        return new Page(ninja);
    }
}
