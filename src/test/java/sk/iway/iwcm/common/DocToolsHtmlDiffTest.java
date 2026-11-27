package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

/**
 * Tests how {@link DocTools#getHtmlDiff(String, String)} renders HTML structure changes.
 */
class DocToolsHtmlDiffTest {

    /**
     * Verifies that replacing a paragraph with a div marks its unchanged text as changed.
     */
    @Test
    void marksTextWhenItsEnclosingTagChanges() throws Exception {
        String diff = DocTools.getHtmlDiff("<div>text</div>", "<p>text</p>");

        assertEquals("text", Jsoup.parse(diff).select("span.diff-html-changed").text());
    }
}
