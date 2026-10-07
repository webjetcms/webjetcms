package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.multistep_form.rest.SaveFormService.FormFiles;
import sk.iway.iwcm.i18n.Prop;

/** Tests sender selection and audit contents from prepared form data without sending an actual email. */
class FormMailServiceSenderTest {

    /** Uses prepared form data for the sender and audit, ignoring stored values and stale session answers. */
    @ParameterizedTest
    @CsvSource({
        "true, true, Jane & Doe, jane@example.com",
        "true, false, recipient@example.com, recipient@example.com",
        "false, true, Jane & Doe, jane@example.com",
        "false, false, recipient@example.com, recipient@example.com"
    })
    void resolvesSenderFromAppropriateSubmissionData(boolean encrypted, boolean formDataPresent,
            String expectedName, String expectedEmail) throws Exception {
        FormSettingsEntity settings = new FormSettingsEntity();
        if (encrypted) settings.setEncryptKey("test-public-key");
        FormsEntity form = new FormsEntity();
        form.setId(42L);
        form.setFormName("contact");
        form.setData(encrypted
            ? "first-name-7~encrypted-first|last-name-2~encrypted-last|email-address-11~encrypted-email"
            : "first-name-7~Stored|last-name-2~Name|email-address-11~stored@example.com");
        Map<String, String> formData = new LinkedHashMap<>();
        if (formDataPresent) {
            formData.put("first-name-7", "Jane &amp;");
            formData.put("last-name-2", "Doe");
            formData.put("email-address-11", "jane@example.com");
        }
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("language", "en");
        request.addHeader("X-CSRF-Token", "current-token");

        try (MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<Prop> props = mockStatic(Prop.class);
                MockedStatic<SendMail> mail = mockStatic(SendMail.class);
                MockedStatic<MultistepFormsService> forms = mockStatic(MultistepFormsService.class, CALLS_REAL_METHODS)) {
            constants.when(() -> Constants.getString("useSMTPServer")).thenReturn("true");
            constants.when(() -> Constants.getString("smtpServer")).thenReturn("localhost");
            constants.when(() -> Constants.getArray(FormMailService.NAME_FIELD_KEY)).thenReturn(new String[] {"first-name", "last-name"});
            constants.when(() -> Constants.getArray(FormMailService.EMAIL_FIELD_KEY)).thenReturn(new String[] {"email"});
            props.when(() -> Prop.getInstance("en")).thenReturn(mock(Prop.class));
            mail.when(() -> SendMail.getSession(any(Properties.class))).thenReturn(Session.getInstance(new Properties()));
            forms.when(() -> MultistepFormsService.getFormIdStatic("contact")).thenReturn(1);

            String sessionPrefix = MultistepFormsService.getSessionKey("contact", request) + "_";
            request.getSession().setAttribute(sessionPrefix + "first-name-7", "Other");
            request.getSession().setAttribute(sessionPrefix + "email-address-11", "other@example.com");

            new FormMailService().sendMail(form, formData, settings, "recipient@example.com", "Contact",
                new FormFiles(), true, "", new StringBuilder("Submitted form"), request);

            ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
            mail.verify(() -> SendMail.sendMessage(sent.capture()));
            InternetAddress sender = (InternetAddress) sent.getValue().getFrom()[0];
            assertEquals(expectedName, sender.getPersonal());
            assertEquals(expectedEmail, sender.getAddress());

            ArgumentCaptor<String> audit = ArgumentCaptor.forClass(String.class);
            adminlog.verify(() -> Adminlog.add(eq(Adminlog.TYPE_MULTISTEP_FORM_USERS), audit.capture(), eq(1L), eq(42L)), atLeastOnce());
            String parameters = audit.getValue().split("form parameters: ")[1].split("formName:")[0];
            if (formDataPresent) {
                assertTrue(parameters.contains("first-name-7: Jane &amp;"));
                assertTrue(parameters.contains("last-name-2: Doe"));
                assertTrue(parameters.contains("email-address-11: jane@example.com"));
            } else {
                assertTrue(parameters.isBlank());
            }
            assertFalse(parameters.contains(encrypted ? "encrypted-first" : "Stored"));
            assertFalse(parameters.contains("other@example.com"));
        }
    }
}
