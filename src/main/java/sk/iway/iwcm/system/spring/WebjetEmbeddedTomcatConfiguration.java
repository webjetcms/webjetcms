package sk.iway.iwcm.system.spring;

import jakarta.servlet.annotation.ServletSecurity;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * Core customizations for the embedded Tomcat server.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnNotWarDeployment
public class WebjetEmbeddedTomcatConfiguration {

    static final String HTTP_REDIRECT_ENABLED_PROPERTY = "webjet.server.http-redirect.enabled";
    static final String HTTP_REDIRECT_PORT_PROPERTY = "webjet.server.http-redirect.port";

    private static final int DEFAULT_HTTP_PORT = 80;

    /**
     * Adds an HTTP connector that redirects every request to the primary HTTPS connector.
     */
    @Bean
    @ConditionalOnBooleanProperty(name = HTTP_REDIRECT_ENABLED_PROPERTY)
    @ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> webjetTomcatHttpRedirectCustomizer(
            ServerProperties serverProperties, TomcatServerProperties tomcatServerProperties,
            Environment environment) {
        int httpPort = environment.getProperty(
            HTTP_REDIRECT_PORT_PROPERTY, Integer.class, DEFAULT_HTTP_PORT
        );
        requireTcpPort(HTTP_REDIRECT_PORT_PROPERTY, httpPort);
        boolean virtualThreads = environment.getProperty(
            "spring.threads.virtual.enabled", Boolean.class, false
        );

        return new HttpRedirectCustomizer(
            httpPort, serverProperties, tomcatServerProperties, virtualThreads
        );
    }

    private static final class HttpRedirectCustomizer
            implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>, Ordered {

        private final int httpPort;
        private final ServerProperties serverProperties;
        private final TomcatServerProperties tomcatProperties;
        private final boolean virtualThreads;

        private HttpRedirectCustomizer(int httpPort, ServerProperties serverProperties,
                TomcatServerProperties tomcatProperties, boolean virtualThreads) {
            this.httpPort = httpPort;
            this.serverProperties = serverProperties;
            this.tomcatProperties = tomcatProperties;
            this.virtualThreads = virtualThreads;
        }

        @Override
        public void customize(TomcatServletWebServerFactory factory) {
            int httpsPort = requireTcpPort("server.port", factory.getPort());
            if (this.httpPort == httpsPort) {
                throw new IllegalStateException(HTTP_REDIRECT_PORT_PROPERTY + " must differ from server.port");
            }

            Connector httpConnector = new Connector(Http11NioProtocol.class.getName());
            Http11NioProtocol protocol = (Http11NioProtocol) httpConnector.getProtocolHandler();
            configureProtocol(factory, protocol);
            configureConnector(factory, httpConnector);

            httpConnector.setScheme("http");
            httpConnector.setSecure(false);
            httpConnector.setPort(this.httpPort);
            httpConnector.setRedirectPort(httpsPort);
            httpConnector.setThrowOnFailure(true);

            factory.addAdditionalConnectors(httpConnector);
            factory.addContextCustomizers(WebjetEmbeddedTomcatConfiguration::requireSecureTransport);
        }

        private void configureProtocol(TomcatServletWebServerFactory factory, Http11NioProtocol protocol) {
            if (factory.getAddress() != null) {
                protocol.setAddress(factory.getAddress());
            }
            if (this.virtualThreads) {
                protocol.setExecutor(new VirtualThreadExecutor("tomcat-http-redirect-"));
            }

            TomcatServerProperties.Threads threads = this.tomcatProperties.getThreads();
            if (threads.getMax() > 0) {
                protocol.setMaxThreads(threads.getMax());
            }
            if (threads.getMinSpare() > 0) {
                protocol.setMinSpareThreads(threads.getMinSpare());
            }
            if (threads.getMaxQueueCapacity() > 0) {
                protocol.setMaxQueueSize(threads.getMaxQueueCapacity());
            }

            int maxRequestHeaderSize = (int) this.serverProperties.getMaxHttpRequestHeaderSize().toBytes();
            if (maxRequestHeaderSize > 0) {
                protocol.setMaxHttpRequestHeaderSize(maxRequestHeaderSize);
            }
            int maxResponseHeaderSize = (int) this.tomcatProperties.getMaxHttpResponseHeaderSize().toBytes();
            if (maxResponseHeaderSize > 0) {
                protocol.setMaxHttpResponseHeaderSize(maxResponseHeaderSize);
            }
            protocol.setMaxSwallowSize((int) this.tomcatProperties.getMaxSwallowSize().toBytes());
            if (this.tomcatProperties.getConnectionTimeout() != null) {
                protocol.setConnectionTimeout((int) this.tomcatProperties.getConnectionTimeout().toMillis());
            }
            if (this.tomcatProperties.getMaxConnections() > 0) {
                protocol.setMaxConnections(this.tomcatProperties.getMaxConnections());
            }
            if (this.tomcatProperties.getAcceptCount() > 0) {
                protocol.setAcceptCount(this.tomcatProperties.getAcceptCount());
            }
            protocol.setProcessorCache(this.tomcatProperties.getProcessorCache());
            if (this.tomcatProperties.getKeepAliveTimeout() != null) {
                protocol.setKeepAliveTimeout((int) this.tomcatProperties.getKeepAliveTimeout().toMillis());
            }
            protocol.setMaxKeepAliveRequests(this.tomcatProperties.getMaxKeepAliveRequests());
        }

        private void configureConnector(TomcatServletWebServerFactory factory, Connector connector) {
            if (factory.getUriEncoding() != null) {
                connector.setURIEncoding(factory.getUriEncoding().name());
            }
            if (factory.getServerHeader() != null && factory.getServerHeader().isBlank() == false) {
                connector.setProperty("server", factory.getServerHeader());
            }

            int maxPostSize = (int) this.tomcatProperties.getMaxHttpFormPostSize().toBytes();
            if (maxPostSize != 0) {
                connector.setMaxPostSize(maxPostSize);
            }
            connector.setMaxParameterCount(this.tomcatProperties.getMaxParameterCount());
            connector.setMaxPartHeaderSize((int) this.tomcatProperties.getMaxPartHeaderSize().toBytes());
            connector.setMaxPartCount(this.tomcatProperties.getMaxPartCount());
            setCharacterProperty(connector, "relaxedPathChars", this.tomcatProperties.getRelaxedPathChars());
            setCharacterProperty(connector, "relaxedQueryChars", this.tomcatProperties.getRelaxedQueryChars());
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }

        private static void setCharacterProperty(Connector connector, String propertyName,
                Iterable<Character> characters) {
            StringBuilder value = new StringBuilder();
            characters.forEach(value::append);
            if (value.isEmpty() == false) {
                connector.setProperty(propertyName, value.toString());
            }
        }
    }

    private static int requireTcpPort(String propertyName, Integer port) {
        if (port == null || port < 1 || port > 65_535) {
            throw new IllegalStateException(propertyName + " must be a TCP port between 1 and 65535");
        }
        return port;
    }

    private static void requireSecureTransport(Context context) {
        SecurityCollection allRequests = new SecurityCollection(
            "WebJET HTTPS transport", "All requests require HTTPS"
        );
        allRequests.addPattern("/*");

        SecurityConstraint httpsConstraint = new SecurityConstraint();
        httpsConstraint.setDisplayName("WebJET HTTPS transport");
        httpsConstraint.setUserConstraint(ServletSecurity.TransportGuarantee.CONFIDENTIAL.name());
        httpsConstraint.addCollection(allRequests);
        context.addConstraint(httpsConstraint);
    }
}
