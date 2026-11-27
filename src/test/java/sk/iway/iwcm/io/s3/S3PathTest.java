package sk.iway.iwcm.io.s3;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies routing boundaries independently of the S3 server. */
class S3PathTest extends BaseWebjetTest {
    private final Map<String, String> previous = new HashMap<>();

    @BeforeEach
    void configure() {
        for (String name : new String[] {"s3Enabled", "s3UseForDirs", "multiDomainEnabled"}) {
            previous.put(name, Constants.getString(name));
        }
        Constants.setBoolean("s3Enabled", true);
        Constants.setBoolean("multiDomainEnabled", false);
        Constants.setString("s3UseForDirs", "/files,/images/,/shared,/WEB-INF/imgcache/,/WEB-INF/formfiles/");
    }

    @AfterEach
    void restore() { previous.forEach(Constants::setString); }

    /** Both the directory itself and its descendants select S3. */
    @ParameterizedTest
    @ValueSource(strings = {"/files", "/files/", "/files/žltý súbor.pdf", "/images//gallery/test.png", "/WEB-INF/formfiles/test.pdf"})
    void routesConfiguredDirectories(String path) { assertTrue(S3Path.usesS3(path)); }

    /** Similar prefixes and executable or temporary resources never select S3. */
    @ParameterizedTest
    @ValueSource(strings = {"/files-other/a", "/images2/a", "/files/test.JSP", "/files/test.jspx", "/files/test.class", "/admin/a", "/WEB-INF/tmp/chunk"})
    void excludesOtherResources(String path) { assertFalse(S3Path.usesS3(path)); }

    /** Traversal must not cross a configured storage boundary. */
    @ParameterizedTest
    @ValueSource(strings = {"/files/../secret", "/files/./test", "/files/..\\secret"})
    void rejectsTraversal(String path) { assertThrows(IllegalArgumentException.class, () -> S3Path.usesS3(path)); }

    /** Disabled storage does not resolve a domain or require connection settings. */
    @Test
    void disabledStorageStaysLocal() {
        Constants.setBoolean("s3Enabled", false);
        assertFalse(S3Path.usesS3("/files/test"));
        assertFalse(S3Path.usesS3(null));
    }

    /** Single-domain keys remain compatible with the supplied integration. */
    @Test
    void capturesDefaultNamespace() { assertEquals("defaulthost/", S3Path.namespace("/files/test")); }
}
