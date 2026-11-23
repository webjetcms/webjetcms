package sk.iway.iwcm.admin.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;

@Execution(ExecutionMode.SAME_THREAD)
class FileArchiveUploadServiceTest {

    private static final String ERROR_INVALID_FOLDER = "admin.upload_iframe.wrong_upload_dir";
    private static final String VALID_REFERER = "https://example.test/apps/file-archive/admin/";

    private String originalArchiveRoot;
    private Identity user;

    @BeforeEach
    void setUp() {
        originalArchiveRoot = Constants.getString("fileArchivDefaultDirPath");
        Constants.setString("fileArchivDefaultDirPath", "custom/archive/");

        user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isEnabledItem("cmp_file_archiv")).thenReturn(true);
        when(user.isFolderWritable("/custom/archive/reports/")).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        Constants.setString("fileArchivDefaultDirPath", originalArchiveRoot);
    }

    @Test
    void allowsConfiguredRelativeArchiveRootOutsideStandardUploadRoots() {
        assertNull(FileArchiveUploadService.validateArchiveUploadPermission(
            user, "custom/archive/reports", VALID_REFERER));
    }

    @Test
    void rejectsInvalidArchiveDestinationOrPermission() {
        assertEquals(ERROR_INVALID_FOLDER,
            FileArchiveUploadService.validateArchiveUploadPermission(
                user, "/custom/other/", VALID_REFERER));
        assertEquals(ERROR_INVALID_FOLDER,
            FileArchiveUploadService.validateArchiveUploadPermission(
                user, null, VALID_REFERER));

        when(user.isFolderWritable("/custom/archive/reports/")).thenReturn(false);
        assertEquals(ERROR_INVALID_FOLDER,
            FileArchiveUploadService.validateArchiveUploadPermission(
                user, "custom/archive/reports", VALID_REFERER));
    }
}
