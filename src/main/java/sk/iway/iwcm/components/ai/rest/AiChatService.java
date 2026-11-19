package sk.iway.iwcm.components.ai.rest;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiErrorCategory;
import com.webjetcms.ai.AiProviderException;
import com.webjetcms.ai.AiRequestLifecycle;
import com.webjetcms.ai.AiRequestLifecycle.State;
import com.webjetcms.ai.AiResponse;
import com.webjetcms.ai.TokenUsage;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
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
import sk.iway.iwcm.utils.Pair;

/** Coordinates durable chat history with provider execution and explicit remote deletion. */
@Service
@RequiredArgsConstructor
public class AiChatService implements DisposableBean {
    private final AiChatStore store;
    private final AssistantDefinitionRepository assistants;
    private final List<AiInterface> providers;
    private final AiTaskRegistry tasks;
    private final AiClient aiClient;
    private final WebjetAiConfigurationService configuration;
    private final AiStatRepository statistics;
    private final ScheduledExecutorService heartbeat = Executors.newScheduledThreadPool(1, task -> {
        Thread thread = new Thread(task, "webjet-ai-chat-lease");
        thread.setDaemon(true);
        return thread;
    });

    public record ThreadView(String id, String title, Long assistantId, String assistantName, String provider, String model,
        Date created, Date updated, String status, long revision, boolean busy, boolean canContinue,
        String unavailableReason, String deletionError, AiChatContext context) { }
    public record MessageView(String id, String turnId, String requestId, String role, String text, String status,
        Date created, int totalTokens, String errorCategory, Integer providerStatusCode, Boolean retryable, List<AttachmentView> attachments) { }
    public record AttachmentView(String id, String name, String mediaType, long size, String url) { }
    public record HistoryPage(List<ThreadView> items, boolean hasMore) { }
    public record Detail(ThreadView thread, List<MessageView> messages) { }
    public record Deletion(boolean deleted, String error) { }
    public record Prepared(Turn turn, AssistantDefinitionEntity assistant, Scope scope) { }

    public HistoryPage list(Scope scope, int page, String query) {
        return list(scope, page, query, null);
    }

    public HistoryPage list(Scope scope, int page, String query, AiChatContext context) {
        var result = store.list(scope, page, query, context);
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
        if (!eligible(assistant, input.context())) throw new ResponseStatusException(HttpStatus.CONFLICT, "assistantChanged");
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
            // Repeated HTTP delivery replays the saved attempt; it must not call the provider again.
            result = savedResult(turn.message(), request);
        } else {
            InputDataDTO input = turnInput(prepared);
            AtomicBoolean started = new AtomicBoolean();
            try {
                result = tasks.runChatTask(turn.thread().getId(), prepared.scope(), () -> {
                    started.set(true);
                    return generate(prepared, input, output, streaming, request);
                }, input);
            } catch (Exception exception) {
                Logger.error(AiChatService.class, "Chat " + turn.thread().getId() + ": execution failed", exception);
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
        Pair<String, String> inputPair = new Pair<>(input.getInputValue(), input.getUserPrompt());
        TokenUsage usage = TokenUsage.EMPTY;
        AssistantResponseDTO result = new AssistantResponseDTO();
        int tokens = 0;
        String status = "error";
        String category = "UNKNOWN";
        Integer providerStatus = null;
        Boolean retryable = null;
        AiRequestLifecycle lifecycle = AiRequestLifecycle.unknown();
        boolean submitted = false;
        var renewal = renewLease(prepared, input);
        try {
            if (assistants.countWithReachedTokenLimit(prepared.assistant().getId(), prepared.scope().domainId()) > 0) {
                category = "TOKEN_LIMIT_REACHED";
                result = failure(category, null, request);
                return result;
            }
            LibrarySupportLogic provider = provider(prepared.assistant().getProvider());
            var providerConfig = configuration.resolveForDomain(provider, prepared.scope().domainName());
            if (!configuration.chatScope(providerConfig).equals(prepared.turn().thread().getProviderScope())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "providerChanged");
            }
            Consumer<String> onText = streamProgress(prepared, text, output);
            if (input.getCancellationToken().isCancelled()) throw new CancellationException();
            // Strip HTML only from the saved field snapshot; document attachments retain their literal content.
            if (AiService.shouldRemoveHtml(prepared.assistant(), input)) {
                input.removeHtml();
            }
            StringBuilder context = new StringBuilder();
            for (var attachment : prepared.turn().question().getAttachments()) {
                if (input.getCancellationToken().isCancelled()) throw new CancellationException();
                byte[] bytes = store.attachment(threadId, attachment.id(), prepared.scope()).bytes();
                var preparedAttachment = provider.prepareChatAttachment(prepared.assistant(), input, providerConfig,
                    new AiChatAttachments.Upload(attachment, bytes));
                // Keep already incurred image costs even when a later attachment is unreadable.
                usage = usage.plus(preparedAttachment.usage());
                tokens = (int)Math.min(Integer.MAX_VALUE, usage.totalTokens());
                preparedAttachment.appendTo(context);
            }
            input.setInputValue(input.getInputValue() + context);
            // Once submitted, a remote response may exist even if the connection fails before its ID arrives.
            // Record any observed ID immediately so deletion can clean up unsuccessful attempts too.
            store.submitted(prepared.scope(), threadId, messageId);
            submitted = true;
            AiResponse response = provider.getPersistentChatResponse(prepared.assistant(), input, onText, streaming, providerConfig,
                responseId -> store.lifecycle(prepared.scope(), threadId, messageId, new AiRequestLifecycle(State.RESPONSE_OBSERVED, responseId)));
            usage = usage.plus(response.usage());
            tokens = (int)Math.min(Integer.MAX_VALUE, usage.totalTokens());
            if (input.getCancellationToken().isCancelled()) throw new CancellationException();
            // The completed provider response is authoritative; streamed text may contain only a partial answer.
            text.setLength(0);
            text.append(response.text());
            status = "complete";
            category = null;
            result = completedResponse(text.toString(), tokens);
            return result;
        } catch (AiChatAttachments.UnreadableException exception) {
            category = "ATTACHMENT_INVALID";
            result = failure(category, null, request);
            return result;
        } catch (AiProviderException exception) {
            category = exception.category().name();
            providerStatus = exception.statusCode() > 0 ? exception.statusCode() : null;
            retryable = exception.retryable();
            if (submitted) lifecycle = exception.lifecycle();
            status = exception.category() == AiErrorCategory.CANCELLED ? "stopped" : "error";
            result = failure(category, providerStatus, retryable, request);
            return result;
        } catch (Exception exception) {
            if (input.getCancellationToken().isCancelled() || exception instanceof CancellationException) {
                category = "CANCELLED";
                status = "stopped";
            } else {
                Logger.error(AiChatService.class, "Chat " + threadId + ": generation failed", exception);
            }
            result = failure(category, providerStatus, request);
            return result;
        } finally {
            renewal.cancel(false);
            // Cancellation interrupts the worker. Temporarily clear the flag so final filesystem writes can run.
            boolean interrupted = Thread.interrupted();
            try {
                // Account for completed calls even if a later attachment or the main generation failed.
                if ("complete".equals(status) || tokens > 0) recordUsage(prepared, tokens, request);
                auditResponse(prepared, inputPair, streaming, result, text.toString(), usage);
                try {
                    // Remote cleanup facts and successful continuation are separate: finish advances only the latter.
                    store.lifecycle(prepared.scope(), threadId, messageId, lifecycle);
                } finally {
                    store.finish(prepared.scope(), threadId, messageId, text.toString(), status, tokens, category, providerStatus, retryable);
                }
                cleanup(threadId, prepared.scope());
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
    }

    /** Builds provider input from the saved question, including the original snapshot on retries. */
    private static InputDataDTO turnInput(Prepared prepared) {
        Turn turn = prepared.turn();
        InputDataDTO input = new InputDataDTO();
        input.setAssistantId(prepared.assistant().getId());
        input.setInputValue(turn.question().getInputValue() == null ? "" : turn.question().getInputValue());
        input.setInputIsSelectedTextInEditor(turn.question().isInputIsSelectedTextInEditor());
        input.setInputValueType(InputDataDTO.InputValueType.TEXT);
        input.setOutputValueType(InputDataDTO.InputValueType.TEXT);
        input.setUserPrompt(turn.question().getText());
        input.setPreviousResponseId(turn.thread().getLastResponseId());
        return input;
    }

    /** Keeps the attempt alive between provider callbacks and observes stops from other cluster nodes. */
    private ScheduledFuture<?> renewLease(Prepared prepared, InputDataDTO input) {
        String threadId = prepared.turn().thread().getId();
        String messageId = prepared.turn().message().getId();
        return heartbeat.scheduleWithFixedDelay(() -> {
            try {
                if (!store.heartbeat(prepared.scope(), threadId, messageId)) input.getCancellationToken().cancel();
            } catch (RuntimeException exception) {
                Logger.error(AiChatService.class, "Chat " + threadId + ": lease renewal failed", exception);
                input.getCancellationToken().cancel();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    /** Saves partial text at most once per second while forwarding every delta to the client. */
    private Consumer<String> streamProgress(Prepared prepared, StringBuilder text, Consumer<String> output) {
        long[] checkpoint = {0};
        return delta -> {
            text.append(delta);
            if (System.currentTimeMillis() - checkpoint[0] >= 1000) {
                if (!store.progress(prepared.scope(), prepared.turn().thread().getId(), prepared.turn().message().getId(), text.toString())) {
                    throw new CancellationException();
                }
                checkpoint[0] = System.currentTimeMillis();
            }
            output.accept(delta);
        };
    }

    private void recordUsage(Prepared prepared, int tokens, HttpServletRequest request) {
        try {
            AiStatService.addRecord(prepared.assistant().getId(), tokens, statistics, request, prepared.scope().domainId());
        } catch (RuntimeException exception) {
            String threadId = prepared.turn().thread().getId();
            Logger.error(AiChatService.class, "Chat " + threadId + ": statistics failed", exception);
            audit(threadId, "statistics_failed", null);
        }
    }

    /** Uses the assistant audit format while retaining saved-conversation identifiers and failure details. */
    private void auditResponse(Prepared prepared, Pair<String, String> input, boolean streaming,
        AssistantResponseDTO result, String text, TokenUsage usage) {
        try {
            String status = result.getError() == null ? "SUCCESS" : "ERROR";
            if ("CANCELLED".equals(result.getErrorCategory())) status = "CANCELLED";
            String method = streaming ? "getPersistentChatStreamResponse" : "getPersistentChatResponse";
            StringBuilder audit = AiAuditLog.create(status, prepared.assistant(), method);
            audit.append("\nThread ID: ").append(prepared.turn().thread().getId());
            audit.append("\nMessage ID: ").append(prepared.turn().message().getId()).append('\n');
            AiAuditLog.appendUsage(audit, usage);
            int totalTokens = (int)Math.min(Integer.MAX_VALUE, usage.totalTokens());
            if (result.getError() != null) audit.append("\nError message:\n").append(result.getError());
            if (result.getErrorCategory() != null) audit.append("\nError category: ").append(result.getErrorCategory());
            if (result.getProviderStatusCode() != null) audit.append("\nProvider status: ").append(result.getProviderStatusCode());
            AiAuditLog.appendResponse(audit, input, text, configuration.auditMaxLength());
            Adminlog.add(Adminlog.TYPE_AI, audit.toString(), totalTokens, -1);
        } catch (RuntimeException exception) {
            Logger.error(AiChatService.class, "Chat " + prepared.turn().thread().getId() + ": audit failed", exception);
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

    /** Removes inactive conversations older than the cutoff and retries pending deletions in this owner scope.
     * @return number of conversations whose remote responses and local files were removed
     */
    public int deleteOlderThan(Scope scope, Date cutoff) {
        int deleted = 0;
        for (String id : store.olderThan(scope, cutoff)) {
            try {
                if (store.stopOlderThan(id, scope, cutoff) && cleanup(id, scope).deleted()) deleted++;
            } catch (RuntimeException exception) {
                Logger.error(AiChatService.class, "Chat " + id + ": retention cleanup failed", exception);
            }
        }
        return deleted;
    }

    private Deletion cleanup(String id, Scope scope) {
        // Claim deletion under the store lock, then release it before making remote calls.
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
                    aiClient.deleteResponse(provider.getProviderId(), message.getProviderResponseId(), config);
                    // Persist each success so retrying deletion skips resources that are already gone.
                    store.deletedResponse(scope, id, claim, message.getId());
                }
            }
            // The store refuses removal while an attempt may still own an unknown remote response.
            store.remove(scope, id, claim);
            audit(id, "deleted", null);
            return new Deletion(true, null);
        } catch (Exception exception) {
            String error = exception instanceof ResponseStatusException failure && "cleanupUnknown".equals(failure.getReason())
                ? "cleanupUnknown" : "deleteFailed";
            if ("deleteFailed".equals(error)) Logger.error(AiChatService.class, "Chat " + id + ": deletion failed", exception);
            store.deletionFailed(scope, id, claim, error);
            audit(id, "delete_failed", null);
            return new Deletion(false, error);
        }
    }

    private ThreadView view(AiChatThread thread) {
        AssistantDefinitionEntity assistant = assistants.findByIdAndDomainId(thread.getAssistantId(), thread.getDomainId()).orElse(null);
        boolean available = eligible(assistant, thread.getContext()) && AiChatStore.sameAssistant(thread, assistant);
        String reason = available ? null : "assistantChanged";
        if ("EXPIRED".equals(thread.getStatus())) reason = "expired";
        if ("DELETE_PENDING".equals(thread.getStatus())) reason = "deletePending";
        if (reason == null && !configuration.chatScope(configuration.resolveForDomain(provider(thread.getProvider()), thread.getDomainName()))
            .equals(thread.getProviderScope())) reason = "providerChanged";
        return new ThreadView(thread.getId(), thread.getTitle(), thread.getAssistantId(), thread.getAssistantName(), thread.getProvider(),
            thread.getModel(), thread.getCreated(), thread.getUpdated(), thread.getStatus(), thread.getRevision(),
            thread.getActiveMessageId() != null, reason == null, reason, thread.getDeletionError(), thread.getContext());
    }

    private static MessageView view(String threadId, AiChatEntry message) {
        return new MessageView(message.getId(), message.getTurnId(), message.getRequestId(), message.getRole(), message.getText(),
            message.getStatus(), message.getCreated(), message.getTotalTokens(), message.getErrorCategory(), message.getProviderStatus(),
            message.getRetryable(),
            message.getAttachments().stream().map(attachment -> new AttachmentView(attachment.id(), attachment.name(), attachment.mediaType(),
                attachment.size(), "/admin/rest/ai/chat/threads/" + threadId + "/attachments/" + attachment.id())).toList());
    }

    private boolean eligible(AssistantDefinitionEntity assistant, AiChatContext context) {
        if (assistant == null || Boolean.FALSE.equals(assistant.getActive()) || !Boolean.FALSE.equals(assistant.getUseTemporal())
            || !"live_chat".equals(assistant.getAction()) || !List.of("openai", "gemini").contains(assistant.getProvider())) return false;
        if (context == null && !AiAssistantsService.isGlobalChatAssistant(assistant)) return false;
        return providers.stream().anyMatch(p -> p.getProviderId().equals(assistant.getProvider()) && p.isInit()
            && p instanceof LibrarySupportLogic library && library.supportsPersistentChat(assistant));
    }

    private LibrarySupportLogic provider(String id) {
        return providers.stream().filter(p -> p.getProviderId().equals(id) && p instanceof LibrarySupportLogic)
            .map(p -> (LibrarySupportLogic)p).findFirst().orElseThrow(() -> new IllegalStateException("Provider unavailable"));
    }

    private static AssistantResponseDTO savedResult(AiChatEntry message, HttpServletRequest request) {
        if (!"complete".equals(message.getStatus())) {
            return failure(message.getErrorCategory() == null ? "PENDING" : message.getErrorCategory(), message.getProviderStatus(),
                message.getRetryable(), request);
        }
        return completedResponse(message.getText(), message.getTotalTokens());
    }

    private static AssistantResponseDTO completedResponse(String text, int tokens) {
        AssistantResponseDTO result = new AssistantResponseDTO();
        result.setResponse(text);
        result.setTotalTokens(tokens);
        return result;
    }

    private static AssistantResponseDTO failure(String category, Integer status, HttpServletRequest request) {
        return failure(category, status, null, request);
    }

    private static AssistantResponseDTO failure(String category, Integer status, Boolean retryable, HttpServletRequest request) {
        AssistantResponseDTO result = new AssistantResponseDTO();
        String messageKey = switch (category) {
            case "TOKEN_LIMIT_REACHED" -> "components.ai_assistants.token_limit.reached.js";
            case "ATTACHMENT_INVALID" -> "components.ai_assistants.chat.history.attachmentUnreadable.js";
            case "INVALID_CONTINUATION" -> "components.ai_assistants.chat.history.expired.js";
            case "CANCELLED" -> "components.ai_assistants.chat.history.stopped.js";
            case "PENDING" -> "components.ai_assistants.chat.history.busy.js";
            default -> "components.ai_assistants.chat.history.generationFailed.js";
        };
        result.setError(Prop.getInstance(request).getText(messageKey));
        result.setErrorCategory(category);
        result.setProviderStatusCode(status);
        if (retryable == null) {
            retryable = !List.of("PENDING", "INVALID_CONTINUATION", "UNSUPPORTED_OPERATION", "ATTACHMENT_INVALID").contains(category);
        }
        result.setRetryable(retryable);
        return result;
    }

    private static void audit(String id, String action, String category) {
        Adminlog.add(Adminlog.TYPE_AI, "Chat " + id + ": " + action + (category == null ? "" : " (" + category + ")"), -1, -1);
    }

    @Override
    public void destroy() { heartbeat.shutdownNow(); }
}
