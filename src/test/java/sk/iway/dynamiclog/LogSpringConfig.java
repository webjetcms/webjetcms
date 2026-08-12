package sk.iway.dynamiclog;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogSpringConfig {

    @Bean
    String preferredLogConfigurationBean() {
        return "preferred-log-configuration";
    }
}
