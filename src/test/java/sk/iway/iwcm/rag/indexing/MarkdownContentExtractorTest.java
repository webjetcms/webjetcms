package sk.iway.iwcm.rag.indexing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.rag.indexing.SlidingWindowChunker.Chunk;

/** Covers Markdown cleanup, title extraction, and heading context for embeddings. */
class MarkdownContentExtractorTest {
    private final MarkdownContentExtractor extractor = new MarkdownContentExtractor();

    /** Hidden HTML stays excluded even when nested, unclosed, or inside a table. */
    @ParameterizedTest
    @ValueSource(strings = {
        "<script>private value", "<div hidden>private value",
        "<div hidden><div>hidden one</div>hidden two</div>",
        "<script>let example = '<script>';</script>",
        "<table><tr aria-hidden='true'><td>private value</td></tr></table>", "<input hidden>"
    })
    void removesHiddenHtml(String html) {
        assertEquals("", extractor.extractText(html));
    }

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

    /** Merged siblings use their common parent; overlapping passages retain their own section context. */
    @Test
    void includesTitleAndHeadingHierarchy() {
        String body = "Enable this option.";
        String text = "# Guide\n\n## Users\n\n### Approval\n\n~~~markdown\n## Example\n~~~\n\n" + body
            + "\n\n### Access\n\n" + body + "\n\nPages\n-----\n\n- Store\n---\n\n### Details\n\n" + body;
        String merged = text.substring(text.indexOf(body), text.indexOf("\n\nPages"));
        List<Chunk> chunks = List.of(new Chunk(text.indexOf(body), merged),
            new Chunk(text.indexOf(body, text.indexOf(body) + body.length()), body), new Chunk(text.lastIndexOf(body), body));

        assertEquals(List.of("Guide > Users\n\n" + merged, "Guide > Users > Access\n\n" + body, "Guide > Pages > Details\n\n" + body),
            extractor.addHeadingContext(text, "Guide", chunks));
    }
}
