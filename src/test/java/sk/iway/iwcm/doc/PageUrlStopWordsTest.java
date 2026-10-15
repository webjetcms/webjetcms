package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies template overrides, legacy sanitization and page URL generation without database writes. */
class PageUrlStopWordsTest extends BaseWebjetTest {
    private static final String FOLDER_PATH = "/news-a-events/";
    private static final String PAGE_NAME = "autotest news a events";

    private boolean originalRemove;
    private String originalWords;
    private String originalExtension;
    private boolean originalLastSlash;
    private int originalLinkType;
    private TemplateDetails template;
    private GroupDetails group;
    private DocDetails page;
    private GroupsDB groups;
    private MockedStatic<TemplatesDB> templateAccess;
    private MockedStatic<GroupsDB> groupAccess;
    private MockedStatic<InitServlet> initialization;

    @BeforeEach
    void setUp() {
        originalRemove = Constants.getBoolean("urlRemoveSpojky");
        originalWords = Constants.getString("urlRemoveSpojkyList");
        originalExtension = Constants.getString("editorPageExtension");
        originalLastSlash = Constants.getBoolean("virtualPathLastSlash");
        originalLinkType = Constants.getInt("linkType");
        Constants.setString("urlRemoveSpojkyList", "a,and");
        Constants.setString("editorPageExtension", ".html");
        Constants.setBoolean("virtualPathLastSlash", true);
        Constants.setInt("linkType", Constants.LINK_TYPE_HTML);
        initialization = mockStatic(InitServlet.class, CALLS_REAL_METHODS);
        initialization.when(InitServlet::isWebjetInitialized).thenReturn(true);

        template = new TemplateDetails();
        template.setTempId(123);
        TemplatesDB templates = mock(TemplatesDB.class);
        when(templates.getTemplate(123)).thenReturn(template);
        templateAccess = mockStatic(TemplatesDB.class);
        templateAccess.when(TemplatesDB::getInstance).thenReturn(templates);

        group = new GroupDetails();
        group.setGroupId(42);
        group.setDefaultDocId(1);
        group.setUrlDirName("news-a-events");
        groups = mock(GroupsDB.class);
        when(groups.getGroup(42)).thenReturn(group);
        when(groups.getGroupsAll()).thenReturn(List.of(group));
        when(groups.getDomain(42)).thenReturn("autotest.example");
        groupAccess = mockStatic(GroupsDB.class);
        groupAccess.when(GroupsDB::getInstance).thenReturn(groups);
        groupAccess.when(() -> GroupsDB.getURLPathGroup(List.of(group), 42)).thenReturn(FOLDER_PATH);

        page = new DocDetails();
        page.setDocId(-1);
        page.setGroupId(42);
        page.setTempId(123);
        page.setTitle(PAGE_NAME);
        page.setNavbar(PAGE_NAME);
        page.setVirtualPath("");
    }

    @AfterEach
    void tearDown() {
        groupAccess.close();
        templateAccess.close();
        initialization.close();
        Constants.setBoolean("urlRemoveSpojky", originalRemove);
        Constants.setString("urlRemoveSpojkyList", originalWords);
        Constants.setString("editorPageExtension", originalExtension);
        Constants.setBoolean("virtualPathLastSlash", originalLastSlash);
        Constants.setInt("linkType", originalLinkType);
    }

    /** Every template/global combination applies to the page segment and preserves the supplied folder prefix. */
    @ParameterizedTest
    @CsvSource({
        "0, false, autotest-news-a-events", "0, true, autotest-news-events",
        "1, false, autotest-news-events", "1, true, autotest-news-events",
        "2, false, autotest-news-a-events", "2, true, autotest-news-a-events"
    })
    void generatesPageUrlWithTemplateMode(int mode, boolean globalRemove, String expectedSlug) {
        template.setUrlRemoveSpojkyMode(mode);
        Constants.setBoolean("urlRemoveSpojky", globalRemove);
        for (String extension : List.of(".html", "/")) {
            Constants.setString("editorPageExtension", extension);
            assertEquals(FOLDER_PATH + expectedSlug + extension, DocDB.getURL(page, group, FOLDER_PATH));
        }
        assertEquals(globalRemove, Constants.getBoolean("urlRemoveSpojky"));
    }

    /** Existing boolean overloads continue to require both the caller and global configuration to enable removal. */
    @ParameterizedTest
    @CsvSource({ "false, false, false", "false, true, false", "true, false, false", "true, true, true" })
    void preservesLegacyBooleanContract(boolean requested, boolean configured, boolean removed) {
        Constants.setBoolean("urlRemoveSpojky", configured);
        String input = "news-a-events.html";
        assertEquals(removed ? "news-events.html" : input, DocTools.removeCharsDir(input, requested));
        assertEquals(configured ? "news-events.html" : input, DocTools.removeCharsDir(input));
    }

    /** Missing templates and values from older imports use the current global setting. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void fallsBackToConfiguration(boolean configured) {
        Constants.setBoolean("urlRemoveSpojky", configured);
        String expected = configured ? "news-events.html" : "news-a-events.html";
        assertEquals(expected, DocTools.removeCharsDir("news-a-events.html", (TemplateDetails) null));
        template.setUrlRemoveSpojkyMode(null);
        assertEquals(expected, DocTools.removeCharsDir("news-a-events.html", template));
        page.setTempId(999);
        assertEquals(FOLDER_PATH + "autotest-" + expected, DocDB.getURL(page, group, FOLDER_PATH));
    }

    /** Forced removal uses the configured word list and still sanitizes invalid characters. */
    @Test
    void usesConfiguredWordsAndSanitizesBothModes() {
        Constants.setBoolean("urlRemoveSpojky", false);
        Constants.setString("urlRemoveSpojkyList", "and");
        template.setUrlRemoveSpojkyMode(1);
        assertEquals("news-a-events-updates.html", DocTools.removeCharsDir("news a events and updates?.html", template));
        template.setUrlRemoveSpojkyMode(2);
        assertEquals("news-a-events-and-updates.html", DocTools.removeCharsDir("news a events and updates?.html", template));
    }

    /** Complete manual URLs and folder home pages do not apply a page-template override to their folder names. */
    @Test
    void preservesExplicitUrlsAndFolderHomePage() {
        Constants.setBoolean("urlRemoveSpojky", true);
        template.setUrlRemoveSpojkyMode(1);
        page.setVirtualPath(FOLDER_PATH + "manual-a-page.html");
        assertEquals(page.getVirtualPath(), DocDB.getURL(page, group, FOLDER_PATH));
        page.setVirtualPath("");
        page.setDocId(group.getDefaultDocId());
        assertEquals(FOLDER_PATH, DocDB.getURL(page, group, FOLDER_PATH));
    }

    /** Initial URL-cache loading must not initialize templates before JPA is ready. */
    @Test
    void generatesStartupFallbackWithoutLoadingTemplates() {
        initialization.when(InitServlet::isWebjetInitialized).thenReturn(false);
        Constants.setBoolean("urlRemoveSpojky", true);
        assertEquals(FOLDER_PATH + "autotest-news-events.html", DocDB.getURL(page, group, FOLDER_PATH));
        templateAccess.verify(TemplatesDB::getInstance, never());
    }

    /** Bulk regeneration uses each version's template even when its stop-word mode overrides the global setting. */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void bulkRegenerationUsesEachVersionsTemplate(boolean globalRemove) {
        Constants.setBoolean("urlRemoveSpojky", globalRemove);
        template.setUrlRemoveSpojkyMode(2);
        page.setDocId(11);
        page.setVirtualPath("/old/autotest-news-a-events.html");
        page.setGenerateUrlFromTitle(true);
        when(groups.getGroupNamePath(42)).thenReturn("/new");

        TemplateDetails pendingTemplate = new TemplateDetails();
        pendingTemplate.setTempId(124);
        pendingTemplate.setUrlRemoveSpojkyMode(1);
        when(TemplatesDB.getInstance().getTemplate(124)).thenReturn(pendingTemplate);
        DocHistory pending = new DocHistory();
        pending.setId(101L);
        pending.setDocId(page.getDocId());
        pending.setGroupId(page.getGroupId());
        pending.setTempId(pendingTemplate.getTempId());
        pending.setTitle(PAGE_NAME);
        pending.setNavbar(PAGE_NAME);
        pending.setVirtualPath("/old/autotest-news-events.html");
        pending.setGenerateUrlFromTitle(true);

        DocDB documents = mock(DocDB.class);
        when(documents.getDocByGroup(42, DocDB.ORDER_TITLE, true, -1, -1, true, false)).thenReturn(List.of(page));
        DocHistoryRepository history = mock(DocHistoryRepository.class);
        when(history.findPendingByGroupId(42)).thenReturn(List.of(pending));

        try (var docAccess = mockStatic(DocDB.class, call -> {
                 if ("getInstance".equals(call.getMethod().getName())) return documents;
                 return call.callRealMethod();
             });
             var tools = mockStatic(Tools.class, CALLS_REAL_METHODS); var editor = mockStatic(EditorDB.class);
             var audit = mockStatic(Adminlog.class); var queries = mockConstruction(SimpleQuery.class)) {
            docAccess.when(() -> DocDB.getRootGroupL(42, null, -1)).thenReturn(new int[] { 42, 0, 0 });
            docAccess.when(() -> DocDB.getGroupDiskPath(List.of(group), 42)).thenReturn("/new/");
            tools.when(() -> Tools.getSpringBean("docHistoryRepository", DocHistoryRepository.class)).thenReturn(history);
            editor.when(() -> EditorDB.setVirtualPath(any(EditorForm.class))).thenCallRealMethod();

            WebpagesService.regenerateUrl(42, new Identity(), new MockHttpServletRequest(), true);

            assertAll(
                () -> verify(queries.constructed().get(0)).execute("UPDATE documents SET virtual_path=?, file_name=?, root_group_l1=?, root_group_l2=?, root_group_l3=?, sync_status=1 WHERE doc_id=?",
                    "/new/autotest-news-a-events.html", "/new", 42, 0, 0, 11),
                () -> verify(history).updateVirtualPath(101L, "/new/autotest-news-events.html")
            );
        }
    }

    /** The editor's temporary page and collision retries retain the selected template in current and legacy save paths. */
    @ParameterizedTest
    @CsvSource({ "1, false, /news-a-events/autotest-news-events-2.html", "2, true, /news-events/autotest-news-a-events-2.html" })
    void editorRetainsTemplateDuringCollisionResolution(int mode, boolean configured, String expectedUrl) {
        template.setUrlRemoveSpojkyMode(mode);
        Constants.setBoolean("urlRemoveSpojky", configured);
        DocDB documents = mock(DocDB.class);
        try (var docAccess = mockStatic(DocDB.class, CALLS_REAL_METHODS)) {
            docAccess.when(DocDB::getInstance).thenReturn(documents);
            when(documents.getDocIdFromURLImpl(anyString(), anyString())).thenReturn(77, -1);
            assertEquals(77, EditorService.computeVirtualPathForNewPage(page, groups));
            assertEquals(expectedUrl, page.getVirtualPath());

            EditorForm legacyPage = new EditorForm();
            legacyPage.setDocId(-1);
            legacyPage.setGroupId(42);
            legacyPage.setTempId(123);
            legacyPage.setTitle(PAGE_NAME);
            legacyPage.setNavbar(PAGE_NAME);
            legacyPage.setVirtualPath("");
            when(documents.getDocIdFromURLImpl(anyString(), anyString())).thenReturn(-1);
            EditorToolsForCore.setVirtualPath(legacyPage);
            assertEquals(expectedUrl.replace("-2.html", ".html"), legacyPage.getVirtualPath());
            legacyPage.setVirtualPath("");
            EditorDB.setVirtualPath(legacyPage);
            assertEquals(expectedUrl.replace("-2.html", ".html"), legacyPage.getVirtualPath());
        }
    }
}
