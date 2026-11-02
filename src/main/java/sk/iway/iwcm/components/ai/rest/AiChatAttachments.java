package sk.iway.iwcm.components.ai.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.extractor.ExtractorFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.components.ai.chat.AiChatAttachment;

/** Validates bounded chat uploads and extracts document content without publishing files. */
public final class AiChatAttachments {
    public static final int MAX_FILES = 5;
    public static final int MAX_BYTES = 10 * 1024 * 1024;
    public static final int MAX_TEXT = 100_000;
    private static final Map<String, String> MEDIA_TYPES = Map.ofEntries(
        Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
        Map.entry("webp", "image/webp"), Map.entry("pdf", "application/pdf"),
        Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
    private static final List<String> TEXT_TYPES = List.of("txt", "md", "csv", "tsv", "json", "xml", "html", "htm",
        "css", "js", "java", "sql", "yaml", "yml", "log");

    private AiChatAttachments() { }

    public record Upload(AiChatAttachment attachment, byte[] bytes) { }

    public static final class UnreadableException extends IOException {
        public UnreadableException() { super("Chat attachment text is unreadable or exceeds the limit"); }
    }

    /** Checks aggregate limits before reading file contents; MIME types come from the accepted format. */
    public static List<Upload> read(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return List.of();
        if (files.size() > MAX_FILES) throw invalid("attachmentLimit");
        long total = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) throw invalid("attachmentInvalid");
            total += file.getSize();
            if (total > MAX_BYTES) throw invalid("attachmentLimit");
        }
        List<Upload> uploads = new ArrayList<>();
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            if (name == null) throw invalid("attachmentInvalid");
            name = name.replace('\\', '/');
            name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
            if (name.isBlank() || name.length() > 200) throw invalid("attachmentInvalid");
            String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            String mediaType = MEDIA_TYPES.get(extension);
            if (mediaType == null && TEXT_TYPES.contains(extension)) mediaType = "text/plain";
            if (mediaType == null) throw invalid("attachmentType");
            try (var input = file.getInputStream()) {
                byte[] bytes = input.readNBytes((int)file.getSize() + 1);
                if (bytes.length != file.getSize()) throw invalid("attachmentInvalid");
                validateSignature(mediaType, bytes);
                if ("text/plain".equals(mediaType)) text(bytes);
                uploads.add(new Upload(new AiChatAttachment(null, name, mediaType, bytes.length,
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))), bytes));
            } catch (IOException | NoSuchAlgorithmException exception) {
                throw invalid("attachmentInvalid");
            }
        }
        return List.copyOf(uploads);
    }

    /** Assigns filenames under the owner lock, continuing after saved and unfinished uploads. */
    public static List<Upload> numbered(Path directory, List<AiChatAttachment> saved, List<Upload> uploads) throws IOException {
        if (uploads.isEmpty()) return List.of();
        long last = saved.size();
        for (AiChatAttachment attachment : saved) last = Math.max(last, sequence(attachment.id()));
        if (Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            try (var files = Files.newDirectoryStream(directory)) {
                for (Path file : files) last = Math.max(last, sequence(file.getFileName().toString()));
            }
        }
        List<Upload> numbered = new ArrayList<>();
        for (Upload upload : uploads) {
            AiChatAttachment attachment = upload.attachment();
            String extension = attachment.name().substring(attachment.name().lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            last = Math.incrementExact(last);
            String id = last + "." + extension;
            numbered.add(new Upload(new AiChatAttachment(id, attachment.name(), attachment.mediaType(), attachment.size(),
                attachment.sha256()), upload.bytes()));
        }
        return List.copyOf(numbered);
    }

    /** Resolves numbered files with their original extensions and legacy UUID-based binary files. */
    public static Path path(Path directory, String threadId, String attachmentId) {
        AiChatStore.requireUuid(threadId);
        if (sequence(attachmentId) > 0) return directory.resolve(threadId).resolve(attachmentId);
        AiChatStore.requireUuid(attachmentId);
        return directory.resolve(threadId).resolve(attachmentId + ".bin");
    }

    private static long sequence(String id) {
        if (id == null || !id.matches("[1-9][0-9]*\\.[a-z]+")) return 0;
        int dot = id.indexOf('.');
        String extension = id.substring(dot + 1);
        if (!MEDIA_TYPES.containsKey(extension) && !TEXT_TYPES.contains(extension)) return 0;
        try {
            return Long.parseLong(id.substring(0, dot));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    public static String documentText(AiChatAttachment attachment, byte[] bytes) throws IOException {
        try {
            return extract(attachment, bytes);
        } catch (IOException | RuntimeException exception) {
            throw new UnreadableException();
        }
    }

    private static String extract(AiChatAttachment attachment, byte[] bytes) throws IOException {
        String value;
        if ("application/pdf".equals(attachment.mediaType())) {
            try (var document = Loader.loadPDF(bytes)) {
                if (document.getNumberOfPages() > 200) throw new IOException("Chat PDF exceeds the page limit");
                PDFTextStripper stripper = new PDFTextStripper();
                value = stripper.getText(document);
            }
        } else if ("text/plain".equals(attachment.mediaType())) {
            value = text(bytes);
        } else {
            try (var input = new ByteArrayInputStream(bytes); var extractor = ExtractorFactory.createExtractor(input)) {
                value = extractor.getText();
            } catch (RuntimeException exception) {
                throw new IOException("Unreadable chat document");
            }
        }
        if (value == null || value.isBlank()) throw new IOException("Chat document has no readable text");
        if (value.length() > MAX_TEXT) throw new IOException("Chat document text exceeds the limit");
        return value;
    }

    private static String text(byte[] bytes) throws CharacterCodingException {
        String value = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        if (value.indexOf('\0') >= 0) throw invalid("attachmentInvalid");
        return value;
    }

    private static void validateSignature(String type, byte[] bytes) {
        boolean valid = switch (type) {
            case "image/png" -> starts(bytes, new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10});
            case "image/jpeg" -> starts(bytes, new byte[] {(byte)255, (byte)216, (byte)255});
            case "image/webp" -> bytes.length >= 12 && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            case "application/pdf" -> starts(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            case "text/plain" -> true;
            default -> starts(bytes, new byte[] {80, 75, 3, 4});
        };
        if (!valid) throw invalid("attachmentInvalid");
    }

    private static boolean starts(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) if (bytes[i] != signature[i]) return false;
        return true;
    }

    private static ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
