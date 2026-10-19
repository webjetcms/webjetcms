package sk.iway.aceintegration.socialmedia;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultManagedHttpClientConnection;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import sk.iway.aceintegration.socialmedia.SocialMediaFeedException.Reason;
import sk.iway.aceintegration.socialmedia.SocialMediaFeedException.FacebookField;

/**
 * Provides the HTTP transport used by the central social-media feed generator.
 *
 * <p>The default client accepts HTTPS requests to the feed hosts and approved image CDN domains,
 * limits response bytes after decompression, and follows redirects only for images. Facebook
 * Page tokens are sent in the authorization header exclusively to the Graph API host.</p>
 *
 * <p>Connections use timeouts, address checks and a connection implementation without Apache
 * header/wire debug logging. Graph API errors are reduced to locally defined diagnostics rather
 * than exposing response messages or tokens. Create one client per run and close it afterwards.</p>
 */
final class SocialMediaHttpClient implements SocialMediaFeedGenerator.Downloader, AutoCloseable {
    private static final Set<String> FEED_HOSTS = Set.of("www.youtube.com", "graph.facebook.com");
    private static final int MAX_ERROR_BYTES = 16 * 1024;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AtomicLong CONNECTION_IDS = new AtomicLong();
    private final CloseableHttpClient client;

    SocialMediaHttpClient() {
        this(HttpClients.custom()
            .useSystemProperties()
            .disableRedirectHandling()
            .disableAutomaticRetries()
            .setConnectionManager(new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", SSLConnectionSocketFactory.getSystemSocketFactory()).build(),
                SocialMediaHttpClient::createConnection, SocialMediaHttpClient::resolvePublicAddresses))
            .setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(10_000).setConnectionRequestTimeout(10_000).setSocketTimeout(20_000).build())
            .build());
    }

    SocialMediaHttpClient(CloseableHttpClient client) {
        this.client = client;
    }

    /**
     * Creates a connection without the header and wire loggers used by Apache's logging wrapper.
     *
     * @param route route supplied by the connection manager; not needed to construct this connection
     * @param config connection settings, or {@code null} to use the default buffer size
     * @return connection with a unique local identifier and no header/wire debug logging
     */
    static ManagedHttpClientConnection createConnection(HttpRoute route, ConnectionConfig config) {
        ConnectionConfig settings = config == null ? ConnectionConfig.DEFAULT : config;
        return new DefaultManagedHttpClientConnection("social-media-" + CONNECTION_IDS.incrementAndGet(), settings.getBufferSize());
    }

    /**
     * Downloads an allowed HTTPS resource, limiting redirects and uncompressed response bytes.
     *
     * <p>Feed requests must complete without redirects. Images may follow at most three redirects,
     * and each destination must still be an approved image URL. A Graph error object is treated
     * as a failure even when the HTTP status is 200. Each response is closed before returning,
     * following a redirect or propagating an error.</p>
     *
     * @param uri resource URL on a supported feed or image host
     * @param token Facebook Page token, or {@code null} for an unauthenticated request
     * @param limit nonnegative maximum decompressed response size in bytes
     * @return complete successful response body
     * @throws IOException if transport fails, a URL or token destination is rejected, the response
     *     is unsuccessful or oversized, or an image exceeds the redirect limit
     */
    @Override
    public byte[] download(URI uri, String token, int limit) throws IOException {
        boolean image = isImageUrl(uri);
        for (int redirects = 0; redirects <= 3; redirects++) {
            if (!isHttps(uri) || !(image ? isImageUrl(uri) : FEED_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT)))) {
                throw new SocialMediaFeedException(Reason.UNAPPROVED_RESOURCE);
            }
            if (token != null && !"graph.facebook.com".equalsIgnoreCase(uri.getHost())) {
                throw new SocialMediaFeedException(Reason.TOKEN_HOST_MISMATCH);
            }
            HttpGet request = new HttpGet(uri);
            request.setHeader("User-Agent", "WebJET-SocialMediaFeed/1.0");
            if (token != null) request.setHeader("Authorization", "Bearer " + token);
            try (CloseableHttpResponse response = client.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                if (image && status >= 300 && status < 400 && response.getFirstHeader("Location") != null) {
                    uri = uri.resolve(response.getFirstHeader("Location").getValue());
                    continue;
                }
                HttpEntity entity = response.getEntity();
                boolean facebook = "graph.facebook.com".equalsIgnoreCase(uri.getHost());
                if (status != 200) {
                    throw facebook ? facebookFailure(entity, status)
                        : new SocialMediaFeedException(Reason.HTTP_ERROR, status, null, null);
                }
                byte[] bytes = readResponse(entity, limit);
                if (facebook) {
                    SocialMediaFeedException failure = facebookFailure(bytes, status);
                    if (failure != null) throw failure;
                }
                return bytes;
            }
        }
        throw new SocialMediaFeedException(Reason.TOO_MANY_REDIRECTS);
    }

    /**
     * Builds a safe exception for a non-200 Graph response using a bounded diagnostic body.
     *
     * <p>Missing, oversized, unreadable or malformed bodies fall back to the HTTP status alone.
     * The enclosing response retains ownership of the entity stream and closes unread content.</p>
     *
     * @param entity response entity, possibly {@code null} or of unknown length
     * @param status upstream HTTP status code
     * @return Graph-specific diagnostic when recognized, otherwise a generic HTTP failure
     */
    private static SocialMediaFeedException facebookFailure(HttpEntity entity, int status) {
        if (entity != null && entity.getContentLength() <= MAX_ERROR_BYTES) {
            try {
                byte[] bytes = entity.getContent().readNBytes(MAX_ERROR_BYTES);
                if (bytes.length < MAX_ERROR_BYTES) {
                    SocialMediaFeedException failure = facebookFailure(bytes, status);
                    if (failure != null) return failure;
                }
            } catch (IOException exception) {
                // The HTTP status is still useful when the optional error body cannot be read.
            }
        }
        return new SocialMediaFeedException(Reason.HTTP_ERROR, status, null, null);
    }

    /**
     * Detects a Graph error object and extracts only safe diagnostic identifiers.
     *
     * <p>For Graph code 100, the upstream message is inspected for known patterns but is never
     * included verbatim. Numeric codes, a fixed reason and an allowlisted field name are the
     * only details retained in the exception.</p>
     *
     * @param bytes response body already bounded by the caller
     * @param status upstream HTTP status, including 200 for successful HTTP responses containing an error
     * @return safe failure, or {@code null} when no error object can be parsed
     */
    private static SocialMediaFeedException facebookFailure(byte[] bytes, int status) {
        try {
            JsonNode root = MAPPER.readTree(bytes);
            JsonNode error = root == null ? null : root.get("error");
            if (error != null && error.isObject()) {
                Integer code = errorNumber(error.get("code"));
                String message = Integer.valueOf(100).equals(code) ? error.path("message").asText("") : "";
                return new SocialMediaFeedException(facebookReason(message), status,
                    code, errorNumber(error.get("error_subcode")), missingFacebookField(message));
            }
        } catch (IOException exception) {
            // The feed parser reports malformed successful responses without exposing their contents.
        }
        return null;
    }

    /**
     * Maps known Graph message fragments to fixed configuration or field-error guidance.
     *
     * @param message non-null upstream message to inspect, or an empty string when inspection is not applicable
     * @return matching predefined reason, or the generic Graph API error reason
     */
    private static Reason facebookReason(String message) {
        if (message.contains("Tried accessing nonexisting field")) {
            if (message.contains("(published_posts)") && message.contains("node type (User)")) {
                return Reason.FACEBOOK_OBJECT_IS_USER;
            }
            return Reason.NONEXISTENT_FACEBOOK_FIELD;
        }
        if (message.contains("Unsupported get request")) return Reason.FACEBOOK_OBJECT_UNAVAILABLE;
        return Reason.FACEBOOK_API_ERROR;
    }

    /**
     * Matches a missing-field diagnostic against the fields requested by this integration.
     *
     * @param message non-null upstream error message
     * @return allowlisted field identifier, or {@code null} for an unknown field or message pattern
     */
    private static FacebookField missingFacebookField(String message) {
        for (FacebookField field : FacebookField.values()) {
            if (message.contains("Tried accessing nonexisting field (" + field.apiName() + ")")) return field;
        }
        return null;
    }

    private static Integer errorNumber(JsonNode value) {
        return value != null && value.isIntegralNumber() && value.canConvertToInt() ? value.intValue() : null;
    }

    /**
     * Reads a complete response within the byte budget, including bodies without a declared length.
     *
     * <p>An excessive declared length is rejected before reading. When the budget is filled,
     * one additional byte is probed to distinguish an exact fit from an oversized response.
     * The enclosing HTTP response owns and closes the stream.</p>
     *
     * @param entity response entity, possibly {@code null}; decompression is handled by the HTTP client
     * @param limit nonnegative maximum body size in bytes
     * @return complete response bytes when they fit within the limit
     * @throws IOException if the entity is missing, the limit is negative, reading fails or the body is oversized
     */
    private static byte[] readResponse(HttpEntity entity, int limit) throws IOException {
        if (entity == null || limit < 0 || entity.getContentLength() > limit) {
            throw new SocialMediaFeedException(Reason.INVALID_RESPONSE_SIZE);
        }
        InputStream input = entity.getContent();
        byte[] bytes = input.readNBytes(limit);
        if (bytes.length == limit && input.read() != -1) {
            throw new SocialMediaFeedException(Reason.RESPONSE_TOO_LARGE);
        }
        return bytes;
    }

    /**
     * Checks the HTTPS URL form and the YouTube/Facebook image CDN host allowlist.
     *
     * @param uri candidate image URI, possibly {@code null}
     * @return whether the URI uses an approved CDN domain or subdomain; no DNS lookup is performed
     */
    static boolean isImageUrl(URI uri) {
        if (!isHttps(uri)) return false;
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return host.equals("ytimg.com") || host.endsWith(".ytimg.com")
            || host.equals("fbcdn.net") || host.endsWith(".fbcdn.net")
            || host.equals("fbsbx.com") || host.endsWith(".fbsbx.com");
    }

    /**
     * Checks the common URL restrictions used for upstream resources and public provider links.
     *
     * @param uri candidate URI, possibly {@code null}
     * @return whether it has an HTTPS scheme and host, no user information or fragment, and no nonstandard port
     */
    static boolean isHttps(URI uri) {
        return uri != null && "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
            && uri.getRawUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)
            && uri.getRawFragment() == null;
    }

    /**
     * Resolves a connection host and rejects the entire result if any address is in a blocked range.
     *
     * <p>Checks cover local, loopback, link-local, site-local and multicast addresses, IPv6 unique
     * local addresses, IPv4 shared address space and the explicitly blocked reserved IPv4 ranges.</p>
     *
     * @param host hostname supplied by the connection manager
     * @return resolved addresses when all pass the address checks
     * @throws UnknownHostException if DNS resolution fails or any resolved address is blocked
     */
    private static InetAddress[] resolvePublicAddresses(String host) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(host);
        for (InetAddress address : addresses) {
            byte[] bytes = address.getAddress();
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()
                || (bytes.length == 4 && ((bytes[0] & 255) == 0 || (bytes[0] & 255) >= 240
                    || ((bytes[0] & 255) == 100 && (bytes[1] & 255) >= 64 && (bytes[1] & 255) <= 127)))
                || (bytes.length == 16 && (bytes[0] & 254) == 252)) {
                throw new UnknownHostException("Non-public address for an upstream host");
            }
        }
        return addresses;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
