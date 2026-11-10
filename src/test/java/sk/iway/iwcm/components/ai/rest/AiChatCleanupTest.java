package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.components.ai.chat.AiChatPaths;

/** Verifies explicit cron scope and owner traversal without following storage links. */
class AiChatCleanupTest {
    @TempDir Path root;

    /** A failing owner does not block other users, and another domain or a linked owner is never visited. */
    @Test
    void cleansOnlyOwnersInTheSelectedDomain() throws Exception {
        root = root.toRealPath();
        Files.createDirectories(root.resolve("7/42"));
        Files.createDirectories(root.resolve("7/43"));
        Files.createDirectories(root.resolve("8/44"));
        Files.createDirectories(root.resolve("7/invalid"));
        Files.createSymbolicLink(root.resolve("7/45"), root.resolve("8/44"));
        var service = mock(AiChatService.class);
        Date cutoff = new Date(1000);
        var first = new AiChatStore.Scope(42, 7, "example.test", root);
        var second = new AiChatStore.Scope(43, 7, "example.test", root);
        when(service.deleteOlderThan(first, cutoff)).thenThrow(new IllegalStateException("Unavailable"));
        when(service.deleteOlderThan(second, cutoff)).thenReturn(2);

        assertEquals(2, AiChatCleanup.cleanup(root, 7, "example.test", cutoff, service));
        verify(service).deleteOlderThan(first, cutoff);
        verify(service).deleteOlderThan(second, cutoff);
        verifyNoMoreInteractions(service);
    }

    /** Missing or nonpositive retention settings fail before resolving any storage or service. */
    @Test
    void requiresExplicitPositiveRetentionAndDomain() {
        assertThrows(IllegalArgumentException.class, () -> AiChatCleanup.main(new String[0]));
        assertThrows(IllegalArgumentException.class, () -> AiChatCleanup.main(new String[] {"0", "7", "example.test"}));
        assertThrows(IllegalArgumentException.class, () -> AiChatCleanup.main(new String[] {"-1", "7", "example.test"}));
        assertThrows(IllegalArgumentException.class, () -> AiChatCleanup.main(new String[] {"30", "7", ""}));
    }

    /** Background cleanup resolves external storage from its explicit domain instead of the current request. */
    @Test
    void resolvesExternalStorageForTheConfiguredDomain() throws Exception {
        Path domainRoot = root.toRealPath().resolve("example.test");
        Path storage = domainRoot.resolve("files/protected/ai-chat");
        Files.createDirectories(storage.resolve("7/42"));
        var service = mock(AiChatService.class);
        try (var tools = mockStatic(Tools.class); var paths = mockStatic(FilePathTools.class)) {
            tools.when(() -> Tools.getSpringBean("aiChatService", AiChatService.class)).thenReturn(service);
            paths.when(() -> FilePathTools.isExternalDir(AiChatPaths.ROOT)).thenReturn(true);
            paths.when(() -> FilePathTools.getDomainBaseFolder("example.test")).thenReturn(domainRoot.toString());

            AiChatCleanup.main(new String[] {"90", "7", "example.test"});

            verify(service).deleteOlderThan(eq(new AiChatStore.Scope(42, 7, "example.test", storage)),
                argThat(cutoff -> cutoff.before(new Date(System.currentTimeMillis() - 89 * 86_400_000L))));
        }
    }
}
