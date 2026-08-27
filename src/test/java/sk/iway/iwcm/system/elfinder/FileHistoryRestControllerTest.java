package sk.iway.iwcm.system.elfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.datatable.DatatableResponse;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.test.BaseWebjetTest;

@Execution(ExecutionMode.SAME_THREAD)
class FileHistoryRestControllerTest extends BaseWebjetTest {

    private static final long HISTORY_ID = 987654321L;
    private static final String MISSING_FOLDER = "file-history-security-test-does-not-exist/";

    @ParameterizedTest
    @MethodSource("deniedRollbacks")
    void rollbackRejectsPathsOutsideWritableFolders(String fileUrl, String historyPath, String writableFolders) {
        FileHistoryEntity entity = createEntity(fileUrl, historyPath);
        FileHistoryRestController controller = createController(entity, writableFolders);

        assertActionNotification(controller, "user.rights.no_folder_rights");
    }

    @ParameterizedTest
    @MethodSource("allowedRollbacks")
    void rollbackAcceptsAuthorizedPaths(String fileUrl, String historyPath, String writableFolders) {
        FileHistoryEntity entity = createEntity(fileUrl, historyPath);
        FileHistoryRestController controller = createController(entity, writableFolders);

        assertActionNotification(controller, "elfinder.file_prop.rollback.src_file_not_found.err");
    }

    @Test
    void deniedRollbackDoesNotCopyFile() {
        FileHistoryEntity entity = createEntity(
            "/templates/" + MISSING_FOLDER + "shell.jsp",
            historyPath("images/" + MISSING_FOLDER));
        FileHistoryRestController controller = createController(entity, "/images/*");

        try (MockedStatic<FileTools> fileTools = mockStatic(FileTools.class)) {
            fileTools.when(() -> FileTools.symlinkReplaceToRootPath(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            assertActionNotification(controller, "user.rights.no_folder_rights");

            fileTools.verify(() -> FileTools.copyFile(any(IwcmFile.class), any(IwcmFile.class)), never());
        }
    }

    @Test
    void authorizedRollbackCopiesHistoryFile() {
        FileHistoryEntity entity = createEntity(
            "/images/" + MISSING_FOLDER + "file.txt",
            historyPath("images/" + MISSING_FOLDER));
        FileHistoryRestController controller = createController(entity, "/images/*");
        IwcmFile historyFolder = mock(IwcmFile.class);
        IwcmFile currentFolder = mock(IwcmFile.class);
        when(historyFolder.getVirtualPath()).thenReturn(removeTrailingSlash(entity.getHistoryPath()));
        when(currentFolder.getVirtualPath()).thenReturn("/images/" + removeTrailingSlash(MISSING_FOLDER));

        try (MockedConstruction<IwcmFile> files = mockConstruction(IwcmFile.class,
                (file, context) -> {
                    when(file.exists()).thenReturn(true);
                    if (context.getCount() == 1) {
                        when(file.getParentFile()).thenReturn(historyFolder);
                    } else if (context.getCount() == 2) {
                        when(file.getParentFile()).thenReturn(currentFolder);
                    } else {
                        when(file.getVirtualPath()).thenReturn(removeTrailingSlash(Constants.getString("fileHistoryPath")));
                    }
                });
                MockedStatic<FileTools> fileTools = mockStatic(FileTools.class)) {
            fileTools.when(() -> FileTools.symlinkReplaceToRootPath(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
            fileTools.when(() -> FileTools.copyFile(any(IwcmFile.class), any(IwcmFile.class))).thenReturn(true);

            assertActionNotification(controller, "elfinder.file_prop.rollback.success", "success");

            assertEquals(3, files.constructed().size());
            fileTools.verify(() -> FileTools.copyFile(files.constructed().get(0), files.constructed().get(1)));
        }
    }

    @Test
    void restEndpointsCannotCreateOrEditHistoryRows() {
        FileHistoryEntity entity = createEntity(
            "/images/" + MISSING_FOLDER + "file.txt",
            historyPath("images/" + MISSING_FOLDER));
        FileHistoryRestController controller = createController(entity, "/images/*");
        String expectedMessage = controller.getProp().getText("datatables.error.recordIsNotEditable");

        RuntimeException insertException = assertThrows(RuntimeException.class,
            () -> controller.insertItem(entity));
        RuntimeException editException = assertThrows(RuntimeException.class,
            () -> controller.editItem(entity, HISTORY_ID));

        assertEquals(expectedMessage, insertException.getMessage());
        assertEquals(expectedMessage, editException.getMessage());
    }

    private static Stream<Arguments> deniedRollbacks() {
        return Stream.of(
            Arguments.of(
                "/templates/" + MISSING_FOLDER + "shell.jsp",
                historyPath("images/" + MISSING_FOLDER),
                "/images/*"),
            Arguments.of(
                "/images/" + MISSING_FOLDER + "file.txt",
                historyPath("templates/" + MISSING_FOLDER),
                "/images/*"),
            Arguments.of(
                "/images/../templates/" + MISSING_FOLDER + "shell.jsp",
                historyPath("images/" + MISSING_FOLDER),
                "/images/*"),
            Arguments.of(
                "/images/" + MISSING_FOLDER + "file.txt",
                historyPath("images/../templates/" + MISSING_FOLDER),
                "/images/*"),
            Arguments.of(
                "/images/" + MISSING_FOLDER + "file.txt",
                "/images/" + MISSING_FOLDER,
                "/images/*"),
            Arguments.of(
                "/images\\" + MISSING_FOLDER + "file.txt",
                historyPath("images/" + MISSING_FOLDER),
                "/images/*"),
            Arguments.of(
                "/images/" + MISSING_FOLDER + "file.txt",
                outsideHistoryPath("images/" + MISSING_FOLDER),
                "/images/*"));
    }

    private static Stream<Arguments> allowedRollbacks() {
        return Stream.of(
            Arguments.of(
                "/images/" + MISSING_FOLDER + "file.txt",
                historyPath("images/" + MISSING_FOLDER),
                "/images/*"),
            Arguments.of(
                "/images/" + MISSING_FOLDER + "file.txt",
                historyPath("images/" + MISSING_FOLDER),
                ""),
            Arguments.of(
                "/templates/" + MISSING_FOLDER + "template.jsp",
                historyPath("templates/" + MISSING_FOLDER),
                ""),
            Arguments.of(
                "/templates/" + MISSING_FOLDER + "template.jsp",
                historyPath("templates/" + MISSING_FOLDER),
                "*"));
    }

    private static FileHistoryEntity createEntity(String fileUrl, String historyPath) {
        FileHistoryEntity entity = new FileHistoryEntity();
        entity.setId(HISTORY_ID);
        entity.setFileUrl(fileUrl);
        entity.setHistoryPath(historyPath);
        return entity;
    }

    private static FileHistoryRestController createController(FileHistoryEntity entity, String writableFolders) {
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        FileHistoryRestController controller = new FileHistoryRestController(null) {
            @Override
            public FileHistoryEntity getOneItem(long id) {
                return entity;
            }

            @Override
            public Prop getProp() {
                return prop;
            }
        };

        Identity user = new Identity();
        user.setWritableFolders(writableFolders);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/rest/elfinder/file-history/action/rollBack");
        request.getSession().setAttribute(Constants.USER_KEY, user);
        controller.setRequest(request);
        return controller;
    }

    private static void assertActionNotification(FileHistoryRestController controller, String expectedTextKey) {
        assertActionNotification(controller, expectedTextKey, "error");
    }

    private static void assertActionNotification(FileHistoryRestController controller, String expectedTextKey, String expectedType) {
        DatatableResponse<FileHistoryEntity> response = controller.action("rollBack", new Long[] { HISTORY_ID }).getBody();

        assertNotNull(response);
        assertNotNull(response.getNotify());
        assertEquals(1, response.getNotify().size());
        NotifyBean notify = response.getNotify().get(0);
        assertEquals(expectedType, notify.getType());
        assertEquals(controller.getProp().getText(expectedTextKey), notify.getText());
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

    private static String removeTrailingSlash(String path) {
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
