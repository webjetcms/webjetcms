package sk.iway.iwcm.form;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.util.List;
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
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.upload.UploadedFile;

/**
 * Verifies that classic forms report EML storage failures as email failures.
 */
class FormMailActionDeliveryTest {

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
			mail.when(() -> SendMail.getSession(any(Properties.class))).thenReturn(Session.getInstance(new Properties()));
			mail.when(() -> SendMail.sendMessage(any(Message.class))).thenThrow(new IOException("EML directory is not writable"));

			String forward = FormMailAction.saveForm(request, Map.<String, List<UploadedFile>>of(), List.of(), null);

			assertTrue(forward.contains("formfail=emailsend"), forward);
			mail.verify(() -> SendMail.sendMessage(any(Message.class)));
		}
	}
}
