package sk.iway.iwcm.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.Column;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.test.BaseWebjetTest;

@Execution(ExecutionMode.SAME_THREAD)
class FileHistoryDBTest extends BaseWebjetTest {

    private static final int HISTORY_ID = 987654321;
    private static final String FILE_URL = "/images/allowed/file.txt";

    @Test
    void historyBeanMapsExistingDomainColumnAndDefaultsToDomainOne() throws Exception {
        Field domainId = FileHistoryBean.class.getDeclaredField("domainId");

        assertEquals("domain_id", domainId.getAnnotation(Column.class).name());
        assertEquals(1, new FileHistoryBean().getDomainId());
    }

    @Test
    void saveFileHistoryAssignsCurrentServerDomain() {
        boolean originalVersioning = Constants.getBoolean("iwfs_useVersioning");
        AtomicReference<FileHistoryBean> savedHistory = new AtomicReference<>();
        IwcmFile source = mock(IwcmFile.class);
        when(source.getVirtualPath()).thenReturn("/images/file-history-domain-test.jsp");

        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
                MockedConstruction<FileHistoryDB> databases = mockConstruction(FileHistoryDB.class,
                    (database, context) -> when(database.save(any(FileHistoryBean.class))).thenAnswer(invocation -> {
                        savedHistory.set(invocation.getArgument(0));
                        return true;
                    }))) {
            Constants.setBoolean("iwfs_useVersioning", true);
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(42);

            assertTrue(IwcmFsDB.saveFileHistory(source, true));
            assertNotNull(savedHistory.get());
            assertEquals(42, savedHistory.get().getDomainId());
        } finally {
            Constants.setBoolean("iwfs_useVersioning", originalVersioning);
        }
    }

    @Test
    void saveFileHistoryFallsBackToDomainOneWithoutTenantContext() {
        boolean originalVersioning = Constants.getBoolean("iwfs_useVersioning");
        boolean originalExternalFiles = Constants.getBoolean("enableStaticFilesExternalDir");
        AtomicReference<FileHistoryBean> savedHistory = new AtomicReference<>();
        IwcmFile source = mock(IwcmFile.class);
        when(source.getVirtualPath()).thenReturn("/images/file-history-domain-test.jsp");

        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
                MockedStatic<SetCharacterEncodingFilter> requestBeans = mockStatic(SetCharacterEncodingFilter.class);
                MockedConstruction<FileHistoryDB> databases = mockConstruction(FileHistoryDB.class,
                    (database, context) -> when(database.save(any(FileHistoryBean.class))).thenAnswer(invocation -> {
                        savedHistory.set(invocation.getArgument(0));
                        return true;
                    }))) {
            Constants.setBoolean("iwfs_useVersioning", true);
            Constants.setBoolean("enableStaticFilesExternalDir", true);
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(-1);
            requestBeans.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(null);

            assertTrue(IwcmFsDB.saveFileHistory(source, true));
            assertEquals(1, databases.constructed().size());
            assertNotNull(savedHistory.get());
            assertEquals(1, savedHistory.get().getDomainId());
        } finally {
            Constants.setBoolean("enableStaticFilesExternalDir", originalExternalFiles);
            Constants.setBoolean("iwfs_useVersioning", originalVersioning);
        }
    }

    @Test
    void metadataAccessRequiresCurrentDomainAndWritableFolder() {
        int domainId = CloudToolsForCore.getDomainId();

        assertTrue(FileHistoryDB.isFileHistoryAccessible(
            FILE_URL, domainId, createUser("/images/allowed/*")));
        assertFalse(FileHistoryDB.isFileHistoryAccessible(
            FILE_URL, domainId + 1, createUser("/images/allowed/*")));
        assertFalse(FileHistoryDB.isFileHistoryAccessible(
            "/templates/protected.jsp", domainId, createUser("/images/*")));
        assertFalse(FileHistoryDB.isFileHistoryAccessible(
            "/images/allowed/../protected.jsp", domainId, createUser("/images/allowed/*")));

        Identity disabledFileBrowser = createUser("/images/allowed/*");
        disabledFileBrowser.addDisabledItem("menuFbrowser");
        assertFalse(FileHistoryDB.isFileHistoryAccessible(FILE_URL, domainId, disabledFileBrowser));

        Identity nonAdmin = createUser("/images/allowed/*");
        nonAdmin.setAdmin(false);
        assertFalse(FileHistoryDB.isFileHistoryAccessible(FILE_URL, domainId, nonAdmin));
    }

    @Test
    void contentAccessRequiresWritableSourceBelowHistoryRoot() {
        int domainId = CloudToolsForCore.getDomainId();
        Identity user = createUser("/images/allowed/*");

        assertTrue(FileHistoryDB.isFileHistoryContentAccessible(
            FILE_URL, historyPath("images/allowed/"), HISTORY_ID, domainId, user));
        assertFalse(FileHistoryDB.isFileHistoryContentAccessible(
            FILE_URL, historyPath("templates/"), HISTORY_ID, domainId, user));
        assertFalse(FileHistoryDB.isFileHistoryContentAccessible(
            FILE_URL, outsideHistoryPath("images/allowed/"), HISTORY_ID, domainId, user));
        assertFalse(FileHistoryDB.isFileHistoryContentAccessible(
            FILE_URL, null, HISTORY_ID, domainId, user));

        String historyRoot = Constants.getString("fileHistoryPath");
        while (historyRoot.length() > 1 && historyRoot.endsWith("/")) {
            historyRoot = historyRoot.substring(0, historyRoot.length() - 1);
        }
        assertTrue(FileHistoryDB.isFileHistoryContentAccessible(
            "/robots.txt", historyRoot + "//", HISTORY_ID, domainId, createUser("*")));
    }

    @Test
    void canonicalAccessRejectsNestedSymlinks(@TempDir Path tempDirectory) throws IOException {
        String originalBasePath = System.getProperty("webjetTestBasepath");
        String originalHistoryPath = Constants.getString("fileHistoryPath");

        try {
            System.setProperty("webjetTestBasepath", tempDirectory.toString());
            Constants.setString("fileHistoryPath", "/history/");

            Path allowedFolder = tempDirectory.resolve("images/allowed");
            Files.createDirectories(allowedFolder);
            Path protectedImageFolder = tempDirectory.resolve("images/protected");
            Files.createDirectories(protectedImageFolder);
            createSymbolicLinkOrSkip(allowedFolder.resolve("same-root-link"), protectedImageFolder);
            Path protectedTemplateFolder = tempDirectory.resolve("templates/protected");
            Files.createDirectories(protectedTemplateFolder);
            createSymbolicLinkOrSkip(allowedFolder.resolve("outside-root-link"), protectedTemplateFolder);

            int domainId = CloudToolsForCore.getDomainId();
            Identity user = createUser("/images/allowed/*");
            assertFalse(FileHistoryDB.isFileHistoryAccessible(
                "/images/allowed/same-root-link/secret.txt", domainId, user));
            assertFalse(FileHistoryDB.isFileHistoryAccessible(
                "/images/allowed/outside-root-link/secret.txt", domainId, user));

            Path currentFile = allowedFolder.resolve("file.txt");
            Files.write(currentFile, "current".getBytes(StandardCharsets.UTF_8));
            Path outsideHistoryFolder = tempDirectory.resolve("templates/history-outside");
            Files.createDirectories(outsideHistoryFolder);
            Files.write(outsideHistoryFolder.resolve(String.valueOf(HISTORY_ID)),
                "history".getBytes(StandardCharsets.UTF_8));
            Path historyParent = tempDirectory.resolve("history/images");
            Files.createDirectories(historyParent);
            createSymbolicLinkOrSkip(historyParent.resolve("allowed"), outsideHistoryFolder);

            assertFalse(FileHistoryDB.isFileHistoryContentAccessible(
                FILE_URL, "/history/images/allowed/", HISTORY_ID, domainId, user));
        } finally {
            Constants.setString("fileHistoryPath", originalHistoryPath);
            if (originalBasePath == null) System.clearProperty("webjetTestBasepath");
            else System.setProperty("webjetTestBasepath", originalBasePath);
        }
    }

    @Test
    void downloadStreamsAuthorizedHistory(@TempDir Path tempDirectory) throws IOException {
        String originalBasePath = System.getProperty("webjetTestBasepath");
        String originalHistoryPath = Constants.getString("fileHistoryPath");
        byte[] expectedContent = "stored history".getBytes(StandardCharsets.UTF_8);

        try {
            System.setProperty("webjetTestBasepath", tempDirectory.toString());
            Constants.setString("fileHistoryPath", "/history/");
            Path storedFile = tempDirectory.resolve("history/images/allowed/" + HISTORY_ID);
            Files.createDirectories(storedFile.getParent());
            Files.write(storedFile, expectedContent);

            FileHistoryBean history = createHistory(FILE_URL, "/history/images/allowed/");
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (MockedConstruction<FileHistoryDB> databases = mockConstruction(FileHistoryDB.class,
                    (database, context) -> when(database.getById(HISTORY_ID)).thenReturn(history))) {
                assertTrue(FileHistoryDB.sendFileFromHistory(
                    FILE_URL, HISTORY_ID, createUser("/images/allowed/*"), response));
                assertArrayEquals(expectedContent, response.getContentAsByteArray());
                assertEquals("text/plain", response.getContentType());
            }
        } finally {
            Constants.setString("fileHistoryPath", originalHistoryPath);
            if (originalBasePath == null) System.clearProperty("webjetTestBasepath");
            else System.setProperty("webjetTestBasepath", originalBasePath);
        }
    }

    @Test
    void downloadRejectsForeignDomainFolderAndRequestPathBeforeOpeningFile() {
        FileHistoryBean foreignDomain = createHistory(FILE_URL, historyPath("images/allowed/"));
        foreignDomain.setDomainId(CloudToolsForCore.getDomainId() + 1);
        assertDownloadRejected(foreignDomain, FILE_URL, createUser("/images/allowed/*"));

        FileHistoryBean protectedFolder = createHistory(
            "/templates/protected.jsp", historyPath("templates/"));
        assertDownloadRejected(protectedFolder, "/templates/protected.jsp", createUser("/images/*"));

        FileHistoryBean differentPath = createHistory(FILE_URL, historyPath("images/allowed/"));
        assertDownloadRejected(differentPath, "/images/allowed/other.txt", createUser("/images/allowed/*"));
    }

    @SuppressWarnings("deprecation")
    @Test
    void legacyDownloadFailsClosedWithoutRequestContext() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<SetCharacterEncodingFilter> requestBeans = mockStatic(SetCharacterEncodingFilter.class)) {
            requestBeans.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(null);

            assertFalse(FileHistoryDB.sendFileFromHistory(FILE_URL, HISTORY_ID, response));
            assertEquals(0, response.getContentAsByteArray().length);
        }
    }

    private static void assertDownloadRejected(FileHistoryBean history, String requestedPath, Identity user) {
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedConstruction<FileHistoryDB> databases = mockConstruction(FileHistoryDB.class,
                (database, context) -> when(database.getById(HISTORY_ID)).thenReturn(history));
                MockedConstruction<IwcmFile> files = mockConstruction(IwcmFile.class)) {
            assertFalse(FileHistoryDB.sendFileFromHistory(requestedPath, HISTORY_ID, user, response));
            assertTrue(files.constructed().isEmpty());
            assertEquals(0, response.getContentAsByteArray().length);
        }
    }

    private static FileHistoryBean createHistory(String fileUrl, String historyPath) {
        FileHistoryBean history = new FileHistoryBean();
        history.setFileHistoryId(HISTORY_ID);
        history.setFileUrl(fileUrl);
        history.setHistoryPath(historyPath);
        history.setDomainId(CloudToolsForCore.getDomainId());
        return history;
    }

    private static Identity createUser(String writableFolders) {
        Identity user = new Identity();
        user.setAdmin(true);
        user.setWritableFolders(writableFolders);
        return user;
    }

    private static String historyPath(String relativeFolder) {
        String historyRoot = Constants.getString("fileHistoryPath");
        if (historyRoot.endsWith("/") == false) historyRoot += "/";
        return historyRoot + relativeFolder;
    }

    private static String outsideHistoryPath(String relativeFolder) {
        String historyRoot = Constants.getString("fileHistoryPath");
        while (historyRoot.endsWith("/")) historyRoot = historyRoot.substring(0, historyRoot.length() - 1);
        return historyRoot + "-outside/" + relativeFolder;
    }

    private static void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            Assumptions.assumeTrue(false, "Symbolic links are not supported: " + ex.getMessage());
        }
    }
}
