package sk.iway.iwcm.rag.indexing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.rag.indexing.SlidingWindowChunker.Chunk;

/** Covers Markdown cleanup, title extraction, and heading context for embeddings. */
class MarkdownContentExtractorTest {
    private final MarkdownContentExtractor extractor = new MarkdownContentExtractor();

    /** Removes indexing noise while preserving visible structure and literal code examples. */
    @Test
    void cleansMarkdownAndPreservesCode() {
        String code = "~~~markdown\n# Example\n[Link](file.md)\n<!-- Keep this comment -->\n~~~";
        String markdown = """
            ---
            title: Hidden metadata
            ---
            # Guide

            Read [settings](settings.md) and [help][reference].

            ![Diagram](diagram.png)

            - **Enabled**: yes

            | Key | Value |
            | --- | --- |
            | Mode | Auto |

            <!-- Internal note -->
            <script>hidden()</script>
            [reference]: help.md

            """ + code;

        assertEquals("# Guide\n\nRead settings and help.\n\nDiagram\n\n- **Enabled**: yes\n\n"
            + "| Key | Value |\n| --- | --- |\n| Mode | Auto |\n\n" + code, extractor.extractText(markdown));
    }

    /** Uses a visible ATX or Setext title, falling back to the path when only code contains headings. */
    @Test
    void extractsTitleOrUsesPath() {
        String code = "~~~markdown\n# Example\n~~~";
        assertEquals("User guide", extractor.extractTitle(code + "\n# [User **guide**](guide.md)", "en/guide.md"));
        assertEquals("Guide", extractor.extractTitle("Guide\n=====", "en/guide.md"));
        assertEquals("en/guide.md", extractor.extractTitle(code, "en/guide.md"));
    }

    /** Repeated passages inherit their own section; code headings and list separators do not become ancestors. */
    @Test
    void includesTitleAndHeadingHierarchy() {
        String body = "Enable this option.";
        String text = "# Guide\n\n## Users\n\n### Approval\n\n~~~markdown\n## Example\n~~~\n\n" + body
            + "\n\nPages\n-----\n\n- Store\n---\n\n### Details\n\n" + body;
        List<Chunk> chunks = List.of(new Chunk(text.indexOf(body), body), new Chunk(text.lastIndexOf(body), body));

        assertEquals(List.of("Guide > Users > Approval\n\n" + body, "Guide > Pages > Details\n\n" + body),
            extractor.addHeadingContext(text, "Guide", chunks));
    }
}
