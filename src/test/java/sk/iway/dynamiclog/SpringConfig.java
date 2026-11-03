package sk.iway.dynamiclog;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfig {

    @Bean
    String legacyLogConfigurationBean() {
        return "legacy-log-configuration";
    }
}
