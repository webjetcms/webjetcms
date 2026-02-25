# Predpoklady a verzie

Aktuálna verzia WebJET CMS vyžaduje `Java 17` a `Tomcat 11`.

Základný projekt vo formáte gradle nájdete na [githube webjetcms/basecms](https://github.com/webjetcms/basecms).

V gradle projektoch stačí zadať verziu v build.gradle:

```gradle
ext {
    webjetVersion = "2026.0";
}
```

Pričom aktuálne existujú nasledovné verzie WebJET:

- `2026.0-jakarta-SNAPSHOT` - pravidelne aktualizovaná verzia z main repozitára s využitím `Jakarta namespace`. Vyžaduje Tomcat 11, dostupná ako [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502?version=2026.0-jakarta-SNAPSHOT)
- `2026.0-jakarta` - stabilizovaná verzia 2026.0 pre aplikačný server Tomcat 11 s využitím `Jakarta namespace`, nepribúdajú do nej denné zmeny.
- `2026.0` - stabilizovaná verzia 2026.0, nepribúdajú do nej denné zmeny.
- `2025.0-jakarta-SNAPSHOT` - stabilizovaná verzia 2025.52 s využitím `Jakarta namespace`. Vyžaduje Tomcat 10/11, dostupná ako [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502?version=2025.0-jakarta-SNAPSHOT)
- `2025.0-SNAPSHOT` - stabilizovaná verzia 2025.52, dostupná ako [GitHub-package](https://github.com/webjetcms/webjetcms/packages/2426502?version=2025.0-SNAPSHOT)
- `2025.0.52` - stabilizovaná verzia 2025.0.52 s opravami chýb voči verzii 2025.0 (bez pridania vylepšení zo SNAPSHOT verzie).
- `2025.0.50` - stabilizovaná verzia 2025.0.50 s opravami chýb voči verzii 2025.0 (bez pridania vylepšení zo SNAPSHOT verzie).
- `2025.40-jakarta` - stabilizovaná verzia 2025.40 pre aplikačný server Tomcat 10/11 s využitím `Jakarta namespace`, nepribúdajú do nej denné zmeny.
- `2025.40` - stabilizovaná verzia 2025.40, nepribúdajú do nej denné zmeny.
- `2025.0.40` - stabilizovaná verzia 2025.0.40 s opravami chýb voči verzii 2025.0 (bez pridania vylepšení zo SNAPSHOT verzie).
- `2025.18` - stabilizovaná verzia 2025.18, nepribúdajú do nej denné zmeny.
- `2025.0.23` - stabilizovaná verzia 2025.0.23 s opravami chýb voči verzii 2025.0 (bez pridania vylepšení zo SNAPSHOT verzie).
- `2025.0` - stabilizovaná verzia 2025.0, nepribúdajú do nej denné zmeny.
- `2024.52` - stabilizovaná verzia 2024.52, nepribúdajú do nej denné zmeny.
- `2024.0.52` - stabilizovaná verzia 2024.0.52 s opravami chýb voči verzii 2024.0 (bez pridania vylepšení zo SNAPSHOT verzie).
- `2024.0.47` - stabilizovaná verzia 2024.0.47 s opravami chýb voči verzii 2024.0 (bez pridania vylepšení zo SNAPSHOT verzie).
- `2024.40` - stabilizovaná verzia 2024.40, nepribúdajú do nej denné zmeny.
- `2024.0-SNAPSHOT` - pravidelne aktualizovaná verzia z master repozitára verzie 2024.0 skompilovaná s Java verzie 17.
- `2024.18` - stabilizovaná verzia 2024.18, nepribúdajú do nej denné zmeny.
- `2024.0.34` - stabilizovaná verzia 2024.0.34 s opravami chýb voči verzii 2024.0 (bez pridania vylepšení zo SNAPSHOT verzie).
- `2024.0.21` - stabilizovaná verzia 2024.0.21 s opravami chýb voči verzii 2024.0 (bez pridania vylepšení zo SNAPSHOT verzie).
- `2024.0.17` - stabilizovaná verzia 2024.0.17 s opravami chýb voči verzii 2024.0 (bez pridania vylepšení zo SNAPSHOT verzie).
- `2024.0.9` - stabilizovaná verzia 2024.0.9 s opravami chýb voči verzii 2024.0 (bez pridania vylepšení zo SNAPSHOT verzie).
- `2024.0` - stabilizovaná verzia 2024.0 (technicky zhodná s 2023.52-java17), nepribúdajú do nej denné zmeny, skompilovaná s Java verzie 17.
- `2023.52-java17` - stabilizovaná verzia 2023.52, nepribúdajú do nej denné zmeny, skompilovaná s Java verzie 17.
- `2023.52` - stabilizovaná verzia 2023.52, nepribúdajú do nej denné zmeny.
- `2023.40-SNAPSHOT-java17` - pravidelne aktualizovaná verzia z master repozitára verzie 2023.40 skompilovaná s Java verzie 17.
- `2023.40-SNAPSHOT` - pravidelne aktualizovaná verzia z master repozitára verzie 2023.40.
- `2023.40` - stabilizovaná verzia 2023.40, nepribúdajú do nej denné zmeny.
- `2023.18-SNAPSHOT-java17` - pravidelne aktualizovaná verzia z master repozitára verzie 2023.18 skompilovaná s Java verzie 17.
- `2023.18-SNAPSHOT` - pravidelne aktualizovaná verzia z master repozitára verzie 2023.18.
- `2023.18` - stabilizovaná verzia 2023.18, nepribúdajú do nej denné zmeny.
- `2023.0-SNAPSHOT` - pravidelne aktualizovaná verzia z master repozitára verzie 2023.0, z dôvodu API zmien táto verzia končí pred vydaním verzie 2023.18 aby nedošlo k neočakávanej zmene API v projektoch.
- `2023.0` - stabilizovaná verzia 2023.0, nepribúdajú do nej denné zmeny.
- `2022.0-SNAPSHOT` - pravidelne aktualizovaná verzia z master repozitára.
- `2022.52` - stabilizovaná verzia 2022.52, nepribúdajú do nej denné zmeny.
- `2022.40` - stabilizovaná verzia 2022.40, nepribúdajú do nej denné zmeny.
- `2022.18` - stabilizovaná verzia 2022.18, nepribúdajú do nej denné zmeny.
- `2022.0` - stabilizovaná verzia 2022.0, nepribúdajú do nej denné zmeny.
- `2021.0-SNAPSHOT` - pravidelne aktualizovaná verzia z master repozitára.
- `2021.52` - stabilizovaná verzia 2021.52, nepribúdajú do nej denné zmeny.
- `2021.40` - stabilizovaná verzia 2021.40, nepribúdajú do nej denné zmeny.
- `2021.13` - stabilizovaná verzia 2021.13, nepribúdajú do nej denné zmeny.

Pre čísla verzií platí:

- `YEAR.0.x` - opravná verzia, **nepribúdajú do nej nové vlastnosti**, počas roka sú v nej opravované nájdené chyby vo WebJET CMS. Použité knižnice sú v prípade potreby aktualizované len vrámci `minor`  verzie. Ak oprava knižnice vyžaduje zmenu v `major` verzii nemôže byť do tejto verzie zapracovaná, keďže to môže niesť riziko zmien v `API`.
- `YEAR.0-SNAPSHOT` - vývojová verzia ktorá **obsahuje nové vlastnosti** a opravy chýb z verzie `YEAR.0.x`.
- `YEAR.WEEK` - **stabilizovaná verzia** z daného týždňa ktorá vznikne zo `SNAPSHOT` verzie po úspešnom viac násobnom testovaní. Opravy ďalších chýb budú zapracované do ďalšej verzie, nevznikne opravná `YEAR.WEEK.x` ale nová `YEAR.WEEK` s novým číslom. V prípade chyby v takejto verzii je teda potrebné rátať s prechodom na ďalšiu stabilnú verziu `YEAR.WEEK` alebo na `YEAR.0-SNAPSHOT` verziu.

Verzia `YEAR.0.x` sa teda zásadne nemení, obsahuje opravy chýb (ak oprava nevyžaduje zásadnú zmenu). Je vhodná na použitie u zákazníka ktorý chce mať stabilnú verziu WebJETu bez pridávania nových vlastností počas roka.

Zároveň ale nemusí byť verzia `YEAR.0.x` najbezpečnejšia. Ak je potrebné aktualizovať použitú knižnicu vo WebJETe a tá obsahuje zásadnejšie zmeny nemôžeme túto zmenu vykonať v `YEAR.0.x` verzii, pretože by sa porušila kompatibilita.

Platí teda, že `YEAR.0.x` je **najstabilnejšia** z pohľadu zmien a `YEAR.0-SNAPSHOT` je **najbezpečnejšia** z pohľadu zraniteľností.

## Zmeny pri prechode na Tomcat 9.0.104+

V [Tomcat od verzie 9.0.104](https://tomcat.apache.org/tomcat-9.0-doc/config/http.html) je zmenená kontrola počtu parametrov pri `multipart` HTTP požiadavke. Je preto potrebné nastaviť/zvýšiť parameter `maxPartCount` na `<Connector` elemente s súbore `tomcat/conf/server.xml` na hodnotu minimálne 100, príklad:

```xml
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443"
               maxPartCount="1000"
               URIEncoding="UTF-8"
               useBodyEncodingForURI="true" relaxedQueryChars="^{}[]|&quot;"
    />
```

## Zmeny pri prechode na Jakarta verziu

Verzia určená pre `jakarta namespace`, vyžaduje aplikačný server Tomcat 11, používa Spring verzie 7. Prelomové zmeny:

- URL adresy - pre URL adresy Spring zaviedol presné zhody, ak REST služba definuje URL adresu s lomkou na konci, musí byť takto použitá. Je rozdiel v URL adrese `/admin/rest/service` a `/admin/rest/service/`.
- V Spring DATA repozitároch pre `IN/NOTIN query` je potrebné pridať `@Query`, inak nebude korektne SQL vytvorené, príklad:

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

Pre vyhľadanie v kóde môžete použiť hľadanie v súboroch `*Repository.java` a hľadať regulárny výraz `\(.*List[^)]*\)`, `\(.*Long\[\][^)]*\)`, `\(.*Integer\[\][^)]*\)`. Odporúčame vykonať kód, v logu sa zobrazí chyba a použiť vygenerované SQL do `Query` hodnoty. Problémom je len kontrola typu, kde `EclipseLink` nevie identifikovať, že má kontrolovať pole/zoznam a nie priamo dátový typ.

V `build.gradle` je potrebné aktualizovať `gretty` konfiguráciu a pridať nastavenie kompilácie `options.compilerArgs += ['-parameters']`:

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

## Zmeny pri prechode na 2025.0-SNAPSHOT

Verzia `2025.0-SNAPSHOT` je dostupná cez [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/2426502?version=2025.0-SNAPSHOT), je preto potrebné doplniť konfiguráciu do vášho `build.gradle` súboru:

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

Žiaľ GitHub Packages nie sú verejne dostupné, je preto potrebné nastaviť prihlasovacie údaje `gpr.user` a `gpr.api-key` v súbore `gradle.properties` alebo cez `ENV` premenné. Prihlasovacie údaje vám poskytneme na vyžiadanie.

!> **Upozornenie:** upravená inicializácia Spring a JPA:

- JPA entity sa v `package sk.iway.INSTALL-NAME` neinicializujú automaticky, predpokladá sa postupný prechod na Spring DATA. Ak potrebujete `@Entity` inicializovať nastavte konfiguračnú premennú `jpaAddPackages` na potrebnú hodnotu - napríklad `sk.iway.INSTALL-NAME`. Inicializujú sa len triedy obsahujúce anotáciu `@Entity` alebo `@Converter`.
- Vo `web.xml` už nie je potrebná inicializácia `Apache Struts`, zmažte celú `<servlet>` sekciu obsahujúcu `<servlet-class>org.apache.struts.action.ActionServlet</servlet-class>` a `<servlet-mapping>` obsahujúci `<servlet-name>action</servlet-name>`.
- Upravené poradie inicializácie Spring - inicializácia WebJET tried sa vykoná pred zákazníckymi triedami `SpringConfig`.
- Upravená inicializácia `Swagger` - ak nie je nastavená konfiguračná premenná `swaggerEnabled` na hodnotu `true` ani sa pri štarte nevykoná prehľadanie Java tried.

## Zmeny pri prechode na 2024.0-SNAPSHOT

Podobne ako pre [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms) verziu je potrebné pridať do `dependencies` bloku časť `implementation("sk.iway:webjet:${webjetVersion}:libs")`:

```gradle
dependencies {
    implementation("sk.iway:webjet:${webjetVersion}")
    implementation("sk.iway:webjet:${webjetVersion}:admin")
    implementation("sk.iway:webjet:${webjetVersion}:components")
    implementation("sk.iway:webjet:${webjetVersion}:libs")
}
```

Zmazané boli nasledovné knižnice, ktoré nie sú používané v štandardnej inštalácii, ak ich váš projekt potrebuje, pridajte ich do vášho `build.gradle` súboru:

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

V sekcii `configurations` zmažte výrazy:

```gradle
all*.exclude group: 'xml-apis', module: 'xml-apis'
all*.exclude group: 'javax.xml.stream', module: 'stax-api'
```

## Zmeny pri prechode na GitHub/Maven Central verziu

- V [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms) je zmenené meno balíkov z `sk.iway` na `com.webjetcms` a pridaná je časť `libs`, ktorá kombinuje všetky pôvodné `sk.iway` závislosti typu `struts,daisydiff,jtidy`. V `build.gradle` upravte:

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

## Zmeny pri prechode na 2023.18-SNAPSHOT/2023.40

- Vo vašom projekte zmažte súbor `src/main/webapp/WEB-INF/struts-config.xml` aby sa použil aktuálny súbor z WebJETu (z jar súboru).

## Zmeny pri prechode na Java 17

Pri prechode na Java verzie 17 je potrebné vo vašom projekte vykonať niekoľko zmien. Projekt [basecms](https://github.com/webjetcms/basecms/tree/release/webjet-2023-18-java17) má pripravenú `branch`, ```release/webjet-2023-18-java17``` s ukážkovou úpravou. V [tomto commit](https://github.com/webjetcms/basecms/commit/e4b9cf6f0a88fd6f0b0cc6c57b28e7a3ec924535) je vidno kompletný zoznam zmien.

Zjednodušený postup je nasledovný:

Aktualizácia `gradle-wrapper` na verziu 8 (z pôvodnej 6), odporúčame najskôr vykonať aktualizáciu na verziu 7 a potom na 8 (priamo na verziu môže 8 aktualizácia skončiť chybou):

```sh
gradlew.bat wrapper --gradle-version 7.4.2
gradlew.bat wrapper --gradle-version 8.1.1
```

Po aktualizácii gradle sa vám projekt nepodarí skompilovať, je potrebné upraviť aj súbor ```build.gradle```, v ktorom sú aktualizované aj viaceré ```plugins```. Dôležitá zmena je prechod WebJET CMS vo verzii ```java17``` na štandardnú verziu ```eclipselink``` s nastavením WebJET generátora primárnych kľúčov.

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

Následne odporúčame reštartovať vaše vývojárske prostredie, v prípade VS Code vykonať akciu ```Java: Clean Java Language Server Workspace``` pre kompletné zmazanie dočasných súborov.

Ak na jednom Tomcat serveri prevádzkujete viacero inštalácii WebJETu je možné, že staršie verzie nebudú plne kompatibilné s Java 17. Pre chyby typu:

```txt
[ERROR] ContextLoader - Context initialization failed <java.lang.IllegalStateException: Cannot load configuration class: sk.iway.iwcm.system.spring.SpringSecurityConf>java.lang.Ill
egalStateException: Cannot load configuration class: sk.iway.iwcm.system.spring.SpringSecurityConf
...
Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make protected final java.lang.Class java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain) throws java.lang.ClassFormatError accessible: module java.base does not "opens java.lang" to unnamed module @4c6e3350
```

nastavte pre Tomcat nasledovné ```JAVA_OPTS```:

```txt
JAVA_OPTS="$JAVA_OPTS --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.naming/javax.naming=ALL-UNNAMED"
```

### Aktualizácia z Java verzie 8

Ak ste prevádzkovali Tomcat ešte s Java verzie 8 môžu vzniknúť problémy s chýbajúcimi knižnicami (tie sú potrebné aj pre Java 11). Ak sa vám v logu objaví chyba ```java.lang.NoClassDefFoundError: javax/activation/DataSource```:

```txt
java.util.concurrent.ExecutionException: org.apache.catalina.LifecycleException: Failed to start component [StandardEngine[Catalina].StandardHost[...].StandardContext[]]
    ...
    Caused by: java.lang.NoClassDefFoundError: javax/activation/DataSource
```

je potrebné do každej inštalácie WebJET CMS do priečinka ```WEB-INF/lib``` skopírovať knižnice z [tohto ZIP archívu](lib-java11.zip) a zmazať súbory (ak existujú):

```txt
jaxb-api-2.1.jar
jaxb-runtime-3.0.0-M2.jar
```

Ak ste používali WebJET verzie `8.0-8.6` - starší ako `08/2019`, alebo sa vám zobrazí pri štarte nasledovná chyba:

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

je potrebné aktualizovať knižnicu `eclipselink` a inicializačnú Java triedu, stiahnite si aktualizačný archív [jpa-wj82.zip](jpa-wj82.zip) a rozbaľte ho v koreňovom priečinku web aplikácie. Prepíšte súbor `eclipselink.jar` a `WebJETJavaSECMPInitializer.class`.

## Zmeny pri aktualizácii na 2023.18

Verzia ```2023.18``` mení API a spôsob generovania distribučných archívov. Hlavné zmeny API sú v použití generických objektov typu ```List/Map``` namiesto špecifických implementácií ```ArrayList/Hashtable```. Z toho dôvodu je potrebné nanovo skompilovať vaše triedy a upraviť JSP súbory.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/sfu5b_S7Q8Q" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

V ```build.gradle``` je potrebné zmazať časť:

```
    implementation("sk.iway:webjet:${webjetVersion}:struts")
    implementation("sk.iway:webjet:${webjetVersion}:daisydiff")
    implementation("sk.iway:webjet:${webjetVersion}:jtidy")
```

Skontrolujte aj súbor `src/main/resources/logback.xml` v ktorom je potrebné upraviť formát dátumu a času v `ConsoleAppender` (zmazané `,SSS`, ak potrebujete logovať aj stotiny sekundy použite `.SSS`):

```xml
    <pattern>[%X{installName}][%c{1}][%p][%X{userId}] %d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
```

Pre zjednodušenie aktualizácie môžete použiť skript ```/admin/update/update-2023-18.jsp``` pre kontrolu a opravu JSP súborov. Zákaznícke Java triedy je potrebné nanovo skompilovať a opraviť chyby z dôvodu zmeny API.

Prečistené/zmazané sú viaceré Java triedy a balíky a príslušné JSP súboru. Pre podporu zmazaných častí v projektoch je potrebné použiť buď príslušný produkt typu WebJET NET alebo do projektu ich preniesť zo zdrojového kódu verzie 8.

Viac informácií je v [zozname zmien](../CHANGELOG-2023.md#odstránenie-závislosti-na-verzii-8).

## Zmeny oproti verzii 8.8

V ```build.gradle``` je oproti verzii 8.8 potrebné zmazať výrazy:

```gradle
    compile("sk.iway:webjet:${webjetVersion}:swagger-ui")

    compile 'taglibs:standard:1.1.2'
    compile 'javax.servlet:jstl:1.2'

    providedCompile 'org.slf4j:slf4j-log4j12:1.7.25'

    exclude group: 'org.slf4j', module: 'log4j-over-slf4j'
```

a v sekcii ```configurations``` upraviť výnimky nasledovne:

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

Z dôvodu prechodu z ```log4j``` na ```logback``` zmažte súbor ```src/main/resources/log4j.properties``` a pridajte súbor ```src/main/resources/logback.xml```:

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

## Úprava generovaného archívu

Ak potrebujete pre nasadenie upraviť ```WAR``` archív, môžete použiť nasledovné tipy:

**Iný web.xml súbor**

Ak potrebujete pre deployment nasadiť iný ```web.xml``` súbor ako používate počas vývoja môžete využiť možnosti, ktoré ponúka [gradle war](https://docs.gradle.org/current/userguide/war_plugin.html) úloha v ```build.gradle```:

```
war {
    ....
    webXml = file('src/someWeb.xml') // copies a file to WEB-INF/web.xml
}
```

**Upravené logovanie**

WebJET zapisuje log súbory pomocou [slf4j/logback](https://logback.qos.ch/manual/configuration.html). Ten umožňuje vyhľadať aj dodatočný konfiguračný súbor napr. ```logback-test.xml```, ktorý sa použije primárne (ak existuje). V ňom môžete mať nastavené logovanie pre vývoj. V súbore ```logback.xml``` budete mať nastavenie pre nasadenie. Pri vytváraní ```WAR``` archívu dodatočný ```logback-test.xml``` v ```build.gradle``` vynecháte:

```
war {
    ....
    rootSpec.exclude('**/logback-*.xml')
}
```

## Doplnkové možnosti nastavenia projektu

### Nastavenie prihlasovania cez sociálne siete

Ak v projekte používate prihlasovanie cez sociálne siete (napr. Facebook) je potrebné do gradle projektu pridať knižnicu ```socialauth```. Tá štandardne nie je súčasťou distribúcie, pretože sa používa zriedka a zároveň obsahuje potencionálnu zraniteľnosť. Knižnicu pridáte v súbore ```build.gradle```:

```gradle
// https://mvnrepository.com/artifact/org.brickred/socialauth
implementation group: 'org.brickred', name: 'socialauth', version: '4.15'
```

### Samostatný web.xml súbor pre deployment

Ak potrebujete pre deployment verzie na prostredia iný web.xml súbor ako pre štandardný vývoj môžete využiť možnosti nastavenia [úlohy war](https://docs.gradle.org/current/userguide/war_plugin.html) v ```build.gradle``` kde je možné priložiť rozdielny ```web.xml``` súbor:

```
war {
    zip64 = true
    webXml = file('src/web-azure.xml')
}
```

## Zmeny v databázovej schéme

Pri zapnutí verzie 2021 sú pridané nové stĺpce do viacerých tabuliek:

- ```_properties_``` - pridaný stĺpec ```update_date```, stĺpec ```id``` nastavený ako ```autoincrement```
- ```crontab``` - pridaný stĺpec task_name
- ```_conf_prepared_``` - nastavený stĺpec ```date_prepared``` pre možnosť vkladať ```NULL``` hodnotu.

### MariaDB - utf8mb4 a InnoDB podpora

WebJET používa predvolene ```storage engine InnoDB```, to je nastavené v konfiguračnej premennej ```mariaDbDefaultEngine```, ktorá vo WebJET 8 má hodnotu ```MyISAM``` a v novom WebJETe hodnotu ```InnoDB```. Pri použití ```InnoDB``` je možné využiť charset ```utf8mb4``` s celou podporou ```uft8``` kódovania (emotikony). Predvolene pri ```MyISAM``` tabuľkách je používané kódovanie ```utf8```, ktoré je ale v MySQL/MariaDB len 3 bytové a nepodporuje teda všetky znaky (rôzne emotikony).

Pre vytvorenie novej databázovej schémy a používateľa môžete použiť nasledovné SQL príkazy:

```sql
CREATE DATABASE xxx_web DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;
CREATE USER xxx_web IDENTIFIED BY 'gJ0gzNJSMwWIv4Fg';
GRANT ALL PRIVILEGES ON xxx_web.* TO `xxx_web`@`%`;
FLUSH PRIVILEGES;
```

## Rollback zmien

Zmeny v databázovej schéme sú spätne kompatibilné s verziou 8.8, štandardne nie je potrebné zmeny v schéme vracať nazad. Ak ale potrebujete zmeny vrátiť použite nasledovné SQL príkazy:

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