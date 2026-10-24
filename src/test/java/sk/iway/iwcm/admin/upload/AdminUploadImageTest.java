package sk.iway.iwcm.admin.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataNode;
import jakarta.servlet.ServletContext;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockPart;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.gallery.ImageInfo;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies format-independent staging, encoding and publishing through the actual image-editor upload endpoint. */
class AdminUploadImageTest extends BaseWebjetTest {
    @TempDir
    Path root;
    private ServletContext previousContext;
    private final Map<String, String> previousSettings = new LinkedHashMap<>();
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        root = root.toRealPath();
        previousContext = Constants.getServletContext();
        Constants.setServletContext(new MockServletContext() {
            @Override
            public String getRealPath(String path) {
                return root.resolve(path.replaceFirst("^/", "")).toString() + ("/".equals(path) ? "/" : "");
            }
        });
        setConfig("FCKConfig.UploadFileTypes[Basic][image]", "png,jpg,jpeg,gif,webp,bmp,avif,svg");
        setConfig("imageAlwaysCreateGalleryBean", "false");
        setConfig("galleryWatermarkApplyOnUpload", "false");
        setConfig("galleryConvertCmykToRgb", "false");
        setConfig("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", root.resolve("missing").toString()));
        Identity user = new Identity();
        user.setAdmin(true);
        user.setWritableFolders("*");
        session = new MockHttpSession();
        session.setAttribute(Constants.USER_KEY, user);
        Files.createDirectories(root.resolve("images/autotest"));
    }

    @AfterEach
    void tearDown() {
        Constants.setServletContext(previousContext);
        previousSettings.forEach(Constants::setString);
    }

    /** Malformed Base64 fails before format-specific processing and must not truncate the destination. */
    @Test
    void invalidBase64KeepsExistingBytes() throws Exception {
        Path target = target("png");
        byte[] original = {1, 2, 3, 4};
        Files.write(target, original);
        JSONObject response = upload("png", "invalid!base64".getBytes(StandardCharsets.US_ASCII), 0, 1);
        assertFailure(response);
        assertArrayEquals(original, Files.readAllBytes(target));
        assertNoStagingFiles();
    }

    /** The editor's PNG transport must not replace an AVIF when the encoder is unavailable. */
    @Test
    void failedAvifEditorSaveKeepsOriginalAndReportsFailure() throws Exception {
        Constants.setString("imageMagickDir", root.resolve("missing").toString());
        byte[] original = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"));
        Files.write(target("avif"), original);
        assertFailure(upload("avif", pngPayload(), 0, 1));
        assertArrayEquals(original, Files.readAllBytes(target("avif")));
        assertNoStagingFiles();
    }

    /** Even a valid Base64 envelope must contain decodable image pixels before a PNG is replaced. */
    @Test
    void nonImagePayloadKeepsExistingPng() throws Exception {
        byte[] original = {1, 2, 3, 4};
        Files.write(target("png"), original);
        assertFailure(upload("png", Base64.getEncoder().encode(original), 0, 1));
        assertArrayEquals(original, Files.readAllBytes(target("png")));
        assertNoStagingFiles();
    }

    /** PNG uploads preserve the received bytes, including metadata that pixel re-encoding would discard. */
    @Test
    void pngSavePreservesEncodedBytesAndMetadata() throws Exception {
        byte[] encoded = pngPayload();
        Files.write(target("png"), new byte[] {1, 2, 3, 4});
        JSONObject response = upload("png", encoded, 0, 1);
        assertTrue(response.getBoolean("success"), response.toString());
        assertArrayEquals(Base64.getDecoder().decode(encoded), Files.readAllBytes(target("png")));
        assertNoStagingFiles();
    }

    /** A different input format still needs conversion before it can be published under a PNG extension. */
    @Test
    void jpegInputForPngDestinationIsConverted() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(20, 16, BufferedImage.TYPE_INT_RGB), "jpg", bytes));
        JSONObject response = upload("png", Base64.getEncoder().encode(bytes.toByteArray()), 0, 1);
        assertTrue(response.getBoolean("success"), response.toString());
        try (var input = Files.newInputStream(target("png"))) {
            ImageInfo info = new ImageInfo(input);
            assertEquals(ImageInfo.FORMAT_PNG, info.getFormat());
            assertEquals(20, info.getWidth());
            assertEquals(16, info.getHeight());
        }
        assertNoStagingFiles();
    }

    /** The destination extension determines the actual bytes; transparency survives in supporting formats. */
    @ParameterizedTest
    @CsvSource({"png,image/png,true", "jpg,image/jpeg,false", "jpeg,image/jpeg,false", "gif,image/gif,true",
        "webp,image/webp,true", "bmp,image/bmp,false", "avif,image/avif,true"})
    void successfulSaveUsesRequestedFormat(String extension, String mime, boolean transparent) throws Exception {
        if ("avif".equals(extension)) assumeTrue(System.getenv("AVIF_TEST_IMAGEMAGICK_DIR") != null,
            "Set AVIF_TEST_IMAGEMAGICK_DIR to run the native AVIF editor-save test");
        Files.write(target(extension), new byte[] {1, 2, 3, 4});
        JSONObject response = upload(extension, pngPayload(), 0, 1);
        assertTrue(response.getBoolean("success"), response.toString());
        try (var input = Files.newInputStream(target(extension))) {
            ImageInfo info = new ImageInfo(input);
            assertEquals(mime, info.getMimeType());
            assertEquals(20, info.getWidth());
            assertEquals(16, info.getHeight());
        }
        Path readable = target(extension);
        if ("avif".equals(extension)) {
            readable = root.resolve("decoded.png");
            assertEquals(0, ImageTools.executeImageMagick(new IwcmFile(target(extension).toFile()),
                new IwcmFile(readable.toFile()), new ArrayList<>(List.of("from", "to"))));
        }
        BufferedImage pixels = ImageIO.read(readable.toFile());
        assertNotNull(pixels);
        if (transparent) {
            assertTrue((pixels.getRGB(0, 0) >>> 24) < 16);
        } else {
            Color corner = new Color(pixels.getRGB(0, 0));
            assertTrue(corner.getRed() > 240 && corner.getGreen() > 240 && corner.getBlue() > 240,
                "Formats without transparency must keep the editor's white background");
        }
        assertNoStagingFiles();
    }

    /** An unsupported output format must fail instead of publishing JPEG bytes under another extension. */
    @Test
    void unavailableWriterKeepsOriginal() throws Exception {
        byte[] original = "<svg/>".getBytes(StandardCharsets.UTF_8);
        Files.write(target("svg"), original);
        assertFailure(upload("svg", pngPayload(), 0, 1));
        assertArrayEquals(original, Files.readAllBytes(target("svg")));
        assertNoStagingFiles();
    }

    /** Assembly of a multi-chunk image must leave the destination untouched until the last chunk. */
    @Test
    void publishesOnlyAfterLastChunk() throws Exception {
        byte[] original = {1, 2, 3, 4};
        Files.write(target("png"), original);
        byte[] encoded = pngPayload();
        int split = encoded.length / 2;
        assertTrue(upload("png", Arrays.copyOfRange(encoded, 0, split), 0, 2).getBoolean("success"));
        assertArrayEquals(original, Files.readAllBytes(target("png")));
        assertTrue(upload("png", Arrays.copyOfRange(encoded, split, encoded.length), 1, 2).getBoolean("success"));
        assertEquals(20, ImageIO.read(target("png").toFile()).getWidth());
        assertNoStagingFiles();
    }

    /** A failed final copy must report an error and remove the temporary upload files. */
    @Test
    void failedPublishCleansTemporaryFiles() throws Exception {
        Files.createDirectory(target("png"));
        Path sentinel = target("png").resolve("keep.txt");
        Files.writeString(sentinel, "keep");
        assertFailure(upload("png", pngPayload(), 0, 1));
        assertEquals("keep", Files.readString(sentinel));
        assertNoStagingFiles();
    }

    private void setConfig(String key, String value) {
        previousSettings.put(key, Constants.getString(key));
        Constants.setString(key, value);
    }

    private Path target(String extension) {
        return root.resolve("images/autotest/image." + extension);
    }

    private JSONObject upload(String extension, byte[] payload, int chunk, int chunks) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.setMethod("POST");
        request.setRequestURI("/admin/upload/chunk");
        request.addParameter("name", "image." + extension);
        request.addParameter("uploadType", "image");
        request.addParameter("destinationFolder", "/images/autotest/");
        request.addParameter("writeDirectlyToDestination", "true");
        request.addParameter("overwriteMode", "overwrite");
        request.addParameter("encoding", "base64");
        request.addParameter("dztotalfilesize", "1000");
        request.addParameter("chunk", Integer.toString(chunk));
        request.addParameter("chunks", Integer.toString(chunks));
        request.addPart(new MockPart("file", "blob", payload));
        MockHttpServletResponse response = new MockHttpServletResponse();
        new AdminUploadServlet().doPost(request, response);
        return new JSONObject(response.getContentAsString(StandardCharsets.UTF_8));
    }

    private byte[] pngPayload() throws Exception {
        BufferedImage pixels = new BufferedImage(20, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = pixels.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(8, 6, 4, 4);
        graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        var writer = ImageIO.getImageWritersByFormatName("png").next();
        try (var output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            var metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(pixels), null);
            IIOMetadataNode tree = new IIOMetadataNode("javax_imageio_png_1.0");
            IIOMetadataNode text = new IIOMetadataNode("tEXt");
            IIOMetadataNode entry = new IIOMetadataNode("tEXtEntry");
            entry.setAttribute("keyword", "Comment");
            entry.setAttribute("value", "Preserve this metadata during Base64 decoding.");
            text.appendChild(entry);
            tree.appendChild(text);
            metadata.mergeTree(tree.getNodeName(), tree);
            writer.write(null, new IIOImage(pixels, null, metadata), null);
        } finally {
            writer.dispose();
        }
        return Base64.getEncoder().encode(bytes.toByteArray());
    }

    private void assertFailure(JSONObject response) {
        assertFalse(response.getBoolean("success"), response.toString());
        assertTrue(response.has("chunk-uploaded"), "The request must reach image processing");
        assertFalse(response.getString("error").isBlank());
    }

    private void assertNoStagingFiles() throws Exception {
        try (var files = Files.list(root.resolve("WEB-INF/tmp"))) {
            assertEquals(0, files.count(), "Upload staging files must be removed");
        }
    }
}
