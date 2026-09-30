# Instalace a spuštění

Pokud ještě nemáte, nainstalujte si [VS Code s doporučenými závislostmi a rozšířeními](https://docs.webjetcms.sk/v8/#/install-config/vscode/setup) - postupujte pouze po část instalace rozšíření, následné kapitoly ```Pull``` projektu z SVN a nastavení Tomcat-u se vás už netýká.

V projektu používáme [lombok](https://projectlombok.org), nainstalujte si rozšíření do vašeho vývojového prostředí - na web stránce klikněte na menu položku ```Install``` av sekci IDEs postupuje podle návodu.

## rspack build JS, CSS a PUG souborů

Soubory pro administraci jsou sestavovány přes rspack ze zdrojových ```js/scss/pug``` souborů. Pro prvotní instalaci spusťte v novém terminálu:

```shell
cd src/main/webapp/admin/v9
npm install
npm run prod
```

kompletní reinstalaci provedete příkazem (pro node ```v17+``` je nutný i parametr ```--legacy-peer-deps```):

```shell
rm -rf node_modules
npm install
```

ten nainstaluje potřebné knihovny, licenci na `Datatables Editor` a sestaví produkční verzi.

Následně můžete spustit **dev režim**, při kterém automaticky **rspack sleduje změny** v ```js/scss/pug``` souborech a builduje ```dist``` adresář:

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

## Build Java tříd a spuštění Tomcat

Kompilace projektu:

```shell
gradlew compileJava - kompilacia projektu
```

včetně obnovení dependencies (WebJETu z artifactory):

```shell
gradlew compileJava --refresh-dependencies --info
```

Spuštění / zastavení Tomcat, vytvoření WAR archivu:

!>**Upozornění:** před spuštěním gradle appRun buildněte jednorázově dist adresář HTML/CSS souborů přes příkaz gradle `npmbuild`, nebo mějte v samostatném terminálu puštěný z adresáře src/main/webapp/admin/v9 příkaz npm run watch.

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

### Souběžné spuštění ve více worktree

Ve VS Code můžete pro každou worktree vybrat samostatný profil spuštění:

| Profil | HTTPS port | HTTP port | Debug port |
| --- | --- | --- | --- |
| `Debug Local DB` | 443 | 80 | 5005 |
| `Debug Local DB 8443` | 8443 | 8080 | 5006 |
| `Debug Local DB 9443` | 9443 | 9080 | 5007 |

V každé worktree spouštějte jeden server a vyberte odlišný profil. Pro profil `Debug Local DB 8443` otevřete administraci na adrese `https://localhost:8443/admin/`.

Porty můžete nastavit také přes Gradle parametry `-PhttpsPort`, `-PhttpPort` a `-PdebugPort`. Bez parametrů zůstávají výchozí hodnoty 443, 80 a 5005. Například:

```shell
webjetDbname=/poolman-local.xml ./gradlew appStartDebug -PhttpsPort=8443 -PhttpPort=8080 -PdebugPort=5006
```

Na macOS úloha VS Code `appStop` používá skript `./app-stop.sh`. Nejprve zavolá `./gradlew appStop`, který použije řídící port uložený v adresáři `build` dané worktree. Po úspěšném odeslání požadavku čeká nejvýše 15 sekund na ukončení serveru. Pokud Gradle selže nebo server zůstane běžet, skript pošle signál `TERM` pouze Java procesu označenému identifikátorem této worktree. Pokud se neukončí do dalších 10 sekund, použije `KILL`.

Úloha `appKill` (`./app-stop.sh --force`) rovnou použije `KILL` pro server dané worktree. K identifikaci procesu slouží JVM parametr přidaný při startu přes aktuální `build.gradle` ; ostatní worktree se nezastavují. Při zastavování není třeba znovu zadávat porty.

## Nastavení hosts souboru

WebJET je licencován podle domén. Pro lokální práci je třeba do hosts souboru (na windows je to c:\windows\system32\drivers\etc\hosts) přidat řádek:

```txt
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
