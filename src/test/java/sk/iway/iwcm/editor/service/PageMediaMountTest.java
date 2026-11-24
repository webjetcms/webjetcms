package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.UploadFileTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.filebrowser.FileReferenceScanner;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.elfinder.UnusedFilesService;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Tests page file cleanup when the filesystem location behind a media root changes. */
class PageMediaMountTest extends BaseWebjetTest {
    /**
     * Verifies that redirecting the media root after capture produces a warning and skips file deletion.
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

        DocDetails page = new DocDetails();
        page.setDocId(17);
        page.setGroupId(3);
        page.setTitle("page");
        Identity user = mock(Identity.class);
        when(user.isFolderWritable(anyString())).thenReturn(true);
        when(user.isEnabledItem("menuFbrowser")).thenReturn(true);
        UnusedFilesService deletion = mock(UnusedFilesService.class);

        try (var paths = mockStatic(Tools.class, CALLS_REAL_METHODS); var groups = mockStatic(GroupsDB.class);
                var docs = mockStatic(DocDB.class); var uploads = mockStatic(UploadFileTools.class);
                var completed = mockStatic(DeleteRequestContext.class); var usage = mockStatic(FileReferenceScanner.class)) {
            paths.when(() -> Tools.getRealPath(anyString())).thenAnswer(call ->
                webroot.resolve(call.<String>getArgument(0).substring(1)).toString());
            paths.when(() -> Tools.getRealPath("/")).thenReturn(webroot + "/");
            DocDB documents = mock(DocDB.class);
            when(documents.getBasicDocDetailsAll()).thenReturn(List.of(page));
            docs.when(DocDB::getInstance).thenReturn(documents);
            GroupsDB folders = mock(GroupsDB.class);
            when(folders.getGroup(3)).thenReturn(new GroupDetails());
            groups.when(GroupsDB::getInstance).thenReturn(folders);
            uploads.when(() -> UploadFileTools.getPageUploadSubDir(eq(17), eq(3), eq("page"), anyString(), anyString(), nullable(String.class)))
                .thenAnswer(call -> call.<String>getArgument(3) + "/page");
            completed.when(() -> DeleteRequestContext.isDeleted(DocDetails.class, 17)).thenReturn(true);

            PageMediaCleanup cleanup = new PageMediaCleanup(deletion);
            cleanup.select(17);
            cleanup.capture(page, user);
            Files.delete(mount);
            Files.createSymbolicLink(mount, replacement.getParent());

            assertEquals("warning", cleanup.finish(user, mock(Prop.class), false).getType());
            verify(deletion).acquireDeleteOperation(anyCollection(), eq(user));
            verify(deletion, never()).deleteFile(anyString(), any());
        }
    }
}
