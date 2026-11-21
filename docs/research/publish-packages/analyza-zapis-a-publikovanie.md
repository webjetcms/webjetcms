# Zápis, publikovanie a zachovanie ID: analýza existujúceho kódu

Stav overený v pracovnom checkoute 1. novembra 2026. Tento dokument zaznamenáva existujúce správanie a jeho dôsledky pre návrh. Alternatívy nižšie nie sú schválené rozhodnutia. Implementácia sa v tejto fáze nemení.

Doplnenie z 2. novembra: [návrh Noviniek](03a-novinky.md) uzatvára nižšie pomenovanú otázku perex väzieb. Pracovné `perex_group` podporuje existujúce kategórie; odvodený `perex_group_doc` sa obnoví iba pre publikovaný dokument. Merge zachová dátumy a autora zvolenej verzie, aby nezmenil výpis Noviniek.

## Hlavné zistenie

Existujúce uloženie stránky alebo priečinka má viac účinkov než zápis jedného riadka. Mení odvodené údaje, históriu, cache a niekedy ďalšie stránky. Pracovnú vetvu nemožno bezpečne doplniť iba prepnutím názvu tabuľky pri jednom `save()`. Potrebuje oddeliť prípravu a kontrolu údajov od zápisu pracovnej verzie a od zverejnenia.

ID existujúcich stránok a priečinkov možno zachovať aktualizáciou pôvodného riadka. Samostatným technickým problémom sú **ID objektov vytvorených až vo vetve**: dnešné stránky a priečinky dostávajú ID pri vložení do produkčnej tabuľky. Médiá používajú iný generátor. Používateľ už potvrdil zachovanie nových ID; následný konkrétny návrh riešenia je v [02a-identifikatory.md](02a-identifikatory.md). Alternatívy nižšie ostávajú záznamom porovnania, nie ďalšími otvorenými produktovými otázkami.

## Stránka: cesta od editora k zápisu

| Miesto | Overené správanie | Dôsledok pre pracovnú vetvu |
| --- | --- | --- |
| [EditorFacade](../../../src/main/java/sk/iway/iwcm/editor/facade/EditorFacade.java), `save()`, riadky 84–158 | Rozpozná multigroup master, prenesie editorové polia, volá `saveEditedDoc()`, nastaví predvolenú stránku priečinka, priradí dočasné médiá, uloží atribúty a multigroup mapovanie. | Presmerovanie samotného `DocDetailsRepository.save()` nestačí. Vedľajšie zápisy musia dostať rovnaký pracovný kontext alebo byť v danej etape zablokované. |
| [EditorService](../../../src/main/java/sk/iway/iwcm/editor/service/EditorService.java), `saveEditedDoc()`, riadky 188–297 | Overuje oprávnenia pred udalosťami, nastavuje autora a dátum, vyvoláva `ON_START`, kontroluje obsah, normalizuje URL/HTML, rieši schvaľovanie a plánovanie. | Validácie a normalizácie sú kandidátom na spoločnú službu. Produkčné udalosti sa pri pracovnom uložení nesmú nechtiac spustiť. |
| `EditorService.saveEditedDoc()`, riadky 297–437 | Vytvára novú históriu, vykoná insert/update, podľa výsledku obnoví cache a URL väzby, uloží poznámku a vyvolá `AFTER_SAVE`. | `documents_history` je dnes súčasť schvaľovania a plánovania. Pracovné verzie vetvy by nemali bez rozlíšenia vstupovať do jeho publikačného frontu. |
| `EditorService.docSave()`, riadky 1406–1439 | Dopĺňa `file_name`, `root_group_l1..l3`, ukladá cez JPA a obnovuje `perex_group_doc`. | Aj pri rozsahu „documents“ sa používa pomocná tabuľka pre perex skupiny. Je potrebné určiť, či bude odvodeným indexom, alebo bude príslušná funkcia vo vetve obmedzená. |
| `EditorService.renameVirtualPath()`, riadky 1442–1457 | Pri zmene URL zavolá opravu doterajších odkazov a vráti zoznam ovplyvnených stránok. | Zmena URL môže rozšíriť množinu zmien mimo pôvodne otvorenej stránky. Tieto zmeny musia byť viditeľné v balíku pred merge. |
| `EditorService.saveAttrs()`, riadky 1382–1395 | Zapisuje `doc_atr`, prípadne pred zápisom odstraňuje existujúce atribúty. | Atribúty nie sú stĺpce `documents`. Ich úprava sa nesmie vo vetve ticho uložiť do produkcie. |

`EditorFacade.getExistingDocForEditor()` na riadku 282 je vhodný orientačný bod pre načítanie editovateľnej verzie. Načítanie cez `getDocForEditor()` navyše spracúva nové stránky, nastavenia priečinka a čistenie dočasných médií. Návrh musí rozlíšiť otvorenie editora a prosté čítanie verzie na porovnanie.

[EditorUtils.getDataAsc()](../../../src/main/java/sk/iway/iwcm/editor/util/EditorUtils.java), riadky 192–330, nie je za každej konfigurácie čistá textová normalizácia. Pri `fulltextExecuteApps` volá [EditorToolsForCore.renderIncludes()](../../../src/main/java/sk/iway/iwcm/common/EditorToolsForCore.java), riadky 295–382: nastavuje request údaje, vykonáva JSP include, prípadne používa loopback HTTP. `fulltextDataAscMethod` navyše môže reflexiou zavolať zákaznícky kód. Toto volanie sa preto nemôže bez oddelenia preniesť do pracovného zápisu alebo krátkej DB transakcie background publishera. Výpočet bez vykonávania aplikácií a kontrolované produkčné indexovanie majú rôzne požiadavky.

Existujú aj ďalšie zapisovacie cesty: [EditorDB](../../../src/main/java/sk/iway/iwcm/editor/EditorDB.java) obsahuje vlastné JDBC inserty/updaty `documents` a [DocDB.saveDoc()](../../../src/main/java/sk/iway/iwcm/doc/DocDB.java) zapisuje stránky priamo. Úprava modernej REST cesty sama osebe nezabezpečí kompatibilitu importov, starších komponentov alebo zákazníckeho kódu.

## Priečinok: zápisy a odvodené zmeny

[GroupsRestController.editItem()](../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java), od riadka 236, dnes spája validáciu a používateľské operácie:

- Pri plánovanej zmene existujúceho priečinka vytvorí plán; pri zmene vyžadujúcej schválenie existujúci priečinok hneď neprepíše.
- Pri vytvorení priečinka vyžadujúceho schválenie najskôr vloží interný priečinok a môže vytvoriť jeho predvolenú stránku. To je dnešný schvaľovací mechanizmus, nie izolovaná pracovná vetva.
- Voliteľne prenáša jazyk, oprávnenia, šablónu, viditeľnosť alebo poradie na potomkov. Zmena názvu URL alebo presun môže prepočítať URL stránok.
- Zmena domény má ďalšie účinky na systémové priečinky, konfiguráciu a v niektorých nasadeniach aj súbory. Nie je ekvivalentom bežnej zmeny názvu priečinka.

[GroupsDB.setGroup()](../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java), riadky 957–1160, robí JDBC zápis priamo do `groups`. Navyše:

1. Vyvolá `ON_START`.
2. Môže synchronizovať názov predvolenej stránky (`changeDocTitle()`).
3. Po zápise vytvorí záznam v `groups_scheduler`, ktorý sa používa aj ako história.
4. Aktualizuje cache stromu a audit, odošle požiadavku na obnovu cache ostatným uzlom.
5. Prepočíta `documents.file_name` a vyvolá `AFTER_SAVE`.

Parameter `publishEvents=false` vypne udalosti, **nevypne ostatné vedľajšie účinky**. Preto nie je hotovým prepínačom na bezpečné uloženie do vetvy.

[SaveGroupListener.changeDirection()](../../../src/main/java/sk/iway/iwcm/editor/rest/SaveGroupListener.java), riadky 37–55, pri udalostiach porovnáva produkčný `parent_group_id` a `url_dir_name`; po zápise spúšťa `WebpagesService.regenerateUrl()`. Je naviazaný na HTTP požiadavku. Background merge nemôže predpokladať, že volanie `GroupsDB.setGroup()` zopakuje celý interaktívny postup vrátane tohto listenera.

Mazanie má samostatnú semantiku: [EditorService.deleteWebpageLogic()](../../../src/main/java/sk/iway/iwcm/editor/service/EditorService.java) od riadka 1581 rozlišuje kôš a trvalé zmazanie, schvaľovanie, históriu a multigroup väzby. [GroupsDB.deleteGroup()](../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java) od riadka 2741 spracúva strom a súvisiace záznamy. V návrhu treba pomenovať rozdiel medzi „zmazať vo vetve“ a vykonaním produkčného mazania pri merge; pri prvom kroku sa produkčné dáta meniť nemajú.

Pri mäkkom zmazaní podstromu `GroupsDB.deleteGroup()`, riadky 2899–2928, presunie pod kôš **iba jeho koreň**. Potomkovia si ponechajú rodičov, stránky svoje `group_id`; stránkam nastaví `available=false`. Presun každého potomka alebo každej stránky samostatne do koša by túto hierarchiu zničil. [GroupsService.recoverGroupFromTrash()](../../../src/main/java/sk/iway/iwcm/editor/service/GroupsService.java), riadky 94–102 a 122–150, obnovuje pôvodného rodiča z poslednej neplánovanej histórie priečinka a dostupnosť stránok z ich `actual` histórie. Publikačný writer preto nesmie pre DELETE nahradiť obnovovacie hodnoty novým záznamom obsahujúcim len cieľový kôš a nedostupnosť.

Ani získanie cieľového koša nemusí byť čisté čítanie: `GroupsDB.getTrashGroup()`, riadky 4271–4276, používa `getCreateGroup()`. Preflight vetvy potrebuje samostatné vyhľadanie existujúceho koša bez vytvárania priečinkov. Chýbajúci kôš je pre dotknuté mazanie problém pripravenosti, nie oprávnenie na skrytý produkčný insert alebo trvalé zmazanie.

## Udalosti, zrkadlenie a viacnásobné umiestnenie

[structuremirroring.SaveListener](../../../src/main/java/sk/iway/iwcm/components/structuremirroring/SaveListener.java) počúva `GroupDetails`, `DocDetails` aj starší `EditorForm`. Následné služby [DocMirroringServiceV9](../../../src/main/java/sk/iway/iwcm/components/structuremirroring/DocMirroringServiceV9.java) a [GroupMirroringServiceV9](../../../src/main/java/sk/iway/iwcm/components/structuremirroring/GroupMirroringServiceV9.java) môžu vytvárať alebo meniť prepojené stránky a priečinky. Listener výnimky loguje; jeho dokončenie samo osebe nie je potvrdením, že všetky odvodené zápisy prebehli.

[MultigroupService.multigroupHandleSlaves()](../../../src/main/java/sk/iway/iwcm/editor/service/MultigroupService.java), riadky 55–162, odstraňuje a obnovuje mapovanie, zapisuje slave stránky cez `DocDB.saveDoc()`, mení predvolené stránky priečinkov, ukladá atribúty a môže odstraňovať staré kópie. `setDefaultDocId()` na riadku 180 môže pri uložení jednej stránky zavolať uloženie priečinka.

Pre návrh sú preto dôležité dve oddelené otázky:

- Ktoré automatické odvodené zmeny majú vznikať už počas práce vo vetve a byť zahrnuté v jej porovnaní?
- Ktoré operácie sa majú vykonať až pri merge a ako zabrániť tomu, aby znovu prepísali už vyriešené verzie iných položiek?

Odpoveď „zapnúť všetky dnešné listenery po každom riadku“ by neposkytovala kontrolovateľný výsledok merge. Odpoveď „vypnúť všetky listenery“ by zase vynechala potrebné odvodené údaje. Rozsah podporovaných automatizácií je potrebné rozhodnúť explicitne.

## Dnešné plánované publikovanie

[DocPublishService.checkWebpagesToPublish()](../../../src/main/java/sk/iway/iwcm/doc/DocPublishService.java), riadky 44–97, kontroluje front najviac raz za päť sekúnd v danej inštancii a samostatne publikuje histórie alebo zneprístupňuje stránky po konci platnosti.

`copyDHistory()` na riadku 105:

1. Znovu načíta históriu a overí termín, schválenie a stav stránky.
2. Nastaví kontext domény a autora publikovanej stránky nezávisle od požiadavky, ktorá publikovanie vyvolala.
3. V režime plánovaných zmien **zachová aktuálny produkčný `virtual_path` a `group_id`**. Tento postup teda nie je pripravený na merge presunutých stránok.
4. Publikuje pracovné súbory, zapíše stránku, potom médiá a multigroup kópie.
5. Dokončí históriu, uprace pracovné súbory a obnoví cache.

Služba obsahuje ochranu proti súbežnému alebo rekurzívnemu publikovaniu stránky v jednom JVM, nie koordináciu celého klastra. Nie je potrebné kvôli návrhu balíkov stavať nový všeobecný distribuovaný transakčný systém; musí však byť jasné, ktorý existujúci scheduler vlastní konkrétny plán. Jednu verziu nesmú nezávisle publikovať dva mechanizmy.

[PlannedChangesService.prepareSave()](../../../src/main/java/sk/iway/iwcm/doc/PlannedChangesService.java), riadky 163–195, vyžaduje už uloženú stránku, zapnutú históriu a dátum publikovania. Obnovuje produkčnú URL a priečinok, inicializuje plánované médiá a deaktivuje staršie pending histórie. Uloženie nového dokumentu výlučne v pracovnej vetve preto nemožno postavit na nezmenenom `prepareSave()`.

`preparePublication()` na riadku 372 najskôr kontroluje súbory, potom ich postupne zverejní. `publishFiles()` na riadku 534 používa zálohu pôvodného súboru a dočasný súbor s atómovým presunom **jednotlivého súboru**. Už nahradené súbory zostávajú zverejnené pri chybe neskoršej položky. DB a súbory netvoria jednu transakciu. Overené pomocné kroky možno použiť ako východisko; súčasný pracovný priestor je však viazaný na `docId`, nie na viac samostatných vetiev.

`PlannedChangesService.checkConflicts()`, riadky 318–327, porovnáva verejné cieľové cesty súborov proti ostatným `findByPublicableTrue()` históriám, nielen proti plánom aktuálnej stránky. Pri externom doménovom úložisku filtruje doménu; `plannedFiles()` na riadkoch 478–495 číta pripravené súbory z filesystemu. Preto nestačí zistiť, že stránky patriace do balíka nemajú individuálny plán: cudzia naplánovaná stránka môže neskôr prepísať rovnaký súbor. Pre vetvy sa z týchto metód oddelí čítací helper s explicitným mapovaním cieľového úložiska, bez volania celej prípravy plánovanej stránky.

[GroupPublisher.main()](../../../src/main/java/sk/iway/iwcm/doc/GroupPublisher.java), riadky 133–181, nezávisle spracúva `groups_scheduler` a zapisuje cez `GroupsDB.setGroup()`. Pri balíku treba určiť vzťah aj k tomuto scheduleru, nielen k `DocPublishService`.

## Médiá a skrytý rozsah tabuliek

[Media](../../../src/main/java/sk/iway/spirit/model/Media.java) používa `media_fk_table_name` a `media_fk_id` na priradenie k vlastníkovi. Skupiny médií sú väzba cez `media_group_to_media` na `media_groups`, nie iba hodnota v riadku `media`.

[MediaService](../../../src/main/java/sk/iway/iwcm/editor/service/MediaService.java), riadky 41–78, prideľuje dočasné médiá novej stránke a pri otvorení novej stránky čistí dočasné médiá používateľa. Pri viacerých vetvách nemožno bez preverenia ponechať dočasný priestor identifikovaný iba používateľom.

`PlannedChangesService.publishMedia()`, riadky 393–445, odstráni produkčnú množinu médií a vloží nové kópie. `copyMedia()` výslovne nekopíruje pôvodné ID. Ak návrh sľúbi zachovanie aj existujúcich `media_id`, toto správanie sa nedá bez úpravy prevziať. Pre zachovanie ID je potrebné porovnať a vykonať insert/update/delete po identite média, nie nahradiť celú množinu novými kópiami.

Aj pri obchodnom rozsahu `documents`, `groups`, `media` a súbory sa teda návrh musí vyjadriť k pomocným väzbám:

| Väzba alebo tabuľka | Význam pre návrh |
| --- | --- |
| `perex_group_doc` | Odvodené priradenia perex skupín; ovplyvňujú výpisy stránok. |
| `media_group_to_media` | Priradenia médií do skupín. Čítanie a merge média bez nich môže meniť význam záznamu. |
| `multigroup_mapping` | Ďalšie umiestnenia stránky; dnešný editor ich aktívne spravuje. |
| `doc_atr` | Editovateľné atribúty stránky mimo `documents`. |
| `documents_history`, `groups_scheduler` | História, schvaľovanie a samostatné plánovanie. |
| Poznámky, audit a odvodené cache/indexy | Súčasť existujúceho uloženia/publikovania; treba odlíšiť záznam pracovného uloženia a produkčného merge. |

Tieto väzby automaticky nerozširujú požadovaný produktový rozsah. Určujú však, ktoré ovládacie prvky bude prvá verzia podporovať, ktoré budú iba na čítanie a ktoré odvodené dáta sa musia prepočítať.

## ID existujúcich a nových objektov

### Overené mechanizmy

| Objekt | Aktuálne prideľovanie ID |
| --- | --- |
| Stránka | [DocDetails](../../../src/main/java/sk/iway/iwcm/doc/DocDetails.java), riadok 97: `@GeneratedValue(strategy = IDENTITY, generator = "S_documents")`. `EditorService.docSave()` po inserte dohľadáva ID podľa priečinka a názvu. Ďalšie JDBC inserty sú v `DocDB` a `EditorDB`. |
| Priečinok | `GroupsDB.setGroup()`, riadky 1016–1097: insert bez ID, potom `max(group_id)` pre názov a parent. Kladné ID znamená UPDATE. |
| Médium | `Media`, riadky 60–63: `WJGen_media`/`TableGenerator`. [WJGenSequence](../../../src/main/java/sk/iway/iwcm/system/jpa/WJGenSequence.java) volá [PkeyGenerator.getNextValue()](../../../src/main/java/sk/iway/iwcm/PkeyGenerator.java). Generátor vie prideľovať ID pred zápisom média. |

Databázové rozdiely sú priamo v blank schémach: [MySQL](../../../src/main/webapp/WEB-INF/sql/blank_web.sql) má pre dokumenty a priečinky `AUTO_INCREMENT`, [SQL Server](../../../src/main/webapp/WEB-INF/sql/blank_web_mssql.sql) `IDENTITY`, [Oracle](../../../src/main/webapp/WEB-INF/sql/blank_web_oracle.sql) sekvencie a triggery a [PostgreSQL](../../../src/main/webapp/WEB-INF/sql/blank_web_pgsql.sql) sekvencie s defaultným `nextval`.

V starom [autoupdate.xml](../../../src/main/webapp/WEB-INF/sql/autoupdate.xml), riadky 189–190, existuje aj registrácia `documents` v `pkey_generator`. Jej existencia **neznamená**, že dnešné insert cesty používajú tento generátor. Samostatná rezervácia cez `PkeyGenerator` nezabráni bežnému `AUTO_INCREMENT`/`IDENTITY` insertu použiť rovnaké číslo.

Záporné ID tiež nie je pripravené univerzálne riešenie: `DocDetails.getId()` vracia pri ID menšom než 1 `null`; `EditorFacade.getDocForEditor()` používa záporné ID pre nové stránky a šablónu novej stránky. Vyžadovalo by zmenu existujúcich konvencií a podporu vo všetkých čítacích cestách.

### Alternatívy na rozhodnutie, zatiaľ neschválené

| Alternatíva | Výhoda | Skutočný zásah alebo obmedzenie |
| --- | --- | --- |
| Existujúce objekty držia pôvodné ID, nové dostanú produkčné ID až pri merge | Zachová dnešné generátory; jednoduchšia produkčná migrácia. | Nový objekt potrebuje pracovnú identitu. Merge musí prepísať všetky jeho väzby a odkazy vrátane `!INCLUDE(...)!` parametrov; pre ľubovoľné zákaznícke aplikácie to nie je všeobecne spoľahlivé. |
| Nový objekt hneď rezervuje riadok v produkčnej tabuľke ako nezverejnený/interný | Získa bežné ID a nemusí prepisovať odkazy pri merge. | Riadok nie je izolovaný od všetkých produkčných SQL dotazov, admin výpisov a aplikácií. Najmä pri priečinku nejde iba o `available=false`. Treba explicitne prijať a pokryť túto semantiku. |
| Jeden spoločný alokátor pre produkčné aj pracovné nové objekty | Trvalé ID vznikne vo vetve bez produkčného obsahového riadka. | Všetky relevantné inserty musia používať ten istý zdroj ID; treba upraviť JPA, JDBC a databázové identity/defaulty podľa podporovaných DB. Existujúci `PkeyGenerator` je kandidát na opätovné použitie, nie hotová kompatibilná náhrada. |
| Prvá etapa upravuje iba existujúce stránky/priečinky | Najmenšie overiteľné jadro zachovania ID a preview. | Je to etapa realizácie, nie splnenie celej požiadavky na fork; vytváranie nových objektov musí mať navrhnutú nadväzujúcu etapu. |

Výber nemožno skryť do implementačného detailu. Určuje rozsah refaktoringu aj to, čo bude fungovať v aplikáciách, ktoré čítajú alebo zapisujú obsah priamo.

## Miesta, na ktorých možno návrh zjednodušiť

Nasledujúce body sú technické odporúčania na diskusiu, nie schválená architektúra:

1. **Samostatná pracovná perzistencia.** Uchovať produkčné ukladanie ako explicitnú cestu a pre vetvu oddeliť uloženie obsahových údajov bez produkčných následkov. Spoločné overenie práv, normalizáciu a odvodenie URL vytiahnuť len tam, kde sa reálne používajú v oboch cestách.
2. **Kontrolovaný aplikačný merge.** Zostaviť výslednú množinu zmien pred zápisom. Následne vykonávať operácie v jednoduchom známom poradí s výsledkom pri položke. Neprisľubovať atómové prepnutie celej DB a filesystemu na základe dnešných služieb.
3. **Explicitné následky.** Odvodené zmeny URL, predvolených stránok a automatických kópií buď zahrnúť do pripravovaného výsledku, alebo v konkrétnej etape príslušnú operáciu zrozumiteľne obmedziť. Už existujúce prepojenia sa nesmú potichu poškodiť.
4. **Jedno vlastníctvo termínu.** Naplánovaný balík potrebuje vlastný termín a jasné pravidlo voči existujúcim plánom stránok/priečinkov. Pri kolízii je jednoduchšie vyžiadať vedomé vyriešenie než pridávať ďalšiu automatickú prioritu schedulerov.
5. **Jeden aplikačný krok, jedna zodpovednosť.** Nie je potrebný všeobecný workflow engine, Git objektový model ani dlhé zámky editora. Potrebné sú však rozpoznateľné zlyhanie, zachované vstupy a prehľad toho, čo sa už zverejnilo.

## Migrácie a overenie pre neskoršiu implementáciu

Nové DB zmeny patria do existujúceho mechanizmu [autoupdate-webjet9.xml](../../../src/main/webapp/WEB-INF/sql/autoupdate-webjet9.xml) a [UpdateDatabase](../../../src/main/java/sk/iway/iwcm/system/UpdateDatabase.java). Aktuálne migračné záznamy rozlišujú MySQL, SQL Server, Oracle a PostgreSQL. Prípadná zmena alokácie ID musí obsahovať plán pre každú podporovanú DB; nestačí generické SQL v návrhu.

Pre prvý implementačný dôkaz majú hodnotu najmä tieto scenáre:

- Pracovné uloženie stránky a presun priečinka nezmenia produkčné riadky, odkazy ani súbory.
- Otvorenie a uloženie vetvy nespustí produkčné zrkadlenie, multigroup zápis alebo čistenie médií inej vetvy.
- Merge aktualizuje existujúce `doc_id` a `group_id`; referencie aplikácií stále ukazujú na tie isté objekty.
- Nový objekt sa správa podľa schválenej politiky ID aj po opätovnom otvorení editora a po merge.
- Konflikt s individuálnym plánom je viditeľný pred spustením merge.
- Pri chybe uprostred publikovania zostanú pracovné vstupy dostupné a stav zodpovedá skutočne vykonaným krokom.
- Produkčné uloženie bez aktívnej vetvy zachová schvaľovanie, históriu, presuny, zrkadlenie a existujúce plánovanie.

Toto je statická analýza zdrojového kódu. Neobsahuje runtime test pridelenia ID ani potvrdenie konkrétneho databázového profilu nasadenia.
