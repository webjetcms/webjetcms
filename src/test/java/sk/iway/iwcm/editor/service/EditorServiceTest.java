package sk.iway.iwcm.editor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDBForTest;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EditorServiceTest extends BaseWebjetTest {

    private EditorService editorService;
    private DocDBForTest docDBForTest;

    private final String title = "Test jUnit page";

    @BeforeAll
    public static void initJpa()
    {
        Constants.setString("jpaAddPackages", "sk.iway.iwcm.components.gdpr.model");
        DBPool.getInstance();
        DBPool.jpaInitialize();

        DocDB.getInstance();
        GroupsDB.getInstance();
    }

    @BeforeEach
    public void setUp() {
        editorService = new EditorService(null, null, null, null, null, null);
        docDBForTest = new DocDBForTest();
    }

    private void cleanDoc(DocDetails doc) {
        doc.setTitle(title);
        doc.setNavbar(title);
        doc.setDocId(-1);
        doc.setVirtualPath("");
    }

    private void testAnotherDoc(DocDetails doc, GroupDetails rootGroup, String expectedPath, long count) {
        String url;
        cleanDoc(doc);
        editorService.setVirtualPath(doc);

        if (Constants.getBoolean("virtualPathLastSlash")==false) url = Tools.replace(expectedPath, "/test-junit-page", "/test-junit-page-"+count);
        else if ("/".equals(Constants.getString("editorPageExtension"))) url = Tools.replace(expectedPath, "/test-junit-page/", "/test-junit-page-"+count+"/");
        else url = Tools.replace(expectedPath, ".html", "-"+count+".html");

        if (count > 990) {
            String now = (""+Tools.getNow());
            now = now.substring(0, now.length()-5);

            url = Tools.replace(url, "-"+count, "-"+now);
            url = Tools.replace(url, ".html", "");
            if (url.endsWith("/")) url = url.substring(0, url.length()-1);

            System.out.println("EditorServiceTest.testUrls("+count+"): url = " + url + " expectedPath = " + doc.getVirtualPath());
            //doc.getVirtualPath should startsWith url
            assertTrue(doc.getVirtualPath().startsWith(url), "Virtual path for another subpage should startsWith. Expected: " + url + " but was: " + doc.getVirtualPath());
        } else {
            System.out.println("EditorServiceTest.testUrls("+count+"): url = " + url + " expectedPath = " + doc.getVirtualPath());
            assertEquals(url, doc.getVirtualPath(), "Virtual path for another subpage should be with page title and number. Expected: " + url + " but was: " + doc.getVirtualPath());
        }
        docDBForTest.addUrlToInternalCache(doc, rootGroup);
    }

    private void testUrls(int maxCount) {
        String expectedPath = "/test-stavov/test-junit-page.html";
        String rootPath = "/test-stavov/";
        if (Constants.getBoolean("virtualPathLastSlash")==false) {
            expectedPath = "/test-stavov/test-junit-page";
            rootPath = "/test-stavov";
        }
        else if ("/".equals(Constants.getString("editorPageExtension"))) {
            expectedPath = "/test-stavov/test-junit-page/";
        }

        GroupDetails rootGroup = GroupsDB.getInstance().getGroup(67);  // 67 is the group id of the group "Test stavov"

        int defaultDocId = rootGroup.getDefaultDocId();

        //root page in folder
        DocDetails doc = new DocDetails();
        cleanDoc(doc);
        doc.setGroupId(rootGroup.getGroupId());

        rootGroup.setDefaultDocId(0);

        editorService.setVirtualPath(doc);
        assertEquals(rootPath, doc.getVirtualPath(), "Virtual path for root folder should be withou page title. Expected: " + rootPath + " but was: " + doc.getVirtualPath());

        rootGroup.setDefaultDocId(defaultDocId);

        //subpage in folder
        doc = new DocDetails();
        cleanDoc(doc);
        doc.setGroupId(rootGroup.getGroupId());

        editorService.setVirtualPath(doc);
        assertEquals(expectedPath, doc.getVirtualPath(), "Virtual path for subpage should be with page title. Expected: " + expectedPath + " but was: " + doc.getVirtualPath());

        //insert it into docDB
        docDBForTest.addUrlToInternalCache(doc, rootGroup);

        //create another page with the same title
        for (long i=0; i<maxCount; i++) {
            testAnotherDoc(doc, rootGroup, expectedPath, i+2);
        }

        rootGroup.setDefaultDocId(defaultDocId);
    }

    /**
     * Test generation of URL addresses for web pages with various settings
     * of virtual path and extension.
     */
    @Test
    @Order(1)
    public void testSetVirtualPath() {
        Constants.setString("editorPageExtension", ".html");
        testUrls(1500);

        Constants.setString("editorPageExtension", "/");
        testUrls(2);

        Constants.setString("editorPageExtension", "/");
        Constants.setBoolean("virtualPathLastSlash", false);

        testUrls(2);
    }

    /**
     * There is reported BUG when ONLY title is set to /ranny-prehlad-07-01-2024 URL is wrong as /ranny-prehlad
     * But in reality it was not a bug, but error in their implementation (not owerriding navbar from template doc).
     */
    @Test
    @Order(2)
    public void testUrlVithNumbers() {
        Constants.setString("editorPageExtension", "/");
        Constants.setBoolean("virtualPathLastSlash", true);

        String title = "Ranný prehľad 07-01-2024";
        String expectedPath = "/test-stavov/ranny-prehlad-07-01-2024/";
        GroupDetails rootGroup = GroupsDB.getInstance().getGroup(67);  // 67 is the group id of the group "Test stavov"

        //root page in folder
        DocDetails doc = new DocDetails();
        doc.setTitle(title);
        doc.setDocId(-1);
        doc.setVirtualPath("");
        doc.setGroupId(rootGroup.getGroupId());

        //test it as in DocDB.saveDoc
        EditorForm ef = new EditorForm(doc);

        editorService.setVirtualPath(doc);
        assertEquals(expectedPath, doc.getVirtualPath(), "Virtual path doesnt contain date. Expected: " + expectedPath + " but was: " + doc.getVirtualPath());

        EditorDB.setVirtualPath(ef);
        assertEquals(expectedPath, ef.getVirtualPath(), "EF virtual path doesnt contain date. Expected: " + expectedPath + " but was: " + ef.getVirtualPath());
    }

    @AfterAll
    public static void tearDown() {
        Constants.setString("editorPageExtension", ".html");
        Constants.setBoolean("virtualPathLastSlash", true);

        //reload DocDB and GroupsDB because we manipulated with them in the test
        DocDB.getInstance(true);
        GroupsDB.getInstance(true);
    }
}