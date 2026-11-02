package sk.iway.iwcm.components.ai.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.components.ai.chat.AiChatAttachment;
import sk.iway.iwcm.components.ai.chat.AiChatFiles;
import sk.iway.iwcm.components.ai.dto.AiChatRequest;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;

/** Covers upload validation, extraction and persistent owner-scoped downloads. */
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

    /** Extracts text from uploads without depending on external document fixtures. */
    @Test
    void extractsDocumentText() throws Exception {
        var text = upload("notes.md", "Príloha 🌍".getBytes(UTF_8)).get(0);
        assertEquals("Príloha 🌍", AiChatAttachments.documentText(text.attachment(), text.bytes()));
        try (var document = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Attachment content");
            document.write(output);
            var file = upload("attachment.docx", output.toByteArray()).get(0);
            assertTrue(AiChatAttachments.documentText(file.attachment(), file.bytes()).contains("Attachment content"));
        }
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
    void retainsPrivateAttachmentsAcrossRetriesAndDeletesThemWithHistory() throws Exception {
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
        Path file = AiChatAttachments.path(root.resolve("42"), id, attachmentId);
        assertTrue(Files.isRegularFile(file));
        assertTrue(Files.isRegularFile(file.resolveSibling("ai_chat.json")));
        assertArrayEquals(uploads.get(0).bytes(), new AiChatStore().attachment(id, attachmentId, scope).bytes());
        assertTrue(store.begin(input, scope, assistant, "config", upload("obrázok.png", uploads.get(0).bytes())).duplicate());
        assertEquals("requestChanged", assertThrows(ResponseStatusException.class,
            () -> store.begin(input, scope, assistant, "config", upload("notes.txt", new byte[] {65}))).getReason());
        List.of(new AiChatStore.Scope(43, 7, "example.test", root), new AiChatStore.Scope(42, 8, "example.test", root),
            new AiChatStore.Scope(42, 7, "other.test", root)).forEach(other -> assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> store.attachment(id, attachmentId, other)).getStatusCode()));
        assertThrows(ResponseStatusException.class, () -> store.attachment(id, "../notes.txt", scope));
        store.finish(scope, id, turn.message().getId(), "", "error", 0, "UNKNOWN", null);
        var retry = store.begin(new AiChatRequest(id, 12L, UUID.randomUUID().toString(), 1L, "Read this", turn.message().getId()), scope, assistant, "config");
        assertEquals(turn.message().getTurnId(), retry.message().getTurnId());
        assertEquals(1, store.get(id, scope).messages().stream().filter(message -> "user".equals(message.getRole())).count());
        store.finish(scope, id, retry.message().getId(), "", "error", 0, "UNKNOWN", null);
        store.stop(id, scope, true);
        store.remove(scope, id, store.claimDeletion(id, scope));
        assertFalse(Files.exists(file));
        assertFalse(Files.exists(file.getParent()));
    }

    /** Restores flat legacy storage, including a migration interrupted after moving one attachment. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void migratesJsonAndAllAttachmentsTogether(boolean interrupted) throws Exception {
        root = root.toRealPath();
        var scope = new AiChatStore.Scope(42, 7, "example.test", root);
        var assistant = new AssistantDefinitionEntity();
        assistant.setId(12L);
        String id = UUID.randomUUID().toString();
        var uploads = AiChatAttachments.read(List.of(
            new MockMultipartFile("files", "first.txt", "text/plain", "First file".getBytes(UTF_8)),
            new MockMultipartFile("files", "second.md", "text/plain", "Second file".getBytes(UTF_8))));
        store.begin(new AiChatRequest(null, 12L, id, null, "Read both", null), scope, assistant, "config", uploads);
        Path owner = root.resolve("42"), folder = owner.resolve(id);
        Path legacy = owner.resolve(id + ".json");
        var document = AiChatFiles.read(folder.resolve("ai_chat.json"));
        var saved = document.getMessages().get(0).getAttachments();
        List<AiChatAttachment> legacyAttachments = new ArrayList<>();
        for (int i = 0; i < uploads.size(); i++) {
            var attachment = saved.get(i);
            String attachmentId = UUID.randomUUID().toString();
            legacyAttachments.add(new AiChatAttachment(attachmentId, attachment.name(), attachment.mediaType(), attachment.size(), attachment.sha256()));
            Path target = interrupted && i == 0 ? folder.resolve(attachmentId + ".bin")
                : owner.resolve(id + "-" + attachmentId + ".bin");
            Files.move(AiChatAttachments.path(owner, id, attachment.id()), target);
        }
        document.getMessages().get(0).setAttachments(legacyAttachments);
        AiChatFiles.write(legacy, document);
        Files.delete(folder.resolve("ai_chat.json"));
        if (!interrupted) Files.delete(folder);

        assertEquals(List.of(id), store.list(scope, 0).items().stream().map(thread -> thread.getId()).toList());
        assertFalse(Files.exists(legacy));
        assertTrue(Files.isRegularFile(folder.resolve("ai_chat.json")));
        for (int i = 0; i < uploads.size(); i++) {
            String attachmentId = legacyAttachments.get(i).id();
            assertFalse(Files.exists(owner.resolve(id + "-" + attachmentId + ".bin")));
            assertArrayEquals(uploads.get(i).bytes(), new AiChatStore().attachment(id, attachmentId, scope).bytes());
        }
        assertEquals(2, store.get(id, scope).messages().get(0).getAttachments().size());
        store.finish(scope, id, document.getMessages().get(1).getId(), "Done", "complete", 1, null, null);
        store.begin(new AiChatRequest(id, 12L, UUID.randomUUID().toString(), 1L, "One more", null),
            scope, assistant, "config", upload("third.txt", "Third file".getBytes(UTF_8)));
        assertArrayEquals("Third file".getBytes(UTF_8), new AiChatStore().attachment(id, "3.txt", scope).bytes());
    }

    /** Keeps original extensions and a shared sequence across turns, retries and unfinished uploads. */
    @Test
    void numbersMixedFileTypesWithoutOverwritingExistingFiles() throws Exception {
        root = root.toRealPath();
        var scope = new AiChatStore.Scope(42, 7, "example.test", root);
        var assistant = new AssistantDefinitionEntity();
        assistant.setId(12L);
        byte[] docx;
        try (var document = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Attachment content");
            document.write(output);
            docx = output.toByteArray();
        }
        List<MultipartFile> files = List.of(
            new MockMultipartFile("files", "document.DOCX", "application/octet-stream", docx),
            new MockMultipartFile("files", "picture.png", "image/png", new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10}),
            new MockMultipartFile("files", "picture.jpg", "image/jpeg", new byte[] {(byte)255, (byte)216, (byte)255}));
        var input = new AiChatRequest(null, 12L, UUID.randomUUID().toString(), null, "Read these", null);
        var turn = store.begin(input, scope, assistant, "config", AiChatAttachments.read(files));
        String id = turn.thread().getId();
        Path folder = root.resolve("42").resolve(id);
        Files.move(folder.resolve("ai_chat.json"), folder.resolve(id + ".json"));
        var saved = new AiChatStore().get(id, scope).messages().get(0).getAttachments();
        assertTrue(Files.isRegularFile(folder.resolve("ai_chat.json")));
        assertFalse(Files.exists(folder.resolve(id + ".json")));
        assertEquals(List.of("1.docx", "2.png", "3.jpg"), saved.stream().map(AiChatAttachment::id).toList());
        for (int i = 0; i < saved.size(); i++) {
            assertArrayEquals(files.get(i).getBytes(), Files.readAllBytes(folder.resolve(saved.get(i).id())));
            assertEquals(files.get(i).getOriginalFilename(), store.attachment(id, saved.get(i).id(), scope).attachment().name());
        }
        assertTrue(store.begin(input, scope, assistant, "config", AiChatAttachments.read(files)).duplicate());
        store.finish(scope, id, turn.message().getId(), "Done", "complete", 1, null, null);
        Files.writeString(folder.resolve("4.png"), "Unfinished upload");
        var nextFiles = AiChatAttachments.read(List.of(
            new MockMultipartFile("files", "picture.jpeg", "image/jpeg", files.get(2).getBytes()),
            new MockMultipartFile("files", "notes.md", "text/plain", "Notes".getBytes(UTF_8))));
        store.begin(new AiChatRequest(id, 12L, UUID.randomUUID().toString(), 1L, "More", null),
            scope, assistant, "config", nextFiles);
        var continued = new AiChatStore().get(id, scope).messages().get(2).getAttachments();
        assertEquals(List.of("5.jpeg", "6.md"), continued.stream().map(AiChatAttachment::id).toList());
        assertArrayEquals(files.get(2).getBytes(), store.attachment(id, "5.jpeg", scope).bytes());
        assertArrayEquals("Notes".getBytes(UTF_8), store.attachment(id, "6.md", scope).bytes());
        assertEquals("Unfinished upload", Files.readString(folder.resolve("4.png")));
    }

    /** Rejects paths and extensions outside the accepted attachment naming formats. */
    @ParameterizedTest
    @ValueSource(strings = { "../1.txt", "1.txt/other", "0.txt", "01.txt", "-1.txt", "1.jsp", "1.bin", "1.txt.exe" })
    void rejectsInvalidStorageNames(String attachmentId) {
        assertThrows(ResponseStatusException.class,
            () -> AiChatAttachments.path(root, UUID.randomUUID().toString(), attachmentId));
    }

    /** Groups multiple turns' files by thread and deletes only the selected thread's complete folder. */
    @Test
    void isolatesThreadFilesAndRemovesTheirDirectory() throws Exception {
        root = root.toRealPath();
        var scope = new AiChatStore.Scope(42, 7, "example.test", root);
        var assistant = new AssistantDefinitionEntity();
        assistant.setId(12L);
        var uploads = AiChatAttachments.read(List.of(
            new MockMultipartFile("files", "notes.txt", "text/plain", "First".getBytes(UTF_8)),
            new MockMultipartFile("files", "notes.txt", "text/plain", "Second".getBytes(UTF_8))));
        String firstId = UUID.randomUUID().toString(), secondId = UUID.randomUUID().toString();
        var first = store.begin(new AiChatRequest(null, 12L, firstId, null, "First chat", null), scope, assistant, "config", uploads);
        store.begin(new AiChatRequest(null, 12L, secondId, null, "Second chat", null), scope, assistant, "config", uploads);
        store.finish(scope, firstId, first.message().getId(), "Answer", "complete", 1, null, null);
        var more = upload("later.txt", "Third".getBytes(UTF_8));
        var next = store.begin(new AiChatRequest(firstId, 12L, UUID.randomUUID().toString(), 1L, "More", null),
            scope, assistant, "config", more);
        Path owner = root.resolve("42"), firstFolder = owner.resolve(firstId), secondFolder = owner.resolve(secondId);
        assertTrue(Files.isRegularFile(firstFolder.resolve("ai_chat.json")));
        assertTrue(Files.isRegularFile(secondFolder.resolve("ai_chat.json")));
        for (int i = 0; i < uploads.size(); i++) {
            String attachmentId = (i + 1) + ".txt";
            assertEquals(firstFolder, AiChatAttachments.path(owner, firstId, attachmentId).getParent());
            assertArrayEquals(uploads.get(i).bytes(), store.attachment(firstId, attachmentId, scope).bytes());
            assertArrayEquals(uploads.get(i).bytes(), store.attachment(secondId, attachmentId, scope).bytes());
        }
        assertArrayEquals(more.get(0).bytes(), store.attachment(firstId, "3.txt", scope).bytes());
        Files.writeString(firstFolder.resolve("unreferenced.bin"), "Unfinished upload");
        Files.writeString(firstFolder.resolve(".chat-interrupted.tmp"), "Unfinished JSON");
        store.finish(scope, firstId, next.message().getId(), "Done", "complete", 1, null, null);
        assertFalse(Files.exists(firstFolder.resolve(".chat-interrupted.tmp")));
        store.stop(firstId, scope, true);
        store.remove(scope, firstId, store.claimDeletion(firstId, scope));
        assertFalse(Files.exists(firstFolder));
        assertTrue(Files.isRegularFile(secondFolder.resolve("ai_chat.json")));
        for (int i = 0; i < uploads.size(); i++) assertArrayEquals(uploads.get(i).bytes(), store.attachment(secondId, (i + 1) + ".txt", scope).bytes());
        assertEquals(List.of(secondId), store.list(scope, 0).items().stream().map(thread -> thread.getId()).toList());
    }

}
