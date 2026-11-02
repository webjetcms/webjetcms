package sk.iway.iwcm.components.ai.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjetcms.ai.AiProviderConfig;

/** Deletes saved provider responses without requiring deletion support in the AI library. */
@Service
public class AiChatResponseDeletion implements DisposableBean {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final CloseableHttpClient client = HttpClients.custom().useSystemProperties()
        .disableAutomaticRetries().disableRedirectHandling().disableCookieManagement().build();

    /** Returns only after deletion or a provider-confirmed missing response; failures retain local history. */
    public void deleteResponse(String provider, String responseId, AiProviderConfig config) throws IOException {
        if (config == null || !config.isConfigured() || responseId == null || responseId.isBlank()
            || ".".equals(responseId) || "..".equals(responseId)) {
            throw new IOException("Missing chat deletion configuration or response ID");
        }
        HttpDelete request = new HttpDelete(endpoint(provider, responseId, config));
        request.setConfig(RequestConfig.custom().setConnectTimeout(config.connectTimeoutMillis())
            .setConnectionRequestTimeout(config.connectTimeoutMillis()).setSocketTimeout(config.responseTimeoutMillis()).build());
        config.trustedHeaders().forEach(request::setHeader);
        request.setHeader("Accept", "application/json");
        if ("openai".equals(provider)) request.setHeader("Authorization", "Bearer " + config.apiKey());
        else request.setHeader("x-goog-api-key", config.apiKey());
        try (var response = client.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            String payload = "";
            if (response.getEntity() != null) {
                try (var input = response.getEntity().getContent()) {
                    byte[] bytes = input.readNBytes(65537);
                    if (bytes.length > 65536) throw new IOException("Oversized chat deletion response");
                    payload = new String(bytes, StandardCharsets.UTF_8);
                }
            }
            JsonNode body = payload.isBlank() ? MAPPER.createObjectNode() : MAPPER.readTree(payload);
            if (body != null && status == 404 && missing(provider, body)) return;
            if (body != null && (status == 200 || status == 204)) {
                if ("openai".equals(provider)) {
                    if (body.path("deleted").asBoolean() && responseId.equals(body.path("id").asText())) return;
                } else if (body.isObject() && body.isEmpty()) return;
            }
            throw new IOException("Provider did not confirm chat deletion (HTTP " + status + ")");
        } catch (IOException | RuntimeException exception) {
            // Provider bodies, URLs and transport exception details may contain credentials.
            throw new IOException("Provider did not confirm chat deletion");
        }
    }

    private static URI endpoint(String provider, String responseId, AiProviderConfig config) throws IOException {
        String fallback = switch (provider) {
            case "openai" -> "https://api.openai.com/v1/";
            case "gemini" -> "https://generativelanguage.googleapis.com/v1beta/";
            default -> throw new IOException("Unsupported chat deletion provider");
        };
        String base = config.baseUri() == null ? fallback : config.baseUri().toString();
        if (!base.endsWith("/")) base += "/";
        URI directory = URI.create(base);
        if ("gemini".equals(provider) && directory.getPath().endsWith("/models/")) directory = directory.resolve("../");
        String resource = "openai".equals(provider) ? "responses/" : "interactions/";
        return directory.resolve(resource + URLEncoder.encode(responseId, StandardCharsets.UTF_8).replace("+", "%20"));
    }

    private static boolean missing(String provider, JsonNode body) {
        JsonNode error = body.path("error");
        if ("openai".equals(provider) && "response_not_found".equals(error.path("code").asText())) return true;
        String message = error.path("message").asText().toLowerCase(Locale.ROOT);
        String resource = "openai".equals(provider) ? "response" : "interaction";
        return message.contains(resource) && (message.contains("not found") || message.contains("does not exist"));
    }

    @Override
    public void destroy() throws IOException { client.close(); }
}
