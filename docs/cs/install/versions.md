# Předpoklady a verze

Aktuální verze WebJET CMS vyžaduje `Java 17` a `Tomcat 11`.

Základní projekt ve formátu gradle naleznete na [githubu webjetcms/basecms](https://github.com/webjetcms/basecms).

V gradle projektech stačí zadat verzi v build.gradle:

```gradle
ext {
    webjetVersion = "2026.0";
}
```

Přičemž aktuálně existují následující verze WebJET:

- `2026.0-boot-SNAPSHOT` - ​​vývojová verze se Spring Boot 4. Přechod z `jakarta` verze vyžaduje [úpravy zákaznického projektu](#změny-při-přechodu-na-spring-boot).
- `2026.18.28-jakarta` - ​​stabilizovaná verze 2026.18 s opravami z verze 2026.0.28, nepřibývají do ní denní změny.
- `2026.0.28-jakarta` - ​​stabilizovaná verze 2026.0.28 s opravami chyb vůči verzi 2026.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2026.0.28` - ​​stabilizovaná verze 2026.0.28 s opravami chyb vůči verzi 2026.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2026.18.25-jakarta` - ​​stabilizovaná verze 2026.18 s opravami z verze 2026.0.25, nepřibývají do ní denní změny.
- `2026.0.25-jakarta` - ​​stabilizovaná verze 2026.0.25 s opravami chyb vůči verzi 2026.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2026.0.25` - ​​stabilizovaná verze 2026.0.25 s opravami chyb vůči verzi 2026.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2026.18-jakarta` - ​​stabilizovaná verze 2026.18, nepřibývají do ní denní změny.
- `2026.0.18-jakarta` - ​​stabilizovaná verze 2026.0.18 s opravami chyb vůči verzi 2026.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2026.0.18` - ​​stabilizovaná verze 2026.0.18 s opravami chyb vůči verzi 2026.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2026.0-jakarta-SNAPSHOT` - ​​pravidelně aktualizovaná verze z main repozitáře s využitím `Jakarta namespace`. Vyžaduje Tomcat 11, dostupná jako [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502?version=2026.0-jakarta-SNAPSHOT)
- `2026.0-SNAPSHOT` - ​​aktualizovaná verze z `hotfix/2026.0` s opravami verze `2026.0` pro Tomcat9/Java 17.
- `2026.0-jakarta` - ​​stabilizovaná verze 2026.0 pro aplikační server Tomcat 11 s využitím `Jakarta namespace`, nepřibývají do ní denní změny.
- `2026.0` - ​​stabilizovaná verze 2026.0, nepřibývají do ní denní změny.
- `2025.0-jakarta-SNAPSHOT` - ​​stabilizovaná verze 2025.52 s využitím `Jakarta namespace`. Vyžaduje Tomcat 10/11, dostupná jako [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502?version=2025.0-jakarta-SNAPSHOT)
- `2025.0-SNAPSHOT` - ​​stabilizovaná verze 2025.52, dostupná jako [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502?version=2025.0-SNAPSHOT)
- `2025.0.52` - ​​stabilizovaná verze 2025.0.52 s opravami chyb vůči verzi 2025.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2025.0.50` - ​​stabilizovaná verze 2025.0.50 s opravami chyb vůči verzi 2025.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2025.40-jakarta` - ​​stabilizovaná verze 2025.40 pro aplikační server Tomcat 10/11 s využitím `Jakarta namespace`, nepřibývají do ní denní změny.
- `2025.40` - ​​stabilizovaná verze 2025.40, nepřibývají do ní denní změny.
- `2025.0.40` - ​​stabilizovaná verze 2025.0.40 s opravami chyb vůči verzi 2025.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2025.18` - ​​stabilizovaná verze 2025.18, nepřibývají do ní denní změny.
- `2025.0.23` - ​​stabilizovaná verze 2025.0.23 s opravami chyb vůči verzi 2025.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2025.0` - ​​stabilizovaná verze 2025.0, nepřibývají do ní denní změny.
- `2024.52` - ​​stabilizovaná verze 2024.52, nepřibývají do ní denní změny.
- `2024.0.52` - ​​stabilizovaná verze 2024.0.52 s opravami chyb vůči verzi 2024.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2024.0.47` - ​​stabilizovaná verze 2024.0.47 s opravami chyb vůči verzi 2024.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2024.40` - ​​stabilizovaná verze 2024.40, nepřibývají do ní denní změny.
- `2024.0-SNAPSHOT` - ​​pravidelně aktualizovaná verze z master repozitáře verze 2024.0 zkompilovaná s Java verze 17.
- `2024.18` - ​​stabilizovaná verze 2024.18, nepřibývají do ní denní změny.
- `2024.0.34` - ​​stabilizovaná verze 2024.0.34 s opravami chyb vůči verzi 2024.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2024.0.21` - ​​stabilizovaná verze 2024.0.21 s opravami chyb vůči verzi 2024.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2024.0.17` - ​​stabilizovaná verze 2024.0.17 s opravami chyb vůči verzi 2024.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2024.0.9` - ​​stabilizovaná verze 2024.0.9 s opravami chyb vůči verzi 2024.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2024.0` - ​​stabilizovaná verze 2024.0 (technicky shodná s 2023.52-java17), nepřibývají do ní denní změny, zkompilovaná s Java verze 17.
- `2023.52-java17` - ​​stabilizovaná verze 2023.52, nepřibývají do ní denní změny, zkompilovaná s Java verze 17.
- `2023.52` - ​​stabilizovaná verze 2023.52, nepřibývají do ní denní změny.
- `2023.40-SNAPSHOT-java17` - ​​pravidelně aktualizovaná verze z master repozitáře verze 2023.40 zkompilovaná s Java verze 17.
- `2023.40-SNAPSHOT` - ​​pravidelně aktualizovaná verze z master repozitáře verze 2023.40.
- `2023.40` - ​​stabilizovaná verze 2023.40, nepřibývají do ní denní změny.
- `2023.18-SNAPSHOT-java17` - ​​pravidelně aktualizovaná verze z master repozitáře verze 2023.18 zkompilovaná s Java verze 17.
- `2023.18-SNAPSHOT` - ​​pravidelně aktualizovaná verze z master repozitáře verze 2023.18.
- `2023.18` - ​​stabilizovaná verze 2023.18, nepřibývají do ní denní změny.
- `2023.0-SNAPSHOT` - ​​pravidelně aktualizovaná verze z master repozitáře verze 2023.0, z důvodu API změn tato verze končí před vydáním verze 2023.18 aby nedošlo k neočekávané změně API v projektech.
- `2023.0` - ​​stabilizovaná verze 2023.0, nepřibývají do ní denní změny.
- `2022.0-SNAPSHOT` - ​​pravidelně aktualizovaná verze z master repozitáře.
- `2022.52` - ​​stabilizovaná verze 2022.52, nepřibývají do ní denní změny.
- `2022.40` - ​​stabilizovaná verze 2022.40, nepřibývají do ní denní změny.
- `2022.18` - ​​stabilizovaná verze 2022.18, nepřibývají do ní denní změny.
- `2022.0` - ​​stabilizovaná verze 2022.0, nepřibývají do ní denní změny.
- `2021.0-SNAPSHOT` - ​​pravidelně aktualizovaná verze z master repozitáře.
- `2021.52` - ​​stabilizovaná verze 2021.52, nepřibývají do ní denní změny.
- `2021.40` - ​​stabilizovaná verze 2021.40, nepřibývají do ní denní změny.
- `2021.13` - ​​stabilizovaná verze 2021.13, nepřibývají do ní denní změny.

Pro čísla verzí platí:

- `YEAR.0.x` - ​​opravná verze, **nepřibývají do ní nové vlastnosti**, během roku jsou v ní opravovány nalezené chyby ve WebJET CMS. Použité knihovny jsou v případě potřeby aktualizovány pouze v rámci `minor` verze. Pokud oprava knihovny vyžaduje změnu v `major` verzi nemůže být do této verze zapracována, protože to může nést riziko změn v `API`.
- `YEAR.0-SNAPSHOT` - ​​vývojová verze která **obsahuje nové vlastnosti** a opravy chyb z verze `YEAR.0.x`.
- `YEAR.WEEK` - ​​**stabilizovaná verze** z daného týdne která vznikne ze `SNAPSHOT` verze po úspěšném vícenásobném testování. Opravy dalších chyb budou zapracovány do další verze, nevznikne opravná `YEAR.WEEK.x` ale nová `YEAR.WEEK` s novým číslem. V případě chyby v takové verzi je tedy třeba počítat s přechodem na další stabilní verzi `YEAR.WEEK` nebo na `YEAR.0-SNAPSHOT` verzi.

Verze `YEAR.0.x` se tedy zásadně nemění, obsahuje opravy chyb (pokud oprava nevyžaduje zásadní změnu). Je vhodná pro použití u zákazníka, který chce mít stabilní verzi WebJETu bez přidávání nových vlastností během roku.

Zároveň ale nemusí být verze `YEAR.0.x` nejbezpečnější. Pokud je třeba aktualizovat použitou knihovnu ve WebJETu a ta obsahuje zásadnější změny nemůžeme tuto změnu provést v `YEAR.0.x` verzi, protože by se porušila kompatibilita.

Platí tedy, že `YEAR.0.x` je **nejstabilnější** z pohledu změn a `YEAR.0-SNAPSHOT` je **nejbezpečnější** z pohledu zranitelností.

## Změny při přechodu na Spring Boot

Přechod ze samostatné konfigurace Spring Framework 7 na Spring Boot 4 mění spouštění aplikace, správu závislostí a vytváření WAR archivů. Nestačí proto změnit pouze `webjetVersion`. Postup vychází z [migračního pull requestu basecms](https://github.com/webjetcms/basecms/pull/2/files) ; soubory pro zkopírování naleznete ve větvi [release/webjet-2026-boot](https://github.com/webjetcms/basecms/tree/release/webjet-2026-boot).

Předpokladem je projekt, který již používá Jakarta verzi se Spring 7. Java zůstává ve verzi 17 nebo novější. Referenční projekt používá Spring Boot `4.1.1`, Gradle `8.14` a Tomcat `11.0.25`. Pokud přecházíte ze starší verze s `javax` balíky, nejprve proveďte i [přechod na Jakarta verzi](#změny-při-přechodu-na-jakarta-verzi).

### Gradle a závislosti

Ještě před úpravou `build.gradle` aktualizujte Gradle wrapper na verzi `8.14`. V kořenové složce projektu spusťte:

```sh
./gradlew wrapper --gradle-version 8.14 --distribution-type bin
./gradlew wrapper --gradle-version 8.14 --distribution-type bin
./gradlew --version
```

První spuštění nastaví požadovanou verzi v `gradle-wrapper.properties`. Druhé již použije Gradle `8.14` a aktualizuje také `gradle-wrapper.jar` a spouštěcí skripty `gradlew` a `gradlew.bat`. Tento dvoukrokový postup doporučuje také [dokumentace Gradle](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:upgrading_wrapper). Posledním příkazem ověříte použitou verzi. Ve Windows nahraďte `./gradlew` za `gradlew.bat`.

V `build.gradle` odstraňte plugin `org.gretty`, celý blok `gretty { ... }` i konfigurace `grettyRunnerTomcat10` a `grettyRunnerTomcat11`, pokud jejich projekt obsahuje. Odstraňte také vazby na původní Gretty úlohy, například konfiguraci JaCoCo pro `appStart`, `appStartDebug` a `appAfterIntegrationTest`. Do stávajících bloků zapracujte tato nastavení; ostatní projektové pluginy a vlastní úkoly zachovejte:

```gradle
plugins {
    id 'java'
    id 'war'
    id 'org.springframework.boot' version '4.1.1'
    id 'io.freefair.lombok' version '8.14'
}

ext {
    webjetVersion = '2026.0-boot-SNAPSHOT'
    tomcatMinimumVersion = '11.0.25'
}

springBoot {
    mainClass = 'sk.iway.iwcm.system.spring.SpringBootStarter'
}

tasks.named('bootJar') { enabled = false }
tasks.named('jar') { enabled = false }
```

Vlastní spouštěcí třídu s `main()` není třeba přidávat. WebJET ji poskytuje v knihovně a kvůli JSP stránkám se aplikace nadále balí do WAR archivu.

Odstraňte proměnnou `springVersion` a ruční verze Spring závislostí, které nahrazuje správa verzí Spring Boot. V `dependencies` přidejte Boot BOM, který sjednotí verze knihoven, a závislosti pro vestavěný Tomcat a JSP:

```gradle
dependencies {
    implementation platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    providedCompile platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    providedRuntime platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)

    providedRuntime 'org.springframework.boot:spring-boot-starter-tomcat-runtime'
    providedRuntime('org.apache.tomcat.embed:tomcat-embed-jasper') {
        exclude group: 'org.apache.tomcat', module: 'tomcat-annotations-api'
    }
    constraints {
        ['implementation', 'providedRuntime'].each { configurationName ->
            ['core', 'el', 'jasper', 'websocket'].each { moduleName ->
                add(configurationName, "org.apache.tomcat.embed:tomcat-embed-${moduleName}") {
                    version {
                        require("[${tomcatMinimumVersion},12.0.0)")
                        prefer(tomcatMinimumVersion)
                    }
                }
            }
        }
    }

    implementation("com.webjetcms:webjetcms:${webjetVersion}")
    implementation("com.webjetcms:webjetcms:${webjetVersion}:admin")
    implementation("com.webjetcms:webjetcms:${webjetVersion}:components")
    implementation("com.webjetcms:webjetcms:${webjetVersion}:libs")

    providedCompile 'jakarta.servlet:jakarta.servlet-api'
    providedCompile 'jakarta.servlet.jsp:jakarta.servlet.jsp-api:4.0.0'
    providedCompile 'jakarta.el:jakarta.el-api:6.0.0'
    providedCompile 'jakarta.annotation:jakarta.annotation-api'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

Původní samostatné testovací závislosti na Spring, JUnit BOM, JUnit Jupiter, Hamcrest, Mockito a testovací implementaci Jakarta EL nahraďte uvedeným `spring-boot-starter-test` a JUnit launcherem. Vlastní knihovny potřebné pro zákaznické testy zachovejte. Ponechte také nastavení kompilátoru `options.compilerArgs += ['-parameters']`.

Pro oba repozitáře nastavte přihlašovací údaje s přístupem ke čtení balíků přes `GPR_USER` a `GPR_API_KEY`, případně `gpr.user` a `gpr.api-key` v lokálním `gradle.properties`.

Z [referenčního build.gradle](https://github.com/webjetcms/basecms/blob/release/webjet-2026-boot/build.gradle) stáhněte také tyto části:

- `bootRun`, výpočet `worktreeId`, úlohu `generateCertificate` a vazbu `bootRun.dependsOn generateCertificate`. Zabezpečují lokální spuštění, ladění na portu `5005`, výběr databázového spojení a vytvoření vývojového HTTPS certifikátu. V `jvmArgs` upravte zákaznické hodnoty, zejména `webjet.smtpServer` a případná další `webjet.*` nastavení.
- Společné nastavení WAR archivů přes `tasks.withType(org.gradle.api.tasks.bundling.War).configureEach { ... }` místo původního bloku `war { ... }`. Tak se pravidla balení použijí na `war` i `bootWar`. Zachovejte vlastní pravidla projektu a vyloučení lokálních `poolman-*.xml` ; pravidla určená k vyřazení ukázkových souborů basecms nepřenášejte na zákaznické soubory.
- Blok `processResources` s vyloučením `certificates/https-keystore.p12`. Vývojový certifikát se generuje do `build/certificates` a nepatří do distribučního archivu.
- Úlohu `verifyBootWar`, která vytvoří a zkontroluje oba WAR archivy včetně WebJET knihoven, konfigurace logování a oddělení knihoven `Tomcat`.

Při balení také změňte vyloučení `**/logback-*.xml` na `**/logback-local*.xml`, jak je popsáno níže. Pokud používáte úlohy `prepareDataForPublicWar`, `explodeWar` nebo vlastní nasazovací skripty, upravte je tak, aby pracovaly s výstupem úlohy `war`. V Gradle získáte jeho cestu přes:

```gradle
tasks.named('war').get().archiveFile.get().asFile
```

Tím se vyhnete odkazu na původní název archivu, který po migraci patří výstupu `bootWar`.

### Třídy JpaDBConfig a SpringConfig

V zákaznické třídě `JpaDBConfig` nastavte vlastní, jedinečný název persistence unit. Do třídy přidejte konstantu, například:

```java
private static final String PERSISTENCE_UNIT_NAME = "basecms";
```

V metodě `entityManagerFactory()` ji nastavte na vytvářeném `LocalContainerEntityManagerFactoryBean`, za nastavením JPA adaptéru:

```java
emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
emf.setPersistenceUnitName(PERSISTENCE_UNIT_NAME);
```

Hodnotu `basecms` nahraďte názvem vlastního projektu. Máte-li více JPA konfigurací, každé dejte odlišný název persistence unit. Zachovejte vlastní balíky v `@EnableJpaRepositories.basePackages` a `emf.setPackagesToScan(...)`. Názvy `entityManagerFactoryRef` a `transactionManagerRef` musí odpovídat příslušným anotacím `@Bean` a být jedinečné v aplikaci. V ukázce jsou to `basecmsEntityManager` a `basecmsTransactionManager`, samotná konfigurace má název `@Configuration("basecms:JpaDBConfig")`.

Do výběru databázové platformy doplňte větev pro PostgreSQL před výchozí větví pro MySQL:

```java
} else if (Constants.DB_TYPE == Constants.DB_PGSQL) {
    properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.PostgreSQL);
} else {
    properties.setProperty(PersistenceUnitProperties.TARGET_DATABASE, TargetDatabase.MySQL);
}
```

Vlastní nastavení `WebJETPersistenceProvider`, databázového spojení a EclipseLink ponechte. Kompletní ukázku naleznete v [JpaDBConfig.java](https://github.com/webjetcms/basecms/blob/release/webjet-2026-boot/src/main/java/sk/iway/basecms/JpaDBConfig.java).

Ve třídě `SpringConfig` doplňte explicitní název konfigurace, aby se neshodoval s konfigurací WebJET CMS. Namísto samotného `@Configuration` použijte například:

```java
@Configuration("basecmsSpringConfig")
@ComponentScan({
    "sk.iway.basecms",
    "sk.iway.basecms.contact"
})
public class SpringConfig {
}
```

Název beanu i skenované balíky přizpůsobte projektu. Stávající zákaznické metody a beany ve třídě zachovejte.

### Konfigurace aplikace a web.xml

Do `src/main/resources` zkopírujte [application.properties](https://github.com/webjetcms/basecms/blob/release/webjet-2026-boot/src/main/resources/application.properties). Pokud jej již máte, slučte nastavení. Soubor obsahuje konfiguraci vestavěného Tomcatu, JSP, kódování, chybových stránek a ukládání relací. Pro lokální spouštění si zkontrolujte zejména:

| Nastavení | Význam a úprava v projektu |
| --- | --- |
| `server.port=443` | HTTPS port vestavěného Tomcatu. |
| `webjet.server.http-redirect.enabled=true`, `webjet.server.http-redirect.port=80` | Zapnutí a port souběžného HTTP konektoru. Navzdory názvu nejde o plošné přesměrování na HTTPS; přesměrování aplikace nadále řídí nastavení WebJET CMS, například `adminRequireSSL`. |
| `server.ssl.key-store=${WEBJET_KEYSTORE_PATH}` | Cesta k certifikátu; při `bootRun` ji nastavuje Gradle na soubor v `build/certificates`. |
| `server.ssl.key-store-password=${WEBJET_HTTPS_KEYSTORE_PASSWORD:changeit}` | Heslo vývojového certifikátu. Proměnnou používá také role `generateCertificate`. |
| `server.tomcat.max-part-count=1000` | Limit počtu částí multipart požadavky, například při odesílání formulářů a souborů. |
| `server.servlet.session.store-dir=${user.dir}/work/sessions` | Složka pro uložení pořadů při korektním vypnutí vestavěného Tomcatu; každá instance má mít vlastní složku. |

Porty a TLS externího Tomcatu se nadále nastavují v jeho `conf/server.xml`. Do `.gitignore` doplňte `/work/` a `/logs/`, aby se provozní soubory neukládaly do Gitu.

Z mapování `StripesFilter` odstraňte `<dispatcher>ERROR</dispatcher>` a ponechte `REQUEST`. Doplňte také MIME mapování přípony `properties` na `text/plain`. Ostatní zákaznické filtry, servlety a mapování zachovejte; konkrétní změny jsou v [web.xml referenčního projektu](https://github.com/webjetcms/basecms/blob/release/webjet-2026-boot/src/main/webapp/WEB-INF/web.xml).

### Logování

Přejmenujte `src/main/resources/logback.xml` na `src/main/resources/logback-spring.xml`. Obsah se v referenčním projektu nemění, takže ponechte vlastní appendery, formát zpráv i nastavené úrovně logování. Název s příponou `-spring` umožní, aby načtení konfigurace řídil Spring Boot; tato varianta doporučuje také [dokumentace Spring Boot](https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.custom-log-configuration).

Ve společném nastavení WAR archivů nahraďte původní pravidlo `rootSpec.exclude('**/logback-*.xml')` tímto:

```gradle
rootSpec.exclude('**/logback-local*.xml')
```

Původní pravidlo by vyřadilo i nový `logback-spring.xml` a nasazená aplikace by tak přišla o zákaznickou konfiguraci logování. Máte-li další lokální nebo testovací konfigurace, například `logback-test.xml`, vylučte je zvlášť. Výsledný WAR musí obsahovat `WEB-INF/classes/logback-spring.xml`.

### Spuštění a vytvoření distribuce

Lokální databázové spojení nastavte v `src/main/resources/poolman-local.xml` a aplikaci spusťte:

```sh
./gradlew bootRun
```

Výchozí soubor spojení je `/poolman-local.xml` ; jiný vyberete proměnnou prostředí `webjetDbname`. Server standardně poslouchá na HTTP portu `80`, HTTPS portu `443` a ladícím portu `5005`. Zastavíte jej přes `Ctrl+C` nebo skriptem `app-stop.sh`, který zkopírujte do kořene projektu. Na macOS/Linux zachovejte právo spouštění skriptů. Ve Windows používejte `gradlew.bat`.

Pro vytvoření a kontrolu distribuce spusťte:

```sh
./gradlew clean verifyBootWar
```

Při názvu projektu `basecms` vzniknou v `build/libs` tyto archivy:

| Archiv | Použití |
| --- | --- |
| `basecms-plain.war` | Výstup úlohy `war`, určený k nasazení do externího Tomcatu 11. Neobsahuje knihovny vestavěného Tomcatu. |
| `basecms.war` | Výstup úlohy `bootWar`, který má knihovny vestavěného Tomcatu odděleny v `WEB-INF/lib-provided`. Lze jej nasadit i do externího Tomcatu. |
| `basecms-public.war` | Veřejná část bez administrace, vytvářená zvlášť příkazem `./gradlew buildAllArtifacts` spolu s `basecms-plain.war`. |

Názvy vycházejí z `rootProject.name` v `settings.gradle` ; pokud projekt nastavuje vlastní názvy archivů nebo verzi, zohledněte je v nasazovacích skriptech. Pro běžné nasazení použijte `-plain.war` a zachovejte dosavadní kontext aplikace, například nasazením jako `ROOT.war`. Databázové spojení nastavte stávajícím způsobem na serveru, protože lokálně `poolman-*.xml` se do WAR nebalí.

!> V této verzi zatím není podporováno přímé spuštění zabaleného WAR přes `java -jar`. WebJET CMS potřebuje rozbalenou webovou složku pro práci se soubory. Používejte `bootRun` při vývoji a WAR nasazený do externího Tomcatu.

Po migraci ověřte start bez chyb v logu, přihlášení do administrace, zobrazení JSP stránek, zákaznické REST služby, práci s databází a nahrávání souborů. Úloha `verifyBootWar` kontroluje obsah archivů, nenahrazuje funkční ověření aplikace.

### Volitelné soubory pro VS Code a Docker

Tyto soubory stáhněte podle toho, jak projekt vyvíjíte a nasazujete:

- **VS Code:** slučte `.vscode/tasks.json` a `.vscode/launch.json` z referenčního projektu. Spouštěcí úlohy používají `bootRun` místo `appStartDebug`, zastavení na macOS/Linux používá `app-stop.sh`. Zkontrolujte hodnoty `projectName` podle Java projektu ve VS Code a zachovejte vlastní ladící konfigurace.
- **Lokální Tomcat v Dockeru:** zkopírujte celou složku `.devcontainer/tomcat/`. V `start-tomcat.sh` upravte `container_name`, `image_name` a `war_file`, který standardně ukazuje na `build/libs/basecms-plain.war`. Zkontrolujte také `poolman_file` a zákaznické hodnoty v `tomcat_jvm_args`, zejména SMTP server. Stejný název kontejneru nastavte v roli `Docker Tomcat 11 Stop` v `.vscode/tasks.json`. Skript sestaví archivy a image, vloží databázovou konfiguraci a vývojový certifikát a spustí Tomcat na portech `80` a `443`. Pokud databáze běží na hostitelském počítači, v `poolman-local.xml` použijte místo `localhost` adresu `host.docker.internal`.
- **Databáze pro vývoj:** podle používané databáze stáhněte příslušné soubory z `.devcontainer/db/` a nastavte `WEBJET_DB_PASS`. Postup je v tamním [README](https://github.com/webjetcms/basecms/blob/release/webjet-2026-boot/.devcontainer/db/README.md). Samostatný `docker-compose-rag-pgsql.yml` potřebujete pouze při použití oddělené PostgreSQL databáze pro RAG. Tyto kontejnery nejsou podmínkou migrace stávajícího databázového serveru.
- **Docker image aplikace:** pokud používáte existující řešení ze složky `docker/`, stáhněte změny v `docker/Dockerfile` a `docker/docker-compose.yml`. Dockerfile používá Tomcat 11, odpovídající adaptér Redisson a rozbaluje `-plain.war` a `-public.war`. Nastavte `ARTIFACT_NAME` podle názvu Gradle projektu v obou částech Dockerfile nebo přes `build.args.ARTIFACT_NAME` v Compose. Upravte název image a hodnoty z `docker/.env.example`, zejména `PROJECT_NAME`, databázové údaje, porty a časové pásmo. Při prvním stažení tohoto řešení zkopírujte celou složku `docker/`, protože Dockerfile také používá jeho konfiguraci Tomcatu a databázového spojení.

Pro stávající databázi používejte běžný start. Při instalaci do nové prázdné databáze je ve VS Code připraveno `Run SETUP`, případně `Docker Tomcat 11 SETUP Start`. Nastavují režim instalace a vyžádají token o délce alespoň 16 znaků. Při ručním spuštění použijte proměnné `WEBJET_SETUP_ENABLED=true` a `WEBJET_SETUP_TOKEN`. Na `/wjerrorpages/setup/setup` se přihlaste jménem `setup` a tokenem jako heslem. Po dokončení aplikaci zastavte, vypněte režim SETUP a spusťte ji běžným způsobem.

## Změny při přechodu na Tomcat 9.0.104+

V [Tomcat od verze 9.0.104](https://tomcat.apache.org/tomcat-9.0-doc/config/http.html) je změněna kontrola počtu parametrů při `multipart` HTTP požadavku. Je proto třeba nastavit/zvýšit parametr `maxPartCount` na `<Connector` elementu s souboru `tomcat/conf/server.xml` na hodnotu minimálně 100, příklad:

```xml
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443"
               maxPartCount="1000"
               URIEncoding="UTF-8"
               useBodyEncodingForURI="true" relaxedQueryChars="^{}[]|&quot;"
    />
```

## Změny při přechodu na Jakarta verzi

Verze určená pro `jakarta namespace`, vyžaduje aplikační server Tomcat 11, používá Spring verze 7. Průlomové změny:

- URL adresy - pro URL adresy Spring zavedl přesné shody, pokud REST služba definuje URL adresu s lomítkem na konci, musí být takto použita. Je rozdíl v URL adrese `/admin/rest/service` a `/admin/rest/service/`.
- Ve Spring DATA repozitářích pro `IN/NOTIN query` je třeba přidat `@Query`, jinak nebude korektně SQL vytvořeno, příklad:

```java
  //old
  Page<DocDetails> findAllByGroupIdIn(int[] groupIds, Pageable pageable);
  List<UserDetailsEntity> findAllByIdIn(List<Long> ids);

  //new - add @Query and @Param to correctly create JPQL query for Eclipselink
  @Query("SELECT d FROM DocDetails d WHERE d.groupId IN :groupIds")
  Page<DocDetails> findAllByGroupIdIn(@Param("groupIds") int[] groupIds, Pageable pageable);

  @Query(value = "SELECT u FROM UserDetailsEntity u WHERE u.id IN :ids")
  List<UserDetailsEntity> findAllByIdIn(@Param("ids") List<Long> ids);
```

Pro vyhledání v kódu můžete použít hledání v souborech `*Repository.java` a hledat regulární výraz `\(.*List[^)]*\)`, `\(.*Long\[\][^)]*\)`, `\(.*Integer\[\][^)]*\)`. Doporučujeme provést kód, v logu se zobrazí chyba a použít vygenerované SQL do `Query` hodnoty. Problémem je pouze kontrola typu, kde `EclipseLink` neumí identifikovat, že má kontrolovat pole/seznam a ne přímo datový typ.

V `build.gradle` je třeba aktualizovat `gretty` konfiguraci a přidat nastavení kompilace `options.compilerArgs += ['-parameters']`:

```gradle
plugins {
    id 'org.gretty' version "5.0.1"
}

configurations {
    grettyRunnerTomcat11 {
    }
}

gretty {
    servletContainer = 'tomcat11'
}

tasks.withType(JavaCompile) {
    //prevent warning messages during compile
    options.compilerArgs += ['-Xlint:none']
    //needed for Spring
    options.compilerArgs += ['-parameters']
}
```

do `configurations` elementu si přidejte výjimku pro `log4j-core`:

```gradle
configurations {
    all*.exclude group: 'org.slf4j', module: 'slf4j-log4j12'
    all*.exclude group: 'org.slf4j', module: 'jcl104-over-slf4j' //je nahradene novsim jcl-over-slf4j
    all*.exclude group: 'commons-logging', module: 'commons-logging'
    all*.exclude group: 'log4j', module: 'log4j'
    all*.exclude group: 'org.apache.logging.log4j', module: 'log4j-core'
```

## Změny při přechodu na 2025.0-SNAPSHOT

Verze `2025.0-SNAPSHOT` je dostupná přes [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/2426502?version=2025.0-SNAPSHOT), je proto třeba doplnit konfiguraci do vašeho `build.gradle` souboru:

```gradle
repositories {
    mavenCentral()
    maven {
        url "https://pd4ml.tech/maven2/"
    }
    maven {
        name = "github"
        url = uri("https://maven.pkg.github.com/webjetcms/webjetcms")
        credentials {
            //define in gradle.properties or as environment variables
            username = project.findProperty("gpr.user") ?: System.getenv("GPR_USER")
            password = project.findProperty("gpr.api-key") ?: System.getenv("GPR_API_KEY")
        }
    }
    flatDir {
       dirs 'libs'
   }
}
```

Bohužel GitHub Packages nejsou veřejně dostupné, je proto nutné nastavit přihlašovací údaje `gpr.user` a `gpr.api-key` v souboru `gradle.properties` nebo přes `ENV` proměnné. Přihlašovací údaje vám poskytneme na vyžádání.

!> **Upozornění:** upravená inicializace Spring a JPA:

- JPA entity se v `package sk.iway.INSTALL-NAME` neinicializují automaticky, předpokládá se postupný přechod na Spring DATA. Pokud potřebujete inicializovat `@Entity`, nastavte konfigurační proměnnou `jpaAddPackages` na potřebnou hodnotu - například `sk.iway.INSTALL-NAME`. Inicializují se pouze třídy obsahující anotaci `@Entity` nebo `@Converter`.
- Ve `web.xml` již není nutná inicializace `Apache Struts`, smažte celou `<servlet>` sekci obsahující `<servlet-class>org.apache.struts.action.ActionServlet</servlet-class>` a `<servlet-mapping>` obsahující `<servlet-name>action</servlet-name>`.
- Upravené pořadí inicializace Spring - inicializace WebJET tříd se provede před zákaznickými třídami `SpringConfig`.
- Upravená inicializace `Swagger` - ​​pokud není nastavena konfigurační proměnná `swaggerEnabled` na hodnotu `true` ani se při startu neprovede prohledání Java tříd.

## Změny při přechodu na 2024.0-SNAPSHOT

Podobně jako pro [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms) verzi je třeba přidat do `dependencies` bloku část `implementation("sk.iway:webjet:${webjetVersion}:libs")`:

```gradle
dependencies {
    implementation("sk.iway:webjet:${webjetVersion}")
    implementation("sk.iway:webjet:${webjetVersion}:admin")
    implementation("sk.iway:webjet:${webjetVersion}:components")
    implementation("sk.iway:webjet:${webjetVersion}:libs")
}
```

Smazány byly následující knihovny, které nejsou používány ve standardní instalaci, pokud je váš projekt potřebuje, přidejte je do vašeho `build.gradle` souboru:

```gradle
dependencies {
    implementation("com.amazonaws:aws-java-sdk-core:1.12.+")
    implementation("com.amazonaws:aws-java-sdk-ses:1.12.+")
    implementation("bsf:bsf:2.4.0")
    implementation("commons-validator:commons-validator:1.3.1")
    implementation("taglibs:datetime:1.0.1")
    implementation("net.htmlparser.jericho:jericho-html:3.1")
    implementation("joda-time:joda-time:2.10.13")
    implementation("io.bit3:jsass:5.1.1")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("org.mcavallo:opencloud:0.3")
    implementation("org.springframework:spring-messaging:${springVersion}")
    implementation("net.sf.uadetector:uadetector-core:0.9.22")
    implementation("net.sf.uadetector:uadetector-resources:2014.10")
    implementation("cryptix:cryptix:3.2.0")
    implementation("org.springframework:spring-messaging:${springVersion}")
    implementation("com.google.protobuf:protobuf-java:3.21.7")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.apache.taglibs:taglibs-standard-spec:1.2.5")
    implementation("org.apache.taglibs:taglibs-standard-impl:1.2.5")
    implementation('com.mchange:c3p0:0.9.5.5')
    implementation("xerces:xercesImpl:2.12.2")
    implementation 'jakarta.xml.bind:jakarta.xml.bind-api:2.3.3'
    implementation 'com.sun.xml.bind:jaxb-ri:2.3.3'
}
```

V sekci `configurations` smažte výrazy:

```gradle
all*.exclude group: 'xml-apis', module: 'xml-apis'
all*.exclude group: 'javax.xml.stream', module: 'stax-api'
```

## Změny při přechodu na GitHub/Maven Central verzi

- V [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms) je změněno jméno balíků z `sk.iway` na `com.webjetcms` a přidána je část `libs`, která kombinuje všechny původní `sk.iway` závislosti typu `struts,daisydiff,jtidy`. V `build.gradle` upravte:

```gradle
repositories {
    mavenCentral()
    maven {
        url "https://pd4ml.tech/maven2/"
    }
    flatDir {
       dirs 'libs'
   }
}

ext {
    webjetVersion = "2024.0.3";
}

dependencies {
    implementation("com.webjetcms:webjetcms:${webjetVersion}")
    implementation("com.webjetcms:webjetcms:${webjetVersion}:admin")
    implementation("com.webjetcms:webjetcms:${webjetVersion}:components")
    implementation("com.webjetcms:webjetcms:${webjetVersion}:libs")
    ...
}
```

## Změny při přechodu na 2023.18-SNAPSHOT/2023.40

- Ve vašem projektu smažte soubor `src/main/webapp/WEB-INF/struts-config.xml` aby se použil aktuální soubor z WebJETu (z jaře souboru).

## Změny při přechodu na Java 17

Při přechodu na Java verze 17 je třeba ve vašem projektu provést několik změn. Projekt [basecms](https://github.com/webjetcms/basecms/tree/release/webjet-2023-18-java17) má připravenou `branch`, ```release/webjet-2023-18-java17``` s ukázkovou úpravou. V [tomto commit](https://github.com/webjetcms/basecms/commit/e4b9cf6f0a88fd6f0b0cc6c57b28e7a3ec924535) je vidět kompletní seznam změn.

Zjednodušený postup je následující:

Aktualizace `gradle-wrapper` na verzi 8 (z původní 6), doporučujeme nejprve provést aktualizaci na verzi 7 a poté na 8 (přímo na verzi může 8 aktualizace skončit chybou):

```sh
gradlew.bat wrapper --gradle-version 7.4.2
gradlew.bat wrapper --gradle-version 8.1.1
```

Po aktualizaci gradle se vám projekt nepodaří zkompilovat, je třeba upravit i soubor ```build.gradle```, ve kterém je aktualizováno i více ```plugins```. Důležitá změna je přechod WebJET CMS ve verzi ```java17``` na standardní verzi ```eclipselink``` s nastavením WebJET generátoru primárních klíčů.

```groovy
plugins {
    ...
    //aktualizacia verzii pluginov
    id 'org.gretty' version "3.1.1"
    id "io.freefair.lombok" version "8.0.1"
    id "org.owasp.dependencycheck" version "8.2.1"
    ...
}

ext {
    //aktualizovana verzia WebJETu
    webjetVersion = "2023.18-SNAPSHOT-java17";
}

dependencies {
    //celu sekciu modules ZMAZAT
    modules {
        module("org.apache.struts:struts-core") { replacedBy("sk.iway:webjet", "mame integrovane") }
        module("org.apache.struts:struts-taglib") { replacedBy("sk.iway:webjet", "mame integrovane") }
        module("org.eclipse.persistence:org.eclipse.persistence.moxy") { replacedBy("sk.iway:webjet", "mame integrovane") }
        module("org.eclipse.persistence:org.eclipse.persistence.core") { replacedBy("sk.iway:webjet", "mame integrovane") }
        module("org.eclipse.persistence:org.eclipse.persistence.asm") { replacedBy("sk.iway:webjet", "mame integrovane") }
        module("org.eclipse.persistence:org.eclipse.persistence.sdo") { replacedBy("sk.iway:webjet", "mame integrovane") }
    }

    //zmazat pri prechode na verziu 2023.18+
    implementation("sk.iway:webjet:${webjetVersion}:struts")
    implementation("sk.iway:webjet:${webjetVersion}:daisydiff")
    implementation("sk.iway:webjet:${webjetVersion}:jtidy")
}

java {
    //nastavenie verzie Java na 17
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

lombok {
    //nastavenie verzie lombok
    version = "1.18.28"
}

gretty {
    //zakomentovat/zmazat managedClassReload = true - nie je mozne pouzit s Java > 8
    //managedClassReload = true
}
```

Následně doporučujeme restartovat vaše vývojářské prostředí, v případě VS Code provést akci ```Java: Clean Java Language Server Workspace``` pro kompletní smazání dočasných souborů.

Pokud na jednom Tomcat serveru provozujete více instalací WebJETu je možné, že starší verze nebudou plně kompatibilní s Java 17. Pro chyby typu:

```txt
[ERROR] ContextLoader - Context initialization failed <java.lang.IllegalStateException: Cannot load configuration class: sk.iway.iwcm.system.spring.SpringSecurityConf>java.lang.Ill
egalStateException: Cannot load configuration class: sk.iway.iwcm.system.spring.SpringSecurityConf
...
Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make protected final java.lang.Class java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain) throws java.lang.ClassFormatError accessible: module java.base does not "opens java.lang" to unnamed module @4c6e3350
```

nastavte pro Tomcat následující ```JAVA_OPTS```:

```txt
JAVA_OPTS="$JAVA_OPTS --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED"
```

### Aktualizace z Java verze 8

Pokud jste provozovali Tomcat ještě s Java verze 8 mohou vzniknout problémy s chybějícími knihovnami (ty jsou potřebné i pro Java 11). Pokud se vám v logu objeví chyba ```java.lang.NoClassDefFoundError: javax/activation/DataSource```:

```txt
java.util.concurrent.ExecutionException: org.apache.catalina.LifecycleException: Failed to start component [StandardEngine[Catalina].StandardHost[...].StandardContext[]]
    ...
    Caused by: java.lang.NoClassDefFoundError: javax/activation/DataSource
```

je třeba do každé instalace WebJET CMS do složky ```WEB-INF/lib``` zkopírovat knihovny z [tohoto ZIP archivu](lib-java11.zip) a smazat soubory (pokud existují):

```txt
jaxb-api-2.1.jar
jaxb-runtime-3.0.0-M2.jar
```

Pokud jste používali WebJET verze `8.0-8.6` - ​​starší než `08/2019`, nebo se vám zobrazí při startu následující chyba:

```txt
[10.09 13:48:16 {vubintra} {JpaTools}] JPA: adding class: sk.iway.spirit.model.Media
[10.09 13:48:16 {vubintra} {JpaTools}] JPA: adding class: sk.iway.iwcm.io.FileHistoryBean
[10.09 13:48:16 {vubintra} {WebJETJavaSECMPInitializer}] initPersistenceUnits[iwcm], beans=82
[10.09 13:48:16 {vubintra}]  [FAIL]
java.lang.NullPointerException
	at org.eclipse.persistence.internal.jpa.EntityManagerSetupImpl.predeploy(EntityManagerSetupImpl.java:2027)
	at org.eclipse.persistence.internal.jpa.deployment.JPAInitializer.callPredeploy(JPAInitializer.java:100)
	at sk.iway.iwcm.system.jpa.WebJETJavaSECMPInitializer.initPersistenceUnits(WebJETJavaSECMPInitializer.java:307)
	at sk.iway.iwcm.system.jpa.WebJETJavaSECMPInitializer.initialize(WebJETJavaSECMPInitializer.java:134)
	at sk.iway.iwcm.system.jpa.WebJETJavaSECMPInitializer.getJavaSECMPInitializer(WebJETJavaSECMPInitializer.java:95)
	at sk.iway.iwcm.system.jpa.WebJETJavaSECMPInitializer.getJavaSECMPInitializer(WebJETJavaSECMPInitializer.java:61)
	at sk.iway.iwcm.system.jpa.WebJETPersistenceProvider.createEntityManagerFactory(WebJETPersistenceProvider.java:32)
```

je třeba aktualizovat knihovnu `eclipselink` a inicializační Java třídu, stáhněte si aktualizační archiv [jpa-wj82.zip](jpa-wj82.zip) a rozbalte jej v kořenové složce web aplikace. Přepište soubor `eclipselink.jar` a `WebJETJavaSECMPInitializer.class`.

## Změny při aktualizaci na 2023.18

Verze ```2023.18``` mění API a způsob generování distribučních archivů. Hlavní změny API jsou v použití generických objektů typu ```List/Map``` namísto specifických implementací ```ArrayList/Hashtable```. Z toho důvodu je třeba nově zkompilovat vaše třídy a upravit JSP soubory.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/sfu5b_S7Q8Q" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

V ```build.gradle``` je třeba smazat část:

```
    implementation("sk.iway:webjet:${webjetVersion}:struts")
    implementation("sk.iway:webjet:${webjetVersion}:daisydiff")
    implementation("sk.iway:webjet:${webjetVersion}:jtidy")
```

Zkontrolujte také soubor `src/main/resources/logback.xml` ve kterém je třeba upravit formát data a času v `ConsoleAppender` (smazané `,SSS`, pokud potřebujete logovat i setiny sekundy použijte `.SSS`):

```xml
    <pattern>[%X{installName}][%c{1}][%p][%X{userId}] %d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
```

Pro zjednodušení aktualizace můžete použít skript ```/admin/update/update-2023-18.jsp``` pro kontrolu a opravu JSP souborů. Zákaznické Java třídy je třeba nově zkompilovat a opravit chyby z důvodu změny API.

Přečištěno/smazáno je více Java třídy a balíky a příslušné JSP souboru. Pro podporu smazaných částí v projektech je třeba použít buď příslušný produkt typu WebJET NET nebo do projektu je přenést ze zdrojového kódu verze 8.

Více informací je v [seznamu změn](../CHANGELOG-2023.md#odstranění-závislosti-na-verzi-8).

## Změny oproti verzi 8.8

V ```build.gradle``` je oproti verzi 8.8 třeba smazat výrazy:

```gradle
    compile("sk.iway:webjet:${webjetVersion}:swagger-ui")

    compile 'taglibs:standard:1.1.2'
    compile 'javax.servlet:jstl:1.2'

    providedCompile 'org.slf4j:slf4j-log4j12:1.7.25'

    exclude group: 'org.slf4j', module: 'log4j-over-slf4j'
```

av sekci ```configurations``` upravit výjimky následovně:

```gradle
configurations {
    all*.exclude group: 'org.slf4j', module: 'slf4j-log4j12'
    all*.exclude group: 'org.slf4j', module: 'jcl104-over-slf4j' //je nahradene novsim jcl-over-slf4j
    all*.exclude group: 'commons-logging', module: 'commons-logging'
    all*.exclude group: 'log4j', module: 'log4j'
    all*.exclude group: 'org.apache.logging.log4j', module: 'log4j-core'

    //javax.xml.stream:stax-api:1.0-2 -> stax:stax-api:1.0.1
    all*.exclude group: 'javax.xml.stream', module: 'stax-api'

    grettyRunnerTomcat85 {

    }
    grettyRunnerTomcat9 {
        // gretty pouziva staru verziu commons-io, ktora koliduje s nasou
        // https://mvnrepository.com/artifact/commons-io/commons-io
        exclude group: 'commons-io', module: 'commons-io'
    }
}
```

Z důvodu přechodu z ```log4j``` na ```logback``` smažte soubor ```src/main/resources/log4j.properties``` a přidejte soubor ```src/main/resources/logback.xml```:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>[%X{installName}][%c{1}][%p][%X{userId}] %d{yyyy-MM-dd HH:mm:ss,SSS} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="IN_MEMORY" class="sk.iway.iwcm.system.logging.InMemoryLoggerAppender" />

    <root level="ERROR">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="IN_MEMORY" />
    </root>

    <logger level="INFO" name="sk.iway"/>

    <!--
    !!! Nastavovat priamo tu len ak je to nevyhnutne potrebne !!!
    Odporucame nastavit logovacie levely v localconf.jsp cez konstantu:
    Constants.setString("logLevels", "sk.iway=DEBUG,org.springframework=DEBUG");

    # Spring logging
    <logger level="DEBUG" name="org.springframework"/>

    # SQL logging
    <logger level="TRACE" name="org.eclipse.persistence"/>
    <logger level="DEBUG" name="org.hibernate"/>
    <logger level="TRACE" name="org.hibernate.type.descriptor.sql.BasicBinder"/>

    # Hibernate logging options (INFO only shows startup messages)
    <logger level="INFO" name="org.hibernate"/>

    # Log JDBC bind parameter runtime arguments
    <logger level="TRACE" name="org.hibernate.type"/>
    -->
</configuration>
```

## Úprava generovaného archivu

Pokud potřebujete pro nasazení upravit ```WAR``` archiv, můžete použít následující tipy:

**Jiný web.xml soubor**

Pokud potřebujete pro deployment nasadit jiný ```web.xml``` soubor než používáte během vývoje můžete využít možnosti, které nabízí [gradle war](https://docs.gradle.org/current/userguide/war_plugin.html) úloha v ```build.gradle```:

```
war {
    ....
    webXml = file('src/someWeb.xml') // copies a file to WEB-INF/web.xml
}
```

**Upravené logování**

WebJET zapisuje log soubory pomocí [slf4j/logback](https://logback.qos.ch/manual/configuration.html). Ten umožňuje vyhledat i dodatečný konfigurační soubor. ```logback-test.xml```, který se použije primárně (pokud existuje). V něm můžete mít nastaveno logování pro vývoj. V souboru ```logback.xml``` budete mít nastavení pro nasazení. Při vytváření ```WAR``` archivu dodatečný ```logback-test.xml``` v ```build.gradle``` vynecháte:

```
war {
    ....
    rootSpec.exclude('**/logback-*.xml')
}
```

## Doplňkové možnosti nastavení projektu

### Nastavení přihlašování přes sociální sítě

Pokud v projektu používáte přihlašování přes sociální sítě (např. Facebook) je třeba do gradle projektu přidat knihovnu ```socialauth```. Ta standardně není součástí distribuce, protože se používá zřídka a zároveň obsahuje potencionální zranitelnost. Knihovnu přidáte v souboru ```build.gradle```:

```gradle
// https://mvnrepository.com/artifact/org.brickred/socialauth
implementation group: 'org.brickred', name: 'socialauth', version: '4.15'
```

### Samostatný web.xml soubor pro deployment

Pokud potřebujete pro deployment verze na prostředí jiný web.xml soubor než pro standardní vývoj můžete využít možnosti nastavení [úkoly war](https://docs.gradle.org/current/userguide/war_plugin.html) v ```build.gradle``` kde je možné přiložit rozdílný ```web.xml``` soubor:

```
war {
    zip64 = true
    webXml = file('src/web-azure.xml')
}
```

## Změny v databázovém schématu

Při zapnutí verze 2021 jsou přidány nové sloupce do více tabulek:

- ```_properties_``` - ​​přidán sloupec ```update_date```, sloupec ```id``` nastaven jako ```autoincrement```
- ```crontab``` - ​​přidán sloupec task_name
- ```_conf_prepared_``` - ​​nastavený sloupec ```date_prepared``` pro možnost vkládat ```NULL``` hodnotu.

### MariaDB - utf8mb4 a InnoDB podpora

WebJET používá ve výchozím nastavení ```storage engine InnoDB```, to je nastaveno v konfigurační proměnné ```mariaDbDefaultEngine```, která ve WebJET 8 má hodnotu ```MyISAM``` av novém WebJETu hodnotu ```InnoDB```. Při použití ```InnoDB``` lze využít charset ```utf8mb4``` s celou podporou ```uft8``` kódování (emotikony). Ve výchozím nastavení u ```MyISAM``` tabulek je používáno kódování ```utf8```, které je ale v MySQL/MariaDB pouze 3 bytové a nepodporuje tedy všechny znaky (různé emotikony).

Pro vytvoření nového databázového schématu a uživatele můžete použít následující SQL příkazy:

```sql
CREATE DATABASE xxx_web DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;
CREATE USER xxx_web IDENTIFIED BY 'gJ0gzNJSMwWIv4Fg';
GRANT ALL PRIVILEGES ON xxx_web.* TO `xxx_web`@`%`;
FLUSH PRIVILEGES;
```

## Rollback změn

Změny v databázovém schématu jsou zpětně kompatibilní s verzí 8.8, standardně není nutné změny ve schématu vracet zpět. Pokud ale potřebujete změny vrátit použijte následující SQL příkazy:

MySQL / Microsoft SQL:

```sql
ALTER TABLE _properties_ DROP COLUMN update_date;
ALTER TABLE _properties_ DROP COLUMN id;
DELETE FROM _db_ WHERE note='13.5.2020 [pgajdos] Pridanie stlpca update_date do _properties_ tabulky';

ALTER TABLE crontab DROP COLUMN task_name;
DELETE FROM _db_ WHERE note='08.07.2020 [pgajdos] Pridanie stlpca task_name do tabulky crontab, umiestnenie stlpca na druhe miesto';
DELETE FROM _db_ WHERE note='09.07.2020 [pgajdos] Zapisanie popisov cronjobov do stlpca task_name, pre tabulku crontab';


ALTER TABLE documents DROP COLUMN temp_field_a_docid;
ALTER TABLE documents DROP COLUMN temp_field_b_docid;
ALTER TABLE documents DROP COLUMN temp_field_c_docid;
ALTER TABLE documents DROP COLUMN temp_field_d_docid;
DELETE FROM _db_ WHERE note='14.12.2021 [sivan] Pridanie stlpcov temp_field_a_docid ... temp_field_d_docid do tabuľky documents';

ALTER TABLE documents_history DROP COLUMN temp_field_a_docid;
ALTER TABLE documents_history DROP COLUMN temp_field_b_docid;
ALTER TABLE documents_history DROP COLUMN temp_field_c_docid;
ALTER TABLE documents_history DROP COLUMN temp_field_d_docid;
DELETE FROM _db_ WHERE note='14.12.2021 [sivan] Pridanie stlpcov temp_field_a_docid ... temp_field_d_docid do tabuľky documents_history';

ALTER TABLE documents DROP COLUMN show_in_navbar;
ALTER TABLE documents DROP COLUMN show_in_sitemap;
ALTER TABLE documents DROP COLUMN logged_show_in_menu;
ALTER TABLE documents DROP COLUMN logged_show_in_navbar;
ALTER TABLE documents DROP COLUMN logged_show_in_sitemap;
DELETE FROM _db_ WHERE note='14.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_menu, logged_show_in_navbar, logged_show_in_sitemap) do tabulky documents';

ALTER TABLE documents_history DROP COLUMN show_in_navbar;
ALTER TABLE documents_history DROP COLUMN show_in_sitemap;
ALTER TABLE documents_history DROP COLUMN logged_show_in_menu;
ALTER TABLE documents_history DROP COLUMN logged_show_in_navbar;
ALTER TABLE documents_history DROP COLUMN logged_show_in_sitemap;
DELETE FROM _db_ WHERE note='18.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_menu, logged_show_in_navbar, logged_show_in_sitemap) do tabulky documents_history';

ALTER TABLE groups DROP COLUMN show_in_navbar;
ALTER TABLE groups DROP COLUMN show_in_sitemap;
ALTER TABLE groups DROP COLUMN logged_show_in_navbar;
ALTER TABLE groups DROP COLUMN logged_show_in_sitemap;
DELETE FROM _db_ WHERE note='28.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_navbar, logged_show_in_sitemap) do tabulky groups.';

ALTER TABLE groups_scheduler DROP COLUMN show_in_navbar;
ALTER TABLE groups_scheduler DROP COLUMN show_in_sitemap;
ALTER TABLE groups_scheduler DROP COLUMN logged_show_in_navbar;
ALTER TABLE groups_scheduler DROP COLUMN logged_show_in_sitemap;
DELETE FROM _db_ WHERE note='28.12.2021 [sivan] Pridanie boolean stlpcov (show_in_navbar, show_in_sitemap, logged_show_in_navbar, logged_show_in_sitemap) do tabulky groups_scheduler.';
```

Oracle:

```sql
DROP TRIGGER T_webjet_properties;
DROP SEQUENCE S_webjet_properties;
ALTER TABLE webjet_properties DROP COLUMN update_date;
ALTER TABLE webjet_properties DROP COLUMN id;
DELETE FROM webjet_db WHERE note='13.5.2020 [pgajdos] Pridanie stlpca update_date do _properties_ tabulky';

ALTER TABLE crontab DROP COLUMN task_name;
DELETE FROM webjet_db WHERE note='08.07.2020 [pgajdos] Pridanie stlpca task_name do tabulky crontab, umiestnenie stlpca na druhe miesto';
DELETE FROM webjet_db WHERE note='09.07.2020 [pgajdos] Zapisanie popisov cronjobov do stlpca task_name, pre tabulku crontab';
```