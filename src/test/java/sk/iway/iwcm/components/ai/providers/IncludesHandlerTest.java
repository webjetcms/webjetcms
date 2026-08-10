package sk.iway.iwcm.components.ai.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.api.Test;

class IncludesHandlerTest {

    @Test
    void restoresPlaceholderSplitAcrossStreamFragments() throws Exception {
        IncludesHandler handler = new IncludesHandler(Map.of(1, "!INCLUDE(/components/banner.jsp)!"));
        StringWriter output = new StringWriter();
        BufferedWriter writer = new BufferedWriter(output);

        handler.handleLine("Before __LOCK_", writer);
        handler.handleLine("1__ after", writer);
        handler.finish(writer);

        String expected = "Before !INCLUDE(/components/banner.jsp)! after";
        assertEquals(expected, output.toString());
        assertEquals(expected, handler.getWholeResponse());
    }

    @Test
    void flushesTrailingTextThatLooksLikeAnIncompletePlaceholder() throws Exception {
        IncludesHandler handler = new IncludesHandler(Map.of());
        StringWriter output = new StringWriter();
        BufferedWriter writer = new BufferedWriter(output);

        handler.handleLine("Response ending with __LOC", writer);
        handler.finish(writer);

        assertEquals("Response ending with __LOC", output.toString());
        assertEquals(output.toString(), handler.getWholeResponse());
    }
}
