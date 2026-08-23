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

## Použitie v klientských projektoch

V klientských projektoch stačí nastaviť príslušnú verziu v build.gradle:

```gradle
ext {
    webjetVersion = "2023.0-SNAPSHOT";
}
```

pokusne sme overili základnú funkčnosť na projektoch s MariaDB, Microsoft SQL aj Oracle DB.
