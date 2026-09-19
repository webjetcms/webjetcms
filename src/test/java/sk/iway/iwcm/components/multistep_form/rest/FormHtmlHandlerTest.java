package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.i18n.Prop;

/**
 * Tests context-specific rendering in {@link FormHtmlHandler}.
 */
class FormHtmlHandlerTest {

    /**
     * Verifies that the localized loader stays inside the form element on a web page.
     */
    @Test
    void appendsLocalizedLoaderInsidePageForm() {
        Prop prop = mock(Prop.class);
        StringBuilder formEndHtml = new StringBuilder("<button type=\"submit\">Send</button></form><script></script>");
        String loaderHtml = "<div class=\"loader\">Loading...</div>";
        when(prop.getText("components.mustistep.form.loader")).thenReturn(loaderHtml);

        FormHtmlHandler.appendFormLoader(formEndHtml, false, prop);

        assertEquals(
            "<button type=\"submit\">Send</button>" + loaderHtml + "</form><script></script>",
            formEndHtml.toString()
        );
        verify(prop).getText("components.mustistep.form.loader");
    }

    /**
     * Verifies that email rendering neither resolves nor appends the loader property.
     */
    @Test
    void omitsLoaderFromEmailForm() {
        Prop prop = mock(Prop.class);
        String originalHtml = "<button type=\"submit\">Send</button></form>";
        StringBuilder formEndHtml = new StringBuilder(originalHtml);

        FormHtmlHandler.appendFormLoader(formEndHtml, true, prop);

        assertEquals(originalHtml, formEndHtml.toString());
        verifyNoInteractions(prop);
    }
}
