package sk.iway.iwcm.components.ai.tools;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** Transport-independent description of a tool and its registered REST operation. */
public record AiToolDefinition(String name, String description, Map<String, Object> inputSchema,
        String path, String httpMethod, Map<String, List<String>> conditions,
        String responseType, Map<String, Object> outputSchema) {
    private static final ObjectMapper VERSION_JSON = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    public record Preview(String name, String description, String httpMethod, String path, boolean write, String version) {}

    public Preview preview() {
        String excerpt = description == null ? "" : description.strip();
        if (excerpt.length() > 160) excerpt = excerpt.substring(0, 160) + "…";
        return new Preview(name, excerpt, httpMethod, path, writes(), version());
    }

    /** Fingerprints the complete contract independently of map iteration order for conversation reuse. */
    @JsonProperty("version")
    public String version() {
        try {
            return DigestUtils.sha256Hex(VERSION_JSON.writeValueAsBytes(Arrays.asList(name, description, httpMethod,
                    path, conditions, inputSchema, responseType, outputSchema)));
        } catch (JsonProcessingException invalidSchema) {
            throw new IllegalArgumentException("Invalid tool definition: " + name, invalidSchema);
        }
    }

    public AiToolDefinition(String name, String description, Map<String, Object> inputSchema,
            String path, String httpMethod, Map<String, List<String>> conditions) {
        this(name, description, inputSchema, path, httpMethod, conditions, "dynamic", null);
    }

    public boolean writes() {
        return !List.of("GET", "HEAD", "OPTIONS").contains(httpMethod);
    }

    public boolean rowReorder() {
        return "POST".equals(httpMethod) && path.endsWith("/row-reorder");
    }
}
