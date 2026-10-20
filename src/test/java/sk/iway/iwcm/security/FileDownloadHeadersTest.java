package sk.iway.iwcm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.header.writers.CacheControlHeadersWriter;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.MultipartFileSender;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GetProtectedFileServlet;
import sk.iway.iwcm.doc.PlannedAssetPaths;
import sk.iway.iwcm.io.IwcmFile;

/** Protected downloads retain cache protection, access checks and conditional response behavior. */
class FileDownloadHeadersTest {

    private static final String FILE_URL = "/files/protected/document.txt";
    private static final String PLANNED_FOLDER = PlannedAssetPaths.ROOT + "42/files/";
    private static final String PLANNED_FILE_URL = PLANNED_FOLDER + "a+b%20c.txt";
    private static final String CONTENT = "confidential document";

    @TempDir
    Path root;

    private ServletContext originalContext;
    private Path file;

    @BeforeEach
    void setUp() throws Exception {
        originalContext = Constants.getServletContext();
        Constants.setServletContext(new MockServletContext() {
            @Override
            public String getRealPath(String path) {
                return root.resolve(path.substring(1)).toString();
            }
        });
        file = root.resolve(FILE_URL.substring(1));
        Files.createDirectories(file.getParent());
        Files.writeString(file, CONTENT);
        Path plannedFile = root.resolve(PLANNED_FOLDER.substring(1)).resolve("a+b c.txt");
        Files.createDirectories(plannedFile.getParent());
        Files.writeString(plannedFile, CONTENT);
    }

    @AfterEach
    void tearDown() {
        Constants.setServletContext(originalContext);
    }

    /** Ordinary and planned files preserve cache headers for full, range and conditional requests. */
    @ParameterizedTest
    @CsvSource({
        "false, '', '', 200, confidential document",
        "false, Range, bytes=2-5, 206, nfid",
        "false, If-None-Match, document.txt, 304, ''",
        "true, '', '', 200, confidential document",
        "true, Range, bytes=2-5, 206, nfid",
        "true, If-None-Match, a+b c.txt, 304, ''"
    })
    void protectedDownloadsOverrideEarlierCacheHeaders(boolean planned, String header, String value, int status, String body)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", planned ? PLANNED_FILE_URL : FILE_URL);
        if (header.isEmpty() == false) request.addHeader(header, value);
        Identity user = new Identity();
        user.setAdmin(true);
        user.setWritableFolders("/files/");
        request.getSession().setAttribute(Constants.USER_KEY, user);
        request.getSession().setAttribute("afterLogonRedirect", "/original-destination/");

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader("Cache-Control", "max-age=3600");
        response.setHeader("Pragma", "");
        response.setDateHeader("Expires", System.currentTimeMillis() + 3600000);

        try (MockedStatic<DocDB> documents = mockStatic(DocDB.class)) {
            documents.when(DocDB::getInstance).thenReturn(mock(DocDB.class));
            new GetProtectedFileServlet().doGet(request, response);
            if (planned) documents.verifyNoInteractions();
        }

        assertEquals(status, response.getStatus());
        assertEquals(body, response.getContentAsString());
        assertEquals(planned ? "/original-destination/" : FILE_URL, request.getSession().getAttribute("afterLogonRedirect"));
        assertEquals(planned ? "noindex, nofollow, noarchive" : null, response.getHeader("X-Robots-Tag"));
        assertNoCache(response);
    }

    /** Missing planned files and directories return an empty 404 without forwarding to the error page. */
    @ParameterizedTest
    @ValueSource(strings = {"missing.txt", ""})
    void plannedDownloadsRejectMissingFilesAndDirectories(String name) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PLANNED_FOLDER + name);
        Identity user = new Identity();
        user.setAdmin(true);
        user.setWritableFolders("/files/");
        request.getSession().setAttribute(Constants.USER_KEY, user);
        request.getSession().setAttribute("afterLogonRedirect", "/original-destination/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<DocDB> documents = mockStatic(DocDB.class)) {
            new GetProtectedFileServlet().doGet(request, response);
            documents.verifyNoInteractions();
        }

        assertEquals(404, response.getStatus());
        assertEquals("", response.getContentAsString());
        assertNull(response.getForwardedUrl());
        assertEquals("/original-destination/", request.getSession().getAttribute("afterLogonRedirect"));
        assertEquals("noindex, nofollow, noarchive", response.getHeader("X-Robots-Tag"));
        assertNoCache(response);
    }

    /** Unauthorized visitors receive the same private 404 for existing and missing planned files. */
    @ParameterizedTest
    @CsvSource({
        "anonymous, a+b%20c.txt", "anonymous, missing.txt",
        "visitor, a+b%20c.txt", "visitor, missing.txt",
        "restrictedAdmin, a+b%20c.txt", "restrictedAdmin, missing.txt"
    })
    void plannedDownloadsCheckAccessBeforeConditionalResponses(String role, String name) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PLANNED_FOLDER + name);
        request.addHeader("If-None-Match", "*");
        request.addHeader("Range", "bytes=2-5");
        if ("anonymous".equals(role) == false) {
            Identity user = new Identity();
            user.setAdmin("restrictedAdmin".equals(role));
            user.setWritableFolders("/images/other/");
            request.getSession().setAttribute(Constants.USER_KEY, user);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<DocDB> documents = mockStatic(DocDB.class)) {
            new GetProtectedFileServlet().doGet(request, response);
            documents.verifyNoInteractions();
        }

        assertEquals(404, response.getStatus());
        assertEquals("", response.getContentAsString());
        assertNull(response.getForwardedUrl());
        assertNull(response.getHeader("ETag"));
        assertNull(request.getSession().getAttribute("afterLogonRedirect"));
        assertEquals("noindex, nofollow, noarchive", response.getHeader("X-Robots-Tag"));
        assertNoCache(response);
    }

    private static void assertNoCache(MockHttpServletResponse response) {
        assertEquals("no-store, no-cache, must-revalidate, max-age=0", response.getHeader("Cache-Control"));
        assertEquals("no-cache", response.getHeader("Pragma"));
        assertEquals(0, response.getDateHeader("Expires"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "bytes=2-5")
    void springWritesCacheHeadersBeforeSmallDownloadIsCommitted(String range) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/files/document.txt");
        if (range != null) request.addHeader("Range", range);
        CompletingResponse response = new CompletingResponse();
        HeaderWriterFilter filter = new HeaderWriterFilter(List.of(new CacheControlHeadersWriter()));

        filter.doFilter(request, response, (req, res) -> {
            try {
                MultipartFileSender.fromFile(new IwcmFile(file))
                    .with((HttpServletRequest) req)
                    .with((HttpServletResponse) res)
                    .serveResource();
            } catch (Exception ex) {
                throw new jakarta.servlet.ServletException(ex);
            }
        });

        assertEquals(range == null ? 200 : 206, response.getStatus());
        assertEquals(range == null ? CONTENT.length() : 4, response.getContentLengthLong());
        assertEquals("no-cache, no-store, max-age=0, must-revalidate", response.cacheControlAtCompletion);
        assertTrue(response.isCommitted());
    }

    /**
     * Captures cache protection when the last body byte is written. A plain Spring mock
     * does not commit at Content-Length and would also accept headers written too late.
     */
    private static class CompletingResponse extends MockHttpServletResponse {
        private String cacheControlAtCompletion;

        @Override
        public ServletOutputStream getOutputStream() {
            return new DelegatingServletOutputStream(super.getOutputStream()) {
                @Override
                public void write(int value) throws IOException {
                    super.write(value);
                    if (getContentLengthLong() > 0 && getContentAsByteArray().length == getContentLengthLong()) {
                        cacheControlAtCompletion = getHeader("Cache-Control");
                        setCommitted(true);
                    }
                }
            };
        }
    }
}
