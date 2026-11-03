# NewsApp a NewsActionBean v pracovnej vetve

**Stav: návrh, bez implementácie.** Doplnené 2. novembra 2026 na výslovnú požiadavku používateľa. Podpora bežnej aplikácie Novinky je povinnou súčasťou prvej verzie. Tento dokument spresňuje [čítanie a Java integráciu](03-nahlad-a-java.md); nevytvára samostatný systém noviniek.

## Výsledok pre redaktora

Redaktor otvorí pracovnú vetvu, pridá novinku do priečinka, vyplní perex, prípadne vyberie existujúcu perex skupinu, a uloží ju. Na stránke s aplikáciou Novinky sa nová stránka zobrazí podľa tých istých podmienok, ktoré použije produkcia po merge. Úprava existujúcej novinky môže zmeniť jej text, pozíciu alebo zaradenie do výpisu. Pracovne odstránená stránka vo výpise nebude. Produkčný výpis sa dovtedy nemení.

Nová stránka sa do zoznamu nepridáva nasilu. Musí vyhovovať zdrojovému priečinku, dostupnosti, perexu, kategóriám, dátumom, právam a ďalším nastaveným filtrom. Napríklad stránka s prázdnym perexom sa pri `perexNotRequired=false` správne nezobrazí. Zobrazenie stránky v editore samo osebe neznamená zaradenie do verejných noviniek.

Pri rovnakých dátach, čase vyhodnotenia, identite návštevníka a šablóne musí náhľad vrátiť rovnaké ID, poradie a počet ako produkcia po použití zvoleného výsledku. Podľa Q16 voľba produkcie mení už samotný náhľad Noviniek, nielen neskorší merge. Platí aj pre parametre aplikácie v HTML vlastníckej stránky, priečinky a médiá. Editor uchová pracovný obsah oddelene. Kým konflikt nemá rozhodnutie, náhľad ukazuje WORK s viditeľným upozornením; zastaraná uložená voľba vyžiada nové porovnanie podľa `03`. Živý overlay nie je predpoveď neskorších cudzích úprav ani individuálnych plánovaných zmien.

## Existujúca cesta a konkrétne zásahy

```text
účinná stránka vrátane svojich !INCLUDE parametrov
  → /components/news/news-velocity.jsp
  → NewsActionBean (dedič NewsApp)
  → podmienky nad účinným stromom a údajmi stránky
  → NewsQuery nad účinným SQL zdrojom documents
  → existujúca Velocity šablóna a stránkovanie
  → odkazy/obrázky cez preview gateway
```

| Existujúci súbor/metóda | Zistenie a navrhovaná úprava |
| --- | --- |
| [NewsApp](../../../src/main/java/sk/iway/iwcm/components/news/NewsApp.java) | Definuje parametre a editor aplikácie. Zachovať existujúci formát `!INCLUDE`, nastavenia aj výber šablóny. `branchId` sa neukladá do obsahu stránky. |
| [news-velocity.jsp](../../../src/main/webapp/components/news/news-velocity.jsp) | Vytvára nový action bean pre každý výskyt a volá `News.action`. Zachovať tento render aj opakované vloženie komponentu na stránke. |
| [NewsActionBean](../../../src/main/java/sk/iway/iwcm/components/news/NewsActionBean.java): `news`, `addGroups`, `addPublishType` | Kontext nastaviť pred bindingom skupín a zostavením kritérií. Použiť účinný rodičovský dokument, strom a jeden čas vyhodnotenia requestu. |
| [NewsQuery](../../../src/main/java/sk/iway/iwcm/components/news/NewsQuery.java): `getSelectSql`, `getNewsList`, `getNewsCount`, `setArguments` | Zoznam aj počet používajú rovnaký parametrizovaný zdroj. Vetva sa skladá pred WHERE, radením a stránkovaním. |
| `NewsActionBean.fillTemplate`, `getPaginator`, `link` | Rovnaká Velocity šablóna; účinné `DocDB`, `GroupsDB`, `MediaDB`; odkazy vrátane stránkovania zachovajú gateway. |
| [NewsRestController](../../../src/main/java/sk/iway/iwcm/components/news/NewsRestController.java) | Overiť override `getAllItems`/`getOneItem`, strom aj `convertIdsToNamePair`; nespoliehať sa iba na úpravu predka `WebpagesDatatable`. |
| [editor-component.html](../../../src/main/webapp/apps/news/admin/editor-component.html) | Vnorená správa noviniek preberie overený admin kontext. Je to administračný zoznam; verejný vzhľad sa kontroluje na stránke v náhľade. |

Nevzniká `BranchNewsApp`, kópia `NewsActionBean`, nová šablóna výpisu ani osobitný index noviniek. Spoločné `BranchSql` a `BranchView` už potrebuje zvyšok návrhu.

## Zdroj dát pred vyhodnotením podmienok

V produkcii `NewsQuery` naďalej používa `documents d`. V overenom vetvovom kontexte dostane od `BranchSql` účinný zdroj s rovnakým aliasom `d`:

```text
LIVE stránky bez účinného pracovného prepisu s rovnakým doc_id
UNION ALL
účinné WORK UPSERT stránky danej vetvy
```

Účinná WORK `DELETE` potlačí produkčný riadok; pri voľbe LIVE sa pracovné zmazanie v náhľade neuplatní. Doménu a povolený rozsah určí spoločný čítač. Až nad týmto zdrojom sa vykonajú dnešné filtre. Nemožno najskôr vybrať desať produkčných noviniek a potom ich nahradiť či doplniť pracovnými: novinka presunutá do filtra by chýbala a počet/ďalšia strana by boli nesprávne.

Navrhovaný kontrakt je SQL fragment + usporiadané bind parametre. Kontext je explicitne prevzatý z overeného requestu, nie z používateľského `filter` alebo voľného URL parametra. Bežné volanie bez vetvového kontextu zostane LIVE. Pre list aj count sa samostatne zostaví celý plán a argumenty; dnešné mutable pole `arguments` nesmie ponechať parametre predchádzajúceho vykonania. Parametre zdroja sa bindujú v poradí SQL pred parametrami vonkajších kritérií.

Projekcia musí obsahovať polia `FieldEnum` aj potrebné `DataAccessHelper.getDocFields()`. `loadData=false` vynecháva telo z výsledného SELECT, nie z dostupných filtrov. HTML sa nesmie skrátiť kvôli UNION/CLOB. Prevádzkové aj nepodporované read-only polia, napríklad `forum_count`, `sync_id`/`sync_status` či existujúce `publish_start`/`publish_end`, sa pre existujúcu pracovnú stránku čítajú z aktuálnej produkcie; pre novú dostanú výslovne určené počiatočné hodnoty. Nevydávajú sa za zmrazený redakčný obsah. Rovnakú klasifikáciu používa detail, SQL projekcia a merge writer; odvodené polia sa prepočítajú z účinných vstupov.

V ADMIN režime je účinný prepis každý pracovný záznam. V PREVIEW režime ho určuje spoločná mapa B/L/W rozhodnutí z `03`: pri zvolenom LIVE stránka zostane v produkčnej vetve SQL, pri WORK sa nahradí pracovnou projekciou alebo vylúči cez DELETE. Výber sa aplikuje **pred všetkými filtrami a count/paging**, neprepisuje až výsledný zoznam. Nečíta sa `resolved_snapshot` cez databázové JSON funkcie a nevzniká druhá tabuľka výsledných stránok.

Oba režimy použijú rovnaké deterministické radenie: doterajšie poradie a posledný rozlišovací `doc_id`, ak už nie je súčasťou poradia. `count` počíta rovnakú množinu bez stránkovacieho limitu; po odpočítaní počiatočného offsetu je minimum nula. Každý `doc_id` je v jednom výpise najviac raz ešte pred aplikovaním LIMIT/OFFSET.

### Tri konkrétne miesta na zachovanie počtu a poradia

1. `perexGroupUseJoin=true` dnes pripája `perex_group_doc p`, ale generované perex kritériá čítajú `d.perex_group` a alias `p` nepoužívajú. Odstrániť tento nepotrebný join v `NewsQuery` pre LIVE aj vetvu; inak viac kategórií násobí jednu novinku. Verejné API a použitie konfigurácie inde v CMS ostanú zachované. Nejde o vypnutie kategorizácie.
2. Pri filtrovaní atribútov môže `doc_atr` join vytvoriť viac riadkov jednej stránky. Zachovať existujúcu booleovskú podmienku nad dokumentom a atribútovými riadkami v poddotaze vracajúcom jedinečné `doc_id` (`GROUP BY doc_id`); vonkajší dotaz načíta dokumenty, zoradí a stránkuje. Počet používa tú istú množinu ID. Nepridávať `DISTINCT` nad celým HTML/CLOB. Detailný SQL tvar a poradie argumentov sa overia v P01; nie je potrebný nový parser filtrov.
3. `getNewsListWithAtributes` dnes obohacuje výsledok cez `HashMap` a vracia `values()`. Zachovať poradie pôvodného zoznamu a pri prázdnom zozname preskočiť dotaz `IN (...)`. Atribúty načítané z produkcie sa pripájajú k oddeleným výsledným objektom.

Tieto úpravy sa overia aj na LIVE výpise, aby sa opravou iba v náhľade nevytvoril rozdiel po publikovaní. Iné nesúvisiace opravy `NewsQuery` nie sú podmienkou tejto funkcie.

## Ktoré kritériá musia fungovať

| Nastavenie aplikácie | Správanie vetvy |
| --- | --- |
| `groupIds`, `alsoSubGroups`, `subGroupsDepth` | ID vybraných skupín znovu rozlíšiť cez účinný `GroupsDB`; zahrnúť nové/presunuté podpriečinky podľa dnešnej hĺbky. Nemení sa dnešné pravidlo pre priečinky skryté z menu. |
| Prázdny `groupIds` | Zachovať dnešný fallback na priečinok aktuálnej **účinnej** stránky. Explicitne vybraný zmazaný/neplatný koreň znamená prázdny výsledok alebo validačnú chybu; nikdy rozšírenie na celý web. |
| `docMode` 0/1/2 | Všetky / iba default / bez default stránok podľa účinných `defaultDocId` priečinkov. |
| `available`, `includeActualDoc` | Len výsledne dostupné stránky; podľa nastavenia vynechať stabilné ID aktuálnej stránky. |
| `perexNotRequired` | Použiť účinné `html_data` z vybranej verzie. Prázdny perex zostáva platným dôvodom nezaradenia. |
| `perexGroup`, `perexGroupNot`, tag, author, `requestPerexGroupsName` | Zachovať dnešné tokenové a include/exclude podmienky nad účinným `perex_group`; názvy sa rozlišujú cez existujúce definície v aktuálnej doméne. |
| `publishType` | Rovnakých päť režimov a hraničných podmienok; čas je určený nižšie. |
| `filter`, `search`, `DatabaseCriteria` | Zachovať dnešné operátory a zloženie AND/OR nad účinnými poliami. Opakované hodnoty a textové vyhľadanie sa nesmú stratiť pri prechode na ďalšiu stranu. |
| Atribútové filtre a `returnDocsWithAtributes` | Existujúce `doc_atr`/`doc_atr_def` sa čítajú LIVE. Nová stránka nemá vlastné atribúty; filter vyžadujúci taký atribút ju nemusí zaradiť. Editácia atribútov sa nepridáva. |
| Radenie, `ascending`, `offset`, `pageSize`, `paging` | Účinné hodnoty priority, publikačného/event/save dátumu, názvu, miesta, ID, ratingu a ďalších existujúcich `OrderEnum`; rovnaký zdroj pre list aj count. |
| `password_protected`, `searchAlsoProtectedPages` | Existujúce pravidlá podľa skutočného používateľa. Prihlásenie administrátora automaticky neznamená simuláciu anonymného návštevníka. |
| `checkDuplicity`, opakované vloženie aplikácie | Deduplikácia v jednom requeste nad účinnými metadátami; existujúce multigroup mapovanie zostáva na čítanie. Žiadna spoločná výsledková množina naprieč vetvami/requestmi. |

Vlastné `ResultCriteria` a `contextClasses`, ktoré vykonávajú zákaznícky Java/SQL kód, patria pod adaptačný kontrakt z dokumentu 03. Bežná aplikácia používa `DatabaseCriteria`; nevyžaduje transparentnú podporu ľubovoľného zákazníckeho predikátu. V prvom rozsahu preview pri pridaní `ResultCriteria` vráti vysvetlenie nepodporovaného rozšírenia, neignoruje ho ani nevráti nefilterovaný počet. LIVE API zostáva zachované. Jeho dnešné počítanie po jednotlivých kritériách a fallback pri nule by si pri neskoršej podpore vyžadovali vlastné cielené overenie, nie skrytú opravu v tejto etape.

## Perex skupiny: malá potrebná úprava rozsahu

Aby nová novinka vyhovela bežnému kategorizačnému filtru, vo vetve možno vyberať **existujúce perex skupiny**. Hodnota je už v `documents.perex_group`; uloží sa do rovnakého poľa pracovnej projekcie. Nová pracovná tabuľka nie je potrebná. Definície skupín sa naďalej spravujú iba v produkcii.

- Mapper načíta/zapíše `perex_group`, editor ponúkne existujúce platné skupiny domény. Snapshot/hash používajú normalizovanú množinu ID bez duplicít; fyzický tokenový formát ostáva kompatibilný s dnešným filtrom.
- Náhľad filtruje účinné `perex_group` z vybranej LIVE/WORK verzie. Pracovné uloženie **nezapisuje** produkčný `perex_group_doc`.
- Merge overí platnosť odkazovaných skupín. Zaniknutá/neprístupná skupina je konkrétna validačná chyba. Kategorizácia je súčasťou konfliktu celej stránky.
- Pri skutočnom zápise obnoví odvodený `perex_group_doc` pre toto jediné `doc_id` podľa existujúcej konfigurácie. Nepoužije sa bez úpravy `DocDB.udpdatePerexGroupDoc`, ktoré prechádza aj multigroup kópie.
- [PerexGroupsService](../../../src/main/java/sk/iway/iwcm/components/perex_groups/PerexGroupsService.java) dostane úzky variant synchronizácie jedného dokumentu s prijatou JDBC `Connection`, bez vlastného commit/close. Riadok stránky, odvodené väzby a checkpoint sa zapíšu v už navrhnutej krátkej transakcii položky. Existujúce repository volania samy osebe nezaručujú rovnakú transakciu.

To je jediná nová úprava vedľajšieho zápisu potrebná pre štandardné kategorizované novinky. Nepridáva správu definícií, multigroup synchronizáciu ani pracovné atribúty.

## Dostupnosť, čas a dátumy uloženia

`NewsActionBean.availableOnly()` vyžaduje `available=true` aj v režime ALL. Pracovné `available` preto znamená zamýšľanú dostupnosť po merge. Izoláciu od produkcie zabezpečuje pracovná tabuľka. Nepoužiť `requestPublish=false` ako spôsob uloženia vetvy: dnešná produkčná cesta tým pri novej stránke vynúti `available=false` a novinku by z výpisu vyradila. Predvolená voľba dostupnosti vychádza z `editorNewDocDefaultAvailableChecked`, redaktor ju môže vedome zmeniť podľa práv.

Existujúce `publish_start`/`publish_end` zostávajú vo vetve read-only podľa pôvodného rozsahu. Pri novej stránke sa `publish_start` inicializuje raz podľa `editorAutoFillPublishStart` na čas vytvorenia alebo NULL, `publish_end` je NULL a individuálne plánovacie príznaky vypnuté. Kopírovanie predlohy novej stránky nesmie potichu zdediť starý koniec alebo budúci individuálny plán. Hodnoty a dostupnosť editor zobrazí; spoločný termín balíka ich neprepisuje. Event dátum zostáva upraviteľný.

Čas `previewAt` je request údaj v `BranchView`, nie nový DB stĺpec ani globálny posun hodín. Pri `SCHEDULED` vetve s budúcim `publish_at` sa použije tento termín; inak čas začiatku requestu. Všetky výskyty Noviniek v tom istom requeste dostanú rovnakú hodnotu. LIVE používa dnešný čas. Po zrušení plánu alebo prechode do `NEEDS_ATTENTION` sa opäť použije aktuálny čas. Posun termínu sa prejaví po obnovení náhľadu.

`addPublishType` použije tento čas namiesto vlastného `Tools.getNow()`. Nemení sa globálne `Tools.getNow`, nespúšťajú sa budúce per-page plány a nepredstiera sa budúci stav iných aplikácií. Banner uvedie napríklad `Novinky vyhodnotené k 1. 12. 2026 09:00 (Europe/Bratislava)` a pri budúcom termíne krátke vysvetlenie rozsahu tejto predikcie. Samostatný simulátor času alebo návštevníckych rolí sa nepridáva.

Presné dnešné pravidlá, ktoré zostanú zachované (`S` = začiatok, `E` = koniec, `T` = čas vyhodnotenia):

| Režim | Podmienka navyše k ostatným filtrom |
| --- | --- |
| NEW | `(S IS NULL OR S <= T) AND (E IS NULL OR E >= T)` |
| VALID | `S IS NOT NULL AND S <= T AND (E IS NULL OR E >= T)` |
| NEXT | `(E IS NOT NULL AND E >= T) OR (E IS NULL AND S IS NOT NULL AND S >= T)` |
| OLD | `E IS NOT NULL AND E <= T` |
| ALL | Bez dátumovej podmienky; dostupnosť a ostatné filtre platia. |

NEXT môže podľa dnešného kódu zahrnúť aj už začaté články. Túto vlastnosť ani spoločnú hraničnú hodnotu konca pre NEW/OLD vetvová funkcia nemení.

Dnešné `EditorService.saveEditedDoc` mení `date_created` a `author_id` pri uložení. Pracovné uloženie zachová tento význam: skutočné uloženie redaktorom aktualizuje tieto hodnoty vo WORK. Náhľad ich používa a merge ich prenesie zo zvolenej výslednej verzie bez ďalšieho prepísania časom/autorom publishera. Audit publikovania eviduje publishera a čas osobitne. Inak by sa radenie SAVE_DATE (`date_created`) a zobrazený autor zmenili samotným merge. DATE naďalej znamená `publish_start`.

## Vykreslenie, preklik a administrácia

`fillTemplate` používa existujúcu produkčnú definíciu News šablóny. Jej zmena vo vetve je mimo rozsahu, výber existujúcej šablóny cez parametre vloženej aplikácie je podporovaný. `news`, aktuálny `docDetails` a jadrové helpery dostanú účinné dáta. Mapper musí naplniť aj prezentačné dátumové vlastnosti tak ako dnešné `DocDB.getDocDetails`. `cropPerex` mení výsledný objekt, preto musí ísť o oddelený objekt requestu.

`getPaginator` dnes vytvára odkazy cez `Tools.getBaseLink`; preview adaptácia zachová gateway, `page` aj ostatné filtre práve raz. Detail novinky, obrázok, thumbnail a médiá musia zostať v rovnakej vetve. Štandardná cesta používa odkazy na ďalšiu stranu; nie je potrebný nový AJAX engine. Vlastné load-more/Headless endpointy alebo legacy `news-json.jsp` nie sú automaticky týmto kontraktom overené a potrebujú príslušnú adaptáciu.

`NewsActionBean` nemá aktívnu spoločnú cache výsledkov noviniek; instance list nie je dôvod zavádzať novú cache vrstvu. Preview obchádza vonkajšiu HTML/component/CDN cache podľa dokumentu 03. Cache produkčných **definícií** News šablón môže zostať spoločná. SQL chyba vetvy sa nesmie zmeniť na zdanlivo prázdny zoznam: v preview sa zobrazí chyba načítania komponentu a zapíše diagnostika, bez produkčného fallbacku.

V admin editore sa po uložení pracovnej novinky zobrazí jej riadok aj v správe Noviniek. Iframe `/apps/news/admin/?include=...` dostane `branchId` aj `branchDomain` ako bootstrap údaje overené voči session/doméne; následné REST požiadavky posielajú obe kontextové hlavičky podľa `03`. Starý iframe po prepnutí vetvy skončí `409` rovnako ako editor. Úprava samotného base controllera nestačí: `NewsRestController` má vlastné override a `convertIdsToNamePair` tiež priamo skenuje `documents.data` pri zisťovaní priečinkov z vložených aplikácií. Tento scan musí použiť účinné stránky, aby našiel aj novo pridanú aplikáciu či zmenené parametre. Práva stromu a `cmp_news` zostávajú zachované.

## Etapy a povinné dôkazy

Podrobný plán P01–P08 zostáva v [06](06-plan-implementacie.md); testy dopĺňajú T16 v [07](07-overenie.md). Novinky sa nesmú presunúť do voliteľného zákazníckeho adaptéra.

| Prípad | Dôkaz | Najskorší krok |
| --- | --- | --- |
| N01 | Nová, zmenená, presunutá a zmazaná novinka; zmena filtra cez pracovné pole; list aj count pred paging; produkcia nedotknutá. | P01 fixture, P03 render |
| N02 | Viac kategórií jednej novinky, include/exclude a `perexGroupUseJoin` true/false; každé ID práve raz, rovnaký počet a poradie. | P01 SQL, P03 náhľad, P04 editácia |
| N03 | Nový/presunutý/zmazaný podpriečinok, hĺbka, zmena default stránky, všetky `docMode`; neplatný koreň nerozšíri výsledky. | P03 |
| N04 | Dostupná aj zámerne nedostupná nová stránka; prázdny perex a oba režimy `perexNotRequired`; päť publish typov vrátane NULL a rovnosti dátumov. | P03 fixture, P04 predvolené hodnoty |
| N05 | Textové filtre, opakované search/tag/author, všetky existujúce radenia vrátane zhody hodnôt; `loadData` true/false; offset, prvá/druhá strana a nulový výsledok. | P03 |
| N06 | Atribútový OR filter zasiahne viac riadkov jednej stránky bez duplicity; enrich zachová poradie; nová stránka bez atribútov; explicitné odmietnutie vlastného ResultCriteria v preview. | P03 |
| N07 | Chránené stránky podľa identity, dva News komponenty s deduplikáciou, pracovné include parametre a efektívne helpery šablóny. | P03, médiá P05 |
| N08 | Klik na ďalšiu stranu, detail, obrázok a médium ostáva vo vetve; produkcia a druhá vetva majú nezávislé výsledky aj po zahriatí cache. | P03, upload P05 |
| N09 | Admin News iframe vrátane novej aplikácie/nového koreňa, uloženie aj prehodenie vetvy v druhom tabe; správne práva a `409`. | P04 |
| N10 | Pri rovnakom T/identite/šablóne porovnať predmerge náhľad s postmerge produkciou: ID, poradie, count, názvy, perex, autor, dátumy, odkazy, médiá. Zahrnúť voľbu LIVE aj WORK, automatické W=B→LIVE a odmietnutie zastaranej voľby. Normalizovať iba preview host/prefix/banner. | P06 |
| N11 | Merge kategorizácie zachová ID, obnoví väzby len daného dokumentu a nezrkadlí kópie; chyba DB kroku nepublikuje samostatne väzby; zaniknutá skupina zastaví preflight. | P06 |
| N12 | Naplánovaný termín a jeho posun menia `previewAt`; zrušený plán používa aktuálny čas. Skutočný merge nepremenúva autora ani nereštartuje SAVE_DATE/DATE. | P07 |

Nadviazať na [apps/news.js](../../../src/test/webapp/tests/apps/news.js), [news-templates.js](../../../src/test/webapp/tests/components/news-templates.js), [NewsRestControllerTest](../../../src/test/java/sk/iway/iwcm/components/news/NewsRestControllerTest.java) a [NewsTreeServiceTest](../../../src/test/java/sk/iway/iwcm/components/news/NewsTreeServiceTest.java). Pribudne DB integračné overenie skutočného `NewsQuery`; mockovaný count ani kontrola uložených parametrov aplikácie nenahradia render výsledkov. P03 môže používať pracovné fixture riadky, P04 musí dokázať reálne vytvorenie novinky redaktorom, P06 aj jej publikovanie.

Rozšírenie používa pôvodných päť tabuliek a existujúci render. Práca je v jednom SQL integračnom bode, účinnom strome, niekoľkých nadväzujúcich vstupoch a overení. Odhad už základné Novinky zahŕňal; P01 ho spresní aj podľa atribútového SQL a podporovaných databáz. Nevzniká ďalšia implementačná etapa ani generický systém verziovania aplikácií.
