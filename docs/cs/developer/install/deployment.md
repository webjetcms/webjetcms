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

[Build soubor](../../../../ant/build.xml) obsahuje více ```task``` elementů, finální je ```deploy```, který má korektně nastavené závislosti, takže stačí spustit ten. Seznam ```taskov```:

- ```setup``` - ​​obnoví závislosti a vygeneruje ```WAR``` archiv
- ```updatezip``` - ​​připraví dočasnou strukturu v ```build/updatezip``` adresáři. Struktura obsahuje rozbalený ```WAR``` archiv, rozbalené ```webjet-XXXX.jar``` soubory (tj. kompletní strukturu adresářů /admin, /components a /WEB-INF/classes)
- ```preparesrc``` - ​​stáhne ```SRC``` jaro soubor a připraví strukturu pro jaro archiv se zdrojovými soubory (spojeno z jaro archivu a zdrojového kódu WebJET 2021)
- ```define-artifact-properties``` - ​​zadefinuje vlastnosti pro generování artifaktů, zde se v ```artifact.version``` nastavuje verze vygenerovaného artifaktu
- ```makejars``` - ​​připraví jaro archivy tříd, /admin a /components adresářů a zdrojových souborů
- ```download``` - ​​pomocná úloha ke stažení jaro archivů, které se nemodifikují (```struts, daisydiff, jtidy, swagger```)
- ```makepom``` - ​​vygenerování ```POM``` souboru, ten se generuje gradle úlohou ```writePom``` na základě definovaných závislostí v ```build.gradle```. Úloha zajišťuje získání správné verze iz definic verze typu ```5.3.+``` a odstranění závislostí na WebJET samotném (který pochází ze závislostí na v8).
- ```finalwar``` - ​​vytvoří ve složce ```build/updatezip/finalwar``` novou strukturu se zkompilovanými třídami včetně ```AspectJ```. Vytvoří JAR archivy ```WEB-INF/lib/webjet-VERZIA.jar``` s Java třídami, JSP soubory admin části a aplikacemi ve formě ```JarPackaging```.
- ```prepareAllJars``` - ​​připraví všechny JAR soubory pro publikování do repozitářů.
- ```deployGithub``` - ​​deploy SNAPSHOT verze na [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/2426502/versions).
- ```deployMavenCentral``` - ​​deploy verze na https://repo1.maven.org/maven2/com/webjetcms/webjetcms/, před spuštěním je třeba volat úlohu ```prepareAllJars```, provést v projektu z ```github```.

Postup vygenerování nové verze:

```shell
#nezabudni vypnut beziaci npm watch a Tomcat !!!
cd ant
ant deploy
```

*Poznámka*: v adresáři ```build/updatezip``` vznikne rozbalená struktura, tu je možné sezipovat a použít jako aktualizační balíček pro WebJET ve staré struktuře (nepoužívající `jar` archivy).

## Kompilace Java a AspectJ

Zdrojové kódy Java se během úlohy ```setup``` kompilují pomocí Gradle. Plugin ```io.freefair.aspectj.post-compile-weaving``` nejprve nechá ```javac``` a anotační procesory jako Lombok a MapStruct vygenerovat třídy a následně je před vytvořením WAR archivu zpracuje pomocí AspectJ weavingu. Samostatná kompilace přes Ant/AJC již není potřeba.

## Použití v klientských projektech

V klientských projektech stačí nastavit příslušnou verzi v build.gradle:

```gradle
ext {
    webjetVersion = "2023.0-SNAPSHOT";
}
```

pokusně jsme ověřili základní funkčnost na projektech s MariaDB, Microsoft SQL i Oracle DB.
