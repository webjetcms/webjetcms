package sk.iway.iwcm.editor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import jakarta.servlet.ServletContext;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Covers placeholder responses, cache recovery and forwarding through a thumbnail rendering node. */
class ThumbServletFallbackTest extends BaseWebjetTest {
    @TempDir
    Path root;
    private ServletContext previousContext;
    private final Map<String, String> previousSettings = new LinkedHashMap<>();
    private byte[] placeholder;

    @BeforeEach
    void setUp() throws Exception {
        root = root.toRealPath();
        previousContext = Constants.getServletContext();
        Constants.setServletContext(new MockServletContext() {
            @Override
            public String getRealPath(String path) {
                return root.resolve(path.replaceFirst("^/", "")).toString() + ("/".equals(path) ? "/" : "");
            }

            @Override
            public String getMimeType(String path) {
                return path.endsWith(".avif") ? "image/avif" : super.getMimeType(path);
            }
        });
        setConfig("multiDomainEnabled", "false");
        setConfig("thumbServletAllowedSizeMode", "allow");
        setConfig("thumbServletCacheDir", "/WEB-INF/imgcache/");
        setConfig("thumbWriteServer", "");
        setConfig("thumbWriteNodeName", "");
        setConfig("imageMagickDir", root.resolve("missing-imagemagick").toString());
        setConfig("galleryEnableWatermarking", "false");
        Files.createDirectories(source("png").getParent());
        Path fallback = root.resolve("components/_common/mime/big_unknown.gif");
        Files.createDirectories(fallback.getParent());
        placeholder = Files.readAllBytes(Path.of("src/main/webapp/components/_common/mime/big_unknown.gif"));
        Files.write(fallback, placeholder);
    }

    @AfterEach
    void tearDown() {
        Constants.setServletContext(previousContext);
        previousSettings.forEach(Constants::setString);
    }

    /** A failed conversion serves a real GIF without caching it under any requested image extension. */
    @ParameterizedTest
    @ValueSource(strings = {"png", "jpg", "avif"})
    void corruptImageReturnsUncachedPlaceholder(String extension) throws Exception {
        byte[] original = {0, 1, 2, 3};
        Files.write(source(extension), original);
        assertPlaceholder(requestImage(extension));
        assertArrayEquals(original, Files.readAllBytes(source(extension)));
        assertFalse(Files.exists(cachedImage(extension)));
    }

    /** Missing AVIF processing support falls back to the same GIF as other processing failures. */
    @Test
    void unavailableAvifCodecReturnsPlaceholder() throws Exception {
        Files.copy(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"), source("avif"));
        assertPlaceholder(requestImage("avif"));
        assertFalse(Files.exists(cachedImage("avif")));
    }

    /** A subsequent request can generate the real thumbnail once the source has been repaired. */
    @Test
    void repairedSourceGeneratesThumbnailAtSameUrl() throws Exception {
        Files.write(source("png"), new byte[] {0, 1, 2, 3});
        assertPlaceholder(requestImage("png"));
        assertTrue(ImageIO.write(new BufferedImage(120, 80, BufferedImage.TYPE_INT_ARGB), "png", source("png").toFile()));
        MockHttpServletResponse response = requestImage("png");
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertEquals("public, max-age=3600", response.getHeader("Cache-Control"));
        BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        assertNotNull(thumbnail);
        assertEquals(30, thumbnail.getWidth());
        assertEquals(20, thumbnail.getHeight());
    }

    /** An existing valid thumbnail remains available and cacheable even when the codec is unavailable. */
    @Test
    void cachedAvifKeepsItsMimeTypeAndCaching() throws Exception {
        byte[] image = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"));
        Files.write(source("avif"), image);
        Files.setLastModifiedTime(source("avif"), FileTime.fromMillis(1000));
        Files.createDirectories(cachedImage("avif").getParent());
        Files.write(cachedImage("avif"), image);
        MockHttpServletResponse response = requestImage("avif");
        assertEquals(200, response.getStatus());
        assertEquals("image/avif", response.getContentType());
        assertEquals("public, max-age=3600", response.getHeader("Cache-Control"));
        assertArrayEquals(image, response.getContentAsByteArray());
    }

    /** Cluster forwarding preserves the rendering node's GIF/no-store response and regular AVIF caching. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void forwardsRenderingNodeResponse(boolean failed) throws Exception {
        byte[] image = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"));
        Files.write(source("avif"), image);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/thumb/", exchange -> {
            byte[] body = failed ? placeholder : image;
            exchange.getResponseHeaders().set("Content-Type", failed ? "image/gif" : "image/avif");
            exchange.getResponseHeaders().set("Cache-Control", failed ? "no-store" : "public, max-age=3600");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            setConfig("thumbWriteServer", "http://127.0.0.1:" + server.getAddress().getPort() + "/thumb/");
            setConfig("thumbWriteNodeName", "autotest-renderer");
            setConfig("clusterMyNodeName", "autotest-reader");
            MockHttpServletResponse response = requestImage("avif");
            if (failed) {
                assertPlaceholder(response);
            } else {
                assertEquals(200, response.getStatus());
                assertEquals("image/avif", response.getContentType());
                assertEquals("public, max-age=3600", response.getHeader("Cache-Control"));
                assertArrayEquals(image, response.getContentAsByteArray());
            }
        } finally {
            server.stop(0);
        }
    }

    private MockHttpServletResponse requestImage(String extension) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thumb/images/autotest/image." + extension);
        request.setAttribute("path_filter_orig_path", request.getRequestURI());
        request.setParameter("w", "30");
        request.setParameter("h", "20");
        request.setQueryString("w=30&h=20");
        MockHttpServletResponse response = new MockHttpServletResponse();
        // Isolate access checks and the existing static-header policy; image processing and response streaming are real.
        try (var paths = mockStatic(PathFilter.class); var spam = mockStatic(SpamProtection.class)) {
            paths.when(() -> PathFilter.setNoCacheHeaders(response)).thenCallRealMethod();
            paths.when(() -> PathFilter.getRealPath(anyString())).thenAnswer(call ->
                root.resolve(((String) call.getArgument(0)).replaceFirst("^/", "")).toString());
            paths.when(() -> PathFilter.setStaticContentHeaders(anyString(), any(), eq(request), eq(response)))
                .thenAnswer(call -> {
                    response.setHeader("Cache-Control", "public, max-age=3600");
                    return true;
                });
            spam.when(() -> SpamProtection.canPost(eq("ThumbServlet"), anyString(), eq(request))).thenReturn(true);
            new ThumbServlet().doGet(request, response);
        }
        return response;
    }

    private void assertPlaceholder(MockHttpServletResponse response) throws Exception {
        assertEquals(200, response.getStatus());
        assertEquals("image/gif", response.getContentType());
        assertNotNull(response.getHeader("Cache-Control"));
        assertTrue(response.getHeader("Cache-Control").contains("no-store"));
        assertArrayEquals(placeholder, response.getContentAsByteArray());
        assertNotNull(ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray())));
    }

    private Path source(String extension) {
        return root.resolve("images/autotest/image." + extension);
    }

    private Path cachedImage(String extension) {
        return Path.of(ThumbServlet.getImagePathCache(root.resolve("WEB-INF/imgcache/images/autotest/image." + extension)
            .toString(), 30, 20, -1, false, "ffffff", -1));
    }

    private void setConfig(String key, String value) {
        previousSettings.putIfAbsent(key, Constants.getString(key));
        Constants.setString(key, value);
    }
}
