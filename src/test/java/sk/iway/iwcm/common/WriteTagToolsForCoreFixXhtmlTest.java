package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.*;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Test for the fixXhtml method in WriteTagToolsForCore.
 * Tests replacement of target="_blank" with onclick and correction of __blank to _blank.
 */
class WriteTagToolsForCoreFixXhtmlTest extends BaseWebjetTest {

    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();
        // Reset constants for editorTargetBlankFunction
        Constants.setString("editorTargetBlankFunction", "");
        Constants.setString("multiDomainEnabled", "false");
    }

    @Test
     @DisplayName("Basic replacement of target='_blank' with single quotes")
    void testFixXhtmlSingleQuotes() {
        Constants.setString("editorTargetBlankFunction", "openInNewTab");

        StringBuilder input = new StringBuilder("<a href='/page.html' target='_blank'>Link</a>");
        StringBuilder result = WriteTagToolsForCore.fixXhtml(input, request);

        assertEquals("<a href='/page.html' onclick=\"openInNewTab\">Link</a>", result.toString());
    }

    @Test
      @DisplayName("Basic replacement of target=\"_blank\" with double quotes")
    void testFixXhtmlDoubleQuotes() {
        Constants.setString("editorTargetBlankFunction", "openInNewTab");

        StringBuilder input = new StringBuilder("<a href=\"/page.html\" target=\"_blank\">Link</a>");
        StringBuilder result = WriteTagToolsForCore.fixXhtml(input, request);

        assertEquals("<a href=\"/page.html\" onclick=\"openInNewTab\">Link</a>", result.toString());
    }

    @Test
       @DisplayName("Replacement of multiple target=\"_blank\" occurrences in one text")
    void testFixXhtmlMultipleOccurrences() {
        Constants.setString("editorTargetBlankFunction", "openInNewTab");

        StringBuilder input = new StringBuilder(
            "<a href='/page1.html' target='_blank'>Link1</a>" +
             "<p>Text between</p>" +
             "<a href='/page2.html' target='_blank'>Link2</a>"
         );
        StringBuilder result = WriteTagToolsForCore.fixXhtml(input, request);

        String expected =
             "<a href='/page1.html' onclick=\"openInNewTab\">Link1</a>" +
             "<p>Text between</p>" +
            "<a href='/page2.html' onclick=\"openInNewTab\">Link2</a>";
        assertEquals(expected, result.toString());
    }

    @Test
     @DisplayName("__blank to _blank replacement with different quote combinations")
    void testFixXhtmlDoubleUnderscoreVariations() {
          // Test single quotes
        StringBuilder input1 = new StringBuilder("<a href='/page.html' target='__blank'>Link</a>");
        StringBuilder result1 = WriteTagToolsForCore.fixXhtml(input1, request);
        assertEquals("<a href='/page.html' target='_blank'>Link</a>", result1.toString());

          // Test double quotes
        StringBuilder input2 = new StringBuilder("<a href=\"/page.html\" target=\"__blank\">Link</a>");
        StringBuilder result2 = WriteTagToolsForCore.fixXhtml(input2, request);
        assertEquals("<a href=\"/page.html\" target=\"_blank\">Link</a>", result2.toString());
    }

     @Test
      @DisplayName("No change when editorTargetBlankFunction is not set")
    void testFixXhtmlNoTargetBlankFunction() {
        Constants.setString("editorTargetBlankFunction", "");

        StringBuilder input = new StringBuilder("<a href='/page.html' target='_blank'>Link</a>");
        StringBuilder result = WriteTagToolsForCore.fixXhtml(input, request);

         // target='_blank' should remain unchanged when editorTargetBlankFunction is empty
        assertEquals("<a href='/page.html' target='_blank'>Link</a>", result.toString());
     }

     @Test
     @DisplayName("No change for empty text")
    void testFixXhtmlEmptyText() {
        StringBuilder input = new StringBuilder("");
        StringBuilder result = WriteTagToolsForCore.fixXhtml(input, request);

        assertEquals("", result.toString());
    }

    @Test
     @DisplayName("No change for null text")
    void testFixXhtmlNullText() {
        StringBuilder result = WriteTagToolsForCore.fixXhtml(null, request);

        assertNull(result);
    }

    @Test
      @DisplayName("No change when text does not contain target blank")
    void testFixXhtmlNoTargetBlank() {
        Constants.setString("editorTargetBlankFunction", "openInNewTab");

        StringBuilder input = new StringBuilder("<p>Regular text without link</p>");
        StringBuilder result = WriteTagToolsForCore.fixXhtml(input, request);

        assertEquals("<p>Regular text without link</p>", result.toString());
     }

     @Test
     @DisplayName("Combination of target blank and __blank replacement in the same text")
    void testFixXhtmlCombinedReplacements() {
        Constants.setString("editorTargetBlankFunction", "openInNewTab");

        StringBuilder input = new StringBuilder(
            "<a href='/page1.html' target='_blank'>Link1</a>" +
            "<a href='/page2.html' target='__blank'>Link2</a>" +
            "<a href='/page3.html' target=\"__blank\">Link3</a>"
        );
        StringBuilder result = WriteTagToolsForCore.fixXhtml(input, request);

        String expected =
            "<a href='/page1.html' onclick=\"openInNewTab\">Link1</a>" +
            "<a href='/page2.html' target='_blank'>Link2</a>" +
            "<a href='/page3.html' target=\"_blank\">Link3</a>";
        assertEquals(expected, result.toString());
    }

    @Test
       @DisplayName("Mix of single and double quotes for target blank")
    void testFixXhtmlMixedQuotes() {
        Constants.setString("editorTargetBlankFunction", "newWindow");

        StringBuilder input = new StringBuilder(
            "<a href='a' target='_blank'>A</a>" +
            "<a href=\"b\" target=\"_blank\">B</a>"
        );
        StringBuilder result = WriteTagToolsForCore.fixXhtml(input, request);

        String expected =
            "<a href='a' onclick=\"newWindow\">A</a>" +
            "<a href=\"b\" onclick=\"newWindow\">B</a>";
        assertEquals(expected, result.toString());
    }

    @Test
       @DisplayName("MultiDomainEnabled = true calls fixDomainPaths")
    void testFixXhtmlMultiDomainEnabled() {
        Constants.setString("multiDomainEnabled", "true");

        StringBuilder input = new StringBuilder("<a href='/files/test.png'>Image</a>");
        StringBuilder result = WriteTagToolsForCore.fixXhtml(input, request);

          // Since multiDomainEnabled is true, fixDomainPaths will be called
          // without domain alias the text should remain unchanged
        assertNotNull(result);
      }

      @Test
      @DisplayName("MultiDomainEnabled = false does not call fixDomainPaths")
    void testFixXhtmlMultiDomainDisabled() {
        Constants.setString("multiDomainEnabled", "false");

        StringBuilder input = new StringBuilder("<a href='/files/test.png'>Image</a>");
        StringBuilder result = WriteTagToolsForCore.fixXhtml(input, request);

        assertEquals("<a href='/files/test.png'>Image</a>", result.toString());
    }

    @Test
    @DisplayName("Replacement priority: first _blank to onclick, then __blank to _blank")
    void testFixXhtmlReplacementOrder() {
           // If we have __blank, it will first be converted to _blank,
           // and then we should check if onclick replacement is needed
           // (but _blank is no longer __blank, so it won't be converted to onclick)
        Constants.setString("editorTargetBlankFunction", "openInNewTab");

           // __blank will be converted to _blank (not to onclick, because _blank is no longer __blank)
        StringBuilder input = new StringBuilder("<a target='__blank'>Link</a>");
        StringBuilder result = WriteTagToolsForCore.fixXhtml(input, request);

           // __blank -> _blank (not onclick, because the order is: first _blank, then __blank)
        assertEquals("<a target='_blank'>Link</a>", result.toString());
      }
    }
