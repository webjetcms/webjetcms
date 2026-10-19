package sk.iway.iwcm.security;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import sk.iway.iwcm.form.FormFileRestriction;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.upload.UploadedFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JUnit tests for FormFileRestriction security fix.
 * Verifies that global file type restrictions are checked before
 * per-form allowed extensions and that filename errors are reported separately.
 */
@Execution(ExecutionMode.SAME_THREAD)
class FormFileRestrictionSecurityTest extends BaseWebjetTest {

    private FormFileRestriction restriction;
    private Prop prop;

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        restriction = new FormFileRestriction();
        prop = Prop.getInstance("en");
    }

    /** Accepts an uploaded PNG with spaces, Unicode punctuation and parentheses in its name. */
    @Test
    void acceptsParenthesesInUploadedImageName() throws Exception {
        String fileName = "Draft B · 08 · System notifications (B11a–c).png";
        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", imageBytes);
        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn(fileName);
        when(file.getFileSize()).thenReturn(imageBytes.size());
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(imageBytes.toByteArray()));
        restriction.setAllowedExtensions("png");

        assertNull(restriction.isSentFileValid(file, prop));
    }

    /** Validates the recovered original name of an XHR upload rather than its temporary prefix. */
    @ParameterizedTest
    @CsvSource({"report (final), ''", "report#1, '#'"})
    void validatesOriginalTemporaryFileName(String baseName, String forbiddenSymbol) throws Exception {
        Path path = temporaryDirectory.resolve("final_" + baseName + "__upload__123.png");
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", path.toFile());
        restriction.setAllowedExtensions("png");

        String error = restriction.isSentFileValid(new IwcmFile(path.toFile()), prop);

        if (forbiddenSymbol.isEmpty()) {
            assertNull(error);
        } else {
            assertEquals("File " + baseName + ".png contains a forbidden character or string in its name: #.", error);
        }
    }

    /** Reports the forbidden part of a filename even when the form has no extension whitelist. */
    @ParameterizedTest
    @CsvSource({"report#1.pdf, '#'", "report@1.pdf, '@'", "report..pdf, '..'", "report.java.pdf, '.java'"})
    void reportsForbiddenFilename(String fileName, String forbiddenSymbol) {
        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn(fileName);
        restriction.setAllowedExtensions("");

        assertEquals("File " + fileName + " contains a forbidden character or string in its name: "
            + forbiddenSymbol + ".", restriction.isSentFileValid(file, prop));
    }

    /** Reports globally forbidden extensions without displaying an empty or misleading allowed list. */
    @ParameterizedTest
    @CsvSource({"jsp, ''", "jsp, 'pdf,jsp'", "exe, ''", "exe, exe"})
    void reportsGloballyForbiddenExtension(String extension, String allowedExtensions) {
        String fileName = "blocked." + extension;
        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn(fileName);
        restriction.setAllowedExtensions(allowedExtensions);

        assertEquals("File " + fileName + " has a forbidden extension: " + extension + ".",
            restriction.isSentFileValid(file, prop));
    }

    /** Retains the form's allowed extension list when a safe file fails its specific restriction. */
    @Test
    void reportsFormExtensionRestriction() {
        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn("report.txt");
        restriction.setAllowedExtensions("pdf");

        assertEquals("File report.txt has a bad extension. Allowed extensions are: pdf.",
            restriction.isSentFileValid(file, prop));
    }

    /** Escapes filename markup and the forbidden token before inserting them into HTML errors. */
    @ParameterizedTest
    @CsvSource(value = {
        "report<b>.pdf | report&lt;b&gt;.pdf | &gt;",
        "report<b.pdf | report&lt;b.pdf | &lt;",
        "report<img src=\"x\" onerror='alert(1)'> &.pdf | report&lt;img src=&quot;x&quot; onerror=&#39;alert(1)&#39;&gt; &amp;.pdf | &#39;"
    }, delimiter = '|')
    void escapesForbiddenFilenameError(String fileName, String escapedFileName, String escapedSymbol) {
        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn(fileName);

        assertEquals("File " + escapedFileName + " contains a forbidden character or string in its name: "
            + escapedSymbol + ".", restriction.isSentFileValid(file, prop));
    }

    /** Escapes original filenames recovered from temporary uploads. */
    @Test
    void escapesOriginalTemporaryFileName() throws Exception {
        Path path = temporaryDirectory.resolve("final_report<b>__upload__123.png");
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", path.toFile());

        assertEquals("File report&lt;b&gt;.png contains a forbidden character or string in its name: &gt;.",
            restriction.isSentFileValid(new IwcmFile(path.toFile()), prop));
    }

    /** Escapes filenames even when the size check fails before filename validation. */
    @Test
    void escapesFilenameInSizeError() {
        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn("<b>large.pdf");
        when(file.getFileSize()).thenReturn(2048);
        restriction.setMaxSizeInKilobytes(1);

        assertTrue(restriction.isSentFileValid(file, prop).startsWith("File &lt;b&gt;large.pdf is too large."));
    }

    /** Escapes filenames when globally forbidden extensions take precedence over forbidden symbols. */
    @Test
    void escapesFilenameInGlobalExtensionError() {
        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn("<b>blocked.jsp");

        assertEquals("File &lt;b&gt;blocked.jsp has a forbidden extension: jsp.",
            restriction.isSentFileValid(file, prop));
    }

    /** Escapes the configured extension list when including it in an HTML error. */
    @Test
    void escapesConfiguredExtensionsInError() {
        UploadedFile file = mock(UploadedFile.class);
        when(file.getFileName()).thenReturn("report.txt");
        restriction.setAllowedExtensions("pdf,<b>");

        assertEquals("File report.txt has a bad extension. Allowed extensions are: pdf,&lt;b&gt;.",
            restriction.isSentFileValid(file, prop));
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
