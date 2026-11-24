package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Checks that direct document writes preserve the URL supplied by their callers. */
class DocDBVirtualPathTest extends BaseWebjetTest {
    private final DocDetails page = new DocDetails();
    private static DocDB documents;

    @BeforeAll
    static void initializeDatabase() {
        DBPool.getInstance();
        DBPool.jpaInitialize();
        documents = DocDB.getInstance();
    }

    @BeforeEach
    void preparePage() {
        page.setGroupId(67);
        page.setTitle("autotest-url-" + UUID.randomUUID());
        page.setNavbar(page.getTitle());
        page.setData("Autotest content");
    }

    @AfterEach
    void deletePage() {
        if (page.getDocId() > 0) assertTrue(DocDB.deleteDoc(page.getDocId(), null, false));
    }

    /** The file index uses the exact source path to find the document and its permission metadata. */
    @Test
    void preservesIndexedFileKey() {
        String file = "/files/" + page.getTitle() + "/ProductSheet.pdf";
        String key = file + ".html";
        page.setExternalLink(file);
        page.setVirtualPath(key);
        page.setPasswordProtected("10");

        assertTrue(DocDB.saveDoc(page));
        String domain = GroupsDB.getInstance().getDomain(page.getGroupId());
        assertEquals(page.getDocId(), documents.getDocIdFromURLImpl(key, domain));
        DocDetails stored = documents.getDoc(page.getDocId(), -1, false);
        assertNotNull(stored);
        assertEquals(key, stored.getVirtualPath());
        assertEquals(file, stored.getExternalLink());
        assertEquals("10", stored.getPasswordProtected());
    }

    /** Updating stock through the direct save path must keep the product's existing URL. */
    @Test
    void preservesProductUrlOnStockUpdate() {
        String url = "/products/" + page.getTitle() + "/ModelX.html";
        page.setVirtualPath(url);
        page.setFieldB("3");
        assertTrue(DocDB.saveDoc(page));

        DocDetails product = documents.getDoc(page.getDocId(), -1, false);
        assertNotNull(product);
        product.setFieldB("2");
        assertTrue(DocDB.saveDoc(product));
        DocDetails stored = documents.getDoc(page.getDocId(), -1, false);
        assertNotNull(stored);
        assertEquals("2", stored.getFieldB());
        assertEquals(url, stored.getVirtualPath());
    }
}
