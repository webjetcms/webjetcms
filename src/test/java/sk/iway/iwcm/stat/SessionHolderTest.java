package sk.iway.iwcm.stat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SessionHolder single logon functionality
 */
class SessionHolderTest extends BaseWebjetTest {

    private static final String INVALIDATE_SESSION_ADDR = "INVALIDATE";

    @BeforeEach
    void setUp() {
        // No setup needed - using singleton SessionHolder.getInstance()
    }

    @Test
    void testSessionDetailsInvalidateField() {
        // Test SessionDetails invalidate field functionality
        SessionDetails sessionDetails = new SessionDetails();

        // Initially invalidate should be false
        assertNotEquals(INVALIDATE_SESSION_ADDR, sessionDetails.getRemoteAddr());

        // Set invalidate to true
        sessionDetails.setRemoteAddr(INVALIDATE_SESSION_ADDR);
        assertEquals(INVALIDATE_SESSION_ADDR, sessionDetails.getRemoteAddr());
    }

    @Test
    void testRefreshMethodInvalidatesUserSessions() {
        // Get the singleton instance instead of creating a new one
        SessionHolder sessionHolder = SessionHolder.getInstance();

        // Create test data - multiple sessions for the same user
        SessionDetails session1 = new SessionDetails();
        session1.setLoggedUserId(123);
        session1.setLogonTime(System.currentTimeMillis());
        session1.setRemoteAddr("127.0.0.1");

        SessionDetails session2 = new SessionDetails();
        session2.setLoggedUserId(123);
        session2.setLogonTime(System.currentTimeMillis());
        session2.setRemoteAddr("127.0.0.2");

        SessionDetails session3 = new SessionDetails();
        session3.setLoggedUserId(456);
        session3.setLogonTime(System.currentTimeMillis());
        session3.setRemoteAddr("127.0.0.3");

        // Put sessions into the holder
        sessionHolder.getDataMap().put("session1", session1);
        sessionHolder.getDataMap().put("session2", session2);
        sessionHolder.getDataMap().put("session3", session3);

        // Verify initial state
        assertNotEquals(INVALIDATE_SESSION_ADDR, session1.getRemoteAddr());
        assertNotEquals(INVALIDATE_SESSION_ADDR, session2.getRemoteAddr());
        assertNotEquals(INVALIDATE_SESSION_ADDR, session3.getRemoteAddr());


        // Call refresh for user 123
        SessionHolder.keepOnlySession(123, "session3");

        // Verify that only sessions for user 123 are marked for invalidation
        assertEquals(INVALIDATE_SESSION_ADDR, session1.getRemoteAddr());
        assertEquals(INVALIDATE_SESSION_ADDR, session2.getRemoteAddr());
        assertNotEquals(INVALIDATE_SESSION_ADDR, session3.getRemoteAddr());

        // Cleanup - remove test sessions
        sessionHolder.getDataMap().remove("session1");
        sessionHolder.getDataMap().remove("session2");
        sessionHolder.getDataMap().remove("session3");
    }

    @Test
    void testRefreshWithNoMatchingUsers() {
        // Get the singleton instance
        SessionHolder sessionHolder = SessionHolder.getInstance();

        // Create test data - sessions for different users
        SessionDetails session1 = new SessionDetails();
        session1.setLoggedUserId(123);
        session1.setLogonTime(System.currentTimeMillis());
        session1.setRemoteAddr("127.0.0.1");

        SessionDetails session2 = new SessionDetails();
        session2.setLoggedUserId(456);
        session2.setLogonTime(System.currentTimeMillis());
        session2.setRemoteAddr("127.0.0.2");

        sessionHolder.getDataMap().put("session1", session1);
        sessionHolder.getDataMap().put("session2", session2);

        // Call refresh for user that doesn't exist
        SessionHolder.keepOnlySession(999, "session1");

        // Verify no sessions are invalidated
        assertNotEquals(INVALIDATE_SESSION_ADDR, session1.getRemoteAddr());
        assertNotEquals(INVALIDATE_SESSION_ADDR, session2.getRemoteAddr());

        // Cleanup
        sessionHolder.getDataMap().remove("session1");
        sessionHolder.getDataMap().remove("session2");
    }
}