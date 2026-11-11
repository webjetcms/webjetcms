package sk.iway.iwcm.gallery;

import java.awt.Dimension;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads AVIF display dimensions from ISO Base Media File Format (ISO BMFF) metadata.
 *
 * <p>The container consists of boxes with a byte size, a four-character type and a
 * payload. Integer fields are read in big-endian order. The {@code ftyp} box identifies
 * AVIF through an {@code avif} or {@code avis} brand. Dimensions are then obtained from
 * either the primary image item in {@code meta} or a picture/video track in {@code moov}:
 * <pre>
 * ftyp                         major and compatible brands
 * meta
 *   pitm                       primary image item ID
 *   iprp
 *     ipco                     ordered image property boxes
 *       ispe / clap / irot     dimensions / clean aperture / rotation
 *     ipma                     item IDs mapped to property indexes
 * moov
 *   trak
 *     tkhd                     track dimensions and transformation matrix
 *     mdia / hdlr              track handler type
 * </pre>
 *
 * <p>Only the resulting display width and height are returned. For a primary item,
 * these include the supported clean-aperture crop and quarter-turn rotation; for a
 * sequence, they come from the first qualifying track header. {@code ImageInfo} uses
 * a successful result to expose the AVIF format and these dimensions.
 *
 * <p>This parser does not decode AV1 pixel data or verify that the compressed image
 * can be decoded. It does not extract alpha presence, bit depth, color profiles,
 * animation frame counts or timing. Metadata payloads are limited to 4 MiB each,
 * and unrelated top-level payloads, including media data, are skipped.
 */
final class AvifImageInfo {
    private static final int MAX_METADATA_SIZE = 4 * 1024 * 1024;

    private AvifImageInfo() {
    }

    /**
     * Scans an AVIF stream for the first supported metadata box yielding display dimensions.
     *
     * <p>At most 1024 top-level boxes are examined. The first must be {@code ftyp};
     * its major brand and compatible brands are checked for {@code avif} or {@code avis},
     * excluding the minor-version field. After identification, {@code meta} is passed to
     * {@link #readPrimaryItem(ByteBuffer)} and {@code moov} to
     * {@link #readSequence(ByteBuffer)} in file order. A successful result ends the scan,
     * so neither source of dimensions takes precedence over an earlier successful one.
     *
     * <p>Normal boxes have an 8-byte header; size {@code 1} selects a 16-byte header
     * containing an extended size. A size of {@code 0} is supported here only for a
     * {@code meta} box after AVIF identification: its payload extends to end of stream
     * and is read with a one-byte probe beyond the 4 MiB limit. Explicitly sized
     * {@code ftyp}, {@code meta} and {@code moov} payloads must also fit that limit.
     * Other payloads are skipped without allocating a buffer for their contents.
     *
     * @param input stream positioned at the first box header; consumed but not closed
     * @return positive display dimensions, or {@code null} if identification fails,
     *         a size or parsing limit is rejected, or no supported dimensions are found
     * @throws IOException if reading or skipping fails, required bytes are missing,
     *                     or a nested box has an invalid size
     */
    static Dimension read(InputStream input) throws IOException {
        DataInputStream stream = new DataInputStream(input);
        boolean avif = false;
        for (int count = 0; count < 1024; count++) {
            long size = Integer.toUnsignedLong(stream.readInt());
            String type = new String(stream.readNBytes(4), StandardCharsets.US_ASCII);
            int header = 8;
            if (size == 1) {
                size = stream.readLong();
                header = 16;
            }
            if (size == 0 && "meta".equals(type) && avif) {
                byte[] data = stream.readNBytes(MAX_METADATA_SIZE + 1);
                return data.length > MAX_METADATA_SIZE ? null : readPrimaryItem(ByteBuffer.wrap(data));
            }
            if (size < header) return null;
            long remaining = size - header;
            if (count == 0 && !"ftyp".equals(type)) return null;
            if ("ftyp".equals(type) || "meta".equals(type) || "moov".equals(type)) {
                if (remaining > MAX_METADATA_SIZE) return null;
                byte[] data = new byte[(int) remaining];
                stream.readFully(data);
                ByteBuffer box = ByteBuffer.wrap(data);
                if ("ftyp".equals(type)) {
                    if (box.remaining() < 8 || box.remaining() % 4 != 0) return null;
                    for (int i = 0; i < data.length; i += 4) {
                        if (i == 4) continue; // The minor version is not a brand.
                        int brand = box.getInt(i);
                        avif |= brand == 0x61766966 || brand == 0x61766973; // avif / avis
                    }
                    if (!avif) return null;
                } else if (avif) {
                    Dimension dimensions = "meta".equals(type) ? readPrimaryItem(box) : readSequence(box);
                    if (dimensions != null) return dimensions;
                }
            } else {
                stream.skipNBytes(remaining);
            }
        }
        return null;
    }

    /**
     * Extracts sequence display dimensions from the first usable picture or video track.
     *
     * <p>For each {@code trak}, the parser pairs its {@code tkhd} track header with
     * the handler in {@code mdia/hdlr}. The handler type at payload offset 8 must be
     * {@code pict} or {@code vide}; tracks with other handlers are ignored. Sample
     * descriptions, frame data and timing tables are not inspected.
     *
     * <p>Version 0 of {@code tkhd} stores width and height at payload offsets 76 and
     * 80 as unsigned 16.16 fixed-point values. Version 1 widens the creation time,
     * modification time and duration fields, shifting these offsets by 12 bytes.
     * Shifting each value right by 16 discards its fractional part; tracks with a
     * zero integer dimension are skipped.
     *
     * <p>The transformation matrix is checked for zero diagonal entries and nonzero
     * off-diagonal entries in its upper-left 2-by-2 portion. This detects the axis
     * exchange used for 90- and 270-degree rotations, so width and height are swapped.
     * Other matrix effects, including scaling and translation, are not applied.
     *
     * @param movie big-endian {@code moov} payload positioned at its first child box;
     *              its position is advanced while parsing
     * @return display dimensions of the first usable track, or {@code null} if none
     *         is found or a candidate track has an unsupported header version
     * @throws IOException if a required field is truncated or a child box has an invalid size
     */
    private static Dimension readSequence(ByteBuffer movie) throws IOException {
        for (Box track : boxes(movie)) {
            if (!"trak".equals(track.type)) continue;
            ByteBuffer header = null;
            boolean picture = false;
            for (Box child : boxes(track.data)) {
                if ("tkhd".equals(child.type)) header = child.data;
                if ("mdia".equals(child.type)) {
                    for (Box media : boxes(child.data)) {
                        if ("hdlr".equals(media.type)) {
                            require(media.data, 12);
                            int handler = media.data.getInt(8);
                            picture = handler == 0x70696374 || handler == 0x76696465; // pict / vide; ignore auxiliary tracks
                        }
                    }
                }
            }
            if (header == null || !picture) continue;
            require(header, 4);
            int version = header.getInt(0) >>> 24;
            if (version > 1) return null;
            int offset = version == 1 ? 12 : 0;
            require(header, 84 + offset);
            int width = header.getInt(76 + offset) >>> 16;
            int height = header.getInt(80 + offset) >>> 16;
            if (width == 0 || height == 0) continue;
            boolean rotated = header.getInt(40 + offset) == 0 && header.getInt(56 + offset) == 0
                && header.getInt(44 + offset) != 0 && header.getInt(52 + offset) != 0;
            return rotated ? new Dimension(height, width) : new Dimension(width, height);
        }
        return null;
    }

    /**
     * Resolves the primary image item's properties and computes its display dimensions.
     *
     * <p>After consuming the {@code meta} FullBox version and flags, the parser follows
     * item-to-property associations instead of using the first dimensions it encounters.
     * This keeps thumbnail or auxiliary-image properties from determining the result.
     * The resolution proceeds as follows:
     * <ol>
     *   <li>Read the primary item ID from {@code pitm}: version 0 uses an unsigned
     *       16-bit ID and version 1 an unsigned 32-bit ID.</li>
     *   <li>Collect the property boxes from {@code iprp/ipco} in their stored order
     *       and retain the {@code iprp/ipma} association tables. Collection precedes
     *       resolution so the associations can refer to properties encountered later.</li>
     *   <li>Walk each {@code ipma} table. Its version selects 16- or 32-bit item IDs,
     *       and the lowest flag bit selects 8- or 16-bit association fields. Mask off
     *       the high essential-property bit to obtain a 7- or 15-bit property index.
     *       Index zero means no property; positive indexes are one-based references
     *       into the collected list. Reject indexes beyond that list, and select only
     *       associations belonging to the primary item. The essential bit itself is
     *       not used to reject unrecognized properties.</li>
     *   <li>Read the base width and height from {@code ispe}, after its FullBox version
     *       and flags. Both must fit a positive Java {@code int}.</li>
     *   <li>If {@code clap} is associated, use its clean-aperture width and height.
     *       Each is an unsigned numerator/denominator pair, rounded to the nearest
     *       integer with half values rounded up using
     *       {@code (numerator + denominator / 2) / denominator}. Denominators must be
     *       nonzero, and the rounded dimensions must be positive and no larger than
     *       the {@code ispe} dimensions. The horizontal and vertical aperture offsets
     *       are not used or validated because only the resulting size is returned.</li>
     *   <li>Use the lowest bit of {@code irot} to detect an odd number of quarter turns.
     *       Swap width and height after applying the aperture for 90- or 270-degree
     *       rotation; 0- and 180-degree rotation leave the dimensions unchanged.</li>
     * </ol>
     *
     * <p>Only {@code ispe}, {@code clap} and {@code irot} affect the result. The method
     * does not follow image-data locations, decode grid tiles or inspect AV1 payloads.
     *
     * @param metadata big-endian {@code meta} payload positioned at its FullBox version
     *                 and flags; its position is advanced while parsing
     * @return primary-item display dimensions, or {@code null} if the primary item or
     *         valid dimensions are missing, a supported version check fails, an association
     *         index is out of range, or the clean-aperture dimensions are invalid
     * @throws IOException if a required field is truncated or a child box has an invalid size
     */
    private static Dimension readPrimaryItem(ByteBuffer metadata) throws IOException {
        require(metadata, 4);
        metadata.getInt(); // FullBox version and flags.
        long primary = -1;
        List<Box> properties = new ArrayList<>();
        List<ByteBuffer> associations = new ArrayList<>();
        for (Box child : boxes(metadata)) {
            if ("pitm".equals(child.type)) {
                require(child.data, 6);
                int version = child.data.getInt() >>> 24;
                if (version > 1) return null;
                primary = version == 0 ? Short.toUnsignedInt(child.data.getShort()) : unsignedInt(child.data);
            } else if ("iprp".equals(child.type)) {
                for (Box propertyBox : boxes(child.data)) {
                    if ("ipco".equals(propertyBox.type)) properties.addAll(boxes(propertyBox.data));
                    if ("ipma".equals(propertyBox.type)) associations.add(propertyBox.data);
                }
            }
        }
        if (primary < 0) return null;
        List<Box> selected = new ArrayList<>();
        for (ByteBuffer association : associations) {
            require(association, 8);
            int flags = association.getInt();
            int version = flags >>> 24;
            if (version > 1) return null;
            long entries = unsignedInt(association);
            for (long entry = 0; entry < entries; entry++) {
                require(association, version == 0 ? 3 : 5);
                long item = version == 0 ? Short.toUnsignedInt(association.getShort()) : unsignedInt(association);
                int count = Byte.toUnsignedInt(association.get());
                for (int i = 0; i < count; i++) {
                    boolean wide = (flags & 1) != 0;
                    require(association, wide ? 2 : 1);
                    int index = wide ? association.getShort() & 0x7fff : association.get() & 0x7f;
                    if (index > properties.size()) return null;
                    if (item == primary && index > 0) selected.add(properties.get(index - 1));
                }
            }
        }
        int width = 0;
        int height = 0;
        boolean rotate = false;
        Dimension aperture = null;
        for (Box property : selected) {
            ByteBuffer data = property.data.duplicate();
            if ("ispe".equals(property.type)) {
                require(data, 12);
                data.getInt();
                width = data.getInt();
                height = data.getInt();
            } else if ("irot".equals(property.type)) {
                require(data, 1);
                rotate = (data.get() & 1) != 0;
            } else if ("clap".equals(property.type)) {
                require(data, 32);
                long widthNumerator = unsignedInt(data);
                long widthDenominator = unsignedInt(data);
                long heightNumerator = unsignedInt(data);
                long heightDenominator = unsignedInt(data);
                if (widthDenominator == 0 || heightDenominator == 0) return null;
                long croppedWidth = (widthNumerator + widthDenominator / 2) / widthDenominator;
                long croppedHeight = (heightNumerator + heightDenominator / 2) / heightDenominator;
                if (croppedWidth > Integer.MAX_VALUE || croppedHeight > Integer.MAX_VALUE) return null;
                aperture = new Dimension((int) croppedWidth, (int) croppedHeight);
            }
        }
        if (width <= 0 || height <= 0) return null;
        if (aperture != null) {
            if (aperture.width <= 0 || aperture.height <= 0 || aperture.width > width || aperture.height > height) return null;
            width = aperture.width;
            height = aperture.height;
        }
        return rotate ? new Dimension(height, width) : new Dimension(width, height);
    }

    /**
     * Splits a container payload into bounded views of its immediate child boxes.
     *
     * <p>Each header contains an unsigned 32-bit size and a four-byte ASCII type.
     * The size includes the header: ordinary headers occupy 8 bytes, while size
     * {@code 1} adds an extended 64-bit size for a 16-byte header. Size {@code 0}
     * consumes the rest of the parent, so that box becomes its final child.
     *
     * <p>The payload length is checked against the bytes remaining in the parent
     * before creating a slice. Each slice shares the backing bytes but has its own
     * position and limit, preventing reads into a sibling box without copying payloads.
     * Only one nesting level is split; callers choose which container types to descend
     * into. FullBox version and flags, when present, remain in the returned payload.
     *
     * @param parent big-endian buffer positioned at the first child header; consumed to its limit
     * @return child boxes in file order, with each payload positioned at zero
     * @throws IOException if a header is truncated, a declared size is smaller than
     *                     its header, or a payload extends beyond the parent buffer
     */
    private static List<Box> boxes(ByteBuffer parent) throws IOException {
        List<Box> result = new ArrayList<>();
        while (parent.hasRemaining()) {
            require(parent, 8);
            long size = unsignedInt(parent);
            byte[] type = new byte[4];
            parent.get(type);
            int header = 8;
            if (size == 1) {
                require(parent, 8);
                size = parent.getLong();
                header = 16;
            } else if (size == 0) {
                size = (long) parent.remaining() + header;
            }
            long length = size - header;
            if (length < 0 || length > parent.remaining()) throw new IOException("Invalid AVIF box size");
            result.add(new Box(new String(type, StandardCharsets.US_ASCII), parent.slice(parent.position(), (int) length)));
            parent.position(parent.position() + (int) length);
        }
        return result;
    }

    private static long unsignedInt(ByteBuffer data) throws IOException {
        require(data, 4);
        return Integer.toUnsignedLong(data.getInt());
    }

    private static void require(ByteBuffer data, int size) throws IOException {
        if (data.remaining() < size) throw new IOException("Truncated AVIF metadata");
    }

    /**
     * Holds a parsed box type and a bounded view of its payload.
     *
     * @param type four-character ASCII box identifier
     * @param data payload without the size/type header, sharing the parent's backing bytes
     *             but maintaining an independent, mutable position and limit
     */
    private record Box(String type, ByteBuffer data) {
    }
}
