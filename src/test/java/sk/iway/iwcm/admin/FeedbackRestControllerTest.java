package sk.iway.iwcm.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Verifies feedback formatting, empty-message validation, and HTML sanitization. */
class FeedbackRestControllerTest {

    @Test
    void preservesEditorFormatting() {
        String result = FeedbackRestController.prepareFeedbackText(
            "<p><strong>Bold</strong> <em>Italic</em> <u>Underlined</u></p><ul><li>Item</li></ul>", true);
        assertTrue(result.contains("<strong>Bold</strong>"));
        assertTrue(result.contains("<em>Italic</em>"));
        assertTrue(result.contains("<u>Underlined</u>"));
        assertTrue(result.contains("<ul>"));
        assertTrue(result.contains("<li>Item</li>"));
    }

    @Test
    void removesExecutableHtml() {
        String result = FeedbackRestController.prepareFeedbackText(
            "<p onclick='alert(1)'>Message<script>alert(1)</script><img src='https://example.com/image.png' onerror='alert(1)'></p>"
                + "<a href='javascript:alert(1)'>Unsafe link</a><a href='https://example.com'>Safe link</a>", true);
        assertFalse(result.contains("onclick"));
        assertFalse(result.contains("script"));
        assertFalse(result.contains("onerror"));
        assertTrue(result.contains("href=\"https://example.com\""));
    }

    @Test
    void rejectsEmptyEditorMarkup() {
        assertThrows(IllegalArgumentException.class, () -> FeedbackRestController.prepareFeedbackText("<p><br></p>", true));
        assertThrows(IllegalArgumentException.class, () -> FeedbackRestController.prepareFeedbackText("<p>&nbsp; </p>", true));
        assertThrows(IllegalArgumentException.class, () -> FeedbackRestController.prepareFeedbackText(null, true));
        assertThrows(IllegalArgumentException.class, () -> FeedbackRestController.prepareFeedbackText(null, false));
    }

    @Test
    void retainsPlainTextCompatibility() {
        assertEquals("<p>First<br/>\nSecond</p>", FeedbackRestController.prepareFeedbackText("First\nSecond", false));
        String result = FeedbackRestController.prepareFeedbackText("<strong>Literal markup</strong>", false);
        assertFalse(result.contains("<strong>"));
    }
}
