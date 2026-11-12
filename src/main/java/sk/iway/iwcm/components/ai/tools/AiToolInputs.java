package sk.iway.iwcm.components.ai.tools;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.NameValueExpression;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

/** Uses the same binding metadata to describe tool arguments and to construct the target request. */
public final class AiToolInputs {
    public record Bound(Map<String, String> pathVariables, Map<String, List<String>> query, Map<String, String> headers, Object body) {
    }

    private enum Location { PATH, QUERY, BODY, FILTERS }
    private record Input(Location location, String wireName, boolean required, Map<String, Object> schema) {
    }

    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{\\*?([\\w-]+)(?=[:}])");
    private static final String SEARCH_KEY = "^search[A-Z][A-Za-z0-9_.]*$";
    private static final int DEFAULT_LIST_SIZE = 20;
    private static final Map<String, Object> STRING = Map.of("type", "string");
    private static final Map<String, Object> QUERY_VALUE = Map.of("oneOf", List.of(STRING, Map.of("type", "array", "items", STRING)));
    private final Map<String, Input> inputs = new LinkedHashMap<>();
    private final Map<String, String> fixedHeaders = new LinkedHashMap<>();
    private final Set<NameValueExpression<String>> conditions;
    private final Map<String, Object> schema;

    AiToolInputs(HandlerMethod handler, RequestMappingInfo mapping, String path) {
        conditions = mapping.getParamsCondition().getExpressions();
        AiToolSchema types = new AiToolSchema();
        Map<String, Object> filterFields = new LinkedHashMap<>();
        boolean queryMap = false;
        boolean datatableList = handler.getMethod().getName().equals("getAll") && path.endsWith("/all");
        boolean datatableSearch = handler.getMethod().getName().equals("findByColumns") && path.endsWith("/search/findByColumns");
        for (MethodParameter param : handler.getMethodParameters()) {
            Class<?> raw = param.getParameterType();
            Type type = GenericTypeResolver.resolveType(param.getGenericParameterType(), handler.getBeanType());
            PathVariable variable = param.getParameterAnnotation(PathVariable.class);
            RequestParam query = param.getParameterAnnotation(RequestParam.class);
            RequestBody body = param.getParameterAnnotation(RequestBody.class);
            String javaName = param.getParameter().getName();
            if (variable != null) {
                String name = named(variable.name(), variable.value(), javaName);
                if (path.contains("{" + name + "}") || path.contains("{" + name + ":") || path.contains("{*" + name + "}")) {
                    add(Location.PATH, name, true, types.describe(type), AiToolRegistry.words(name));
                }
            } else if (body != null) {
                add(Location.BODY, "body", body.required() && !param.isOptional(), types.describe(type), "JSON request body.");
            } else if (query != null && query.value().isEmpty() && query.name().isEmpty() && Map.class.isAssignableFrom(raw)) {
                queryMap = true;
            } else if (query != null) {
                String name = named(query.name(), query.value(), javaName);
                add(Location.QUERY, name, query.required() && ValueConstants.DEFAULT_NONE.equals(query.defaultValue()) && !param.isOptional(),
                        types.describe(type), AiToolRegistry.words(name));
            } else if (Pageable.class.isAssignableFrom(raw)) {
                add(Location.QUERY, "page", false, Map.of("type", "integer", "minimum", 0), "Zero-based page number.");
                Map<String, Object> size = new LinkedHashMap<>(Map.of("type", "integer", "minimum", 1));
                // The DataTable /all handler selects unpaged mode when the size parameter is absent.
                if (datatableList) size.put("default", DEFAULT_LIST_SIZE);
                add(Location.QUERY, "size", false, size, "Number of records per page.");
                add(Location.QUERY, "sort", false, Map.of("type", "array", "items", Map.of("type", "string")), "Sort criteria in property,direction format.");
            } else if (param.hasParameterAnnotation(ModelAttribute.class) || (param.getParameterAnnotations().length == 0 && !contextType(raw))) {
                if (BeanUtils.isSimpleProperty(raw)) add(Location.QUERY, javaName, false, types.describe(type), AiToolRegistry.words(javaName));
                else filterFields.putAll(AiToolSchema.filters(type, datatableSearch));
            }
        }
        if (datatableSearch || datatableList) {
            add(Location.QUERY, "export", false, Map.of("type", "boolean"), "Apply DataTable export processing.");
        }
        if (queryMap || !filterFields.isEmpty()) {
            Map<String, Object> filters = new LinkedHashMap<>();
            filters.put("type", "object");
            filters.put("properties", filterFields);
            if (datatableSearch) filters.put("patternProperties", Map.of(SEARCH_KEY, STRING));
            filters.put("additionalProperties", queryMap && !datatableSearch ? QUERY_VALUE : false);
            add(Location.FILTERS, "filters", false, filters, queryMap && !datatableSearch
                    ? "Additional query parameters, with repeated values supplied as an array of strings." : "Query fields used to filter the records.");
        }
        Matcher variables = PATH_VARIABLE.matcher(path);
        while (variables.find()) {
            String name = variables.group(1);
            add(Location.PATH, name, true, Map.of("type", "string"), AiToolRegistry.words(name));
        }
        for (NameValueExpression<String> condition : conditions) {
            String name = condition.getName();
            String key = keyFor(Location.QUERY, name);
            if ((condition.isNegated() && condition.getValue() == null) || (!condition.isNegated() && condition.getValue() != null)) {
                inputs.remove(key);
            } else if (!condition.isNegated()) {
                Input input = inputs.get(key);
                if (input == null) add(Location.QUERY, name, true, Map.of("type", "string"), AiToolRegistry.words(name));
                else inputs.put(key, new Input(input.location(), name, true, input.schema()));
            }
        }
        // Header selectors are literals supplied by the developer. Caller arguments can never supply headers.
        for (NameValueExpression<String> header : mapping.getHeadersCondition().getExpressions()) {
            if (!header.isNegated() && AiToolRequest.safeMappingHeader(header.getName())) {
                fixedHeaders.put(header.getName(), header.getValue() == null ? "true" : header.getValue());
            }
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        inputs.forEach((key, input) -> {
            properties.put(key, input.schema());
            if (input.required()) required.add(key);
        });
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("type", "object");
        object.put("properties", properties);
        if (!required.isEmpty()) object.put("required", required);
        object.put("additionalProperties", false);
        if (!types.definitions().isEmpty()) object.put("$defs", types.definitions());
        schema = Collections.unmodifiableMap(object);
    }

    private static boolean contextType(Class<?> raw) {
        return ServletRequest.class.isAssignableFrom(raw) || ServletResponse.class.isAssignableFrom(raw)
                || HttpSession.class.isAssignableFrom(raw) || Principal.class.isAssignableFrom(raw)
                || Errors.class.isAssignableFrom(raw) || raw == Locale.class || raw.getName().startsWith("org.springframework.")
                || raw.getName().startsWith("java.io.");
    }

    private static String named(String name, String value, String fallback) {
        if (!name.isEmpty()) return name;
        return value.isEmpty() ? fallback : value;
    }

    private String keyFor(Location location, String name) {
        String key = name;
        while (inputs.containsKey(key)) {
            Input existing = inputs.get(key);
            if (existing.location() == location && existing.wireName().equals(name)) break;
            key = location.name().toLowerCase(Locale.ROOT) + "_" + key;
        }
        return key;
    }

    private void add(Location location, String name, boolean required, Map<String, Object> type, String label) {
        Map<String, Object> property = new LinkedHashMap<>(type);
        property.put("description", label);
        inputs.putIfAbsent(keyFor(location, name), new Input(location, name, required, property));
    }

    public Map<String, Object> schema() {
        return schema;
    }

    public Bound bind(Map<String, Object> arguments) {
        if (!inputs.keySet().containsAll(arguments.keySet())) throw new IllegalArgumentException("Unknown tool argument.");
        Map<String, String> path = new LinkedHashMap<>();
        Map<String, List<String>> query = new LinkedHashMap<>();
        Object body = null;
        for (var entry : inputs.entrySet()) {
            Input input = entry.getValue();
            Object value = arguments.get(entry.getKey());
            if (value == null) {
                if (input.required()) throw new IllegalArgumentException("Missing required argument: " + entry.getKey());
                value = input.schema().get("default");
                if (value == null) continue;
            }
            String name = input.wireName();
            switch (input.location()) {
                case PATH -> path.put(name, scalar(value, input.schema()));
                case QUERY -> addQuery(query, name, values(value, input.schema()));
                case BODY -> body = value;
                case FILTERS -> {
                    if (!(value instanceof Map<?, ?> fields)) throw new IllegalArgumentException("Expected a filter object.");
                    bindFilters(query, fields, input.schema());
                }
            }
        }
        for (NameValueExpression<String> condition : conditions) {
            String name = condition.getName();
            if (condition.isNegated()) {
                if (query.containsKey(name) && (condition.getValue() == null || query.get(name).contains(condition.getValue()))) {
                    throw new IllegalArgumentException("Query conflicts with the selected mapping.");
                }
            } else if (condition.getValue() != null) {
                addQuery(query, name, List.of(condition.getValue()));
            }
        }
        return new Bound(path, query, Collections.unmodifiableMap(fixedHeaders), body);
    }

    private void bindFilters(Map<String, List<String>> query, Map<?, ?> fields, Map<String, Object> filterSchema) {
        Map<?, ?> known = (Map<?, ?>) filterSchema.get("properties");
        boolean freeForm = filterSchema.get("additionalProperties") instanceof Map;
        for (var field : fields.entrySet()) {
            if (!(field.getKey() instanceof String key) || key.isBlank() || key.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("Invalid query field name.");
            }
            if (inputs.values().stream().anyMatch(input -> input.location() == Location.QUERY && input.wireName().equals(key))) {
                throw new IllegalArgumentException("Use the declared argument for query field: " + key);
            }
            if (!known.containsKey(key) && !freeForm && !(filterSchema.containsKey("patternProperties") && key.matches(SEARCH_KEY))) {
                throw new IllegalArgumentException("Unknown filter field.");
            }
            List<String> values = freeForm && field.getValue() instanceof List<?> list
                    ? list.stream().map(item -> scalar(item, STRING)).toList() : List.of(scalar(field.getValue(), STRING));
            addQuery(query, key, values);
        }
    }

    private static void addQuery(Map<String, List<String>> query, String key, List<String> values) {
        if (query.putIfAbsent(key, values) != null) throw new IllegalArgumentException("Duplicate query field: " + key);
    }

    private static List<String> values(Object value, Map<String, Object> schema) {
        if ("array".equals(schema.get("type"))) {
            if (!(value instanceof List<?> list)) throw new IllegalArgumentException("Expected an array argument.");
            Map<?, ?> items = (Map<?, ?>) schema.get("items");
            return list.stream().map(item -> scalar(item, items)).toList();
        }
        return List.of(scalar(value, schema));
    }

    private static String scalar(Object value, Map<?, ?> schema) {
        Object type = schema.get("type");
        if (value instanceof String s && !"integer".equals(type) && !"number".equals(type) && !"boolean".equals(type)) return s;
        if (value instanceof Boolean b && "boolean".equals(type)) return b.toString();
        if (value instanceof Number n) {
            if ("integer".equals(type)) {
                try {
                    BigDecimal decimal = new BigDecimal(n.toString());
                    StreamReadConstraints.defaults().validateBigIntegerScale(decimal.scale());
                    return decimal.toBigIntegerExact().toString();
                } catch (ArithmeticException | StreamConstraintsException ex) {
                    throw new IllegalArgumentException("Expected an integer argument.", ex);
                }
            }
            if ("number".equals(type) || type instanceof List<?>) return n.toString();
        }
        throw new IllegalArgumentException("Argument does not match its declared type.");
    }
}
