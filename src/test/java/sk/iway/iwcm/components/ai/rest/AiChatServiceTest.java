package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiErrorCategory;
import com.webjetcms.ai.AiLifecycleListener;
import com.webjetcms.ai.AiRequestLifecycle;
import com.webjetcms.ai.AiRequestLifecycle.State;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.AiProviderException;
import com.webjetcms.ai.AiResponse;
import com.webjetcms.ai.AiResponseDeletion;
import com.webjetcms.ai.TokenUsage;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.components.ai.dto.AiChatRequest;
import sk.iway.iwcm.components.ai.dto.AiChatContext;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.providers.LibrarySupportLogic;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatEntity;
import sk.iway.iwcm.i18n.Prop;

/** Verifies saved chat cleanup, retry metadata and continuation from completed turns only. */
class AiChatServiceTest {
    @TempDir Path root;
    private final AiChatStore store = new AiChatStore();
    private final LibrarySupportLogic provider = mock(LibrarySupportLogic.class);
    private final AiClient aiClient = mock(AiClient.class);
    private final AiTaskRegistry tasks = mock(AiTaskRegistry.class);
    private final AiStatRepository statistics = mock(AiStatRepository.class);
    private final WebjetAiConfigurationService configuration = mock(WebjetAiConfigurationService.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final AiProviderConfig config = AiProviderConfig.builder("key").build();
    private AiChatStore.Scope scope;
    private AiChatService service;
    private AssistantDefinitionEntity assistant;
    private MockedStatic<Adminlog> audit;
    private MockedStatic<Prop> translations;

    @BeforeEach
    void configure() throws Exception {
        scope = new AiChatStore.Scope(42, 7, "example.test", root.toRealPath());
        assistant = new AssistantDefinitionEntity();
        assistant.setId(12L);
        assistant.setName("Assistant");
        assistant.setProvider("openai");
        assistant.setModel("model");
        assistant.setAction("live_chat");
        assistant.setClassName(AiAssistantsService.CHAT_TARGET);
        assistant.setActive(true);
        assistant.setUseTemporal(false);
        var assistants = mock(AssistantDefinitionRepository.class);
        when(assistants.findByIdAndDomainId(12L, 7)).thenReturn(Optional.of(assistant));
        when(provider.getProviderId()).thenReturn("openai");
        when(provider.isInit()).thenReturn(true);
        when(provider.supportsPersistentChat(assistant)).thenReturn(true);
        doCallRealMethod().when(provider).prepareChatAttachment(any(), any(), any(), any());
        when(configuration.resolveForDomain(provider, scope.domainName())).thenReturn(config);
        when(configuration.chatScope(config)).thenReturn("scope");
        when(configuration.auditMaxLength()).thenReturn(2000);
        when(tasks.runChatTask(anyString(), same(scope), any(), any())).thenAnswer(call ->
            call.<Callable<AssistantResponseDTO>>getArgument(2).call());
        service = new AiChatService(store, assistants, List.of(provider), tasks, aiClient, configuration, statistics);
        audit = mockStatic(Adminlog.class);
        translations = mockStatic(Prop.class);
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(call -> call.getArgument(0));
        translations.when(() -> Prop.getInstance(request)).thenReturn(prop);
        when(provider.getPersistentChatResponse(any(), any(), any(), anyBoolean(), same(config), any())).thenAnswer(call -> {
            call.<AiLifecycleListener>getArgument(5).onResponseId("response-one");
            return new AiResponse("Answer", List.of(), new TokenUsage(1, 2, 3, Map.of("cached_tokens", 1L)), "completed", "response-one");
        });
    }

    @AfterEach
    void close() { service.destroy(); translations.close(); audit.close(); }

    private AiChatService.Prepared prepare(String thread, Long revision) {
        return service.prepare(new AiChatRequest(thread, 12L, UUID.randomUUID().toString(), revision, "Question", null), scope);
    }

    private AssistantResponseDTO execute(AiChatService.Prepared prepared) {
        return service.execute(prepared, text -> { }, false, request);
    }

    /** Saved turns use the standard AI audit and record consumption once, including after a replay. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void auditsAssistantInputAndUsage(boolean streaming) {
        assistant.setClassName("sk.iway.iwcm.doc.DocDetails");
        assistant.setFieldFrom("data");
        assistant.setFieldTo("data");
        assistant.setKeepHtml(true);
        var context = new AiChatContext("/admin/rest/web-pages", "42", "data");
        String requestId = UUID.randomUUID().toString();
        var prepared = service.prepare(new AiChatRequest(null, 12L, requestId, null, "Question", null,
            context, "<p>Original page</p>"), scope);

        var response = service.execute(prepared, text -> { }, streaming, request);
        assertEquals(3, response.getTotalTokens());
        var duplicate = service.prepare(new AiChatRequest(prepared.turn().thread().getId(), 12L, requestId, 0L,
            "Question", null, context, "<p>Original page</p>"), scope);
        service.execute(duplicate, text -> { }, streaming, request);

        audit.verify(() -> Adminlog.add(eq(Adminlog.TYPE_AI), argThat(value ->
            value.startsWith("SUCCESS: Assistant (openai)")
                && value.contains("Assistant ID: 12") && value.contains("Assistant name: Assistant")
                && value.contains("Provider: openai") && value.contains("Model: model")
                && value.contains("Field from: data") && value.contains("Field to: data")
                && value.contains("Thread ID: " + prepared.turn().thread().getId())
                && value.contains("Action cost:") && value.contains("input_tokens: 1")
                && value.contains("output_tokens: 2") && value.contains("total_tokens: 3")
                && value.contains("cached_tokens: 1")
                && value.contains("Input value:\n<p>Original page</p>")
                && value.contains("User prompt:\nQuestion") && value.contains("AI response:\nAnswer")), eq(3), eq(-1)), times(1));
        var records = org.mockito.ArgumentCaptor.forClass(AiStatEntity.class);
        verify(statistics, times(1)).save(records.capture());
        assertEquals(12L, records.getValue().getAssistantId());
        assertEquals(7, records.getValue().getDomainId());
        assertEquals(3, records.getValue().getUsedTokens());
    }

    /** Failed and cancelled calls retain the same assistant, question and provider status metadata. */
    @ParameterizedTest
    @CsvSource({"UNKNOWN,503,ERROR", "CANCELLED,0,CANCELLED"})
    void auditsFailedAndCancelledTurns(AiErrorCategory category, int providerStatus, String auditStatus) throws Exception {
        when(provider.getPersistentChatResponse(any(), any(), any(), anyBoolean(), any(), any())).thenThrow(
            new AiProviderException("openai", providerStatus, "Provider failure", null, false).withCategory(category));
        var prepared = prepare(null, null);

        assertEquals(category.name(), execute(prepared).getErrorCategory());

        audit.verify(() -> Adminlog.add(eq(Adminlog.TYPE_AI), argThat(value ->
            value.startsWith(auditStatus + ": Assistant (openai) getPersistentChatResponse")
                && value.contains("Assistant ID: 12") && value.contains("Model: model")
                && value.contains("Thread ID: " + prepared.turn().thread().getId())
                && value.contains("User prompt:\nQuestion") && value.contains("Error message:")
                && value.contains("Error category: " + category)
                && (providerStatus == 0 || value.contains("Provider status: " + providerStatus))), eq(0), eq(-1)));
        verifyNoInteractions(statistics);
    }

    /** Disabling or truncating audit content preserves assistant metadata, thread IDs and consumption. */
    @ParameterizedTest
    @ValueSource(ints = {0, 40})
    void respectsAuditContentLimit(int maxLength) {
        when(configuration.auditMaxLength()).thenReturn(maxLength);
        var prepared = prepare(null, null);
        execute(prepared);

        var description = org.mockito.ArgumentCaptor.forClass(String.class);
        audit.verify(() -> Adminlog.add(eq(Adminlog.TYPE_AI), description.capture(), eq(3), eq(-1)));
        String value = description.getValue();
        assertTrue(value.contains("Assistant ID: 12"));
        assertTrue(value.contains("Thread ID: " + prepared.turn().thread().getId()));
        assertTrue(value.contains("total_tokens: 3"));
        if (maxLength == 0) {
            assertFalse(value.contains("Input value:"));
            assertFalse(value.contains("User prompt:"));
            assertFalse(value.contains("AI response:"));
        } else {
            int contentStart = value.indexOf("\nInput value:\n");
            assertTrue(contentStart > 0);
            assertTrue(value.length() - contentStart <= maxLength);
        }
    }

    /** Audit storage errors cannot turn a completed answer into a failure or prevent saving its history. */
    @Test
    void auditFailurePreservesAnswerAndStatistics() {
        audit.when(() -> Adminlog.add(eq(Adminlog.TYPE_AI), anyString(), eq(3), eq(-1)))
            .thenThrow(new IllegalStateException("Audit unavailable"));
        var prepared = prepare(null, null);

        assertEquals("Answer", execute(prepared).getResponse());
        assertEquals("complete", store.get(prepared.turn().thread().getId(), scope).messages().get(1).getStatus());
        verify(statistics).save(any(AiStatEntity.class));
    }

    /** A completed provider response still incurs cost when a stop arrives before its answer is accepted. */
    @Test
    void recordsReportedUsageWhenStoppedAfterProviderResponse() throws Exception {
        when(provider.getPersistentChatResponse(any(), any(), any(), anyBoolean(), any(), any())).thenAnswer(call -> {
            call.<InputDataDTO>getArgument(1).getCancellationToken().cancel();
            return new AiResponse("Late answer", List.of(), new TokenUsage(1, 2, 3, Map.of()), "completed");
        });
        var prepared = prepare(null, null);

        assertEquals("CANCELLED", execute(prepared).getErrorCategory());
        var records = org.mockito.ArgumentCaptor.forClass(AiStatEntity.class);
        verify(statistics).save(records.capture());
        assertEquals(3, records.getValue().getUsedTokens());
        audit.verify(() -> Adminlog.add(eq(Adminlog.TYPE_AI), startsWith("CANCELLED: Assistant (openai)"), eq(3), eq(-1)));
        assertEquals(3, store.get(prepared.turn().thread().getId(), scope).messages().get(1).getTotalTokens());
    }

    /** Provider retry recommendations survive both transports, a store reload and an idempotent result replay. */
    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void preservesProviderRetryRecommendation(boolean retryable, boolean streaming) throws Exception {
        var failure = new AiProviderException("openai", 400, "Rejected", null, retryable)
            .withLifecycle(new AiRequestLifecycle(State.REJECTED, null));
        when(provider.getPersistentChatResponse(any(), any(), any(), eq(streaming), same(config), any())).thenThrow(failure);
        var prepared = prepare(null, null);

        var response = service.execute(prepared, text -> { }, streaming, request);
        assertEquals(retryable, response.getRetryable());
        assertEquals("UNKNOWN", response.getErrorCategory());
        assertEquals(400, response.getProviderStatusCode());

        String id = prepared.turn().thread().getId();
        var saved = new AiChatStore().get(id, scope).messages().get(1);
        assertEquals(retryable, saved.getRetryable());
        assertEquals(retryable, service.detail(id, scope).messages().get(1).retryable());
        var duplicate = service.prepare(new AiChatRequest(id, 12L, prepared.turn().message().getRequestId(), 0L, "Question", null), scope);
        assertTrue(duplicate.turn().duplicate());
        assertEquals(retryable, service.execute(duplicate, text -> { }, streaming, request).getRetryable());
        verify(provider, times(1)).getPersistentChatResponse(any(), any(), any(), eq(streaming), same(config), any());
    }

    /** Removing CHAT_SEARCH preserves history but prevents new global conversations and continuation. */
    @Test
    void removedGlobalBindingMakesHistoryReadOnly() {
        var first = prepare(null, null);
        execute(first);
        String id = first.turn().thread().getId();
        assistant.setClassName("sk.iway.iwcm.doc.DocDetails");

        var detail = service.detail(id, scope);

        assertFalse(detail.thread().canContinue());
        assertEquals("assistantChanged", detail.thread().unavailableReason());
        assertEquals(2, detail.messages().size());
        assertEquals("Answer", detail.messages().get(1).text());
        assertEquals(1, service.list(scope, 0, "").items().size());
        assertThrows(ResponseStatusException.class, () -> prepare(id, 1L));
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> prepare(null, null));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("assistantChanged", error.getReason());
    }

    /** Attachments reach the provider unchanged and streamed replies survive a store reload. */
    @Test
    void savesAttachmentContextAndStreamedResponse() throws Exception {
        String text = "Before !INCLUDE(/components/gallery/gallery.jsp, perexGroup=1)! after";
        var files = AiChatAttachments.read(List.of(
            new MockMultipartFile("files", "notes.txt", "text/plain", text.getBytes(StandardCharsets.UTF_8)),
            new MockMultipartFile("files", "image.png", "image/png", new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10})));
        var prepared = service.prepare(new AiChatRequest(null, 12L, UUID.randomUUID().toString(), null, "Read these", null), scope, files);
        when(provider.describeChatImage(any(), any(), any(), same(config))).thenReturn(
            new AiResponse("Image description", List.of(), new TokenUsage(2, 2, 4, Map.of()), "completed"));
        when(provider.getPersistentChatResponse(any(), any(), any(), eq(true), same(config), any())).thenAnswer(call -> {
            InputDataDTO input = call.getArgument(1);
            assertTrue(input.getInputValue().contains(text));
            assertTrue(input.getInputValue().contains("Image description"));
            call.<AiLifecycleListener>getArgument(5).onResponseId("response-one");
            call.<Consumer<String>>getArgument(2).accept(text);
            return new AiResponse(text, List.of(), new TokenUsage(1, 2, 3, Map.of()), "completed", "response-one");
        });
        StringBuilder streamed = new StringBuilder();
        var result = service.execute(prepared, streamed::append, true, request);
        assertEquals(text, streamed.toString());
        assertEquals(7, result.getTotalTokens());
        var saved = new AiChatStore().get(prepared.turn().thread().getId(), scope).messages();
        assertEquals(text, saved.get(1).getText());
        assertEquals(2, saved.get(0).getAttachments().size());
        var records = org.mockito.ArgumentCaptor.forClass(AiStatEntity.class);
        verify(statistics).save(records.capture());
        assertEquals(7, records.getValue().getUsedTokens());
        audit.verify(() -> Adminlog.add(eq(Adminlog.TYPE_AI), argThat(value ->
            value.startsWith("SUCCESS: Assistant (openai) getPersistentChatStreamResponse")
                && value.contains("input_tokens: 3") && value.contains("output_tokens: 4")
                && value.contains("total_tokens: 7")), eq(7), eq(-1)));
    }

    /** A later unreadable document does not discard the cost of an image already described for this attempt. */
    @Test
    void retainsImageUsageWhenFollowingAttachmentFails() throws Exception {
        var uploads = AiChatAttachments.read(List.of(
            new MockMultipartFile("files", "image.png", "image/png", new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10}),
            new MockMultipartFile("files", "broken.pdf", "application/pdf", "%PDF-broken".getBytes(StandardCharsets.UTF_8))));
        when(provider.describeChatImage(any(), any(), any(), same(config))).thenReturn(
            new AiResponse("Image description", List.of(), new TokenUsage(2, 2, 4, Map.of()), "completed"));
        var prepared = service.prepare(new AiChatRequest(null, 12L, UUID.randomUUID().toString(), null, "Read these", null), scope, uploads);

        var response = execute(prepared);
        assertEquals("ATTACHMENT_INVALID", response.getErrorCategory());
        assertFalse(response.getRetryable());
        var saved = new AiChatStore().get(prepared.turn().thread().getId(), scope).messages().get(1);
        assertEquals(4, saved.getTotalTokens());
        assertFalse(saved.isStorageUnknown());
        var duplicate = service.prepare(new AiChatRequest(prepared.turn().thread().getId(), 12L,
            prepared.turn().message().getRequestId(), 0L, "Read these", null), scope, uploads);
        assertFalse(execute(duplicate).getRetryable());
        verify(provider, never()).getPersistentChatResponse(any(), any(), any(), anyBoolean(), any(), any());
        var records = org.mockito.ArgumentCaptor.forClass(AiStatEntity.class);
        verify(statistics, times(1)).save(records.capture());
        assertEquals(4, records.getValue().getUsedTokens());
        audit.verify(() -> Adminlog.add(eq(Adminlog.TYPE_AI), argThat(value ->
            value.startsWith("ERROR: Assistant (openai)") && value.contains("Error category: ATTACHMENT_INVALID")
                && value.contains("input_tokens: 2") && value.contains("output_tokens: 2")
                && value.contains("total_tokens: 4")), eq(4), eq(-1)), times(1));
    }

    /** Unexpected failures finish the saved turn and expose a localized error instead of provider diagnostics. */
    @Test
    void persistsUnexpectedFailureWithoutExposingProviderDetails() throws Exception {
        IllegalStateException cause = new IllegalStateException("Provider adapter failed");
        when(provider.getPersistentChatResponse(any(), any(), any(), anyBoolean(), any(), any())).thenThrow(cause);
        var prepared = prepare(null, null);
        String id = prepared.turn().thread().getId();

        var response = execute(prepared);
        assertEquals("UNKNOWN", response.getErrorCategory());
        assertEquals("components.ai_assistants.chat.history.generationFailed.js", response.getError());
        assertTrue(response.getRetryable());
        assertEquals("error", new AiChatStore().get(id, scope).messages().get(1).getStatus());
        var duplicate = service.prepare(new AiChatRequest(id, 12L, prepared.turn().message().getRequestId(), 0L, "Question", null), scope);
        assertTrue(execute(duplicate).getRetryable());
        verify(provider, times(1)).getPersistentChatResponse(any(), any(), any(), anyBoolean(), any(), any());
    }

    /** Keeps history after partial cleanup and retries only responses without confirmed deletion or absence. */
    @ParameterizedTest
    @EnumSource(AiResponseDeletion.class)
    void continuesDeduplicatesAndDeletesConversation(AiResponseDeletion outcome) throws Exception {
        var first = prepare(null, null);
        assertEquals("Answer", execute(first).getResponse());
        String id = first.turn().thread().getId();
        var duplicate = service.prepare(new AiChatRequest(id, 12L, first.turn().message().getRequestId(), 0L, "Question", null), scope);
        assertEquals("Answer", execute(duplicate).getResponse());
        verify(provider, times(1)).getPersistentChatResponse(any(), any(), any(), anyBoolean(), any(), any());
        when(provider.getPersistentChatResponse(any(), any(), any(), anyBoolean(), same(config), any())).thenAnswer(call -> {
            assertEquals("response-one", call.<InputDataDTO>getArgument(1).getPreviousResponseId());
            call.<AiLifecycleListener>getArgument(5).onResponseId("response-two");
            return new AiResponse("Next answer", List.of(), new TokenUsage(1, 2, 3, Map.of()), "completed", "response-two");
        });
        assertEquals("Next answer", execute(prepare(id, 1L)).getResponse());
        when(aiClient.deleteResponse("openai", "response-one", config)).thenReturn(outcome);
        when(aiClient.deleteResponse("openai", "response-two", config))
            .thenThrow(new AiProviderException("openai", "Unavailable")).thenReturn(outcome);
        assertEquals("deleteFailed", service.delete(id, scope).error());
        var saved = new AiChatStore().get(id, scope);
        assertEquals("DELETE_PENDING", saved.thread().getStatus());
        assertEquals(4, saved.messages().size());
        assertTrue(saved.messages().get(1).isProviderDeleted());
        assertFalse(saved.messages().get(3).isProviderDeleted());
        assertTrue(service.delete(id, scope).deleted());
        verify(aiClient).deleteResponse("openai", "response-one", config);
        verify(aiClient, times(2)).deleteResponse("openai", "response-two", config);
        assertTrue(store.list(scope, 0).items().isEmpty());
    }

    /** Field chats honor the HTML preference on retries while preserving literal attachments and the original field snapshot. */
    @ParameterizedTest
    @CsvSource({"false,false,false", "false,true,false", "true,false,false", "true,true,false",
        "false,false,true", "false,true,true", "true,false,true", "true,true,true"})
    void fieldChatHonorsHtmlPreferenceAndPreservesRetryContext(boolean keepHtml, boolean streaming, boolean inputIsSelectedTextInEditor) throws Exception {
        assistant.setClassName("sk.iway.iwcm.doc.DocDetails");
        assistant.setKeepHtml(keepHtml);
        var context = new AiChatContext("/admin/rest/web-pages", "42", "data");
        String originalPage = "<p>Original page</p>!INCLUDE(/components/page.jsp)!";
        String document = "<p>Document context</p>!INCLUDE(/components/attachment.jsp)!";
        var files = AiChatAttachments.read(List.of(new MockMultipartFile("files", "notes.txt", "text/plain",
            document.getBytes(StandardCharsets.UTF_8))));
        var first = service.prepare(new AiChatRequest(null, 12L, UUID.randomUUID().toString(), null, "Question", null,
            context, originalPage, inputIsSelectedTextInEditor), scope, files);
        when(provider.getPersistentChatResponse(any(), any(), any(), eq(streaming), same(config), any()))
            .thenThrow(new AiProviderException("openai", 503, "Try again", null, true));
        assertNotNull(service.execute(first, text -> { }, streaming, request).getError());
        String id = first.turn().thread().getId();
        var retry = service.prepare(new AiChatRequest(id, 12L, UUID.randomUUID().toString(), 1L, "Question", first.turn().message().getId(),
            context, "<p>Changed page</p>"), scope);
        assertNotNull(service.execute(retry, text -> { }, streaming, request).getError());

        var inputs = org.mockito.ArgumentCaptor.forClass(InputDataDTO.class);
        verify(provider, times(2)).getPersistentChatResponse(any(), inputs.capture(), any(), eq(streaming), same(config), any());
        String attachmentContext = "\nAttachment: notes.txt\n" + document + "\n";
        for (InputDataDTO input : inputs.getAllValues()) {
            assertEquals(inputIsSelectedTextInEditor, input.isInputIsSelectedTextInEditor());
            assertTrue(input.getInputValue().endsWith(attachmentContext));
            String pageContext = input.getInputValue().substring(0, input.getInputValue().length() - attachmentContext.length());
            assertEquals(keepHtml ? originalPage : "Original page", pageContext.strip());
        }
        var savedQuestion = new AiChatStore().get(id, scope).messages().get(0);
        assertEquals(originalPage, savedQuestion.getInputValue());
        assertEquals(inputIsSelectedTextInEditor, savedQuestion.isInputIsSelectedTextInEditor());
        assertEquals(files.get(0).attachment().sha256(), savedQuestion.getAttachments().get(0).sha256());
        assertEquals(context, service.detail(id, scope).thread().context());
        assertTrue(service.detail(id, scope).thread().canContinue());
        assertTrue(service.list(scope, 0, "").items().isEmpty());
        assertEquals(1, service.list(scope, 0, "", context).items().size());
    }

    /** Deletion finishes after a queued task is cancelled before generation starts. */
    @Test
    void deletesConversationWhenQueuedTaskIsCancelled() throws Exception {
        var prepared = prepare(null, null);
        String id = prepared.turn().thread().getId();
        doAnswer(call -> {
            assertFalse(service.delete(id, scope).deleted());
            return AssistantResponseDTO.fromError(new CancellationException());
        }).when(tasks).runChatTask(eq(id), same(scope), any(), any());

        assertEquals("CANCELLED", execute(prepared).getErrorCategory());
        assertEquals(HttpStatus.NOT_FOUND,
            assertThrows(ResponseStatusException.class, () -> store.get(id, scope)).getStatusCode());
        verify(provider, never()).getPersistentChatResponse(any(), any(), any(), anyBoolean(), any(), any());
    }

    /** Failed turns keep the last continuation and use lifecycle facts, not HTTP status, for cleanup. */
    @ParameterizedTest
    @CsvSource({"NOT_SUBMITTED,503,", "REJECTED,503,", "UNKNOWN,429,", "RESPONSE_OBSERVED,404,failed-response", "RESPONSE_OBSERVED,429,"})
    void expirationKeepsHistoryAndDoesNotReplay(State state, int status, String responseId) throws Exception {
        var first = prepare(null, null);
        execute(first);
        String id = first.turn().thread().getId();
        when(provider.getPersistentChatResponse(any(), any(), any(), anyBoolean(), any(), any())).thenThrow(
            new AiProviderException("openai", status, "Expired", null, false).withCategory(AiErrorCategory.INVALID_CONTINUATION)
                .withLifecycle(new AiRequestLifecycle(state, responseId)));
        assertEquals("INVALID_CONTINUATION", execute(prepare(id, 1L)).getErrorCategory());
        var detail = service.detail(id, scope);
        assertFalse(detail.thread().canContinue());
        assertEquals(4, detail.messages().size());
        var saved = new AiChatStore().get(id, scope);
        assertEquals("response-one", saved.thread().getLastResponseId());
        assertEquals(responseId, saved.messages().get(3).getProviderResponseId());
        boolean unknown = state == State.UNKNOWN || (state == State.RESPONSE_OBSERVED && responseId == null);
        assertEquals(unknown, saved.messages().get(3).isStorageUnknown());
        assertEquals(!unknown, service.delete(id, scope).deleted());
        verify(provider, times(2)).getPersistentChatResponse(any(), any(), any(), anyBoolean(), any(), any());
    }
}
