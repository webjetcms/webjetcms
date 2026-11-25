package sk.iway.dynamicinstall;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("sk.iway.dynamicinstall")
public class SpringConfig {

    @Bean
    String dynamicInstallBean() {
        return "dynamic-install";
    }
}
