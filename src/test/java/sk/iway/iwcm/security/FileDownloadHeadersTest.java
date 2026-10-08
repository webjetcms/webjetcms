package sk.iway.iwcm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import sk.iway.iwcm.io.IwcmFile;

class FileDownloadHeadersTest {

    private static final String FILE_URL = "/files/protected/document.txt";
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
    }

    @AfterEach
    void tearDown() {
        Constants.setServletContext(originalContext);
    }

    @ParameterizedTest
    @CsvSource({
        "'', '', 200, confidential document",
        "Range, bytes=2-5, 206, nfid",
        "If-None-Match, document.txt, 304, ''"
    })
    void protectedDownloadsOverrideEarlierCacheHeaders(String header, String value, int status, String body)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", FILE_URL);
        if (header.isEmpty() == false) request.addHeader(header, value);
        Identity user = new Identity();
        user.setAdmin(true);
        request.getSession().setAttribute(Constants.USER_KEY, user);

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader("Cache-Control", "max-age=3600");
        response.setHeader("Pragma", "");
        response.setDateHeader("Expires", System.currentTimeMillis() + 3600000);

        try (MockedStatic<DocDB> documents = mockStatic(DocDB.class)) {
            documents.when(DocDB::getInstance).thenReturn(mock(DocDB.class));
            new GetProtectedFileServlet().doGet(request, response);
        }

        assertEquals(status, response.getStatus());
        assertEquals(body, response.getContentAsString());
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
                throw new javax.servlet.ServletException(ex);
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
