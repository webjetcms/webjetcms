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
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static sk.iway.iwcm.test.AvifAssertions.assertAnimation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import jakarta.servlet.ServletContext;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.gallery.GalleryDB;
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

    /** A failed conversion serves a real GIF with the configured static-content cache policy. */
    @ParameterizedTest
    @ValueSource(strings = {"png", "jpg", "avif"})
    void corruptImageReturnsCacheablePlaceholder(String extension) throws Exception {
        byte[] original = {0, 1, 2, 3};
        Files.write(source(extension), original);
        assertPlaceholder(requestImage(extension));
        assertArrayEquals(original, Files.readAllBytes(source(extension)));
        assertFalse(Files.exists(cachedImage(extension)));
    }

    /** Missing AVIF processing support returns a cacheable GIF without creating a thumbnail on disk. */
    @Test
    void unavailableAvifCodecReturnsPlaceholder() throws Exception {
        Files.copy(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"), source("avif"));
        assertPlaceholder(requestImage("avif"));
        assertFalse(Files.exists(cachedImage("avif")));
    }

    /** A timed-out conversion serves the placeholder without caching a partial AVIF. */
    @Test
    void animatedAvifTimeoutReturnsPlaceholder() throws Exception {
        String executable = System.getProperty("os.name").toLowerCase().contains("win") ? "magick.exe" : "magick";
        Files.createFile(root.resolve(executable));
        setConfig("imageMagickDir", root.toString());
        byte[] original = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-animated-alpha.avif"));
        Files.write(source("avif"), original);
        Process process = mock(Process.class);
        try (var builders = mockConstruction(ProcessBuilder.class, (builder, context) -> {
            when(builder.redirectOutput(any(ProcessBuilder.Redirect.class))).thenReturn(builder);
            when(builder.redirectError(any(ProcessBuilder.Redirect.class))).thenReturn(builder);
            when(builder.start()).thenReturn(process);
        })) {
            assertPlaceholder(requestImage("avif"));
            verify(process).waitFor(60, TimeUnit.SECONDS);
            verify(process).destroyForcibly();
        }
        assertFalse(Files.exists(cachedImage("avif")));
        assertArrayEquals(original, Files.readAllBytes(source("avif")));
    }

    /** A request reaching the server after source repair generates the thumbnail at the same URL. */
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
    @ParameterizedTest
    @ValueSource(strings = {"autotest-transparent.avif", "autotest-animated-alpha.avif"})
    void cachedAvifKeepsItsMimeTypeAndCaching(String fixture) throws Exception {
        byte[] image = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/" + fixture));
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

    /** Every thumbnail mode preserves animation, and cached responses no longer require the codec. */
    @ParameterizedTest
    @ValueSource(ints = {-1, 1, 2, 3, 4, 5, 6})
    void generatesAndCachesAnimatedAvif(int ip) throws Exception {
        setConfig("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
        if (System.getenv("AVIF_TEST_IMAGEMAGICK_DIR") != null) assertTrue(ImageTools.existsImageMagickConvertCommand());
        else assumeTrue(ImageTools.existsImageMagickConvertCommand());
        byte[] original = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-animated-alpha.avif"));
        Files.write(source("avif"), original);
        Files.setLastModifiedTime(source("avif"), FileTime.fromMillis(1000));
        // The source is outside the gallery; only its optional database crop selection is simulated.
        try (var gallery = mockStatic(GalleryDB.class, CALLS_REAL_METHODS)) {
            gallery.when(() -> GalleryDB.getGalleryBean(anyString(), any())).thenReturn(null);
            MockHttpServletResponse response = requestImage("avif", ip);
            assertEquals(200, response.getStatus());
            assertEquals("image/avif", response.getContentType());
            Path output = root.resolve("response.avif");
            Files.write(output, response.getContentAsByteArray());
            assertAnimation(output, 30, 20, 70, 230);
            setConfig("imageMagickDir", root.resolve("missing-imagemagick").toString());
            MockHttpServletResponse cached = requestImage("avif", ip);
            assertEquals("image/avif", cached.getContentType());
            assertEquals("public, max-age=3600", cached.getHeader("Cache-Control"));
            assertArrayEquals(response.getContentAsByteArray(), cached.getContentAsByteArray());
            assertArrayEquals(original, Files.readAllBytes(source("avif")));
        }
    }

    /** Cluster responses cache both GIF placeholders and AVIF images while honoring explicit no-store. */
    @ParameterizedTest
    @CsvSource({"true,false,false", "false,false,false", "true,true,false", "false,false,true"})
    void forwardsRenderingNodeResponse(boolean failed, boolean noStore, boolean animated) throws Exception {
        byte[] image = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/"
            + (animated ? "autotest-animated-alpha.avif" : "autotest-transparent.avif")));
        Files.write(source("avif"), image);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/thumb/", exchange -> {
            byte[] body = failed ? placeholder : image;
            exchange.getResponseHeaders().set("Content-Type", failed ? "image/gif" : "image/avif");
            exchange.getResponseHeaders().set("Cache-Control", noStore ? "no-store" : "public, max-age=3600");
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
                assertPlaceholder(response, noStore ? "no-store" : "public, max-age=3600");
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
        return requestImage(extension, -1);
    }

    private MockHttpServletResponse requestImage(String extension, int ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thumb/images/autotest/image." + extension);
        request.setAttribute("path_filter_orig_path", request.getRequestURI());
        request.setParameter("w", "30");
        request.setParameter("h", "20");
        request.setQueryString("w=30&h=20");
        if (ip > 0) {
            request.setParameter("ip", Integer.toString(ip));
            request.setQueryString(request.getQueryString() + "&ip=" + ip);
        }
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
        assertPlaceholder(response, "public, max-age=3600");
    }

    private void assertPlaceholder(MockHttpServletResponse response, String cacheControl) throws Exception {
        assertEquals(200, response.getStatus());
        assertEquals("image/gif", response.getContentType());
        assertEquals(cacheControl, response.getHeader("Cache-Control"));
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
