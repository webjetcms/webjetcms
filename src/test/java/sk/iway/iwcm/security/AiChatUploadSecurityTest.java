package sk.iway.iwcm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.io.IwcmFsDB;

/** Verifies private upload protection without changing the shared writer's stream ownership contract. */
class AiChatUploadSecurityTest {

    @TempDir
    Path root;

    private ServletContext originalContext;

    @BeforeEach
    void setUp() throws IOException {
        root = root.toRealPath();
        originalContext = Constants.getServletContext();
        Constants.setServletContext(new MockServletContext() {
            @Override
            public String getRealPath(String path) {
                return root.resolve(path.substring(1)).toString();
            }
        });
    }

    @AfterEach
    void tearDown() {
        Constants.setServletContext(originalContext);
    }

    /** Rejects private overwrites, preserves history, and closes only inputs owned by the writer. */
    @Test
    void rejectsPrivateUploadAndPreservesInputOwnership() throws IOException {
        Path history = root.resolve("files/protected/ai-chat/42/934be8ec-6b59-4cbe-9575-a1a69f943dea/ai_chat.json");
        Files.createDirectories(history.getParent());
        Files.writeString(history, "private history");

        for (boolean closeInput : new boolean[] { true, false }) {
            TrackingInput input = new TrackingInput("replacement");
            assertThrows(SecurityException.class,
                () -> IwcmFsDB.writeFiletoDest(input, history.toFile(), input.available(), closeInput));
            assertEquals("private history", Files.readString(history));
            assertEquals(closeInput, input.closed);
        }
    }

    /** Keeps ordinary uploads working and leaves caller-owned inputs open. */
    @Test
    void writesOrdinaryUploadAndPreservesInputOwnership() throws IOException {
        Path destination = root.resolve("files/protected/document.txt");

        for (boolean closeInput : new boolean[] { true, false }) {
            TrackingInput input = new TrackingInput("ordinary upload");
            IwcmFsDB.writeFiletoDest(input, destination.toFile(), input.available(), closeInput);
            assertEquals("ordinary upload", Files.readString(destination));
            assertEquals(closeInput, input.closed);
        }
    }

    private static final class TrackingInput extends ByteArrayInputStream {
        private boolean closed;

        private TrackingInput(String content) {
            super(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
