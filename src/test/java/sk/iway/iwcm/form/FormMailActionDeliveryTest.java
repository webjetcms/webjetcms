package sk.iway.iwcm.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.upload.UploadedFile;

/**
 * Verifies classic form email encoding and reporting of EML storage failures.
 */
class FormMailActionDeliveryTest {

	/** Preserves the visitor name in serialized sender and reply-to headers independently of the body charset. */
	@ParameterizedTest
	@CsvSource({"true, false", "false, false", "true, true", "false, true"})
	void preservesSenderNameWithAsciiBody(boolean asciiBody, boolean protectedSender) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("language", "en");
		request.setParameter("fields", "name");
		request.setParameter("fname", "name");
		request.setParameter("name", "Søren");
		request.setParameter("email", "sender@example.com");
		request.setParameter("recipients", "recipient@example.com");
		request.setParameter("forceTextPlain", "true");
		request.setParameter("formMailEncoding", Boolean.toString(asciiBody));
		request.setServerName("localhost");

		try (MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class);
			 MockedStatic<Constants> constants = mockStatic(Constants.class);
			 MockedStatic<Prop> props = mockStatic(Prop.class);
			 MockedStatic<DocDB> docs = mockStatic(DocDB.class);
			 MockedStatic<TemplatesDB> templates = mockStatic(TemplatesDB.class);
			 MockedStatic<SendMail> mail = mockStatic(SendMail.class)) {
			constants.when(() -> Constants.getString("formmailAllowedRecipients")).thenReturn("@example.com");
			constants.when(() -> Constants.getString("useSMTPServer")).thenReturn("true");
			constants.when(() -> Constants.getString("smtpServer")).thenReturn("localhost");
			if (protectedSender) {
				constants.when(() -> Constants.getString(SendMail.EMAIL_PROTECTION_SENDER_KEY)).thenReturn("noreply@example.com");
			}
			props.when(() -> Prop.getInstance("en")).thenReturn(mock(Prop.class));
			docs.when(DocDB::getInstance).thenReturn(mock(DocDB.class));
			templates.when(TemplatesDB::getInstance).thenReturn(mock(TemplatesDB.class));
			mail.when(() -> SendMail.getSession(any(Properties.class))).thenCallRealMethod();

			String forward = FormMailAction.saveForm(request, Map.<String, List<UploadedFile>>of(), List.of(), null);
			assertTrue(forward.contains("formsend=true"), forward);
			ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
			mail.verify(() -> SendMail.sendMessage(sent.capture()));
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			sent.getValue().writeTo(bytes);
			MimeMessage parsed = new MimeMessage(sent.getValue().getSession(), new ByteArrayInputStream(bytes.toByteArray()));
			InternetAddress sender = (InternetAddress) parsed.getFrom()[0];
			assertEquals(protectedSender ? "noreply@example.com" : "sender@example.com", sender.getAddress());
			InternetAddress visitor = protectedSender ? (InternetAddress) parsed.getReplyTo()[0] : sender;
			assertEquals("sender@example.com", visitor.getAddress());
			assertEquals("Søren", visitor.getPersonal());
			assertTrue(parsed.isMimeType("text/plain"));
			assertTrue((asciiBody ? "ASCII" : "UTF-8").equalsIgnoreCase(new ContentType(parsed.getContentType()).getParameter("charset")));
		}
	}

	@Test
	void reportsFailedEmlWriteAsFormFailure() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("language", "en");
		request.setParameter("fields", "name");
		request.setParameter("name", "Alice");
		request.setParameter("email", "sender@example.com");
		request.setParameter("recipients", "recipient@example.com");
		request.setParameter("forceTextPlain", "true");
		request.setServerName("localhost");

		try (MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class);
			 MockedStatic<Constants> constants = mockStatic(Constants.class);
			 MockedStatic<Prop> props = mockStatic(Prop.class);
			 MockedStatic<DocDB> docs = mockStatic(DocDB.class);
			 MockedStatic<TemplatesDB> templates = mockStatic(TemplatesDB.class);
			 MockedStatic<SendMail> mail = mockStatic(SendMail.class)) {
			constants.when(() -> Constants.getString("formmailAllowedRecipients")).thenReturn("@example.com");
			constants.when(() -> Constants.getString("useSMTPServer")).thenReturn("true");
			constants.when(() -> Constants.getString("smtpServer")).thenReturn("localhost");
			props.when(() -> Prop.getInstance("en")).thenReturn(mock(Prop.class));
			docs.when(DocDB::getInstance).thenReturn(mock(DocDB.class));
			templates.when(TemplatesDB::getInstance).thenReturn(mock(TemplatesDB.class));
			mail.when(() -> SendMail.getSession(any(Properties.class))).thenAnswer(invocation -> {
				Properties input = invocation.getArgument(0);
				assertTrue(input != System.getProperties(), "Form mail must use application-local properties");
				return invocation.callRealMethod();
			});
			mail.when(() -> SendMail.sendMessage(any(Message.class))).thenThrow(new IOException("EML directory is not writable"));

			String forward = FormMailAction.saveForm(request, Map.<String, List<UploadedFile>>of(), List.of(), null);

			assertTrue(forward.contains("formfail=emailsend"), forward);
			mail.verify(() -> SendMail.sendMessage(any(Message.class)));
		}
	}
}
