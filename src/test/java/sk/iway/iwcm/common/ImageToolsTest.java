package sk.iway.iwcm.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.test.BaseWebjetTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageToolsTest extends BaseWebjetTest {

    @BeforeEach
    void setUp() {
        Constants.setString("imageMagickCustomParams", "");
        Constants.setString("imageMagickCustomParams_resize", "");
        Constants.setString("imageMagickCustomParams_crop", "");
        Constants.setString("imageMagickCustomParams_rotate", "");
        Constants.setString("imageMagickCustomParams_jpg", "");
        Constants.setString("imageMagickCustomParams_png", "");
        Constants.setString("imageMagickCustomParams_webp", "");
        Constants.setString("imageMagickCustomParams_resize_jpg", "");
        Constants.setString("imageMagickCustomParams_resize_png", "");
    }

    private List<String> createResizeArgs(String inputFile, String outputFile) {
        List<String> args = new ArrayList<>();
        args.add("/usr/bin/magick");
        args.add(inputFile);
        args.add("-strip");
        args.add("-resize");
        args.add("640x427!");
        args.add(outputFile);
        return args;
    }

    @Test
    void testSingleLineParamsBackwardCompatible() {
        // Single line value - all params go before operation (after input file, at position 2)
        Constants.setString("imageMagickCustomParams", "-filter Lanczos -interlace Plane");

        List<String> args = createResizeArgs("input.jpg", "output.jpg");
        ImageTools.addImageMagickCustomParams(args);

        // Expected: magick input.jpg -filter Lanczos -interlace Plane -strip -resize 640x427! output.jpg
        assertEquals("/usr/bin/magick", args.get(0));
        assertEquals("input.jpg", args.get(1));
        assertEquals("-filter", args.get(2));
        assertEquals("Lanczos", args.get(3));
        assertEquals("-interlace", args.get(4));
        assertEquals("Plane", args.get(5));
        assertEquals("-strip", args.get(6));
        assertEquals("-resize", args.get(7));
        assertEquals("640x427!", args.get(8));
        assertEquals("output.jpg", args.get(9));
    }

    @Test
    void testTwoLineParams() {
        // Two lines: line 1 = before operation, line 2 = after operation
        Constants.setString("imageMagickCustomParams", "-filter Lanczos\n-interlace Plane -sampling-factor 4:2:0");

        List<String> args = createResizeArgs("input.jpg", "output.jpg");
        ImageTools.addImageMagickCustomParams(args);

        // Expected: magick input.jpg -filter Lanczos -strip -resize 640x427! -interlace Plane -sampling-factor 4:2:0 output.jpg
        assertEquals("/usr/bin/magick", args.get(0));
        assertEquals("input.jpg", args.get(1));
        assertEquals("-filter", args.get(2));
        assertEquals("Lanczos", args.get(3));
        assertEquals("-strip", args.get(4));
        assertEquals("-resize", args.get(5));
        assertEquals("640x427!", args.get(6));
        assertEquals("-interlace", args.get(7));
        assertEquals("Plane", args.get(8));
        assertEquals("-sampling-factor", args.get(9));
        assertEquals("4:2:0", args.get(10));
        assertEquals("output.jpg", args.get(11));
    }

    @Test
    void testEmptyFirstLineOnlyOutputParams() {
        // Empty first line = only output params (after operation)
        Constants.setString("imageMagickCustomParams_png", "\n-define png:compression-level=9");

        List<String> args = createResizeArgs("input.png", "output.png");
        ImageTools.addImageMagickCustomParams(args);

        // Expected: magick input.png -strip -resize 640x427! -define png:compression-level=9 output.png
        assertEquals("/usr/bin/magick", args.get(0));
        assertEquals("input.png", args.get(1));
        assertEquals("-strip", args.get(2));
        assertEquals("-resize", args.get(3));
        assertEquals("640x427!", args.get(4));
        assertEquals("-define", args.get(5));
        assertEquals("png:compression-level=9", args.get(6));
        assertEquals("output.png", args.get(7));
    }

    @Test
    void testCombineBaseAndExtParams() {
        // Base params with 2 lines + ext params with 2 lines
        Constants.setString("imageMagickCustomParams", "-filter Lanczos\n-interlace Plane");
        Constants.setString("imageMagickCustomParams_jpg", "\n-define jpeg:optimize-coding=true");

        List<String> args = createResizeArgs("input.jpg", "output.jpg");
        ImageTools.addImageMagickCustomParams(args);

        // Combined: input="-filter Lanczos", output="-interlace Plane -define jpeg:optimize-coding=true"
        // Expected: magick input.jpg -filter Lanczos -strip -resize 640x427! -interlace Plane -define jpeg:optimize-coding=true output.jpg
        assertEquals("/usr/bin/magick", args.get(0));
        assertEquals("input.jpg", args.get(1));
        assertEquals("-filter", args.get(2));
        assertEquals("Lanczos", args.get(3));
        assertEquals("-strip", args.get(4));
        assertEquals("-resize", args.get(5));
        assertEquals("640x427!", args.get(6));
        assertEquals("-interlace", args.get(7));
        assertEquals("Plane", args.get(8));
        assertEquals("-define", args.get(9));
        assertEquals("jpeg:optimize-coding=true", args.get(10));
        assertEquals("output.jpg", args.get(11));
    }

    @Test
    void testNoCustomParams() {
        List<String> args = createResizeArgs("input.jpg", "output.jpg");
        List<String> originalArgs = new ArrayList<>(args);
        ImageTools.addImageMagickCustomParams(args);

        assertEquals(originalArgs, args);
    }

    @Test
    void testQualityParamRemoval() {
        // When custom params contain quality, existing -quality should be removed
        Constants.setString("imageMagickCustomParams_webp", "\n-quality 80 -define webp:method=6");

        List<String> args = new ArrayList<>();
        args.add("/usr/bin/magick");
        args.add("input.webp");
        args.add("-quality");
        args.add("85");
        args.add("-strip");
        args.add("-resize");
        args.add("640x427!");
        args.add("output.webp");

        ImageTools.addImageMagickCustomParams(args);

        // -quality 85 should be removed, custom -quality 80 should be in output position
        assertFalse(args.contains("85"), "Original quality value should be removed");
        assertTrue(args.indexOf("-quality") > args.indexOf("-resize"), "Custom -quality should be after resize");
        assertTrue(args.indexOf("output.webp") == args.size() - 1, "Output file should be last");
    }

    @Test
    void testSpecificOperationFormatParams() {
        // Most specific key: imageMagickCustomParams_resize_jpg
        Constants.setString("imageMagickCustomParams_resize_jpg", "-filter Mitchell\n-define jpeg:optimize-coding=true");
        Constants.setString("imageMagickCustomParams", "-filter Lanczos\n-interlace Plane");
        Constants.setString("imageMagickCustomParams_jpg", "\n-define jpeg:fancy-upsampling=false");

        List<String> args = createResizeArgs("input.jpg", "output.jpg");
        ImageTools.addImageMagickCustomParams(args);

        // Should use only imageMagickCustomParams_resize_jpg (most specific)
        assertEquals("-filter", args.get(2));
        assertEquals("Mitchell", args.get(3));
        // Output params
        int outputIdx = args.indexOf("-define");
        assertTrue(outputIdx > args.indexOf("-resize"), "-define should be after resize");
        assertEquals("jpeg:optimize-coding=true", args.get(outputIdx + 1));
        // Should NOT contain -interlace Plane or Lanczos from base params
        assertFalse(args.contains("Lanczos"));
        assertFalse(args.contains("Plane"));
    }

    @Test
    void testNoOperationFound() {
        // Args without resize/crop/rotate - no params should be injected
        Constants.setString("imageMagickCustomParams", "-filter Lanczos");

        List<String> args = new ArrayList<>(Arrays.asList("/usr/bin/magick", "input.jpg", "-strip", "output.jpg"));
        List<String> originalArgs = new ArrayList<>(args);
        ImageTools.addImageMagickCustomParams(args);

        assertEquals(originalArgs, args);
    }

    @Test
    void testCombineCustomParamsHelper() {
        // Both single line
        assertEquals("-filter Lanczos -interlace Plane", ImageTools.combineCustomParams("-filter Lanczos", "-interlace Plane"));

        // Both two lines
        assertEquals("-a -b\n-c -d", ImageTools.combineCustomParams("-a\n-c", "-b\n-d"));

        // Base two lines + ext one line (output only via \n prefix)
        assertEquals("-filter Lanczos\n-interlace Plane -define jpeg:optimize-coding=true",
            ImageTools.combineCustomParams("-filter Lanczos\n-interlace Plane", "\n-define jpeg:optimize-coding=true"));

        // Base empty
        assertEquals("\n-define png:compression-level=9", ImageTools.combineCustomParams("", "\n-define png:compression-level=9"));

        // Base null
        assertEquals("-filter Lanczos", ImageTools.combineCustomParams(null, "-filter Lanczos"));
    }

    @Test
    void testCropOperation() {
        Constants.setString("imageMagickCustomParams", "-filter Lanczos\n-interlace Plane");

        List<String> args = new ArrayList<>();
        args.add("/usr/bin/magick");
        args.add("input.jpg");
        args.add("-crop");
        args.add("100x100+10+20");
        args.add("output.jpg");

        ImageTools.addImageMagickCustomParams(args);

        // Input params at position 2
        assertEquals("-filter", args.get(2));
        assertEquals("Lanczos", args.get(3));
        // Operation
        assertEquals("-crop", args.get(4));
        assertEquals("100x100+10+20", args.get(5));
        // Output params before output file
        assertEquals("-interlace", args.get(6));
        assertEquals("Plane", args.get(7));
        assertEquals("output.jpg", args.get(8));
    }
}
