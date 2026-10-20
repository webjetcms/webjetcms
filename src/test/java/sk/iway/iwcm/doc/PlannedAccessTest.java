package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.editor.InlineEditor;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.Media;

/** Anonymous and ordinary logged-in visitors receive identical private 404 responses. */
class PlannedAccessTest {
    /** Private history stays editable in the admin iframe while ordinary previews remain read-only. */
    @Test
    void allowsInlineEditingOfPrivateHistoryInAdmin() {
        DocHistory history = new DocHistory();
        history.setDocId(42);
        history.setPublicable(true);
        DocDetailsRepository documents = mock(DocDetailsRepository.class);
        when(documents.isPlannedMediaInitialized(42)).thenReturn(true);
        Identity editor = new Identity();
        editor.setAdmin(true);
        editor.setDisabledItemsTable(java.util.Map.of("pageSave", "pageSave"));
        DocHistoryRepository repository = mock(DocHistoryRepository.class);
        when(repository.findById(123L)).thenReturn(java.util.Optional.of(history));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("inlineEditorAdmin", "true");
        try (var tools = mockStatic(Tools.class); var users = mockStatic(UsersDB.class);
             var editorDB = mockStatic(EditorDB.class);
             var access = mockStatic(PlannedAccess.class, CALLS_REAL_METHODS)) {
            tools.when(() -> Tools.getSpringBean("docHistoryRepository", DocHistoryRepository.class)).thenReturn(repository);
            tools.when(() -> Tools.getSpringBean("docDetailsRepository", DocDetailsRepository.class)).thenReturn(documents);
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(editor);
            access.when(PlannedAccess::enabled).thenReturn(true);
            editorDB.when(() -> EditorDB.isPageEditable(editor, 42)).thenReturn(true);
            access.when(() -> PlannedAccess.masterId(42)).thenReturn(42);

            assertTrue(PlannedAccess.historyPreview(123, request, response));
            assertEquals(Boolean.TRUE, request.getAttribute("isPreview"));
            assertEquals(42, request.getAttribute(PlannedAccess.PREVIEW_DOC));
            assertEquals("private, no-store", response.getHeader("Cache-Control"));
            assertTrue(InlineEditor.isInlineEditingEnabled(request));

            editorDB.when(() -> EditorDB.isPageEditable(editor, 42)).thenReturn(false);
            MockHttpServletResponse denied = new MockHttpServletResponse();
            assertFalse(PlannedAccess.historyPreview(123, request, denied));
            assertEquals(404, denied.getStatus());

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

    /** Private originals and thumbnails use the same public folder permissions. */
    @Test
    void checksTargetFolderPermissions() {
        Identity editor = new Identity();
        editor.setAdmin(true);
        editor.setWritableFolders("/images/allowed/");
        MockHttpServletRequest request = new MockHttpServletRequest();
        try (var users = mockStatic(UsersDB.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(editor);
            users.when(() -> UsersDB.isFolderWritable(anyString(), anyString())).thenCallRealMethod();
            for (String prefix : new String[] {"", "/thumb"}) {
                String path = prefix + "/files/protected/planned-changes/42/images/allowed/photo.webp";
                editor.setWritableFolders("/images/allowed/");
                assertTrue(PlannedAccess.check(path, request, new MockHttpServletResponse()));
                editor.setWritableFolders("/images/other/");
                MockHttpServletResponse denied = new MockHttpServletResponse();
                assertFalse(PlannedAccess.check(path, request, denied));
                assertEquals(404, denied.getStatus());
            }
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
             var editorDB = mockStatic(EditorDB.class);
             var access = mockStatic(PlannedAccess.class, CALLS_REAL_METHODS)) {
            tools.when(() -> Tools.getSpringBean("docHistoryRepository", DocHistoryRepository.class)).thenReturn(repository);
            access.when(PlannedAccess::enabled).thenReturn(false);
            assertFalse(PlannedAccess.historyPreview(123, request, response));
            assertEquals(404, response.getStatus());
            assertEquals("private, no-store", response.getHeader("Cache-Control"));
        }
        history.setPerexImage(null);
        history.setFieldA("<img src='/files/protected/planned-changes/42/images/secret.webp'>");
        assertTrue(PlannedChangesService.hasPrivateReferences(history));
    }

    /** Planned media remain private and honor the existing all-media permission without pageSave. */
    @Test
    void readsPlannedMediaWithExistingMediaPermissions() {
        Media media = new Media();
        media.setMediaFkTableName(PlannedChangesService.MEDIA_TABLE);
        media.setMediaFkId(42);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestBean context = mock(RequestBean.class);
        when(context.getRequest()).thenReturn(request);
        Identity user = new Identity();
        user.setAdmin(true);
        user.setDisabledItemsTable(java.util.Map.of("pageSave", "pageSave", "editor_edit_media_all", "editor_edit_media_all"));
        try (var persistence = mockStatic(JpaTools.class); var users = mockStatic(UsersDB.class);
             var requests = mockStatic(SetCharacterEncodingFilter.class); var editor = mockStatic(EditorDB.class)) {
            persistence.when(() -> JpaTools.findFirstByMatchingProperty(Media.class, "id", 7)).thenReturn(media);
            requests.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(context);
            assertNull(MediaDB.getMedia(7));

            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            assertNull(MediaDB.getMedia(7));
            editor.when(() -> EditorDB.isPageEditable(user, 42)).thenReturn(true);
            assertSame(media, MediaDB.getMedia(7));

            editor.when(() -> EditorDB.isPageEditable(user, 42)).thenReturn(false);
            user.setDisabledItemsTable(java.util.Map.of("pageSave", "pageSave"));
            assertSame(media, MediaDB.getMedia(7));
        }
    }

    @Test
    void doesNotChangeOrdinaryAssetResponses() throws Exception {
        assertEquals("/images/current.webp", PlannedAssetPaths.removePlannedPrefix("/images/current.webp"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertTrue(PlannedAccess.check("/images/current.webp", new MockHttpServletRequest(), response));
        assertNull(response.getHeader("Cache-Control"));
    }
}
