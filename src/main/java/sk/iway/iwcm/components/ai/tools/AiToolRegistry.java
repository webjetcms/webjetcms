package sk.iway.iwcm.components.ai.tools;

import java.lang.reflect.Modifier;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/** Indexes opted-in MVC mappings. User authorization is evaluated by the catalog. */
@Component
public class AiToolRegistry {
    public record Entry(HandlerMethod handler, RequestMappingInfo mapping, RequestMethod method, String path,
            AiToolDefinition definition, AiToolInputs inputs) {
        Class<?> controllerType() {
            return ClassUtils.getUserClass(handler.getBeanType());
        }

        AiToolDefinition definition(Prop language) {
            if (language == null) return definition;
            return new AiToolDefinition(definition.name(), operationDescription(handler, language), definition.inputSchema(),
                    path, method.name(), definition.conditions(), definition.responseType(), definition.outputSchema());
        }
    }

    private record Operation(String baseName, HandlerMethod handler, RequestMappingInfo mapping,
            String path, RequestMethod verb) {
        String identity() {
            return handler.getBeanType().getName() + "|" + handler.getMethod().toGenericString() + "|" + path + "|" + verb
                    + "|" + conditions(mapping, false);
        }
    }

    private final RequestMappingHandlerMapping handlerMapping;
    private Map<String, Entry> index;

    public AiToolRegistry(@Lazy RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    public List<Entry> entries() {
        return List.copyOf(index().values());
    }

    public Entry lookup(String name) {
        return index().get(name);
    }

    // Handler mappings are complete when the first request arrives, including customer controllers.
    private synchronized Map<String, Entry> index() {
        if (index != null) return index;
        List<Operation> operations = new ArrayList<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> collect(operations, mapping, handler));
        Map<String, Integer> counts = new HashMap<>();
        for (Operation op : operations) counts.merge(op.baseName(), 1, Integer::sum);

        Map<String, Entry> result = new TreeMap<>();
        for (Operation op : operations) {
            String name = op.baseName();
            if (counts.get(name) > 1 || name.length() > 64) {
                String suffix = "_" + op.verb().name().toLowerCase(Locale.ROOT) + "_"
                        + DigestUtils.sha256Hex(op.identity()).substring(0, 12);
                name = name.substring(0, Math.min(name.length(), 64 - suffix.length())) + suffix;
            }
            AiToolInputs inputs = new AiToolInputs(op.handler(), op.mapping(), op.path());
            AiToolOutput output;
            try {
                output = AiToolOutput.from(op.handler(), op.mapping(), op.verb());
            } catch (IllegalArgumentException | IllegalStateException unsupportedModel) {
                Logger.warn(AiToolRegistry.class, "Cannot describe the response for AI tool {}", name, unsupportedModel);
                output = new AiToolOutput("dynamic", null);
            }
            AiToolDefinition definition = new AiToolDefinition(name, operationDescription(op.handler(), null), inputs.schema(), op.path(),
                    op.verb().name(), conditions(op.mapping(), true), output.responseType(), output.schema());
            if (result.put(name, new Entry(op.handler(), op.mapping(), op.verb(), op.path(), definition, inputs)) != null) {
                throw new IllegalStateException("Duplicate AI tool name: " + name);
            }
        }
        index = Collections.unmodifiableMap(result);
        return index;
    }

    private static void collect(List<Operation> operations, RequestMappingInfo mapping, HandlerMethod handler) {
        Class<?> type = ClassUtils.getUserClass(handler.getBeanType());
        AiTool annotation = type.getDeclaredAnnotation(AiTool.class);
        if (annotation == null || Modifier.isAbstract(type.getModifiers())) return;
        String methodName = handler.getMethod().getName();
        var included = Arrays.asList(annotation.include());
        if (included.isEmpty() && !DatatableRestControllerV2.class.isAssignableFrom(type)) return;
        if (!included.isEmpty() && !included.contains(methodName)) return;
        if (Arrays.asList(annotation.exclude()).contains(methodName)) return;
        String baseName = resource(type) + "_" + identifier(methodName);
        Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
        if (methods.isEmpty()) {
            boolean hasBody = Arrays.stream(handler.getMethodParameters()).anyMatch(p -> p.hasParameterAnnotation(RequestBody.class));
            methods = Set.of(!annotation.readOnly() && hasBody ? RequestMethod.POST : RequestMethod.GET);
        }
        for (RequestMethod verb : methods) {
            if (annotation.readOnly() && verb != RequestMethod.GET) continue;
            for (String path : mapping.getPatternValues()) {
                operations.add(new Operation(baseName, handler, mapping, path, verb));
            }
        }
    }

    private static Map<String, List<String>> conditions(RequestMappingInfo mapping, boolean publicDescriptor) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        add(result, "params", mapping.getParamsCondition().getExpressions());
        if (!mapping.getHeadersCondition().isEmpty()) {
            result.put("headers", mapping.getHeadersCondition().getExpressions().stream()
                    .map(header -> publicDescriptor && !AiToolRequest.safeMappingHeader(header.getName())
                            ? header.getName() + " (from authenticated request)" : header.toString()).sorted().toList());
        }
        add(result, "consumes", mapping.getConsumesCondition().getExpressions());
        add(result, "produces", mapping.getProducesCondition().getExpressions());
        if (!mapping.getVersionCondition().isEmpty()) result.put("version", List.of(mapping.getVersionCondition().toString()));
        if (mapping.getCustomCondition() != null) result.put("custom", List.of(mapping.getCustomCondition().toString()));
        return Collections.unmodifiableMap(result);
    }

    private static void add(Map<String, List<String>> result, String key, Collection<?> values) {
        if (!values.isEmpty()) result.put(key, values.stream().map(Object::toString).sorted().toList());
    }

    private static String resource(Class<?> type) {
        return identifier(type.getSimpleName().replaceFirst("(?:RestController|Controller)$", ""));
    }

    static String controllerDescription(Class<?> controllerType, Prop language) {
        String description = controllerType.getDeclaredAnnotation(AiTool.class).description().strip();
        return description.isEmpty() ? "" : translated(language, description, description);
    }

    private static String operationDescription(HandlerMethod handler, Prop language) {
        AiToolDescription annotation = handler.getMethodAnnotation(AiToolDescription.class);
        if (annotation != null && !annotation.value().isBlank()) {
            String description = annotation.value().strip();
            return translated(language, description, description);
        }

        String resource = resource(ClassUtils.getUserClass(handler.getBeanType()));
        String method = identifier(handler.getMethod().getName());
        String text = translated(language, "components.ai.tools." + resource + "_" + method + ".description", null);
        if (text != null) return text;
        String template = translated(language, "components.ai.tools.operation." + method, null);
        if (template != null) {
            String title = translated(language, "components." + resource + ".title", resource.replace('_', ' '));
            return template.replace("{1}", title);
        }
        String words = words(handler.getMethod().getName());
        return Character.toUpperCase(words.charAt(0)) + words.substring(1) + ".";
    }

    private static String translated(Prop language, String key, String fallback) {
        if (language != null) {
            String text = language.getTextProp(key);
            if (text != null) return text;
        }
        return translated(key, fallback);
    }

    static String identifier(String name) {
        String token = Normalizer.normalize(name, Normalizer.Form.NFKD).replaceAll("\\p{M}+", "")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2").replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^a-zA-Z0-9]+", "_").replaceAll("^_+|_+$", "").toLowerCase(Locale.ROOT);
        return token.isEmpty() ? "operation_" + DigestUtils.sha256Hex(name).substring(0, 12) : token;
    }

    static String words(String name) {
        return identifier(name).replace('_', ' ').replaceAll("\\bid\\b", "ID");
    }

    static String translated(String key, String fallback) {
        if (key.isBlank() || Constants.getServletContext() == null) return fallback;
        // Read the English bundle without the session language, domain prefixes or missing-key fallback.
        return Prop.getInstance("en").getProperties().getProperty(key, fallback);
    }
}
