package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.components.ai.dto.AiChatRequest;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;

/** Verifies the CMS adapter's ownership, REST errors and compatibility with existing chat JSON. */
class AiChatStoreTest {
    @TempDir Path root;

    @BeforeEach
    void canonicalTemporaryDirectory() throws Exception { root = root.toRealPath(); }
    private final AiChatStore store = new AiChatStore();

    private AiChatStore.Scope scope() { return new AiChatStore.Scope(42, 7, "example.test", root); }

    private AssistantDefinitionEntity assistant() {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setId(12L);
        assistant.setName("Assistant");
        assistant.setProvider("openai");
        assistant.setModel("model");
        assistant.setLastUpdate(new Date(1000));
        return assistant;
    }

    /** Renames flat and folder-based legacy history, continues the conversation and preserves its metadata. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void migratesAndContinuesExistingJson(boolean inThreadFolder) throws Exception {
        String id = "e6d838f7-29bc-4e14-aeb2-38d3243ff029";
        Path directory = Files.createDirectories(root.resolve("42"));
        if (inThreadFolder) directory = Files.createDirectory(directory.resolve(id));
        Path path = directory.resolve(id + ".json");
        Files.writeString(path, """
            {
              "schemaVersion": 1,
              "thread": {
                "id": "%s", "userId": 42, "domainId": 7, "domainName": "example.test",
                "title": "Existing chat", "assistantId": 12, "assistantName": "Assistant",
                "provider": "openai", "providerScope": "provider-scope", "model": "model", "assistantVersion": 1000,
                "created": "2026-10-30T10:00:00.000+00:00", "updated": "2026-10-30T10:00:00.000+00:00",
                "status": "ACTIVE", "revision": 1, "nextSequence": 2, "lastResponseId": "response-one",
                "activeMessageId": null, "leaseUntil": null, "stopRequested": false,
                "deletionError": null, "deletionLeaseId": null
              },
              "messages": [
                {"id":"question", "turnId":"question", "requestId":"request", "sortOrder":0,
                 "role":"user", "text":"First", "status":"complete", "created":1000, "updated":1000},
                {"id":"answer", "turnId":"question", "requestId":"request", "sortOrder":1,
                 "role":"assistant", "text":"First answer", "status":"complete", "created":1000, "updated":1000,
                 "totalTokens":12, "providerResponseId":"response-one", "storageUnknown":false, "providerDeleted":false}
              ]
            }
            """.formatted(id));
        byte[] original = Files.readAllBytes(path);
        assertEquals(id, store.list(scope(), 0).items().get(0).getId());
        var transcript = store.get(id, scope());
        assertFalse(Files.exists(path));
        path = root.resolve("42").resolve(id).resolve("ai_chat.json");
        assertTrue(Files.isRegularFile(path));
        assertArrayEquals(original, Files.readAllBytes(path));
        assertEquals(42, transcript.thread().getUserId());
        assertEquals(12L, transcript.thread().getAssistantId());
        assertEquals(2, transcript.messages().size());
        var turn = store.begin(new AiChatRequest(id, 12L, UUID.randomUUID().toString(), 1L, "Next", null),
            scope(), assistant(), "provider-scope");
        assertEquals(2L, turn.thread().getRevision());
        var saved = new ObjectMapper().readTree(path.toFile());
        assertEquals(1, saved.path("schemaVersion").asInt());
        assertEquals(42, saved.path("thread").path("userId").asInt());
        assertEquals(7, saved.path("thread").path("domainId").asInt());
        assertEquals("provider-scope", saved.path("thread").path("providerScope").asText());
        assertEquals(4, new AiChatStore().get(id, scope()).messages().size());
    }

    /** Missing conversations must not leave empty thread folders behind. */
    @Test
    void missingThreadDoesNotCreateDirectory() {
        String id = UUID.randomUUID().toString();
        assertEquals(HttpStatus.NOT_FOUND,
            assertThrows(ResponseStatusException.class, () -> store.get(id, scope())).getStatusCode());
        assertFalse(Files.exists(root.resolve("42").resolve(id)));
    }

    /** Thread directories cannot redirect reads or writes outside private owner storage. */
    @Test
    void rejectsSymbolicThreadDirectories() throws Exception {
        String id = UUID.randomUUID().toString();
        Path outside = Files.createDirectory(root.resolve("outside"));
        Path file = outside.resolve("ai_chat.json");
        Files.writeString(file, "unchanged");
        Files.createSymbolicLink(Files.createDirectories(root.resolve("42")).resolve(id), outside);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
            assertThrows(ResponseStatusException.class, () -> store.get(id, scope())).getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
            () -> store.begin(new AiChatRequest(null, 12L, id, null, "Question", null), scope(), assistant(), "scope")).getStatusCode());
        assertTrue(store.list(scope(), 0).items().isEmpty());
        assertEquals("unchanged", Files.readString(file));
    }

    /** A conflicting legacy JSON must never overwrite an already stored conversation. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void preservesConflictingStorageFiles(boolean inThreadFolder) throws Exception {
        String id = UUID.randomUUID().toString();
        store.begin(new AiChatRequest(null, 12L, id, null, "Question", null), scope(), assistant(), "scope");
        Path path = root.resolve("42").resolve(id).resolve("ai_chat.json");
        byte[] original = Files.readAllBytes(path);
        Path directory = inThreadFolder ? path.getParent() : root.resolve("42");
        Path legacy = directory.resolve(id + ".json");
        Files.copy(path, legacy);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
            assertThrows(ResponseStatusException.class, () -> store.get(id, scope())).getStatusCode());
        assertArrayEquals(original, Files.readAllBytes(path));
        assertArrayEquals(original, Files.readAllBytes(legacy));
    }

    /** A fixed history filename must still match the conversation ID in its parent directory. */
    @Test
    void rejectsHistoryInAnotherThreadFolder() throws Exception {
        String id = UUID.randomUUID().toString(), otherId = UUID.randomUUID().toString();
        store.begin(new AiChatRequest(null, 12L, id, null, "Question", null), scope(), assistant(), "scope");
        Path owner = root.resolve("42");
        Files.move(owner.resolve(id), owner.resolve(otherId));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
            assertThrows(ResponseStatusException.class, () -> store.get(otherId, scope())).getStatusCode());
    }

    /** Hides conversations from other owners/domains and preserves continuation compatibility checks. */
    @Test
    void retainsCmsOwnershipAndAssistantChecks() {
        String id = UUID.randomUUID().toString();
        var input = new AiChatRequest(null, 12L, id, null, "Question", null);
        var turn = store.begin(input, scope(), assistant(), "provider-scope");
        for (var other : new AiChatStore.Scope[] {
            new AiChatStore.Scope(43, 7, "example.test", root),
            new AiChatStore.Scope(42, 8, "example.test", root),
            new AiChatStore.Scope(42, 7, "other.test", root)
        }) {
            assertTrue(store.list(other, 0).items().isEmpty());
            assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> store.get(id, other)).getStatusCode());
        }
        store.finish(scope(), id, turn.message().getId(), "Answer", "complete", 1, null, null);
        var changed = assistant();
        changed.setLastUpdate(new Date(2000));
        var next = new AiChatRequest(id, 12L, UUID.randomUUID().toString(), 1L, "Next", null);
        assertEquals("assistantChanged", assertThrows(ResponseStatusException.class,
            () -> store.begin(next, scope(), changed, "provider-scope")).getReason());
        assertEquals("providerChanged", assertThrows(ResponseStatusException.class,
            () -> store.begin(next, scope(), assistant(), "changed-scope")).getReason());
        assertTrue(store.begin(input, scope(), changed, "changed-scope").duplicate());
    }

    /** Searches titles, full questions, answers and assistant metadata without case or accent sensitivity. */
    @Test
    void searchesConversationTextAndVisibleMetadata() {
        String id = UUID.randomUUID().toString();
        var assistant = assistant();
        assistant.setName("Špeciálny asistent");
        var turn = store.begin(new AiChatRequest(null, 12L, id, null,
            "Hlavné mesto " + "x".repeat(80) + " Skrytá otázka", null), scope(), assistant, "private-provider-scope");
        store.finish(scope(), id, turn.message().getId(), "Odpoveď je Bratislava.", "complete", 1, null, null);

        for (String query : new String[] { " HLAVNE MESTO ", "skryta otazka", "ODPOVED JE BRATISLAVA", "specialny asistent", "OPENAI" }) {
            var result = store.list(scope(), 0, query);
            assertEquals(1, result.items().size(), query);
            assertEquals(id, result.items().get(0).getId(), query);
        }
        assertTrue(store.list(scope(), 0, "missing phrase").items().isEmpty());
        assertTrue(store.list(scope(), 0, "private-provider-scope").items().isEmpty());
        assertEquals(1, store.list(scope(), 0, " \t ").items().size());
        assertEquals(1, store.list(scope(), 0, null).items().size());
        for (var other : new AiChatStore.Scope[] {
            new AiChatStore.Scope(43, 7, "example.test", root),
            new AiChatStore.Scope(42, 8, "example.test", root),
            new AiChatStore.Scope(42, 7, "other.test", root)
        }) {
            assertTrue(store.list(other, 0, "bratislava").items().isEmpty());
        }
    }

    /** Filters all saved threads before pagination, including matches older than the unfiltered first page. */
    @Test
    void paginatesMatchingThreadsOnly() {
        var matchingIds = new HashSet<String>();
        for (int i = 0; i < 63; i++) {
            String id = UUID.randomUUID().toString();
            boolean matching = i < 32;
            store.begin(new AiChatRequest(null, 12L, id, null, matching ? "Needle " + i : "Unrelated " + i, null),
                scope(), assistant(), "scope");
            if (matching) matchingIds.add(id);
        }
        var first = store.list(scope(), 0, "needle");
        assertEquals(30, first.items().size());
        assertTrue(first.hasMore());
        var second = store.list(scope(), 1, "needle");
        assertEquals(2, second.items().size());
        assertFalse(second.hasMore());
        var returnedIds = new HashSet<String>();
        first.items().forEach(thread -> assertTrue(returnedIds.add(thread.getId())));
        second.items().forEach(thread -> assertTrue(returnedIds.add(thread.getId())));
        assertEquals(matchingIds, returnedIds);
        assertTrue(store.list(scope(), 2, "needle").items().isEmpty());
        assertEquals(first.items().stream().map(thread -> thread.getId()).toList(),
            store.list(scope(), -1, "needle").items().stream().map(thread -> thread.getId()).toList());
    }

    /** Preserves the HTTP status and reason payload for CMS storage failures. */
    @Test
    void preservesRestErrorContract() {
        AiChatController controller = new AiChatController(null, store);
        var statuses = new HttpStatus[] { HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND,
            HttpStatus.CONFLICT, HttpStatus.SERVICE_UNAVAILABLE };
        for (var status : statuses) {
            var response = controller.error(new ResponseStatusException(status, "reason"));
            assertEquals(status, response.getStatusCode());
            assertEquals("reason", response.getBody().get("error"));
        }
    }

    /** Repeated completion must preserve the answer and the ID used by the next request. */
    @Test
    void finalizationIsIdempotentAndExpiredHistoryStaysReadable() {
        String id = UUID.randomUUID().toString();
        var first = store.begin(new AiChatRequest(null, 12L, id, null, "First", null), scope(), assistant(), "scope");
        store.submitted(scope(), id, first.message().getId());
        store.responseId(scope(), id, first.message().getId(), "response-one");
        store.finish(scope(), id, first.message().getId(), "Answer", "complete", 2, null, null);
        store.finish(scope(), id, first.message().getId(), "", "stopped", 0, "CANCELLED", null);
        assertEquals("Answer", store.get(id, scope()).messages().get(1).getText());
        var next = store.begin(new AiChatRequest(id, 12L, UUID.randomUUID().toString(), 1L, "Next", null),
            scope(), assistant(), "scope");
        assertEquals("response-one", next.thread().getLastResponseId());
        store.finish(scope(), id, next.message().getId(), "", "error", 0, "INVALID_CONTINUATION", 404);
        assertEquals("EXPIRED", store.get(id, scope()).thread().getStatus());
        assertEquals(4, store.get(id, scope()).messages().size());
        assertEquals("expired", assertThrows(ResponseStatusException.class, () -> store.begin(
            new AiChatRequest(id, 12L, UUID.randomUUID().toString(), 2L, "Again", null), scope(), assistant(), "scope")).getReason());
    }

    /** Local history is removed only after the known remote response has been deleted. */
    @Test
    void deletionRequiresRemoteConfirmationAndPreservesUnknownRequests() {
        String id = UUID.randomUUID().toString();
        var turn = store.begin(new AiChatRequest(null, 12L, id, null, "Question", null), scope(), assistant(), "scope");
        String message = turn.message().getId();
        store.submitted(scope(), id, message);
        store.stop(id, scope(), true);
        assertNull(store.claimDeletion(id, scope()));
        store.finish(scope(), id, message, "Partial", "stopped", 0, "CANCELLED", null);
        String claim = store.claimDeletion(id, scope());
        assertNotNull(claim);
        assertThrows(ResponseStatusException.class, () -> store.remove(scope(), id, claim));
        store.responseId(scope(), id, message, "known-response");
        assertThrows(ResponseStatusException.class, () -> store.remove(scope(), id, claim));
        store.deletedResponse(scope(), id, claim, message);
        store.remove(scope(), id, claim);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> store.get(id, scope())).getStatusCode());
    }
}
