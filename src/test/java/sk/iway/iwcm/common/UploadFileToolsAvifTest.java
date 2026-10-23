package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies custom AVIF upload restrictions and original-file handling in the gallery upload path. */
class UploadFileToolsAvifTest extends BaseWebjetTest {
    @TempDir
    Path root;

    /** Adding AVIF support does not bypass a user's configured list of allowed image types. */
    @Test
    void respectsCustomImageAllowlist() {
        String key = "FCKConfig.UploadFileTypes[Basic][image]";
        String previous = Constants.getString(key);
        try {
            Identity user = mock(Identity.class);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/admin/upload/chunk");
            Constants.setString(key, "jpg,png");
            assertFalse(UploadFileTools.isFileAllowed("image", "autotest.avif", 100, user, request));
            Constants.setString(key, "jpg,png,avif");
            assertTrue(UploadFileTools.isFileAllowed("image", "autotest.AVIF", 100, user, request));
        } finally {
            Constants.setString(key, previous);
        }
    }

    /** Reuploading while the codec is unavailable must replace an older archived original. */
    @Test
    void reuploadWithoutCodecUpdatesArchivedOriginal() throws Exception {
        root = root.toRealPath();
        ServletContext previousContext = Constants.getServletContext();
        String previousCommand = Constants.getString("imageMagickDir");
        String previousBase = Constants.getString("galleryDirName");
        try {
            Constants.setServletContext(new MockServletContext() {
                @Override
                public String getRealPath(String path) {
                    return root.resolve(path.replaceFirst("^/", "")).toString() + ("/".equals(path) ? "/" : "");
                }
            });
            Constants.setString("imageMagickDir", root.resolve("missing").toString());
            Constants.setString("galleryDirName", "gallery");
            Path source = root.resolve("images/gallery/autotest/image.avif");
            Files.createDirectories(source.getParent());
            byte[] uploaded = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"));
            Files.write(source, uploaded);
            Path archived = source.resolveSibling("o_image.avif");
            Files.write(archived, new byte[] {1, 2, 3});
            assertTrue(UploadFileTools.handleGallery(new IwcmFile(source.toFile()), null).isEmpty());
            assertArrayEquals(uploaded, Files.readAllBytes(archived));
            assertArrayEquals(uploaded, Files.readAllBytes(source));
        } finally {
            Constants.setServletContext(previousContext);
            Constants.setString("imageMagickDir", previousCommand);
            Constants.setString("galleryDirName", previousBase);
        }
    }
}
