package sk.iway.iwcm.components.ai.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AiSkillEntity;
import sk.iway.iwcm.components.ai.jpa.AiSkillRepository;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.local.embedding.LocalEmbeddingProvider;
import sk.iway.iwcm.components.ai.providers.local.translate.LocalTranslateProvider;

/** The effective definition for one invocation, kept separate from the stored assistant and editor data. */
public record AiAssistantRuntime(AssistantDefinitionEntity assistant, String skillInstructions,
        List<AiToolDefinition> tools, boolean hasWriteTool) {

    private static final Pattern TOOL_REFERENCE = Pattern.compile("@(tool|group)\\(([A-Za-z0-9_.$-]+)\\)");

    private static final Pattern SKILL_REFERENCE = Pattern.compile("@skill\\(([0-9]+)\\)");

    /** Extracts distinct numeric skill IDs for save validation and runtime lookup. */
    public static Set<Long> referencedSkillIds(String instructions) {
        return SKILL_REFERENCE.matcher(instructions == null ? "" : instructions).results()
                .map(match -> Tools.getLongValue(match.group(1), -1L))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static AiAssistantRuntime compose(AiSkillRepository repository, AssistantDefinitionEntity assistant,
            int domainId, AiToolRegistry registry, Predicate<AiToolRegistry.Entry> mayUse) {
        String own = assistant.getInstructions();
        if (!acceptsSkills(assistant) || own == null || own.indexOf('@') < 0) {
            return new AiAssistantRuntime(assistant, "", List.of(), false);
        }
        var skillIds = referencedSkillIds(own);
        List<AiSkillEntity> skills = skillIds.isEmpty() ? List.of() : repository.findByDomainIdAndIdInOrderByIdAsc(domainId, skillIds);
        var names = new LinkedHashSet<String>();
        var groups = new LinkedHashMap<String, List<String>>();
        collectToolReferences(own, names, groups);
        for (var skill : skills) collectToolReferences(skill.getInstructions(), names, groups);

        boolean browser = "browser".equals(assistant.getProvider());
        var visible = new LinkedHashMap<String, AiToolDefinition>();
        boolean hasWrite = false;
        if (!browser || "LanguageModel".equals(browserApi(assistant))) {
            var entries = groups.isEmpty()
                    ? names.stream().map(registry::lookup).filter(Objects::nonNull).toList()
                    : registry.entries();
            for (var entry : entries) {
                var tool = entry.definition();
                var members = groups.get(entry.assignmentGroup());
                if (members == null && !names.contains(tool.name())) continue;
                // Keep write-preset safeguards even when the caller cannot use this tool.
                hasWrite |= tool.writes();
                if (!mayUse.test(entry)) continue;
                visible.put(tool.name(), tool);
                if (members != null) members.add(tool.name());
            }
        }

        var loadedIds = new LinkedHashSet<Long>();
        var guidance = new StringBuilder();
        for (var skill : skills) {
            if (guidance.isEmpty()) guidance.append("The following skills are optional. Use the relevant guidance and tools only when they help with the user's request.");
            loadedIds.add(skill.getId());
            guidance.append("\n\nSkill #").append(skill.getId()).append(" — ").append(skill.getName()).append("\n")
                    .append(expandToolReferences(skill.getInstructions(), visible.keySet(), groups));
        }
        own = expandToolReferences(own, visible.keySet(), groups);
        own = SKILL_REFERENCE.matcher(own).replaceAll(link -> {
            long id = Tools.getLongValue(link.group(1), -1L);
            return loadedIds.contains(id) ? "skill #" + id : "[skill unavailable]";
        });
        var effective = new AssistantDefinitionEntity();
        BeanUtils.copyProperties(assistant, effective);
        if (!browser && !guidance.isEmpty()) own += "\n\n" + guidance;
        effective.setInstructions(own);
        return new AiAssistantRuntime(effective, guidance.toString(), List.copyOf(visible.values()), hasWrite);
    }

    private static void collectToolReferences(String instructions, Set<String> names, Map<String, List<String>> groups) {
        Matcher links = TOOL_REFERENCE.matcher(instructions);
        while (links.find()) {
            String key = links.group(2);
            if ("tool".equals(links.group(1))) names.add(key);
            else groups.computeIfAbsent(key, ignored -> new ArrayList<>());
        }
    }

    private static String expandToolReferences(String instructions, Set<String> visible, Map<String, List<String>> groups) {
        return TOOL_REFERENCE.matcher(instructions).replaceAll(link -> {
            String key = link.group(2);
            if ("tool".equals(link.group(1))) return visible.contains(key) ? Matcher.quoteReplacement(key) : "[tool unavailable]";
            var members = groups.get(key);
            return members.isEmpty() ? "[no available tools in this group]"
                    : Matcher.quoteReplacement("(choose an appropriate tool from: " + String.join(", ", members) + ")");
        });
    }

    static boolean acceptsSkills(AssistantDefinitionEntity assistant) {
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
