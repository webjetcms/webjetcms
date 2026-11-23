package sk.iway.iwcm.components.ai.tools;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.configuration.ConfigurationController;
import sk.iway.iwcm.components.configuration.model.ConfDetailsDto;
import sk.iway.iwcm.helpers.DataSanitizer;
import sk.iway.iwcm.system.datatable.DatatableResponse;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.node.ObjectNode;

/** Projects configuration tool results after authorization and before MVC writes the response. */
@ControllerAdvice(assignableTypes = ConfigurationController.class)
public class ConfigurationToolResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servlet)
                || servlet.getServletRequest().getAttribute(AiToolService.TARGET) == null) return body;
        if (!(body instanceof ConfDetailsDto || body instanceof Page<?> || body instanceof DatatableResponse<?>)) return body;
        if (!JacksonJsonHttpMessageConverter.class.isAssignableFrom(converterType)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);
        }
        return new ToolResult(body);
    }

    @JsonSerialize(using = ToolResultSerializer.class)
    private record ToolResult(Object body) {}

    private static class ToolResultSerializer extends ValueSerializer<ToolResult> {
        @Override
        public void serialize(ToolResult result, JsonGenerator generator, SerializationContext context) {
            // Use the active MVC mapper so dates, paging and editor metadata retain their wire format.
            ObjectNode body = context.valueToTree(result.body());
            if (result.body() instanceof ConfDetailsDto) {
                redactRow(body);
            } else {
                String rows = result.body() instanceof Page<?> ? "content" : "data";
                for (JsonNode row : body.path(rows)) {
                    if (row instanceof ObjectNode object) redactRow(object);
                }
            }
            generator.writeTree(body);
        }
    }

    private static void redactRow(ObjectNode row) {
        String name = row.path("name").asString("");
        boolean sensitive = DataSanitizer.isSensitive(name) || encrypted(row, "value")
                || encrypted(row, "displayValue") || encrypted(row, "oldValue");
        if (sensitive) {
            row.remove("value");
            row.remove("displayValue");
            row.remove("oldValue");
            row.put("redacted", true);
            row.put("configured", StringUtils.isNotEmpty(Constants.getString(name)));
        }
        redactChanges(row.path("confPrepared"), sensitive);
        redactChanges(row.path("confHistory"), sensitive);
    }

    private static void redactChanges(JsonNode changes, boolean sensitiveParent) {
        for (JsonNode item : changes) {
            if (item instanceof ObjectNode change && (sensitiveParent
                    || DataSanitizer.isSensitive(change.path("name").asString("")) || encrypted(change, "value"))) {
                change.remove("value");
                change.put("redacted", true);
            }
        }
    }

    private static boolean encrypted(ObjectNode row, String field) {
        return row.path(field).asString("").startsWith("encrypted:");
    }
}
