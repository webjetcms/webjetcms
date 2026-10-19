package sk.iway.iwcm.admin.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.PlannedAccess;

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

    /** Planned uploads use target folder permissions and still validate the uploaded file. */
    @Test
    void validatesPlannedUploadsThroughCommonChecks() throws Exception {
        String folder = "/files/protected/planned-changes/42/images/gallery/";
        when(user.isFolderWritable(folder)).thenReturn(false);
        try (var access = mockStatic(PlannedAccess.class)) {
            access.when(() -> PlannedAccess.checkedFile(anyString())).thenReturn(Path.of("/private/tmp"));

            assertNull(validate(folder, "photo.jpg"));
            assertNull(validate(folder.substring(0, folder.length() - 1), "photo.jpg"));
            assertEquals(AdminUploadValidator.ERROR_INVALID_FILE, validate(folder, "shell.php7"));

            when(user.isFolderWritable("/images/gallery/")).thenReturn(false);
            assertEquals(AdminUploadValidator.ERROR_INVALID_FOLDER, validate(folder, "photo.jpg"));
        }
    }

    /** Upload rights depend on the target folder even when the page cannot be edited. */
    @Test
    void allowsPlannedUploadWithoutPagePermission() throws Exception {
        try (var access = mockStatic(PlannedAccess.class)) {
            access.when(() -> PlannedAccess.requirePage(42, user)).thenThrow(new IllegalArgumentException("Denied"));
            access.when(() -> PlannedAccess.checkedFile(anyString())).thenReturn(Path.of("/private/tmp"));
            assertNull(validate("/files/protected/planned-changes/42/images/gallery/", "photo.jpg"));
            access.verify(() -> PlannedAccess.requirePage(42, user), org.mockito.Mockito.never());
        }
    }

    /** Rejected source or target files remain upload validation errors. */
    @ParameterizedTest
    @ValueSource(strings = {
        "/files/protected/planned-changes/42/images/gallery/photo.jpg", "/images/gallery/photo.jpg"
    })
    void rejectsUnsafePlannedUploadPaths(String unsafePath) throws Exception {
        try (var access = mockStatic(PlannedAccess.class)) {
            access.when(() -> PlannedAccess.checkedFile(anyString())).thenReturn(Path.of("/private/tmp"));
            access.when(() -> PlannedAccess.checkedFile(unsafePath)).thenThrow(new IOException("Symlink"));
            assertEquals(AdminUploadValidator.ERROR_INVALID_FOLDER,
                validate("/files/protected/planned-changes/42/images/gallery/", "photo.jpg"));
        }
    }

    @Test
    void archiveChunkUsesOnlyDestinationIndependentValidation() {
        MockHttpServletRequest chunkRequest = new MockHttpServletRequest();
        chunkRequest.setRequestURI("/admin/upload/chunk");

        assertNull(AdminUploadValidator.validateUserAndFile(
            "document.pdf", "fileArchive", 0, user, chunkRequest));
        assertEquals(AdminUploadValidator.ERROR_INVALID_FOLDER,
            AdminUploadValidator.validateChunk(
                "files/archiv/", "document.pdf", "fileArchive", 0, true, user, chunkRequest));
        assertEquals(AdminUploadValidator.ERROR_INVALID_FILE,
            AdminUploadValidator.validateUserAndFile(
                "shell.jspx", "fileArchive", 0, user, chunkRequest));

        Identity nonAdmin = mock(Identity.class);
        assertEquals("admin.logon.timeoutTitle", AdminUploadValidator.validateUserAndFile(
            "document.pdf", "fileArchive", 0, nonAdmin, chunkRequest));
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
