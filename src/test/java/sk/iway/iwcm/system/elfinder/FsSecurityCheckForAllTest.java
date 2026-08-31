package sk.iway.iwcm.system.elfinder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
class FsSecurityCheckForAllTest extends BaseWebjetTest {

    private static final String ARCHIVE_PATH_CONSTANT = "fileArchivDefaultDirPath";

    private String originalArchivePath;

    @BeforeEach
    void setUpArchivePath() {
        originalArchivePath = Constants.getString(ARCHIVE_PATH_CONSTANT);
        Constants.setString(ARCHIVE_PATH_CONSTANT, "files/archiv/");
    }

    @AfterEach
    void restoreArchivePath() {
        Constants.setString(ARCHIVE_PATH_CONSTANT, originalArchivePath);
    }

    @ParameterizedTest
    @ValueSource(ints = {FsService.TYPE_LINK, FsService.TYPE_IMAGES, FsService.TYPE_MULTIMEDIA, FsService.TYPE_VIDEOS})
    void shouldMakeArchiveReadOnlyInSelectionDialogs(int selectedType) throws Exception {
        FsSecurityCheckForAll security = writableSecurity();
        FsService fsService = fsService(selectedType);

        assertFalse(security.isWritable(fsService, item("/files/archiv")));
        assertTrue(security.isLocked(fsService, item("/files/archiv")));
        assertFalse(security.isWritable(fsService, item("/files/archiv/contracts/document.pdf")));
        assertTrue(security.isLocked(fsService, item("/files/archiv/contracts/document.pdf")));
        assertFalse(security.isWritable(fsService, item("/files/ARCHIV/contracts/document.pdf")));
        assertFalse(security.isWritable(fsService, item("/files/archiv./contracts/document.pdf")));
        assertFalse(security.isWritable(fsService, item("/files/archiv /contracts/document.pdf")));
    }

    @ParameterizedTest
    @ValueSource(ints = {FsService.TYPE_ALL, FsService.TYPE_FILES})
    void shouldKeepArchiveWritableInExplorer(int selectedType) throws Exception {
        FsSecurityCheckForAll security = writableSecurity();
        IwcmFsItem archiveItem = item("/files/archiv/document.pdf");

        assertTrue(security.isWritable(fsService(selectedType), archiveItem));
        assertFalse(security.isLocked(fsService(selectedType), archiveItem));
    }

    @Test
    void shouldMatchConfiguredArchivePathWithoutMatchingSimilarPrefix() throws Exception {
        Constants.setString(ARCHIVE_PATH_CONSTANT, "/custom/documents");
        FsSecurityCheckForAll security = writableSecurity();
        FsService fsService = fsService(FsService.TYPE_LINK);

        assertFalse(security.isWritable(fsService, item("/custom/documents/report.pdf")));
        assertTrue(security.isWritable(fsService, item("/custom/documents-old/report.pdf")));
    }

    private FsSecurityCheckForAll writableSecurity() {
        FsSecurityCheckForAll security = new FsSecurityCheckForAll();
        security.setWritable(true);
        return security;
    }

    private FsService fsService(int selectedType) {
        FsService fsService = mock(FsService.class);
        when(fsService.getSelectedType()).thenReturn(selectedType);
        return fsService;
    }

    private IwcmFsItem item(String virtualPath) {
        IwcmFile file = mock(IwcmFile.class);
        when(file.getVirtualPath()).thenReturn(virtualPath);
        return new IwcmFsItem(mock(IwcmFsVolume.class), file);
    }
}
