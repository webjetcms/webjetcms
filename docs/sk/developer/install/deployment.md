# Deployment buildu na artifactory/maven server

Deployment jar archívov pre použitie v klientských projektoch je založené na príprave jar archívov kompatibilných s pôvodnou štruktúrou WebJET 8.

## Pred vytvorením buildu

Pred vytvorením buildu je potrebné manuálne vykonať/skontrolovať nasledovné kroky:

- pripraviť popis zmien v súbore ```docs/CHANGELOG.md```
- upraviť súbor ```docs/README.md``` - pridať na vrch najnovšiu verziu z changelogu a dole zmazať poslednú (v README.md sa zobrazuje 5 posledných verzií)
- upraviť prekladový kľúč ```admin.overview.changelog``` so sumárom zmien aktuálnej verzie zobrazený pod uvítacím textom na úvodnej obrazovke
- upraviť ```src/main/webapp/admin/v9/json/wjnews.LANG.json``` - doplniť sumár a odkaz na changelog najnovšej verzie

Ak sa mení verzia, aktualizujte ju v:

- `ant/build.xml`

odtiaľ sa prenesie aj do `build.properties` pre zobrazenie verzie v administrácii.

## ANT task

[Build súbor](../../../../ant/build.xml) obsahuje viacero ```task``` elementov, finálny je ```deploy```, ktorý má korektne nastavené závislosti, takže stačí spustiť ten. Zoznam ```taskov```:

- ```setup``` - obnoví závislosti a vygeneruje ```WAR``` archív
- ```updatezip``` - pripraví dočasnú štruktúru v ```build/updatezip``` adresári. Štruktúra obsahuje rozbalený ```WAR``` archív, rozbalené ```webjet-XXXX.jar``` súbory (čiže kompletnú štruktúru adresárov /admin, /components a /WEB-INF/classes)
- ```preparesrc``` - stiahne ```SRC``` jar súbor a pripraví štruktúru pre jar archív so zdrojovými súbormi (spojené z jar archívu a zdrojového kódu WebJET 2021)
- ```define-artifact-properties``` - zadefinuje vlastnosti pre generovanie artifaktov, tu sa v ```artifact.version``` nastavuje verzia vygenerovaného artifaktu
- ```makejars``` - pripraví jar archívy tried, /admin a /components adresárov a zdrojových súborov
- ```download``` - pomocná úloha na stiahnutie jar archívov, ktoré sa nemodifikujú (```struts, daisydiff, jtidy, swagger```)
- ```makepom``` - vygenerovanie ```POM``` súboru, ten sa generuje gradle úlohou ```writePom``` na základe definovaných závislostí v ```build.gradle```. Úloha zabezpečuje získanie správnej verzie aj z definícií verzie typu ```5.3.+``` a odstránenie závislostí na WebJETe samotnom (ktorý pochádza zo závislostí na v8).
- ```finalwar``` - vytvorí v priečinku ```build/updatezip/finalwar``` novú štruktúru so skompilovanými triedami vrátane ```AspectJ```. Vytvorí JAR archívy ```WEB-INF/lib/webjet-VERZIA.jar``` s Java triedami, JSP súbormi admin časti a aplikáciami vo forme ```JarPackaging```.
- ```prepareAllJars``` - pripraví všetky JAR súbory na publikovanie do repozitárov.
- ```deployStaging``` - deploy verzie na https://repo1.maven.org/maven2/com/webjetcms/webjetcms/, pred spustením je potrebné volať úlohu ```prepareAllJars```, vykonať v projekte z ```github```.

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
  - spojí zdrojové kódy aktuálnej a verzie 8 z ```/src/main/java```, ```/src/webjet8/java``` a ```/src/main/aspectj``` do ```src-utf8```
- task ```delombok```
  - vykoná sa ```delombok``` - extrapolovanie lombok anotácií, keďže ```AspectJ``` má s lombok anotáciami problém
  - výsledok je v adresári ```/src-delombok/```
- task ```compileMapstruct```
  - skompilujú sa cez štandardné ```javac``` všetky triedy z adresára ```/src-delombok/```, pretože ```AspectJ``` nevie korektne skompilovať ```mapstruct``` triedy (vytvoriť ich ```Impl``` verziu)
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
