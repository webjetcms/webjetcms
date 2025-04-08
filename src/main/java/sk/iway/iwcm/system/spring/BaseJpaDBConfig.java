package sk.iway.iwcm.system.spring;

import java.util.Properties;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.TargetDatabase;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.jpa.WebJETJavaSECMPInitializer;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;

/**
 * Base class for Spring DATA config, you need to extend this class and override entityManagerFactory() and transactionManager() methods
 * http://docs.webjetcms.sk/latest/en/custom-apps/spring-config/
 */

public class BaseJpaDBConfig {

    /**
     * In your implementation you need to override this method and annotate it with @Bean("basecmsTransactionManager")
     * @return
     */
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    /**
     * In your implementation you need to override this method and annotate it with @Bean("basecmsEntityManager")
     * @return
     */
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        return getEntityManager(null, null);
    }

    /**
     * Base class to setup JPA EntityManager
     * @param datasource
     * @param packagesToScan
     * @return
     */
    public LocalContainerEntityManagerFactoryBean getEntityManager(String datasource, String[] packagesToScan) {
        Logger.println(this, "Loading JpaDBConfig");

        if (datasource == null) datasource = "iwcm";

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPersistenceProvider(new WebJETPersistenceProvider());
        emf.setDataSource(DBPool.getInstance().getDataSource(datasource));
        emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());

        // Zoznam packages ktore sa maju skenovat pre databazove entity/DAO !!
        emf.setPackagesToScan(packagesToScan);

        Properties properties = new Properties();
        // https://stackoverflow.com/questions/10769051/eclipselinkjpavendoradapter-instead-of-hibernatejpavendoradapter-issue
        properties.setProperty("eclipselink.weaving", "false");
        properties.setProperty("eclipselink.session.customizer", "sk.iway.webjet.v9.JpaSessionCustomizer");

        if (Constants.DB_TYPE == Constants.DB_ORACLE) properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.Oracle);
        else if (Constants.DB_TYPE == Constants.DB_MSSQL) properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.SQLServer);
        else if (Constants.DB_TYPE == Constants.DB_PGSQL) properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.PostgreSQL);
        else properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.MySQL);

        WebJETJavaSECMPInitializer.setDefaultProperties(properties);
        emf.setJpaProperties(properties);

        return emf;
    }

}
