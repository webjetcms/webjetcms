package sk.iway.iwcm.rag.pgvector;

import java.util.Properties;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.TargetDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.jpa.WebJETJavaSECMPInitializer;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;

/**
 * JPA configuration for RAG pgvector entities.
 * Uses either the primary DB (if it's PostgreSQL) or a secondary 'rag_jpa' datasource.
 * If no pgvector-capable DB is available, this config is effectively unused.
 */
@Configuration("rag:JpaDBConfig")
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "ragEntityManager",
    transactionManagerRef = "ragTransactionManager",
    basePackages = { "sk.iway.iwcm.rag.pgvector" }
)
public class PgvectorJpaConfig {

    private static final String RAG_DATASOURCE_NAME = "rag_jpa";

    /**
     * Create the JPA transaction manager for RAG entities.
     */
    @Bean("ragTransactionManager")
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    /**
     * Create the JPA EntityManagerFactory for RAG entities.
     * Uses the RAG datasource if available, otherwise falls back to the default 'iwcm' datasource.
     */
    @Bean("ragEntityManager")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        Logger.println(PgvectorJpaConfig.class, "loading RAG RagJpaConfig");

        String dsName = getRagDataSourceName();
        if (Tools.isEmpty(dsName)) {
            Logger.println(PgvectorJpaConfig.class, "RAG datasource not available, using iwcm as fallback");
            dsName = "iwcm";
        }

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPersistenceProvider(new WebJETPersistenceProvider());
        emf.setDataSource(DBPool.getInstance().getDataSource(dsName));
        emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        emf.setPersistenceUnitName(dsName);
        emf.setPackagesToScan("sk.iway.iwcm.rag.pgvector");

        Properties properties = new Properties();
        properties.setProperty("eclipselink.weaving", "false");
        properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.PostgreSQL);
        WebJETJavaSECMPInitializer.setDefaultProperties(properties);
        emf.setJpaProperties(properties);

        return emf;
    }

    /**
     * Returns the datasource name for RAG pgvector operations.
        * If a secondary 'rag_jpa' datasource exists, use it.
        * Otherwise, if primary DB is PostgreSQL, use 'iwcm', because it can run pgvector extension.
     * Otherwise return null (RAG not available).
     */
    public static String getRagDataSourceName() {
        if (DBPool.getInstance().getDataSource(RAG_DATASOURCE_NAME) != null) {
            return RAG_DATASOURCE_NAME;
        }
        if (Constants.DB_TYPE == Constants.DB_PGSQL) {
            return "iwcm";
        }
        return null;
    }

    /**
     * Check if RAG vector store is available (pgvector-capable DB configured).
     */
    public static boolean isRagAvailable() {
        return getRagDataSourceName() != null && Constants.getBoolean("ragSemanticSearchEnabled");
    }
}
