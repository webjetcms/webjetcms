package sk.iway.iwcm.system.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests proxy exception matching and routing without opening network connections. */
class WebJETProxySelectorTest {

    private static final String PROXY_HOST = "127.0.0.2";
    private static final String HTTPS_PROXY_HOST = "127.0.0.3";
    private final Map<String, String> originalProperties = new HashMap<>();
    private ProxySelector originalSelector;

    @BeforeEach
    void setUp() {
        originalSelector = ProxySelector.getDefault();
        for (String key : List.of("http.proxyHost", "http.proxyPort", "https.proxyHost", "https.proxyPort", "http.nonProxyHosts", "http.proxySet")) {
            originalProperties.put(key, System.getProperty(key));
            System.clearProperty(key);
        }
        ProxySelector.setDefault(null);
    }

    @AfterEach
    void tearDown() {
        ProxySelector.setDefault(originalSelector);
        originalProperties.forEach((key, value) -> {
            if (value == null) System.clearProperty(key);
            else System.setProperty(key, value);
        });
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
        "*.firma.sk; api.firma.sk; true",
        "*.firma.sk; firma.sk; false",
        "*.firma.sk; api.firma.sk.evil.sk; false",
        "*.firma.sk; deep.api.firma.sk; true",
        "127.*; 127.0.0.1; true",
        "127.*; 128.0.0.1; false",
        "API.FIRMA.SK; api.firma.sk; true",
        "api.firma.sk; API.FIRMA.SK; true",
        "api.firma.sk; sub.api.firma.sk; false",
        "api.firma.sk; api.firma.sk.evil.sk; false",
        "api.firma.sk; apiXfirmaXsk; false",
        "*; anything.example; true",
        "**; anything.example; true",
        "***; anything.example; false",
        "*firma*; api.firma.sk; true",
        "*firma*; firma; true",
        "*firma*; api.example.sk; false",
        "api*service.sk; api-service.sk; false",
        "*api*service*; api-service.sk; false",
        "api**; api-service.sk; false",
        "**firma.sk; api.firma.sk; false",
        "api.*; api.firma.sk; true",
        "api.+; api.firma.sk; false",
        "api.?; api.x; false",
        "[ab].firma.sk; a.firma.sk; false",
        "(api).firma.sk; api.firma.sk; false",
        "^api.firma.sk$; api.firma.sk; false",
        "[::1]; [::1]; true",
        "[2001:DB8::1]; [2001:db8::1]; true",
        "[2001:db8::*; [2001:db8::2]; true",
        "[::1]; [::2]; false",
        "localhost,*.firma.sk; api.firma.sk; true",
        "localhost|*.firma.sk; api.firma.sk; true",
        "' , localhost, | *.firma.sk || 127.* , '; api.firma.sk; true",
        "' , localhost, | *.firma.sk || 127.* , '; 127.0.0.1; true",
        "' , localhost, | *.firma.sk || 127.* , '; external.sk; false",
        "''; localhost; false",
        "''; 127.0.0.1; false",
        "''; [::1]; false",
        "' , | , '; 127.0.0.1; false"
    })
    void matchesWholeHostForHttpAndHttps(String exceptions, String host, boolean direct) {
        ProxySelector selector = configure(exceptions, HTTPS_PROXY_HOST, 8443);
        for (String scheme : List.of("http", "https")) {
            Proxy expected = direct ? Proxy.NO_PROXY : proxyFor(scheme);
            for (String suffix : List.of("/", ":8181/path/api.firma.sk?host=localhost")) {
                assertEquals(List.of(expected), selector.select(URI.create(scheme + "://" + host + suffix)));
            }
        }
    }

    @Test
    void publishesNormalizedExceptions() {
        configure(" , localhost, | *.FIRMA.sk || 127.* , ", HTTPS_PROXY_HOST, 8443);
        assertEquals("localhost|*.FIRMA.sk|127.*", System.getProperty("http.nonProxyHosts"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", " , | , \t "})
    void emptyExceptionsPreserveJvmPropertyButDoNotAffectSelector(String exceptions) {
        System.setProperty("http.nonProxyHosts", "*");
        ProxySelector selector = configure(exceptions, HTTPS_PROXY_HOST, 8443);
        assertEquals("*", System.getProperty("http.nonProxyHosts"));
        assertEquals(List.of(proxyFor("http")), selector.select(URI.create("http://127.0.0.1/")));
        assertEquals(List.of(proxyFor("https")), selector.select(URI.create("https://localhost/")));
    }

    @Test
    void emptyExceptionsLeaveAbsentJvmPropertyUnset() {
        configure("", HTTPS_PROXY_HOST, 8443);
        assertEquals(null, System.getProperty("http.nonProxyHosts"));
    }

    @ParameterizedTest
    @CsvSource({"'', 8443", "127.0.0.3, 0", "'', 0"})
    void httpsFallsBackToHttpProxy(String httpsHost, int httpsPort) {
        ProxySelector selector = configure("*.firma.sk", httpsHost, httpsPort);
        assertEquals(List.of(proxyFor("http")), selector.select(URI.create("https://external.sk/")));
        assertEquals(List.of(Proxy.NO_PROXY), selector.select(URI.create("https://api.firma.sk/")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://api.firma.sk/file", "socket://api.firma.sk:1234"})
    void delegatesOtherProtocolsEvenWithWildcardException(String url) {
        ProxySelector delegate = mock(ProxySelector.class);
        URI uri = URI.create(url);
        List<Proxy> expected = List.of(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(PROXY_HOST, 1080)));
        when(delegate.select(uri)).thenReturn(expected);
        ProxySelector.setDefault(delegate);
        assertEquals(expected, configure("*", HTTPS_PROXY_HOST, 8443).select(uri));
        verify(delegate).select(uri);
    }

    @ParameterizedTest
    @ValueSource(strings = {"socket://api.firma.sk:80", "socket://api.firma.sk:443"})
    void preservesDirectRoutingForOtherProtocolsOnHttpPorts(String url) {
        ProxySelector delegate = mock(ProxySelector.class);
        ProxySelector.setDefault(delegate);
        assertEquals(List.of(Proxy.NO_PROXY), configure("", HTTPS_PROXY_HOST, 8443).select(URI.create(url)));
        verifyNoInteractions(delegate);
    }

    @Test
    void handlesMissingDelegateAndRejectsNullUri() {
        ProxySelector selector = configure("*", HTTPS_PROXY_HOST, 8443);
        assertEquals(List.of(Proxy.NO_PROXY), selector.select(URI.create("ftp://external.sk/file")));
        assertThrows(IllegalArgumentException.class, () -> selector.select(null));
        assertEquals(List.of(proxyFor("http")), selector.select(URI.create("http:/missing-host")));
    }

    private ProxySelector configure(String exceptions, String httpsHost, int httpsPort) {
        WebJETProxySelector.initProxy(PROXY_HOST, 8080, "", "", exceptions, httpsHost, httpsPort);
        return ProxySelector.getDefault();
    }

    private Proxy proxyFor(String scheme) {
        if ("https".equals(scheme)) return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(HTTPS_PROXY_HOST, 8443));
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_HOST, 8080));
    }
}
