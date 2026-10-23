package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies the shared editing flow with ImageMagick and the existing Java fallback. */
class ImageToolsTransformTest extends BaseWebjetTest {
    @TempDir
    Path directory;

    /** Resize, crop and rotation keep the output format, dimensions and supported transparency. */
    @ParameterizedTest
    @CsvSource({
        "png,true,true", "gif,true,true", "webp,true,true",
        "jpg,true,false", "jpeg,true,false", "bmp,true,false",
        "png,false,true", "gif,false,true", "webp,false,true",
        "jpg,false,false", "jpeg,false,false", "bmp,false,false"
    })
    void editsImage(String extension, boolean useImageMagick, boolean transparent) throws Exception {
        String previous = Constants.getString("imageMagickDir");
        boolean stripExif = Constants.getBoolean("galleryStripExif");
        try {
            Constants.setString("imageMagickDir", useImageMagick
                ? System.getenv().getOrDefault("AVIF_TEST_IMAGEMAGICK_DIR", "/usr/bin")
                : directory.resolve("missing-imagemagick").toString());
            Constants.setBoolean("galleryStripExif", false);
            if (useImageMagick) {
                if (System.getenv("AVIF_TEST_IMAGEMAGICK_DIR") != null) {
                    assertTrue(ImageTools.existsImageMagickConvertCommand());
                } else {
                    assumeTrue(ImageTools.existsImageMagickConvertCommand(), "Requires ImageMagick");
                }
            }

            BufferedImage source = new BufferedImage(120, 80,
                transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = source.createGraphics();
            try {
                graphics.setColor(Color.RED);
                graphics.fillRect(40, 20, 40, 40);
            } finally {
                graphics.dispose();
            }
            Path target = directory.resolve("image." + extension);
            IwcmFile file = new IwcmFile(target.toFile());
            assertEquals(0, ImageTools.writeImage(source, file.getName(), 100, file));

            assertEquals(0, ImageTools.resizeImage(file, 60, 40));
            assertDimensions(target, 60, 40);
            assertEquals(0, ImageTools.cropImage(file, 40, 30, 5, 5));
            assertDimensions(target, 40, 30);
            assertEquals(0, ImageTools.rotateImage(file, 90));
            assertDimensions(target, 30, 40);
            assertEquals(0, ImageTools.rotateImage(file, 45));
            BufferedImage rotated = ImageIO.read(target.toFile());
            assertNotNull(rotated);
            assertTrue(rotated.getWidth() > 40 && rotated.getHeight() > 40);
            if (transparent) {
                assertTrue(rotated.getColorModel().hasAlpha());
                assertEquals(0, rotated.getRGB(0, 0) >>> 24, "Rotation corners must remain transparent");
            }
            try (var stream = ImageIO.createImageInputStream(target.toFile())) {
                var readers = ImageIO.getImageReaders(stream);
                assertTrue(readers.hasNext(), "Output must remain a readable image");
                var reader = readers.next();
                try {
                    String expectedFormat = "jpg".equals(extension) ? "jpeg" : extension;
                    assertTrue(expectedFormat.equalsIgnoreCase(reader.getFormatName()), "Output format must match its extension");
                } finally {
                    reader.dispose();
                }
            }
        } finally {
            Constants.setString("imageMagickDir", previous);
            Constants.setBoolean("galleryStripExif", stripExif);
        }
    }

    private void assertDimensions(Path path, int width, int height) throws Exception {
        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image);
        assertEquals(width, image.getWidth());
        assertEquals(height, image.getHeight());
    }
}
