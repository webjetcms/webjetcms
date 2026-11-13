package sk.iway.iwcm.components.ai.tools;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.swagger.v3.core.util.Json;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/** Describes concrete JSON types in the direction in which MVC uses them. */
final class AiToolSchema {
    private static final ObjectMapper MAPPER = Json.mapper();
    private final Map<String, Map<String, Object>> definitions = new LinkedHashMap<>();
    private final boolean output;

    AiToolSchema() {
        this(false);
    }

    private AiToolSchema(boolean output) {
        this.output = output;
    }

    Map<String, Map<String, Object>> definitions() {
        return definitions;
    }

    Map<String, Object> describe(Type type) {
        return describe(MAPPER.constructType(type), false);
    }

    static Map<String, Object> output(JavaType type) {
        AiToolSchema builder = new AiToolSchema(true);
        Map<String, Object> schema = new LinkedHashMap<>(builder.describe(type, true));
        if (!builder.definitions.isEmpty()) schema.put("$defs", builder.definitions);
        return schema;
    }

    private Map<String, Object> describe(JavaType type, boolean root) {
        Class<?> raw = ClassUtils.resolvePrimitiveIfNecessary(type.getRawClass());
        if (raw == Object.class) return Map.of();
        if (output && type.isTypeOrSubTypeOf(tools.jackson.databind.JsonNode.class)) {
            if (type.isTypeOrSubTypeOf(tools.jackson.databind.node.ObjectNode.class)) return Map.of("type", "object");
            if (type.isTypeOrSubTypeOf(tools.jackson.databind.node.ArrayNode.class)) return Map.of("type", "array");
            return Map.of();
        }
        if (raw == Optional.class) return memberSchema(type.containedTypeOrUnknown(0));
        if (output && (raw == byte[].class || raw == char[].class)) return Map.of("type", "string");
        if (type.isMapLikeType()) return Map.of("type", "object", "additionalProperties", memberSchema(type.getContentType()));
        if (type.isCollectionLikeType() || type.isArrayType()) return Map.of("type", "array", "items", memberSchema(type.getContentType()));
        if (raw.isEnum()) {
            if (!output) return Map.of("type", "string", "enum", Arrays.stream(raw.getEnumConstants()).map(e -> ((Enum<?>) e).name()).toList());
            AnnotatedMember value = MAPPER.getSerializationConfig().introspect(type).findJsonValueAccessor();
            // In Jackson 3 the default string can come from a locale-sensitive toString().
            return value == null ? Map.of("type", "string") : describe(value.getType(), root);
        }
        if (raw == Boolean.class) return Map.of("type", "boolean");
        if (raw == Byte.class || raw == Short.class || raw == Integer.class || raw == Long.class || raw == BigInteger.class) return Map.of("type", "integer");
        if (Number.class.isAssignableFrom(raw)) return Map.of("type", "number");
        if (Date.class.isAssignableFrom(raw)) return output ? Map.of("type", "integer") : Map.of("type", List.of("integer", "string"));
        if (output && TemporalAccessor.class.isAssignableFrom(raw)) {
            // Java time values can use timestamps, date component arrays or ISO strings, depending on their serializer.
            return Map.of("type", List.of("integer", "array", "string"));
        }
        if (BeanUtils.isSimpleValueType(raw) || TemporalAccessor.class.isAssignableFrom(raw)) return Map.of("type", "string");

        String key = AiToolRegistry.identifier(raw.getSimpleName()) + "_" + DigestUtils.sha256Hex(type.toCanonical()).substring(0, 16);
        if (!root && definitions.containsKey(key)) return Map.of("$ref", "#/$defs/" + key);
        BeanDescription description = output ? MAPPER.getSerializationConfig().introspect(type) : MAPPER.getDeserializationConfig().introspect(type);
        if (output && description.findJsonValueAccessor() != null) {
            JavaType value = description.findJsonValueAccessor().getType();
            return value.equals(type) ? Map.of() : describe(value, root);
        }
        Map<String, Object> bean = new LinkedHashMap<>();
        // Reserve the definition before traversing a recursive model.
        if (!root) definitions.put(key, bean);
        Map<String, Object> fields = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        JsonIgnoreProperties.Value ignored = output ? MAPPER.getSerializationConfig().getDefaultPropertyIgnorals(raw, description.getClassInfo()) : null;
        Set<String> omitted = ignored == null ? Set.of() : ignored.findIgnoredForSerialization();
        for (BeanPropertyDefinition prop : description.findProperties()) {
            if (output ? !prop.couldSerialize() : !prop.couldDeserialize()) continue;
            AnnotatedMember member = prop.getPrimaryMember();
            if (output && (omitted.contains(prop.getName()) || member.hasAnnotation(JsonBackReference.class))) continue;
            Map<String, Object> field = new LinkedHashMap<>(memberSchema(prop.getPrimaryType(), output ? member.getAnnotation(JsonFormat.class) : null));
            field.put("description", label(prop));
            if (!output && raw == DatatableRequest.class && "action".equals(prop.getName())) {
                field.put("enum", List.of("create", "edit", "remove"));
            }
            fields.put(prop.getName(), field);
            if (!output && (prop.isRequired() || member.hasAnnotation(NotBlank.class) || member.hasAnnotation(NotNull.class)
                    || member.hasAnnotation(NotEmpty.class))) required.add(prop.getName());
        }
        bean.put("type", "object");
        bean.put("properties", fields);
        if (!output && raw == DatatableRequest.class) required = List.of("action", "data");
        if (!required.isEmpty()) bean.put("required", required);
        return root ? bean : Map.of("$ref", "#/$defs/" + key);
    }

    private Map<String, Object> memberSchema(JavaType type) {
        return memberSchema(type, null);
    }

    private Map<String, Object> memberSchema(JavaType type, JsonFormat format) {
        Map<String, Object> schema = describe(type, false);
        if (!output) return schema;
        if (format != null) {
            if (format.shape() == JsonFormat.Shape.STRING) schema = Map.of("type", "string");
            else if (format.shape().isNumeric()) schema = Map.of("type", "number");
        }
        if (type.isPrimitive() || schema.isEmpty()) return schema;
        Object jsonType = schema.get("type");
        if (jsonType == null) return Map.of("anyOf", List.of(schema, Map.of("type", "null")));
        List<Object> nullable = new ArrayList<>();
        if (jsonType instanceof List<?> types) nullable.addAll(types);
        else nullable.add(jsonType);
        if (!nullable.contains("null")) nullable.add("null");
        Map<String, Object> result = new LinkedHashMap<>(schema);
        result.put("type", nullable);
        return result;
    }

    /** Spring binds model attributes by property name. DataTable search also accepts prefixed column filters. */
    static Map<String, Object> filters(Type model, boolean search) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (BeanPropertyDefinition prop : properties(MAPPER.constructType(model))) {
            if (!prop.couldDeserialize() || !BeanUtils.isSimpleValueType(prop.getRawPrimaryType())) continue;
            String name = prop.getName();
            Map<String, Object> filter = Map.of("type", "string", "description", label(prop));
            fields.put(name, filter);
            if (search) fields.put("search" + Character.toUpperCase(name.charAt(0)) + name.substring(1), filter);
        }
        return fields;
    }

    private static List<BeanPropertyDefinition> properties(JavaType type) {
        return MAPPER.getDeserializationConfig().introspect(type).findProperties();
    }

    private static String label(BeanPropertyDefinition prop) {
        DataTableColumn column = prop.getPrimaryMember().getAnnotation(DataTableColumn.class);
        String fallback = AiToolRegistry.words(prop.getName());
        if (column == null) return fallback;
        String key = column.title();
        if (key.isBlank()) {
            key = "components." + AiToolRegistry.identifier(prop.getPrimaryMember().getDeclaringClass().getSimpleName())
                    + "." + AiToolRegistry.identifier(prop.getInternalName());
        }
        return AiToolRegistry.translated(key, fallback);
    }
}
