# Nasazení sestavení na serveru artifactory/maven

Nasazení archivů jar pro použití v klientských projektech je založeno na přípravě archivů jar kompatibilních s původní strukturou WebJET 8.

## Před vytvořením sestavení

Před vytvořením sestavení je třeba ručně provést/kontrolovat následující kroky:
- připravit popis změn v souboru `docs/CHANGELOG.md`
- upravit soubor `docs/README.md` - přidejte poslední verzi ze seznamu změn na začátek a odstraňte poslední verzi dole (README.md ukazuje posledních 5 verzí).
- upravit překladový klíč `admin.overview.changelog` se souhrnem změn aktuální verze zobrazeným pod uvítacím textem na domovské obrazovce.
- Upravit podle `src/main/webapp/admin/v9/json/wjnews.LANG.json` - přidat shrnutí a odkaz na seznam změn nejnovější verze.

Pokud se verze změní, aktualizujte ji v:
- `ant/build.xml`

odtud se také přenáší do `build.properties` pro zobrazení verze v administraci.

## Úloha ANT

[Build soubor](../../../../ant/build.xml) obsahuje několik `task` prvků, posledním prvkem je `deploy` který má nastavené správné závislosti, takže stačí spustit tento. Seznam `taskov`:
- `setup` - obnovuje závislosti a generuje `WAR` archiv
- `updatezip` - připravit dočasnou strukturu v `build/updatezip` adresář. Struktura obsahuje rozbalený `WAR` archiv, rozbaleno `webjet-XXXX.jar` soubory (tj. kompletní struktura adresářů /admin, /components a /WEB-INF/classes).
- `preparesrc` - stahuje se `SRC` jar a připraví strukturu archivu jar se zdrojovými soubory (propojenými z archivu jar a zdrojového kódu WebJET 2021).
- `define-artifact-properties` - definuje vlastnosti pro generování artefaktů, zde v části `artifact.version` nastaví verzi generovaného artefaktu
- `makejars` - připravuje archivy jar tříd, adresáře /admin a /components a zdrojové soubory.
- `download` - pomocná úloha, která stáhne sklenici archivů, které se nemění (`struts, daisydiff, jtidy, swagger`)
- `makepom` - generace `POM` soubor, který je generován úlohou gradle `writePom` na základě definovaných závislostí v `build.gradle`. Úloha zajišťuje, že správná verze je získána také z definic verzí typu `5.3.+` a odstranění závislostí na samotném WebJETu (které pocházejí ze závislostí na v8).
- `finalwar` - vytvoří ve složce `build/updatezip/finalwar` nová struktura se sestavenými třídami, včetně `AspectJ`. Vytváří archivy JAR `WEB-INF/lib/webjet-VERZIA.jar` s třídami Java, soubory JSP administrátorské části a aplikacemi ve formě `JarPackaging`.
- `prepareAllJars` - připraví všechny soubory JAR k publikování do úložišť.
- `deployStaging` - nasadit verzi na adrese https://repo1.maven.org/maven2/com/webjetcms/webjetcms/, před jejím spuštěním je třeba zavolat úlohu `prepareAllJars`, prováděné v rámci projektu od `github`.

Postup generování nové verze:

```shell
#nezabudni vypnut beziaci npm watch a Tomcat !!!
cd ant
ant deploy
```

*Poznámka*: v adresáři `build/updatezip` je vytvořena rozbalená struktura, lze ji zazipovat a použít jako aktualizační balíček pro WebJET ve staré struktuře (bez použití `jar` archivy).

## Kompletní kompilace zdrojového kódu

Ve WebJET 2021 je často upravována existující třída z verze 8, což může vést k nekompatibilitě tříd. Z tohoto důvodu existuje speciální úloha ANT `compile` v souboru `ant/compile.xml`. Použije původní zdrojový kód verze 8, rozbalí jej do dočasného adresáře, přidá zdrojový kód verze 2021 a poté vše zkompiluje, včetně použití funkce `AspectJ` kompilátor.

Vše se odehrává v adresáři `/build/updatezip`, je třeba před zahájením kompilace zavolat následující příkazy:

```sh
ant updatezip
ant download
```

Postup je následující:
- úkol `prepareSrc`
  - sloučí zdrojový kód aktuální verze a verze 8 aplikace `/src/main/java`, `/src/webjet8/java` a `/src/main/aspectj` na `src-utf8`
- úkol `delombok`
  - se provádí `delombok` - extrapolace anotací lomboku, zatímco `AspectJ` má problém s anotacemi lomboku
  - výsledek je v adresáři `/src-delombok/`
- úkol `compileMapstruct`
  - zkompilované pomocí standardního `javac` všechny třídy z adresáře `/src-delombok/` protože `AspectJ` nelze správně zkompilovat `mapstruct` třídy (vytvořit je `Impl` verze)
  - další jarní knihovny z adresáře `/ant/libs`, jedná se o třídy, které jsou ve starém kódu WebJET a jsou potřebné pro kompilaci, ale již nejsou potřeba pro spuštění WebJET CMS.
  - výsledek kompilace je v `/WebContent/WEB-INF/classes/`
  - do adresáře `/src-aspectj/` zdrojový kód je zkopírován, ale všechny třídy mapstruct jsou smazány (ty, které obsahují `mapper/mappers`)
  - z adresáře `/WebContent/WEB-INF/classes/` všechny zkompilované třídy jsou smazány s výjimkou `mapper` třídy
  - ze zbývajících tříd je vytvořen soubor JAR. `/WebContent/WEB-INF/generated-sources/mapper-impl.jar`
- úkol `compile`
  - který má být proveden `AspectJ` kompilace z adresáře `/src-aspectj/` do adresáře `/WebContent/WEB-INF/classes`
  - používá soubor JAR `/WebContent/WEB-INF/generated-sources/` se sestavenými `mapper` třídy
  - do výsledného adresáře `/WebContent/WEB-INF/classes` rozšiřuje obsah `mapper-impl.jar` se sestavenými `MapperImpl.class` tříd, aby bylo možné `classes` adresář obsahoval všechny třídy
  - na `classes` adresář kopíruje z `/src-delombok/` soubory typu `*.properties` a `*.xml`

Výsledkem je, že v adresáři `/WebContent/WEB-INF/classes` je zkompilován proti kompletnímu zdrojovému kódu WebJETu verze 8 a aktuální verzi.

Volání tohoto samostatného mravence `tasku` je integrován přímo do hlavního sestavení, kde je úloha `compile` volání tohoto samostatného úkolu. Při spuštění procesu nasazení tedy není nutné žádné další volání kompilace, vše se provede automaticky.

## Použití v klientských projektech

V klientských projektech stačí nastavit příslušnou verzi v souboru build.gradle:

```gradle
ext {
    webjetVersion = "2023.0-SNAPSHOT";
}
```

jsme experimentálně ověřili základní funkčnost na projektech s databázemi MariaDB, Microsoft SQL a Oracle DB.
