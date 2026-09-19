package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.mail.Message;
import jakarta.mail.Session;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.multistep_form.rest.SaveFormService.FormFiles;
import sk.iway.iwcm.components.multistep_form.support.SaveFormException;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

/**
 * Verifies that multistep forms report EML storage failures as email failures.
 */
class FormMailServiceDeliveryTest {

	@Test
	void reportsFailedEmlWriteAsFormFailure() throws Exception {
		FormMailService service = new FormMailService();
		FormSettingsEntity settings = new FormSettingsEntity();
		FormsEntity form = new FormsEntity();
		form.setId(42L);
		form.setFormName("contact");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("language", "en");
		request.setServerName("localhost");
		Prop prop = mock(Prop.class);

		try (MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class);
			 MockedStatic<Constants> constants = mockStatic(Constants.class);
			 MockedStatic<Prop> props = mockStatic(Prop.class);
			 MockedStatic<SendMail> mail = mockStatic(SendMail.class);
			 MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
			 MockedStatic<MultistepFormsService> forms = mockStatic(MultistepFormsService.class)) {
			constants.when(() -> Constants.getString("useSMTPServer")).thenReturn("true");
			constants.when(() -> Constants.getString("smtpServer")).thenReturn("localhost");
			props.when(() -> Prop.getInstance("en")).thenReturn(prop);
			mail.when(() -> SendMail.getSession(any(Properties.class))).thenReturn(Session.getInstance(new Properties()));
			mail.when(() -> SendMail.sendMessage(any(Message.class))).thenThrow(new IOException("EML directory is not writable"));
			forms.when(() -> MultistepFormsService.getFormIdStatic("contact")).thenReturn(1);
			forms.when(() -> MultistepFormsService.getFormDataAsMap(form)).thenReturn(Map.of());
			org.mockito.Mockito.when(prop.getText("checkform.emailNotSend")).thenReturn("Email could not be saved");

			SaveFormException failure = assertThrows(SaveFormException.class, () ->
				service.sendMail(form, settings, "recipient@example.com", "Contact", new FormFiles(), true, null, new StringBuilder("Submitted form"), request));

			assertEquals("Email could not be saved", failure.getMessage());
			mail.verify(() -> SendMail.sendMessage(any(Message.class)));
		}
	}
}
