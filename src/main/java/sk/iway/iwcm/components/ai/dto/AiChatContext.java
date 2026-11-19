package sk.iway.iwcm.components.ai.dto;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Identifies the edited record and target field within the owner's private chat history. */
public record AiChatContext(String entityType, String entityId, String fieldName) {
    public AiChatContext {
        if (invalid(entityType) || invalid(entityId) || invalid(fieldName)
            || "0".equals(entityId) || "-1".equals(entityId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalidInput");
        }
    }

    private static boolean invalid(String value) {
        return value == null || value.isBlank() || value.length() > 512;
    }

    /** Requires a complete context whenever any field filter is supplied. */
    public static AiChatContext from(String entityType, String entityId, String fieldName) {
        if (entityType == null && entityId == null && fieldName == null) return null;
        return new AiChatContext(entityType, entityId, fieldName);
    }
}
