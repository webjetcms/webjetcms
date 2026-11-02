package sk.iway.iwcm.components.ai.rest;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.dto.AiChatRequest;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.rest.AiChatStore.Scope;
import sk.iway.iwcm.users.UsersDB;

/** Private chat endpoints; Provider identifiers never come from client-supplied history. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/rest/ai/chat")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_ai_button')")
public class AiChatController {
    private final AiChatService service;
    private final AiChatStore store;
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/threads")
    public AiChatService.HistoryPage list(@RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "") String query, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return service.list(scope(request), page, query);
    }

    @GetMapping("/threads/{id}")
    public AiChatService.Detail get(@PathVariable String id, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return service.detail(id, scope(request));
    }

    @PostMapping("/threads/{id}/stop")
    public Map<String, Boolean> stop(@PathVariable String id, HttpServletRequest request) {
        service.stop(id, scope(request));
        return Map.of("stopped", true);
    }

    @DeleteMapping("/threads/{id}")
    public AiChatService.Deletion delete(@PathVariable String id, HttpServletRequest request) {
        return service.delete(id, scope(request));
    }

    @PostMapping(value = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void send(@RequestBody AiChatRequest input, HttpServletRequest request, HttpServletResponse response) throws IOException {
        AiChatService.Prepared prepared = service.prepare(input, scope(request));
        send(prepared, request, response);
    }

    @PostMapping(value = "/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void upload(@RequestParam String message, @RequestParam("files") List<MultipartFile> files,
        HttpServletRequest request, HttpServletResponse response) throws IOException {
        Scope owner = scope(request);
        AiChatRequest input;
        try {
            input = mapper.readValue(message, AiChatRequest.class);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalidInput");
        }
        send(service.prepare(input, owner, AiChatAttachments.read(files)), request, response);
    }

    @GetMapping("/threads/{id}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> attachment(@PathVariable String id, @PathVariable String attachmentId,
        @RequestParam(defaultValue = "false") boolean download, HttpServletRequest request) {
        var content = store.attachment(id, attachmentId, scope(request));
        var attachment = content.attachment();
        ContentDisposition disposition = (!download && attachment.mediaType().startsWith("image/")
            ? ContentDisposition.inline() : ContentDisposition.attachment()).filename(attachment.name(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(attachment.mediaType()))
            .header("Cache-Control", "no-store").header("X-Content-Type-Options", "nosniff")
            .header("Content-Security-Policy", "default-src 'none'; sandbox")
            .header("Content-Disposition", disposition.toString()).body(content.bytes());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", "attachmentLimit"));
    }

    private void send(AiChatService.Prepared prepared, HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean streaming = request.getHeader("Accept") != null && request.getHeader("Accept").contains(MediaType.APPLICATION_NDJSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("X-AI-Thread-Id", prepared.turn().thread().getId());
        response.setContentType((streaming ? MediaType.APPLICATION_NDJSON_VALUE : MediaType.APPLICATION_JSON_VALUE) + ";charset=UTF-8");
        if (!streaming) {
            AssistantResponseDTO result = service.execute(prepared, text -> { }, false, request);
            mapper.writeValue(response.getOutputStream(), result);
            return;
        }
        AssistantStreamWriter writer = new AssistantStreamWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        AtomicBoolean writable = new AtomicBoolean(true);
        try {
            writer.writeMetadata(service.metadata(prepared));
        } catch (IOException exception) {
            if (!prepared.turn().duplicate()) store.finish(prepared.scope(), prepared.turn().thread().getId(), prepared.turn().message().getId(),
                "", "stopped", 0, "CANCELLED", null);
            throw exception;
        }
        try {
            AssistantResponseDTO result = service.execute(prepared, text -> {
                synchronized (writer) {
                    if (!writable.get()) throw new CancellationException();
                    try {
                        writer.write(text);
                        writer.flush();
                    } catch (IOException exception) {
                        writable.set(false);
                        throw new CancellationException();
                    }
                }
            }, true, request);
            synchronized (writer) {
                if (writable.getAndSet(false)) writer.writeResult(result);
            }
        } finally {
            synchronized (writer) {
                writable.set(false);
                writer.close();
            }
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("error", exception.getReason() == null ? "generationFailed" : exception.getReason()));
    }

    private static Scope scope(HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || user.getUserId() < 1) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "signedOut");
        return new Scope(user.getUserId(), CloudToolsForCore.getDomainId(), CloudToolsForCore.getDomainName());
    }
}
