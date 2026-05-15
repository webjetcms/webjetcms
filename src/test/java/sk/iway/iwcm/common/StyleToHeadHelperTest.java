package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.doc.showdoc.StyleToHeadHelper;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Tests for StyleToHeadHelper class.
 * Verifies extraction, deduplication, and collection of style tags from HTML.
 */
class StyleToHeadHelperTest extends BaseWebjetTest {

    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();
        // Enable the feature for testing
        Constants.setBoolean("moveStyleToHead", true);
    }

    @Test
    @DisplayName("Extract single style tag from HTML")
    void testExtractSingleStyleTag() {
        StringBuilder input = new StringBuilder(
            "<div class=\"video\"><style>.video { color: red; }</style><p>Content</p></div>"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        // Style tag should be removed from output
        assertEquals("<div class=\"video\"><p>Content</p></div>", result.toString());

        // Style should be collected
        assertTrue(StyleToHeadHelper.hasCollectedStyles(request));

        // Verify collected styles
        String collected = StyleToHeadHelper.getCollectedStyles(request);
        assertTrue(collected.contains("<style>.video { color: red; }</style>"));
    }

    @Test
    @DisplayName("Extract multiple style tags from HTML")
    void testExtractMultipleStyleTags() {
        StringBuilder input = new StringBuilder(
            "<style>.class1 { color: red; }</style>" +
            "<div>Content</div>" +
            "<style>.class2 { color: blue; }</style>"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        // Style tags should be removed
        assertEquals("<div>Content</div>", result.toString());

        // Both styles should be collected
        String collected = StyleToHeadHelper.getCollectedStyles(request);
        assertTrue(collected.contains("<style>.class1 { color: red; }</style>"));
        assertTrue(collected.contains("<style>.class2 { color: blue; }</style>"));
    }

    @Test
    @DisplayName("Deduplicate identical style tags")
    void testDeduplicateIdenticalStyles() {
        // First call
        StringBuilder input1 = new StringBuilder(
            "<style>.video { width: 100%; }</style><div>Video 1</div>"
        );
        StyleToHeadHelper.extractAndCollectStyles(input1, request);

        // Second call with same style
        StringBuilder input2 = new StringBuilder(
            "<style>.video { width: 100%; }</style><div>Video 2</div>"
        );
        StyleToHeadHelper.extractAndCollectStyles(input2, request);

        // Style should appear only once
        String collected = StyleToHeadHelper.getCollectedStyles(request);
        int count = countOccurrences(collected, "<style>.video { width: 100%; }</style>");
        assertEquals(1, count, "Duplicate style should be deduplicated");
    }

    @Test
    @DisplayName("Preserve order of first occurrence")
    void testPreserveOrderOfStyleTags() {
        StringBuilder input1 = new StringBuilder("<style>.first { color: red; }</style>");
        StyleToHeadHelper.extractAndCollectStyles(input1, request);

        StringBuilder input2 = new StringBuilder("<style>.second { color: blue; }</style>");
        StyleToHeadHelper.extractAndCollectStyles(input2, request);

        StringBuilder input3 = new StringBuilder("<style>.third { color: green; }</style>");
        StyleToHeadHelper.extractAndCollectStyles(input3, request);

        String collected = StyleToHeadHelper.getCollectedStyles(request);

        int firstIndex = collected.indexOf(".first");
        int secondIndex = collected.indexOf(".second");
        int thirdIndex = collected.indexOf(".third");

        assertTrue(firstIndex < secondIndex, "First style should appear before second");
        assertTrue(secondIndex < thirdIndex, "Second style should appear before third");
    }

    @Test
    @DisplayName("Handle empty input")
    void testEmptyInput() {
        StringBuilder input = new StringBuilder();
        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        assertEquals("", result.toString());
        assertFalse(StyleToHeadHelper.hasCollectedStyles(request));
    }

    @Test
    @DisplayName("Handle null input")
    void testNullInput() {
        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(null, request);
        assertNull(result);
        assertFalse(StyleToHeadHelper.hasCollectedStyles(request));
    }

    @Test
    @DisplayName("Handle input without style tags")
    void testInputWithoutStyleTags() {
        StringBuilder input = new StringBuilder("<div><p>No styles here</p></div>");
        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        assertEquals("<div><p>No styles here</p></div>", result.toString());
        assertFalse(StyleToHeadHelper.hasCollectedStyles(request));
    }

    @Test
    @DisplayName("Handle style tag with attributes")
    void testStyleTagWithAttributes() {
        StringBuilder input = new StringBuilder(
            "<style type=\"text/css\" media=\"print\">.print-only { display: block; }</style><div>Content</div>"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        assertEquals("<div>Content</div>", result.toString());

        String collected = StyleToHeadHelper.getCollectedStyles(request);
        assertTrue(collected.contains("<style type=\"text/css\" media=\"print\">.print-only { display: block; }</style>"));
    }

    @Test
    @DisplayName("Handle multiline CSS content")
    void testMultilineCssContent() {
        StringBuilder input = new StringBuilder(
            "<style>\n" +
            ".class1 {\n" +
            "    color: red;\n" +
            "    background: blue;\n" +
            "}\n" +
            ".class2 {\n" +
            "    margin: 10px;\n" +
            "}\n" +
            "</style><div>Content</div>"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        assertEquals("<div>Content</div>", result.toString());
        assertTrue(StyleToHeadHelper.hasCollectedStyles(request));

        String collected = StyleToHeadHelper.getCollectedStyles(request);
        assertTrue(collected.contains(".class1"));
        assertTrue(collected.contains(".class2"));
    }

    @Test
    @DisplayName("Handle case insensitive style tags")
    void testCaseInsensitiveStyleTags() {
        StringBuilder input = new StringBuilder(
            "<STYLE>.upper { color: red; }</STYLE>" +
            "<Style>.mixed { color: blue; }</Style>"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        assertEquals("", result.toString());

        String collected = StyleToHeadHelper.getCollectedStyles(request);
        assertTrue(collected.contains(".upper"));
        assertTrue(collected.contains(".mixed"));
    }

    @Test
    @DisplayName("Feature disabled - return unchanged content")
    void testFeatureDisabled() {
        Constants.setBoolean("moveStyleToHead", false);

        StringBuilder input = new StringBuilder(
            "<style>.video { color: red; }</style><div>Content</div>"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        // Style should NOT be removed when feature is disabled
        assertEquals("<style>.video { color: red; }</style><div>Content</div>", result.toString());
        assertFalse(StyleToHeadHelper.hasCollectedStyles(request));
    }

    @Test
    @DisplayName("Clear collected styles")
    void testClearCollectedStyles() {
        StringBuilder input = new StringBuilder("<style>.test { }</style>");
        StyleToHeadHelper.extractAndCollectStyles(input, request);

        assertTrue(StyleToHeadHelper.hasCollectedStyles(request));

        StyleToHeadHelper.clearCollectedStyles(request);

        assertFalse(StyleToHeadHelper.hasCollectedStyles(request));
        assertEquals("", StyleToHeadHelper.getCollectedStyles(request));
    }

    @Test
    @DisplayName("Get collected styles returns empty string when none collected")
    void testGetCollectedStylesWhenEmpty() {
        String collected = StyleToHeadHelper.getCollectedStyles(request);
        assertEquals("", collected);
    }

    @Test
    @DisplayName("Collected styles include HTML comment header")
    void testCollectedStylesHaveComment() {
        StringBuilder input = new StringBuilder("<style>.test { }</style>");
        StyleToHeadHelper.extractAndCollectStyles(input, request);

        String collected = StyleToHeadHelper.getCollectedStyles(request);
        assertTrue(collected.contains("<!-- WJ:"), "Collected styles should include identifying comment");
    }

    /**
     * Helper method to count occurrences of a substring
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
