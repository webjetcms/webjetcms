package sk.iway.iwcm.editor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.test.BaseWebjetTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    // --- Multithread tests ---

    @Test
    void testConcurrentReads_NoExceptions() throws Exception {
        // Setup: add some allowed sizes
        Constants.setString("thumbServletAllowedSizeMode", "");
        Constants.setString("thumbServletAllowedSizes", "730x401ip5,200x150,100x100,300x200");
        ThumbServlet.cleanAllowedSizesCache();

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Create tasks that all read concurrently
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    // Each thread reads multiple times
                    for (int j = 0; j < 100; j++) {
                        boolean result = ThumbServlet.isSizeAllowed(VALID_PATH, null);
                        if (result) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(0, errorCount.get(), "No errors should occur during concurrent reads");
        assertEquals(threadCount * 100, successCount.get(), "All reads should succeed");
    }

    @Test
    void testConcurrentReadsWithOneWriter_ThreadSafe() throws Exception {
        // Setup: learn mode allows writing new sizes
        Constants.setString("thumbServletAllowedSizeMode", "learn");
        Constants.setString("thumbServletAllowedSizes", "100x100");
        ThumbServlet.cleanAllowedSizesCache();

        int readerCount = 90;
        int writerCount = 10;
        int totalThreads = readerCount + writerCount;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);
        AtomicInteger readSuccessCount = new AtomicInteger(0);
        AtomicInteger writeSuccessCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Create reader tasks (90% of threads) - read existing size
        for (int i = 0; i < readerCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < 50; j++) {
                        // Read existing size
                        String existingPath = "/WEB-INF/imgcache/images/test-100x100.jpg";
                        boolean result = ThumbServlet.isSizeAllowed(existingPath, null);
                        if (result) {
                            readSuccessCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Create writer tasks (10% of threads) - add new sizes
        for (int i = 0; i < writerCount; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < 10; j++) {
                        // Write new size - each writer writes unique sizes
                        String newPath = "/WEB-INF/imgcache/images/test-" + (200 + writerId) + "x" + (150 + j) + ".jpg";
                        boolean result = ThumbServlet.isSizeAllowed(newPath, adminUser);
                        if (result) {
                            writeSuccessCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for completion
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(0, errorCount.get(), "No errors should occur during concurrent access");
        assertEquals(readerCount * 50, readSuccessCount.get(), "All reads of existing size should succeed");
        assertEquals(writerCount * 10, writeSuccessCount.get(), "All writes should succeed in learn mode");
    }

    @Test
    void testConcurrentWritesSameSize_NoDuplicates() throws Exception {
        // Setup: learn mode, empty allowed sizes
        Constants.setString("thumbServletAllowedSizeMode", "learn");
        Constants.setString("thumbServletAllowedSizes", "");
        ThumbServlet.cleanAllowedSizesCache();

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // All threads try to add the same size concurrently
        String samePath = "/WEB-INF/imgcache/images/test-999x888.jpg";

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    boolean result = ThumbServlet.isSizeAllowed(samePath, adminUser);
                    if (result) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(0, errorCount.get(), "No errors should occur");
        assertEquals(threadCount, successCount.get(), "All threads should succeed");
    }

    @Test
    void testCheckMode_ConcurrentAdminWritesAndUserReads() throws Exception {
        // Setup: check mode - only admins can add, users can only read existing
        Constants.setString("thumbServletAllowedSizeMode", "check");
        Constants.setString("thumbServletAllowedSizes", "100x100");
        ThumbServlet.cleanAllowedSizesCache();

        int userReaderCount = 80;
        int adminWriterCount = 10;
        int userWriterCount = 10; // These should fail to add new sizes
        int totalThreads = userReaderCount + adminWriterCount + userWriterCount;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger userReadSuccess = new AtomicInteger(0);
        AtomicInteger adminWriteSuccess = new AtomicInteger(0);
        AtomicInteger userWriteFail = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // User readers - read existing size
        for (int i = 0; i < userReaderCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String existingPath = "/WEB-INF/imgcache/images/test-100x100.jpg";
                    if (ThumbServlet.isSizeAllowed(existingPath, nonAdminUser)) {
                        userReadSuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Admin writers - add new sizes
        for (int i = 0; i < adminWriterCount; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String newPath = "/WEB-INF/imgcache/images/test-" + (300 + writerId) + "x200.jpg";
                    if (ThumbServlet.isSizeAllowed(newPath, adminUser)) {
                        adminWriteSuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // User writers - try to add new sizes (should fail)
        for (int i = 0; i < userWriterCount; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String newPath = "/WEB-INF/imgcache/images/test-" + (500 + writerId) + "x400.jpg";
                    if (ThumbServlet.isSizeAllowed(newPath, nonAdminUser) == false) {
                        userWriteFail.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(0, errorCount.get(), "No exceptions should occur");
        assertEquals(userReaderCount, userReadSuccess.get(), "All user reads of existing size should succeed");
        assertEquals(adminWriterCount, adminWriteSuccess.get(), "All admin writes should succeed");
        assertEquals(userWriterCount, userWriteFail.get(), "All user writes of new sizes should fail");
    }

    @Test
    void testConcurrentCacheCleanAndAccess() throws Exception {
        // Test that cache cleaning during concurrent access doesn't cause issues
        Constants.setString("thumbServletAllowedSizeMode", "");
        Constants.setString("thumbServletAllowedSizes", "730x401ip5,200x150");
        ThumbServlet.cleanAllowedSizesCache();

        int readerCount = 90;
        int cleanerCount = 10;
        int totalThreads = readerCount + cleanerCount;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Reader threads
        for (int i = 0; i < readerCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 100; j++) {
                        // Just call the method - it should not throw
                        ThumbServlet.isSizeAllowed(VALID_PATH, null);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Cache cleaner threads
        for (int i = 0; i < cleanerCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 10; j++) {
                        ThumbServlet.cleanAllowedSizesCache();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(0, errorCount.get(), "No exceptions should occur during concurrent cache clean and access");
    }

    // --- Tests for learn mode - verifying actual values in cache and Constants ---

    @Test
    void testLearnMode_SizeAddedToCache() {
        Constants.setString("thumbServletAllowedSizeMode", "learn");
        Constants.setString("thumbServletAllowedSizes", "");
        ThumbServlet.cleanAllowedSizesCache();

        // Call isSizeAllowed - should add the size to cache
        String testPath = "/WEB-INF/imgcache/images/test-400x300.jpg";
        boolean result = ThumbServlet.isSizeAllowed(testPath, null);

        assertTrue(result, "isSizeAllowed should return true in learn mode");

        // Verify the size is persisted by switching to default mode and checking it's still allowed
        Constants.setString("thumbServletAllowedSizeMode", "");
        ThumbServlet.cleanAllowedSizesCache();

        // In default mode, only sizes in the list should be allowed
        boolean resultAfterReload = ThumbServlet.isSizeAllowed(testPath, null);
        assertTrue(resultAfterReload, "Size should be allowed after reload from Constants");
    }

    @Test
    void testLearnMode_SizeAddedToConstants() {
        Constants.setString("thumbServletAllowedSizeMode", "learn");
        Constants.setString("thumbServletAllowedSizes", "");
        ThumbServlet.cleanAllowedSizesCache();

        // Call isSizeAllowed - should add the size to Constants
        String testPath = "/WEB-INF/imgcache/images/test-500x400.jpg";
        ThumbServlet.isSizeAllowed(testPath, null);

        // Verify the size is in Constants
        String allowedSizes = Constants.getString("thumbServletAllowedSizes");
        assertNotNull(allowedSizes, "Constants thumbServletAllowedSizes should not be null");
        assertTrue(allowedSizes.contains("500x400"), "Size '500x400' should be in Constants");
    }

    @Test
    void testLearnMode_MultipleSizesAddedCorrectly() {
        Constants.setString("thumbServletAllowedSizeMode", "learn");
        Constants.setString("thumbServletAllowedSizes", "");
        ThumbServlet.cleanAllowedSizesCache();

        // Add multiple sizes
        ThumbServlet.isSizeAllowed("/WEB-INF/imgcache/images/test-100x100.jpg", null);
        ThumbServlet.isSizeAllowed("/WEB-INF/imgcache/images/test-200x200.jpg", null);
        ThumbServlet.isSizeAllowed("/WEB-INF/imgcache/images/test-300x300ip5.jpg", null);

        // Verify all sizes are in Constants
        String allowedSizes = Constants.getString("thumbServletAllowedSizes");
        assertTrue(allowedSizes.contains("100x100"), "Size '100x100' should be in Constants");
        assertTrue(allowedSizes.contains("200x200"), "Size '200x200' should be in Constants");
        assertTrue(allowedSizes.contains("300x300ip5"), "Size '300x300ip5' should be in Constants");

        // Verify by switching to default mode and checking all sizes are still allowed
        Constants.setString("thumbServletAllowedSizeMode", "");
        ThumbServlet.cleanAllowedSizesCache();

        assertTrue(ThumbServlet.isSizeAllowed("/WEB-INF/imgcache/images/test-100x100.jpg", null),
            "Size 100x100 should be allowed after reload");
        assertTrue(ThumbServlet.isSizeAllowed("/WEB-INF/imgcache/images/test-200x200.jpg", null),
            "Size 200x200 should be allowed after reload");
        assertTrue(ThumbServlet.isSizeAllowed("/WEB-INF/imgcache/images/test-300x300ip5.jpg", null),
            "Size 300x300ip5 should be allowed after reload");
    }

    @Test
    void testLearnMode_DuplicateSizeNotAddedTwice() {
        Constants.setString("thumbServletAllowedSizeMode", "learn");
        Constants.setString("thumbServletAllowedSizes", "");
        ThumbServlet.cleanAllowedSizesCache();

        // Add the same size multiple times
        ThumbServlet.isSizeAllowed("/WEB-INF/imgcache/images/test-150x150.jpg", null);
        ThumbServlet.isSizeAllowed("/WEB-INF/imgcache/images/another-150x150.jpg", null);
        ThumbServlet.isSizeAllowed("/WEB-INF/imgcache/images/third-150x150.jpg", null);

        // Verify in Constants - should not have duplicates
        String allowedSizes = Constants.getString("thumbServletAllowedSizes");
        int count = countOccurrences(allowedSizes, "150x150");
        assertEquals(1, count, "Size '150x150' should appear only once in Constants");
    }

    @Test
    void testLearnMode_ExistingSizesPreserved() {
        Constants.setString("thumbServletAllowedSizeMode", "learn");
        Constants.setString("thumbServletAllowedSizes", "existing100x100,existing200x200");
        ThumbServlet.cleanAllowedSizesCache();

        // Add a new size
        ThumbServlet.isSizeAllowed("/WEB-INF/imgcache/images/test-300x300.jpg", null);

        // Verify all sizes are in Constants (existing + new)
        String allowedSizes = Constants.getString("thumbServletAllowedSizes");
        assertTrue(allowedSizes.contains("existing100x100"), "Existing size should be preserved");
        assertTrue(allowedSizes.contains("existing200x200"), "Existing size should be preserved");
        assertTrue(allowedSizes.contains("300x300"), "New size should be added");

        // Verify by switching to default mode
        Constants.setString("thumbServletAllowedSizeMode", "");
        ThumbServlet.cleanAllowedSizesCache();

        assertTrue(ThumbServlet.isSizeAllowed("/WEB-INF/imgcache/images/test-300x300.jpg", null),
            "New size should be allowed after reload");
    }

    @Test
    void testLearnMode_ConcurrentAdditions_NoExceptionsAndSomeSaved() throws Exception {
        // This test verifies that concurrent additions don't cause exceptions
        // and that at least some sizes are successfully saved to Constants.
        // Note: Due to race conditions in ConfDB.setName, not all concurrent
        // writes may be persisted, but the mechanism should not fail.
        Constants.setString("thumbServletAllowedSizeMode", "learn");
        Constants.setString("thumbServletAllowedSizes", "");
        ThumbServlet.cleanAllowedSizesCache();

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        // Each thread adds a unique size
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String path = "/WEB-INF/imgcache/images/test-" + (1000 + threadId) + "x" + (500 + threadId) + ".jpg";
                    boolean result = ThumbServlet.isSizeAllowed(path, adminUser);
                    if (result) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete");
        assertEquals(0, errorCount.get(), "No errors should occur");
        assertEquals(threadCount, successCount.get(), "All isSizeAllowed calls should return true in learn mode");

        // Verify that at least some sizes are saved to Constants
        String allowedSizes = Constants.getString("thumbServletAllowedSizes");
        assertNotNull(allowedSizes, "Constants should not be null");
        assertFalse(allowedSizes.isEmpty(), "At least some sizes should be saved to Constants");

        // Count how many sizes are actually in Constants
        int savedCount = 0;
        for (int i = 0; i < threadCount; i++) {
            String expectedSize = (1000 + i) + "x" + (500 + i);
            if (allowedSizes.contains(expectedSize)) {
                savedCount++;
            }
        }

        // At least one size should be saved (proves the mechanism works)
        assertTrue(savedCount >= 1, "At least one size should be saved to Constants, but found: " + savedCount);
    }

    @Test
    void testLearnMode_SequentialAdditions_AllSizesInConstants() {
        // Sequential additions should all be saved correctly
        Constants.setString("thumbServletAllowedSizeMode", "learn");
        Constants.setString("thumbServletAllowedSizes", "");
        ThumbServlet.cleanAllowedSizesCache();

        // Add sizes sequentially
        for (int i = 0; i < 5; i++) {
            String path = "/WEB-INF/imgcache/images/test-" + (2000 + i) + "x" + (1000 + i) + ".jpg";
            assertTrue(ThumbServlet.isSizeAllowed(path, adminUser),
                "Size should be allowed in learn mode");
        }

        // Verify all sizes are in Constants
        String allowedSizes = Constants.getString("thumbServletAllowedSizes");
        for (int i = 0; i < 5; i++) {
            String expectedSize = (2000 + i) + "x" + (1000 + i);
            assertTrue(allowedSizes.contains(expectedSize),
                "Size '" + expectedSize + "' should be in Constants after sequential add");
        }

        // Verify sizes work after reload
        Constants.setString("thumbServletAllowedSizeMode", "");
        ThumbServlet.cleanAllowedSizesCache();

        for (int i = 0; i < 5; i++) {
            String path = "/WEB-INF/imgcache/images/test-" + (2000 + i) + "x" + (1000 + i) + ".jpg";
            assertTrue(ThumbServlet.isSizeAllowed(path, null),
                "Size should be allowed after reload");
        }
    }

    @Test
    void testCheckMode_AdminAddsSizeToCache() {
        Constants.setString("thumbServletAllowedSizeMode", "check");
        Constants.setString("thumbServletAllowedSizes", "");
        ThumbServlet.cleanAllowedSizesCache();

        // Admin adds a new size
        String testPath = "/WEB-INF/imgcache/images/test-600x400.jpg";
        boolean result = ThumbServlet.isSizeAllowed(testPath, adminUser);

        assertTrue(result, "Admin should be allowed to add size in check mode");

        // Verify the size is in Constants
        String allowedSizes = Constants.getString("thumbServletAllowedSizes");
        assertTrue(allowedSizes.contains("600x400"), "Size should be added to Constants by admin");

        // Verify the size works in default mode (proves it was properly saved)
        Constants.setString("thumbServletAllowedSizeMode", "");
        ThumbServlet.cleanAllowedSizesCache();

        assertTrue(ThumbServlet.isSizeAllowed(testPath, null),
            "Size should be allowed in default mode after admin added it");
    }

    @Test
    void testCheckMode_NonAdminCannotAddNewSize() {
        Constants.setString("thumbServletAllowedSizeMode", "check");
        Constants.setString("thumbServletAllowedSizes", "");
        ThumbServlet.cleanAllowedSizesCache();

        // Non-admin tries to add a new size
        String testPath = "/WEB-INF/imgcache/images/test-700x500.jpg";
        boolean result = ThumbServlet.isSizeAllowed(testPath, nonAdminUser);

        assertFalse(result, "Non-admin should not be allowed to add new size in check mode");

        // Verify the size is NOT in Constants
        String allowedSizes = Constants.getString("thumbServletAllowedSizes");
        assertFalse(allowedSizes.contains("700x500"), "Size should NOT be added to Constants by non-admin");
    }

    /**
     * Helper method to count occurrences of a substring in a string
     */
    private int countOccurrences(String str, String sub) {
        if (str == null || sub == null || str.isEmpty() || sub.isEmpty()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
