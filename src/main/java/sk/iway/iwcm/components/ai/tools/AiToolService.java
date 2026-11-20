package sk.iway.iwcm.components.ai.tools;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.tools.AiToolAuthorization.Access;
import sk.iway.iwcm.i18n.Prop;
import tools.jackson.databind.json.JsonMapper;

/** Provides tool catalogs and assistant runtime metadata in an authenticated CMS request. */
@Service
@RequiredArgsConstructor
public class AiToolService {

    static final String TARGET = AiToolService.class.getName() + ".target";
    static final String WRITE = AiToolService.class.getName() + ".write";
    // The browser accepts up to 1 MiB per catalog response in ai-tool-transport.js.
    private static final int MAX_CATALOG_BYTES = 1024 * 1024;
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final AiToolRegistry registry;
    private final AiToolAuthorization authorization;

    /** True while the named tool dispatcher is invoking a handler of this controller. */
    public static boolean isToolRequest(HttpServletRequest request, Class<?> controller) {
        return request != null && request.getAttribute(TARGET) instanceof AiToolRegistry.Entry entry
                && controller.isAssignableFrom(entry.controllerType());
    }

    public List<AiToolDefinition> list(HttpServletRequest request) {
        return list(request, null);
    }

    public List<AiToolDefinition.Preview> preview(HttpServletRequest request) {
        return authorization.visibleEntries(request)
                .map(entry -> entry.definition().preview()).toList();
    }

    public List<AiToolDefinition> details(List<String> names, HttpServletRequest request) {
        return names.stream().distinct().flatMap(name -> findVisible(name, request, null).stream()).toList();
    }

    public AiAssistantRuntime resolve(AssistantDefinitionEntity assistant, int domainId, HttpServletRequest request) {
        return authorization.resolve(assistant, domainId, request);
    }

    /** Backend calls include linked instructions eagerly because they cannot run the browser tool loop. */
    public AiAssistantRuntime resolveForBackend(AssistantDefinitionEntity assistant, int domainId, HttpServletRequest request) {
        return authorization.resolveForBackend(assistant, domainId, request);
    }

    /** Loads only a skill linked to the active assistant in the current user's domain. */
    public AiAssistantRuntime.Skill loadSkill(String name, HttpServletRequest request) {
        var skill = authorization.assignedAssistant(request).map(runtime -> runtime.skills().get(name))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (skill.directTools().isEmpty() || JSON.writeValueAsBytes(skill).length <= MAX_CATALOG_BYTES) return skill;
        // Large schemas remain available individually through ai_tools_details.
        return new AiAssistantRuntime.Skill(skill.name(), skill.version(), skill.instructions());
    }

    public record BrowserTool(String name, boolean write) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RuntimeMetadata(String provider, String action, boolean toolsEnabled, boolean toolsWrite,
            String instructions, String skillInstructions, List<BrowserTool> tools, String toolInstructions) {}

    public RuntimeMetadata runtimeMetadata(long assistantId, HttpServletRequest request) {
        var runtime = authorization.assistant(assistantId, request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var assistant = runtime.assistant();
        // Browser assistants do not resolve references, but their runtime response includes the original instructions.
        if ("browser".equals(assistant.getProvider())) {
            return new RuntimeMetadata(assistant.getProvider(), assistant.getAction(), false, false,
                    assistant.getInstructions(), "", List.of(), null);
        }
        return new RuntimeMetadata(assistant.getProvider(), assistant.getAction(), !runtime.tools().isEmpty(),
                runtime.hasWriteTool(), null, null, null, null);
    }

    /** An editor can offer a preset on an empty field when its skills or visible tools supply context. */
    public boolean hasInstructionContext(AssistantDefinitionEntity assistant, HttpServletRequest request) {
        return authorization.hasInstructionContext(assistant, CloudToolsForCore.getDomainId(), request);
    }

    public boolean isWriteTool(String name) {
        var entry = registry.lookup(name);
        return entry != null && entry.definition().writes();
    }

    public boolean isRowReorderTool(String name) {
        var entry = registry.lookup(name);
        return entry != null && entry.definition().rowReorder();
    }

    public List<AiToolDefinition> list(HttpServletRequest request, Prop language) {
        return authorization.visibleEntries(request).map(entry -> entry.definition(language)).toList();
    }

    /** Tool and group metadata for the instruction link picker. */
    public record AssignmentTool(String name, String description, String group, String groupLabel, String httpMethod, String path) {}

    public List<AssignmentTool> assignmentTools(HttpServletRequest request, Prop language) {
        return authorization.visibleEntries(request).map(entry -> {
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
        var groups = authorization.visibleEntries(request).collect(Collectors.groupingBy(AiToolRegistry.Entry::controllerType));
        return groups.entrySet().stream().map(group -> new AiToolGroup(group.getKey(), group.getValue(), language)).toList();
    }

    public Optional<AiToolDefinition> findVisible(String name, HttpServletRequest request, Prop language) {
        return Optional.ofNullable(registry.lookup(name))
            .filter(entry -> authorization.mayList(entry, request, Access.ASSIGNED))
            .map(entry -> entry.definition(language));
    }
}
