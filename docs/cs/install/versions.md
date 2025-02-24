# Předpoklady a verze

WebJET 2024 vyžaduje `Java 17` a `Tomcat 9`.

Základní projekt ve formátu gradle naleznete na adrese [githube webjetcms/basecms](https://github.com/webjetcms/basecms).

V projektech gradle stačí zadat verzi v souboru build.gradle:

```gradle
ext {
    webjetVersion = "2024.18";
}
```

V současné době jsou k dispozici následující verze WebJET:
- `2025.0` - Stabilizovaná verze 2025.0, nebyly přidány žádné denní změny
- `2024.52` - stabilizovaná verze 2024.52, nebyly přidány žádné denní změny
- `2024.0.52` - Stabilizovaná verze 2024.0.52 s opravami chyb proti verzi 2024.0 (bez přidání vylepšení z verze SNAPSHOT).
- `2024.0.47` - Stabilizovaná verze 2024.0.47 s opravami chyb proti verzi 2024.0 (bez přidání vylepšení z verze SNAPSHOT).
- `2024.40` - stabilizovaná verze 2024.40, nebyly přidány žádné denní změny
- `2024.0-SNAPSHOT` - pravidelně aktualizovaná verze z hlavního úložiště verze 2024.0 zkompilovaná s Javou verze 17.
- `2024.18` - Stabilizovaná verze 2024.18, nebyly přidány žádné denní změny
- `2024.0.34` - Stabilizovaná verze 2024.0.34 s opravami chyb proti verzi 2024.0 (bez přidání vylepšení z verze SNAPSHOT).
- `2024.0.21` - Stabilizovaná verze 2024.0.21 s opravami chyb proti verzi 2024.0 (bez přidání vylepšení z verze SNAPSHOT).
- `2024.0.17` - Stabilizovaná verze 2024.0.17 s opravami chyb proti verzi 2024.0 (bez přidání vylepšení z verze SNAPSHOT).
- `2024.0.9` - Stabilizovaná verze 2024.0.9 s opravami chyb proti verzi 2024.0 (bez přidání vylepšení z verze SNAPSHOT).
- `2024.0` - Stabilizovaná verze 2024.0 (technicky totožná s 2023.52-java17), nebyly přidány žádné denní změny, zkompilováno s Javou verze 17.
- `2023.52-java17` - Stabilizovaná verze 2023.52, nebyly přidány žádné denní změny, zkompilováno s Javou verze 17.
- `2023.52` - Stabilizovaná verze 2023.52, nebyly přidány žádné denní změny
- `2023.40-SNAPSHOT-java17` - pravidelně aktualizovaná verze z hlavního úložiště verze 2023.40 zkompilovaná s Javou verze 17.
- `2023.40-SNAPSHOT` - pravidelně aktualizovaná verze z hlavního úložiště verze 2023.40
- `2023.40` - stabilizovaná verze 2023.40, nebyly přidány žádné denní změny
- `2023.18-SNAPSHOT-java17` - pravidelně aktualizovaná verze z hlavního úložiště verze 2023.18 zkompilovaná s Javou verze 17.
- `2023.18-SNAPSHOT` - pravidelně aktualizovaná verze z hlavního úložiště verze 2023.18
- `2023.18` - Stabilizovaná verze 2023.18, nebyly přidány žádné denní změny
- `2023.0-SNAPSHOT` - pravidelně aktualizovaná verze z hlavního úložiště verze 2023.0, kvůli změnám API tato verze končí před vydáním verze 2023.18, aby se předešlo neočekávaným změnám API v projektech.
- `2023.0` - Stabilizovaná verze 2023.0, nebyly přidány žádné denní změny
- `2022.0-SNAPSHOT` - pravidelně aktualizovaná verze z hlavního úložiště
- `2022.52` - stabilizovaná verze 2022.52, nebyly přidány žádné denní změny
- `2022.40` - stabilizovaná verze 2022.40, nebyly přidány žádné denní změny
- `2022.18` - stabilizovaná verze 2022.18, nebyly přidány žádné denní změny
- `2022.0` - stabilizovaná verze 2022.0, nebyly přidány žádné denní změny
- `2021.0-SNAPSHOT` - pravidelně aktualizovaná verze z hlavního úložiště
- `2021.52` - stabilizovaná verze 2021.52, nebyly přidány žádné denní změny
- `2021.40` - stabilizovaná verze 2021.40, nebyly přidány žádné denní změny
- `2021.13` - Stabilizovaná verze 2021.13, nebyly přidány žádné denní změny

Čísla verzí:
- `YEAR.0.x` - Opravená verze, **nejsou do něj přidány žádné nové vlastnosti**, v průběhu roku opravuje chyby nalezené v systému WebJET CMS. Použité knihovny jsou aktualizovány pouze v rámci `minor` Verze. Pokud oprava knihovny vyžaduje změnu `major` verze nemůže být začleněna do této verze, protože by mohla nést riziko změn v oblasti `API`.
- `YEAR.0-SNAPSHOT` - vývojová verze, která **obsahuje nové funkce** a opravy chyb z verze `YEAR.0.x`.
- `YEAR.WEEK` - **stabilizovaná verze** z tohoto týdne, který je vytvořen z `SNAPSHOT` verze po úspěšném vícenásobném testování. Opravy ostatních chyb budou začleněny do příští verze, nebude vytvořen žádný patch. `YEAR.WEEK.x` ale nový `YEAR.WEEK` s novým číslem. V případě chyby v takové verzi je proto nutné počítat s přechodem na další stabilní verzi. `YEAR.WEEK` nebo na `YEAR.0-SNAPSHOT` verze.

Zobrazit celou verzi `YEAR.0.x` není tedy zásadně změněn, obsahuje opravy chyb (pokud oprava nevyžaduje zásadní změnu). Je vhodná pro použití zákazníkem, který chce mít stabilní verzi WebJETu bez přidávání nových funkcí v průběhu roku.

Současně však tato verze nemusí být. `YEAR.0.x` nejbezpečnější. Pokud je třeba aktualizovat knihovnu použitou v systému WebJET a ta obsahuje zásadní změny, nemůžeme tuto změnu provést v rámci `YEAR.0.x` verze, protože by to narušilo kompatibilitu.

Je tedy pravda, že `YEAR.0.x` Je **nejstabilnější** z hlediska změn a `YEAR.0-SNAPSHOT` Je **nejbezpečnější** z hlediska zranitelnosti.

## Změny při přechodu na rok 2024.0-SNAPSHOT

Podobně pro [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms) je třeba přidat do `dependencies` bloková část `implementation("sk.iway:webjet:${webjetVersion}:libs")`:

```gradle
dependencies {
    implementation("sk.iway:webjet:${webjetVersion}")
    implementation("sk.iway:webjet:${webjetVersion}:admin")
    implementation("sk.iway:webjet:${webjetVersion}:components")
    implementation("sk.iway:webjet:${webjetVersion}:libs")
}
```

Následující knihovny, které se nepoužívají ve standardní instalaci, byly odstraněny, pokud je váš projekt potřebuje, přidejte je do svého projektu. `build.gradle` soubor:

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

V části `configurations` odstraňte výrazy:

```gradle
all*.exclude group: 'xml-apis', module: 'xml-apis'
all*.exclude group: 'javax.xml.stream', module: 'stax-api'
```

## Změny při přechodu na verzi GitHub/Maven Central

- V [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms) název balíčků se změní z `sk.iway` na adrese `com.webjetcms` a doplňuje se oddíl `libs` který kombinuje všechny původní `sk.iway` závislosti typu `struts,daisydiff,jtidy`. V `build.gradle` upravit:

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
- V projektu odstraňte soubor `src/main/webapp/WEB-INF/struts-config.xml` použít aktuální soubor WebJET (ze souboru jar).

## Změny při přechodu na Javu 17

Při přechodu na Javu verze 17 je třeba v projektu provést několik změn. Projekt [basecms](https://github.com/webjetcms/basecms/tree/release/webjet-2023-18-java17) připravila `branch`, `release/webjet-2023-18-java17` s povrchovou úpravou vzorku. V [tato revize](https://github.com/webjetcms/basecms/commit/e4b9cf6f0a88fd6f0b0cc6c57b28e7a3ec924535) je vidět kompletní seznam změn.

Zjednodušený postup je následující:

Aktualizace `gradle-wrapper` na verzi 8 (z původní verze 6), doporučujeme nejprve provést upgrade na verzi 7 a poté na verzi 8 (přímý upgrade na verzi 8 může vést k chybě):

```sh
gradlew.bat wrapper --gradle-version 7.4.2
gradlew.bat wrapper --gradle-version 8.1.1
```

Po aktualizaci gradle nebudete moci projekt zkompilovat, musíte také upravit soubor `build.gradle`, v níž se řada `plugins`. Důležitou změnou je přechod systému WebJET CMS ve verzi `java17` na standardní verzi `eclipselink` s nastavením generátoru primárních klíčů WebJET.

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

Poté doporučujeme restartovat vývojové prostředí, v případě VS Code proveďte akci `Java: Clean Java Language Server Workspace` úplně odstranit dočasné soubory.

Pokud na jednom serveru Tomcat používáte více instalací WebJET, je možné, že starší verze nebudou plně kompatibilní s Javou 17:

```
[ERROR] ContextLoader - Context initialization failed <java.lang.IllegalStateException: Cannot load configuration class: sk.iway.iwcm.system.spring.SpringSecurityConf>java.lang.Ill
egalStateException: Cannot load configuration class: sk.iway.iwcm.system.spring.SpringSecurityConf
...
Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make protected final java.lang.Class java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain) throws java.lang.ClassFormatError accessible: module java.base does not "opens java.lang" to unnamed module @4c6e3350
```

nastavit následující pro Tomcat `JAVA_OPTS`:

```
JAVA_OPTS="$JAVA_OPTS --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED"
```

### Aktualizace z verze Java 8

Pokud jste dosud používali Tomcat s verzí Javy 8, můžete mít problémy s chybějícími knihovnami (ty jsou potřebné i pro Javu 11). Pokud se v protokolu objeví chyba `java.lang.NoClassDefFoundError: javax/activation/DataSource`:

```
java.util.concurrent.ExecutionException: org.apache.catalina.LifecycleException: Failed to start component [StandardEngine[Catalina].StandardHost[...].StandardContext[]]
    ...
    Caused by: java.lang.NoClassDefFoundError: javax/activation/DataSource
```

je nutné přidat ke každé instalaci WebJET CMS do složky `WEB-INF/lib` zkopírované knihovny z [tohoto archivu ZIP](lib-java11.zip) a odstranit soubory (pokud existují):

```
jaxb-api-2.1.jar
jaxb-runtime-3.0.0-M2.jar
```

Pokud používáte verzi WebJET. `8.0-8.6` - starší než `08/2019`, nebo se při spuštění zobrazí následující chyba:

```
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

je třeba aktualizovat knihovnu `eclipselink` a inicializační třídu jazyka Java, stáhněte si archiv aktualizace [jpa-wj82.zip](jpa-wj82.zip) a rozbalte jej do kořenové složky webové aplikace. Soubor přepište `eclipselink.jar` a `WebJETJavaSECMPInitializer.class`.

## Změny při aktualizaci na verzi 2023.18

Zobrazit celou verzi `2023.18` mění rozhraní API a způsob generování distribučních archivů. Hlavní změny v rozhraní API spočívají v použití generických objektů typu `List/Map` místo konkrétních implementací `ArrayList/Hashtable`. Proto je třeba překompilovat třídy a upravit soubory JSP.

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/sfu5b_S7Q8Q" title="Přehrávač videí YouTube" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

V `build.gradle` je nutné tuto část odstranit:

```
    implementation("sk.iway:webjet:${webjetVersion}:struts")
    implementation("sk.iway:webjet:${webjetVersion}:daisydiff")
    implementation("sk.iway:webjet:${webjetVersion}:jtidy")
```

Zkontrolujte také soubor `src/main/resources/logback.xml` ve kterém je formát data a času v `ConsoleAppender` (smazáno) `,SSS`, pokud potřebujete zaznamenat i setiny sekundy, použijte funkci `.SSS`):

```xml
    <pattern>[%X{installName}][%c{1}][%p][%X{userId}] %d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
```

Chcete-li aktualizaci zjednodušit, můžete použít skript `/admin/update/update-2023-18.jsp` pro kontrolu a opravu souborů JSP. Je třeba překompilovat třídy Java zákazníka a opravit chyby v důsledku změn API.

Je vyčištěno/odstraněno několik tříd a balíčků jazyka Java a odpovídající soubor JSP. Chcete-li odstraněné části v projektech podporovat, musíte buď použít příslušný produkt WebJET NET, nebo je do projektu přenést ze zdrojového kódu verze 8.

Více informací najdete v [seznam změn](../CHANGELOG-2023.md#odstranění-závislosti-na-verzi-8).

## Změny oproti verzi 8.8

V `build.gradle` je nutné odstranit výrazy oproti verzi 8.8:

```gradle
    compile("sk.iway:webjet:${webjetVersion}:swagger-ui")

    compile 'taglibs:standard:1.1.2'
    compile 'javax.servlet:jstl:1.2'

    providedCompile 'org.slf4j:slf4j-log4j12:1.7.25'

    exclude group: 'org.slf4j', module: 'log4j-over-slf4j'
```

a v části `configurations` upravit výjimky takto:

```gradle
configurations {
    all*.exclude group: 'org.slf4j', module: 'slf4j-log4j12'
    all*.exclude group: 'org.slf4j', module: 'jcl104-over-slf4j' //je nahradene novsim jcl-over-slf4j
    all*.exclude group: 'commons-logging', module: 'commons-logging'
    all*.exclude group: 'log4j', module: 'log4j'

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

Vzhledem k přechodu z `log4j` na adrese `logback` odstranit soubor `src/main/resources/log4j.properties` a přidejte soubor `src/main/resources/logback.xml`:

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

## Úprava vygenerovaného archivu

Pokud potřebujete upravit nasazení `WAR` archiv, můžete použít následující tipy:

**Další soubor web.xml**

Pokud potřebujete nasadit jiný `web.xml` soubor, který používáte při vývoji, můžete využít možností, které nabízí [gradle war](https://docs.gradle.org/current/userguide/war_plugin.html) roli v `build.gradle`:

```
war {
    ....
    webXml = file('src/someWeb.xml') // copies a file to WEB-INF/web.xml
}
```

**Upravené logovaní**

WebJET zapisuje soubory protokolu pomocí [slf4j/logback](https://logback.qos.ch/manual/configuration.html). To umožňuje vyhledat další konfigurační soubor, např. `logback-test.xml` které mají být primárně použity (pokud existují). V něm můžete mít nastaveno přihlašování pro vývoj. V souboru `logback.xml` budete připraveni k nasazení. Při vytváření `WAR` archivovat další `logback-test.xml` v `build.gradle` přeskočíte:

```
war {
    ....
    rootSpec.exclude('**/logback-*.xml')
}
```

## Další možnosti nastavení projektu

### Nastavení přihlášení do sociálních sítí

Pokud ve svém projektu používáte přihlašování přes sociální sítě (např. Facebook), musíte do gradle projektu přidat knihovnu. `socialauth`. Ve výchozím nastavení není součástí distribuce, protože se používá jen zřídka a obsahuje potenciální zranitelnost. Knihovnu přidáte v souboru `build.gradle`:

```gradle
// https://mvnrepository.com/artifact/org.brickred/socialauth
implementation group: 'org.brickred', name: 'socialauth', version: '4.15'
```

### Samostatný soubor web.xml pro nasazení

Pokud potřebujete pro nasazení verze do prostředí jiný soubor web.xml než pro standardní vývoj, můžete použít možnosti nastavení. [válečné úkoly](https://docs.gradle.org/current/userguide/war_plugin.html) v `build.gradle` kde je možné připojit jiný `web.xml` Soubor:

```
war {
    zip64 = true
    webXml = file('src/web-azure.xml')
}
```

## Změny ve schématu databáze

Při zapnutí verze 2021 jsou do několika tabulek přidány nové sloupce:
- `_properties_` - přidán sloupec `update_date`, sloupec `id` nastavit jako `autoincrement`
- `crontab` - přidán sloupec task\_name
- `_conf_prepared_` - nastavit sloupec `date_prepared` pro možnost vložení `NULL` Hodnota.

### MariaDB - podpora utf8mb4 a InnoDB

Ve výchozím nastavení se používá WebJET `storage engine InnoDB`, která je nastavena v konfigurační proměnné `mariaDbDefaultEngine`, která má ve WebJET 8 hodnotu `MyISAM` a v nové hodnotě WebJET `InnoDB`. Při použití `InnoDB` je možné použít znakovou sadu `utf8mb4` s plnou podporou `uft8` kódování (emotikony). Ve výchozím nastavení, když `MyISAM` kódování je použito v tabulkách `utf8`, který má v MySQL/MariaDB pouze 3 bajty, a proto nepodporuje všechny znaky (různé emotikony).

K vytvoření nového databázového schématu a uživatele můžete použít následující příkazy SQL:

```sql
CREATE DATABASE xxx_web DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;
CREATE USER xxx_web IDENTIFIED BY 'gJ0gzNJSMwWIv4Fg';
GRANT ALL PRIVILEGES ON xxx_web.* TO `xxx_web`@`%`;
FLUSH PRIVILEGES;
```

## Zrušení změn

Změny schématu databáze jsou zpětně kompatibilní s verzí 8.8, ve výchozím nastavení není třeba změny schématu vracet. Pokud však potřebujete změny vrátit, použijte následující příkazy SQL:

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
