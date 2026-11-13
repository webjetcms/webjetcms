package sk.iway.iwcm.components.ai.tools;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.util.Json;
import jakarta.servlet.ServletResponse;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/** Describes the successful HTTP response body. MVC handles error responses separately. */
record AiToolOutput(String responseType, Map<String, Object> schema) {
    private static final List<Class<?>> BODY_WRAPPERS = List.of(HttpEntity.class, Callable.class,
            CompletionStage.class, DeferredResult.class, WebAsyncTask.class);

    static AiToolOutput from(HandlerMethod handler, RequestMappingInfo mapping, RequestMethod verb) {
        ResponseStatus status = handler.getMethodAnnotation(ResponseStatus.class);
        if (verb == RequestMethod.HEAD || (status != null && Set.of(204, 205, 304).contains(status.code().value()))) {
            return new AiToolOutput("empty", null);
        }
        Type resolved = GenericTypeResolver.resolveType(handler.getMethod().getGenericReturnType(), handler.getBeanType());
        JavaType type = bodyType(Json.mapper().constructType(resolved));
        if (type.hasRawClass(HttpHeaders.class)) return new AiToolOutput("empty", null);
        if (type.hasRawClass(void.class) || type.hasRawClass(Void.class)) {
            boolean writesResponse = Arrays.stream(handler.getMethod().getParameterTypes()).anyMatch(p ->
                    ServletResponse.class.isAssignableFrom(p) || OutputStream.class.isAssignableFrom(p) || Writer.class.isAssignableFrom(p));
            return new AiToolOutput(writesResponse ? "dynamic" : "empty", null);
        }
        if (type.hasRawClass(String.class)) return new AiToolOutput("text", null);
        if (type.hasRawClass(byte[].class) || type.isTypeOrSubTypeOf(Resource.class)) return new AiToolOutput("binary", null);
        if (type.isTypeOrSubTypeOf(StreamingResponseBody.class) || type.isTypeOrSubTypeOf(ResponseBodyEmitter.class)
                || type.isTypeOrSubTypeOf(ModelAndView.class) || type.isTypeOrSubTypeOf(View.class)
                || type.getRawClass().getPackageName().startsWith("jakarta.servlet")) return new AiToolOutput("dynamic", null);

        Set<MediaType> produces = mapping.getProducesCondition().getProducibleMediaTypes();
        boolean declaresJson = !produces.isEmpty() && produces.stream().allMatch(media ->
                media.getSubtype().equals("json") || media.getSubtype().endsWith("+json"));
        if (!produces.isEmpty() && !declaresJson) return new AiToolOutput("dynamic", null);
        if (type.hasRawClass(Object.class)) return declaresJson ? new AiToolOutput("json", Map.of()) : new AiToolOutput("dynamic", null);
        if (type.isTypeOrSubTypeOf(com.fasterxml.jackson.databind.JsonNode.class)) return new AiToolOutput("dynamic", null);

        if (handler.getMethod().getDeclaringClass() == DatatableRestControllerV2.class) {
            if (handler.getMethod().getName().equals("getAll")) {
                // getAll always wraps the page with options, notifications, summary and redirect.
                type = Json.mapper().getTypeFactory().constructSpecializedType(type, DatatablePageImpl.class);
            } else if (handler.getMethod().getName().equals("delete")) {
                return new AiToolOutput("json", Map.of("type", "object", "properties", Map.of("result", Map.of("type", "boolean")),
                        "required", List.of("result")));
            }
        }
        return new AiToolOutput("json", AiToolSchema.output(type));
    }

    private static JavaType bodyType(JavaType type) {
        for (Class<?> wrapper : BODY_WRAPPERS) {
            if (type.isTypeOrSubTypeOf(wrapper)) {
                JavaType nested = type.findSuperType(wrapper).containedTypeOrUnknown(0);
                return nested.equals(type) ? Json.mapper().constructType(Object.class) : bodyType(nested);
            }
        }
        return type;
    }
}
