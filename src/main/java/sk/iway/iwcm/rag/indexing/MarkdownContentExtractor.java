package sk.iway.iwcm.rag.indexing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.rag.service.RagEntityType;

/** Prepares Markdown for chunking while preserving document structure and code examples. */
@Component
public class MarkdownContentExtractor implements ContentExtractor<String> {

    private static final Pattern FENCE = Pattern.compile("^(?: {0,3}>[ \\t]?)*[ \\t]*(?:[-+*] |[0-9]+[.)] )?(`{3,}|~{3,})(.*)$");
    private static final Pattern REFERENCE = Pattern.compile(
        "(?m)^ {0,3}\\[([^\\]\\n]+)\\]:[ \\t]*(?:\\n[ \\t]*)?(?:<[^>\\n]+>|\\S+)"
        + "(?:[ \\t]+(?:\"[^\"\\n]*\"|'[^'\\n]*'|\\([^\\n]*\\)))?[ \\t]*"
        + "(?:\\n[ \\t]+(?:\"[^\"\\n]*\"|'[^'\\n]*'|\\([^\\n]*\\))[ \\t]*)?(?=\\n|$)");
    private static final Pattern HTML_TAG = Pattern.compile(
        "</?([A-Za-z][A-Za-z0-9:-]*)(?:\\s+(?:[^'\"<>]|\"[^\"]*\"|'[^']*')*)?\\s*/?>");
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\n[ \\t]*\\n");
    private static final Pattern HEADING = Pattern.compile("^ {0,3}(#{1,6})(?:[ \\t]+(.*)|$)");
    private static final Set<String> HIDDEN_TAGS = Set.of("script", "style", "template", "nav", "footer");
    private static final Set<String> BLOCK_TAGS = Set.of("p", "div", "section", "article", "blockquote", "ul", "ol", "table", "tr", "br", "hr");

    /**
     * Removes front matter, link destinations, image paths, and hidden HTML before chunking.
     * Link labels, existing image descriptions, headings, lists, tables, and code examples are retained.
     *
     * @param markdown source Markdown, or {@code null}
     * @return cleaned Markdown, or an empty string for null input
     */
    @Override
    public String extractText(String markdown) {
        if (markdown == null) return "";
        String source = markdown.replaceFirst("\\A\uFEFF", "").replace("\r\n", "\n").replace('\r', '\n')
            .replaceFirst("\\A---[ \\t]*\\n[\\s\\S]*?\\n(?:---|\\.\\.\\.)[ \\t]*(?:\\n|$)", "");
        ProtectedCode code = protectCode(source);
        String text = code.text().replaceAll("(?s)<!--.*?(?:-->|\\z)", "");
        Set<String> references = new HashSet<>();
        Matcher definitions = REFERENCE.matcher(text);
        text = definitions.replaceAll(match -> {
            references.add(normalizeReference(match.group(1)));
            return "";
        });
        text = cleanLinks(cleanHtml(text), references).replaceAll("\\n(?:[ \\t]*\\n){2,}", "\n\n").strip();
        return code.restore(text);
    }

    /**
     * Uses the first level-one heading outside comments and code, falling back to the relative file path.
     *
     * @param markdown source Markdown, or {@code null}
     * @param sourcePath non-null relative path used when no heading is found
     * @return heading or fallback path, truncated to at most 512 characters
     */
    public String extractTitle(String markdown, String sourcePath) {
        String title = findHeadings(extractText(markdown)).stream()
            .filter(heading -> heading.level() == 1 && heading.title().isBlank() == false)
            .map(Heading::title).findFirst().orElse(sourcePath);
        return title.substring(0, Math.min(512, title.length()));
    }

    /**
     * Adds the document title and the shared heading hierarchy of each passage to its embedding input.
     * Merged sibling subsections use their common parent. Passage bodies remain unchanged, code
     * headings are ignored, and overlapping passages retain context at their own source positions.
     *
     * @param text cleaned Markdown with normalized newlines, as supplied to the chunker
     * @param title document title or source-path fallback
     * @param chunks ordered chunks with offsets in the trimmed source text
     * @return contextual embedding inputs in chunk order
     */
    public List<String> addHeadingContext(String text, String title, List<SlidingWindowChunker.Chunk> chunks) {
        List<Heading> headings = findHeadings(text);
        List<Heading> ancestors = new ArrayList<>();
        List<String> inputs = new ArrayList<>();
        int headingIndex = 0;
        int leadingWhitespace = 0;
        while (leadingWhitespace < text.length() && text.charAt(leadingWhitespace) <= ' ') leadingWhitespace++;
        for (SlidingWindowChunker.Chunk chunk : chunks) {
            int startOffset = chunk.startOffset() + leadingWhitespace;
            while (headingIndex < headings.size() && headings.get(headingIndex).offset() <= startOffset) {
                Heading heading = headings.get(headingIndex++);
                ancestors.removeIf(ancestor -> ancestor.level() >= heading.level());
                ancestors.add(heading);
            }
            int endOffset = startOffset + chunk.text().length();
            if (chunk.startOffset() == 0) {
                for (int i = 0; i < chunk.text().length() && chunk.text().charAt(i) <= ' '; i++) endOffset--;
            }
            List<Heading> shared = new ArrayList<>(ancestors);
            for (int i = headingIndex; i < headings.size() && headings.get(i).offset() < endOffset; i++) {
                int level = headings.get(i).level();
                shared.removeIf(heading -> heading.level() >= level);
            }
            StringJoiner context = new StringJoiner(" > ").add(title);
            for (Heading heading : shared) {
                if (heading.title().isBlank() == false && (heading.level() != 1 || heading.title().equals(title) == false)) {
                    context.add(heading.title());
                }
            }
            inputs.add(context + "\n\n" + chunk.text());
        }
        return inputs;
    }

    /** Finds ATX and Setext headings, mapping protected code placeholders back to source offsets. */
    List<Heading> findHeadings(String text) {
        ProtectedCode code = protectCode(text);
        List<Heading> headings = new ArrayList<>();
        String previous = "";
        int offset = 0;
        int previousOffset = 0;
        for (String line : code.text().split("\\n", -1)) {
            Matcher atx = HEADING.matcher(line);
            String title = null;
            int level = 0;
            int headingOffset = offset;
            if (atx.matches()) {
                level = atx.group(1).length();
                title = atx.group(2) == null ? "" : atx.group(2).replaceFirst("[ \\t]+#+[ \\t]*$", "");
            } else if (line.matches(" {0,3}(?:=+|-+)[ \\t]*") && isSetextContent(previous, code)) {
                level = line.stripLeading().charAt(0) == '=' ? 1 : 2;
                title = previous;
                headingOffset = previousOffset;
            }
            if (title != null) {
                title = code.restore(title).replaceAll("(\\*\\*|__)(.*?)\\1", "$2").replaceAll("`+", "").trim();
                headings.add(new Heading(headingOffset, offset + code.restore(line).length(), level, title));
            }
            previous = line;
            previousOffset = offset;
            offset += code.restore(line).length() + 1;
        }
        return headings;
    }

    /** Distinguishes a Setext heading's text from preceding list items, quotes, rules, and code blocks. */
    private boolean isSetextContent(String line, ProtectedCode code) {
        if (line.isBlank() || HEADING.matcher(line).matches()
                || line.matches(" {0,3}(?:[-+*](?:[ \\t]+.*)?|[0-9]+[.)][ \\t]+.*|>.*|(?:[-*_][ \\t]*){3,})")) return false;
        return code.isBlock(line) == false;
    }

    record Heading(int offset, int endOffset, int level, String title) { }

    /**
     * Protects fenced, indented, and inline code with collision-free placeholders before cleanup.
     * Unclosed fences are preserved to the end of the document. Comments are skipped as a whole
     * so example delimiters inside hidden comments cannot hide the following documentation.
     *
     * @param source Markdown with normalized line endings
     * @return protected text and the original examples needed to restore it
     */
    ProtectedCode protectCode(String source) {
        String prefix = "\uE000WJ_CODE_";
        while (source.contains(prefix)) prefix += "_";
        List<String> examples = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < source.length();) {
            int end = i;
            if (source.startsWith("<!--", i)) {
                int close = source.indexOf("-->", i + 4);
                end = close < 0 ? source.length() : close + 3;
                text.append(source, i, end);
                i = end;
                continue;
            }
            if (i == 0 || source.charAt(i - 1) == '\n') {
                int lineEnd = lineEnd(source, i);
                String line = source.substring(i, lineEnd);
                Matcher fence = FENCE.matcher(line);
                if (fence.matches() && (fence.group(1).charAt(0) != '`' || fence.group(2).contains("`") == false)) {
                    end = source.length();
                    for (int next = lineEnd + 1; next < source.length();) {
                        int nextEnd = lineEnd(source, next);
                        Matcher closing = FENCE.matcher(source.substring(next, nextEnd));
                        if (closing.matches() && closing.group(1).charAt(0) == fence.group(1).charAt(0)
                                && closing.group(1).length() >= fence.group(1).length() && closing.group(2).isBlank()) {
                            end = nextEnd;
                            break;
                        }
                        next = nextEnd + 1;
                    }
                } else if ((line.startsWith("    ") || line.startsWith("\t")) && line.stripLeading().matches("(?:[-+*] |[0-9]+[.)] ).*") == false) {
                    end = lineEnd;
                    for (int next = lineEnd + 1; next < source.length();) {
                        int nextEnd = lineEnd(source, next);
                        String nextLine = source.substring(next, nextEnd);
                        if (nextLine.startsWith("    ") || nextLine.startsWith("\t")) end = nextEnd;
                        else if (nextLine.isBlank() == false) break;
                        next = nextEnd + 1;
                    }
                }
            }
            if (end == i && source.charAt(i) == '`') {
                int length = runLength(source, i, '`');
                Matcher paragraph = PARAGRAPH_BREAK.matcher(source);
                int limit = paragraph.find(i + length) ? paragraph.start() : source.length();
                int close = i + length;
                while ((close = source.indexOf('`', close)) >= 0 && close < limit) {
                    int closingLength = runLength(source, close, '`');
                    if (closingLength == length) {
                        end = close + length;
                        break;
                    }
                    close += closingLength;
                }
            }
            if (end > i) {
                text.append(prefix).append(examples.size()).append(';');
                examples.add(source.substring(i, end));
                i = end;
            } else {
                char current = source.charAt(i++);
                text.append(current);
                if (current == '\\' && i < source.length()) text.append(source.charAt(i++));
            }
        }
        return new ProtectedCode(text.toString(), prefix, examples);
    }

    private static int lineEnd(String source, int start) {
        int end = source.indexOf('\n', start);
        return end < 0 ? source.length() : end;
    }

    private static int runLength(String source, int start, char marker) {
        int end = start;
        while (end < source.length() && source.charAt(end) == marker) end++;
        return end - start;
    }

    /**
     * Unwraps inline and defined reference links, recursively preserving nested image descriptions.
     * Unresolved references and incomplete syntax remain unchanged.
     *
     * @param text prose with protected code examples
     * @param references normalized labels of link definitions
     * @return prose without link destinations or image paths
     */
    private String cleanLinks(String text, Set<String> references) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length();) {
            char current = text.charAt(i);
            int open = current == '!' && i + 1 < text.length() && text.charAt(i + 1) == '[' ? i + 1 : i;
            int close = text.charAt(open) == '[' ? closingDelimiter(text, open, '[', ']') : -1;
            if (close >= 0) {
                String label = text.substring(open + 1, close);
                int end = -1;
                if (close + 1 < text.length() && text.charAt(close + 1) == '(') {
                    end = closingDelimiter(text, close + 1, '(', ')');
                } else if (close + 1 < text.length() && text.charAt(close + 1) == '[') {
                    int referenceEnd = closingDelimiter(text, close + 1, '[', ']');
                    if (referenceEnd >= 0) {
                        String reference = text.substring(close + 2, referenceEnd);
                        if (references.contains(normalizeReference(reference.isEmpty() ? label : reference))) end = referenceEnd;
                    }
                } else if (references.contains(normalizeReference(label))) end = close;
                if (end >= 0) {
                    result.append(cleanLinks(label, references));
                    i = end + 1;
                    continue;
                }
            }
            result.append(current);
            i++;
            if (current == '\\' && i < text.length()) result.append(text.charAt(i++));
        }
        return result.toString();
    }

    /** Finds a balanced delimiter, allowing escaped characters and quoted link titles. */
    private int closingDelimiter(String text, int start, char opening, char closing) {
        int depth = 1;
        for (int i = start + 1; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '\\') i++;
            else if (opening == '(' && (current == '<' || ((current == '\'' || current == '"') && Character.isWhitespace(text.charAt(i - 1))))) {
                char quote = current == '<' ? '>' : current;
                while (++i < text.length() && text.charAt(i) != quote) {
                    if (text.charAt(i) == '\\') i++;
                }
            } else if (current == opening) depth++;
            else if (current == closing && --depth == 0) return i;
        }
        return -1;
    }

    private String normalizeReference(String label) {
        return label.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * Removes HTML tags and hidden elements, using the existing HTML parser for attribute values.
     * Block and table separators remain readable; standalone URL autolinks retain their values.
     *
     * @param text prose with protected code examples
     * @return visible prose and existing image descriptions
     */
    private String cleanHtml(String text) {
        StringBuilder result = new StringBuilder();
        Matcher tags = HTML_TAG.matcher(text);
        int offset = 0;
        while (tags.find(offset)) {
            result.append(text, offset, tags.start());
            String name = tags.group(1).toLowerCase(Locale.ROOT);
            boolean closing = tags.group().startsWith("</");
            Element element = closing ? null : Jsoup.parseBodyFragment(tags.group()).getElementsByTag(name).first();
            offset = tags.end();
            if (closing == false && (HIDDEN_TAGS.contains(name) || (element != null
                    && (element.hasAttr("hidden") || "true".equalsIgnoreCase(element.attr("aria-hidden")))))) {
                Matcher end = Pattern.compile("(?i)</" + Pattern.quote(name) + "\\s*>").matcher(text);
                if (end.find(offset)) offset = end.end();
            } else if ("img".equals(name) && element != null) result.append(element.attr("alt"));
            else if (name.matches("h[1-6]")) result.append(closing ? "\n" : "\n" + "#".repeat(name.charAt(1) - '0') + " ");
            else if ("li".equals(name)) result.append(closing ? "\n" : "\n- ");
            else if ("th".equals(name) || "td".equals(name)) result.append(closing ? "" : " | ");
            else if (BLOCK_TAGS.contains(name)) result.append('\n');
        }
        result.append(text, offset, text.length());
        return result.toString().replaceAll("<([A-Za-z][A-Za-z0-9+.-]*:[^<>\\s]+|[^<>\\s]+@[^<>\\s]+)>", "$1");
    }

    record ProtectedCode(String text, String prefix, List<String> examples) {
        /** Identifies a protected block while allowing an inline code span at the start of a line. */
        boolean isBlock(String line) {
            if (line.startsWith(prefix) == false) return false;
            String original = restore(line);
            return original.startsWith("`") == false || original.contains("\n") || FENCE.matcher(original).matches();
        }

        String restore(String cleaned) {
            for (int i = 0; i < examples.size(); i++) cleaned = cleaned.replace(prefix + i + ';', examples.get(i));
            return cleaned;
        }
    }

    @Override
    public RagEntityType getEntityType() {
        return RagEntityType.MARKDOWN;
    }
}
