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

- ```setup``` - obnoví závislosti a vygeneruje ```WAR``` archív
- ```updatezip``` - pripraví dočasnú štruktúru v ```build/updatezip``` adresári. Štruktúra obsahuje rozbalený ```WAR``` archív, rozbalené ```webjet-XXXX.jar``` súbory (čiže kompletnú štruktúru adresárov /admin, /components a /WEB-INF/classes)
- ```preparesrc``` - stiahne ```SRC``` jar súbor a pripraví štruktúru pre jar archív so zdrojovými súbormi (spojené z jar archívu a zdrojového kódu WebJET 2021)
- ```define-artifact-properties``` - zadefinuje vlastnosti pre generovanie artifaktov, tu sa v ```artifact.version``` nastavuje verzia vygenerovaného artifaktu
- ```makejars``` - pripraví jar archívy tried, /admin a /components adresárov a zdrojových súborov
- ```download``` - pomocná úloha na stiahnutie jar archívov, ktoré sa nemodifikujú (```struts, daisydiff, jtidy, swagger```)
- ```makepom``` - vygenerovanie ```POM``` súboru, ten sa generuje gradle úlohou ```writePom``` na základe definovaných závislostí v ```build.gradle```. Úloha zabezpečuje získanie správnej verzie aj z definícií verzie typu ```5.3.+``` a odstránenie závislostí na WebJETe samotnom (ktorý pochádza zo závislostí na v8).
- ```finalwar``` - vytvorí v priečinku ```build/updatezip/finalwar``` novú štruktúru so skompilovanými triedami vrátane ```AspectJ```. Vytvorí JAR archívy ```WEB-INF/lib/webjet-VERZIA.jar``` s Java triedami, JSP súbormi admin časti a aplikáciami vo forme ```JarPackaging```.
- ```prepareAllJars``` - pripraví všetky JAR súbory na publikovanie do repozitárov.
- ```deployGithub``` - deploy SNAPSHOT verzie na [GitHub Packages](https://github.com/webjetcms/webjetcms/packages/2426502/versions).
- ```deployMavenCentral``` - deploy verzie na https://repo1.maven.org/maven2/com/webjetcms/webjetcms/, pred spustením je potrebné volať úlohu ```prepareAllJars```, vykonať v projekte z ```github```.

Postup vygenerovania novej verzie:

```shell
#nezabudni vypnut beziaci npm watch a Tomcat !!!
cd ant
ant deploy
```

*Poznámka*: v adresári ```build/updatezip``` vznikne rozbalená štruktúra, tú je možné zozipovať a použiť ako aktualizačný balík pre WebJET v starej štruktúre (nepoužívajúcej `jar` archívy).

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
