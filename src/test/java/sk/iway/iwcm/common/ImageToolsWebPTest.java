package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.luciad.imageio.webp.WebPImageWriterSpi;

import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Covers WebP encoding when multiple web applications share the JVM's ImageIO registry. */
class ImageToolsWebPTest extends BaseWebjetTest {

    @TempDir
    Path directory;

    /** Reproduces a provider registered by another deployment of the same WebP library. */
    @Test
    void writesWebPWhenRegistryPrefersAnotherWebappClassLoader() throws Exception {
        URL library = WebPImageWriterSpi.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader otherWebapp = new URLClassLoader(new URL[] { library }, getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith("com.luciad.imageio.webp.")) return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) loaded = findClass(name);
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
        }) {
            ImageWriterSpi foreign = (ImageWriterSpi) otherWebapp.loadClass(WebPImageWriterSpi.class.getName())
                .getConstructor().newInstance();
            IIORegistry registry = IIORegistry.getDefaultInstance();
            registry.registerServiceProvider(foreign, ImageWriterSpi.class);
            try {
                Iterator<ImageWriterSpi> providers = registry.getServiceProviders(ImageWriterSpi.class, false);
                while (providers.hasNext()) {
                    ImageWriterSpi provider = providers.next();
                    if (provider != foreign) registry.setOrdering(ImageWriterSpi.class, foreign, provider);
                }
                ImageWriter preferred = ImageIO.getImageWritersByFormatName("webp").next();
                try {
                    assertSame(otherWebapp, preferred.getClass().getClassLoader(), "The fixture must reproduce the foreign provider");
                } finally {
                    preferred.dispose();
                }
                assertLosslessRoundtrip("foreign-provider.webp");
            } finally {
                registry.deregisterServiceProvider(foreign, ImageWriterSpi.class);
            }
        }
    }

    /** Quality 100 must actually pass lossless parameters to the writer and preserve alpha. */
    @Test
    void quality100PreservesPixelsAndTransparency() throws Exception {
        assertLosslessRoundtrip("lossless.webp");
    }

    private void assertLosslessRoundtrip(String name) throws Exception {
        BufferedImage original = new BufferedImage(16, 12, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                original.setRGB(x, y, ((80 + x * 10) << 24) | ((x * 17) << 16) | ((y * 21) << 8) | ((x + y) * 9));
            }
        }
        Path output = directory.resolve(name);
        assertEquals(0, ImageTools.writeImage(original, name, 100, new IwcmFile(output.toFile())));
        BufferedImage decoded = ImageIO.read(output.toFile());
        assertNotNull(decoded);
        assertEquals(original.getWidth(), decoded.getWidth());
        assertEquals(original.getHeight(), decoded.getHeight());
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                assertEquals(original.getRGB(x, y), decoded.getRGB(x, y), "Lossless pixel at " + x + "," + y);
            }
        }
    }
}
