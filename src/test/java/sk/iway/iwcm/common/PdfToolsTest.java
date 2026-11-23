package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies that stylesheet filtering never turns CSS into visible PDF text. */
class PdfToolsTest extends BaseWebjetTest {

    /** Removes complete excluded style blocks, including multiline CSS and BOM characters. */
    @Test
    void excludesStylesWithoutPrintingTheirContents() throws IOException {
        String html = "<html><head><style type='text/css'>\uFEFF\n"
            + ".first { color: red; }\n</style>"
            + "<STYLE media=\"screen\">\n.second { color: blue; }\n</STYLE>"
            + "</head><body><p>Submitted answer</p></body></html>";

        String text = renderText(html, new MockHttpServletRequest());

        assertTrue(text.contains("Submitted answer"));
        assertFalse(text.contains(".first"));
        assertFalse(text.contains(".second"));
        assertFalse(text.contains("\uFEFF"));
    }

    /** Keeps print and all-media styles active during PDF rendering. */
    @ParameterizedTest
    @ValueSource(strings = {"print", "all"})
    void appliesPrintStyles(String media) throws IOException {
        String html = "<html><head><style media=\"" + media + "\">"
            + ".hidden { display: none; }</style></head><body>"
            + "<p>Submitted answer</p><p class='hidden'>Hidden content</p></body></html>";

        String text = renderText(html, new MockHttpServletRequest());

        assertTrue(text.contains("Submitted answer"));
        assertFalse(text.contains("Hidden content"));
        assertFalse(text.contains(".hidden"));
    }

    /** Preserves screen styles when the caller explicitly requests screen rendering. */
    @Test
    void appliesStylesInScreenMode() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("screen", "true");
        String html = "<html><head><style>.hidden { display: none; }</style></head>"
            + "<body><p>Submitted answer</p><p class='hidden'>Hidden content</p></body></html>";

        String text = renderText(html, request);

        assertTrue(text.contains("Submitted answer"));
        assertFalse(text.contains("Hidden content"));
    }

    private String renderText(String html, MockHttpServletRequest request) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfTools.renderHtmlCode(html, output, request, null, false);
        try (PDDocument document = Loader.loadPDF(output.toByteArray())) {
            return new PDFTextStripper().getText(document);
        }
    }
}
