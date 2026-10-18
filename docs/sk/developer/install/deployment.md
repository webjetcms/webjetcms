# Deployment buildu na artifactory/maven server

Deployment jar archívov pre použitie v klientských projektoch je založené na príprave jar archívov kompatibilných s pôvodnou štruktúrou WebJET 8.

## Pred vytvorením buildu

Pred vytvorením buildu je potrebné manuálne vykonať/skontrolovať nasledovné kroky:

- pripraviť popis zmien v súbore ```docs/CHANGELOG.md```
- upraviť súbor ```docs/README.md``` - pridať na vrch najnovšiu verziu z changelogu
- upraviť prekladový kľúč ```admin.overview.changelog``` so sumárom zmien aktuálnej verzie zobrazený pod uvítacím textom na úvodnej obrazovke

Ak sa mení verzia, aktualizujte ju v:

- `ant/build.xml`

odtiaľ sa prenesie aj do `build.properties` pre zobrazenie verzie v administrácii.

## ANT task

[Build súbor](../../../../ant/build.xml) obsahuje viacero cieľov. Ich aktuálny zoznam a popis zobrazíte príkazom ```ant -f ant/build.xml -p```. Samostatný cieľ ```deploy``` neexistuje; podľa typu výstupu alebo cieľového repozitára použite jeden z nasledujúcich cieľov:

- ```setup``` - obnoví závislosti, skompiluje projekt a vygeneruje ```WAR``` archív, JavaDoc a zdrojové súbory spracované cez Delombok
- ```expandwar``` - spustí ```setup``` a rozbalí vygenerovaný ```WAR``` archív do adresára ```build/updatezip/WebContent```
- ```define-artifact-properties``` - definuje vlastnosti pre generovanie artifaktov; verzia vygenerovaného artifaktu sa nastavuje vo vlastnosti ```artifact.version```
- ```makejars``` - pripraví JAR archívy s triedami, zdrojovými súbormi, JavaDoc dokumentáciou a obsahom adresárov ```/admin``` a ```/components```
- ```makepom``` - vygeneruje ```POM``` súbor Gradle úlohou ```writePom``` na základe závislostí definovaných v ```build.gradle```
- ```finalwar``` - vytvorí štruktúru ```build/updatezip/finalwar``` a archív ```build/updatezip/webjetcms.war``` s aplikáciami zabalenými ako JAR súbory
- ```createUpdateZip``` - vytvorí ```build/updatezip/artifacts/archive.zip``` pre aktualizáciu starej rozbalenej inštalácie bez JAR balenia
- ```createUpdateZipJar``` - vytvorí ```build/updatezip/artifacts/archive-jar.zip``` pre inštaláciu používajúcu JAR balenie; spúšťa sa po príprave artifaktov
- ```prepareAllJars``` - pripraví všetky JAR súbory a ```POM``` súbor na publikovanie do repozitárov
- ```deployGithub``` - pripraví artifakty a publikuje SNAPSHOT verziu do [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/2426502/versions)
- ```deployMavenCentral``` - po potvrdení verzie pripraví artifakty a podpisy, vytvorí publikačný ZIP súbor a odošle ho do [Maven Central](https://repo1.maven.org/maven2/com/webjetcms/webjetcms/)

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

## Zoznam noviniek na docs serveri

Verejné súbory `wjnews.sk.json`, `wjnews.cs.json` a `wjnews.en.json` vytvára [centrálny generátor](social-media-feed.md) priamo na docs serveri. Cieľ `rsyncToLicenseServer` prenáša iba aktualizačné archívy na licenčný server; žiadne JSON súbory z `admin/v9/json/` na docs server nekopíruje. Nasadenie WebJET CMS tak neprepisuje generované novinky.

Generátor spustite cez automatizovanú úlohu na docs serveri. Zachovaná verejná URL je `https://docs.webjetcms.sk/json/wjnews.sk.json`, pre ostatné jazyky sa mení iba jazykový kód na `cs` alebo `en`. Novinky nainštalovanej verzie zostávajú súčasťou prekladového kľúča `admin.overview.changelog` a dashboard ich kombinuje s týmto zoznamom.

Balík `sk.iway.aceintegration.socialmedia` je vylúčený zo štandardného Gradle WAR a JAR aj z dopĺňania ukážkových tried v Ant `finalwar`. Ant JAR a aktualizačné ZIP archívy ho tiež nezahŕňajú. Na docs server ho nasaďte osobitne podľa návodu generátora.

## Použitie v klientských projektoch

V klientských projektoch stačí nastaviť príslušnú verziu v build.gradle:

```gradle
ext {
    webjetVersion = "2023.0-SNAPSHOT";
}
```

pokusne sme overili základnú funkčnosť na projektoch s MariaDB, Microsoft SQL aj Oracle DB.
