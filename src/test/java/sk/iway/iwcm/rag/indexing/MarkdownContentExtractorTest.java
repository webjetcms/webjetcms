package sk.iway.iwcm.rag.indexing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests preprocessing before chunking and preservation of documentation structure and code examples. */
class MarkdownContentExtractorTest {
    private static final String FRONT_MATTER = "\uFEFF---\r\ntitle: hidden\r\n---\r\n# Search\r\n\r\n<!-- author note -->\r\n";
    private final MarkdownContentExtractor extractor = new MarkdownContentExtractor();

    /** Verifies each cleanup rule independently, including malformed syntax and whitespace normalization. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("cleanupCases")
    void cleansMarkdown(String description, String markdown, String expected) {
        assertEquals(expected, extractor.extractText(markdown));
    }

    private static Stream<Arguments> cleanupCases() {
        return Stream.of(
            Arguments.of("null input", null, ""),
            Arguments.of("front matter, BOM, comments, and CRLF", FRONT_MATTER, "# Search"),
            Arguments.of("noise-only document", "![](image.png)\n\n<!-- hidden -->\n\n[link]: other.md", ""),
            Arguments.of("alternative front matter terminator", "---\ntitle: Hidden\n...\n\n", ""),
            Arguments.of("blank lines after removing noise", "First\n\n![](image.png)\n\n\n<!-- hidden -->\n\nLast", "First\n\nLast"),
            Arguments.of("formatted heading label", "# [User **guide**](../guide.md)", "# User **guide**"),
            Arguments.of("parentheses and title in link", "Read [settings](../settings_(new).md \"Settings\").", "Read settings."),
            Arguments.of("image description", "![Permission settings](../images/settings.png)", "Permission settings"),
            Arguments.of("linked image", "[![Diagram](diagram.svg)](../diagram.md)", "Diagram"),
            Arguments.of("reference links", "[permissions][access], [reference][] and [shortcut].\n\n"
                + "[access]: ../permissions.md \"Access\"\n[reference]: ../reference.md\n[shortcut]: ../shortcut.md",
                "permissions, reference and shortcut."),
            Arguments.of("multiline label", "[two\nlines](../multiline.md)", "two\nlines"),
            Arguments.of("escaped brackets", "[escaped \\[label\\]](../escaped.md)", "escaped \\[label\\]"),
            Arguments.of("documented URLs, paths, and incomplete links",
                "Endpoint: <https://example.com/api>. Path: /admin/users.\nhttps://example.com/help [unfinished](../page.md",
                "Endpoint: https://example.com/api. Path: /admin/users.\nhttps://example.com/help [unfinished](../page.md"),
            Arguments.of("indented code with trailing newline", "    [Example](file.md)\n", "    [Example](file.md)"),
            Arguments.of("unmatched inline delimiter", "An unmatched `delimiter.\n\nRead [guide](guide.md) and `value`.",
                "An unmatched `delimiter.\n\nRead guide and `value`."),
            Arguments.of("nested list link and unresolved reference", "- Parent\n    - [Child](child.md \"A ) title\")\n\n[Unknown][missing]",
                "- Parent\n    - Child\n\n[Unknown][missing]")
        );
    }

    /** Finds visible level-one headings and uses the source path when all headings are inside code. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("titleCases")
    void extractsTitle(String description, String markdown, String expected) {
        assertEquals(expected, extractor.extractTitle(markdown, "en/guide.md"));
    }

    private static Stream<Arguments> titleCases() {
        return Stream.of(
            Arguments.of("front matter and comments", FRONT_MATTER, "Search"),
            Arguments.of("formatted link in heading", "# [User **guide**](../guide.md)", "User guide"),
            Arguments.of("fenced code and closing heading markers", "```sh\n# example\n```\n# Actual title ##", "Actual title"),
            Arguments.of("comment heading", "<!--\n# Hidden heading\n-->\n# Actual title", "Actual title"),
            Arguments.of("indented code heading", "    # Example\n\n# Actual title", "Actual title"),
            Arguments.of("nested fences", "````\n```\n# Example\n````\n# Actual title", "Actual title"),
            Arguments.of("setext heading", "Setext title\n============", "Setext title"),
            Arguments.of("source path fallback", "```sh\n# example\n```", "en/guide.md")
        );
    }

    /** Preserves each code syntax exactly, including comments, indentation, and internal blank lines. */
    @ParameterizedTest(name = "code example {index}")
    @ValueSource(strings = {
        "````markdown\n```html\n<!-- Keep this comment -->\n<img src=\"image.png\">\n```\n\n\n[Link](file.md)\n````",
        "~~~markdown\n![Example](image.png)\n~~~",
        "    <!-- Keep indented code -->\n\n\n    [Link](file.md)",
        "Example: ``[link](file.md)\ntext <!-- inline code -->``.",
        "> ```markdown\n> [Example](file.md)\n> ```",
        "```markdown\n[Example](file.md)\n\n\n"
    })
    void preservesCodeExamples(String markdown) {
        assertEquals(markdown, extractor.extractText(markdown));
    }

    /** Leaves headings, nested lists, table alignment, and inline code intact while cleaning a table link. */
    @Test
    void preservesDocumentStructure() {
        String markdown = """
            ## Settings

            - **Enabled**: yes
              - Optional value
            1. Open `config.properties`.

            | Key | Description |
            | :-- | --: |
            | `url` | [Endpoint](endpoint.md) |

            > See `![example](image.png)` and `<!-- example -->`.
            """;

        assertEquals(markdown.replace("[Endpoint](endpoint.md)", "Endpoint").stripTrailing(), extractor.extractText(markdown));
    }

    /** Removes HTML resource attributes and hidden content while keeping descriptions and cell boundaries. */
    @Test
    void cleansHtmlOutsideCode() {
        String markdown = """
            # HTML

            Read <a href="../guide.md">the guide</a> and <img src="diagram.png" alt="Flow diagram">.

            <div>
            <p>Visible paragraph</p>
            <img src="screen.png" alt="Settings screen">
            <!-- internal note -->
            <script>secretScript()</script>
            <style>.private { display: none; }</style>
            <nav>Repeated navigation</nav>
            <span hidden>Hidden text</span>
            <table><tr><th>Key</th><th>Value</th></tr><tr><td>Mode</td><td>Auto</td></tr></table>
            </div>
            """;

        assertEquals("""
            # HTML

            Read the guide and Flow diagram.

            Visible paragraph

            Settings screen

             | Key | Value

             | Mode | Auto""", extractor.extractText(markdown));
    }
}
