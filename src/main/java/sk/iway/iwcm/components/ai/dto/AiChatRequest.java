package sk.iway.iwcm.components.ai.dto;

/** Only user-controlled chat input; provider IDs, ownership and history are resolved on the server. */
public record AiChatRequest(String threadId, Long assistantId, String requestId, Long revision,
    String userPrompt, String retryMessageId, AiChatContext context, String inputValue) {
    public AiChatRequest(String threadId, Long assistantId, String requestId, Long revision,
        String userPrompt, String retryMessageId) {
        this(threadId, assistantId, requestId, revision, userPrompt, retryMessageId, null, null);
    }
}
