package sk.iway.aceintegration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;

class SpringConfigTest {

    @Test
    void additionalConnectorUsesConfiguredMultipartPartLimit() {
        TomcatServerProperties tomcatServerProperties = new TomcatServerProperties();
        tomcatServerProperties.setMaxPartCount(1_000);
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();

        new SpringConfig.EmbeddedTomcatConfiguration()
            .tomcatHttpConnectorCustomizer(tomcatServerProperties)
            .customize(factory);

        assertEquals(1, factory.getAdditionalConnectors().size());
        assertEquals(1_000, factory.getAdditionalConnectors().get(0).getMaxPartCount());
    }
}
