package sk.iway.iwcm.components.ai.tools;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.server.RequestPath;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.ServletRequestPathUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.tools.AiToolAuthorization.Access;
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

    public List<AiToolDefinition.Preview> preview(HttpServletRequest request) {
        return registry.entries().stream().filter(entry -> mayList(entry, request, Access.CATALOG))
                .map(entry -> entry.definition().preview()).toList();
    }

    public List<AiToolDefinition> details(List<String> names, HttpServletRequest request) {
        return names.stream().distinct().map(registry::lookup).filter(Objects::nonNull)
                .filter(entry -> mayList(entry, request, Access.CATALOG)).map(AiToolRegistry.Entry::definition).toList();
    }

    public List<AiToolDefinition> assigned(long assistantId, HttpServletRequest request) {
        return assigned(authorization.assistant(assistantId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), request);
    }

    /** Resolves the selected assistant's registered names against the caller's current endpoint permissions. */
    public List<AiToolDefinition> assigned(AssistantDefinitionEntity assistant, HttpServletRequest request) {
        if (Boolean.FALSE.equals(assistant.getActive()) || !AiToolProtocol.supports(assistant)) return List.of();
        return assignedEntries(assistant).filter(entry -> mayList(entry, request)).map(AiToolRegistry.Entry::definition).toList();
    }

    public boolean hasAssignedWrite(AssistantDefinitionEntity assistant) {
        return assignedEntries(assistant).anyMatch(entry -> entry.definition().writes());
    }

    public boolean isWriteTool(String name) {
        var entry = registry.lookup(name);
        return entry != null && entry.definition().writes();
    }

    private Stream<AiToolRegistry.Entry> assignedEntries(AssistantDefinitionEntity assistant) {
        return Arrays.stream(assistant.getToolNames().split(",")).distinct().map(registry::lookup).filter(Objects::nonNull);
    }

    public List<AiToolDefinition> list(HttpServletRequest request, Prop language) {
        return visibleEntries(request).map(entry -> entry.definition(language)).toList();
    }

    public List<AiToolGroup> listGroups(HttpServletRequest request, Prop language) {
        var groups = visibleEntries(request).collect(Collectors.groupingBy(AiToolRegistry.Entry::controllerType));
        return groups.entrySet().stream().map(group -> new AiToolGroup(group.getKey(), group.getValue(), language)).toList();
    }

    private Stream<AiToolRegistry.Entry> visibleEntries(HttpServletRequest request) {
        return registry.entries().stream().filter(entry -> mayList(entry, request));
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
        return mayList(entry, request, Access.ASSIGNED);
    }

    private boolean mayList(AiToolRegistry.Entry entry, HttpServletRequest request, Access access) {
        return AiToolServletRoutes.mayList(request.getServletContext(), entry) && authorization.isAllowed(entry, request, access);
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
            if (request.getHeader(AiToolAuthorization.ASSISTANT_HEADER) != null) {
                AiToolArgumentValidator.validate(entry.definition().inputSchema(), arguments);
            }
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
