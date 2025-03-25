# Deployment buildu na artifactory/maven server

Deployment jaro archivů pro použití v klientských projektech je založeno na přípravě jaro archivů kompatibilních s původní strukturou WebJET 8.

## Před vytvořením buildu

Před vytvořením buildu je třeba manuálně provést/zkontrolovat následující kroky:
- připravit popis změn v souboru `docs/CHANGELOG.md`
- upravit soubor `docs/README.md` - přidat na vrch nejnovější verzi z changelogu a dole smazat poslední (v README.md se zobrazuje 5 posledních verzí)
- upravit překladový klíč `admin.overview.changelog` se sumářem změn aktuální verze zobrazený pod uvítacím textem na úvodní obrazovce
- upravit `src/main/webapp/admin/v9/json/wjnews.LANG.json` - doplnit sumář a odkaz na changelog nejnovější verze

Pokud se mění verze, aktualizujte ji v:
- `ant/build.xml`

odtud se přenese i do `build.properties` pro zobrazení verze v administraci.

## ANT task

[Build soubor](../../../../ant/build.xml) obsahuje více `task` elementů, finální je `deploy`, který má korektně nastavené závislosti, takže stačí spustit ten. Seznam `taskov`:
- `setup` - obnoví závislosti a vygeneruje `WAR` archiv
- `updatezip` - připraví dočasnou strukturu v `build/updatezip` adresáři. Struktura obsahuje rozbalený `WAR` archiv, rozbaleno `webjet-XXXX.jar` soubory (tj. kompletní strukturu adresářů /admin, /components a /WEB-INF/classes)
- `preparesrc` - stáhne `SRC` jaro soubor a připraví strukturu pro jaro archiv se zdrojovými soubory (spojeno z jaro archivu a zdrojového kódu WebJET 2021)
- `define-artifact-properties` - zadefinuje vlastnosti pro generování artifaktů, zde se v `artifact.version` nastavuje verze vygenerovaného artifaktu
- `makejars` - připraví jaro archivy tříd, /admin a /components adresářů a zdrojových souborů
- `download` - pomocný úkol ke stažení jaro archivů, které se nemodifikují (`struts, daisydiff, jtidy, swagger`)
- `makepom` - vygenerování `POM` souboru, ten se generuje gradle úkolem `writePom` na základě definovaných závislostí v `build.gradle`. Úloha zajišťuje získání správné verze iz definic verze typu `5.3.+` a odstranění závislostí na WebJETu samotném (který pochází ze závislostí na v8).
- `finalwar` - vytvoří ve složce `build/updatezip/finalwar` novou strukturu se zkompilovanými třídami včetně `AspectJ`. Vytvoří JAR archivy `WEB-INF/lib/webjet-VERZIA.jar` s Java třídami, JSP soubory admin části a aplikacemi ve formě `JarPackaging`.
- `prepareAllJars` - připraví všechny JAR soubory pro publikování do repozitářů.
- `deployStaging` - deploy verze na https://repo1.maven.org/maven2/com/webjetcms/webjetcms/, před spuštěním je třeba volat úlohu `prepareAllJars`, provést v projektu z `github`.

Postup vygenerování nové verze:

```shell
#nezabudni vypnut beziaci npm watch a Tomcat !!!
cd ant
ant deploy
```

*Poznámka*: v adresáři `build/updatezip` vznikne rozbalená struktura, tu lze sezipovat a použít jako aktualizační balíček pro WebJET ve staré struktuře (nepoužívající `jar` archivy).

## Kompletní zkompilování zdrojových kódů

Ve WebJET 2021 se často upravuje stávající třída z verze 8, což může vést k nekompatibilitě třídy. Z toho důvodu existuje speciální ANT task `compile` v souboru `ant/compile.xml`. Ten použije originální zdrojové kódy k verzi 8, rozbalí je do dočasného adresáře, doplní zdrojovými kódy z verze 2021 a následně vše zkompiluje včetně použití `AspectJ` kompilátoru.

Všechno se děje v adresáři `/build/updatezip`, před spuštěním kompilace je třeba zavolat následující příkazy:

```sh
ant updatezip
ant download
```

Postup je následující:
- task `prepareSrc`
  - spojí zdrojové kódy aktuální a verze 8 z `/src/main/java`, `/src/webjet8/java` a `/src/main/aspectj` do `src-utf8`
- task `delombok`
  - provede se `delombok` - extrapolování lombok anotací, neboť `AspectJ` má s lombok anotacemi problém
  - výsledek je v adresáři `/src-delombok/`
- task `compileMapstruct`
  - zkompilují se přes standardní `javac` všechny třídy z adresáře `/src-delombok/`, protože `AspectJ` neumí korektně zkompilovat `mapstruct` třídy (vytvořit je `Impl` verzi)
  - dodatečné jaro knihovny bere z adresáře `/ant/libs`, jedná se o třídy, které jsou ve starém kódu WebJETu a jsou potřebné ke kompilaci, ale již nejsou potřebné pro běh WebJET CMS
  - výsledek kompilace je v `/WebContent/WEB-INF/classes/`
  - do adresáře `/src-aspectj/` se zkopíruje zdrojový kód, ale vymažou se všechny mapstruct třídy (ty, které obsahují v názvu `mapper/mappers`)
  - z adresáře `/WebContent/WEB-INF/classes/` se smažou všechny zkompilované třídy kromě `mapper` tříd
  - ze tříd co zůstanou se vytvoří JAR soubor `/WebContent/WEB-INF/generated-sources/mapper-impl.jar`
- task `compile`
  - provede `AspectJ` kompilaci z adresáře `/src-aspectj/` do adresáře `/WebContent/WEB-INF/classes`
  - použije JAR soubor `/WebContent/WEB-INF/generated-sources/` se zkompilovanými `mapper` třídami
  - do výsledného adresáře `/WebContent/WEB-INF/classes` rozbalí obsah `mapper-impl.jar` se zkompilovanými `MapperImpl.class` třídami, aby `classes` adresář obsahoval všechny třídy
  - do `classes` adresáře zkopíruje z `/src-delombok/` soubory typu `*.properties` a `*.xml`

Výsledkem je, že v adresáři `/WebContent/WEB-INF/classes` je vše zkompilováno vůči kompletnímu zdrojovému kódu WebJET verze 8 i aktuální verze.

Volání tohoto samostatného ant `tasku` je začleněno přímo do hlavního buildu, kde existuje task `compile` volající tento samostatný task. Takže při spuštění deployment procesu není zapotřebí žádné dodatečné volání kompilace, vše proběhne automaticky.

## Použití v klientských projektech

V klientských projektech stačí nastavit příslušnou verzi v build.gradle:

```gradle
ext {
    webjetVersion = "2023.0-SNAPSHOT";
}
```

pokusně jsme ověřili základní funkčnost na projektech s MariaDB, Microsoft SQL i Oracle DB.
