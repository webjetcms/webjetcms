package sk.iway.iwcm.rag.vectorjpa;

import java.util.Properties;

import jakarta.persistence.EntityManagerFactory;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.TargetDatabase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.rag.vectorstore.VectorStoreType;
import sk.iway.iwcm.rag.vectorstore.VectorStoreDataSourceResolver;
import sk.iway.iwcm.rag.vectorstore.VectorStoreDataSourceResolver.Resolution;
import sk.iway.iwcm.system.jpa.WebJETJavaSECMPInitializer;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;

/**
 * JPA configuration for RAG entities.
 * Uses an explicitly configured {@code rag_jpa} datasource or the primary datasource.
 * The vector backend and JPA dialect are detected from JDBC metadata.
 * Registered only by {@link VectorSpringConfig} in its isolated persistence context;
 * deliberately has no component stereotype to avoid discovery by the main application.
 */
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "ragEntityManager",
    transactionManagerRef = "ragTransactionManager",
    basePackages = { "sk.iway.iwcm.rag.vectorjpa" }
)
public class VectorJpaConfig {

    /**
     * Create the JPA transaction manager for RAG entities.
     */
    @Bean("ragTransactionManager")
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
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        Logger.println(VectorJpaConfig.class, "loading RAG RagJpaConfig");

        Resolution resolution = VectorStoreDataSourceResolver.resolve();
        if (resolution.isSupported() == false) {
            throw new DataAccessResourceFailureException("RAG vector store is unavailable: " + resolution.reason());
        }
        String dsName = resolution.dataSourceName();
        Logger.println(VectorJpaConfig.class,
            "Using RAG datasource " + dsName + " with backend " + resolution.backend());

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPersistenceProvider(new WebJETPersistenceProvider());
        emf.setDataSource(DBPool.getInstance().getDataSource(dsName));
        emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        emf.setPersistenceUnitName(dsName);
        emf.setPackagesToScan("sk.iway.iwcm.rag.vectorjpa");

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
        if (resolution.backend() == VectorStoreType.POSTGRESQL) return TargetDatabase.PostgreSQL;
        if (resolution.backend() == VectorStoreType.MARIADB) return TargetDatabase.MySQL;

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
