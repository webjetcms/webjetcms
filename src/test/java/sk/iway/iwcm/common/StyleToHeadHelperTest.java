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
    @DisplayName("Extract single link stylesheet from HTML")
    void testExtractSingleLinkStylesheet() {
        StringBuilder input = new StringBuilder(
            "<div class=\"video\"><link href=\"/components/video/videojs/video-js.css\" rel=\"stylesheet\"><p>Content</p></div>"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        // Link tag should be removed from output
        assertEquals("<div class=\"video\"><p>Content</p></div>", result.toString());

        // Link should be collected
        assertTrue(StyleToHeadHelper.hasCollectedStyles(request));

        // Verify collected styles
        String collected = StyleToHeadHelper.getCollectedStyles(request);
        assertTrue(collected.contains("<link href=\"/components/video/videojs/video-js.css\" rel=\"stylesheet\">"));
    }

    @Test
    @DisplayName("Extract self-closing link stylesheet")
    void testExtractSelfClosingLinkStylesheet() {
        StringBuilder input = new StringBuilder(
            "<div><link href=\"/css/style.css\" rel=\"stylesheet\" /><p>Content</p></div>"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        assertEquals("<div><p>Content</p></div>", result.toString());

        String collected = StyleToHeadHelper.getCollectedStyles(request);
        assertTrue(collected.contains("<link href=\"/css/style.css\" rel=\"stylesheet\" />"));
    }

    @Test
    @DisplayName("Extract link with single quotes around stylesheet")
    void testExtractLinkWithSingleQuotes() {
        StringBuilder input = new StringBuilder(
            "<div><link href=\"/css/style.css' rel='stylesheet'><p>Content</p></div>"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        assertEquals("<div><p>Content</p></div>", result.toString());

        String collected = StyleToHeadHelper.getCollectedStyles(request);
        assertTrue(collected.contains("rel='stylesheet'"));
    }

    @Test
    @DisplayName("Do NOT extract link without rel=stylesheet (e.g. favicon)")
    void testDoNotExtractNonStylesheetLink() {
        StringBuilder input = new StringBuilder(
            "<div><link rel=\"icon\" href=\"/favicon.ico\" /><p>Content</p></div>"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        // Link should remain in output (not a stylesheet)
        assertEquals("<div><link rel=\"icon\" href=\"/favicon.ico\" /><p>Content</p></div>", result.toString());
        assertFalse(StyleToHeadHelper.hasCollectedStyles(request));
    }

    @Test
    @DisplayName("Extract mixed style and link elements preserving order")
    void testMixedStyleAndLinkPreservesOrder() {
        StringBuilder input = new StringBuilder(
            "<link href=\"/css/base.css\" rel=\"stylesheet\">" +
            "<style>.inline1 { color: red; }</style>" +
            "<div>Content</div>" +
            "<link href=\"/css/component.css\" rel=\"stylesheet\">" +
            "<style>.inline2 { color: blue; }</style>"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        // All style and link tags should be removed
        assertEquals("<div>Content</div>", result.toString());

        String collected = StyleToHeadHelper.getCollectedStyles(request);

        // Verify order: base.css -> inline1 -> component.css -> inline2
        int baseIndex = collected.indexOf("/css/base.css");
        int inline1Index = collected.indexOf(".inline1");
        int componentIndex = collected.indexOf("/css/component.css");
        int inline2Index = collected.indexOf(".inline2");

        assertTrue(baseIndex < inline1Index, "base.css should appear before inline1");
        assertTrue(inline1Index < componentIndex, "inline1 should appear before component.css");
        assertTrue(componentIndex < inline2Index, "component.css should appear before inline2");
    }

    @Test
    @DisplayName("Extract link and style within same component output")
    void testLinkAndStyleInSameComponent() {
        StringBuilder input = new StringBuilder(
            "<div class=\"video\">" +
            "<link href=\"/components/video/videojs/video-js.css\" rel=\"stylesheet\">" +
            "<style>.video { width: 100%; }</style>" +
            "<video class=\"video-js\"></video>" +
            "</div>"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        assertEquals("<div class=\"video\"><video class=\"video-js\"></video></div>", result.toString());

        String collected = StyleToHeadHelper.getCollectedStyles(request);

        // Link should come before inline style (preserving document order)
        int linkIndex = collected.indexOf("video-js.css");
        int styleIndex = collected.indexOf(".video { width: 100%; }");

        assertTrue(linkIndex < styleIndex, "Link stylesheet should appear before inline style");
    }

    @Test
    @DisplayName("Deduplicate identical link stylesheets")
    void testDeduplicateIdenticalLinkStylesheets() {
        // First call
        StringBuilder input1 = new StringBuilder(
            "<link href=\"/css/shared.css\" rel=\"stylesheet\"><div>Component 1</div>"
        );
        StyleToHeadHelper.extractAndCollectStyles(input1, request);

        // Second call with same link
        StringBuilder input2 = new StringBuilder(
            "<link href=\"/css/shared.css\" rel=\"stylesheet\"><div>Component 2</div>"
        );
        StyleToHeadHelper.extractAndCollectStyles(input2, request);

        // Link should appear only once
        String collected = StyleToHeadHelper.getCollectedStyles(request);
        int count = countOccurrences(collected, "<link href=\"/css/shared.css\" rel=\"stylesheet\">");
        assertEquals(1, count, "Duplicate link stylesheet should be deduplicated");
    }

    @Test
    @DisplayName("Complex real-world scenario with video component")
    void testRealWorldVideoComponentScenario() {
        // Simulates multiple video components each with their own link and style
        StringBuilder input1 = new StringBuilder(
            "<div class=\"video-wrapper\">" +
            "<link href=\"/components/video/videojs/video-js.css\" rel=\"stylesheet\">" +
            "<style>.video-1 { aspect-ratio: 16/9; }</style>" +
            "<video id=\"video-1\" class=\"video-js\"></video>" +
            "</div>"
        );
        StringBuilder result1 = StyleToHeadHelper.extractAndCollectStyles(input1, request);

        StringBuilder input2 = new StringBuilder(
             "<div class=\"video-wrapper\">" +
             "<link href=\"/components/video/videojs/video-js.css\" rel=\"stylesheet\">" +
             "<style>.video-2 { aspect-ratio: 4/3; }</style>" +
             "<video id=\"video-2\" class=\"video-js\"></video>" +
             "</div>"
         );
        StringBuilder result2 = StyleToHeadHelper.extractAndCollectStyles(input2, request);

         // Both video wrappers should have link and style removed
        assertEquals("<div class=\"video-wrapper\"><video id=\"video-1\" class=\"video-js\"></video></div>", result1.toString());
        assertEquals("<div class=\"video-wrapper\"><video id=\"video-2\" class=\"video-js\"></video></div>", result2.toString());

        String collected = StyleToHeadHelper.getCollectedStyles(request);

          // Link stylesheet should appear only once (deduplicated)
        assertEquals(1, countOccurrences(collected, "video-js.css"), "video-js.css link should be deduplicated");

          // Both inline styles should be present
        assertTrue(collected.contains(".video-1 { aspect-ratio: 16/9; }"));
        assertTrue(collected.contains(".video-2 { aspect-ratio: 4/3; }"));

          // Link should come before both inline styles
        int linkIndex = collected.indexOf("video-js.css");
        int video1Index = collected.indexOf(".video-1");
        int video2Index = collected.indexOf(".video-2");
        assertTrue(linkIndex < video1Index, "Link should appear before video-1 style");
        assertTrue(video1Index < video2Index, "video-1 style should appear before video-2 style");
     }
    @Test
    @DisplayName("Handle case insensitive link rel=stylesheet")
    void testCaseInsensitiveLinkStylesheet() {
        StringBuilder input = new StringBuilder(
            "<LINK HREF=\"/css/upper.css\" REL=\"stylesheet\">" +
            "<Link Href=\"/css/mixed.css\" Rel=\"Stylesheet\" />"
        );

        StringBuilder result = StyleToHeadHelper.extractAndCollectStyles(input, request);

        assertEquals("", result.toString());

        String collected = StyleToHeadHelper.getCollectedStyles(request);
        assertTrue(collected.contains("upper.css"));
        assertTrue(collected.contains("mixed.css"));
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
