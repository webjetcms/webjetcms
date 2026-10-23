package sk.iway.iwcm.common;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.gallery.ImageInfo;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;

/**
 * Provides AVIF encoding and transformations for {@link ImageTools} through ImageMagick.
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
     * Encodes pixels as AVIF through a temporary PNG image.
     *
     * <p>The target is replaced only after encoding and AVIF validation succeed.
     * The temporary PNG is deleted on completion, with cleanup failures logged.
     *
     * @param image source pixels to encode
     * @param target destination file in local or database storage
     * @return {@code 0} on success, or {@code 2} if writing the intermediate PNG,
     *         encoding, validating or replacing the target fails
     */
    static int write(BufferedImage image, IwcmFile target) {
        Path input = null;
        try {
            input = Files.createTempFile("webjet-avif-pixels-", ".png");
            if (!ImageIO.write(image, "png", input.toFile())) return 2;
            return transform(new IwcmFile(input.toFile()), target, List.of("from", "-resize", "100%", "to"));
        } catch (IOException ex) {
            Logger.error(ex);
            return 2;
        } finally {
            if (input != null) {
                try {
                    Files.deleteIfExists(input);
                } catch (IOException ex) {
                    Logger.error(ex);
                }
            }
        }
    }

    /**
     * Applies ImageMagick operations to the first source frame and writes validated AVIF output.
     *
     * <p>Copies the source to a temporary directory, applies automatic orientation and
     * configured AVIF parameters, and produces 8-bit sRGB output with the crop canvas reset.
     * The destination is replaced only after the output is recognized as AVIF.
     * Temporary files are removed on completion, with cleanup failures logged.
     *
     * @param from source AVIF file, or a PNG intermediate when the filename is not AVIF
     * @param to destination file in local or database storage; may be the source file
     * @param operations ImageMagick arguments without the executable, containing a
     *                   {@code from} placeholder and ending with a {@code to} placeholder;
     *                   the supplied list is not modified
     * @return {@code 0} on success, or {@code 2} if processing, output validation or
     *         destination replacement fails
     */
    static int transform(IwcmFile from, IwcmFile to, List<String> operations) {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("webjet-avif-");
            // Select the decoder for either an AVIF source or the PNG intermediate used for BufferedImage encoding.
            String inputFormat = isAvif(from.getName()) ? "avif" : "png";
            Path input = directory.resolve("input." + inputFormat);
            Path output = directory.resolve("output.avif");
            try (InputStream stream = new IwcmInputStream(from)) {
                Files.copy(stream, input);
            }
            List<String> args = new ArrayList<>(operations);
            args.set(args.indexOf("from"), input.toString());
            args.set(args.indexOf("to"), output.toString());
            args.add(0, ImageTools.getImageMagickCommand());
            ImageTools.addImageMagickCustomParams(args, "avif");
            int inputIndex = args.indexOf(input.toString());
            args.set(inputIndex, inputFormat + ":" + input + "[0]");
            args.add(inputIndex + 1, "-auto-orient");
            args.set(args.size() - 1, "avif:" + output);
            // Output is a static, ordinary SDR image. Keep alpha while resetting crop canvas metadata.
            args.addAll(args.size() - 1, List.of("+repage", "-colorspace", "sRGB", "-depth", "8"));
            run(args, directory);
            try (InputStream stream = Files.newInputStream(output)) {
                ImageInfo info = new ImageInfo();
                info.setInput(stream);
                if (!info.check() || info.getFormat() != ImageInfo.FORMAT_AVIF) return 2;
            }
            return FileTools.copyFile(new IwcmFile(output.toFile()), to) ? 0 : 2;
        } catch (IOException | IllegalArgumentException ex) {
            Logger.error(ex);
            return 2;
        } finally {
            cleanup(directory);
        }
    }

    /**
     * Runs ImageMagick with a 60-second timeout and captures combined output in a log file.
     *
     * <p>A nonzero exit code is reported with up to 4096 bytes of log output.
     * Interruption restores the thread's interrupt status, and the process is forcibly
     * terminated when waiting finishes or fails.
     *
     * @param command executable and arguments to pass to the process
     * @param directory existing directory in which to create {@code imagemagick.log}
     * @throws IOException if starting the process or reading its log fails, the timeout
     *                     expires, waiting is interrupted or the process exits unsuccessfully
     */
    private static void run(List<String> command, Path directory) throws IOException {
        Path log = directory.resolve("imagemagick.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            if (!process.waitFor(60, TimeUnit.SECONDS)) throw new IOException("ImageMagick timed out");
            if (process.exitValue() != 0) {
                try (InputStream stream = Files.newInputStream(log)) {
                    throw new IOException(new String(stream.readNBytes(4096), StandardCharsets.UTF_8));
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("ImageMagick interrupted", ex);
        } finally {
            process.destroyForcibly();
        }
    }

    /**
     * Attempts to delete temporary files and their containing directory, logging I/O failures.
     *
     * @param directory temporary directory containing only files, or {@code null} if none was created
     */
    private static void cleanup(Path directory) {
        if (directory == null) return;
        try {
            List<Path> files;
            try (var children = Files.list(directory)) {
                files = children.toList();
            }
            for (Path child : files) Files.deleteIfExists(child);
            Files.deleteIfExists(directory);
        } catch (IOException ex) {
            Logger.error(ex);
        }
    }

}
