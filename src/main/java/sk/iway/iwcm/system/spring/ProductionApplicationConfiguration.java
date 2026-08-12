package sk.iway.iwcm.system.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = WebjetBootstrapMode.PROPERTY_NAME, havingValue = WebjetBootstrapMode.PRODUCTION_VALUE)
@ComponentScan(
    basePackages = {
        "sk.iway.iwcm",
        "sk.iway.basecms",
        "sk.iway.aceintegration",
        "sk.iway.iway",
        "sk.iway.webjet.v9"
    },
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "sk\\.iway\\.iwcm\\.setup\\..*"),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            SpringBootStarter.class,
            SpringBootStarter.ProductionServletConfiguration.class,
            SpringAppInitializer.class,
            SetupApplicationConfiguration.class,
            ProductionApplicationConfiguration.class
        })
    }
)
public class ProductionApplicationConfiguration {
}
