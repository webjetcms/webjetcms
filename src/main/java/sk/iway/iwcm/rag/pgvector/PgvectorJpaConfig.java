package sk.iway.iwcm.rag.pgvector;

import java.util.Properties;

import jakarta.persistence.EntityManagerFactory;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.TargetDatabase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.rag.vectorstore.VectorStoreBackend;
import sk.iway.iwcm.rag.vectorstore.VectorStoreDataSourceResolver;
import sk.iway.iwcm.rag.vectorstore.VectorStoreDataSourceResolver.Resolution;
import sk.iway.iwcm.system.jpa.WebJETJavaSECMPInitializer;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;

/**
 * JPA configuration for RAG entities.
 * Uses an explicitly configured {@code rag_jpa} datasource or the primary datasource.
 * The vector backend and JPA dialect are detected from JDBC metadata.
 */
@Configuration("rag:JpaDBConfig")
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "ragEntityManager",
    transactionManagerRef = "ragTransactionManager",
    basePackages = { "sk.iway.iwcm.rag.pgvector" },
    bootstrapMode = BootstrapMode.LAZY
)
public class PgvectorJpaConfig {

    /**
     * Create the JPA transaction manager for RAG entities.
     */
    @Bean("ragTransactionManager")
    @Lazy
    public PlatformTransactionManager transactionManager(
        @Qualifier("ragEntityManager") EntityManagerFactory entityManagerFactory
    ) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }

    /**
     * Create the JPA EntityManagerFactory for RAG entities.
     * Uses the RAG datasource selected by {@link VectorStoreDataSourceResolver}.
     */
    @Bean("ragEntityManager")
    @Lazy
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        Logger.println(PgvectorJpaConfig.class, "loading RAG RagJpaConfig");

        Resolution resolution = VectorStoreDataSourceResolver.resolve();
        String dsName = resolution.dataSourceName();
        if (dsName == null) dsName = VectorStoreDataSourceResolver.PRIMARY_DATASOURCE_NAME;

        if (resolution.isSupported()) {
            Logger.println(PgvectorJpaConfig.class,
                "Using RAG datasource " + dsName + " with backend " + resolution.backend());
        } else {
            Logger.warn(PgvectorJpaConfig.class,
                "RAG vector store is unavailable: " + resolution.reason());
        }

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPersistenceProvider(new WebJETPersistenceProvider());
        emf.setDataSource(DBPool.getInstance().getDataSource(dsName));
        emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        emf.setPersistenceUnitName(dsName);
        emf.setPackagesToScan("sk.iway.iwcm.rag.pgvector");

        Properties properties = new Properties();
        properties.setProperty("eclipselink.weaving", "false");
        properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, getTargetDatabase(resolution));
        WebJETJavaSECMPInitializer.setDefaultProperties(properties);
        emf.setJpaProperties(properties);

        return emf;
    }

    /**
     * Returns the datasource name for supported RAG vector operations.
     *
     * @return datasource name, or {@code null} when the selected datasource is unsupported
     */
    public static String getRagDataSourceName() {
        return VectorStoreDataSourceResolver.getRagDataSourceName();
    }

    /**
     * Check if a supported and enabled RAG vector store is available.
     */
    public static boolean isRagAvailable() {
        return VectorStoreDataSourceResolver.isRagAvailable();
    }

    private static String getTargetDatabase(Resolution resolution) {
        if (resolution.backend() == VectorStoreBackend.POSTGRESQL) return TargetDatabase.PostgreSQL;
        if (resolution.backend() == VectorStoreBackend.MARIADB) return TargetDatabase.MySQL;

        String productName = resolution.databaseProductName();
        if (productName != null) {
            String normalized = productName.toLowerCase(java.util.Locale.ROOT);
            if (normalized.contains("postgresql")) return TargetDatabase.PostgreSQL;
            if (normalized.contains("mysql") || normalized.contains("mariadb")) return TargetDatabase.MySQL;
            if (normalized.contains("oracle")) return TargetDatabase.Oracle;
            if (normalized.contains("microsoft") || normalized.contains("sql server")) return TargetDatabase.SQLServer;
        }

        return TargetDatabase.Auto;
    }
}
