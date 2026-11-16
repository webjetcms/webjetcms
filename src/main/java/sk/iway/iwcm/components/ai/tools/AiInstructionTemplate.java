package sk.iway.iwcm.components.ai.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.util.ConcurrentLruCache;

/** Reusable syntax of an assistant or skill instruction, independent of domain and user permissions. */
final class AiInstructionTemplate {

    private static final Pattern LINK = Pattern.compile("@(tool|group)\\(([A-Za-z0-9_.$-]+)\\)|@skill\\(([^()\\r\\n]+)\\)");
    private static final int MAX_CACHED_CHARS = 8_192;
    private static final ConcurrentLruCache<String, AiInstructionTemplate> CACHE =
            new ConcurrentLruCache<>(256, AiInstructionTemplate::new);

    private enum Kind { TOOL, GROUP, SKILL }
    private record Link(int start, int end, Kind kind, String name) {}

    private final String source;
    private final List<Link> links;
    private final Set<String> skillNames;
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
            skillNames = Set.of();
            toolNames = Set.of();
            groupNames = Set.of();
            return;
        }

        var matches = new ArrayList<Link>();
        var skills = new LinkedHashSet<String>();
        var tools = new LinkedHashSet<String>();
        var groups = new LinkedHashSet<String>();
        do {
            String name = matcher.group(2);
            String skill = matcher.group(3);
            Kind kind;
            if (skill != null) {
                kind = Kind.SKILL;
                name = AiSkillLink.decode(skill);
                skills.add(name);
            } else if ("tool".equals(matcher.group(1))) {
                kind = Kind.TOOL;
                tools.add(name);
            } else {
                kind = Kind.GROUP;
                groups.add(name);
            }
            matches.add(new Link(matcher.start(), matcher.end(), kind, name));
        } while (matcher.find());
        links = List.copyOf(matches);
        skillNames = Collections.unmodifiableSet(skills);
        toolNames = Collections.unmodifiableSet(tools);
        groupNames = Collections.unmodifiableSet(groups);
    }

    Set<String> skillNames() { return skillNames; }
    Set<String> toolNames() { return toolNames; }
    Set<String> groupNames() { return groupNames; }
    boolean hasReferences() { return !links.isEmpty(); }

    /** Expands each reference at its original position. A missing resolver or replacement leaves the original link intact. */
    String render(Function<String, String> tool, Function<String, String> group, Function<String, String> skill) {
        if (links.isEmpty()) return source;
        var result = new StringBuilder(source.length());
        int cursor = 0;
        for (Link link : links) {
            result.append(source, cursor, link.start());
            var resolver = switch (link.kind()) {
                case TOOL -> tool;
                case GROUP -> group;
                case SKILL -> skill;
            };
            String replacement = resolver == null ? null : resolver.apply(link.name());
            if (replacement == null) result.append(source, link.start(), link.end());
            else result.append(replacement);
            cursor = link.end();
        }
        return result.append(source, cursor, source.length()).toString();
    }
}
