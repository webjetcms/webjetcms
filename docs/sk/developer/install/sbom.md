# Generovanie SBOM

SBOM (*Software Bill of Materials*) je strojovo čitateľný zoznam softvérových komponentov a ich závislostí. Pre WebJET CMS ho môžete vygenerovať na požiadanie z Gradle závislostí a npm lockfile administrácie. Výstup obsahuje aj tranzitívne závislosti, teda knižnice, ktoré používajú priamo deklarované knižnice.

Generátor vytvára jeden spoločný súbor vo formáte **CycloneDX 1.6 JSON**. Slúži ako podklad pre evidenciu komponentov a CRA analýzu. Rozsah je obmedzený na tieto dva zdroje; pred odovzdaním SBOM celého produktu treba doplniť nižšie uvedené knižnice mimo správy balíkov.

## Spustenie

Potrebujete prostredie na zostavenie WebJET CMS: Java 17, Apache Ant, Node.js aspoň 20.18.0, npm aspoň 9 a Git. Ant spúšťa Gradle wrapper z repozitára.

Z koreňa repozitára spustite:

```shell
ant -f ant/build.xml sbom
```

Verzia sa automaticky prevezme z vlastnosti `artifact.version` definovanej v targete `define-artifact-properties` v `ant/build.xml`. Je teda rovnaká ako verzia používaná pri príprave distribučných artefaktov. Netreba ju zadávať ani udržiavať na ďalšom mieste.

Z priečinka `ant` stačí spustiť `ant sbom`. Vo Windows target zvolí `gradlew.bat`. Generovanie nespúšťa CMS, aktualizáciu `build.properties`, kompiláciu, testy ani zostavenie frontendových súborov.

Ak potrebujete jednorazovo iné označenie reportu, použite Ant property:

```shell
ant -f ant/build.xml -Dartifact.version=2026.0 sbom
```

Táto hodnota označuje report, neprepína Git vetvu ani verzie závislostí.

Pri prvom spustení je potrebný prístup k repozitárom závislostí, Gradle Plugin Portal a npm registry. Java časť používa prístupy k repozitárom nastavené v `build.gradle`, vrátane `GPR_USER` a `GPR_API_KEY` alebo príslušných Gradle properties pre WebJET AI. Prístupové údaje nepatria do SBOM.

Generátor používa [CycloneDX Gradle plugin](https://github.com/CycloneDX/cyclonedx-gradle-plugin) vo verzii `3.5.1` a [CycloneDX npm](https://github.com/CycloneDX/cyclonedx-node-npm) vo verzii `6.0.1`. Ant target načíta konfiguráciu `ant/sbom.init.gradle`, odovzdá jej verziu artefaktu a spustí Gradle úlohu `sbom`. Gradle plugin sa načíta iba pri použití tohto init skriptu. npm nástroj sa stiahne cez `npm exec` do npm cache; nemení `package.json`, `package-lock.json` ani neinštaluje závislosti administrácie.

Ak sú nástroje aj závislosti už v lokálnej cache, môžete použiť:

```shell
ant -f ant/build.xml -Dsbom.offline=true sbom
```

Voľba sa uplatní na Gradle aj získanie npm nástroja. Chýbajúci obsah cache spôsobí chybu.

## Výstup

Výsledný súbor určený na uloženie do repozitára je **`ant/sbom/webjetcms.cdx.json`**. Target `sbom` ho po úspešnom generovaní a validácii skopíruje z `build/reports/sbom/webjetcms.cdx.json`. Aktualizuje sa aj pri ručnom spustení `ant sbom`.

Obsahuje Java aj npm komponenty, pričom balík administrácie je pripojený ako závislosť WebJET CMS. Zachované sú aj vzájomné väzby tranzitívnych závislostí a metadáta oboch generátorov.

V priečinku `build/reports/sbom/` zostávajú pomocné exporty na kontrolu pôvodu údajov:

| Súbor | Obsah |
| --- | --- |
| `webjetcms-java.cdx.json` | Vyriešené Java závislosti z `distributionRuntimeClasspath`, ich verzie, vzájomné väzby, kontrolné súčty a dostupné metadáta. |
| `webjetcms-admin.cdx.json` | Závislosti administrácie z `src/main/webapp/admin/v9/package-lock.json` vrátane tranzitívnych, voliteľných a peer závislostí, bez `devDependencies`. |

Spoločný aj Java report používajú verziu WebJET CMS z Ant vlastnosti `artifact.version`. npm report zachováva identitu koreňového balíka z `package.json`; jeho `1.0.0` je verzia balíka administrácie, nie verzia CMS. Verzia CMS je tiež zapísaná v `metadata.properties` pod názvom `webjet:releaseVersion` v npm aj spoločnom reporte.

Skript kontroluje zhodu deklarácií závislostí v `package.json` a koreňovej časti lockfile. Pri chybe vyriešenia Java alebo npm závislostí skončí chybou. Úspešný záver `BUILD SUCCESSFUL` znamená, že oba pomocné aj spoločný report prešli validáciou schémy CycloneDX aj kontrolou jednoznačnosti identifikátorov a existencie odkazovaných komponentov. Spoločný súbor sa nahradí až po úspešnej validácii.

Spoločný report má v `compositions` hodnotu `aggregate: incomplete`, pretože inventár manifestov nepokrýva celý distribuovaný produkt.

Ďalšie spustenie súbory prepíše. Po neúspešnom spustení nepoužívajte obsah výstupného priečinka ako nový report: môže obsahovať starší alebo iba čiastočne obnovený výstup.

## Automatická aktualizácia pri Maven deploymente

Target `deployGithub` najskôr spustí `prepareAllJars`, následne `sbom` a až potom publikovanie do GitHub Packages. SBOM sa teda generuje po zostavení Maven artefaktov v tom istom pracovnom priečinku. Príprava aktualizačných ZIP súborov cez `createUpdateZip` tento krok nespúšťa.

Pri publikovaní sa rovnaký report priloží ako Maven artefakt s classifierom `cyclonedx` a typom `json`, teda `webjetcms-<verzia>-cyclonedx.json`. Chyba generovania, validácie, kopírovania alebo publikovania spôsobí zlyhanie targetu `deployGithub`.

Existujúci GitLab job `deploy-snapshot` môže ponechať svoje volania `ant deployGithub` a následne `ant gitPush`. Target `gitPush` zahrnie do existujúceho build commitu aj `ant/sbom/webjetcms.cdx.json` spolu so `src/main/webapp/WEB-INF/build.properties`. Samotné `ant sbom` ani `ant deployGithub` nevytvárajú Git commit.

Pri následnom ručnom spustení `ant -Dcompress=true deployMavenCentral` sa použije existujúci `ant/sbom/webjetcms.cdx.json` z predchádzajúceho `deployGithub` pre rovnakú verziu. SBOM sa znovu negeneruje. Po príprave JAR súborov sa skopíruje medzi artefakty a do publikačného ZIP-u sa pridá ako `webjetcms-<verzia>-cyclonedx.json` spolu s GPG podpisom a kontrolnými súčtami MD5 a SHA-1. Ak zdrojový SBOM chýba alebo podpisovanie zlyhá, publikovanie sa zastaví.

V každej vetve sa udržiava jeden súbor s posledným uloženým SBOM; staršie verzie zostávajú v Git histórii. Vo vetve `release/2026.18` bude report z generovania uloženého v danej vetve. Konkrétne vydanie identifikuje príslušný commit alebo release tag. Report obsahuje čas generovania a nové UUID, preto sa zmení aj pri opakovanom generovaní s rovnakými závislosťami.

## Rozsah a obmedzenia

- **Java distribúcia:** používa sa `distributionRuntimeClasspath`, takže report nezahŕňa vývojový `webjet-ai-local`, samostatné testovacie konfigurácie ani nástroje zostavenia. Konfigurácia obsahuje aj API poskytované kontajnerom, napríklad Servlet/JSP API. Tie `war` pri balení odpočíta cez `providedRuntime`, preto report nie je presným zoznamom súborov vo `WEB-INF/lib`. Závislosť deklarovaná v `implementation` zostane zahrnutá, aj keď jej názov obsahuje `test`.
- **npm pred zostavením:** zdrojom je lockfile, nie obsah `node_modules` ani hotových JS/CSS súborov. Zoznam môže obsahovať komponenty, ktoré sa do výsledného bundle nedostanú. Vylúčenie `devDependencies` vychádza z deklarácií projektu; nezisťuje, či sa časť niektorého vývojového balíka dostala do distribuovaných súborov.
- **Zmeny cez postinstall:** `postinstall.sh` inštaluje DataTables Editor z lokálneho ZIP-u. Lockfile eviduje npm balík `datatables.net-editor`, ktorého verzia a kontrolný súčet nemusia zodpovedať následne vloženému licencovanému kódu. Ten treba evidovať podľa skutočne dodaného ZIP-u.
- **Ručne vložené knižnice:** CKEditor, staršie JS knižnice v administrácii, knižnice komponentov a zákaznícke rozšírenia mimo uvedených manifestov vyžadujú samostatnú inventarizáciu. Rovnako treba overiť obsah shaded JAR súborov, natívnych knižníc a ďalších vložených balíkov.
- **Lokálne JAR súbory:** knižnice z `libs`, ktoré Gradle vyrieši ako závislosti, sú zahrnuté aj s kontrolnými súčtami. Ak nemajú Maven POM, môžu chýbať údaje o dodávateľovi, licencii alebo vnútorných závislostiach. Upozornenie `metadata is unavailable` neznamená, že sa komponent z reportu vynechal.
- **Prostredie nasadenia:** Tomcat, JDK, operačný systém, databázový server ani obsah kontajnerového obrazu sa týmito dvoma zdrojmi neinventarizujú.

SBOM je inventár, nie výsledok kontroly zraniteľností ani potvrdenie súladu s CRA. Dostupnosť licenčných údajov závisí od metadát jednotlivých balíkov.

## Použitie pre konkrétne vydanie

Generujte report pri zostavení vydania z rovnakého checkoutu a s rovnakým vyriešením závislostí. `build.gradle` obsahuje aj dynamické verzie, napríklad `7.0.+`, takže neskoršie spustenie na rovnakom commite môže vybrať novšiu verziu knižnice. Samotný parameter s číslom vydania pôvodné vyriešenie neobnoví.

Spoločný report aj pomocné exporty archivujte spolu s distribučným balíkom, jeho kontrolným súčtom, Git commitom a informáciou o prípadných lokálnych zmenách. Pre opakovateľné vydania je potrebné uchovať aj presné vyriešené verzie závislostí, napríklad pomocou Gradle dependency locking. Pred označením reportu za SBOM celej distribúcie doplňte inventár knižníc mimo Gradle a npm a porovnajte ho s dodávanými súbormi.
