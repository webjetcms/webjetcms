package sk.iway.iwcm.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import sk.iway.iwcm.form.FormFileRestriction;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.upload.UploadedFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JUnit tests for FormFileRestriction security fix.
 * Verifies that global file type restrictions are checked before
 * per-form allowed extensions.
 */
@Execution(ExecutionMode.SAME_THREAD)
class FormFileRestrictionSecurityTest extends BaseWebjetTest {

    private FormFileRestriction restriction;

    @BeforeEach
    void setUp() {
        restriction = new FormFileRestriction();
    }

    // --- Tests for global file type restriction check ---

    @Test
    void testHasAllowedExtension_BlocksDangerousExtensions() {
        // Global file type restrictions should block dangerous extensions
        // even if they are in the allowed extensions list
        restriction.setAllowedExtensions("jpg,png,jsp,exe");

        // Create a mock UploadedFile
        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn("shell.jsp");

        assertFalse(restriction.isSentFileValid(file),
            "JSP files should be blocked by global restrictions");
    }

    @Test
    void testHasAllowedExtension_BlocksExecutableExtensions() {
        restriction.setAllowedExtensions("jpg,png,exe,bat");

        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn("malware.exe");

        assertFalse(restriction.isSentFileValid(file),
            "EXE files should be blocked by global restrictions");
    }

    @Test
    void testHasAllowedExtension_AllowsSafeExtensions() {
        restriction.setAllowedExtensions("jpg,png,gif,pdf");

        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn("photo.jpg");

        assertTrue(restriction.isSentFileValid(file),
            "JPG files should be allowed");
    }

    @Test
    void testHasAllowedExtension_AllowsWhenNoRestrictions() {
        // When no allowedExtensions are set, only global restrictions apply
        restriction.setAllowedExtensions("");

        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn("document.pdf");

        assertTrue(restriction.isSentFileValid(file),
            "PDF files should be allowed when no specific restrictions");
    }

    @Test
    void testIsBelowMaxSize_PassesWhenMaxSizeIsZero() {
        restriction.setMaxSizeInKilobytes(0);

        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn("document.pdf");
        when(file.getFileSize()).thenReturn(1024 * 1024); // 1 MB

        assertTrue(restriction.isSentFileValid(file),
            "When maxSize is 0, size check should pass");
    }

    @Test
    void testIsBelowMaxSize_PassesWhenWithinLimit() {
        restriction.setMaxSizeInKilobytes(1024); // 1 MB

        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn("document.pdf");
        when(file.getFileSize()).thenReturn(512 * 1024); // 512 KB

        assertTrue(restriction.isSentFileValid(file),
            "Files within size limit should be allowed");
    }

    @Test
    void testIsBelowMaxSize_FailsWhenExceedsLimit() {
        restriction.setMaxSizeInKilobytes(1024); // 1 MB

        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn("document.pdf");
        when(file.getFileSize()).thenReturn(2048 * 1024); // 2 MB

        assertFalse(restriction.isSentFileValid(file),
            "Files exceeding size limit should be blocked");
    }

    @Test
    void testIsSentFileValid_CombinesAllChecks() {
        restriction.setFormName("contact");
        restriction.setAllowedExtensions("jpg,png,pdf");
        restriction.setMaxSizeInKilobytes(5120); // 5 MB

        // Valid file
        UploadedFile validFile = mock(UploadedFile.class);
        when(validFile.getFileName()).thenReturn("photo.jpg");
        when(validFile.getFileSize()).thenReturn(1024 * 1024); // 1 MB

        assertTrue(restriction.isSentFileValid(validFile),
            "Valid file should pass all checks");

        // File with dangerous extension
        UploadedFile dangerousFile = mock(UploadedFile.class);
        when(dangerousFile.getFileName()).thenReturn("shell.jsp");
        when(dangerousFile.getFileSize()).thenReturn(1024);

        assertFalse(restriction.isSentFileValid(dangerousFile),
            "Dangerous extension should fail global check");

        // File exceeding size limit
        UploadedFile largeFile = mock(UploadedFile.class);
        when(largeFile.getFileName()).thenReturn("large.pdf");
        when(largeFile.getFileSize()).thenReturn(6 * 1024 * 1024); // 6 MB

        assertFalse(restriction.isSentFileValid(largeFile),
            "Large file should fail size check");

        // File with wrong extension
        UploadedFile wrongExtFile = mock(UploadedFile.class);
        when(wrongExtFile.getFileName()).thenReturn("script.php");
        when(wrongExtFile.getFileSize()).thenReturn(1024);

        assertFalse(restriction.isSentFileValid(wrongExtFile),
            "Wrong extension should fail restriction check");
    }

    @Test
    void testFormNameSetter() {
        restriction.setFormName("contact_form");
        assertEquals("contact_form", restriction.getFormName());
    }

    @Test
    void testAllowedExtensionsSetter() {
        restriction.setAllowedExtensions("jpg,png,gif,pdf");
        assertEquals("jpg,png,gif,pdf", restriction.getAllowedExtensions());
    }

    @Test
    void testMaxSizeInKilobytesSetter() {
        restriction.setMaxSizeInKilobytes(1024);
        assertEquals(1024, restriction.getMaxSizeInKilobytes());
    }

    @Test
    void testPictureDimensionsChecks() {
        restriction.setPictureWidth(800);
        restriction.setPictureHeight(600);
        assertEquals(800, restriction.getPictureWidth());
        assertEquals(600, restriction.getPictureHeight());
    }
}
