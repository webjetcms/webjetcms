package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.editor.EditorForm;

class EditorToolsForCoreTextTest {

    private MockedStatic<Constants> constants;

    @BeforeEach
    void configureLimit() {
        constants = mockStatic(Constants.class);
        constants.when(() -> Constants.getString(anyString())).thenReturn("");
        constants.when(() -> Constants.getInt("fulltextMaxHtmlLength")).thenReturn(2000000);
    }

    @AfterEach
    void restoreConfiguration() {
        constants.close();
    }

    @Test
    void preservesTextAfterLongAttributesBeyondTheOldLimit() {
        String html = "<div data-padding=\"" + "x".repeat(1500000) + "\">INDEX_TAIL</div>";

        String result = EditorToolsForCore.removeHtmlTagsKeepLength(html);

        assertEquals(html.length(), result.length());
        assertEquals("INDEX_TAIL", result.trim());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<div title=\"a < b\">INDEX_TAIL</div>",
        "<div title='a < b'>INDEX_TAIL</div>",
        "<div title=\"a > b\">INDEX_TAIL</div>",
        "<div title='a \"quoted\" < b > c'>INDEX_TAIL</div>",
        "<!-- a < b --><p>INDEX_TAIL</p>",
        "<!-- <script>ignoredComment > ' \" --><p>INDEX_TAIL</p>"
    })
    void preservesTextPositionsAfterQuotedAttributesAndComments(String prefix) {
        String html = prefix + "<p>OTHER_TEXT</p>";

        String result = EditorToolsForCore.removeHtmlTagsKeepLength(html);

        assertEquals(html.length(), result.length());
        assertEquals(html.indexOf("INDEX_TAIL"), result.indexOf("INDEX_TAIL"));
        assertEquals(html.indexOf("OTHER_TEXT"), result.indexOf("OTHER_TEXT"));
        assertEquals("INDEX_TAIL OTHER_TEXT", result.trim().replaceAll("\\s+", " "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"<div title=\"", "<!--"})
    void preservesInputLimitWhenAnAttributeOrCommentIsTruncated(String prefix) {
        constants.when(() -> Constants.getInt("fulltextMaxHtmlLength")).thenReturn(32);
        String html = "Visible" + prefix + "<".repeat(40);

        String result = EditorToolsForCore.removeHtmlTagsKeepLength(html);

        assertEquals(32, result.length());
        assertEquals("Visible", result.trim());
    }

    @Test
    void removesLongScriptAndStyleContentsWhilePreservingTextPositions() {
        String html = "<script data-padding=\"" + "x".repeat(120000) + "\">"
                + "ignoredScript".repeat(12000) + "</script>"
                + "<style>" + ".ignoredStyle{}".repeat(12000) + "</style><p>Visible text</p>";

        String result = EditorToolsForCore.removeHtmlTagsKeepLength(html);

        assertEquals(html.length(), result.length());
        assertEquals("Visible text", result.trim());
        assertEquals(html.indexOf("Visible text"), result.indexOf("Visible text"));
    }

    @ParameterizedTest
    @CsvSource({"31,false", "32,false", "33,true"})
    void enforcesConfiguredInputLimitAndWarnsOnlyWhenTruncating(int length, boolean truncated) {
        constants.when(() -> Constants.getInt("fulltextMaxHtmlLength")).thenReturn(32);
        String html = "x".repeat(length);

        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            String result = EditorToolsForCore.removeHtmlTagsKeepLength(html);

            assertEquals(html.substring(0, Math.min(length, 32)), result);
            if (truncated) {
                logger.verify(() -> Logger.warn(EditorToolsForCore.class,
                        "Truncating HTML input from {} to {} characters (fulltextMaxHtmlLength).", length, 32));
            } else {
                logger.verify(() -> Logger.warn(EditorToolsForCore.class,
                        "Truncating HTML input from {} to {} characters (fulltextMaxHtmlLength).", length, 32), never());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    void invalidLimitsRetainTheDefaultSizeBound(int configuredLimit) {
        constants.when(() -> Constants.getInt("fulltextMaxHtmlLength")).thenReturn(configuredLimit);
        String html = "x".repeat(2000000) + "TRUNCATED_TAIL";

        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            String result = EditorToolsForCore.removeHtmlTagsKeepLength(html);

            assertEquals(2000000, result.length());
            assertFalse(result.contains("TRUNCATED_TAIL"));
            logger.verify(() -> Logger.warn(EditorToolsForCore.class,
                    "Truncating HTML input from {} to {} characters (fulltextMaxHtmlLength).", html.length(), 2000000));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesLuceneBodyTextWithOrWithoutRenderedApps(boolean executeApps) {
        String html = "<!-- ignoredComment < b --><div title=\"a < b\" data-padding=\""
                + "x".repeat(120000) + "\">INDEX_TAIL</div><p>OTHER_TEXT</p>"
                + "<script>ignoredScript</script><style>.ignoredStyle{}</style>";
        EditorForm form = new EditorForm();
        form.setTitle("Page title");
        form.setData(html);
        MockHttpServletRequest request = new MockHttpServletRequest();
        constants.when(() -> Constants.getBoolean("fulltextExecuteApps")).thenReturn(executeApps);

        try (MockedStatic<DocDB> documents = mockStatic(DocDB.class);
                MockedStatic<EditorToolsForCore> editor = mockStatic(EditorToolsForCore.class, CALLS_REAL_METHODS)) {
            editor.when(() -> EditorToolsForCore.renderIncludes(any(DocDetails.class), eq(true), eq(request)))
                    .thenReturn(html);

            String dataAsc = EditorToolsForCore.getDataAsc(html, form, true, request);
            String plainText = SearchTools.htmlToPlain(dataAsc);

            assertTrue(plainText.contains("INDEX_TAIL"));
            assertTrue(plainText.contains("OTHER_TEXT"));
            assertTrue(plainText.contains("Page title"));
            assertFalse(plainText.contains("ignoredComment"));
            assertFalse(plainText.contains("ignoredScript"));
            assertFalse(plainText.contains("ignoredStyle"));
            if (executeApps) {
                editor.verify(() -> EditorToolsForCore.renderIncludes(any(DocDetails.class), eq(true), eq(request)));
            } else {
                editor.verify(() -> EditorToolsForCore.renderIncludes(any(DocDetails.class), eq(true), eq(request)), never());
            }
        }
    }
}
