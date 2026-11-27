package sk.iway.iwcm.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.domainRedirects.DomainRedirectDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GetProtectedFileServlet;
import sk.iway.iwcm.io.FileCache;
import sk.iway.iwcm.io.s3.S3Storage;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies protected-file authorization before direct S3, external-directory and cache serving. */
@Execution(ExecutionMode.SAME_THREAD)
class PathFilterProtectedFilesTest extends BaseWebjetTest {
    private static final String FILE_URL = "/files/protected/users/alice/%C5%BElt%C3%BD%20dokument.png";
    private static final String CONTENT = "confidential document";
    private static final String[] BACKENDS = {"local", "external", "s3", "cache"};

    @TempDir Path root;
    private final Map<String, String> previous = new HashMap<>();
    private final S3Storage storage = mock(S3Storage.class);
    private ServletContext originalContext;
    private Object originalProtectedDirs;
    private MockedStatic<DocDB> documents;
    private MockedStatic<DomainRedirectDB> redirects;
    private MockedStatic<S3Storage> s3;
    private MockedStatic<FileCache> cache;

    @BeforeEach
    void configure() throws Exception {
        for (String name : new String[] {"s3Enabled", "s3UseForDirs", "multiDomainEnabled", "cloudStaticFilesDir",
                "enableStaticFilesExternalDir", "cacheStaticContentForAdmin", "fileAccessDeniedDocId"}) {
            previous.put(name, Constants.getString(name));
        }
        Constants.setBoolean("multiDomainEnabled", false);
        Constants.setString("s3UseForDirs", "/files");
        Constants.setBoolean("cacheStaticContentForAdmin", true);
        Constants.setInt("fileAccessDeniedDocId", 0);
        originalContext = Constants.getServletContext();
        Constants.setServletContext(new MockServletContext() {
            @Override
            public String getRealPath(String path) { return root.resolve(path.substring(1)).toString(); }
        });
        originalProtectedDirs = ReflectionTestUtils.getField(PathFilter.class, "passwordProtected");
        ReflectionTestUtils.setField(PathFilter.class, "passwordProtected", new HashMap<>());
        documents = mockStatic(DocDB.class);
        documents.when(DocDB::getInstance).thenReturn(mock(DocDB.class));
        redirects = mockStatic(DomainRedirectDB.class);
        s3 = mockStatic(S3Storage.class);
        s3.when(S3Storage::getInstance).thenReturn(storage);
        cache = mockStatic(FileCache.class);
        cache.when(() -> FileCache.getObject(anyString())).thenReturn(CONTENT.getBytes(StandardCharsets.UTF_8));
        String key = "defaulthost" + Tools.URLDecode(FILE_URL);
        when(storage.metadata(key)).thenReturn(new S3Storage.Metadata(key, false, CONTENT.length(), 1000, "test-etag"));
        when(storage.open(key, 0, (long) CONTENT.length()))
                .thenAnswer(invocation -> new ByteArrayInputStream(CONTENT.getBytes(StandardCharsets.UTF_8)));
    }

    @AfterEach
    void restore() {
        cache.close();
        s3.close();
        redirects.close();
        documents.close();
        ReflectionTestUtils.setField(PathFilter.class, "passwordProtected", originalProtectedDirs);
        Constants.setServletContext(originalContext);
        previous.forEach(Constants::setString);
    }

    static Stream<Arguments> encodedRequests() {
        return Stream.of(BACKENDS).flatMap(backend -> Stream.of("anonymous", "bob").flatMap(user ->
                Stream.of("/files/%70rotected", "/%66iles/protected", "/files/other/%2e%2e/protected")
                        .map(prefix -> Arguments.of(backend, user, prefix))));
    }

    /** Encoded or container-normalized prefix aliases cannot reach any file-serving backend. */
    @ParameterizedTest
    @MethodSource("encodedRequests")
    void rejectsProtectedPrefixAliases(String backend, String user, String prefix) throws Exception {
        configureBackend(backend);
        String url = prefix + FILE_URL.substring("/files/protected".length());
        MockHttpServletRequest request = request(url, user);
        if (prefix.startsWith("/%66")) {
            request.addHeader("If-None-Match", "*");
            request.addHeader("Range", "bytes=0-3");
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new PathFilter().doFilter(request, response, chain);

        assertEquals(404, response.getStatus());
        assertEquals("", response.getContentAsString());
        assertEquals("no-store, no-cache, must-revalidate, max-age=0", response.getHeader("Cache-Control"));
        assertNull(response.getHeader("ETag"));
        verifyNoInteractions(chain, storage);
        cache.verifyNoInteractions();
    }

    static Stream<Arguments> ordinaryRequests() {
        return Stream.of(BACKENDS).flatMap(backend -> Stream.of("anonymous", "bob", "alice")
                .map(user -> Arguments.of(backend, user)));
    }

    /** Every backend retains servlet authorization and permits the owner's encoded filename. */
    @ParameterizedTest
    @MethodSource("ordinaryRequests")
    void delegatesProtectedDownloadsToAuthorizationServlet(String backend, String user) throws Exception {
        configureBackend(backend);
        MockHttpServletRequest request = request(FILE_URL, user);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            new GetProtectedFileServlet().doGet((HttpServletRequest) invocation.getArgument(0),
                    (HttpServletResponse) invocation.getArgument(1));
            return null;
        }).when(chain).doFilter(request, response);

        new PathFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        if ("alice".equals(user)) {
            assertEquals(200, response.getStatus());
            assertEquals(CONTENT, response.getContentAsString());
            assertEquals("no-store, no-cache, must-revalidate, max-age=0", response.getHeader("Cache-Control"));
        } else {
            assertEquals("", response.getContentAsString());
            if ("bob".equals(user)) assertEquals(403, response.getStatus());
            else assertEquals("/components/user/logon.jsp?permsDenied=true", response.getForwardedUrl());
            verifyNoInteractions(storage);
        }
        cache.verifyNoInteractions();
    }

    private void configureBackend(String backend) throws Exception {
        Constants.setBoolean("s3Enabled", "s3".equals(backend));
        Constants.setBoolean("enableStaticFilesExternalDir", "external".equals(backend));
        Constants.setString("cloudStaticFilesDir", "external".equals(backend) ? root.resolve("external").toString() : "");
        cache.when(FileCache::useFileCache).thenReturn("cache".equals(backend));
        if (!"s3".equals(backend)) {
            Path file = Path.of(Tools.getRealPath(FILE_URL));
            Files.createDirectories(file.getParent());
            Files.writeString(file, CONTENT);
        }
    }

    private MockHttpServletRequest request(String url, String login) {
        MockHttpServletRequest request = new MockHttpServletRequest(Constants.getServletContext(), "GET", "/cms" + url);
        request.setContextPath("/cms");
        request.setServletPath("/files/protected");
        request.setPathInfo(Tools.URLDecode(FILE_URL.substring("/files/protected".length())));
        if (!"anonymous".equals(login)) {
            Identity user = new Identity();
            user.setLoginName(login);
            request.getSession().setAttribute(Constants.USER_KEY, user);
        }
        return request;
    }
}
