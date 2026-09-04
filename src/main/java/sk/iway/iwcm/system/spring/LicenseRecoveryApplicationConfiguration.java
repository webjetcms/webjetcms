package sk.iway.iwcm.system.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.setup.LicenseController;
import sk.iway.iwcm.setup.SetupSpringConfig;

/**
 * Minimal application graph used to replace an invalid license without
 * exposing the installation setup or starting production services.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    name = WebjetBootstrapMode.PROPERTY_NAME,
    havingValue = WebjetBootstrapMode.LICENSE_RECOVERY_VALUE
)
@Import({SetupSpringConfig.class, LicenseController.class})
public class LicenseRecoveryApplicationConfiguration {

    @Bean
    public FilterRegistrationBean<SetCharacterEncodingFilter> licenseRecoveryCharacterEncodingFilterRegistration() {
        FilterRegistrationBean<SetCharacterEncodingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SetCharacterEncodingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("licenseRecoverySetCharacterEncodingFilter");
        return registration;
    }
}
