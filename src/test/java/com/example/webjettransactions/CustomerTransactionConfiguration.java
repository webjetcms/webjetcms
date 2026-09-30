package com.example.webjettransactions;

import static org.mockito.Mockito.mock;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/** Customer configuration discovered through additional package scanning in transaction tests. */
@Configuration(proxyBeanMethods = false)
public class CustomerTransactionConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "test.customer.template", havingValue = "standard")
    static class StandardTemplate {
        @Bean
        TransactionTemplate transactionTemplate(
                @Qualifier("basecmsTransactionManager") PlatformTransactionManager manager) {
            return new TransactionTemplate(manager);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "test.customer.template", havingValue = "named")
    static class NamedTemplate {
        @Bean
        TransactionTemplate customerTransactionTemplate(
                @Qualifier("basecmsTransactionManager") PlatformTransactionManager manager) {
            return new TransactionTemplate(manager);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "test.customer.template", havingValue = "operations")
    static class CustomOperations {
        @Bean
        TransactionOperations customerTransactionOperations() {
            return mock(TransactionOperations.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "test.customer.primary", havingValue = "true")
    static class PrimaryManager {
        @Bean
        @Primary
        PlatformTransactionManager customerPrimaryTransactionManager() {
            return mock(PlatformTransactionManager.class);
        }
    }
}
