package sk.iway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Verifies text extraction behavior across HTML parser upgrades. */
class Html2TextTest {

    /** Preserves Unicode and entities while excluding executable element contents. */
    @Test
    void extractsVisibleTextAndDecodesEntities() {
        String html = "<style>.hidden { display: none }</style><h1>Žltý &amp; modrý</h1>"
            + "<p>Prvý&nbsp;<strong>odsek</strong>.</p><script>alert('ignored')</script>";

        assertEquals("Žltý & modrý Prvý odsek.", Html2Text.html2text(html));
        assertEquals("Žltý & modrý Prvý odsek.", Html2Text.html2text(html, false));
    }

    /** Keeps the utility's null and plain-text shortcuts unchanged. */
    @Test
    void preservesPlainTextAndHandlesNull() {
        assertEquals("", Html2Text.html2text(null));
        assertEquals("Plain &amp; text", Html2Text.html2text("Plain &amp; text"));
        assertEquals("", Html2Text.html2text(null, false));
        assertEquals("", Html2Text.html2text("", false));
        assertEquals("Plain &amp; text", Html2Text.html2text("Plain &amp; text", false));
    }

    /** Forced parsing decodes entities without tags in one pass and handles empty input. */
    @Test
    void forcesParsingWithoutHtmlTags() {
        assertEquals("John's R&D", Html2Text.html2text("John&#39;s R&amp;D", true));
        assertEquals("Žltá modrá", Html2Text.html2text("Žltá&nbsp;  modrá", true));
        assertEquals("<strong>Text</strong> &amp;", Html2Text.html2text("&lt;strong&gt;Text&lt;/strong&gt; &amp;amp;", true));
        assertEquals("", Html2Text.html2text(null, true));
        assertEquals("", Html2Text.html2text("", true));
    }

    /** Extracts heading text in document order, including nested inline elements. */
    @Test
    void extractsSelectedElements() {
        Html2Text document = new Html2Text("<h1>First <em>heading</em></h1>"
            + "<p>Body</p><h2>Second &amp; last</h2>");

        assertEquals(List.of("First heading", "Second & last"), document.getTextByElement("h1,h2"));
    }
}
