package sk.iway.iwcm.admin.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Identity;

class AdminUploadValidatorTest {

    private Identity user;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isFolderWritable(anyString())).thenReturn(true);
        when(user.isEnabledItem("editor_unlimited_upload")).thenReturn(true);

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/admin/upload/overwrite");
        request = mockRequest;
    }

    @ParameterizedTest
    @ValueSource(strings = { "/images/", "/images/gallery/", "/files/docs/", "/shared/" })
    void allowsWritableUploadFolders(String folder) {
        assertNull(validate(folder, "photo.jpg"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/templates/", "/imagesX/", "/files-backup/", "/sharedX/",
        "/images/../templates/", "/images\\..\\templates\\"
    })
    void rejectsFoldersOutsideAllowedRoots(String folder) {
        assertEquals(AdminUploadValidator.ERROR_INVALID_FOLDER, validate(folder, "photo.jpg"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "shell.jsp", "shell.jspx", "shell.php7", "payload.jar" })
    void rejectsForbiddenFileTypes(String fileName) {
        assertEquals(AdminUploadValidator.ERROR_INVALID_FILE, validate("/images/", fileName));
    }

    @Test
    void rejectsFolderWithoutWritePermission() {
        when(user.isFolderWritable("/images/gallery/")).thenReturn(false);

        assertEquals(AdminUploadValidator.ERROR_INVALID_FOLDER,
            validate("/images/gallery/", "photo.jpg"));
    }

    @Test
    void protectedFolderExceptionAppliesOnlyToChunkUpload() {
        when(user.isFolderWritable("/files/protected/upload/")).thenReturn(false);

        assertNull(AdminUploadValidator.validateChunk(
            "/files/protected/upload/", "photo.jpg", "file", 0, false, user, request));
        assertEquals(AdminUploadValidator.ERROR_INVALID_FOLDER,
            AdminUploadValidator.validateChunk(
                "/files/protected/upload/", "photo.jpg", "file", 0, true, user, request));
        assertEquals(AdminUploadValidator.ERROR_INVALID_FOLDER,
            AdminUploadValidator.validateConflict(
                "/files/protected/upload/", "photo.jpg", "file", 0, user, request));
    }

    @Test
    void normalizesFolderAndFileName() {
        assertEquals("/images/gallery/",
            AdminUploadValidator.normalizeDestinationFolder("/images/gallery"));
        assertEquals("photo.jpg", AdminUploadValidator.normalizeFileName("Photo.JPG"));
    }

    private String validate(String folder, String fileName) {
        return AdminUploadValidator.validateConflict(folder, fileName, "file", 0, user, request);
    }
}
