package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
        DocDetails history = new DocDetails();
        history.setDocId(42);
        history.setPublicable(true);
        DocDetailsRepository documents = mock(DocDetailsRepository.class);
        when(documents.isPlannedMediaInitialized(42)).thenReturn(true);
        Identity editor = new Identity();
        editor.setAdmin(true);
        editor.setDisabledItemsTable(java.util.Map.of("pageSave", "pageSave"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("inlineEditorAdmin", "true");
        try (var tools = mockStatic(Tools.class); var users = mockStatic(UsersDB.class);
             var mappings = mockStatic(MultigroupMappingDB.class)) {
            tools.when(() -> Tools.getSpringBean("docDetailsRepository", DocDetailsRepository.class)).thenReturn(documents);
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(editor);

            PlannedAccess.historyPreview(history, request, response);
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
                    assertFalse(PlannedAccess.isPathAccessible(path, request, response));
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
                assertTrue(PlannedAccess.isPathAccessible(path, request, new MockHttpServletResponse()));
                editor.setWritableFolders("/images/other/");
                MockHttpServletResponse denied = new MockHttpServletResponse();
                assertFalse(PlannedAccess.isPathAccessible(path, request, denied));
                assertEquals(404, denied.getStatus());
            }
        }
    }

    /** Only pending history with an initialized media set switches the matching page's media. */
    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void selectsMediaOnlyForPendingHistory(boolean publicable, boolean initialized) {
        DocDetails history = new DocDetails();
        history.setDocId(43);
        history.setPublicable(publicable);
        DocDetailsRepository documents = mock(DocDetailsRepository.class);
        when(documents.isPlannedMediaInitialized(42)).thenReturn(initialized);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        try (var tools = mockStatic(Tools.class); var mappings = mockStatic(MultigroupMappingDB.class)) {
            tools.when(() -> Tools.getSpringBean("docDetailsRepository", DocDetailsRepository.class)).thenReturn(documents);
            mappings.when(() -> MultigroupMappingDB.getMasterDocId(43)).thenReturn(42);

            PlannedAccess.historyPreview(history, request, response);

            assertEquals(200, response.getStatus());
            assertEquals(Boolean.TRUE, request.getAttribute("isPreview"));
            assertEquals("private, no-store", response.getHeader("Cache-Control"));
            assertEquals(publicable && initialized, PlannedAccess.isPlannedPreview(request, 43));
            assertEquals(publicable && initialized, PlannedAccess.isPlannedPreview(request, 42));
            assertFalse(PlannedAccess.isPlannedPreview(request, 10));
            if (!publicable) verifyNoInteractions(documents);
        }
    }

    /** Planned media remain private and honor the existing all-media permission without pageSave. */
    @Test
    void readsPlannedMediaWithExistingMediaPermissions() {
        Media media = new Media();
        media.setMediaFkTableName(PlannedChangesService.PLANNED_MEDIA_TABLE);
        media.setMediaFkId(42);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestBean context = mock(RequestBean.class);
        when(context.getRequest()).thenReturn(request);
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
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
            when(user.isEnabledItem("editor_edit_media_all")).thenReturn(true);
            assertSame(media, MediaDB.getMedia(7));
        }
    }

    @Test
    void doesNotChangeOrdinaryAssetResponses() throws Exception {
        assertEquals("/images/current.webp", PlannedAssetPaths.removePlannedPrefix("/images/current.webp"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertTrue(PlannedAccess.isPathAccessible("/images/current.webp", new MockHttpServletRequest(), response));
        assertNull(response.getHeader("Cache-Control"));
    }
}
