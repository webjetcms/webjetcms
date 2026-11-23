package sk.iway.iwcm.rag.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/** Covers language-filtered scanning and documentation path boundaries. */
class MarkdownSourceServiceTest {
    private final MarkdownSourceService sources = new MarkdownSourceService();
    @TempDir Path directory;

    /** Scanning includes supported language folders and rejects linked files and directories. */
    @Test
    void scansSupportedLanguagesWithoutFollowingSymlinks() throws Exception {
        Path guide = Files.createDirectories(directory.resolve("sk/guide"));
        Files.createDirectories(directory.resolve("unknown"));
        Files.writeString(guide.resolve("README.md"), "# Guide");
        Files.writeString(guide.resolve("_sidebar.md"), "Navigation");
        Files.writeString(directory.resolve("README.md"), "Unclassified");
        Files.writeString(directory.resolve("unknown/README.md"), "Unsupported language");
        Files.createSymbolicLink(directory.resolve("sk/linked"), guide);
        Files.createSymbolicLink(guide.resolve("linked.md"), guide.resolve("README.md"));
        String root = "file:" + directory;
        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getString("ragMarkdownFolders")).thenReturn(root);
            assertEquals(List.of("sk/guide/README.md"), sources.getFiles(root));
            assertEquals(List.of(), sources.getFiles(root, "sk", false));
            assertEquals("sk", sources.detectLanguage(root, "sk/guide/README.md"));
            assertThrows(IOException.class, () -> sources.resolveFile(root, "sk/linked/README.md"));
            assertThrows(IOException.class, () -> sources.resolveFile(root, "sk/guide/linked.md"));
        }
    }

    /** Configured web roots cannot escape the deployment through symlinks or relative traversal. */
    @Test
    void confinesSourcesToTheConfiguredRoot(@TempDir Path outside) throws Exception {
        Files.createSymbolicLink(directory.resolve("docs"), outside);
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
             MockedStatic<Tools> tools = mockStatic(Tools.class)) {
            constants.when(() -> Constants.getString("ragMarkdownFolders")).thenReturn("/docs");
            tools.when(() -> Tools.getRealPath("/")).thenReturn(directory.toString());
            assertThrows(IOException.class, () -> sources.resolveRoot("/docs"));
            assertThrows(IllegalArgumentException.class, () -> sources.requireRoot("/docs/other"));
            assertThrows(IllegalArgumentException.class, () -> sources.resolveFile("/docs", "../secret.md"));
            assertThrows(IllegalArgumentException.class, () -> sources.requireDirectory("users/../secret"));
            assertNull(sources.getRelativePath("/docs", "/docs-other/sk/README.md"));
        }
    }
}
