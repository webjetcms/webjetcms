package sk.iway.iwcm.components.ai.dto;

import java.util.List;
import sk.iway.iwcm.components.ai.tools.AiToolProtocol.Result;

/** Only user-controlled chat input; provider IDs, ownership and history are resolved on the server. */
public record AiChatRequest(String threadId, Long assistantId, String requestId, Long revision,
    String userPrompt, String retryMessageId, AiChatContext context, String inputValue, boolean inputIsSelectedTextInEditor,
    List<Result> toolResults) {
    public AiChatRequest {
        toolResults = toolResults == null ? List.of() : List.copyOf(toolResults);
    }

    public AiChatRequest(String threadId, Long assistantId, String requestId, Long revision,
        String userPrompt, String retryMessageId, AiChatContext context, String inputValue, boolean inputIsSelectedTextInEditor) {
        this(threadId, assistantId, requestId, revision, userPrompt, retryMessageId, context, inputValue, inputIsSelectedTextInEditor, List.of());
    }
    public AiChatRequest(String threadId, Long assistantId, String requestId, Long revision,
        String userPrompt, String retryMessageId, AiChatContext context, String inputValue) {
        this(threadId, assistantId, requestId, revision, userPrompt, retryMessageId, context, inputValue, false);
    }

    public AiChatRequest(String threadId, Long assistantId, String requestId, Long revision,
        String userPrompt, String retryMessageId) {
        this(threadId, assistantId, requestId, revision, userPrompt, retryMessageId, null, null);
    }
}
