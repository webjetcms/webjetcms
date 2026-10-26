package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.MultigroupMapping;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.editor.rest.WebpagesDatatable;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;

/** Checks authorization against stored pages independently of submitted destinations. */
class EditorSavePermissionsTest {
    private EditorService editor;
    private Identity user;
    private DocDB documents;
    private GroupsDB groups;
    private DocDetails original;
    private MockedStatic<GroupsDB> groupAccess;
    private MockedStatic<DocDB> docAccess;
    private MockedStatic<MultigroupMappingDB> mappings;
    private MockedStatic<Constants> constants;

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

    @AfterEach
    void tearDown() {
        constants.close();
        mappings.close();
        docAccess.close();
        groupAccess.close();
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
        WebpagesDatatable controller = mock(WebpagesDatatable.class, CALLS_REAL_METHODS);
        doReturn(user).when(controller).getUser();
        try (var deployment = mockStatic(InitServlet.class)) {
            deployment.when(InitServlet::isTypeCloud).thenReturn(cloud);
            assertFalse(controller.checkItemPerms(page(42, 25), 42L));
            assertFalse(controller.checkItemPerms(page(999, 25), 999L));
            original.setGroupId(25);
            when(user.isDisabledItem("pageSave")).thenReturn(true);
            assertTrue(controller.checkItemPerms(page(42, 10), 42L));
        }
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
