package sk.iway.iwcm.components.ai.tools;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.swagger.v3.core.util.Json;

/** Builds example values from the JSON Schema emitted by the tool registry. */
final class AiToolExample {
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private final JsonNode root;
    private final boolean response;
    private final Set<String> references = new HashSet<>();

    private AiToolExample(Map<String, Object> schema, boolean response) {
        this.root = Json.mapper().valueToTree(schema);
        this.response = response;
    }

    static String forInput(Map<String, Object> schema) {
        return new AiToolExample(schema, false).render();
    }

    static String forResponse(Map<String, Object> schema) {
        return new AiToolExample(schema, true).render();
    }

    private String render() {
        JsonNode example = sample(root);
        return example == null ? null : Json.pretty(example);
    }

    private JsonNode sample(JsonNode schema) {
        if (schema.has("$ref")) {
            String reference = schema.get("$ref").asText();
            if (!reference.startsWith("#/") || !references.add(reference)) return null;
            JsonNode value = sample(root.at(reference.substring(1)));
            references.remove(reference);
            return value;
        }
        if (schema.has("default")) return schema.get("default");
        if (schema.path("enum").has(0)) return schema.get("enum").get(0);
        JsonNode variants = schema.has("anyOf") ? schema.get("anyOf") : schema.path("oneOf");
        if (variants.isArray()) {
            JsonNode fallback = null;
            for (JsonNode variant : variants) {
                JsonNode candidate = sample(variant);
                if (candidate == null) continue;
                if (!candidate.isNull()) return candidate;
                fallback = candidate;
            }
            return fallback;
        }
        JsonNode type = schema.path("type");
        if (type.isArray()) {
            type = type.valueStream().filter(candidate -> !"null".equals(candidate.asText())).findFirst().orElse(type.path(0));
        }
        return switch (type.asText()) {
            case "object" -> {
                var object = NODES.objectNode();
                var properties = schema.path("properties");
                var required = schema.path("required");
                var names = properties.fieldNames();
                if (!response && !required.isEmpty()) names = required.valueStream().map(JsonNode::asText).iterator();
                boolean defaultsOnly = !response && schema == root && required.isEmpty();
                while (names.hasNext()) {
                    String name = names.next();
                    JsonNode property = properties.path(name);
                    if (defaultsOnly && !property.has("default")) continue;
                    JsonNode value = sample(property);
                    if (value != null) object.set(name, value);
                }
                if (properties.isEmpty() && schema.path("additionalProperties").isObject()) {
                    JsonNode value = sample(schema.get("additionalProperties"));
                    if (value != null) object.set("1", value);
                }
                yield object;
            }
            case "array" -> {
                var array = NODES.arrayNode();
                var item = sample(schema.path("items"));
                if (item != null) array.add(item);
                yield array;
            }
            case "integer", "number" -> schema.has("minimum") ? schema.get("minimum") : NODES.numberNode(response ? 0 : 1);
            case "boolean" -> NODES.booleanNode(true);
            case "string" -> NODES.textNode("string");
            case "null" -> NODES.nullNode();
            default -> null;
        };
    }
}
