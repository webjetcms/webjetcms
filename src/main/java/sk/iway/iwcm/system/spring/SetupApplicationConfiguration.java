package sk.iway.iwcm.system.spring;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import sk.iway.iwcm.SetCharacterEncodingFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = WebjetBootstrapMode.PROPERTY_NAME, havingValue = WebjetBootstrapMode.SETUP_VALUE)
@ComponentScan("sk.iway.iwcm.setup")
public class SetupApplicationConfiguration {

    @Bean
    public FilterRegistrationBean<SetCharacterEncodingFilter> setupCharacterEncodingFilterRegistration() {
        FilterRegistrationBean<SetCharacterEncodingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SetCharacterEncodingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("failedSetCharacterEncodingFilter");
        return registration;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnNotWarDeployment
    static class EmbeddedServletContainerConfiguration {

        @Bean
        public WebServerFactoryCustomizer<TomcatServletWebServerFactory> setupTomcatHttpConnectorCustomizer(
                TomcatServerProperties tomcatServerProperties) {
            return factory -> {
                Connector httpConnector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
                httpConnector.setScheme("http");
                httpConnector.setSecure(false);
                httpConnector.setPort(80);
                httpConnector.setRedirectPort(443);
                httpConnector.setMaxPartCount(tomcatServerProperties.getMaxPartCount());
                factory.addAdditionalConnectors(httpConnector);
            };
        }
    }
}
