package sk.iway.iwcm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipInputStream;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.offline.OfflineService;
import sk.iway.iwcm.system.Archive;
import sk.iway.iwcm.system.zip.ZipEntry;
import sk.iway.iwcm.system.zip.ZipOutputStream;

/** Verifies that recursive ZIP exports never include private chat history or attachments. */
class AiChatArchiveSecurityTest {

    private static final String CHAT_ROOT = "/files/protected/ai-chat";
    private static final String THREAD = CHAT_ROOT + "/42/934be8ec-6b59-4cbe-9575-a1a69f943dea";
    private static final String MARKER = "export-test-marker.txt";

    @TempDir
    Path root;

    private ServletContext originalContext;
    private ServletContext context;

    enum Exporter { ARCHIVE, OFFLINE }

    @BeforeEach
    void setUp() throws IOException {
        root = root.toRealPath();
        originalContext = Constants.getServletContext();
        context = new MockServletContext() {
            @Override
            public String getRealPath(String path) {
                return root.resolve(path.substring(1)).toString();
            }
        };
        Constants.setServletContext(context);

        for (String path : new String[] { "/files/protected/document.txt",
                "/files/protected/ai-chat-backup/keep.txt", THREAD + "/ai_chat.json", THREAD + "/1.png" }) {
            Path file = root.resolve(path.substring(1));
            Files.createDirectories(file.getParent());
            Files.writeString(file, "content");
        }
    }

    @AfterEach
    void tearDown() {
        Constants.setServletContext(originalContext);
    }

    /** Both exporters preserve ordinary files and exclude parent-selected or directly selected chats. */
    @ParameterizedTest
    @EnumSource(Exporter.class)
    void excludesChatsFromParentExports(Exporter exporter) throws Exception {
        assertEquals(Set.of("document.txt", "keep.txt"), export(exporter, "/files"));
        assertEquals(Set.of(), export(exporter, THREAD));
    }

    /** Canonical path checks also exclude symbolic aliases of private files. */
    @ParameterizedTest
    @EnumSource(Exporter.class)
    void excludesPrivateSymbolicAliases(Exporter exporter) throws Exception {
        try {
            Files.createSymbolicLink(root.resolve("files/chat-alias"), root.resolve(CHAT_ROOT.substring(1)));
            Files.createSymbolicLink(root.resolve("files/chat-attachment.txt"), root.resolve(THREAD.substring(1) + "/1.png"));
        } catch (UnsupportedOperationException | IOException exception) {
            assumeTrue(false, "Symbolic links are unavailable: " + exception.getMessage());
        }
        assertEquals(Set.of("document.txt", "keep.txt"), export(exporter, "/files"));
        assertEquals(Set.of(), export(exporter, "/files/chat-alias"));
    }

    private Set<String> export(Exporter exporter, String directory) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            // The legacy ZIP writer requires at least one entry, even when every selected file is private.
            zip.putNextEntry(new ZipEntry(MARKER));
            zip.closeEntry();
            if (exporter == Exporter.ARCHIVE) {
                ReflectionTestUtils.invokeMethod(Archive.class, "getFilesFromDir", directory, context, zip, null, "export.zip");
            } else {
                ReflectionTestUtils.invokeMethod(new OfflineService(), "getFilesFromDir",
                    root.resolve(directory.substring(1)).toString(), "/", context, zip, null);
            }
        }

        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.getName().equals(MARKER)) entries.add(Path.of(entry.getName()).getFileName().toString());
            }
        }
        return entries;
    }
}
