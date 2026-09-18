package sk.iway.iwcm.rag.pgvector;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.AbstractLazyCreationTargetSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.interceptor.TransactionalProxy;

/**
 * Exposes the RAG repository without including its optional database in CMS JPA startup.
 */

@Configuration
public class PgvectorSpringConfig {

    /** Returns a proxy that initializes RAG persistence on the first repository operation. */
    @Bean
    public EmbeddingChunkRepository embeddingChunkRepository() {
        ProxyFactory proxy = new ProxyFactory();
        // Transactions belong to the repository inside the isolated persistence context.
        proxy.setInterfaces(EmbeddingChunkRepository.class, TransactionalProxy.class);
        proxy.setTargetSource(new AbstractLazyCreationTargetSource() {
            @Override
            protected Object createObject() {
                return ragPersistenceContext().getBean(EmbeddingChunkRepository.class);
            }
        });
        return (EmbeddingChunkRepository) proxy.getProxy();
    }

    /**
     * Creates an isolated context so global JpaContext and metamodel scans cannot initialize RAG.
     * Failed initialization is not cached, allowing a later request to retry after database recovery.
     *
     * @return RAG persistence context, closed with the main application context
     */
    @Bean(destroyMethod = "close")
    @Lazy
    public AnnotationConfigApplicationContext ragPersistenceContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(PgvectorJpaConfig.class);
        try {
            context.refresh();
            return context;
        } catch (RuntimeException | Error e) {
            context.close();
            throw e;
        }
    }
}
