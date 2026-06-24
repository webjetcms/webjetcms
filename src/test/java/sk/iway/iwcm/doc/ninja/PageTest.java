package sk.iway.iwcm.doc.ninja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.test.BaseWebjetTest;

class PageTest extends BaseWebjetTest {

    @BeforeAll
    static void initJpa() {
        DBPool.getInstance();
        DBPool.jpaInitialize();
        DocDB.getInstance();
    }

    @Test
    void getSeoImageDimensions() {
        DocDB docDB = DocDB.getInstance();
        DocDetails doc = docDB.getDoc(141);
        assertNotNull(doc);

        Page page = createPage(doc);

        assertEquals(265, page.getSeoImageWidth());
        assertEquals(225, page.getSeoImageHeight());
    }

    @Test
    void getSeoImageDimensionsWithoutImage() {
        Page page = createPage(null);

        assertEquals(0, page.getSeoImageWidth());
        assertEquals(0, page.getSeoImageHeight());
    }

    private Page createPage(DocDetails doc) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("docDetails", doc);
        return new Ninja(request).getPage();
    }
}
