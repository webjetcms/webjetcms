package com.example.webjetadditional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdditionalPackageConfiguration {

    @Bean
    String additionalPackageBean() {
        return "additional-package";
    }
}
