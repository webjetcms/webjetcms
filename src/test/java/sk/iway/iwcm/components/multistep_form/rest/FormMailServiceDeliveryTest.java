package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.mail.Message;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
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
 * Verifies multistep form email encoding and reporting of EML storage failures.
 */
class FormMailServiceDeliveryTest {

	/** Preserves the serialized sender name while retaining the form's selected body charset. */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void preservesSenderNameWithAsciiBody(boolean asciiBody) throws Exception {
		FormSettingsEntity settings = new FormSettingsEntity();
		settings.setFormMailEncoding(asciiBody);
		settings.setForceTextPlain(true);
		FormsEntity form = new FormsEntity();
		form.setId(42L);
		form.setFormName("contact");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("language", "en");
		request.setServerName("localhost");

		try (MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class);
			 MockedStatic<Constants> constants = mockStatic(Constants.class);
			 MockedStatic<Prop> props = mockStatic(Prop.class);
			 MockedStatic<SendMail> mail = mockStatic(SendMail.class);
			 MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
			 MockedStatic<MultistepFormsService> forms = mockStatic(MultistepFormsService.class)) {
			constants.when(() -> Constants.getString("useSMTPServer")).thenReturn("true");
			constants.when(() -> Constants.getString("smtpServer")).thenReturn("localhost");
			constants.when(() -> Constants.getArray(FormMailService.NAME_FIELD_KEY)).thenReturn(new String[] {"name"});
			constants.when(() -> Constants.getArray(FormMailService.EMAIL_FIELD_KEY)).thenReturn(new String[] {"email"});
			props.when(() -> Prop.getInstance("en")).thenReturn(mock(Prop.class));
			mail.when(() -> SendMail.getSession(any(Properties.class))).thenCallRealMethod();
			forms.when(() -> MultistepFormsService.getFormIdStatic("contact")).thenReturn(1);

			new FormMailService().sendMail(form, Map.of("name", "Søren", "email", "sender@example.com"), settings,
				"recipient@example.com", "Contact", new FormFiles(), true, null, new StringBuilder("Submitted form"), request);

			ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
			mail.verify(() -> SendMail.sendMessage(sent.capture()));
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			sent.getValue().writeTo(bytes);
			MimeMessage parsed = new MimeMessage(sent.getValue().getSession(), new ByteArrayInputStream(bytes.toByteArray()));
			InternetAddress sender = (InternetAddress) parsed.getFrom()[0];
			assertEquals("sender@example.com", sender.getAddress());
			assertEquals("Søren", sender.getPersonal());
			assertTrue(parsed.isMimeType("text/plain"));
			assertTrue((asciiBody ? "ASCII" : "UTF-8").equalsIgnoreCase(new ContentType(parsed.getContentType()).getParameter("charset")));
			assertEquals("Submitted form", parsed.getContent());
		}
	}

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
			mail.when(() -> SendMail.getSession(any(Properties.class))).thenAnswer(invocation -> {
				Properties input = invocation.getArgument(0);
				assertTrue(input != System.getProperties(), "Form mail must use application-local properties");
				return invocation.callRealMethod();
			});
			mail.when(() -> SendMail.sendMessage(any(Message.class))).thenThrow(new IOException("EML directory is not writable"));
			forms.when(() -> MultistepFormsService.getFormIdStatic("contact")).thenReturn(1);
			org.mockito.Mockito.when(prop.getText("checkform.emailNotSend")).thenReturn("Email could not be saved");

			SaveFormException failure = assertThrows(SaveFormException.class, () ->
				service.sendMail(form, Map.of(), settings, "recipient@example.com", "Contact", new FormFiles(), true, null, new StringBuilder("Submitted form"), request));

			assertEquals("Email could not be saved", failure.getMessage());
			mail.verify(() -> SendMail.sendMessage(any(Message.class)));
		}
	}
}
