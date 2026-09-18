package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import jakarta.mail.Message;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import sk.iway.iwcm.helpers.MailHelper;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.utils.Pair;

/**
 * Verifies sender protection against the headers of messages prepared for SMTP.
 */
class SendMailSenderTest {

    private MockedStatic<Constants> constants;
    private MockedStatic<Prop> props;
    private MockedStatic<Transport> transport;
    private MockedStatic<Adminlog> adminlog;
    private Properties systemProperties;

    @BeforeEach
    void setUp() {
        systemProperties = (Properties) System.getProperties().clone();
        constants = mockStatic(Constants.class);
        props = mockStatic(Prop.class);
        transport = mockStatic(Transport.class);
        adminlog = mockStatic(Adminlog.class);
        constants.when(() -> Constants.getString(anyString())).thenReturn("");
        constants.when(() -> Constants.getString("smtpServer")).thenReturn("localhost");
        constants.when(() -> Constants.getString("emailEncoding")).thenReturn("UTF-8");
        constants.when(() -> Constants.getString(SendMail.EMAIL_PROTECTION_SENDER_KEY)).thenReturn("noreply@example.com");
        constants.when(() -> Constants.getString(SendMail.EMAIL_PROTECTION_SENDER_NAME_KEY)).thenReturn("Protected sender");
        constants.when(() -> Constants.getString("formMailFixedSenderEmail")).thenReturn("forms@example.com");
    }

    @AfterEach
    void tearDown() {
        adminlog.close();
        transport.close();
        props.close();
        constants.close();
        System.setProperties(systemProperties);
    }

    /**
     * Preserves either configured address, its display name and its explicit or absent reply-to.
     */
    @ParameterizedTest
    @CsvSource({
        "noreply@example.com, visitor@external.com",
        "forms@example.com, visitor@external.com",
        "noreply@example.com,",
        "forms@example.com,"
    })
    void preservesAllowedSenders(String senderEmail, String replyTo) throws Exception {
        Message message = send(senderEmail, replyTo, "recipient@example.com");

        assertSender(message, senderEmail, "Original sender");
        assertReplyTo(message, replyTo);
    }

    /**
     * Rewrites other senders, including addresses on an allowed sender's domain, and preserves explicit reply-to.
     */
    @ParameterizedTest
    @CsvSource({
        "visitor@external.com, , visitor@external.com",
        "other@example.com, , other@example.com",
        "visitor@external.com, replies@example.com, replies@example.com"
    })
    void protectsOtherSenders(String senderEmail, String replyTo, String expectedReplyTo) throws Exception {
        Message message = send(senderEmail, replyTo, "recipient@example.com");

        assertSender(message, "noreply@example.com", "Protected sender");
        assertReplyTo(message, expectedReplyTo);
    }

    /**
     * Retains the existing same-as-email display-name setting when replacing an unapproved sender.
     */
    @Test
    void appliesSameAsEmailNameWhenReplacingSender() throws Exception {
        constants.when(() -> Constants.getString(SendMail.EMAIL_PROTECTION_SENDER_NAME_KEY)).thenReturn("same-as-email");

        Message message = send("visitor@external.com", null, "recipient@example.com");

        assertSender(message, "noreply@example.com", "noreply@example.com");
        assertReplyTo(message, "visitor@external.com");
    }

    /**
     * Leaves messages unchanged when sender protection has no valid replacement address.
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "invalid-address"})
    void preservesSenderWhenProtectionIsDisabled(String protectionEmail) throws Exception {
        constants.when(() -> Constants.getString(SendMail.EMAIL_PROTECTION_SENDER_KEY)).thenReturn(protectionEmail);

        Message message = send("visitor@external.com", null, "recipient@example.com");

        assertSender(message, "visitor@external.com", "Original sender");
        assertReplyTo(message, null);
    }

    /**
     * Keeps the existing Cloud helpdesk exception to sender rewriting.
     */
    @Test
    void preservesHelpdeskException() throws Exception {
        Message message = send("visitor@external.com", null, "helpdesk@websupport.sk");

        assertSender(message, "visitor@external.com", "Original sender");
        assertReplyTo(message, null);
    }

    private Message send(String senderEmail, String replyTo, String recipientEmail) {
        MailHelper helper = new MailHelper()
            .setFromName("Original sender")
            .setFromEmail(senderEmail)
            .setToEmail(recipientEmail)
            .setReplyTo(replyTo)
            .setSubject("Sender protection")
            .setMessage("Submitted form")
            .setBaseHref("https://example.com")
            .setSendLaterWhenException(false)
            .setWriteToAuditLog(false);
        Pair<Boolean, Exception> result = helper.sendCapturingException();
        assertNull(result.getSecond());
        assertTrue(result.getFirst());

        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        transport.verify(() -> Transport.send(message.capture()));
        return message.getValue();
    }

    private void assertSender(Message message, String email, String name) throws Exception {
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertEquals(email, from.getAddress());
        assertEquals(name, from.getPersonal());
    }

    private void assertReplyTo(Message message, String replyTo) throws Exception {
        if (replyTo == null) {
            assertNull(message.getHeader("Reply-To"));
        } else {
            assertEquals(replyTo, ((InternetAddress) message.getReplyTo()[0]).getAddress());
        }
    }
}
