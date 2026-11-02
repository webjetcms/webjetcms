package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import com.webjetcms.ai.AiCapabilities;
import com.webjetcms.ai.AiCapabilities.Continuation;
import com.webjetcms.ai.AiCapabilities.Operation;
import com.webjetcms.ai.AiCapabilities.Support;
import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiErrorCategory;
import com.webjetcms.ai.AiOperation;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.AiProviderException;
import com.webjetcms.ai.AiRequest;
import com.webjetcms.ai.AiResponse;
import com.webjetcms.ai.AiStreamListener;
import com.webjetcms.ai.TokenUsage;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.components.ai.dto.AiChatRequest;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.providers.LibrarySupportLogic;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.components.ai.providers.openai.OpenAiService;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;

/** Verifies saved conversations using only final provider responses and previousResponseId. */
class AiChatServiceTest {
    @TempDir Path root;
    private final AiChatStore store = new AiChatStore();
    private final LibrarySupportLogic provider = mock(LibrarySupportLogic.class);
    private final AiChatResponseDeletion deletion = mock(AiChatResponseDeletion.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final AiProviderConfig config = AiProviderConfig.builder("key").build();
    private AiChatStore.Scope scope;
    private AiChatService service;
    private MockedStatic<Adminlog> audit;
    private MockedStatic<Prop> translations;

    @BeforeEach
    void configure() throws Exception {
        scope = new AiChatStore.Scope(42, 7, "example.test", root.toRealPath());
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setId(12L);
        assistant.setName("Assistant");
        assistant.setProvider("openai");
        assistant.setModel("model");
        assistant.setAction("live_chat");
        assistant.setActive(true);
        assistant.setUseTemporal(false);
        assistant.setLastUpdate(new Date(1000));
        var assistants = mock(AssistantDefinitionRepository.class);
        when(assistants.findByIdAndDomainId(12L, 7)).thenReturn(Optional.of(assistant));
        when(provider.getProviderId()).thenReturn("openai");
        when(provider.isInit()).thenReturn(true);
        when(provider.supportsPersistentChat(assistant)).thenReturn(true);
        var configuration = mock(WebjetAiConfigurationService.class);
        when(configuration.resolveForDomain(provider, scope.domainName())).thenReturn(config);
        when(configuration.chatScope(config)).thenReturn("scope");
        var tasks = mock(AiTaskRegistry.class);
        when(tasks.runChatTask(anyString(), any(), any())).thenAnswer(call ->
            call.<Callable<AssistantResponseDTO>>getArgument(1).call());
        service = new AiChatService(store, assistants, List.of(provider), tasks, deletion, configuration, mock(AiStatRepository.class));
        audit = mockStatic(Adminlog.class);
        translations = mockStatic(Prop.class);
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(call -> call.getArgument(0));
        translations.when(() -> Prop.getInstance(request)).thenReturn(prop);
        when(provider.getPersistentChatResponse(any(), any(), any(), anyBoolean(), same(config))).thenReturn(
            new AiResponse("Answer", List.of(), new TokenUsage(1, 2, 3, Map.of()), "completed", "response-one"));
    }

    @AfterEach
    void close() { service.destroy(); translations.close(); audit.close(); }

    private AiChatService.Prepared prepare(String thread, Long revision) {
        return service.prepare(new AiChatRequest(thread, 12L, UUID.randomUUID().toString(), revision, "Question", null), scope);
    }

    private AssistantResponseDTO execute(AiChatService.Prepared prepared) {
        return service.execute(prepared, text -> { }, false, request);
    }

    /** Document text and image descriptions reach the saved chat, and image usage is included. */
    @Test
    void suppliesAttachmentContextAndKeepsPrivateDownloadMetadata() throws Exception {
        var files = AiChatAttachments.read(List.of(
            new org.springframework.mock.web.MockMultipartFile("files", "notes.txt", "text/plain",
                "Document content".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
            new org.springframework.mock.web.MockMultipartFile("files", "image.png", "image/png",
                new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10})));
        var prepared = service.prepare(new AiChatRequest(null, 12L, UUID.randomUUID().toString(), null, "Read these", null), scope, files);
        when(provider.describeChatImage(any(), any(), any(), same(config))).thenReturn(
            new AiResponse("Image description", List.of(), new TokenUsage(2, 2, 4, Map.of()), "completed"));
        when(provider.getPersistentChatResponse(any(), any(), any(), anyBoolean(), same(config))).thenAnswer(call -> {
            InputDataDTO input = call.getArgument(1);
            assertTrue(input.getInputValue().contains("Document content"));
            assertTrue(input.getInputValue().contains("Image description"));
            assertEquals("Read these", input.getUserPrompt());
            return new AiResponse("Summary", List.of(), new TokenUsage(1, 2, 3, Map.of()), "completed", "response-one");
        });
        assertEquals(7, execute(prepared).getTotalTokens());
        var detail = service.detail(prepared.turn().thread().getId(), scope);
        assertEquals(2, detail.messages().get(0).attachments().size());
        assertTrue(detail.messages().get(0).attachments().get(0).url().startsWith("/admin/rest/ai/chat/threads/"));
        assertTrue(service.delete(detail.thread().id(), scope).deleted());
    }

    /** Literal INCLUDE commands survive provider input, checkpoints, saved history and continuation after reload. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesAttachmentIncludesAcrossSavedTurns(boolean streaming) throws Exception {
        String attachmentText = "Before !INCLUDE(/components/gallery/gallery.jsp, perexGroup=1)! after";
        String expectedFirst = "\nAttachment: notes.txt\n" + attachmentText + "\n";
        String expectedNext = "Earlier attachment:\n" + expectedFirst;
        var files = AiChatAttachments.read(List.of(new MockMultipartFile("files", "notes.txt", "text/plain",
            attachmentText.getBytes(StandardCharsets.UTF_8))));
        var first = service.prepare(new AiChatRequest(null, 12L, UUID.randomUUID().toString(), null,
            "Read this attachment", null), scope, files);
        first.assistant().setInstructions("Help with the supplied attachment.");
        first.assistant().setUseStreaming(true);
        String id = first.turn().thread().getId();

        AiClient client = mock(AiClient.class);
        when(client.capabilities("openai", "model")).thenReturn(new AiCapabilities(Map.of(
            AiOperation.CHAT, new Operation(Support.SUPPORTED, Support.SUPPORTED, Support.SUPPORTED,
                Continuation.PROVIDER_STORAGE_REQUIRED)), Support.UNKNOWN, Support.UNKNOWN));
        List<AiRequest> providerRequests = new ArrayList<>();
        String[] remoteContext = {null};
        Answer<AiResponse> respond = invocation -> {
            AiRequest providerRequest = invocation.getArgument(1);
            providerRequests.add(providerRequest);
            boolean initial = providerRequest.previousResponseId() == null;
            if (initial) remoteContext[0] = providerRequest.inputText();
            String text = initial ? remoteContext[0] : "Earlier attachment:\n" + remoteContext[0];
            if (streaming) invocation.<AiStreamListener>getArgument(3).onTextDelta(text);
            return new AiResponse(text, List.of(), TokenUsage.EMPTY, "completed", initial ? "response-one" : "response-two");
        };
        if (streaming) {
            when(client.stream(eq("openai"), any(), same(config), any(), any())).thenAnswer(respond);
        } else {
            when(client.execute(eq("openai"), any(), same(config), any())).thenAnswer(respond);
        }
        when(provider.getPersistentChatResponse(any(), any(), any(), anyBoolean(), same(config))).thenAnswer(call ->
            new OpenAiService(client, null).getPersistentChatResponse(call.getArgument(0), call.getArgument(1),
                call.getArgument(2), call.getArgument(3), config));

        List<String> streamed = new ArrayList<>();
        List<String> checkpoints = new ArrayList<>();
        java.util.function.Consumer<String> output = delta -> {
            streamed.add(delta);
            var saved = new AiChatStore().get(id, scope).messages();
            checkpoints.add(saved.get(saved.size() - 1).getText());
        };
        AssistantResponseDTO firstResult = service.execute(first, output, streaming, request);
        var reloaded = new AiChatStore().get(id, scope);
        var next = service.prepare(new AiChatRequest(id, 12L, UUID.randomUUID().toString(),
            reloaded.thread().getRevision(), "Repeat the earlier attachment", null), scope);
        AssistantResponseDTO nextResult = service.execute(next, output, streaming, request);

        assertEquals(expectedFirst, firstResult.getResponse());
        assertEquals(expectedFirst, reloaded.messages().get(1).getText());
        assertEquals(expectedNext, nextResult.getResponse());
        assertEquals(expectedNext, new AiChatStore().get(id, scope).messages().get(3).getText());
        assertEquals(2, providerRequests.size());
        assertEquals(expectedFirst, providerRequests.get(0).inputText());
        assertEquals("response-one", providerRequests.get(1).previousResponseId());
        assertEquals("", providerRequests.get(1).inputText());
        assertEquals("Repeat the earlier attachment", providerRequests.get(1).userPrompt());
        assertEquals("Help with the supplied attachment.", providerRequests.get(1).instructions());
        assertEquals(streaming ? List.of(expectedFirst, expectedNext) : List.of(), streamed);
        assertEquals(streaming ? List.of(expectedFirst, expectedNext) : List.of(), checkpoints);
    }

    /** Continuation uses the saved ID and duplicate submissions do not call the provider twice. */
    @Test
    void continuesFromFinalResponseAndDeduplicatesRequests() throws Exception {
        var first = prepare(null, null);
        assertEquals("Answer", execute(first).getResponse());
        String id = first.turn().thread().getId();
        var duplicate = service.prepare(new AiChatRequest(id, 12L, first.turn().message().getRequestId(), 0L, "Question", null), scope);
        assertEquals("Answer", execute(duplicate).getResponse());
        verify(provider, times(1)).getPersistentChatResponse(any(), any(), any(), anyBoolean(), any());
        when(provider.getPersistentChatResponse(any(), any(), any(), anyBoolean(), same(config))).thenAnswer(call -> {
            assertEquals("response-one", call.<InputDataDTO>getArgument(1).getPreviousResponseId());
            return new AiResponse("Next answer", List.of(), new TokenUsage(1, 2, 3, Map.of()), "completed", "response-two");
        });
        assertEquals("Next answer", execute(prepare(id, 1L)).getResponse());
        assertTrue(service.delete(id, scope).deleted());
        verify(deletion).deleteResponse("openai", "response-one", config);
        verify(deletion).deleteResponse("openai", "response-two", config);
        assertTrue(store.list(scope, 0).items().isEmpty());
    }

    /** Expiration stops continuation without resending historical text or silently starting a new context. */
    @Test
    void expirationKeepsHistoryAndDoesNotReplay() throws Exception {
        var first = prepare(null, null);
        execute(first);
        String id = first.turn().thread().getId();
        when(provider.getPersistentChatResponse(any(), any(), any(), anyBoolean(), any())).thenThrow(
            new AiProviderException("openai", 404, "Expired", null, false).withCategory(AiErrorCategory.INVALID_CONTINUATION));
        assertEquals("INVALID_CONTINUATION", execute(prepare(id, 1L)).getErrorCategory());
        var detail = service.detail(id, scope);
        assertFalse(detail.thread().canContinue());
        assertEquals("expired", detail.thread().unavailableReason());
        assertEquals(4, detail.messages().size());
        verify(provider, times(2)).getPersistentChatResponse(any(), any(), any(), anyBoolean(), any());
        assertTrue(service.delete(id, scope).deleted());
    }

    /** A failed remote deletion retains the transcript and can be retried. */
    @Test
    void failedDeletionKeepsHistoryUntilRetrySucceeds() throws Exception {
        var first = prepare(null, null);
        execute(first);
        String id = first.turn().thread().getId();
        doThrow(new IOException("Unavailable")).doNothing().when(deletion).deleteResponse("openai", "response-one", config);
        assertEquals("deleteFailed", service.delete(id, scope).error());
        assertEquals(2, store.get(id, scope).messages().size());
        assertTrue(service.delete(id, scope).deleted());
    }
}
