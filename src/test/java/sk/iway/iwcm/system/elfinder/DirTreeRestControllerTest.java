package sk.iway.iwcm.system.elfinder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeMoveItem;
import sk.iway.iwcm.test.BaseWebjetTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Test
    void shouldShowOnlyWritableFoldersAndTheirParents() {
        Identity user = createRestrictedUser();
        List<String> writableFolderRoots = DirTreeRestController.getFolderRoots(user.getWritableFolders());

        assertTrue(DirTreeRestController.isFolderVisible(user, "/images", writableFolderRoots, List.of()));
        assertTrue(DirTreeRestController.isFolderVisible(user, "/images/gallery", writableFolderRoots, List.of()));
        assertTrue(DirTreeRestController.isFolderVisible(user, "/images/gallery/test-vela-foto", writableFolderRoots, List.of()));
        assertTrue(DirTreeRestController.isFolderVisible(user, "/images/banner", writableFolderRoots, List.of()));
        assertFalse(DirTreeRestController.isFolderVisible(user, "/images/other", writableFolderRoots, List.of()));
        assertFalse(DirTreeRestController.isFolderVisible(user, "/files", writableFolderRoots, List.of()));
    }

    @Test
    void shouldNotShowChildrenForNonRecursiveWritePermission() {
        Identity user = new Identity();
        user.setWritableFolders("/images/exact/");
        List<String> writableFolderRoots = DirTreeRestController.getFolderRoots(user.getWritableFolders());

        assertTrue(DirTreeRestController.isFolderVisible(user, "/images", writableFolderRoots, List.of()));
        assertTrue(DirTreeRestController.isFolderVisible(user, "/images/exact", writableFolderRoots, List.of()));
        assertFalse(DirTreeRestController.isFolderVisible(user, "/images/exact/child", writableFolderRoots, List.of()));
    }

    @Test
    void shouldAllowSelectionAccordingToWritableOnlyAndVisibilityMode() {
        Identity user = createRestrictedUser();
        List<String> alwaysShownFolderRoots = List.of("/images/common");

        assertFalse(DirTreeRestController.isFolderSelectable(user, "/images/gallery", true, false, alwaysShownFolderRoots));
        assertTrue(DirTreeRestController.isFolderSelectable(user, "/images/other", false, false, alwaysShownFolderRoots));
        assertFalse(DirTreeRestController.isFolderSelectable(user, "/images/other", false, true, alwaysShownFolderRoots));
        assertTrue(DirTreeRestController.isFolderSelectable(user, "/images/gallery/test-vela-foto", true, true, alwaysShownFolderRoots));
        assertTrue(DirTreeRestController.isFolderSelectable(user, "/images/common/shared", true, false, alwaysShownFolderRoots));
        assertFalse(DirTreeRestController.isFolderSelectable(user, "/images/common/shared", true, true, alwaysShownFolderRoots));
    }

    @Test
    void shouldPrepareRootFolderWhenFilteredChildrenAreEmpty() throws IOException {
        String rootVirtualPath = createTestRoot();
        List<DirTreeItem> items = new ArrayList<>();

        new DirTreeRestController().prepareParents(rootVirtualPath, items, true);

        assertEquals(1, items.size());
        assertEquals(rootVirtualPath, items.get(0).getVirtualPath());
        assertEquals("#", items.get(0).getParent());
        assertTrue(items.get(0).getState().isDisabled());
    }

    @Test
    void shouldRejectForbiddenTreePathsBeforeDirectoryListing() {
        assertTrue(DirTreeRestController.isPathAllowed("/"));
        assertFalse(DirTreeRestController.isPathAllowed("/../../.."));
        assertFalse(DirTreeRestController.isPathAllowed("/%2e%2e/%2e%2e"));

        JsTreeMoveItem item = new JsTreeMoveItem();
        item.setId("/../../..");
        Map<String, Object> result = new HashMap<>();

        DirTreeRestController controller = new DirTreeRestController();
        controller.tree(result, item);

        assertEquals(Boolean.FALSE, result.get("result"));
        assertFalse(result.containsKey("items"));
    }

    @Test
    void shouldRequireCanonicalPathContainment() throws IOException {
        String rootVirtualPath = createTestRoot();
        Path rootPath = Path.of(Tools.getRealPath(rootVirtualPath));
        Path childPath = rootPath.resolve("child");
        Files.createDirectories(childPath);

        assertTrue(DirTreeRestController.isWithinCanonicalRoot(childPath.toString(), rootPath.toString()));
        assertFalse(DirTreeRestController.isWithinCanonicalRoot(rootPath.resolve("../outside").toString(), rootPath.toString()));
    }

    private JsTreeMoveItem createConfigItem() {
        JsTreeMoveItem item = new JsTreeMoveItem();
        item.setSkipFoldersConst(SKIP_FOLDERS_CONST);
        return item;
    }

    private Identity createRestrictedUser() {
        Identity user = new Identity();
        user.setWritableFolders("/images/banner/*\n/images/gallery/test-vela-foto/*");
        return user;
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
