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
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;

/** Checks deferred contracts, content versions and server validation independently of provider behavior. */
class AiToolProtocolTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Map<String, Object> schema = Map.of("type", "object", "properties", Map.of("body", Map.of("$ref", "#/$defs/Record")),
            "required", List.of("body"), "$defs", Map.of("Record", Map.of("type", "object", "properties",
                Map.of("title", Map.of("type", "string")), "required", List.of("title"))));
    private final AiToolDefinition save = new AiToolDefinition("save", "Save a record", schema, "/records", "POST", Map.of());

    @Test
    void requestsFolderSelectionOnlyForAStandaloneDecision() {
        var prop = mock(Prop.class);
        org.mockito.Mockito.when(prop.getText("components.ai_assistants.chat.chooseFolder.js")).thenReturn("Select a folder");
        var response = new AssistantResponseDTO();
        response.setResponse("Example: [[WEBJET_SELECT_PAGE_FOLDER]]");
        AiToolProtocol.resolveFolderRequest(response, prop);
        assertFalse(response.isFolderRequested());
        response.setResponse(" [[WEBJET_SELECT_PAGE_FOLDER]]\n");
        AiToolProtocol.resolveFolderRequest(response, prop);
        assertTrue(response.isFolderRequested());
        assertEquals("Select a folder", response.getResponse());
    }

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

    @Test
    void offersPermittedDirectToolsAndDefersIndirectAssignments() {
        var loader = new AiToolDefinition(AiAssistantRuntime.LOAD_TOOLS, "Load", Map.of(), "/details", "GET", Map.of());
        var find = new AiToolDefinition("find", "Find", Map.of(), "/find", "GET", Map.of());
        var runtime = new AiAssistantRuntime(new AssistantDefinitionEntity(), "", List.of(save, find, loader), true);
        var cases = Map.of("@tool(save) @tool(find) @tool(save)", List.of("save", "find"),
                "@group(records) @skill(records)", List.<String>of(), "@tool(save) @tool(hidden) @group(records)", List.of("save"));
        cases.forEach((instructions, expected) -> {
            var protocol = AiToolProtocol.prepare(runtime, List.of(), instructions);
            var offered = protocol.nativeTools();
            var names = offered.stream().map(com.webjetcms.ai.AiTool::name).toList();
            assertTrue(names.contains(loader.name()));
            assertEquals(expected, names.stream().filter(name -> !loader.name().equals(name)).toList(), instructions);
            assertEquals(!expected.contains("save"), protocol.nativeInstructions(offered).contains("save: Save a record"));
        });
    }

    /** Hiding a schema from the prompt does not hide it from argument validation or allow unassigned calls. */
    @Test
    void validatesDeferredArgumentsAndAllowsOrdinaryAnswers() {
        var protocol = new AiToolProtocol(List.of(save), List.of());
        assertFalse(protocol.instructions().contains("\"title\"") || protocol.instructions().contains("\"$defs\""));
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
        String call = "{\"type\":\"call\",\"name\":\"%s\",\"arguments\":{\"body\":{\"title\":\"%s\"}}}";
        for (String invalid : List.of(call.formatted("unassigned", "Page"), call.formatted("save", "Page") + " {}",
                call.formatted("save", "€".repeat(AiToolProtocol.MAX_ARGUMENT_BYTES / 3)))) {
            answer = new AssistantResponseDTO();
            protocol.apply(invalid, answer, null);
            assertEquals(AiToolProtocol.INVALID_TOOL_STEP, answer.getErrorCategory());
        }
    }

}
