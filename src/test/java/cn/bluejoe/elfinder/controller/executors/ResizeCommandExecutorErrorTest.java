package cn.bluejoe.elfinder.controller.executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.ServletContext;
import org.json.JSONObject;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.util.FsServiceUtils;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies that failed image operations return errors instead of a changed-file response. */
class ResizeCommandExecutorErrorTest extends BaseWebjetTest {
    @TempDir
    Path root;

    /** Real processing failures must preserve the input and reach the shared result-code translation. */
    @ParameterizedTest
    @CsvSource({"png,resize", "png,crop", "png,rotate", "avif,resize"})
    void failedImageOperationReportsError(String extension, String mode) throws Exception {
        root = root.toRealPath();
        ServletContext previousContext = Constants.getServletContext();
        String previousCommand = Constants.getString("imageMagickDir");
        String virtualPath = "/images/broken." + extension;
        Path image = root.resolve(virtualPath.substring(1));
        Files.createDirectories(image.getParent());
        byte[] invalid = {0, 1, 2, 3};
        Files.write(image, invalid);
        FsService service = mock(FsService.class);
        FsItemEx item = mock(FsItemEx.class);
        when(item.isWritable(item)).thenReturn(true);
        when(item.getPath()).thenReturn(virtualPath);
        when(item.getVolumeId()).thenReturn("test");

        try (MockedStatic<FsServiceUtils> lookup = mockStatic(FsServiceUtils.class)) {
            lookup.when(() -> FsServiceUtils.findItem(service, "image")).thenReturn(item);
            Constants.setServletContext(new MockServletContext() {
                @Override
                public String getRealPath(String path) {
                    return root.resolve(path.replaceFirst("^/", "")).toString() + ("/".equals(path) ? "/" : "");
                }
            });
            Constants.setString("imageMagickDir", root.resolve("missing").toString());
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addParameter("target", "image");
            request.addParameter("mode", mode);
            request.addParameter("width", "30");
            request.addParameter("height", "20");
            request.addParameter("degree", "90");
            JSONObject response = new JSONObject();

            new ResizeCommandExecutor().execute(service, request, Constants.getServletContext(), response);

            assertEquals(Prop.getInstance(request).getText("gallery.resizing.error_3"), response.getString("error"));
            assertFalse(response.has("changed"));
            assertArrayEquals(invalid, Files.readAllBytes(image));
        } finally {
            Constants.setServletContext(previousContext);
            Constants.setString("imageMagickDir", previousCommand);
        }
    }
}
