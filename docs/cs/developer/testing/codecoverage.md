# Pokrytí kódem

> Měření pokrytí kódem testy vyjadřuje procento kódu, který je proveden během testovacích scénářů. Čím větší je procento pokrytí kódu testovacími scénáři, tím nižší je šance, že kód obsahuje nezjištěné softwarové chyby.

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/vJkto5AcQeA" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
</div>

WebJET obsahuje integrovanou knihovnu [jacoco](https://github.com/jacoco/jacoco), která během běhu sleduje, která část kódu je provedena a následně vygeneruje podrobný report v HTML formátu. V něm můžete procházet jednotlivými balíky až do úrovně java tříd a metod s podrobným zvýrazněním, které části kódu se během testů provedly a které ne.

Specifikum je sledování pokrytí kódu testy i **během standardního běhu aplikačního serveru**, pokrytí se tedy měří během běžné práce a tedy i **během provedení automatizovaných e2e testů**.

Pro část kódu, která se během testů neprovedla, doporučujeme přidat další testovací scénáře.

![](jacoco.png)

Můžete se podívat [naposledy vygenerovaný stav pokrytí testy](http://docs.webjetcms.sk/latest/codecoverage-report/index.html).

## Použití

Při standardním použití ve VS Code se automaticky po provedení úloh `appStart` vygeneruje report do `./build/jacoco/report/index.html`. Pokud se nevygeneruje (pokud např. úlohu zastavíte přes `CTRL+C`) spusťte `gradlew generateJacocoReport` pro vygenerování reportu. Pro roli `appStartDebug` je `jacoco` vypnuto, protože nefungoval HotSwap, můžete se ale rozhodnout, co je pro vás důležitější.

V terminálu se vám zobrazí odkaz na otevření reportu v prohlížeči. Můžete tak snadno již během psaní testů sledovat, jak se vám daří pokrýt testy kód příslušných služeb.

Připraven je také skript `./ant/codecoverage.sh` pro kompletní vygenerování reportu. Skript spustí jak standardní JUnit testy, tak automatizované e2e testy. Využitý je úkol `appBeforeIntegrationTest`, která nastartuje aplikační server na pozadí, aby mohly pokračovat automatizované testy. Aplikační server je zastaven pomocí úlohy `appAfterIntegrationTest`, která zároveň spustí generování HTML reportů.

```shell
#!/bin/sh

echo ">>>>>>>>>>> Compiling project"
cd ..
gradlew clean
gradlew compileJava
gradlew npminstall
gradlew npmbuild

echo ">>>>>>>>>>> Executing JUnit test"
gradlew test

echo ">>>>>>>>>>> Starting app server"
gradlew appBeforeIntegrationTest

echo ">>>>>>>>>>> Executing e2e/codeceptjs tests"
cd src/test/webapp
npm run singlethread
npm run parallel8
cd ../../..

sleep 30

echo ">>>>>>>>>>> Stopping app server"
gradlew appAfterIntegrationTest
```

Výsledkem je soubor `build/jacoco/test.exec` s výsledky JUnit testů a `build/jacoco/appBeforeIntegrationTest.exec` s výsledky e2e/codeceptjs testů. Report je vygenerován spojením výsledků ze všech `.exec` souborů ve složce `build/jacoco`.

## Detaily implementace

Rozšíření `jacoco` je přidáno v `build.gradle` a je nastaveno pro použití v testech, ale i při standardním spuštění aplikačního serveru. Mírně zvyšuje zátěž na procesor a paměť, v případě potřeby můžete rozšíření vypnout nastavením atributu `enabled` na `false` v `tasks.appStart` a `tasks.appStartDebug`.

Takové nastavení ale má tu výhodu, že během běžné práce vývojáře je průběžně generován report pokrytí kódu a lze jej zkontrolovat po každém zastavení aplikačního serveru pomocí nastavení `finalizedBy tasks.generateJacocoReport`. Aplikační server standardně zastavíte pomocí `gradlew appStop`. Pokud se aplikační server zastavíte například. pomocí `CTRL+C` můžete report vygenerovat manuálně voláním `gradlew generateJacocoReport`.

Použitá verze [jacoco](https://github.com/jacoco/jacoco/releases) se nastavuje v proměnné `toolVersion`, nastavením na hodnotu `+` se automaticky použije nejnovější dostupná verze.

```groovy
plugins {
    ...
    id 'jacoco'
}

jacoco {
    //set latest version
    toolVersion = "+"
}
gretty {
    ...
    afterEvaluate {
        tasks.appStart {
            file("${rootDir}/build/jacoco/appStart.exec").delete()
            jacoco {
                enabled = true
            }
            finalizedBy tasks.generateJacocoReport
        }
        tasks.appStartDebug {
            file("${rootDir}/build/jacoco/appStartDebug.exec").delete()
            jacoco {
                //enabled = true
            }
            //finalizedBy tasks.generateJacocoReport
        }
        tasks.appAfterIntegrationTest {
            finalizedBy tasks.generateJacocoReport
        }
    }
}

task('generateJacocoReport', type: JacocoReport) {

  executionData fileTree(project.rootDir.absolutePath).include("**/build/jacoco/*.exec")

  sourceDirectories.setFrom project.files(project.sourceSets.main.allSource.srcDirs)
  classDirectories.setFrom project.sourceSets.main.output

  def reportDir = project.reporting.file("${rootDir}/build/jacoco/report")
  reports {
    html.destination = reportDir
  }
  doLast {
    System.out.println "Jacoco report for server created: file://${reportDir.toURI().path}/index.html"
  }
}
```

Klíčové je nastavení `afterEvaluate` pro gretty. To aktivuje `jacoco` i pro úkoly `appStart` a případně i pro `appStartDebug`, protože [standardně je jacoco](https://gretty-gradle-plugin.github.io/gretty-doc/Code-coverage-support.html) aktivní pouze pro roli `appBeforeIntegrationTest`.

Role `generateJacocoReport` vygeneruje HTML report do složky `./build/jacoco/report`. V terminálu zobrazí odkaz na otevření souboru v prohlížeči:

```groovy
gradlew generateJacocoReport

> Task :generateJacocoReport
Jacoco report for server created: file:///Users/xxx/Documents/workspace/webjet/build/jacoco/report/index.html
```

v prohlížeči tedy stačí, když otevřete uvedený odkaz `file:///Users/xxx/Documents/workspace/webjet/build/jacoco/report/index.html` pro zobrazení vygenerovaného reportu.
