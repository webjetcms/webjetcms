package sk.iway.iwcm.editor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for ThumbServlet.isSizeAllowed method.
 * Tests different thumbServletAllowedSizeMode configurations and user permissions.
 *
 * Modes:
 * - "deny" - returns false for non-admins, admins can still access
 * - "allow" - returns true for everyone
 * - "learn" - adds size to allowed list and returns true for everyone
 * - "check" - only admins can add new sizes, non-admins can only use existing sizes
 * - default (no mode or empty) - only allows sizes in the allowed list
 */
@Execution(ExecutionMode.SAME_THREAD)
class ThumbServletIsSizeAllowedTest extends BaseWebjetTest {

    private Identity adminUser;
    private Identity nonAdminUser;

    // Example path with size suffix: -730x401ip5.jpg
    private static final String VALID_PATH = "/WEB-INF/imgcache/images/test-730x401ip5.jpg";
    private static final String VALID_PATH_2 = "/WEB-INF/imgcache/images/test-200x150.jpg";
    private static final String INVALID_PATH_NO_DASH = "/WEB-INF/imgcache/images/test.jpg";
    private static final String INVALID_PATH_DOT_BEFORE_DASH = "/WEB-INF/imgcache/images/test.something-here";

    @BeforeEach
    void setUp() {
        // Create admin user
        adminUser = new Identity();
        adminUser.setAdmin(true);

        // Create non-admin user
        nonAdminUser = new Identity();
        nonAdminUser.setAdmin(false);

        // Clean cache before each test
        ThumbServlet.cleanAllowedSizesCache();
        Constants.setString("thumbServletAllowedSizes", "");
        Constants.setString("thumbServletAllowedSizeMode", "");
    }

    @AfterEach
    void tearDown() {
        // Reset state after each test
        ThumbServlet.cleanAllowedSizesCache();
        Constants.setString("thumbServletAllowedSizes", "");
        Constants.setString("thumbServletAllowedSizeMode", "");
    }

    // --- Tests for "deny" mode ---

    @Test
    void testDenyMode_NullUser_ReturnsFalse() {
        Constants.setString("thumbServletAllowedSizeMode", "deny");

        assertFalse(ThumbServlet.isSizeAllowed(VALID_PATH, null),
            "deny mode should return false for null user");
    }

    @Test
    void testDenyMode_NonAdminUser_ReturnsFalse() {
        Constants.setString("thumbServletAllowedSizeMode", "deny");

        assertFalse(ThumbServlet.isSizeAllowed(VALID_PATH, nonAdminUser),
            "deny mode should return false for non-admin user");
    }

    @Test
    void testDenyMode_AdminUser_ReturnsTrue() {
        Constants.setString("thumbServletAllowedSizeMode", "deny");
        // For admin users in deny mode, it should still check the allowed sizes
        // Since size is not in the list and mode is deny, admin can still access

        // First add the size to allowed list
        Constants.setString("thumbServletAllowedSizes", "730x401ip5");
        ThumbServlet.cleanAllowedSizesCache();

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, adminUser),
            "deny mode should allow admin user with valid size");
    }

    // --- Tests for "allow" mode ---

    @Test
    void testAllowMode_NullUser_ReturnsTrue() {
        Constants.setString("thumbServletAllowedSizeMode", "allow");

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, null),
            "allow mode should return true for null user");
    }

    @Test
    void testAllowMode_NonAdminUser_ReturnsTrue() {
        Constants.setString("thumbServletAllowedSizeMode", "allow");

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, nonAdminUser),
            "allow mode should return true for non-admin user");
    }

    @Test
    void testAllowMode_AdminUser_ReturnsTrue() {
        Constants.setString("thumbServletAllowedSizeMode", "allow");

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, adminUser),
            "allow mode should return true for admin user");
    }

    // --- Tests for "learn" mode ---

    @Test
    void testLearnMode_NullUser_AddsSizeAndReturnsTrue() {
        Constants.setString("thumbServletAllowedSizeMode", "learn");

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, null),
            "learn mode should return true and add size for null user");
    }

    @Test
    void testLearnMode_NonAdminUser_AddsSizeAndReturnsTrue() {
        Constants.setString("thumbServletAllowedSizeMode", "learn");

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, nonAdminUser),
            "learn mode should return true and add size for non-admin user");
    }

    @Test
    void testLearnMode_AdminUser_AddsSizeAndReturnsTrue() {
        Constants.setString("thumbServletAllowedSizeMode", "learn");

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, adminUser),
            "learn mode should return true and add size for admin user");
    }

    // --- Tests for "check" mode ---

    @Test
    void testCheckMode_NonAdminUser_SizeNotInList_ReturnsFalse() {
        Constants.setString("thumbServletAllowedSizeMode", "check");

        assertFalse(ThumbServlet.isSizeAllowed(VALID_PATH, nonAdminUser),
            "check mode should return false for non-admin user when size not in list");
    }

    @Test
    void testCheckMode_NonAdminUser_SizeInList_ReturnsTrue() {
        Constants.setString("thumbServletAllowedSizeMode", "check");
        Constants.setString("thumbServletAllowedSizes", "730x401ip5");
        ThumbServlet.cleanAllowedSizesCache();

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, nonAdminUser),
            "check mode should return true for non-admin user when size is in list");
    }

    @Test
    void testCheckMode_AdminUser_AddsSizeAndReturnsTrue() {
        Constants.setString("thumbServletAllowedSizeMode", "check");

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, adminUser),
            "check mode should return true and add size for admin user");
    }

    @Test
    void testCheckMode_NullUser_SizeNotInList_ReturnsFalse() {
        Constants.setString("thumbServletAllowedSizeMode", "check");

        assertFalse(ThumbServlet.isSizeAllowed(VALID_PATH, null),
            "check mode should return false for null user when size not in list");
    }

    // --- Tests for default mode (empty or not set) ---

    @Test
    void testDefaultMode_SizeNotInList_ReturnsFalse() {
        Constants.setString("thumbServletAllowedSizeMode", "");

        assertFalse(ThumbServlet.isSizeAllowed(VALID_PATH, nonAdminUser),
            "default mode should return false when size not in list");
    }

    @Test
    void testDefaultMode_SizeInList_ReturnsTrue() {
        Constants.setString("thumbServletAllowedSizeMode", "");
        Constants.setString("thumbServletAllowedSizes", "730x401ip5");
        ThumbServlet.cleanAllowedSizesCache();

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, nonAdminUser),
            "default mode should return true when size is in list");
    }

    @Test
    void testDefaultMode_AdminUser_SizeNotInList_ReturnsFalse() {
        Constants.setString("thumbServletAllowedSizeMode", "");

        assertFalse(ThumbServlet.isSizeAllowed(VALID_PATH, adminUser),
            "default mode should return false even for admin when size not in list");
    }

    // --- Tests for invalid path formats ---

    @Test
    void testInvalidPath_NoDash_ReturnsFalse() {
        Constants.setString("thumbServletAllowedSizeMode", "allow");

        // Even in allow mode, invalid path format should be rejected
        // Actually, allow mode returns true immediately before path validation
        assertTrue(ThumbServlet.isSizeAllowed(INVALID_PATH_NO_DASH, null),
            "allow mode returns true before path validation");

        // In default mode, invalid path should return false
        Constants.setString("thumbServletAllowedSizeMode", "");
        assertFalse(ThumbServlet.isSizeAllowed(INVALID_PATH_NO_DASH, null),
            "invalid path (no dash) should return false in default mode");
    }

    @Test
    void testInvalidPath_DotBeforeDash_ReturnsFalse() {
        Constants.setString("thumbServletAllowedSizeMode", "");

        assertFalse(ThumbServlet.isSizeAllowed(INVALID_PATH_DOT_BEFORE_DASH, null),
            "invalid path (dot before dash) should return false");
    }

    // --- Tests for allowed sizes configuration ---

    @Test
    void testAllowedSizes_CommaSeparated() {
        Constants.setString("thumbServletAllowedSizeMode", "");
        Constants.setString("thumbServletAllowedSizes", "730x401ip5,200x150,100x100");
        ThumbServlet.cleanAllowedSizesCache();

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, null),
            "comma-separated sizes should be parsed correctly");
        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH_2, null),
            "comma-separated sizes should be parsed correctly");
    }

    @Test
    void testAllowedSizes_NewlineSeparated() {
        Constants.setString("thumbServletAllowedSizeMode", "");
        Constants.setString("thumbServletAllowedSizes", "730x401ip5\n200x150\n100x100");
        ThumbServlet.cleanAllowedSizesCache();

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, null),
            "newline-separated sizes should be parsed correctly");
        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH_2, null),
            "newline-separated sizes should be parsed correctly");
    }

    @Test
    void testAllowedSizes_WithWhitespace() {
        Constants.setString("thumbServletAllowedSizeMode", "");
        Constants.setString("thumbServletAllowedSizes", "  730x401ip5  ,  200x150  ");
        ThumbServlet.cleanAllowedSizesCache();

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, null),
            "sizes with whitespace should be trimmed and parsed correctly");
    }

    // --- Tests for cache functionality ---

    @Test
    void testCleanAllowedSizesCache_ResetsCache() {
        Constants.setString("thumbServletAllowedSizeMode", "");
        Constants.setString("thumbServletAllowedSizes", "730x401ip5");
        ThumbServlet.cleanAllowedSizesCache();

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, null),
            "size should be allowed after cache is loaded");

        // Change the allowed sizes
        Constants.setString("thumbServletAllowedSizes", "");
        // Without cleaning cache, old value would still be used
        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH, null),
            "size should still be allowed due to cache");

        // Clean cache and verify new value is used
        ThumbServlet.cleanAllowedSizesCache();
        assertFalse(ThumbServlet.isSizeAllowed(VALID_PATH, null),
            "size should not be allowed after cache is cleaned");
    }

    // --- Tests for size part extraction ---

    @Test
    void testSizePart_WithIp() {
        Constants.setString("thumbServletAllowedSizeMode", "");
        Constants.setString("thumbServletAllowedSizes", "730x401ip5");
        ThumbServlet.cleanAllowedSizesCache();

        String pathWithIp = "/WEB-INF/imgcache/images/test-730x401ip5.jpg";
        assertTrue(ThumbServlet.isSizeAllowed(pathWithIp, null),
            "size part with ip should be extracted correctly");
    }

    @Test
    void testSizePart_WithIpAndColor() {
        Constants.setString("thumbServletAllowedSizeMode", "");
        Constants.setString("thumbServletAllowedSizes", "730x401ip5ncff00ffq90");
        ThumbServlet.cleanAllowedSizesCache();

        String pathWithIpColor = "/WEB-INF/imgcache/images/test-730x401ip5ncff00ffq90.jpg";
        assertTrue(ThumbServlet.isSizeAllowed(pathWithIpColor, null),
            "size part with ip and color should be extracted correctly");
    }

    @Test
    void testSizePart_SimpleWidthHeight() {
        Constants.setString("thumbServletAllowedSizeMode", "");
        Constants.setString("thumbServletAllowedSizes", "200x150");
        ThumbServlet.cleanAllowedSizesCache();

        assertTrue(ThumbServlet.isSizeAllowed(VALID_PATH_2, null),
            "simple width x height size should be extracted correctly");
    }
}
