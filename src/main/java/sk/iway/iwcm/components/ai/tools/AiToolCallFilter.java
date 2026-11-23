package sk.iway.iwcm.components.ai.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ServletRequestPathUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import com.webjetcms.ai.AiToolArgumentValidator;
import sk.iway.iwcm.PathFilter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Translates a JSON tool call before WebJET captures target-specific request and domain information. */
@Component
@RequiredArgsConstructor
public class AiToolCallFilter extends HttpFilter {

    static final String TOOL_RESPONSE_HEADER = "X-WebJET-AI-Tool-Response";
    private static final Pattern ROUTE = Pattern.compile("/admin/rest/ai/tools/([A-Za-z0-9_-]{1,64})/call");
    private static final String FAILURE = AiToolCallFilter.class.getName() + ".failure";
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();
    private static final TypeReference<Map<String, Object>> ARGUMENTS = new TypeReference<>() {};
    private final AiToolRegistry registry;
    private final AiToolAuthorization authorization;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        var route = ROUTE.matcher(request.getRequestURI().substring(request.getContextPath().length()));
        if (!"POST".equals(request.getMethod()) || !route.matches()) {
            chain.doFilter(request, response);
            return;
        }
        Map<String, Object> arguments = null;
        try {
            if (request.getContentType() != null && MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(request.getContentType()))) {
                if (request.getContentLengthLong() > AiToolProtocol.MAX_ARGUMENT_BYTES) {
                    response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                    return;
                }
                byte[] bytes = request.getInputStream().readNBytes(AiToolProtocol.MAX_ARGUMENT_BYTES + 1);
                if (bytes.length > AiToolProtocol.MAX_ARGUMENT_BYTES) {
                    response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                    return;
                }
                arguments = JSON.readValue(bytes, ARGUMENTS);
            }
        } catch (JacksonException | IllegalArgumentException malformedInput) {
            // The authenticated fallback reports invalid arguments only for a visible tool.
        }
        dispatch(route.group(1), arguments, request, response, chain);
    }

    /**
     * Binds arguments to one registered mapping and continues through the server's native REST filter chain.
     * Unresolved calls continue on the original route so its admin filters and error handler still run.
     */
    private void dispatch(String name, Map<String, Object> arguments, HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (!AiToolServletRoutes.usesDispatcher(request)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        AiToolRegistry.Entry entry = registry.lookup(name);
        if (entry == null) {
            chain.doFilter(request, response);
            return;
        }
        AiToolRequest target;
        try {
            if (arguments == null) throw new IllegalArgumentException("Expected an object");
            if (entry.inputs().requiresBodyValidation() || request.getHeader(AiToolAuthorization.ASSISTANT_HEADER) != null) {
                entry = registry.validateCall(entry, arguments);
            }
            target = new AiToolRequest(request, entry, entry.inputs().bind(arguments));
        } catch (IllegalArgumentException invalidArguments) {
            String correction = invalidArguments instanceof AiToolArgumentValidator.InvalidArguments ? invalidArguments.getMessage() : null;
            request.setAttribute(FAILURE, new Failure(entry, authorization, correction));
            chain.doFilter(request, response);
            return;
        }
        if (!AiToolServletRoutes.permits(request, target)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        RequestPath originalPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
        request.setAttribute(AiToolService.TARGET, entry);
        if (entry.definition().writes()) request.setAttribute(AiToolService.WRITE, new AiToolProtocol.Call(name, arguments, true));
        request.setAttribute(PathFilter.AI_TOOL_ENTRY_PATH, "/admin/rest/ai/tools/" + entry.definition().name() + "/call");
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        try {
            ServletRequestPathUtils.parseAndCache(target);
            chain.doFilter(target, response);
            status = response.getStatus();
        } finally {
            request.removeAttribute(AiToolService.TARGET);
            request.removeAttribute(AiToolService.WRITE);
            request.removeAttribute(PathFilter.AI_TOOL_ENTRY_PATH);
            ServletRequestPathUtils.setParsedRequestPath(originalPath, request);
            AiToolAudit.finish(request, status);
        }
    }

    public static void rejectUntranslated(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getAttribute(FAILURE) instanceof Failure failure && failure.authorization().isAllowed(failure.entry(), request)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            if (failure.correction() != null) {
                response.setHeader(TOOL_RESPONSE_HEADER, "invalid-arguments");
                response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(failure.correction());
            }
            return;
        }
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    private record Failure(AiToolRegistry.Entry entry, AiToolAuthorization authorization, String correction) {}
}
