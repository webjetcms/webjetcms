package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies the documented domain folders using real CMS file writes, reads and HTTP responses. */
@Execution(ExecutionMode.SAME_THREAD)
class FilePathToolsTest extends BaseWebjetTest {
    @TempDir Path root;
    private Path webroot;
    private final Map<String, String> previous = new HashMap<>();
    private String originalBase;
    private RequestBean originalRequest;

    @BeforeEach
    void configure() throws Exception {
        root = root.toRealPath();
        webroot = Files.createDirectory(root.resolve("webapp"));
        originalBase = System.getProperty("webjetTestBasepath");
        System.setProperty("webjetTestBasepath", webroot.toString());
        originalRequest = SetCharacterEncodingFilter.getCurrentRequestBean();
        for (String name : new String[] {"multiDomainEnabled", "enableStaticFilesExternalDir", "cloudStaticFilesDir",
                "s3Enabled", "iwfs_useDB", "iwfs_useVersioning"}) {
            previous.put(name, Constants.getString(name));
        }
        Constants.setBoolean("multiDomainEnabled", true);
        Constants.setBoolean("enableStaticFilesExternalDir", true);
        Constants.setBoolean("s3Enabled", false);
        Constants.setBoolean("iwfs_useDB", false);
        Constants.setBoolean("iwfs_useVersioning", false);
        IwcmFsDB.init();
    }

    @AfterEach
    void restore() {
        SetCharacterEncodingFilter.setCurrentRequestBean(originalRequest);
        if (originalBase == null) System.clearProperty("webjetTestBasepath");
        else System.setProperty("webjetTestBasepath", originalBase);
        previous.forEach(Constants::setString);
        IwcmFsDB.init();
    }

    /** Both documented storage roots isolate domain files while sharing /shared and web application assets. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void separatesFilesBetweenDomains(boolean useFileRoot) throws Exception {
        Path storage = useFileRoot ? webroot.resolve("static-files") : root.resolve("external-static-files");
        Constants.setString("cloudStaticFilesDir", useFileRoot
            ? "{FILE_ROOT}static-files" : storage + File.separator);
        for (String path : new String[] {"/files/reports/document.txt", "/images/gallery/photo.png", "/WEB-INF/imgcache/photo.png"}) {
            selectDomain("WWW.Alpha.Example");
            IwcmFile alpha = write(path, "alpha content");
            Path alphaPath = storage.resolve("alpha.example" + path);
            assertEquals(alphaPath, Path.of(alpha.getPath()));
            assertEquals("alpha content", Files.readString(alphaPath));

            selectDomain("beta.example");
            assertFalse(IwcmFile.fromVirtualPath(path).exists(), path);
            IwcmFile beta = write(path, "beta content");
            Path betaPath = storage.resolve("beta.example" + path);
            assertEquals(betaPath, Path.of(beta.getPath()));
            assertContent(path, "beta content");

            selectDomain("alpha.example");
            assertContent(path, "alpha content");
            write(path, "alpha updated");
            assertContent(path, "alpha updated");
            assertEquals("beta content", Files.readString(betaPath));
            assertTrue(IwcmFile.fromVirtualPath(path).delete(), path);
            assertFalse(Files.exists(alphaPath), path);

            selectDomain("beta.example");
            assertContent(path, "beta content");
            assertFalse(Files.exists(webroot.resolve(path.substring(1))), path);
        }

        String path = "/shared/document.txt";
        selectDomain("alpha.example");
        IwcmFile alpha = write(path, "shared content");
        assertEquals(storage.resolve("shared/shared/document.txt"), Path.of(alpha.getPath()));

        selectDomain("beta.example");
        assertEquals(alpha.getPath(), IwcmFile.fromVirtualPath(path).getPath());
        assertContent(path, "shared content");
        write(path, "shared updated");

        selectDomain("alpha.example");
        assertContent(path, "shared updated");

        for (String asset : new String[] {"/css/site.css", "/jscripts/site.js", "/templates/site/layout.html"}) {
            selectDomain("alpha.example");
            assertEquals(webroot.resolve(asset.substring(1)), Path.of(Tools.getRealPath(asset)));
            selectDomain("beta.example");
            assertEquals(webroot.resolve(asset.substring(1)), Path.of(Tools.getRealPath(asset)));
        }
    }

    private void selectDomain(String domain) {
        RequestBean request = new RequestBean();
        request.setDomain(domain);
        SetCharacterEncodingFilter.setCurrentRequestBean(request);
    }

    private IwcmFile write(String path, String content) throws Exception {
        IwcmFile file = IwcmFile.fromVirtualPath(path);
        assertEquals(path, file.getVirtualPath());
        IwcmFile parent = file.getParentFile();
        assertTrue(parent.isDirectory() || parent.mkdirs());
        try (IwcmOutputStream output = new IwcmOutputStream(file)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    private void assertContent(String path, String expected) throws Exception {
        byte[] bytes = expected.getBytes(StandardCharsets.UTF_8);
        try (IwcmInputStream input = new IwcmInputStream(path, true)) {
            assertArrayEquals(bytes, input.readAllBytes(), path);
        }
        MockHttpServletRequest request = new MockHttpServletRequest(Constants.getServletContext(), "GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertTrue(FilePathTools.writeFileOut(path, request, response), path);
        assertEquals(200, response.getStatus(), path);
        assertArrayEquals(bytes, response.getContentAsByteArray(), path);
    }
}
