package sk.iway.basecms;

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
 * Persistence configuration owned by the optional Base CMS example module.
 */
@Configuration("basecms:JpaDBConfig")
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "basecmsEntityManager",
    transactionManagerRef = "basecmsTransactionManager",
    basePackages = "sk.iway.basecms.contact"
)
public class JpaDBConfig {

    private static final String PERSISTENCE_UNIT_NAME = "basecms";

    @Bean("basecmsTransactionManager")
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean("basecmsEntityManager")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        Logger.println(this, "loading basecms JpaDBConfig");

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPersistenceProvider(new WebJETPersistenceProvider());
        emf.setDataSource(DBPool.getInstance().getDataSource("iwcm"));
        emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        emf.setPersistenceUnitName(PERSISTENCE_UNIT_NAME);
        emf.setPackagesToScan("sk.iway.basecms.contact");

        Properties properties = new Properties();
        properties.setProperty("eclipselink.weaving", "false");

        if (Constants.DB_TYPE == Constants.DB_ORACLE) {
            properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.Oracle);
        } else if (Constants.DB_TYPE == Constants.DB_MSSQL) {
            properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.SQLServer);
        } else if (Constants.DB_TYPE == Constants.DB_PGSQL) {
            properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.PostgreSQL);
        } else {
            properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.MySQL);
        }

        WebJETJavaSECMPInitializer.setDefaultProperties(properties);
        emf.setJpaProperties(properties);
        return emf;
    }
}
