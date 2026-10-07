package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.components.forms.FormsEntity;
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

    /** Resolves selected and unselected options from exact or legacy values containing commas. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void resolvesSelectedOptionsWithCommaContainingValues(boolean exactSelections) {
        FormHtmlHandler handler = mock(FormHtmlHandler.class, CALLS_REAL_METHODS);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ReflectionTestUtils.setField(handler, "formName", "contact-form");
        ReflectionTestUtils.setField(handler, "formData", Map.of("choices", "Research, development"));
        try (MockedStatic<MultistepFormsService> forms = mockStatic(MultistepFormsService.class)) {
            forms.when(() -> MultistepFormsService.getSavedSelectedValues("contact-form", "choices", request))
                .thenReturn(exactSelections ? new String[] {"Research, development"} : null);

            assertTrue((Boolean) ReflectionTestUtils.invokeMethod(handler, "isCheckboxOrRadioSelected", "Research, development", "choices", request));
            assertFalse((Boolean) ReflectionTestUtils.invokeMethod(handler, "isCheckboxOrRadioSelected", "Other", "choices", request));
        }
    }
}
