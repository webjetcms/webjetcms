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

[Build súbor](../../../../ant/build.xml) obsahuje viacero ```task``` elementov, finálny je ```deploy```, ktorý má korektne nastavené závislosti, takže stačí spustiť ten. Zoznam ```taskov```:

- ```setup``` - obnoví závislosti a vygeneruje ```WAR``` archív; zlyhanie ktoréhokoľvek Gradle kroku okamžite zastaví Ant build
- ```updatezip``` - pripraví dočasnú štruktúru v ```build/updatezip``` adresári. Štruktúra obsahuje rozbalený ```WAR``` archív, rozbalené ```webjet-XXXX.jar``` súbory (čiže kompletnú štruktúru adresárov /admin, /components a /WEB-INF/classes)
- ```preparesrc``` - stiahne ```SRC``` jar súbor a pripraví štruktúru pre jar archív so zdrojovými súbormi (spojené z jar archívu a zdrojového kódu WebJET 2021)
- ```define-artifact-properties``` - zadefinuje vlastnosti pre generovanie artifaktov, tu sa v ```artifact.version``` nastavuje verzia vygenerovaného artifaktu
- ```makejars``` - pripraví jar archívy tried, /admin a /components adresárov a zdrojových súborov
- ```download``` - pomocná úloha na stiahnutie jar archívov, ktoré sa nemodifikujú (```struts, daisydiff, jtidy, swagger```)
- ```makepom``` - vygeneruje a overí ```POM``` súbor Gradle úlohou ```verifyGeneratedPom```. Konkrétne verzie priamych závislostí zapisuje Gradle cez ```versionMapping```; WebJET následne upraví legacy rozsah a odstráni historické interné závislosti.
- ```finalwar``` - vytvorí v priečinku ```build/updatezip/finalwar``` novú štruktúru so skompilovanými triedami vrátane ```AspectJ```. Vytvorí JAR archívy ```WEB-INF/lib/webjet-VERZIA.jar``` s Java triedami, JSP súbormi admin časti a aplikáciami vo forme ```JarPackaging```.
- ```prepareAllJars``` - pripraví všetky JAR súbory na publikovanie do repozitárov.
- ```deployGithub``` - deploy SNAPSHOT verzie na [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/2426502/versions).
- ```deployMavenCentral``` - deploy verzie na https://repo1.maven.org/maven2/com/webjetcms/webjetcms/, pred spustením je potrebné volať úlohu ```prepareAllJars```, vykonať v projekte z ```github```.

Spring Boot Gradle plugin vytvára [executable a plain archív](https://docs.spring.io/spring-boot/gradle-plugin/packaging.html#packaging-executable-and-plain-archives). Úloha `bootWar` vytvorí `build/libs/webjetcms.war`, ktorý je spustiteľný cez `java -jar` a zároveň nasaditeľný do externého Tomcatu. Štandardná Gradle úloha `war` dostane od Spring Boot pluginu classifier `plain` a vytvorí `build/libs/webjetcms-plain.war`. Legacy Ant úlohy `expandwar` a `finalwar` zámerne rozbaľujú `webjetcms-plain.war`, pretože zodpovedá pôvodnému štandardnému WAR bez Spring Boot loadera a adresára `WEB-INF/lib-provided`. Ant úloha `setup` preto volá Gradle úlohu `prepareAntWar`, ktorá vytvorí plain WAR a osobitne pripraví BOM-resolvené `providedRuntime` knižnice iba pre legacy `javac` a AspectJ classpath. Tieto knižnice sa nepridávajú do výsledného legacy archívu. Executable variant `webjetcms.war` sa používa pre Spring Boot distribúciu a samostatný smoke test nasadenia do externého Tomcatu.

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
}
```

`SpringBootPlugin.BOM_COORDINATES` použije rovnakú verziu BOM ako aplikovaný Spring Boot Gradle plugin. Nevznikne tak druhá verzia, ktorú by bolo potrebné manuálne synchronizovať. Platforma ovplyvňuje iba konfiguráciu, v ktorej je deklarovaná, a konfigurácie, ktoré z nej dedia. WebJET ju preto deklaruje v `implementation` aj v samostatne resolvovateľných legacy konfiguráciách `providedCompile` a `providedRuntime`.

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

Závislosti bez verzie sú platné v `build.gradle`, pretože ich verzie dodáva BOM. WebJET cez `versionMapping` zámerne publikuje konkrétne vyhodnotené verzie priamych závislostí, aby POM zodpovedal priamym závislostiam, s ktorými bol zostavený a testovaný. Nejde o uzamknutie celého tranzitívneho grafu.

Publikácia preto používa Gradle [`versionMapping`](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:resolved_dependencies). Do POM sa pre priame závislosti zapisujú verzie vybrané z `runtimeClasspath`, teda verzie, s ktorými bol WebJET zostavený a testovaný. Import Spring Boot BOM sa zároveň publikuje v časti `dependencyManagement`.

Maven `dependencyManagement` sa automaticky nededí iba tým, že projekt pridá WebJET ako závislosť. Maven konzument preto musí vo vlastnom builde importovať rovnaký Spring Boot BOM. Inak môže závislosť dostupná iba cez knižnicu tretej strany ponechať inú Spring Boot patch verziu, hoci priame WebJET závislosti už majú vyhodnotené verzie.

Ak publikovaný WebJET POM používa vyššiu vyhodnotenú verziu bezpečnostne aktualizovanej knižnice ako importovaný Boot BOM, Maven konzument ju musí zopakovať vo vlastnom `dependencyManagement`. Správa závislostí konzumenta má prednosť aj pred priamou verziou vo WebJET POM. Aktuálne sa to týka Tomcatu a Log4j-to-SLF4J bridge; konfigurácia vyzerá nasledovne:

```xml
<properties>
    <spring-boot.version>4.1.1</spring-boot.version>
    <webjet-tomcat.version>11.0.25</webjet-tomcat.version>
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
            <groupId>org.apache.tomcat.embed</groupId>
            <artifactId>tomcat-embed-core</artifactId>
            <version>${webjet-tomcat.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.tomcat.embed</groupId>
            <artifactId>tomcat-embed-el</artifactId>
            <version>${webjet-tomcat.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.tomcat.embed</groupId>
            <artifactId>tomcat-embed-jasper</artifactId>
            <version>${webjet-tomcat.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.tomcat.embed</groupId>
            <artifactId>tomcat-embed-websocket</artifactId>
            <version>${webjet-tomcat.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-to-slf4j</artifactId>
            <version>${webjet-log4j-to-slf4j.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Verzia Spring Boot sa musí zhodovať s verziou pluginu v príslušnom WebJET release. Hodnoty Tomcatu a Log4j bridge sa musia zhodovať s konkrétnymi vyhodnotenými verziami v publikovanom WebJET POM; `tomcatMinimumVersion` a `log4jToSlf4jMinimumVersion` sú iba spodné hranice a po budúcej aktualizácii BOM môžu byť nižšie než skutočne vybrané verzie. Keď už Boot BOM spravuje rovnakú alebo novšiu kompatibilnú verziu, príslušné samostatné položky sa odstránia.

Úloha `writePom` už verzie sama nevypočítava. Nad POM vygenerovaným Gradlom vykoná iba WebJET špecifické úpravy rozsahu a odstránenie historických interných závislostí. Deklarované vylúčenia zachováva, aby sa k Maven konzumentovi nepreniesli vylúčené tranzitívne artefakty. Verziu preto nikdy nepridávajte do `dependencies` iba kvôli generovaniu POM.

Oba POM varianty je možné vygenerovať a ich priame závislosti automaticky porovnať s vyhodnoteným `runtimeClasspath` príkazom:

```shell
./gradlew verifyGeneratedPom \
    -Dwjgroup=com.webjetcms \
    -Dwjname=webjetcms \
    -Dwjversion=TEST-SNAPSHOT
```

Kontrola overí štandardný POM v `build/publications/maven/pom-default.xml` aj legacy POM v `build/updatezip/artifacts/webjetcms-TEST-SNAPSHOT.pom`. Maven musí oba vyhodnotiť ako `jar` (Gradle pre túto predvolenú hodnotu element `packaging` v XML vynechá), každá bežná priama závislosť musí mať konkrétnu vyhodnotenú verziu, Spring Boot BOM musí byť importovaný práve raz a samostatný import Jackson 2 ani Jackson 3 BOM mimo Spring Boot BOM nesmie byť prítomný. Kontrola navyše chráni Tomcat a Log4j bezpečnostné spodné hranice; Tomcat moduly musia zostať priame, zarovnané a so zachovaným vylúčením duplicitného `tomcat-annotations-api`.

Rovnakú kontrolu spúšťa CI aj Ant úloha `makepom`. Zlyhanie validácie tak zastaví release ešte pred podpisovaním a nahrávaním artefaktov.

Postup vygenerovania novej verzie:

```shell
#nezabudni vypnut beziaci npm watch a Tomcat !!!
cd ant
ant deploy
```

*Poznámka*: v adresári ```build/updatezip``` vznikne rozbalená štruktúra, tú je možné zozipovať a použiť ako aktualizačný balík pre WebJET v starej štruktúre (nepoužívajúcej `jar` archívy).

## Kompletné skompilovanie zdrojových kódov

Vo WebJET 2021 sa často upravuje existujúca trieda z verzie 8, čo môže viesť k nekompatibilite triedy. Z toho dôvodu existuje špeciálny ANT task ```compile``` v súbore ```ant/compile.xml```. Ten použije originálne zdrojové kódy k verzii 8, rozbalí ich do dočasného adresára, doplní zdrojovými kódmi z verzie 2021 a následne všetko skompiluje vrátane použitia ```AspectJ``` kompilátora.

Všetko sa deje v adresári ```/build/updatezip```, pred spustením kompilácie je potrebné zavolať nasledovné príkazy:

```sh
ant updatezip
ant download
```

Postup je nasledovný:

- task ```prepareSrc```
  - spojí zdrojové kódy aktuálnej a verzie 8 z ```/src/main/java``` a ```/src/main/aspectj``` do ```src-utf8```
- task ```delombok```
  - vykoná sa ```delombok``` - extrapolovanie lombok anotácií, keďže ```AspectJ``` má s lombok anotáciami problém
  - výsledok je v adresári ```/src-delombok/```
- task ```compileMapstruct```
  - skompilujú sa cez štandardné ```javac``` všetky triedy z adresára ```/src-delombok/```, pretože ```AspectJ``` nevie korektne skompilovať ```mapstruct``` triedy (vytvoriť ich ```Impl``` verziu)
  - classpath obsahuje knižnice z plain WAR a Gradle-resolvené `providedRuntime` knižnice pripravené úlohou `prepareAntWar` v adresári `/build/ant-provided-runtime`; tým používa rovnaké verzie API a embedded Tomcatu ako hlavný Gradle build bez ich pridania do výsledného legacy archívu
  - dodatočné jar knižnice berie z adresára ```/ant/libs```, jedná sa o triedy, ktoré sú v starom kóde WebJETu a sú potrebné na kompiláciu, ale už nie sú potrebné na beh WebJET CMS
  - výsledok kompilácie je v ```/WebContent/WEB-INF/classes/```
  - do adresára ```/src-aspectj/``` sa skopíruje zdrojový kód, ale vymažú sa všetky mapstruct triedy (tie, ktoré obsahujú v názve ```mapper/mappers```)
  - z adresára ```/WebContent/WEB-INF/classes/``` sa zmažú všetky skompilované triedy okrem ```mapper``` tried
  - z tried čo zostanú sa vytvorí JAR súbor ```/WebContent/WEB-INF/generated-sources/mapper-impl.jar```
- task ```compile```
  - vykoná ```AspectJ``` kompiláciu z adresára ```/src-aspectj/``` do adresára ```/WebContent/WEB-INF/classes```
  - použije JAR súbor ```/WebContent/WEB-INF/generated-sources/``` so skompilovanými ```mapper``` triedami
  - do výsledného adresára ```/WebContent/WEB-INF/classes``` rozbalí obsah ```mapper-impl.jar``` so skompilovanými ```MapperImpl.class``` triedami, aby ```classes``` adresár obsahoval všetky triedy
  - do ```classes``` adresára skopíruje z ```/src-delombok/``` súbory typu ```*.properties``` a ```*.xml```

Výsledkom je, že v adresári ```/WebContent/WEB-INF/classes``` je všetko skompilované voči kompletnému zdrojovému kódu WebJET verzie 8 aj aktuálnej verzie.

Volanie tohto samostatného ant ```tasku``` je začlenené priamo do hlavného buildu, kde existuje task ```compile``` volajúci tento samostatný task. Takže pri spustení deployment procesu nie je potrebné žiadne dodatočné volanie kompilácie, všetko prebehne automaticky.

## Použitie v klientských projektoch

V klientských projektoch stačí nastaviť príslušnú verziu v build.gradle:

```gradle
ext {
    webjetVersion = "2023.0-SNAPSHOT";
}
```

pokusne sme overili základnú funkčnosť na projektoch s MariaDB, Microsoft SQL aj Oracle DB.
