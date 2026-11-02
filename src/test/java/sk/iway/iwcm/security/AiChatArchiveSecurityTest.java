package sk.iway.iwcm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
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

        write("/files/public.txt", "Public file");
        write("/files/protected/document.txt", "Ordinary protected file");
        write("/files/protected/ai-chat-backup/keep.txt", "Unrelated sibling directory");
        write(THREAD + "/ai_chat.json", "Private conversation");
        write(THREAD + "/1.png", "Private image attachment");
        write(THREAD + "/nested/notes.css", "Private nested attachment");
        write(CHAT_ROOT + "/43/other-thread/ai_chat.json", "Another user's private conversation");
    }

    @AfterEach
    void tearDown() {
        Constants.setServletContext(originalContext);
    }

    /** Parent exports preserve ordinary files while excluding every user's entire private subtree. */
    @ParameterizedTest
    @EnumSource(Exporter.class)
    void excludesChatsFromParentExports(Exporter exporter) throws Exception {
        Map<String, String> protectedFiles = Map.of(
            "files/protected/document.txt", "Ordinary protected file",
            "files/protected/ai-chat-backup/keep.txt", "Unrelated sibling directory");
        Map<String, String> allFiles = new LinkedHashMap<>(protectedFiles);
        allFiles.put("files/public.txt", "Public file");

        assertEquals(allFiles, export(exporter, "/files"));
        assertEquals(protectedFiles, export(exporter, "/files/protected"));
    }

    /** Selecting the private root, an owner directory or a conversation directly exports no data. */
    @ParameterizedTest
    @EnumSource(Exporter.class)
    void excludesDirectPrivateExports(Exporter exporter) throws Exception {
        for (String path : new String[] {CHAT_ROOT, CHAT_ROOT + "/42", THREAD}) {
            assertEquals(Map.of(), export(exporter, path), path);
        }
    }

    /** Canonical path checks reject directory and file aliases without excluding ordinary symbolic links. */
    @ParameterizedTest
    @EnumSource(Exporter.class)
    void excludesPrivateSymbolicAliases(Exporter exporter) throws Exception {
        symbolicLink("/files/chat-alias", CHAT_ROOT);
        symbolicLink("/files/chat-attachment.txt", THREAD + "/1.png");
        symbolicLink("/files/ordinary-alias.txt", "/files/protected/document.txt");

        assertEquals(Map.of(
            "files/public.txt", "Public file",
            "files/protected/document.txt", "Ordinary protected file",
            "files/protected/ai-chat-backup/keep.txt", "Unrelated sibling directory",
            "files/ordinary-alias.txt", "Ordinary protected file"), export(exporter, "/files"));
        assertEquals(Map.of(), export(exporter, "/files/chat-alias"));
    }

    private void write(String path, String content) throws IOException {
        Path file = root.resolve(path.substring(1));
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private void symbolicLink(String path, String target) throws IOException {
        try {
            Files.createSymbolicLink(root.resolve(path.substring(1)), root.resolve(target.substring(1)));
        } catch (UnsupportedOperationException | IOException exception) {
            assumeTrue(false, "Symbolic links are unavailable: " + exception.getMessage());
        }
    }

    private Map<String, String> export(Exporter exporter, String directory) throws Exception {
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

        Map<String, String> entries = new LinkedHashMap<>();
        String absolutePrefix = root.toString().replace('\\', '/') + "/";
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.equals(MARKER)) continue;
                if (name.startsWith(absolutePrefix)) name = name.substring(absolutePrefix.length());
                if (name.startsWith("/")) name = name.substring(1);
                entries.put(name, new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }
}
