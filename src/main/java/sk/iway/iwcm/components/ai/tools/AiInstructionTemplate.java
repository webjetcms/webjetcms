package sk.iway.iwcm.components.ai.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.util.ConcurrentLruCache;

import sk.iway.iwcm.Tools;

/** Reusable syntax of an assistant or skill instruction, independent of domain and user permissions. */
final class AiInstructionTemplate {

    private static final Pattern LINK = Pattern.compile("@(tool|group)\\(([A-Za-z0-9_.$-]+)\\)|@skill\\(([0-9]+)\\)");
    private static final int MAX_CACHED_CHARS = 8_192;
    private static final ConcurrentLruCache<String, AiInstructionTemplate> CACHE =
            new ConcurrentLruCache<>(256, AiInstructionTemplate::new);

    private enum Kind { TOOL, GROUP, SKILL }
    private record Link(int start, int end, Kind kind, String name, Long skillId) {}

    private final String source;
    private final List<Link> links;
    private final Set<Long> skillIds;
    private final Set<String> toolNames;
    private final Set<String> groupNames;

    static AiInstructionTemplate parse(String source) {
        String text = source == null ? "" : source;
        return text.length() <= MAX_CACHED_CHARS ? CACHE.get(text) : new AiInstructionTemplate(text);
    }

    private AiInstructionTemplate(String source) {
        this.source = source;
        var matcher = LINK.matcher(source);
        if (!matcher.find()) {
            links = List.of();
            skillIds = Set.of();
            toolNames = Set.of();
            groupNames = Set.of();
            return;
        }

        var matches = new ArrayList<Link>();
        var skills = new LinkedHashSet<Long>();
        var tools = new LinkedHashSet<String>();
        var groups = new LinkedHashSet<String>();
        do {
            String name = matcher.group(2);
            String digits = matcher.group(3);
            Long skillId = null;
            Kind kind;
            if (digits != null) {
                kind = Kind.SKILL;
                skillId = Tools.getLongValue(digits, -1L);
                skills.add(skillId);
            } else if ("tool".equals(matcher.group(1))) {
                kind = Kind.TOOL;
                tools.add(name);
            } else {
                kind = Kind.GROUP;
                groups.add(name);
            }
            matches.add(new Link(matcher.start(), matcher.end(), kind, name, skillId));
        } while (matcher.find());
        links = List.copyOf(matches);
        skillIds = Collections.unmodifiableSet(skills);
        toolNames = Collections.unmodifiableSet(tools);
        groupNames = Collections.unmodifiableSet(groups);
    }

    Set<Long> skillIds() { return skillIds; }
    Set<String> toolNames() { return toolNames; }
    Set<String> groupNames() { return groupNames; }
    boolean hasReferences() { return !links.isEmpty(); }

    /** Expands each reference at its original position. A null skill resolver leaves skill links literal. */
    String render(Function<String, String> tool, Function<String, String> group, Function<Long, String> skill) {
        if (links.isEmpty()) return source;
        var result = new StringBuilder(source.length());
        int cursor = 0;
        for (Link link : links) {
            result.append(source, cursor, link.start());
            switch (link.kind()) {
                case TOOL -> result.append(tool.apply(link.name()));
                case GROUP -> result.append(group.apply(link.name()));
                case SKILL -> {
                    if (skill == null) result.append(source, link.start(), link.end());
                    else result.append(skill.apply(link.skillId()));
                }
            }
            cursor = link.end();
        }
        return result.append(source, cursor, source.length()).toString();
    }
}
