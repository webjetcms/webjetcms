package sk.iway.iwcm.system.spring;

import java.util.EnumSet;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

import sk.iway.iwcm.SetCharacterEncodingFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = WebjetBootstrapMode.PROPERTY_NAME, havingValue = WebjetBootstrapMode.SETUP_VALUE)
@ComponentScan("sk.iway.iwcm.setup")
public class SetupApplicationConfiguration {

    @Bean
    public FilterRegistrationBean<SetCharacterEncodingFilter> setupCharacterEncodingFilterRegistration() {
        FilterRegistrationBean<SetCharacterEncodingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SetCharacterEncodingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("failedSetCharacterEncodingFilter");
        return registration;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class SetupSecurityConfiguration {

        private static final int SETUP_SECURITY_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

        @Bean
        DelegatingFilterProxyRegistrationBean setupSecurityFilterChainRegistration() {
            DelegatingFilterProxyRegistrationBean registration = new DelegatingFilterProxyRegistrationBean(
                AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME
            );
            registration.setName("webjetSetupSecurityFilter");
            registration.setOrder(SETUP_SECURITY_FILTER_ORDER);
            registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
            return registration;
        }

        @Bean
        PasswordEncoder setupPasswordEncoder() {
            return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        }

        @Bean
        UserDetailsService setupUserDetailsService(Environment environment, PasswordEncoder setupPasswordEncoder) {
            String token = WebjetSetupProperties.requireToken(environment);
            UserDetails setupUser = User.withUsername("setup")
                .password(setupPasswordEncoder.encode(token))
                .roles("SETUP")
                .build();
            return new InMemoryUserDetailsManager(setupUser);
        }

        @Bean
        SecurityFilterChain setupSecurityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/wjerrorpages/setup/**").hasRole("SETUP")
                .anyRequest().denyAll()
            );
            http.formLogin(form -> form
                .defaultSuccessUrl("/wjerrorpages/setup/setup")
                .permitAll()
            );
            http.logout(logout -> logout.permitAll());
            http.sessionManagement(session -> session
                .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
            );
            return http.build();
        }
    }
}
