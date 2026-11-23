# Generování SBOM

SBOM (*Software Bill of Materials*) je strojově čitelný seznam softwarových komponent a jejich závislostí. Pro WebJET CMS jej můžete vygenerovat na požádání z Gradle závislostí a npm lockfile administrace. Výstup obsahuje také tranzitivní závislosti, tedy knihovny, které používají přímo deklarované knihovny.

Generátor vytváří jeden společný soubor ve formátu **CycloneDX 1.6 JSON**. Slouží jako podklad pro evidenci komponent a CRA analýzu. Rozsah je omezen na tyto dva zdroje; před odevzdáním SBOM celého produktu je třeba doplnit níže uvedené knihovny mimo správu balíků.

## Spuštění

Potřebujete prostředí pro sestavení WebJET CMS: Java 17, Apache Ant, Node.js alespoň 20.18.0, npm alespoň 9 a Git. Ant spouští Gradle wrapper z repozitáře.

Z kořene repozitáře spusťte:

```shell
ant -f ant/build.xml sbom
```

Verze se automaticky stahuje z vlastnosti `artifact.version` definované v targetu `define-artifact-properties` v `ant/build.xml`. Je tedy stejná jako verze používaná při přípravě distribučních artefaktů. Není třeba ji zadávat ani udržovat na dalším místě.

Ze složky `ant` stačí spustit `ant sbom`. Ve Windows target zvolí `gradlew.bat`. Generování nespouští CMS, aktualizaci `build.properties`, kompilaci, testy ani sestavení frontendových souborů.

Pokud potřebujete jednorázově jiné označení reportu, použijte Ant property:

```shell
ant -f ant/build.xml -Dartifact.version=2026.0 sbom
```

Tato hodnota označuje report, nepřepíná Git větev ani verze závislostí.

Při prvním spuštění je zapotřebí přístup k repozitářům závislostí, Gradle Plugin Portal a npm registru. Java část používá přístupy k repozitářům nastavené v `build.gradle`, včetně `GPR_USER` a `GPR_API_KEY` nebo příslušných Gradle properties pro WebJET AI. Přístupové údaje nepatří do SBOM.

Generátor používá [CycloneDX Gradle plugin](https://github.com/CycloneDX/cyclonedx-gradle-plugin) ve verzi `3.5.1` a [CycloneDX npm](https://github.com/CycloneDX/cyclonedx-node-npm). Ant target načte konfiguraci `ant/sbom.init.gradle`, předá její verzi artefaktu a spustí Gradle úlohu `sbom`. Gradle plugin se načte pouze při použití tohoto init skriptu. npm nástroj se stáhne přes `npm exec` do npm cache; nemění `package.json`, `package-lock.json` ani neinstaluje závislosti administrace.

Pokud jsou nástroje i závislosti již v lokální cache, můžete použít:

```shell
ant -f ant/build.xml -Dsbom.offline=true sbom
```

Volba se uplatní na Gradle i získání npm nástroje. Chybějící obsah cache způsobí chybu.

## Výstup

Výsledný soubor určený k uložení do repozitáře je **`ant/sbom/webjetcms.cdx.json`**. Target `sbom` jej po úspěšném generování a validaci zkopíruje z `build/reports/sbom/webjetcms.cdx.json`. Aktualizuje se také při ručním spuštění `ant sbom`.

Obsahuje Java i npm komponenty, přičemž balíček administrace je připojen jako závislost WebJET CMS. Zachovány jsou i vzájemné vazby tranzitivních závislostí a metadata obou generátorů.

Ve složce `build/reports/sbom/` zůstávají pomocné exporty pro kontrolu původu dat:

| Soubor | Obsah |
| --- | --- |
| `webjetcms-java.cdx.json` | Vyřešeno Java závislosti z `distributionRuntimeClasspath`, jejich verze, vzájemné vazby, kontrolní součty a dostupná metadata. |
| `webjetcms-admin.cdx.json` | Závislosti administrace z `src/main/webapp/admin/v9/package-lock.json` včetně tranzitivních, volitelných a peer závislostí, bez `devDependencies`. |

Společný i Java report používají verzi WebJET CMS z Ant vlastnosti `artifact.version`. npm report zachovává identitu kořenového balíku z `package.json` ; jeho `1.0.0` je verze sady administrace, nikoli verze CMS. Verze CMS je také zapsána v `metadata.properties` pod názvem `webjet:releaseVersion` v npm i společném reportu.

Skript kontroluje shodu deklarací závislostí v `package.json` a kořenové části lockfile. Při chybě vyřešení Java nebo npm závislostí skončí chybou. Úspěšný závěr `BUILD SUCCESSFUL` znamená, že oba pomocné i společný report prošly validací schématu CycloneDX i kontrolou jednoznačnosti identifikátorů a existence odkazovaných komponent. Společný soubor se nahradí až po úspěšné validaci.

Společný report má v `compositions` hodnotu `aggregate: incomplete`, protože inventář manifestů nepokrývá celý distribuovaný produkt.

Další spuštění soubory přepíše. Po neúspěšném spuštění nepoužívejte obsah výstupní složky jako nový report: může obsahovat starší nebo pouze částečně obnovený výstup.

## Automatická aktualizace při Maven deploymentu

Target `deployGithub` nejprve spustí `prepareAllJars`, následně `sbom` a teprve potom publikování do GitHub Packages. SBOM se tedy generuje po sestavení Maven artefaktů ve stejné pracovní složce. Příprava aktualizačních ZIP souborů přes `createUpdateZip` tento krok nespouští.

Při publikování se stejný report přiloží jako Maven artefakt s classifierem `cyclonedx` a typem `json`, tedy `webjetcms-<verzia>-cyclonedx.json`. Chyba generování, validace, kopírování nebo publikování způsobí selhání targetu `deployGithub`.

Stávající GitLab job `deploy-snapshot` může ponechat svá volání `ant deployGithub` a následně `ant gitPush`. Target `gitPush` zahrne do stávajícího build commitu i `ant/sbom/webjetcms.cdx.json` spolu se `src/main/webapp/WEB-INF/build.properties`. Samotné `ant sbom` ani `ant deployGithub` nevytvářejí Git commit.

Při následném ručním spuštění `ant -Dcompress=true deployMavenCentral` se použije existující `ant/sbom/webjetcms.cdx.json` z předchozího `deployGithub` pro stejnou verzi. SBOM se znovu negeneruje. Po přípravě JAR souborů se zkopíruje mezi artefakty a do publikačního ZIPu se přidá jako `webjetcms-<verzia>-cyclonedx.json` spolu s GPG podpisem a kontrolními součty MD5 a SHA-1. Pokud zdrojový SBOM chybí nebo podepisování selže, publikování se zastaví.

V každé větvi se udržuje jeden soubor s posledním uloženým SBOM; starší verze zůstávají v Git historii. Ve větvi `release/2026.18` bude report z generování uloženého v dané větvi. Konkrétní vydání identifikuje příslušný commit nebo release tag. Report obsahuje čas generování a nové UUID, proto se změní i při opakovaném generování se stejnými závislostmi.

## Rozsah a omezení

- **Java distribuce:** používá se `distributionRuntimeClasspath`, takže report nezahrnuje vývojový `webjet-ai-local`, samostatné testovací konfigurace ani nástroje sestavení. Konfigurace obsahuje také API poskytované kontejnerem, například Servlet/JSP API. Ty `war` při balení odečte přes `providedRuntime`, proto report není přesným seznamem souborů ve `WEB-INF/lib`. Závislost deklarovaná v `implementation` zůstane zahrnuta, i když její název obsahuje `test`.
- **npm před sestavením:** zdrojem je lockfile, ne obsah `node_modules` ani hotových JS/CSS souborů. Seznam může obsahovat komponenty, které se do výsledného bundle nedostanou. Vyloučení `devDependencies` vychází z deklarací projektu; nezjišťuje, zda se část některého vývojového balíčku dostala do distribuovaných souborů.
- **Změny přes postinstall:** `postinstall.sh` instaluje DataTables Editor z lokálního ZIPu. Lockfile eviduje npm balík `datatables.net-editor`, jehož verze a kontrolní součet nemusí odpovídat následně vloženému licencovanému kódu. Ten je třeba evidovat podle skutečně dodaného ZIPu.
- **Ručně vložené knihovny:** CKEditor, starší JS knihovny v administraci, knihovny komponent a zákaznická rozšíření mimo uvedených manifestů vyžadují samostatnou inventarizaci. Rovněž je třeba ověřit obsah shaded JAR souborů, nativních knihoven a dalších vložených balíků.
- **Lokální JAR soubory:** knihovny z `libs`, které Gradle vyřeší jako závislosti, jsou zahrnuty is kontrolními součty. Pokud nemají Maven POM, mohou chybět údaje o dodavateli, licenci nebo vnitřních závislostech. Upozornění `metadata is unavailable` neznamená, že se komponenta z reportu vynechal.
- **Prostředí nasazení:** Tomcat, JDK, operační systém, databázový server ani obsah kontejnerového obrazu se těmito dvěma zdroji neinventarizují.

SBOM je inventář, ne výsledek kontroly zranitelností ani potvrzení souladu s CRA. Dostupnost licenčních údajů závisí na metadatech jednotlivých balíků.

## Použití pro konkrétní vydání

Generujte report při sestavení vydání ze stejného checkoutu a se stejným vyřešením závislostí. `build.gradle` obsahuje také dynamické verze, například `7.0.+`, takže pozdější spuštění na stejném commitu může vybrat novější verzi knihovny. Samotný parametr s číslem vydání původní vyřešení neobnoví.

Společný report i pomocné exporty archivujte spolu s distribučním balíkem, jeho kontrolním součtem, Git commitem a informací o případných lokálních změnách. Pro opakovatelná vydání je třeba uchovat i přesně vyřešené verze závislostí, například pomocí Gradle dependency locking. Před označením reportu za SBOM celé distribuce doplňte inventář knihoven mimo Gradle a npm a porovnejte jej s dodávanými soubory.
