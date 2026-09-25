package sk.iway.legacylog;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfig {

    @Bean
    String legacyLogFallbackBean() {
        return "legacy-log-fallback";
    }
}
