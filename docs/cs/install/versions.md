# Předpoklady a verze

WebJET 2024 vyžaduje `Java 17` a `Tomcat 9`.

Základní projekt ve formátu gradle naleznete na [githube webjetcms/basecms](https://github.com/webjetcms/basecms).

V gradle projektech stačí zadat verzi v build.gradle:

```gradle
ext {
    webjetVersion = "2024.18";
}
```

Přičemž aktuálně existují následující verze WebJET:
- `2025.0-SNAPSHOT` - pravidelně aktualizovaná verze z main repozitáře verze 2025, dostupná jako [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502)
- `2025.0` - stabilizovaná verze 2025.0, nepřibývají do ní denní změny
- `2024.52` - stabilizovaná verze 2024.52, nepřibývají do ní denní změny
- `2024.0.52` - stabilizovaná verze 2024.0.52 s opravami chyb vůči verzi 2024.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2024.0.47` - stabilizovaná verze 2024.0.47 s opravami chyb vůči verzi 2024.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2024.40` - stabilizovaná verze 2024.40, nepřibývají do ní denní změny
- `2024.0-SNAPSHOT` - pravidelně aktualizovaná verze z master repozitáře verze 2024.0 zkompilovaná s Java verze 17.
- `2024.18` - stabilizovaná verze 2024.18, nepřibývají do ní denní změny
- `2024.0.34` - stabilizovaná verze 2024.0.34 s opravami chyb vůči verzi 2024.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2024.0.21` - stabilizovaná verze 2024.0.21 s opravami chyb vůči verzi 2024.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2024.0.17` - stabilizovaná verze 2024.0.17 s opravami chyb vůči verzi 2024.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2024.0.9` - stabilizovaná verze 2024.0.9 s opravami chyb vůči verzi 2024.0 (bez přidání vylepšení ze SNAPSHOT verze).
- `2024.0` - stabilizovaná verze 2024.0 (technicky shodná s 2023.52-java17), nepřibývají do ní denní změny, zkompilovaná s Java verze 17.
- `2023.52-java17` - stabilizovaná verze 2023.52, nepřibývají do ní denní změny, zkompilovaná s Java verze 17.
- `2023.52` - stabilizovaná verze 2023.52, nepřibývají do ní denní změny
- `2023.40-SNAPSHOT-java17` - pravidelně aktualizovaná verze z master repozitáře verze 2023.40 zkompilovaná s Java verze 17.
- `2023.40-SNAPSHOT` - pravidelně aktualizovaná verze z master repozitáře verze 2023.40
- `2023.40` - stabilizovaná verze 2023.40, nepřibývají do ní denní změny
- `2023.18-SNAPSHOT-java17` - pravidelně aktualizovaná verze z master repozitáře verze 2023.18 zkompilovaná s Java verze 17.
- `2023.18-SNAPSHOT` - pravidelně aktualizovaná verze z master repozitáře verze 2023.18
- `2023.18` - stabilizovaná verze 2023.18, nepřibývají do ní denní změny
- `2023.0-SNAPSHOT` - pravidelně aktualizovaná verze z master repozitáře verze 2023.0, z důvodu API změn tato verze končí před vydáním verze 2023.18 aby nedošlo k neočekávané změně API v projektech.
- `2023.0` - stabilizovaná verze 2023.0, nepřibývají do ní denní změny
- `2022.0-SNAPSHOT` - pravidelně aktualizovaná verze z master repozitáře
- `2022.52` - stabilizovaná verze 2022.52, nepřibývají do ní denní změny
- `2022.40` - stabilizovaná verze 2022.40, nepřibývají do ní denní změny
- `2022.18` - stabilizovaná verze 2022.18, nepřibývají do ní denní změny
- `2022.0` - stabilizovaná verze 2022.0, nepřibývají do ní denní změny
- `2021.0-SNAPSHOT` - pravidelně aktualizovaná verze z master repozitáře
- `2021.52` - stabilizovaná verze 2021.52, nepřibývají do ní denní změny
- `2021.40` - stabilizovaná verze 2021.40, nepřibývají do ní denní změny
- `2021.13` - stabilizovaná verze 2021.13, nepřibývají do ní denní změny

Pro čísla verzí platí:
- `YEAR.0.x` - opravná verze, **nepřibývají do ní nové vlastnosti**, během roku jsou v ní opravovány nalezené chyby ve WebJET CMS. Použité knihovny jsou v případě potřeby aktualizovány pouze v rámci `minor` verze. Pokud oprava knihovny vyžaduje změnu v `major` verzi nemůže být do této verze zapracována, protože to může nést riziko změn v `API`.
- `YEAR.0-SNAPSHOT` - vývojová verze která **obsahuje nové vlastnosti** a opravy chyb z verze `YEAR.0.x`.
- `YEAR.WEEK` - **stabilizovaná verze** z daného týdne která vznikne ze `SNAPSHOT` verze po úspěšném vícenásobném testování. Opravy dalších chyb budou zapracovány do další verze, nevznikne opravná `YEAR.WEEK.x` ale nová `YEAR.WEEK` s novým číslem. V případě chyby v takové verzi je tedy třeba počítat s přechodem na další stabilní verzi `YEAR.WEEK` nebo na `YEAR.0-SNAPSHOT` verzi.

Verze `YEAR.0.x` se tedy zásadně nemění, obsahuje opravy chyb (pokud oprava nevyžaduje zásadní změnu). Je vhodná pro použití u zákazníka, který chce mít stabilní verzi WebJETu bez přidávání nových vlastností během roku.

Zároveň ale nemusí být verze `YEAR.0.x` nejbezpečnější. Pokud je třeba aktualizovat použitou knihovnu ve WebJETu a ta obsahuje zásadnější změny nemůžeme tuto změnu provést v `YEAR.0.x` verzi, protože by se porušila kompatibilita.

Platí tedy, že `YEAR.0.x` je **nejstabilnější** z pohledu změn a `YEAR.0-SNAPSHOT` je **nejbezpečnější** z pohledu zranitelností.

## Změny při přechodu na 2025.0-SNAPSHOT

Verze `2025.0-SNAPSHOT` je dostupná přes [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/), je proto třeba doplnit konfiguraci do vašeho `build.gradle` souboru:

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

Bohužel GitHub Packages nejsou veřejně dostupné, je proto třeba nastavit přihlašovací údaje `gpr.user` a `gpr.api-key` v souboru `gradle.properties` nebo přes `ENV` proměnné. Přihlašovací údaje vám poskytneme na vyžádání.

!> **Upozornění:** upravená inicializace Spring a JPA:
- JPA entity se v `package sk.iway.INSTALL-NAME` neinicializují automaticky, předpokládá se postupný přechod na Spring DATA. Pokud potřebujete `@Entity` inicializovat nastavte konfigurační proměnnou `jpaAddPackages` na potřebnou hodnotu - například `sk.iway.INSTALL-NAME`. Inicializují se pouze třídy obsahující anotaci `@Entity` nebo `@Converter`.
- Ve `web.xml` již není nutná inicializace `Apache Struts`, smažte celou `<servlet>` sekci obsahující `<servlet-class>org.apache.struts.action.ActionServlet</servlet-class>` a `<servlet-mapping>` obsahující `<servlet-name>action</servlet-name>`.
- Upravené pořadí inicializace Spring - inicializace WebJET tříd se provede před zákaznickými třídami `SpringConfig`.
- Upravená inicializace `Swagger` - není-li nastavena konfigurační proměnná `swaggerEnabled` na hodnotu `true` ani se při startu neprovede prohledání Java tříd.

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

Při přechodu na Java verze 17 je třeba ve vašem projektu provést několik změn. Projekt [basecms](https://github.com/webjetcms/basecms/tree/release/webjet-2023-18-java17) má připravenou `branch`, `release/webjet-2023-18-java17` s ukázkovou úpravou. V [tomto commit](https://github.com/webjetcms/basecms/commit/e4b9cf6f0a88fd6f0b0cc6c57b28e7a3ec924535) je vidět kompletní seznam změn.

Zjednodušený postup je následující:

Aktualizace `gradle-wrapper` na verzi 8 (z původní 6), doporučujeme nejprve provést aktualizaci na verzi 7 a poté na 8 (přímo na verzi může 8 aktualizace skončit chybou):

```sh
gradlew.bat wrapper --gradle-version 7.4.2
gradlew.bat wrapper --gradle-version 8.1.1
```

Po aktualizaci gradle se vám projekt nepodaří zkompilovat, je třeba upravit i soubor `build.gradle`, ve kterém je aktualizováno i více `plugins`. Důležitá změna je přechod WebJET CMS ve verzi `java17` na standardní verzi `eclipselink` s nastavením WebJET generátoru primárních klíčů.

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

Následně doporučujeme restartovat vaše vývojářské prostředí, v případě VS Code provést akci `Java: Clean Java Language Server Workspace` pro kompletní smazání dočasných souborů.

Pokud na jednom Tomcat serveru provozujete více instalací WebJETu je možné, že starší verze nebudou plně kompatibilní s Java 17. Pro chyby typu:

```
[ERROR] ContextLoader - Context initialization failed <java.lang.IllegalStateException: Cannot load configuration class: sk.iway.iwcm.system.spring.SpringSecurityConf>java.lang.Ill
egalStateException: Cannot load configuration class: sk.iway.iwcm.system.spring.SpringSecurityConf
...
Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make protected final java.lang.Class java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain) throws java.lang.ClassFormatError accessible: module java.base does not "opens java.lang" to unnamed module @4c6e3350
```

nastavte pro Tomcat následující `JAVA_OPTS`:

```
JAVA_OPTS="$JAVA_OPTS --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED"
```

### Aktualizace z Java verze 8

Pokud jste provozovali Tomcat ještě s Java verze 8 mohou vzniknout problémy s chybějícími knihovnami (ty jsou potřebné i pro Java 11). Pokud se vám v logu objeví chyba `java.lang.NoClassDefFoundError: javax/activation/DataSource`:

```
java.util.concurrent.ExecutionException: org.apache.catalina.LifecycleException: Failed to start component [StandardEngine[Catalina].StandardHost[...].StandardContext[]]
    ...
    Caused by: java.lang.NoClassDefFoundError: javax/activation/DataSource
```

je třeba do každé instalace WebJET CMS do složky `WEB-INF/lib` zkopírovat knihovny z [tohoto ZIP archivu](lib-java11.zip) a smazat soubory (pokud existují):

```
jaxb-api-2.1.jar
jaxb-runtime-3.0.0-M2.jar
```

Pokud jste používali WebJET verze `8.0-8.6` - starší než `08/2019`, nebo se vám zobrazí při startu následující chyba:

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

je třeba aktualizovat knihovnu `eclipselink` a inicializační Java třídu, stáhněte si aktualizační archiv [jpa-wj82.zip](jpa-wj82.zip) a rozbalte jej v kořenové složce web aplikace. Přepište soubor `eclipselink.jar` a `WebJETJavaSECMPInitializer.class`.

## Změny při aktualizaci na 2023.18

Verze `2023.18` mění API a způsob generování distribučních archivů. Hlavní změny API jsou v použití generických objektů typu `List/Map` namísto specifických implementací `ArrayList/Hashtable`. Z toho důvodu je třeba nově zkompilovat vaše třídy a upravit JSP soubory.

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/sfu5b_S7Q8Q" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

V `build.gradle` je třeba smazat část:

```
    implementation("sk.iway:webjet:${webjetVersion}:struts")
    implementation("sk.iway:webjet:${webjetVersion}:daisydiff")
    implementation("sk.iway:webjet:${webjetVersion}:jtidy")
```

Zkontrolujte také soubor `src/main/resources/logback.xml` ve kterém je třeba upravit formát data a času v `ConsoleAppender` (smazáno `,SSS`, pokud potřebujete logovat i setiny sekundy použijte `.SSS`):

```xml
    <pattern>[%X{installName}][%c{1}][%p][%X{userId}] %d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
```

Pro zjednodušení aktualizace můžete použít skript `/admin/update/update-2023-18.jsp` pro kontrolu a opravu JSP souborů. Zákaznické Java třídy je třeba nově zkompilovat a opravit chyby z důvodu změny API.

Přečištěno/smazáno je více Java třídy a balíky a příslušné JSP souboru. Pro podporu smazaných částí v projektech je třeba použít buď příslušný produkt typu WebJET NET nebo do projektu je přenést ze zdrojového kódu verze 8.

Více informací je v [seznamu změn](../CHANGELOG-2023.md#odstranění-závislosti-na-verzi-8).

## Změny oproti verzi 8.8

V `build.gradle` je oproti verzi 8.8 třeba smazat výrazy:

```gradle
    compile("sk.iway:webjet:${webjetVersion}:swagger-ui")

    compile 'taglibs:standard:1.1.2'
    compile 'javax.servlet:jstl:1.2'

    providedCompile 'org.slf4j:slf4j-log4j12:1.7.25'

    exclude group: 'org.slf4j', module: 'log4j-over-slf4j'
```

a v sekci `configurations` upravit výjimky následovně:

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

Z důvodu přechodu z `log4j` na `logback` smažte soubor `src/main/resources/log4j.properties` a přidejte soubor `src/main/resources/logback.xml`:

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

Pokud potřebujete pro nasazení upravit `WAR` archiv, můžete použít následující tipy:

**Jiný web.xml soubor**

Pokud potřebujete pro deployment nasadit jiný `web.xml` soubor jak používáte během vývoje můžete využít možností, které nabízí [gradle war](https://docs.gradle.org/current/userguide/war_plugin.html) role v `build.gradle`:

```
war {
    ....
    webXml = file('src/someWeb.xml') // copies a file to WEB-INF/web.xml
}
```

**Upravené logování**

WebJET zapisuje log soubory pomocí [slf4j/logback](https://logback.qos.ch/manual/configuration.html). Ten umožňuje vyhledat i dodatečný konfigurační soubor. `logback-test.xml`, který se použije primárně (pokud existuje). V něm můžete mít nastaveno logování pro vývoj. V souboru `logback.xml` budete mít nastavení pro nasazení. Při vytváření `WAR` archivu dodatečný `logback-test.xml` v `build.gradle` vynecháte:

```
war {
    ....
    rootSpec.exclude('**/logback-*.xml')
}
```

## Doplňkové možnosti nastavení projektu

### Nastavení přihlašování přes sociální sítě

Pokud v projektu používáte přihlašování přes sociální sítě (např. Facebook) je třeba do gradle projektu přidat knihovnu `socialauth`. Ta standardně není součástí distribuce, protože se používá zřídka a zároveň obsahuje potencionální zranitelnost. Knihovnu přidáte v souboru `build.gradle`:

```gradle
// https://mvnrepository.com/artifact/org.brickred/socialauth
implementation group: 'org.brickred', name: 'socialauth', version: '4.15'
```

### Samostatný web.xml soubor pro deployment

Pokud potřebujete pro deployment verze na prostředí jiný web.xml soubor než pro standardní vývoj můžete využít možnosti nastavení [úkoly war](https://docs.gradle.org/current/userguide/war_plugin.html) v `build.gradle` kde lze přiložit rozdílný `web.xml` soubor:

```
war {
    zip64 = true
    webXml = file('src/web-azure.xml')
}
```

## Změny v databázovém schématu

Při zapnutí verze 2021 jsou přidány nové sloupce do více tabulek:
- `_properties_` - přidaný sloupec `update_date`, sloupec `id` nastaven jako `autoincrement`
- `crontab` - přidán sloupec task\_name
- `_conf_prepared_` - nastavený sloupec `date_prepared` pro možnost vkládat `NULL` hodnotu.

### MariaDB - utf8mb4 a InnoDB podpora

WebJET používá ve výchozím nastavení `storage engine InnoDB`, to je nastaveno v konfigurační proměnné `mariaDbDefaultEngine`, která ve WebJET 8 má hodnotu `MyISAM` a v novém WebJETu hodnotu `InnoDB`. Při použití `InnoDB` lze využít charset `utf8mb4` s celou podporou `uft8` kódování (emotikony). Ve výchozím nastavení `MyISAM` tabulkách je používáno kódování `utf8`, které je ale v MySQL/MariaDB jen 3 bytové a nepodporuje tedy všechny znaky (různé emotikony).

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
