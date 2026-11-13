package sk.iway.iwcm.components.ai.tools;

import java.util.List;
import java.util.Map;

/** Transport-independent description of a tool and its registered REST operation. */
public record AiToolDefinition(String name, String description, Map<String, Object> inputSchema,
        String path, String httpMethod, Map<String, List<String>> conditions,
        String responseType, Map<String, Object> outputSchema) {
    public AiToolDefinition(String name, String description, Map<String, Object> inputSchema,
            String path, String httpMethod, Map<String, List<String>> conditions) {
        this(name, description, inputSchema, path, httpMethod, conditions, "dynamic", null);
    }
}
