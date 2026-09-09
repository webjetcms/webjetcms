package cn.bluejoe.elfinder.controller.executors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsSecurityChecker;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.elfinder.IwcmFsVolume;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ExtractCommandExecutorForbiddenNameTest extends BaseWebjetTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSkipForbiddenEntriesAndExtractRemainingFiles() throws Exception {
        byte[] content = new byte[] {1, 2, 3};
        List<String> allowedNames = List.of("before.txt", "nested/Name$subname.class", "nested/Name$1.class", "after.txt");
        List<String> skippedNames = List.of("skipped$file.txt", "nested/source.java", "folder$/Name.class");
        assertTrue(FileBrowserTools.hasForbiddenSymbol(skippedNames.get(0)));
        Path archive = tempDir.resolve("import.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (String name : List.of("before.txt", "skipped$file.txt", "nested/Name$subname.class",
                    "nested/source.java", "nested/Name$1.class", "folder$/Name.class", "after.txt")) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(content);
                zip.closeEntry();
            }
        }

        Path outputDirectory = Files.createDirectory(tempDir.resolve("files"));
        FsService service = mock(FsService.class);
        FsSecurityChecker security = mock(FsSecurityChecker.class);
        when(service.getSecurityChecker()).thenReturn(security);
        when(security.isWritable(eq(service), any())).thenReturn(true);
        FsItemEx source = mock(FsItemEx.class);

        try (MockedStatic<Tools> tools = mockStatic(Tools.class);
             MockedStatic<Adminlog> audit = mockStatic(Adminlog.class)) {
            tools.when(() -> Tools.getRealPath(anyString())).thenAnswer(call ->
                tempDir.resolve(((String) call.getArgument(0)).replaceFirst("^/+", "")).toString());
            tools.when(() -> Tools.replace(anyString(), anyString(), anyString())).thenAnswer(call ->
                ((String) call.getArgument(0)).replace((String) call.getArgument(1), (String) call.getArgument(2)));

            IwcmFsVolume volume = new IwcmFsVolume("root", new IwcmFile(tempDir.toString()));
            FsItemEx parent = new FsItemEx(volume.fromPath("/files"), service);
            when(source.getParent()).thenReturn(parent);
            ExtractCommandExecutor executor = new ExtractCommandExecutor();

            assertTrue(executor.areAllExtractEntriesWritable("/import.zip", parent));
            List<FsItemEx> added = executor.unZipFile("/import.zip", "/files", source);
            assertNotNull(added);
            for (String name : allowedNames) {
                assertArrayEquals(content, Files.readAllBytes(outputDirectory.resolve(name)), name);
            }
            for (String name : skippedNames) {
                assertFalse(Files.exists(outputDirectory.resolve(name)), name);
            }
            for (FsItemEx item : added) {
                assertNotNull(item.getPath());
                assertFalse(skippedNames.contains(item.getName()));
            }
            try (var files = Files.walk(outputDirectory)) {
                assertEquals(allowedNames.stream().sorted().toList(), files.filter(Files::isRegularFile)
                    .map(path -> outputDirectory.relativize(path).toString().replace('\\', '/')).sorted().toList());
            }
        }
    }
}
