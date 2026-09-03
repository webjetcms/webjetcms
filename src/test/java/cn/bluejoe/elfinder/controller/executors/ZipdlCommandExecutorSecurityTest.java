package cn.bluejoe.elfinder.controller.executors;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.service.FsVolume;
import cn.bluejoe.elfinder.util.FsServiceUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.elfinder.IwcmFsVolume;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZipdlCommandExecutorSecurityTest extends BaseWebjetTest {

    @Test
    void shouldDownloadReadOnlyFolderUsingTemporaryZipOutsideSource(@TempDir Path root) throws Exception {
        Path source = Files.createDirectories(root.resolve("source/archiv"));
        Path sourceFile = Files.writeString(source.resolve("document.txt"), "document content");
        Path serverTemp = root.resolve("server-temp");
        FsVolume sourceVolume = mock(IwcmFsVolume.class);
        FsItemEx document = fileItem(sourceFile, sourceVolume);
        FsItemEx folder = folderItem("archiv", source, sourceVolume, List.of(document));
        FsService fsService = mock(FsService.class);
        MockHttpServletRequest request = createRequest("folderHash");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<IwcmFsDB> tempDirectory = mockStatic(IwcmFsDB.class)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "folderHash")).thenReturn(folder);
            tempDirectory.when(IwcmFsDB::getTempDir).thenReturn(serverTemp.toString());

            new ZipdlCommandExecutor().execute(fsService, request, response, null);
        }

        TemporaryZip temporaryZip = temporaryZip(request, response);
        assertTrue(temporaryZip.path.startsWith(
            serverTemp.resolve(ZipdlCommandExecutor.ZIPDL_TEMP_DIRECTORY).toRealPath()));
        assertFalse(temporaryZip.path.startsWith(source.toRealPath()));
        assertEquals(Map.of("archiv/document.txt", "document content"), readZip(temporaryZip.path));
        assertEquals("document content", Files.readString(sourceFile));
        try (var sourceFiles = Files.list(source)) {
            assertEquals(1, sourceFiles.count());
        }
        verifySourceWasNotMutated(sourceVolume, folder, document);

        try (MockedStatic<IwcmFsDB> tempDirectory = mockStatic(IwcmFsDB.class)) {
            tempDirectory.when(IwcmFsDB::getTempDir).thenReturn(serverTemp.toString());
            downloadAndAssertCleanup(fsService, temporaryZip, (MockHttpSession) request.getSession());
        }
        assertEquals("document content", Files.readString(sourceFile));
    }

    @Test
    void shouldDownloadMultipleWritableFilesUsingTemporaryZipWithoutChangingSources(@TempDir Path root) throws Exception {
        Path source = Files.createDirectories(root.resolve("source/archiv"));
        Path firstPath = Files.writeString(source.resolve("first.txt"), "first content");
        Path secondPath = Files.writeString(source.resolve("second.txt"), "second content");
        Path serverTemp = root.resolve("server-temp");
        FsVolume sourceVolume = mock(IwcmFsVolume.class);
        FsItemEx first = fileItem(firstPath, sourceVolume);
        FsItemEx second = fileItem(secondPath, sourceVolume);
        when(first.isWritable(first)).thenReturn(true);
        when(first.isLocked(first)).thenReturn(false);
        when(second.isWritable(second)).thenReturn(true);
        when(second.isLocked(second)).thenReturn(false);
        FsService fsService = mock(FsService.class);
        MockHttpServletRequest request = createRequest("firstHash", "secondHash");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<IwcmFsDB> tempDirectory = mockStatic(IwcmFsDB.class);
             MockedConstruction<ArchiveCommandExecutor> archives = mockConstruction(ArchiveCommandExecutor.class)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "firstHash")).thenReturn(first);
            finder.when(() -> FsServiceUtils.findItem(fsService, "secondHash")).thenReturn(second);
            tempDirectory.when(IwcmFsDB::getTempDir).thenReturn(serverTemp.toString());

            new ZipdlCommandExecutor().execute(fsService, request, response, null);

            assertTrue(archives.constructed().isEmpty());
        }

        TemporaryZip temporaryZip = temporaryZip(request, response);
        Map<String, String> expected = Map.of(
            "first.txt", "first content",
            "second.txt", "second content");
        assertEquals(expected, readZip(temporaryZip.path));
        verifySourceWasNotMutated(sourceVolume, first, second);

        MockHttpServletResponse downloadResponse;
        try (MockedStatic<IwcmFsDB> tempDirectory = mockStatic(IwcmFsDB.class)) {
            tempDirectory.when(IwcmFsDB::getTempDir).thenReturn(serverTemp.toString());
            downloadResponse = download(fsService, temporaryZip, (MockHttpSession) request.getSession());
        }
        assertEquals(expected, readZip(downloadResponse.getContentAsByteArray()));
        assertFalse(Files.exists(temporaryZip.path));
        assertNull(request.getSession().getAttribute(temporaryZip.sessionAttribute));
        assertEquals("first content", Files.readString(firstPath));
        assertEquals("second content", Files.readString(secondPath));
    }

    @Test
    void shouldRejectForgedTemporaryZipToken() throws Exception {
        FsService fsService = mock(FsService.class);
        MockHttpServletRequest request = downloadRequest("forged-token", new MockHttpSession());
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ZipdlCommandExecutor().execute(fsService, request, response, null);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    }

    @Test
    void shouldConsumeTemporaryZipTokenOnlyOnce(@TempDir Path root) throws Exception {
        Path serverTemp = root.resolve("server-temp");
        FsService fsService = mock(FsService.class);
        FsVolume sourceVolume = mock(IwcmFsVolume.class);
        FsItemEx item = fileItem(Files.writeString(root.resolve("source.txt"), "content"), sourceVolume);
        MockHttpServletRequest createRequest = createRequest("sourceHash");
        MockHttpServletResponse createResponse = new MockHttpServletResponse();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<IwcmFsDB> tempDirectory = mockStatic(IwcmFsDB.class)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "sourceHash")).thenReturn(item);
            tempDirectory.when(IwcmFsDB::getTempDir).thenReturn(serverTemp.toString());
            new ZipdlCommandExecutor().execute(fsService, createRequest, createResponse, null);

            TemporaryZip temporaryZip = temporaryZip(createRequest, createResponse);
            MockHttpSession session = (MockHttpSession) createRequest.getSession();
            MockHttpServletResponse firstResponse = download(fsService, temporaryZip, session);
            assertEquals(HttpServletResponse.SC_OK, firstResponse.getStatus());

            MockHttpServletResponse secondResponse = new MockHttpServletResponse();
            new ZipdlCommandExecutor().execute(
                fsService, downloadRequest(temporaryZip.token, session), secondResponse, null);
            assertEquals(HttpServletResponse.SC_FORBIDDEN, secondResponse.getStatus());
        }
    }

    @Test
    void shouldRejectSessionPathOutsideTemporaryDirectoryWithoutDeletingIt(@TempDir Path root) throws Exception {
        Path serverTemp = root.resolve("server-temp");
        Path protectedFile = Files.writeString(root.resolve("outside.zip"), "do not delete");
        String token = "registered-token";
        String sessionAttribute = ZipdlCommandExecutor.ZIPDL_SESSION_ATTRIBUTE_PREFIX + token;
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(sessionAttribute, protectedFile.toString());
        MockHttpServletRequest request = downloadRequest(token, session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<IwcmFsDB> tempDirectory = mockStatic(IwcmFsDB.class)) {
            tempDirectory.when(IwcmFsDB::getTempDir).thenReturn(serverTemp.toString());
            new ZipdlCommandExecutor().execute(mock(FsService.class), request, response, null);
        }

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(Files.exists(protectedFile));
        assertEquals("do not delete", Files.readString(protectedFile));
        assertNull(session.getAttribute(sessionAttribute));
    }

    @Test
    void shouldDeleteTemporaryZipWhenDownloadFails(@TempDir Path root) throws Exception {
        Path serverTemp = root.resolve("server-temp");
        FsService fsService = mock(FsService.class);
        FsItemEx item = fileItem(
            Files.writeString(root.resolve("source.txt"), "content"), mock(IwcmFsVolume.class));
        MockHttpServletRequest createRequest = createRequest("sourceHash");
        MockHttpServletResponse createResponse = new MockHttpServletResponse();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<IwcmFsDB> tempDirectory = mockStatic(IwcmFsDB.class)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "sourceHash")).thenReturn(item);
            tempDirectory.when(IwcmFsDB::getTempDir).thenReturn(serverTemp.toString());
            new ZipdlCommandExecutor().execute(fsService, createRequest, createResponse, null);

            TemporaryZip temporaryZip = temporaryZip(createRequest, createResponse);
            MockHttpServletRequest downloadRequest = downloadRequest(
                temporaryZip.token, (MockHttpSession) createRequest.getSession());
            HttpServletResponse failingResponse = mock(HttpServletResponse.class);
            when(failingResponse.getOutputStream()).thenReturn(failingOutputStream());

            assertThrows(IOException.class, () ->
                new ZipdlCommandExecutor().execute(fsService, downloadRequest, failingResponse, null));
            assertFalse(Files.exists(temporaryZip.path));
            assertNull(createRequest.getSession().getAttribute(temporaryZip.sessionAttribute));
        }
    }

    @Test
    void shouldDeletePartialTemporaryZipWhenNestedSourceIsNotReadable(@TempDir Path root) throws Exception {
        Path serverTemp = root.resolve("server-temp");
        FsService fsService = mock(FsService.class);
        FsItemEx readable = fileItem(
            Files.writeString(root.resolve("readable.txt"), "content"), mock(IwcmFsVolume.class));
        FsItemEx unreadable = mock(FsItemEx.class);
        when(unreadable.isReadable(unreadable)).thenReturn(false);
        when(unreadable.getName()).thenReturn("unreadable.txt");
        FsItemEx folder = folderItem(
            "archiv", root.resolve("source/archiv"), mock(IwcmFsVolume.class), List.of(readable, unreadable));
        MockHttpServletRequest request = createRequest("folderHash");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<IwcmFsDB> tempDirectory = mockStatic(IwcmFsDB.class)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "folderHash")).thenReturn(folder);
            tempDirectory.when(IwcmFsDB::getTempDir).thenReturn(serverTemp.toString());

            new ZipdlCommandExecutor().execute(fsService, request, response, null);
        }

        assertTrue(new JSONObject(response.getContentAsString()).has("error"));
        Path zipDirectory = serverTemp.resolve(ZipdlCommandExecutor.ZIPDL_TEMP_DIRECTORY);
        assertTrue(Files.isDirectory(zipDirectory));
        try (var files = Files.list(zipDirectory)) {
            assertEquals(0, files.count());
        }
    }

    private FsItemEx fileItem(Path path, FsVolume sourceVolume) throws Exception {
        byte[] content = Files.readAllBytes(path);
        FsItemEx item = mock(FsItemEx.class);
        when(item.isReadable(item)).thenReturn(true);
        when(item.isWritable(item)).thenReturn(false);
        when(item.isLocked(item)).thenReturn(true);
        when(item.isFolder()).thenReturn(false);
        when(item.getName()).thenReturn(path.getFileName().toString());
        when(item.getPath()).thenReturn(path.toString());
        when(item.getLastModified()).thenReturn(Files.getLastModifiedTime(path).toMillis());
        when(item.getSize()).thenReturn((long) content.length);
        when(item.getVolume()).thenReturn(sourceVolume);
        when(item.openInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(content));
        return item;
    }

    private FsItemEx folderItem(String name, Path path, FsVolume sourceVolume, List<FsItemEx> children) throws Exception {
        FsItemEx folder = mock(FsItemEx.class);
        when(folder.isReadable(folder)).thenReturn(true);
        when(folder.isWritable(folder)).thenReturn(false);
        when(folder.isLocked(folder)).thenReturn(true);
        when(folder.isFolder()).thenReturn(true);
        when(folder.getName()).thenReturn(name);
        when(folder.getPath()).thenReturn(path.toString());
        when(folder.getVolume()).thenReturn(sourceVolume);
        when(folder.listChildren()).thenReturn(children);
        return folder;
    }

    private MockHttpServletRequest createRequest(String... targets) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("targets[]", targets);
        return request;
    }

    private MockHttpServletRequest downloadRequest(String token, MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.addParameter("download", "1");
        request.addParameter("targets[]", ZipdlCommandExecutor.ZIPDL_HASH_PREFIX + token);
        return request;
    }

    private TemporaryZip temporaryZip(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        String responseToken = new JSONObject(response.getContentAsString())
            .getJSONObject("zipdl")
            .getString("file");
        String token = responseToken.substring(ZipdlCommandExecutor.ZIPDL_HASH_PREFIX.length());
        String sessionAttribute = ZipdlCommandExecutor.ZIPDL_SESSION_ATTRIBUTE_PREFIX + token;
        String path = (String) request.getSession().getAttribute(sessionAttribute);
        return new TemporaryZip(token, sessionAttribute, Path.of(path));
    }

    private MockHttpServletResponse download(
        FsService fsService, TemporaryZip temporaryZip, MockHttpSession session) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ZipdlCommandExecutor().execute(
            fsService,
            downloadRequest(temporaryZip.token, session),
            response,
            null);
        return response;
    }

    private void downloadAndAssertCleanup(
        FsService fsService, TemporaryZip temporaryZip, MockHttpSession session) throws Exception {
        MockHttpServletResponse response = download(fsService, temporaryZip, session);
        assertEquals(Map.of("archiv/document.txt", "document content"),
            readZip(response.getContentAsByteArray()));
        assertFalse(Files.exists(temporaryZip.path));
        assertNull(session.getAttribute(temporaryZip.sessionAttribute));
    }

    private Map<String, String> readZip(Path zip) throws IOException {
        return readZip(Files.readAllBytes(zip));
    }

    private Map<String, String> readZip(byte[] zip) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    private ServletOutputStream failingOutputStream() {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // No asynchronous writes in this test.
            }

            @Override
            public void write(int value) throws IOException {
                throw new IOException("response write failed");
            }
        };
    }

    private void verifySourceWasNotMutated(FsVolume sourceVolume, FsItemEx... items) throws Exception {
        verify(sourceVolume, never()).createFile(any(FsItem.class));
        verify(sourceVolume, never()).createFolder(any(FsItem.class), any(FsItemEx.class));
        verify(sourceVolume, never()).deleteFile(any(FsItem.class));
        verify(sourceVolume, never()).deleteFolder(any(FsItem.class));
        verify(sourceVolume, never()).rename(any(FsItem.class), any(FsItem.class));
        for (FsItemEx item : items) {
            verify(item, never()).createFile();
            verify(item, never()).createFolder();
            verify(item, never()).delete();
            verify(item, never()).renameTo(any(FsItemEx.class));
        }
    }

    private record TemporaryZip(String token, String sessionAttribute, Path path) {
    }
}
