package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.io.IwcmFile;

/** Verifies SVG sanitization and preservation across XML parser upgrades. */
class ImageToolsSvgTest {

    /** Removes executable content while retaining SVG casing, namespaces, and safe references. */
    @Test
    void sanitizesSvgWithoutDamagingDrawingAttributes() {
        String path = "/test/image.svg";
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" "
            + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 10 10\" onload=\"bad()\">"
            + "<script>bad()</script><defs><path id=\"shape\" d=\"M0 0L10 10\"/></defs>"
            + "<use id=\"safe\" xlink:href=\"#shape\"/>"
            + "<a id=\"unsafe\" xlink:href=\"javascript:bad()\" onclick=\"bad()\"><text>Label</text></a>"
            + "</svg>";
        IwcmFile file = mock(IwcmFile.class);
        when(file.getAbsolutePath()).thenReturn(path);
        AtomicReference<String> saved = new AtomicReference<>();

        try (MockedStatic<FileTools> files = mockStatic(FileTools.class)) {
            files.when(() -> FileTools.readFileContent(path)).thenReturn(svg);
            files.when(() -> FileTools.saveFileContent(eq(path), anyString(), eq("UTF-8")))
                .thenAnswer(invocation -> {
                    saved.set(invocation.getArgument(1));
                    return true;
                });

            ImageTools.sanitizeSvgFile(file);
        }

        assertNotNull(saved.get(), "Sanitized SVG must be written");
        assertFalse(saved.get().contains("bad()"), "Executable input must be removed");
        Document document = Jsoup.parse(saved.get(), "", Parser.xmlParser());
        assertEquals("0 0 10 10", document.selectFirst("svg").attr("viewBox"));
        assertEquals("http://www.w3.org/1999/xlink", document.selectFirst("svg").attr("xmlns:xlink"));
        assertEquals("#shape", document.getElementById("safe").attr("xlink:href"));
        assertEquals("M0 0L10 10", document.getElementById("shape").attr("d"));
        assertEquals("Label", document.getElementById("unsafe").text());
    }
}
