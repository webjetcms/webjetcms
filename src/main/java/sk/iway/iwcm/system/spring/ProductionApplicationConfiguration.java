package sk.iway.iwcm.system.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import sk.iway.iwcm.rag.vectorjpa.VectorSpringConfig;
import sk.iway.webjet.v9.V9SpringConfig;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = WebjetBootstrapMode.PROPERTY_NAME, havingValue = WebjetBootstrapMode.PRODUCTION_VALUE)
@Import({
    BaseSpringConfig.class,
    V9SpringConfig.class,
    VectorSpringConfig.class,
    SpringSecurityConf.class,
    GlobalExceptionHandler.class,
    WebjetCustomerSpringConfigurationImportSelector.class,
    WebjetAdditionalSpringPackagesRegistrar.class
})
@ComponentScan(
    basePackages = {
        "sk.iway.iwcm.system.spring.openapi",
        "sk.iway.iwcm.system.spring.services",
        "sk.iway.iwcm.system.spring.webjet_component"
    }
)
public class ProductionApplicationConfiguration {
}
