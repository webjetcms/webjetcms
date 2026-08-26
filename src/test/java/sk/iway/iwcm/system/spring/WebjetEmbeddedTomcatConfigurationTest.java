package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.mock.env.MockEnvironment;

class WebjetEmbeddedTomcatConfigurationTest {

    @TempDir
    Path tomcatBase;

    @Test
    void httpConnectorRedirectsAllRequestsToTheConfiguredHttpsPort() {
        TomcatServerProperties tomcatProperties = new TomcatServerProperties();
        tomcatProperties.setMaxPartCount(1_000);
        MockEnvironment environment = new MockEnvironment()
            .withProperty(WebjetEmbeddedTomcatConfiguration.HTTP_REDIRECT_PORT_PROPERTY, "8080");
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(8443);
        factory.setUriEncoding(StandardCharsets.ISO_8859_1);
        factory.setAddress(InetAddress.getLoopbackAddress());
        factory.setServerHeader("WebJET redirect test");
        factory.addProtocolHandlerCustomizers(
            (Http11NioProtocol protocol) -> protocol.setConnectionTimeout(4_321)
        );
        factory.addConnectorCustomizers(customizedConnector -> {
            ((AbstractProtocol<?>) customizedConnector.getProtocolHandler()).setMaxConnections(321);
            customizedConnector.setSecure(true);
        });

        redirectCustomizer(tomcatProperties, environment).customize(factory);

        assertEquals(1, factory.getAdditionalConnectors().size());
        Connector connector = factory.getAdditionalConnectors().get(0);
        assertEquals("http", connector.getScheme());
        assertFalse(connector.getSecure());
        assertEquals(8080, connector.getPort());
        assertEquals(8443, connector.getRedirectPort());
        assertEquals(1_000, connector.getMaxPartCount());
        assertEquals(StandardCharsets.ISO_8859_1.name(), connector.getURIEncoding());
        assertEquals("WebJET redirect test", connector.getProperty("server"));
        Http11NioProtocol protocol = assertInstanceOf(Http11NioProtocol.class, connector.getProtocolHandler());
        assertEquals(InetAddress.getLoopbackAddress(), protocol.getAddress());
        assertEquals(4_321, protocol.getConnectionTimeout());
        assertEquals(321, protocol.getMaxConnections());

        StandardContext context = new StandardContext();
        factory.getContextCustomizers().forEach(customizer -> customizer.customize(context));

        SecurityConstraint[] constraints = context.findConstraints();
        assertEquals(1, constraints.length);
        assertEquals("CONFIDENTIAL", constraints[0].getUserConstraint());
        assertFalse(constraints[0].getAuthConstraint());
        SecurityCollection[] collections = constraints[0].findCollections();
        assertEquals(1, collections.length);
        assertTrue(collections[0].findPattern("/*"));
    }

    @Test
    void embeddedTomcatRedirectsPlainHttpBeforeInvokingTheServlet() throws Exception {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(9443);
        factory.setBaseDirectory(tomcatBase.toFile());
        MockEnvironment environment = new MockEnvironment()
            .withProperty(WebjetEmbeddedTomcatConfiguration.HTTP_REDIRECT_PORT_PROPERTY, "8080");
        redirectCustomizer(new TomcatServerProperties(), environment).customize(factory);
        Connector httpConnector = factory.getAdditionalConnectors().get(0);
        httpConnector.setPort(0);
        factory.setPort(0);
        AtomicBoolean servletInvoked = new AtomicBoolean();
        WebServer server = null;

        try {
            server = factory.getWebServer(servletContext -> {
                ServletRegistration.Dynamic servlet = servletContext.addServlet("redirectProbe", new HttpServlet() {
                    @Override
                    protected void doGet(HttpServletRequest request, HttpServletResponse response)
                            throws IOException {
                        servletInvoked.set(true);
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                });
                servlet.addMapping("/probe");
            });
            server.start();

            HttpResponse<Void> response = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()
                .send(
                    HttpRequest.newBuilder(URI.create(
                        "http://127.0.0.1:" + httpConnector.getLocalPort() + "/probe?value=1"
                    ))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build(),
                    HttpResponse.BodyHandlers.discarding()
                );

            assertEquals(HttpServletResponse.SC_FOUND, response.statusCode());
            assertEquals("https://127.0.0.1:9443/probe?value=1",
                response.headers().firstValue("Location").orElseThrow());
            assertFalse(servletInvoked.get());
        } finally {
            if (server != null) {
                server.destroy();
            }
        }
    }

    @Test
    void redirectRejectsAnUnknownHttpsPort() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
            () -> redirectCustomizer(new TomcatServerProperties(), new MockEnvironment()).customize(factory));

        assertEquals("server.port must be a TCP port between 1 and 65535", thrown.getMessage());
    }

    private WebServerFactoryCustomizer<TomcatServletWebServerFactory> redirectCustomizer(
            TomcatServerProperties tomcatProperties, MockEnvironment environment) {
        return new WebjetEmbeddedTomcatConfiguration()
            .webjetTomcatHttpRedirectCustomizer(tomcatProperties, environment);
    }
}
