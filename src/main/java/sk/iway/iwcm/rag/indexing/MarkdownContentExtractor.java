package sk.iway.iwcm.rag.indexing;

import org.springframework.stereotype.Component;

import sk.iway.iwcm.rag.service.RagEntityType;

/** Keeps Markdown content, including HTML comments, while removing YAML front matter. */
@Component
public class MarkdownContentExtractor implements ContentExtractor<String> {

    /**
     * Removes YAML front matter and byte-order marks, normalizes line endings, and trims Markdown.
     * HTML comments and other Markdown content are retained.
     *
     * @param markdown source Markdown, or {@code null}
     * @return normalized Markdown, or an empty string for null input
     */
    @Override
    public String extractText(String markdown) {
        if (markdown == null) return "";
        // Preserve all HTML comments: they can be part of documentation code examples.
        return markdown.replace("\uFEFF", "").replace("\r\n", "\n").replace('\r', '\n')
            .replaceFirst("\\A---[ \\t]*\\n[\\s\\S]*?\\n(?:---|\\.\\.\\.)[ \\t]*(?:\\n|$)", "").trim();
    }

    /**
     * Uses the first level-one heading outside comments and fenced code, falling back to the relative file path.
     *
     * @param markdown source Markdown, or {@code null}
     * @param sourcePath non-null relative path used when no heading is found
     * @return heading or fallback path, truncated to at most 512 characters
     */
    public String extractTitle(String markdown, String sourcePath) {
        String fence = null;
        for (String line : extractText(markdown).replaceAll("(?s)<!--.*?-->", "").split("\\n")) {
            String trimmed = line.stripLeading();
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                String marker = trimmed.substring(0, 3);
                if (fence == null) fence = marker;
                else if (fence.equals(marker)) fence = null;
                continue;
            }
            if (fence == null && trimmed.startsWith("# ")) {
                String title = trimmed.substring(2).replaceFirst("[ \\t]+#+[ \\t]*$", "").trim();
                if (title.isEmpty() == false) return title.substring(0, Math.min(512, title.length()));
            }
        }
        return sourcePath.substring(0, Math.min(512, sourcePath.length()));
    }

    @Override
    public RagEntityType getEntityType() {
        return RagEntityType.MARKDOWN;
    }
}
