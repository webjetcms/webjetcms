package sk.iway.iwcm.components.ai.rest;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.webjetcms.ai.AiErrorCategory;
import com.webjetcms.ai.AiProviderException;
import com.webjetcms.ai.AiResponse;
import com.webjetcms.ai.BinaryContent;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.components.ai.chat.AiChatEntry;
import sk.iway.iwcm.components.ai.chat.AiChatThread;
import sk.iway.iwcm.components.ai.dto.*;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.providers.*;
import sk.iway.iwcm.components.ai.rest.AiChatStore.Scope;
import sk.iway.iwcm.components.ai.rest.AiChatStore.Turn;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.components.ai.stat.rest.AiStatService;
import sk.iway.iwcm.i18n.Prop;

/** Coordinates durable chat history with provider execution and explicit remote deletion. */
@Service
@RequiredArgsConstructor
public class AiChatService implements DisposableBean {
    private final AiChatStore store;
    private final AssistantDefinitionRepository assistants;
    private final List<AiInterface> providers;
    private final AiTaskRegistry tasks;
    private final AiChatResponseDeletion deletion;
    private final WebjetAiConfigurationService configuration;
    private final AiStatRepository statistics;
    private final ScheduledExecutorService heartbeat = Executors.newScheduledThreadPool(1, task -> {
        Thread thread = new Thread(task, "webjet-ai-chat-lease");
        thread.setDaemon(true);
        return thread;
    });

    public record ThreadView(String id, String title, Long assistantId, String assistantName, String provider, String model,
        Date created, Date updated, String status, long revision, boolean busy, boolean canContinue,
        String unavailableReason, String deletionError) { }
    public record MessageView(String id, String turnId, String requestId, String role, String text, String status,
        Date created, int totalTokens, String errorCategory, Integer providerStatusCode, List<AttachmentView> attachments) { }
    public record AttachmentView(String id, String name, String mediaType, long size, String url) { }
    public record HistoryPage(List<ThreadView> items, boolean hasMore) { }
    public record Detail(ThreadView thread, List<MessageView> messages) { }
    public record Deletion(boolean deleted, String error) { }
    public record Prepared(Turn turn, AssistantDefinitionEntity assistant, Scope scope) { }

    public HistoryPage list(Scope scope, int page, String query) {
        var result = store.list(scope, page, query);
        return new HistoryPage(result.items().stream().map(this::view).toList(), result.hasMore());
    }

    public Detail detail(String id, Scope scope) {
        var transcript = store.get(id, scope);
        return new Detail(view(transcript.thread()), transcript.messages().stream().map(message -> view(id, message)).toList());
    }

    public Prepared prepare(AiChatRequest input, Scope scope) {
        return prepare(input, scope, List.of());
    }

    public Prepared prepare(AiChatRequest input, Scope scope, List<AiChatAttachments.Upload> uploads) {
        if (input == null || input.assistantId() == null || input.userPrompt() == null || input.userPrompt().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalidInput");
        }
        AssistantDefinitionEntity assistant = assistants.findByIdAndDomainId(input.assistantId(), scope.domainId()).orElse(null);
        if (!eligible(assistant)) throw new ResponseStatusException(HttpStatus.CONFLICT, "assistantChanged");
        String providerScope = configuration.chatScope(configuration.resolveForDomain(provider(assistant.getProvider()), scope.domainName()));
        return new Prepared(store.begin(input, scope, assistant, providerScope, uploads), assistant, scope);
    }

    public Map<String, Object> metadata(Prepared prepared) {
        Turn turn = prepared.turn();
        return Map.of("threadId", turn.thread().getId(), "messageId", turn.message().getId(),
            "userMessageId", turn.message().getTurnId(), "revision", turn.thread().getRevision());
    }

    public AssistantResponseDTO execute(Prepared prepared, Consumer<String> output, boolean streaming, HttpServletRequest request) {
        Turn turn = prepared.turn();
        AssistantResponseDTO result;
        if (turn.duplicate()) {
            result = savedResult(turn.message(), request);
        } else {
            InputDataDTO input = new InputDataDTO();
            input.setAssistantId(prepared.assistant().getId());
            input.setInputValue("");
            input.setInputValueType(InputDataDTO.InputValueType.TEXT);
            input.setOutputValueType(InputDataDTO.InputValueType.TEXT);
            var transcript = store.get(turn.thread().getId(), prepared.scope());
            input.setUserPrompt(transcript.messages().stream().filter(m -> m.getId().equals(turn.message().getTurnId()))
                .findFirst().orElseThrow().getText());
            input.setPreviousResponseId(turn.thread().getLastResponseId());
            AtomicBoolean started = new AtomicBoolean();
            try {
                result = tasks.runChatTask(turn.thread().getId(), prepared.scope(), () -> {
                    started.set(true);
                    return generate(prepared, input, output, streaming, request);
                }, input);
            } catch (Exception exception) {
                result = failure("UNKNOWN", null, request);
            } finally {
                if (!started.get()) {
                    store.finish(prepared.scope(), turn.thread().getId(), turn.message().getId(), "", "stopped", 0, "CANCELLED", null);
                    // Queued tasks never enter generate(), so finish any pending deletion here.
                    cleanup(turn.thread().getId(), prepared.scope());
                }
            }
        }
        result.setThreadId(turn.thread().getId());
        result.setMessageId(turn.message().getId());
        result.setRevision(turn.thread().getRevision());
        return result;
    }

    private AssistantResponseDTO generate(Prepared prepared, InputDataDTO input, Consumer<String> output,
        boolean streaming, HttpServletRequest request) {
        String threadId = prepared.turn().thread().getId();
        String messageId = prepared.turn().message().getId();
        StringBuilder text = new StringBuilder();
        long[] checkpoint = {0};
        int tokens = 0;
        String status = "error";
        String category = "UNKNOWN";
        Integer providerStatus = null;
        var renewal = heartbeat.scheduleWithFixedDelay(() -> {
            try {
                if (!store.heartbeat(prepared.scope(), threadId, messageId)) input.getCancellationToken().cancel();
            } catch (RuntimeException exception) {
                input.getCancellationToken().cancel();
            }
        }, 0, 10, TimeUnit.SECONDS);
        try {
            LibrarySupportLogic provider = provider(prepared.assistant().getProvider());
            var providerConfig = configuration.resolveForDomain(provider, prepared.scope().domainName());
            if (!configuration.chatScope(providerConfig).equals(prepared.turn().thread().getProviderScope())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "providerChanged");
            }
            Consumer<String> onText = delta -> {
                if (input.getCancellationToken().isCancelled()) throw new CancellationException();
                text.append(delta);
                if (System.currentTimeMillis() - checkpoint[0] >= 1000) {
                    if (!store.progress(prepared.scope(), threadId, messageId, text.toString())) throw new CancellationException();
                    checkpoint[0] = System.currentTimeMillis();
                }
                output.accept(delta);
            };
            if (input.getCancellationToken().isCancelled()) throw new CancellationException();
            AiChatEntry question = store.get(threadId, prepared.scope()).messages().stream()
                .filter(message -> message.getId().equals(prepared.turn().message().getTurnId())).findFirst().orElseThrow();
            StringBuilder context = new StringBuilder();
            for (var attachment : question.getAttachments()) {
                if (input.getCancellationToken().isCancelled()) throw new CancellationException();
                byte[] bytes = store.attachment(threadId, attachment.id(), prepared.scope()).bytes();
                String content;
                if (attachment.mediaType().startsWith("image/")) {
                    AiResponse description = provider.describeChatImage(new BinaryContent(bytes, attachment.mediaType(), attachment.name()),
                        prepared.assistant(), input, providerConfig);
                    content = description.text();
                    tokens += (int)Math.min(Integer.MAX_VALUE - tokens, description.usage().totalTokens());
                } else {
                    content = AiChatAttachments.documentText(attachment, bytes);
                }
                context.append("\nAttachment: ").append(attachment.name()).append('\n').append(content).append('\n');
                if (context.length() > AiChatAttachments.MAX_TEXT) throw new AiChatAttachments.UnreadableException();
            }
            input.setInputValue(context.toString());
            store.submitted(prepared.scope(), threadId, messageId);
            AiResponse response = provider.getPersistentChatResponse(prepared.assistant(), input, onText, streaming, providerConfig);
            store.responseId(prepared.scope(), threadId, messageId, response.responseId());
            if (input.getCancellationToken().isCancelled()) throw new CancellationException();
            text.setLength(0);
            text.append(response.text());
            tokens += (int)Math.min(Integer.MAX_VALUE - tokens, response.usage().totalTokens());
            status = "complete";
            category = null;
            AssistantResponseDTO result = new AssistantResponseDTO();
            result.setResponse(text.toString());
            result.setTotalTokens(tokens);
            try {
                AiStatService.addRecord(prepared.assistant().getId(), tokens, statistics, request, prepared.scope().domainId());
            } catch (RuntimeException exception) {
                audit(threadId, "statistics_failed", null);
            }
            return result;
        } catch (AiChatAttachments.UnreadableException exception) {
            category = "ATTACHMENT_INVALID";
            return failure(category, null, request);
        } catch (AiProviderException exception) {
            category = exception.category().name();
            providerStatus = exception.statusCode() > 0 ? exception.statusCode() : null;
            if (List.of(400, 401, 403, 404, 409, 422, 429).contains(exception.statusCode())) {
                store.rejected(prepared.scope(), threadId, messageId);
            }
            status = exception.category() == AiErrorCategory.CANCELLED ? "stopped" : "error";
            return failure(category, providerStatus, request);
        } catch (Exception exception) {
            if (input.getCancellationToken().isCancelled() || exception instanceof CancellationException) {
                category = "CANCELLED";
                status = "stopped";
            }
            return failure(category, providerStatus, request);
        } finally {
            renewal.cancel(false);
            boolean interrupted = Thread.interrupted();
            try {
                store.finish(prepared.scope(), threadId, messageId, text.toString(), status, tokens, category, providerStatus);
                audit(threadId, status, category);
                try {
                    if ("DELETE_PENDING".equals(store.get(threadId, prepared.scope()).thread().getStatus())) {
                        cleanup(threadId, prepared.scope());
                    }
                } catch (ResponseStatusException exception) {
                    if (exception.getStatusCode() != HttpStatus.NOT_FOUND) throw exception;
                }
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
    }

    public void stop(String id, Scope scope) {
        store.stop(id, scope, false);
        tasks.cancelChatTask(id, scope);
    }

    public Deletion delete(String id, Scope scope) {
        store.stop(id, scope, true);
        tasks.cancelChatTask(id, scope);
        return cleanup(id, scope);
    }

    private Deletion cleanup(String id, Scope scope) {
        String claim;
        try {
            claim = store.claimDeletion(id, scope);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) return new Deletion(true, null);
            throw exception;
        }
        if (claim == null) return new Deletion(false, null);
        try {
            var transcript = store.get(id, scope);
            LibrarySupportLogic provider = provider(transcript.thread().getProvider());
            var config = configuration.resolveForDomain(provider, transcript.thread().getDomainName());
            if (!configuration.chatScope(config).equals(transcript.thread().getProviderScope())) {
                store.deletionFailed(scope, id, claim, "providerChanged");
                return new Deletion(false, "providerChanged");
            }
            for (AiChatEntry message : transcript.messages()) {
                if (message.getProviderResponseId() != null && !message.isProviderDeleted()) {
                    deletion.deleteResponse(provider.getProviderId(), message.getProviderResponseId(), config);
                    store.deletedResponse(scope, id, claim, message.getId());
                }
            }
            if (transcript.messages().stream().anyMatch(AiChatEntry::isStorageUnknown)) {
                store.deletionFailed(scope, id, claim, "cleanupUnknown");
                return new Deletion(false, "cleanupUnknown");
            }
            store.remove(scope, id, claim);
            audit(id, "deleted", null);
            return new Deletion(true, null);
        } catch (Exception exception) {
            store.deletionFailed(scope, id, claim, "deleteFailed");
            audit(id, "delete_failed", null);
            return new Deletion(false, "deleteFailed");
        }
    }

    private ThreadView view(AiChatThread thread) {
        AssistantDefinitionEntity assistant = assistants.findByIdAndDomainId(thread.getAssistantId(), thread.getDomainId()).orElse(null);
        boolean available = eligible(assistant) && AiChatStore.sameAssistant(thread, assistant);
        String reason = available ? null : "assistantChanged";
        if ("EXPIRED".equals(thread.getStatus())) reason = "expired";
        if ("DELETE_PENDING".equals(thread.getStatus())) reason = "deletePending";
        if (reason == null && !configuration.chatScope(configuration.resolveForDomain(provider(thread.getProvider()), thread.getDomainName()))
            .equals(thread.getProviderScope())) reason = "providerChanged";
        return new ThreadView(thread.getId(), thread.getTitle(), thread.getAssistantId(), thread.getAssistantName(), thread.getProvider(),
            thread.getModel(), thread.getCreated(), thread.getUpdated(), thread.getStatus(), thread.getRevision(),
            thread.getActiveMessageId() != null, reason == null, reason, thread.getDeletionError());
    }

    private static MessageView view(String threadId, AiChatEntry message) {
        return new MessageView(message.getId(), message.getTurnId(), message.getRequestId(), message.getRole(), message.getText(),
            message.getStatus(), message.getCreated(), message.getTotalTokens(), message.getErrorCategory(), message.getProviderStatus(),
            message.getAttachments().stream().map(attachment -> new AttachmentView(attachment.id(), attachment.name(), attachment.mediaType(),
                attachment.size(), "/admin/rest/ai/chat/threads/" + threadId + "/attachments/" + attachment.id())).toList());
    }

    private boolean eligible(AssistantDefinitionEntity assistant) {
        if (assistant == null || Boolean.FALSE.equals(assistant.getActive()) || !Boolean.FALSE.equals(assistant.getUseTemporal())
            || !"live_chat".equals(assistant.getAction()) || !List.of("openai", "gemini").contains(assistant.getProvider())) return false;
        return providers.stream().anyMatch(p -> p.getProviderId().equals(assistant.getProvider()) && p.isInit()
            && p instanceof LibrarySupportLogic library && library.supportsPersistentChat(assistant));
    }

    private LibrarySupportLogic provider(String id) {
        return providers.stream().filter(p -> p.getProviderId().equals(id) && p instanceof LibrarySupportLogic)
            .map(p -> (LibrarySupportLogic)p).findFirst().orElseThrow(() -> new IllegalStateException("Provider unavailable"));
    }

    private static AssistantResponseDTO savedResult(AiChatEntry message, HttpServletRequest request) {
        if (!"complete".equals(message.getStatus())) {
            return failure(message.getErrorCategory() == null ? "PENDING" : message.getErrorCategory(), message.getProviderStatus(), request);
        }
        AssistantResponseDTO result = new AssistantResponseDTO();
        result.setResponse(message.getText());
        result.setTotalTokens(message.getTotalTokens());
        return result;
    }

    private static AssistantResponseDTO failure(String category, Integer status, HttpServletRequest request) {
        AssistantResponseDTO result = new AssistantResponseDTO();
        result.setError(Prop.getInstance(request).getText("components.ai_assistants.chat.history."
            + ("ATTACHMENT_INVALID".equals(category) ? "attachmentUnreadable" : "INVALID_CONTINUATION".equals(category) ? "expired"
                : "CANCELLED".equals(category) ? "stopped" : "PENDING".equals(category) ? "busy" : "generationFailed") + ".js"));
        result.setErrorCategory(category);
        result.setProviderStatusCode(status);
        result.setRetryable(!List.of("PENDING", "INVALID_CONTINUATION", "UNSUPPORTED_OPERATION", "ATTACHMENT_INVALID").contains(category));
        return result;
    }

    private static void audit(String id, String action, String category) {
        Adminlog.add(Adminlog.TYPE_AI, "Chat " + id + ": " + action + (category == null ? "" : " (" + category + ")"), -1, -1);
    }

    @Override
    public void destroy() { heartbeat.shutdownNow(); }
}
