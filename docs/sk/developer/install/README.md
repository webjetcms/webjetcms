# Inštalácia a spustenie


<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [Inštalácia a spustenie](#inštalácia-a-spustenie)
  - [Webpack build JS, CSS a PUG súborov](#webpack-build-js-css-a-pug-súborov)
  - [Build Java tried a spustenie Tomcatu](#build-java-tried-a-spustenie-tomcatu)
  - [Nastavenie hosts súboru](#nastavenie-hosts-súboru)
  - [Testovanie](#testovanie)

<!-- /code_chunk_output -->


Ak ešte nemáte, nainštalujte si [VS Code s odporúčanými závislosťami a rozšíreniami](https://docs.webjetcms.sk/v8/#/install-config/vscode/setup) - postupujte iba po časť inštalácie rozšírení, následné kapitoly ```Pull``` projektu z SVN a nastavenie Tomcat-u sa vás už netýka.

V projekte používame [lombok](https://projectlombok.org), nainštalujte si rozšírenie do vášho vývojového prostredia - na web stránke kliknite na menu položku ```Install``` a v sekcii IDEs postupuje podla návodu.

## Webpack build JS, CSS a PUG súborov

Súbory pre administráciu sú zostavované cez webpack zo zdrojových ```js/scss/pug``` súborov. Pre prvotnú inštaláciu spustite v novom termináli:

```shell
cd src/main/webapp/admin/v9
npm install
npm run prod
```

kompletnú reinštaláciu vykonáte príkazom (pre node ```v17+``` je potrebny aj parameter ```--legacy-peer-deps```):

```shell
rm -rf node_modules
npm install
```

ten nainštaluje potrebné knižnice, licenciu na `Datatables Editor` a zostaví produkčnú verziu.

Následne môžete spustiť **dev režim**, pri ktorom automaticky **webpack sleduje zmeny** v ```js/scss/pug``` súboroch a builduje ```dist``` adresár:

```shell
cd src/main/webapp/admin/v9
npm run watch
```

ALEBO môžete využiť `gradle task`:

```shell
#kontinualny watch zmien v suboroch a buildovanie dist adresara
gradlew npmwatch
#alebo jednorazovy build
gradlew npmbuild
```

**Produkčnú verziu** vygenerujete cez:

```shell
cd src/main/webapp/admin/v9
npm run prod
```

## Build Java tried a spustenie Tomcatu

Kompilácia projektu:

```shell
gradlew compileJava - kompilacia projektu
```

vrátane refreshu dependencies (WebJETu z artifactory):

```shell
gradlew compileJava --refresh-dependencies --info
```

Spustenie / zastavenie Tomcatu, vytvorenie WAR archívu:

!>**Upozornenie:** pred spustením gradle appRun buildnite jednorázovo dist adresár HTML/CSS súborov cez príkaz gradle npmbuild, alebo majte v samostatnom termináli pustený z adresára src/main/webapp/admin/v9 príkaz npm run watch.

```shell
gradlew appRun
gradlew appStop
gradlew war
```

Zobrazenie všetkých závislostí JAR knižníc:

```shell
gradlew -q dependencies --configuration runtimeClasspath
```

Aktualizácia gradle wrapper

```shell
./gradlew wrapper --gradle-version 6.9.2
```

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/ZHb8714HXNY" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Nastavenie hosts súboru

WebJET je licencovaný podľa domén. Pre lokálnu prácu je potrebné do hosts súboru (na windows je to c:\windows\system32\drivers\etc\hosts) pridať riadok:

```
127.0.0.1   iwcm.interway.sk
```

!>**Upozornenie:** na `Windows` je potrebné súbor editovať s admin právami.

WebJET bude po spustení dostupný lokálne ako http://iwcm.interway.sk/admin/.

## Testovanie

Na testovanie sa používa [Playwright](https://github.com/microsoft/playwright/tree/master/docs) a [CodeceptJS](https://codecept.io/basics/).

Prvotná inštalácia:

```shell
cd src/test/webapp/
npm install
```

Spustenie všetkých testov:

```shell
cd src/test/webapp/
npx codeceptjs run --steps
```
