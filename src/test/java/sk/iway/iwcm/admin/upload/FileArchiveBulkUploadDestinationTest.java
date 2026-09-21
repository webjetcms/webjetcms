package sk.iway.iwcm.admin.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.file_archiv.FileArchiveRepository;
import sk.iway.iwcm.i18n.Prop;

/**
 * Verifies bulk-upload conflict routing before persistence or file replacement is attempted.
 */
@Execution(ExecutionMode.SAME_THREAD)
class FileArchiveBulkUploadDestinationTest {

    private static final String SELECTED_FOLDER = "/custom/archive/reports/";
    private static final String CATEGORY_FOLDER = "/custom/archive/finance/";
    private static final String FILE_NAME = "report.pdf";
    private static final String FILE_KEY = "bulk-upload-key";

    @TempDir
    Path temporaryDirectory;

    private String originalArchiveRoot;
    private boolean originalCategoryAsLink;
    private Identity user;
    private Prop prop;
    private FileArchiveRepository repository;
    private MockedStatic<Tools> tools;
    private MockedStatic<AdminUploadServlet> uploads;

    @BeforeEach
    void setUp() {
        originalArchiveRoot = Constants.getString("fileArchivDefaultDirPath");
        originalCategoryAsLink = Constants.getBoolean("fileArchivUseCategoryAsLink");
        Constants.setString("fileArchivDefaultDirPath", "custom/archive/");
        Constants.setBoolean("fileArchivUseCategoryAsLink", true);

        repository = mock(FileArchiveRepository.class);
        user = mock(Identity.class);
        prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(user.isFolderWritable(SELECTED_FOLDER)).thenReturn(true);
        when(user.isFolderWritable(CATEGORY_FOLDER)).thenReturn(true);
        when(repository.findIdByFilePathAndFileName(eq("custom/archive/reports/"), eq(FILE_NAME), anyInt()))
            .thenReturn(Optional.of(1L));
        when(repository.findIdByFilePathAndFileName(eq("custom/archive/finance/"), eq(FILE_NAME), anyInt()))
            .thenReturn(Optional.of(2L));

        tools = mockStatic(Tools.class, invocation -> {
            if ("getRealPath".equals(invocation.getMethod().getName())) {
                return temporaryDirectory.resolve(invocation.<String>getArgument(0).replaceFirst("^/", "")).toString();
            }
            return invocation.callRealMethod();
        });
        tools.when(() -> Tools.getSpringBean("fileArchiveRepository", FileArchiveRepository.class)).thenReturn(repository);
        uploads = mockStatic(AdminUploadServlet.class);
        uploads.when(() -> AdminUploadServlet.getTempFilePath(FILE_KEY))
            .thenReturn(temporaryDirectory.resolve(FILE_NAME).toString());
        uploads.when(() -> AdminUploadServlet.getOriginalFileName(FILE_KEY)).thenReturn(FILE_NAME);
    }

    @AfterEach
    void tearDown() {
        if (uploads != null) uploads.close();
        if (tools != null) tools.close();
        Constants.setString("fileArchivDefaultDirPath", originalArchiveRoot);
        Constants.setBoolean("fileArchivUseCategoryAsLink", originalCategoryAsLink);
    }

    /**
     * Both immediate and scheduled uploads must resolve conflicts in the final category folder.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void findsCategoryConflictAndPreservesTemporaryUpload(boolean saveLater) {
        JSONObject output = upload("Finance", saveLater);

        assertTrue(output.getBoolean("exists"));
        assertEquals(CATEGORY_FOLDER, output.getString("destinationFolder"));
        verify(repository).findIdByFilePathAndFileName(eq("custom/archive/finance/"), eq(FILE_NAME), anyInt());
        verify(repository, never()).findIdByFilePathAndFileName(eq("custom/archive/reports/"), anyString(), anyInt());
        uploads.verify(() -> AdminUploadServlet.deleteTempFile(FILE_KEY), never());
    }

    /**
     * Categories remain metadata when category-based destinations are disabled.
     */
    @Test
    void usesSelectedFolderWhenCategoryRoutingIsDisabled() {
        Constants.setBoolean("fileArchivUseCategoryAsLink", false);
        JSONObject output = upload("Finance", false);

        assertTrue(output.getBoolean("exists"));
        assertEquals(SELECTED_FOLDER, output.getString("destinationFolder"));
        verify(repository, never()).findIdByFilePathAndFileName(eq("custom/archive/finance/"), anyString(), anyInt());
    }

    /**
     * An omitted category retains the selected destination even when category routing is enabled.
     */
    @Test
    void usesSelectedFolderWhenCategoryIsOmitted() {
        JSONObject output = upload(null, false);

        assertTrue(output.getBoolean("exists"));
        assertEquals(SELECTED_FOLDER, output.getString("destinationFolder"));
    }

    /**
     * A same-name document in the selected folder must not block creation in another category folder.
     */
    @Test
    void ignoresConflictOutsideEffectiveDestination() {
        when(repository.findIdByFilePathAndFileName(eq("custom/archive/finance/"), eq(FILE_NAME), anyInt()))
            .thenReturn(Optional.empty());
        try (MockedStatic<FileArchiveUploadService> service = mockStatic(FileArchiveUploadService.class, invocation -> {
            if ("validateAndSaveArchiveEntity".equals(invocation.getMethod().getName())) return false;
            return invocation.callRealMethod();
        })) {
            JSONObject output = upload("Finance", false);

            assertFalse(output.getBoolean("exists"));
            assertEquals(CATEGORY_FOLDER, output.getString("destinationFolder"));
            service.verify(() -> FileArchiveUploadService.validateAndSaveArchiveEntity(eq(user), eq(prop), any(), eq(repository), eq(output)));
            verify(repository, never()).findIdByFilePathAndFileName(eq("custom/archive/reports/"), anyString(), anyInt());
        }
    }

    /**
     * Access to the selected folder does not authorize conflict lookup in a restricted category folder.
     */
    @Test
    void rejectsUnwritableCategoryBeforeConflictLookup() {
        when(user.isFolderWritable(CATEGORY_FOLDER)).thenReturn(false);
        JSONObject output = upload("Finance", false);

        assertFalse(output.getBoolean("success"));
        assertEquals("admin.upload_iframe.wrong_upload_dir", output.getString("error"));
        verifyNoInteractions(repository);
        uploads.verify(() -> AdminUploadServlet.deleteTempFile(FILE_KEY));
    }

    private JSONObject upload(String category, boolean saveLater) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (category != null) request.addParameter("category", category);
        if (saveLater) {
            request.addParameter("editorFields.saveLater", "true");
            request.addParameter("editorFields.dateUploadLater", Long.toString(System.currentTimeMillis() + 3_600_000L));
            request.addParameter("editorFields.emails", "editor@example.com");
        }
        JSONObject output = new JSONObject().put("success", true).put("exists", false).put("destinationFolder", SELECTED_FOLDER);
        FileArchiveUploadService.saveNewArchiveFile(user, prop, SELECTED_FOLDER, FILE_NAME, FILE_NAME, FILE_KEY,
            FileArchiveBulkUploadOptions.fromRequest(request), output);
        return output;
    }
}
