# Konfigurácia Spring

Pred programovaním je potrebné nakonfigurovať načítanie ```Spring``` tried a repozitárov. Je potrebné vytvoriť súboru ```SpringConfig.java``` pre konfiguráciu ```Spring``` a ```JpaDBConfig.java``` pre konfiguráciu repozitárov. Vytvoríte ich v package ```sk.iway.INSTALL_NAME```, aby ich WebJET pri štarte načítal a inicializoval. Hodnotu ```INSTALL_NAME``` nahradíte hodnotou konf. premennej ```installName```.

## Nastavenie Spring

V triede ```SpringConfig``` je potrené nastaviť v anotácii ```@ComponentScan``` packages, ktoré obsahujú ```Spring``` triedy.

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

## Nastavenie JPA

V triede ```JpaDBConfig``` (technicky je jedno ako sa volá, musí byť ale v package, ktorý je nastavený v ```SpringConfig``` v sekcii ```@ComponentScan```) je podobne potrebné v anotácii ```@EnableJpaRepositories.basePackages``` nastaviť packages obsahujúce ```Spring DATA``` repozitáre. Do ```emf.setPackagesToScan``` je potrebné pridať `packages` obsahujúce `JPA` entity (zvyčajne sú to rovnaké `packages`).

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

### Vlastné databázové spojenie

Klientské entity môžu používať samostatné DB spojenie definované v `poolman.xml`, napríklad `jpa_data`. V uvedenom príklade nastavte názov existujúceho spojenia:

```java
String dataSourceName = "jpa_data";
```

Prepojenie repozitárov, entít a transakcií určuje klientský `JpaDBConfig`:

| Nastavenie | Význam v uvedenom príklade |
| --- | --- |
| `emf.setDataSource(...)` | DB spojenie `jpa_data` získané cez `DBPool`. |
| `emf.setPackagesToScan(...)` | Balíky klientských JPA entít. |
| `entityManagerFactoryRef = "basecmsEntityManager"` | Klientské repozitáre používajú tento `EntityManagerFactory`. |
| `transactionManagerRef = "basecmsTransactionManager"` | Transakcie repozitárov riadi tento manažér, naviazaný na rovnaký `EntityManagerFactory`. |

Názov DB spojenia `jpa_data` a názvy Spring beanov `basecmsEntityManager` či `basecmsTransactionManager` sú samostatné nastavenia a nemusia sa zhodovať. Ak už klient toto prepojenie používa, oprava vytvárania predvoleného `TransactionTemplate` nevyžaduje zmenu jeho `poolman.xml` ani repozitárov.

### Transakcie v klientskych službách

Pri spojení viacerých operácií nad klientskými repozitármi do jednej transakcie určite manažér na metóde služby pomocou Spring anotácie `org.springframework.transaction.annotation.Transactional`:

```java
@Transactional("basecmsTransactionManager")
public void saveOrder() {
    // Update customer entities through their repositories.
}
```

Hodnota anotácie je názov transakčného manažéra, nie názov DB spojenia. Samotné `@Transactional` potrebuje jednoznačný predvolený manažér; nastavenie `transactionManagerRef` na repozitároch neurčuje manažér anotácie na službe. Pri viacerých manažéroch bez určeného predvoleného manažéra preto použite explicitný názov.

WebJET kvôli predvolenému `TransactionTemplate` neoznačuje CMS manažér ako `@Primary`. Existujúci klientský `@Primary` zostáva zachovaný. Vytvorenie šablóny nemení výber manažéra pre `@Transactional` ani pre klientské repozitáre.

### Programové transakcie

Pre programové riadenie transakcií vytvára WebJET predvolený bean `transactionTemplate`, ktorý je explicitne naviazaný na `webjet2022TransactionManager`. Používa teda CMS databázu aj vtedy, keď má klient vlastný transakčný manažér označený ako `@Primary`. Databáza šablóny sa nevyberá podľa entít alebo repozitárov použitých vo volaní `execute(...)`.

Ak klient potrebuje `TransactionTemplate` pre svoju databázu `jpa_data`, pridá do svojho `JpaDBConfig` vlastný bean s klientským manažérom:

```java
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

// Add this bean method to JpaDBConfig.
@Bean
public TransactionTemplate customerTransactionTemplate(
        @Qualifier("basecmsTransactionManager") PlatformTransactionManager manager) {
    return new TransactionTemplate(manager);
}
```

Klientské konfigurácie sa spracujú pred vyhodnotením podmienok automatickej konfigurácie. Ak klient už definuje vlastný bean typu `TransactionOperations` (vrátane `TransactionTemplate`), WebJET predvolenú šablónu nevytvorí, bez ohľadu na názov klientského beanu. Existujúcu klientskú šablónu preto nie je potrebné premenovať. Pri viacerých vlastných šablónach vyberte pri injektovaní konkrétnu cez `@Qualifier("customerTransactionTemplate")`.

Vlastnú šablónu potrebujete iba pri použití programových transakcií nad klientskou databázou. Pre klientské repozitáre a služby so správne zvoleným manažérom v `@Transactional` ju netreba vytvárať.

## Nastavenie SpringSecurity

Ak potrebujete upraviť nastavenie pre ```SpringSecurity``` môžete vo vašej triede ```SpringConfig``` implementovať ```sk.iway.iwcm.system.spring.ConfigurableSecurity```. V metóde ```configureSecurity(HttpSecurity http)``` máte dostupný objekt ```HttpSecurity``` v ktorom môžete doplniť potrebné nastavenia:

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
