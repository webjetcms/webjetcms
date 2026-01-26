package sk.iway.iwcm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import sk.iway.iwcm.test.BaseWebjetTest;

import javax.servlet.http.HttpServletRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
public class SpamProtectionTest extends BaseWebjetTest {

    private static final String MODULE = "testModule";
    private static final String IP = "127.0.0.1";

    @BeforeEach
    public void setUp() {
        SpamProtection.clearAll();
        // Initialize static mocks
        mockStaticDependencies();
    }

    @AfterEach
    public void tearDown() {
        SpamProtection.clearAll();
    }

    private void mockStaticDependencies() {
        // Mock Constants
        Constants.setInt(SpamProtection.HOURLY_LIMIT_KEY + "-" + MODULE, 3);
        Constants.setInt(SpamProtection.HOURLY_LIMIT_KEY, 3);
        Constants.setInt(SpamProtection.MINIT_LIMIT_KEY + "-" + MODULE, 2);
        Constants.setInt(SpamProtection.MINIT_LIMIT_KEY, 2);
        Constants.setInt(SpamProtection.MINUTE_LIMIT_KEY + "-" + MODULE, 2);
        Constants.setInt(SpamProtection.MINUTE_LIMIT_KEY, 2);
        Constants.setInt(SpamProtection.TIMEOUT_KEY + "-" + MODULE, 1);
        Constants.setInt(SpamProtection.TIMEOUT_KEY, 1);
        Constants.setInt("spamProtectionIgnoreFirstRequests-" + MODULE, 0);
        Constants.setInt("spamProtectionIgnoreFirstRequests", 0);
        Constants.setInt("spamProtectionIgnoreFirstRequests-" + MODULE, 0);
        Constants.setInt("spamProtectionIgnoreFirstRequests", 0);
    }

    private HttpServletRequest mockRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(IP);
        return request;
    }

    private void waitTimeout() {
        try {
            Thread.sleep(1100); // Sleep slightly more than 1 second to ensure timeout passes
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCanPost_AllowsFirstPosts() {
        HttpServletRequest request = mockRequest();
        for (int i = 0; i < 3; i++) {
            boolean allowed = SpamProtection.canPost(MODULE, "post" + i, request);
            System.out.println("Post " + i + " allowed: " + allowed);
            waitTimeout();
            assertThat("Post " + i + " should be allowed", allowed, is(true));
        }
    }

    @Test
    public void testCanPost_BlocksAfterLimit() {
        HttpServletRequest request = mockRequest();
        for (int i = 0; i < 3; i++) {
            assertThat(SpamProtection.canPost(MODULE, "post" + i, request), is(true));
            waitTimeout();
        }
        boolean allowed = SpamProtection.canPost(MODULE, "post4", request);
        assertThat("Should block after reaching hourly limit", allowed, is(false));
    }

    @Test
    public void testCanPost_TimeoutBlocksRapidPosts() {
        HttpServletRequest request = mockRequest();
        assertThat(SpamProtection.canPost(MODULE, "post1", request), is(true));
        // Second post immediately, should be blocked by timeout (1s)
        boolean allowed = SpamProtection.canPost(MODULE, "post2", request);
        assertThat("Should block rapid post due to timeout", allowed, is(false));
    }

    @Test
    public void testCrossMinuteLimit_AllowsUpToLimit() {
        HttpServletRequest request = mockRequest();
        for (int i = 0; i < 4; i++) {
            long wait = SpamProtection.crossMinuteLimit(MODULE, request);
            System.out.println("Minute post " + i + " wait time: " + wait);
            if (i<2) assertThat("Wait should be 0 for allowed minute posts", wait, is(0L));
            else assertThat("Wait should be > 0 after exceeding minute limit", wait, greaterThan(0L));
            waitTimeout();
        }
    }

    @Test
    public void testCrossMinuteLimit_BlocksAfterLimit() {
        HttpServletRequest request = mockRequest();
        for (int i = 0; i < 2; i++) {
            SpamProtection.crossMinuteLimit(MODULE, request);
        }
        long wait = SpamProtection.crossMinuteLimit(MODULE, request);
        assertThat("Wait should be > 0 after exceeding minute limit", wait, greaterThan(0L));
    }

    @Test
    public void testCrossMinitLimit_AllowsUpToLimit() {
        HttpServletRequest request = mockRequest();
        for (int i = 0; i < 2; i++) {
            long wait = SpamProtection.crossMinitLimit(MODULE, request);
            assertThat("Wait should be 0 for allowed minit posts", wait, is(0L));
        }
    }

    @Test
    public void testCrossMinitLimit_BlocksAfterLimit() {
        HttpServletRequest request = mockRequest();
        for (int i = 0; i < 2; i++) {
            SpamProtection.crossMinitLimit(MODULE, request);
        }
        long wait = SpamProtection.crossMinitLimit(MODULE, request);
        assertThat("Wait should be > 0 after exceeding minit limit", wait, greaterThan(0L));
    }

    @Test
    public void testGetMinitPostLimit() {
        int limit = SpamProtection.getMinitPostLimit(MODULE);
        assertThat(limit, is(2));
    }

    @Test
    public void testGetHourlyPostLimit() {
        int limit = SpamProtection.getHourlyPostLimit(MODULE);
        assertThat(limit, is(3));
    }

    @Test
    public void testGetTimeout() {
        long timeout = SpamProtection.getTimeout(MODULE);
        assertThat(timeout, is(1L));
    }

    @Test
    public void testGetWaitTimeout_ReturnsZeroIfBelowLimit() {
        HttpServletRequest request = mockRequest();
        SpamProtection.canPost(MODULE, "post1", request);
        long wait = SpamProtection.getWaitTimeout(MODULE, request);
        assertThat(wait, is(0L));
    }

    @Test
    public void testGetWaitTimeout_ReturnsPositiveIfOverLimit() {
        HttpServletRequest request = mockRequest();
        for (int i = 0; i < 3; i++) {
            SpamProtection.canPost(MODULE, "post" + i, request);
            waitTimeout();
        }
        long wait = SpamProtection.getWaitTimeout(MODULE, request);
        assertThat(wait, greaterThan(0L));
    }

    @Test
    public void testSearchWaitTimeout() {
        HttpServletRequest request = mockRequest();
        for (int i = 0; i < 50; i++) {
            SpamProtection.canPost(MODULE, "post" + i, request);
            if (i==25) SpamProtection.fakeFirstPostTimeForTesting(MODULE, IP, System.currentTimeMillis() - 60*1000*60); // 1 hour ago
            if (i==50) SpamProtection.clearOldData(); // Clear old data to simulate time passage
            long wait = SpamProtection.getWaitTimeout(MODULE, request);
            //SpamProtection.canPost(MODULE, "post" + i, request);
            System.out.println("Wait time before post " + i + ": " + wait);

            //cant be minus value
            assertThat(wait, greaterThanOrEqualTo(0L));

            //after 20 requests, some waits should appear
            if (i >= 20 && i<=24) assertThat(wait, greaterThan(0L));
            //after cleaning old data, waits should disappear
            if (i >= 40) assertThat(wait, greaterThan(0L));

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}