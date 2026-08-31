package cn.bluejoe.elfinder.controller.executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.fileupload2.core.FileItem;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.util.FsServiceUtils;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.test.BaseWebjetTest;

class UploadCommandExecutorReadOnlyTest extends BaseWebjetTest {

    @Test
    void shouldRejectRenameOfExistingFileInReadOnlyFolder(@TempDir Path tempDir) throws Exception {
        Path source = Files.writeString(tempDir.resolve("existing.txt"), "original");
        Path renamed = tempDir.resolve("existing-1.txt");
        FsService fsService = mock(FsService.class);
        FsItemEx folder = writableFilesFolder();
        Map<String, FileItem<?>> files = new LinkedHashMap<>();

        MockHttpServletRequest request = uploadRequest(files);
        request.addParameter("renames[]", "/archiv/existing.txt");
        JSONObject json = new JSONObject();
        AtomicReference<FsItemEx> protectedDestination = new AtomicReference<>();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockCurrentUser();
             MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
             MockedConstruction<FsItemEx> destinations = mockDestinations(protectedDestination)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "filesHash")).thenReturn(folder);
            tools.when(() -> Tools.getRealPath("/files//archiv/existing.txt")).thenReturn(source.toString());
            tools.when(() -> Tools.getRealPath("/files//archiv/existing-1.txt")).thenReturn(renamed.toString());

            new UploadCommandExecutor().execute(fsService, request, null, json);
        }

        assertNotNull(protectedDestination.get());
        verify(protectedDestination.get()).isWritable(protectedDestination.get());
        assertTrue(Files.exists(source));
        assertTrue(Files.notExists(renamed));
        assertTrue(json.has("error"));
        assertEquals(0, ((Object[]) json.get("added")).length);
    }

    @Test
    void shouldRejectRenameWithParentPathSegment() throws Exception {
        FsService fsService = mock(FsService.class);
        FsItemEx folder = writableFilesFolder();
        Map<String, FileItem<?>> files = new LinkedHashMap<>();

        MockHttpServletRequest request = uploadRequest(files);
        request.addParameter("renames[]", "../archiv/forged.txt");
        JSONObject json = new JSONObject();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockCurrentUser()) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "filesHash")).thenReturn(folder);

            new UploadCommandExecutor().execute(fsService, request, null, json);
        }

        assertTrue(json.has("error"));
        assertEquals(0, ((Object[]) json.get("added")).length);
    }

    @Test
    void shouldRejectNestedUploadIntoReadOnlyFolder() throws Exception {
        FsService fsService = mock(FsService.class);
        FsItemEx folder = writableFilesFolder();
        FileItem<?> upload = mock(FileItem.class);
        Map<String, FileItem<?>> files = new LinkedHashMap<>();
        files.put("/archiv/forged.txt", upload);

        MockHttpServletRequest request = uploadRequest(files);
        JSONObject json = new JSONObject();
        AtomicReference<FsItemEx> protectedDestination = new AtomicReference<>();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockCurrentUser();
             MockedConstruction<FsItemEx> destinations = mockDestinations(protectedDestination)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "filesHash")).thenReturn(folder);

            new UploadCommandExecutor().execute(fsService, request, null, json);
        }

        assertNotNull(protectedDestination.get());
        verify(protectedDestination.get()).isWritable(protectedDestination.get());
        verify(upload, never()).getInputStream();
        assertTrue(json.has("error"));
        assertEquals(0, ((Object[]) json.get("added")).length);
    }

    @Test
    void shouldRejectChunkedUploadPathIntoReadOnlyFolder() throws Exception {
        FsService fsService = mock(FsService.class);
        FsItemEx folder = writableFilesFolder();
        FileItem<?> upload = mock(FileItem.class);
        when(upload.getSize()).thenReturn(3L);
        when(upload.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));
        Map<String, FileItem<?>> files = new LinkedHashMap<>();
        files.put("upload[]", upload);

        MockHttpServletRequest request = uploadRequest(files);
        request.addParameter("cid", "chunk-id");
        request.addParameter("chunk", "forged.txt.0_0.part");
        request.addParameter("range", "0,3,3");
        request.addParameter("upload_path[]", "/archiv/forged.txt");
        JSONObject json = new JSONObject();
        AtomicReference<FsItemEx> protectedDestination = new AtomicReference<>();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockCurrentUser();
             MockedConstruction<FsItemEx> destinations = mockDestinations(protectedDestination)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "filesHash")).thenReturn(folder);

            new UploadCommandExecutor().execute(fsService, request, null, json);
        }

        assertNotNull(protectedDestination.get());
        verify(protectedDestination.get()).isWritable(protectedDestination.get());
        verify(upload).delete();
        assertTrue(json.has("error"));
        assertEquals(0, ((Object[]) json.get("added")).length);
    }

    @Test
    void shouldRejectChunkedUploadPathIntoReadOnlyFolderForFileTarget(@TempDir Path tempDir) throws Exception {
        Path targetFile = Files.writeString(tempDir.resolve("current.txt"), "original");
        FsService fsService = mock(FsService.class);
        FsItemEx target = mock(FsItemEx.class);
        FsItemEx targetParent = mock(FsItemEx.class);
        when(target.getPath()).thenReturn("/files/current.txt");
        when(target.isWritable(target)).thenReturn(true);
        when(target.getParent()).thenReturn(targetParent);
        FileItem<?> upload = mock(FileItem.class);
        when(upload.getSize()).thenReturn(3L);
        when(upload.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));
        Map<String, FileItem<?>> files = new LinkedHashMap<>();
        files.put("upload[]", upload);

        MockHttpServletRequest request = uploadRequest(files);
        request.addParameter("cid", "chunk-file-target-id");
        request.addParameter("chunk", "forged.txt.0_0.part");
        request.addParameter("range", "0,3,3");
        request.addParameter("upload_path[]", "/archiv/forged.txt");
        JSONObject json = new JSONObject();
        AtomicReference<FsItemEx> protectedDestination = new AtomicReference<>();
        AtomicReference<FsItemEx> protectedDestinationParent = new AtomicReference<>();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockCurrentUser();
             MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
             MockedConstruction<FsItemEx> destinations = mockConstruction(FsItemEx.class, (item, context) -> {
                 FsItemEx parent = (FsItemEx) context.arguments().get(0);
                 String relativePath = (String) context.arguments().get(1);
                 String parentPath = parent == targetParent ? "/files" : "/files/current.txt";
                 String path = parentPath + "/" + relativePath;
                 when(item.getPath()).thenReturn(path);
                 boolean writable = path.startsWith("/files/archiv/")==false;
                 when(item.isWritable(item)).thenReturn(writable);
                 when(item.getParent()).thenReturn(targetParent);
                 if (writable==false) {
                     protectedDestination.set(item);
                     protectedDestinationParent.set(parent);
                 }
             })) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "filesHash")).thenReturn(target);
            tools.when(() -> Tools.getRealPath("/files/current.txt")).thenReturn(targetFile.toString());

            new UploadCommandExecutor().execute(fsService, request, null, json);
        }

        assertNotNull(protectedDestination.get());
        assertSame(targetParent, protectedDestinationParent.get());
        verify(protectedDestination.get()).isWritable(protectedDestination.get());
        verify(upload).delete();
        assertEquals("original", Files.readString(targetFile));
        assertTrue(json.has("error"));
        assertEquals(0, ((Object[]) json.get("added")).length);
    }

    private FsItemEx writableFilesFolder() throws Exception {
        FsItemEx folder = mock(FsItemEx.class);
        when(folder.getPath()).thenReturn("/files");
        when(folder.isWritable(folder)).thenReturn(true);
        when(folder.isFolder()).thenReturn(true);
        return folder;
    }

    private MockHttpServletRequest uploadRequest(Map<String, FileItem<?>> files) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("target", "filesHash");
        request.setAttribute("MultipartWrapper.files", files);
        return request;
    }

    private MockedStatic<sk.iway.iwcm.system.elfinder.FsService> mockCurrentUser() {
        Identity user = new Identity();
        user.setWritableFolders("*");
        MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockStatic(sk.iway.iwcm.system.elfinder.FsService.class);
        currentUser.when(sk.iway.iwcm.system.elfinder.FsService::getCurrentUser).thenReturn(user);
        return currentUser;
    }

    private MockedConstruction<FsItemEx> mockDestinations(AtomicReference<FsItemEx> protectedDestination) {
        return mockConstruction(FsItemEx.class, (item, context) -> {
            String relativePath = (String) context.arguments().get(1);
            String path = "/files/" + relativePath;
            boolean validPath = relativePath.contains("..")==false;
            when(item.getPath()).thenReturn(validPath ? path : null);
            boolean writable = validPath && path.startsWith("/files/archiv/")==false;
            when(item.isWritable(item)).thenReturn(writable);
            if (writable==false) protectedDestination.set(item);
        });
    }
}
