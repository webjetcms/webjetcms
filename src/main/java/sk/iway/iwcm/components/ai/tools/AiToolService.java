package sk.iway.iwcm.components.ai.tools;

import java.io.IOException;
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

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.common.CloudToolsForCore;
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
        return visibleEntries(request, Access.CATALOG)
                .map(entry -> entry.definition().preview()).toList();
    }

    public List<AiToolDefinition> details(List<String> names, HttpServletRequest request) {
        return names.stream().distinct().map(registry::lookup).filter(Objects::nonNull)
                .filter(entry -> authorization.mayList(entry, request, Access.CATALOG)).map(AiToolRegistry.Entry::definition).toList();
    }

    public AiAssistantRuntime resolve(AssistantDefinitionEntity assistant, int domainId, HttpServletRequest request) {
        return authorization.resolve(assistant, domainId, request);
    }

    public record BrowserTool(String name, boolean write) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RuntimeMetadata(String provider, String action, boolean toolsEnabled, boolean toolsWrite,
            String instructions, String skillInstructions, List<BrowserTool> tools, String toolInstructions) {}

    public RuntimeMetadata runtimeMetadata(long assistantId, HttpServletRequest request) {
        var runtime = authorization.assistant(assistantId, request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var assistant = runtime.assistant();
        boolean browser = "browser".equals(assistant.getProvider());
        var definitions = runtime.tools();
        return new RuntimeMetadata(assistant.getProvider(), assistant.getAction(), !definitions.isEmpty(), runtime.hasWriteTool(),
                browser ? assistant.getInstructions() : null, browser ? runtime.skillInstructions() : null,
                browser ? definitions.stream().map(tool -> new BrowserTool(tool.name(), tool.writes())).toList() : null,
                browser && !definitions.isEmpty() ? AiToolProtocol.instructions(definitions) : null);
    }

    /** An editor can offer a preset on an empty field when its skills or visible tools supply context. */
    public boolean hasInstructionContext(AssistantDefinitionEntity assistant, HttpServletRequest request) {
        return authorization.hasInstructionContext(assistant, CloudToolsForCore.getDomainId(), request);
    }

    public boolean isWriteTool(String name) {
        var entry = registry.lookup(name);
        return entry != null && entry.definition().writes();
    }

    public List<AiToolDefinition> list(HttpServletRequest request, Prop language) {
        return visibleEntries(request, Access.ASSIGNED).map(entry -> entry.definition(language)).toList();
    }

    /** Tool and group metadata for the instruction link picker. */
    public record AssignmentTool(String name, String description, String group, String groupLabel, String httpMethod, String path) {}

    public List<AssignmentTool> assignmentTools(HttpServletRequest request, Prop language) {
        return visibleEntries(request, Access.ASSIGNED).map(entry -> {
            Class<?> type = entry.controllerType();
            String group = entry.assignmentGroup();
            String resource = group.equals(type.getName()) ? AiToolRegistry.resource(type) : group;
            String label = AiToolRegistry.translated(language, "components.ai_assistants.tools.group." + resource, resource.replace('_', ' '));
            var tool = entry.definition(language);
            return new AssignmentTool(tool.name(), tool.description(), group,
                    label, tool.httpMethod(), tool.path());
        }).toList();
    }

    public List<AiToolGroup> listGroups(HttpServletRequest request, Prop language) {
        var groups = visibleEntries(request, Access.ASSIGNED).collect(Collectors.groupingBy(AiToolRegistry.Entry::controllerType));
        return groups.entrySet().stream().map(group -> new AiToolGroup(group.getKey(), group.getValue(), language)).toList();
    }

    private Stream<AiToolRegistry.Entry> visibleEntries(HttpServletRequest request, Access access) {
        return registry.entries().stream().filter(entry -> authorization.mayList(entry, request, access));
    }

    public Optional<AiToolDefinition> findVisible(String name, HttpServletRequest request, Prop language) {
        return Optional.ofNullable(registry.lookup(name))
            .filter(entry -> authorization.mayList(entry, request, Access.ASSIGNED))
            .map(entry -> entry.definition(language));
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
            String correction = invalidArguments instanceof AiToolArgumentValidator.InvalidArguments ? invalidArguments.getMessage() : null;
            AiToolCallFilter.invalid(request, entry, authorization, correction);
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
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        try {
            ServletRequestPathUtils.parseAndCache(target);
            chain.doFilter(target, response);
            status = response.getStatus();
        } finally {
            request.removeAttribute(TARGET);
            request.removeAttribute(PathFilter.AI_TOOL_ENTRY_PATH);
            ServletRequestPathUtils.setParsedRequestPath(originalPath, request);
            AiToolAudit.finish(request, status);
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
