package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Verifies protocol-specific ports in absolute URLs used by requests and background jobs.
 */
class ToolsGetBaseHrefTest {

    private MockedStatic<Constants> constants;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        constants = mockStatic(Constants.class, CALLS_REAL_METHODS);
        constants.when(() -> Constants.getString("httpServerName")).thenReturn("");
        constants.when(() -> Constants.getInt("httpServerPort")).thenReturn(8080);
        constants.when(() -> Constants.getInt("httpsServerPort")).thenReturn(8443);
        constants.when(() -> Constants.getBoolean("serverBeyoundProxy")).thenReturn(false);

        request = new MockHttpServletRequest();
        request.setServerName("cms.example.test");
        request.setContextPath("/webjet");
    }

    @AfterEach
    void tearDown() {
        constants.close();
    }

    @ParameterizedTest
    @CsvSource({
        "false, 80,   8443, 8081, http://cms.example.test/webjet",
        "true,  8080, 443,  8444, https://cms.example.test/webjet",
        "false, 8080, 8443, 8081, http://cms.example.test:8080/webjet",
        "true,  8080, 8443, 8444, https://cms.example.test:8443/webjet",
        "false, 443,  8443, 8081, http://cms.example.test:443/webjet",
        "true,  8080, 80,   8444, https://cms.example.test:80/webjet",
        "false, -1,   8443, 8081, http://cms.example.test:8081/webjet",
        "true,  8080, -1,   8444, https://cms.example.test:8444/webjet",
        "false, -1,   8443, 80,   http://cms.example.test/webjet",
        "true,  8080, -1,   443,  https://cms.example.test/webjet",
        "false, -1,   8443, 443,  http://cms.example.test:443/webjet",
        "true,  8080, -1,   80,   https://cms.example.test:80/webjet"
    })
    void selectsPortForRequestProtocol(boolean secure, int httpPort, int httpsPort, int requestPort, String expected) {
        constants.when(() -> Constants.getInt("httpServerPort")).thenReturn(httpPort);
        constants.when(() -> Constants.getInt("httpsServerPort")).thenReturn(httpsPort);
        request.setSecure(secure);
        request.setServerPort(requestPort);

        assertEquals(expected, Tools.getBaseHref(request));
    }

    @Test
    void usesHttpsPortBehindProxy() {
        constants.when(() -> Constants.getBoolean("serverBeyoundProxy")).thenReturn(true);
        constants.when(() -> Constants.getString("requestIsSecureHeaderName")).thenReturn("x-forwarded-proto");
        request.addHeader("x-forwarded-proto", "https");
        request.setServerPort(8080);

        assertEquals("https://cms.example.test:8443/webjet", Tools.getBaseHref(request));
    }

    @ParameterizedTest
    @CsvSource({
        "http://public.example.test,  true,  http://public.example.test:8080",
        "https://public.example.test, false, https://public.example.test:8443"
    })
    void selectsPortForConfiguredUrlProtocol(String serverName, boolean secure, String expected) {
        constants.when(() -> Constants.getString("httpServerName")).thenReturn(serverName);
        request.setSecure(secure);

        assertEquals(expected, Tools.getBaseHref(request));
    }

    @ParameterizedTest
    @CsvSource({
        "public.example.test,         8080, 8443, http://public.example.test:8080",
        "http://public.example.test,  8080, 8443, http://public.example.test:8080",
        "https://public.example.test, 8080, 8443, https://public.example.test:8443",
        "http://public.example.test,  80,   8443, http://public.example.test",
        "https://public.example.test, 8080, 443,  https://public.example.test"
    })
    void selectsPortWithoutRequest(String serverName, int httpPort, int httpsPort, String expected) {
        constants.when(() -> Constants.getString("httpServerName")).thenReturn(serverName);
        constants.when(() -> Constants.getInt("httpServerPort")).thenReturn(httpPort);
        constants.when(() -> Constants.getInt("httpsServerPort")).thenReturn(httpsPort);

        assertEquals(expected, Tools.getBaseHref(null));
    }
}
