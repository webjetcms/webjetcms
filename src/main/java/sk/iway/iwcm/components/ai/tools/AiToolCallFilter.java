package sk.iway.iwcm.components.ai.tools;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Translates a JSON tool call before WebJET captures target-specific request and domain information. */
@Component
public class AiToolCallFilter extends GenericFilterBean {

    private static final String PREFIX = "/admin/rest/ai/tools/";
    private static final String SUFFIX = "/call";
    private static final String FAILURE = AiToolCallFilter.class.getName() + ".failure";
    private static final int MAX_ENVELOPE_BYTES = 1024 * 1024;
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();
    private static final TypeReference<Map<String, Object>> ARGUMENTS = new TypeReference<>() {};
    private final AiToolService service;

    public AiToolCallFilter(AiToolService service) {
        this.service = service;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        String path = http.getRequestURI().substring(http.getContextPath().length());
        if (!"POST".equals(http.getMethod()) || path.length() <= PREFIX.length() + SUFFIX.length()
                || !path.startsWith(PREFIX) || !path.endsWith(SUFFIX)) {
            chain.doFilter(request, response);
            return;
        }
        String name = path.substring(PREFIX.length(), path.length() - SUFFIX.length());
        if (!name.matches("[A-Za-z0-9_-]{1,64}")) {
            chain.doFilter(request, response);
            return;
        }
        Map<String, Object> arguments = null;
        try {
            if (http.getContentType() != null && MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(http.getContentType()))) {
                if (http.getContentLengthLong() > MAX_ENVELOPE_BYTES) {
                    ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                    return;
                }
                byte[] bytes = http.getInputStream().readNBytes(MAX_ENVELOPE_BYTES + 1);
                if (bytes.length > MAX_ENVELOPE_BYTES) {
                    ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                    return;
                }
                arguments = JSON.readValue(bytes, ARGUMENTS);
            }
        } catch (JacksonException | IllegalArgumentException malformedInput) {
            // The authenticated fallback reports invalid arguments only for a visible tool.
        }
        service.call(name, arguments, http, (HttpServletResponse) response, chain);
    }

    static void invalid(HttpServletRequest request, AiToolRegistry.Entry entry, AiToolAuthorization authorization) {
        request.setAttribute(FAILURE, new Failure(entry, authorization));
    }

    public static void rejectUntranslated(HttpServletRequest request, HttpServletResponse response) {
        int status = HttpServletResponse.SC_NOT_FOUND;
        if (request.getAttribute(FAILURE) instanceof Failure failure && failure.authorization().isAllowed(failure.entry(), request)) {
            status = HttpServletResponse.SC_BAD_REQUEST;
        }
        response.setStatus(status);
    }

    private record Failure(AiToolRegistry.Entry entry, AiToolAuthorization authorization) {}
}
