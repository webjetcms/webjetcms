package sk.iway.iwcm.system.elfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
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
    void authorizedRollbackCopiesHistoryFile(@TempDir java.nio.file.Path tempDirectory) throws Exception {
        String originalBasePath = System.getProperty("webjetTestBasepath");
        String originalHistoryPath = Constants.getString("fileHistoryPath");

        try {
            System.setProperty("webjetTestBasepath", tempDirectory.toString());
            Constants.setString("fileHistoryPath", "/history/");
            FileHistoryEntity entity = createEntity(
                "/images/" + MISSING_FOLDER + "file.txt",
                historyPath("images/" + MISSING_FOLDER));
            java.nio.file.Path historyFile = tempDirectory.resolve("history/images/" + MISSING_FOLDER + HISTORY_ID);
            java.nio.file.Path currentFile = tempDirectory.resolve("images/" + MISSING_FOLDER + "file.txt");
            Files.createDirectories(historyFile.getParent());
            Files.createDirectories(currentFile.getParent());
            Files.write(historyFile, "history".getBytes(StandardCharsets.UTF_8));
            Files.write(currentFile, "current".getBytes(StandardCharsets.UTF_8));
            String expectedHistoryPath = historyFile.toFile().getCanonicalPath();
            String expectedCurrentPath = currentFile.toFile().getCanonicalPath();
            FileHistoryRestController controller = createController(entity, "/images/*");

            try (MockedStatic<FileTools> fileTools = mockStatic(FileTools.class)) {
                fileTools.when(() -> FileTools.symlinkReplaceToRootPath(anyString()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
                fileTools.when(() -> FileTools.copyFile(any(IwcmFile.class), any(IwcmFile.class))).thenReturn(true);

                assertActionNotification(controller, "elfinder.file_prop.rollback.success", "success");

                fileTools.verify(() -> FileTools.copyFile(
                    argThat((IwcmFile file) -> expectedHistoryPath.equals(file.getCanonicalPath())),
                    argThat((IwcmFile file) -> expectedCurrentPath.equals(file.getCanonicalPath()))));
            }
        } finally {
            Constants.setString("fileHistoryPath", originalHistoryPath);
            if (originalBasePath == null) System.clearProperty("webjetTestBasepath");
            else System.setProperty("webjetTestBasepath", originalBasePath);
        }
    }

    @Test
    void allReturnsOnlyAuthorizedFileHistory() {
        String fileUrl = "/images/" + MISSING_FOLDER + "file.txt";
        FileHistoryEntity entity = createEntity(fileUrl, historyPath("images/" + MISSING_FOLDER));
        FileHistoryRepository repository = mock(FileHistoryRepository.class);
        FileHistoryRestController controller = createController(entity, "/images/*", repository);
        setRequest(controller, "/admin/rest/elfinder/file-history/all", fileUrl);
        Pageable pageable = PageRequest.of(0, 10);
        Page<FileHistoryEntity> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);
        int domainId = CloudToolsForCore.getDomainId();
        when(repository.findAllByFileUrlAndDomainIdOrderByChangeDateDesc(fileUrl, domainId, pageable))
            .thenReturn(expected);

        Page<FileHistoryEntity> actual = controller.getAllItems(pageable);

        assertSame(expected, actual);
        verify(repository).findAllByFileUrlAndDomainIdOrderByChangeDateDesc(fileUrl, domainId, pageable);
    }

    @Test
    void allRejectsFileOutsideWritableFolders() {
        String fileUrl = "/templates/" + MISSING_FOLDER + "template.jsp";
        FileHistoryEntity entity = createEntity(fileUrl, historyPath("templates/" + MISSING_FOLDER));
        FileHistoryRepository repository = mock(FileHistoryRepository.class);
        FileHistoryRestController controller = createController(entity, "/images/*", repository);
        setRequest(controller, "/admin/rest/elfinder/file-history/all", fileUrl);

        Page<FileHistoryEntity> actual = controller.getAllItems(PageRequest.of(0, 10));

        assertTrue(actual.isEmpty());
        verifyNoInteractions(repository);
    }

    @SuppressWarnings("unchecked")
    @Test
    void searchAddsExactFileAndDomainPredicates() {
        String fileUrl = "/images/" + MISSING_FOLDER + "file.txt";
        FileHistoryEntity entity = createEntity(fileUrl, historyPath("images/" + MISSING_FOLDER));
        FileHistoryRestController controller = createController(entity, "/images/*");
        setRequest(controller, "/admin/rest/elfinder/file-history/search", fileUrl);
        Root<FileHistoryEntity> root = mock(Root.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path<Object> fileUrlPath = mock(Path.class);
        Path<Object> domainIdPath = mock(Path.class);
        Predicate filePredicate = mock(Predicate.class);
        Predicate domainPredicate = mock(Predicate.class);
        List<Predicate> predicates = new ArrayList<>();
        int domainId = CloudToolsForCore.getDomainId();
        when(root.get("fileUrl")).thenReturn(fileUrlPath);
        when(root.get("domainId")).thenReturn(domainIdPath);
        when(builder.equal(fileUrlPath, fileUrl)).thenReturn(filePredicate);
        when(builder.equal(domainIdPath, domainId)).thenReturn(domainPredicate);

        controller.addSpecSearch(Collections.emptyMap(), predicates, root, builder);

        assertEquals(2, predicates.size());
        assertSame(filePredicate, predicates.get(0));
        assertSame(domainPredicate, predicates.get(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    void searchRejectsFileOutsideWritableFolders() {
        String fileUrl = "/templates/" + MISSING_FOLDER + "template.jsp";
        FileHistoryEntity entity = createEntity(fileUrl, historyPath("templates/" + MISSING_FOLDER));
        FileHistoryRestController controller = createController(entity, "/images/*");
        setRequest(controller, "/admin/rest/elfinder/file-history/search", fileUrl);
        Root<FileHistoryEntity> root = mock(Root.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Predicate deniedPredicate = mock(Predicate.class);
        List<Predicate> predicates = new ArrayList<>();
        when(builder.disjunction()).thenReturn(deniedPredicate);

        controller.addSpecSearch(Collections.emptyMap(), predicates, root, builder);

        assertEquals(1, predicates.size());
        assertSame(deniedPredicate, predicates.get(0));
        verifyNoInteractions(root);
    }

    @Test
    void getOneReturnsAuthorizedFileHistory() {
        FileHistoryEntity entity = createEntity(
            "/images/" + MISSING_FOLDER + "file.txt",
            historyPath("images/" + MISSING_FOLDER));
        FileHistoryRestController controller = createController(entity, "/images/*");
        setRequest(controller, "/admin/rest/elfinder/file-history/" + HISTORY_ID, null);

        assertSame(entity, controller.getOne(HISTORY_ID));
    }

    @Test
    void getOneRejectsForeignDomain() {
        FileHistoryEntity entity = createEntity(
            "/images/" + MISSING_FOLDER + "file.txt",
            historyPath("images/" + MISSING_FOLDER));
        entity.setDomainId(CloudToolsForCore.getDomainId() + 1);
        FileHistoryRestController controller = createController(entity, "/images/*");
        setRequest(controller, "/admin/rest/elfinder/file-history/" + HISTORY_ID, null);

        assertThrows(ConstraintViolationException.class, () -> controller.getOne(HISTORY_ID));
    }

    @Test
    void getOneRejectsFileOutsideWritableFolders() {
        FileHistoryEntity entity = createEntity(
            "/templates/" + MISSING_FOLDER + "template.jsp",
            historyPath("templates/" + MISSING_FOLDER));
        FileHistoryRestController controller = createController(entity, "/images/*");
        setRequest(controller, "/admin/rest/elfinder/file-history/" + HISTORY_ID, null);

        assertThrows(ConstraintViolationException.class, () -> controller.getOne(HISTORY_ID));
    }

    @Test
    void getOneRejectsMismatchedId() {
        FileHistoryEntity entity = createEntity(
            "/images/" + MISSING_FOLDER + "file.txt",
            historyPath("images/" + MISSING_FOLDER));
        FileHistoryRestController controller = createController(entity, "/images/*");
        long requestedId = HISTORY_ID + 1;
        setRequest(controller, "/admin/rest/elfinder/file-history/" + requestedId, null);

        assertThrows(ConstraintViolationException.class, () -> controller.getOne(requestedId));
    }

    @Test
    void sumAllReturnsEmptyObject() {
        FileHistoryEntity entity = createEntity(
            "/images/" + MISSING_FOLDER + "file.txt",
            historyPath("images/" + MISSING_FOLDER));
        FileHistoryRestController controller = createController(entity, "/images/*");
        setRequest(controller, "/admin/rest/elfinder/file-history/sumAll", null);

        assertEquals("{}", controller.getSum(entity, new String[] { "id", "userId" }));
    }

    @ParameterizedTest(name = "missingDomainId={0}")
    @ValueSource(booleans = { false, true })
    void rollbackRejectsEntityOutsideCurrentDomain(boolean missingDomainId) {
        FileHistoryEntity entity = createEntity(
            "/images/" + MISSING_FOLDER + "file.txt",
            historyPath("images/" + MISSING_FOLDER));
        entity.setDomainId(missingDomainId ? null : CloudToolsForCore.getDomainId() + 1);
        FileHistoryRestController controller = createController(entity, "/images/*");

        try (MockedConstruction<IwcmFile> files = mockConstruction(IwcmFile.class);
                MockedStatic<FileTools> fileTools = mockStatic(FileTools.class)) {
            assertActionNotification(controller, "user.rights.no_folder_rights");

            assertEquals(0, files.constructed().size());
            fileTools.verify(() -> FileTools.copyFile(any(IwcmFile.class), any(IwcmFile.class)), never());
        }
    }

    @Test
    void restEndpointsCannotCreateEditOrDeleteHistoryRows() {
        FileHistoryEntity entity = createEntity(
            "/images/" + MISSING_FOLDER + "file.txt",
            historyPath("images/" + MISSING_FOLDER));
        FileHistoryRepository repository = mock(FileHistoryRepository.class);
        FileHistoryRestController controller = createController(entity, "/images/*", repository);
        String expectedMessage = controller.getProp().getText("datatables.error.recordIsNotEditable");

        RuntimeException insertException = assertThrows(RuntimeException.class,
            () -> controller.insertItem(entity));
        RuntimeException editException = assertThrows(RuntimeException.class,
            () -> controller.editItem(entity, HISTORY_ID));
        RuntimeException deleteException = assertThrows(RuntimeException.class,
            () -> controller.delete(HISTORY_ID, entity));

        assertEquals(expectedMessage, insertException.getMessage());
        assertEquals(expectedMessage, editException.getMessage());
        assertEquals(expectedMessage, deleteException.getMessage());
        verifyNoInteractions(repository);
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
        entity.setDomainId(CloudToolsForCore.getDomainId());
        return entity;
    }

    private static FileHistoryRestController createController(FileHistoryEntity entity, String writableFolders) {
        return createController(entity, writableFolders, null);
    }

    private static FileHistoryRestController createController(FileHistoryEntity entity, String writableFolders, FileHistoryRepository repository) {
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        FileHistoryRestController controller = new FileHistoryRestController(repository) {
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
        user.setAdmin(true);
        user.setWritableFolders(writableFolders);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/rest/elfinder/file-history/action/rollBack");
        request.getSession().setAttribute(Constants.USER_KEY, user);
        controller.setRequest(request);
        return controller;
    }

    private static void setRequest(FileHistoryRestController controller, String requestUri, String filePath) {
        MockHttpServletRequest request = (MockHttpServletRequest) controller.getRequest();
        request.setRequestURI(requestUri);
        if (filePath != null) request.setParameter("filePath", filePath);
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

}
