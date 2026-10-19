package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.editor.InlineEditor;
import sk.iway.iwcm.users.UsersDB;

/** Anonymous and ordinary logged-in visitors receive identical private 404 responses. */
class PlannedAccessTest {
    /** Private history stays editable in the admin iframe while ordinary previews remain read-only. */
    @Test
    void allowsInlineEditingOfPrivateHistoryInAdmin() {
        DocHistory history = new DocHistory();
        history.setDocId(42);
        history.setPublicable(true);
        DocDetails page = mock(DocDetails.class);
        when(page.getDocId()).thenReturn(42);
        when(page.isPlannedMediaInitialized()).thenReturn(true);
        Identity editor = new Identity();
        editor.setAdmin(true);
        DocHistoryRepository repository = mock(DocHistoryRepository.class);
        when(repository.findById(123L)).thenReturn(java.util.Optional.of(history));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("inlineEditorAdmin", "true");
        try (var tools = mockStatic(Tools.class); var users = mockStatic(UsersDB.class);
             var access = mockStatic(PlannedAccess.class, CALLS_REAL_METHODS)) {
            tools.when(() -> Tools.getSpringBean("docHistoryRepository", DocHistoryRepository.class)).thenReturn(repository);
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(editor);
            access.when(PlannedAccess::enabled).thenReturn(true);
            access.when(() -> PlannedAccess.requirePage(42, editor)).thenReturn(page);

            assertTrue(PlannedAccess.historyPreview(123, request, response));
            assertEquals(Boolean.TRUE, request.getAttribute("isPreview"));
            assertEquals(42, request.getAttribute(PlannedAccess.PREVIEW_DOC));
            assertEquals("private, no-store", response.getHeader("Cache-Control"));
            assertTrue(InlineEditor.isInlineEditingEnabled(request));

            request.removeParameter("inlineEditorAdmin");
            request.setAttribute("isInlineEditing", "true");
            assertFalse(InlineEditor.isInlineEditingEnabled(request));
            request.setParameter("inlineEditorAdmin", "true");
            editor.setAdmin(false);
            assertFalse(InlineEditor.isInlineEditingEnabled(request));
            editor.setAdmin(true);
            request.setAttribute("disableInlineEditing", Boolean.TRUE);
            assertFalse(InlineEditor.isInlineEditingEnabled(request));
        }
    }

    @Test
    void deniesBothOriginalAndWarmedThumbnailBeforeExistenceChecks() {
        for (String path : new String[] {"/files/protected/planned-changes/42/images/photo.webp", "/thumb/files/protected/planned-changes/42/images/photo.webp"}) {
            for (Identity visitor : new Identity[] {null, new Identity()}) {
                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();
                try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class); MockedStatic<Tools> tools = mockStatic(Tools.class);
                     MockedStatic<MultigroupMappingDB> mappings = mockStatic(MultigroupMappingDB.class)) {
                    users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(visitor);
                    // No authorized page can be resolved; the response must not depend on file existence.
                    assertFalse(PlannedAccess.check(path, request, response));
                    assertEquals(404, response.getStatus());
                    assertEquals("private, no-store", response.getHeader("Cache-Control"));
                    assertEquals("", response.getContentAsString());
                } catch (java.io.UnsupportedEncodingException exception) { fail(exception); }
            }
        }
    }

    /** Checks page, domain and destination ACLs independently of file existence. */
    @Test
    void requiresEditorPageDomainAndTargetFolderPermissions() throws Exception {
        String path = "/files/protected/planned-changes/42/images/allowed/photo.webp";
        Identity editor = new Identity();
        editor.setAdmin(true);
        editor.setWritableFolders("/images/allowed/");
        DocDetails doc = new DocDetails();
        doc.setDocId(42);
        doc.setGroupId(1);
        GroupDetails group = new GroupDetails();
        group.setDomainName("one.example.com");
        DocDetailsRepository repository = mock(DocDetailsRepository.class);
        when(repository.findById(42)).thenReturn(doc);
        GroupsDB groupDB = mock(GroupsDB.class);
        when(groupDB.getGroup(1)).thenReturn(group);
        try (var tools = mockStatic(Tools.class); var mappings = mockStatic(MultigroupMappingDB.class);
             var groups = mockStatic(GroupsDB.class); var domains = mockStatic(sk.iway.iwcm.common.CloudToolsForCore.class);
             var pages = mockStatic(sk.iway.iwcm.editor.EditorDB.class); var users = mockStatic(UsersDB.class);
             var access = mockStatic(PlannedAccess.class, CALLS_REAL_METHODS)) {
            tools.when(() -> Tools.getSpringBean("docDetailsRepository", DocDetailsRepository.class)).thenReturn(repository);
            tools.when(() -> Tools.isEmpty(anyString())).thenCallRealMethod();
            groups.when(GroupsDB::getInstance).thenReturn(groupDB);
            domains.when(sk.iway.iwcm.common.CloudToolsForCore::getDomainName).thenReturn("one.example.com");
            pages.when(() -> sk.iway.iwcm.editor.EditorDB.isPageEditable(any(Identity.class), any(sk.iway.iwcm.editor.EditorForm.class))).thenReturn(true);
            access.when(() -> PlannedAccess.checkedFile(anyString())).thenReturn(java.nio.file.Path.of("/private/tmp"));
            users.when(() -> UsersDB.isFolderWritable(anyString(), anyString())).thenReturn(true);
            assertTrue(PlannedAccess.canAccess(path, editor));
            domains.when(sk.iway.iwcm.common.CloudToolsForCore::getDomainName).thenReturn("two.example.com");
            assertFalse(PlannedAccess.canAccess(path, editor));
            domains.when(sk.iway.iwcm.common.CloudToolsForCore::getDomainName).thenReturn("one.example.com");
            users.when(() -> UsersDB.isFolderWritable(anyString(), anyString())).thenReturn(false);
            assertFalse(PlannedAccess.canAccess(path, editor));
            users.when(() -> UsersDB.isFolderWritable(anyString(), anyString())).thenReturn(true);
            editor.setAdmin(false);
            assertFalse(PlannedAccess.canAccess(path, editor));
            editor.setAdmin(true);
            pages.when(() -> sk.iway.iwcm.editor.EditorDB.isPageEditable(any(Identity.class), any(sk.iway.iwcm.editor.EditorForm.class))).thenReturn(false);
            assertFalse(PlannedAccess.canAccess(path, editor));
        }
    }

    @Test
    void keepsPerexOnlyHistoryPrivateAfterFeatureIsDisabled() {
        DocHistory history = new DocHistory();
        history.setDocId(42);
        history.setPerexImage("/files/protected/planned-changes/42/images/secret.webp");
        DocHistoryRepository repository = mock(DocHistoryRepository.class);
        when(repository.findById(123L)).thenReturn(java.util.Optional.of(history));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        try (var tools = mockStatic(Tools.class); var users = mockStatic(UsersDB.class);
             var mappings = mockStatic(MultigroupMappingDB.class);
             var access = mockStatic(PlannedAccess.class, CALLS_REAL_METHODS)) {
            tools.when(() -> Tools.getSpringBean("docHistoryRepository", DocHistoryRepository.class)).thenReturn(repository);
            access.when(PlannedAccess::enabled).thenReturn(false);
            access.when(() -> PlannedAccess.requirePage(42, null)).thenThrow(new IllegalArgumentException("Denied"));
            assertFalse(PlannedAccess.historyPreview(123, request, response));
            assertEquals(404, response.getStatus());
            assertEquals("private, no-store", response.getHeader("Cache-Control"));
        }
        history.setPerexImage(null);
        history.setFieldA("<img src='/files/protected/planned-changes/42/images/secret.webp'>");
        assertTrue(PlannedChangesService.hasPrivateReferences(history));
    }

    @Test
    void doesNotChangeOrdinaryAssetResponses() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertTrue(PlannedAccess.check("/images/current.webp", new MockHttpServletRequest(), response));
        assertNull(response.getHeader("Cache-Control"));
    }
}
