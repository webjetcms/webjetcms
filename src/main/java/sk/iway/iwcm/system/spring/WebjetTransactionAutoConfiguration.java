package sk.iway.iwcm.system.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/** Supplies a CMS transaction template while preserving customer transaction managers and templates. */
@AutoConfiguration(before = TransactionAutoConfiguration.class)
@ConditionalOnBean(name = "webjet2022TransactionManager")
public final class WebjetTransactionAutoConfiguration {

    /** Creates the default template only when the customer has not supplied transaction operations. */
    @Bean
    @ConditionalOnMissingBean(TransactionOperations.class)
    TransactionTemplate transactionTemplate(
            @Qualifier("webjet2022TransactionManager") PlatformTransactionManager manager) {
        return new TransactionTemplate(manager);
    }
}
