package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.gallery.ImageInfo;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies AVIF processing and preservation of originals when the codec is unavailable. */
class ImageToolsAvifTest extends BaseWebjetTest {
    @TempDir
    Path directory;

    /** Failed encoding must not replace AVIF with JPEG or damage the existing file. */
    @Test
    void missingCodecPreservesOriginal() throws Exception {
        Path target = directory.resolve("image.avif");
        byte[] original = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"));
        Files.write(target, original);
        String previous = Constants.getString("imageMagickDir");
        try {
            Constants.setString("imageMagickDir", directory.resolve("missing").toString());
            assertNotEquals(0, ImageTools.writeImage(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB),
                "image.avif", 80, new IwcmFile(target.toFile())));
            assertArrayEquals(original, Files.readAllBytes(target));
        } finally {
            Constants.setString("imageMagickDir", previous);
        }
    }

    /** Upload watermarking changes AVIF pixels while respecting the global switch and path exceptions. */
    @ParameterizedTest
    @CsvSource({"true,false", "false,false", "true,true"})
    void appliesUploadWatermarkToAvif(boolean enabled, boolean excluded) throws Exception {
        directory = directory.toRealPath();
        ServletContext previousContext = Constants.getServletContext();
        RequestBean previousRequest = SetCharacterEncodingFilter.getCurrentRequestBean();
        Map<String, String> settings = Map.of(
            "imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"),
            "galleryWatermarkApplyOnUpload", Boolean.toString(enabled),
            "galleryWatermarkApplyOnUploadExceptions", excluded ? "upload-skip" : "",
            "galleryWatermarkApplyOnUploadDir", "/watermarks/",
            "galleryWatermarkGravity", "Center",
            "galleryWatermarkSaturation", "100",
            "galleryWatermarkSvgSizePercent", "5",
            "galleryWatermarkSvgMinHeight", "30");
        Map<String, String> previousSettings = new HashMap<>();
        settings.keySet().forEach(key -> previousSettings.put(key, Constants.getString(key)));
        try {
            settings.forEach(Constants::setString);
            requireCodec();
            String composite = System.getProperty("os.name").toLowerCase().contains("win") ? "magick.exe" : "composite";
            assertTrue(Files.exists(Path.of(Constants.getString("imageMagickDir"), composite)), "Requires ImageMagick composite");
            Constants.setServletContext(new MockServletContext() {
                @Override
                public String getRealPath(String path) {
                    return directory.resolve(path.replaceFirst("^/", "")).toString() + ("/".equals(path) ? "/" : "");
                }
            });
            RequestBean request = new RequestBean();
            request.setDomain("watermark.example");
            SetCharacterEncodingFilter.setCurrentRequestBean(request);
            Files.createDirectories(directory.resolve("watermarks"));
            Files.writeString(directory.resolve("watermarks/default.svg"),
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"30\" height=\"30\"><rect width=\"30\" height=\"30\" fill=\"blue\"/></svg>");
            Path target = directory.resolve("upload-skip.avif");
            byte[] original = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"));
            Files.write(target, original);

            GalleryDB.applyWatermarkOnUpload(new IwcmFile(target.toFile()));

            if (enabled && !excluded) {
                assertDimensions(target, 120, 80);
                BufferedImage pixels = readPixels(target);
                assertTrue(pixels.getColorModel().hasAlpha());
                assertTrue((pixels.getRGB(0, 0) >>> 24) < 16, "Watermarking must preserve the transparent corner");
                Color center = new Color(pixels.getRGB(60, 40));
                assertTrue(center.getBlue() > 200 && center.getRed() < 60,
                    "The blue watermark must replace the red center of the uploaded AVIF");
            } else {
                assertArrayEquals(original, Files.readAllBytes(target));
            }
        } finally {
            Constants.setServletContext(previousContext);
            SetCharacterEncodingFilter.setCurrentRequestBean(previousRequest);
            previousSettings.forEach(Constants::setString);
        }
    }

    /** Direct callers can strip AVIF metadata, while disabling the setting preserves every byte. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void stripsAvifExifWhenEnabled(boolean enabled) throws Exception {
        String previous = Constants.getString("imageMagickDir");
        String stripExif = Constants.getString("galleryStripExif");
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            requireCodec();
            Constants.setString("galleryStripExif", Boolean.toString(enabled));
            Path target = directory.resolve("metadata.avif");
            byte[] original = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-exif.avif"));
            Files.write(target, original);
            assertEquals("exif", readProfiles(target));

            GalleryDB.stripExif(target.toString());

            assertDimensions(target, 8, 8);
            assertEquals(enabled ? "" : "exif", readProfiles(target));
            if (!enabled) assertArrayEquals(original, Files.readAllBytes(target));
        } finally {
            Constants.setString("imageMagickDir", previous);
            Constants.setString("galleryStripExif", stripExif);
        }
    }

    /** The same processing entry points used by file-manager edits preserve AVIF and alpha. */
    @Test
    void resizesCropsAndRotatesAvif() throws Exception {
        String previous = Constants.getString("imageMagickDir");
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            requireCodec();
            Path target = directory.resolve("image.avif");
            Files.copy(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"), target);
            IwcmFile image = new IwcmFile(target.toFile());
            assertEquals(0, ImageTools.resizeImage(image, 60, 40));
            assertDimensions(target, 60, 40);
            assertEquals(0, ImageTools.cropImage(image, 40, 30, 0, 0));
            assertDimensions(target, 40, 30);
            assertEquals(0, ImageTools.rotateImage(image, 90));
            assertDimensions(target, 30, 40);
            assertTransparent(target);
            assertEquals(0, ImageTools.rotateImage(image, 45));
            assertTransparent(target);
        } finally {
            Constants.setString("imageMagickDir", previous);
        }
    }

    /** Thumbnail crop/resize preserves the uploaded original and writes a correctly sized AVIF. */
    @Test
    void generatesTransparentThumbnail() throws Exception {
        String previous = Constants.getString("imageMagickDir");
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            requireCodec();
            Path source = Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif");
            byte[] original = Files.readAllBytes(source);
            Path target = directory.resolve("thumb.avif");
            assertEquals(0, GalleryDBTools.cropAndResize(new IwcmFile(source.toFile()), 100, 70, 0, 0,
                50, 35, null, true, new IwcmFile(target.toFile()), 80, 5));
            assertDimensions(target, 50, 35);
            assertTransparent(target);
            assertArrayEquals(original, Files.readAllBytes(source));
        } finally {
            Constants.setString("imageMagickDir", previous);
        }
    }

    /** Gallery creates AVIF small/large variants and retains the original bytes. */
    @Test
    void generatesGalleryVariantsAndKeepsOriginal() throws Exception {
        String previous = Constants.getString("imageMagickDir");
        String cache = Constants.getString("thumbServletCacheDir");
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            requireCodec();
            Constants.setString("thumbServletCacheDir", directory.toString());
            Path target = directory.resolve("gallery.avif");
            byte[] original = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"));
            Files.write(target, original);
            assertTrue(GalleryDB.resizePictureImpl(new java.awt.Dimension[] {
                new java.awt.Dimension(30, 20), new java.awt.Dimension(60, 40)
            }, target.toString(), null, null, "C", "W"));
            assertDimensions(directory.resolve("s_gallery.avif"), 30, 20);
            assertDimensions(target, 60, 40);
            assertArrayEquals(original, Files.readAllBytes(directory.resolve("o_gallery.avif")));
        } finally {
            Constants.setString("imageMagickDir", previous);
            Constants.setString("thumbServletCacheDir", cache);
        }
    }

    /** An AVIF source smaller than the requested bounds is copied unchanged without a codec. */
    @ParameterizedTest
    @CsvSource({"S,animated", "C,animated", "S,transparent", "C,transparent"})
    void largerThumbnailCopiesOriginal(String resizeMode, String fixture) throws Exception {
        String previous = Constants.getString("imageMagickDir");
        String cache = Constants.getString("thumbServletCacheDir");
        try {
            Constants.setString("imageMagickDir", directory.resolve("missing").toString());
            Constants.setString("thumbServletCacheDir", directory.toString());
            Path source = Path.of("src/test/webapp/tests/webpages/autotest-" + fixture + ".avif");
            byte[] original = Files.readAllBytes(source);
            Path target = directory.resolve("copy.avif");
            assertEquals(1, GalleryDB.resizePicture(source.toString(), target.toString(), 240, 160, resizeMode));
            assertDimensions(target, 120, 80);
            assertArrayEquals(original, Files.readAllBytes(target));
            assertArrayEquals(original, Files.readAllBytes(source));
        } finally {
            Constants.setString("imageMagickDir", previous);
            Constants.setString("thumbServletCacheDir", cache);
        }
    }

    /** Resizing an animated source still produces a static thumbnail from its first frame. */
    @ParameterizedTest
    @ValueSource(strings = {"S", "C"})
    void smallerThumbnailOfAnimationIsStatic(String resizeMode) throws Exception {
        String previous = Constants.getString("imageMagickDir");
        String cache = Constants.getString("thumbServletCacheDir");
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            requireCodec();
            Constants.setString("thumbServletCacheDir", directory.toString());
            Path source = Path.of("src/test/webapp/tests/webpages/autotest-animated.avif");
            byte[] original = Files.readAllBytes(source);
            Path target = directory.resolve("animation.avif");
            assertEquals(0, GalleryDB.resizePicture(source.toString(), target.toString(), 60, 40, resizeMode));
            assertDimensions(target, 60, 40);
            // ImageMagick writes static AVIF with avif, timed sequences with avis.
            assertEquals("avif", new String(Files.readAllBytes(target), 8, 4, java.nio.charset.StandardCharsets.US_ASCII));
            assertArrayEquals(original, Files.readAllBytes(source));
        } finally {
            Constants.setString("imageMagickDir", previous);
            Constants.setString("thumbServletCacheDir", cache);
        }
    }

    /** Invalid image metadata must be reported as a failed gallery operation. */
    @Test
    void rejectsMalformedGalleryInput() throws Exception {
        String previous = Constants.getString("imageMagickDir");
        String cache = Constants.getString("thumbServletCacheDir");
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            requireCodec();
            Constants.setString("thumbServletCacheDir", directory.toString());
            Path target = directory.resolve("corrupt.avif");
            byte[] invalid = {0, 0, 0, 32};
            Files.write(target, invalid);
            org.junit.jupiter.api.Assertions.assertFalse(GalleryDB.resizePictureImpl(new java.awt.Dimension[] {
                new java.awt.Dimension(30, 20), new java.awt.Dimension(60, 40)
            }, target.toString(), null, null, "C", "W"));
            assertArrayEquals(invalid, Files.readAllBytes(target));
        } finally {
            Constants.setString("imageMagickDir", previous);
            Constants.setString("thumbServletCacheDir", cache);
        }
    }

    private void requireCodec() {
        if (System.getenv("AVIF_TEST_IMAGEMAGICK_DIR") != null) {
            assertTrue(ImageTools.existsImageMagickConvertCommand(), "The explicitly configured ImageMagick must exist");
        } else {
            assumeTrue(ImageTools.existsImageMagickConvertCommand(), "Requires ImageMagick; configure AVIF_TEST_IMAGEMAGICK_DIR with AVIF support");
        }
    }

    /** A native conversion error cannot damage the source. */
    @ParameterizedTest
    @CsvSource({"avif,2", "png,3"})
    void failedConversionPreservesOriginal(String extension, int expectedResult) throws Exception {
        String previous = Constants.getString("imageMagickDir");
        String parametersKey = "imageMagickCustomParams_resize_" + extension;
        String parameters = Constants.getString(parametersKey);
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            requireCodec();
            Path target = directory.resolve("invalid-operation." + extension);
            if ("avif".equals(extension)) {
                Files.copy(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"), target);
            } else {
                ImageIO.write(new BufferedImage(120, 80, BufferedImage.TYPE_INT_ARGB), "png", target.toFile());
            }
            byte[] original = Files.readAllBytes(target);
            Constants.setString(parametersKey, "-invalid-image-test-option");
            assertEquals(expectedResult, ImageTools.executeImageMagick(new IwcmFile(target.toFile()),
                new ArrayList<>(List.of("from", "-resize", "60x40", "to"))));
            assertArrayEquals(original, Files.readAllBytes(target));
        } finally {
            Constants.setString("imageMagickDir", previous);
            Constants.setString(parametersKey, parameters);
        }
    }

    private void assertDimensions(Path path, int width, int height) throws Exception {
        try (var stream = Files.newInputStream(path)) {
            ImageInfo info = new ImageInfo(stream);
            assertEquals("AVIF", info.getFormatName());
            assertEquals(width, info.getWidth());
            assertEquals(height, info.getHeight());
        }
    }

    private String readProfiles(Path image) throws Exception {
        Path profiles = directory.resolve("profiles.txt");
        Process process = new ProcessBuilder(ImageTools.getImageMagickCommand(), image.toString(), "-format", "%[profiles]", "info:")
            .redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(profiles.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS));
            assertEquals(0, process.exitValue());
            return Files.readString(profiles).trim();
        } finally {
            process.destroyForcibly();
        }
    }

    private void assertTransparent(Path image) throws Exception {
        BufferedImage pixels = readPixels(image);
        assertTrue(pixels.getColorModel().hasAlpha());
        assertTrue((pixels.getRGB(0, 0) >>> 24) < 16, "The corner must remain transparent after lossy encoding");
    }

    private BufferedImage readPixels(Path image) throws Exception {
        Path decoded = directory.resolve("decoded.png");
        Process process = new ProcessBuilder(ImageTools.getImageMagickCommand(), image.toString(), decoded.toString())
            .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS));
            assertEquals(0, process.exitValue());
        } finally {
            process.destroyForcibly();
        }
        return ImageIO.read(decoded.toFile());
    }
}
