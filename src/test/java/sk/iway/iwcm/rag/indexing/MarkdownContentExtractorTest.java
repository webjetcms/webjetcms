package sk.iway.iwcm.rag.indexing;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Tests Markdown normalization, front-matter removal, and title extraction outside comments and code.
 */
class MarkdownContentExtractorTest {
    private final MarkdownContentExtractor extractor = new MarkdownContentExtractor();

    /**
     * Verifies that front matter and the byte-order mark are removed, line endings are normalized, and HTML comments remain.
     */
    @Test
    void normalizesLineEndingsAndRemovesFrontMatterWhilePreservingComments() {
        String markdown = "\uFEFF---\r\ntitle: hidden\r\n---\r\n# Search\r\n\r\n<!-- author note -->\r\n";

        assertEquals("# Search\n\n<!-- author note -->", extractor.extractText(markdown));
        assertEquals("Search", extractor.extractTitle(markdown, "en/search.md"));
    }

    /**
     * Verifies that code and comment headings are ignored, missing titles use the source path, and null content becomes empty text.
     */
    @Test
    void ignoresHeadingsInsideCodeAndCommentsAndFallsBackToThePath() {
        assertEquals("Actual title", extractor.extractTitle("```sh\n# example\n```\n# Actual title ##", "en/a.md"));
        assertEquals("Actual title", extractor.extractTitle("<!--\n# Hidden heading\n-->\n# Actual title", "en/a.md"));
        assertEquals("en/a.md", extractor.extractTitle("```sh\n# example\n```", "en/a.md"));
        assertEquals("", extractor.extractText(null));
    }
}
