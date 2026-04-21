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
public class RagJpaConfig {

    private static final String RAG_DATASOURCE_NAME = "rag_jpa";

    @Bean("ragTransactionManager")
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean("ragEntityManager")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        Logger.println(RagJpaConfig.class, "loading RAG RagJpaConfig");

        String dsName = getRagDataSourceName();
        if (dsName == null) {
            Logger.println(RagJpaConfig.class, "RAG datasource not available, using iwcm as fallback");
            dsName = "iwcm";
        }

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPersistenceProvider(new WebJETPersistenceProvider());
        emf.setDataSource(DBPool.getInstance().getDataSource(dsName));
        emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
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
     * If primary DB is PostgreSQL, use it directly.
     * If a secondary 'rag_jpa' datasource exists, use that.
     * Otherwise return null (RAG not available).
     */
    public static String getRagDataSourceName() {
        if (Constants.DB_TYPE == Constants.DB_PGSQL) {
            return "iwcm";
        }
        if (DBPool.getInstance().getDataSource(RAG_DATASOURCE_NAME) != null) {
            return RAG_DATASOURCE_NAME;
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
