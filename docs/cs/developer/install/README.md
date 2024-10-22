# Instalace a uvedení do provozu

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Instalace a uvedení do provozu](#instalace-a-uvedení-do-provozu)
  - [Webpack sestavuje soubory JS, CSS a PUG](#Webpack-build-js-css-a-pug-soubory)
  - [Sestavení tříd jazyka Java a spuštění aplikace Tomcat](#sestavit-třídy-java-a-spustit-tomcat)
  - [Nastavení souboru hosts](#nastavení-souboru-hosts)
  - [Testování](#testování)

<!-- /code_chunk_output -->

Pokud ji ještě nemáte, nainstalujte si [Kód VS s doporučenými závislostmi a rozšířeními](https://docs.webjetcms.sk/v8/#/install-config/vscode/setup) - sledujte pouze instalační část rozšíření, následující kapitoly `Pull` projektu z SVN a nastavení Tomcatu se vás již netýká.

V projektu používáme [lombok](https://projectlombok.org), nainstalujte rozšíření do vývojového prostředí - na webové stránce klikněte na položku nabídky `Install` a postupujte podle pokynů v části IDE.

## Webpack sestavuje soubory JS, CSS a PUG

Soubory pro správu jsou kompilovány ze zdrojových kódů pomocí aplikace Webpack. `js/scss/pug` soubory. Počáteční instalaci spusťte v novém terminálu:

```shell
cd src/main/webapp/admin/v9
npm install
npm run prod
```

kompletní přeinstalace se provádí příkazem (pro uzel `v17+` je také zapotřebí parametr `--legacy-peer-deps`):

```shell
rm -rf node_modules
npm install
```

ten nainstaluje potřebné knihovny, licencuje `Datatables Editor` a zkompiluje produkční verzi.

`FontAwesome` licence je zakoupena a zajištěna skrytým souborem. [.npmrc](../../../src/main/webapp/admin/v9/.npmrc) obsahující ověřovací token.

Poté můžete začít **režim dev** čímž se automaticky **webpack sleduje změny** v `js/scss/pug` soubory a sestavy `dist` Adresář:

```shell
cd src/main/webapp/admin/v9
npm run watch
```

NEBO můžete použít `gradle task`:

```shell
#kontinualny watch zmien v suboroch a buildovanie dist adresara
gradlew npmwatch
#alebo jednorazovy build
gradlew npmbuild
```

**Výrobní verze** generujete prostřednictvím:

```shell
cd src/main/webapp/admin/v9
npm run prod
```

## Sestavení tříd jazyka Java a spuštění aplikace Tomcat

Sestavení projektu:

```shell
gradlew compileJava - kompilacia projektu
```

včetně obnovení závislostí (WebJET z artefaktu):

```shell
gradlew compileJava --refresh-dependencies --info
```

Spusťte/zastavte Tomcat, vytvořte archiv WAR:

**Varování:** před spuštěním gradle appRun jednou sestavte adresář dist se soubory HTML/CSS pomocí gradle npmbuild nebo spusťte npm run watch ze src/main/webapp/admin/v9 v samostatném terminálu.

```shell
gradlew appRun
gradlew appStop
gradlew war
```

Zobrazení všech závislostí knihovny JAR:

```shell
gradlew -q dependencies --configuration runtimeClasspath
```

Aktualizace gradle wrapper

```shell
./gradlew wrapper --gradle-version 6.9.2
```

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/ZHb8714HXNY" title="Přehrávač videí YouTube" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Nastavení souboru hosts

WebJET je licencován podle domény. Chcete-li pracovat lokálně, musíte přidat řádek do souboru hosts (ve Windows je to c:\windows\system32\drivers\etc\hosts):

```
127.0.0.1   iwcm.interway.sk
```

**Varování:** na adrese `Windows` musíte soubor upravit s právy správce.

WebJET bude po spuštění k dispozici lokálně jako http://iwcm.interway.sk/admin/.

## Testování

Pro testování se používá [Playwright](https://github.com/microsoft/playwright/tree/master/docs) a [CodeceptJS](https://codecept.io/basics/).

Počáteční instalace:

```shell
cd src/test/webapp/
npm install
```

Spusťte všechny testy:

```shell
cd src/test/webapp/
npx codeceptjs run --steps
```
