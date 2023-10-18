package sk.iway.basecms;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Ukazkova konfiguracia Spring
 * http://docs.webjetcms.sk/v2022/#/custom-apps/spring-config/
 */

@Configuration
@ComponentScan({
    "sk.iway.basecms",
    "sk.iway.basecms.contact",
    "sk.iway.demo8"
})
public class SpringConfig {

}
