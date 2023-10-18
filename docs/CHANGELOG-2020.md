# Changelog verzia 2020

## 2020.53

> 2020.53 prináša kompatibilitu s použitím v projektoch klientov. Balík je dostupný na ```artifactory/maven``` serveri ako verzia ```2021.0-SNAPSHOT```. Pri prvom spustení pridáva databázové stĺpce do niektorých tabuliek, zmena je ale spätne kompatibilná a aj naďalej je možné sa vrátiť k verzii WebJET 8.8.

V klientských projektoch stačí nastaviť príslušnú verziu v ```build.gradle```:

```gradle
ext {
    webjetVersion = "2021.0-SNAPSHOT";
}
```

pokusne sme overili základnú funkčnosť na projektoch s MariaDB, Microsoft SQL aj Oracle DB.

**Build Artifactory/Maven verzie**

#52318 [Build súbor](../ant/build.xml) obsahuje viacero ```task``` elementov, finálny je ```deploy```, ktorý má korektne nastavené závislosti, takže stačí spustiť ten. Zoznam ```taskov```:

- ```setup``` - obnoví závislosti a vygeneruje ```WAR``` archív
- ```updatezip``` - pripraví dočasnú štruktúru v ```build/updatezip``` adresári. Štruktúra obsahuje rozbalený ```WAR``` archív, rozbalené ```webjet-XXXX.jar``` súbory (čiže kompletnú štruktúru adresárov /admin, /components a /WEB-INF/classes)
- ```preparesrc``` - stiahne ```SRC``` jar súbor a pripraví štruktúru pre jar archív so zdrojovými súbormi (spojené z jar archívu a zdrojového kódu WebJET 2021)
- ```define-artifact-properties``` - zadefinuje vlastnosti pre generovanie artifaktov, tu sa v ```artifact.version``` nastavuje verzia vygenerovaného artifaktu
- ```makejars``` - pripraví jar archívy tried, /admin a /components adresárov a zdrojových súborov
- ```download``` - pomocná úloha na stiahnutie ```pom``` súborov a jar archívov, ktoré sa nemodifikujú (```struts, daisydiff, jtidy, swagger```)
- ```deploy``` - samotný uloženie artifaktov na ```artrifactory``` server, obsahuje definíciu závislostí (generovanie ```POM``` súboru)

Postup vygenerovania novej verzie:

```shell
cd ant
ant deploy
```

*Poznámka*: v adresári ```build/updatezip``` vznikne rozbalená štruktúra, tú je možné zozipovať a použiť ako aktualizačný balík pre WebJET v starej štruktúre (nepoužívajúcej jar archívy).

**JPA a Spring inicializácia**

JPA a Spring inicializácia je presunutá do package ```sk.iway.webjet.v9```. Z dôvodu spätnej kompatibility sme museli vyriešiť problém s pomenovaním databázových tabuliek ```_adminlog_``` a ```_properties_``` v Oracle DB. Tam sa používa názov ```webjet_adminlog``` a ```webjet_properties```. Meno tabuľky je ale priamo v JPA entitách. Využili sme možnosť použitia interface ```SessionCustomizer```, ktorý je implementovaný v triede [JpaSessionCustomizer](../src/main/java/sk/iway/webjet/v9/JpaSessionCustomizer.java). Ten pri inicializácii JPA prechádza všetky nájdené JPA entity a pre uvedené databázové tabuľky v prípade Oracle inštalácie zmení hodnotu anotácie.

Pre JPA sme pridali korektné nastavenie typu databázy (predchádzajúca verzia mala nastavenú len možnosť MariaDB).

**Úvodná obrazovka**

#47428 Narezaný dizajn úvodnej obrazovky, naplnené dátami. Zatiaľ sú dáta vkladané priamo v [ThymeleafAdminController](../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java), neskôr po finalizácii vlastností bude čítané ajax-om.

Obrazovka je narezaná s využitím VUE komponentov. Využité sú nové DTO objekty pre ```DocDetails, Audit a User beany```.

Dostupný je JavaScript objekt ```window.currentUser```. Pridané VUE filtre pre formátovanie dátumu a času - ```formatDate,formatDateTime,formatDateTimeSeconds,formatTime```.

**Iné zmeny**

- [Skripty] opravený typ auditného záznamu z ```CLIENT_SPECIFIC``` na korektný ```INSERT_SCRIPT```, pridaná metóda ```toString()``` na doc a group beanoch pre lepšie zaznačenie zvoleného obmedzenia na adresáre/stránky v auditnom zázname (zapíše sa ID záznamu a cesta k adresáru/stránke).
- [WebJET] upravená inicializácia na vyhľadanie SpringConfig v package ```sk.iway.webjet.v9```, automatické hľadanie prekladových súborov a autoupdate aj v ```text-webjetXX.properties``` a ```autoupdate-webjetXX.xml``` (pričom webjetXX je hodnota z konfiguračnej premennej ```defaultSkin```).
- ```autoupdate.xml``` - zlepšená kompatibilita s WebJET 8, aby bolo možné spustiť v projekte WebJET 2021 a zároveň sa bez dopadov vrátiť nazad k WebJET 8.8. Pridané sú stĺpce ```id,update_date``` do tabuľky ```_properties_``` a stĺpec ```task_name``` do tabuľky ```crontab```. Stĺpce nekolidujú so starou verziou a nebránia použitiu  administrácie vo verzii 8.8.

**Dokumentácia**

- je nasadená na http://docs.webjetcms.sk/v2021/, zatiaľ nie je vygenerované bočné menu, riešené gradle úlohou ```rsyncDocs```
- doplnená dokumentácia k vkladaniu [HTML kódu bez escapingu](developer/frameworks/thymeleaf.md#základny-výpis-textu--atribútu) v Thymeleaf
- doplnená dokumentácia k [formátovaniu dátumu a času](developer/frameworks/vue.md#formátovanie-dátumu-a-času) vo Vue

<img class="meme" title="meme" src="_media/meme/2020-53.jpg"/>

## 2020.51

**Monitorovanie servera - zaznamenané hodnoty**

Nová verzia zobrazenia zaznamenaných hodnôt monitorovania servera. Zobrazuje tabuľku aj grafy, pričom pre grafy používa agregáciu dát podľa rozsahu dní:

- 0-5 = presná hodnota bez agregácie
- 5-10 = agregácia na 10 minútový interval
- 10-14 = agregácia na 30 minútový interval
- 14-30 = agregácia na hodinu
- 30-60 = agregácia na 4 hodiny
- 60+ = agregácia na 12 hodín

Ak by sa agregácia nevykonala, padol by pravdepodobne prehliadač na množstvo dát v grafe. Implementácia je v triede [MonitoringAggregator](../src/main/java/sk/iway/iwcm/components/monitoring/MonitoringAggregator.java), zaznamenáva sa najvyššia hodnota v danom časovom intervale.

- #50053 [Grafy] - doplnená základná dokumentácia ku knižnici [amcharts](developer/frameworks/amcharts.md).
- #50053 [Grafy] - v ```app.js``` doplnená asynchrónna inicializácia knižnice amcharts volaním ```window.initAmcharts()```.

<img class="meme" title="meme" src="_media/meme/2020-51.jpg"/>

## 2020.50

> 2020.50 prináša korektné generovanie menu položiek administrácie podľa dostupnosti novej verzie admin stránky. V menu tak plynulo prechádzate medzi verziou 8 a 9. V prípade potreby sa viete prepnúť na pôvodnú verziu admin stránky.
>
> Vylepšený je aj editor stránok, automaticky nastavuje svoju výšku podľa veľkosti okna. Pribudli aj ikony stavov adresárov a web stránok v stromovej štruktúre.

**Web stránky**

  - Zmazaný pôvodný ukážkový kód editácie stránky a adresára, už sa používajú len datatabuľkové zobrazenia.
  - Doplnené stavové ikony v stromovej štruktúre podľa pôvodného návrhu pre adresár (interný, nezobraziť v menu, zaheslovaný) a web stránku (vypnuté zobrazenie, presmerovaná, nevyhladateľná, zaheslovaná).
  - Dialóg editora má automatický počítanú výšku podľa veľkosti okna (ale minimálne 300 bodov). Výška sa prepočítava každé 3 sekundy.
  - Doplnené je kliknutie na web stránku v stromovej štruktúre. To je pomerne komplikované, keďže editor je integrovaný s datatabuľkou. Najskôr sa teda overí, či je v datatabuľke zobrazený adresár v ktorom sa stránka nachádza. Ak nie, odloží sa hodnota ```docId``` do premennej ```docidAfterLoad```. Tá sa kontroluje po načítaní zoznamu stránok adresára, ak je nastavená automaticky sa vyvolá editácia danej stránky.

**Generovanie menu**

- #51997 [Navigácia] Upravené generovanie menu položiek. Vo verzii v8 aj v9 sa generujú linky na už prerobené stránky vo v9, takže sa dá transparentne prechádzať medzi starým a novým WebJETom. Pri požiadavke na zobrazenie starej v8 verzie je možné do URL adresy zadať ```/admin/v8``` čo prepne generovanie menu do starej verzie a je možné zobraziť starú verziu administrácie. Do v9 sa dostanete zavolaním URL adresy ```/admin/v9```. Pre prechod do v8 je aj ikona v hornej časti stránky.

**Dokumentácia**

Doplnená dokumentácia k použitiu [jsTree](developer/jstree/README.md).

## 2020.49

**Import/Export:**

Implementovaný systém pre import a export dát medzi datatabuľkami.

Export:

- Umožňuje export do **Excelu(xlsx), PDF a priama tlač na tlačiareň**
- Súboru sa nastaví meno podľa aktuálneho ```title``` stránky a automaticky sa doplní aktuálny dátum a čas.
- Exportu je možné nastaviť typ dát (**aktuálna strana/všetky, filtrované/všetky/označené riadky, zoradenie, stĺpce**).
- Pri serverovom stránkovaní sa najskôr vykoná volanie REST služby, z ktorej sa získa **maximálne 50 000 záznamov**. Pri potrebe exportovať viac záznamov použite viacnásobné exportovanie s využitím filtrovania.
- Spracovanie serverových dát prebieha mimo datatabuľky (kvôli výkonu), nanovo sa vykresľujú ```datetime``` stĺpce a ```radio button``` (podporované sú ```date-time```, ```date```, ```*-select```, ```*-boolean```).
- Pri exporte sa v prvom riadku pripraví zoznam stĺpcov, **import následne nie je citlivý na poradie stĺpcov**.
- Pre **výberové polia** (select/číselníky) sa **exportuje textová hodnota** a pri importe sa nazad rekonštruuje na ID. Umožňuje to mať **rozdielne ID naviazaných záznamov** medzi prostrediami (napr. ID šablóny pre web stránku), ak sa zhoduje meno, korektne sa záznam spáruje. V exporte je následne aj ľudsky zrozumiteľný text namiesto ID.

Import:

- Umožňuje **importovať dáta ako nové** (doplnia sa do databázy) alebo **párovať existujúce dáta podľa zvoleného stĺpca** (napr. meno, URL adresa a podobne). Pri párovaní automaticky vyhľadá najskôr záznam v databáze a následne ho aktualizuje. Ak neexistuje, vytvorí nový záznam.
- **Importuje sa z formátu xlsx**.
- Import sa vykonáva **postupne v dávkach po 25 záznamoch**, aby nebol zaťažený server.

**Datatables:**

- Upravené filtrovanie podľa dátumov - do UI pridaná možnosť nastavenia času, na backende už nie je potrebné nastavovať čas konca dňa
- Pridané atribúty (potrebujeme ich pre volanie REST služby pre export dát):
  - ```DATA.jsonOptions``` - nastavenie z odpovede REST služby pre vykreslenie (potrebujeme pre export číselníkových dáta)
  - ```DATA.urlLatest``` - kompletná URL adresa posledného REST volania
  - ```DATA.urlLatestParams``` - všetky parametre posledného REST volania (aktuálna stránka, veľkosť stránky, filtre)

## 2020.45

- #47293 [Mazanie záznamov v databáze] Prerobená verzia mazania dát z WJ8 do datatable. Využíva niekoľko hackov, ako nastavenie [externého filtra](developer/datatables/README.md#externý-filter), vypnutie stránkovania (option paging)

**Datatables:**

- Pridaná možnosť **nezobraziť filter** v hlavičke tabuľky pre stĺpec nastavením atribútu ```filter=false``` v anotácii
- Pridaná možnosť **vypnúť stránkovanie** pomocou ```options paging: false``` [dokumentácia](developer/datatables/README.md#možnosti-konfigurácie)

## 2020.44

> Verzia 2020.44 prináša **novú dokumentáciu** k [WebJET JavaScript funkciám](developer/frameworks/webjetjs.md), [Automatizovanému testovaniu datatabuľky](developer/testing/datatable.md), pridávame príklad pre [Dynamické zmenu hodnôt vo výberovom poli v Datatables Editore](developer/datatables-editor/README.md#dynamická-zmena-hodnôt-vo-výberovom-poli) a iné menšie zmeny.
>
> Do WebJET 2021 sme preniesli aplikáciu **Mazanie cache objektov** a dokončili sme aplikáciu **Šablóny**. V nej pribudol výber JSP súboru šablóny na serveri s dynamickým načítaním na základe vybranej skupiny šablón a nastavenie adresárov pre zobrazenie šablóny. Pridali sme aj [automatizované testy](../src/test/webapp/tests/components/templates.js).
>
> Aplikáciu **Monitorovanie servera/aktuálne hodnoty** sme vizuálne doladili (odsadenia, zobrazenie v mobilnej verzii) a upravili sme živé dáta v grafoch. Teraz sa korektne posúvajú v reálnom čase.
>
> Do datatable sme pridali možnosť [potvrdenia vykonanej akcie](developer/datatables/README.md#tlačidlo-pre-vykonanie-serverovej-akcie) pre špeciálne tlačidlá v datatabuľke (ako napr. zmazať všetko).

- [VS Code] upravené [tasks.json](../.vscode/tasks.json) a [launch.json](../.vscode/launch.json) pridaním debug konfigurácie ```Debug```. Tá spustí aplikačný server a následne sa pripojí v debug režime. Žiaľ kvôli [chybe v gretty](https://github.com/gretty-gradle-plugin/gretty/issues/166) je vypnutie riešené ```killnutím``` procesu (zatiaľ neotestované na windows). Vo VS Code teda priamo v záložke Run viete spustiť debug konfiguráciu "Debug".
- [Layout] Vizuálne opravené zobrazenie výberu domény v pravej hornej časti
- #47293 [toastr] Refaktorovaný kód používajúci priamo toastr na API volania [WJ.notify](developer/frameworks/webjetjs.md#notifikácie)

**Datatables:**

- #47293 [Datatable] Upravená funkcia ```TABLE.executeAction``` pre možnosť potvrdenia akcie pred vykonaním
- #45718 [Datatable Editor] Vypnuté odosielanie editora po stlačení Enter nastavením konfigurácie ```onReturn: false```. Robilo to problém vo filtri výberových polí, kde po stlačení Enter sa nastavila hodnota a aj sa odoslal formulár.
- #47293 [Datatables] Upravený kód [DatatableRestControllerV2.action](../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) pre možnosť spustenia akcie aj pre nezadanú entitu (null hodnota). Používa sa pre akcie typy "Zmazať všetko"

**Mazanie Cache objektov:**

- #47293 Prerobená verzia z WebJET 8 do datatable. Cache objekty ale nie sú Spring DATA repozitár. Implementovaný je vlastný [service](../src/main/java/sk/iway/iwcm/components/memory_cleanup/cache_objects/CacheObjectsService.java), ktorý zabezpečuje API pre prístup k Cache triede a zoznamu objektov.
- #47293 V triede [CacheObjectsService](../src/main/java/sk/iway/iwcm/components/memory_cleanup/cache_objects/CacheObjectsService.java) sú implementované metódy ```deleteAllCacheBeans()``` pre zmazanie celej cache vrátane DocDB, GroupsDB a iných objektov. Metóda ```deletePictureCache()``` implementuje zmazanie cache ```/thumb``` obrázkov.

**Šablóny:**

- #45718 [Šablóny] pridaná [REST služba /forward](../src/main/java/sk/iway/iwcm/components/templates/TemplatesController.java) pre výber JSP šablóny so zoznamu súborov v ```/templates``` adresári
- #45718 [Šablóny] pridaná možnosť nastavovania adresárov, v ktorých sa majú šablóny zobrazovať

**Testovanie:**

- [Testovanie] Refaktorovaný kód automatického testu [DataTables.js](../src/test/webapp/pages/DataTables.js) z parametrického konštruktora na ```options``` objekt. Doplnená možnosť pridania krokov testu pre nový záznam, editáciu, vyhľadanie aj zmazanie záznamov. Upravené čakanie z fixného ```I.wait(1)``` na ```DTE.waitForLoader()```, doplnené ```I.say``` pre lepšiu orientáciu v logu testu. Test vyhľadávania doplnený o overenie nájdených záznamov.
- [Testovanie] Pridaná šablóna ```default``` pre možnosť testovania výberu JSP súborov šablón

**Dokumentácia:**

- [Datatable] Doplnená dokumentácia k [tlačidlu pre vykonanie serverovej akcie](developer/datatables/README.md#tlačidlo-pre-vykonanie-serverovej-akcie) (napr. rotácia obrázku, zmazanie všetkých záznamov)
- Doplnená dokumentácia k použitiu [Gitlabu vo VS Code](developer/guildelines/gitlab.md#práca-vo-vs-code)
- Pridaná dokumentácia [WebJET JavaScript funkcií](developer/frameworks/webjetjs.md) pre zobrazenie notifikácie, potvrdenia akcie a ďalších. Refaktorovaný ich kód pre lepšie použitie.
- Doplnený zoznam [špeciálnych funkcií](developer/testing/README.md) pre testovanie datatabuľky
- Pridaný opis použitia a implementácie [Automatizovaného testovania datatabuľky](developer/testing/datatable.md)
- #45718 pridaný návod na [dynamickú zmenu hodnôt v select boxe](developer/datatables-editor/README.md#dynamická-zmena-hodnôt-vo-výberovom-poli) v Datatable Editore podľa iných polí

## 2020.43

- [Layout] Upravené generovanie breadcrumb navigácie - pôvodné elementy sa skladali z názvu ```pills-#tab[1]```, znak # je teraz z textu odstránený, čiže teraz sa generuje ako ```pills-tab[0]```
- #47419 [Monitorovanie servera] - upravené vizuálne zobrazenie (odsadenia), prepracovaný spôsob zobrazenia čiar v grafe Voľného miesta na disku (zosúladenie medzi hodnotami v GB a pozíciami grafu)
- #47419 [Monitorovanie servera] - zväčšený čas zobrazený na grafe aktuálnych hodnôt na 10 minút, doplnené popisy, presunuté nastavenie intervalu do hlavičky, presunuté preklady to ```properties``` súboru

## 2020.42

> Verzia 2020.42 prináša najmä [automatický spôsob auditovania JPA entít](developer/backend/auditing.md). Jednoduchou anotáciou zabezpečíte vytvorenie auditných záznamov pri každej manipulácii s entitou. Auditný záznam obsahuje aj zoznam zmenených atribútov (vo forme stará hodnota -> nová hodnota), respektíve zoznam všetkých atribútov pri vytvorení a zmazaní entity.
>
> Rozšírili a zdokumentovali sme možnosti JSON poľa pre [pridávanie adresárov a web stránok](developer/datatables-editor/field-json.md) v datatabuľkách.
>
> Datatabuľkám sme pridali možnosť nastavenia [predvoleného spôsobu usporiadania](developer/datatables/README.md#usporiadanie) a dátumovo-časovým poliam sme pridali aj zobrazenie sekúnd.
>
> Do WebJETu sme pridali knižnicu ```oshi-core``` pre získanie záťaže CPU a amcharts pre zobrazenie grafov v Monitorovaní servera.
>
> Rozšírili sme dokumentáciu o sekciu s popisom [Spring frameworku](developer/frameworks/spring.md), pridali sme sekciu [riešenie problémov](developer/troubles/README.md), upravili [príklady](developer/frameworks/example.md) a doplnili dokumentáciu k [testovaniu](developer/testing/README.md).

- #46891 [Testovanie] - upravené existujúce testy na nové vlastnosti a nastavenia tabuliek, v ```audit-search.js``` upravené fixné čakanie na dynamické pomocou ```DT.waitForLoader```, v ```forms.js``` prerobené testovanie hľadania podľa dátumov
- #46891 [Datatable] - pridané tlačidlo pre znova načítanie (obnovu) údajov tabuľky zo servera
- #46891 [Audit] - pridané zobrazenie stĺpca s ID entity
- #46891 [Prekladové kľúče] - opravené vyhľadávanie podľa dátumu zmeny
- #46891 [Auditing] - zapnuté auditovanie JPA entít ```FormsEntity, GalleryDimension, GallleryEntity, InsertScriptBean, InsertScriptDocBean, InsertScriptGroupBean, TranslationkeyEntity, AuditNotifyEntity```
- #46891 [Auditing] - premenovaná tabuľka ```webjet_audit``` na ```webjet_adminlog``` podľa pôvodného názvu v Oracle
- #46891 [Testovanie] - pridané [rozšírenie codeceptjs-chai](developer/testing/README.md#assert-knižnica) pre jednoduché písanie ```assert``` podmienok
- #46891 [Dokumentácia] - pridaná dokumentácia k [auditingu](developer/backend/auditing.md), [riešeniu problémov](developer/troubles/README.md) a doplnené informácie k [testovaniu](developer/testing/README.md#webjet-doplnkové-funkcie)
- upravené nastavenie v ```build.gradle``` ```options.encoding=utf-8```, opravená diakritika v ```PathFilter```
- #46891 [Testovanie] - pridané testovanie auditného záznamu do štandardného volania ```DataTables.baseTest('domainRedirectTable');```
- #46891 [Datatable] - pridaná možnosť nastavenia [spôsobu usporiadania](developer/datatables/README.md#usporiadanie) pri zobrazení stránky, nastavené usporiadanie pre konfiguráciu (podľa dátumu zmeny),
- #46891 [Datatable] - pridané sekundy k formátovaniu dátumu a času
- #46891 [Audit] - pridané auditovanie exportu datatabuľky

## 2020.41

- [Dokumentácia] upravená dokumentácia [Ukážkový kód](developer/frameworks/example.md) - PUG upravený na použitie anotácií, doplnené príklady
- [Dokumentácia] doplnené základné informácie k [EclipseLink JPA, Spring DATA a Spring REST](developer/frameworks/spring.md)

## 2020.40

- #47419 [Monitorovanie servera] modul zobrazuje aktuálne hodnoty monitorovania servera, vrátane grafov CPU a pamäte
- #47419 [Monitorovanie servera] pridaná knižnica [oshi-core](https://github.com/oshi/oshi) pre získanie údajov zaťaženia CPU
- #47419 [Monitorovanie servera] pridaná knižnica [amcharts](https://www.amcharts.com/) pre zobrazovanie grafov, použitá je komerčná licencia
- #46261 [Skripty] modul doplnený o možnosť nastavenia adresára, alebo web stránky pre skript
- #46261 [DTED json field] refaktorovaný kód [dátového poľa JSON pre DTED](developer/datatables-editor/field-json.md). Doplnená možnosť konverzie JSON objektov a textu tlačítka, vytvorená dokumentácia.
- #47293 [Datatable] pridaná možnosť zobraziť vlastné tlačidlo iba keď je vybraný nejaký riadok pomocou kódu ```$.fn.dataTable.Buttons.showIfRowSelected(this, dt);``` [dokumentácia](developer/datatables/README.md)
- #47293 [Datatable] pridaná podpora ```range``` aj na číselné hodnoty, prefix je ```range:```, funguje podobne ako prefix ```daterange:```
- #47293 [Persistent cache] zobrazenie záznamov cez datatable, Spring DATA

## 2020.39

> V tomto vydaní sme do datatabuliek pridali možnosti **obnovenia tabuľky** po volaní REST služby (napr. vo web stránkach keď sa stránka presunie do iného adresára), možnosť ```fetchOnEdit``` pre **obnovenie editovaného záznamu** pred zobrazením editora a ```fetchOnCreate``` pre **získanie dát pre nový záznam** (napr. pre web stránku nastaviť šablónu, poradie usporiadania).
>
> Do Java anotácií sme pridali možnosť **skrytia stĺpca** v tabuľke alebo v editore pomocou parametrov ```hidden``` alebo ```hiddenEditor```.
>
> Pridali sme tiež **editovanie JSON objektov**, zatiaľ je napojené na editáciu adresára a zoznam adresárov (napr. pre web stránku ako ```GroupDetails``` objekty). Implementované to je ako VUE komponenta.

- #47341 - všetko nižšie sa týka branche/tiketu #47341
- [Web stránky] Sfunkčnené nastavovanie adresára a kópie stránky v adresároch. Riešené pomocou VUE komponenty [vue-folder-tree.vue](../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/folder-tree/vue-folder-tree.vue). Využíva sa JSTree otvorené do celo obrazovkového dialógu. V anotácii ```@DatatableColumn``` sa zapisuje ako ```DataTableColumnType.JSON``` kde pomocou atribútu ```className = "dt-tree-group"``` sa nastaví výber jedného adresára alebo pomocou ```className = "dt-tree-group-array"``` výber viacerých adresárov. Objekty sú typu ```GroupDetails```.
- [Web stránky] - vyriešený presun stránky do iného adresára s vyvolaním obnovenia datatabuľky a stromovej štruktúry stránok
- [Web stránky] - doplnené mazanie web stránok
- [Datatables] Doplnená možnosť [aktualizácie datatabuľky](developer/datatables/restcontroller.md#reload-datatabuľky) po uložení dát (napr. keď sa web stránka presunie do iného adresára). Pridaná metóda ```setForceReload(boolean)``` do REST controllera a JavaScript udalosti ```WJ.DTE.forceReload```.
- [Datatables] Pridaná možnosť nezobrazenia stĺpca v datatabuľke alebo v editore parametrom ```hidden``` (pre nezobrazenie v datatabuľke) alebo ```hiddenEditor``` (pre nezobrazenie v editore) anotácie [@DatatableColumn](developer/datatables-editor/datatable-columns.md#vlasnosti-datatablecolumn). Na rozdiel od parametra ```visible``` nie je takéto stĺpce možné zobraziť ani cez nastavenia datatabuľky na frontende.
- [Datatables] Pridaný inicializačný parameter ```fetchOnEdit``` - po nastavení na true bude pred editáciou záznamu vykonané REST volanie pre získanie aktuánych dát editovaného záznamu. Pri použití datatabuľky napr. pre web stránky sa pred otvorením editora aktualizuje daný záznam zo servera a do editora sa teda otvorí vždy najnovšia verzia. Implementované cez JS funkciu ```refreshRow``` a zákaznícke tlačítko ```$.fn.dataTable.ext.buttons.editRefresh``` ktorým sa nahradí štandardné tlačítko ```edit```.
- [Datatables] Pridaný inicializačný parameter ```fetchOnCreate``` - po nastavení na ```true``` bude pred vytvorením nového záznamu vykonané REST volanie s hodnotou -1 pre získanie dát nového objektu. Výhodné to je pre nastavenie predvolených údajov pre nový záznam. Napr. pre web stránku sa pred nastaví adresár, poradie usporiadania, šablóna a podobne.
- [Datatables] Pridané automatické zobrazenie ID záznamu v prvom stĺpci, kde doteraz boli len výberové polia na označenie riadku
- [Datatables] Upravené zobrazenie chybového hlásenia zo servera tak, aby nebolo medzi tlačítkami Zrušiť a Uložiť ale naľavo od nich.

- [Testovanie] pridaná funkcia ```DT.waitForLoader```, ktorá čaká na zobrazenie a následné schovanie informácie "Spracúvam" v datatabuľke. Používa sa ako ```DT.waitForLoader("#forms-list_processing");```

## 2020.36

- #44968 [Prekladové kľúče] Upravené ukladanie prekladu po zmene jazyka - vytvorí sa kópia prekladového kľúča, pridané ukladanie dátumu poslednej zmeny, opravené testy
- #44968 [Datatables] Pridaná možnosť nezobraziť stĺpec v datatabuľke pomocou anotácie ```hidden=true```, takýto stĺpec na rozdiel od ```visible``` sa nedá ani v nastaveniach stĺpcov datatabuľky zobraziť. Môže ale naďalej byť použitý v editore, pokiaľ má anotáciu pre editor.

## 2020.34

- #47008 [Datatables] Pridané automatické generovanie ```required``` atribútu v ```columns``` definícii ak má pole anotáciu ```@NotEmpty``` alebo ```@NotBlank```. Implementované v [DatatableColumnEditor.java](../src/main/java/sk/iway/iwcm/system/datatable/json/DataTableColumnEditor.java).
- #47008 [Automatizované testovanie] Pridaný [automatizovaný test](../src/test/webapp/pages/DataTables.js) datatabuľky s čítaním definície priamo z ```columns``` objektu. Realizuje štandardný CRUD test. Ukážkové použitie v [redirects.js](../src/test/webapp/tests/components/redirects.js):

```javascript
const requiredFields = ['oldUrl', 'newUrl'];
DataTables.baseTest('redirectTable', requiredFields);
```

## 2020.33

- #47341 [Datatables] - Pridaný nový typ poľa JSON, zatiaľ sa zobrazuje ako ```textarea``` ale pri čítaní a zapisovaní sa dáta posielajú korektne ako JSON objekty. Implementované ako nový field type json v index.js v kóde ```$.fn.dataTable.Editor.fieldTypes.json```
- #47425 [Datatables] - Opravené hromadné označenie riadkov a volanie zmazania na serveri
- #44890 [Testy] - [pridaný návod](developer/testing/README.md#čakanie-na-dokončenie) na použitie ```I.waitFor*``` namiesto fixného ```I.wait```. Upravený test [webpages.js](../src/test/webapp/tests/components/webpages.js) na použitie ```waitFor``` a ```within```, keďže stránka obsahuje dve datatabuľky.
- #44890 [Datatables Editor] - Pridaný prenos CSS triedy z atribútu ```className``` anotácie na celý riadok v editore div.DTE_Field. Viď komentár ```//prenesieme hodnotu className aj do DT editora do prislusneho riadku``` v [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js).
- #44890 [Datatables] - Upravené porovnanie ```true/false``` hodnôt pre výberové pole. Podobne ako pre začiarkávacie pole sa musí porovnávať nereťazcová hodnota ```true``` (viď komentár ```ak mame true/false select box, aj tomu musime zmenit value na true/false namiesto String hodnot``` v [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)).

## 2020.32

- #44890 [Datatables] - Upravená funkčnosť ```BOOLEAN``` ```dt-format-boolean-true```, v JSON objekte je hodnota prenášaná ako ```true``` ale v ```options``` objekte ako ```"true"``` (reťazec). Nefungovalo teda porovnanie a nastavenie hodnoty. Upravený kód tak, že sa ```editor.options``` zmenia z ```"true" na true``` hodnotu. Následne už funguje porovnanie s objektom a správne nastavenie ```checkboxu```. Upravené aj filtrovanie pri klientskom stránkovaní, kde sa neporovnáva label ale value hodnota (viď komentár ```//neplati pre column-type-boolean``` v [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)).
- #44890 [Datatables] - Doplnený prenos data atribútov aj na polia typu ```checkbox``` a ```radio``` (podmienka ```"div.DTE_Field_Type_"+data.editor.type```). Nastavia sa na prvý element v zozname. Umožňujú nastavovať atribúty ako ```data-dt-field-hr``` alebo ```data-dt-field-headline```.
- #44890 [Dátum a čas] - Upravené správanie sa hodnoty 0 v dátume vo formáte primitívneho typu ```long```. Pre hodnotu 0 sa zobrazí prázdny dátum a čas namiesto 1.1.1970. Upravené v ```renderDate``` v [datatables-config.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-config.js).
- #44890 [Datatables] - Upravené generovanie ```Options``` polí pre editor pomocou [LabelValue](../src/main/java/sk/iway/iwcm/system/datatable/json/LabelValue.java) objektu.

## 2020.31

- #44890 [Datatables] - Pridaná možnosť v anotácii nastaviť usporiadanie polí vo výstupnom JSON objekte pomocou atribútu ```sortAfter``` v anotácii ```@DatatableColumn```. Sortovanie sa vykonáva v triede [DatatableColumnsFactory.java](../src/main/java/sk/iway/iwcm/system/datatable/DataTableColumnsFactory.java) v metóde ```sortColumns```.
- #44890 [Datatables] - Pridané možnosti anotácie ```DataTableColumnType.BOOLEAN``` pre true/false hodnoty a ```DataTableColumnType.CHECKBOX``` pre štandardné začiarkávacie polia.
- #44890 [Datatables] - Pridaná možnosť anotácií [vnorených objektov](developer/datatables-editor/datatable-columns.md#vnorené-atribúty) pomocou ```@DatatableColumnNested```. Atribútom prefix (predvolená hodnota je auto, čo vygeneruje prefix podľa mena anotovaného fieldu) je možné určiť prefix atribútu vo vygenerovanom JSON objekte (napr. nastavením na hodnotu ```editorFields``` sa do JSONu vygeneruje pole ```allowChangeUrl``` ako ```editorFields.allowChangeUrl```). Prefix môže byť aj prázdny, vtedy sa vygeneruje len meno premennej (ak má napr. ```gettery/settery``` v pôvodnom objekte ako sú napr. [FieldsFromAtoE](../src/main/java/sk/iway/iwcm/doc/FieldsFromAtoE.java) v [DocDetails](../src/main/java/sk/iway/iwcm/doc/DocDetails.java) objekte.)

## 2020.30

- #44890 [Datatables] - Vyriešené vkladanie viacerých datatables do web stránky. Každej tabuľke sa nastaví HTML atribút ID aj keď nie je v ```options``` (predvolene datatableInit). ID atribút sa následne vkladá aj do ďalších selektorov pre správne mapovanie filtrov a tlačidiel na správny datatable. Upravené funkcie ```$.fn.dataTable.Editor.display.bootstrap.init``` a ```$.fn.dataTable.Editor.display.bootstrap.open``` pre podporu viacerých DT editorov v stránke.

## 2020.29

- #346264 [Web stránky] Pridaná možnosť pridávať/editovať adresáre v stromovej štruktúre web stránok. Implementované pomocou schovanej datatabuľky a DT editora.
- #45763 [Stromová štruktúra] Pridané API na [editáciu stromovej štruktúry](developer/jstree/README.md), použité v sekcii web stránky a foto galéria. Je už možné presúvať položky v adresárovej štruktúre web stránok a presúvať adresáre v galérii.
- #44836 [Web stránky] Doplnené polia do DT editora, backend ich zatiaľ nespracováva
- #47125 [Úlohy na pozadí] Prerobené do datatables, pridaný stĺpec s menom úlohy do databázy

## 2020.28

- #45532 [Formuláre] Pridaný automatizovaný vyhľadávania
- #45409 [Audit] Pridané testy filtrovania
- [Datatables] pre začiarkávacie pole automaticky nastavený atribút ```falseValue``` na nie (ak nie je zadaný)
- Opravené nastavenie diakritiky pre JS súbory z ```/admin/v9/dist/``` adresára (na výstup posielané v UTF-8)

## 2020.27

- #46264 [standalone datatables editor] Pridaná možnosť označiť datatabuľku ako schovanú ([atribút hideTable v options](developer/datatables/README.md)) pre možnosť ```standalone``` DT editora pre jstree
- #45721 [Skupiny šablón] Upravené zobrazenie v datatabuľke na použitie anotácii, pridaný test

## 2020.26

- #43588 [Datatables] Opravené opakované nastavenie select boxu vo filtri a v editore (po druhom nastavení nešli vybrať hodnoty)
- #45532 [Formuláre] Pridaná možnosť základného vyhľadávania v dátach formulárov, limitované na maximálne 6 stĺpcov (z dôvodu API Spring DATA)
- #45718 [Šablóny] Verzia zoznamu šablón cez datatabuľku, využíva ```TemplatesDB``` API
- #45490 [Audit] Zobrazenie záznamov auditu a notifikácií. Obe cez Spring DATA, pre audit doplnené select boxy pre filtrovanie v datatabuľke (napojené na options editora), doplnená možnosť [custom filtrovania](./datatables/restcontroller.md#špeciálne-vyhľadávanie) v REST controlleri.
- #44968 [Prekladové kľúče] Zobrazenie a editácia prekladových kľúčov, implementované čítanie pôvodných hodnôt, mapovanie ```.properties``` a DB zmien.

## 2020.25

- #45532 [Formuláre] Zobrazenie zoznamu formulárov a dát formulárov (dynamické dáta) cez datatabuľku.
- #45679 [Datatables] Anotácie pre generovanie ```columns``` konfigurácie prenesené do WebJET 8 pre možnosť anotácie existujúcich beanov
- #45382 [Datatables] Import a export dát - úprava dialógu importu dát

## 2020.23

- #45685 [CI-CD] Nakonfigurovaná CI-CD pipeline na Gitlab serveri, nočný build z master branche je dostupný na http://demotest.webjetcms.sk/admin/
- #45382 [Datatables] Iniciálna verzia importu dát formou postupného posielania dát z importného excelu do datatables editora, možnosť nastavenia režimu importu (zatiaľ chýba podpora na backende)
- #45379 [Datatables] Refaktorovaný kód prispôsobenia datatabuľky z ```app.js``` do samostatného súboru [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) s možnosťou použitia aj ako [NPM modulu](../src/main/webapp/admin/v9/npm_packages/README.md) (napr. na frontende v šablóne). Refaktorovaný kód pre možnosť použitia viacerých datatabuliek na jednej stránke.
- #45679 [Datatables] Pridaná možnosť [generovať columns konfiguráciu](developer/datatables-editor/datatable-columns.md) datatabuľky pomocou anotácii Java beanov

## 2020.22

- #44836 [Web stránky] Zmigrovaný kód úprav v ckeditore z WJ8. Upravené sú vkladanie obrázkov, formuláre a ďalšie CK editor dialógy. Doplnený skrytý formulár obsahujúci ```docid, groupid, virtualpath``` pre kompatibilitu náhľadu aplikácie a dialógov z WJ8.
- #44836 [Web stránky] Pre editor sa v ```options``` pošle ```original``` template objekt, ktorý je potrebný na získanie ciest k CSS štýlom. Pri otvorení DT editora sú zachované CSS začínajúce na /admin (pre CK editor rozšírenia), zvyšné sa zmenia na základe priradenej šablóny.
- #44836 [Web stránky] DT editor má dostupný priamo [JSON objekt](./datatables-editor/README.md#ukážky-kódu) editovaného záznamu (nie všetkých dát). Pre ```options``` číselníky doplnená možnosť posielania [original objektu](./datatables/restcontroller.md). JSON objekt získaný zo servera (obsahujúci kompletné dáta všetkých riadkov a options property) je dostupný v DT ako DATA.json.
- #44737 [Konfigurácia] Doplnený [autotest](../src/test/webapp/tests/components/configuration.js).
- #44293 [Doménové presmerovania] Doplnený [autotest](../src/test/webapp/tests/components/domain-redirects.js).

## 2020.21

- [Testovanie] Pridané CSS triedy ```dt-filter-MENO``` na filter poliach v datatabuľke pre možnosť nastavenia filtrovania v automatizovanom teste ```I.fillField("input.dt-filter-newUrl", randomNumber);```. Príklad v teste [redirects.js](../src/test/webapp/tests/components/redirects.js)
- [Presmerovania] Doplnené [anotácie povinných polí](./datatables/restcontroller.md#valid%c3%a1cia--povinn%c3%a9-polia) na UrlRedirectBean.java (vo WJ8)
- [Galéria] Doplnené preklady textov pre editor obrázkov a oblasť záujmu, pridaná možnosť nastavenia oblasti záujmu do vstupných polí, pridaný [auto test](../src/test/webapp/tests/components/gallery.js)
- [Dokumentácia] Pridaný [ukážkový kód](developer/frameworks/example.md) využitia frameworkov
- [Testovanie] Pridané [generovanie reportov](./testing/README.md#generovanie-html-reportu) cez mochawesome a CodeceptUI pre [ovládanie testov](./testing/README.md#codecept-ui) cez prehliadač
- [Datatable] Pridaná možnosť zobrazenia dát (stĺpcov a polí v editore) [na základe práv](./datatables/README.md#zobrazenie-dát-na-základe-práv) prihláseného používateľa

## 2020.20

- [Datatable] Pridané [vyhľadávanie podľa rozsahu dátumov](./datatables/restcontroller.md#vyh%c4%bead%c3%a1vanie-pod%c4%bea-rozsahu-d%c3%a1tumov) špecifickým spracovaním URL parametra s prefixom ```daterange:od-do```.
- [Dokumentácia] Doplnený manuál použitia [assert](./testing/README.md#assert-kni%c5%benica) knižnice, použitia a písania nových [prekladových kľúčov](./frameworks/thymeleaf.md#prekladov%c3%bd-text)
- [Galéria] Upravené zobrazovanie stĺpcov jazykových mutácii obrázka na využitie API datatables (aby korektne fungovala aj inline editácia)
- [Prekladové kľúče] Nová verzia zobrazenia a editácie [prekladových kľúčov](../src/main/webapp/admin/v9/views/pages/settings/translation-keys.pug)
- [Datatable] Refaktorovaný kód [DatatableRestControllerV2.java](../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) s komentovaným rozdelením na entitné a restové metódy
- [Konfigurácia] Nová verzia zobrazenia [konfigurácie](../src/main/webapp/admin/v9/views/pages/settings/configuration.pug) WebJETu
- [Dokumentácia] Doplnená dokumentácia k [SFC Vue](developer/frameworks/vue.md)
- [Fotogaléria] Doplnené napojenie [editora obrázkov](../src/main/webapp/admin/v9/src/js/image-editor.js) na ```chunked upload``` a toastr notifikácie
- [Presmerovania] Nová verzia zobrazenia [presmerovaní](../src/main/webapp/admin/v9/views/pages/settings/redirect.pug)
- [Fotogaléria] Zmenený doplnok JCrop pre označenie oblasti záujmu na ```vue-advanced-cropper```

## 2020.19

- [Testovanie] Základný koncept [automatizovaného testovania](developer/testing/README.md)
- [Dokumentácia] Rozšírenie [dokumentácie](README.md)
- [Web stránky] Iniciálna verzia integrácie [ckeditora do datatabuľky](../src/main/webapp/admin/v9/src/js/datatables-ckeditor.js) a zobrazenie [web stránok](../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug)
- [Datatable] Opravený problém s formátom dátumov, automatické nastavenie podľa ```window.userLang``` na moment knižnici - nastavenie ```wireformat``` (predvolene x) a formát atribútov v editore(predvolene L HH:mm)
- [Galéria] Doplnená možnosť pridať a [upraviť adresár](../src/main/webapp/admin/v9/views/partials/folder-tree-galleria.pug), napojené na Vue

## 2020.18

- [Doménové presmerovania] Nová verzia zobrazenia [doménových presmerovaní](../src/main/webapp/admin/v9/views/pages/settings/domain-redirect.pug)
- [Bezpečnosť] Bezpečnosť volania všetkých rest služieb je zabezpečená [automatickým nastavením](../src/main/webapp/admin/v9/views/partials/head.pug) CSRF tokenu pre všetky ajax požiadavky
- [Upload] Vyriešené volanie korektného spracovania súborov po ich nahratí a API pre zmazanie súboru (vyčistenie galérie, video náhľadov, fulltext indexu)
- [Galéria] Pridaný [editor obrázkov](../src/main/webapp/admin/v9/src/js/image-editor.js)
- [Web stránky] Základná implementácia zobrazenia [stromovej štruktúry](../src/main/java/sk/iway/iwcm/admin/layout/JsTreeItem.java) stránok

## 2020.13
[#43588 - wj9 - sfunkčnenie základného rozloženia] - úloha #5 =>

- upravená lombook konfigurácia (v build.gradle - ```config['lombok.accessors.chain'] = 'true'```) aby ```settre``` vracali ```this``` pre reťazenie
- webpack nastavený tak, aby neodstraňoval úvodzovky v atribútoch (lebo TH môže vrátiť viac slovný výraz - ```removeAttributeQuotes: false```)
- MENU - doplnené objekty MenuBean a MenuService ktoré generujú menu, je spravený preklad pôvodných ModuleInfo z ```Modules``` objektu (skupiny, ikony) + upravené texty cez ```text-webjet9.properties```

## 2020.12

[#43588 - wj9 - sfunkčnenie základného rozloženia] - úloha #1 =>
Commitol som prvú verziu, sfunkčnené je:

- klik na logo + ```tooltip``` s verziou (TODO: to by chcelo ešte nejako lepšie vypísať, TODO: doplniť logá za NET a LMS)
- prepnutie domény (sú tam pre ```iwcm.interway.sk``` aj ilustračné dáta), TODO: je to rozbité, ```@MHO``` treba naštýlovať
- pomocník
- meno prihláseného používateľa
- odhlasovací odkaz
- TODO: zvyšné ikony v hlavičke

PLUS:

- na projekte zapnutý lombook - https://projectlombok.org
- spravené základné triedy ```LayoutBean``` a ```HeaderBean``` s ich vložením do modelu
- doplnené ```webjet.js``` s JS funkciami WJ.xxx
- základný skelet na preklad textov, pridaná ```WJ.translate```, musí sa to ale volať ako ```async```, čo spomaľuje zobrazenie.

Navrhujem v budúcnosti prekladové kľúče prenášať z HTML kódu ako parameter JS funkcie (v pug súbore sa preklad vloží jednoducho ako ```#{prekladovy.kluc}``` a na serveri sa preloží.