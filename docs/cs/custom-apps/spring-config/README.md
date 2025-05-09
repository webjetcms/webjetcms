# Konfigurace Spring

Před programováním je třeba nakonfigurovat načítání `Spring` tříd a repozitářů. Je třeba vytvořit souboru `SpringConfig.java` pro konfiguraci `Spring` a `JpaDBConfig.java` pro konfiguraci repozitářů. Vytvoříte je v package `sk.iway.INSTALL_NAME`, aby je WebJET při startu načítal a inicializoval. Hodnotu `INSTALL_NAME` nahradíte hodnotou konf. proměnné `installName`.

## Nastavení Spring

Ve třídě `SpringConfig` je třeba nastavit v anotaci `@ComponentScan` packages, které obsahují `Spring` třídy.

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

## Nastavení JPA

Ve třídě `JpaDBConfig` (technicky je jedno jak se jmenuje, musí být ale v package, který je nastaven v `SpringConfig` v sekci `@ComponentScan`) je podobně nutné v anotaci `@EnableJpaRepositories.basePackages` nastavit packages obsahující `Spring DATA` repozitáře. Do `emf.setPackagesToScan` je třeba přidat `packages` obsahující `JPA` entity (obvykle jsou to stejné `packages`).

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

## Nastavení SpringSecurity

Pokud potřebujete upravit nastavení pro `SpringSecurity` můžete ve vaší třídě `SpringConfig` implementovat `sk.iway.iwcm.system.spring.ConfigurableSecurity`. V metodě `configureSecurity(HttpSecurity http)` máte dostupný objekt `HttpSecurity` ve kterém můžete doplnit potřebná nastavení:

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
