package sk.iway.iwcm.components.ai.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.i18n.Prop;

/** Checks deferred contracts, content versions and server validation independently of provider behavior. */
class AiToolProtocolTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Map<String, Object> schema = Map.of("type", "object", "properties", Map.of("body", Map.of("$ref", "#/$defs/Record")),
            "required", List.of("body"), "$defs", Map.of("Record", Map.of("type", "object", "properties",
                Map.of("title", Map.of("type", "string")), "required", List.of("title"))));
    private final AiToolDefinition save = new AiToolDefinition("save", "Save a record", schema, "/records", "POST", Map.of());

    /** The version identifies the complete contract, including changes omitted from the short preview. */
    @Test
    void versionsIgnoreMapOrderButChangeWithTheContract() throws Exception {
        var reordered = new LinkedHashMap<String, Object>();
        schema.keySet().stream().sorted(java.util.Comparator.reverseOrder()).forEach(key -> reordered.put(key, schema.get(key)));
        var same = new AiToolDefinition(save.name(), save.description(), reordered, save.path(), save.httpMethod(), save.conditions());
        assertEquals(save.version(), same.version());
        assertEquals(save.version(), save.preview().version());
        assertEquals(save.version(), JSON.readTree(JSON.writeValueAsString(save)).path("version").asText());
        assertEquals(save.version(), tools.jackson.databind.json.JsonMapper.builder().build().valueToTree(save).path("version").asText());

        var changed = new LinkedHashMap<>(schema);
        changed.put("required", List.of());
        assertNotEquals(save.version(), new AiToolDefinition(save.name(), save.description(), changed, save.path(), save.httpMethod(), save.conditions()).version());
        assertNotEquals(save.version(), new AiToolDefinition(save.name(), save.description(), schema, "/records/new", save.httpMethod(), save.conditions()).version());
        assertNotEquals(save.version(), new AiToolDefinition(save.name(), save.description(), schema, save.path(), "PUT", save.conditions()).version());
        assertNotEquals(save.version(), new AiToolDefinition(save.name(), save.description(), schema, save.path(), save.httpMethod(),
                save.conditions(), "json", Map.of("type", "object")).version());
        String description = "Guidance. ".repeat(30);
        var first = new AiToolDefinition("save", description + "First", schema, "/records", "POST", Map.of());
        var second = new AiToolDefinition("save", description + "Second", schema, "/records", "POST", Map.of());
        assertEquals(first.preview().description(), second.preview().description());
        assertTrue(first.preview().description().length() <= 161);
        assertNotEquals(first.version(), second.version());
    }

    /** Hiding a schema from the prompt does not hide it from argument validation or allow unassigned calls. */
    @Test
    void validatesDeferredArgumentsAndAllowsOrdinaryAnswers() {
        var protocol = new AiToolProtocol(List.of(save), List.of());
        assertFalse(protocol.instructions().contains("\"title\"") || protocol.instructions().contains("\"$defs\""));
        assertTrue(protocol.instructions().contains("name and version match"));
        assertTrue(protocol.instructions().contains("Reload it if the version changes"));
        var answer = new AssistantResponseDTO();
        protocol.apply("Hello!", answer, mock(Prop.class));
        assertEquals("Hello!", answer.getResponse());
        assertNull(answer.getToolCall());
        protocol.apply("{\"type\":\"call\",\"name\":\"save\",\"arguments\":{\"body\":{}}}", answer, mock(Prop.class));
        assertNull(answer.getToolCall());
        assertTrue(answer.getToolCorrection().contains("arguments.body.title is required"));

        answer = new AssistantResponseDTO();
        protocol.apply("{\"type\":\"call\",\"name\":\"save\",\"arguments\":{\"body\":{\"title\":\"Page\"}}}", answer, mock(Prop.class));
        assertEquals("save", answer.getToolCall().name());
        assertTrue(answer.getToolCall().write());
        answer = new AssistantResponseDTO();
        protocol.apply("{\"type\":\"call\",\"name\":\"unassigned\",\"arguments\":{}}", answer, mock(Prop.class));
        assertNull(answer.getToolCall());
        assertEquals(AiToolProtocol.INVALID_TOOL_STEP, answer.getErrorCategory());
    }

}
