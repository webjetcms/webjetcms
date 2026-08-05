package sk.iway.iwcm.gallery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.test.BaseWebjetTest;

class GalleryDBResizePictureTest extends BaseWebjetTest {

    @TempDir
    Path tempDir;

    private String originalThumbServletCacheDir;
    private String originalImageMagickDir;
    private boolean originalGalleryEnableWatermarking;

    @BeforeEach
    void setUp() {
        originalThumbServletCacheDir = Constants.getString("thumbServletCacheDir");
        originalImageMagickDir = Constants.getString("imageMagickDir");
        originalGalleryEnableWatermarking = Constants.getBoolean("galleryEnableWatermarking");

        // Keep this focused test independent of the database and external ImageMagick.
        Constants.setString("thumbServletCacheDir", tempDir.toString());
        Constants.setString("imageMagickDir", tempDir.resolve("missing-imagemagick").toString());
        Constants.setBoolean("galleryEnableWatermarking", false);
    }

    @AfterEach
    void tearDown() {
        Constants.setString("thumbServletCacheDir", originalThumbServletCacheDir);
        Constants.setString("imageMagickDir", originalImageMagickDir);
        Constants.setBoolean("galleryEnableWatermarking", originalGalleryEnableWatermarking);
    }

    @Test
    void resizePictureUsesCropForSmallAndExactWidthForLargeImage() throws IOException {
        Path image = createSourceImage("gallery.png", 640, 455);
        Dimension[] dimensions = {
            new Dimension(200, 200),
            new Dimension(400, 400)
        };

        boolean resized = GalleryDB.resizePictureImpl(dimensions, image.toString(), null, null, "C", "W");

        assertTrue(resized);
        assertImageDimensions(tempDir.resolve("s_gallery.png"), 200, 200);
        assertImageDimensions(image, 400, 284);
    }

    @Test
    void resizePictureRegeneratesLargeImageWithSmallModeAfterRemovingOverride() throws IOException {
        Path image = createSourceImage("gallery.png", 640, 455);

        boolean resizedWithOverride = GalleryDB.resizePictureImpl(
            new Dimension[] { new Dimension(200, 200), new Dimension(400, 300) },
            image.toString(),
            null,
            null,
            "C",
            "W"
        );
        assertTrue(resizedWithOverride);
        assertImageDimensions(image, 400, 284);

        boolean resizedWithFallback = GalleryDB.resizePictureImpl(
            new Dimension[] { new Dimension(200, 200), new Dimension(400, 300) },
            image.toString(),
            null,
            null,
            "C",
            null
        );

        assertTrue(resizedWithFallback);
        assertImageDimensions(image, 400, 300);
    }

    @Test
    void resizePictureKeepsOriginalLargeImageForZeroDimensions() throws IOException {
        Path image = createSourceImage("gallery.png", 640, 455);
        Dimension[] dimensions = {
            new Dimension(200, 200),
            new Dimension(0, 0)
        };

        boolean resized = GalleryDB.resizePictureImpl(dimensions, image.toString(), null, null, "C", "W");

        assertTrue(resized);
        assertImageDimensions(tempDir.resolve("s_gallery.png"), 200, 200);
        assertImageDimensions(image, 640, 455);
    }

    private Path createSourceImage(String name, int width, int height) throws IOException {
        BufferedImage source = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        Path image = tempDir.resolve(name);
        assertTrue(ImageIO.write(source, "png", image.toFile()));
        return image;
    }

    private void assertImageDimensions(Path image, int expectedWidth, int expectedHeight) throws IOException {
        BufferedImage actual = ImageIO.read(image.toFile());
        assertNotNull(actual, "Generated image must be readable: " + image);
        assertEquals(expectedWidth, actual.getWidth(), "Unexpected image width: " + image);
        assertEquals(expectedHeight, actual.getHeight(), "Unexpected image height: " + image);
    }
}
