package sk.iway.iwcm.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.test.BaseWebjetTest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for PathFilter security fixes.
 * Verifies that path blocking and path safety checks work correctly.
 * Uses reflection to test private methods isPathBlocked and isPathSafe.
 */
@Execution(ExecutionMode.SAME_THREAD)
class PathFilterSecurityTest extends BaseWebjetTest {

    private Method isPathBlockedMethod;
    private Method isPathSafeMethod;

    public PathFilterSecurityTest() throws Exception {
        // Get private methods via reflection
        isPathBlockedMethod = PathFilter.class.getDeclaredMethod("isPathBlocked", String.class);
        isPathBlockedMethod.setAccessible(true);
        isPathSafeMethod = PathFilter.class.getDeclaredMethod("isPathSafe", String.class);
        isPathSafeMethod.setAccessible(true);
    }

    // --- Tests for isPathBlocked ---

    @Test
    void testIsPathBlocked_NullPath() throws Exception {
        assertFalse((Boolean) isPathBlockedMethod.invoke(null, (String) null),
            "null path should not be blocked");
    }

    @Test
    void testIsPathBlocked_EmptyPath() throws Exception {
        assertFalse((Boolean) isPathBlockedMethod.invoke(null, ""),
            "empty path should not be blocked");
    }

    @Test
    void testIsPathBlocked_NormalPathsNotBlocked() throws Exception {
        assertFalse((Boolean) isPathBlockedMethod.invoke(null, "/admin/documents/"),
            "Normal admin path should not be blocked");
        assertFalse((Boolean) isPathBlockedMethod.invoke(null, "/showdoc.do?docid=123"),
            "Normal document path should not be blocked");
        assertFalse((Boolean) isPathBlockedMethod.invoke(null, "/components/gallery/"),
            "Normal component path should not be blocked");
    }

    @Test
    void testIsPathBlocked_WhenBlockedPathsConfigured() throws Exception {
        // Set blocked paths via Constants
        Constants.setString("pathFilterBlockedPaths", ".git,.env,wp-config");

        try {
            assertTrue((Boolean) isPathBlockedMethod.invoke(null, "/admin/.git/HEAD"),
                "Path containing .git should be blocked");
            assertTrue((Boolean) isPathBlockedMethod.invoke(null, "/.env"),
                "Path containing .env should be blocked");

            assertFalse((Boolean) isPathBlockedMethod.invoke(null, "/admin/documents/"),
                "Normal path should not be blocked");
        } finally {
            PathFilter.resetBlockedPaths();
        }
    }

    // --- Tests for isPathSafe ---

    @Test
    void testIsPathSafe_NullPath() throws Exception {
        assertTrue((Boolean) isPathSafeMethod.invoke(null, (String) null),
            "null path should be considered safe");
    }

    @Test
    void testIsPathSafe_EmptyPath() throws Exception {
        assertTrue((Boolean) isPathSafeMethod.invoke(null, ""),
            "empty path should be considered safe");
    }

    @Test
    void testIsPathSafe_NormalPaths() throws Exception {
        assertTrue((Boolean) isPathSafeMethod.invoke(null, "/admin/documents/"),
            "Normal path should be safe");
        assertTrue((Boolean) isPathSafeMethod.invoke(null, "/showdoc.do?docid=123"),
            "Normal query string path should be safe");
        assertTrue((Boolean) isPathSafeMethod.invoke(null, "/components/gallery/image.jpg"),
            "Normal component path should be safe");
    }

    @Test
    void testIsPathSafe_PathWithSingleQuoteBlocked() throws Exception {
        assertFalse((Boolean) isPathSafeMethod.invoke(null, "/admin/doc'"),
            "Path with single quote should be blocked");
    }

    @Test
    void testIsPathSafe_PathWithDoubleQuoteBlocked() throws Exception {
        assertFalse((Boolean) isPathSafeMethod.invoke(null, "/admin/doc\""),
            "Path with double quote should be blocked");
    }

    @Test
    void testIsPathSafe_PathWithCarriageReturnBlocked() throws Exception {
        assertFalse((Boolean) isPathSafeMethod.invoke(null, "/admin/doc\r"),
            "Path with carriage return should be blocked");
    }

    @Test
    void testIsPathSafe_PathWithNewLineBlocked() throws Exception {
        assertFalse((Boolean) isPathSafeMethod.invoke(null, "/admin/doc\n"),
            "Path with newline should be blocked");
    }

    @Test
    void testIsPathSafe_CRLFAttackPatternsBlocked() throws Exception {
        assertFalse((Boolean) isPathSafeMethod.invoke(null, "/admin/doc%0D"),
            "Path with %0D should be blocked (CRLF attack)");
        assertFalse((Boolean) isPathSafeMethod.invoke(null, "/admin/doc%0A"),
            "Path with %0A should be blocked (CRLF attack)");
        assertFalse((Boolean) isPathSafeMethod.invoke(null, "/admin/doc%0d"),
            "Path with %0d should be blocked (CRLF attack)");
        assertFalse((Boolean) isPathSafeMethod.invoke(null, "/admin/doc%0a"),
            "Path with %0a should be blocked (CRLF attack)");
    }

    @Test
    void testIsPathSafe_PathTraversalBlocked() throws Exception {
        assertFalse((Boolean) isPathSafeMethod.invoke(null, "/admin/../../etc/passwd"),
            "Path traversal with ../ should be blocked");
    }

    @Test
    void testIsPathSafe_BackslashBlocked() throws Exception {
        assertFalse((Boolean) isPathSafeMethod.invoke(null, "/admin/doc\\file"),
            "Path with backslash should be blocked");
    }

    // --- Tests for resetBlockedPaths ---

    @Test
    void testResetBlockedPaths_ClearsConfiguration() {
        // Set blocked paths
        Constants.setString("pathFilterBlockedPaths", ".git,.env");

        // Reset
        PathFilter.resetBlockedPaths();

        // The reset simply clears the cached array - verify it doesn't throw
        assertDoesNotThrow(PathFilter::resetBlockedPaths,
            "resetBlockedPaths should not throw");
    }
}
