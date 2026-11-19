package sk.iway.iwcm.components.ai.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.components.ai.dto.AiChatRequest;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;

/** Covers upload validation, text extraction and private attachment lifecycle. */
class AiChatAttachmentsTest {
    @TempDir Path root;
    private final AiChatStore store = new AiChatStore();

    private static List<AiChatAttachments.Upload> upload(String name, byte[] bytes) {
        return AiChatAttachments.read(List.of(new MockMultipartFile("files", name, "application/octet-stream", bytes)));
    }

    @Test
    void validatesTypeSignatureAndLimits() {
        Map.of("script.jsp", new byte[] {65}, "image.png", new byte[] {65}, "empty.txt", new byte[0],
            "large.txt", new byte[AiChatAttachments.MAX_BYTES + 1], "binary.txt", new byte[] {65, 0, 66})
            .forEach((name, bytes) -> assertThrows(ResponseStatusException.class, () -> upload(name, bytes)));
        var file = new MockMultipartFile("files", "notes.txt", "text/plain", new byte[] {65});
        assertThrows(ResponseStatusException.class, () -> AiChatAttachments.read(Collections.nCopies(6, file)));
        assertEquals("notes.txt", upload("../../notes.txt", new byte[] {65}).get(0).attachment().name());
    }

    @Test
    void extractsPlainTextAndOfficeDocuments() throws Exception {
        var text = upload("notes.md", "Príloha 🌍".getBytes(UTF_8)).get(0);
        assertEquals("Príloha 🌍", AiChatAttachments.documentText(text.attachment(), text.bytes()));
        try (var document = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Attachment content");
            document.write(output);
            var file = upload("attachment.docx", output.toByteArray()).get(0);
            assertTrue(AiChatAttachments.documentText(file.attachment(), file.bytes()).contains("Attachment content"));
        }
    }

    /** Only numbered attachment IDs with supported extensions resolve inside a thread directory. */
    @Test
    void rejectsInvalidAndDevelopmentAttachmentPaths() {
        String threadId = UUID.randomUUID().toString();
        for (String id : List.of("../1.txt", "1/2.txt", "0.txt", "1.jsp", UUID.randomUUID().toString())) {
            assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> AiChatAttachments.path(root, threadId, id)).getStatusCode());
        }
        assertEquals(root.resolve(threadId).resolve("2.png"), AiChatAttachments.path(root, threadId, "2.png"));
    }

    @Test
    void extractsPdfText() throws Exception {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var page = new PDPage();
            document.addPage(page);
            try (var content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.showText("Attachment content");
                content.endText();
            }
            document.save(output);
            var file = upload("attachment.pdf", output.toByteArray()).get(0);
            assertTrue(AiChatAttachments.documentText(file.attachment(), file.bytes()).contains("Attachment content"));
        }
    }

    @Test
    void persistsAttachmentsAcrossRetriesAndDeletesOnlyTheirThread() throws Exception {
        root = root.toRealPath();
        var scope = new AiChatStore.Scope(42, 7, "example.test", root);
        var assistant = new AssistantDefinitionEntity();
        assistant.setId(12L);
        var input = new AiChatRequest(null, 12L, UUID.randomUUID().toString(), null, "Read this", null);
        var uploads = upload("obrázok.png", new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10});
        var turn = store.begin(input, scope, assistant, "config", uploads);
        String id = turn.thread().getId();
        String attachmentId = store.get(id, scope).messages().get(0).getAttachments().get(0).id();
        assertEquals("1.png", attachmentId);
        assertArrayEquals(uploads.get(0).bytes(), new AiChatStore().attachment(id, attachmentId, scope).bytes());
        assertTrue(store.begin(input, scope, assistant, "config", uploads).duplicate());
        List.of(new AiChatStore.Scope(43, 7, "example.test", root), new AiChatStore.Scope(42, 8, "example.test", root),
            new AiChatStore.Scope(42, 7, "other.test", root)).forEach(other -> assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> store.attachment(id, attachmentId, other)).getStatusCode()));
        assertThrows(ResponseStatusException.class, () -> store.attachment(id, "../notes.txt", scope));

        store.finish(scope, id, turn.message().getId(), "", "error", 0, "UNKNOWN", null);
        var retry = store.begin(new AiChatRequest(id, 12L, UUID.randomUUID().toString(), 1L, "Read this", turn.message().getId()),
            scope, assistant, "config");
        assertEquals(turn.message().getTurnId(), retry.message().getTurnId());
        assertEquals(turn.question().getId(), retry.question().getId());
        assertEquals("Read this", retry.question().getText());
        assertEquals(turn.question().getAttachments(), retry.question().getAttachments());
        store.finish(scope, id, retry.message().getId(), "Answer", "complete", 1, null, null);
        var next = store.begin(new AiChatRequest(id, 12L, UUID.randomUUID().toString(), 2L, "More", null),
            scope, assistant, "config", upload("notes.md", "Notes".getBytes(UTF_8)));
        assertArrayEquals("Notes".getBytes(UTF_8), store.attachment(id, "2.md", scope).bytes());
        assertArrayEquals(uploads.get(0).bytes(), store.attachment(id, attachmentId, scope).bytes());

        var other = store.begin(new AiChatRequest(null, 12L, UUID.randomUUID().toString(), null, "Other", null),
            scope, assistant, "config", uploads);
        store.finish(scope, id, next.message().getId(), "Done", "complete", 1, null, null);
        store.stop(id, scope, true);
        store.remove(scope, id, store.claimDeletion(id, scope));
        assertFalse(Files.exists(root.resolve("7/42").resolve(id)));
        assertArrayEquals(uploads.get(0).bytes(), store.attachment(other.thread().getId(), "1.png", scope).bytes());
    }
}
