package sk.iway.iwcm.setup;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

final class SetupCompletionState {

    private static final String STATE_ATTRIBUTE = SetupCompletionState.class.getName() + ".state";

    private enum State {
        ACTIVE,
        RUNNING,
        COMPLETED
    }

    private SetupCompletionState() {
    }

    static boolean isCompleted(HttpServletRequest request) {
        return getState(request).get() == State.COMPLETED;
    }

    static boolean tryStart(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AtomicReference<State> state = getState(request);
        if (state.compareAndSet(State.ACTIVE, State.RUNNING)) {
            return true;
        }
        if (state.get() == State.COMPLETED) {
            sendCompletedError(response);
        } else {
            response.sendError(HttpServletResponse.SC_CONFLICT,
                "Another WebJET setup operation is already running");
        }
        return false;
    }

    static void resetAfterFailure(HttpServletRequest request) {
        getState(request).compareAndSet(State.RUNNING, State.ACTIVE);
    }

    static void markCompleted(HttpServletRequest request) {
        getState(request).set(State.COMPLETED);
    }

    static boolean rejectIfCompleted(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isCompleted(request)) {
            sendCompletedError(response);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static AtomicReference<State> getState(HttpServletRequest request) {
        ServletContext servletContext = request.getServletContext();
        Object current = servletContext.getAttribute(STATE_ATTRIBUTE);
        if (current instanceof AtomicReference<?>) {
            return (AtomicReference<State>) current;
        }
        synchronized (servletContext) {
            current = servletContext.getAttribute(STATE_ATTRIBUTE);
            if (current instanceof AtomicReference<?>) {
                return (AtomicReference<State>) current;
            }
            AtomicReference<State> created = new AtomicReference<>(State.ACTIVE);
            servletContext.setAttribute(STATE_ATTRIBUTE, created);
            return created;
        }
    }

    private static void sendCompletedError(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_GONE,
            "WebJET setup has already completed; restart the application server");
    }
}
