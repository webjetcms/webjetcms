package sk.iway.iwcm.rag.pgvector;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
* Spring configuration for RAG module.
 */

@Configuration
@ComponentScan({
    "sk.iway.iwcm.rag.pgvector",
})
public class PgvectorSpringConfig {

}