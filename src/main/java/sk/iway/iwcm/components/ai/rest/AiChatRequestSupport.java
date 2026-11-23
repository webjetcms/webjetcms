package sk.iway.iwcm.components.ai.rest;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Shared multipart message format and pre-stream JSON errors for saved and session-only chats. */
abstract class AiChatRequestSupport {
    final ObjectMapper mapper = new ObjectMapper();

    <T> T readMessage(String message, Class<T> type) {
        try {
            return mapper.readValue(message, type);
        } catch (IOException invalidJson) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalidInput");
        }
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge() {
        return error(new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "attachmentLimit"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("error", exception.getReason() == null ? "generationFailed" : exception.getReason()));
    }
}
