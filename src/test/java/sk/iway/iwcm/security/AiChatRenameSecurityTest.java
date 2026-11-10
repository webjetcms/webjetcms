package sk.iway.iwcm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.filebrowser.FbrowserController;
import sk.iway.iwcm.system.elfinder.FilePropertiesDTO;
import sk.iway.iwcm.system.elfinder.FilePropertiesRestController;

/** Verifies generic renames and the private-path restriction at the file-properties endpoint. */
class AiChatRenameSecurityTest {

    private static final String CHAT_ROOT = "/files/protected/ai-chat";
    private static final String HISTORY = CHAT_ROOT + "/7/42/934be8ec-6b59-4cbe-9575-a1a69f943dea/ai_chat.json";

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
        Files.createDirectories(path(HISTORY).getParent());
        Files.writeString(path(HISTORY), "private history");
    }

    @AfterEach
    void tearDown() {
        Constants.setServletContext(originalContext);
    }

    /** Internal rename operations can work with chat files without feature-specific restrictions. */
    @Test
    void genericRenameCanTargetChatStorage() throws IOException {
        Path source = path("/files/protected/document.txt");
        Files.writeString(source, "ordinary file");
        assertTrue(Tools.renameFile(source.toString(), path(HISTORY).toString()));
        assertFalse(Files.exists(source));
        assertEquals("ordinary file", Files.readString(path(HISTORY)));
    }

    /** Keeps ordinary sibling files renameable. */
    @Test
    void renamesOrdinaryFiles() throws IOException {
        Path file = path("/files/protected/document.txt");
        Path renamedFile = path("/files/protected/renamed.txt");
        Files.writeString(file, "ordinary file");
        assertTrue(Tools.renameFile(file.toString(), renamedFile.toString()));
        assertFalse(Files.exists(file));
        assertEquals("ordinary file", Files.readString(renamedFile));
    }

    /** Rejects a directly submitted chat filename before audit or metadata updates. */
    @Test
    void legacyPropertiesRejectPrivateRenameWithoutSideEffects() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/admin/fbrowser/fileprop/");
        request.setParameter("prop", "yes");
        Identity user = new Identity();
        user.setAdmin(true);
        request.getSession().setAttribute(Constants.USER_KEY, user);
        EditForm form = new EditForm();
        form.setDir(CHAT_ROOT + "/7/42/934be8ec-6b59-4cbe-9575-a1a69f943dea");
        form.setOrigFile("ai_chat.json");
        form.setFile("renamed.json");

        try (var audit = mockStatic(Adminlog.class); var documents = mockStatic(DocDB.class);
             var database = mockStatic(DBPool.class)) {
            assertEquals("redirect:/403", new FbrowserController().executeSave(form, request, new MockHttpServletResponse()));
            audit.verifyNoInteractions();
            documents.verifyNoInteractions();
            database.verifyNoInteractions();
        }

        assertEquals("private history", Files.readString(path(HISTORY)));
        assertFalse(Files.exists(path(form.getDir() + "/renamed.json")));
    }

    private Path path(String virtualPath) {
        return root.resolve(virtualPath.substring(1));
    }

    /** The modern properties endpoint rejects a directly submitted chat path before saving. */
    @Test
    void propertiesEndpointRejectsChatPaths() {
        FilePropertiesDTO entity = new FilePropertiesDTO();
        entity.setDirPath(CHAT_ROOT + "/7/42/934be8ec-6b59-4cbe-9575-a1a69f943dea");
        entity.setOriginalFileName("ai_chat.json");
        entity.setFileName("renamed.json");
        var controller = new FilePropertiesRestController(new MockHttpServletResponse());
        var error = org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class, () -> controller.beforeSave(entity));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }
}
