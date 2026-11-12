package sk.iway.iwcm.components.ai.tools;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Service;
import org.springframework.web.util.ServletRequestPathUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.i18n.Prop;

/** Provides the English tool catalog and dispatches calls in an authenticated CMS request. */
@Service
@RequiredArgsConstructor
public class AiToolService {

    static final String TARGET = AiToolService.class.getName() + ".target";
    private final AiToolRegistry registry;
    private final AiToolAuthorization authorization;

    public List<AiToolDefinition> list(HttpServletRequest request) {
        return list(request, null);
    }

    public List<AiToolDefinition> list(HttpServletRequest request, Prop language) {
        return registry.entries().stream()
            .filter(entry -> mayList(entry, request))
            .map(entry -> entry.definition(language))
            .toList();
    }

    public Optional<AiToolDefinition> findVisible(String name, HttpServletRequest request) {
        return findVisible(name, request, null);
    }

    public Optional<AiToolDefinition> findVisible(String name, HttpServletRequest request, Prop language) {
        return Optional.ofNullable(registry.lookup(name))
            .filter(entry -> mayList(entry, request))
            .map(entry -> entry.definition(language));
    }

    private boolean mayList(AiToolRegistry.Entry entry, HttpServletRequest request) {
        return AiToolServletRoutes.mayList(request.getServletContext(), entry) && authorization.isAllowed(entry, request);
    }

    /**
     * Binds arguments to one registered mapping and continues through the server's native REST filter chain.
     * Start the remaining authentication, security and MVC chain with the translated request.
     */
    public void call(String name, Map<String, Object> arguments, HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (!AiToolServletRoutes.usesDispatcher(request)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        AiToolRegistry.Entry entry = registry.lookup(name);
        if (entry == null) {
            reject(request, response, chain);
            return;
        }
        AiToolRequest target;
        try {
            if (arguments == null) throw new IllegalArgumentException("Expected an object");
            target = new AiToolRequest(request, entry, entry.inputs().bind(arguments));
        } catch (IllegalArgumentException invalidArguments) {
            AiToolCallFilter.invalid(request, entry, authorization);
            reject(request, response, chain);
            return;
        }
        if (!AiToolServletRoutes.permits(request, target)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        RequestPath originalPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
        request.setAttribute(TARGET, entry);
        request.setAttribute(PathFilter.AI_TOOL_ENTRY_PATH, "/admin/rest/ai/tools/" + entry.definition().name() + "/call");
        try {
            ServletRequestPathUtils.parseAndCache(target);
            chain.doFilter(target, response);
        } finally {
            request.removeAttribute(TARGET);
            request.removeAttribute(PathFilter.AI_TOOL_ENTRY_PATH);
            ServletRequestPathUtils.setParsedRequestPath(originalPath, request);
        }
    }

    private static void reject(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        // The public HTTP facade still needs the ordinary admin filters on an unresolved call.
        if (request.getRequestURI().startsWith(request.getContextPath() + "/admin/rest/ai/tools/")
                && request.getRequestURI().endsWith("/call") && "POST".equals(request.getMethod())) {
            chain.doFilter(request, response);
        } else {
            AiToolCallFilter.rejectUntranslated(request, response);
        }
    }
}
