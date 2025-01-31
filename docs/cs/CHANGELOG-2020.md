# Seznam změn verze 2020

## 2020.53

> 2020.53 přináší kompatibilitu pro použití v klientských projektech. Balíček je k dispozici na `artifactory/maven` serveru jako verze `2021.0-SNAPSHOT`. Při prvním spuštění přidá do některých tabulek databázové sloupce, ale změna je zpětně kompatibilní a stále se můžete vrátit k WebJETu 8.8.

V klientských projektech stačí nastavit příslušnou verzi v položce `build.gradle`:

```gradle
ext {
    webjetVersion = "2021.0-SNAPSHOT";
}
```

jsme experimentálně ověřili základní funkčnost na projektech s databázemi MariaDB, Microsoft SQL a Oracle DB.

**Build verzí Artifactory/Maven**

#52318 [Build soubor](../ant/build.xml) obsahuje několik `task` prvků, posledním prvkem je `deploy` který má nastavené správné závislosti, takže stačí spustit tento. Seznam `taskov`:
- `setup` - obnovuje závislosti a generuje `WAR` archiv
- `updatezip` - připravit dočasnou strukturu v `build/updatezip` adresář. Struktura obsahuje rozbalený `WAR` archiv, rozbaleno `webjet-XXXX.jar` soubory (tj. kompletní struktura adresářů /admin, /components a /WEB-INF/classes).
- `preparesrc` - stahuje se `SRC` jar a připraví strukturu archivu jar se zdrojovými soubory (propojenými z archivu jar a zdrojového kódu WebJET 2021).
- `define-artifact-properties` - definuje vlastnosti pro generování artefaktů, zde v části `artifact.version` nastaví verzi generovaného artefaktu
- `makejars` - připravuje archivy jar tříd, adresáře /admin a /components a zdrojové soubory.
- `download` - pomocná úloha ke stažení `pom` soubory a archivy jar, které nejsou upraveny (`struts, daisydiff, jtidy, swagger`)
- `deploy` - skutečné uložení artefaktů na místě. `artrifactory` serveru, obsahuje definici závislosti (generování `POM` soubor)

Postup generování nové verze:

```shell
cd ant
ant deploy
```

*Poznámka*: v adresáři `build/updatezip` je vytvořena rozbalená struktura, kterou lze zazipovat a použít jako aktualizační balíček pro WebJET ve staré struktuře (bez použití Spring archivů).

**Inicializace JPA a jara**

Inicializace JPA a jara je přesunuta do balíčku `sk.iway.webjet.v9`. Z důvodu zpětné kompatibility jsme museli vyřešit problém s pojmenováním databázových tabulek. `_adminlog_` a `_properties_` v DB Oracle. Používá se název `webjet_adminlog` a `webjet_properties`. Název tabulky je však přímo v entitách JPA. Využili jsme možnosti použít rozhraní `SessionCustomizer`, která je implementována ve třídě [JpaSessionCustomizer](../src/main/java/sk/iway/webjet/v9/JpaSessionCustomizer.java). Při inicializaci JPA prochází všechny nalezené entity JPA a v případě instalace Oracle mění hodnotu anotace pro zadané tabulky databáze.

Pro JPA jsme přidali správné nastavení typu databáze (v předchozí verzi byla nastavena pouze možnost MariaDB).

**Domovská obrazovka**

#47428 Design rozřezané úvodní obrazovky naplněný daty. Data se zatím zadávají přímo do [ThymeleafAdminController](../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java), bude načtena ajaxem později po dokončení vlastností.

Obrazovka se řeže pomocí komponent VUE. Nové objekty DTO se používají pro `DocDetails, Audit a User beany`.

Objekt JavaScript je k dispozici `window.currentUser`. Přidány filtry VUE pro formátování data a času - `formatDate,formatDateTime,formatDateTimeSeconds,formatTime`.

**Další změny**

- [Skripty] opraven typ auditního záznamu z `CLIENT_SPECIFIC` opravit `INSERT_SCRIPT`, přidaná metoda `toString()` na fazolích dokumentů a skupin, aby bylo možné lépe zaznamenat vybrané omezení na adresáře/stránky v auditním záznamu (zapíše se ID záznamu a cesta k adresáři/stránce).
- [WebJET] upravena inicializace pro nalezení SpringConfig v balíčku `sk.iway.webjet.v9`, automatické vyhledávání překladových souborů a automatická aktualizace také v. `text-webjetXX.properties` a `autoupdate-webjetXX.xml` (kde webjetXX je hodnota z konfigurační proměnné `defaultSkin`).
- `autoupdate.xml` - Vylepšená kompatibilita s WebJET 8, aby bylo možné spustit projekt WebJET 2021 a zároveň se vrátit zpět k WebJET 8.8 bez dopadu. přidány sloupce `id,update_date` ke stolu `_properties_` a sloupec `task_name` ke stolu `crontab`. Sloupce nejsou v rozporu se starou verzí a nebrání používání správy ve verzi 8.8.

**Dokumentace**

- je nasazen na http://docs.webjetcms.sk/v2021/, zatím není vygenerováno žádné postranní menu, řeší se pomocí úlohy gradle `rsyncDocs`
- vyplněná dokumentace pro vložení [Kód HTML bez escapování](developer/frameworks/thymeleaf.md#základní-text---výpis-atributů) v Thymeleaf
- doplněná dokumentace pro [formátování data a času](developer/frameworks/vue.md#formátování-data-a-času) ve službě Vue

![meme](_media/meme/2020-53.jpg ":no-zoom")

## 2020.51

**Monitorování serveru - zaznamenané hodnoty**

Nová verze zobrazení zaznamenaných hodnot monitorování serveru. Zobrazuje tabulku i grafy a pro grafy používá agregaci dat podle denního rozsahu:
- 0-5 = přesná hodnota bez agregace
- 5-10 = agregace do 10minutového intervalu
- 10-14 = agregace na 30minutový interval
- 14-30 = agregace za hodinu
- 30-60 = agregace po dobu 4 hodin
- 60+ = agregace po dobu 12 hodin

Pokud by agregace nebyla provedena, prohlížeč by pravděpodobně klesl na množství dat v grafu. Implementace se nachází ve třídě [MonitoringAggregator](../src/main/java/sk/iway/iwcm/components/monitoring/MonitoringAggregator.java), je zaznamenána nejvyšší hodnota v daném časovém intervalu.

- #50053 [Grafy] - přidána základní dokumentace ke knihovně [amcharts](developer/frameworks/amcharts.md).
- #50053 [Grafy] - v `app.js` přidána asynchronní inicializace knihovny amcharts voláním `window.initAmcharts()`.

![meme](_media/meme/2020-51.jpg ":no-zoom")

## 2020.50

> 2020.50 zavádí správné generování položek menu administrace podle dostupnosti nové verze stránky administrace. V nabídce lze plynule přepínat mezi verzí 8 a 9. V případě potřeby lze přepnout na původní verzi stránky administrace.
>
> Vylepšen byl také editor stránek, který automaticky upravuje svou výšku podle velikosti okna. Byly také přidány ikony pro stavy adresářů a webových stránek ve stromové struktuře.

**Webové stránky**

- Odstraněn původní kód pro úpravu ukázkové stránky a adresáře, nadále se používají pouze datové pohledy.
- Přidány stavové ikony ve stromové struktuře podle původního návrhu pro adresář (interní, nezobrazuje se v nabídce, zaheslovaný) a webovou stránku (zobrazení zakázáno, přesměrováno, nevyhledatelné, zaheslované).
- Dialogové okno editoru má automaticky vypočítanou výšku podle velikosti okna (nejméně však 300 pixelů). Výška se přepočítává každé 3 sekundy.
- Přidána klikatelná webová stránka ve stromové struktuře. Je to poměrně složité, protože editor je integrován s datovou tabulkou. Nejprve se tedy zkontroluje, zda datová tabulka zobrazuje adresář, ve kterém se stránka nachází. Pokud ne, hodnota se zahodí `docId` do proměnné `docidAfterLoad`. Tento parametr se kontroluje po načtení seznamu stránek v adresáři, pokud je nastaven, automaticky se spustí editace stránky.

**Generování nabídek**

- #51997 [Navigace] Upraveno generování položek menu. Ve verzích v8 i v9 se generují odkazy na stránky již přepracované ve verzi v9, takže můžete transparentně přecházet mezi starým a novým WebJETem. Při požadavku na zobrazení staré verze v8 je možné zadat do adresy URL následující údaje `/admin/v8` která přepne generování nabídky na starou verzi a je možné zobrazit starou verzi administrace. Na verzi v9 se dostanete zavoláním adresy URL `/admin/v9`. V horní části stránky je také ikona pro přepnutí na verzi v8.

**Dokumentace**

Doplněná dokumentace pro použití [jsTree](developer/jstree/README.md).

## 2020.49

**Import/export:**

Implementoval systém pro import a export dat mezi datovými tabulkami.

Export:
- Umožňuje export do **Excel(xlsx), PDF a přímý tisk na tiskárnu**
- Soubor je pojmenován podle aktuálního `title` a automaticky se přidá aktuální datum a čas.
- Export lze nastavit na datový typ (**aktuální stránka/všechny, filtrované/všechny/označené řádky, třídění, sloupce**).
- Při stránkování serveru se nejprve provede volání služby REST, ze které se provede **maximálně 50 000 záznamů**. Pokud potřebujete exportovat více záznamů, použijte vícenásobný export pomocí filtrování.
- Data serveru jsou zpracovávána mimo datovou tabulku (z výkonnostních důvodů), jsou znovu vykreslena. `datetime` sloupců a `radio button` (podporovány jsou `date-time`, `date`, `*-select`, `*-boolean`).
- Při exportu je v prvním řádku připraven seznam sloupců, **import proto není citlivý na pořadí sloupců**.
- Pro **výběrová pole** (select/digits) s **exportovat textovou hodnotu** a při importu se rekonstruuje zpět na ID. To umožňuje mít **různá ID vázaných záznamů** mezi prostředími (např. ID šablony webové stránky), pokud se název shoduje, je záznam správně spárován. V důsledku toho export obsahuje místo ID také lidsky čitelný text.

Dovoz:
- Povoleno **importovat data jako nová** (pro přidání do databáze) nebo **porovnat existující data podle vybraného sloupce** (např. název, adresa URL atd.). Při porovnávání nejprve automaticky vyhledá záznam v databázi a poté jej aktualizuje. Pokud neexistuje, vytvoří nový záznam.
- **Import z formátu xlsx**.
- Import se provádí **postupně v dávkách po 25 záznamech** aby nedošlo k přetížení serveru.

**Datatables:**

- Upraveno filtrování podle data - přidána možnost nastavení času do uživatelského rozhraní, již není nutné nastavovat čas konce dne na backendu
- Přidány atributy (potřebujeme je pro volání služby REST pro export dat):
  - `DATA.jsonOptions` - nastavení z odpovědi služby REST pro vykreslování (potřebujeme pro export dat kódové knihy).
  - `DATA.urlLatest` - úplná adresa URL posledního volání REST
  - `DATA.urlLatestParams` - všechny parametry posledního volání REST (aktuální stránka, velikost stránky, filtry).

## 2020.45

- #47293 [Mazání záznamů v databázi] Přepracovaná verze mazání dat z WJ8 do datové tabulky. Používá několik hacků, jako je nastavení [externí filtr](developer/datatables/README.md#externí-filtr), zakázat stránkování (možnost stránkování)

**Datatables:**

- Přidána možnost **nezobrazovat filtr** v záhlaví tabulky pro sloupec nastavením atributu `filter=false` v anotaci
- Přidána možnost **zakázat stránkování** přes `options paging: false`, [Dokumentace](developer/datatables/README.md#možnosti-konfigurace)

## 2020.44

> Verze 2020.44 přináší **nová dokumentace** k [Funkce WebJET v jazyce JavaScript](developer/frameworks/webjetjs.md), [Automatizované testování datové tabulky](developer/testing/datatable.md), přidáváme příklad pro [Dynamická změna hodnot ve výběrovém poli v Editoru datových tabulek](developer/datatables-editor/README.md#dynamická-změna-hodnot-ve-výběrovém-poli) a další drobné změny.
>
> Aplikaci jsme přenesli na WebJET 2021 **Odstranění objektů mezipaměti** a my jsme vyplnili žádost **Šablony**. Přidává výběr souborů šablon JSP na serveru s dynamickým načítáním na základě vybrané skupiny šablon a nastavení adresáře pro zobrazení šablony. Přidali jsme také [automatizované testy](../src/test/webapp/tests/components/templates.js).
>
> Aplikace **Sledování serveru/skutečné hodnoty** jsme vizuálně doladili (odsazení, zobrazení mobilní verze) a upravili živá data v grafech. Nyní se správně posouvají v reálném čase.
>
> Do datové tabulky jsme přidali možnost [potvrzení o přijatých opatřeních](developer/datatables/README.md#tlačítko-pro-provedení-akce-serveru) pro speciální tlačítka v datové tabulce (například delete all).
- [Kód VS] upraveno [tasks.json](../.vscode/tasks.json) a [launch.json](../.vscode/launch.json) přidání konfigurace ladění `Debug`. Spustí aplikační server a poté se připojí v režimu ladění. Bohužel kvůli [chyba v gretty](https://github.com/gretty-gradle-plugin/gretty/issues/166) vypnutí je vyřešeno `killnutím` (zatím nebylo testováno ve Windows). Ve VS Code můžete spustit konfiguraci "Debug" přímo na kartě Spustit.
- [Rozložení] Vizuálně opraveno zobrazení výběru domény v pravé horní části
- #47293 [toastr] Přepracovaný kód používající toastr přímo pro volání API [WJ.notify](developer/frameworks/webjetjs.md#oznámení)

**Datatables:**

- #47293 [Datatable] Upravená funkce `TABLE.executeAction` pro možnost potvrdit akci před provedením
- #45718 [Datový editor] Zakázáno odesílání editoru po stisknutí klávesy Enter podle nastavení konfigurace `onReturn: false`. Způsobovalo to problém ve filtru výběrového pole, kde stisknutí klávesy Enter nastavilo hodnotu a zároveň odeslalo formulář.
- #47293 [Datové tabulky] Upravený kód [DatatableRestControllerV2.action](../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) pro možnost spuštění akce i pro nezadanou entitu (nulová hodnota). Používá se pro akce typu "Smazat vše"

**Odstranění objektů mezipaměti:**

- #47293 Převedena verze z WebJET 8 na datovou. Cache objekty, ale ne úložiště Spring DATA. Implementováno je vlastní [služba](../src/main/java/sk/iway/iwcm/components/memory_cleanup/cache_objects/CacheObjectsService.java), který poskytuje rozhraní API pro přístup k seznamu tříd a objektů mezipaměti.
- #47293 Ve třídě [CacheObjectsService](../src/main/java/sk/iway/iwcm/components/memory_cleanup/cache_objects/CacheObjectsService.java) jsou implementovány metody `deleteAllCacheBeans()` odstranit celou mezipaměť včetně DocDB, GroupsDB a dalších objektů. Metoda `deletePictureCache()` implementuje vymazání mezipaměti `/thumb` Obrázky.

**Šablony:**

- Přidáno #45718 [Šablony] [Služba REST /forward](../src/main/java/sk/iway/iwcm/components/templates/TemplatesController.java) vybrat šablonu JSP ze seznamu souborů ve složce `/templates` adresář
- #45718 [Šablony] přidána možnost nastavit adresáře, ve kterých se mají zobrazovat šablony

**Testování:**

- [Testování] Přepracovaný kód automatického testování [DataTables.js](../src/test/webapp/pages/DataTables.js) z parametrického konstruktoru na `options` objekt. Přidána možnost přidat testovací kroky pro nový záznam, úpravy, vyhledávání a mazání záznamů. Upraveno čekání z pevné `I.wait(1)` na adrese `DTE.waitForLoader()`, doplněné `I.say` pro lepší orientaci v testovacím logu. Test vyhledávání doplněný o ověření nalezených záznamů.
- [Testování] Přidána šablona `default` pro možnost testovat vybrané soubory šablon JSP.

**Dokumentace:**

- [Datatable] Doplňková dokumentace pro [tlačítko pro provedení akce serveru](developer/datatables/README.md#tlačítko-pro-provedení-akce-serveru) (např. otočit obrázek, odstranit všechny záznamy).
- Doplněná dokumentace pro použití [Gitlabu ve VS Code](developer/guildelines/gitlab.md#práce-v-kódu-vs)
- Přidána dokumentace [Funkce WebJET v jazyce JavaScript](developer/frameworks/webjetjs.md) zobrazit oznámení, potvrzení akcí a další informace. Přepracování jejich kódu pro lepší použití.
- Doplněný seznam [speciální funkce](developer/testing/README.md) pro testování datových tabulek
- Přidán popis použití a provedení [Automatizované testování datových souborů](developer/testing/datatable.md)
- #45718 přidány pokyny pro [dynamická změna hodnot v poli pro výběr](developer/datatables-editor/README.md#dynamická-změna-hodnot-ve-výběrovém-poli) v Editoru datových souborů podle jiných polí

## 2020.43

- [Rozvržení] Upraveno generování drobečkové navigace - původní prvky se skládaly z nadpisu `pills-#tab[1]`, znak # je nyní z textu odstraněn, takže je nyní generován jako `pills-tab[0]`
- #47419 [Sledování serveru] - upraveno vizuální zobrazení (posuny), přepracován způsob zobrazení řádků v grafu Volné místo na disku (zarovnání hodnot GB a pozic v grafu)
- #47419 [Sledování serveru] - prodloužení času zobrazovaného v grafu aktuálních hodnot na 10 minut, přidání popisů, přesun nastavení intervalu do záhlaví, přesun překladů do sekce `properties` soubor

## 2020.42

> Verze 2020.42 zavádí zejména [automatický audit subjektů JPA](developer/backend/auditing.md). Jednoduchá anotace zajistí, že se pro každou manipulaci s entitou vytvoří auditní stopy. Auditní záznam obsahuje také seznam změněných atributů (ve tvaru stará hodnota -> nová hodnota) nebo seznam všech atributů při vytvoření a smazání entity.
>
> Rozšířili jsme a zdokumentovali možnosti pole JSON pro [přidávání adresářů a webových stránek](developer/datatables-editor/field-json.md) v datových tabulkách.
>
> Přidali jsme možnost nastavit datové tabulky [výchozí způsob uspořádání](developer/datatables/README.md#Uspořádání) a pole s datem a časem, přidali jsme také zobrazení sekund.
>
> Do služby WebJET jsme přidali knihovnu `oshi-core` k získání zátěže procesoru a amcharts k zobrazení grafů v nástroji Sledování serveru.
>
> Rozšířili jsme dokumentaci o část popisující [Spring frameworku](developer/frameworks/spring.md), přidali jsme oddíl [řešení problémů](developer/troubles/README.md), upraveno [Příklady](developer/frameworks/example.md) a dokončil dokumentaci pro [testování](developer/testing/README.md).

- #46891 [Testování] - upraveny stávající testy na nové vlastnosti a nastavení tabulek, in `audit-search.js` upraveno pevné čekání na dynamické pomocí `DT.waitForLoader`, v `forms.js` přepracované testování vyhledávání dat
- #46891 [Datatable] - přidáno tlačítko pro opětovné načtení (obnovení) dat tabulky ze serveru
- #46891 [Audit] - přidáno zobrazení sloupce ID subjektu
- #46891 [Překladové klíče] - opraveno vyhledávání podle data změny
- #46891 [Auditování] - Povoleno auditování entit JPA `FormsEntity, GalleryDimension, GallleryEntity, InsertScriptBean, InsertScriptDocBean, InsertScriptGroupBean, TranslationkeyEntity, AuditNotifyEntity`
- #46891 [Auditing] - přejmenovaná tabulka `webjet_audit` na adrese `webjet_adminlog` původním názvem v Oracle
- #46891 [Testování] - přidáno [kód rozšířeníceptjs-chai](developer/testing/README.md#knihovna-assert) pro snadné psaní `assert` podmínky
- #46891 [Dokumentace] - přidána dokumentace pro [audit](developer/backend/auditing.md), [řešení problémů](developer/troubles/README.md) a doplněné informacemi o [testování](developer/testing/README.md#další-funkce-webjetu)
- upravená nastavení v `build.gradle`, `options.encoding=utf-8`, opravená diakritika v `PathFilter`
- #46891 [Testování] - do standardního volání přidáno testování auditní stopy `DataTables.baseTest('domainRedirectTable');`
- #46891 [Datatable] - přidána možnost nastavení [způsob uspořádání](developer/datatables/README.md#Uspořádání) při zobrazení stránky nastavte pořadí konfigurace (podle data změny),
- #46891 [Datatable] - přidání sekund do formátování data a času
- #46891 [Audit] - přidán audit exportu datových souborů

## 2020.41

- [Dokumentace] upravená dokumentace [Ukázka kódu](developer/frameworks/example.md) - PUG upraven pro použití anotací, přidány příklady
- [Dokumentace] doplněná o základní informace o [EclipseLink JPA, Spring DATA a Spring REST](developer/frameworks/spring.md)

## 2020.40

- #47419 [Sledování serveru] modul zobrazuje aktuální hodnoty sledování serveru, včetně grafů procesoru a paměti
- #47419 [Sledování serveru] přidána knihovna [oshi-core](https://github.com/oshi/oshi) k získání údajů o zatížení procesoru
- #47419 [Sledování serveru] přidána knihovna [amcharts](https://www.amcharts.com/) pro zobrazení grafů se používá komerční licence.
- #46261 [Skripty] modul přidal možnost nastavit adresář nebo webovou stránku pro skript
- #46261 [Pole DTED json] refaktorizovaný kód [Datové pole JSON pro DTED](developer/datatables-editor/field-json.md). Přidána možnost konverze objektu JSON a textu tlačítka, vytvořena dokumentace.
- #47293 [Datatable] přidána možnost zobrazit vlastní tlačítko pouze při výběru řádku pomocí kódu `$.fn.dataTable.Buttons.showIfRowSelected(this, dt);`, [Dokumentace](developer/datatables/README.md)
- #47293 [Datatable] přidána podpora `range` i pro číselné hodnoty je předpona `range:`, funguje podobně jako předpona `daterange:`
- #47293 [Persistentní mezipaměť] zobrazení záznamů přes datatable, Spring DATA

## 2020.39

> V této verzi jsme přidali možnosti do datových tabulek. **aktualizace tabulky** po volání služby REST (např. na webových stránkách při přesunu stránky do jiného adresáře), možnost `fetchOnEdit` Pro **obnovení upraveného záznamu** před zobrazením editoru a `fetchOnCreate` Pro **získání dat pro nový záznam** (např. pro šablonu webové stránky, pořadí rozvržení).
>
> Do anotací Java jsme přidali možnost **skrývání sloupců** v tabulce nebo v editoru pomocí parametrů `hidden` nebo `hiddenEditor`.
>
> Přidali jsme také **editace objektů JSON** je mezitím spojen s úpravou adresáře a výpisem adresáře (např. pro webovou stránku jako např. `GroupDetails` je implementován jako komponenta VUE.

- #47341 - vše níže uvedené souvisí s větví/značkou #47341
- [Webové stránky] Funkční nastavení kopírování adresářů a stránek v adresářích. Vyřešeno pomocí komponenty VUE [vue-folder-tree.vue](../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/folder-tree/vue-folder-tree.vue). JSTree a otevře se do celoobrazovkového dialogu. V anotaci `@DatatableColumn` se zapíše jako `DataTableColumnType.JSON` kde pomocí atributu `className = "dt-tree-group"` je nastaven na výběr jednoho adresáře nebo `className = "dt-tree-group-array"` výběr více adresářů. Objekty jsou typu `GroupDetails`.
- [Webové stránky] - vyřešeno přesunutí stránky do jiného adresáře s obnovením datové tabulky a stromové struktury stránky
- [Webové stránky] - odstranění přidaných webových stránek
- [Datové tabulky] Přidána možnost [aktualizace datových souborů](developer/datatables/restcontroller.md#načíst-datové-tabulky) po uložení dat (např. při přesunu webové stránky do jiného adresáře). Přidána metoda `setForceReload(boolean)` k událostem řadiče REST a JavaScriptu `WJ.DTE.forceReload`.
- [Datové tabulky] Přidána možnost nezobrazovat sloupec v datové tabulce nebo v editoru podle parametru `hidden` (pro nezobrazení v datové tabulce) nebo `hiddenEditor` (nezobrazuje se v editoru) anotace [@DatatableColumn](developer/datatables-editor/datatable-columns.md#vlastnosti-datatablecolumn). Na rozdíl od parametru `visible` není možné zobrazit takové sloupce ani prostřednictvím nastavení datových tabulek na frontend.
- [Datové tabulky] Přidán inicializační parametr `fetchOnEdit` - pokud je nastavena na hodnotu true, bude před úpravou záznamu provedeno volání REST pro načtení aktuálních dat upravovaného záznamu. Při použití datové tabulky, například pro webovou stránku, je záznam aktualizován ze serveru před otevřením editoru, takže v editoru bude vždy otevřena nejnovější verze. Implementováno prostřednictvím funkce JS `refreshRow` a zákaznické tlačítko `$.fn.dataTable.ext.buttons.editRefresh` nahradit standardní tlačítko `edit`.
- [Datové tabulky] Přidán inicializační parametr `fetchOnCreate` - po nastavení na `true` bude provedeno volání REST s hodnotou -1 pro získání dat nového objektu před vytvořením nového záznamu. To je vhodné pro nastavení výchozích dat nového záznamu. Například pro webovou stránku se před záznamem nastaví adresář, pořadí rozvržení, šablona atd.
- [Datové tabulky] Přidáno automatické zobrazení ID záznamu v prvním sloupci, kde dříve byla pouze výběrová pole pro označení řádku.
- [Datové tabulky] Upraveno zobrazení chybové zprávy ze serveru tak, aby se nenacházela mezi tlačítky Storno a Uložit, ale vlevo od nich.
- [Testování] přidána funkce `DT.waitForLoader` který čeká na zobrazení informace "Zpracování" a poté se skryje v datové tabulce. Používá se jako `DT.waitForLoader("#forms-list_processing");`

## 2020.36

- #44968 [Překladové klíče] Upraveno ukládání překladu po změně jazyka - vytvořena kopie překladového klíče, přidáno ukládání data poslední změny, opraveny testy
- #44968 [Datové tabulky] Přidána možnost nezobrazovat sloupec v datové tabulce pomocí anotace `hidden=true`, takový sloupec na rozdíl od `visible` nelze zobrazit ani v nastavení datového sloupce. V editoru jej však stále lze použít, pokud má anotaci pro editor.

## 2020.34

- #47008 [Datové tabulky] Přidáno automatické generování `required` atributu v `columns` definice, pokud má pole anotaci `@NotEmpty` nebo `@NotBlank`. Implementováno v [DatatableColumnEditor.java](../src/main/java/sk/iway/iwcm/system/datatable/json/DataTableColumnEditor.java).
- #47008 [Automatizované testování] Přidáno [automatizovaný test](../src/test/webapp/pages/DataTables.js) datových tabulek se čtením definice přímo z `columns` objekt. Provede standardní test CRUD. Ukázka použití v [redirects.js](../src/test/webapp/tests/components/redirects.js):

```javascript
const requiredFields = ['oldUrl', 'newUrl'];
DataTables.baseTest('redirectTable', requiredFields);
```

## 2020.33

- #47341 [Datové tabulky] - Přidán nový typ pole JSON, zatím se zobrazuje jako `textarea` ale při čtení a zápisu jsou data správně odesílána jako objekty JSON. Implementováno jako nové pole typu json v index.js v kódu `$.fn.dataTable.Editor.fieldTypes.json`
- #47425 [Datové tabulky] - Opraveno hromadné označování a mazání řádků na serveru
- #44890 [Testy] - [přidané pokyny](developer/testing/README.md#čekání-na-dokončení) pro použití `I.waitFor*` místo pevných `I.wait`. Upravený test [webpages.js](../src/test/webapp/tests/components/webpages.js) pro použití `waitFor` a `within`, protože stránka obsahuje dvě datové tabulky.
- #44890 [Datatables Editor] - Přidán přenos třídy CSS z atributu `className` anotace na celém řádku v editoru div.DTE\_Field. Viz komentář `//prenesieme hodnotu className aj do DT editora do prislusneho riadku` v [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js).
- #44890 [Datové tabulky] - Upravené porovnání `true/false` hodnoty pro pole výběru. Podobně jako u zaškrtávacího políčka je třeba porovnat neřetězcovou hodnotu. `true` (viz komentář `ak mame true/false select box, aj tomu musime zmenit value na true/false namiesto String hodnot` v [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)).

## 2020.32

- #44890 [Datové tabulky] - Upravená funkčnost `BOOLEAN`, `dt-format-boolean-true`, v objektu JSON je hodnota předána jako `true` ale v `options` objekt jako `"true"` (řetězec). Porovnání a nastavení hodnoty tedy nefungovalo. Upravil jsem kód tak, aby `editor.options` se změní z `"true" na true` Hodnota. Pak funguje porovnání s objektem a správné nastavení `checkboxu`. Upraveno také filtrování v klientském stránkování, kde se hodnota hodnoty neporovnává se štítkem (viz komentář `//neplati pre column-type-boolean` v [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)).
- #44890 [Datové tabulky] - Přenos datových atributů přidán i do polí typu `checkbox` a `radio` (podmínka `"div.DTE_Field_Type_"+data.editor.type`). Jsou nastaveny na první prvek v seznamu. Umožňují nastavit atributy jako např. `data-dt-field-hr` nebo `data-dt-field-headline`.
- #44890 [Datum a čas] - Upraveno chování hodnoty 0 ve formátu data primitivního typu `long`. Při hodnotě 0 se místo 1.1.1970 zobrazí prázdné datum a čas. Upraveno v `renderDate` v [datatables-config.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-config.js).
- #44890 [Datové tabulky] - Upravené generování `Options` pole pro editor pomocí [LabelValue](../src/main/java/sk/iway/iwcm/system/datatable/json/LabelValue.java) objekt.

## 2020.31

- #44890 [Datové tabulky] - Přidána možnost v anotaci nastavit uspořádání polí ve výstupním objektu JSON pomocí atributu `sortAfter` v anotaci `@DatatableColumn`. Třídění se provádí ve třídě [DatatableColumnsFactory.java](../src/main/java/sk/iway/iwcm/system/datatable/DataTableColumnsFactory.java) v metodě `sortColumns`.
- #44890 [Datové tabulky] - Přidány možnosti anotace `DataTableColumnType.BOOLEAN` pro hodnoty true/false a `DataTableColumnType.CHECKBOX` pro standardní zaškrtávací políčka.
- #44890 [Datové tabulky] - Přidána možnost anotace [vnořené objekty](developer/datatables-editor/datatable-columns.md#vnořené-atributy) přes `@DatatableColumnNested`. Atribut prefix (výchozí hodnota je auto, která generuje prefix podle názvu anotovaného pole) lze použít k určení prefixu atributu ve vygenerovaném objektu JSON (např. nastavením na hodnotu `editorFields` je vygenerováno pole ve formátu JSON `allowChangeUrl` Stejně jako `editorFields.allowChangeUrl`). Prefix může být také prázdný, pak se generuje pouze název proměnné (pokud má např. `gettery/settery` v původním objektu, například. [FieldsFromAtoE](../src/main/java/sk/iway/iwcm/doc/FieldsFromAtoE.java) v [DocDetails](../src/main/java/sk/iway/iwcm/doc/DocDetails.java) zařízení.)

## 2020.30

- #44890 [Datové tabulky] - Vyřešeno vložení více datových tabulek do webové stránky. Každá datová tabulka bude mít nastaven svůj atribut HTML ID, i když není v položce `options` (ve výchozím nastavení datatableInit). Atribut ID se pak vkládá i do dalších selektorů, aby se filtry a tlačítka správně namapovaly na správnou datovou tabulku. Upravené funkce `$.fn.dataTable.Editor.display.bootstrap.init` a `$.fn.dataTable.Editor.display.bootstrap.open` pro podporu více editorů DT na stránce.

## 2020.29

- #346264 [Webové stránky] Přidána možnost přidávat/upravovat adresáře ve stromové struktuře webových stránek. Implementováno pomocí skrytého datového souboru a editoru DT.
- #45763 [Stromová struktura] Přidáno rozhraní API pro [úprava stromové struktury](developer/jstree/README.md), použité v sekci webových stránek a ve fotogalerii. Již nyní je možné přesouvat položky v adresářové struktuře webových stránek a přesouvat adresáře v galerii.
- #44836 [Webové stránky] Přidána pole do editoru DT, backend je zatím nezpracovává
- #47125 [Úkoly na pozadí] Přepracováno na datové tabulky, přidán sloupec s názvem úkolu do databáze

## 2020.28

- #45532 [Formuláře] Přidáno automatické vyhledávání
- #45409 [Audit] Přidány testy filtrování
- [Datatables] pro atribut checkbox automaticky nastaven `falseValue` na ne (pokud není uvedeno)
- Opraveno nastavení přízvuku pro soubory JS z `/admin/v9/dist/` adresáře (výstup je odesílán v UTF-8)

## 2020.27

- #46264 [samostatný editor datových tabulek] Přidána možnost označit datovou tabulku jako skrytou ([atribut hideTable v možnostech](developer/datatables/README.md)) pro možnost `standalone` Editor DT pro jstree
- #45721 [Template Groups] Upraveno zobrazení datatable pro použití anotace, přidán test

## 2020.26

- #43588 [Datové tabulky] Opraveno opakované nastavení výběrového pole ve filtru a v editoru (po druhém nastavení nebylo možné vybrat hodnoty)
- #45532 [Formuláře] Přidána možnost základního vyhledávání v datech formuláře, omezená na maximálně 6 sloupců (kvůli Spring DATA API)
- #45718 [Šablony] Verze seznamu šablon přes datovou tabulku, používá `TemplatesDB` API
- #45490 [Audit] Zobrazení protokolů a oznámení auditu. Obojí přes Spring DATA, pro audit přidány výběrové boxy pro filtrování v datové tabulce (propojeno s editorem možností), přidána možnost [vlastní filtrování](./datatables/restcontroller.md#speciální-vyhledávání) v řadiči REST.
- #44968 [Překladové klíče] Zobrazení a editace překladových klíčů, implementované čtení původních hodnot, mapování `.properties` a změny DB.

## 2020.25

- #45532 [Formuláře] Zobrazení seznamu formulářů a dat formuláře (dynamická data) pomocí datatable.
- #45679 [Datové tabulky] Anotace pro generování `columns` konfigurace přenesené do WebJET 8 pro anotaci existujících fazolí.
- #45382 [Datové tabulky] Import a export dat - úprava dialogu pro import dat

## 2020.23

- #45685 [CI-CD] Konfigurace CI-CD pipeline na serveru Gitlab, noční sestavení z hlavní větve dostupné na adrese http://demotest.webjetcms.sk/admin/
- #45382 [Datové tabulky] Počáteční verze importu dat odesláním dat z importního excelu do editoru datových tabulek, možnost nastavení režimu importu (zatím bez podpory na backendu)
- #45379 [Datové tabulky] Přepracovaný kód přizpůsobení datové tabulky z `app.js` do samostatného souboru [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) s možností použití jako [Modul NPM](../src/main/webapp/admin/v9/npm_packages/README.md) (např. na frontend v šabloně). Přepracovaný kód pro možnost použití více datových tabulek na jedné stránce.
- #45679 [Datové tabulky] Přidána možnost [generování konfigurace sloupců](developer/datatables-editor/datatable-columns.md) datové tabulky pomocí anotace Java bean

## 2020.22

- #44836 [Webové stránky] Přenesen kód úprav ckeditoru z WJ8. Upraveno vkládání obrázků, formuláře a další dialogová okna editoru CK. Přidán skrytý formulář obsahující `docid, groupid, virtualpath` pro kompatibilitu náhledu aplikace a dialogových oken z WJ8.
- #44836 [Webové stránky] Pro redaktora, the `options` zasílá `original` objekt šablony, který je potřebný k získání cest ke stylům CSS. Při otevření editoru DT se zachová CSS začínající na /admin (pro editor rozšíření CK), zbytek se změní podle přiřazené šablony.
- #44836 [Webová stránka] Editor DT je k dispozici přímo [Objekt JSON](./datatables-editor/README.md#ukázky-kódu) upraveného záznamu (ne všechny údaje). Pro `options` přidána možnost odesílání číselníků [původní objekt](./datatables/restcontroller.md). Objekt JSON načtený ze serveru (obsahující kompletní data všech řádků a vlastnost options) je k dispozici v DT jako DATA.json.
- #44737 [Konfigurace] Aktualizováno [autotest](../src/test/webapp/tests/components/configuration.js).
- #44293 [Přesměrování domény] Aktualizováno [autotest](../src/test/webapp/tests/components/domain-redirects.js).

## 2020.21

- [Testování] Přidány třídy CSS `dt-filter-MENO` na filtračních polích v datové tabulce pro možnost nastavení filtrování v automatizovaném testu. `I.fillField("input.dt-filter-newUrl", randomNumber);`. Příklad v testu [redirects.js](../src/test/webapp/tests/components/redirects.js)
- [Přesměrování] Přidáno [anotace povinných polí](./datatables/restcontroller.md#platná%c3%a1cia---povinná%c3%a9-pole) do UrlRedirectBean.java (v WJ8)
- [Galerie] Přidány překlady textu pro editor obrázků a oblast zájmu, přidána možnost nastavení oblasti zájmu ve vstupních polích, přidáno [automatický test](../src/test/webapp/tests/components/gallery.js)
- [Dokumentace] Přidáno [vzorový kód](developer/frameworks/example.md) použití rámců
- [Testování] Přidáno [generování zpráv](./testing/README.md#generování-html-sestav) přes mochawesome a CodeceptUI pro [testovací kontrola](./testing/README.md#kódové-rozhraní-ui) prostřednictvím prohlížeče
- [Datatable] Přidána možnost zobrazení dat (sloupců a polí v editoru) [na základě práv](./datatables/README.md#zobrazení-dat-na-základě-práv) přihlášený uživatel

## 2020.20

- [Datovatelné] Přidáno [vyhledávání podle rozsahu dat](./datatables/restcontroller.md#vyhnout-se%c4%bead%c3%a1vanie-pod%c4%bea-range-d%c3%a1tumov) specifické zpracování parametru URL s předponou `daterange:od-do`.
- [Dokumentace] Doplněná uživatelská příručka [assert](./testing/README.md#assert-book%c5%girl) knihovny, používání a psaní nových [překladové klíče](./frameworks/thymeleaf.md#překlad%c3%bd-text)
- [Galerie] Upraveno zobrazení sloupců s mutacemi jazyků obrázků tak, aby používaly datové tabulky API (aby správně fungovala inline editace)
- [Překladové klávesy] Nová verze zobrazení a editace [překladové klíče](../src/main/webapp/admin/v9/views/pages/settings/translation-keys.pug)
- [Datatable] Přepracovaný kód [DatatableRestControllerV2.java](../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) s komentovaným rozdělením na metody entity a ostatní metody
- [Konfigurace] Nová verze zobrazení [konfigurace](../src/main/webapp/admin/v9/views/pages/settings/configuration.pug) WebJET
- [Dokumentace] Doplňková dokumentace pro [SFC Vue](developer/frameworks/vue.md)
- [Fotogalerie] Další připojení [editor obrázků](../src/main/webapp/admin/v9/src/js/image-editor.js) na adrese `chunked upload` a oznámení toastr
- [Přesměrování] Nová verze zobrazení [přesměrováno](../src/main/webapp/admin/v9/views/pages/settings/redirect.pug)
- [Fotogalerie] Změna doplňku JCrop pro označení zájmové oblasti na obrazovce `vue-advanced-cropper`

## 2020.19

- [Testování] Základní koncept [automatizované testování](developer/testing/README.md)
- [Dokumentace] Rozšíření [Dokumentace](README.md)
- [Webové stránky] Počáteční verze integrace [ckeditor na datovou tabulku](../src/main/webapp/admin/v9/src/js/datatables-ckeditor.js) a zobrazení [webové stránky](../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug)
- [Datatable] Opraven problém s formátem data, automatické nastavení podle `window.userLang` v současné době knihovna - nastavení `wireformat` (výchozí x) a formát atributů v editoru(výchozí L HH:mm)
- [Galerie] Přidána možnost přidat a [upravit adresář](../src/main/webapp/admin/v9/views/partials/folder-tree-galleria.pug), připojené k Vue

## 2020.18

- [Přesměrování domény] Nová verze zobrazení [přesměrování domény](../src/main/webapp/admin/v9/views/pages/settings/domain-redirect.pug)
- [Zabezpečení] Je zajištěna bezpečnost volání všech ostatních služeb. [automatické nastavení](../src/main/webapp/admin/v9/views/partials/head.pug) CSRF token pro všechny ajaxové požadavky
- [Upload] Vyřešeno volání pro správné zpracování souborů po nahrání a API pro mazání souborů (vyčištění galerie, miniatur videa, fulltextového indexu)
- [Galerie] Přidáno [editor obrázků](../src/main/webapp/admin/v9/src/js/image-editor.js)
- [Webové stránky] Základní implementace zobrazení [stromové struktury](../src/main/java/sk/iway/iwcm/admin/layout/JsTreeItem.java) Stránka

## 2020.13

[#43588 - wj9 - základní funkčnost rozvržení] - úkol #5 =>
- upravená konfigurace lombooku (v souboru build.gradle - `config['lombok.accessors.chain'] = 'true'`), aby `settre` Vrácené `this` pro řetězení
- webpack nastaven tak, aby neodstraňoval uvozovky v atributech (protože TH může vrátit víceslovný výraz - `removeAttributeQuotes: false`)
- MENU - přidány objekty MenuBean a MenuService, které generují menu, překlad původního ModuleInfo z angličtiny `Modules` objekt (skupiny, ikony) + upravené texty prostřednictvím `text-webjet9.properties`

## 2020.12

[#43588 - wj9 - základní funkčnost rozvržení] - úkol #1 => Odevzdal jsem první verzi, zprovoznil jsem ji:
- klikněte na logo + `tooltip` s verzí (TODO: bylo by lepší to nějak vypsat, TODO: přidat loga pro NET a LMS)
- přepínání domén (jsou zde pro `iwcm.interway.sk` také ilustrační data), TODO: je to rozbité, `@MHO` je třeba být rozrušený
- Nápověda
- jméno přihlášeného uživatele
- odkaz na odhlášení
- TODO: zbývající ikony v záhlaví

PLUS:
- lombook povolen na projektu - https://projectlombok.org
- vytvořené základní třídy `LayoutBean` a `HeaderBean` s jejich vložením do modelu
- Doplněno `webjet.js` s funkcemi JS WJ.xxx
- základní kostra pro překlad textů, přidáno `WJ.translate`, ale musí se volat jako `async` což zpomaluje zobrazení.

Do budoucna navrhuji předávat překladové klíče z HTML kódu jako parametr funkce JS (v souboru pug se překlad jednoduše vloží jako `#{prekladovy.kluc}` a server je přeložen.
