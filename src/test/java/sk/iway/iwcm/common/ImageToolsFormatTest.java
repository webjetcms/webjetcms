package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.Locale;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Verifies image recognition and processing capabilities shared with upload defaults. */
class ImageToolsFormatTest {

    /** Existing image formats retain their resizing and raster transparency behavior. */
    @ParameterizedTest
    @CsvSource({
        "jpg, false, true", "jpeg, false, true", "gif, true, true", "png, true, true",
        "svg, false, false", "webp, true, true", "bmp, false, true", "avif, true, true"
    })
    void recognizesFormatsRegardlessOfCase(String extension, boolean transparency, boolean resizable) {
        assertTrue(ImageTools.getSupportedImageExtensions().contains(extension));
        for (String suffix : new String[] { extension, extension.toUpperCase(Locale.ROOT) }) {
            String fileName = "/images/photo." + suffix;
            assertTrue(ImageTools.isImage(fileName));
            assertEquals(transparency, ImageTools.supportsTransparency(fileName));
            assertEquals(resizable, ImageTools.isResizableImage(fileName));
            assertEquals(transparency ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB,
                ImageTools.getBufferedImageType(fileName));
        }
    }

    /** Attachments, missing extensions and disguised filenames are not processable images. */
    @ParameterizedTest
    @ValueSource(strings = {"", "photo", "photo.", "photo.jpg.exe", "photo.tiff", "photo.psd", "document.pdf"})
    void rejectsUnsupportedImages(String fileName) {
        assertFalse(ImageTools.isImage(fileName));
        assertFalse(ImageTools.supportsTransparency(fileName));
        assertFalse(ImageTools.isResizableImage(fileName));
        assertEquals(BufferedImage.TYPE_INT_RGB, ImageTools.getBufferedImageType(fileName));
    }
}
