package sk.iway.aceintegration.socialmedia;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/** Downloads bounded public feed data and images without forwarding credentials to other hosts. */
final class SocialMediaHttpClient implements SocialMediaFeedGenerator.Downloader, AutoCloseable {
    private static final Set<String> FEED_HOSTS = Set.of("www.youtube.com", "graph.facebook.com");
    private final CloseableHttpClient client = HttpClients.custom()
        .useSystemProperties()
        .disableRedirectHandling()
        .disableAutomaticRetries()
        .setDnsResolver(SocialMediaHttpClient::resolvePublicAddresses)
        .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectTimeout(10_000).setConnectionRequestTimeout(10_000).setSocketTimeout(20_000).build())
        .build();

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
                throw new IOException("Unapproved feed resource host");
            }
            if (token != null && !"graph.facebook.com".equalsIgnoreCase(uri.getHost())) {
                throw new IOException("Refusing to send a Page token to another host");
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
                if (status != 200) throw new IOException("Upstream HTTP status " + status);
                HttpEntity entity = response.getEntity();
                if (entity == null || entity.getContentLength() > limit) throw new IOException("Invalid response size");
                try (InputStream input = entity.getContent(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (bytes.size() + count > limit) throw new IOException("Response exceeds size limit");
                        bytes.write(buffer, 0, count);
                    }
                    return bytes.toByteArray();
                }
            }
        }
        throw new IOException("Too many image redirects");
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
