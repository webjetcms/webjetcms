package sk.iway.iwcm.common;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDBForTest;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for UploadFileTools.getPageUploadSubDir, in particular the path computation
 * for new (unsaved) pages via getNewPageVirtualPathFolderName.
 *
 * Uses group 67 ("Test stavov") which is always present in the test database.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UploadFileToolsTest extends BaseWebjetTest {

    private static final int TEST_GROUP_ID = 67; // "Test stavov" group
    private static final String TEST_TITLE = "Test jUnit Upload Page";
    private static final String TEST_TITLE_EXISTING = "Upratovanie";
    private static final String EXPECTED_URL_NAME = "test-junit-upload-page";
    private static final String EXPECTED_URL_NAME_EXISTING = "upratovanie";
    private static final String IMAGE_PREFIX = "/images";

    private DocDBForTest docDBForTest;

    @BeforeAll
    static void initJpa() {
        DBPool.getInstance();
        DBPool.jpaInitialize();

        DocDB.getInstance();
        GroupsDB.getInstance();
    }

    @BeforeEach
    void setUp() {
        docDBForTest = new DocDBForTest();
        Constants.setBoolean("elfinderCreateFolderForPages", true);
        Constants.setBoolean("galleryUploadDirVirtualPath", false);
        Constants.setString("editorPageExtension", ".html");
    }

    /**
     * When no page with the given title exists yet, the upload subdir should contain
     * the plain sanitized title as the folder name (no suffix).
     */
    @Test
    @Order(1)
    void testNewPageUniqueTitleUsesDirectFolderName() {
        String result = UploadFileTools.getPageUploadSubDir(-1, TEST_GROUP_ID, TEST_TITLE, IMAGE_PREFIX);

        assertTrue(result.contains(EXPECTED_URL_NAME),
            "Upload subdir should contain sanitized title '" + EXPECTED_URL_NAME + "'. Got: " + result);
        assertFalse(result.contains(EXPECTED_URL_NAME + "-2"),
            "Upload subdir must NOT contain duplicate suffix when title is unique. Got: " + result);
    }

    /**
     * When a page with the same URL already exists in the group, the upload subdir for
     * a new page with the same title should use the -2 suffix that the page will actually
     * receive on save — matching the behaviour of EditorService.setVirtualPath.
     */
    @Test
    @Order(2)
    void testNewPageDuplicateTitleUsesSuffixedFolderName() {
        // Simulate an existing page occupying the base URL in this group
        GroupDetails group = GroupsDB.getInstance().getGroup(TEST_GROUP_ID);
        String groupUrlPath = GroupsDB.getInstance().getURLPath(TEST_GROUP_ID);

        DocDetails existingDoc = new DocDetails();
        existingDoc.setDocId(-1);
        existingDoc.setTitle(TEST_TITLE);
        existingDoc.setNavbar(TEST_TITLE);
        existingDoc.setVirtualPath(groupUrlPath + "/" + EXPECTED_URL_NAME + ".html");
        existingDoc.setGroupId(TEST_GROUP_ID);
        docDBForTest.addUrlToInternalCache(existingDoc, group);

        // A new page with the same title should receive the -2 upload folder
        String result = UploadFileTools.getPageUploadSubDir(-1, TEST_GROUP_ID, TEST_TITLE, IMAGE_PREFIX);

        assertTrue(result.contains(EXPECTED_URL_NAME + "-2"),
            "Upload subdir for duplicate-title page should contain '" + EXPECTED_URL_NAME + "-2'. Got: " + result);

        //verify existing title
        DocDB docDB = DocDB.getInstance();
        DocDetails mainDoc = docDB.getDoc(93); // "Upratovanie" in group 67
        String existingPath = "/test-stavov/"+EXPECTED_URL_NAME_EXISTING+".html";
        assertEquals(existingPath, mainDoc.getVirtualPath(),
            "Pre-existing page should have expected URL name '" + EXPECTED_URL_NAME_EXISTING + "'. Got: " + mainDoc.getVirtualPath());

        //check existing docid
        result = UploadFileTools.getPageUploadSubDir(mainDoc.getDocId(), TEST_GROUP_ID, TEST_TITLE_EXISTING, IMAGE_PREFIX);
        System.out.println("Result for existing title: " + result);
        String expectedDir = IMAGE_PREFIX + "/test-stavov/"+EXPECTED_URL_NAME_EXISTING;
        assertEquals(expectedDir, result,
            "Upload subdir for existing-title page should be '" + expectedDir + "'. Got: " + result);

        //check new docid with existing title
        result = UploadFileTools.getPageUploadSubDir(-1, TEST_GROUP_ID, TEST_TITLE_EXISTING, IMAGE_PREFIX);
        expectedDir = IMAGE_PREFIX + "/test-stavov/"+EXPECTED_URL_NAME_EXISTING+"-2";
        assertEquals(expectedDir, result,
            "Upload subdir for new page with existing title should be '" + expectedDir + "'. Got: " + result);
    }

    @AfterAll
    static void tearDown() {
        Constants.setBoolean("elfinderCreateFolderForPages", false);
        Constants.setBoolean("galleryUploadDirVirtualPath", false);
        Constants.setString("editorPageExtension", ".html");

        // Reload DocDB and GroupsDB to remove test URLs from internal cache
        DocDB.getInstance(true);
        GroupsDB.getInstance(true);
    }
}
