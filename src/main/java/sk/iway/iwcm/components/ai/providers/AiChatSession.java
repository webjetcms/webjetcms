package sk.iway.iwcm.components.ai.providers;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.users.UsersDB;

/** Keeps completed provider continuations private to the authenticated CMS session. */
final class AiChatSession {

    private static final String SESSION_ATTRIBUTE = AiChatSession.class.getName();
    private static final String TURN_ATTRIBUTE = SESSION_ATTRIBUTE + ".turn";
    private static final int MAX_CONTINUATIONS = 64;
    private static final String UNAVAILABLE_MESSAGE =
        "This chat conversation is unavailable or its assistant configuration has changed. Start a new chat.";

    private AiChatSession() { }

    /**
     * Resolves an opaque browser token without exposing provider response IDs to the browser.
     *
     * @param conversationId token returned for a completed turn, or empty for a new chat
     * @param assistant current assistant configuration
     * @param request authenticated request carrying the conversation's session
     * @return immutable provider continuation and protected INCLUDE mappings
     * @throws IllegalStateException if authentication or the conversation scope no longer matches
     */
    static Continuation resolve(String conversationId, AssistantDefinitionEntity assistant, HttpServletRequest request) {
        if (request == null) throw new IllegalStateException("Sign in before starting or continuing a chat.");
        request.removeAttribute(TURN_ATTRIBUTE);
        HttpSession session = request.getSession(false);
        Scope scope = scope(assistant, session);
        Continuation continuation = new Continuation(null, Map.of());
        synchronized (session) {
            if (conversationId != null && conversationId.isEmpty() == false) {
                StoredContinuation stored = history(session).continuations().get(conversationId);
                if (stored == null || scope.equals(stored.scope()) == false) {
                    throw new IllegalStateException(UNAVAILABLE_MESSAGE);
                }
                continuation = stored.continuation();
            }
            request.setAttribute(TURN_ATTRIBUTE, new TurnContext(session, scope));
        }
        return continuation;
    }

    /**
     * Stores a completed turn as a new snapshot, preserving older tokens for concurrent branches.
     *
     * @param assistant assistant configuration used for the completed turn
     * @param request authenticated request carrying the conversation's session
     * @param responseId successful provider response ID
     * @param includes protected INCLUDE mappings needed by subsequent turns
     * @return opaque token for continuing from this response
     * @throws IllegalStateException if the resolved request scope changed or the response ID is unusable
     */
    static String remember(AssistantDefinitionEntity assistant, HttpServletRequest request, String responseId,
        Map<String, String> includes) {
        Object captured = request == null ? null : request.getAttribute(TURN_ATTRIBUTE);
        if (captured instanceof TurnContext == false) {
            throw new IllegalStateException("Resolve the chat conversation before storing its response.");
        }
        TurnContext context = (TurnContext) captured;
        if (responseId == null || responseId.isBlank()) {
            throw new IllegalStateException("The chat provider did not return a completed response ID.");
        }
        Continuation continuation = new Continuation(responseId, includes);
        String conversationId = UUID.randomUUID().toString();
        HttpSession session = request.getSession(false);
        if (session != context.session()) throw new IllegalStateException(UNAVAILABLE_MESSAGE);
        synchronized (session) {
            if (context.scope().equals(scope(assistant, session)) == false) {
                throw new IllegalStateException(UNAVAILABLE_MESSAGE);
            }
            Map<String, StoredContinuation> continuations = new LinkedHashMap<>(history(session).continuations());
            continuations.put(conversationId, new StoredContinuation(context.scope(), continuation));
            while (continuations.size() > MAX_CONTINUATIONS) {
                continuations.remove(continuations.keySet().iterator().next());
            }
            session.setAttribute(SESSION_ATTRIBUTE, new History(Collections.unmodifiableMap(continuations)));
        }
        return conversationId;
    }

    private static History history(HttpSession session) {
        Object value = session.getAttribute(SESSION_ATTRIBUTE);
        return value instanceof History history ? history : new History(Map.of());
    }

    private static Scope scope(AssistantDefinitionEntity assistant, HttpSession session) {
        Identity user = UsersDB.getCurrentUser(session);
        if (user == null || user.getUserId() < 1) {
            throw new IllegalStateException("Sign in before starting or continuing a chat.");
        }
        if (assistant == null || assistant.getId() == null || assistant.getId() < 1) {
            throw new IllegalStateException("Select an assistant before starting or continuing a chat.");
        }
        return new Scope(user.getUserId(), CloudToolsForCore.getDomainId(), CloudToolsForCore.getDomainName(),
            assistant.getId(), assistant.getProvider(), assistant.getModel(),
            assistant.getLastUpdate() == null ? null : assistant.getLastUpdate().getTime());
    }

    /** A completed provider response and a defensive copy of its protected INCLUDE mappings. */
    record Continuation(String responseId, Map<String, String> includes) implements Serializable {
        Continuation {
            includes = includes == null ? Map.of() : Map.copyOf(includes);
        }
    }

    private record Scope(int userId, int domainId, String domainName, Long assistantId, String provider,
        String model, Long lastUpdate) implements Serializable { }

    private record TurnContext(HttpSession session, Scope scope) { }

    private record StoredContinuation(Scope scope, Continuation continuation) implements Serializable { }

    private record History(Map<String, StoredContinuation> continuations) implements Serializable { }
}
