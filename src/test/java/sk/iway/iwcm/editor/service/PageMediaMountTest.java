package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.UploadFileTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.rest.GroupSchedulerDtoRepository;
import sk.iway.iwcm.filebrowser.FileReferenceScanner;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.elfinder.UnusedFilesService;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Tests page file cleanup when the filesystem location behind a media root changes. */
class PageMediaMountTest extends BaseWebjetTest {
    /** A cleanup flag submitted for a live page cannot capture files or run a deletion scan. */
    @Test
    void keepsFilesWhenMovingAPageToTrash() {
        DocDetails page = new DocDetails();
        page.setDocId(17);
        page.setGroupId(3);
        var history = mock(DocHistoryRepository.class);
        var deletion = mock(UnusedFilesService.class);
        var cleanup = new PageMediaCleanup(deletion, history, mock(GroupSchedulerDtoRepository.class));
        Identity user = mock(Identity.class);
        try (var groups = mockStatic(GroupsDB.class); var completed = mockStatic(DeleteRequestContext.class)) {
            groups.when(GroupsDB::getInstance).thenReturn(mock(GroupsDB.class));
            completed.when(() -> DeleteRequestContext.isDeleted(DocDetails.class, 17)).thenReturn(true);
            assertFalse(cleanup.canClean(page, user));
            cleanup.select(17);
            cleanup.capture(page, user);
            assertNull(cleanup.finish(user, mock(Prop.class), false));
            verifyNoInteractions(history, deletion);
        }
    }

    /** Resolves a folder deleted with its children while keeping the live Trash tree intact. */
    @Test
    void resolvesTheOriginalPathOfANestedTrashedFolder() {
        var history = mock(GroupSchedulerDtoRepository.class);
        var previous = mock(GroupSchedulerDtoRepository.GroupParent.class);
        when(previous.getGroupId()).thenReturn(2);
        when(previous.getParentGroupId()).thenReturn(1);
        when(history.findParentsForRecovery(List.of(2))).thenReturn(List.of(previous));
        var pageHistory = mock(DocHistoryRepository.class);
        var folders = new TrashPageMediaFolders(pageHistory, history);
        var root = folder(1, 0, "Home", "site");
        root.setDomainName("example.test");
        var moved = folder(2, 30, "News", "news");
        var child = folder(3, 2, "Awards", "awards");
        var trash = folder(30, 1, "Trash", "trash");
        GroupsDB db = mock(GroupsDB.class);
        when(db.getTrashGroupsAllDomains()).thenReturn(List.of(trash));
        when(db.getTopLevelGroupsInTrash()).thenReturn(List.of(moved));
        when(db.getGroup(1)).thenReturn(root);
        when(db.getGroup(2)).thenReturn(moved);
        when(db.getGroup(3)).thenReturn(child);
        when(db.isInTrash(3)).thenReturn(true);
        when(db.getParentGroups(3, true)).thenReturn(List.of(trash, moved, child));
        DocDetails page = new DocDetails();
        try (var groups = mockStatic(GroupsDB.class)) {
            groups.when(GroupsDB::getInstance).thenReturn(db);
            groups.when(() -> GroupsDB.getURLPathDomainGroup(anyMap(), eq(3))).thenCallRealMethod();
            page.setGroupId(3);
            assertEquals(new TrashPageMediaFolders.Folder(3, "/Home/News/Awards", "/site/news/awards", "example.test"), folders.forPage(page));
            assertEquals(30, moved.getParentGroupId());
            page.setGroupId(30);
            assertNull(folders.forPage(page));
        }
    }

    private static GroupDetails folder(int id, int parent, String name, String url) {
        GroupDetails group = new GroupDetails();
        group.setGroupId(id);
        group.setParentGroupId(parent);
        group.setGroupName(name);
        group.setUrlDirName(url);
        return group;
    }

    /**
     * Verifies that a nested alias protects linked files and redirecting the media root after capture skips deletion.
     *
     * @param webroot temporary web root containing the original and replacement media directories
     * @throws Exception if the test directories, files or symbolic links cannot be created or replaced
     */
    @Test
    @EnabledOnOs({ OS.MAC, OS.LINUX })
    void keepsFilesWhenTheMediaRootChangesAfterCapture(@TempDir Path webroot) throws Exception {
        Path original = Files.createDirectories(webroot.resolve("original/page"));
        Path replacement = Files.createDirectories(webroot.resolve("replacement/page"));
        Files.createFile(original.resolve("document.pdf"));
        Files.createFile(replacement.resolve("document.pdf"));
        Path mount = Files.createSymbolicLink(webroot.resolve("files"), original.getParent());
        Files.createFile(original.resolve("unused.pdf"));
        Files.createFile(Files.createDirectory(mount.resolve("archive")).resolve("document.pdf"));
        Files.createSymbolicLink(Files.createDirectory(mount.resolve("public")).resolve("manuals"), original);
        Path tenantPage = Files.createDirectories(mount.resolve("tenant/page"));
        Files.createFile(tenantPage.resolve("document.pdf"));
        Files.createSymbolicLink(Files.createDirectory(mount.resolve("tenant/public")).resolve("manuals"), tenantPage);
        Files.writeString(Files.createDirectory(webroot.resolve("templates")).resolve("links.html"),
            "<a href='/files/public/manuals/document.pdf'>Download</a>");

        DocDetails page = new DocDetails();
        page.setDocId(17);
        page.setGroupId(30);
        page.setTitle("page");
        DocDetails second = new DocDetails();
        second.setDocId(18);
        second.setGroupId(30);
        second.setTitle("second");
        Files.createFile(Files.createDirectories(mount.resolve("second")).resolve("second.pdf"));
        Identity user = mock(Identity.class);
        when(user.isFolderWritable(anyString())).thenReturn(true);
        when(user.isEnabledItem("menuFbrowser")).thenReturn(true);
        UnusedFilesService deletion = mock(UnusedFilesService.class);

        try (var paths = mockStatic(Tools.class, CALLS_REAL_METHODS); var groups = mockStatic(GroupsDB.class);
                var docs = mockStatic(DocDB.class); var uploads = mockStatic(UploadFileTools.class);
                var completed = mockStatic(DeleteRequestContext.class); var pool = mockStatic(DBPool.class)) {
            paths.when(() -> Tools.getRealPath(anyString())).thenAnswer(call ->
                webroot.resolve(call.<String>getArgument(0).substring(1)).toString());
            paths.when(() -> Tools.getRealPath("/")).thenReturn(webroot + "/");
            pool.when(DBPool::getConnection).thenReturn(mock(Connection.class, RETURNS_DEEP_STUBS));
            String tenantFile = "/files/tenant/page/document.pdf";
            assertEquals(Set.of("/files/page/document.pdf", tenantFile), FileReferenceScanner.referencedElsewhere(
                Set.of("/files/page/document.pdf", tenantFile, "/files/page/unused.pdf", "/files/archive/document.pdf"),
                Map.of(tenantFile, "/files/page/document.pdf"), Set.of()));
            DocDB documents = mock(DocDB.class);
            when(documents.getBasicDocDetailsAll()).thenReturn(List.of(page, second));
            docs.when(DocDB::getInstance).thenReturn(documents);
            GroupsDB folders = mock(GroupsDB.class);
            GroupDetails originalFolder = new GroupDetails();
            originalFolder.setFullPath("/News");
            when(folders.getGroup(3)).thenReturn(originalFolder);
            GroupDetails trash = new GroupDetails();
            trash.setGroupId(30);
            when(folders.getTrashGroupsAllDomains()).thenReturn(List.of(trash));
            when(folders.isInTrash(30)).thenReturn(true);
            when(folders.getParentGroups(30, true)).thenReturn(List.of(trash));
            groups.when(GroupsDB::getInstance).thenReturn(folders);
            uploads.when(() -> UploadFileTools.getPageUploadSubDir(anyInt(), eq(3), anyString(), anyString(), anyString(), nullable(String.class)))
                .thenAnswer(call -> call.<String>getArgument(3) + "/" + call.<String>getArgument(2));
            uploads.when(() -> UploadFileTools.getPageUploadSubDir(eq(-1), eq(3), isNull(), anyString(), anyString(), nullable(String.class)))
                .thenAnswer(call -> call.<String>getArgument(3) + "/news");
            completed.when(() -> DeleteRequestContext.isDeleted(DocDetails.class, 17)).thenReturn(true);

            DocHistoryRepository history = mock(DocHistoryRepository.class);
            var origins = List.of(originalGroup(17), originalGroup(18));
            when(history.findGroupsBeforeTrash(Set.of(30))).thenReturn(origins);
            PageMediaCleanup cleanup = new PageMediaCleanup(deletion, history, mock(GroupSchedulerDtoRepository.class));
            assertEquals(List.of(new PageMediaCleanup.PageFiles("/News/page", List.of("/files/page/unused.pdf")),
                new PageMediaCleanup.PageFiles("/News/second", List.of("/files/second/second.pdf"))), cleanup.preview(List.of(page, second), user));
            verifyNoInteractions(deletion);
            cleanup.select(17);
            cleanup.capture(page, user);
            Files.delete(mount);
            Files.createSymbolicLink(mount, replacement.getParent());

            assertEquals("warning", cleanup.finish(user, mock(Prop.class), false).getType());
            verify(deletion).acquireDeleteOperation(anyCollection(), eq(user));
            verify(deletion, never()).deleteFile(anyString(), any());
        }
    }

    private static DocHistoryRepository.PageGroup originalGroup(int pageId) {
        DocHistoryRepository.PageGroup row = mock(DocHistoryRepository.PageGroup.class);
        when(row.getDocId()).thenReturn(pageId);
        when(row.getGroupId()).thenReturn(3);
        return row;
    }
}
