package sk.iway.aceintegration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Ukazkova konfiguracia Spring
 * http://docs.webjetcms.sk/latest/custom-apps/spring-config/
 */

@Configuration
@ComponentScan({
    "sk.iway.aceintegration",
})
public class SpringConfig {

}
