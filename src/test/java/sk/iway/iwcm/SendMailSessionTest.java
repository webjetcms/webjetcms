package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;

/** Verifies isolation of SMTP and legacy Amazon SES sessions across configuration changes. */
class SendMailSessionTest {

    private MockedStatic<Constants> constants;

    @BeforeEach
    void setUp() {
        constants = mockStatic(Constants.class);
        constants.when(() -> Constants.getString(anyString())).thenReturn("");
    }

    @AfterEach
    void tearDown() {
        constants.close();
    }

    /** Keeps SSL and plain SMTP sessions independent regardless of their creation order. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void isolatesSslAndPlainSmtp(boolean sslFirst) {
        Session first = smtpSession(sslFirst);
        Properties firstProperties = (Properties) first.getProperties().clone();
        Session second = smtpSession(!sslFirst);
        Session ssl = sslFirst ? first : second;
        Session plain = sslFirst ? second : first;

        assertNotSame(first, second);
        assertNotSame(first.getProperties(), second.getProperties());
        assertEquals(firstProperties, first.getProperties());
        assertEquals("ssl.example.com", ssl.getProperty("mail.smtp.host"));
        assertEquals("465", ssl.getProperty("mail.smtp.socketFactory.port"));
        assertEquals("javax.net.ssl.SSLSocketFactory", ssl.getProperty("mail.smtp.socketFactory.class"));
        assertEquals("plain.example.com", plain.getProperty("mail.smtp.host"));
        assertEquals("25", plain.getProperty("mail.smtp.port"));
        assertNull(plain.getProperty("mail.smtp.socketFactory.class"));
        assertNull(plain.getProperty("mail.smtp.socketFactory.port"));
    }

    /** Reusing caller properties does not carry TLS, authentication or timeout settings into the next session. */
    @Test
    void snapshotsPropertiesAndAuthenticationBeforeConfigurationChanges() {
        Properties input = new Properties();
        input.setProperty("mail.smtp.host", "tls.example.com");
        constants.when(() -> Constants.getBoolean("smtpUseTLS")).thenReturn(true);
        constants.when(() -> Constants.getString("smtpPort")).thenReturn("587");
        constants.when(() -> Constants.getString("smtpTLSVersion")).thenReturn("TLSv1.2");
        constants.when(() -> Constants.getString("smtpUser")).thenReturn("test-user");
        constants.when(() -> Constants.getString("smtpPassword")).thenReturn("test-password");
        constants.when(() -> Constants.getString("smtpAuthMechanism")).thenReturn("LOGIN");
        constants.when(() -> Constants.getInt("smtpConnectionTimeoutMillis")).thenReturn(30000);
        Session tls = SendMail.getSession(input);

        constants.when(() -> Constants.getBoolean("smtpUseTLS")).thenReturn(false);
        constants.when(() -> Constants.getString("smtpPort")).thenReturn("");
        constants.when(() -> Constants.getString("smtpTLSVersion")).thenReturn("");
        constants.when(() -> Constants.getString("smtpUser")).thenReturn("");
        constants.when(() -> Constants.getString("smtpPassword")).thenReturn("");
        constants.when(() -> Constants.getString("smtpAuthMechanism")).thenReturn("");
        constants.when(() -> Constants.getInt("smtpConnectionTimeoutMillis")).thenReturn(0);
        input.setProperty("mail.smtp.host", "plain.example.com");
        Session plain = SendMail.getSession(input);

        assertEquals(1, input.size());
        assertEquals("tls.example.com", tls.getProperty("mail.smtp.host"));
        assertEquals("587", tls.getProperty("mail.smtp.port"));
        assertEquals("true", tls.getProperty("mail.smtp.starttls.enable"));
        assertEquals("TLSv1.2", tls.getProperty("mail.smtp.ssl.protocols"));
        assertEquals("LOGIN", tls.getProperty("mail.smtp.auth.mechanisms"));
        assertEquals(30000, tls.getProperties().get("mail.smtp.connectiontimeout"));
        PasswordAuthentication authentication = tls.requestPasswordAuthentication(null, 587, "smtp", "", "");
        assertEquals("test-user", authentication.getUserName());
        assertEquals("test-password", authentication.getPassword());
        assertEquals("plain.example.com", plain.getProperty("mail.smtp.host"));
        assertEquals("25", plain.getProperty("mail.smtp.port"));
        for (String key : new String[] {"starttls.enable", "ssl.protocols", "auth", "auth.mechanisms", "connectiontimeout"}) {
            assertNull(plain.getProperties().get("mail.smtp." + key), key);
        }
        assertNull(plain.requestPasswordAuthentication(null, 25, "smtp", "", ""));
    }

    /** Keeps each SES account's credentials and optional endpoint local and out of subsequent SMTP sessions. */
    @Test
    void isolatesAmazonSesAccountsAndSmtp() {
        constants.when(() -> Constants.getBoolean("useAmazonSES")).thenReturn(true);
        constants.when(() -> Constants.getString("amazonAccessKey")).thenReturn("test-access-a");
        constants.when(() -> Constants.getString("amazonSecretKey")).thenReturn("test-secret-a");
        constants.when(() -> Constants.getString("amazonEmailServiceHost")).thenReturn("ses.example.com");
        Properties input = new Properties();
        Session first = SendMail.getSession(input);

        constants.when(() -> Constants.getString("amazonAccessKey")).thenReturn("test-access-b");
        constants.when(() -> Constants.getString("amazonSecretKey")).thenReturn("test-secret-b");
        constants.when(() -> Constants.getString("amazonEmailServiceHost")).thenReturn("");
        Session second = SendMail.getSession(input);
        constants.when(() -> Constants.getBoolean("useAmazonSES")).thenReturn(false);
        Session smtp = SendMail.getSession(input);

        assertNotSame(first, second);
        assertNotSame(second, smtp);
        assertEquals("test-access-a", first.getProperty("mail.aws.user"));
        assertEquals("test-secret-a", first.getProperty("mail.aws.password"));
        assertEquals("ses.example.com", first.getProperty("mail.aws.host"));
        assertEquals("test-access-b", second.getProperty("mail.aws.user"));
        assertEquals("test-secret-b", second.getProperty("mail.aws.password"));
        assertNull(second.getProperty("mail.aws.host"));
        assertNull(smtp.getProperty("mail.aws.user"));
        assertNull(smtp.getProperty("mail.aws.password"));
        assertEquals(new Properties(), input);
    }

    /** Concurrent callers with different configurations retain their own host and port. */
    @Test
    void isolatesConcurrentSessions() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        try {
            Future<Session> first = executor.submit(() -> concurrentSession("first.example.com", "25", ready));
            Future<Session> second = executor.submit(() -> concurrentSession("second.example.com", "587", ready));
            Session firstSession = first.get(10, TimeUnit.SECONDS);
            Session secondSession = second.get(10, TimeUnit.SECONDS);

            assertEquals("first.example.com", firstSession.getProperty("mail.smtp.host"));
            assertEquals("25", firstSession.getProperty("mail.smtp.port"));
            assertEquals("second.example.com", secondSession.getProperty("mail.smtp.host"));
            assertEquals("587", secondSession.getProperty("mail.smtp.port"));
            assertNotSame(firstSession, secondSession);
        } finally {
            executor.shutdownNow();
        }
    }

    private static Session concurrentSession(String host, String port, CountDownLatch ready) throws Exception {
        try (MockedStatic<Constants> configuration = mockStatic(Constants.class)) {
            configuration.when(() -> Constants.getString(anyString())).thenReturn("");
            configuration.when(() -> Constants.getString("smtpPort")).thenReturn(port);
            Properties input = new Properties();
            input.setProperty("mail.smtp.host", host);
            ready.countDown();
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Both callers must be ready before creating sessions");
            return SendMail.getSession(input);
        }
    }

    private Session smtpSession(boolean ssl) {
        constants.when(() -> Constants.getBoolean("smtpUseSSL")).thenReturn(ssl);
        constants.when(() -> Constants.getString("smtpPort")).thenReturn(ssl ? "465" : "25");
        Properties input = new Properties();
        input.setProperty("mail.smtp.host", ssl ? "ssl.example.com" : "plain.example.com");
        return SendMail.getSession(input);
    }
}
