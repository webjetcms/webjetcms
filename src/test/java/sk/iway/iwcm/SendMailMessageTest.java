package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/**
 * Verifies delivery of already composed messages through SMTP or EML storage.
 */
class SendMailMessageTest {

	@Test
	void savesCompleteMessagesWithUniqueNamesWithoutSmtp(@TempDir Path temporaryDirectory) throws Exception {
		Path emailDirectory = temporaryDirectory.resolve("emails");
		MimeMessage first = message();
		MimeMessage second = message();

		try (MockedStatic<Constants> constants = mockStatic(Constants.class);
			 MockedStatic<Tools> tools = mockToolsWithRealPath(emailDirectory);
			 MockedStatic<Transport> transport = mockStatic(Transport.class)) {
			constants.when(() -> Constants.getBoolean("sendMailSaveEmail")).thenReturn(true);
			constants.when(() -> Constants.getString("sendMailSaveEmailPath", "")).thenReturn("/files/protected/emails");

			assertTrue(SendMail.sendMessage(first));
			assertTrue(SendMail.sendMessage(second));
			transport.verifyNoInteractions();
		}

		List<Path> files;
		try (var paths = Files.list(emailDirectory)) {
			files = paths.toList();
		}
		assertEquals(2, files.size());
		assertTrue(files.stream().allMatch(path -> path.getFileName().toString().endsWith(".eml")));

		try (InputStream input = Files.newInputStream(files.get(0))) {
			MimeMessage saved = new MimeMessage(Session.getInstance(new Properties()), input);
			assertEquals("Form submission", saved.getSubject());
			assertEquals("recipient@example.com", ((InternetAddress) saved.getRecipients(Message.RecipientType.TO)[0]).getAddress());
			assertEquals("copy@example.com", ((InternetAddress) saved.getRecipients(Message.RecipientType.CC)[0]).getAddress());
			assertEquals("blind@example.com", ((InternetAddress) saved.getRecipients(Message.RecipientType.BCC)[0]).getAddress());
			assertEquals("reply@example.com", ((InternetAddress) saved.getReplyTo()[0]).getAddress());
			assertEquals("localhost", saved.getHeader("X-server-name", null));
			Multipart content = (Multipart) saved.getContent();
			assertEquals("Submitted form", ((String) content.getBodyPart(0).getContent()).trim());
			assertEquals("attachment.txt", content.getBodyPart(1).getFileName());
			assertEquals("Attached content", ((String) content.getBodyPart(1).getContent()).trim());
		}
	}

	@Test
	void sendsThroughSmtpWhenSavingIsDisabled() throws Exception {
		MimeMessage message = message();
		try (MockedStatic<Constants> constants = mockStatic(Constants.class);
			 MockedStatic<Transport> transport = mockStatic(Transport.class)) {
			constants.when(() -> Constants.getBoolean("sendMailSaveEmail")).thenReturn(false);

			assertFalse(SendMail.sendMessage(message));
			transport.verify(() -> Transport.send(message));
		}
	}

	@Test
	void rejectsMissingSavePathWithoutSending() throws Exception {
		try (MockedStatic<Constants> constants = mockStatic(Constants.class);
			 MockedStatic<Transport> transport = mockStatic(Transport.class)) {
			constants.when(() -> Constants.getBoolean("sendMailSaveEmail")).thenReturn(true);
			constants.when(() -> Constants.getString("sendMailSaveEmailPath", "")).thenReturn("");

			assertThrows(IOException.class, () -> SendMail.sendMessage(message()));
			transport.verifyNoInteractions();
		}
	}

	@Test
	void rejectsUnwritableSavePathWithoutSending(@TempDir Path temporaryDirectory) throws Exception {
		Path fileInPlaceOfDirectory = temporaryDirectory.resolve("not-a-directory");
		Files.writeString(fileInPlaceOfDirectory, "existing file");

		try (MockedStatic<Constants> constants = mockStatic(Constants.class);
			 MockedStatic<Tools> tools = mockToolsWithRealPath(fileInPlaceOfDirectory);
			 MockedStatic<Transport> transport = mockStatic(Transport.class)) {
			constants.when(() -> Constants.getBoolean("sendMailSaveEmail")).thenReturn(true);
			constants.when(() -> Constants.getString("sendMailSaveEmailPath", "")).thenReturn("/files/protected/emails");

			assertThrows(IOException.class, () -> SendMail.sendMessage(message()));
			transport.verifyNoInteractions();
		}
	}

	private static MockedStatic<Tools> mockToolsWithRealPath(Path realPath) {
		return mockStatic(Tools.class, invocation -> {
			if ("getRealPath".equals(invocation.getMethod().getName())) return realPath.toString();
			return invocation.callRealMethod();
		});
	}

	private static MimeMessage message() throws Exception {
		MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
		message.setFrom(new InternetAddress("sender@example.com"));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("recipient@example.com"));
		message.setRecipients(Message.RecipientType.CC, InternetAddress.parse("copy@example.com"));
		message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse("blind@example.com"));
		message.setReplyTo(InternetAddress.parse("reply@example.com"));
		message.setHeader("X-server-name", "localhost");
		message.setSubject("Form submission");

		MimeBodyPart body = new MimeBodyPart();
		body.setText("Submitted form", "UTF-8");
		MimeBodyPart attachment = new MimeBodyPart();
		attachment.setText("Attached content", "UTF-8");
		attachment.setFileName("attachment.txt");
		Multipart parts = new MimeMultipart("mixed");
		parts.addBodyPart(body);
		parts.addBodyPart(attachment);
		message.setContent(parts);
		return message;
	}
}
