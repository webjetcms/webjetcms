package sk.iway.iwcm.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.common.MultipartFileSender;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.elfinder.FsService;
import sk.iway.iwcm.system.elfinder.FsSecurityCheckForAll;
import sk.iway.iwcm.system.elfinder.IwcmFsItem;
import sk.iway.iwcm.system.elfinder.IwcmFsVolume;
import sk.iway.iwcm.system.elfinder.UnusedFilesService;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies how file-management and download entry points handle private chat files. */
class AiChatDeletionSecurityTest extends BaseWebjetTest {
    private static final String THREAD = "/files/protected/ai-chat/7/42/934be8ec-6b59-4cbe-9575-a1a69f943dea";
    @TempDir Path root;
    private ServletContext originalContext;

    @BeforeEach
    void setUp() throws Exception {
        root = root.toRealPath();
        originalContext = Constants.getServletContext();
        Constants.setServletContext(new MockServletContext() {
            @Override
            public String getRealPath(String path) {
                return root.resolve(path.substring(1)).toString();
            }
        });
        for (String name : new String[] {THREAD + "/ai_chat.json", THREAD + "/1.txt"}) {
            Path path = path(name);
            Files.createDirectories(path.getParent());
            Files.writeString(path, "old content");
        }
        Files.writeString(path("/files/protected/recent.txt"), "recent content");
    }

    @AfterEach
    void restoreContext() {
        Constants.setServletContext(originalContext);
    }

    /** File-manager deletion still rejects directly selected private files and folders. */
    @Test
    void fileManagerRejectsPrivateDeletion() throws Exception {
        var volume = new IwcmFsVolume("files", file("/"));
        Identity user = new Identity();
        user.setAdmin(true);
        user.setWritableFolders("/");
        try (var users = mockStatic(FsService.class)) {
            users.when(FsService::getCurrentUser).thenReturn(user);
            assertFalse(volume.deleteFolder(new IwcmFsItem(volume, file(THREAD))));
            assertFalse(volume.deleteFile(new IwcmFsItem(volume, file(THREAD + "/1.txt"))));
        }
        assertTrue(Files.exists(path(THREAD + "/ai_chat.json")));
        assertTrue(Files.exists(path("/files/protected/recent.txt")));
    }

    /** Chat storage does not lock its parent folders or override ordinary administrator deletion. */
    @Test
    void fileManagerDoesNotLockChatAncestors() throws Exception {
        var volume = new IwcmFsVolume("files", file("/"));
        Identity user = mock(Identity.class);
        when(user.getWritableFolders()).thenReturn("*");
        var parent = new IwcmFsItem(volume, file("/files/protected"));
        assertFalse(new FsSecurityCheckForAll().isLocked(mock(FsService.class), parent));
        try (var users = mockStatic(FsService.class); var indexer = mockStatic(FileIndexerTools.class);
             var audit = mockStatic(Adminlog.class)) {
            users.when(FsService::getCurrentUser).thenReturn(user);
            assertTrue(volume.deleteFolder(parent));
        }
        assertFalse(Files.exists(path("/files/protected")));
    }

    /** The shared HTTP sender protects chat files for direct calls and the FilePathTools wrapper. */
    @Test
    void publicDownloadsStillRequireTheChatEndpoint() throws Exception {
        var request = new MockHttpServletRequest();
        var direct = new MockHttpServletResponse();
        MultipartFileSender.fromFile(file(THREAD + "/ai_chat.json")).with(request).with(direct).serveResource();
        assertEquals(404, direct.getStatus());
        assertEquals("no-store", direct.getHeader("Cache-Control"));
        var wrapped = new MockHttpServletResponse();
        assertTrue(FilePathTools.writeFileOut(file(THREAD + "/ai_chat.json"), request, wrapped));
        assertEquals(404, wrapped.getStatus());
    }

    /** elFinder hides private storage and rejects direct listing. */
    @Test
    void fileManagerFiltersPrivateListingsAtTheEntryPoint() {
        var volume = new IwcmFsVolume("files", file("/"));
        assertEquals(0, volume.listChildren(new IwcmFsItem(volume, file(THREAD))).length);
        Identity user = new Identity();
        user.setAdmin(true);
        user.setWritableFolders("/");
        try (var users = mockStatic(FsService.class)) {
            users.when(FsService::getCurrentUser).thenReturn(user);
            Set<String> visible = Arrays.stream(volume.listChildren(new IwcmFsItem(volume, file("/files/protected"))))
                .map(item -> ((IwcmFsItem)item).getFile().getName()).collect(Collectors.toSet());
            assertEquals(Set.of("recent.txt"), visible);
        }
    }

    /** The unused-files deletion API applies the existing folder permissions to directly submitted chat paths. */
    @Test
    void unusedFilesDeletionRejectsPrivatePaths() throws Exception {
        var service = new UnusedFilesService();
        Identity user = new Identity();
        user.setAdmin(true);
        user.setWritableFolders("/");
        try {
            var error = assertThrows(ResponseStatusException.class, () -> service.deleteFile(THREAD + "/ai_chat.json", user));
            assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
            assertTrue(Files.exists(path(THREAD + "/ai_chat.json")));
        } finally {
            service.destroy();
        }
    }

    private Path path(String virtualPath) { return root.resolve(virtualPath.substring(1)); }
    private IwcmFile file(String virtualPath) { return new IwcmFile(path(virtualPath)); }
}
