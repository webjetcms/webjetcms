package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.ai.dto.AiChatRequest;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;

/** Covers chat persistence when the deployment's files directory is a symbolic link to shared storage. */
class AiChatSymlinkTest {
    @TempDir Path temporary;
    private Path webapp;
    private Path storage;
    private ServletContext originalContext;
    private String originalExternalDirectory;
    private final AiChatStore store = new AiChatStore();

    @BeforeEach
    void setUp() throws IOException {
        temporary = temporary.toRealPath();
        webapp = Files.createDirectory(temporary.resolve("webapp"));
        storage = Files.createDirectory(temporary.resolve("shared-uploads"));
        Files.createSymbolicLink(webapp.resolve("files"), storage);
        originalContext = Constants.getServletContext();
        originalExternalDirectory = Constants.getString("cloudStaticFilesDir");
        Constants.setString("cloudStaticFilesDir", "");
        Constants.setServletContext(new MockServletContext() {
            @Override
            public String getRealPath(String path) {
                return webapp.resolve(path.substring(1)).toString();
            }
        });
    }

    @AfterEach
    void tearDown() {
        Constants.setServletContext(originalContext);
        Constants.setString("cloudStaticFilesDir", originalExternalDirectory);
    }

    /** First use and existing storage both support history, messages and owner-scoped attachment downloads. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void persistsChatsThroughSymbolicFilesRoot(boolean existingStorage) throws Exception {
        if (existingStorage) Files.createDirectories(storage.resolve("protected/ai-chat/42"));
        var scope = new AiChatStore.Scope(42, 7, "example.test");
        assertTrue(store.list(scope, 0).items().isEmpty());
        var assistant = new AssistantDefinitionEntity();
        assistant.setId(12L);
        var input = new AiChatRequest(null, 12L, UUID.randomUUID().toString(), null, "Question", null);
        byte[] content = "Attachment content".getBytes(StandardCharsets.UTF_8);
        var uploads = AiChatAttachments.read(List.of(new MockMultipartFile("files", "notes.txt", "text/plain", content)));
        var turn = store.begin(input, scope, assistant, "config", uploads);
        String id = turn.thread().getId();
        store.finish(scope, id, turn.message().getId(), "Answer", "complete", 1, null, null);

        var reloaded = new AiChatStore();
        var reloadedScope = new AiChatStore.Scope(42, 7, "example.test");
        assertEquals(id, reloaded.list(reloadedScope, 0).items().get(0).getId());
        assertEquals("Answer", reloaded.get(id, reloadedScope).messages().get(1).getText());
        assertArrayEquals(content, reloaded.attachment(id, "1.txt", reloadedScope).bytes());
        assertTrue(Files.exists(storage.resolve("protected/ai-chat/42/" + id + "/ai_chat.json")));

        for (var other : List.of(new AiChatStore.Scope(43, 7, "example.test"),
                new AiChatStore.Scope(42, 8, "example.test"), new AiChatStore.Scope(42, 7, "other.test"))) {
            assertTrue(reloaded.list(other, 0).items().isEmpty());
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> reloaded.attachment(id, "1.txt", other)).getStatusCode());
        }
    }
}
