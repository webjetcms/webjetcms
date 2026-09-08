package sk.iway.iwcm.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

class SetupCompletionStateTest {

    @Test
    void completionIsScopedToTheCurrentWebApplication() {
        MockHttpServletRequest completedRequest = new MockHttpServletRequest(new MockServletContext());
        MockHttpServletRequest freshApplicationRequest = new MockHttpServletRequest(new MockServletContext());

        assertFalse(SetupCompletionState.isCompleted(completedRequest));
        SetupCompletionState.markCompleted(completedRequest);

        assertTrue(SetupCompletionState.isCompleted(completedRequest));
        assertFalse(SetupCompletionState.isCompleted(freshApplicationRequest));
    }

    @Test
    void completedSetupRequestsAreRejectedUntilAFullRestart() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
        MockHttpServletResponse response = new MockHttpServletResponse();
        SetupCompletionState.markCompleted(request);

        assertTrue(SetupCompletionState.rejectIfCompleted(request, response));
        assertTrue(response.isCommitted());
        assertEquals(410, response.getStatus());
    }

    @Test
    void onlyOneSetupOperationCanRunAtATime() throws Exception {
        MockHttpServletRequest firstRequest = new MockHttpServletRequest(new MockServletContext());
        MockHttpServletRequest secondRequest = new MockHttpServletRequest(firstRequest.getServletContext());
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        assertTrue(SetupCompletionState.tryStart(firstRequest, firstResponse));
        assertFalse(SetupCompletionState.tryStart(secondRequest, secondResponse));
        assertEquals(409, secondResponse.getStatus());
        assertFalse(SetupCompletionState.tryStart(secondRequest, new MockHttpServletResponse()));

        SetupCompletionState.resetAfterFailure(firstRequest);
        assertTrue(SetupCompletionState.tryStart(secondRequest, new MockHttpServletResponse()));
    }

    @Test
    void completedOperationCannotBeStartedAgain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());

        assertTrue(SetupCompletionState.tryStart(request, new MockHttpServletResponse()));
        SetupCompletionState.markCompleted(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(SetupCompletionState.tryStart(request, response));
        assertEquals(410, response.getStatus());
    }
}
