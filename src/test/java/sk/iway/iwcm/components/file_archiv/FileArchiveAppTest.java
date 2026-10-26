package sk.iway.iwcm.components.file_archiv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies document ordering through the public application handler and editor options. */
class FileArchiveAppTest extends BaseWebjetTest {

    private FileArchiveRepository repository;
    private FileArchiveApp app;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        repository = mock(FileArchiveRepository.class);
        app = new FileArchiveApp(repository);
        app.setDir("/files/archiv/");
        request = new MockHttpServletRequest();
    }

    /** Folder-based and explicitly selected documents must use every order in both directions. */
    @ParameterizedTest
    @MethodSource("sortCases")
    void sortsMainAndSelectedFiles(String order, List<Long> ascending, List<Long> descending) {
        List<FileArchivatorBean> files = files();
        when(repository.findAll(any(Specification.class))).thenReturn(files);
        when(repository.findAllMainFilesUploadedNotPatternIdsIn(any(), anyInt(), any(Pageable.class))).thenReturn(files);
        app.setGlobalIds("3,1,2");
        app.setOrderMain(order);

        for (boolean selected : new boolean[] {false, true}) {
            app.setShowOnlySelected(selected);
            app.setAscMain(true);
            assertEquals(ascending, ids(render()));
            app.setAscMain(false);
            assertEquals(descending, ids(render()));
        }
        assertEquals(List.of(3L, 1L, 2L), ids(files));
    }

    /** History and patterns use independent directions without mutating cached history. */
    @ParameterizedTest
    @MethodSource("sortCases")
    void sortsHistoryAndPatterns(String order, List<Long> ascending, List<Long> descending) {
        List<FileArchivatorBean> files = files();
        FileArchivatorBean main = file(10, "autotest parent", 10);
        app.setShowOnlySelected(true);
        app.setGlobalIds("10");
        app.setArchiv(true);
        app.setShowPatterns(true);
        app.setOrder(order);
        app.setOrderPatterns(order);
        when(repository.findAllMainFilesUploadedNotPatternIdsIn(any(), anyInt(), any(Pageable.class))).thenReturn(List.of(main));
        when(repository.findAll(any(Specification.class))).thenReturn(files);

        try (var history = mockStatic(FileArchivatorDB.class)) {
            history.when(() -> FileArchivatorDB.getByReferenceId(10L)).thenReturn(files);
            app.setAsc(true);
            app.setAscPatterns(false);
            BeanWrapperImpl result = new BeanWrapperImpl(render().get(0));
            assertEquals(ascending, ids((List<?>) result.getPropertyValue("archivFiles")));
            assertEquals(descending, ids((List<?>) result.getPropertyValue("patterns")));

            app.setAsc(false);
            app.setAscPatterns(true);
            result = new BeanWrapperImpl(render().get(0));
            assertEquals(descending, ids((List<?>) result.getPropertyValue("archivFiles")));
            assertEquals(ascending, ids((List<?>) result.getPropertyValue("patterns")));
        }
        assertEquals(List.of(3L, 1L, 2L), ids(files));
    }

    /** Nullable fields sort first ascending and last descending; ties use name and then ID. */
    @ParameterizedTest
    @ValueSource(strings = {"validFrom", "validTo", "productCode", "product", "category", "fileSize"})
    void handlesMissingValuesAndStableTies(String order) {
        FileArchivatorBean missing = file(4, "autotest missing", 20);
        new BeanWrapperImpl(missing).setPropertyValue(order, null);
        List<FileArchivatorBean> files = List.of(
            file(3, "autotest beta", 20), file(2, "autotest ALPHA", 20), missing, file(1, "autotest alpha", 20));
        when(repository.findAll(any(Specification.class))).thenReturn(files);
        app.setOrderMain(order);

        assertEquals(List.of(4L, 1L, 2L, 3L), ids(render()));
        app.setAscMain(false);
        assertEquals(List.of(1L, 2L, 3L, 4L), ids(render()));
    }

    /** Missing and unsupported stored values retain the original upload-date fallback. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown", "dateInsert", "TIME"})
    void preservesDefaultOrder(String order) {
        when(repository.findAll(any(Specification.class))).thenReturn(files());
        app.setOrderMain(order);
        assertEquals(List.of(2L, 3L, 1L), ids(render()));
    }

    /** Accented names follow the shared Slovak collation rather than Unicode code-point order. */
    @Test
    void sortsAccentedNamesAlphabetically() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(
            file(3, "autotest dom", 1), file(2, "autotest čaj", 1), file(1, "autotest cesta", 1)));
        app.setOrderMain("virtual_file_name");
        assertEquals(List.of(1L, 2L, 3L), ids(render()));
        app.setAscMain(false);
        assertEquals(List.of(3L, 2L, 1L), ids(render()));
    }

    /** Sorting must not bypass the existing visibility, pattern and validity filters. */
    @Test
    void keepsMainFileVisibilityFiltering() {
        FileArchivatorBean hidden = file(4, "autotest hidden", 1);
        hidden.setShowFile(false);
        FileArchivatorBean future = file(5, "autotest future", 1);
        future.setValidFrom(new Date(Long.MAX_VALUE));
        FileArchivatorBean pattern = file(6, "autotest pattern", 1);
        pattern.setReferenceToMain("files/archiv/main.pdf");
        when(repository.findAll(any(Specification.class)))
            .thenReturn(List.of(hidden, future, pattern, file(1, "autotest visible", 1)));
        assertEquals(List.of(1L), ids(render()));
    }

    /** The editor offers the same saved values in all selectors and allows merging common options. */
    @Test
    void providesSharedEditorOptions() {
        Map<String, List<OptionDto>> options = app.getAppOptions(null, request);
        List<String> expected = List.of("priority", "time", "validFrom", "validTo", "virtual_file_name",
            "fileName", "productCode", "product", "category", "fileSize");
        for (String field : List.of("orderMain", "order", "orderPatterns")) {
            assertEquals(expected, options.get(field).stream().map(OptionDto::getValue).toList());
        }
        options.putIfAbsent("wrapperClass", List.of(new OptionDto("autotest wrapper", "autotest", null)));
        assertEquals("autotest", options.get("wrapperClass").get(0).getValue());
    }

    private List<?> render() {
        ExtendedModelMap model = new ExtendedModelMap();
        app.view(model, request);
        return (List<?>) model.get("mainFiles");
    }

    private static List<Long> ids(List<?> files) {
        return files.stream().map(file -> ((FileArchivatorBean) file).getId()).toList();
    }

    private static Stream<Arguments> sortCases() {
        return Stream.of("priority", "time", "validFrom", "validTo", "virtual_file_name",
            "fileName", "productCode", "product", "category", "fileSize")
            .map(order -> {
                if ("virtual_file_name".equals(order)) {
                    return Arguments.of(order, List.of(1L, 2L, 3L), List.of(3L, 1L, 2L));
                }
                return Arguments.of(order, List.of(2L, 3L, 1L), List.of(1L, 3L, 2L));
            });
    }

    private static List<FileArchivatorBean> files() {
        return List.of(file(3, "autotest beta", 20), file(1, "autotest alpha", 100), file(2, "autotest ALPHA", 3));
    }

    private static FileArchivatorBean file(long id, String name, int value) {
        FileArchivatorBean file = new FileArchivatorBean();
        file.setId(id);
        file.setVirtualFileName(name);
        file.setFilePath("files/archiv/");
        String text = "beta";
        if (value == 3) text = "Alpha";
        else if (value == 100) text = "gamma";
        file.setFileName("autotest-" + text + ".pdf");
        file.setProductCode(text);
        file.setProduct(text);
        file.setCategory(text);
        file.setDateInsert(new Date(value * 1000L));
        file.setValidFrom(new Date(value * 1000L));
        file.setValidTo(new Date(4102444800000L + value * 1000L));
        file.setPriority(value);
        file.setFileSize((long) value);
        return file;
    }
}
