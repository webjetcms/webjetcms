# Dátový model pracovnej vetvy

**Realizácia P02 (4. november 2026):** platné API, migrácia a hranice sú v [P02-vysledky.md](P02-vysledky.md), konkrétne polia v [matici stĺpcov](P02-matica-stlpcov.md). Nasledujúci návrh zahŕňa aj dosiaľ neimplementované etapy; stav kódu určuje [STAV.md](STAV.md).

**Aktualizácia Q20, 11. novembra 2026:** vlastníctvo balíka používa štandardné `domainId`; podpora bez `enableStaticFilesExternalDir=true` sa nevyžaduje. Názov domény slúži na zobrazenie a URL, nie na samostatné oddelenie balíkov.

Stav: technický návrh na posúdenie. Vychádza z uzatvorených Q1–Q19 v [registri rozhodnutí](01-rozhodnutia.md). Názvy nových tabuliek a tried sú navrhované, nejde o existujúci kód.

## Odporúčanie

Pridať päť tabuliek: `branches`, `branch_documents`, `branch_groups`, `branch_media`, `branch_files`. Produkčné `documents`, `groups`, `media` zachovajú dnešný význam aj primárne kľúče. Nepridávame do nich `branch_id`, nemeníme všetky cudzie odkazy na zložený kľúč a nekopírujeme celú databázu.

Pracovná tabuľka mapujúca existujúcu tabuľku sa podľa požiadavky používateľa z 3. novembra 2026 volá **`branch_` + presný názov pôvodnej tabuľky**, vrátane množného čísla:

| Produkčná tabuľka | Pracovná tabuľka |
| --- | --- |
| `documents` | `branch_documents` |
| `groups` | `branch_groups` |
| `media` | `branch_media` |

Pomocné tabuľky bez priameho produkčného náprotivku majú názvy `branches` (balíky), `branch_files` (súbory a adresárové markery) a neskôr `branch_items` (spoločné položky ďalších modulov). `branch_files` mapuje filesystem, nie produkčnú tabuľku `files`. Pravidlo pomenovania neurčuje počet tabuliek budúcich modulov; ich spoločné úložisko zostáva podľa dokumentu 08.

Každá pracovná obsahová tabuľka obsahuje iba zmenené položky. Ukladá pôvodný stav pre trojcestné porovnanie a aktuálnu pracovnú verziu. Stránky, priečinky a médiá majú typované pracovné stĺpce. Základ a ručné vyriešenie konfliktu sú JSON v textovom/CLOB stĺpci; SQL nad JSON nie je potrebné.

Typované pracovné dokumenty sú potrebné najmä pre novinky: účinný obsah sa musí skladať **pred** SQL filtrom, počtom a stránkovaním. Jedna univerzálna tabuľka s JSON by síce mala menej stĺpcov, ale presunula by zložitosť do náhľadu existujúcich aplikácií. Pri priečinkoch a médiách rovnaký tvar uľahčí mapovanie a kontrolu polí.

Päť tabuliek je rozsah jadra, nie predpis kopírovať tabuľky každého budúceho modulu. [Návrh rozšírení](08-rozsirovanie-modulov.md) pridáva s prvým ďalším modulom jednu spoločnú `branch_items` pre explicitné JSON objekty; banner aj obrázok galérie ju môžu zdieľať. Dnešné typy a budúce JSON typy používajú spoločný obal a merge, ale každý typ má iba jedno autoritatívne pracovné úložisko. Prvá migrácia stále vytvára iba uvedených päť tabuliek.

## Čo sa mení v existujúcich tabuľkách

| Tabuľka/úložisko | Navrhovaná zmena |
| --- | --- |
| `documents` | Žiadny stĺpec vetvy a žiadna zmena PK. Bežné zápisy ostanú produkčné. Pribudne podporovaná operácia vloženia nového záznamu s už rezervovaným `doc_id`. |
| `groups` | Žiadny stĺpec vetvy a žiadna zmena PK. Nová explicitná insert operácia s rezervovaným `group_id`; dnešné `groupId > 0` nesmie nesprávne zvoliť UPDATE. |
| `media` | Žiadny stĺpec vetvy a žiadna zmena PK. Merge aktualizuje podľa `media_id`; nepoužije vymazanie celej sady a vytvorenie kópií. |
| `media_group_to_media` | Súčasť zápisu konkrétneho média pri merge. Pracovný zoznam existujúcich kategórií sa uchová s médiom. Definície `media_groups` sa nemenia. |
| `perex_group_doc` | Odvodený index hodnoty `documents.perex_group`; pri merge sa synchronizuje iba pre konkrétny dokument. Pracovné priradenia existujúcich perex skupín sú v `branch_documents.perex_group`, bez novej väzbovej tabuľky. |
| `documents_history`, audit, existujúce indexy | Naplnia sa pri skutočnom publikovaní príslušnými podporovanými operáciami. Nie sú úložiskom vetiev. História vetvy nevyžaduje novú plnú revíznu databázu. |
| `groups_scheduler`, per-page plánované údaje | Nie sú schedulerom balíkov. Pred merge sa kontroluje kolízia; starý plán sa potichu nemení. |
| `pkey_generator` | Existujúci mechanizmus pre médiá potrebuje striktné hlásenie zlyhania a ochranu proti zníženiu čítača; podrobnosti v návrhu ID. |
| Súborový obsah | Pracovné a základné kópie v neverejnom úložisku. Pôvodná logická URL zostáva v HTML. |

Podmienky databázového enginu a konkrétne operácie rezervácie sú v [identifikátoroch](02a-identifikatory.md). „Bez zmeny PK“ neznamená „bez zmeny Java insert ciest“.

## `branches`

Nasleduje logická schéma. Migrácia musí použiť typy a generátory danej podporovanej DB podľa existujúceho XML update mechanizmu.

| Stĺpec | Logický typ | Význam |
| --- | --- | --- |
| `id` | BIGINT, PK | Identita balíka, samostatná od obsahových ID. |
| `domain_id` | INT, NOT NULL | Doména vlastníka podľa `CloudToolsForCore.getDomainId()`. Funkcia vyžaduje `enableStaticFilesExternalDir=true` podľa Q20; repozitár používa `DomainIdRepository`. |
| `name` | VARCHAR(255), NOT NULL | Názov kampane. |
| `description` | TEXT/CLOB, nullable | Poznámka koordinátora. |
| `state` | VARCHAR(32), NOT NULL | Životný cyklus opísaný v merge dokumente. |
| `revision` | BIGINT, NOT NULL | Zvýši sa pri každej zmene obsahu alebo rozhodnutia konfliktu. Nie globálny CMS zámok. |
| `publish_at` | TIMESTAMP, nullable | Jeden termín celého balíka, uložený podľa projektu ako jednoznačný čas; UI ho zobrazuje s časovou zónou. |
| `scheduled_by` | INT, nullable | Používateľ, ktorého práva sa znova overia pri plánovanom spustení. |
| `created_by`, `updated_by` | INT | Pôvodca a posledný editor. |
| `created_at`, `updated_at` | TIMESTAMP | Čas vytvorenia a poslednej úpravy. |
| `merge_started_at`, `merge_finished_at` | TIMESTAMP, nullable | Evidencia posledného/úspešného publikovania. |
| `last_error` | TEXT/CLOB, nullable | Konkrétny problém poslednej kontroly/publikovania. |

Indexy: PK `id`, `(domain_id, state)`, `(state, publish_at)`. Názov nemusí byť jedinečný; ID rozlíši dva rovnaké názvy kampaní. Samostatné členstvá tímu a ACL vetvy sa v prvej verzii nezavádzajú; platia existujúce obsahové práva a nové funkčné právo k obrazovke balíkov.

## Spoločné pracovné stĺpce

Nasledujúce stĺpce majú všetky štyri obsahové tabuľky; nejde o ďalšiu univerzálnu tabuľku. Spoločné Java mapovanie možno vyjadriť malou `@MappedSuperclass` pre metadáta vetvy, ak to nenarúša existujúce mapovanie obsahu.

| Stĺpec | Logický typ | Význam |
| --- | --- | --- |
| `row_id` | BIGINT, PK | Interná identita pracovného riadka. Nikdy sa nepoužije v odkazoch stránky. |
| `branch_id` | BIGINT, NOT NULL | Vlastník pracovného riadka. |
| `operation` | VARCHAR(8), NOT NULL | `UPSERT` alebo `DELETE`. Rozlíšenie nová/zmenená položka plynie z `base_exists`. |
| `base_exists` | BOOLEAN, NOT NULL | Či existovala položka pri zachytení základu. Pre nový objekt false. |
| `base_snapshot` | TEXT/CLOB, nullable | Kanonický pôvodný podporovaný obsah. Pri novom objekte NULL. |
| `base_hash` | CHAR(64), nullable | SHA-256 kanonického pôvodného obsahu. Rýchle porovnanie; obsah pre diff zostáva v snapshote. |
| `updated_by`, `updated_at` | INT, TIMESTAMP | Posledná pracovná úprava. |
| `resolved_snapshot` | TEXT/CLOB, nullable | Pripravený výsledok automatického alebo ručného merge, vrátane rozhodnutia o existencii položky. |
| `resolved_choice` | VARCHAR(8), nullable | Výslovná používateľská voľba `LIVE` alebo `WORK`; NULL znamená použitie automatických pravidiel B/L/W. Túto voľbu vie preview použiť bez SQL nad JSON. |
| `resolved_live_hash`, `resolved_work_hash` | CHAR(64), nullable | Vstupy, ku ktorým sa ručné rozhodnutie vzťahuje. Zmena vstupu zneplatní rozhodnutie. |
| `apply_state` | VARCHAR(16), NOT NULL | `PENDING`, `APPLYING`, `DONE`, `ERROR`. |
| `apply_phase` | VARCHAR(24), nullable | Malý pevný zoznam krokov pre daný typ, napr. dáta/odvodené údaje. Nie všeobecný workflow engine. |
| `applied_hash` | CHAR(64), nullable | Skutočný kanonický výsledok poslednej dokončenej fázy, vrátane medzistavu priečinka pred doplnením default stránky. |
| `apply_error` | TEXT/CLOB, nullable | Zlyhanie tejto položky. |

Prázdny základ, chýbajúca pracovná hodnota a explicitné vymazanie sú tri odlišné stavy. Nevyjadrovať ich iba SQL NULL v obsahových poliach.

`resolved_snapshot` je výsledok merge, nie ďalšia editovateľná verzia. Pri naplánovaní alebo spustení sa vyplní pre každú položku vrátane automaticky vybraných výsledkov bez konfliktu. `resolved_choice` sa ukladá iba pri výslovnej voľbe používateľa; automatický výsledok sa mimo zmrazeného merge plánu vždy znovu odvodí z B/L/W. Pracovné stĺpce sú zdrojom editora. Preklikateľný náhľad podľa Q16 používa zvolený výsledok: aktuálne LIVE alebo typované WORK, podľa mapy rozhodnutí v `03`. Nečíta starý `resolved_snapshot` ako obsah náhľadu.

Vlastnú kombináciu používateľ pripraví bežnou úpravou vo vetve; zvýši sa revision, vyčistí voľba a jej odvodený výsledok a vykoná nové porovnanie. Základ prvej úpravy ostáva zachovaný. Zmena LIVE zneplatní uloženú voľbu cez hash; preview vyžiada nové porovnanie, nesmie ju potichu zameniť za WORK. Chýbajúca položka má osobitný kanonický hash stavu ABSENT; SQL NULL v poli sa s týmto stavom nezamieňa. Pre stránky/priečinky zmazané presunom do koša sa eviduje stav TRASH podľa `04`, nie fyzická absencia riadka.

Spoločné pravidlo projekcie pre detail aj SQL: podporované obsahové polia zo zvolenej verzie, odvodené polia z jej účinných vstupov, prevádzkové a nepodporované read-only polia existujúceho objektu z aktuálneho LIVE. Týka sa aj `sync_id`/`sync_status` a existujúcich `publish_start`/`publish_end`, ktoré merge nemá prepísať pracovnou kópiou. Nový objekt má explicitné počiatočné hodnoty podľa mapovania a `03a`. P02 odovzdá maticu všetkých konkrétnych stĺpcov; všeobecné kopírovanie beanu nie je implementácia tohto kontraktu.

## Dokumenty

`branch_documents` obsahuje `doc_id INT NOT NULL`, spoločné pracovné stĺpce a projekciu dnešných obsahových stĺpcov `documents`. Jedinečnosť `(branch_id, doc_id)`, index `(branch_id, group_id)`, prípadne `(branch_id, virtual_path)` podľa plánu skutočných query. Nevytvárať na pracovnej tabuľke fulltextové indexy pre každú vetvu; všeobecné vyhľadávanie v tomto rozsahu nebuduje samostatný index vetvy.

Presná východisková projekcia je definovaná mapovaním [DocBasic](../../../src/main/java/sk/iway/iwcm/doc/DocBasic.java) a perzistentnými poľami [DocDetails](../../../src/main/java/sk/iway/iwcm/doc/DocDetails.java), overenými proti výslednej schéme po migráciách. Starý `blank_web.sql` sám osebe nie je kompletná aktuálna schéma.

| Skupina polí | Pravidlo |
| --- | --- |
| Obsah a názvy: `title`, `data`, `navbar`, HTML údaje, perex, obrázok, voliteľné polia | Podporovaná úprava. Konflikt sa vyhodnotí za celý podporovaný obsah stránky; diff môže zvýrazniť jednotlivé polia. HTML sa automaticky nespája. |
| Umiestnenie a URL: `group_id`, `virtual_path`, URL generovanie, `external_link` | Podporované, s kontrolou cieľového priečinka a kolízií URL. Odvodené URL sa vypočítajú rovnakou normalizáciou ako pri produkcii. |
| Odkazy na šablóny a obsahové fragmenty | Možno vybrať existujúcu dostupnú šablónu/stránku; definície šablón sa nemenia. Nové stránkové ID vetvy sú použiteľné ihneď. |
| Dostupnosť, navigácia, zoradenie, prístupové nastavenia | Podporované v medziach dnešných oprávnení. Náhľad vetvy sám osebe nenahrádza právo upravovať ani sprístupniť obsah. |
| `perex_group` | Podporované priradenie existujúcich perex skupín; normalizovaná množina ID je súčasťou snapshotu/hashu a konfliktu celej stránky. Definície ostávajú produkčné. Náhľad filtruje pracovné pole, odvodený `perex_group_doc` sa aktualizuje až pri merge. |
| `date_created`, `author_id` | Pri skutočnom pracovnom uložení sa normalizujú podľa dnešného významu posledného uloženia; sú súčasťou pracovného obsahu. Merge zachová hodnoty z vybranej výslednej verzie. Autor/čas publikovania patria do samostatného auditu, nesmú zmeniť News radenie SAVE_DATE. |
| `data_asc`, `root_group_l1..l3`, odvodené `file_name` | Odvodené údaje; nepovažovať ich za nezávislý redakčný konflikt. Prepočítať zo zvoleného výsledku. |
| `views_total`, `views_month`, `forum_count` a prevádzkové príznaky | Neprenášať pracovný snapshot späť do produkcie. Počítadlá sa nesmú vrátiť na staršie hodnoty. |
| `sync_id`, `sync_status`, multigroup, atribúty | V prvom rozsahu nemeniť. Samotný fyzický stĺpec v documents ešte neznamená podporu súvisiaceho workflow. |
| Dátumy individuálneho štartu a konca publikovania | Existujúce hodnoty vo vetve nemeniteľné; čas vykonania publikovania riadi balík. Nová stránka dostane začiatok raz podľa `editorAutoFillPublishStart` (čas vytvorenia alebo NULL), koniec NULL a vypnuté individuálne plánovacie príznaky; nezdedí skrytý plán z predlohy. Existujúca budúca individuálna zmena sa vyrieši pred merge. Event dátum zostáva upraviteľný. |

Implementácia vytvorí explicitný `BranchDocumentMapper` a zoznam podporovaných/odvodených/prevádzkových polí. Test musí zachytiť nové perzistentné pole, ktoré nebolo do jednej kategórie zaradené. Nepoužívať slepé kopírovanie všetkých Java properties pri merge.

Pracovné `available` znamená výslednú dostupnosť stránky po merge; nevnucovať false na ukrytie pracovného obsahu, ktorý je izolovaný samostatnou tabuľkou. Podrobné pravidlá perex kategorizácie, dátumov a mapovania pre `NewsApp`/`NewsActionBean` sú v [03a](03a-novinky.md). Snapshot kanonizuje perex skupiny ako zoradené jedinečné ID, writer zachová dnešný tokenový formát. Pri merge sa overí existencia a dostupnosť odkazovaných skupín v doméne.

## Priečinky

`branch_groups` obsahuje `group_id INT NOT NULL`, spoločné stĺpce a typované perzistentné stĺpce dnešnej tabuľky `groups`. Jedinečnosť `(branch_id, group_id)`, index `(branch_id, parent_group_id)`.

Podporovať názov, navigačný názov, rodiča v rovnakej doméne, URL segment, poradie, existujúcu šablónu, jazyk, dostupné navigačné/prístupové nastavenia, voliteľné polia a default stránku. `groups.domain_name` zostáva obsahovým poľom pôvodného priečinka iba na čítanie; vlastníctvo balíka určuje `branches.domain_id`. Zmeny synchronizačných identifikátorov, schvaľovacích definícií a súborových vlastností priečinka sú mimo rozsahu.

Zmena rodiča či URL segmentu vytvorí aj pracovné zmeny ovplyvnených stránok, ak dnešný workflow ich URL alebo odvodené údaje mení. V prehľade sú označené ako odvodené z presunu/premenovania. Pri merge sa produkčný strom neprepočítava prekvapivo nad inou množinou stránok; noví potomkovia vytvorení medzitým vyžadujú nové posúdenie rozsahu.

Efektívny strom sa skladá z kópie produkčnej štruktúry a pracovných prepisov. Nikdy sa nemenia objekty v globálnom `GroupsDB`. Kontroly: existujúci rodič, zákaz cyklu, default stránka patrí do platného výsledku, presun neprekročí doménu.

## Médiá

`branch_media` obsahuje `media_id INT NOT NULL`, spoločné stĺpce a obsahové stĺpce `media`, vrátane `media_fk_table_name`, `media_fk_id`, odkazov, titulkov, lokalizovaných popisov, poradia a voliteľných polí. Pridať `group_ids_snapshot TEXT/CLOB` so zoradeným zoznamom existujúcich kategórií. Jedinečnosť `(branch_id, media_id)`, index `(branch_id, media_fk_id)`.

Prvý rozsah podporuje vlastníka `documents`. Práva a doména sa odvodia od vlastníckej stránky; samotný `media_id` ani číselný `domain_id` nestačí. Nové médium má vlastné stabilné ID. Dočasné `documents_temp` médiá používateľa sa nesmú miešať medzi produkciou a vetvou; podporovaný editor môže vytvoriť pracovné médium rovno pri konkrétnom rezervovanom `doc_id`.

Kategórie sa pri merge synchronizujú iba pre dané médium. Ak kategória medzitým zanikla, validácia ukáže konkrétnu neplatnú väzbu. Nepridávať druhú správu kategórií do vetvy.

Nové médium potrebuje explicitný INSERT s rezervovaným `media_id`; kladné ID nesmie omylom vyvolať iba UPDATE alebo pridelenie iného ID JPA generátorom. Existujúce médium sa aktualizuje podľa ID. Zápis riadka a jeho kategorizácie je jedna krátka DB operácia, nie vymazanie celej sady príloh stránky.

`media.last_update` je technický údaj viditeľnosti, nie čas redakčnej úpravy vo vetve. [MediaDB.getMedia(DocDetails)](../../../src/main/java/sk/iway/spirit/MediaDB.java) a [media.jsp](../../../src/main/webapp/components/media/media.jsp) filtrujú médiá podľa `last_update <= documents.date_created`. Nastavenie nového média na aktuálny čas by preto skrylo prílohu nezmenenej staršej stránky aj po merge. Pre skutočne zmenený `WORK` UPSERT sa pri publikovaní odvodí `last_update = new Date(owner.getDateCreated() / 1000 * 1000)` z **výslednej zvolenej vlastníckej stránky**, rovnako ako dnes v `PlannedChangesService.publishMedia`. Náhľad použije tú istú projekciu nad vlastníkom zo svojho účinného pohľadu; produkčný JPA objekt sa tým nemení. Dátum ani autor stránky sa kvôli médiu neposúvajú.

Nezmenené médiá, rozhodnutie `LIVE` a no-op zachovajú pôvodný `last_update` aj dnešné filtrovanie. Hodnota sa nepovažuje za editovateľné obsahové pole ani samostatný konfliktový vstup; čas pracovnej úpravy eviduje `updated_at`. Nesmie sa plošne vypnúť dátumový filter pre všetky médiá vo vetve, pretože by sa mohli zobraziť existujúce nepublikované úpravy produkčných médií. Dnešné per-page plánovanie, jeho osobitný náhľad a filtrovanie historických stránok zostávajú zachované.

Existujúci M:N filter médií sa musí adaptovať pred stránkovaním: produkčný join vyhodnotí iba médiá bez pracovného prepisu. Pre zmenené médiá služba vyhodnotí malý zoznam pracovných `group_ids_snapshot` a pripraví zodpovedajúce `media_id`; následne zlúči výsledky a až potom aplikuje poradie, počet a limit. Samotný produkčný join plus výmena už načítaných riadkov nestačí. Pri bežnom zozname príloh jednej stránky je vhodné zlúčenie v pamäti; globálny administračný zoznam potrebuje spoločné podmienky/count aj pre pracovné ID.

## Súbory

`branch_files` obsahuje spoločné pracovné stĺpce, `entry_kind VARCHAR(16) NOT NULL` s hodnotami `FILE` alebo `DIRECTORY`, `logical_path VARCHAR(...)`, `base_content_hash CHAR(64)`, `work_content_hash CHAR(64)`, veľkosť a MIME informáciu. Adresárový marker používa tú istú tabuľku; nemá binárny obsah a jeho obsahové hashe, veľkosť a MIME sú NULL. Jeho kanonický stav rozlišuje existenciu a druh položky na danej ceste. Dĺžku cesty a indexovanie zladiť s podporovanými DB a existujúcimi CMS limitmi; nepoužívať neoverený univerzálny index na dlhom UTF-8 VARCHAR.

Odporúčanie: `path_key CHAR(64)` = SHA-256 normalizovanej logickej cesty, UNIQUE `(branch_id, path_key)`, pričom service vždy porovná aj plnú uloženú cestu. Tak netreba rozdielne indexy dlhých ciest podľa databázy. Normalizácia zachová pravidlá case sensitivity backendu, odstráni dot-segmenty a vyhodnotí povolený obsahový koreň; nesmie prepustiť cestu mimo neho. Koncová lomka nevytvorí druhú identitu toho istého adresára; súbor a adresár nemôžu obsadiť tú istú normalizovanú cestu.

Neverejné úložisko sa odvodí serverom napr. `<private-storage>/branches/<branchId>/files/<rowId>/base` a `/work`. `rowId` identifikuje uložené bajty; používateľ neposiela fyzickú cestu. Na produkčnom multi-node nasadení musí byť tento priestor dostupný publisheru a uzlom náhľadu. V prvej podporovanej konfigurácii použiť spoločný filesystem, rovnako ako existujúce plánované súbory; IwcmFsDB storage je osobitné rozšírenie, nie tichý fallback do produkcie.

Doména sama neurčuje vlastníctvo súboru: dve domény môžu mať ten istý fyzický `/images/logo.png`. Podľa Q13 použijeme dnešné mapovanie cez `Tools`/`FilePathTools`, povolené redakčné korene a existujúce adresárové práva. Nepridáva sa nová mapa vlastníctva súborov. Ak nasadenie používa spoločný fyzický priestor, UI pri súborových zmenách upozorní, že publikovanie ovplyvní všetky weby používajúce danú cestu. Taká zmena je dovolená oprávnenému používateľovi. Limit jednej domény sa týka obsahového stromu a kontextu vetvy; nie je prísľubom doménovej izolácie už zdieľaných súborov. Spoločné System stránky mimo doménového rozsahu zostávajú read-only. Neskúmame všetky potenciálne odkazy na súbor z iných webov.

- `FILE/UPSERT`: pracovný obsah existuje, prípadná pôvodná kópia a hash sa zachytia pri prvej zmene.
- `FILE/DELETE`: viditeľný tombstone, náhľad nepadne späť na produkčný súbor.
- Presun/premenovanie jednotlivého súboru: `DELETE` starej cesty a `UPSERT` novej. Kopírovanie jednotlivého súboru vytvorí iba cieľový `UPSERT`. Zachovanie identity binárneho súboru sa nezavádza.
- `DIRECTORY/UPSERT`: iba vytvorenie prázdneho adresára cez `mkdir`. Marker je viditeľný aj po obnovení administrácie bez potreby prvého uploadu. Pri merge sa adresár vytvorí; už existujúci adresár na rovnakej ceste je po kontrole práv prípustný no-op. Súbor na tejto ceste je kolízia, nie cieľ na prepísanie.

Podľa Q17 prvá verzia podporuje `mkdir` a operácie s jednotlivými súbormi. Presun, premenovanie, mazanie a rekurzívne kopírovanie súborových adresárov server odmietne; adresárový `DELETE` nie je podporovaná položka. Pracovný listing zohľadní explicitné adresárové markery aj rodičovské cesty pracovných súborov. Nový adresár dedí dnešné LIVE vlastnosti najbližšieho predka vrátane ochrany; neukladá sa snapshot `dirprop` ani nová tabuľka adresárových vlastností. Odobratie markeru z balíka sa odmietne, ak od neho závisia ponechané pracovné súbory alebo adresárové markery. Inak sa odstráni iba pracovný marker, nikdy produkčný adresár.

Podľa Q18 sú galerijné cesty v prvej verzii iba na čítanie až do samostatného E03. Zákaz sa overí pre každú logickú cestu, ktorú operácia mení, pomocou dnešnej klasifikácie [GalleryDB.isGalleryFolder](../../../src/main/java/sk/iway/iwcm/gallery/GalleryDB.java), vrátane zdedených nastavení podadresárov. Platí pre hlavný obrázok, `o_` originál aj `s_` variant a pre založenie adresára v galérii. Obísť ho nesmie priamy upload, overwrite, správca súborov ani premenovanie/presun do alebo z galérie. Bežné obrázky a prílohy mimo tohto rozsahu naďalej používajú pracovnú súborovú službu; galerijný metadata writer sa nezapína potichu.

Pracovné nahrávanie zapisuje temp súbor a až po úspechu nahradí pracovnú kópiu. Neúspešný upload neznehodnotí predošlú pracovnú verziu. Hash sa používa pre konflikt obsahu; rovnaká veľkosť a mtime samy osebe nie sú dôkaz rovnakých bajtov.

## Zloženie účinných údajov

Pre každú identitu platí:

```text
pracovný DELETE        -> položka v náhľade neexistuje
pracovný UPSERT        -> pracovná verzia, pôvodné stabilné obsahové ID
žiadny pracovný riadok -> aktuálna produkčná verzia
```

Príklad: produkčný docId 42 má názov A. Vo vetve zmením názov na B, základ je A. Produkcia neskôr zmení názov na C. Náhľad vetvy ukazuje B, merge ukáže A/C/B. Iná stránka 43, ktorú som vo vetve nemenil, ukazuje priebežne dnešnú produkciu.

Prvá zmena sa musí uložiť proti verzii zobrazenej v editore. GET vráti hash pôvodného podporovaného obsahu; pri prvom SAVE sa porovná. Ak sa produkcia medzi otvorením a uložením zmenila, editor vyžiada obnovenie/porovnanie. Inak by sme zachytili nový základ spolu so starými hodnotami formulára a stratili schopnosť rozpoznať konflikt. Nie je potrebný dlhý databázový zámok.

Vrátenie všetkých pracovných polí na dnešnú produkciu môže používateľ potvrdiť akciou „Odobrať z balíka“; pracovný riadok sa odstráni. Pri novom objekte sa rezervované ID znovu nepoužíva. Zrušená vetva môže zanechať medzery v číselnej rade, čo je normálne.

## Migrácia a zapnutie

1. XML migrácia pridá päť tabuliek a indexy. Nekopíruje existujúci web.
2. Feature flag ostane vypnutý, kým neprejdú overenia rezervácie ID, izolácie a náhľadu podporovanej šablóny.
3. Kontrola pripravenosti overí DB engine/verziu, ID generátory, práva explicitného insertu, private filesystem a smerovanie preview gateway.
4. Chýbajúca podmienka zablokuje zapnutie funkcie s konkrétnym vysvetlením. Automatická konverzia veľkých zákazníckych tabuliek alebo zmena proxy nie je súčasťou migrácie.
5. Pri vypnutí funkcie produkcia ďalej číta pôvodné tabuľky. Rozpracované vetvy zostávajú uložené; schéma ani obsah sa automaticky nemažú.

Uchovanie uzavretých vetiev bude explicitná správcovská operácia. Prvá verzia môže mať ručné odstránenie uzavretej vetvy a jej pracovných bajtov; netreba na začiatku zavádzať automatickú retenciu, delta kompresiu ani deduplikačný storage.
