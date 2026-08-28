# Deployment buildu na artifactory/maven server

Deployment jar archívov pre použitie v klientských projektoch je založené na príprave jar archívov kompatibilných s pôvodnou štruktúrou WebJET 8.

## Pred vytvorením buildu

Pred vytvorením buildu je potrebné manuálne vykonať/skontrolovať nasledovné kroky:

- pripraviť popis zmien v súbore ```docs/CHANGELOG.md```
- upraviť súbor ```docs/README.md``` - pridať na vrch najnovšiu verziu z changelogu
- upraviť prekladový kľúč ```admin.overview.changelog``` so sumárom zmien aktuálnej verzie zobrazený pod uvítacím textom na úvodnej obrazovke
- upraviť ```src/main/webapp/admin/v9/json/wjnews.LANG.json``` - doplniť sumár a odkaz na changelog najnovšej verzie

Ak sa mení verzia, aktualizujte ju v:

- `ant/build.xml`

odtiaľ sa prenesie aj do `build.properties` pre zobrazenie verzie v administrácii.

## ANT task

[Build súbor](../../../../ant/build.xml) obsahuje viacero cieľov. Ich aktuálny zoznam a popis zobrazíte príkazom ```ant -f ant/build.xml -p```. Samostatný cieľ ```deploy``` neexistuje; podľa typu výstupu alebo cieľového repozitára použite jeden z nasledujúcich cieľov:

- ```setup``` - obnoví závislosti, skompiluje projekt a vygeneruje ```WAR``` archív, JavaDoc a zdrojové súbory spracované cez Delombok
- ```expandwar``` - spustí ```setup``` a rozbalí vygenerovaný ```WAR``` archív do adresára ```build/updatezip/WebContent```
- ```define-artifact-properties``` - definuje vlastnosti pre generovanie artifaktov; verzia vygenerovaného artifaktu sa nastavuje vo vlastnosti ```artifact.version```
- ```makejars``` - pripraví JAR archívy s triedami, zdrojovými súbormi, JavaDoc dokumentáciou a obsahom adresárov ```/admin``` a ```/components```
- ```makepom``` - vygeneruje a overí ```POM``` súbor Gradle úlohou ```verifyGeneratedPom```. Konkrétne verzie priamych závislostí zapisuje Gradle cez ```versionMapping```; WebJET následne nastaví kontrakt pre externý Tomcat, upraví legacy rozsah a odstráni historické interné závislosti.
- ```finalwar``` - vytvorí štruktúru ```build/updatezip/finalwar``` a archív ```build/updatezip/webjetcms.war``` s aplikáciami zabalenými ako JAR súbory
- ```createUpdateZip``` - vytvorí ```build/updatezip/artifacts/archive.zip``` pre aktualizáciu starej rozbalenej inštalácie bez JAR balenia
- ```createUpdateZipJar``` - vytvorí ```build/updatezip/artifacts/archive-jar.zip``` pre inštaláciu používajúcu JAR balenie; spúšťa sa po príprave artifaktov
- ```prepareAllJars``` - pripraví všetky JAR súbory a ```POM``` súbor na publikovanie do repozitárov
- ```deployGithub``` - pripraví artifakty a publikuje SNAPSHOT verziu do [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/2426502/versions)
- ```deployMavenCentral``` - po potvrdení verzie pripraví artifakty a podpisy, vytvorí publikačný ZIP súbor a odošle ho do [Maven Central](https://repo1.maven.org/maven2/com/webjetcms/webjetcms/)

Spring Boot Gradle plugin vytvára [executable a plain archív](https://docs.spring.io/spring-boot/gradle-plugin/packaging.html#packaging-executable-and-plain-archives). Úloha `bootWar` vytvorí `build/libs/webjetcms.war`, ktorý je spustiteľný cez `java -jar` a zároveň nasaditeľný do externého Tomcatu. Embedded runtime je v ňom uložený v `WEB-INF/lib-provided`, takže ho externý kontajner nenačíta. Štandardná Gradle úloha `war` dostane od Spring Boot pluginu classifier `plain` a vytvorí `build/libs/webjetcms-plain.war`. Legacy Ant úlohy `expandwar` a `finalwar` zámerne rozbaľujú `webjetcms-plain.war`, pretože zodpovedá pôvodnému štandardnému WAR bez Spring Boot loadera a adresára `WEB-INF/lib-provided`.

Maven/JAR publikácia a plain WAR sú určené pre zákaznícke projekty nasadené do externého Tomcatu. Embedded Tomcat slúži iba pre lokálny `bootRun` a executable WAR. Ant úloha `setup` preto volá Gradle úlohu `prepareAntWar`, ktorá vytvorí plain WAR a osobitne pripraví BOM-resolvené `providedRuntime` knižnice iba pre legacy `javac` a AspectJ classpath. Tieto knižnice sa nepridávajú do výsledného legacy archívu ani do runtime závislostí Maven konzumenta.

Pri priamom spustení Ant úlohy je potrebné najskôr aktivovať verziu Node.js definovanú v koreňovom súbore `.nvmrc` príkazmi `nvm install` a `nvm use`. Skript `ant/deploy.sh` tieto príkazy vykoná automaticky.

## Správa verzií Java závislostí

WebJET používa natívnu podporu Gradle pre [Spring Boot BOM](https://docs.spring.io/spring-boot/gradle-plugin/managing-dependencies.html). BOM (Bill of Materials) nepridáva knižnice do projektu, ale poskytuje odporúčanú množinu vzájomne kompatibilných verzií Spring Boot, Spring Framework, Spring Security, Jackson, Tomcat a ďalších knižníc.

BOM sa importuje ako Gradle platforma:

```gradle
dependencies {
    implementation platform(
        org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
    )
    providedCompile platform(
        org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
    )
    providedRuntime platform(
        org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
    )

    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.security:spring-security-webauthn'
    providedRuntime 'org.springframework.boot:spring-boot-starter-tomcat-runtime'
    providedRuntime 'org.apache.tomcat.embed:tomcat-embed-jasper'
}
```

`SpringBootPlugin.BOM_COORDINATES` použije rovnakú verziu BOM ako aplikovaný Spring Boot Gradle plugin. Nevznikne tak druhá verzia, ktorú by bolo potrebné manuálne synchronizovať. Platforma ovplyvňuje iba konfiguráciu, v ktorej je deklarovaná, a konfigurácie, ktoré z nej dedia. WebJET ju preto deklaruje v `implementation` aj v samostatne resolvovateľných legacy konfiguráciách `providedCompile` a `providedRuntime`. Spring Boot 4 oddeľuje runtime kontajnera do `spring-boot-starter-tomcat-runtime`; Jasper je deklarovaný osobitne, pretože runtime starter ho neobsahuje.

Pri pridávaní alebo aktualizovaní závislostí platia tieto pravidlá:

- pri artefakte spravovanom Spring Boot BOM sa verzia neuvádza; verzia Spring Boot sa nesmie použiť ako verzia Spring Security alebo inej samostatne verzovanej knižnice,
- pri artefakte, ktorý BOM nespravuje, sa uvádza explicitná verzia,
- výnimkou je spoločný pin pre nástroje mimo grafu závislostí; aktuálne `aspectJVersion` musí zarovnať AspectJ runtime, Java agent a Ant kompilátor a úloha `verifyGeneratedPom` ich zhodu kontroluje,
- používa sa `platform`, nie `enforcedPlatform`; bežná platforma poskytuje odporúčania a umožňuje Gradlu vyhodnotiť ostatné verzie a obmedzenia v grafe, ktoré môžu vybrať inú kompatibilnú verziu,
- bezpečnostná výnimka musí byť úzko zamerané obmedzenie s uvedeným dôvodom; kombinácia rozsahu `require` a minimálnej verzie `prefer` vytvorí bezpečnostnú spodnú hranicu, ale dovolí budúcemu Boot BOM vybrať novšiu povolenú verziu,
- nepoužíva sa globálny `force`, `strictly` alebo `resolutionStrategy.useVersion`, ktorý by mohol budúcu opravenú verziu znížiť,
- každá výnimka sa overí grafom závislostí a integračnými testami a odstráni sa, keď ju už aktuálny Boot BOM nepotrebuje.

Knižnica `webjet-ai` musí byť kompilovaná a testovaná s verziovým radom Jacksonu kompatibilným s aktuálnym Spring Boot BOM. WebJET preto nepridáva samostatný Jackson `enforcedPlatform`. Ak by budúca verzia knižnice vyžadovala novší Jackson, musí sa ako vedomá výnimka posúdiť a otestovať celá rodina Jackson knižníc.

Aktuálne vybrané verzie je možné skontrolovať napríklad príkazmi:

```shell
./gradlew dependencyInsight \
    --dependency spring-security-webauthn \
    --configuration runtimeClasspath

./gradlew dependencyInsight \
    --dependency jackson-databind \
    --configuration runtimeClasspath

./gradlew dependencyInsight \
    --dependency tomcat-embed-core \
    --configuration runtimeClasspath
```

### Generovanie Maven POM

Závislosti bez verzie sú platné v `build.gradle`, pretože ich verzie dodáva BOM. Publikácia používa Gradle [`versionMapping`](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:resolved_dependencies), ktoré do POM pre priame závislosti zapíše konkrétne verzie vybrané z `runtimeClasspath`. POM tak zodpovedá verziám, s ktorými bol WebJET zostavený a testovaný; nejde o uzamknutie celého tranzitívneho grafu. Import Spring Boot BOM sa zároveň publikuje v časti `dependencyManagement`.

Interný Gradle graf zámerne obsahuje embedded Tomcat pre `bootRun` a executable WAR. Publikačný POM má iný runtime kontrakt: `spring-boot-starter-tomcat-runtime`, `tomcat-embed-jasper` a API dodávané aplikačným serverom publikuje so scope `provided`. Zo `spring-boot-starter-webmvc` vylučuje `spring-boot-starter-tomcat`; zo Springdoc vetvy vylučuje `tomcat-embed-el`. Bez týchto vylúčení by sa Tomcat vrátil inou tranzitívnou cestou aj napriek scope `provided`. Maven scope `provided` nie je tranzitívny, preto zákaznícky WAR tieto knižnice nepreberie.

Generovanie Gradle module metadata (`.module`) je pre túto publikáciu vypnuté. Metadata komponentu by opisovala interný executable-WAR graf a Gradle konzument by ju uprednostnil pred POM-om, čím by obišiel externý Tomcat kontrakt. Maven POM je preto jediný publikovaný model závislostí.

Časť `dependencyManagement` môže naďalej obsahovať verzie `tomcat-embed-*`, ktoré vznikli z Gradle constraints. Samotný blok žiadnu knižnicu nepridáva. Udržiava zarovnanie interného executable WAR-u a využije sa iba v projekte, ktorý si embedded Tomcat vedome pridá ako vlastnú závislosť.

Maven `dependencyManagement` sa automaticky nededí iba tým, že projekt pridá WebJET ako závislosť. Maven konzument preto musí vo vlastnom builde importovať rovnaký Spring Boot BOM. Inak môže závislosť dostupná iba cez knižnicu tretej strany ponechať inú Spring Boot patch verziu, hoci priame WebJET závislosti už majú vyhodnotené verzie.

Ak publikovaný WebJET POM používa vyššiu vyhodnotenú verziu bezpečnostne aktualizovanej knižnice ako importovaný Boot BOM, Maven konzument ju musí zopakovať vo vlastnom `dependencyManagement`. Správa závislostí konzumenta má prednosť aj pred priamou verziou vo WebJET POM. Aktuálne sa to týka Log4j-to-SLF4J bridge; konfigurácia vyzerá nasledovne:

```xml
<properties>
    <spring-boot.version>4.1.1</spring-boot.version>
    <webjet-log4j-to-slf4j.version>2.26.1</webjet-log4j-to-slf4j.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-to-slf4j</artifactId>
            <version>${webjet-log4j-to-slf4j.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Verzia Spring Boot sa musí zhodovať s verziou pluginu v príslušnom WebJET release. Hodnota Log4j bridge sa musí zhodovať s konkrétnou vyhodnotenou verziou v publikovanom WebJET POM; `log4jToSlf4jMinimumVersion` je iba spodná hranica a po budúcej aktualizácii BOM môže byť nižšia než skutočne vybraná verzia. Keď už Boot BOM spravuje rovnakú alebo novšiu kompatibilnú verziu, samostatná položka sa odstráni.

Verzia externého Tomcatu je prevádzková požiadavka na aplikačný server, nie závislosť zákazníckeho WAR-u. Musí spĺňať verziu podporovanú daným WebJET release a bezpečnostné aktualizácie sa vykonávajú aktualizáciou servera. `tomcatMinimumVersion` v `build.gradle` chráni iba embedded runtime vývojového/executable variantu; zákaznícky projekt nemá pridávať `tomcat-embed-*` do svojho `dependencyManagement`, pokiaľ vedome nevytvára vlastnú embedded distribúciu.

Úloha `writePom` už verzie sama nevypočítava. Nad POM vygenerovaným Gradlom vykoná WebJET špecifické úpravy rozsahu a odstránenie historických interných závislostí. Exclusions tvoria súčasť publikačného kontraktu; najmä vylúčenia embedded Tomcatu sa nesmú pri úprave alebo porovnávaní POM odstrániť. Verziu preto nikdy nepridávajte do `dependencies` iba kvôli generovaniu POM.

Oba POM varianty je možné vygenerovať a ich priame závislosti automaticky porovnať s vyhodnoteným `runtimeClasspath` príkazom:

```shell
./gradlew verifyGeneratedPom \
    -Dwjgroup=com.webjetcms \
    -Dwjname=webjetcms \
    -Dwjversion=TEST-SNAPSHOT
```

Kontrola overí štandardný POM v `build/publications/maven/pom-default.xml` aj legacy POM v `build/updatezip/artifacts/webjetcms-TEST-SNAPSHOT.pom`. Maven musí oba vyhodnotiť ako `jar` (Gradle pre túto predvolenú hodnotu element `packaging` v XML vynechá), každá bežná priama závislosť musí mať konkrétnu vyhodnotenú verziu, Spring Boot BOM musí byť importovaný práve raz a samostatný import Jackson 2 ani Jackson 3 BOM mimo Spring Boot BOM nesmie byť prítomný.

Kontrola navyše chráni dve odlišné strany kontraktu. Interný `runtimeClasspath` a executable WAR musia mať všetky štyri Tomcat moduly zarovnané nad bezpečnostnou spodnou hranicou. Publikovaný POM musí mať runtime kontajnera a serverové API ako `provided`, zachovať vylúčenia tranzitívnych Tomcat vetiev a nesmie syntetickému zákazníckemu projektu sprístupniť `spring-boot-starter-tomcat*`, `spring-boot-tomcat`, `tomcat-annotations-api` ani `tomcat-embed-*`. Resolverový smoke test tak zachytí aj novú nepriamu cestu cez inú knižnicu.

Rovnakú kontrolu spúšťa CI aj Ant úloha `makepom`. Zlyhanie validácie tak zastaví release ešte pred podpisovaním a nahrávaním artefaktov.

Postup vygenerovania novej verzie:

```shell
# nezabudnite vypnúť bežiaci npm watch a Tomcat
cd ant

# iba lokálna príprava JAR a POM súborov
ant prepareAllJars

# publikovanie SNAPSHOT verzie do GitHub Packages
ant deployGithub

# publikovanie verzie do Maven Central
ant deployMavenCentral
```

Publikačné ciele ```deployGithub``` a ```deployMavenCentral``` automaticky spustia ```prepareAllJars```. Aktualizačné ZIP archívy je možné vytvoriť samostatne:

```shell
cd ant
ant createUpdateZip
ant prepareAllJars createUpdateZipJar
```

## Kompilácia Java a AspectJ

Zdrojové kódy Java sa počas úlohy ```setup``` kompilujú cez Gradle. Plugin ```io.freefair.aspectj.post-compile-weaving``` najskôr nechá ```javac``` a anotačné procesory ako Lombok a MapStruct vygenerovať triedy a následne ich pred vytvorením WAR archívu spracuje pomocou `AspectJ weaving`. Samostatná kompilácia cez Ant/AJC už nie je potrebná.

## Použitie v klientských projektoch

V klientských projektoch stačí nastaviť príslušnú verziu v build.gradle:

```gradle
ext {
    webjetVersion = "2023.0-SNAPSHOT";
}

dependencies {
    implementation("com.webjetcms:webjetcms:${webjetVersion}")

    // API poskytuje externý Tomcat; do WEB-INF/lib sa nesmú zabaliť.
    providedCompile 'jakarta.servlet:jakarta.servlet-api'
    providedCompile 'jakarta.servlet.jsp:jakarta.servlet.jsp-api'
    providedCompile 'jakarta.el:jakarta.el-api'
    providedCompile 'jakarta.annotation:jakarta.annotation-api'
}
```

Klientsky build musí mať pre uvedené závislosti dostupné verzie, ideálne importom rovnakého Spring Boot BOM. Pri Maven projekte sa rovnaké API deklarujú so scope `provided`. Priame `provided` deklarácie v klientskom projekte zároveň zabezpečia, že API privedené tranzitívne inou knižnicou neskončia vo výslednom `WEB-INF/lib`.

pokusne sme overili základnú funkčnosť na projektoch s MariaDB, Microsoft SQL aj Oracle DB.
