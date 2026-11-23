package sk.iway.iwcm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.filebrowser.MultipleFileUploadAction;
import sk.iway.upload.DiskMultiPartRequestHandler;
import sk.iway.upload.UploadedFile;

/** Verifies that the legacy file-manager upload cannot overwrite private chat history. */
class AiChatUploadSecurityTest {

    @TempDir
    Path root;

    private ServletContext originalContext;

    @BeforeEach
    void setUp() throws IOException {
        root = root.toRealPath();
        originalContext = Constants.getServletContext();
        Constants.setServletContext(new MockServletContext() {
            @Override
            public String getRealPath(String path) {
                return root.resolve(path.substring(1)).toString();
            }
        });
    }

    @AfterEach
    void tearDown() {
        Constants.setServletContext(originalContext);
    }

    @Test
    void rejectsPrivateDestinationBeforeProcessingTheUpload() throws Exception {
        String directory = "/files/protected/ai-chat/7/42/934be8ec-6b59-4cbe-9575-a1a69f943dea";
        Path history = root.resolve(directory.substring(1)).resolve("ai_chat.json");
        Files.createDirectories(history.getParent());
        Files.writeString(history, "private history");

        var request = new MockHttpServletRequest("POST", "/admin/multiplefileupload.do");
        var response = new MockHttpServletResponse();
        var admin = new Identity();
        admin.setAdmin(true);
        request.getSession().setAttribute(Constants.USER_KEY, admin);
        request.setParameter("dir", directory);

        var upload = mock(UploadedFile.class);
        when(upload.getFileName()).thenReturn("ai_chat.json");
        when(upload.getFileSize()).thenReturn(10);
        RequestBean original = SetCharacterEncodingFilter.getCurrentRequestBean();
        try (var multipart = mockConstruction(DiskMultiPartRequestHandler.class, (handler, context) -> {
            when(handler.handleRequest(request)).thenReturn(request);
            when(handler.getFileElements()).thenReturn(Map.of("file", upload));
        })) {
            SetCharacterEncodingFilter.setCurrentRequestBean(new RequestBean());
            new MultipleFileUploadAction().execute(request, response);
        } finally {
            SetCharacterEncodingFilter.setCurrentRequestBean(original);
        }

        assertEquals("Upload error", response.getContentAsString().trim());
        assertEquals("private history", Files.readString(history));
        verify(upload, never()).getInputStream();
    }
}
