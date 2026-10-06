package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

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

/** Tests sender selection from encrypted submissions without sending an actual email. */
class FormMailServiceSenderTest {

    /** Uses only the matching encrypted submission's session values and preserves unencrypted sender lookup. */
    @ParameterizedTest
    @CsvSource({
        "true, true, Jane Doe, jane@example.com",
        "true, false, recipient@example.com, recipient@example.com",
        "false, true, Stored Name, stored@example.com"
    })
    void resolvesSenderFromAppropriateSubmissionData(boolean encrypted, boolean sessionValuesPresent,
            String expectedName, String expectedEmail) throws Exception {
        FormSettingsEntity settings = new FormSettingsEntity();
        if (encrypted) settings.setEncryptKey("test-public-key");
        FormsEntity form = new FormsEntity();
        form.setId(42L);
        form.setFormName("contact");
        form.setData(encrypted
            ? "first-name-7~encrypted-first|last-name-2~encrypted-last|email-address-11~encrypted-email"
            : "first-name-7~Stored|last-name-2~Name|email-address-11~stored@example.com");
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
            if (sessionValuesPresent) {
                request.getSession().setAttribute(sessionPrefix + "first-name-7", "Jane");
                request.getSession().setAttribute(sessionPrefix + "last-name-2", "Doe");
                request.getSession().setAttribute(sessionPrefix + "email-address-11", "jane@example.com");
            }
            for (String otherPrefix : new String[] {"MultistepForm_contact_1_other-token_",
                    "MultistepForm_contact_2_current-token_", "MultistepForm_other-form_1_current-token_"}) {
                request.getSession().setAttribute(otherPrefix + "first-name-7", "Other");
                request.getSession().setAttribute(otherPrefix + "email-address-11", "other@example.com");
            }

            new FormMailService().sendMail(form, settings, "recipient@example.com", "Contact",
                new FormFiles(), true, "", new StringBuilder("Submitted form"), request);

            ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
            mail.verify(() -> SendMail.sendMessage(sent.capture()));
            InternetAddress sender = (InternetAddress) sent.getValue().getFrom()[0];
            assertEquals(expectedName, sender.getPersonal());
            assertEquals(expectedEmail, sender.getAddress());
        }
    }
}
