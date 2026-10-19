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

/** Downloads bounded public feed data and images without forwarding credentials to other hosts. */
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

    /** Keeps bearer tokens and response bodies out of Apache HTTP header and wire debug logs. */
    static ManagedHttpClientConnection createConnection(HttpRoute route, ConnectionConfig config) {
        ConnectionConfig settings = config == null ? ConnectionConfig.DEFAULT : config;
        return new DefaultManagedHttpClientConnection("social-media-" + CONNECTION_IDS.incrementAndGet(), settings.getBufferSize());
    }

    /**
     * Downloads an allowed HTTPS resource, limiting redirects and uncompressed response bytes.
     *
     * @param uri resource URL
     * @param token optional Facebook Page token, sent only to the Graph API
     * @param limit maximum response size in bytes
     * @return downloaded bytes
     * @throws IOException when the URL, response or size is invalid
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

    /** Reads only a small diagnostic body; the enclosing response closes any unread connection. */
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

    /** Extracts numeric Graph identifiers without retaining its message, trace ID or other response data. */
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

    /** Maps recognized Graph errors to fixed guidance without including any upstream text. */
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

    /** Returns a locally defined field name, never text copied from the response. */
    private static FacebookField missingFacebookField(String message) {
        for (FacebookField field : FacebookField.values()) {
            if (message.contains("Tried accessing nonexisting field (" + field.apiName() + ")")) return field;
        }
        return null;
    }

    private static Integer errorNumber(JsonNode value) {
        return value != null && value.isIntegralNumber() && value.canConvertToInt() ? value.intValue() : null;
    }

    /** Bounds decompressed content while leaving stream ownership to the enclosing HTTP response. */
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

    static boolean isImageUrl(URI uri) {
        if (!isHttps(uri)) return false;
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return host.equals("ytimg.com") || host.endsWith(".ytimg.com")
            || host.equals("fbcdn.net") || host.endsWith(".fbcdn.net")
            || host.equals("fbsbx.com") || host.endsWith(".fbsbx.com");
    }

    static boolean isHttps(URI uri) {
        return uri != null && "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
            && uri.getRawUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)
            && uri.getRawFragment() == null;
    }

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
