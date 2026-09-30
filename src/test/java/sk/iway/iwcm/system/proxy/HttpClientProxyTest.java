package sk.iway.iwcm.system.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.provider.openai.OpenAiProvider;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.components.ai.providers.openai.OpenAiService;
import sk.iway.iwcm.components.offline.OfflineService;
import sk.iway.iwcm.components.proxy.ProxyByHttpClient4;
import sk.iway.iwcm.components.proxy.jpa.ProxyBean;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@Timeout(15)
class HttpClientProxyTest {

    enum Client { TOOLS_GET, TOOLS_POST, AI_MODELS, PROXY_MODULE, OFFLINE }

    private static final String HOST = "127.0.0.1";
    private static final String DIRECT_BODY = "direct";
    private static final String PROXY_BODY = "proxy";

    private final Map<String, String> originalProperties = new HashMap<>();
    private final Map<String, String> originalConstants = new HashMap<>();
    private final AtomicInteger proxyRequests = new AtomicInteger();
    private final AtomicReference<String> lastMethod = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastProxyAuthorization = new AtomicReference<>();
    private final AtomicReference<String> lastTargetAuthorization = new AtomicReference<>();
    private ProxySelector originalSelector;
    private Authenticator originalAuthenticator;
    private SSLSocketFactory originalSslSocketFactory;
    private HttpServer target;
    private HttpServer proxy;
    private volatile boolean requireProxyAuth;
    private volatile boolean requireTargetAuth;

    @BeforeEach
    void setUp() throws IOException {
        originalSelector = ProxySelector.getDefault();
        originalAuthenticator = Authenticator.getDefault();
        originalSslSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        for (String key : List.of("http.proxyHost", "http.proxyPort", "https.proxyHost", "https.proxyPort", "http.nonProxyHosts", "http.proxySet")) {
            originalProperties.put(key, System.getProperty(key));
        }
        for (String key : List.of("proxyUser", "proxyPassword", "natUrlTranslate")) {
            originalConstants.put(key, Constants.getString(key));
            Constants.setString(key, "");
        }
        ProxySelector.setDefault(null);
        Authenticator.setDefault(null);
        target = HttpServer.create(new InetSocketAddress(HOST, 0), 0);
        target.createContext("/", exchange -> respond(exchange, false));
        target.start();
        proxy = HttpServer.create(new InetSocketAddress(HOST, 0), 0);
        proxy.createContext("/", exchange -> respond(exchange, true));
        proxy.start();
    }

    @AfterEach
    void tearDown() {
        if (target != null) target.stop(0);
        if (proxy != null) proxy.stop(0);
        ProxySelector.setDefault(originalSelector);
        Authenticator.setDefault(originalAuthenticator);
        HttpsURLConnection.setDefaultSSLSocketFactory(originalSslSocketFactory);
        originalProperties.forEach((key, value) -> {
            if (value == null) System.clearProperty(key);
            else System.setProperty(key, value);
        });
        originalConstants.forEach(Constants::setString);
    }

    @ParameterizedTest
    @EnumSource(Client.class)
    void usesConfiguredProxy(Client client) throws Exception {
        configureProxy("");
        assertEquals(PROXY_BODY, download(client));
        assertEquals(1, proxyRequests.get());
    }

    @ParameterizedTest
    @EnumSource(Client.class)
    void bypassesProxyForExcludedHost(Client client) throws Exception {
        configureProxy(HOST);
        assertEquals(DIRECT_BODY, download(client));
        assertEquals(0, proxyRequests.get());
    }

    @ParameterizedTest
    @EnumSource(Client.class)
    void connectsDirectlyWithoutProxy(Client client) throws Exception {
        assertEquals(DIRECT_BODY, download(client));
        assertEquals(0, proxyRequests.get());
    }

    @ParameterizedTest
    @EnumSource(Client.class)
    void authenticatesUsingWebjetProxyCredentials(Client client) throws Exception {
        requireProxyAuth = true;
        configureProxy("");
        assertEquals(PROXY_BODY, download(client));
        assertEquals(basic("proxy-user:proxy-password"), lastProxyAuthorization.get());
    }

    @Test
    void keepsTargetAndProxyCredentialsSeparate() throws Exception {
        requireProxyAuth = true;
        requireTargetAuth = true;
        configureProxy("");
        assertEquals(PROXY_BODY, download(Client.PROXY_MODULE));
        assertEquals(basic("proxy-user:proxy-password"), lastProxyAuthorization.get());
        assertEquals(basic("target-user:target-password"), lastTargetAuthorization.get());
    }

    private void configureProxy(String exceptions) {
        Constants.setString("proxyUser", "proxy-user");
        Constants.setString("proxyPassword", "proxy-password");
        int port = proxy.getAddress().getPort();
        WebJETProxySelector.initProxy(HOST, port, "proxy-user", "proxy-password", exceptions, HOST, port);
        WebJETProxySelector.setAuthenticator();
    }

    private String download(Client client) throws Exception {
        String url = "http://" + HOST + ":" + target.getAddress().getPort() + "/file.json";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/local/file.json");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContentType("application/x-www-form-urlencoded");
        request.setUserPrincipal(() -> "test-user");
        switch (client) {
            case TOOLS_GET:
                return Tools.downloadUrl(url, request);
            case TOOLS_POST:
                request.setMethod("POST");
                request.addParameter("text", "test value");
                String response = Tools.downloadUrl(url, request);
                assertEquals("POST", lastMethod.get());
                assertEquals("text=test+value", lastBody.get());
                return response;
            case AI_MODELS:
                WebjetAiConfigurationService configuration = mock(WebjetAiConfigurationService.class);
                AiProviderConfig providerConfig = AiProviderConfig.builder("test-api-key")
                    .baseUri(URI.create("http://" + HOST + ":" + target.getAddress().getPort() + "/"))
                    .allowInsecureHttpForLocalTesting()
                    .build();
                try (AiClient aiClient = AiClient.of(new OpenAiProvider())) {
                    OpenAiService service = new OpenAiService(aiClient, configuration);
                    when(configuration.resolve(service, request)).thenReturn(providerConfig);
                    List<LabelValue> models = service.getSupportedModels(null, request);
                    assertEquals(1, models.size());
                    return models.get(0).getLabel();
                }
            case PROXY_MODULE:
                ProxyBean mapping = new ProxyBean();
                mapping.setLocalUrl("/local/");
                mapping.setRemoteUrl("/");
                mapping.setRemoteServer("http://" + HOST);
                mapping.setRemotePort(target.getAddress().getPort());
                mapping.setIncludeExt("");
                if (requireTargetAuth) {
                    mapping.setAuthMethod("basic");
                    mapping.setAuthUsername("target-user");
                    mapping.setAuthPassword("target-password");
                }
                MockHttpServletResponse servletResponse = new MockHttpServletResponse();
                ProxyByHttpClient4.service(mapping, request, servletResponse);
                assertEquals(200, servletResponse.getStatus());
                return servletResponse.getContentAsString();
            case OFFLINE:
                return OfflineService.downloadUrl(url, "test-context-key", request);
            default:
                throw new IllegalArgumentException("Unexpected client: " + client);
        }
    }

    private void respond(HttpExchange exchange, boolean viaProxy) throws IOException {
        try {
            lastMethod.set(exchange.getRequestMethod());
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (viaProxy) {
                proxyRequests.incrementAndGet();
                String proxyAuthorization = exchange.getRequestHeaders().getFirst("Proxy-Authorization");
                String targetAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
                lastProxyAuthorization.set(proxyAuthorization);
                lastTargetAuthorization.set(targetAuthorization);
                if (requireProxyAuth && basic("proxy-user:proxy-password").equals(proxyAuthorization) == false) {
                    exchange.getResponseHeaders().add("Proxy-Authenticate", "Basic realm=\"proxy\"");
                    exchange.sendResponseHeaders(407, -1);
                    return;
                }
                if (requireTargetAuth && basic("target-user:target-password").equals(targetAuthorization) == false) {
                    exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"target\"");
                    exchange.sendResponseHeaders(401, -1);
                    return;
                }
            }
            String responseBody = viaProxy ? PROXY_BODY : DIRECT_BODY;
            String contentType = "text/plain; charset=UTF-8";
            if ("/models".equals(exchange.getRequestURI().getPath())) {
                responseBody = "{\"data\":[{\"id\":\"" + responseBody + "\"}]}";
                contentType = "application/json; charset=UTF-8";
            }
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        } finally {
            exchange.close();
        }
    }

    private static String basic(String credentials) {
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
