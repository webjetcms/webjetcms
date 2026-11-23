package sk.iway.iwcm.gallery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/** Verifies AVIF identification and dimensions without an installed image codec. */
class ImageInfoAvifTest {
    /** Primary-item associations select the display image, then apply its crop and rotation. */
    @Test
    void selectsPrimaryItemInsteadOfThumbnailAndAppliesDisplayTransform() throws Exception {
        byte[] properties = box("ipco", box("ispe", ints(0, 32, 32)),
            box("ispe", ints(0, 640, 480)), box("irot", new byte[] {1}),
            box("clap", ints(600, 1, 400, 1, 0, 1, 0, 1)));
        ByteArrayOutputStream associationBytes = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(associationBytes)) {
            data.writeInt(0x01000001); // Version 1: 32-bit item IDs and 16-bit property indexes.
            data.writeInt(2);
            data.writeInt(1);
            data.writeByte(1);
            data.writeShort(0x8001);
            data.writeInt(70000);
            data.writeByte(3);
            data.writeShort(0x8002);
            data.writeShort(0x8003);
            data.writeShort(0x8004);
        }
        byte[] metadata = box("meta", ints(0), box("pitm", ints(0x01000000, 70000)),
            box("iprp", properties, box("ipma", associationBytes.toByteArray())));
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.write(box("ftyp", ints(0x6d696631, 0, 0x61766966))); // avif as a compatible brand.
        file.write(metadata);
        ImageInfo info = new ImageInfo();
        info.setInput((java.io.DataInput) new DataInputStream(new ByteArrayInputStream(file.toByteArray())));
        assertTrue(info.check());
        assertEquals(400, info.getWidth());
        assertEquals(600, info.getHeight());
    }

    private static byte[] ints(int... values) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(bytes)) {
            for (int value : values) data.writeInt(value);
        }
        return bytes.toByteArray();
    }

    private static byte[] box(String type, byte[]... contents) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(bytes)) {
            int length = 8;
            for (byte[] content : contents) length += content.length;
            data.writeInt(length);
            data.writeBytes(type);
            for (byte[] content : contents) data.write(content);
        }
        return bytes.toByteArray();
    }

    /** Timed AVIF sequences store dimensions in a picture track instead of a primary item. */
    @Test
    void readsAnimatedAvifDimensions() throws Exception {
        try (InputStream input = Files.newInputStream(Path.of("src/test/webapp/tests/webpages/autotest-animated.avif"))) {
            ImageInfo info = new ImageInfo(input);
            assertEquals("AVIF", info.getFormatName());
            assertEquals(120, info.getWidth());
            assertEquals(80, info.getHeight());
        }
    }

    /** Truncated metadata and unrelated ISO BMFF files must not be accepted as AVIF images. */
    @Test
    void rejectsIncompleteHeadersAndOtherBrands() throws Exception {
        byte[] fixture = Files.readAllBytes(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"));
        for (int length : new int[] {0, 2, 7, 16, 40, 80}) {
            ImageInfo info = new ImageInfo();
            info.setInput(new java.io.ByteArrayInputStream(java.util.Arrays.copyOf(fixture, length)));
            assertFalse(info.check(), "Truncated AVIF at " + length);
        }
        byte[] other = new String(fixture, java.nio.charset.StandardCharsets.ISO_8859_1)
            .replace("avif", "heic").getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        ImageInfo info = new ImageInfo();
        info.setInput(new java.io.ByteArrayInputStream(other));
        assertFalse(info.check());
    }
    /** Reads a real AVIF containing a separate alpha image. */
    @Test
    void readsTransparentAvifDimensions() throws Exception {
        try (InputStream input = Files.newInputStream(Path.of("src/test/webapp/tests/webpages/autotest-transparent.avif"))) {
            ImageInfo info = new ImageInfo();
            info.setInput(input);
            assertTrue(info.check());
            assertEquals("AVIF", info.getFormatName());
            assertEquals("image/avif", info.getMimeType());
            assertEquals(120, info.getWidth());
            assertEquals(80, info.getHeight());
        }
    }
}
