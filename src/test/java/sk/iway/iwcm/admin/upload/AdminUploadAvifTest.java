package sk.iway.iwcm.admin.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import jakarta.servlet.ServletContext;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPart;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies the actual editor upload endpoint preserves the original when AVIF encoding fails. */
class AdminUploadAvifTest extends BaseWebjetTest {
    @TempDir
    Path root;

    /** The editor's Base64 payload must never replace an AVIF when its codec is missing. */
    @Test
    void failedEditorSaveKeepsOriginalAndReportsFailure() throws Exception {
        root = root.toRealPath();
        ServletContext originalContext = Constants.getServletContext();
        String originalCommand = Constants.getString("imageMagickDir");
        String uploadTypes = Constants.getString("FCKConfig.UploadFileTypes[Basic][image]");
        Path target = root.resolve("images/autotest/image.avif");
        Files.createDirectories(target.getParent());
        byte[] original = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"));
        Files.write(target, original);
        try {
            Constants.setServletContext(new MockServletContext() {
                @Override
                public String getRealPath(String path) {
                    return root.resolve(path.replaceFirst("^/", "")).toString() + ("/".equals(path) ? "/" : "");
                }
            });
            Constants.setString("imageMagickDir", root.resolve("missing").toString());
            Constants.setString("FCKConfig.UploadFileTypes[Basic][image]", "avif");
            Identity user = new Identity();
            user.setAdmin(true);
            user.setWritableFolders("*");
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setMethod("POST");
            request.setRequestURI("/admin/upload/chunk");
            request.getSession().setAttribute(Constants.USER_KEY, user);
            request.addParameter("name", "image.avif");
            request.addParameter("uploadType", "image");
            request.addParameter("destinationFolder", "/images/autotest/");
            request.addParameter("writeDirectlyToDestination", "true");
            request.addParameter("overwriteMode", "overwrite");
            request.addParameter("encoding", "base64");
            request.addParameter("dztotalfilesize", "100");
            // Valid PNG pixels, matching the image editor's transport format.
            java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_ARGB), "png", png);
            request.addPart(new MockPart("file", "blob", Base64.getEncoder().encode(png.toByteArray())));
            MockHttpServletResponse response = new MockHttpServletResponse();
            new AdminUploadServlet().doPost(request, response);
            JSONObject result = new JSONObject(response.getContentAsString(StandardCharsets.UTF_8));
            assertFalse(result.getBoolean("success"));
            assertTrue(result.getString("error").length() > 0);
            assertArrayEquals(original, Files.readAllBytes(target));
            // A chunk-uploaded field proves this reached assembly rather than failing permission checks.
            assertTrue(result.has("chunk-uploaded"));
        } finally {
            Constants.setServletContext(originalContext);
            Constants.setString("imageMagickDir", originalCommand);
            Constants.setString("FCKConfig.UploadFileTypes[Basic][image]", uploadTypes);
        }
    }
}
