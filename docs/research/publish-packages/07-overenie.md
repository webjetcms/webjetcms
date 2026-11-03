# Overenie budúcej implementácie

**Aktuálny stav: testovací plán.** Tu uvedené scenáre sa v návrhovej fáze nespúšťali. Overené boli zdrojové integračné body a odkazy dokumentácie, nie fungujúca pracovná vetva.

Použiť reálnu podporovanú databázu, existujúce Java testovacie zázemie a CodeceptJS/Playwright. H2 nie je náhrada za správanie identity, sekvencií, CLOB/UNION alebo transakcií. Čistý merge algoritmus a kanonizáciu možno testovať bez DB.

## Akceptačné prípady

| ID | Scenár | Požadovaný výsledok |
| --- | --- | --- |
| T01 | ID existujúcej a novej stránky, priečinka a média; produkčný insert medzi dvoma pracovnými vytvoreniami; reštart DB | Všetky identity sú odlišné a po merge zostanú rovnaké. Pred merge neexistuje nový produkčný obsahový riadok. |
| T02 | Uloženie stránky, priečinka, média a upload vo vetve | Produkčný obsah, kategorizácia, história, galéria a indexy sa nezmenia. Výnimkou sú technická spotreba ID a audit práce s vetvou. |
| T03 | Skutočný `menu_ul_li.jsp`/`MenuULLI`, M01–M11 z [03b](03b-menu.md); preklik cez menu, breadcrumb, novinky a default/premenovanú stránku | Rovnaký zvolený výsledok vetvy ovplyvní položky, názvy, poradie, aktívnu cestu, viditeľnosť, HTML/perex a odkazy. Nový/zmazaný obsah sa zobrazí konzistentne, vrátane `onlySetVariables`. |
| T04 | Vetva A, vetva B a produkcia; zmena neupravenej produkčnej položky | Pracovné verzie sa neprepisujú; neupravená položka sa aktualizuje z produkcie; editor uchová pracovný prepis, PREVIEW rešpektuje zvolený výsledok podľa Q16. |
| T05 | Obmedzený redaktor, cudzia doména/ID, anonymný preview request, priamy asset/thumbnail request | Server odmietne neoprávnené čítanie/zápis; náhľad ani obrázok neobídu autentifikáciu. |
| T06 | Formulár otvorený v produkcii alebo branch, potom prepnutie kontextu v druhom tabe | Starý request sa odmietne s možnosťou zachovať obsah formulára; nikdy sa neuloží do iného kontextu. |
| T07 | Nové/nahradené/zmazané obrázky a PDF; CKEditor upload, moderný upload, elFinder; verejná cache/CDN | Produkcia vracia pôvodné bajty, gateway bajty zvolené mapou LIVE/WORK. DELETE nepadne na produkciu. Thumbnail ani `304` nepoužije cudziu verziu. |
| T08 | Merge matica B/L/W, vrátane nových objektov, mazania a neskoršej zmeny po vyriešení konfliktu | Pravidlá celého objektu sú dodržané; zastarané rozhodnutie sa neprenesie na nový obsah. Zmena dvoch rôznych polí v produkcii/vetve je v tejto verzii tiež konflikt. |
| T09 | Médiá: titulok, poradie, kategorizácia, nové médium, vymazanie, vlastník novej stránky | Upsert zachová `media_id`; DELETE ovplyvní iba konkrétne médium a väzby, nie automaticky súbor. Pracovná kategorizácia ovplyvní filtre/count. |
| T10 | SQL/JPA filter, count, sort a paging noviniek/admin zoznamu s novou a zmazanou položkou | Výsledok vzniká nad účinnou množinou pred limitom. Nové položky sa nestratia a zmazané nezostanú v počte. |
| T11 | Presun/premenovanie/mazanie priečinka, default stránka, nový potomok v produkcii, kolízia URL | Všetky odvodené zmeny sú viditeľné; neplatný strom alebo zmenená množina potomkov zastaví merge. Existujúce ID sa nezmenia. |
| T12 | Spoločný termín, posun, zrušenie, vypnutý používateľ, odobraté právo, súbeh s per-page plánom | V čase spustenia sa preverí aktuálny stav; problém zastaví balík bez slepého prepísania. Termín sa mení jednou operáciou. |
| T13 | Produkčné uloženie/merge na webe so schvaľovaním, zrkadlením a multigroup | Dnešné produkčné workflow zostane zachované. Branch merge neobíde práva a nevytvorí neviditeľné preklady/kópie; nepodporovaný multigroup zápis sa odmietne. |
| T14 | Feature flag off/on, odmietnutý engine/práva, rovnaká konfigurácia uzlov, dirty-read helpery | Vypnutá funkcia nemení produkčný režim; zapnutie bez splnených predpokladov zlyhá konkrétnou správou. Technický ID riadok sa nedostane do core čítania/cache/indexu. |
| T15 | Chyba medzi položkami merge, chyba po zápise dát pred cache, reštart a pokračovanie, ďalšia produkčná zmena | UI rozlíši nedokončené a dokončené kroky; pokračovanie neprepíše novšiu produkciu a znovu neprideľuje ID. Cache/index možno obnoviť bez opätovného nevhodného obsahového zápisu. Po čiastočnom publikovaní je obsah/štruktúra vetvy uzavretá; znovu vybrať LIVE/WORK možno iba pre nedotknuté položky. |
| T16 | Kompletná `NewsApp`/`NewsActionBean`: N01–N12 z [03a](03a-novinky.md) | Nové/upravené/zmazané novinky, účinné priečinky, kategórie, dátumy, práva a filtre ovplyvnia render, count a paging. Admin iframe zachová vetvu. Pri rovnakých vstupoch sa náhľad zhoduje s produkciou po merge; zachová autora, dátumy a perex väzby. |

T16 sa overuje postupne: SQL fixture v P01, render/čítanie v P03, reálna editácia v P04, médiá v P05, porovnanie po merge v P06 a termín v P07. Nie je to až koncový voliteľný test. Presná matica a úmyselné hranice zákazníckych kritérií sú v [03a — Novinky](03a-novinky.md).

T03 zahŕňa konkrétny menu kontrakt z [03b](03b-menu.md): P01 overí základ cez JSP, P03 M01–M09 s fixture, P04 editor parametrov M10 a skutočné redakčné zmeny, P06 rozhodnutia Q16 a zhodu po merge M11. Nadviazať na [apps/menu.js](../../../src/test/webapp/tests/apps/menu.js), ale doplniť render a prekliky; kontrola parametrov ani admin stromu nestačí. Menu nepoužíva `previewAt` Noviniek ako nový časový filter.

## Pripravenosť na ďalšie moduly

V rámci P02/P06/P08 overiť kontrakt z [08](08-rozsirovanie-modulov.md): všetky dnešné typy používajú spoločné porovnanie/stav/UI a svoje jediné úložisko; neznámy typ sa pri príprave nepreskočí; súborová služba sa neocitne v DB transakcii. Súčasná akceptácia nevyžaduje generickú tabuľku ani implementované budúce moduly.

Pri neskoršom E01/E02 sa doplnia reálne testy JSON snapshotu/verzie, nového a upraveného bannera, jeho obrázka a spoločného merge; E03 pridá galerijné metadáta, súbory a ID. Podľa výslovného pokynu používateľa bannerové štatistiky zostanú bez úprav: nulové zápisy štatistík z preview nie sú akceptačným kritériom tohto rozšírenia. Obsahový merge existujúce počítadlá neprepisuje. Presné budúce scenáre určuje dokument 08.

## Jednotkové testy

Najväčšiu hodnotu majú čisté testy trojcestného merge a kanonického obsahu. Parametrizovaná matica pokryje:

- W = B → produkcia; L = B → pracovná verzia; L = W → žiadny nový obsahový zápis; ostatné rozdiely → konflikt.
- chýbajúci základ, nové ID, pracovná DELETE, zmazanie v produkcii a rovnaké zmazanie na oboch stranách;
- NULL, prázdny text, časové údaje, usporiadanie kategórií médií, odlíšenie ABSENT od NULL poľa;
- prevádzkové počítadlá a zmeny odvodených polí nevytvoria redakčný konflikt ani sa neprepíšu zo snapshotu;
- ručná voľba platí len pre aktuálny work/live hash a revíziu, kým ju ďalšia úprava nezneplatní.

Netreba testovať súkromné gettery alebo presné rozdelenie tried. Test má dokazovať správanie, ktoré môže pri refaktoringu zlyhať.

## DB integračné overenie

Povinné pre každú deklarovanú DB: migrácia, kanonický read/write, CLOB/text projekcia v účinnom zdroji dokumentov, filter/count/paging, rezervácia ID a explicitný insert, zachovanie runtime polí a média s kategorizáciou. Osobitné scenáre ID sú v [02a](02a-identifikatory.md).

Rezervačný test cielene podrží transakciu po technickom inserte a z druhej connection spustí produkčný lookup, novinky a relevantný index/export. Vrátený výsledok nesmie obsahovať nepotvrdený riadok. To je cielený test skutočnej závislosti, nie záťažový test stoviek redaktorov.

MySQL/MariaDB: overiť reálny engine existujúcich aj nových dotknutých tabuliek. SQL Server: overiť explicitný insert, oprávnenie ALTER a upratanie `IDENTITY_INSERT`. PostgreSQL/Oracle: používať skutočnú sekvenciu aktuálnej schémy. Neúspech jedného profilu nie je dôvod potichu znížiť deklarovanú podporu.

## E2E overenie

Nové scenáre umiestniť napr. do `src/test/webapp/tests/webpages/branches/`; názov adresára je návrh. Použiť existujúce `I`, `DT`, `DTE`, `Document` a `Browser` helpery a postup z `wj-codeceptjs` pri realizácii. Používateľove súčasné lokálne úpravy testov sa zachovajú.

Minimálny spoločný príbeh:

1. Cez `Nová pracovná verzia` vytvoriť prázdnu vetvu a overiť, že admin výber ostal pôvodný. Aktivovať ju cez `Upravovať web`, upraviť existujúcu stránku a založiť nový priečinok s dvoma stránkami; zmeny sa do balíka pridajú uložením bez osobitného zaraďovania.
2. Nahrať obrázok a PDF, pridať médium pod stabilným ID.
3. V náhľade prejsť z úvodnej stránky cez reálne `menu_ul_li.jsp`/`MenuULLI` na novú stránku a stiahnuť pracovné PDF. Novú dostupnú novinku s perexom a existujúcou perex skupinou nájsť aj cez reálnu aplikáciu Novinky, jej filter a stránkovanie.
4. Vedľa otvoriť produkciu: žiadne pracovné údaje ani bajty sa nezobrazia.
5. Upraviť existujúcu produkčnú stránku, zistiť konflikt, vybrať správnu celú verziu.
6. Publikovať a overiť ID, strom, mediálne/perex väzby, verejné URL a súbory. Porovnať menu pred merge a po ňom podľa M11 vrátane default odkazu a aktívnych tried. Pri rovnakom T, identite a šablóne porovnať predmerge a postmerge Novinky vrátane poradia, count, autora a dátumov.
7. V samostatnom scenári naplánovať balík, spôsobiť nový konflikt a overiť zastavenie.

Rovnakú prílohu otestovať aj cez thumbnail a po predchádzajúcom zahriatí produkčnej cache. HTML nesmie obsahovať fyzické pracovné cesty. Nepodporovaný formulárový zápis z preview nesmie odoslať email či vytvoriť produkčnú objednávku.

UI scenáre doplnia druhý vstup vytvorenia cez prepínač, návrat na produkciu, neuložený formulár, read-only naplánovanú verziu a uzavretie práve vybranej verzie. V poslednom prípade starý SAVE zlyhá a neuloží do LIVE; prechod na produkciu je výslovný. Podrobnosti sú v častiach 1–2 a akceptácii [05](05-rozhranie.md).

Pri vytváraní otvoriť dialóg pre doménu A a v druhej záložke zmeniť doménu na B. Odoslanie pôvodného formulára vráti `409`, zachová jeho text a nevytvorí balík v nesprávnej doméne.

Spresnenia po kontrole návrhu (patria do existujúcich T01–T16, nejde o ďalšiu etapu):

- **T03/T08/T16:** po voľbe `Použiť produkciu` sa stránka, URL, menu, News list/count/paging, médiá a súbory zhodujú so zvoleným výsledkom. Editor naďalej uchová WORK. Otestovať aj automatické W=B→LIVE; pri novom nevyriešenom konflikte náhľad jasne označí pracovný predpoklad. Zmena LIVE po uloženej voľbe zastaví náhľad s odkazom na nové porovnanie, nepoužije neplatné rozhodnutie.
- **T03/T05:** centrálna administrácia na inom hostiteľovi než verejný web; jej session otvorí náhľad správnej logickej domény aj po prepnutí admin domény. Overiť `RequestBean`, `DocDB.getDomain`, routovanie, absolútne odkazy a média bez prenesenia prihlásenia na verejný hostiteľ.
- **T05:** na povolenej stránkovej preview URL odmietnuť `historyid`, `forward`, `forwarddoccompare` a `inlineEditorAdmin`, pričom bežné News page/search/filter parametre ostanú funkčné.
- **T06:** stará LIVE obrazovka domény A, druhý tab prepne na LIVE domény B. Oba majú branchId=0, ale pôvodný request musí vrátiť `409` pre nesúlad domény.
- **T07:** nové PDF prejde cez `MediaDB.filterByAvailability`; perex obrázky small/normal/original a thumbnail s nakonfigurovaným externým serverom zobrazia pracovné bajty bez produkčnej cache. Prázdny adresár zostane v branch liste po obnovení, nevznikne na produkcii a publikuje sa pred jeho súbormi. Galerijné cesty, rekurzívne operácie adresárov a vedľajšie zápisy redirect/file_atr/dirprop server odmietne alebo v podporovanej operácii vôbec nespustí.
- **T09/T16:** pridať/upraviť médium starej stránky bez uloženia stránky. WORK médium použije `lastUpdate` odvodený z výsledného `dateCreated` vlastníka na sekundy; overiť vlastníka z WORK aj zvoleného LIVE. Nezmenené LIVE médium s budúcim `lastUpdate` musí zostať skryté dnešným cutoffom. Autor a SAVE_DATE stránky sa neposunú.
- **T10/T16:** po prvej pracovnej úprave zmeniť v produkcii read-only `publish_start`/`publish_end` alebo prevádzkový údaj. Účinný detail aj SQL používajú aktuálne LIVE hodnoty, ktoré zachová merge; nová stránka má dohodnuté počiatočné hodnoty.
- **T11/T15:** zmazať viacúrovňový podstrom a obnoviť ho dnešnou obnovou z koša. Zachovať parent väzby potomkov, vlastnícke groupId stránok, pôvodnú dostupnosť a všetky ID. Overiť chybu medzi zneprístupnením stránok a presunom koreňa. Samotná kontrola nesmie vytvoriť kôš ani iný produkčný záznam.
- **T12:** balík mení iba súbor, ale individuálny plán stránky mimo balíka cieli na rovnaký fyzický súbor. Príprava aj spustenie konflikt ukážu; plán sa nezruší. Oddelené doménové úložiská s rovnakou logickou cestou nevytvoria falošný konflikt.
- **T02/T13:** s `fulltextExecuteApps` a nakonfigurovaným fulltext hookom nevykoná branch save živé JSP, loopback request ani zákaznícky zápis. Podporované odvodenie textu a následné indexovanie sa overia osobitne.

Pred merge načítať dokument a médium aj cez ich produkčné JPA repositories. Po JDBC publikovaní musí nové repository čítanie a verejný render vrátiť ten istý nový obsah aj kategorizáciu; tým sa overí invalidácia persistence cache, nie iba HTML cache.

Existujúce regresné súbory, vyberať podľa skutočne upraveného toku:

- [publishing.js](../../../src/test/webapp/tests/webpages/publishing.js), [planned-publishing.js](../../../src/test/webapp/tests/webpages/planned-publishing.js), [planned-publishing-new-page.js](../../../src/test/webapp/tests/webpages/planned-publishing-new-page.js);
- [approve-logic.js](../../../src/test/webapp/tests/webpages/approve-logic.js), [group-approve-logic.js](../../../src/test/webapp/tests/webpages/group-approve-logic.js), [webpage-perms.js](../../../src/test/webapp/tests/webpages/webpage-perms.js);
- [media.js](../../../src/test/webapp/tests/webpages/media.js), [media-groups.js](../../../src/test/webapp/tests/webpages/media-groups.js), [virtual-path.js](../../../src/test/webapp/tests/webpages/virtual-path.js), [webpage-history.js](../../../src/test/webapp/tests/webpages/webpage-history.js);
- [multigroup.js](../../../src/test/webapp/tests/webpages/multigroup.js) a [mirroring.js](../../../src/test/webapp/tests/webpages/mirroring.js), ak sa mení oddeľovanie udalostí.
- [apps/news.js](../../../src/test/webapp/tests/apps/news.js), [news-templates.js](../../../src/test/webapp/tests/components/news-templates.js), [NewsRestControllerTest](../../../src/test/java/sk/iway/iwcm/components/news/NewsRestControllerTest.java) a [NewsTreeServiceTest](../../../src/test/java/sk/iway/iwcm/components/news/NewsTreeServiceTest.java); doplniť skutočné SQL a render výsledkov, nie iba uloženie parametrov alebo mockovaný count.

## Nástroje a evidencia

Presné aktuálne príkazy sú v [pracovnom návode](../../../.github/agents/CONTRIBUTING.md). Typické budúce overenie:

```sh
./gradlew compileJava
./gradlew test --tests 'sk.iway.iwcm.branch.*'
npm --prefix src/main/webapp/admin/v9 run prod
```

Vzor CodeceptJS z `src/test/webapp` po spustení testovacieho CMS:

```sh
CODECEPT_URL='https://hostname-testovacieho-webu' CODECEPT_SHOW=false \
  npx codeceptjs run tests/webpages/branches/ --steps
```

Balík testov, hostname a cesty vzniknú až pri implementácii. Príkazy sú návody, nie tvrdenie, že už existujú nové triedy alebo testy. DB profil a prihlasovanie sa prevezmú z bezpečne nakonfigurovaného prostredia; tajné hodnoty sa nezapisujú do dokumentácie ani testovacích logov.

Pri uzavretí kroku zapísať príkaz, prostredie/DB verziu, výsledok, cestu k reportu a nevykonané prípady. Kompilácia nenahrádza úspešný DB test a úspešný backend test nenahrádza overenie klikateľného náhľadu.
