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

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.filebrowser.FbrowserController;

/** Verifies that legacy rename operations preserve private chat files and ordinary file management. */
class AiChatRenameSecurityTest {

    private static final String CHAT_ROOT = "/files/protected/ai-chat";
    private static final String HISTORY = CHAT_ROOT + "/42/934be8ec-6b59-4cbe-9575-a1a69f943dea/ai_chat.json";

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

    /** Rejects moving an ordinary file into private storage without changing either file. */
    @Test
    void rejectsPrivateDestination() throws IOException {
        Path source = path("/files/protected/document.txt");
        Files.writeString(source, "ordinary file");
        assertFalse(Tools.renameFile(source.toString(), path(HISTORY).toString()));
        assertEquals("ordinary file", Files.readString(source));
        assertEquals("private history", Files.readString(path(HISTORY)));
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

    /** Rejects renaming a parent of private chats before audit or metadata updates. */
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
        form.setDir("/files");
        form.setOrigFile("protected");
        form.setFile("protected-copy");

        try (var audit = mockStatic(Adminlog.class); var documents = mockStatic(DocDB.class);
             var database = mockStatic(DBPool.class)) {
            assertEquals("redirect:/403", new FbrowserController().executeSave(form, request, new MockHttpServletResponse()));
            audit.verifyNoInteractions();
            documents.verifyNoInteractions();
            database.verifyNoInteractions();
        }

        assertEquals("private history", Files.readString(path(HISTORY)));
        assertFalse(Files.exists(path("/files/protected-copy")));
    }

    private Path path(String virtualPath) {
        return root.resolve(virtualPath.substring(1));
    }
}
