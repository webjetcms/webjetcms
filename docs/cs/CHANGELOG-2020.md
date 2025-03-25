# Changelog verze 2020

## 2020.53

> 2020.53 přináší kompatibilitu s použitím v projektech klientů. Balíček je dostupný na `artifactory/maven` serveru jako verze `2021.0-SNAPSHOT`. Při prvním spuštění přidává databázové sloupce do některých tabulek, změna je ale zpětně kompatibilní ai nadále je možno se vrátit k verzi WebJET 8.8.

V klientských projektech stačí nastavit příslušnou verzi v `build.gradle`:

```gradle
ext {
    webjetVersion = "2021.0-SNAPSHOT";
}
```

pokusně jsme ověřili základní funkčnost na projektech s MariaDB, Microsoft SQL i Oracle DB.

**Build Artifactory/Maven verze**

#52318 [Build soubor](../ant/build.xml) obsahuje více `task` elementů, finální je `deploy`, který má korektně nastavené závislosti, takže stačí spustit ten. Seznam `taskov`:
- `setup` - obnoví závislosti a vygeneruje `WAR` archiv
- `updatezip` - připraví dočasnou strukturu v `build/updatezip` adresáři. Struktura obsahuje rozbalený `WAR` archiv, rozbaleno `webjet-XXXX.jar` soubory (tj. kompletní strukturu adresářů /admin, /components a /WEB-INF/classes)
- `preparesrc` - stáhne `SRC` jaro soubor a připraví strukturu pro jaro archiv se zdrojovými soubory (spojeno z jaro archivu a zdrojového kódu WebJET 2021)
- `define-artifact-properties` - zadefinuje vlastnosti pro generování artifaktů, zde se v `artifact.version` nastavuje verze vygenerovaného artifaktu
- `makejars` - připraví jaro archivy tříd, /admin a /components adresářů a zdrojových souborů
- `download` - pomocný úkol ke stažení `pom` souborů a jaro archivů, které se nemodifikují (`struts, daisydiff, jtidy, swagger`)
- `deploy` - samotný uložení artifaktů na `artrifactory` server, obsahuje definici závislostí (generování `POM` souboru)

Postup vygenerování nové verze:

```shell
cd ant
ant deploy
```

*Poznámka*: v adresáři `build/updatezip` vznikne rozbalená struktura, tu lze sezipovat a použít jako aktualizační balíček pro WebJET ve staré struktuře (nepoužívající jaro archivy).

**JPA a Spring inicializace**

JPA a Spring inicializace je přesunuta do package `sk.iway.webjet.v9`. Z důvodu zpětné kompatibility jsme museli vyřešit problém s pojmenováním databázových tabulek `_adminlog_` a `_properties_` v Oracle DB. Tam se používá název `webjet_adminlog` a `webjet_properties`. Jméno tabulky je ale přímo v JPA entitách. Využili jsme možnosti použití interface `SessionCustomizer`, který je implementován ve třídě [JpaSessionCustomizer](../src/main/java/sk/iway/webjet/v9/JpaSessionCustomizer.java). Ten při inicializaci JPA prochází všechny nalezené JPA entity a pro uvedené databázové tabulky v případě Oracle instalace změní hodnotu anotace.

Pro JPA jsme přidali korektní nastavení typu databáze (předchozí verze měla nastavenou jen možnost MariaDB).

**Úvodní obrazovka**

#47428 Nařezaný design úvodní obrazovky, naplněné daty. Zatím jsou data vkládána přímo v [ThymeleafAdminController](../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java), později po finalizaci vlastností bude čteno ajaxem.

Obrazovka je nařezaná s využitím VUE komponentů. Využity jsou nové DTO objekty pro `DocDetails, Audit a User beany`.

Dostupný je JavaScript objekt `window.currentUser`. Přidány VUE filtry pro formátování data a času - `formatDate,formatDateTime,formatDateTimeSeconds,formatTime`.

**Jiné změny**

- [Skripty] opravený typ auditního záznamu z `CLIENT_SPECIFIC` na korektní `INSERT_SCRIPT`, přidaná metoda `toString()` na doc a group beanech pro lepší zaznačení zvoleného omezení na adresáře/stránky v auditním záznamu (zapíše se ID záznamu a cesta k adresáři/stránce).
- [WebJET] upravená inicializace pro vyhledání SpringConfig v package `sk.iway.webjet.v9`, automatické hledání překladových souborů a autoupdate také v `text-webjetXX.properties` a `autoupdate-webjetXX.xml` (přičemž webjetXX je hodnota z konfigurační proměnné `defaultSkin`).
- `autoupdate.xml` - zlepšená kompatibilita s WebJET 8, aby bylo možné spustit v projektu WebJET 2021 a zároveň se bez dopadů vrátit zpět k WebJET 8.8. Přidány jsou sloupce `id,update_date` do tabulky `_properties_` a sloupec `task_name` do tabulky `crontab`. Sloupce nekolidují se starou verzí a nebrání použití administrace ve verzi 8.8.

**Dokumentace**

- je nasazena na http://docs.webjetcms.sk/v2021/, zatím není vygenerováno boční menu, řešeno gradle úkolem `rsyncDocs`
- doplněná dokumentace k vkládání [HTML kódu bez escapingu](developer/frameworks/thymeleaf.md#základní-výpis-textu--atributu) v Thymeleaf
- doplněná dokumentace k [formátování data a času](developer/frameworks/vue.md#formátování-data-a-času) ve Vue

![meme](_media/meme/2020-53.jpg ":no-zoom")

## 2020.51

**Monitorování serveru - zaznamenané hodnoty**

Nová verze zobrazení zaznamenaných hodnot monitorování serveru. Zobrazuje tabulku i grafy, přičemž pro grafy používá agregaci dat podle rozsahu dní:
- 0-5 = přesná hodnota bez agregace
- 5-10 = agregace na 10 minutový interval
- 10-14 = agregace na 30 minutový interval
- 14-30 = agregace na hodinu
- 30-60 = agregace na 4 hodiny
- 60+ = agregace na 12 hodin

Pokud by se agregace neprovedla, padl by pravděpodobně prohlížeč na množství dat v grafu. Implementace je ve třídě [MonitoringAggregator](../src/main/java/sk/iway/iwcm/components/monitoring/MonitoringAggregator.java), zaznamenává se nejvyšší hodnota v daném časovém intervalu.

- #50053 [Grafy] - doplněna základní dokumentace ke knihovně [amcharts](developer/frameworks/amcharts.md).
- #50053 [Grafy] - v `app.js` doplněna asynchronní inicializace knihovny amcharts voláním `window.initAmcharts()`.

![meme](_media/meme/2020-51.jpg ":no-zoom")

## 2020.50

> 2020.50 přináší korektní generování menu položek administrace dle dostupnosti nové verze admin stránky. V menu tak plynule přecházíte mezi verzí 8 a 9. V případě potřeby se umíte přepnout na původní verzi admin stránky.
>
> Vylepšen je i editor stránek, automaticky nastavuje svoji výšku podle velikosti okna. Přibyly také ikony stavů adresářů a web stránek ve stromové struktuře.

**Web stránky**

- Smazán původní ukázkový kód editace stránky a adresáře, již se používají pouze datatabulková zobrazení.
- Doplněné stavové ikony ve stromové struktuře dle původního návrhu pro adresář (interní, nezobrazit v menu, zaheslovaný) a web stránku (vypnuté zobrazení, přesměrovaná, nevyhledatelná, zaheslovaná).
- Dialog editoru má automatický počítanou výšku podle velikosti okna (ale minimálně 300 bodů). Výše se přepočítává každé 3 sekundy.
- Doplněno je kliknutí na web stránku ve stromové struktuře. To je poměrně komplikované, jelikož editor je integrován s datatabulkou. Nejprve se tedy ověří, zda je v datatabulce zobrazen adresář ve kterém se stránka nachází. Pokud ne, odloží se hodnota `docId` do proměnné `docidAfterLoad`. Ta se kontroluje po načtení seznamu stránek adresáře, pokud je nastavena automaticky se vyvolá editace dané stránky.

**Generování menu**

- #51997 [Navigace] Upraveno generování menu položek. Ve verzi v8 i v9 se generují linky na již předělané stránky ve v9, takže lze transparentně přecházet mezi starým a novým WebJETem. Při požadavku na zobrazení staré v8 verze lze do URL adresy zadat `/admin/v8` což přepne generování menu do staré verze a lze zobrazit starou verzi administrace. Do v9 se dostanete zavoláním URL adresy `/admin/v9`. Pro přechod do v8 je i ikona v horní části stránky.

**Dokumentace**

Doplněna dokumentace k použití [jsTree](developer/jstree/README.md).

## 2020.49

**Import/Export:**

Implementovaný systém pro import a export dat mezi datatabulkami.

Export:
- Umožňuje export do **Excelu(xlsx), PDF a přímý tisk na tiskárnu**
- Souboru se nastaví jméno podle aktuálního `title` stránky a automaticky se doplní aktuální datum a čas.
- Exportu lze nastavit typ dat (**aktuální strana/všechny, filtrované/všechny/označené řádky, řazení, sloupce**).
- Při serverovém stránkování se nejprve provede volání REST služby, ze které se získá **maximálně 50 000 záznamů**. Při potřebě exportovat více záznamů použijte vícenásobné exportování s využitím filtrování.
- Zpracování serverových dat probíhá mimo datatabulky (kvůli výkonu), nově se vykreslují `datetime` sloupce a `radio button` (podporovány jsou `date-time`, `date`, `*-select`, `*-boolean`).
- Při exportu se v prvním řádku připraví seznam sloupců, **import následně není citlivý na pořadí sloupců**.
- Pro **výběrová pole** (select/číselníky) se **exportuje textová hodnota** a při importu se zpět rekonstruuje na ID. Umožňuje to mít **rozdílné ID navázaných záznamů** mezi prostředími (např. ID šablony pro web stránku), pokud se shoduje jméno, korektně se záznam spáruje. V exportu je následně i lidsky srozumitelný text namísto ID.

Import:
- Umožňuje **importovat data jako nová** (doplní se do databáze) nebo **párovat existující data podle zvoleného sloupce** (např. jméno, URL adresa a podobně). Při párování automaticky vyhledá nejprve záznam v databázi a následně jej aktualizuje. Pokud neexistuje, vytvoří nový záznam.
- **Importuje se z formátu xlsx**.
- Import se provádí **postupně v dávkách po 25 záznamech**, aby nebyl zatížen server.

**Datatables:**

- Upravené filtrování podle dat - do UI přidána možnost nastavení času, na backendu již není nutné nastavovat čas konce dne
- Přidány atributy (potřebujeme je pro volání REST služby pro export dat):
  - `DATA.jsonOptions` - nastavení z odpovědi REST služby pro vykreslení (potřebujeme pro export číselníkových data)
  - `DATA.urlLatest` - kompletní URL adresa posledního REST volání
  - `DATA.urlLatestParams` - všechny parametry posledního REST volání (aktuální stránka, velikost stránky, filtry)

## 2020.45

- #47293 [Mazání záznamů v databázi] Předělaná verze mazání dat z WJ8 do datatable. Využívá několik hacků, jako nastavení [externího filtru](developer/datatables/README.md#externí-filtr), vypnutí stránkování (option paging)

**Datatables:**

- Přidána možnost **nezobrazit filtr** v hlavičce tabulky pro sloupec nastavením atributu `filter=false` v anotaci
- Přidána možnost **vypnout stránkování** pomocí `options paging: false`, [dokumentace](developer/datatables/README.md#možnosti-konfigurace)

## 2020.44

> Verze 2020.44 přináší **novou dokumentaci** k [WebJET JavaScript funkcím](developer/frameworks/webjetjs.md), [Automatizovanému testování datatabulky](developer/testing/datatable.md), přidáváme příklad pro [Dynamické změnu hodnot ve výběrovém poli v Datatables Editoru](developer/datatables-editor/README.md#dynamická-změna-hodnot-ve-výběrovém-poli) a jiné menší změny.
>
> Do WebJET 2021 jsme přenesli aplikaci **Mazání cache objektů** a dokončili jsme aplikaci **Šablony**. V ní přibyl výběr JSP souboru šablony na serveru s dynamickým načtením na základě vybrané skupiny šablon a nastavení adresářů pro zobrazení šablony. Přidali jsme také [automatizované testy](../src/test/webapp/tests/components/templates.js).
>
> Aplikaci **Monitorování serveru/aktuální hodnoty** jsme vizuálně doladili (odsazení, zobrazení v mobilní verzi) a upravili jsme živá data v grafech. Nyní se korektně posouvají v reálném čase.
>
> Do datatable jsme přidali možnost [potvrzení provedené akce](developer/datatables/README.md#tlačítko-pro-provedení-serverové-akce) pro speciální tlačítka v datatabulce (jako např. smazat vše).
- [VS Code] upraveno [tasks.json](../.vscode/tasks.json) a [launch.json](../.vscode/launch.json) přidáním debug konfigurace `Debug`. Ta spustí aplikační server a následně se připojí v debug režimu. Bohužel kvůli [chybě v gretty](https://github.com/gretty-gradle-plugin/gretty/issues/166) je vypnutí řešeno `killnutím` procesu (dosud neotestovány na windows). Ve VS Code tedy přímo v záložce Run umíte spustit debug konfiguraci "Debug".
- [Layout] Vizuálně opravené zobrazení výběru domény v pravé horní části
- #47293 [toastr] Refaktorovaný kód používající přímo toastr na API volání [WJ.notify](developer/frameworks/webjetjs.md#notifikace)

**Datatables:**

- #47293 [Datatable] Upravená funkce `TABLE.executeAction` pro možnost potvrzení akce před provedením
- #45718 [Datatable Editor] Vypnuto odesílání editoru po stisknutí Enter nastavením konfigurace `onReturn: false`. Dělalo to problém ve filtru výběrových polí, kde po stisku Enter se nastavila hodnota a také se odeslal formulář.
- #47293 [Datatables] Upravený kód [DatatableRestControllerV2.action](../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) pro možnost spuštění akce i pro nezadanou entitu (null hodnota). Používá se pro akce typy "Smazat vše"

**Mazání Cache objektů:**

- #47293 Předělaná verze z WebJET 8 do datatable. Cache objekty ale nejsou Spring DATA repozitář. Implementován je vlastní [service](../src/main/java/sk/iway/iwcm/components/memory_cleanup/cache_objects/CacheObjectsService.java), který zajišťuje API pro přístup k Cache třídě a seznamu objektů.
- #47293 Ve třídě [CacheObjectsService](../src/main/java/sk/iway/iwcm/components/memory_cleanup/cache_objects/CacheObjectsService.java) jsou implementovány metody `deleteAllCacheBeans()` pro smazání celé cache včetně DocDB, GroupsDB a jiných objektů. Metoda `deletePictureCache()` implementuje smazání cache `/thumb` obrázků.

**Šablony:**

- #45718 [Šablony] přidána [REST služba /forward](../src/main/java/sk/iway/iwcm/components/templates/TemplatesController.java) pro výběr JSP šablony ze seznamu souborů v `/templates` adresáři
- #45718 [Šablony] přidána možnost nastavování adresářů, ve kterých se mají šablony zobrazovat

**Testování:**

- [Testování] Refaktorovaný kód automatického testu [DataTables.js](../src/test/webapp/pages/DataTables.js) z parametrického konstruktoru na `options` objekt. Doplněna možnost přidání kroků testu pro nový záznam, editaci, vyhledání i smazání záznamů. Upravené čekání z fixního `I.wait(1)` na `DTE.waitForLoader()`, doplněno `I.say` pro lepší orientaci v logu testu. Test vyhledávání doplněn o ověření nalezených záznamů.
- [Testování] Přidána šablona `default` pro možnost testování výběru JSP souborů šablon

**Dokumentace:**

- [Datatable] Doplněná dokumentace k [tlačítku pro provedení serverové akce](developer/datatables/README.md#tlačítko-pro-provedení-serverové-akce) (např. rotace obrázku, smazání všech záznamů)
- Doplněna dokumentace k použití [Gitlabu ve VS Code](developer/guildelines/gitlab.md#práce-ve-vs-code)
- Přidána dokumentace [WebJET JavaScript funkcí](developer/frameworks/webjetjs.md) pro zobrazení notifikace, potvrzení akce a dalších. Refaktorovaný jejich kód pro lepší použití.
- Doplněný seznam [speciálních funkcí](developer/testing/README.md) pro testování datatabulky
- Přidán popis použití a implementace [Automatizovaného testování datatabulky](developer/testing/datatable.md)
- #45718 přidán návod na [dynamickou změnu hodnot v select boxu](developer/datatables-editor/README.md#dynamická-změna-hodnot-ve-výběrovém-poli) v Datatable Editoru podle jiných polí

## 2020.43

- [Layout] Upraveno generování breadcrumb navigace - původní elementy se skládaly z názvu `pills-#tab[1]`, znak # je nyní z textu odstraněn, tedy nyní se generuje jako `pills-tab[0]`
- #47419 [Monitorování serveru] - upravené vizuální zobrazení (odsazení), propracovaný způsob zobrazení čar v grafu Volného místa na disku (sladění mezi hodnotami v GB a pozicemi grafu)
- #47419 [Monitorování serveru] - zvětšený čas zobrazený na grafu aktuálních hodnot na 10 minut, doplněné popisy, přesunuté nastavení intervalu do hlavičky, přesunuté překlady to `properties` souboru

## 2020.42

> Verze 2020.42 přináší zejména [automatický způsob auditování JPA entit](developer/backend/auditing.md). Jednoduchou anotací zabezpečíte vytvoření auditních záznamů při každé manipulaci s entitou. Auditní záznam obsahuje také seznam změněných atributů (ve formě stará hodnota -> nová hodnota), respektive seznam všech atributů při vytvoření a smazání entity.
>
> Rozšířili a zdokumentovali jsme možnosti JSON pole pro [přidávání adresářů a web stránek](developer/datatables-editor/field-json.md) v datatabulkách.
>
> Datatabulkám jsme přidali možnost nastavení [výchozího způsobu uspořádání](developer/datatables/README.md#uspořádání) a datumově-časovým polím jsme přidali i zobrazení vteřin.
>
> Do WebJETu jsme přidali knihovnu `oshi-core` pro získání zátěže CPU a amcharts pro zobrazení grafů v Monitorování serveru.
>
> Rozšířili jsme dokumentaci o sekci s popisem [Spring frameworku](developer/frameworks/spring.md), přidali jsme sekci [řešení problémů](developer/troubles/README.md), upravili [příklady](developer/frameworks/example.md) a doplnily dokumentaci k [testování](developer/testing/README.md).

- #46891 [Testování] - upravené stávající testy na nové vlastnosti a nastavení tabulek, v `audit-search.js` upravené fixní čekání na dynamické pomocí `DT.waitForLoader`, v `forms.js` předělané testování hledání podle dat
- #46891 [Datatable] - přidáno tlačítko pro znovu načtení (obnovu) dat tabulky ze serveru
- #46891 [Audit] - přidáno zobrazení sloupce s ID entity
- #46891 [Překladové klíče] - opraveno vyhledávání podle data změny
- #46891 [Auditing] - zapnuto auditování JPA entit `FormsEntity, GalleryDimension, GallleryEntity, InsertScriptBean, InsertScriptDocBean, InsertScriptGroupBean, TranslationkeyEntity, AuditNotifyEntity`
- #46891 [Auditing] - přejmenovaná tabulka `webjet_audit` na `webjet_adminlog` podle původního názvu v Oracle
- #46891 [Testování] - přidáno [rozšíření codeceptjs-chai](developer/testing/README.md#assert-knihovna) pro snadné psaní `assert` podmínek
- #46891 [Dokumentace] - přidaná dokumentace k [auditingu](developer/backend/auditing.md), [řešení problémů](developer/troubles/README.md) a doplněné informace k [testování](developer/testing/README.md#webjet-doplňkové-funkce)
- upravené nastavení v `build.gradle`, `options.encoding=utf-8`, opravená diakritika v `PathFilter`
- #46891 [Testování] - přidáno testování auditního záznamu do standardního volání `DataTables.baseTest('domainRedirectTable');`
- #46891 [Datatable] - přidána možnost nastavení [způsobu uspořádání](developer/datatables/README.md#uspořádání) při zobrazení stránky, nastavené uspořádání pro konfiguraci (dle data změny),
- #46891 [Datatable] - přidáno vteřiny k formátování data a času
- #46891 [Audit] - přidáno auditování exportu datatabulky

## 2020.41

- [Dokumentace] upravená dokumentace [Ukázkový kód](developer/frameworks/example.md) - PUG upravený pro použití anotací, doplněné příklady
- [Dokumentace] doplněné základní informace k [EclipseLink JPA, Spring Data a Spring REST](developer/frameworks/spring.md)

## 2020.40

- #47419 [Monitorování serveru] modul zobrazuje aktuální hodnoty monitorování serveru, včetně grafů CPU a paměti
- #47419 [Monitorování serveru] přidána knihovna [oshi-core](https://github.com/oshi/oshi) pro získání údajů zatížení CPU
- #47419 [Monitorování serveru] přidána knihovna [amcharts](https://www.amcharts.com/) pro zobrazování grafů, použita je komerční licence
- #46261 [Skripty] modul doplněn o možnost nastavení adresáře, nebo web stránky pro skript
- #46261 [DTED json field] refaktorovaný kód [datového pole JSON pro DTED](developer/datatables-editor/field-json.md). Doplněna možnost konverze JSON objektů a textu tlačítka, vytvořena dokumentace.
- #47293 [Datatable] přidána možnost zobrazit vlastní tlačítko pouze když je vybrán nějaký řádek pomocí kódu `$.fn.dataTable.Buttons.showIfRowSelected(this, dt);`, [dokumentace](developer/datatables/README.md)
- #47293 [Datatable] přidána podpora `range` i na číselné hodnoty, prefix je `range:`, funguje podobně jako prefix `daterange:`
- #47293 [Persistent cache] zobrazení záznamů přes datatable, Spring DATA

## 2020.39

> V tomto vydání jsme do datatabulek přidali možnosti **obnovení tabulky** po volání REST služby (např. ve web stránkách když se stránka přesune do jiného adresáře), možnost `fetchOnEdit` pro **obnovení editovaného záznamu** před zobrazením editoru a `fetchOnCreate` pro **získání dat pro nový záznam** (např. pro web stránku nastavit šablonu, pořadí uspořádání).
>
> Do Java anotací jsme přidali možnost **skrytí sloupce** v tabulce nebo v editoru pomocí parametrů `hidden` nebo `hiddenEditor`.
>
> Přidali jsme také **editování JSON objektů**, zatím je napojeno na editaci adresáře a seznam adresářů (např. pro web stránku jako `GroupDetails` objekty). Prováděno to je jako VUE komponenta.

- #47341 - vše níže se týká branche/tiketu #47341
- [Web stránky] Zfunkčněno nastavování adresáře a kopie stránky v adresářích. Řešeno pomocí VUE komponenty [vue-folder-tree.vue](../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/folder-tree/vue-folder-tree.vue). Využívá se JSTree otevřené do celo obrazovkového dialogu. V anotaci `@DatatableColumn` se zapisuje jako `DataTableColumnType.JSON` kde pomocí atributu `className = "dt-tree-group"` se nastaví výběr jednoho adresáře nebo pomocí `className = "dt-tree-group-array"` výběr více adresářů. Objekty jsou typu `GroupDetails`.
- [Web stránky] - vyřešen přesun stránky do jiného adresáře s vyvoláním obnovení datatabulky a stromové struktury stránek
- [Web stránky] - doplněno mazání web stránek
- [Datatables] Doplněna možnost [aktualizace datatabulky](developer/datatables/restcontroller.md#reload-datatabulky) po uložení dat (např. když se web stránka přesune do jiného adresáře). Přidaná metoda `setForceReload(boolean)` do REST controlleru a JavaScript události `WJ.DTE.forceReload`.
- [Datatables] Přidána možnost nezobrazení sloupce v datatabulce nebo v editoru parametrem `hidden` (pro nezobrazení v datatabulce) nebo `hiddenEditor` (pro nezobrazení v editoru) anotace [@DatatableColumn](developer/datatables-editor/datatable-columns.md#vlastnosti-datatablecolumn). Na rozdíl od parametru `visible` není možné takové sloupce zobrazit ani přes nastavení datatabulky na frontendu.
- [Datatables] Přidán inicializační parametr `fetchOnEdit` - po nastavení na true bude před editací záznamu provedeno REST volání pro získání aktuálních dat editovaného záznamu. Při použití datatabulky např. pro web stránky se před otevřením editoru aktualizuje daný záznam ze serveru a do editoru se tedy otevře vždy nejnovější verze. Implementováno přes JS funkci `refreshRow` a zákaznické tlačítko `$.fn.dataTable.ext.buttons.editRefresh` kterým se nahradí standardní tlačítko `edit`.
- [Datatables] Přidán inicializační parametr `fetchOnCreate` - po nastavení na `true` bude před vytvořením nového záznamu provedeno REST volání s hodnotou -1 pro získání dat nového objektu. Výhodné to je pro nastavení výchozích údajů pro nový záznam. Např. pro web stránku se před nastaví adresář, pořadí uspořádání, šablona a podobně.
- [Datatables] Přidáno automatické zobrazení ID záznamu v prvním sloupci, kde dosud byla pouze výběrová pole pro označení řádku
- [Datatables] Upraveno zobrazení chybového hlášení ze serveru tak, aby nebylo mezi tlačítky Zrušit a Uložit ale nalevo od nich.
- [Testování] přidána funkce `DT.waitForLoader`, která čeká na zobrazení a následné schování informace "Zpracovávám" v datatabulce. Používá se jako `DT.waitForLoader("#forms-list_processing");`

## 2020.36

- #44968 [Překladové klíče] Upraveno ukládání překladu po změně jazyka - vytvoří se kopie překladového klíče, přidáno ukládání data poslední změny, opraveny testy
- #44968 [Datatables] Přidána možnost nezobrazit sloupec v datatabulce pomocí anotace `hidden=true`, takový sloupec na rozdíl od `visible` nelze ani v nastavení sloupců datatabulky zobrazit. Může ale nadále být použit v editoru, pokud má anotaci pro editor.

## 2020.34

- #47008 [Datatables] Přidáno automatické generování `required` atributu v `columns` definici pokud má pole anotaci `@NotEmpty` nebo `@NotBlank`. Prováděno v [DatatableColumnEditor.java](../src/main/java/sk/iway/iwcm/system/datatable/json/DataTableColumnEditor.java).
- #47008 [Automatizované testování] Přidán [automatizovaný test](../src/test/webapp/pages/DataTables.js) datatabulky se čtením definice přímo z `columns` objektu. Realizuje standardní CRUD test. Ukázkové použití v [redirects.js](../src/test/webapp/tests/components/redirects.js):

```javascript
const requiredFields = ['oldUrl', 'newUrl'];
DataTables.baseTest('redirectTable', requiredFields);
```

## 2020.33

- #47341 [Datatables] - Přidán nový typ pole JSON, zatím se zobrazuje jako `textarea` ale při čtení a zapisování se data posílají korektně jako JSON objekty. Implementováno jako nový field typ json v index.js v kódu `$.fn.dataTable.Editor.fieldTypes.json`
- #47425 [Datatables] - Opraveno hromadné označení řádků a volání smazání na serveru
- #44890 [Testy] - [přidán návod](developer/testing/README.md#čekání-na-dokončení) k použití `I.waitFor*` místo fixního `I.wait`. Upravený test [webpages.js](../src/test/webapp/tests/components/webpages.js) k použití `waitFor` a `within`, protože stránka obsahuje dvě datatabulky.
- #44890 [Datatables Editor] - Přidán přenos CSS třídy z atributu `className` anotace na celý řádek v editoru div.DTE\_Field. Viz komentář `//prenesieme hodnotu className aj do DT editora do prislusneho riadku` v [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js).
- #44890 [Datatables] - Upraveno srovnání `true/false` hodnot pro výběrové pole. Podobně jako pro zaškrtávací pole se musí porovnávat neřetězcová hodnota `true` (viz komentář `ak mame true/false select box, aj tomu musime zmenit value na true/false namiesto String hodnot` v [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)).

## 2020.32

- #44890 [Datatables] - Upravená funkčnost `BOOLEAN`, `dt-format-boolean-true`, v JSON objektu je hodnota přenášena jako `true` ale v `options` objektu jako `"true"` (řetězec). Nefungovalo tedy srovnání a nastavení hodnoty. Upravený kód tak, že se `editor.options` změní z `"true" na true` hodnotu. Následně již funguje srovnání s objektem a správné nastavení `checkboxu`. Upraveno také filtrování při klientském stránkování, kde se nesrovnává label ale value hodnota (viz komentář `//neplati pre column-type-boolean` v [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)).
- #44890 [Datatables] - Doplněný přenos data atributů i na pole typu `checkbox` a `radio` (podmínka `"div.DTE_Field_Type_"+data.editor.type`). Nastaví se na první element v seznamu. Umožňují nastavovat atributy jako `data-dt-field-hr` nebo `data-dt-field-headline`.
- #44890 [Datum a čas] - Upravené chování hodnoty 0 v datu ve formátu primitivního typu `long`. Pro hodnotu 0 se zobrazí prázdné datum a čas místo 1.1.1970. Upraveno v `renderDate` v [datatables-config.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-config.js).
- #44890 [Datatables] - Upraveno generování `Options` polí pro editor pomocí [LabelValue](../src/main/java/sk/iway/iwcm/system/datatable/json/LabelValue.java) objektu.

## 2020.31

- #44890 [Datatables] - Přidána možnost v anotaci nastavit uspořádání polí ve výstupním JSON objektu pomocí atributu `sortAfter` v anotaci `@DatatableColumn`. Sortování se provádí ve třídě [DatatableColumnsFactory.java](../src/main/java/sk/iway/iwcm/system/datatable/DataTableColumnsFactory.java) v metodě `sortColumns`.
- #44890 [Datatables] - Přidáno možnosti anotace `DataTableColumnType.BOOLEAN` pro true/false hodnoty a `DataTableColumnType.CHECKBOX` pro standardní zaškrtávací pole.
- #44890 [Datatables] - Přidána možnost anotací [vnořených objektů](developer/datatables-editor/datatable-columns.md#vnořené-atributy) pomocí `@DatatableColumnNested`. Atributem prefix (výchozí hodnota je auto, což vygeneruje prefix podle jména anotovaného fieldu) lze určit prefix atributu ve vygenerovaném JSON objektu (např. nastavením na hodnotu `editorFields` se do JSONu vygeneruje pole `allowChangeUrl` jak `editorFields.allowChangeUrl`). Prefix může být i prázdný, tehdy se vygeneruje pouze jméno proměnné (pokud má např. proměnnou `gettery/settery` v původním objektu jako jsou [FieldsFromAtoE](../src/main/java/sk/iway/iwcm/doc/FieldsFromAtoE.java) v [DocDetails](../src/main/java/sk/iway/iwcm/doc/DocDetails.java) objektu.)

## 2020.30

- #44890 [Datatables] - Vyřešeno vkládání více datatables do web stránky. Každé tabulce se nastaví HTML atribut ID i když není v `options` (výchozí datatableInit). ID atribut se následně vkládá i do dalších selektorů pro správné mapování filtrů a tlačítek na správný datatable. Upravené funkce `$.fn.dataTable.Editor.display.bootstrap.init` a `$.fn.dataTable.Editor.display.bootstrap.open` pro podporu více DT editorů ve stránce.

## 2020.29

- #346264 [Web stránky] Přidána možnost přidávat/editovat adresáře ve stromové struktuře web stránek. Implementováno pomocí schované datatabulky a DT editoru.
- #45763 [Stromová struktura] Přidáno API na [editaci stromové struktury](developer/jstree/README.md), použity v sekci web stránky a foto galerie. Je již možné přesouvat položky v adresářové struktuře webových stránek a přesouvat adresáře v galerii.
- #44836 [Web stránky] Doplněná pole do DT editoru, backend je zatím nezpracovává
- #47125 [Úkoly na pozadí] Předěláno do datatables, přidán sloupec se jménem úlohy do databáze

## 2020.28

- #45532 [Formuláře] Přidán automatizovaný vyhledávání
- #45409 [Audit] Přidány testy filtrování
- [Datatables] pro zaškrtávací pole automaticky nastavený atribut `falseValue` na ne (není-li zadán)
- Opraveno nastavení diakritiky pro JS soubory z `/admin/v9/dist/` adresáře (na výstup posílané v UTF-8)

## 2020.27

- #46264 [standalone datatables editor] Přidána možnost označit datatabulku jako schovanou ([atribut hideTable v options](developer/datatables/README.md)) pro možnost `standalone` DT editoru pro jstree
- #45721 [Skupiny šablon] Upraveno zobrazení v datatabulce pro použití anotací, přidán test

## 2020.26

- #43588 [Datatables] Opraveno opakované nastavení select boxu ve filtru a v editoru (po druhém nastavení nešli vybrat hodnoty)
- #45532 [Formuláře] Přidána možnost základního vyhledávání v datech formulářů, limitované na maximálně 6 sloupců (z důvodu API Spring DATA)
- #45718 [Šablony] Verze seznamu šablon přes datatabulku, využívá `TemplatesDB` API
- #45490 [Audit] Zobrazení záznamů auditu a notifikací. Obě přes Spring DATA, pro audit doplněné select boxy pro filtrování v datatabulce (napojeno na options editoru), doplněna možnost [custom filtrování](./datatables/restcontroller.md#speciální-vyhledávání) v REST controlleru.
- #44968 [Překladové klíče] Zobrazení a editace překladových klíčů, implementovaná četba původních hodnot, mapování `.properties` a DB změn.

## 2020.25

- #45532 [Formuláře] Zobrazení seznamu formulářů a dat formulářů (dynamická data) přes datatabulku.
- #45679 [Datatables] Anotace pro generování `columns` konfigurace přenesené do WebJET 8 pro možnost anotace stávajících beanů
- #45382 [Datatables] Import a export dat - úprava dialogu importu dat

## 2020.23

- #45685 [CI-CD] Nakonfigurována CI-CD pipeline na Gitlab serveru, noční build z master branche je dostupný na http://demotest.webjetcms.sk/admin/
- #45382 [Datatables] Iniciální verze importu dat formou postupného posílání dat z importního excelu do datatables editoru, možnost nastavení režimu importu (zatím chybí podpora na backendu)
- #45379 [Datatables] Refaktorovaný kód přizpůsobení datatabulky z `app.js` do samostatného souboru [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) s možností použití i jako [NPM modulu](../src/main/webapp/admin/v9/npm_packages/README.md) (např. na frontendu v šabloně). Refaktorovaný kód pro možnost použití více datatabulek na jedné stránce.
- #45679 [Datatables] Přidána možnost [generovat columns konfiguraci](developer/datatables-editor/datatable-columns.md) datatabulky pomocí anotací Java beanů

## 2020.22

- #44836 [Web stránky] Zmigrovaný kód úprav v ckeditoru z WJ8. Upraveny jsou vkládání obrázků, formuláře a další CK editor dialogy. Doplněný skrytý formulář obsahující `docid, groupid, virtualpath` pro kompatibilitu náhledu aplikace a dialogů z WJ8.
- #44836 [Web stránky] Pro editor se v `options` pošle `original` template objekt, který je potřebný k získání cest k CSS stylům. Při otevření DT editoru jsou zachovány CSS začínající na /admin (pro CK editor rozšíření), zbylé se změní na základě přiřazené šablony.
- #44836 [Web stránky] DT editor má dostupný přímo [JSON objekt](./datatables-editor/README.md#ukázky-kódu) editovaného záznamu (ne všech dat). Pro `options` číselníky doplněna možnost posílání [original objektu](./datatables/restcontroller.md). JSON objekt získaný ze serveru (obsahující kompletní data všech řádků a options property) je dostupný v DT jako DATA.json.
- #44737 [Konfigurace] Doplněn [autotest](../src/test/webapp/tests/components/configuration.js).
- #44293 [Doménové přesměrování] Doplněn [autotest](../src/test/webapp/tests/components/domain-redirects.js).

## 2020.21

- [Testování] Přidáno CSS třídy `dt-filter-MENO` na filtr polích v datatabulce pro možnost nastavení filtrování v automatizovaném testu `I.fillField("input.dt-filter-newUrl", randomNumber);`. Příklad v testu [redirects.js](../src/test/webapp/tests/components/redirects.js)
- [Přesměrování] Doplněno [anotace povinných polí](./datatables/restcontroller.md#valid%c3%a1cia--povinn%c3%a9-pole) na UrlRedirectBean.java (ve WJ8)
- [Galerie] Doplněny překlady textů pro editor obrázků a oblast zájmu, přidána možnost nastavení oblasti zájmu do vstupních polí, přidán [auto test](../src/test/webapp/tests/components/gallery.js)
- [Dokumentace] Přidán [ukázkový kód](developer/frameworks/example.md) využití frameworků
- [Testování] Přidáno [generování reportů](./testing/README.md#generování-html-reportu) přes mochawesome a CodeceptUI pro [ovládání testů](./testing/README.md#codecept-ui) přes prohlížeč
- [Datatable] Přidána možnost zobrazení dat (sloupců a polí v editoru) [na základě práv](./datatables/README.md#zobrazení-dat-na-základě-práv) přihlášeného uživatele

## 2020.20

- [Datatable] Přidáno [vyhledávání podle rozsahu dat](./datatables/restcontroller.md#vyh%c4%bead%c3%a1vání-pod%c4%bea-rozsahu-d%c3%a1tumů) specifickým zpracováním URL parametru s prefixem `daterange:od-do`.
- [Dokumentace] Doplněný manuál použití [assert](./testing/README.md#assert-knize%c5%benice) knihovny, použití a psaní nových [překladových klíčů](./frameworks/thymeleaf.md#překladů%c3%bd-text)
- [Galerie] Upraveno zobrazování sloupců jazykových mutací obrázku na využití API datatables (aby korektně fungovala i inline editace)
- [Překladové klíče] Nová verze zobrazení a editace [překladových klíčů](../src/main/webapp/admin/v9/views/pages/settings/translation-keys.pug)
- [Datatable] Refaktorovaný kód [DatatableRestControllerV2.java](../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) s komentovaným rozdělením na entitní a restové metody
- [Konfigurace] Nová verze zobrazení [konfigurace](../src/main/webapp/admin/v9/views/pages/settings/configuration.pug) WebJETu
- [Dokumentace] Doplněná dokumentace k [SFC Vue](developer/frameworks/vue.md)
- [Fotogalerie] Doplněné napojení [editoru obrázků](../src/main/webapp/admin/v9/src/js/image-editor.js) na `chunked upload` a toastr oznámení
- [Přesměrování] Nová verze zobrazení [přesměrování](../src/main/webapp/admin/v9/views/pages/settings/redirect.pug)
- [Fotogalerie] Pozměněný doplněk JCrop pro označení oblasti zájmu na `vue-advanced-cropper`

## 2020.19

- [Testování] Základní koncept [automatizovaného testování](developer/testing/README.md)
- [Dokumentace] Rozšíření [dokumentace](README.md)
- [Web stránky] Iniciální verze integrace [ckeditora do datatabulky](../src/main/webapp/admin/v9/src/js/datatables-ckeditor.js) a zobrazení [web stránek](../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug)
- [Datatable] Opraven problém s formátem dat, automatické nastavení podle `window.userLang` na moment knihovně - nastavení `wireformat` (výchozí x) a formát atributů v editoru (výchozí L HH:mm)
- [Galerie] Doplněna možnost přidat a [upravit adresář](../src/main/webapp/admin/v9/views/partials/folder-tree-galleria.pug), napojeno na Vue

## 2020.18

- [Doménové přesměrování] Nová verze zobrazení [doménových přesměrování](../src/main/webapp/admin/v9/views/pages/settings/domain-redirect.pug)
- [Bezpečnost] Bezpečnost volání všech rest služeb je zajištěna [automatickým nastavením](../src/main/webapp/admin/v9/views/partials/head.pug) CSRF tokenu pro všechny ajax požadavky
- [Upload] Vyřešeno volání korektního zpracování souborů po jejich nahrání a API pro smazání souboru (vyčištění galerie, video náhledů, fulltext indexu)
- [Galerie] Přidán [editor obrázků](../src/main/webapp/admin/v9/src/js/image-editor.js)
- [Web stránky] Základní implementace zobrazení [stromové struktury](../src/main/java/sk/iway/iwcm/admin/layout/JsTreeItem.java) stránek

## 2020.13

[#43588 - wj9 - zfunkčnění základního rozložení] - úloha #5 =>
- upravená lombook konfigurace (v build.gradle - `config['lombok.accessors.chain'] = 'true'`) aby `settre` vraceli `this` pro řetězení
- webpack nastaven tak, aby neodstraňoval uvozovky v atributech (neboť TH může vrátit více slovní výraz - `removeAttributeQuotes: false`)
- MENU - doplněné objekty MenuBean a MenuService které generují menu, je proveden překlad původních ModuleInfo z `Modules` objektu (skupiny, ikony) + upravené texty přes `text-webjet9.properties`

## 2020.12

[#43588 - wj9 - zfunkčnení základního rozložení] - role #1 => Commitol jsem první verzi, zfunkčněno je:
- klik na logo + `tooltip` s verzí (TODO: to by chtělo ještě nějak lépe vypsat, TODO: doplnit loga za NET a LMS)
- přepnutí domény (jsou tam pro `iwcm.interway.sk` i ilustrační data), TODĚ: je to rozbité, `@MHO` třeba nastylovat
- pomocník
- jméno přihlášeného uživatele
- odhlašovací odkaz
- TODO: zbývající ikony v hlavičce

PLUS:
- na projektu zapnutý lombook - https://projectlombok.org
- provedeny základní třídy `LayoutBean` a `HeaderBean` s jejich vložením do modelu
- doplněno `webjet.js` s JS funkcemi WJ.xxx
- základní skelet pro překlad textů, přidána `WJ.translate`, musí se to ale jmenovat jako `async`, což zpomaluje zobrazení.

Navrhuji v budoucnu překladové klíče přenášet z HTML kódu jako parametr JS funkce (v pug souboru se překlad vloží jednoduše jako `#{prekladovy.kluc}` a na serveru se přeloží.
