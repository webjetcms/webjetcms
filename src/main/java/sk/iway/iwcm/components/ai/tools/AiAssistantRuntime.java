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

    /** Extracts distinct numeric skill IDs for save validation and runtime lookup. */
    public static Set<Long> referencedSkillIds(String instructions) {
        return AiInstructionTemplate.parse(instructions).skillIds();
    }

    static AiAssistantRuntime compose(AiSkillRepository repository, AssistantDefinitionEntity assistant,
            int domainId, AiToolRegistry registry, Predicate<AiToolRegistry.Entry> mayUse) {
        if (!canHaveReferences(assistant)) return new AiAssistantRuntime(assistant, "", List.of(), false);
        var own = AiInstructionTemplate.parse(assistant.getInstructions());
        if (!own.hasReferences()) return new AiAssistantRuntime(assistant, "", List.of(), false);

        var skillIds = own.skillIds();
        List<AiSkillEntity> skills = skillIds.isEmpty() ? List.of() : repository.findByDomainIdAndIdInOrderByIdAsc(domainId, skillIds);
        var skillTemplates = new HashMap<Long, AiInstructionTemplate>();
        var names = new LinkedHashSet<>(own.toolNames());
        var groups = new LinkedHashSet<>(own.groupNames());
        for (var skill : skills) {
            var template = AiInstructionTemplate.parse(skill.getInstructions());
            skillTemplates.put(skill.getId(), template);
            names.addAll(template.toolNames());
            groups.addAll(template.groupNames());
        }

        var visible = new LinkedHashMap<String, AiToolDefinition>();
        var groupMembers = new HashMap<String, List<String>>();
        boolean hasWrite = false;
        if (acceptsTools(assistant)) {
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
        }

        var groupLabels = new HashMap<String, String>();
        groupMembers.forEach((group, members) -> groupLabels.put(group, "(choose an appropriate tool from: " + String.join(", ", members) + ")"));
        Function<String, String> toolLabel = name -> visible.containsKey(name) ? name : "[tool unavailable]";
        Function<String, String> groupLabel = group -> groupLabels.getOrDefault(group, "[no available tools in this group]");
        var guidance = new StringBuilder();
        for (var skill : skills) {
            if (guidance.isEmpty()) guidance.append("The following skills are optional. Use the relevant guidance and tools only when they help with the user's request.");
            guidance.append("\n\nSkill #").append(skill.getId()).append(" — ").append(skill.getName()).append("\n")
                    .append(skillTemplates.get(skill.getId()).render(toolLabel, groupLabel, null));
        }
        String instructions = own.render(toolLabel, groupLabel, id -> skillTemplates.containsKey(id) ? "skill #" + id : "[skill unavailable]");
        var effective = new AssistantDefinitionEntity();
        BeanUtils.copyProperties(assistant, effective);
        String skillInstructions = guidance.toString();
        if (!"browser".equals(assistant.getProvider()) && !skillInstructions.isEmpty()) instructions += "\n\n" + skillInstructions;
        effective.setInstructions(instructions);
        return new AiAssistantRuntime(effective, skillInstructions, List.copyOf(visible.values()), hasWrite);
    }

    /** Checks tool support after the action and provider have passed the skill eligibility check. */
    static boolean acceptsTools(AssistantDefinitionEntity assistant) {
        return !"browser".equals(assistant.getProvider()) || "LanguageModel".equals(browserApi(assistant));
    }

    static boolean canHaveReferences(AssistantDefinitionEntity assistant) {
        String instructions = assistant.getInstructions();
        if (instructions == null || instructions.indexOf('@') < 0) return false;
        if (!("generate_text".equals(assistant.getAction()) || "live_chat".equals(assistant.getAction()))) return false;
        String provider = assistant.getProvider();
        if (LocalTranslateProvider.PROVIDER_ID.equals(provider) || LocalEmbeddingProvider.PROVIDER_ID.equals(provider)
                || "local-embedding".equals(provider)) return false;
        if (!"browser".equals(provider)) return true;
        return switch (browserApi(assistant)) {
            case "LanguageModel", "Writer", "Rewriter", "Summarizer" -> true;
            default -> false;
        };
    }

    static String browserApi(AssistantDefinitionEntity assistant) {
        String config = assistant.getInstructions();
        int delimiter = config == null ? -1 : config.indexOf(':');
        return delimiter < 0 ? "" : config.substring(0, delimiter).strip();
    }
}
