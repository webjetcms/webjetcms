package sk.iway.iwcm.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.gallery.ImageInfo;

/**
 * Provides AVIF-specific ImageMagick parameters and output validation for {@link ImageTools}.
 *
 * <p>Produces a static, 8-bit sRGB image from the first source frame and verifies
 * the staged output as AVIF before replacing a file in local or database storage.
 */
public final class AvifTools {
    private AvifTools() {
    }

    public static boolean isAvif(String name) {
        return "avif".equalsIgnoreCase(FileTools.getFileExtension(name));
    }

    /**
     * Selects the first source frame and prepares static, 8-bit sRGB AVIF output.
     * Call after injecting custom parameters, while the input and output are still plain paths.
     * @param args command with the executable first, input second and output last
     */
    static void addImageMagickParams(List<String> args) {
        String input = args.get(1);
        String decoder = isAvif(input) ? "avif:" : "";
        args.set(1, decoder + input + "[0]");
        args.add(2, "-auto-orient");
        args.set(args.size() - 1, "avif:" + args.get(args.size() - 1));
        args.addAll(args.size() - 1, List.of("+repage", "-colorspace", "sRGB", "-depth", "8"));
    }

    /**
     * Checks the staged output before the shared processing flow replaces the destination.
     * @param output local file produced by ImageMagick
     * @return true if the file contains a recognizable AVIF image
     * @throws IOException if the output cannot be read
     */
    static boolean isValidOutput(Path output) throws IOException {
        try (InputStream stream = Files.newInputStream(output)) {
            ImageInfo info = new ImageInfo();
            info.setInput(stream);
            return info.check() && info.getFormat() == ImageInfo.FORMAT_AVIF;
        }
    }

}
