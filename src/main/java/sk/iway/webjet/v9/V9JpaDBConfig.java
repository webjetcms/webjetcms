package sk.iway.webjet.v9;

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
import sk.iway.iwcm.system.logging.InMemoryLoggingDB;

/**
 * JpaDBConfig - zakladna konfiguracia JPA aby sme vedeli, co mame preniest potom do standardneho WJ
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "webjet2022EntityManager",
    transactionManagerRef = "webjet2022TransactionManager",
    basePackages = {
        "sk.iway.iwcm.components.gallery",
        "sk.iway.iwcm.system",
        "sk.iway.iwcm.system.audit",
        "sk.iway.iwcm.components.redirects",
        "sk.iway.iwcm.components.translation_keys.jpa",
        "sk.iway.iwcm.components.forms",
        "sk.iway.iwcm.components.forms.archive",
        "sk.iway.iwcm.components.insertScript",
        "sk.iway.iwcm.components.memory_cleanup.memory_cleanup_date_dependent",
        "sk.iway.iwcm.components.memory_cleanup",
        "sk.iway.iwcm.components.monitoring.jpa",
        "sk.iway.iwcm.components.media",
        "sk.iway.spirit.model",
        "sk.iway.iwcm.editor.rest",
        "sk.iway.iwcm.components.configuration",
        "sk.iway.iwcm.components.users.userdetail",
        "sk.iway.iwcm.components.users.usergroups",
        "sk.iway.iwcm.components.users.groups_approve",
        "sk.iway.iwcm.components.users.permgroups",
        "sk.iway.iwcm.components.perex_groups",
        "sk.iway.iwcm.users",
        "sk.iway.iwcm.users.jpa",
        "sk.iway.iwcm.components.gdpr.model",
        "sk.iway.iwcm.components.qa.jpa",
        "sk.iway.iwcm.doc",
        "sk.iway.iwcm.components.export",
        "sk.iway.iwcm.components.banner",
        "sk.iway.iwcm.dmail.jpa",
        "sk.iway.iwcm.stat.jpa",
        "sk.iway.iwcm.components.calendar.jpa",
        "sk.iway.iwcm.components.reservation.jpa",
        "sk.iway.iwcm.components.inquiry.jpa",
        "sk.iway.iwcm.doc.attributes.jpa",
        "sk.iway.basecms.contact",
        "sk.iway.iwcm.components.proxy.jpa",
        "sk.iway.iwcm.components.enumerations.model",
        "sk.iway.iwcm.components.response_header.jpa",
        "sk.iway.iwcm.components.forum.jpa",
        "sk.iway.iwcm.components.seo.jpa",
        "sk.iway.iwcm.components.rating.jpa",
        "sk.iway.iwcm.components.restaurant_menu.jpa"
    }
) // package s repozitarmi
public class V9JpaDBConfig {

    @Bean("webjet2022EntityManager")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory()
    {
        InMemoryLoggingDB.setQueueSize(Constants.getInt("loggingInMemoryQueueSize"));

        Logger.println(V9JpaDBConfig.class, "WebJET V9JpaDBConfig");

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPersistenceProvider(new WebJETPersistenceProvider());
        emf.setDataSource(DBPool.getInstance().getDataSource("iwcm"));
        emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        emf.setPackagesToScan(
                            "sk.iway.iwcm.components.gallery",
                            "sk.iway.iwcm.system",
                            "sk.iway.iwcm.system.audit",
                            "sk.iway.iwcm.components.forms",
                            "sk.iway.iwcm.components.forms.archive",
                            "sk.iway.iwcm.components.redirects",
                            "sk.iway.iwcm.components.translation_keys.jpa",
                            "sk.iway.iwcm.components.insertScript",
                            "sk.iway.iwcm.components.memory_cleanup.memory_cleanup_date_dependent",
                            "sk.iway.iwcm.components.memory_cleanup",
                            "sk.iway.iwcm.components.monitoring.jpa",
                            "sk.iway.iwcm.components.media",
                            "sk.iway.spirit.model",
                            "sk.iway.iwcm.editor.rest",
                            "sk.iway.iwcm.components.configuration",
                            "sk.iway.iwcm.components.users.userdetail",
                            "sk.iway.iwcm.components.users.usergroups",
                            "sk.iway.iwcm.components.users.groups_approve",
                            "sk.iway.iwcm.components.users.permgroups",
                            "sk.iway.iwcm.components.perex_groups",
                            "sk.iway.iwcm.doc",
                            "sk.iway.iwcm.users",
                            "sk.iway.iwcm.users.jpa",
                            "sk.iway.iwcm.components.gdpr.model",
                            "sk.iway.iwcm.components.qa.jpa",
                            "sk.iway.iwcm.components.export",
                            "sk.iway.iwcm.components.banner",
                            "sk.iway.iwcm.dmail.jpa",
                            "sk.iway.iwcm.stat.jpa",
                            "sk.iway.iwcm.components.calendar.jpa",
                            "sk.iway.iwcm.components.reservation.jpa",
                            "sk.iway.iwcm.components.inquiry.jpa",
                            "sk.iway.iwcm.doc.attributes.jpa",
                            "sk.iway.basecms.contact",
                            "sk.iway.iwcm.components.proxy.jpa",
                            "sk.iway.iwcm.components.enumerations.model",
                            "sk.iway.iwcm.components.response_header.jpa",
                            "sk.iway.iwcm.components.forum.jpa",
                            "sk.iway.iwcm.components.seo.jpa",
                            "sk.iway.iwcm.components.rating.jpa",
                            "sk.iway.iwcm.components.restaurant_menu.jpa"
        );

        Properties properties = new Properties();
        properties.setProperty("eclipselink.weaving", "false");
        properties.setProperty("eclipselink.session.customizer", "sk.iway.webjet.v9.JpaSessionCustomizer");

        if (Constants.DB_TYPE == Constants.DB_ORACLE) properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.Oracle);
        else if (Constants.DB_TYPE == Constants.DB_MSSQL) properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.SQLServer);
        else properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.MySQL);

        WebJETJavaSECMPInitializer.setDefaultProperties(properties);
        emf.setJpaProperties(properties);

        return emf;
    }

    /*@Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return transactionManager();
    }*/

    @Bean("webjet2022TransactionManager")
    public PlatformTransactionManager transactionManager(){
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

}
