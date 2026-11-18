package sk.iway.iwcm.components.ai.providers;

import java.util.Map;

import com.webjetcms.ai.TokenUsage;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.utils.Pair;

/** Shared audit formatting for assistant calls and saved chat turns. */
public final class AiAuditLog {
    private AiAuditLog() { }

    /** Creates the standard result heading and assistant metadata. */
    public static StringBuilder create(String status, AssistantDefinitionEntity assistant, String methodName) {
        StringBuilder audit = new StringBuilder(status).append(": ");
        audit.append(assistant.getName()).append(" (").append(assistant.getProvider()).append(") ");
        audit.append(methodName).append('\n');
        audit.append("\nAssistant ID: ").append(assistant.getId()).append('\n');
        if (Tools.isNotEmpty(assistant.getName())) audit.append("Assistant name: ").append(assistant.getName()).append('\n');
        if (Tools.isNotEmpty(assistant.getProvider())) audit.append("Provider: ").append(assistant.getProvider()).append('\n');
        if (Tools.isNotEmpty(assistant.getModel())) audit.append("Model: ").append(assistant.getModel()).append('\n');
        if (Tools.isNotEmpty(assistant.getFieldFrom())) audit.append("Field from: ").append(assistant.getFieldFrom()).append('\n');
        if (Tools.isNotEmpty(assistant.getFieldTo())) audit.append("Field to: ").append(assistant.getFieldTo());
        return audit;
    }

    /** Appends the standard counters and any additional token details reported by the provider. */
    public static void appendUsage(StringBuilder audit, TokenUsage usage) {
        TokenUsage normalizedUsage = usage == null ? TokenUsage.EMPTY : usage;
        audit.append("\nAction cost:\n");
        audit.append("\t input_tokens: ").append(normalizedUsage.inputTokens()).append('\n');
        audit.append("\t output_tokens: ").append(normalizedUsage.outputTokens()).append('\n');
        for (Map.Entry<String, Long> detail : normalizedUsage.details().entrySet()) {
            audit.append("\t ").append(detail.getKey()).append(": ").append(detail.getValue()).append('\n');
        }
        audit.append("\t total_tokens: ").append(normalizedUsage.totalTokens()).append('\n');
    }

    /** Appends input, user prompt and output within the configured content limit; zero omits content. */
    public static void appendResponse(StringBuilder audit, Pair<String, String> input, String textResponse, int maxLength) {
        if (maxLength <= 0) return;
        StringBuilder details = new StringBuilder("\nInput value:\n");
        details.append(Tools.isEmpty(input.getFirst()) ? " - " : input.getFirst());
        if (Tools.isNotEmpty(input.getSecond())) {
            details.append("\nUser prompt:\n").append(input.getSecond());
        }
        details.append("\nAI response:\n").append(textResponse);
        audit.append(DB.prepareString(details.toString(), maxLength));
    }
}
