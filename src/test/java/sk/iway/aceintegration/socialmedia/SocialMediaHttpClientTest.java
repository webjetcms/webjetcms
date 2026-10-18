package sk.iway.aceintegration.socialmedia;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import sk.iway.aceintegration.socialmedia.SocialMediaFeedException.Reason;

/** Verifies safe upstream diagnostics and credential handling without external HTTP calls. */
class SocialMediaHttpClientTest {
    private static final URI FACEBOOK = URI.create("https://graph.facebook.com/v26.0/123/published_posts");
    private static final String SECRET = "private-token-sentinel";

    /** Graph codes remain useful while messages, request URLs, trace IDs and tokens stay out of diagnostics. */
    @Test
    void reportsOnlyNumericFacebookErrorDetails() throws Exception {
        String body = "{\"error\":{\"code\":190,\"error_subcode\":463,\"message\":\"" + SECRET
            + "\",\"fbtrace_id\":\"" + SECRET + "\"}}";
        CloseableHttpClient transport = transport(400, new ByteArrayEntity(bytes(body)));
        try (SocialMediaHttpClient client = new SocialMediaHttpClient(transport)) {
            SocialMediaFeedException error = assertThrows(SocialMediaFeedException.class,
                () -> client.download(FACEBOOK, SECRET, 4096));
            assertEquals("Facebook API returned an error; HTTP 400; Facebook code 190; Facebook subcode 463",
                SocialMediaFeedException.safeSummary(error));
            assertFalse(error.toString().contains(SECRET));
            ArgumentCaptor<HttpUriRequest> request = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(transport).execute(request.capture());
            assertEquals("Bearer " + SECRET, request.getValue().getFirstHeader("Authorization").getValue());
            assertFalse(request.getValue().getURI().toString().contains(SECRET));
        }
    }

    /** Known Graph errors expose only allowlisted fields and fixed guidance, never arbitrary names or tokens. */
    @Test
    void classifiesKnownGraphFailuresWithoutExposingMessages() throws Exception {
        String[][] cases = {
            { "Tried accessing nonexisting field (published_posts) on node type (User)",
                "Configured Facebook object is a User instead of a Page; verify socialMediaFacebookPageId", "published_posts" },
            { "Tried accessing nonexisting field (attachments) on node type (Post)",
                "Requested Facebook field does not exist for this object", "attachments" },
            { "Tried accessing nonexisting field (title) on node type (PostAttachment)",
                "Requested Facebook field does not exist for this object", "title" },
            { "Tried accessing nonexisting field (description) on node type (PostAttachment)",
                "Requested Facebook field does not exist for this object", "description" },
            { "Tried accessing nonexisting field (other_field) on node type (User)",
                "Requested Facebook field does not exist for this object" },
            { "Tried accessing nonexisting field (" + SECRET + ") on node type (User)",
                "Requested Facebook field does not exist for this object" },
            { "Unsupported get request. Object with ID '123' does not exist or requires permissions",
                "Facebook object is unavailable; verify socialMediaFacebookPageId and Page permissions" },
            { "A different provider error", "Facebook API returned an error" }
        };
        for (String[] item : cases) {
            String body = "{\"error\":{\"code\":100,\"message\":\"" + item[0] + " " + SECRET + "\"}}";
            try (SocialMediaHttpClient client = new SocialMediaHttpClient(transport(400, new ByteArrayEntity(bytes(body))))) {
                SocialMediaFeedException error = assertThrows(SocialMediaFeedException.class,
                    () -> client.download(FACEBOOK, SECRET, 4096));
                String field = item.length == 3 ? "; requested field " + item[2] : "";
                assertEquals(item[1] + "; HTTP 400; Facebook code 100" + field, SocialMediaFeedException.safeSummary(error));
                assertFalse(error.toString().contains(SECRET));
            }
        }
    }

    /** HTTP failures remain diagnosable when a proxy returns HTML or Graph fields have unsafe types. */
    @Test
    void retainsHttpStatusForMalformedOrNonNumericErrors() throws Exception {
        for (String body : List.of("<html>" + SECRET + "</html>", "{broken " + SECRET,
            "{\"error\":{\"code\":\"" + SECRET + "\",\"error_subcode\":999999999999999999999999}}")) {
            try (SocialMediaHttpClient client = new SocialMediaHttpClient(transport(403, new ByteArrayEntity(bytes(body))))) {
                SocialMediaFeedException error = assertThrows(SocialMediaFeedException.class,
                    () -> client.download(FACEBOOK, SECRET, 4096));
                String summary = SocialMediaFeedException.safeSummary(error);
                assertTrue(summary.contains("HTTP 403"));
                assertFalse(summary.contains(SECRET));
                assertFalse(summary.contains("Facebook code"));
                assertFalse(summary.contains("Facebook subcode"));
            }
        }
        try (SocialMediaHttpClient client = new SocialMediaHttpClient(transport(429, null))) {
            SocialMediaFeedException error = assertThrows(SocialMediaFeedException.class,
                () -> client.download(URI.create("https://www.youtube.com/feeds/videos.xml"), null, 4096));
            assertEquals("Upstream HTTP request failed; HTTP 429", error.getMessage());
        }
    }

    /** An unknown-length error response is read only up to the diagnostic limit and preserves its HTTP status. */
    @Test
    void boundsErrorBodyReads() throws Exception {
        byte[] body = bytes(SECRET.repeat(3000));
        ByteArrayInputStream input = new ByteArrayInputStream(body);
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(input);
        entity.setContentLength(-1);
        try (SocialMediaHttpClient client = new SocialMediaHttpClient(transport(502, entity))) {
            SocialMediaFeedException error = assertThrows(SocialMediaFeedException.class,
                () -> client.download(FACEBOOK, SECRET, 4096));
            assertEquals("Upstream HTTP request failed; HTTP 502", error.getMessage());
            assertEquals(body.length - 16 * 1024, input.available());
        }
        HttpEntity declaredLarge = mock(HttpEntity.class);
        when(declaredLarge.getContentLength()).thenReturn(16 * 1024L + 1);
        try (SocialMediaHttpClient client = new SocialMediaHttpClient(transport(500, declaredLarge))) {
            SocialMediaFeedException error = assertThrows(SocialMediaFeedException.class,
                () -> client.download(FACEBOOK, SECRET, 4096));
            assertEquals("Upstream HTTP request failed; HTTP 500", error.getMessage());
            verify(declaredLarge, org.mockito.Mockito.never()).getContent();
        }
    }

    /** Graph errors are recognized even at HTTP 200, while normal feed bytes reach the parser unchanged. */
    @Test
    void recognizesGraphErrorsInSuccessfulHttpResponses() throws Exception {
        byte[] errorBody = bytes("{\"error\":{\"code\":100,\"message\":\"" + SECRET + "\"}}");
        try (SocialMediaHttpClient client = new SocialMediaHttpClient(transport(200, new ByteArrayEntity(errorBody)))) {
            SocialMediaFeedException error = assertThrows(SocialMediaFeedException.class,
                () -> client.download(FACEBOOK, SECRET, 4096));
            assertEquals("Facebook API returned an error; HTTP 200; Facebook code 100", error.getMessage());
        }
        byte[] feed = bytes("{\"data\":[]}");
        try (SocialMediaHttpClient client = new SocialMediaHttpClient(transport(200, new ByteArrayEntity(feed)))) {
            assertArrayEquals(feed, client.download(FACEBOOK, SECRET, 4096));
        }
    }

    /** Unexpected exceptions and causes cannot introduce arbitrary upstream strings into a safe summary. */
    @Test
    void summarizesOnlyTrustedExceptions() {
        assertEquals("IOException", SocialMediaFeedException.safeSummary(new IOException(SECRET)));
        SocialMediaFeedException error = new SocialMediaFeedException(Reason.MISSING_FACEBOOK_TOKEN);
        error.initCause(new IOException(SECRET));
        assertEquals("Facebook Page token is not configured", SocialMediaFeedException.safeSummary(error));
        assertFalse(SocialMediaFeedException.safeSummary(new SocialMediaFeedException(Reason.NO_AVAILABLE_POSTS)).contains(SECRET));
    }

    /** A Page token cannot leave Graph API, and invalid resource URLs are rejected before any HTTP call. */
    @Test
    void refusesUnsafeTargetsBeforeSendingCredentials() throws Exception {
        CloseableHttpClient transport = mock(CloseableHttpClient.class);
        SocialMediaHttpClient client = new SocialMediaHttpClient(transport);
        assertThrows(SocialMediaFeedException.class,
            () -> client.download(URI.create("https://www.youtube.com/feeds/videos.xml"), SECRET, 4096));
        assertThrows(SocialMediaFeedException.class,
            () -> client.download(URI.create("https://graph.facebook.com.evil.invalid/feed"), SECRET, 4096));
        verifyNoInteractions(transport);
    }

    /** The production connection factory sends headers but never writes secrets to Apache header or wire logs. */
    @Test
    void keepsCredentialsAndBodiesOutOfDebugLogs() throws Exception {
        Logger headers = (Logger) LoggerFactory.getLogger("org.apache.http.headers");
        Logger wire = (Logger) LoggerFactory.getLogger("org.apache.http.wire");
        Level oldHeaders = headers.getLevel();
        Level oldWire = wire.getLevel();
        ListAppender<ILoggingEvent> log = new ListAppender<>();
        log.start();
        headers.addAppender(log);
        wire.addAppender(log);
        headers.setLevel(Level.DEBUG);
        wire.setLevel(Level.DEBUG);
        try {
            ByteArrayOutputStream sent = new ByteArrayOutputStream();
            byte[] body = bytes(SECRET);
            Socket socket = mock(Socket.class);
            when(socket.getOutputStream()).thenReturn(sent);
            when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(bytes(
                "HTTP/1.1 200 OK\r\nContent-Length: " + body.length + "\r\n\r\n" + SECRET)));
            try (ManagedHttpClientConnection connection = SocialMediaHttpClient.createConnection(null, ConnectionConfig.DEFAULT)) {
                connection.bind(socket);
                BasicHttpRequest request = new BasicHttpRequest("GET", "/feed");
                request.setHeader("Authorization", "Bearer " + SECRET);
                connection.sendRequestHeader(request);
                connection.flush();
                HttpResponse response = connection.receiveResponseHeader();
                connection.receiveResponseEntity(response);
                assertArrayEquals(body, response.getEntity().getContent().readAllBytes());
            }
            assertTrue(sent.toString(StandardCharsets.UTF_8).contains("Bearer " + SECRET));
            assertTrue(log.list.isEmpty(), "The dedicated connection must not emit header or wire events.");
        } finally {
            headers.detachAppender(log);
            wire.detachAppender(log);
            headers.setLevel(oldHeaders);
            wire.setLevel(oldWire);
            log.stop();
        }
    }

    private static CloseableHttpClient transport(int status, HttpEntity entity) throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, status, SECRET));
        when(response.getEntity()).thenReturn(entity);
        CloseableHttpClient transport = mock(CloseableHttpClient.class);
        when(transport.execute(any(HttpUriRequest.class))).thenReturn(response);
        return transport;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
