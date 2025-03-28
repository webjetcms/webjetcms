# Instalace a spuštění

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Instalace a spuštění](#instalace-a-spuštění)
  - [Webpack build JS, CSS a PUG souborů](#webpack-build-js-css-a-pug-souborů)
  - [Build Java tříd a spuštění Tomcatu](#build-java-tříd-a-spuštění-tomcatu)
  - [Nastavení hosts souboru](#nastavení-hosts-souboru)
  - [Testování](#testování)

<!-- /code_chunk_output -->

Pokud ještě nemáte, nainstalujte si [VS Code s doporučenými závislostmi a rozšířeními](https://docs.webjetcms.sk/v8/#/install-config/vscode/setup) - postupujte pouze po část instalace rozšíření, následné kapitoly `Pull` projektu z SVN a nastavení Tomcat-u se vás už netýká.

V projektu používáme [lombok](https://projectlombok.org), nainstalujte si rozšíření do vašeho vývojového prostředí - na web stránce klikněte na menu položku `Install` av sekci IDEs postupuje podle návodu.

## Webpack build JS, CSS a PUG souborů

Soubory pro administraci jsou sestavovány přes webpack ze zdrojových `js/scss/pug` souborů. Pro prvotní instalaci spusťte v novém terminálu:

```shell
cd src/main/webapp/admin/v9
npm install
npm run prod
```

kompletní reinstalaci provedete příkazem (pro node `v17+` je zapotřebí i parametr `--legacy-peer-deps`):

```shell
rm -rf node_modules
npm install
```

ten nainstaluje potřebné knihovny, licenci na `Datatables Editor` a sestaví produkční verzi.

Následně můžete spustit **dev režim**, při kterém automaticky **webpack sleduje změny** v `js/scss/pug` souborech a builduje `dist` adresář:

```shell
cd src/main/webapp/admin/v9
npm run watch
```

NEBO můžete využít `gradle task`:

```shell
#kontinualny watch zmien v suboroch a buildovanie dist adresara
gradlew npmwatch
#alebo jednorazovy build
gradlew npmbuild
```

**Produkční verzi** vygenerujete přes:

```shell
cd src/main/webapp/admin/v9
npm run prod
```

## Build Java tříd a spuštění Tomcatu

Kompilace projektu:

```shell
gradlew compileJava - kompilacia projektu
```

včetně refreshu dependencies (WebJET z artifactory):

```shell
gradlew compileJava --refresh-dependencies --info
```

Spuštění / zastavení Tomcatu, vytvoření WAR archivu:

!>**Upozornění:** před spuštěním gradle appRun buildněte jednorázově dist adresář HTML/CSS souborů přes příkaz gradle npmbuild, nebo mějte v samostatném terminálu puštěný z adresáře src/main/webapp/admin/v9 příkaz npm run watch.

```shell
gradlew appRun
gradlew appStop
gradlew war
```

Zobrazení všech závislostí JAR knihoven:

```shell
gradlew -q dependencies --configuration runtimeClasspath
```

Aktualizace gradle wrapper

```shell
./gradlew wrapper --gradle-version 6.9.2
```

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/ZHb8714HXNY" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Nastavení hosts souboru

WebJET je licencován podle domén. Pro lokální práci je třeba do hosts souboru (na windows je to c:\windows\system32\drivers\etc\hosts) přidat řádek:

```
127.0.0.1   iwcm.interway.sk
```

!>**Upozornění:** na `Windows` je třeba soubor editovat s admin právy.

WebJET bude po spuštění dostupný lokálně jako http://iwcm.interway.sk/admin/.

## Testování

K testování se používá [Playwright](https://github.com/microsoft/playwright/tree/master/docs) a [CodeceptJS](https://codecept.io/basics/).

Prvotní instalace:

```shell
cd src/test/webapp/
npm install
```

Spuštění všech testů:

```shell
cd src/test/webapp/
npx codeceptjs run --steps
```
