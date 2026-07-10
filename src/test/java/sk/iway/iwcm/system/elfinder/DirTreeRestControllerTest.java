package sk.iway.iwcm.system.elfinder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeMoveItem;
import sk.iway.iwcm.test.BaseWebjetTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirTreeRestControllerTest extends BaseWebjetTest {

    private static final String SKIP_FOLDERS_CONST = "dirTreeRestControllerTestSkipFolders";
    private Path testRootPath;

    @AfterEach
    void tearDown() throws IOException {
        Constants.setString(SKIP_FOLDERS_CONST, "");

        if (testRootPath != null && Files.exists(testRootPath)) {
            try (var walk = Files.walk(testRootPath)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Best effort cleanup to keep tests isolated.
                    }
                });
            }
        }
    }

    @Test
    void shouldHideParentFolderWhenItContainsOnlySkippedSubfolders() throws IOException {
        String rootVirtualPath = createTestRoot();
        createDirectory(rootVirtualPath + "/files/archiv_insert_later");
        createDirectory(rootVirtualPath + "/marketing");
        createDirectory(rootVirtualPath + "/sales");

        Constants.setString(SKIP_FOLDERS_CONST, "files/archiv_insert_later/");

        List<DirTreeItem> itemsToCheck = List.of(
                new DirTreeItem(rootVirtualPath + "/files"),
                new DirTreeItem(rootVirtualPath + "/marketing"),
                new DirTreeItem(rootVirtualPath + "/sales")
        );

        List<DirTreeItem> filtered = DirTreeRestController.getAllowedFolders(itemsToCheck, createConfigItem());

        List<String> visiblePaths = virtualPaths(filtered);
        assertFalse(visiblePaths.contains(rootVirtualPath + "/files"), "Parent folder should be hidden when it contains only skipped subfolders.");
        assertTrue(visiblePaths.contains(rootVirtualPath + "/marketing"));
        assertTrue(visiblePaths.contains(rootVirtualPath + "/sales"));
    }

    @Test
    void shouldKeepParentFolderWhenItContainsAtLeastOneAllowedSubfolder() throws IOException {
        String rootVirtualPath = createTestRoot();
        createDirectory(rootVirtualPath + "/files/archiv_insert_later");
        createDirectory(rootVirtualPath + "/files/public");

        Constants.setString(SKIP_FOLDERS_CONST, "files/archiv_insert_later/");

        List<DirTreeItem> itemsToCheck = List.of(new DirTreeItem(rootVirtualPath + "/files"));

        List<DirTreeItem> filtered = DirTreeRestController.getAllowedFolders(itemsToCheck, createConfigItem());

        assertEquals(1, filtered.size());
        assertEquals(rootVirtualPath + "/files", filtered.get(0).getVirtualPath());
        assertEquals(Boolean.TRUE, filtered.get(0).getChildren(), "Parent folder should still be expandable because it has an allowed subfolder.");
    }

    @Test
    void shouldHideFolderThatDirectlyMatchesSkipRule() throws IOException {
        String rootVirtualPath = createTestRoot();
        createDirectory(rootVirtualPath + "/files/archiv_insert_later");
        createDirectory(rootVirtualPath + "/files/visible");

        Constants.setString(SKIP_FOLDERS_CONST, "files/archiv_insert_later/");

        List<DirTreeItem> itemsToCheck = List.of(
                new DirTreeItem(rootVirtualPath + "/files/archiv_insert_later"),
                new DirTreeItem(rootVirtualPath + "/files/visible")
        );

        List<DirTreeItem> filtered = DirTreeRestController.getAllowedFolders(itemsToCheck, createConfigItem());

        List<String> visiblePaths = virtualPaths(filtered);
        assertFalse(visiblePaths.contains(rootVirtualPath + "/files/archiv_insert_later"));
        assertTrue(visiblePaths.contains(rootVirtualPath + "/files/visible"));
    }

    private JsTreeMoveItem createConfigItem() {
        JsTreeMoveItem item = new JsTreeMoveItem();
        item.setSkipFoldersConst(SKIP_FOLDERS_CONST);
        return item;
    }

    private String createTestRoot() throws IOException {
        String rootVirtualPath = "/files/dir-tree-rest-controller-test-" + System.nanoTime();
        testRootPath = Path.of(Tools.getRealPath(rootVirtualPath));
        Files.createDirectories(testRootPath);
        return rootVirtualPath;
    }

    private void createDirectory(String virtualPath) throws IOException {
        Files.createDirectories(Path.of(Tools.getRealPath(virtualPath)));
    }

    private List<String> virtualPaths(List<DirTreeItem> items) {
        List<String> result = new java.util.ArrayList<>();
        for (DirTreeItem item : items) {
            result.add(item.getVirtualPath());
        }
        return result;
    }
}