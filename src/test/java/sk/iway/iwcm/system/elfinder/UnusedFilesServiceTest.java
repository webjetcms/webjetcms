package sk.iway.iwcm.system.elfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for package-private static helpers in {@link UnusedFilesService}:
 * {@code scanCoversFolder}, {@code normalizeVirtualPath} and {@code getParentFolder}.
 * These run without the cache, database or executor infrastructure.
 */
class UnusedFilesServiceTest {

    @Test
    void shouldCoverSameFolder() {
        assertTrue(UnusedFilesService.scanCoversFolder("/files/marketing", false, "/files/marketing"));
        assertTrue(UnusedFilesService.scanCoversFolder("/files/marketing", true, "/files/marketing"));
    }

    @Test
    void shouldNotCoverSubfolderWhenNonRecursive() {
        assertFalse(UnusedFilesService.scanCoversFolder("/files/marketing", false, "/files/marketing/2026"));
    }

    @Test
    void shouldCoverSubfolderWhenRecursive() {
        assertTrue(UnusedFilesService.scanCoversFolder("/files/marketing", true, "/files/marketing/2026"));
        assertTrue(UnusedFilesService.scanCoversFolder("/files/marketing", true, "/files/marketing/2026/q1"));
    }

    @Test
    void shouldNotCoverSiblingFolderSharingPrefix() {
        //- "/files/marketing2" must not be treated as a subfolder of "/files/marketing"
        assertFalse(UnusedFilesService.scanCoversFolder("/files/marketing", true, "/files/marketing2"));
        assertFalse(UnusedFilesService.scanCoversFolder("/files/marketing", false, "/files/marketing2"));
    }

    @Test
    void shouldNotCoverUnrelatedFolder() {
        assertFalse(UnusedFilesService.scanCoversFolder("/files/marketing", true, "/files/sales"));
        assertFalse(UnusedFilesService.scanCoversFolder("/files/marketing", false, "/files/sales"));
    }

    @Test
    void shouldCoverEverythingFromRootWhenRecursive() {
        assertTrue(UnusedFilesService.scanCoversFolder("/", true, "/files/marketing"));
        assertTrue(UnusedFilesService.scanCoversFolder("/", true, "/"));
        assertFalse(UnusedFilesService.scanCoversFolder("/", false, "/files/marketing"));
    }

    // --- normalizeVirtualPath tests ---

    @Test
    void normalizeShouldResolveParentReferences() {
        assertEquals("/files", UnusedFilesService.normalizeVirtualPath("/files/sub/.."));
    }

    @Test
    void normalizeShouldAddLeadingSlash() {
        assertEquals("/files/test", UnusedFilesService.normalizeVirtualPath("files/test"));
    }

    @Test
    void normalizeShouldRemoveTrailingSlash() {
        assertEquals("/files/test", UnusedFilesService.normalizeVirtualPath("/files/test/"));
    }

    @Test
    void normalizeShouldPreserveRootPath() {
        assertEquals("/", UnusedFilesService.normalizeVirtualPath("/"));
    }

    @Test
    void normalizeShouldDecodeUrlEncoding() {
        assertEquals("/files/test file", UnusedFilesService.normalizeVirtualPath("/files/test%20file"));
    }

    @Test
    void normalizeShouldReplaceBackslashes() {
        assertEquals("/files/test", UnusedFilesService.normalizeVirtualPath("\\files\\test"));
    }

    @Test
    void normalizeShouldCollapseDuplicateSlashes() {
        assertEquals("/files/test", UnusedFilesService.normalizeVirtualPath("/files//test"));
    }

    // --- getParentFolder tests ---

    @Test
    void parentFolderShouldReturnParentDirectory() {
        assertEquals("/files/marketing", UnusedFilesService.getParentFolder("/files/marketing/image.jpg"));
    }

    @Test
    void parentFolderShouldReturnRootForTopLevelFile() {
        assertEquals("/", UnusedFilesService.getParentFolder("/image.jpg"));
    }

    @Test
    void parentFolderShouldReturnRootWhenNoSlash() {
        assertEquals("/", UnusedFilesService.getParentFolder("/"));
    }

    @Test
    void parentFolderShouldReturnDeepParent() {
        assertEquals("/a/b/c", UnusedFilesService.getParentFolder("/a/b/c/file.txt"));
    }
}
