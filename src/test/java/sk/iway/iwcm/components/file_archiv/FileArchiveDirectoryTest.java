package sk.iway.iwcm.components.file_archiv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.admin.upload.AdminUploadServlet;
import sk.iway.iwcm.i18n.Prop;

@Isolated
class FileArchiveDirectoryTest {
    private static final String DIRECTORY = "files/archiv/balik-sluzieb-pre-fyzicke-osoby/";

    @TempDir
    Path webRoot;
    private ServletContext previousContext;
    private final FileArchiveRestController controller = spy(new FileArchiveRestController(mock(FileArchiveRepository.class)));

    @BeforeEach
    void setUp() {
        previousContext = Constants.getServletContext();
        ServletContext context = mock(ServletContext.class);
        when(context.getRealPath("/")).thenReturn(webRoot.toString());
        Constants.setServletContext(context);

        Identity user = mock(Identity.class);
        when(user.isEnabledItem("cmp_file_archiv")).thenReturn(true);
        when(user.isEnabledItem("cmp_fileArchiv_edit_del_rollback")).thenReturn(true);
        doReturn(user).when(controller).getUser();
        doReturn(mock(Prop.class)).when(controller).getProp();
        doReturn(new MockHttpServletRequest()).when(controller).getRequest();
    }

    @AfterEach
    void restoreContext() {
        Constants.setServletContext(previousContext);
    }

    @Test
    void metadataEditKeepsStoredDirectory() {
        FileArchivatorBean stored = spy(new FileArchivatorBean());
        stored.setId(1L);
        stored.setFilePath(DIRECTORY);
        doReturn(true).when(stored).save();
        doReturn(stored).when(controller).getOne(1L);

        FileArchivatorBean submitted = new FileArchivatorBean();
        submitted.setId(stored.getId());
        submitted.setFilePath(DIRECTORY);
        submitted.setEditorFields(new FileArchivatorEditorFields());
        submitted.getEditorFields().setDir(DIRECTORY);

        assertEquals(DIRECTORY, controller.editItem(submitted, 1L).getFilePath());
    }

    @Test
    void newUploadWritesFileInSelectedDirectory() throws IOException {
        Files.createDirectories(webRoot.resolve(DIRECTORY));
        Path source = Files.writeString(webRoot.resolve("upload.tmp"), "archive content");
        FileArchivatorBean upload = spy(new FileArchivatorBean());
        doReturn(true).when(upload).save();
        upload.setEditorFields(new FileArchivatorEditorFields());
        upload.getEditorFields().setFile("upload-key");
        upload.getEditorFields().setDir("/" + DIRECTORY);

        try (var uploads = mockStatic(AdminUploadServlet.class);
             var ids = mockStatic(PkeyGenerator.class);
             var audit = mockStatic(Adminlog.class)) {
            uploads.when(() -> AdminUploadServlet.getTempFilePath("upload-key")).thenReturn(source.toString());
            uploads.when(() -> AdminUploadServlet.getOriginalFileName("upload-key")).thenReturn("price list.pdf");

            controller.insertItem(upload);
        }

        assertEquals(DIRECTORY, FileArchivSupportMethodsService.normalizeToOldPath(upload.getFilePath()));
        assertEquals("archive content", Files.readString(webRoot.resolve(DIRECTORY).resolve("price-list.pdf")));
    }
}
