package sk.iway.iwcm.components.ai.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.core.util.Json;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.webjetcms.ai.AiToolArgumentValidator;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.datatable.json.DataTableColumnEditor;

/** Verifies that the tool detail exposes nested requirements without changing the AI contract. */
class AiToolEntityTest {
    /** Required paths cover repeated types and arrays, stop at recursion, and exclude optional branches. */
    @Test
    void followsRequiredObjectsAndArrayItemsWithoutIncludingOptionalBranches() throws Exception {
        var detail = detail(inputs("nested", NestedBody.class));

        assertEquals(Set.of("body", "body.first", "body.first.name", "body.second", "body.second.name",
                "body.rows", "body.rows[].name", "body.next"), Set.copyOf(Arrays.asList(detail.getRequired().split(", "))));
        assertNull(detail(inputs("optional", NestedBody.class)).getRequired());
        assertEquals("body, body.action, body.data, body.data.*.title, body.data.*.navbar",
                detail(inputs("editor", DatatableRequest.class)).getRequired());
    }

    /** The schema matches the frontend's field metadata even when Jackson exposes a transient field through accessors. */
    @Test
    void requiredFieldsMatchDataTableEditorMetadata() throws Exception {
        var schema = inputs("editorFields", EditorBody.class);
        JsonNode root = Json.mapper().valueToTree(schema);
        JsonNode body = root.at(root.at("/properties/body/$ref").asText().substring(1));
        JsonNode nested = root.at(body.at("/properties/editorFields/$ref").asText().substring(1));
        for (var field : RequiredEditorFields.class.getDeclaredFields()) {
            String name = field.getName().equals("title") ? "heading" : field.getName();
            boolean expected = new DataTableColumnEditor(field).isRequired();
            assertEquals(expected, Json.mapper().convertValue(body.path("required"), List.class).contains(name), name);
            assertEquals(expected, Json.mapper().convertValue(nested.path("required"), List.class).contains(name), "editorFields." + name);
        }
        assertTrue(detail(schema).getRequired().contains("body.editorFields.heading"));
        for (String reference : root.findValuesAsText("$ref")) {
            assertTrue(reference.startsWith("#/"));
            assertFalse(root.at(reference.substring(1)).isMissingNode(), reference);
        }
        var validFields = Map.<String, Object>of("heading", "Page", "values", List.of("a"), "count", 1);
        var arguments = new LinkedHashMap<>(validFields);
        arguments.put("editorFields", validFields);
        AiToolArgumentValidator.validate(schema, Map.of("body", arguments));
        arguments.remove("heading");
        var missing = assertThrows(AiToolArgumentValidator.InvalidArguments.class,
                () -> AiToolArgumentValidator.validate(schema, Map.of("body", arguments)));
        assertEquals("arguments.body.heading is required.", missing.getMessage());
    }

    private static Map<String, Object> inputs(String method, Class<?> parameter) throws Exception {
        var handler = new HandlerMethod(new TestController(), TestController.class.getMethod(method, parameter));
        return new AiToolInputs(handler, RequestMappingInfo.paths("/test").build(), "/test").schema();
    }

    private static AiToolEntity detail(Map<String, Object> schema) {
        return AiToolEntity.detail(new AiToolDefinition("test_add", "Test", schema, "/test", "POST", Map.of(),
                "json", Map.of("type", "object")), null);
    }

    static class TestController {
        public void nested(@RequestBody NestedBody body) { }
        public void editor(@RequestBody DatatableRequest<Long, DocDetails> body) { }
        public void optional(@RequestBody(required = false) NestedBody body) { }
        public void editorFields(@RequestBody EditorBody body) { }
    }

    static class NamedBody {
        @NotBlank public String name;
    }

    static class NestedBody {
        @NotNull public NamedBody first;
        @NotNull public NamedBody second;
        public NamedBody optional;
        @NotNull public List<NamedBody> rows;
        @NotNull public NestedBody next;
    }

    static class RequiredEditorFields {
        @DataTableColumn @NotBlank private transient String title;
        @DataTableColumn @NotEmpty public List<String> values;
        @DataTableColumn @NotNull public Integer count;
        @DataTableColumn public String optional;

        @JsonProperty("heading")
        public String getTitle() { return title; }
        @JsonProperty("heading")
        public void setTitle(String title) { this.title = title; }
    }

    static class EditorBody extends RequiredEditorFields {
        @NotNull @DataTableColumnNested private transient RequiredEditorFields editorFields;
        public RequiredEditorFields getEditorFields() { return editorFields; }
        public void setEditorFields(RequiredEditorFields editorFields) { this.editorFields = editorFields; }
    }
}
