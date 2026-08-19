package sk.iway.iwcm.components.apphtmlembed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class HtmlEmbedAppTest {

    @Test
    void getPreviewHtmlCodeEscapesScriptOnlyEmbed() {
        String htmlCode = "<script>" +
            "const value = \"</pre><img src=x onerror=alert(1)>\";" +
            "const condition = value && value.includes('&');" +
            "const writeMacro = '!WRITE(secret)!';" +
            "const callMacro = '!CALL(java.lang.Runtime.getRuntime())!';" +
            "const includeMacro = '!INCLUDE(/components/example.jsp)!';" +
            "</script>";

        String previewHtml = HtmlEmbedApp.getPreviewHtmlCode(htmlCode, true);

        assertTrue(previewHtml.startsWith("<pre class=\"html-embed-preview-code\""));
        assertTrue(previewHtml.contains("&lt;script&gt;"));
        assertTrue(previewHtml.contains("&lt;/pre&gt;&lt;img src=x onerror=alert(1)&gt;"));
        assertTrue(previewHtml.contains("value &amp;&amp; value.includes(&#39;&amp;&#39;)"));
        assertTrue(previewHtml.contains("&#33;WRITE(secret)&#33;"));
        assertTrue(previewHtml.contains("&#33;CALL(java.lang.Runtime.getRuntime())&#33;"));
        assertTrue(previewHtml.contains("&#33;INCLUDE(/components/example.jsp)&#33;"));
        assertFalse(previewHtml.contains("<script>"));
        assertFalse(previewHtml.contains("<img"));
        assertFalse(previewHtml.contains("!WRITE("));
        assertFalse(previewHtml.contains("!CALL("));
        assertFalse(previewHtml.contains("!INCLUDE("));

        Element previewCode = Jsoup.parseBodyFragment(previewHtml)
            .selectFirst("pre.html-embed-preview-code");
        assertNotNull(previewCode);
        assertEquals(htmlCode, previewCode.wholeText());
        assertTrue(previewCode.children().isEmpty());
    }

    @Test
    void getPreviewHtmlCodeSupportsMultipleScriptsCommentsAndWhitespace() {
        String htmlCode = " \n<!-- analytics -->\n" +
            "<SCRIPT src=\"/analytics.js\"></SCRIPT>\n" +
            "<script>console.log('loaded');</script>\n";

        assertTrue(HtmlEmbedApp.isScriptOnly(htmlCode));
        assertTrue(
            HtmlEmbedApp.getPreviewHtmlCode(htmlCode, true)
                .contains("&lt;SCRIPT src=&quot;/analytics.js&quot;&gt;")
        );
    }

    @Test
    void getPreviewHtmlCodeKeepsEmbedWithVisibleHtml() {
        String htmlCode = "<div>Visible content</div><script>console.log('loaded');</script>";

        assertFalse(HtmlEmbedApp.isScriptOnly(htmlCode));
        assertEquals(htmlCode, HtmlEmbedApp.getPreviewHtmlCode(htmlCode, true));
        assertFalse(HtmlEmbedApp.isScriptOnly(
            "<img src=\"/images/example.png\"><script>console.log('loaded');</script>"
        ));
        assertFalse(HtmlEmbedApp.isScriptOnly(
            "<iframe src=\"/example\"></iframe><script>console.log('loaded');</script>"
        ));
        assertFalse(HtmlEmbedApp.isScriptOnly("<div><script>console.log('loaded');</script></div>"));
        assertFalse(HtmlEmbedApp.isScriptOnly(
            "<div id=\"target\"></div>" +
            "<script>document.getElementById('target').textContent = 'Loaded';</script>"
        ));
    }

    @Test
    void getPreviewHtmlCodeKeepsScriptOutsidePreview() {
        String htmlCode = "<script>console.log('loaded');</script>";

        assertEquals(htmlCode, HtmlEmbedApp.getPreviewHtmlCode(htmlCode, false));
    }

    @Test
    void isScriptOnlyRejectsEmptyContent() {
        assertFalse(HtmlEmbedApp.isScriptOnly(null));
        assertFalse(HtmlEmbedApp.isScriptOnly(""));
        assertFalse(HtmlEmbedApp.isScriptOnly(" \n\t"));
    }
}
