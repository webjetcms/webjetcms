package sk.iway.iwcm.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.components.multistep_form.mvc.MultistepFormApp;
import sk.iway.iwcm.i18n.Prop;

/** Verifies public component error messages use user-facing names. */
class WriteTagTest {

    /** Spring applications expose their localized AppStore title instead of a Java class name. */
    @Test
    void resolvesSpringApplicationNameFromAppStoreAnnotation() {
        Prop prop = mock(Prop.class);
        when(prop.getText("multistep_form.title")).thenReturn("Form");
        when(prop.getText("writetag.error", "Form", "Form", "Form")).thenReturn("Unexpected error in Form");

        String message = WriteTag.getSpringErrorMessage(prop, "writetag.error", MultistepFormApp.class.getName());

        assertEquals("Unexpected error in Form", message);
        assertFalse(message.contains(MultistepFormApp.class.getName()));
        verify(prop).getText("multistep_form.title");
    }

    /** Missing application classes still fall back to a package-free readable name. */
    @Test
    void humanizesMissingSpringApplicationClassName() {
        Prop prop = mock(Prop.class);
        when(prop.getText("writetag.error", "Unknown Spring", "Unknown Spring", "Unknown Spring"))
            .thenReturn("Unexpected error in Unknown Spring");

        String message = WriteTag.getSpringErrorMessage(prop, "writetag.error", "com.example.UnknownSpringApp");

        assertEquals("Unexpected error in Unknown Spring", message);
        assertFalse(message.contains("com.example"));
    }

    /** Java class names remain private even when a non-standard lowercase class name is unavailable. */
    @Test
    void hidesMissingLowercaseSpringApplicationClassName() {
        Prop prop = mock(Prop.class);
        when(prop.getText("writetag.error", "contact", "contact", "contact"))
            .thenReturn("Unexpected error in contact");

        String message = WriteTag.getSpringErrorMessage(prop, "writetag.error", "com.example.contactApp");

        assertEquals("Unexpected error in contact", message);
        assertFalse(message.contains("com.example"));
    }

    /** JSP component names retain the established path and filename substitutions. */
    @Test
    void preservesLegacyJspComponentName() {
        Prop prop = mock(Prop.class);
        when(prop.getText("writetag.error", "/components/contact-form/form_mail.jsp", "form_mail.jsp", "form mail"))
            .thenReturn("Unexpected error in form mail");

        assertEquals(
            "Unexpected error in form mail",
            WriteTag.getErrorMessage(prop, "writetag.error", "/components/contact-form/form_mail.jsp")
        );
    }
}
