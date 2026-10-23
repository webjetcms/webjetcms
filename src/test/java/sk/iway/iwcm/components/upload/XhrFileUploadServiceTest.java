package sk.iway.iwcm.components.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockPart;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies default and configured extension restrictions through the XHR upload service. */
class XhrFileUploadServiceTest extends BaseWebjetTest {

    private static final String EXTENSIONS_SETTING = "xhrFileUploadAllowedExtensions";
    private String previousExtensions;
    private boolean hadExtensionsSetting;

    @TempDir
    Path directory;

    @BeforeEach
    void useDefaultExtensions() {
        hadExtensionsSetting = Constants.containsKey(EXTENSIONS_SETTING);
        previousExtensions = Constants.getString(EXTENSIONS_SETTING);
        Constants.deleteConstant(EXTENSIONS_SETTING);
    }

    @AfterEach
    void restoreExtensions() {
        if (hadExtensionsSetting) Constants.setString(EXTENSIONS_SETTING, previousExtensions);
        else Constants.deleteConstant(EXTENSIONS_SETTING);
    }

    /** Every registered image format can be uploaded, including uppercase extensions. */
    @ParameterizedTest
    @MethodSource("sk.iway.iwcm.common.ImageTools#getSupportedImageExtensions")
    void uploadsSupportedImagesByDefault(String extension) throws Exception {
        assertTrue(upload("photo." + extension).isSuccess());
        assertTrue(upload("photo." + extension.toUpperCase(Locale.ROOT)).isSuccess());
    }

    /** Existing document, archive, video and unsupported image attachments remain uploadable. */
    @ParameterizedTest
    @ValueSource(strings = {"doc", "docx", "xls", "xlsx", "xml", "ppt", "pptx", "pdf", "tiff", "psd", "zip", "rar", "mp4"})
    void uploadsExistingAttachmentsByDefault(String extension) throws Exception {
        assertTrue(upload("attachment." + extension).isSuccess());
    }

    /** Explicit configuration replaces the defaults, including the shared image extensions. */
    @Test
    void configuredExtensionsCanExcludeSupportedImages() throws Exception {
        Constants.setString(EXTENSIONS_SETTING, "pdf");
        assertRejected("photo.avif");
        assertRejected("photo.png");
        assertTrue(upload("document.pdf").isSuccess());
    }

    /** Unknown extensions and script filenames remain rejected by the default list. */
    @ParameterizedTest
    @ValueSource(strings = {"photo.jpg.jsp", "photo.unknown", "photo"})
    void rejectsFilesOutsideDefaultExtensions(String fileName) throws Exception {
        assertRejected(fileName);
    }

    /** A configured wildcard still respects the general upload restriction on executable files. */
    @Test
    void wildcardKeepsGeneralUploadRestrictions() throws Exception {
        Constants.setString(EXTENSIONS_SETTING, "*");
        assertTrue(upload("attachment.custom").isSuccess());
        assertRejected("script.jsp");
    }

    private void assertRejected(String fileName) throws Exception {
        XhrFileUploadResponse response = upload(fileName);
        assertFalse(response.isSuccess());
        assertEquals(Prop.getInstance().getText("components.forum.new.upload_not_allowed_filetype"), response.getError());
        assertEquals(fileName, response.getFile());
    }

    private XhrFileUploadResponse upload(String fileName) throws Exception {
        byte[] content = {1, 2, 3};
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addPart(new MockPart("file", fileName, content));
        XhrFileUploadService service = new XhrFileUploadService();
        try (var tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
             var events = mockStatic(WebjetEventPublisher.class)) {
            tools.when(() -> Tools.getRealPath(anyString()))
                .thenAnswer(call -> directory.resolve(call.getArgument(0, String.class)).toString());
            XhrFileUploadResponse response = service.processUpload(request);
            if (response.isSuccess()) {
                assertArrayEquals(content, Files.readAllBytes(Path.of(service.getTempFilePath(response.getKey().get(0)))));
            }
            return response;
        }
    }
}
