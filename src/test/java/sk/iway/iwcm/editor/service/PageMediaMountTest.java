package sk.iway.iwcm.editor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.rest.GroupSchedulerDtoRepository;
import sk.iway.iwcm.filebrowser.FileReferenceScanner;
import sk.iway.iwcm.system.elfinder.UnusedFilesService;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Tests page file cleanup when the filesystem location behind a media root changes. */
class PageMediaMountTest extends BaseWebjetTest {
    private final UnusedFilesService deletion = mock(UnusedFilesService.class);
    private final PageMediaCleanup cleanup = new PageMediaCleanup(deletion, mock(DocHistoryRepository.class), mock(GroupSchedulerDtoRepository.class));
    private final Identity user = mock(Identity.class);

    /** Retains a moved page's files and rechecks an earlier folder's file after its last reference is deleted. */
    @Test
    void rechecksSharedFilesAcrossFolderDeletes(@TempDir Path webroot) {
        String shared = "/files/shared.pdf";
        String moved = "/files/moved.pdf";
        when(user.isEnabledItem("menuFbrowser")).thenReturn(true);
        when(deletion.deleteFile(shared, user)).thenReturn(true);
        try (var queries = mockConstruction(SimpleQuery.class, (query, context) ->
                when(query.forListInteger(anyString())).thenReturn(context.getCount() == 1 ? List.of(18) : List.of()));
             var scan = mockStatic(FileReferenceScanner.class);
             var paths = mockStatic(Tools.class, CALLS_REAL_METHODS)) {
            paths.when(() -> Tools.getRealPath(anyString())).thenAnswer(call -> webroot.resolve(call.<String>getArgument(0).substring(1)).toString());
            scan.when(() -> FileReferenceScanner.referencedElsewhere(anySet(), anyMap(), anySet())).thenAnswer(call -> {
                assertEquals(Set.of(shared), call.getArgument(0));
                Set<Integer> deleted = call.getArgument(2);
                if (deleted.equals(Set.of(17))) return Set.of(shared);
                assertEquals(Set.of(17, 19), deleted);
                return Set.of();
            });
            cleanup.deleteFolderFiles(new HashMap<>(Map.of(17, Set.of(shared), 18, Set.of(moved))), user);
            verify(deletion, never()).deleteFile(anyString(), any());

            cleanup.deleteFolderFiles(new HashMap<>(Map.of(19, Set.of())), user);
            verify(deletion).deleteFile(shared, user);
            verify(deletion, never()).deleteFile(moved, user);
        }
    }

    /** A blank stored URL and a storage root replaced after capture both keep files out of deletion. */
    @Test
    @EnabledOnOs({ OS.MAC, OS.LINUX })
    void keepsFilesWhenStorageChanges(@TempDir Path webroot) throws Exception {
        String file = "/files/news/photo.jpg";
        Path original = Files.createDirectories(webroot.resolve("original/news"));
        Files.createFile(original.resolve("photo.jpg"));
        Files.createFile(Files.createDirectories(webroot.resolve("original/generated")).resolve("photo.jpg"));
        Path replacement = Files.createDirectories(webroot.resolve("replacement/news"));
        Files.createFile(replacement.resolve("photo.jpg"));
        Path mount = Files.createSymbolicLink(webroot.resolve("files"), original.getParent());

        GroupDetails group = new GroupDetails();
        group.setGroupId(3);
        DocDetails page = new DocDetails();
        page.setDocId(17);
        page.setGroupId(3);
        DocDetails blank = new DocDetails();
        blank.setDocId(18);
        blank.setGroupId(3);
        blank.setVirtualPath("/generated.html");

        Connection connection = mock(Connection.class, RETURNS_DEEP_STUBS);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(contains("virtual_path")).executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, true, false);
        when(rows.getInt(1)).thenReturn(17, 18);
        when(rows.getInt(2)).thenReturn(3);
        when(rows.getString("virtual_path")).thenReturn("/news.html", " ");
        DocDB documents = mock(DocDB.class);
        when(documents.getBasicDocDetails(17, false)).thenReturn(page);
        when(documents.getBasicDocDetails(18, false)).thenReturn(blank);
        when(documents.getBasicDocDetailsAll()).thenReturn(List.of(page, blank));
        GroupsDB folders = mock(GroupsDB.class);
        when(folders.getGroup(3)).thenReturn(group);
        when(user.isFolderWritable(anyString())).thenReturn(true);
        when(user.isEnabledItem("menuFbrowser")).thenReturn(true);

        boolean previous = Constants.getBoolean("galleryUploadDirVirtualPath");
        try (var paths = mockStatic(Tools.class, CALLS_REAL_METHODS); var groups = mockStatic(GroupsDB.class);
                var docs = mockStatic(DocDB.class); var scan = mockStatic(FileReferenceScanner.class);
                var queries = mockConstruction(SimpleQuery.class)) {
            Constants.setBoolean("galleryUploadDirVirtualPath", true);
            paths.when(() -> Tools.getRealPath(anyString())).thenAnswer(call -> webroot.resolve(call.<String>getArgument(0).substring(1)).toString());
            groups.when(GroupsDB::getInstance).thenReturn(folders);
            docs.when(DocDB::getInstance).thenReturn(documents);
            docs.when(() -> DocDB.getURLFromDocId(17, null)).thenReturn("/news.html");
            docs.when(() -> DocDB.getURLFromDocId(18, null)).thenReturn(blank.getVirtualPath());

            var files = cleanup.captureFolderPages(List.of(group), user, connection);
            assertEquals(Map.of(17, Set.of(file), 18, Set.of()), files);
            verify(documents, never()).getDoc(anyInt());

            Files.delete(mount);
            Files.createSymbolicLink(mount, replacement.getParent());
            cleanup.deleteFolderFiles(files, user);
            verify(deletion).acquireDeleteOperation(anyCollection(), eq(user));
            verify(deletion, never()).deleteFile(anyString(), any());
        } finally {
            Constants.setBoolean("galleryUploadDirVirtualPath", previous);
        }
    }
}
