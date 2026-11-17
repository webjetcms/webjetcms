package sk.iway.iwcm.components.ai.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.beans.BeanUtils;

import sk.iway.iwcm.components.ai.jpa.AiSkillEntity;
import sk.iway.iwcm.components.ai.jpa.AiSkillRepository;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.local.embedding.LocalEmbeddingProvider;
import sk.iway.iwcm.components.ai.providers.local.translate.LocalTranslateProvider;

/** The effective definition for one invocation, kept separate from the stored assistant and editor data. */
public record AiAssistantRuntime(AssistantDefinitionEntity assistant, String skillInstructions,
        List<AiToolDefinition> tools, boolean hasWriteTool) {

    /** Extracts distinct skill names for save validation and runtime lookup. */
    public static Set<String> referencedSkillNames(String instructions) {
        return AiInstructionTemplate.parse(instructions).skillNames();
    }

    /** Checks for skill, tool or group links using the same syntax as runtime resolution. */
    public static boolean hasReferences(String instructions) {
        return AiInstructionTemplate.parse(instructions).hasReferences();
    }

    static AiAssistantRuntime compose(AiSkillRepository repository, AssistantDefinitionEntity assistant,
            int domainId, AiToolRegistry registry, Predicate<AiToolRegistry.Entry> mayUse) {
        if (!canHaveReferences(assistant)) return new AiAssistantRuntime(assistant, "", List.of(), false);
        var own = AiInstructionTemplate.parse(assistant.getInstructions());
        if (!own.hasReferences()) return new AiAssistantRuntime(assistant, "", List.of(), false);

        var skillNames = own.skillNames();
        List<AiSkillEntity> skills = skillNames.isEmpty() ? List.of() : repository.findByDomainIdAndNameInOrderByNameAsc(domainId, skillNames)
                .stream().filter(skill -> skillNames.contains(skill.getName())).toList();
        var skillTemplates = new HashMap<String, AiInstructionTemplate>();
        var names = new LinkedHashSet<>(own.toolNames());
        var groups = new LinkedHashSet<>(own.groupNames());
        for (var skill : skills) {
            var template = AiInstructionTemplate.parse(skill.getInstructions());
            skillTemplates.put(skill.getName(), template);
            names.addAll(template.toolNames());
            groups.addAll(template.groupNames());
        }

        var visible = new LinkedHashMap<String, AiToolDefinition>();
        var groupMembers = new HashMap<String, List<String>>();
        boolean hasWrite = false;
        for (var entry : registry.assignedEntries(names, groups)) {
            var tool = entry.definition();
            // Keep write-preset safeguards even when the caller cannot use this tool.
            hasWrite |= tool.writes();
            if (!mayUse.test(entry)) continue;
            visible.put(tool.name(), tool);
            if (!groups.isEmpty()) {
                String group = entry.assignmentGroup();
                if (groups.contains(group)) groupMembers.computeIfAbsent(group, ignored -> new ArrayList<>()).add(tool.name());
            }
        }

        var groupLabels = new HashMap<String, String>();
        groupMembers.forEach((group, members) -> groupLabels.put(group, "(choose an appropriate tool from: " + String.join(", ", members) + ")"));
        Function<String, String> toolLabel = name -> visible.containsKey(name) ? name : "[tool unavailable]";
        Function<String, String> groupLabel = group -> groupLabels.getOrDefault(group, "[no available tools in this group]");
        var guidance = new StringBuilder();
        if (!skills.isEmpty()) guidance.append("Use the skills that apply to the user's request.");
        for (var skill : skills) {
            guidance.append("\n\nSkill: ").append(skill.getName()).append("\n")
                    .append(skillTemplates.get(skill.getName()).render(toolLabel, groupLabel, null));
        }
        String instructions = own.render(toolLabel, groupLabel, name -> skillTemplates.containsKey(name) ? "skill " + name : "[skill unavailable]");
        var effective = new AssistantDefinitionEntity();
        BeanUtils.copyProperties(assistant, effective);
        String skillInstructions = guidance.toString();
        if (!skillInstructions.isEmpty()) instructions += "\n\n" + skillInstructions;
        effective.setInstructions(instructions);
        return new AiAssistantRuntime(effective, skillInstructions, List.copyOf(visible.values()), hasWrite);
    }

    static boolean canHaveReferences(AssistantDefinitionEntity assistant) {
        String instructions = assistant.getInstructions();
        return instructions != null && instructions.indexOf('@') >= 0 && supportsReferences(assistant.getProvider(), assistant.getAction());
    }

    /** Whether the selected provider and action can use instruction references. */
    public static boolean supportsReferences(String provider, String action) {
        if (!("generate_text".equals(action) || "live_chat".equals(action))) return false;
        return !"browser".equals(provider) && !LocalTranslateProvider.PROVIDER_ID.equals(provider)
                && !LocalEmbeddingProvider.PROVIDER_ID.equals(provider) && !"local-embedding".equals(provider);
    }
}
