package cn.bluejoe.elfinder.controller.executors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.util.FsServiceUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExtractCommandExecutorReadOnlyTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRejectReadOnlyZipEntryDestination() throws Exception {
        Path zipFile = createZip("archiv/document.pdf");
        FsItemEx outputFolder = mock(FsItemEx.class);

        try (MockedStatic<Tools> tools = mockStatic(Tools.class);
             MockedConstruction<FsItemEx> destinations = mockConstruction(FsItemEx.class, (destination, context) -> {
                 when(destination.getPath()).thenReturn("/files/archiv/document.pdf");
                 when(destination.isWritable(destination)).thenReturn(false);
             })) {
            tools.when(() -> Tools.getRealPath("/files/import.zip")).thenReturn(zipFile.toString());

            assertFalse(new ExtractCommandExecutor().areAllExtractEntriesWritable("/files/import.zip", outputFolder));
            verify(destinations.constructed().get(0)).isWritable(destinations.constructed().get(0));
        }
    }

    @Test
    void shouldRecheckDestinationImmediatelyBeforeExtracting() throws Exception {
        Path zipFile = createZip("archiv/document.pdf");
        Path outputDirectory = Files.createDirectory(tempDir.resolve("output"));
        FsItemEx fsi = mock(FsItemEx.class);
        FsItemEx outputFolder = mock(FsItemEx.class);
        when(fsi.getParent()).thenReturn(outputFolder);

        try (MockedStatic<Tools> tools = mockStatic(Tools.class);
             MockedConstruction<FsItemEx> destinations = mockConstruction(FsItemEx.class, (destination, context) -> {
                 when(destination.getPath()).thenReturn("/files/archiv/document.pdf");
                 when(destination.isWritable(destination)).thenReturn(false);
             })) {
            tools.when(() -> Tools.getRealPath("/files/import.zip")).thenReturn(zipFile.toString());
            tools.when(() -> Tools.getRealPath("/files")).thenReturn(outputDirectory.toString());
            tools.when(() -> Tools.getRealPath("/")).thenReturn(tempDir.toString());

            assertNull(new ExtractCommandExecutor().unZipFile("/files/import.zip", "/files", fsi));
            verify(destinations.constructed().get(0)).isWritable(destinations.constructed().get(0));
        }

        try (var files = Files.list(outputDirectory)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void shouldValidateEntriesBeforeCreatingExtractFolder() throws Exception {
        FsService fsService = mock(FsService.class);
        FsItemEx zipFile = mock(FsItemEx.class);
        FsItemEx zipParent = mock(FsItemEx.class);
        FsItemEx extractFolder = mock(FsItemEx.class);
        Identity user = mock(Identity.class);
        Prop prop = mock(Prop.class);
        when(zipFile.isWritable(zipFile)).thenReturn(true);
        when(zipFile.getParent()).thenReturn(zipParent);
        when(zipFile.getPath()).thenReturn("/files/import.zip");
        when(zipFile.getName()).thenReturn("import.zip");
        when(zipParent.isWritable(zipParent)).thenReturn(true);
        when(extractFolder.getPath()).thenReturn("/files/unzip-test");
        when(user.getWritableFolders()).thenReturn("/");
        when(prop.getText(anyString(), anyString())).thenReturn("Extract is not allowed");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("target", "zipHash");
        request.addParameter("makedir", "1");
        JSONObject json = new JSONObject();
        RejectingExtractCommandExecutor executor = new RejectingExtractCommandExecutor();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockStatic(sk.iway.iwcm.system.elfinder.FsService.class);
             MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
             MockedStatic<Prop> props = mockStatic(Prop.class);
             MockedConstruction<FsItemEx> extractItems = mockConstruction(FsItemEx.class, (extractItem, context) ->
                 when(extractItem.getParent()).thenReturn(extractFolder))) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "zipHash")).thenReturn(zipFile);
            currentUser.when(sk.iway.iwcm.system.elfinder.FsService::getCurrentUser).thenReturn(user);
            users.when(() -> UsersDB.isFolderWritable("/", "/files/import.zip")).thenReturn(true);
            props.when(() -> Prop.getInstance(request)).thenReturn(prop);

            executor.execute(fsService, request, null, json);
        }

        verify(extractFolder, never()).createFolder();
        assertFalse(executor.unzipCalled);
        assertTrue(json.has("error"));
    }

    @Test
    void shouldValidateAndExtractAgainstTheSameParentPath() throws Exception {
        FsService fsService = mock(FsService.class);
        FsItemEx zipFile = mock(FsItemEx.class);
        FsItemEx zipParent = mock(FsItemEx.class);
        Identity user = mock(Identity.class);
        Prop prop = mock(Prop.class);
        when(zipFile.isWritable(zipFile)).thenReturn(true);
        when(zipFile.getParent()).thenReturn(zipParent);
        when(zipFile.getPath()).thenReturn("/files/archiv.zip/import.zip");
        when(zipParent.isWritable(zipParent)).thenReturn(true);
        when(zipParent.getPath()).thenReturn("/files/archiv.zip");
        when(user.getWritableFolders()).thenReturn("/");
        when(prop.getText(anyString(), anyString())).thenReturn("Extract is not allowed");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("target", "zipHash");
        JSONObject json = new JSONObject();
        CapturingExtractCommandExecutor executor = new CapturingExtractCommandExecutor();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockStatic(sk.iway.iwcm.system.elfinder.FsService.class);
             MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
             MockedStatic<Prop> props = mockStatic(Prop.class)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "zipHash")).thenReturn(zipFile);
            currentUser.when(sk.iway.iwcm.system.elfinder.FsService::getCurrentUser).thenReturn(user);
            users.when(() -> UsersDB.isFolderWritable("/", "/files/archiv.zip/import.zip")).thenReturn(true);
            props.when(() -> Prop.getInstance(request)).thenReturn(prop);

            executor.execute(fsService, request, null, json);
        }

        assertSame(zipParent, executor.validatedOutputFolder);
        assertEquals("/files/archiv.zip", executor.extractedOutputFolder);
    }

    private Path createZip(String entryName) throws IOException {
        Path zipFile = tempDir.resolve("import.zip");
        try (java.util.zip.ZipOutputStream output = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipFile))) {
            output.putNextEntry(new java.util.zip.ZipEntry(entryName));
            output.write(1);
            output.closeEntry();
        }
        return zipFile;
    }

    private static class RejectingExtractCommandExecutor extends ExtractCommandExecutor {
        private boolean unzipCalled;

        @Override
        protected boolean areAllExtractEntriesWritable(String zipFile, FsItemEx outputFolder) {
            return false;
        }

        @Override
        protected List<FsItemEx> unZipFile(String zipFile, String outputFolder, FsItemEx fsi) {
            unzipCalled = true;
            return List.of();
        }
    }

    private static class CapturingExtractCommandExecutor extends ExtractCommandExecutor {
        private FsItemEx validatedOutputFolder;
        private String extractedOutputFolder;

        @Override
        protected boolean areAllExtractEntriesWritable(String zipFile, FsItemEx outputFolder) {
            validatedOutputFolder = outputFolder;
            return true;
        }

        @Override
        protected List<FsItemEx> unZipFile(String zipFile, String outputFolder, FsItemEx fsi) {
            extractedOutputFolder = outputFolder;
            return List.of();
        }
    }
}
