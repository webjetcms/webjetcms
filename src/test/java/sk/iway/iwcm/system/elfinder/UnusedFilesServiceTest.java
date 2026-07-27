package sk.iway.iwcm.system.elfinder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the scan/delete conflict-detection logic used by {@link UnusedFilesService}.
 * These verify {@link UnusedFilesService#scanCoversFolder(String, boolean, String)} in isolation,
 * without requiring the cache, database or executor infrastructure.
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
}
