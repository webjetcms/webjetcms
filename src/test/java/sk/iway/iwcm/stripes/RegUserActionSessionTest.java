package sk.iway.iwcm.stripes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import net.sourceforge.stripes.action.ActionBeanContext;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.test.SerializingMockHttpSession;
import sk.iway.iwcm.users.UserDetails;

/** Verifies the session update performed after saving a public user profile. */
class RegUserActionSessionTest extends BaseWebjetTest {
    /** Profile, delivery and group changes remain visible on the next session read. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void persistsUpdatedProfile(boolean serialized) {
        MockHttpSession session = sessionWithUser(serialized);
        UserDetails changed = new UserDetails();
        changed.setUserId(42);
        changed.setFirstName("Updated");
        changed.setEmail("updated@example.com");
        changed.setDeliveryCity("Bratislava");
        changed.setUserGroupsIds("2,3");

        updateLoggedUser(session, changed);

        Identity restored = (Identity) session.getAttribute(Constants.USER_KEY);
        assertEquals(42, restored.getUserId());
        assertEquals("Updated", restored.getFirstName());
        assertEquals("updated@example.com", restored.getEmail());
        assertEquals("Bratislava", restored.getDeliveryCity());
        assertEquals("2,3", restored.getUserGroupsIds());
    }

    /** Editing another account must not replace or modify the current session identity. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesSessionWhenEditingAnotherUser(boolean serialized) {
        MockHttpSession session = sessionWithUser(serialized);
        UserDetails changed = new UserDetails();
        changed.setUserId(43);
        changed.setFirstName("Other user");

        updateLoggedUser(session, changed);

        Identity restored = (Identity) session.getAttribute(Constants.USER_KEY);
        assertEquals(42, restored.getUserId());
        assertEquals("Original", restored.getFirstName());
    }

    /** Updating a profile does not log an anonymous visitor in. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesAnonymousSession(boolean serialized) {
        MockHttpSession session = serialized ? new SerializingMockHttpSession() : new MockHttpSession();
        UserDetails changed = new UserDetails();
        changed.setUserId(42);

        updateLoggedUser(session, changed);

        assertNull(session.getAttribute(Constants.USER_KEY));
    }

    private MockHttpSession sessionWithUser(boolean serialized) {
        MockHttpSession session = serialized ? new SerializingMockHttpSession() : new MockHttpSession();
        Identity user = new Identity();
        user.setUserId(42);
        user.setFirstName("Original");
        session.setAttribute(Constants.USER_KEY, user);
        return session;
    }

    private void updateLoggedUser(MockHttpSession session, UserDetails changed) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        ActionBeanContext context = new ActionBeanContext();
        context.setRequest(request);
        RegUserAction action = new RegUserAction();
        action.setContext(context);
        action.setUser(changed);
        ReflectionTestUtils.invokeMethod(action, "updateLoggedUser");
    }
}
