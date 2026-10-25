package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static sk.iway.iwcm.test.AvifAssertions.assertAnimation;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.gallery.ImageInfo;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
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

    /** The shared flow uses AVIF output settings for both AVIF input and the PNG encoding intermediate. */
    @ParameterizedTest
    @ValueSource(strings = {"png", "avif"})
    void createsAvifInNewDirectoryWithOutputSettings(String extension) throws Exception {
        String previous = Constants.getString("imageMagickDir");
        String pngParameters = Constants.getString("imageMagickCustomParams_resize_png");
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            requireCodec();
            Constants.setString("imageMagickCustomParams_resize_png", "-invalid-image-test-option");
            Path source = directory.resolve("source." + extension);
            if ("avif".equals(extension)) {
                Files.copy(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"), source);
            } else {
                ImageIO.write(new BufferedImage(120, 80, BufferedImage.TYPE_INT_ARGB), "png", source.toFile());
            }
            byte[] original = Files.readAllBytes(source);
            Path target = directory.resolve("new folder/thumbnail.AVIF");

            assertEquals(0, ImageTools.executeImageMagick(new IwcmFile(source.toFile()), new IwcmFile(target.toFile()),
                List.of("from", "-resize", "60x40", "to")));

            assertDimensions(target, 60, 40);
            assertTransparent(target);
            assertArrayEquals(original, Files.readAllBytes(source));
        } finally {
            Constants.setString("imageMagickDir", previous);
            Constants.setString("imageMagickCustomParams_resize_png", pngParameters);
        }
    }

    /** Pixel encoding uses the requested AVIF format even if the destination has a temporary suffix. */
    @Test
    void writesAvifPixelsToTemporaryFileName() throws Exception {
        String previous = Constants.getString("imageMagickDir");
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            requireCodec();
            Path target = directory.resolve("encoded.tmp");
            assertEquals(0, ImageTools.writeImage(new BufferedImage(20, 16, BufferedImage.TYPE_INT_ARGB),
                "photo.avif", 80, new IwcmFile(target.toFile())));
            assertDimensions(target, 20, 16);
            assertTransparent(target);
        } finally {
            Constants.setString("imageMagickDir", previous);
        }
    }

    /** Real AVIF conversion stages DB input/output locally and cleans up files; database I/O is simulated. */
    @ParameterizedTest
    @CsvSource({"true,true,false", "true,false,false", "false,true,false", "true,true,true", "true,false,true"})
    void convertsBetweenLocalAndDatabaseStorage(boolean sourceInDB, boolean targetInDB, boolean animated) throws Exception {
        String previous = Constants.getString("imageMagickDir");
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            requireCodec();
            Path staging = Files.createDirectory(directory.resolve("staging"));
            Path source = directory.resolve("database/source.avif");
            Path target = directory.resolve((targetInDB ? "database/" : "local/") + "result.avif");
            Path original = Path.of("src/test/webapp/tests/webpages/"
                + (animated ? "autotest-animated-alpha.avif" : "autotest-transparent.avif"));

            try (var storage = mockStatic(IwcmFsDB.class); var files = mockStatic(FileTools.class)) {
                storage.when(() -> IwcmFsDB.getVirtualPath(anyString())).thenAnswer(call -> call.getArgument(0));
                storage.when(() -> IwcmFsDB.useDBStorage(anyString()))
                    .thenAnswer(call -> ((String) call.getArgument(0)).contains("/database/"));
                storage.when(() -> IwcmFsDB.getTempFilePath(anyString()))
                    .thenAnswer(call -> staging.resolve(Path.of((String) call.getArgument(0)).getFileName()).toString());
                storage.when(() -> IwcmFsDB.writeFileToDisk(any(File.class), any(File.class), eq(true)))
                    .thenAnswer(call -> {
                        Files.copy(original, ((File) call.getArgument(1)).toPath());
                        return null;
                    });
                files.when(() -> FileTools.getFileExtension(anyString())).thenCallRealMethod();
                files.when(() -> FileTools.copyFile(any(IwcmFile.class), any(IwcmFile.class))).thenAnswer(call -> {
                    Path encoded = Path.of(((IwcmFile) call.getArgument(0)).getAbsolutePath());
                    Path destination = Path.of(((IwcmFile) call.getArgument(1)).getAbsolutePath());
                    Files.createDirectories(destination.getParent());
                    Files.copy(encoded, destination);
                    return true;
                });

                IwcmFile output = new IwcmFile(target.toFile());
                int result = sourceInDB
                    ? ImageTools.executeImageMagick(new IwcmFile(source.toFile()), output, List.of("from", "-resize", "20x16!", "to"))
                    : ImageTools.writeImage(new BufferedImage(20, 16, BufferedImage.TYPE_INT_ARGB), "image.avif", 80, output);
                assertEquals(0, result);
            }

            assertDimensions(target, 20, 16);
            assertTransparent(target);
            if (animated) assertAnimation(target, 20, 16, 70, 230);
            try (var stagedFiles = Files.list(staging)) {
                assertEquals(0, stagedFiles.count(), "Temporary database input and output must be removed");
            }
        } finally {
            Constants.setString("imageMagickDir", previous);
        }
    }

    /** Upload watermarking changes AVIF pixels while respecting the global switch and path exceptions. */
    @ParameterizedTest
    @CsvSource({"true,false,false,false", "false,false,false,false", "true,true,false,false",
        "true,false,true,false", "false,false,true,false", "true,true,true,false", "true,false,true,true"})
    void appliesUploadWatermarkToAvif(boolean enabled, boolean excluded, boolean animated, boolean databaseWatermark) throws Exception {
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
            byte[] original = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/"
                + (animated ? "autotest-animated-alpha.avif" : "autotest-transparent.avif")));
            Files.write(target, original);

            if (databaseWatermark) {
                List<Path> staged = new ArrayList<>();
                try (var storage = mockStatic(IwcmFsDB.class, CALLS_REAL_METHODS)) {
                    storage.when(() -> IwcmFsDB.useDBStorage(anyString()))
                        .thenAnswer(call -> "/watermarks/default.svg".equals(call.getArgument(0)));
                    storage.when(IwcmFsDB::getFatIdTable).thenReturn(Map.of("/watermarks/default.svg", 1));
                    storage.when(() -> IwcmFsDB.writeFileToDisk(any(File.class), any(File.class), eq(true)))
                        .thenAnswer(call -> {
                            Path temporary = ((File) call.getArgument(1)).toPath();
                            staged.add(temporary);
                            Files.copy(directory.resolve("watermarks/default.svg"), temporary, StandardCopyOption.REPLACE_EXISTING);
                            return null;
                        });
                    GalleryDB.applyWatermarkOnUpload(new IwcmFile(target.toFile()));
                }
                assertEquals(1, staged.size(), "The database watermark is staged once");
                assertTrue(staged.stream().noneMatch(Files::exists), "Temporary watermarks must be removed");
            } else {
                GalleryDB.applyWatermarkOnUpload(new IwcmFile(target.toFile()));
            }

            if (enabled && !excluded) {
                assertDimensions(target, 120, 80);
                BufferedImage pixels = readPixels(target);
                assertTrue(pixels.getColorModel().hasAlpha());
                assertTrue((pixels.getRGB(0, 0) >>> 24) < 16, "Watermarking must preserve the transparent corner");
                Color center = new Color(pixels.getRGB(60, 40));
                assertTrue(center.getBlue() > 200 && center.getRed() < 60,
                    "The blue watermark must replace the red center of the uploaded AVIF");
                if (animated) {
                    assertAnimation(target, 120, 80, 70, 230);
                    BufferedImage second = readPixels(target, 1);
                    assertTrue((second.getRGB(0, 0) >>> 24) < 16);
                    assertTrue(new Color(second.getRGB(60, 40)).getBlue() > 200, "Watermark on the second frame");
                    assertTrue((second.getRGB(30, 40) >>> 24) < 16, "No previous-frame residue");
                }
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
    @ParameterizedTest
    @ValueSource(strings = {"autotest-transparent.avif", "autotest-animated-alpha.avif"})
    void generatesGalleryVariantsAndKeepsOriginal(String fixture) throws Exception {
        String previous = Constants.getString("imageMagickDir");
        String cache = Constants.getString("thumbServletCacheDir");
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            requireCodec();
            Constants.setString("thumbServletCacheDir", directory.toString());
            Path target = directory.resolve("gallery.avif");
            byte[] original = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/" + fixture));
            Files.write(target, original);
            assertTrue(GalleryDB.resizePictureImpl(new java.awt.Dimension[] {
                new java.awt.Dimension(30, 20), new java.awt.Dimension(60, 40)
            }, target.toString(), null, null, "C", "W"));
            assertDimensions(directory.resolve("s_gallery.avif"), 30, 20);
            assertDimensions(target, 60, 40);
            assertArrayEquals(original, Files.readAllBytes(directory.resolve("o_gallery.avif")));
            if (fixture.contains("animated")) {
                assertAnimation(directory.resolve("s_gallery.avif"), 30, 20, 70, 230);
                assertAnimation(target, 60, 40, 70, 230);
            }
        } finally {
            Constants.setString("imageMagickDir", previous);
            Constants.setString("thumbServletCacheDir", cache);
        }
    }

    /** An AVIF source smaller than the requested bounds is copied unchanged without a codec. */
    @ParameterizedTest
    @ValueSource(strings = {"S", "C"})
    void largerThumbnailCopiesOriginal(String resizeMode) throws Exception {
        String previous = Constants.getString("imageMagickDir");
        String cache = Constants.getString("thumbServletCacheDir");
        try {
            Constants.setString("imageMagickDir", directory.resolve("missing").toString());
            Constants.setString("thumbServletCacheDir", directory.toString());
            Path source = Path.of("src/test/webapp/tests/webpages/autotest-animated.avif");
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

    /** Resizing and cropping preserve both frames, their timing and independent transparency. */
    @ParameterizedTest
    @CsvSource({"S,60,40", "C,40,40"})
    void smallerThumbnailPreservesAnimation(String resizeMode, int width, int height) throws Exception {
        String previous = Constants.getString("imageMagickDir");
        String cache = Constants.getString("thumbServletCacheDir");
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            requireCodec();
            Constants.setString("thumbServletCacheDir", directory.toString());
            Path source = Path.of("src/test/webapp/tests/webpages/autotest-animated-alpha.avif");
            byte[] original = Files.readAllBytes(source);
            Path target = directory.resolve("animation.avif");
            assertEquals(0, GalleryDB.resizePicture(source.toString(), target.toString(), width, height, resizeMode));
            assertAnimation(target, width, height, 70, 230);
            BufferedImage first = readPixels(target, 0);
            BufferedImage second = readPixels(target, 1);
            assertTrue(new Color(first.getRGB(width / 4, height / 2)).getRed() > 200);
            assertTrue(new Color(second.getRGB(3 * width / 4, height / 2)).getBlue() > 200);
            assertTrue((first.getRGB(3 * width / 4, height / 2) >>> 24) < 16);
            assertTrue((second.getRGB(width / 4, height / 2) >>> 24) < 16, "No previous-frame residue");
            assertArrayEquals(original, Files.readAllBytes(source));
        } finally {
            Constants.setString("imageMagickDir", previous);
            Constants.setString("thumbServletCacheDir", cache);
        }
    }

    /** Invalid image metadata must be reported as a failed gallery operation. */
    @Test
    void rejectsMalformedGalleryInput() throws Exception {
        String cache = Constants.getString("thumbServletCacheDir");
        try {
            Constants.setString("thumbServletCacheDir", directory.toString());
            Path target = directory.resolve("corrupt.avif");
            byte[] invalid = {0, 0, 0, 32};
            Files.write(target, invalid);
            org.junit.jupiter.api.Assertions.assertFalse(GalleryDB.resizePictureImpl(new java.awt.Dimension[] {
                new java.awt.Dimension(30, 20), new java.awt.Dimension(60, 40)
            }, target.toString(), null, null, "C", "W"));
            assertArrayEquals(invalid, Files.readAllBytes(target));
        } finally {
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

    /** Server-side edits and metadata stripping preserve the complete AVIF sequence. */
    @Test
    void editsAnimatedAvif() throws Exception {
        String previous = Constants.getString("imageMagickDir");
        String stripExif = Constants.getString("galleryStripExif");
        try {
            Constants.setString("imageMagickDir", System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin"));
            Constants.setString("galleryStripExif", "true");
            requireCodec();
            Path target = directory.resolve("animated.avif");
            Files.copy(Path.of("src/test/webapp/tests/webpages/autotest-animated-alpha.avif"), target);
            IwcmFile image = new IwcmFile(target.toFile());
            assertEquals(0, ImageTools.resizeImage(image, 60, 40));
            assertAnimation(target, 60, 40, 70, 230);
            assertEquals(0, ImageTools.cropImage(image, 50, 30, 0, 0));
            assertAnimation(target, 50, 30, 70, 230);
            assertEquals(0, ImageTools.rotateImage(image, 90));
            assertAnimation(target, 30, 50, 70, 230);
            GalleryDB.stripExif(target.toString());
            assertAnimation(target, 30, 50, 70, 230);
            assertTransparent(target);
        } finally {
            Constants.setString("imageMagickDir", previous);
            Constants.setString("galleryStripExif", stripExif);
        }
    }

    /** A native timeout kills the process and leaves both the original and staging directory intact. */
    @Test
    void timeoutPreservesAnimatedOriginal() throws Exception {
        Path target = directory.resolve("timeout.avif");
        byte[] original = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-animated-alpha.avif"));
        Files.write(target, original);
        Process process = mock(Process.class);
        try (var builders = mockConstruction(ProcessBuilder.class, (builder, context) -> {
            when(builder.redirectOutput(any(ProcessBuilder.Redirect.class))).thenReturn(builder);
            when(builder.redirectError(any(ProcessBuilder.Redirect.class))).thenReturn(builder);
            when(builder.start()).thenReturn(process);
        })) {
            assertEquals(2, ImageTools.executeImageMagick(new IwcmFile(target.toFile()), List.of("from", "-resize", "60x40", "to")));
            verify(process).waitFor(60, TimeUnit.SECONDS);
            verify(process).destroyForcibly();
        }
        assertArrayEquals(original, Files.readAllBytes(target));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count(), "No staged output remains after a timeout");
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
        return readPixels(image, 0);
    }

    private BufferedImage readPixels(Path image, int frame) throws Exception {
        Path decoded = directory.resolve("decoded.png");
        Process process = new ProcessBuilder(ImageTools.getImageMagickCommand(), image + "[" + frame + "]", decoded.toString())
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
