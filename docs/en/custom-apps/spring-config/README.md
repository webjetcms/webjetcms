# Spring configuration

Before programming it is necessary to configure the loading `Spring` classes and repositories. It is necessary to create a file `SpringConfig.java` for configuration `Spring` a `JpaDBConfig.java` for configuring repositories. You create them in package `sk.iway.INSTALL_NAME` to be loaded and initialized by WebJET at startup. The value `INSTALL_NAME` replace with the value of the conf. variable `installName`.

## Spring settings

In class `SpringConfig` should be set in the annotation `@ComponentScan` packages containing `Spring` Classes.

```java
package sk.iway.basecms;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({
    "sk.iway.basecms",
    "sk.iway.basecms.contact"
})
public class SpringConfig {

}
```

## JPA settings

In class `JpaDBConfig` (technically it doesn't matter what it's called, but it must be in the package that is set in `SpringConfig` in section `@ComponentScan`) is similarly needed in the annotation `@EnableJpaRepositories.basePackages` set packages containing `Spring DATA` repositories. Do `emf.setPackagesToScan` it is necessary to add `packages` Containing `JPA` entities (usually these are the same `packages`).

```java
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
 *     Dolezite:
 *     1.) nastavit anotaciu @EnableJpaRepositories na package ktore obsahuju @Repository
 *     2.) nastavit setPackagesToScan() na entity ktore pouzivame v repozitaroch
 *     3.) pokial sa trieda vola JpaDBConfig, zmenit name pri anotacii @Configuration, musi byt jedinecny
 *     4.) zmenit hodnoty entityManagerFactoryRef a transactionManagerRef, musia byt jedinecne
 *     5.) zmenit name pri @Bean podla hodnot entityManagerFactoryRef a transactionManagerRef
 *     6.) over ci neimplementujes triedu TransactionManagementConfigurer - to dat prec spolu aj s @Override metody annotationDrivenTransactionManager
 */
@Configuration("basecms:JpaDBConfig")
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "basecmsEntityManager",
    transactionManagerRef = "basecmsTransactionManager",
    basePackages = {
        "sk.iway.basecms.contact"
    }
)
public class JpaDBConfig {

    @Bean("basecmsTransactionManager")
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean("basecmsEntityManager")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        Logger.println(this, "loading basecms JpaDBConfig");

        String dataSourceName = "iwcm";

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPersistenceProvider(new WebJETPersistenceProvider());
        emf.setDataSource(DBPool.getInstance().getDataSource(dataSourceName));
        emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        emf.setPersistenceUnitName(dataSourceName);

        // Zoznam packages ktore sa maju skenovat pre databazove entity/DAO !!
        emf.setPackagesToScan(
                "sk.iway.basecms.contact"
        );

        Properties properties = new Properties();
        // https://stackoverflow.com/questions/10769051/eclipselinkjpavendoradapter-instead-of-hibernatejpavendoradapter-issue
        properties.setProperty("eclipselink.weaving", "false");

        if (Constants.DB_TYPE == Constants.DB_ORACLE) properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.Oracle);
        else if (Constants.DB_TYPE == Constants.DB_MSSQL) properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.SQLServer);
        else if (Constants.DB_TYPE == Constants.DB_PGSQL) properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.PostgreSQL);
        else properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.MySQL);

        WebJETJavaSECMPInitializer.setDefaultProperties(properties);
        emf.setJpaProperties(properties);

        return emf;
    }

}
```

## SpringSecurity Settings

If you need to adjust the settings for `SpringSecurity` you can in your class `SpringConfig` Implement `sk.iway.iwcm.system.spring.ConfigurableSecurity`. In the method `configureSecurity(HttpSecurity http)` you have an available object `HttpSecurity` where you can add the necessary settings:

```java
import sk.iway.iwcm.system.spring.ConfigurableSecurity;

public class SpringConfig implements ConfigurableSecurity {
    ...
    @Override
    public void configureSecurity(HttpSecurity http) throws Exception {
        //pridaj filter na prihlasovanie cez ApiToken
        http.addFilterAfter(new ApiTokenAuthFilter(), BasicAuthenticationFilter.class);
    }
}
```
