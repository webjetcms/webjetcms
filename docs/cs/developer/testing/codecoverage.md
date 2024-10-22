# Pokrytí kódu

> Měření pokrytí kódu testy vyjadřuje procento kódu, které se provede během testovacích scénářů. Čím vyšší je procento pokrytí kódu testovacími scénáři, tím nižší je pravděpodobnost, že kód obsahuje neodhalené softwarové chyby.

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/vJkto5AcQeA" title="Přehrávač videí YouTube" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
</div>

WebJET obsahuje integrovanou knihovnu [jacoco](https://github.com/jacoco/jacoco) který sleduje, která část kódu je během běhu spuštěna, a poté generuje podrobnou zprávu ve formátu HTML. V něm můžete procházet jednotlivé balíčky až na úroveň javových tříd a metod a podrobně zvýraznit, které části kódu byly během testů provedeny a které nikoli.

Specifičnost sleduje pokrytí kódu jak testy, tak i **během standardního běhu aplikačního serveru**, pokrytí se proto měří v běžné pracovní době, a proto se **při provádění automatizovaných testů e2e**.

Pro část kódu, která se během testů nespustila, doporučujeme přidat další testovací scénáře.

![](jacoco.png)

Můžete si prohlédnout [poslední vygenerovaný stav pokrytí testů](http://docs.webjetcms.sk/latest/codecoverage-report/index.html).

## Použití

Při standardním použití ve VS Code se automaticky provede po úlohách. `appStart` vygeneruje zprávu do `./build/jacoco/report/index.html`. Pokud se nevytvoří (např. pokud úlohu zastavíte pomocí funkce `CTRL+C`) začít `gradlew generateJacocoReport` pro vygenerování zprávy. Pro úlohu `appStartDebug` Je `jacoco` vypnuta, protože HotSwap nefungoval, ale můžete se rozhodnout, co je pro vás důležitější.

V terminálu se zobrazí odkaz pro otevření zprávy v prohlížeči. Tímto způsobem můžete snadno zjistit, jak dobře si vedete při pokrývání kódu příslušných služeb testy během psaní testů.

Připraven je také scénář `./ant/codecoverage.sh` pro kompletní vygenerování zprávy. Skript spouští standardní testy JUnit, ale také automatizované testy e2e. Úloha se používá `appBeforeIntegrationTest` který spustí aplikační server na pozadí, aby mohly pokračovat automatizované testy. Aplikační server se zastaví pomocí úlohy `appAfterIntegrationTest`, což rovněž vyvolá generování sestav HTML.

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

Výsledkem je sada `build/jacoco/test.exec` s výsledky testů JUnit a `build/jacoco/appBeforeIntegrationTest.exec` s výsledky testů e2e/codeceptjs. Zpráva je generována kombinací výsledků ze všech `.exec` soubory ve složce `build/jacoco`.

## Podrobnosti o provádění

Rozšíření `jacoco` se přidává do `build.gradle` a je nastaven pro použití v testech, ale také pro standardní spuštění aplikačního serveru. Mírně zvyšuje zatížení procesoru a paměti, v případě potřeby můžete rozšíření zakázat nastavením atributu `enabled` na adrese `false` v `tasks.appStart` a `tasks.appStartDebug`.

Takové nastavení má však tu výhodu, že během běžné práce vývojáře je průběžně generována zpráva o pokrytí kódu, kterou lze po každém zastavení aplikačního serveru zkontrolovat pomocí nastavení. `finalizedBy tasks.generateJacocoReport`. Chcete-li ve výchozím nastavení zastavit aplikační server, použijte `gradlew appStop`. Pokud aplikační server zastavíte např. pomocí `CTRL+C` můžete sestavu vygenerovat ručně voláním `gradlew generateJacocoReport`.

Použitá verze [jacoco](https://github.com/jacoco/jacoco/releases) je nastaven v proměnné `toolVersion`, nastavením na hodnotu `+` se automaticky použije nejnovější dostupná verze.

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

Klíčem je nastavení `afterEvaluate` pro gretty. Tím se aktivuje `jacoco` také pro úkoly `appStart` a případně také pro `appStartDebug` protože [standardem je jacoco](https://gretty-gradle-plugin.github.io/gretty-doc/Code-coverage-support.html) aktivní pouze pro daný úkol `appBeforeIntegrationTest`.

Úkol `generateJacocoReport` vygeneruje zprávu HTML do složky `./build/jacoco/report`. Terminál zobrazí odkaz pro otevření souboru v prohlížeči:

```groovy
gradlew generateJacocoReport

> Task :generateJacocoReport
Jacoco report for server created: file:///Users/xxx/Documents/workspace/webjet/build/jacoco/report/index.html
```

v prohlížeči stačí otevřít výše uvedený odkaz. `file:///Users/xxx/Documents/workspace/webjet/build/jacoco/report/index.html` zobrazit vygenerovanou zprávu.
