# P06 — Porovnanie a manuálny merge

Stav: **DONE na uvedenom POC profile**, 9. november 2026. Východisko `3c1dbdb7b` (P05), vetva `feature/58858-publishing-packages`. Rozsah končí ručným publikovaním; plánovanie P07 sa nezačalo. Zmeny zostávajú necommitnuté.

## Implementácia

- `BranchMergeService` načíta čerstvé BASE/LIVE/WORK, porovná celý objekt, uloží voľbu viazanú na oba vstupné hashe a pripraví úplný výsledok. Úprava v bežnom editore naďalej mení WORK. Existujúca spoločná mapa rozhodnutí okamžite mení preklikateľný náhľad, Novinky, menu, médiá a súbory; zastaraná voľba vyžiada nové porovnanie.
- Kontrola celého balíka zahŕňa aktuálne práva publikujúceho, schvaľovanie zdrojových aj cieľových priečinkov, existenciu šablón/kategórií, výsledný strom, default stránky, referencie, URL v príslušnej doméne, rezervované ID, pribudnutých potomkov, súborové bajty a individuálne plány a čakajúce schválenia stránok/priečinkov. Dostupnosť kategórie sa overuje aj vo výslednom priečinku. Súborové plány sa kontrolujú aj pre stránky mimo balíka a znovu pred dotknutým súborovým krokom.
- `BranchPublishService` ukladá nemenné ciele a prevzatie vetvy v jednej krátkej transakcii. Jeden executor vykoná pevné poradie z dokumentu 04. Každý DB krok spája obsah, mediálne väzby, históriu a checkpoint na jednom spojení. Po úspešnom commite volá existujúce `Adminlog.add(...)` so štandardnými auditnými účinkami; dokončenie stránky používa existujúce perex API. Celý balík ani filesystem nepatria do globálnej transakcie.
- Existujúce objekty sa aktualizujú, nové používajú rezervované ID. Ak pri presune priečinka vyhrá LIVE, obsah zostane produkčný a prepočítajú sa len potrebné odvodené údaje umiestnenia. Skutočný no-op nevytvára obsahový zápis ani históriu. Zvolený autor, SAVE_DATE, perex skupiny a mediálne ID/kategórie sa zachovajú. Zmena samotného média starej stránky neposúva dátum stránky. Perex väzby sa zapisujú len danému `doc_id`; nevznikajú zrkadlené kópie ani preklady.
- Súbor sa zálohuje, overí a nahradí cez temp súbor. Pri chybe medzi výmenou a checkpointom pokračovanie rozpozná vlastný výsledný hash. Originály a pracovné súbory zostávajú dostupné. Pri zapnutej bežnej file-history sa pred zmenou súboru volá pôvodné `IwcmFsDB.saveFileHistory(file, false)`, potom sa uloží počiatočný súborový checkpoint.
- Mazanie podstromu najskôr zneprístupní pripravené stránky, potom presunie iba koreň pod kôš. Stránky ostanú vo svojich priečinkoch, vnorení rodičia aj ID sa zachovajú. Úplné obnovovacie snapshoty sú povinné aj pri vypnutej bežnej histórii. Tesne pred koreňovým krokom sa zamknú a overia aj už zneprístupnené stránky; cudzia obsahová zmena sa neprevezme ako vlastný medzistav.
- Explicitné účinky obnovujú DocDB/GroupsDB, cache, Spring aj legacy EclipseLink cache a nakonfigurovaný Lucene index. Bežné produkčné save API ostáva zachované; publisher neprehráva všeobecnú sadu editorových listenerov.

## UI a API

Prehľad otvorí **Zmeny** pri aktívnej verzii alebo **Spravovať pracovné verzie → Detail**. Obsahuje typové filtre, tri stavy položky, rozhodnutie Produkcia/Pracovná verzia, bežný editor, náhľad a potvrdenie presného publikačného zoznamu. Súbory majú samostatné autentifikované BASE/LIVE/WORK verzie. Pri chybe vidno hotové, chybné aj čakajúce položky a akciu na nové overenie a pokračovanie.

Nové riadiace endpointy pod `/admin/rest/branches/{id}`: `GET` detail, `POST /prepare`, `POST /resolve`, `POST /merge`, `POST /resume`, `GET /progress`, `GET /file-version`. Mutácie kontrolujú CSRF, revíziu a príslušné hashe; spustenie vracia `202`. Zatvorenie prehliadača nezastaví worker. Tieto cesty používajú technické LIVE čítanie nezávisle od aktuálnej editovanej vetvy.

Priamy odkaz používa fragment `#branchCompare={id}`. Existujúce admin kontroly Referera sa nerozširujú o nový voľný query parameter.

## Prostredie

MariaDB **12.2.2/InnoDB**, Connector/J **3.5.10**, Java **17.0.18**, jedna inštancia **127.0.0.1:33316/webjet_p01**, lokálny filesystem, `poolman-local-p01.xml`, šablóna `aceintegration/jet/blank.jsp`. Pred začatím boli overené listenery DB PID 59488 a pôvodného POC servera PID 79456 na 8086/8446. Java zapisovacie testy bežali pri zastavenom app serveri, sekvenčne. DB 3306 sa nepoužila. Predvolené zdrojové feature flagy zostali vypnuté.

## Overenia

| Oblasť | Výsledok a dôkaz |
| --- | --- |
| Spoločná Java sada | **154 celkom, 139 PASS, 0 zlyhaní/chýb, 15 zámerných skipov**. Zahŕňa pôvodnú sadu P05 a P06. `build/test-results/p06/java-final/`, `java-final.log`. |
| Cielené P06 | **13 aktívnych PASS**: balík s 20 stránkami, presunom priečinka, existujúcim/novým médiom a prílohou; konflikt a stale voľba; práva/plány/URL/nový potomok; chyba v polovici; voľba LIVE pri presune s obnovou odvodených údajov; skutočný no-op bez novej histórie; tri súborové konflikty; výmena súboru pred checkpointom; finálne účinky; rollback stránky s perex väzbami; cudzia doména; obsadenie rezervovaného ID pri pokračovaní; podstrom s vypnutou históriou. `java-final/TEST-sk.iway.iwcm.branch.BranchMergeTest.xml`. |
| T01/T09 | Pôvodné aj rezervované ID stránok/priečinkov/médií zachované. Overené poradie média 0, kategorizácia aj nové médium na novej stránke. Starší vlastník média nemá WORK stránkový riadok a jeho autor/dátum sa pri merge nemení. |
| T08 | Matica celého objektu a ručné LIVE/WORK; zmena podkladu odmietne starú voľbu aj preview. Nový DB riadok pod rezervovaným ID sa nedá prepísať ani cez resolve po čiastočnom merge. |
| T11/T13 | Úplný preflight odmieta nový produkčný podstrom, kolíziu URL, chýbajúcu kategóriu a cudzie individuálne plány; existujúce schvaľovanie sa neobíde. Rovnaká URL v inej doméne je povolená. Podporované zápisy nevolajú zrkadlenie/preklady; multigroup zostáva blokované. |
| T15 | Po chybe pri desiatej stránke hotové zápisy/história nepribúdajú druhýkrát. Novšia produkčná zmena zastaví pokračovanie. File crash recovery nemení znova mtime. Zlyhanie po finálnych účinkoch opakuje dokončenie bez obsahových zápisov. |
| N11 | Pôvodný odovzdávací beh overil spoločný rollback stránky/perex väzieb SQL triggerom. Po review sa tento kontrakt zjednodušil: perex používa existujúce API po commite stránky, pozri záznam nižšie. Zaniknutá perex skupina naďalej zastaví preflight. |
| Kôš | DB overenie zachováva vnorenie a povinné snapshoty pri `editorDisableHistory=true`, vrátane chyby pred presunom koreňa a cudzej úpravy tesne pred koreňovým krokom. Reálna obnova cez UI prešla pri vypnutej histórii aj počas obnovy; následne sa runtime nastavenie vrátilo na pôvodné false. |
| Frontend | Produkčný build úspešný, dve existujúce size warnings. Mockované UI kontroly 1360/1100/700 px, klávesnica a bezpečné zobrazenie HTML prešli; tieto dôkazy sa nevykazujú ako DB/browser publikovanie. `frontend.log`, `ui-mock/`. |

**Finálny CodeceptJS/Playwright beh: 5/5 PASS v jednom spoločnom behu**, `build/test-results/p06/browser-final.log`, screenshoty `browser-final/`, dôkazy `browser-*.json`:

- **N10/N11 a M09/M11:** balík **#1661** publikovaný cez skutočné tlačidlo UI. Stránka **#1945764** vyhrala WORK, stránka **#1945765** LIVE. Predmerge náhľad a verejný výsledok majú presne rovnaké celé HTML News aj menu, vrátane whitespace, ID, poradia, počtu **21 položiek News** (20 stránok a starší vlastník média), titulkov, perexov, autorov/dátumov, médií, aktívnych tried a odkazov. Odstránili sa iba hostiteľ a transportný prefix náhľadu; banner je mimo porovnávaných komponentov. Dáta majú pevný dátum z roku 2020 a `publishType=all`, počas porovnania sa nemenili časové vstupy ani identita/šablóna. Zahriaty skutočný DocDetails repository aj verejný render po merge čítajú nový výsledok.
- **Médium bez SAVE:** vlastník **#1945763** má po merge stále autora **1**, SAVE_DATE **1577880000000** a rovnaký obsah/kategorizáciu. Vo WORK nemal stránkový riadok; zmenilo sa iba jeho médium.
- **Kôš:** balík **#1664**, obnovený koreň **#1943278**, vnorený priečinok **#1943281**, stránka **#1945830**. Skutočná administrátorská obnova zachovala rodičov, plný obsah, dostupnosť, autora, dátum aj všetky ID. Bežná história bola vypnutá pri zmazaní aj obnove.
- **T15 medzi procesmi:** vlastný balík **#1663** zlyhal po desiatej stránke v samostatnom ukončenom Java testovacom procese. Nový app server obnovil trvalý priebeh a UI úspešne dokončilo zvyšok. Stránka **#1945808** má po pokračovaní stále presne **1 historický a 1 publikačný auditný záznam**. Jej plný repository výsledok sa nezmenil; všetkých 25 položiek je DONE.
- **UI a hranice:** tri autentifikované súborové verzie, mobilné rozloženie, klávesnica, zrozumiteľný stav „obsah zapísaný, chyba dokončenia“, samostatné overenie súboru po chybe a anonymous 403 pre detail/súbor/merge prešli. Demo zostalo bez rozhodnutia.

SQL potvrdenie je vo `final-identities.txt`, súhrn v `verification.json`. Gradle uvádza 154 testov/15 skipov; XML vrátane reprezentácie vypnutých kontajnerov uvádzajú 156/17. V oboch je **139 úspešných a 0 chýb**.

**P05 zachované:** obsah štyroch pracovných tabuliek vetiev #841/#842 je pred a po finálnych skúškach bajtovo rovnaký. SHA-256 **`4228ad98cd0b365f339d19d0c6e65ed81a3d5385b22ba242bbf6e74633c8b912`**, súbory `p05-before.sql`/`p05-after.sql` majú lokálne práva 600. Nové demo stránky balíka #1315 stále nemajú LIVE riadky, starší vlastník má 0 WORK stránkových riadkov. Demo má `DRAFT`, revíziu **25** a nevyriešený konflikt.

POC DB **33316** a server **8086/8446** zostávajú spustené na review. Žiadny Git commit ani P07 sa nevykonal.

## Pripravené nezverejnené demo

Balík **autotest P06 test-1c9f3c9d (#1315)** zostáva `DRAFT` s nevyriešeným konfliktom. Obsahuje 20 pracovných stránok, presun priečinka do nového priečinka, úpravu média starej stránky, nové médium a prílohu. P05 vetvy **#841/#842** sa na publikovanie nepoužívajú.

Prihlásiť sa účtom `tester-p05` na [admin login](http://iwcm.interway.sk:8086/admin/logon/). Heslo je v existujúcom lokálnom `/tmp/webjet-p05-login.properties`, nie v Gite. Vybrať doménu **iwcm.interway.sk**.

1. Otvoriť [prehľad demo balíka](http://iwcm.interway.sk:8086/admin/v9/webpages/web-pages-list/?groupid=1942460#branchCompare=1315) alebo vybrať balík a kliknúť **Zmeny**.
2. Pri stránke **#1943462** kliknúť **Porovnať**. BASE je pôvodná stránka, LIVE má titulok `autotest P06 concurrent LIVE title`, WORK má `autotest P06 WORK 0` a novú URL. Vybrať **Produkcia** alebo **Pracovná verzia** a uložiť voľbu pre celú položku. Vlastnú kombináciu uložiť cez bežný pracovný editor a následne znovu porovnať.
3. Otvoriť [celý náhľad](http://iwcm.interway.sk:8086/admin/webpages/branch-preview/1315/p06-autotest-test-1c9f3c9d/). Skontrolovať Novinky, menu, prekliky a médiá. Výber LIVE/WORK sa prejaví aj tu; verejný obsah sa zatiaľ nemení.
4. V prehľade vykonať nové porovnanie, skontrolovať zoznam a kliknúť **Publikovať**. Potvrdiť zobrazený zoznam. Priebeh zostáva dostupný aj po obnovení stránky.
5. Otvoriť [verejnú stránku](http://iwcm.interway.sk:8086/p06-autotest-test-1c9f3c9d/) a porovnať výsledok s posledným náhľadom. Po úspechu je balík uzavretý; v administrácii vedome vybrať **Produkcia**.

Manifest dema: `/tmp/webjet-p06-manual.properties`. Ostatné publikačné skúšky používajú oddelené vlastné P06 dáta. Príprava dema sa nesmie opakovane spustiť nad existujúcim manifestom.

## Hranice

Podporený a overovaný je uvedený jediný MariaDB/InnoDB profil, jedna inštancia, lokálny spoločný filesystem a reprezentatívna šablóna. Externé doménové úložisko, IwcmFsDB storage, CDN, iné DB/topológie a ľubovoľné zákaznícke hooky nie sú deklarované ako podporené. Preflight odmieta `fulltextExecuteApps`, vlastný `fulltextDataAscMethod` a externé statické úložisko. Bežná file-history používa svoj existujúci JPA mechanizmus mimo obsahovej JDBC transakcie.

Publikovanie je postupné; počas behu môže návštevník krátko vidieť zmiešaný stav. Pri chybe ostávajú už publikované položky verejné. Automatický rollback celého balíka ani všeobecný editor čiastočne aplikovaných položiek sa nepridáva. Cudzia zmena takého medzistavu vyžaduje posúdenie správcu. Zmena URL nevytvára automatický redirect. Platí P05 hranica galérie, archívu, atribútov a adresárových operácií. Plánovanie termínu patrí výhradne P07.

Hlavný agent a dvaja pomocníci pracovali so zdedeným nastavením; presný identifikátor modelu/reasoning je neoverený. Nezávislá kontrola backendu a vlastný diff viedli ku konkrétnym opravám s regresiami. Bez Git commitov, produkčného nasadenia a P07.

## Opakovanie

Java testy spúšťať len pri zastavenom POC app serveri. Izolovaná DB 33316 musí bežať. Pred každým štartom overiť listenery; štart/stop je rovnaký ako v [P04 výsledkoch](P04-vysledky.md#opakovanie-testov-a-štart).

```sh
JAVA_HOME=/Users/jeeff/.sdkman/candidates/java/17.0.18-tem WEBJET_P06_DB=true \
  ./gradlew --offline test -DwebjetDbname=/poolman-local-p01.xml \
  --tests 'sk.iway.iwcm.branch.BranchMergeTest'
```

Bez ďalších fixture flagov po review beží 15 izolovaných integračných testov; tri jednorazové prípravy sa preskočia. Každý beh používa vlastné označené dáta.

Browser scenáre sa spúšťajú zo `src/test/webapp`:

```sh
WEBJET_P06_BROWSER=true CODECEPT_URL=http://iwcm.interway.sk:8086 CODECEPT_SHOW=false \
  npx codeceptjs run tests/webpages/branch-p06.js --steps
```

Po úspešnom browser behu sú zapisovacie fixture uzavreté alebo obnovené. Opakovaný plný beh preto vyžaduje nové vlastné fixture, nie opakovaný merge tých istých ID. Pri zastavenom app serveri archivovať iba `/tmp/webjet-p06-publish.properties` a `/tmp/webjet-p06-partial.properties`, zachovať demo manifest a private bajty a spustiť:

```sh
JAVA_HOME=/Users/jeeff/.sdkman/candidates/java/17.0.18-tem \
WEBJET_P06_DB=true WEBJET_P06_BROWSER_FIXTURE=true \
WEBJET_P06_PARTIAL_FIXTURE=true WEBJET_P06_TRASH_FIXTURE=true \
  ./gradlew --offline test -DwebjetDbname=/poolman-local-p01.xml \
  --tests 'sk.iway.iwcm.branch.BranchMergeTest.prepareBrowserPublication' \
  --tests 'sk.iway.iwcm.branch.BranchMergeTest.prepareRetainedPartialPublication' \
  --tests 'sk.iway.iwcm.branch.BranchMergeTest.subtreeTrashKeepsNestingAndMandatorySnapshotsWhenHistoryDisabled'
```

Potom znova spustiť POC server. Tento postup zároveň overuje pokračovanie v inom JVM procese. Nespúšťať `prepareRetainedDemo` znova a nepoužiť P05 vetvy na publikovanie.

## Úprava po review: audit po commite

Podľa pokynu používateľa publisher zapisuje audit existujúcim `Adminlog.add(...)` až po úspešnom commite obsahu a checkpointu. Nový transakčný overload bol odstránený; `Adminlog.java` sa oproti východisku nemení. Platí to pre DB položky, súbory aj koreň mazaného podstromu. Auditná tabuľka sa vyradila z transakčných kontrol engine/práv. Obsah, história, mediálne väzby a checkpoint zostávajú v spoločnej transakcii.

Pôvodné výsledky vyššie zachytávajú odovzdanie P06. **Overenie po úprave: 18 testov celkom, 15 PASS, 0 chýb, 3 jednorazové fixture preskočené** na izolovanej DB 33316. Dve nové regresie overujú čerstvým nezávislým DB čítaním commit pred volaním existujúceho auditu, jediné volanie pre každú z 25 položiek aj po pokračovaní a nezavolanie auditu pri skutočnom SQL zlyhaní checkpointu s rollbackom obsahu. Reporty sú v `build/test-results/p06/audit-review/`. Pri páde procesu medzi commitom a auditom môže audit chýbať; pokračovanie už hotový dátový krok ani jeho audit neopakuje. Nepridáva sa osobitný mechanizmus doručovania auditu.

## Úprava po review: existujúce perex API

Nový `PerexGroupsService.savePerexGroupsDoc(Connection, ...)` bol odstránený; `PerexGroupsService.java` sa oproti P05 nemení. Publisher pri dokončení stránky po commite používa existujúce `savePerexGroupsDoc(docId, int[])`. Kategórie načíta z čerstvého produkčného riadka po kontrole hashu. Volanie prebehne pred obnovou cache/indexu a `DONE`, takže funguje aj pri pokračovaní po už uloženom dátovom kroku. Zachováva kategorizáciu zvoleného LIVE aj presunu do koša, nemení iné stránky a nevolá multigroup rozšírenie. Odvodený `perex_group_doc` už nepatrí do engine kontroly spoločnej JDBC transakcie; kontrola DB oprávnení zostáva.

Pôvodný test vynútenej SQL chyby perex väzby bol nahradený overením bežného API: chýbajúca kategória zastaví prípravu, publikovanie zosúladí 20 stránok a zachová existujúcu väzbu staršej stránky mimo balíka vrátane jej ID. Opakované volanie nevytvorí duplicitnú väzbu. Sada teraz inicializuje skutočné Spring Data JPA repository proti MariaDB, nie náhradné SQL ani databázový mock. **18 testov celkom, 15 PASS, 0 chýb, 3 jednorazové fixture preskočené**, vrátane LIVE voľby, pokračovania, stabilných ID a obnovovacieho podstromu. Finálne reporty sú `build/test-results/p06/perex-review/java.log` a `TEST-sk.iway.iwcm.branch.BranchMergeTest.xml`. Prvý pokus narazil na nedostatok pamäte v testovacom bootstrape so statickým mockom `Tools`; bootstrap bol nahradený skutočným Spring kontextom a finálny beh prešiel. Celá publikačná browser sada z pôvodného odovzdania sa pri tejto úprave znovu nespúšťala. Demo #1315 zostáva nezverejnené.

Pri záverečnej kontrole už pôvodný POC DB proces nebežal. MariaDB bola znovu spustená nad zachovaným `/tmp/webjet-p03-review-db/data` na 33316 (PID 76342), následne WebJET na 8086/8446 (PID 77118). Read-only kontrola potvrdila P05 #841/#842 v `DRAFT` a P06 #1315 v `DRAFT`, revízia 25. Pracovná DB 3306 sa nemenila.

Po obnove prostredia prešiel existujúci browser scenár pripraveného dema: **1/1 PASS**. Skutočné prihlásenie, porovnanie BASE/LIVE/WORK, binárne verzie a mobilný dialóg; balík ostal `DRAFT`, revízia 25, s nezvoleným konfliktom. Dôkazy `perex-review/browser-demo.log` a `browser-retained-demo.json`.

## Úprava po review: pôvodné prekladové kľúče polí

Odstránené duplicitné názvy polí z SK/CZ/EN bundle. `BranchMergeLabels` načíta pôvodné `@DataTableColumn.title` z entít vrátane zdedeného `DocBasic` a vnorených editorových polí. Mapovanie rozlišuje typ položky: `html_head` používa pre stránku `editor.tab.html_header`, pre priečinok `groupedit.htmlHead`; `field_a` používa pôvodný kľúč stránky, priečinka alebo média. Len pri poliach bez vlastnej anotácie sa odkazuje na príslušný existujúci editorový kľúč. Názvy sa pri každom reporte preložia cez `Prop`, takže rešpektujú načítané jazykové, skinové a zákaznícke úpravy pôvodných kľúčov. Cache obsahuje iba mená kľúčov, nie ich preložené hodnoty. Žiadne JS aliasy pôvodných názvov nevznikajú.

Report vracia `fieldLabels` pre jednotlivé typy a JS ich vkladá cez `textContent`. Nové kľúče polí zostali štyri: typ vlastníka média, popis média, typ súborovej položky a SHA-256 odtlačok. Jazykové varianty média používajú spoločný názov s označením jazyka. Tlačidlá Zavrieť/Zrušiť/Áno/Nie používajú existujúce `button.*`. Počet nových kľúčov oproti P05 klesol zo **176 na 86 v každom z troch jazykov**; spolu o 270 riadkov. Snapshoty, rozhodnutia a publikačný plán sa nemenia.

Overenie: **2/2 Java PASS**, vrátane úplnosti názvov všetkých podporovaných polí, odlišných pôvodných kľúčov podľa typu a zmeny pôvodného prekladu cez skutočné `Prop.getText`. Frontend production build prešiel s dvoma existujúcimi size warnings. Dôkazy `build/test-results/p06/labels-review/`.

Skutočný browser scenár dema: **1/1 PASS**. Po zobrazení nezmenených polí sa názvy `html_head` a `field_a` v merge porovnali s reálnymi DataTables stĺpcami editora: zhodne **HTML kód do hlavičky** a **Pole A**. Overené aj spoločné tlačidlo **Zatvoriť**, súborové verzie a mobilné rozloženie. Demo #1315 ostalo `DRAFT`, revízia 25, bez voľby riešenia. Prvý pokus skončil na `checkOption` pri prerenderovaní checkboxu; scenár používa bežné kliknutie a kontrolu nového stavu. Finálny dôkaz `labels-review/browser-demo.log` a `browser-retained-demo.json`. POC DB 33316 a server 8086/8446 ostávajú spustené.

## Úprava po review: existujúce API histórie súborov

Odstránený nový `IwcmFsDB.saveFileHistory(Connection, IwcmFile, userId)`; `IwcmFsDB.java` je bez diffu oproti P05. Publisher používa pôvodné `saveFileHistory(file, false)` pred nahradením alebo zmazaním súboru, iba pri zapnutej bežnej histórii. `false` uloží pôvodné bajty aj pred DELETE. Autor a doména sa získajú z kontextu publishera, ktorý už obsahuje aktuálneho publikujúceho. Pri neúspešnom návrate sa daný súbor nezmení. Zmizlo duplicitné SQL, vlastné prideľovanie ID a kontrola spoločnej transakcie. História používa pôvodný `FileHistoryDB`/JPA postup; osobitná InnoDB kontrola a požiadavka na vopred inicializovaný file-history generátor boli odstránené.

Po úspešnom uložení histórie nasleduje checkpoint `FILE_STARTED`, potom samotná výmena súboru. Pri pokračovaní po výmene sa neuloží nová história ani nezopakuje nahradenie. Pri prerušení medzi históriou a počiatočným checkpointom môže vzniknúť ďalšia historická kópia; nepridáva sa osobitná transakcia alebo recovery mechanizmus. Súkromná záloha P06 ostáva zachovaná aj pri vypnutej bežnej histórii.

Overenie na izolovanej DB 33316 pri zastavenom app serveri: **20 testov celkom, 17 PASS, 0 chýb, 3 jednorazové fixture preskočené** (`BranchMergeTest` a `BranchMergeLabelsTest`). Existujúce súborové scenáre teraz zapínajú bežnú históriu a kontrolujú skutočné JPA riadky, autora/doménu, pôvodné bajty, zálohu pred DELETE a jediný historický záznam po pokračovaní cez už nahradený súbor. Voľba LIVE bez obsahovej zmeny históriu nevytvorí. Bootstrap testov doplnil skutočné mapovanie legacy `FileHistoryBean`; prvý pokus bez neho skončil na neznámej JPA entite. Testy čistia iba svoje označené historické záznamy a súbory. Finálne reporty sú v `build/test-results/p06/file-history-review/`. Browser sa pre túto backendovú úpravu znovu nespúšťal. Demo #1315 sa nepoužilo na publikovanie.
