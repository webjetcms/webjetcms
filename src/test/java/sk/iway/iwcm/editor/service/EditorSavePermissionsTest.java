package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.tools.AiToolService;
import sk.iway.iwcm.components.basket.rest.ProductListRestController;
import sk.iway.iwcm.components.basket.rest.ProductListService;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocEditorFields;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.MultigroupMapping;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.rest.GroupsRestController;
import sk.iway.iwcm.editor.rest.WebpagesDatatable;
import sk.iway.iwcm.editor.rest.WebpagesRestController;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;

/** Checks authorization against stored pages independently of submitted destinations. */
class EditorSavePermissionsTest {
    private EditorService editor;
    private Identity user;
    private DocDB documents;
    private GroupsDB groups;
    private DocDetails original;
    @AutoClose private MockedStatic<GroupsDB> groupAccess;
    @AutoClose private MockedStatic<DocDB> docAccess;
    @AutoClose private MockedStatic<MultigroupMappingDB> mappings;
    @AutoClose private MockedStatic<Constants> constants;

    @BeforeEach
    void setUp() {
        user = mock(Identity.class);
        when(user.getUserId()).thenReturn(7);
        when(user.isEnabledItem(anyString())).thenReturn(true);
        when(user.getEditableGroups(true)).thenReturn("25");
        when(user.getEditablePages()).thenReturn("");
        documents = mock(DocDB.class);
        groups = mock(GroupsDB.class);
        for (int id : new int[] {10, 25, 26}) {
            GroupDetails group = new GroupDetails();
            group.setGroupId(id);
            when(groups.getGroup(id)).thenReturn(group);
        }
        when(groups.getParents(10)).thenReturn("1");
        when(groups.getParents(25)).thenReturn("1");
        when(groups.getParents(26)).thenReturn("25,1");
        original = page(42, 10);
        when(documents.getDoc(42, -1, false)).thenReturn(original);

        groupAccess = mockStatic(GroupsDB.class, CALLS_REAL_METHODS);
        groupAccess.when(GroupsDB::getInstance).thenReturn(groups);
        docAccess = mockStatic(DocDB.class);
        docAccess.when(DocDB::getInstance).thenReturn(documents);
        mappings = mockStatic(MultigroupMappingDB.class);
        constants = mockStatic(Constants.class, CALLS_REAL_METHODS);
        constants.when(() -> Constants.getString("webpagesFunctionsPerms")).thenReturn("menuWebpages");
        constants.when(() -> Constants.getBoolean("adminCheckUserGroups")).thenReturn(false);

        editor = new EditorService();
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(call -> call.getArgument(0));
        ReflectionTestUtils.setField(editor, "currentUser", user);
        ReflectionTestUtils.setField(editor, "docDB", documents);
        ReflectionTestUtils.setField(editor, "groupsDB", groups);
        ReflectionTestUtils.setField(editor, "prop", prop);
    }

    /** A permitted destination cannot grant access to a foreign page, even for a draft. */
    @Test
    void rejectsForeignPageBeforeEventsOrPersistence() {
        DocDetails submitted = page(42, 25);
        DocDetailsRepository pages = mock(DocDetailsRepository.class);
        DocHistoryRepository history = mock(DocHistoryRepository.class);
        ReflectionTestUtils.setField(editor, "docRepo", pages);
        ReflectionTestUtils.setField(editor, "historyRepo", history);
        try (var events = mockStatic(WebjetEventPublisher.class)) {
            assertThrows(RuntimeException.class, () -> editor.saveEditedDoc(submitted));
            verifyNoInteractions(pages, history);
            events.verifyNoInteractions();
        }
        assertEquals(10, original.getGroupId());
        assertEquals("autotest original", original.getTitle());
    }

    /** Preparing defaults for a folder with a template preserves its HTML and leaves the template untouched. */
    @Test
    void copiesConfiguredTemplateForNewPage() {
        EditorService service = spy(editor);
        GroupDetails folder = groups.getGroup(25);
        folder.setTempId(4);
        folder.setNewPageDocIdTemplate(1);
        original.setDocId(1);
        original.setData("<section>Template content</section>");
        doReturn(original).when(service).getDoc(1, -1);
        doReturn(false).when(service).setSortPriority(any(), any());
        try (var defaults = mockStatic(WebpagesService.class); var facade = mockStatic(EditorFacade.class)) {
            DocDetails page = service.prepareNewDocForEditor(-1, folder);
            assertEquals(-1, page.getDocId());
            assertEquals(25, page.getGroupId());
            assertEquals(4, page.getTempId());
            assertEquals(original.getData(), page.getData());
            assertEquals(1, original.getDocId());
            assertEquals(10, original.getGroupId());
        }
    }

    /** Preparing a named-tool page leaves temporary media and the open editor's preview domain intact. */
    @Test
    void preparingPagePreservesOpenEditor() {
        var request = new MockHttpServletRequest();
        request.getSession().setAttribute("preview.editorDomainName", "original.example");
        groups.getGroup(25).setDomainName("folder.example");
        var saves = mock(EditorService.class);
        DocDetails defaults = page(-1, 25);
        defaults.setEditorFields(new DocEditorFields());
        when(saves.prepareNewDocForEditor(-1, groups.getGroup(25))).thenReturn(defaults);
        var media = mock(MediaService.class);
        var facade = new EditorFacade(saves, null, media, null, request);
        try (var tools = mockStatic(AiToolService.class)) {
            tools.when(() -> AiToolService.isToolRequest(request, WebpagesRestController.class)).thenReturn(true);
            facade.getDocForEditor(-1, -1, 25);
            assertFalse(defaults.getEditorFields().isRequestPublish());
            verifyNoInteractions(media);
            assertEquals("original.example", request.getSession().getAttribute("preview.editorDomainName"));
        }
        facade.getDocForEditor(-1, -1, 25);
        verify(media).deleteTempMedia();
        assertEquals("folder.example", request.getSession().getAttribute("preview.editorDomainName"));
    }

    /** The incomplete editor payload from a draft create is rejected before touching the editor or media. */
    @Test
    void rejectsMissingEditorDestinationBeforeSaving() {
        EditorService saves = mock(EditorService.class);
        EditorFacade facade = new EditorFacade(saves, null, null, null, null);
        DocDetails doc = page(-1, 25);
        for (DocEditorFields fields : new DocEditorFields[] {null, new DocEditorFields()}) {
            doc.setEditorFields(fields);
            assertEquals(400, assertThrows(ResponseStatusException.class, () -> facade.save(doc)).getStatusCode().value());
        }
        verifyNoInteractions(saves);
    }

    /** A new editor page reaches JPA without its temporary ID and uses the ID from that insert. */
    @Test
    void persistsNewPageWithGeneratedId() {
        DocDetails draft = page(-1, 25);
        var pages = mock(DocDetailsRepository.class);
        ReflectionTestUtils.setField(editor, "docRepo", pages);
        when(pages.save(draft)).thenAnswer(call -> {
            assertEquals(0, draft.getDocId());
            return page(88, 25);
        });
        ReflectionTestUtils.invokeMethod(editor, "docSave", draft, true, mock(DebugTimer.class));
        assertEquals(88, draft.getDocId());
        verify(pages).save(draft);
        verifyNoMoreInteractions(pages);
    }

    /** A page-specific grant authorizes editing in place, but not moving into another restricted folder. */
    @Test
    void separatesPageGrantsFromDestinationPermissions() {
        when(user.getEditablePages()).thenReturn("42");
        assertDoesNotThrow(() -> editor.checkSavePermissions(user, page(42, 10)));
        assertDoesNotThrow(() -> editor.checkSavePermissions(user, page(42, 25)));
        original.setGroupId(25);
        assertThrows(RuntimeException.class, () -> editor.checkSavePermissions(user, page(42, 10)));
    }

    /** Inherited folder grants still allow ordinary edits and moves between permitted folders. */
    @Test
    void preservesInheritedFolderPermissions() {
        original.setGroupId(26);
        assertDoesNotThrow(() -> editor.checkSavePermissions(user, page(42, 26)));
        assertDoesNotThrow(() -> editor.checkSavePermissions(user, page(42, 25)));
    }

    /** Creation checks a real destination; empty and nonexistent IDs cannot use the legacy group fallback. */
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 999})
    void rejectsInvalidDestination(int groupId) {
        when(user.getEditablePages()).thenReturn("42");
        assertThrows(RuntimeException.class, () -> editor.checkSavePermissions(user, page(42, groupId)));
        assertThrows(RuntimeException.class, () -> editor.checkSavePermissions(user, page(-1, groupId)));
    }

    /** Creation and page-save permissions remain separate. */
    @Test
    void preservesOperationPermissionsAndRejectsMissingPages() {
        assertDoesNotThrow(() -> editor.checkSavePermissions(user, page(-1, 25)));
        when(user.isDisabledItem("addPage")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> editor.checkSavePermissions(user, page(-1, 25)));
        when(user.isDisabledItem("pageSave")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> editor.checkSavePermissions(user, page(42, 25)));
        when(user.isDisabledItem("pageSave")).thenReturn(false);
        assertThrows(RuntimeException.class, () -> editor.checkSavePermissions(user, page(999, 25)));
        assertThrows(RuntimeException.class, () -> editor.checkSavePermissions(null, page(42, 25)));
    }

    /** A submitted access-group change cannot bypass the stored page's access restrictions. */
    @Test
    void checksStoredAccessGroups() {
        original.setGroupId(25);
        original.setPasswordProtected("99");
        DocDetails submitted = page(42, 25);
        constants.when(() -> Constants.getBoolean("adminCheckUserGroups")).thenReturn(true);
        docAccess.when(() -> DocDB.canAccess(submitted, user, true)).thenReturn(true);
        assertThrows(RuntimeException.class, () -> editor.checkSavePermissions(user, submitted));
        docAccess.verify(() -> DocDB.canAccess(original, user, true));
    }

    /** Editing a linked master via an authorized copy retains the established multigroup permission. */
    @Test
    void preservesLinkedPageEditingWithoutAllowingUnauthorizedMoves() {
        MultigroupMapping mapping = new MultigroupMapping();
        mapping.setDocId(43);
        mappings.when(() -> MultigroupMappingDB.getSlaveMappings(42)).thenReturn(List.of(mapping));
        when(documents.getBasicDocDetails(43, true)).thenReturn(page(43, 25));
        assertDoesNotThrow(() -> editor.checkSavePermissions(user, page(42, 10)));
        assertThrows(RuntimeException.class, () -> editor.checkSavePermissions(user, page(42, 999)));
    }

    /** REST access uses stored scope in both deployment modes and does not require save permission for reading. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void checksRestAccessAgainstStoredPage(boolean cloud) {
        var controller = mock(WebpagesRestController.class, CALLS_REAL_METHODS);
        var folders = mock(GroupsRestController.class, CALLS_REAL_METHODS);
        doReturn(user).when(controller).getUser();
        doReturn(user).when(folders).getUser();
        when(documents.getBasicDocDetails(42, false)).thenReturn(original);
        when(documents.getBasicDocDetails(43, false)).thenReturn(page(43, 10));
        when(user.getEditablePages()).thenReturn("43");
        try (var deployment = mockStatic(InitServlet.class)) {
            deployment.when(InitServlet::isTypeCloud).thenReturn(cloud);
            assertFalse(controller.checkItemPerms(page(42, 25), 42L));
            assertFalse(controller.checkItemPerms(page(999, 25), 999L));
            assertFalse(controller.exists(42));
            assertFalse(folders.exists(10));
            for (int id : List.of(-1, 0, 999)) {
                assertFalse(controller.exists(id));
                assertFalse(folders.exists(id));
            }
            assertEquals(!cloud, controller.exists(43));
            original.setGroupId(25);
            groups.getGroup(25).setDomainName(CloudToolsForCore.getDomainName());
            when(user.isDisabledItem("pageSave")).thenReturn(true);
            assertTrue(controller.checkItemPerms(page(42, 10), 42L));
            assertTrue(controller.exists(42));
            assertTrue(folders.exists(25));
            groups.getGroup(25).setDomainName("other.example");
            assertEquals(!cloud, controller.exists(42));
            assertEquals(!cloud, folders.exists(25));
        }
    }

    /** Category creation reaches the action handler without a selected product, but still requires a user. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void dispatchesCategoryCreationWithoutSelectedProduct(boolean cloud) {
        try (var deployment = mockStatic(InitServlet.class);
                var products = mockStatic(ProductListService.class)) {
            deployment.when(InitServlet::isTypeCloud).thenReturn(cloud);
            EditorFacade facade = mock(EditorFacade.class);
            ProductListRestController controller = spy(new ProductListRestController(mock(DocDetailsRepository.class), facade, null));
            doReturn(user).when(controller).getUser();
            Prop prop = mock(Prop.class);
            when(prop.getText(anyString())).thenAnswer(call -> call.getArgument(0));
            doReturn(prop).when(controller).getProp();
            String customData = "{\"groupId\":\"25\",\"newGroupName\":\"autotest category\"}";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("customData", customData);
            doReturn(request).when(controller).getRequest();
            products.when(() -> ProductListService.addProductGroup(user, customData, facade))
                .thenReturn(ProductListService.AddingStatus.SUCCESS);

            var response = controller.action("addProductGroup", new Long[] {-1L});
            assertNotNull(response.getBody());
            assertNull(response.getBody().getError());

            doReturn(null).when(controller).getUser();
            assertThrows(ConstraintViolationException.class,
                () -> controller.action("addProductGroup", new Long[] {-1L}));
            products.verify(() -> ProductListService.addProductGroup(user, customData, facade));
            products.verifyNoMoreInteractions();
            verify(controller, never()).getOneItem(anyLong());
        }
    }

    /** Only the no-selection sentinel may omit a record; missing records and users remain unauthorized. */
    @Test
    void rejectsMissingRecordsAndUsers() {
        WebpagesDatatable controller = mock(WebpagesDatatable.class, CALLS_REAL_METHODS);
        doReturn(user).when(controller).getUser();
        for (Long id : new Long[] {null, -2L, 0L, 42L, 999L}) {
            assertFalse(controller.checkItemPerms(null, id));
        }
        doReturn(null).when(controller).getUser();
        assertFalse(controller.checkItemPerms(null, -1L));
        assertFalse(controller.checkItemPerms(page(-1, 25), -1L));
        assertFalse(controller.checkItemPerms(page(42, 25), 42L));
    }

    /** Modules sharing the page editor retain their own entry permissions without menuWebpages. */
    @ParameterizedTest
    @ValueSource(strings = {"menuWebpages", "cmp_blog", "cmp_blog_admin", "cmp_news", "cmp_abtesting", "cmp_basket"})
    void preservesSharedEditorModulePermissions(String permission) {
        constants.when(() -> Constants.getString("webpagesFunctionsPerms"))
            .thenReturn("menuWebpages|cmp_blog|cmp_blog_admin|cmp_news|cmp_abtesting|cmp_basket");
        when(user.isEnabledItem(anyString())).thenReturn(false);
        original.setGroupId(25);
        assertFalse(EditorService.hasPagePermission(user, original));
        when(user.isEnabledItem(permission)).thenReturn(true);
        assertTrue(EditorService.hasPagePermission(user, original));
        assertDoesNotThrow(() -> editor.checkSavePermissions(user, page(42, 25)));
    }

    private static DocDetails page(int id, int groupId) {
        DocDetails doc = new DocDetails();
        doc.setDocId(id);
        doc.setGroupId(groupId);
        doc.setTitle("autotest original");
        doc.setData("<p>autotest original content</p>");
        return doc;
    }
}
