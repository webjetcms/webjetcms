package sk.iway.iwcm.components.ai.rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.webjetcms.ai.AiRequestLifecycle;
import com.webjetcms.ai.AiRequestLifecycle.State;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.chat.AiChatDocument;
import sk.iway.iwcm.components.ai.chat.AiChatEntry;
import sk.iway.iwcm.components.ai.chat.AiChatEntry.WriteOutcome;
import sk.iway.iwcm.components.ai.chat.AiChatFiles;
import sk.iway.iwcm.components.ai.chat.AiChatPaths;
import sk.iway.iwcm.components.ai.chat.AiChatThread;
import sk.iway.iwcm.components.ai.chat.AiChatAttachment;
import sk.iway.iwcm.components.ai.rest.AiChatAttachments.Upload;
import sk.iway.iwcm.components.ai.dto.AiChatRequest;
import sk.iway.iwcm.components.ai.dto.AiChatContext;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.tools.AiToolProtocol;
import sk.iway.iwcm.components.ai.tools.AiToolProtocol.Call;
import sk.iway.iwcm.i18n.Prop;

/** Private JSON conversations. Provider calls never hold filesystem locks. */
@Service
public class AiChatStore {
    public record Scope(int userId, int domainId, String domainName, Path root) {
        public Scope(int userId, int domainId, String domainName) {
            this(userId, domainId, domainName, storageRoot());
        }

        /** Resolves trusted deployment links even before the chat storage directory exists. */
        static Path storageRoot() {
            try {
                return new File(Tools.getRealPath(AiChatPaths.ROOT)).getCanonicalFile().toPath();
            } catch (IOException exception) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "storageUnavailable", exception);
            }
        }
    }
    public record Turn(AiChatThread thread, AiChatEntry question, AiChatEntry message, boolean duplicate) { }
    public record Transcript(AiChatThread thread, List<AiChatEntry> messages) { }
    public record History(List<AiChatThread> items, boolean hasMore) { }

    public History list(Scope scope, int page) {
        return list(scope, page, "");
    }

    public History list(Scope scope, int page, String query) {
        return list(scope, page, query, null);
    }

    public History list(Scope scope, int page, String query, AiChatContext context) {
        String search = normalizeSearch(query).strip();
        return locked(scope, directory -> {
            List<AiChatThread> history = new ArrayList<>();
            try (var folders = Files.newDirectoryStream(directory, path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))) {
                for (Path folder : folders) {
                    Path path = threadPath(directory, folder.getFileName().toString());
                    AiChatDocument document = AiChatFiles.read(path);
                    if (document == null) continue;
                    AiChatThread thread = document.getThread();
                    validateFile(path, thread);
                    if (belongsTo(thread, scope) && Objects.equals(thread.getContext(), context)
                        && matchesSearch(document, search)) history.add(thread);
                }
            }
            history.sort(Comparator.comparing(AiChatThread::getUpdated).reversed().thenComparing(AiChatThread::getId, Comparator.reverseOrder()));
            int start = (int)Math.min(history.size(), Math.max(0L, page) * 30L);
            int end = Math.min(history.size(), start + 30);
            return new History(List.copyOf(history.subList(start, end)), end < history.size());
        });
    }

    private static boolean matchesSearch(AiChatDocument document, String search) {
        if (search.isEmpty()) return true;
        AiChatThread thread = document.getThread();
        return normalizeSearch(thread.getTitle()).contains(search)
            || normalizeSearch(thread.getAssistantName()).contains(search)
            || normalizeSearch(thread.getProvider()).contains(search)
            || document.getMessages().stream().anyMatch(message -> normalizeSearch(message.getText()).contains(search));
    }

    private static String normalizeSearch(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    public Transcript get(String id, Scope scope) {
        requireUuid(id);
        return locked(scope, directory -> {
            Path path = threadPath(directory, id);
            AiChatDocument document = read(path, scope, true);
            if (recover(document)) AiChatFiles.write(path, document);
            return new Transcript(document.getThread(), ordered(document));
        });
    }

    public Turn begin(AiChatRequest input, Scope scope, AssistantDefinitionEntity assistant, String providerScope) {
        return begin(input, scope, assistant, providerScope, List.of());
    }

    public Turn begin(AiChatRequest input, Scope scope, AssistantDefinitionEntity assistant, String providerScope, List<Upload> uploads) {
        requireUuid(input.requestId());
        try {
            AiToolProtocol.validateResults(input.toolResults());
        } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalidInput");
        }
        // A new thread uses the request ID so resending its first message finds the same conversation.
        String id = input.threadId() == null ? input.requestId() : input.threadId();
        requireUuid(id);
        return locked(scope, directory -> {
            Path path = threadPath(directory, id);
            AiChatDocument document = read(path, scope, input.threadId() != null);
            if (document == null) document = createDocument(id, input, scope, assistant, providerScope);
            AiChatThread thread = document.getThread();
            if (!Objects.equals(thread.getContext(), input.context())) throw conflict("requestChanged");
            boolean recovered = recover(document);
            // Check duplicate delivery before revision/busy checks: the original request already advanced them.
            Turn duplicate = duplicateTurn(document, input, uploads);
            if (duplicate != null) {
                if (input.toolResults().size() > duplicate.message().getToolResults().size()) {
                    if (!sameAssistant(thread, assistant) || !providerScope.equals(thread.getProviderScope())) throw conflict("assistantChanged");
                    resumeToolRound(duplicate, input);
                    AiChatFiles.write(path, document);
                    return new Turn(thread, duplicate.question(), duplicate.message(), false);
                }
                if (recovered) AiChatFiles.write(path, document);
                return duplicate;
            }
            if (!input.toolResults().isEmpty()) throw conflict("requestChanged");
            // A genuinely new attempt must still match the configuration and revision the client last read.
            if ("EXPIRED".equals(thread.getStatus())) throw conflict("expired");
            if (!"ACTIVE".equals(thread.getStatus())) throw conflict("deletePending");
            if (thread.getActiveMessageId() != null) throw conflict("busy");
            if (!sameAssistant(thread, assistant)) throw conflict("assistantChanged");
            if (!providerScope.equals(thread.getProviderScope())) throw conflict("providerChanged");
            if (input.threadId() != null && !Objects.equals(input.revision(), thread.getRevision())) throw conflict("stale");

            List<Upload> storedUploads = AiChatAttachments.numbered(path.getParent(),
                document.getMessages().stream().flatMap(message -> message.getAttachments().stream()).toList(), uploads);
            AiChatEntry question = prepareQuestion(document, input, storedUploads);
            // Reserve the answer before provider execution so another request cannot start a competing turn.
            AiChatEntry answer = message(document, input.requestId(), "assistant", question.getId());
            answer.setStatus("pending");
            thread.setRevision(thread.getRevision() + 1);
            thread.setActiveMessageId(answer.getId());
            thread.setStopRequested(false);
            thread.setLeaseUntil(lease());
            thread.setUpdated(new Date());
            // Publish JSON last so a saved message only references uploads that were written successfully.
            writeTurn(directory, path, document, storedUploads);
            return new Turn(thread, question, answer, false);
        });
    }

    /** Captures the owner and configuration that subsequent turns must continue to match. */
    private static AiChatDocument createDocument(String id, AiChatRequest input, Scope scope,
        AssistantDefinitionEntity assistant, String providerScope) {
        AiChatThread thread = new AiChatThread();
        thread.setId(id);
        thread.setUserId(scope.userId());
        thread.setDomainId(scope.domainId());
        thread.setDomainName(scope.domainName());
        thread.setAssistantId(assistant.getId());
        thread.setAssistantName(assistant.getName());
        if (Tools.isNotEmpty(assistant.getDescription())) {
            thread.setAssistantName(assistant.getDisplayName(Prop.getInstance()));
        }
        thread.setProvider(assistant.getProvider());
        thread.setProviderScope(providerScope);
        thread.setModel(assistant.getModel());
        thread.setAssistantVersion(assistantVersion(assistant));
        thread.setContext(input.context());
        String title = input.userPrompt().strip().replaceAll("(?U)\\s+", " ");
        thread.setTitle(title.substring(0, title.offsetByCodePoints(0, Math.min(80, title.codePointCount(0, title.length())))));
        thread.setCreated(new Date());
        thread.setUpdated(thread.getCreated());
        AiChatDocument document = new AiChatDocument();
        document.setThread(thread);
        return document;
    }

    /** Replays the same HTTP delivery only when its original question and uploads still match. */
    private static Turn duplicateTurn(AiChatDocument document, AiChatRequest input, List<Upload> uploads) {
        AiChatEntry answer = document.getMessages().stream()
            .filter(message -> input.requestId().equals(message.getRequestId()) && "assistant".equals(message.getRole()))
            .findFirst().orElse(null);
        if (answer == null) return null;
        AiChatEntry question = entry(document, answer.getTurnId());
        if (!Objects.equals(question.getText(), input.userPrompt()) || !Objects.equals(document.getThread().getAssistantId(), input.assistantId())) throw conflict("requestChanged");
        int shared = Math.min(input.toolResults().size(), answer.getToolResults().size());
        if (shared > 0 && !AiToolProtocol.sameJson(input.toolResults().subList(0, shared),
                answer.getToolResults().subList(0, shared))) throw conflict("requestChanged");
        if (input.retryMessageId() == null && (!Objects.equals(question.getInputValue(), input.inputValue())
            || question.isInputIsSelectedTextInEditor() != input.inputIsSelectedTextInEditor()
            || (input.toolResults().isEmpty() && !sameAttachments(question.getAttachments(), uploads)))) throw conflict("requestChanged");
        if (!input.toolResults().isEmpty() && !uploads.isEmpty()) throw conflict("requestChanged");
        return new Turn(document.getThread(), question, answer, true);
    }

    /** Only the next matching endpoint result can claim another model round. */
    private static void resumeToolRound(Turn turn, AiChatRequest input) {
        AiChatEntry answer = turn.message();
        AiChatThread thread = turn.thread();
        Call pending = answer.getPendingToolCall();
        if (!"ACTIVE".equals(thread.getStatus()) || thread.isStopRequested() || !answer.getId().equals(thread.getActiveMessageId())
                || pending == null || input.threadId() == null || !Objects.equals(input.revision(), thread.getRevision())
                || input.toolResults().size() != answer.getToolResults().size() + 1) throw conflict("stale");
        var result = input.toolResults().get(input.toolResults().size() - 1);
        if (!pending.name().equals(result.name()) || !AiToolProtocol.sameJson(pending.arguments(), result.arguments())) throw conflict("requestChanged");
        if (pending.write()) {
            List<WriteOutcome> outcomes = answer.getWriteOutcomes();
            var outcome = WriteOutcome.from(result);
            if (outcomes.isEmpty()) outcomes.add(outcome); // A pending call saved before write outcomes were recorded.
            else outcomes.set(outcomes.size() - 1, outcome);
        }
        answer.setToolResults(input.toolResults());
        answer.setPendingToolCall(null);
        thread.setLeaseUntil(lease());
    }

    /** A retry reuses the saved field snapshot and attachments; a new prompt captures them once. */
    private static AiChatEntry prepareQuestion(AiChatDocument document, AiChatRequest input, List<Upload> uploads) {
        if (input.retryMessageId() != null) {
            if (!uploads.isEmpty()) throw conflict("requestChanged");
            List<AiChatEntry> history = ordered(document);
            AiChatEntry last = history.isEmpty() ? null : history.get(history.size() - 1);
            if (last == null || !last.getId().equals(input.retryMessageId())
                || !("error".equals(last.getStatus()) || "stopped".equals(last.getStatus()))) throw conflict("stale");
            if (last.isToolWriteIssued()) throw conflict("requestChanged");
            AiChatEntry question = entry(document, last.getTurnId());
            if (!Objects.equals(question.getText(), input.userPrompt())) throw conflict("requestChanged");
            return question;
        }
        AiChatEntry question = message(document, input.requestId(), "user", null);
        question.setTurnId(question.getId());
        question.setText(input.userPrompt());
        question.setInputValue(input.inputValue());
        question.setInputIsSelectedTextInEditor(input.inputIsSelectedTextInEditor());
        question.setStatus("complete");
        question.setAttachments(uploads.stream().map(Upload::attachment).toList());
        return question;
    }

    /** Writes uploads and the transcript under the owner lock, removing new uploads if the save fails. */
    private static void writeTurn(Path directory, Path path, AiChatDocument document, List<Upload> uploads) throws IOException {
        List<Path> written = new ArrayList<>();
        try {
            AiChatFiles.directory(path.getParent());
            for (Upload upload : uploads) {
                Path attachmentPath = AiChatAttachments.path(directory, document.getThread().getId(), upload.attachment().id());
                try (var output = Files.newOutputStream(attachmentPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS)) {
                    written.add(attachmentPath);
                    if (Files.getFileStore(attachmentPath).supportsFileAttributeView("posix")) {
                        Files.setPosixFilePermissions(attachmentPath, PosixFilePermissions.fromString("rw-------"));
                    }
                    output.write(upload.bytes());
                }
            }
            AiChatFiles.write(path, document);
        } catch (IOException exception) {
            for (Path attachmentPath : written) {
                try {
                    Files.deleteIfExists(attachmentPath);
                } catch (IOException cleanupFailure) {
                    exception.addSuppressed(cleanupFailure);
                }
            }
            throw exception;
        }
    }

    public record AttachmentContent(AiChatAttachment attachment, byte[] bytes) { }

    /** Resolves attachment membership only after verifying conversation ownership and domain. */
    public AttachmentContent attachment(String id, String attachmentId, Scope scope) {
        requireUuid(id);
        return locked(scope, directory -> {
            AiChatDocument document = read(threadPath(directory, id), scope, true);
            AiChatAttachment attachment = document.getMessages().stream().flatMap(message -> message.getAttachments().stream())
                .filter(item -> attachmentId.equals(item.id())).findFirst().orElseThrow(AiChatStore::missing);
            try (var input = Files.newInputStream(AiChatAttachments.path(directory, id, attachmentId), LinkOption.NOFOLLOW_LINKS)) {
                byte[] bytes = input.readNBytes(AiChatAttachments.MAX_BYTES + 1);
                if (bytes.length != attachment.size() || bytes.length > AiChatAttachments.MAX_BYTES) throw new IOException("Invalid attachment size");
                return new AttachmentContent(attachment, bytes);
            }
        });
    }

    private static boolean sameAttachments(List<AiChatAttachment> saved, List<Upload> uploads) {
        if (saved.size() != uploads.size()) return false;
        for (int i = 0; i < saved.size(); i++) {
            AiChatAttachment item = saved.get(i);
            AiChatAttachment upload = uploads.get(i).attachment();
            if (!Objects.equals(item.name(), upload.name()) || !Objects.equals(item.mediaType(), upload.mediaType())
                || !Objects.equals(item.sha256(), upload.sha256())) return false;
        }
        return true;
    }

    public boolean heartbeat(Scope scope, String threadId, String messageId) {
        return update(threadId, scope, false, document -> {
            if (document == null || !messageId.equals(document.getThread().getActiveMessageId())) return false;
            AiChatThread thread = document.getThread();
            thread.setLeaseUntil(lease());
            return !thread.isStopRequested() && "ACTIVE".equals(thread.getStatus());
        });
    }

    public void submitted(Scope scope, String threadId, String messageId) {
        update(threadId, scope, true, document -> {
            AiChatThread thread = document.getThread();
            if (!messageId.equals(thread.getActiveMessageId()) || thread.isStopRequested()) throw conflict("stopped");
            entry(document, messageId).setStorageUnknown(true);
            return null;
        });
    }

    /** Persists provider facts for cleanup without advancing successful continuation. */
    public void lifecycle(Scope scope, String threadId, String messageId, AiRequestLifecycle lifecycle) {
        String responseId = lifecycle.responseId();
        if (lifecycle.state() == State.UNKNOWN || (lifecycle.state() == State.RESPONSE_OBSERVED && responseId == null)) return;
        if (responseId != null && responseId.length() > 2000) throw conflict("invalidResponse");
        update(threadId, scope, true, document -> {
            AiChatEntry message = entry(document, messageId);
            if (responseId != null && !responseId.equals(message.getProviderResponseId())) {
                if (message.getProviderResponseId() != null && !message.isProviderDeleted()) {
                    message.getEarlierProviderResponseIds().add(message.getProviderResponseId());
                }
                message.setProviderResponseId(responseId);
                message.setProviderDeleted(false);
            }
            message.setStorageUnknown(false);
            return null;
        });
    }

    public boolean progress(Scope scope, String threadId, String messageId, String text) {
        return update(threadId, scope, true, document -> {
            AiChatThread thread = document.getThread();
            if (!messageId.equals(thread.getActiveMessageId())) return false;
            AiChatEntry message = entry(document, messageId);
            message.setText(text);
            message.setUpdated(new Date());
            return !thread.isStopRequested();
        });
    }

    /** Releases the provider worker while the browser invokes this call through its authenticated session. */
    public boolean awaitToolResult(Scope scope, String threadId, String messageId, Call call, int tokens) {
        return update(threadId, scope, true, document -> {
            AiChatThread thread = document.getThread();
            if (!messageId.equals(thread.getActiveMessageId()) || thread.isStopRequested()) return false;
            AiChatEntry answer = entry(document, messageId);
            answer.setPendingToolCall(call);
            if (call.write()) answer.getWriteOutcomes().add(WriteOutcome.pending(call.name()));
            answer.setToolWriteIssued(answer.isToolWriteIssued() || call.write());
            answer.setTotalTokens(tokens);
            answer.setUpdated(new Date());
            thread.setLeaseUntil(lease());
            return true;
        });
    }

    public void finish(Scope scope, String threadId, String messageId, String text, String status, int tokens,
        String category, Integer providerStatus) {
        finish(scope, threadId, messageId, text, status, tokens, category, providerStatus, null);
    }

    /** Persists turn completion together with the provider's optional retry recommendation. */
    public void finish(Scope scope, String threadId, String messageId, String text, String status, int tokens,
        String category, Integer providerStatus, Boolean retryable) {
        update(threadId, scope, false, document -> {
            if (document == null) return null;
            AiChatThread thread = document.getThread();
            AiChatEntry message = entry(document, messageId);
            if (!messageId.equals(thread.getActiveMessageId())) return null;
            boolean stopped = thread.isStopRequested();
            message.setText(text);
            message.setStatus(stopped ? "stopped" : status);
            message.setTotalTokens(tokens);
            message.setErrorCategory(stopped ? "CANCELLED" : category);
            message.setProviderStatus(providerStatus);
            message.setRetryable(message.isToolWriteIssued() && (stopped || !"complete".equals(status)) ? Boolean.FALSE : retryable);
            message.setPendingToolCall(null);
            message.setUpdated(new Date());
            // Failed/stopped attempts remain in history and cleanup tracking, but never become continuation points.
            if (!stopped && "complete".equals(status)) thread.setLastResponseId(message.getProviderResponseId());
            if (!stopped && "INVALID_CONTINUATION".equals(category)) thread.setStatus("EXPIRED");
            thread.setActiveMessageId(null);
            thread.setLeaseUntil(null);
            thread.setUpdated(new Date());
            return null;
        });
    }

    public void stop(String id, Scope scope, boolean delete) {
        update(id, scope, true, document -> {
            recover(document);
            AiChatThread thread = document.getThread();
            thread.setStopRequested(true);
            if (thread.getActiveMessageId() != null && entry(document, thread.getActiveMessageId()).getPendingToolCall() != null) {
                thread.setLeaseUntil(null);
                recover(document);
            }
            if (delete) {
                thread.setStatus("DELETE_PENDING");
                thread.setUpdated(new Date());
            }
            return null;
        });
    }

    /** Finds retention candidates across all editor contexts without paginating a changing history. */
    public List<String> olderThan(Scope scope, Date cutoff) {
        return locked(scope, directory -> {
            List<String> ids = new ArrayList<>();
            try (var folders = Files.newDirectoryStream(directory, path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))) {
                for (Path folder : folders) {
                    Path path = threadPath(directory, folder.getFileName().toString());
                    AiChatDocument document = AiChatFiles.read(path);
                    if (document == null) continue;
                    validateFile(path, document.getThread());
                    if (belongsTo(document.getThread(), scope) && retentionCandidate(document.getThread(), cutoff)) {
                        ids.add(document.getThread().getId());
                    }
                }
            }
            return ids;
        });
    }

    /** Rechecks activity under the owner lock before reserving an inactive conversation for deletion. */
    public boolean stopOlderThan(String id, Scope scope, Date cutoff) {
        requireUuid(id);
        return update(id, scope, false, document -> {
            if (document == null || !retentionCandidate(document.getThread(), cutoff)) return false;
            recover(document);
            AiChatThread thread = document.getThread();
            if (thread.getActiveMessageId() != null) return false;
            thread.setStopRequested(true);
            if (!"DELETE_PENDING".equals(thread.getStatus())) {
                thread.setStatus("DELETE_PENDING");
                thread.setUpdated(new Date());
            }
            return true;
        });
    }

    private static boolean retentionCandidate(AiChatThread thread, Date cutoff) {
        return "DELETE_PENDING".equals(thread.getStatus()) || thread.getUpdated().before(cutoff);
    }

    public String claimDeletion(String id, Scope scope) {
        return update(id, scope, true, document -> {
            recover(document);
            AiChatThread thread = document.getThread();
            if (!"DELETE_PENDING".equals(thread.getStatus()) || thread.getActiveMessageId() != null
                || (thread.getDeletionLeaseId() != null && thread.getLeaseUntil() != null
                    && thread.getLeaseUntil().after(new Date()))) return null;
            String claim = UUID.randomUUID().toString();
            thread.setDeletionLeaseId(claim);
            thread.setLeaseUntil(lease());
            thread.setDeletionError(null);
            return claim;
        });
    }

    public void deletedResponse(Scope scope, String id, String claim, String messageId) {
        deletedResponse(scope, id, claim, messageId, null);
    }

    public void deletedResponse(Scope scope, String id, String claim, String messageId, String responseId) {
        update(id, scope, true, document -> {
            deletionOwner(document, claim);
            AiChatEntry message = entry(document, messageId);
            if (responseId == null || responseId.equals(message.getProviderResponseId())) message.setProviderDeleted(true);
            else message.getEarlierProviderResponseIds().remove(responseId);
            document.getThread().setLeaseUntil(lease());
            return null;
        });
    }

    public void deletionFailed(Scope scope, String id, String claim, String error) {
        update(id, scope, false, document -> {
            if (document == null) return null;
            AiChatThread thread = document.getThread();
            if (!Objects.equals(claim, thread.getDeletionLeaseId()) || thread.getActiveMessageId() != null) return null;
            thread.setDeletionError(error);
            thread.setDeletionLeaseId(null);
            thread.setLeaseUntil(null);
            return null;
        });
    }

    public void remove(Scope scope, String id, String claim) {
        requireUuid(id);
        locked(scope, directory -> {
            Path path = threadPath(directory, id);
            AiChatDocument document = read(path, scope, true);
            deletionOwner(document, claim);
            for (AiChatEntry message : document.getMessages()) {
                if (message.isStorageUnknown() || !message.getEarlierProviderResponseIds().isEmpty()
                        || (message.getProviderResponseId() != null && !message.isProviderDeleted())) {
                    throw conflict("cleanupUnknown");
                }
            }
            try (var files = Files.newDirectoryStream(path.getParent())) {
                for (Path file : files) if (!file.equals(path)) Files.delete(file);
            }
            Files.delete(path);
            Files.delete(path.getParent());
            return null;
        });
    }

    private <T> T update(String id, Scope scope, boolean required, Function<AiChatDocument, T> action) {
        requireUuid(id);
        return locked(scope, directory -> {
            Path path = threadPath(directory, id);
            AiChatDocument document = read(path, scope, required);
            T result = action.apply(document);
            if (document != null) {
                document.getMessages().sort(Comparator.comparingLong(AiChatEntry::getSortOrder));
                AiChatFiles.write(path, document);
            }
            return result;
        });
    }

    private <T> T locked(Scope scope, AiChatFiles.Operation<T> action) {
        try {
            return AiChatFiles.locked(scope.root(), scope.domainId(), scope.userId(), action);
        } catch (IOException exception) {
            Logger.error(AiChatStore.class, "Chat storage failed for user " + scope.userId()
                + " in domain " + scope.domainName(), exception);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "storageUnavailable", exception);
        }
    }

    /** Resolves the current transcript without creating missing thread folders. */
    private static Path threadPath(Path directory, String id) throws IOException {
        requireUuid(id);
        Path folder = directory.resolve(id);
        if (Files.exists(folder, LinkOption.NOFOLLOW_LINKS)) AiChatFiles.directory(folder);
        return folder.resolve("ai_chat.json");
    }

    private static AiChatDocument read(Path path, Scope scope, boolean required) throws IOException {
        AiChatDocument document = AiChatFiles.read(path);
        if (document == null) {
            if (required) throw missing();
            return null;
        }
        validateFile(path, document.getThread());
        if (!belongsTo(document.getThread(), scope)) throw missing();
        return document;
    }

    private static void validateFile(Path path, AiChatThread thread) throws IOException {
        boolean matchesPath = "ai_chat.json".equals(path.getFileName().toString())
            && path.getParent().getFileName().toString().equals(thread.getId());
        if (thread.getId() == null || !matchesPath
            || thread.getUserId() == null || thread.getDomainId() == null || thread.getDomainName() == null
            || thread.getCreated() == null || thread.getUpdated() == null) throw new IOException("Invalid chat metadata");
    }

    private static void deletionOwner(AiChatDocument document, String claim) {
        AiChatThread thread = document.getThread();
        if (!Objects.equals(claim, thread.getDeletionLeaseId()) || thread.getActiveMessageId() != null) throw conflict("busy");
    }

    private static AiChatEntry entry(AiChatDocument document, String id) {
        return document.getMessages().stream().filter(m -> m.getId().equals(id)).findFirst().orElseThrow(AiChatStore::missing);
    }

    private static List<AiChatEntry> ordered(AiChatDocument document) {
        return document.getMessages().stream().sorted(Comparator.comparingLong(AiChatEntry::getSortOrder)).toList();
    }

    private static boolean recover(AiChatDocument document) {
        AiChatThread thread = document.getThread();
        if (thread.getActiveMessageId() == null || (thread.getLeaseUntil() != null && thread.getLeaseUntil().after(new Date()))) return false;
        // A worker no longer renews this attempt. Keep its partial text and remote-resource facts for later cleanup.
        AiChatEntry message = entry(document, thread.getActiveMessageId());
        message.setStatus("stopped");
        Call pending = message.getPendingToolCall();
        message.setErrorCategory(pending != null && pending.write() ? "TOOL_OUTCOME_UNKNOWN" : "CANCELLED");
        if (message.isToolWriteIssued()) message.setRetryable(false);
        message.setPendingToolCall(null);
        message.setUpdated(new Date());
        thread.setActiveMessageId(null);
        thread.setLeaseUntil(null);
        thread.setStopRequested(true);
        return true;
    }

    private static AiChatEntry message(AiChatDocument document, String requestId, String role, String turnId) {
        AiChatThread thread = document.getThread();
        AiChatEntry message = new AiChatEntry();
        message.setId(UUID.randomUUID().toString());
        message.setRequestId(requestId);
        message.setTurnId(turnId);
        message.setRole(role);
        message.setSortOrder(thread.getNextSequence());
        thread.setNextSequence(thread.getNextSequence() + 1);
        message.setText("");
        message.setCreated(new Date());
        message.setUpdated(message.getCreated());
        document.getMessages().add(message);
        return message;
    }

    public static boolean sameAssistant(AiChatThread thread, AssistantDefinitionEntity assistant) {
        return assistant != null && Objects.equals(thread.getAssistantId(), assistant.getId())
            && Objects.equals(thread.getProvider(), assistant.getProvider()) && Objects.equals(thread.getModel(), assistant.getModel())
            && Objects.equals(thread.getAssistantVersion(), assistantVersion(assistant));
    }

    private static Long assistantVersion(AssistantDefinitionEntity assistant) {
        return assistant.getLastUpdate() == null ? null : assistant.getLastUpdate().getTime();
    }

    private static boolean belongsTo(AiChatThread thread, Scope scope) {
        return Objects.equals(thread.getUserId(), scope.userId()) && Objects.equals(thread.getDomainId(), scope.domainId())
            && Objects.equals(thread.getDomainName(), scope.domainName());
    }

    private static Date lease() {
        return new Date(System.currentTimeMillis() + Math.max(120_000L,
            Constants.getInt("ai_providerResponseTimeoutSeconds") * 1000L + 60_000L));
    }

    public static void requireUuid(String id) {
        try {
            if (id == null || id.length() != 36 || !UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalidId");
        }
    }

    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "notFound"); }
    private static ResponseStatusException conflict(String reason) { return new ResponseStatusException(HttpStatus.CONFLICT, reason); }
}
