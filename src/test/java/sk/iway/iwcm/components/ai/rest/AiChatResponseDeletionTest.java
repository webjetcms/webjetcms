package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.webjetcms.ai.AiProviderConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Exercises CMS-owned deletion over HTTP, including provider confirmation and credential handling. */
class AiChatResponseDeletionTest {
    private final AiChatResponseDeletion deletion = new AiChatResponseDeletion();
    private final AtomicReference<HttpExchange> request = new AtomicReference<>();
    private final AtomicInteger calls = new AtomicInteger();
    private HttpServer server;
    private int status = 200;
    private String payload = "";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            request.set(exchange);
            exchange.getResponseHeaders().set("Location", base() + "redirected");
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() throws IOException { server.stop(0); deletion.destroy(); }

    private String base() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta/"; }

    private AiProviderConfig config(String suffix) {
        return AiProviderConfig.builder("secret-key").baseUri(URI.create(base() + suffix))
            .connectTimeout(Duration.ofSeconds(2)).responseTimeout(Duration.ofSeconds(2))
            .allowInsecureHttpForLocalTesting().build();
    }

    /** Uses the same base-URI rules as generation and encodes provider IDs as one path segment. */
    @ParameterizedTest
    @CsvSource({"openai, responses", "gemini, interactions"})
    void deletesWithConfiguredCredentials(String provider, String resource) throws IOException {
        payload = "openai".equals(provider) ? "{\"id\":\"a/b +?\",\"deleted\":true}" : "{}";
        deletion.deleteResponse(provider, "a/b +?", config("gemini".equals(provider) ? "models" : ""));
        assertEquals("DELETE", request.get().getRequestMethod());
        assertEquals("/v1beta/" + resource + "/a%2Fb%20%2B%3F", request.get().getRequestURI().getRawPath());
        String header = "openai".equals(provider) ? "Authorization" : "x-goog-api-key";
        assertEquals("openai".equals(provider) ? "Bearer secret-key" : "secret-key", request.get().getRequestHeaders().getFirst(header));
    }

    /** A missing response is successful only when the provider identifies the missing resource. */
    @ParameterizedTest
    @CsvSource({"openai, response", "gemini, interaction"})
    void acceptsConfirmedAbsenceButRejectsGeneric404(String provider, String resource) throws IOException {
        status = 404;
        payload = "{\"error\":{\"message\":\"The " + resource + " does not exist\"}}";
        deletion.deleteResponse(provider, "response-id", config(""));
        payload = "{\"error\":{\"message\":\"Route not found\"}}";
        assertThrows(IOException.class, () -> deletion.deleteResponse(provider, "response-id", config("")));
    }

    /** Unconfirmed deletion fails, and redirects cannot forward provider credentials. */
    @Test
    void rejectsUnconfirmedDeletionAndRedirects() {
        payload = "{}";
        assertThrows(IOException.class, () -> deletion.deleteResponse("openai", "response-id", config("")));
        status = 302;
        int before = calls.get();
        assertThrows(IOException.class, () -> deletion.deleteResponse("openai", "response-id", config("")));
        assertEquals(before + 1, calls.get());
    }
}
