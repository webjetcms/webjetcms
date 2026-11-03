package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import com.webjetcms.ai.AiRequestLifecycle;
import com.webjetcms.ai.AiRequestLifecycle.State;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.components.ai.dto.AiChatRequest;
import sk.iway.iwcm.components.ai.dto.AiChatContext;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;

/** Covers persistent conversation lifecycle, isolation and history lookup. */
class AiChatStoreTest {
    @TempDir Path root;
    private final AiChatStore store = new AiChatStore();
    private final AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
    private AiChatStore.Scope scope;

    @BeforeEach
    void setUp() throws Exception {
        root = root.toRealPath();
        scope = new AiChatStore.Scope(42, 7, "example.test", root);
        assistant.setId(12L);
        assistant.setName("Assistant");
        assistant.setProvider("openai");
        assistant.setLastUpdate(new Date(1000));
    }

    private AiChatRequest request(String id, Long revision, String prompt) {
        return new AiChatRequest(id, 12L, UUID.randomUUID().toString(), revision, prompt, null);
    }

    @Test
    void persistsAndContinuesConversationUntilProviderHistoryExpires() {
        var input = request(null, null, "First");
        var first = store.begin(input, scope, assistant, "config");
        String id = first.thread().getId(), message = first.message().getId();
        assertTrue(store.begin(input, scope, assistant, "config").duplicate());
        store.submitted(scope, id, message);
        store.lifecycle(scope, id, message, new AiRequestLifecycle(State.RESPONSE_OBSERVED, "response-one"));
        store.finish(scope, id, message, "Answer", "complete", 2, null, null);
        store.finish(scope, id, message, "", "stopped", 0, "CANCELLED", null);
        assertEquals("Answer", new AiChatStore().get(id, scope).messages().get(1).getText());
        var next = store.begin(request(id, 1L, "Next"), scope, assistant, "config");
        assertEquals("response-one", next.thread().getLastResponseId());
        store.finish(scope, id, next.message().getId(), "", "error", 0, "INVALID_CONTINUATION", 404);
        assertEquals(4, store.get(id, scope).messages().size());
        assertEquals("expired", assertThrows(ResponseStatusException.class,
            () -> store.begin(request(id, 2L, "Again"), scope, assistant, "config")).getReason());
    }

    @Test
    void enforcesOwnershipAndContinuationCompatibility() {
        var turn = store.begin(request(null, null, "Question"), scope, assistant, "config");
        String id = turn.thread().getId();
        for (var other : new AiChatStore.Scope[] {
            new AiChatStore.Scope(43, 7, "example.test", root),
            new AiChatStore.Scope(42, 8, "example.test", root),
            new AiChatStore.Scope(42, 7, "other.test", root)
        }) {
            assertTrue(store.list(other, 0, "Question").items().isEmpty());
            assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> store.get(id, other)).getStatusCode());
        }
        store.finish(scope, id, turn.message().getId(), "Answer", "complete", 1, null, null);
        var next = request(id, 1L, "Next");
        assertEquals("providerChanged", assertThrows(ResponseStatusException.class,
            () -> store.begin(next, scope, assistant, "changed-config")).getReason());
        assistant.setLastUpdate(new Date(2000));
        assertEquals("assistantChanged", assertThrows(ResponseStatusException.class,
            () -> store.begin(next, scope, assistant, "config")).getReason());
    }

    @Test
    void searchesFullTextIgnoringCaseAndAccentsBeforePagination() {
        var matchingIds = new HashSet<String>();
        for (int i = 0; i < 63; i++) {
            var turn = store.begin(request(null, null, "Title " + "x".repeat(80)), scope, assistant, "config");
            String id = turn.thread().getId();
            store.finish(scope, id, turn.message().getId(), i < 32 ? "Odpoveď" : "Unrelated", "complete", 1, null, null);
            if (i < 32) matchingIds.add(id);
        }
        var first = store.list(scope, 0, " ODPOVED ");
        var second = store.list(scope, 1, "odpoved");
        assertEquals(30, first.items().size());
        assertTrue(first.hasMore());
        assertEquals(2, second.items().size());
        assertFalse(second.hasMore());
        var returnedIds = new HashSet<String>();
        first.items().forEach(thread -> returnedIds.add(thread.getId()));
        second.items().forEach(thread -> returnedIds.add(thread.getId()));
        assertEquals(matchingIds, returnedIds);
        assertTrue(store.list(scope, 0, "missing phrase").items().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void migratesLegacyHistoryFilenames(boolean inThreadFolder) throws Exception {
        var turn = store.begin(request(null, null, "Question"), scope, assistant, "config");
        String id = turn.thread().getId();
        store.finish(scope, id, turn.message().getId(), "Answer", "complete", 1, null, null);
        Path path = root.resolve("42").resolve(id).resolve("ai_chat.json");
        Path legacy = (inThreadFolder ? path.getParent() : root.resolve("42")).resolve(id + ".json");
        byte[] original = Files.readAllBytes(path);
        Files.move(path, legacy);
        assertEquals("Answer", store.get(id, scope).messages().get(1).getText());
        assertFalse(Files.exists(legacy));
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    /** Field filtering precedes pagination and excludes other records, fields, resources and standalone chats. */
    @Test
    void isolatesFieldHistoryAndPreservesContextAfterReload() {
        var context = new AiChatContext("/admin/rest/web-pages", "42", "data");
        var ids = new HashSet<String>();
        for (int i = 0; i < 32; i++) {
            var input = new AiChatRequest(null, 12L, UUID.randomUUID().toString(), null, "Field question", null, context, "Original HTML");
            var turn = store.begin(input, scope, assistant, "config");
            ids.add(turn.thread().getId());
        }
        for (var other : new AiChatContext[] { null,
            new AiChatContext(context.entityType(), "43", context.fieldName()),
            new AiChatContext(context.entityType(), context.entityId(), "title"),
            new AiChatContext("/admin/rest/groups", context.entityId(), context.fieldName()) }) {
            store.begin(new AiChatRequest(null, 12L, UUID.randomUUID().toString(), null, "Field question", null, other, null), scope, assistant, "config");
        }
        var reloaded = new AiChatStore();
        var first = reloaded.list(scope, 0, "FIELD", context);
        var second = reloaded.list(scope, 1, "FIELD", context);
        assertEquals(30, first.items().size());
        assertTrue(first.hasMore());
        assertEquals(2, second.items().size());
        assertFalse(second.hasMore());
        var actual = new HashSet<String>();
        first.items().forEach(thread -> actual.add(thread.getId()));
        second.items().forEach(thread -> actual.add(thread.getId()));
        assertEquals(ids, actual);
        assertEquals(1, reloaded.list(scope, 0).items().size());
        var saved = reloaded.get(first.items().get(0).getId(), scope);
        assertEquals(context, saved.thread().getContext());
        assertEquals("Original HTML", saved.messages().get(0).getInputValue());
    }

    /** Neither continuation nor duplicate request IDs may move a conversation into another field. */
    @Test
    void rejectsChangingThreadContextAndReusesOriginalInputForRetry() {
        var context = new AiChatContext("/admin/rest/web-pages", "42", "data");
        var input = new AiChatRequest(null, 12L, UUID.randomUUID().toString(), null, "Question", null, context, "Original HTML");
        var turn = store.begin(input, scope, assistant, "config");
        String id = turn.thread().getId();
        store.finish(scope, id, turn.message().getId(), "", "error", 0, "UNKNOWN", null);
        for (String requestId : new String[] {input.requestId(), UUID.randomUUID().toString()}) {
            assertEquals("requestChanged", assertThrows(ResponseStatusException.class, () -> store.begin(
                new AiChatRequest(id, 12L, requestId, 1L, "Question", null, null, "Original HTML"), scope, assistant, "config")).getReason());
        }
        assertEquals("requestChanged", assertThrows(ResponseStatusException.class, () -> store.begin(
            new AiChatRequest(id, 12L, input.requestId(), 1L, "Question", null, context, "Changed HTML"), scope, assistant, "config")).getReason());
        var retry = store.begin(new AiChatRequest(id, 12L, UUID.randomUUID().toString(), 1L, "Question", turn.message().getId(),
            context, "Changed HTML"), scope, assistant, "config");
        assertEquals("Original HTML", retry.question().getInputValue());
        assertThrows(ResponseStatusException.class, () -> AiChatContext.from(context.entityType(), null, context.fieldName()));
    }

    @Test
    void rejectsSymbolicThreadDirectories() throws Exception {
        String id = UUID.randomUUID().toString();
        Path outside = Files.createDirectory(root.resolve("outside"));
        Path file = outside.resolve("ai_chat.json");
        Files.writeString(file, "unchanged");
        Files.createSymbolicLink(Files.createDirectories(root.resolve("42")).resolve(id), outside);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
            assertThrows(ResponseStatusException.class, () -> store.get(id, scope)).getStatusCode());
        assertThrows(ResponseStatusException.class,
            () -> store.begin(new AiChatRequest(null, 12L, id, null, "Question", null), scope, assistant, "config"));
        assertEquals("unchanged", Files.readString(file));
    }

    @Test
    void deletesLocalHistoryOnlyAfterRemoteConfirmation() {
        var turn = store.begin(request(null, null, "Question"), scope, assistant, "config");
        String id = turn.thread().getId(), message = turn.message().getId();
        store.submitted(scope, id, message);
        store.stop(id, scope, true);
        assertNull(store.claimDeletion(id, scope));
        store.finish(scope, id, message, "Partial", "stopped", 0, "CANCELLED", null);
        String claim = store.claimDeletion(id, scope);
        assertThrows(ResponseStatusException.class, () -> store.remove(scope, id, claim));
        store.lifecycle(scope, id, message, new AiRequestLifecycle(State.RESPONSE_OBSERVED, "known-response"));
        assertThrows(ResponseStatusException.class, () -> store.remove(scope, id, claim));
        store.deletedResponse(scope, id, claim, message);
        store.remove(scope, id, claim);
        assertEquals(HttpStatus.NOT_FOUND,
            assertThrows(ResponseStatusException.class, () -> store.get(id, scope)).getStatusCode());
    }
}
