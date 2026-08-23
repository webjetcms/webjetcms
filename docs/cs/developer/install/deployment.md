# Deployment buildu na artifactory/maven server

Deployment jaro archivů pro použití v klientských projektech je založeno na přípravě jaro archivů kompatibilních s původní strukturou WebJET 8.

## Před vytvořením buildu

Před vytvořením buildu je třeba manuálně provést/zkontrolovat následující kroky:

- připravit popis změn v souboru ```docs/CHANGELOG.md```
- upravit soubor ```docs/README.md``` - ​​přidat na vrch nejnovější verzi z changelogu
- upravit překladový klíč ```admin.overview.changelog``` se sumárem změn aktuální verze zobrazený pod uvítacím textem na úvodní obrazovce
- upravit ```src/main/webapp/admin/v9/json/wjnews.LANG.json``` - ​​doplnit sumář a odkaz na changelog nejnovější verze

Pokud se mění verze, aktualizujte ji v:

- `ant/build.xml`

odtud se přenese i do `build.properties` pro zobrazení verze v administraci.

## ANT task

[Build soubor](../../../../ant/build.xml) obsahuje více cílů. Jejich aktuální seznam a popis zobrazíte příkazem ```ant -f ant/build.xml -p```. Samostatný cíl ```deploy``` neexistuje; podle typu výstupu nebo cílového repozitáře použijte jeden z následujících cílů:

- ```setup``` - ​​obnoví závislosti, zkompiluje projekt a vygeneruje ```WAR``` archiv, JavaDoc a zdrojové soubory zpracované přes Delombok
- ```expandwar``` - ​​spustí ```setup``` a rozbalí vygenerovaný ```WAR``` archiv do adresáře ```build/updatezip/WebContent```
- ```define-artifact-properties``` - ​​definuje vlastnosti pro generování artifaktů; verze vygenerovaného artifaktu se nastavuje ve vlastnosti ```artifact.version```
- ```makejars``` - ​​připraví JAR archivy s třídami, zdrojovými soubory, JavaDoc dokumentací a obsahem adresářů ```/admin``` a ```/components```
- ```makepom``` - ​​vygeneruje ```POM``` soubor Gradle úlohou ```writePom``` na základě závislostí definovaných v ```build.gradle```
- ```finalwar``` - ​​vytvoří strukturu ```build/updatezip/finalwar``` a archiv ```build/updatezip/webjetcms.war``` s aplikacemi zabalenými jako JAR soubory
- ```createUpdateZip``` - ​​vytvoří ```build/updatezip/artifacts/archive.zip``` pro aktualizaci staré rozbalené instalace bez JAR balení
- ```createUpdateZipJar``` - ​​vytvoří ```build/updatezip/artifacts/archive-jar.zip``` pro instalaci používající JAR balení; spouští se po přípravě artifaktů
- ```prepareAllJars``` - ​​připraví všechny JAR soubory a ```POM``` soubor pro publikování do repozitářů
- ```deployGithub``` - ​​připraví artifakty a publikuje SNAPSHOT verzi do [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/2426502/versions)
- ```deployMavenCentral``` - ​​po potvrzení verze připraví artifakty a podpisy, vytvoří publikační ZIP soubor a odešle jej do [Maven Central](https://repo1.maven.org/maven2/com/webjetcms/webjetcms/)

Postup vygenerování nové verze:

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

Publikační cíle ```deployGithub``` a ```deployMavenCentral``` automaticky spustí ```prepareAllJars```. Aktualizační ZIP archivy lze vytvořit samostatně:

```shell
cd ant
ant createUpdateZip
ant prepareAllJars createUpdateZipJar
```

## Kompilace Java a AspectJ

Zdrojové kódy Java se během úlohy ```setup``` kompilují přes Gradle. Plugin ```io.freefair.aspectj.post-compile-weaving``` nejprve nechá ```javac``` a anotační procesory jako Lombok a MapStruct vygenerovat třídy a následně je před vytvořením WAR archivu zpracuje pomocí `AspectJ weaving`. Samostatná kompilace přes Ant/AJC již není nutná.

## Použití v klientských projektech

V klientských projektech stačí nastavit příslušnou verzi v build.gradle:

```gradle
ext {
    webjetVersion = "2023.0-SNAPSHOT";
}
```

pokusně jsme ověřili základní funkčnost na projektech s MariaDB, Microsoft SQL i Oracle DB.
