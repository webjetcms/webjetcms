package sk.iway.iwcm.editor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDBForTest;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocEditorFields;
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

    private String numberedPath(String expectedPath, long count) {
        if (Constants.getBoolean("virtualPathLastSlash")==false) return Tools.replace(expectedPath, "/test-junit-page", "/test-junit-page-"+count);
        if ("/".equals(Constants.getString("editorPageExtension"))) return Tools.replace(expectedPath, "/test-junit-page/", "/test-junit-page-"+count+"/");
        return Tools.replace(expectedPath, ".html", "-"+count+".html");
    }

    private void testAnotherDoc(DocDetails doc, GroupDetails rootGroup, String expectedPath, long count) {
        cleanDoc(doc);
        editorService.setVirtualPath(doc);
        String url = numberedPath(expectedPath, count);

        if (count > 990) {
            String now = (""+Tools.getNow());
            now = now.substring(0, now.length()-5);

            url = Tools.replace(url, "-"+count, "-"+now);
            url = Tools.replace(url, ".html", "");
            if (url.endsWith("/")) url = url.substring(0, url.length()-1);

            //System.out.println("EditorServiceTest.testUrls("+count+"): url = " + url + " expectedPath = " + doc.getVirtualPath());
            //doc.getVirtualPath should startsWith url
            assertTrue(doc.getVirtualPath().startsWith(url), "Virtual path for another subpage should startsWith. Expected: " + url + " but was: " + doc.getVirtualPath());
        } else {
            //System.out.println("EditorServiceTest.testUrls("+count+"): url = " + url + " expectedPath = " + doc.getVirtualPath());
            assertEquals(url, doc.getVirtualPath(), "Virtual path for another subpage should be with page title and number. Expected: " + url + " but was: " + doc.getVirtualPath());
        }
        docDBForTest.addUrlToInternalCache(doc, rootGroup);
    }

    private void testUrls(boolean testConflictLimit) {
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

        testAnotherDoc(doc, rootGroup, expectedPath, 2);
        testAnotherDoc(doc, rootGroup, expectedPath, 3);

        if (testConflictLimit) {
            // Seed intermediate collisions directly to avoid repeating the entire URL search for each page.
            for (long count=4; count<990; count++) {
                cleanDoc(doc);
                doc.setVirtualPath(numberedPath(expectedPath, count));
                docDBForTest.addUrlToInternalCache(doc, rootGroup);
            }
            testAnotherDoc(doc, rootGroup, expectedPath, 990);
            testAnotherDoc(doc, rootGroup, expectedPath, 991);
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
        testUrls(true);

        Constants.setString("editorPageExtension", "/");
        testUrls(false);

        Constants.setString("editorPageExtension", "/");
        Constants.setBoolean("virtualPathLastSlash", false);

        testUrls(false);
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

    /** A pasted headline is cleaned before the editor stores its manual URL. */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void normalizesManualPageUrl(boolean lastSlash) {
        Constants.setBoolean("virtualPathLastSlash", lastSlash);
        String folder = "/autotest-manual-url/novinky-tlacove-spravy/";
        String headline = "Orange vypína 3G sieť, aby mohol ďalej vylepšovať parametre modernejších sietí, vrátane 5G";
        String slug = "orange-vypina-3g-siet-aby-mohol-dalej-vylepsovat-parametre-modernejsich-sieti-vratane-5g";
        DocDetails page = new DocDetails();
        page.setGroupId(67);
        page.setGenerateUrlFromTitle(false);
        page.setVirtualPath(folder + headline + "/");
        EditorForm form = new EditorForm(page);
        editorService.setVirtualPath(page);
        String expected = folder + slug + (lastSlash ? "/" : "");
        assertEquals(expected, page.getVirtualPath());
        EditorDB.setVirtualPath(form);
        assertEquals(expected, form.getVirtualPath());
        form.setVirtualPath(folder + headline + "/");
        EditorToolsForCore.setVirtualPath(form);
        assertEquals(expected, form.getVirtualPath());
    }

    /** The editable suffix can be reused when the page inherits a different folder prefix. */
    @ParameterizedTest
    @ValueSource(strings = { "", ".html", "/" })
    public void normalizesInheritedPageUrl(String ending) {
        Constants.setString("editorPageExtension", ".html");
        DocDetails page = new DocDetails();
        page.setGenerateUrlFromTitle(false);
        page.setUrlInheritGroup(true);
        page.setEditorVirtualPath("Nová správa" + ending);
        DocEditorFields fields = new DocEditorFields();
        fields.setGroupDetails(GroupsDB.getInstance().getGroup(67));
        fields.toDocDetails(page);
        editorService.setVirtualPath(page);
        assertEquals("nova-sprava" + ending, page.getEditorVirtualPath());
        String expectedEnding = ending.isEmpty() ? ".html" : ending;
        assertEquals(DocDB.normalizeVirtualPath("/test-stavov/nova-sprava" + expectedEnding), page.getVirtualPath());
    }

    /** Collision detection uses the URL that will be stored. */
    @Test
    public void resolvesCollisionAfterCleaningManualUrl() {
        Constants.setString("editorPageExtension", ".html");
        DocDetails existing = new DocDetails();
        existing.setGroupId(67);
        existing.setVirtualPath("/autotest-manual-url/nova-sprava.html");
        docDBForTest.addUrlToInternalCache(existing, GroupsDB.getInstance().getGroup(67));

        DocDetails page = new DocDetails();
        cleanDoc(page);
        page.setGroupId(67);
        page.setGenerateUrlFromTitle(false);
        page.setVirtualPath("/autotest-manual-url/Nová správa.html");
        editorService.setVirtualPath(page);
        assertEquals("/autotest-manual-url/nova-sprava-2.html", page.getVirtualPath());
        assertFalse(editorService.getNotify().isEmpty());
    }

    /** Only path text with whitespace, accents or uppercase letters is normalized. */
    @ParameterizedTest
    @CsvSource(value = {
        "/novinky/nova sprava.html | /novinky/nova-sprava.html",
        "/novinky/správa.html | /novinky/sprava.html",
        "/novinky/ORANGE.html | /novinky/orange.html",
        "/novinky/akcia+bonus.html | /novinky/akcia+bonus.html",
        "/novinky/akcia+bonus%2F2026.html?kod=ABC#Obsah | /novinky/akcia+bonus%2F2026.html?kod=ABC#Obsah",
        "/API.v1/Case%20Sensitive/Žltá%20správa.html?q=A+B&lang=sk#Obsah | /api.v1/case%20sensitive/zlta%20sprava.html?q=A+B&lang=sk#Obsah",
        "/API.v1/Nová správa/* | /api.v1/nova-sprava/*"
    }, delimiter = '|')
    public void preservesManualUrlSyntax(String input, String expected) {
        DocDetails page = new DocDetails();
        page.setGroupId(67);
        page.setVirtualPath(input);
        editorService.setVirtualPath(page);
        assertEquals(expected, page.getVirtualPath());
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
