package sk.iway.iwcm.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import sk.iway.iwcm.gallery.ImageInfo;

/** Checks generated AVIF dimensions and sequence timing independently of the native decoder. */
public final class AvifAssertions {
    private AvifAssertions() {
    }

    /** Verifies the picture track's frame durations in milliseconds, excluding auxiliary alpha tracks. */
    public static void assertAnimation(Path image, int width, int height, int... durations) throws Exception {
        try (var input = Files.newInputStream(image)) {
            ImageInfo info = new ImageInfo(input);
            assertEquals("AVIF", info.getFormatName());
            assertEquals(width, info.getWidth());
            assertEquals(height, info.getHeight());
        }
        ByteBuffer movie = box(ByteBuffer.wrap(Files.readAllBytes(image)), "moov");
        while (movie.hasRemaining()) {
            ByteBuffer track = box(movie, "trak");
            ByteBuffer media = box(track, "mdia");
            if (box(media.duplicate(), "hdlr").getInt(8) != 0x70696374) continue; // pict
            ByteBuffer header = box(media.duplicate(), "mdhd");
            long timescale = Integer.toUnsignedLong(header.getInt(header.get(0) == 1 ? 20 : 12));
            assertTrue(timescale > 0);
            ByteBuffer timing = box(box(box(media, "minf"), "stbl"), "stts");
            timing.getInt(); // Version and flags.
            int entries = timing.getInt();
            int frame = 0;
            for (int entry = 0; entry < entries; entry++) {
                int count = timing.getInt();
                long ticks = Integer.toUnsignedLong(timing.getInt());
                assertTrue(count > 0 && count <= durations.length - frame, "Unexpected frame count");
                for (int i = 0; i < count; i++) {
                    assertEquals(durations[frame++] * timescale, ticks * 1000, "Frame duration");
                }
            }
            assertEquals(durations.length, frame, "Animation frame count");
            return;
        }
        fail("Missing AVIF picture track");
    }

    private static ByteBuffer box(ByteBuffer parent, String type) {
        while (parent.remaining() >= 8) {
            long size = Integer.toUnsignedLong(parent.getInt());
            byte[] name = new byte[4];
            parent.get(name);
            int header = 8;
            if (size == 1) {
                size = parent.getLong();
                header = 16;
            }
            if (size == 0) size = parent.remaining() + header;
            assertTrue(size >= header && size - header <= parent.remaining(), "Invalid BMFF box size");
            ByteBuffer content = parent.slice();
            content.limit((int) size - header);
            parent.position(parent.position() + content.limit());
            if (type.equals(new String(name, StandardCharsets.US_ASCII))) return content;
        }
        return fail("Missing BMFF box: " + type);
    }
}
